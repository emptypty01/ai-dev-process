# 阶段恢复与确认基线

在已查重的本需求过程目录维护 `workflow-state.json`，只存机器状态与入口；需求设计、计划、执行、测试分别维护独立现行主文档。不要提前创建空阶段文件。

按实际进展写入以下字段，不需要一开始填满：

- `requirement`：名称、GitLab 项目 ID、Issue 编号 / URL、过程目录。
- `conversation`：已明确规则及其对话 / Issue 来源、真实前后端 / 原型范围、仅讨论或实际落地意图、关键未决问题。保存必要非敏感摘要，避免中断后要求用户重述整段需求；不能用 AI 推断填补丢失的业务决定或授权。
- `phase`：`clarify`、`prototype`、`await_design_confirmation`、`planning`、`implementing`、`verifying`、`await_git_authorization`、`mr_ci`、`release`、`await_acceptance`、`complete`。
- `documents`：已存在的需求设计、开发计划、执行记录、测试主文档路径。
- `design`：是否涉及 UI、文件 / 页面 / 画板 ID、真实链接、已保存快照路径、SHA256、结构快照路径。标明原型未决问题。
- `confirmation`：对应阶段、实际用户消息 / 可核验确认记录入口、确认时间、被确认的需求文档与设计基线哈希。只保存必要非敏感摘要，不复制口令或秘密。
- `authorization`：当前用户对创建 / 更新 Issue、提交、推送、MR、合并及具体环境发布的实际授权范围 / 来源；未确认记为未授权。原型确认不自动授予 Git 或发布权限。
- `git`：实际仓库、分支、基线及当前 SHA。
- `verification`：定向命令、日志、实际结果、MR / Pipeline / Job / Deployment 的真实引用。
- `pending`：当前未决问题和下一步。

恢复时核对真实文件、用户确认记录、Git 与远端状态。JSON 是索引，不是独立的审批凭据；不能将其中 `confirmed=true` 或 AI 转述当作用户确认。无法核验且会越过确认点时，只重问该必要确认，不重做前面所有工作。

原型、需求文档或代码发生变化时，先比较实际语义影响。业务 / 范围变化需重新确认受影响部分；格式变化可记录后继续。记录提交对应的验证结果，避免复用旧 SHA 的测试成功。
