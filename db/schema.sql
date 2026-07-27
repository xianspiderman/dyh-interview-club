-- DYH Interview Club database schema
CREATE DATABASE IF NOT EXISTS `dyh_club` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `dyh_club`;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `auth_permission` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) DEFAULT NULL COMMENT '权限名称',
  `parent_id` bigint(20) DEFAULT NULL COMMENT '父id',
  `type` tinyint(4) DEFAULT NULL COMMENT '权限类型 0菜单 1操作',
  `menu_url` varchar(255) DEFAULT NULL COMMENT '菜单路由',
  `status` tinyint(2) DEFAULT NULL COMMENT '状态 0启用 1禁用',
  `show` tinyint(2) DEFAULT NULL COMMENT '展示状态 0展示 1隐藏',
  `icon` varchar(128) DEFAULT NULL COMMENT '图标',
  `permission_key` varchar(64) DEFAULT NULL COMMENT '权限唯一标识',
  `created_by` varchar(32) DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int(11) DEFAULT '0' COMMENT '是否被删除 0为删除 1已删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `auth_role` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `role_name` varchar(32) DEFAULT NULL COMMENT '角色名称',
  `role_key` varchar(64) DEFAULT NULL COMMENT '角色唯一标识',
  `created_by` varchar(32) DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int(11) DEFAULT '0' COMMENT '是否被删除 0未删除 1已删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `auth_role_permission` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `role_id` bigint(20) DEFAULT NULL COMMENT '角色id',
  `permission_id` bigint(20) DEFAULT NULL COMMENT '权限id',
  `created_by` varchar(32) DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int(11) DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8 COMMENT='角色权限关联表';

CREATE TABLE IF NOT EXISTS `auth_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_name` varchar(32) DEFAULT NULL COMMENT '用户名称/账号',
  `nick_name` varchar(32) DEFAULT NULL COMMENT '昵称',
  `email` varchar(32) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(32) DEFAULT NULL COMMENT '手机号',
  `password` varchar(64) DEFAULT NULL COMMENT '密码',
  `sex` tinyint(2) DEFAULT NULL COMMENT '性别',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像',
  `status` tinyint(2) DEFAULT NULL COMMENT '状态 0启用 1禁用',
  `introduce` varchar(255) DEFAULT NULL COMMENT '个人介绍',
  `ext_json` varchar(255) DEFAULT NULL COMMENT '特殊字段',
  `created_by` varchar(32) DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int(11) DEFAULT '0' COMMENT '是否被删除 0未删除 1已删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8 COMMENT='用户信息表';

CREATE TABLE IF NOT EXISTS `auth_user_role` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户id',
  `role_id` bigint(20) DEFAULT NULL COMMENT '角色id',
  `created_by` varchar(32) DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int(11) DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8 COMMENT='用户角色表';

CREATE TABLE IF NOT EXISTS `interview_history` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `avg_score` double DEFAULT NULL,
  `key_words` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `tip` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `interview_url` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `created_by` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `created_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(255) DEFAULT NULL,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` int(11) DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=latin1;

CREATE TABLE IF NOT EXISTS `interview_question_history` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `interview_id` int(11) NOT NULL,
  `score` double DEFAULT NULL,
  `key_words` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `question` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `answer` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `user_answer` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `created_by` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `created_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(255) DEFAULT NULL,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` int(11) DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=latin1;

CREATE TABLE IF NOT EXISTS `practice_detail` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `practice_id` bigint(20) DEFAULT NULL COMMENT '练题id',
  `subject_id` bigint(20) DEFAULT NULL COMMENT '题目id',
  `subject_type` int(11) DEFAULT NULL COMMENT '题目类型',
  `answer_status` int(11) DEFAULT NULL COMMENT '回答状态',
  `answer_content` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '回答内容',
  `created_by` varchar(32) CHARACTER SET utf8 DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8 DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int(11) DEFAULT '0' COMMENT '是否被删除 0为删除 1已删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=159 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='练习详情表';

