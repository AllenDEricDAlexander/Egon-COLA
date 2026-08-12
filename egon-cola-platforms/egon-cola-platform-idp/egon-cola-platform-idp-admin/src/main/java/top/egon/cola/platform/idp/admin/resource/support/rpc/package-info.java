/**
 * Resource Server 业务域的内部 Egon-RPC 传输适配器。
 * 本包把跨进程 Protobuf 消息转换为领域请求并委托既有准入服务，不承载准入规则，也不开放
 * HTTP 后端接口。
 *
 * <p>Internal Egon-RPC transport adapters for the Resource Server domain. This package converts
 * cross-process Protobuf messages into domain requests and delegates to the existing admission
 * service; it contains no admission rules and exposes no HTTP backend endpoint.</p>
 */
package top.egon.cola.platform.idp.admin.resource.support.rpc;
