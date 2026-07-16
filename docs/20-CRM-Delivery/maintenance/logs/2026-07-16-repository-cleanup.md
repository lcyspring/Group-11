# 仓库运行时目录清理记录

- 分支：`develop`
- 日期：2026-07-16

## 清理内容

- 删除空的 `uploads/` 运行时目录；应用启动或大文件上传功能会按配置自动创建 `uploads/` 与 `uploads/chunks/`。
- 将 `dev/`、`docker-compose/`、`docker-images/` 和 `/uploads/` 加入 `.gitignore`，避免旧 Docker 文件、镜像归档和运行时上传数据重新进入仓库。

## 数据库目录核查

核查未发现 `.bak`、`.old`、`.tmp` 或重复导出文件。`database/base` 是 Podman 初始化事实源，`database/new` 由初始化脚本或 YAML 兼容迁移清单加载，均予以保留。

唯一未被任何初始化脚本、Podman manifest 或运行流程引用的 `database/replace-en/dict_data_label_to_english.sql` 已删除；该文件是一次性手工翻译导出，不属于可重复迁移，`database/replace-en` 目录随之移除。

## 验证

- `uploads/chunks` 不存在，`uploads/` 目录不再作为仓库内容维护。
- 所有数据库 SQL 文件均仍可被 `podman/Containerfile` 或兼容迁移流程访问。
- 后续如需删除数据库迁移，必须先从初始化脚本、manifest 和文档引用中移除，并完成全新数据库初始化验证。
