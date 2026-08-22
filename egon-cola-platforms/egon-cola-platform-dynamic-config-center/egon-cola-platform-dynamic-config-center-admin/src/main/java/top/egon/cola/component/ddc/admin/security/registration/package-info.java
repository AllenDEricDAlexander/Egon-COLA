/**
 * DDC PLATFORM SERVICE registration security boundary.
 *
 * <p>DDC PLATFORM SERVICE registration security boundary.</p>
 *
 * <p>本包统一校验 IdP PLATFORM SERVICE Token、当前 Resource 运行态投影以及注册请求的
 * {@code bizCode + appCode + env + instanceId} 精确绑定，并仅向租约层输出 verified identity。
 * 原始 Token 不会进入 Redis、数据库、发现目录或日志。</p>
 *
 * <p>This package centrally validates IdP PLATFORM SERVICE tokens, the current Resource runtime
 * projection, and exact {@code bizCode + appCode + env + instanceId} request binding. It exposes
 * only a verified identity to the lease layer; raw tokens never enter Redis, the database,
 * discovery catalogs, or logs.</p>
 */
package top.egon.cola.component.ddc.admin.security.registration;
