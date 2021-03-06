
-- ----------------------------
-- SQL模板表
-- ----------------------------
DROP TABLE IF EXISTS SQL_TEMPLATE;
CREATE TABLE SQL_TEMPLATE (
  ID INT(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  EN_NAME VARCHAR(200) NOT NULL COMMENT '模板名称-英文',
  CH_NAME VARCHAR(200) NOT NULL COMMENT '模板名称-中文',
  STATUS CHAR(1) NOT NULL DEFAULT '1' COMMENT '状态（0正常 1停用）',
  PLATFORM CHAR(10) DEFAULT NULL COMMENT '平台',
  JDBC CHAR(100) DEFAULT NULL COMMENT 'JDBC',
  SCRIPT TEXT COMMENT 'SCRIPT',
  REMARK VARCHAR(500) DEFAULT NULL COMMENT '备注',
  CREATE_BY VARCHAR(30) NOT NULL COMMENT '建立人员',
  CREATE_TIME DATETIME NOT NULL COMMENT '建立时间',
  UPDATE_BY VARCHAR(30) DEFAULT NULL COMMENT '修改人员',
  UPDATE_TIME DATETIME DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (ID)
) ENGINE=INNODB DEFAULT CHARSET=UTF8 COMMENT='SQL模板表';

-- ----------------------------
-- SQL模板value表 模板1-N value
-- ----------------------------
DROP TABLE IF EXISTS SQL_TEMPLATE_VALUE;
CREATE TABLE SQL_TEMPLATE_VALUE (
  ID INT(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  TEMPLATE_ID INT(11) NOT NULL COMMENT '模板ID',
  TEMPLATE_VALUE VARCHAR(30) NOT NULL COMMENT '参数键值',
  VALUE_EN_NAME VARCHAR(200) NOT NULL COMMENT '名称-英文',
  VALUE_CH_NAME VARCHAR(200) NOT NULL COMMENT '名称-中文',
  EN_PLACEHOLDER VARCHAR(200) DEFAULT NULL COMMENT '提示信息-英文',
  CH_PLACEHOLDER VARCHAR(200) DEFAULT NULL COMMENT '提示信息-中文',
  HTML_TYPE VARCHAR(30) NOT NULL COMMENT '显示类型',
  PRIMARY KEY (ID)
) ENGINE=INNODB DEFAULT CHARSET=UTF8 COMMENT='SQL模板value表';


-- ----------------------------
-- SQL模板和角色关联表  角色1-N模板
-- ----------------------------
DROP TABLE IF EXISTS SYS_ROLE_TEMPLATE;
CREATE TABLE SYS_ROLE_TEMPLATE (
  ROLE_ID   INT(11) NOT NULL COMMENT '角色ID',
  TEMPLATE_ID   INT(11) NOT NULL COMMENT '模板ID',
  PRIMARY KEY(ROLE_ID, TEMPLATE_ID)
) ENGINE=INNODB CHARSET=UTF8 COMMENT = 'SQL模板和角色关联表';