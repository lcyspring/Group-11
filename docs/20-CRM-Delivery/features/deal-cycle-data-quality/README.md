# CRM-FEATURE-050：成交周期历史补录数据质量治理

## 完成内容

- 日期、员工、地区和产品四类成交周期接口增加 `negativeSampleCount`；
- 负样本按客户去重，避免合同多产品行重复计数；
- 保留真实负周期参与平均，不篡改、不静默归零、不取绝对值；
- 员工、地区和产品表增加数据质量警告列，三语文本同步；
- 成交周期图允许绘制负值，历史异常不再被 Y 轴下限隐藏；
- YAML 血缘目录显式登记指标时间、公式和负值处理口径；
- 真实验收脚本只接受 YAML 配置路径，对四类 API、指标目录和 MySQL 做对账。

## 边界

本功能解决“异常被隐藏或无法定位”的数据治理问题，不自动判定历史合同日期为错误。如需改变成交时间定义，必须另行签署业务口径和数据迁移方案。

## 验证入口

- 构建：`podman/config/verify-crm-statistics-data-quality-ubuntu-26.04.yaml`；
- 运行验收：`podman/tests/acceptance/verify-crm-deal-cycle-data-quality.sh`；
- 无凭据示例：`podman/config/verify-crm-deal-cycle-data-quality.example.yaml`；
- 测试与覆盖率：`docs/20-CRM-Delivery/testing/deal-cycle-data-quality/`。
