# 编译工具链镜像可移植性测试计划

1. check 两个本地工具链镜像存在；
2. save 为 OCI tar 并生成 SHA-256；
3. load 前校验 checksum，再加载两个镜像；
4. 核对镜像标签与 ID；
5. push 必须限制到 YAML registry 且要求已登录；
6. 归档、checksum 和本机配置必须被 Git 忽略。
