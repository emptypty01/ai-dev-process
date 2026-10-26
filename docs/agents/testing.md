# 定向验证约定

## 当前商品与多商品订单需求

- 后端行为：使用 `.gitlab-ci.yml` 的 `-Dtest` 选择器，订单服务指定5个方法，并运行订单请求、商品服务及商品请求的本需求用例；当前选择器包含12个，不运行全部订单服务方法。实际执行批次与修复后的定向重跑见需求测试文档，不把选择器数量当作已执行结果。
- 后端制品：`mvn -f backend/pom.xml -B -ntp -DskipTests package`，仅构建，不把跳过测试记为通过。
- 前端：`npm ci --no-audit --no-fund` 后 `npm run test:money` 与 `npm run build`；另行定向浏览器验证商品维护、多商品创建、展开、取消与错误恢复。
- 真实 MySQL：在独立数据库验证 V1 → V2，旧订单回填后仍可查询 / 取消；运行 `python scripts/smoke_products_orders.py --base-url <本地验证地址> --result-file <本需求log目录>/api-result.json`，保留测试记录。可选 `--legacy-order-id` 验证预置历史样本；不在发布环境直接执行。
- API 0.2.0 金额响应为字符串；新增 / 修改商品及多商品下单验证不能继续使用旧的单商品请求格式。

## 结果

命令、实际代码版本、日志、已执行与未执行记录在需求目录的测试主文档。CI JUnit 与制品绑定 commit SHA。

全量回归、全仓库 / 全模块套件不自动运行，需要用户明确要求；后续需求应按变更选择具体用例，不机械沿用本示例类。
