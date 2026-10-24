# 定向验证约定

## 示例需求

- 后端行为：`mvn -f backend/pom.xml -B -ntp -Dtest=OrderServiceTest test`，指定 7 个行为用例。
- 后端制品：`mvn -f backend/pom.xml -B -ntp -DskipTests package`，仅构建，不把跳过测试记为通过。
- 前端：`npm ci --no-audit --no-fund` 后 `npm run build`；另行定向浏览器验证创建、取消及空状态。
- 真实 MySQL：应用启动后运行 `python scripts/smoke_orders.py`，该脚本创建并取消一笔演示订单，保留记录。

## 结果

命令、实际代码版本、日志、已执行与未执行记录在需求目录的测试主文档。CI JUnit 与制品绑定 commit SHA。

全量回归、全仓库 / 全模块套件不自动运行，需要用户明确要求；后续需求应按变更选择具体用例，不机械沿用本示例类。