CREATE TABLE IF NOT EXISTS `practice_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `set_id` bigint(20) DEFAULT NULL COMMENT '套题id',
  `complete_status` int(11) DEFAULT NULL COMMENT '是否完成 1完成 0未完成',
  `time_use` varchar(32) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '用时',
  `submit_time` datetime DEFAULT NULL COMMENT '交卷时间',
  `correct_rate` decimal(10,2) DEFAULT NULL COMMENT '正确率',
  `created_by` varchar(32) CHARACTER SET utf8 DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8 DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int(11) DEFAULT '0' COMMENT '是否被删除 0为删除 1已删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=44 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='练习表';

CREATE TABLE IF NOT EXISTS `practice_set` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `set_name` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '套题名称',
  `set_type` int(11) DEFAULT NULL COMMENT '套题类型 1实时生成 2预设套题',
  `set_heat` int(11) DEFAULT NULL COMMENT '热度',
  `set_desc` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '套题描述',
  `primary_category_id` bigint(20) DEFAULT NULL COMMENT '大类id',
  `created_by` varchar(32) CHARACTER SET utf8 DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8 DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int(11) DEFAULT '0' COMMENT '是否被删除 0为删除 1已删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='套题信息表';

CREATE TABLE IF NOT EXISTS `practice_set_detail` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `set_id` bigint(20) NOT NULL COMMENT '套题id',
  `subject_id` bigint(20) DEFAULT NULL COMMENT '题目id',
  `subject_type` int(11) DEFAULT NULL COMMENT '题目类型',
  `created_by` varchar(32) CHARACTER SET utf8 DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8 DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int(11) DEFAULT '0' COMMENT '是否被删除 0为删除 1已删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=159 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='套题内容表';

CREATE TABLE IF NOT EXISTS `sensitive_words` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `words` varchar(1024) DEFAULT NULL COMMENT '内容',
  `type` int(11) DEFAULT '0' COMMENT '1=黑名单 2=白名单',
  `is_deleted` int(11) DEFAULT NULL COMMENT '是否被删除 0为删除 1已删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感词表';

CREATE TABLE IF NOT EXISTS `share_circle` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '圈子ID',
  `parent_id` bigint(20) NOT NULL COMMENT '父级ID,-1为大类',
  `circle_name` varchar(16) NOT NULL COMMENT '圈子名称',
  `icon` varchar(255) DEFAULT NULL COMMENT '圈子图片',
  `created_by` varchar(32) DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int(11) DEFAULT '0' COMMENT '是否被删除 0为删除 1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COMMENT='圈子信息';

CREATE TABLE IF NOT EXISTS `share_comment_reply` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `moment_id` int(11) NOT NULL COMMENT '原始动态ID',
  `reply_type` int(11) NOT NULL COMMENT '回复类型 1评论 2回复',
  `to_id` bigint(20) DEFAULT NULL COMMENT '评论目标id',
  `to_user` varchar(32) DEFAULT NULL COMMENT '评论人',
  `to_user_author` int(11) DEFAULT NULL COMMENT '评论人是否作者 1=是 0=否',
  `reply_id` bigint(20) DEFAULT NULL COMMENT '回复目标id',
  `reply_user` varchar(32) DEFAULT NULL COMMENT '回复人',
  `replay_author` int(11) DEFAULT NULL COMMENT '回复人是否作者 1=是 0=否',
  `content` varchar(1024) DEFAULT NULL COMMENT '内容',
  `pic_urls` varchar(1024) DEFAULT NULL COMMENT '图片内容',
  `parent_id` int(11) NOT NULL,
  `leaf_node` varchar(255) DEFAULT NULL,
  `children` varchar(255) DEFAULT NULL,
  `root_node` varchar(255) DEFAULT NULL,
  `created_by` varchar(32) DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int(11) DEFAULT '0' COMMENT '是否被删除 0为删除 1已删除',
  PRIMARY KEY (`id`,`parent_id`) USING BTREE,
  KEY `idx_moment_id` (`moment_id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COMMENT='评论及回复信息';

CREATE TABLE IF NOT EXISTS `share_message` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `from_id` varchar(32) NOT NULL COMMENT '来自人',
  `to_id` varchar(32) NOT NULL COMMENT '送达人',
  `content` varchar(256) DEFAULT NULL COMMENT '消息内容',
  `is_read` int(11) DEFAULT '0' COMMENT '是否被阅读 1是 2否',
  `created_by` varchar(32) DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int(11) DEFAULT '0' COMMENT '是否被删除 0为删除 1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_to_id` (`to_id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

CREATE TABLE IF NOT EXISTS `share_moment` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '动态ID',
  `circle_id` bigint(20) NOT NULL COMMENT '圈子ID',
  `content` varchar(1024) DEFAULT NULL COMMENT '动态内容',
  `pic_urls` varchar(1024) DEFAULT NULL COMMENT '动态图片内容',
  `reply_count` int(11) NOT NULL DEFAULT '0' COMMENT '回复数',
  `created_by` varchar(32) DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int(11) DEFAULT '0' COMMENT '是否被删除 0为删除 1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_circle_id` (`circle_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COMMENT='动态信息';

