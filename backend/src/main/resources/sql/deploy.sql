-- CatMunity 数据库初始化脚本
-- 创建数据库
CREATE DATABASE IF NOT EXISTS catmunity DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE catmunity;

-- 用户表
CREATE TABLE IF NOT EXISTS `users` (
                                       `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
                                       `avatar` VARCHAR(500) DEFAULT NULL COMMENT '用户头像URL',
    `identifier` VARCHAR(100) NOT NULL COMMENT '用户账号',
    `password` VARCHAR(255) NOT NULL COMMENT '用户密码',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '用户联系电话',
    `type` TINYINT NOT NULL DEFAULT 2 COMMENT '用户类型：0-超级管理员，1-普通管理员，2-普通用户',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `last_active` DATETIME DEFAULT NULL COMMENT '最近活跃时间',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '用户邮箱',
    `status` TINYINT DEFAULT 0 COMMENT '账号状态：0-正常，1-封禁',
    `ban_reason` VARCHAR(500) DEFAULT NULL COMMENT '封禁原因',
    `banned_at` DATETIME DEFAULT NULL COMMENT '封禁时间',
    UNIQUE KEY `uk_identifier` (`identifier`),
    KEY `idx_phone` (`phone`),
    KEY `idx_email` (`email`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 猫咪档案表
CREATE TABLE IF NOT EXISTS `cat_profile` (
                                             `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '猫咪档案ID',
                                             `name` VARCHAR(50) NOT NULL COMMENT '猫咪名字',
    `breed` VARCHAR(50) DEFAULT NULL COMMENT '猫咪品种',
    `gender` TINYINT DEFAULT NULL COMMENT '性别：0-母猫，1-公猫',
    `birthday` DATE DEFAULT NULL COMMENT '猫咪生日',
    `weight` DECIMAL(5,2) DEFAULT NULL COMMENT '体重kg',
    `photo_urls` TEXT DEFAULT NULL COMMENT '照片URL列表JSON',
    `status` TINYINT DEFAULT 0 COMMENT '状态：0-正常，1-已领养，2-其他',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY `idx_name` (`name`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='猫咪档案表';

-- 猫咪健康事件表
CREATE TABLE IF NOT EXISTS `cat_health_event` (
                                                  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '健康事件ID',
                                                  `cat_profile_id` BIGINT NOT NULL COMMENT '猫咪档案ID',
                                                  `event_type` TINYINT NOT NULL COMMENT '事件类型：1-疫苗，2-驱虫，3-绝育，4-体检，5-疾病，6-美容，99-其他',
                                                  `title` VARCHAR(100) NOT NULL COMMENT '事件标题',
    `description` TEXT DEFAULT NULL COMMENT '事件描述',
    `event_date` DATE NOT NULL COMMENT '事件日期',
    `next_remind_date` DATE DEFAULT NULL COMMENT '下次提醒日期',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY `idx_cat_profile_id` (`cat_profile_id`),
    KEY `idx_event_date` (`event_date`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='猫咪健康事件表';

-- 猫咪打卡表
CREATE TABLE IF NOT EXISTS `cat_checkin` (
                                             `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '打卡ID',
                                             `user_id` BIGINT NOT NULL COMMENT '用户ID',
                                             `cat_profile_id` BIGINT NOT NULL COMMENT '猫咪档案ID',
                                             `checkin_date` DATE NOT NULL COMMENT '打卡日期',
                                             `photo_url` VARCHAR(500) DEFAULT NULL COMMENT '打卡照片URL',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '打卡备注',
    `synced_to_post` TINYINT DEFAULT 0 COMMENT '是否同步到社区：0-否，1-是',
    `post_id` BIGINT DEFAULT NULL COMMENT '关联帖子ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY `idx_user_id` (`user_id`),
    KEY `idx_cat_profile_id` (`cat_profile_id`),
    KEY `idx_checkin_date` (`checkin_date`),
    UNIQUE KEY `uk_user_cat_date` (`user_id`, `cat_profile_id`, `checkin_date`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='猫咪打卡表';

-- 帖子表
CREATE TABLE IF NOT EXISTS `post` (
                                      `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '帖子ID',
                                      `user_id` BIGINT NOT NULL COMMENT '用户ID',
                                      `cat_profile_id` BIGINT DEFAULT NULL COMMENT '关联猫咪档案ID',
                                      `content` TEXT NOT NULL COMMENT '帖子内容',
                                      `images` TEXT DEFAULT NULL COMMENT '图片URL列表JSON',
                                      `tags` TEXT DEFAULT NULL COMMENT '标签列表JSON',
                                      `like_count` INT DEFAULT 0 COMMENT '点赞数',
                                      `comment_count` INT DEFAULT 0 COMMENT '评论数',
                                      `favorite_count` INT DEFAULT 0 COMMENT '收藏数',
                                      `repost_count` INT DEFAULT 0 COMMENT '转发数',
                                      `status` TINYINT DEFAULT 0 COMMENT '状态：0-正常，1-违规待处理，2-已删除',
                                      `violation_reason` VARCHAR(500) DEFAULT NULL COMMENT '违规原因',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY `idx_user_id` (`user_id`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_status` (`status`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子表';

-- 评论表
CREATE TABLE IF NOT EXISTS `comment` (
                                         `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '评论ID',
                                         `post_id` BIGINT NOT NULL COMMENT '帖子ID',
                                         `user_id` BIGINT NOT NULL COMMENT '用户ID',
                                         `parent_id` BIGINT DEFAULT NULL COMMENT '父评论ID',
                                         `content` TEXT NOT NULL COMMENT '评论内容',
                                         `like_count` INT DEFAULT 0 COMMENT '点赞数',
                                         `status` TINYINT DEFAULT 0 COMMENT '状态：0-正常，1-违规，2-已删除',
                                         `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                         `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                         KEY `idx_post_id` (`post_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_parent_id` (`parent_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

-- 帖子点赞表
CREATE TABLE IF NOT EXISTS `post_like` (
                                           `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '点赞ID',
                                           `post_id` BIGINT NOT NULL COMMENT '帖子ID',
                                           `user_id` BIGINT NOT NULL COMMENT '用户ID',
                                           `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                           UNIQUE KEY `uk_post_user` (`post_id`, `user_id`),
    KEY `idx_user_id` (`user_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子点赞表';

-- 帖子收藏表
CREATE TABLE IF NOT EXISTS `post_favorite` (
                                               `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '收藏ID',
                                               `post_id` BIGINT NOT NULL COMMENT '帖子ID',
                                               `user_id` BIGINT NOT NULL COMMENT '用户ID',
                                               `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                               UNIQUE KEY `uk_post_user` (`post_id`, `user_id`),
    KEY `idx_user_id` (`user_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子收藏表';

-- 关注关系表
CREATE TABLE IF NOT EXISTS `follow` (
                                        `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                                        `follower_id` BIGINT NOT NULL COMMENT '关注者ID',
                                        `following_id` BIGINT NOT NULL COMMENT '被关注者ID',
                                        `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                        UNIQUE KEY `uk_follower_following` (`follower_id`, `following_id`),
    KEY `idx_following_id` (`following_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='关注关系表';

-- 内容审核记录表
CREATE TABLE IF NOT EXISTS `moderation_record` (
                                                   `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '审核记录ID',
                                                   `content_type` TINYINT NOT NULL COMMENT '内容类型：1-帖子，2-评论',
                                                   `content_id` BIGINT NOT NULL COMMENT '内容ID',
                                                   `content_summary` VARCHAR(500) DEFAULT NULL COMMENT '内容摘要',
    `original_content` TEXT DEFAULT NULL COMMENT '原始内容',
    `author_id` BIGINT NOT NULL COMMENT '发布者ID',
    `ai_risk_score` DOUBLE DEFAULT NULL COMMENT 'AI风险评分0-1',
    `risk_level` TINYINT DEFAULT 0 COMMENT '风险等级：0-安全，1-低，2-中，3-高，4-极高',
    `violation_types` VARCHAR(255) DEFAULT NULL COMMENT '违规类型逗号分隔',
    `ai_analysis` TEXT DEFAULT NULL COMMENT 'AI分析结果',
    `status` TINYINT DEFAULT 0 COMMENT '审核状态：0-待审核，1-已通过，2-已拒绝，3-AI标记待复核',
    `reviewer_id` BIGINT DEFAULT NULL COMMENT '审核人ID',
    `reviewer_name` VARCHAR(100) DEFAULT NULL COMMENT '审核人名称',
    `review_comment` VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
    `reviewed_at` DATETIME DEFAULT NULL COMMENT '审核时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY `idx_content` (`content_type`, `content_id`),
    KEY `idx_author_id` (`author_id`),
    KEY `idx_status` (`status`),
    KEY `idx_risk_level` (`risk_level`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容审核记录表';

-- 用户举报表
CREATE TABLE IF NOT EXISTS `report` (
                                        `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '举报ID',
                                        `content_type` TINYINT NOT NULL COMMENT '内容类型：1-帖子，2-评论',
                                        `content_id` BIGINT NOT NULL COMMENT '被举报内容ID',
                                        `reported_user_id` BIGINT NOT NULL COMMENT '被举报人ID',
                                        `reporter_id` BIGINT NOT NULL COMMENT '举报人ID',
                                        `reason_type` TINYINT NOT NULL COMMENT '举报原因类型',
                                        `reason_description` VARCHAR(500) DEFAULT NULL COMMENT '举报原因描述',
    `status` TINYINT DEFAULT 0 COMMENT '状态：0-待处理，1-已处理，2-已忽略',
    `handler_id` BIGINT DEFAULT NULL COMMENT '处理人ID',
    `handler_result` VARCHAR(500) DEFAULT NULL COMMENT '处理结果',
    `handled_at` DATETIME DEFAULT NULL COMMENT '处理时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY `idx_content` (`content_type`, `content_id`),
    KEY `idx_reporter_id` (`reporter_id`),
    KEY `idx_status` (`status`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户举报表';

-- 帖子举报表
CREATE TABLE IF NOT EXISTS `post_report` (
                                             `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '举报ID',
                                             `post_id` BIGINT NOT NULL COMMENT '帖子ID',
                                             `reporter_id` BIGINT NOT NULL COMMENT '举报用户ID',
                                             `reported_user_id` BIGINT NOT NULL COMMENT '被举报用户ID',
                                             `report_type` TINYINT NOT NULL COMMENT '举报类型：1-广告，2-色情，3-暴力，4-违法，5-虚假，6-攻击，99-其他',
                                             `description` VARCHAR(500) DEFAULT NULL COMMENT '举报描述',
    `status` TINYINT DEFAULT 0 COMMENT '状态：0-待处理，1-已处理，2-已驳回',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `handled_at` DATETIME DEFAULT NULL COMMENT '处理时间',
    KEY `idx_post_id` (`post_id`),
    KEY `idx_reporter_id` (`reporter_id`),
    KEY `idx_status` (`status`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子举报表';

-- 知识库数据集表
CREATE TABLE IF NOT EXISTS `dataset` (
                                         `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                                         `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
                                         `name` VARCHAR(100) NOT NULL COMMENT '数据集名称',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '数据集描述',
    `app_count` INT DEFAULT 0 COMMENT '应用次数',
    `doc_count` INT DEFAULT 0 COMMENT '文档数',
    `disabled` TINYINT DEFAULT 1 COMMENT '是否禁用：1-启用，-1-禁用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `disabled_at` DATETIME DEFAULT NULL COMMENT '禁用时间',
    KEY `idx_user_id` (`user_id`),
    KEY `idx_disabled` (`disabled`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库数据集表';

-- 知识库文件表
CREATE TABLE IF NOT EXISTS `dataset_files` (
                                               `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                                               `name` VARCHAR(255) NOT NULL COMMENT '文件名',
    `file_path` VARCHAR(500) NOT NULL COMMENT '文件路径',
    `file_type` VARCHAR(50) DEFAULT NULL COMMENT '文件类型',
    `file_size` BIGINT DEFAULT NULL COMMENT '文件大小字节',
    `dataset_id` BIGINT NOT NULL COMMENT '所属数据集ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '上传用户ID',
    `disabled` TINYINT DEFAULT 1 COMMENT '是否禁用：1-启用，-1-禁用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `disabled_at` DATETIME DEFAULT NULL COMMENT '禁用时间',
    `hit_count` INT DEFAULT 0 COMMENT '命中次数',
    KEY `idx_dataset_id` (`dataset_id`),
    KEY `idx_user_id` (`user_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库文件表';

-- AI聊天记录表
CREATE TABLE IF NOT EXISTS `chat_logs` (
                                           `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                                           `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
                                           `session_id` VARCHAR(100) DEFAULT NULL COMMENT '会话ID',
    `message_id` VARCHAR(100) NOT NULL COMMENT '消息ID',
    `text` TEXT DEFAULT NULL COMMENT '消息文本',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1-成功，0-中断',
    `message_type` VARCHAR(20) DEFAULT NULL COMMENT '消息类型：system/user/assistant',
    `metadata` JSON DEFAULT NULL COMMENT '元数据',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY `idx_user_id` (`user_id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_message_id` (`message_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI聊天记录表';

-- AI反馈评价表
CREATE TABLE IF NOT EXISTS `evaluate` (
                                          `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                                          `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
                                          `session_id` VARCHAR(100) DEFAULT NULL COMMENT '会话ID',
    `message_id` VARCHAR(100) DEFAULT NULL COMMENT '消息ID',
    `rating` INT DEFAULT NULL COMMENT '评价：1-点赞，-1-点踩',
    `comment` TEXT DEFAULT NULL COMMENT '反馈内容',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY `idx_user_id` (`user_id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_message_id` (`message_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI反馈评价表';


INSERT INTO `users` (identifier, password, phone, type, created_at, updated_at, last_active, email) VALUES
                                                                                                        ('xiaoTnb','b6aab45f109e1ea8851af151e5f01503','13113340545',0,'2025-10-03 15:00:12','2025-10-03 15:00:12',NULL,NULL),
                                                                                                        ('AAA','e0c79caa654708df3fecdf49391f0a33',NULL,0,'2025-10-14 14:07:37','2025-10-14 14:07:37',NULL,NULL),
                                                                                                        ('admin1','b6aab45f109e1ea8851af151e5f01503',NULL,1,'2025-10-22 13:25:42','2025-10-22 13:25:42',NULL,NULL),
                                                                                                        ('user1','b6aab45f109e1ea8851af151e5f01503',NULL,2,'2025-10-22 13:26:46','2025-10-22 13:26:46',NULL,NULL);
