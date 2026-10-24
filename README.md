# 订单管理 AI 研发流程示例

本项目用于本地演练 GitLab Issues → Penpot 设计 → Codex CLI + Skills → MR → 定向 CI → 手工发布。

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
mvn -f backend/pom.xml -B -ntp -Dtest=OrderServiceTest,OrderRequestTest test
npm --prefix frontend run build
.\scripts\start-demo.ps1 -Build
python scripts/smoke_orders.py
```

需要本机 Docker 部署时运行 `scripts/deploy-local.ps1 -Version <明确版本>`；停止开发进程使用 `scripts/stop-demo.ps1`。

## Codex 使用

在仓库目录运行 `codex`，复用本机 ChatGPT 登录。先阅读 `AGENTS.md`。项目技能位于 `.agents/skills/`，来源和适配哈希见 `docs/agents/skills-source.json`。

- 需求澄清：`$grill-me`。
- 已讨论内容整理：`$to-spec`。
- 按业务行为拆分：`$to-tickets`。
- 按已确认 Issue 开发：`$implement`。
- 根据明确基线评审：`$code-review`。

项目 MCP 配置提供 `gitlab_local`（只读 Issue）和 `penpot_design`（设计读取与写入）。Penpot 操作前确认正确文件在浏览器中连接了 MCP；结束设计会话后可从 Penpot 主菜单断开。

Penpot 只读检查使用 `read_current_design`，执行固定的查询；通用 `execute_code` 包含设计写入能力，交互使用时按实际操作批准，不能在无人值守执行中绕过审批。

示例请求：

> 使用 gitlab_local 读取 Issue 1，核对当前实现与 AC-01–AC-05。先报告缺项与定向验证建议，不提交、不推送、不执行全量回归。

## CI/CD 与交付

- MR 与默认分支运行：交接检查、`OrderServiceTest` 与 `OrderRequestTest` 定向测试、Vue 构建、Java 打包。
- 默认受保护分支的 `deploy-local` 必须人工触发，部署 `http://localhost:5174`。
- 普通验证与发布使用不同 Runner；发布凭据仅给受保护分支的 `local-demo` 环境。
- 主分支禁止直接推送，合并由 Maintainer 处理。GitLab CE Free 的批准记录不构成强制批准规则。
- 需求中验收、MR、Pipeline、发布分别记录；不将一次本地通过写成 CI 或业务验收通过。
- 不自动提交 / 推送，不自动全量回归。

接口契约见 `contracts/openapi.yaml`。此演示不包含支付、库存、权限与生产发布。
