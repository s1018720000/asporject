package com.as.webhook.service;

import com.as.webhook.domain.PushObject;
import com.as.webhook.utils.Result;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

public interface IWebhookService {

    public Map<String, Result> doPush(PushObject pushObject, HttpServletRequest request) throws Exception;

    public Map<String, Result> run(String jobId, String elasticId, String apiId, HttpServletRequest request);

    /**
     * 查询webhook请求记录
     *
     * @param id webhook请求记录ID
     * @return webhook请求记录
     */
    public PushObject selectWebhookRecordById(Long id);

    /**
     * 查询webhook请求记录列表
     *
     * @param pushObject webhook请求记录
     * @return webhook请求记录集合
     */
    public List<PushObject> selectWebhookRecordList(PushObject pushObject);

    /**
     * 新增webhook请求记录
     *
     * @param pushObject webhook请求记录
     * @return 结果
     */
    public int insertWebhookRecord(PushObject pushObject);

    /**
     * 修改webhook请求记录
     *
     * @param pushObject webhook请求记录
     * @return 结果
     */
    public int updateWebhookRecord(PushObject pushObject);

    /**
     * 批量删除webhook请求记录
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteWebhookRecordByIds(String ids);

    /**
     * 删除webhook请求记录信息
     *
     * @param id webhook请求记录ID
     * @return 结果
     */
    public int deleteWebhookRecordById(Long id);

}