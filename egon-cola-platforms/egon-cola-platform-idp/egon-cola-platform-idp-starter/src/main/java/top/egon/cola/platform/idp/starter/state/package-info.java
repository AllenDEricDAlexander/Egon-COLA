/**
 * 定义并实现 IdP 用户实时状态读取边界。
 * 验证器通过该状态确认用户仍处于启用状态且令牌版本未过期，从而在 JWT 自身到期前支持即时失效。
 *
 * <p>Defines and implements the boundary for reading current IdP user state. The verifier uses this
 * state to confirm that the user remains active and that the token version is current, enabling
 * immediate invalidation before the JWT itself expires.</p>
 */
package top.egon.cola.platform.idp.starter.state;
