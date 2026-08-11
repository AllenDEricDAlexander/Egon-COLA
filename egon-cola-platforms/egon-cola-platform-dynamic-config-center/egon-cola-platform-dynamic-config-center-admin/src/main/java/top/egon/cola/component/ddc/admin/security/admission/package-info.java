/**
 * DDC Resource Server 准入安全边界。
 *
 * <p>DDC Resource Server admission security boundary.</p>
 *
 * <p>本包统一校验 IdP 短期 Admission Ticket、当前 Resource 运行态投影以及注册请求的
 * {@code bizCode + appCode + env + instanceId} 精确绑定，并仅向租约层输出可审计声明。
 * 原始票据不会进入 Redis、数据库、发现目录或日志。</p>
 *
 * <p>This package centrally validates short-lived IdP Admission Tickets, the current Resource
 * runtime projection, and exact {@code bizCode + appCode + env + instanceId} request binding. It
 * exposes only auditable claims to the lease layer; raw tickets never enter Redis, the database,
 * discovery catalogs, or logs.</p>
 */
package top.egon.cola.component.ddc.admin.security.admission;
