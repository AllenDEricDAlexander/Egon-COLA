# DDC Admin Web 设计

## 目标

在 DDC Admin 现有 `18070` 服务中提供可直接打开的管理页面，覆盖服务注册查看和配置中心管理，并继续使用当前 JWT 权限体系。

## 方案选择

采用 Spring Boot 同源静态页面和 JWT Admin API。页面资源放在 DDC Admin 的 `static/ddc-admin` 下；服务目录通过新增的 JWT 只读接口访问现有 `DdcManagementFacade`；配置增删改、发布复用现有 `/api/v1/ddc/configs` 接口。

没有选择独立 React 工程，因为当前需求不需要新的构建链和依赖。没有把页面加入 Gateway Admin Web，因为 DDC 的权限、生命周期和部署边界应保持独立。没有让浏览器调用 HMAC OpenAPI，因为这会向前端暴露服务凭据。

## 页面范围

- 登录：输入 Bearer JWT，只保存在当前浏览器会话。
- 服务注册：按环境、命名空间和服务类型加载 HTTP Provider、RPC Provider、Internal Gateway 服务目录；选择服务后显示实例地址、状态、租约和心跳时间。
- 配置管理：按应用、环境、命名空间和配置键查询；支持创建、修改、发布和刷新列表。
- 应用初始化：支持创建 DDC 应用和命名空间，避免空库无法创建配置。
- 错误处理：统一展示 HTTP 状态和后端错误消息；401 清除当前会话 Token 并返回登录区。

## 安全边界

- `/ddc-admin/**` 静态资源匿名可访问，不包含凭据。
- `/api/v1/ddc/registry/**` 只允许 `DDC_READ` 或 `*` capability。
- 配置写入和发布继续分别受 `DDC_WRITE`、`DDC_PUBLISH` 或 `*` capability 控制。
- 页面不读取 Redis、PostgreSQL 或 HMAC Secret，不增加数据库迁移。

## 验证

- MVC 测试验证 JWT 注册中心投影和参数转发。
- Security MVC 测试验证匿名拒绝、`DDC_READ` 放行和静态入口匿名可访问。
- 资源契约测试验证页面、脚本、样式及两个核心功能入口存在。
- 模块测试、可执行 JAR 打包和本机真实 DDC/Redis/PostgreSQL 页面/API 验证全部通过后交付。
