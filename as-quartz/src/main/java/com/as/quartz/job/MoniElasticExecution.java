package com.as.quartz.job;

import com.as.common.constant.Constants;
import com.as.common.constant.DictTypeConstants;
import com.as.common.constant.ScheduleConstants;
import com.as.common.utils.DateUtils;
import com.as.common.utils.DictUtils;
import com.as.common.utils.ExceptionUtil;
import com.as.common.utils.StringUtils;
import com.as.common.utils.spring.SpringUtils;
import com.as.quartz.domain.MoniElastic;
import com.as.quartz.domain.MoniElasticLog;
import com.as.quartz.service.IMoniElasticLogService;
import com.as.quartz.service.IMoniElasticService;
import com.as.quartz.util.AbstractQuartzJob;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;

/**
 * SQL检测任务执行类（禁止并发执行）
 *
 * @author kolin
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class MoniElasticExecution extends AbstractQuartzJob {
    private static final Logger log = LoggerFactory.getLogger(MoniElasticExecution.class);

    /**
     * 创建任务名时使用的前缀，如ELASTIC-JOB-1
     */
    private static final String JOB_CODE = "ELASTIC-JOB";

    private final MoniElasticLog moniElasticLog = new MoniElasticLog();

    private MoniElastic moniElastic = new MoniElastic();

    /**
     * 执行方法
     *
     * @param context 工作执行上下文对象
     * @param job     系统计划任务
     * @throws Exception 执行过程中的异常
     */
    @Override
    protected void doExecute(JobExecutionContext context, Object job) throws Exception {
        SearchResponse searchResponse = SpringUtils.getBean(IMoniElasticService.class).doElasticSearch(moniElastic);
        SearchHit[] hits = searchResponse.getHits().getHits();
        //储存结果
        String result = String.format("find %s hits", hits.length);
        moniElasticLog.setExecuteResult(result);
        if (ScheduleConstants.MATCH_NO_NEED.equals(moniElastic.getAutoMatch())) {
            moniElasticLog.setExpectedResult("No need match");
            moniElasticLog.setStatus(Constants.SUCCESS);
            moniElasticLog.setAlertStatus(Constants.FAIL);
        } else if (doMatch(hits)) {
            Map<String, String> compareResult = null;
            if (hits.length > 0 && hits[0].getSourceAsString().contains("win info")) {
                compareResult = SpringUtils.getBean(IMoniElasticService.class).doPf1DrawCompare(hits);
                doCompare(compareResult);
            } else if (hits.length > 0 && hits[0].getSourceAsString().contains("win_nos")) {
                compareResult = SpringUtils.getBean(IMoniElasticService.class).doPf2DrawCompare(hits);
                doCompare(compareResult);
            } else {
                moniElasticLog.setStatus(Constants.FAIL);
                moniElasticLog.setAlertStatus(Constants.SUCCESS);
                sendAlert();
            }
        } else {
            moniElasticLog.setStatus(Constants.SUCCESS);
            moniElasticLog.setAlertStatus(Constants.FAIL);
        }

    }

    private void doCompare(Map<String, String> compareResult) throws Exception {
        String result;
        String index = StringUtils.isNull(compareResult) ? "0" : compareResult.get("index");
        if (!"0".equals(index)) {
            result = String.format("find %s hits;\n", index) + compareResult.get("result");
            moniElasticLog.setExecuteResult(result);
            moniElasticLog.setStatus(Constants.FAIL);
            moniElasticLog.setAlertStatus(Constants.SUCCESS);
            sendAlert();
        } else {
            result = "find 0 hits";
            moniElasticLog.setExecuteResult(result);
            moniElasticLog.setStatus(Constants.SUCCESS);
            moniElasticLog.setAlertStatus(Constants.FAIL);
        }
    }

    private void sendAlert() throws Exception {
        //发送告警
        if (Constants.SUCCESS.equals(moniElastic.getTelegramAlert())) {
            SendResponse sendResponse = sendTelegram();
            if (!sendResponse.isOk()) {
                moniElasticLog.setStatus(Constants.ERROR);
                moniElasticLog.setExceptionLog("Telegram send message error: ".concat(sendResponse.description()));
            } else {
                //更新最后告警时间
                moniElastic.setLastAlert(DateUtils.getNowDate());
                SpringUtils.getBean(IMoniElasticService.class).updateMoniElasticLastAlertTime(moniElastic);
            }
        }
    }

    /**
     * 任务执行前方法，在doExecute()方法前执行
     *
     * @param context 工作执行上下文对象
     */
    @Override
    protected void before(JobExecutionContext context, Object job) {
        moniElastic = (MoniElastic) job;
        moniElasticLog.setStartTime(new Date());
        moniElasticLog.setElasticId(moniElastic.getId());
        moniElasticLog.setExpectedResult(moniElastic.getExpectedResult());
        //输出日志
        log.info("[Elastic任务]任务ID:{},任务名称:{},准备执行",
                moniElastic.getId(), moniElastic.getChName());
    }

    /**
     * 执行后方法，在doExecute()方法后执行
     *
     * @param context 工作执行上下文对象
     * @param job     系统计划任务
     */
    @Override
    protected void after(JobExecutionContext context, Object job, Exception e) {
        if (e != null) {
            moniElasticLog.setStatus(Constants.ERROR);
            moniElasticLog.setAlertStatus(Constants.SUCCESS);
            moniElasticLog.setExceptionLog(ExceptionUtil.getExceptionMessage(e).replace("\"", "'"));
        }
    }

    /**
     * finally中执行方法
     *
     * @param context 工作执行上下文对象
     * @param job     系统计划任务
     */
    @Override
    protected void doFinally(JobExecutionContext context, Object job) {
        moniElasticLog.setEndTime(new Date());
        long runTime = (moniElasticLog.getEndTime().getTime() - moniElasticLog.getStartTime().getTime()) / 1000;
        moniElasticLog.setExecuteTime(runTime);
        String operator = (String) context.getMergedJobDataMap().get("operator");
        if (StringUtils.isNotEmpty(operator)) {
            moniElasticLog.setOperator(operator);
        } else {
            moniElasticLog.setOperator("system");
        }

        //插入日志到数据库中
        SpringUtils.getBean(IMoniElasticLogService.class).addJobLog(moniElasticLog);
        //输出日志
        log.info("[Elastic任务]任务ID:{},任务名称:{},开始时间:{},结束时间:{},执行结束,耗时：{}秒,执行状态:{}",
                moniElastic.getId(), moniElastic.getChName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, moniElasticLog.getStartTime()),
                DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, moniElasticLog.getEndTime()), runTime, Constants.SUCCESS.equals(moniElasticLog.getStatus()) ? "Success" : "failed");
    }

    /**
     * 比对获取结果与预期结果
     *
     * @throws Exception
     */
    private boolean doMatch(SearchHit[] hits) throws Exception {

        String autoMatch = moniElastic.getAutoMatch();

        int rows = hits.length;

        //大于比对
        if (ScheduleConstants.MATCH_GREATER.equals(autoMatch)) {
            moniElasticLog.setExpectedResult("Execute Result Greater than [" + moniElastic.getExpectedResult() + "]");
            return rows > Integer.parseInt(moniElastic.getExpectedResult());
        }

        //小于比对
        if (ScheduleConstants.MATCH_LESS.equals(autoMatch)) {
            moniElasticLog.setExpectedResult("Execute Result Less than [" + moniElastic.getExpectedResult() + "]");
            return rows < Integer.parseInt(moniElastic.getExpectedResult());
        }

        // 等于比对
        if (ScheduleConstants.MATCH_EQUAL.equals(autoMatch)) {
            moniElasticLog.setExpectedResult("Execute Result Equal to [" + moniElastic.getExpectedResult() + "]");
            return rows < Integer.parseInt(moniElastic.getExpectedResult());
        }

        //	不等于比对
        if (ScheduleConstants.MATCH_NOT_EQUAL.equals(autoMatch)) {
            moniElasticLog.setExpectedResult("Execute Result not Equal to [" + moniElastic.getExpectedResult() + "]");
            return rows != Integer.parseInt(moniElastic.getExpectedResult());
        }

        // 无资料
        if (ScheduleConstants.MATCH_EMPTY.equals(autoMatch)) {
            moniElasticLog.setExpectedResult("Execute Result is empty");
            return rows == 0;
        }

        // 有资料
        if (ScheduleConstants.MATCH_NOT_EMPTY.equals(autoMatch)) {
            moniElasticLog.setExpectedResult("Execute Result is not empty");
            return rows != 0;
        }

        //若是沒有進行到比對代表有問題，需拋出例外。
        throw new Exception("Never do match function");
    }


    private SendResponse sendTelegram() throws Exception {
        String telegramConfig = DictUtils.getDictRemark(DictTypeConstants.TELEGRAM_NOTICE_GROUP, moniElastic.getTelegramConfig());
        if (StringUtils.isEmpty(telegramConfig)) {
            //若是沒有设置telegram通知群组,则抛出例外
            throw new Exception("Cant find any telegram group setting");
        }
        String[] tgData = telegramConfig.split(";");
        if (tgData.length != 2) {
            //若是数量不等于2，则配置错误
            throw new Exception("telegram group Configuration error, please check");
        }
        String telegramInfo = moniElastic.getTelegramInfo();
        telegramInfo = telegramInfo.replace("{id}", String.valueOf(moniElasticLog.getElasticId()))
                .replace("{asid}", moniElastic.getAsid())
                .replace("{priority}", moniElastic.getPriority() == "1" ? "NU" : "URG")
                .replace("{zh_name}", moniElastic.getChName())
                .replace("{en_name}", moniElastic.getEnName())
                .replace("{platform}", DictUtils.getDictLabel(DictTypeConstants.UB8_PLATFORM_TYPE, moniElastic.getPlatform()))
                .replace("{descr}", moniElastic.getDescr())
                .replace("{result}", moniElasticLog.getExecuteResult().replace(";", ""));
        TelegramBot telegramBot = new TelegramBot(tgData[0]);

        SendMessage sendMessage = new SendMessage(tgData[1], telegramInfo).parseMode(ParseMode.Markdown);
//        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup(
//                new InlineKeyboardButton("View log details in webpage").url(ASConfig.getAsDomain().concat(DETAIL_URL).concat(String.valueOf(moniJobLog.getId()))));
//        sendPhoto.replyMarkup(inlineKeyboard);
        return telegramBot.execute(sendMessage);
    }

    /**
     * 使用toString方法构建任务名
     *
     * @return
     */
    @Override
    public String toString() {
        return JOB_CODE + "-" + id;
    }


    /**
     * 静态方法，获取一个任务执行对象
     *
     * @param moniElastic
     * @return
     */
    public static MoniElasticExecution buildJob(MoniElastic moniElastic) {
        MoniElasticExecution moniElasticExecution = new MoniElasticExecution();
        moniElasticExecution.setId(String.valueOf(moniElastic.getId()));
        moniElasticExecution.setCronExpression(moniElastic.getCronExpression());
        moniElasticExecution.setStatus(moniElastic.getStatus());
        moniElasticExecution.setJobPlatform(moniElastic.getPlatform());
        moniElasticExecution.setJobContent(moniElastic);
        return moniElasticExecution;
    }

}