/**
 * Tianshu 与 Egon RPC 之间的专用适配模块。
 * 该模块隔离 Tianshu 领域模型与 RPC 传输契约，Tianshu Starter 和通用 RPC Starter
 * 均不得反向依赖此包。
 * / Dedicated adapter between Tianshu and Egon RPC. This module isolates Tianshu
 * domain models from RPC transport contracts; neither the Tianshu Starter nor the
 * general-purpose RPC Starter may depend back on this package.
 */
@org.springframework.lang.NonNullApi
package top.egon.cola.component.rpc.tianshu;
