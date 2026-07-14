# PODMAN-BUILD-BUG-004：Server clean 删除 CRM 覆盖率证据

更新日期：2026-07-14

## 现象

首次容器运行中，CRM 测试和 JaCoCo 报告生成后通过了非空检查；后续 Server `clean package` 成功，但构建退出时 `mitedtsm-module-crm/target/site/jacoco/jacoco.csv` 与 Surefire 报告已经不存在。

## 根因

入口脚本先执行 CRM 测试，再执行包含 CRM 模块的 Server Reactor `clean`。Maven 按设计删除各模块 `target/`，因此先前测试证据无法作为最终构建产物核验。

## 修复

把 CRM 测试和 JaCoCo 调整到 Server、InitService 打包之后执行。Web 构建不操作 Server `target/`，报告将保留到容器退出后。

## 验证

完整容器复跑后 CRM Surefire 58/58，JaCoCo CSV 14 KB，并在 Web 构建完成后继续保留。

## 状态

已关闭。
