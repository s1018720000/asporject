package com.as.webhook.controller;

import com.as.common.annotation.Log;
import com.as.common.config.ASConfig;
import com.as.common.constant.Constants;
import com.as.common.constant.DictTypeConstants;
import com.as.common.core.controller.BaseController;
import com.as.common.core.domain.AjaxResult;
import com.as.common.core.page.TableDataInfo;
import com.as.common.enums.BusinessType;
import com.as.common.utils.DictUtils;
import com.as.common.utils.poi.ExcelUtil;
import com.as.webhook.domain.PushObject;
import com.as.webhook.service.IWebhookService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * webhook请求记录Controller
 *
 * @author kolin
 * @date 2021-09-07
 */
@Controller
@RequestMapping("/as/webhook")
public class ASHookManageController extends BaseController {
    private String prefix = "webhook";

    @Autowired
    private IWebhookService webhookService;

    @RequiresPermissions("as:webhook:view")
    @GetMapping()
    public String webhook() {
        return prefix + "/webhook";
    }

    /**
     * 查询webhook请求记录列表
     */
    @RequiresPermissions("as:webhook:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(PushObject pushObject) {
        startPage();
        List<PushObject> list = webhookService.selectWebhookRecordList(pushObject);
        return getDataTable(list);
    }

    /**
     * 导出webhook请求记录列表
     */
    @RequiresPermissions("as:webhook:export")
    @Log(title = "webhook请求记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(PushObject pushObject) {
        List<PushObject> list = webhookService.selectWebhookRecordList(pushObject);
        ExcelUtil<PushObject> util = new ExcelUtil<PushObject>(PushObject.class);
        return util.exportExcel(list, "webhook请求记录数据");
    }

    /**
     * 新增webhook请求记录
     */
    @GetMapping("/add")
    public String add() {
        return prefix + "/add";
    }

    /**
     * 删除webhook请求记录
     */
    @RequiresPermissions("as:webhook:remove")
    @Log(title = "webhook请求记录", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        return toAjax(webhookService.deleteWebhookRecordByIds(ids));
    }

    /**
     * 日志详情
     *
     * @param id
     * @param mmap
     * @return
     */
    @RequiresPermissions("as:webhook:detail")
    @GetMapping("/detail/{id}")
    public String detail(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("name", "webhookRecord");
        PushObject pushObject = webhookService.selectWebhookRecordById(id);
        mmap.put("webhookRecord", pushObject);
        return prefix + "/detail";
    }

    /**
     * webhook详情
     *
     * @param mmap
     * @return
     */
    @RequiresPermissions("as:webhook:detail")
    @GetMapping("/hookDetail")
    public String hookDetail(ModelMap mmap) {
        mmap.put("pushUrl", ASConfig.getAsDomain() + "/webhook/push");
        mmap.put("method", "GET or POST");
        mmap.put("pushParam", "{\"type\":\"log/tg/mail\",\"asid\":\"\",\"title\":\"webhook push\",\"descr\":\"webhook push\",\"remark\":\"webhook push\",\"tgId\":\"as_test\",\"reporter\":\"kolin\",\"mailAdd\":\"c98fb80a.my-cpg.com@apac.teams.ms\"}");
        mmap.put("cbUrl", ASConfig.getAsDomain() + "/webhook/cb");
        mmap.put("cbParam", "{\"reporter\":\"kolin\",\"job\":999,\"elastic\":999,\"api\":999}");
        return prefix + "/hookDetail";
    }

    /**
     * 推送模板
     *
     * @param mmap
     * @return
     */
    @RequiresPermissions("as:webhook:view")
    @GetMapping("/tgPushTemplate")
    public String tgPushTemplate(ModelMap mmap) {
        String apiTemplate = DictUtils.getDictRemark(DictTypeConstants.JOB_PUSH_TEMPLATE, Constants.DESCR_TEMPLATE_WEBHOOK);
        mmap.put("template", apiTemplate);
        return prefix + "/tgTemplate";
    }

    /**
     * 保存推送模板
     *
     * @param template
     * @return
     */
    @RequiresPermissions("as:webhook:view")
    @GetMapping("/tgPushTemplateSave")
    @Log(title = "webhook请求记录", businessType = BusinessType.UPDATE)
    @ResponseBody
    public AjaxResult tgPushTemplateSave(@RequestParam(name = "template") String template) {
        return toAjax(webhookService.updateTgTemplate(template));
    }

    /**
     * 推送模板
     *
     * @param mmap
     * @return
     */
    @RequiresPermissions("as:webhook:view")
    @GetMapping("/mailPushTemplate")
    public String mailPushTemplate(ModelMap mmap) {
        String apiTemplate = DictUtils.getDictRemark(DictTypeConstants.JOB_PUSH_TEMPLATE, Constants.MAIL_TEMPLATE_WEBHOOK);
        mmap.put("template", apiTemplate);
        return prefix + "/mailTemplate";
    }

    /**
     * 保存推送模板
     *
     * @param template
     * @return
     */
    @RequiresPermissions("as:webhook:view")
    @GetMapping("/mailPushTemplateSave")
    @Log(title = "webhook请求记录", businessType = BusinessType.UPDATE)
    @ResponseBody
    public AjaxResult mailPushTemplateSave(@RequestParam(name = "template") String template) {
        return toAjax(webhookService.updateMailTemplate(template));
    }
}
