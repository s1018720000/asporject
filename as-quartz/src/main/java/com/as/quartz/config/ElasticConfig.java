package com.as.quartz.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Elastic配置
 *
 * @author kolin
 */
@Configuration
public class ElasticConfig {
    private Logger logger = LoggerFactory.getLogger(ElasticConfig.class);

    /**
     * pf1 url
     */
    @Value("${elastic.pf1.url}")
    private String pf1Url;

    /**
     * pf1 port
     */
    @Value("${elastic.pf1.port}")
    private int pf1Port;

    /**
     * pf2 url
     */
    @Value("${elastic.pf2.url}")
    private String pf2Url;

    /**
     * pf2 port
     */
    @Value("${elastic.pf2.port}")
    private int pf2Port;

//    @Bean(name = "PF1Elasticsearch")
//    public RestHighLevelClient PF1Client() {
//
//        RestHighLevelClient client = null;
//        try {
//            //UAT  172.27.130.48  172.27.1.48  172.27.101.48
//            //kibana2.pf1uat1-oob.com 172.27.101.48
//            //PROD  172.30.1.48
//            // 该方法接收一个RequestConfig.Builder对象，对该对象进行修改后然后返回。
//            client = new RestHighLevelClient(
//                    RestClient.builder(
//                            new HttpHost(pf1Url, pf1Port, "http"))
//                            .setRequestConfigCallback(requestConfigBuilder -> {
//                                return requestConfigBuilder.setConnectTimeout(5000 * 1000) // 连接超时（默认为1秒）
//                                        .setSocketTimeout(6000 * 1000)// 套接字超时（默认为30秒）//更改客户端的超时限制默认30秒现在改为100*1000分钟
//                                        .setConnectionRequestTimeout(5000 * 1000); //请求超时
//                            }));
//
//            logger.info("PF1 ElasticsearchClient 连接成功 ===");
//        } catch (Exception e) {
//            logger.error(e.getMessage());
//        }
//        return client;
//    }
//
//    @Bean(name = "PF2Elasticsearch")
//    public RestHighLevelClient PF2Client() {
//
//        RestHighLevelClient client = null;
//        try {
//            //UAT  172.27.130.95
//            //log-kafka1.pf2uat1-oob.com 172.27.113.95
//            //PROD  172.30.13.95
//            client = new RestHighLevelClient(
//                    RestClient.builder(
//                            new HttpHost(pf2Url, pf2Port, "http"))
//                            .setRequestConfigCallback(requestConfigBuilder -> {
//                                return requestConfigBuilder.setConnectTimeout(5000 * 1000) // 连接超时（默认为1秒）
//                                        .setSocketTimeout(6000 * 1000)// 套接字超时（默认为30秒）//更改客户端的超时限制默认30秒现在改为100分钟
//                                        .setConnectionRequestTimeout(5000 * 1000); //请求超时
//                            }));
//
//            logger.info("PF2 ElasticsearchClient 连接成功 ===");
//        } catch (Exception e) {
//            logger.error(e.getMessage());
//        }
//        return client;
//    }
}
