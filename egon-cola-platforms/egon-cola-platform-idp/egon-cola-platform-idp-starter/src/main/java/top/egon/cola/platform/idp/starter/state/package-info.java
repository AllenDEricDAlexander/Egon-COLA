/**
 * 定义并实现 IdP 用户、Resource Server 与 OAuth Client 运行态读取边界。
 * 验证器通过这些投影确认 USER、SERVICE 和目标 Resource 仍处于当前有效状态。
 *
 * <p>Defines and implements boundaries for reading current IdP user, Resource Server, and OAuth
 * Client runtime state. The verifier uses these projections to confirm that USER, SERVICE, and the
 * target Resource remain current and valid.</p>
 */
package top.egon.cola.platform.idp.starter.state;