CREATE TABLE IF NOT EXISTS `subject_brief` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `subject_id` int(20) DEFAULT NULL COMMENT '题目id',
  `subject_answer` text COMMENT '题目答案',
  `created_by` varchar(32) DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int(11) DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=281 DEFAULT CHARSET=utf8 COMMENT='简答题';

CREATE TABLE IF NOT EXISTS `subject_category` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `category_name` varchar(16) DEFAULT NULL COMMENT '分类名称',
  `category_type` tinyint(2) DEFAULT NULL COMMENT '分类类型',
  `image_url` varchar(64) DEFAULT NULL COMMENT '图标连接',
  `parent_id` bigint(20) DEFAULT NULL COMMENT '父级id',
  `created_by` varchar(32) DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除 0: 未删除 1: 已删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8 COMMENT='题目分类';

CREATE TABLE IF NOT EXISTS `subject_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `subject_name` varchar(128) DEFAULT NULL COMMENT '题目名称',
  `subject_difficult` tinyint(4) DEFAULT NULL COMMENT '题目难度',
  `settle_name` varchar(32) DEFAULT NULL COMMENT '出题人名',
  `subject_type` tinyint(4) DEFAULT NULL COMMENT '题目类型 1单选 2多选 3判断 4简答',
  `subject_score` tinyint(4) DEFAULT NULL COMMENT '题目分数',
  `subject_parse` varchar(512) DEFAULT NULL COMMENT '题目解析',
  `created_by` varchar(32) DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `is_deleted` int(11) DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=338 DEFAULT CHARSET=utf8 COMMENT='题目信息表';

CREATE TABLE IF NOT EXISTS `subject_judge` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `subject_id` bigint(20) DEFAULT NULL COMMENT '题目id',
  `is_correct` tinyint(2) DEFAULT NULL COMMENT '是否正确',
  `created_by` varchar(32) DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int(11) DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8 COMMENT='判断题';

CREATE TABLE IF NOT EXISTS `subject_label` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `label_name` varchar(32) DEFAULT NULL COMMENT '标签分类',
  `sort_num` int(11) DEFAULT NULL COMMENT '排序',
  `category_id` varchar(50) DEFAULT NULL,
  `created_by` varchar(32) DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int(11) DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=64 DEFAULT CHARSET=utf8 COMMENT='题目标签表';

CREATE TABLE IF NOT EXISTS `subject_liked` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `subject_id` bigint(20) DEFAULT NULL COMMENT '题目id',
  `like_user_id` varchar(32) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '点赞人id',
  `status` int(11) DEFAULT NULL COMMENT '点赞状态 1点赞 0不点赞',
  `created_by` varchar(32) CHARACTER SET utf8 DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8 DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `is_deleted` int(11) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_like` (`subject_id`,`like_user_id`) USING BTREE COMMENT '点赞唯一索引'
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='题目点赞表';

CREATE TABLE IF NOT EXISTS `subject_mapping` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `subject_id` bigint(20) DEFAULT NULL COMMENT '题目id',
  `category_id` bigint(20) DEFAULT NULL COMMENT '分类id',
  `label_id` bigint(20) DEFAULT NULL COMMENT '标签id',
  `created_by` varchar(32) DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `is_deleted` int(11) DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=547 DEFAULT CHARSET=utf8 COMMENT='题目分类关系表';

CREATE TABLE IF NOT EXISTS `subject_multiple` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `subject_id` bigint(20) DEFAULT NULL COMMENT '题目id',
  `option_type` bigint(4) DEFAULT NULL COMMENT '选项类型',
  `option_content` varchar(64) DEFAULT NULL COMMENT '选项内容',
  `is_correct` tinyint(2) DEFAULT NULL COMMENT '是否正确',
  `created_by` varchar(32) DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int(11) DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8 COMMENT='多选题信息表';

CREATE TABLE IF NOT EXISTS `subject_radio` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `subject_id` bigint(20) DEFAULT NULL COMMENT '题目id',
  `option_type` tinyint(4) DEFAULT NULL COMMENT 'a,b,c,d',
  `option_content` varchar(128) DEFAULT NULL COMMENT '选项内容',
  `is_correct` tinyint(2) DEFAULT NULL COMMENT '是否正确',
  `created_by` varchar(32) DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `is_deleted` int(11) DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8 COMMENT='单选题信息表';

SET FOREIGN_KEY_CHECKS = 1;
