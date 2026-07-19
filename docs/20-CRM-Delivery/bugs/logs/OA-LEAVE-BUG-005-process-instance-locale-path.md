# OA-LEAVE-BUG-005：请假列表显示流程实例翻译键

状态：Fixed

日期：2026-07-19

分支：`develop`

## 现象

请假管理的申请时间筛选框显示 `bpm.instance.startDate` 和 `bpm.instance.endDate`，撤销请假时的原因、
标题、必填提示和成功提示也使用了同一组无效路径。

## 根因

BPM 语言包把流程实例文案定义在 `process.instance` 下，请假列表却省略了 `process` 层级。命名空间
封装因此解析为不存在的 `bpm.instance.*`，最终把完整翻译键直接显示给用户。

## 修复关键

- 日期范围占位符统一改为 `process.instance.startDate/endDate`；
- 撤销原因、弹窗标题、必填提示和成功提示统一使用 `process.instance.*`；
- 回归测试禁止请假列表重新出现 `t('instance.*')`，并逐项校验六个有效路径。

## 回归

- OA 请假源码契约测试通过；
- ESLint 使用 `--max-warnings=0`；
- Ubuntu 26.04 Vite 8 production build 和三语言包完整性校验通过。
