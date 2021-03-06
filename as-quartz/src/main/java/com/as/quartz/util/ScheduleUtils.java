package com.as.quartz.util;

import com.as.common.config.ASConfig;
import com.as.common.constant.DictTypeConstants;
import com.as.common.constant.ScheduleConstants;
import com.as.common.utils.DictUtils;
import com.as.common.utils.StringUtils;
import com.as.common.utils.spring.SpringUtils;
import com.as.webhook.utils.SubStrUtil;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.SendDocument;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.response.SendResponse;
import org.quartz.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 定时任务工具类
 *
 * @author kolin
 */
public class ScheduleUtils {
    //详见tg bot api文档：https://core.telegram.org/bots/api#formatting-options
    public static ParseMode parseMode = ParseMode.MarkdownV2;

    /**
     * 得到quartz任务类
     *
     * @param job 执行计划
     * @return 具体执行任务类
     */
    private static Class<? extends Job> getQuartzJobClass(AbstractQuartzJob job) {
        return job.getClass();
    }

    /**
     * 构建任务触发对象
     */
    public static TriggerKey getTriggerKey(String jobId, String jobGroup) {
        return TriggerKey.triggerKey(jobId, jobGroup);
    }

    /**
     * 构建任务键对象
     */
    public static JobKey getJobKey(String jobCode, String jobGroup) {
        return JobKey.jobKey(jobCode, jobGroup);
    }

    /**
     * 创建定时任务
     */
    public static void createScheduleJob(Scheduler scheduler, AbstractQuartzJob job) throws SchedulerException {
        Class<? extends Job> jobClass = getQuartzJobClass(job);
        // 构建job信息
        String jobCode = job.toString();
        String jobGroup = job.getJobGroup();
        JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(getJobKey(jobCode, jobGroup)).build();

        // 表达式调度构建器
        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(job.getCronExpression());

        // 按新的cronExpression表达式构建一个新的trigger
        CronTrigger trigger = TriggerBuilder.newTrigger().withIdentity(getTriggerKey(jobCode, jobGroup))
                .withSchedule(cronScheduleBuilder).build();

        // 放入参数，运行时的方法可以获取
        jobDetail.getJobDataMap().put(ScheduleConstants.TASK_PROPERTIES, job.getJobContent());

        // 判断是否存在
        if (scheduler.checkExists(getJobKey(jobCode, jobGroup))) {
            // 防止创建时存在数据问题 先移除，然后在执行创建操作
            scheduler.deleteJob(getJobKey(jobCode, jobGroup));
        }

        scheduler.scheduleJob(jobDetail, trigger);

        // 暂停任务
        if (job.getStatus().equals(ScheduleConstants.Status.PAUSE.getValue())) {
            scheduler.pauseJob(ScheduleUtils.getJobKey(jobCode, jobGroup));
        }
    }


    /**
     * 获取TG配置
     */
    public static String[] getTgData(String telegramConfig, boolean isWebhook) throws Exception {
        String[] tgData = new String[2];
        //如果是dev环境则返回测试群组
        if ("dev".equals(SpringUtils.getActiveProfile())) {
            tgData[0] = "1937111623:AAHDVpT1bezDDJ_Lf7HmyYCRd8mZeSlHCwM";
            tgData[1] = "-532553117";
//            tgData[1] = "736145377";
            return tgData;
        }

        String config = DictUtils.getDictRemark(DictTypeConstants.TELEGRAM_NOTICE_GROUP, telegramConfig);
        if (StringUtils.isEmpty(config)) {
            //若是沒有设置telegram通知群组,则抛出例外
            throw new Exception("Cant find telegram group setting");
        }
        String[] tgDataTmp = config.split(";");
        if (tgDataTmp.length < 2) {
            //若是数量小于2，则配置错误
            throw new Exception("telegram group Configuration error, please check");
        }
        if (tgDataTmp.length > 2) {
            tgData[0] = tgDataTmp[1];
            tgData[1] = tgDataTmp[2];
            if (isWebhook) {
                tgData[0] = tgDataTmp[0];
            }
        } else {
            tgData[0] = tgDataTmp[0];
            tgData[1] = tgDataTmp[1];
        }
        return tgData;
    }

