# CRM 跟进统计正确性测试计划

更新日期：2026-07-14

## 自动化用例

1. Mapper 返回跟进次数 7、去重客户数 3 时，按日期响应必须保持 7/3。
2. 同一输入下，按员工响应必须保持 7/3，并正常拼接员工昵称。
3. 五条跟进统计 SQL 均必须包含逻辑删除过滤。
4. 运行全部 `Crm*Test`，防止影响其他 CRM 核心链。
5. 生成 JaCoCo CSV 并记录修复后的模块覆盖率。

## Ubuntu 容器命令

```bash
cd podman
bash ./compile.sh ./config/test-crm-ubuntu-26.04.yaml
```

命令行只指定 YAML 配置路径；JDK、Maven、线程、缓存和测试/覆盖率开关均来自配置文件。
