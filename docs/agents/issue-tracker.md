# Issue 追踪约定

- 实例：`http://localhost:8088`，仅本机访问。
- 项目：初始化后由 `D:\docker\ai-dev-platform\project.json` 记录真实 ID 与地址。
- 读取：`python scripts/gitlab_issue.py read <编号>`，输出正文、评论与标签。
- 更新：使用 GitLab 网页；程序写入必须是用户当前任务已授权的内容。
- 本机脚本通过受限目录读取 API Token，不接受把 Token 写入命令或项目文件。
- 通用平台也支持 GitLab CLI `glab`；本项目脚本为 Windows 本地使用提供已验证入口。
- 状态标签：`flow/backlog`、`flow/clarify`、`flow/ready`、`flow/doing`、`flow/review`、`flow/accept`、`flow/done`。同一 Issue 只保留一个流程状态。
- 准入标签只能在实际确认需求、设计、依赖和定向验证范围后设置；AI 生成规格不直接代表准入通过。
- MR、测试和部署证据回链 Issue；MR 合并后仍需按验收标准决定何时关闭需求。

## 领域资料

先读取当前 Issue 指向的需求与方案；长期术语见 `GLOSSARY.md`。新增架构决策确有复用价值时再维护公共说明。
