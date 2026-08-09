/**
 * DDC Admin 直连 RPC Provider 边界。
 * 本包仅负责 protobuf 映射、认证上下文读取和应用门面委托，不访问持久化或 Redis。
 * / Direct RPC provider boundary for DDC Admin. This package only maps
 * protobuf messages, reads authenticated context, and delegates to application
 * facades; it does not access persistence or Redis.
 */
@org.springframework.lang.NonNullApi
package top.egon.cola.component.ddc.admin.rpc.provider;
