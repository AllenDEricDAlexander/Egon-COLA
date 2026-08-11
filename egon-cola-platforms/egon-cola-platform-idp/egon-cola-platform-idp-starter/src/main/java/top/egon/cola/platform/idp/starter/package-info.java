/**
 * 为普通 Servlet 应用提供统一 IdP 资源服务器接入能力。
 * 该模块使用已登记机器凭证取得 DDC Admission Ticket，验证 IdP 已签发的访问令牌，读取
 * 用户实时状态并形成统一身份上下文；它不审批 Resource、不签发 Token，也不负责业务权限决策。
 *
 * <p>Provides unified IdP resource-server integration for regular Servlet applications. The module
 * obtains DDC Admission Tickets with registered machine credentials, validates access tokens
 * already issued by IdP, reads current user state, and builds a unified identity context. It does
 * not approve Resources, issue tokens, or own business authorization decisions.</p>
 */
package top.egon.cola.platform.idp.starter;
