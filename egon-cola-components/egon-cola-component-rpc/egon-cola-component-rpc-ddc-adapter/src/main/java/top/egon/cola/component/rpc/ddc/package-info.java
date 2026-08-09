/**
 * DDC 与 Egon RPC 之间的专用适配模块。
 * 该模块隔离 DDC 领域模型与 RPC 传输契约，DDC Starter 和通用 RPC Starter
 * 均不得反向依赖此包。
 * / Dedicated adapter between DDC and Egon RPC. This module isolates DDC
 * domain models from RPC transport contracts; neither the DDC Starter nor the
 * general-purpose RPC Starter may depend back on this package.
 */
@org.springframework.lang.NonNullApi
package top.egon.cola.component.rpc.ddc;
