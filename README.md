# 订单管理 AI 研发流程示例

本项目用于本地演练 GitLab Issues → Penpot 设计 → Codex CLI + Skills → MR → 定向 CI → 手工发布。

当前工作区增加商品配置与多商品订单（API 0.2.0）：商品类型 / 名称 / 价格维护及启停、多商品下单、订单展开快照明细。创建请求为 `items[{productId,quantity}]`，金额响应为十进制字符串。Flyway V2 回填旧订单后移除订单主表的单商品字段，前后端须同版本发布；迁移后回滚需恢复数据库备份，不能只切换旧应用镜像。

## 本地入口

| 服务 | 地址 |
| --- | --- |
| GitLab 私有项目 | http://localhost:8088/developer/order-workflow-demo |
| Penpot | http://localhost:9001 |
| 开发页面 | http://localhost:5173 |
| 开发 API | http://localhost:8081/api/health |
| Docker 发布页面 | http://localhost:5174 |
| Docker 发布 API | http://localhost:8082/api/health |

平台配置在 `D:\docker\ai-dev-platform`，账户由 `secrets/accounts.json` 保管。口令和 Token 不进入本仓库。

## 启动与验证

前置条件：Docker 平台已启动；Java 21、Maven、Node 可用；平台受限配置存在。

```powershell
cd D:\workspace_empty\ai-dev-process
npm --prefix frontend ci --no-audit --no-fund
mvn -f backend/pom.xml -B -ntp '-Dtest=OrderServiceTest#createsConfiguredSnapshotsWithExactTotal+mergesDuplicateProductsBeforePricing+rejectsUnavailableProductsWithoutCreatingPartialOrder+rejectsEmptyAndInvalidOrCombinedQuantities+repeatedCancellationReturnsExistingResult,OrderRequestTest,ProductServiceTest,ProductRequestTest' test
npm --prefix frontend run test:money
npm --prefix frontend run build
.\scripts\start-demo.ps1 -Build
python scripts/smoke_orders.py
```

上述启动会对目标数据库执行 Flyway，先确认迁移范围与备份。需要本机 Docker 部署且已获授权时运行 `scripts/deploy-local.ps1 -Version <明确版本>`；停止开发进程使用 `scripts/stop-demo.ps1`。

已有开发进程不会因 `-Build` 自动重启；升级开发服务时，先确认数据库备份与迁移范围，再停止本仓库的旧开发进程并重新启动，确保前后端使用同版接口。

商品与多商品订单的真实 API 定向验证使用 `python scripts/smoke_products_orders.py --base-url http://127.0.0.1:8083 --result-file <本需求log目录>/api-result.json`；会在指定本地验证环境新增并保留测试记录。前端开发代理可用 `API_PROXY_TARGET` 指向独立验证 API。

## Codex 使用

在仓库目录运行 `codex`，复用本机 ChatGPT 登录。先阅读 `AGENTS.md`。项目技能位于 `.agents/skills/`；8 个上游技能的来源和适配哈希见 `docs/agents/skills-source.json`，`team-ai-workflow` 是本项目新增的编排技能。

- 需求澄清：`$grill-me`。
- 已讨论内容整理：`$to-spec`。
- 按业务行为拆分：`$to-tickets`。
- 按已确认 Issue 开发：`$implement`。
- 根据明确基线评审：`$code-review`。
- 整个新需求或断点续做：`$team-ai-workflow`，按需自动生成 Penpot 原型，等待用户确认后读取原型与需求设计、制定计划并继续开发 / 定向测试；Git 和发布按该需求授权执行。

项目 MCP 配置提供 `gitlab_local`（只读 Issue）和 `penpot_design`（设计读取与写入）。Penpot 操作前确认正确文件在浏览器中连接了 MCP；结束设计会话后可从 Penpot 主菜单断开。

Penpot 只读检查使用 `read_current_design`，执行固定的查询；通用 `execute_code` 包含设计写入能力，交互使用时按实际操作批准，不能在无人值守执行中绕过审批。

确认原型后，使用 `read_design_spec` 读取当前页面的节点、文案、几何与基础样式，并结合 `export_shape` 检查实际图像。详细结构工具有截断标记，不能把画板名称或不完整读取当成完整设计。

可以直接用自然语言讨论要实现的需求；Codex 按项目约定自动使用统一技能，提取已聊规则、补问关键缺项。聊完后只需：

```text
把刚才聊的需求按流程推进。
```

Codex 自行整理 Issue、需求设计及适用的 Penpot 原型；交付后回复“确认，继续”，即可继续计划、开发与定向验证。只讨论时说“先只讨论，暂不落地”；仅要求原型不默认包含前端实现，Git / 发布仍依据本次实际授权。

如需显式选择技能，输入 `$team-ai-workflow 把刚才聊的需求按流程推进`。会话中断后，在同一仓库输入 `$team-ai-workflow 继续 Issue #N`。技能按本需求过程目录中的 `workflow-state.json` 及真实远端状态恢复；该索引不能替代用户确认或授权。

新需求启动开发服务时使用 `.\scripts\start-demo.ps1 -Build -LogDirectory 'D:/plan&exec&test/<本需求目录>/log'`，将日志归入本需求目录。

示例请求：

> 使用 gitlab_local 读取 Issue 1，核对当前实现与 AC-01–AC-05。先报告缺项与定向验证建议，不提交、不推送、不执行全量回归。

## CI/CD 与交付

- MR 与默认分支运行：交接检查、订单与商品的指定方法 / 请求定向测试、金额计算用例、Vue 构建、Java 打包；完整选择器见 `.gitlab-ci.yml`。
- 默认受保护分支的 `deploy-local` 必须人工触发，部署 `http://localhost:5174`。
- 普通验证与发布使用不同 Runner；发布凭据仅给受保护分支的 `local-demo` 环境。
- 主分支禁止直接推送，合并由 Maintainer 处理。GitLab CE Free 的批准记录不构成强制批准规则。
- 需求中验收、MR、Pipeline、发布分别记录；不将一次本地通过写成 CI 或业务验收通过。
- 不自动提交 / 推送，不自动全量回归。

接口契约见 `contracts/openapi.yaml`。此演示不包含支付、库存、权限与生产发布。
