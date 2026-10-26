# 当前本地工具与边界

## 工作目录与服务

- 仓库：`D:\workspace_empty\ai-dev-process`，GitLab 项目入口由平台 `project.json` 记录。
- 平台：`D:\docker\ai-dev-platform`；GitLab `http://localhost:8088`，Penpot `http://localhost:9001`。
- 项目 MCP 在 `.codex/config.toml`：`gitlab_local` 与 `penpot_design`。优先使用交互 `codex`，保留设计写入工具的正常审批。

## GitLab

- `gitlab_local.read_issue` 仅能读取当前项目 Issue。创建 / 更新 Issue、MR、合并和发布不会由该只读工具完成。
- 优先使用已可用的 `glab` 或已核实 API；本机有通用入口 `python D:/docker/ai-dev-platform/scripts/workflow-api.py <get|post|put> <API路径> --body-file <公开JSON正文文件> --output <本需求日志文件>`。
- 编号与路径来自当前任务和真实查询；JSON 正文文件不含凭据，放在本需求 `log/`。该辅助程序内部读取受限 Token，不在命令中传 Token。
- 当前 Token 可能过期。遇到 401 / 403 时说明具体缺失能力，交由用户在本地处理；不切换管理员身份、创建更高权限 Token 或读取 / 输出秘密文件来规避。
- 不能复用平台 `merge-workflow.py`、`play-deployment.py`、`finalize-issue.py` 等首次演练脚本，它们绑定旧的 Issue / MR / SHA。应先读取配置核对范围，再以通用 API 操作本次对象。
- 本机 Git ASKPASS 辅助可由 Git 调用内部读取受限凭据；只有本次提交 / 推送已获授权时才配置给该 Git 命令，不直接调用 ASKPASS 输出口令。

## Penpot

先检查 `read_current_design` 返回的真实文件 / 页面。`read_design_spec` 会读取当前页面的节点、文案、几何和基础样式，返回读取数量与截断标记；`export_shape` 用于实际 PNG / SVG。若截断，应改用经 API 文档核实的只读分批读取，不把不完整结果当作完整原型。

如果没有连接，优先复用已打开的正确设计文件。环境有浏览器自动化能力时可以按对应浏览器技能打开本地页面并连接，复用已有登录；需要用户登录或选择目标文件时才请求该必要操作。不要读取包含 MCP Token 的集成设置页，不导出凭据，不修改账户或 Token。

当前连接一次只对应一个活动 Penpot 页面。为新需求创建独立页面 / 画板，命名关联真实 Issue；原型确认及计划生成前重新核对文件 / 页面 ID。浏览器标签页关闭或休眠后可能需要重新连接。

若新只读工具未出现在旧会话中，重新打开 Codex 会话；也可使用现有 `execute_code` 按正常审批只读分批检查。不能通过关闭审批来运行通用代码。

## 验证与发布

- CI 当前仅对 MR 与默认分支触发，`backend-targeted` 的类名清单需随本次测试更新。
- 新需求开发启动命令为 `.\scripts\start-demo.ps1 -Build -LogDirectory 'D:/plan&exec&test/<本需求目录>/log'`；不传该参数会沿用首次平台部署日志路径。其他构建 / 测试输出同样重定向到本需求 `log/`。
- `deploy-local` 只适用于受保护 main，人工触发，可在用户已明确授权该需求发布后通过 API执行。状态为 manual / blocked 时，先核对其他必需作业成功。
- 取本次实际源码 / 合并 SHA 和 Job ID；验证实际镜像、制品、部署记录及发布后业务行为。
- 回滚使用保留的已验证旧 SHA 镜像。不能把当前工作区重新打包并标成旧 SHA 来伪造回滚；数据库迁移单独处理。
