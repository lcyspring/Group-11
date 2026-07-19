# Deno 与 Vite 8 工具链迁移覆盖率

`verify:web-media` 使用 Deno 原生 coverage profile，覆盖媒体 URL 归一化、空/非法 origin、null URL、
循环引用和退休 CommonJS 图标生成器防回归：

| 指标 | 结果 | KDL 门槛 |
|---|---:|---:|
| 行 | 100% | 90% |
| 分支 | 100% | 90% |
| 函数 | 100% | 90% |

LCOV 输出为 ignored 的 `Web/coverage/verify-web-media.lcov`。纯 ESLint、`ts:check` 不属于可执行代码
测试，必须使用 `web.coverage_enabled: false`，不以空 profile 冒充覆盖率。
