package com.as.system.domain;

import com.as.common.core.domain.BaseEntity;
import com.as.common.enums.OnlineStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;

/**
 * 当前在线会话 sys_user_online
 *
 * @author kolin
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SysUserOnline extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 用户会话id
     */
    private String sessionId;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 登录名称
     */
    private String loginName;

    /**
     * 登录IP地址
     */
    private String ipaddr;

    /**
     * 登录地址
     */
    private String loginLocation;

    /**
     * 浏览器类型
     */
    private String browser;

    /**
     * 操作系统
     */
    private String os;

    /**
     * session创建时间
     */
    private Date startTimestamp;

    /**
     * session最后访问时间
     */
    private Date lastAccessTime;

    /**
     * 超时时间，单位为分钟
     */
    private Long expireTime;

    /**
     * 在线状态
     */
    private OnlineStatus status = OnlineStatus.on_line;

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("sessionId" , getSessionId())
                .append("loginName" , getLoginName())
                .append("deptName" , getDeptName())
                .append("ipaddr" , getIpaddr())
                .append("loginLocation" , getLoginLocation())
                .append("browser" , getBrowser())
                .append("os" , getOs())
                .append("status" , getStatus())
                .append("startTimestamp" , getStartTimestamp())
                .append("lastAccessTime" , getLastAccessTime())
                .append("expireTime" , getExpireTime())
                .toString();
    }
}
