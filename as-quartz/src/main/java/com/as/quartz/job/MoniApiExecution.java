package com.as.quartz.job;

import com.as.common.constant.Constants;
import com.as.common.constant.DictTypeConstants;
import com.as.common.utils.DateUtils;
import com.as.common.utils.DictUtils;
import com.as.common.utils.ExceptionUtil;
import com.as.common.utils.StringUtils;
import com.as.common.utils.spring.SpringUtils;
import com.as.quartz.domain.MoniApi;
import com.as.quartz.domain.MoniApiLog;
import com.as.quartz.service.IMoniApiLogService;
import com.as.quartz.service.IMoniApiService;
import com.as.quartz.util.AbstractQuartzJob;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import java.util.Date;

/**
 * SQL检测任务执行类（禁止并发执行）
 *
 * @author kolin
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class MoniApiExecution extends AbstractQuartzJob {
    private static final Logger log = LoggerFactory.getLogger(MoniApiExecution.class);

    /**
     * 创建任务名时使用的前缀，如API-JOB-1
     */
    private static final String JOB_CODE = "API-JOB";

    private final MoniApiLog moniApiLog = new MoniApiLog();

    private MoniApi moniApi = new MoniApi();

    /**
     * 执行方法
     *
     * @param context 工作执行上下文对象
     * @param job     系统计划任务
     * @throws Exception 执行过程中的异常
     */
    @Override
    protected void doExecute(JobExecutionContext context, Object job) throws Exception {
        ResponseEntity<String> response = SpringUtils.getBean(IMoniApiService.class).doUrlCheck(moniApi);
        //保存执行结果
        moniApiLog.setExecuteResult(response.getStatusCode().toString());
        if (response.getStatusCodeValue() == Integer.parseInt(moniApi.getExpectedCode())) {
            moniApiLog.setStatus(Constants.SUCCESS);
            moniApiLog.setAlertStatus(Constants.FAIL);
        } else {
            moniApiLog.setStatus(Constants.FAIL);
            moniApiLog.setAlertStatus(Constants.SUCCESS);

            //发送告警
            if (Constants.SUCCESS.equals(moniApi.getTelegramAlert())) {
                SendResponse sendResponse = sendTelegram();
                if (!sendResponse.isOk()) {
                    moniApiLog.setStatus(Constants.ERROR);
                    moniApiLog.setExceptionLog("Telegram send message error: ".concat(sendResponse.description()));
                } else {
                    //更新最后告警时间
                    moniApi.setLastAlert(DateUtils.getNowDate());
                    SpringUtils.getBean(IMoniApiService.class).updateMoniApiLastAlertTime(moniApi);
                }
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
        moniApi = (MoniApi) job;
        moniApiLog.setStartTime(new Date());
        moniApiLog.setApiId(moniApi.getId());
        moniApiLog.setExpectedCode(moniApi.getExpectedCode());
        //输出日志
        log.info("[API检测任务]任务ID:{},任务名称:{},准备执行",
                moniApi.getId(), moniApi.getChName());
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
            moniApiLog.setStatus(Constants.ERROR);
            moniApiLog.setAlertStatus(Constants.SUCCESS);
            moniApiLog.setExceptionLog(ExceptionUtil.getExceptionMessage(e).replace("\"", "'"));
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
        moniApiLog.setEndTime(new Date());
        long runTime = (moniApiLog.getEndTime().getTime() - moniApiLog.getStartTime().getTime()) / 1000;
        moniApiLog.setExecuteTime(runTime);
        String operator = (String) context.getMergedJobDataMap().get("operator");
        if (StringUtils.isNotEmpty(operator)) {
            moniApiLog.setOperator(operator);
        } else {
            moniApiLog.setOperator("system");
        }

        //插入日志到数据库中
        SpringUtils.getBean(IMoniApiLogService.class).addJobLog(moniApiLog);
        //输出日志
        log.info("[API检测任务]任务ID:{},任务名称:{},开始时间:{},结束时间:{},执行结束,耗时：{}秒,执行状态:{}",
                moniApi.getId(), moniApi.getChName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, moniApiLog.getStartTime()),
                DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, moniApiLog.getEndTime()), runTime, Constants.SUCCESS.equals(moniApiLog.getStatus()) ? "Success" : "failed");
    }


    private SendResponse sendTelegram() throws Exception {
        String telegramConfig = DictUtils.getDictRemark(DictTypeConstants.TELEGRAM_NOTICE_GROUP, moniApi.getTelegramConfig());
        if (StringUtils.isEmpty(telegramConfig)) {
            //若是沒有设置telegram通知群组,则抛出例外
            throw new Exception("Cant find any telegram group setting");
        }
        String[] tgData = telegramConfig.split(";");
        if (tgData.length != 2) {
            //若是数量不等于2，则配置错误
            throw new Exception("telegram group Configuration error, please check");
        }
        String telegramInfo = moniApi.getTelegramInfo();
        telegramInfo = telegramInfo.replace("{id}", String.valueOf(moniApiLog.getApiId()))
                .replace("{asid}", moniApi.getAsid())
                .replace("{priority}", moniApi.getPriority() == "1" ? "NU" : "URG")
                .replace("{zh_name}", moniApi.getChName())
                .replace("{en_name}", moniApi.getEnName())
                .replace("{platform}", DictUtils.getDictLabel(DictTypeConstants.UB8_PLATFORM_TYPE, moniApi.getPlatform()))
                .replace("{descr}", moniApi.getDescr())
                .replace("{result}", moniApiLog.getExecuteResult());
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
     * @param moniApi
     * @return
     */
    public static MoniApiExecution buildJob(MoniApi moniApi) {
        MoniApiExecution moniApiExecution = new MoniApiExecution();
        moniApiExecution.setId(String.valueOf(moniApi.getId()));
        moniApiExecution.setCronExpression(moniApi.getCronExpression());
        moniApiExecution.setStatus(moniApi.getStatus());
        moniApiExecution.setJobPlatform(moniApi.getPlatform());
        moniApiExecution.setJobContent(moniApi);
        return moniApiExecution;
    }

}