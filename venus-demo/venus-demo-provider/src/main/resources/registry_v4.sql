/*
MySQL Data Transfer
Source Host: 10.32.173.250
Source Database: registry_new
Target Host: 10.32.173.250
Target Database: registry_new
Date: 2017/11/14 10:13:09
*/

SET FOREIGN_KEY_CHECKS=0;
-- ----------------------------
-- Table structure for t_admin
-- ----------------------------
DROP TABLE IF EXISTS `t_admin`;
CREATE TABLE `t_admin` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(32) NOT NULL COMMENT '用户名',
  `fullname` varchar(64) NOT NULL COMMENT '管理员姓名',
  `active` tinyint(1) NOT NULL COMMENT '激活态 0 - 关闭， 1- 开启',
  `create_time` timestamp NULL DEFAULT NULL,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username_UNIQUE` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=33 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for t_venus_application
-- ----------------------------
DROP TABLE IF EXISTS `t_venus_application`;
CREATE TABLE `t_venus_application` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `app_code` varchar(100) NOT NULL,
  `provider` tinyint(1) DEFAULT NULL COMMENT '是否为注册方应用 0否1是',
  `consumer` tinyint(1) DEFAULT NULL COMMENT '是否为订阅方应用 0否1是',
  `create_name` varchar(50) DEFAULT NULL,
  `update_name` varchar(50) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `app_code_UNIQUE` (`app_code`)
) ENGINE=InnoDB AUTO_INCREMENT=2413 DEFAULT CHARSET=utf8 COMMENT='应用名表';

-- ----------------------------
-- Table structure for t_venus_server
-- ----------------------------
DROP TABLE IF EXISTS `t_venus_server`;
CREATE TABLE `t_venus_server` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `hostname` varchar(36) NOT NULL COMMENT 'Venus服务主机ip或主机名',
  `port` int(11) NOT NULL COMMENT 'Venus服务端口  订阅方的端口用0表示 ',
  `create_time` datetime DEFAULT NULL,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_hostname_port` (`hostname`,`port`)
) ENGINE=InnoDB AUTO_INCREMENT=1324 DEFAULT CHARSET=utf8 COMMENT='Venus服务器表';

-- ----------------------------
-- Table structure for t_venus_service
-- ----------------------------
DROP TABLE IF EXISTS `t_venus_service`;
CREATE TABLE `t_venus_service` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) COLLATE utf8_bin DEFAULT NULL COMMENT 'Venus服务名称',
  `interface_name` varchar(256) COLLATE utf8_bin DEFAULT NULL,
  `version` varchar(45) COLLATE utf8_bin DEFAULT NULL,
  `version_range` varchar(45) COLLATE utf8_bin DEFAULT NULL COMMENT '兼容老的版本号',
  `description` varchar(256) COLLATE utf8_bin NOT NULL COMMENT 'Venus服务描述',
  `app_id` int(11) DEFAULT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1' COMMENT '服务总开关 0 - 关闭， 1- 开启',
  `create_time` timestamp NULL DEFAULT NULL,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `registe_type` int(11) DEFAULT '0' COMMENT '注册类型：0 手动：1 自动',
  `methods` varchar(16392) COLLATE utf8_bin DEFAULT NULL COMMENT '接口对应方法',
  `is_delete` tinyint(1) DEFAULT NULL COMMENT '是否删除：0否，1 是',
  PRIMARY KEY (`id`),
  KEY `idx_appid` (`app_id`)
) ENGINE=InnoDB AUTO_INCREMENT=12007 DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='Venus服务表';

-- ----------------------------
-- Table structure for t_venus_service_config
-- ----------------------------
DROP TABLE IF EXISTS `t_venus_service_config`;
CREATE TABLE `t_venus_service_config` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `type` int(11) DEFAULT NULL COMMENT '1- 路由规则，2-流控配置，3-降级配置',
  `config` varchar(500) DEFAULT NULL,
  `service_id` int(11) DEFAULT NULL,
  `create_name` varchar(45) DEFAULT NULL,
  `update_name` varchar(45) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=103 DEFAULT CHARSET=utf8 COMMENT='服务配置表';

-- ----------------------------
-- Table structure for t_venus_service_mapping
-- ----------------------------
DROP TABLE IF EXISTS `t_venus_service_mapping`;
CREATE TABLE `t_venus_service_mapping` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `service_id` int(11) NOT NULL COMMENT 'Venus服务id',
  `server_id` int(11) NOT NULL COMMENT 'Venus服务器id',
  `provider_app_id` int(11) DEFAULT NULL COMMENT '提供服务的应用ID',
  `consumer_app_id` int(11) DEFAULT NULL COMMENT '消费服务的应用ID',
  `version` varchar(64) DEFAULT NULL COMMENT 'Venus服务版本',
  `active` tinyint(1) NOT NULL COMMENT '是否激活服务 0 - 关闭， 1  -  开启',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '运行状态  0 - 正常， 1  -  流控中， 2 - 降级中',
  `sync` tinyint(1) NOT NULL COMMENT '是否注册中心与服务端同步',
  `role` varchar(45) DEFAULT NULL COMMENT '角色：provider注册服务 consumer订阅服务',
  `weight` int(11) DEFAULT '1',
  `create_time` datetime DEFAULT NULL,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `registe_time` datetime DEFAULT NULL COMMENT '注册时间',
  `heartbeat_time` datetime DEFAULT NULL COMMENT '心跳时间',
  `is_delete` tinyint(1) DEFAULT NULL COMMENT '是否逻辑删除 0否 1是',
  `has_heartbeat` tinyint(1) DEFAULT NULL COMMENT '是否有过心跳 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_provider_id` (`provider_app_id`),
  KEY `idx_consumer_id` (`consumer_app_id`),
  KEY `idx_serviceid` (`service_id`),
  KEY `idx_role` (`role`),
  KEY `idx_serverid` (`server_id`)
) ENGINE=InnoDB AUTO_INCREMENT=433739 DEFAULT CHARSET=utf8 COMMENT='Venus服务映射表';

-- ----------------------------
-- Records 
-- ----------------------------
INSERT INTO `t_admin` VALUES ('32', 'admin', 'admin', '1', '2017-09-01 16:17:51', '2017-11-10 14:54:16');
