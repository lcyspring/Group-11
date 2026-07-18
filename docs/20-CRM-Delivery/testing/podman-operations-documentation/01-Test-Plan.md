# Podman 操作与字段文档测试计划

1. 检查编译、镜像封装、部署、停止和低频运维的 KDL 字段均有中文说明；
2. 检查 `check`、`replace`、`fast`、`frontends-only` 与三个 `replace-*` 模式边界；
3. 检查三阶段脚本互不隐式调用，数据库不被封装为项目镜像；
4. 检查 H5 产物、本机 KDL、OCI tar、诊断包和备份均按规则忽略；
5. 执行 runtime-config 门禁，确认所有入口遵守单 KDL 参数契约且不改变运行 Pod。