    public static InlineKeyboardMarkup getInlineKeyboardMarkup(String jobUrl, String logUrl, String jobId, String logId, String kibanaUrl) {
        //新增List方便未來新增
        List<InlineKeyboardButton> Lists = new ArrayList<>();
        Lists.add(new InlineKeyboardButton("JOB Details").url(ASConfig.getAsDomain().concat(jobUrl).concat(jobId)));
        Lists.add(new InlineKeyboardButton("LOG Details").url(ASConfig.getAsDomain().concat(logUrl).concat(logId)));
        Lists.add(new InlineKeyboardButton("Kibana Link").url(kibanaUrl));
        //陣列排序
        InlineKeyboardButton[][] Buttons;
        //判斷kibanaUrl為空值或是null
        if(kibanaUrl!=null && !"".equals(kibanaUrl)){
            Buttons= new InlineKeyboardButton[][]{
                    {Lists.get(0), Lists.get(1)},
                    {Lists.get(2)}
            };
        }else{
            Buttons= new InlineKeyboardButton[][]{
                    {Lists.get(0), Lists.get(1)}
            };
        }
        return new InlineKeyboardMarkup(Buttons);
    }

    /**
     * 发送tg告警 文字
     */
    public static SendResponse sendMessage(String bot, String chatId, String telegramInfo, InlineKeyboardMarkup inlineKeyboard) {
        TelegramBot messageBot = new TelegramBot.Builder(bot).okHttpClient(OkHttpUtils.getInstance()).build();
        SendMessage sendMessage = new SendMessage(chatId, telegramInfo).parseMode(parseMode);
        if (StringUtils.isNotNull(inlineKeyboard)) {
            sendMessage.replyMarkup(inlineKeyboard);
        }
        return messageBot.execute(sendMessage);
    }

    /**
     * webhook tg push
     */
    public static SendResponse sendMessageForWebhook(String bot, String chatId, String telegramInfo, InlineKeyboardMarkup inlineKeyboard) {
        TelegramBot messageBot = new TelegramBot.Builder(bot).okHttpClient(OkHttpUtils.getInstance()).build();
        SendMessage sendMessage = new SendMessage(chatId, telegramInfo).parseMode(parseMode);
        if (StringUtils.isNotNull(inlineKeyboard)) {
            sendMessage.replyMarkup(inlineKeyboard);
        }
        SendResponse response = null;
        try {
            response = messageBot.execute(sendMessage);
            return response;
        } catch (Exception e) {
            //发送失败拆分字符后重新发送一次
            int length = 500;
            if (telegramInfo.length() > length) {
                List<String> telegramInfoList = SubStrUtil.getStrList(telegramInfo, length);
                for (String info : telegramInfoList) {
                    TelegramBot resendBot = new TelegramBot.Builder(bot).okHttpClient(OkHttpUtils.getInstance()).build();
                    SendMessage resendMessage = new SendMessage(chatId, removeMarkdown(info));
                    response = resendBot.execute(resendMessage);
                }
                return response;
            } else {
                throw e;
            }
        }
    }

    /**
     * 发送tg告警 图片
     */
    public static SendResponse sendPhoto(String bot, String chatId, String telegramInfo, InlineKeyboardMarkup inlineKeyboard, File file) {
        TelegramBot photoBot = new TelegramBot.Builder(bot).okHttpClient(OkHttpUtils.getInstance()).build();
        SendPhoto sendPhoto = new SendPhoto(chatId, file);
        sendPhoto.caption(telegramInfo).parseMode(parseMode);
        if (StringUtils.isNotNull(inlineKeyboard)) {
            sendPhoto.replyMarkup(inlineKeyboard);
        }
        return photoBot.execute(sendPhoto);
    }

    /**
     * 发送tg告警 文件形式
     */
    public static SendResponse sendDocument(String bot, String chatId, String telegramInfo, InlineKeyboardMarkup inlineKeyboard, File file) {
        TelegramBot documentBot = new TelegramBot.Builder(bot).okHttpClient(OkHttpUtils.getInstance()).build();
        SendDocument sendDocument = new SendDocument(chatId, file);
        sendDocument.caption(telegramInfo).parseMode(parseMode);
        if (StringUtils.isNotNull(inlineKeyboard)) {
            sendDocument.replyMarkup(inlineKeyboard);
        }
        return documentBot.execute(sendDocument);
    }

    public static void deleteMessage(TelegramBot bot, String chatId, Integer messageId) {
        if (StringUtils.isNotNull(messageId)) {
            DeleteMessage deleteMessage = new DeleteMessage(chatId, messageId);
            bot.execute(deleteMessage);
        }
    }


    public static String processStr(String str) {
        return str.replace("\\", "")
                .replace("*", "\\*")
                .replace("_", "\\_")
                .replace("`", "\\`")
                .replace("]", "\\]")
                .replace("[", "\\[")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("~", "\\~")
                .replace(">", "\\>")
                .replace("#", "\\#")
                .replace("+", "\\+")
                .replace("-", "\\-")
                .replace("=", "\\=")
                .replace("|", "\\|")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace(".", "\\.")
                .replace("!", "\\!");
    }

    public static String removeMarkdown(String str) {
        return str.replace("\\", "")
                .replace("*", "")
                .replace("_", "")
                .replace("__", "")
                .replace("`", "")
                .replace("~", "");
    }
}