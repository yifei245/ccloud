-- ----------------------------------------------------------------------------
-- ccloud 建库脚本
--
-- 字符集选 utf8mb4：MySQL 的 utf8 实际只有 3 字节，存不了 emoji 与部分生僻字，
-- 用户昵称、商品评价这类字段一旦遇到就会插入失败。
-- 排序规则选 utf8mb4_0900_ai_ci：MySQL 8.0 的默认值，比旧的 general_ci 排序更准确。
-- ----------------------------------------------------------------------------

DROP DATABASE IF EXISTS `ccloud`;
CREATE DATABASE `ccloud` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `ccloud`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
