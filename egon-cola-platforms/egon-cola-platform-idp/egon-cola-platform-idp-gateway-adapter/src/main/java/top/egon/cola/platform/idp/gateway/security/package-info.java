/**
 * 实现 Gateway 场景下的 IdP 凭据处理、身份认证与可信身份传播。
 * 入站伪造身份头先被清理，Bearer 令牌通过共享规则验证后形成 Gateway 主体，
 * 最终仅把固定身份标识映射给 HTTP 后端；本包不负责业务授权。
 *
 * <p>Implements IdP credential handling, identity authentication, and trusted-identity propagation
 * for Gateway traffic. Spoofable inbound identity headers are removed first, the Bearer token is
 * validated by shared rules and converted into a Gateway principal, and only fixed identity
 * identifiers are then mapped to HTTP backends. This package does not perform business
 * authorization.</p>
 */
package top.egon.cola.platform.idp.gateway.security;
