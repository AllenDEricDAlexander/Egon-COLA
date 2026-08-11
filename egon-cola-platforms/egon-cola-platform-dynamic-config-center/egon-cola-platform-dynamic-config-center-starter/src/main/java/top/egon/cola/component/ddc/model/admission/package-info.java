/**
 * DDC 生产端使用的 Resource Server 准入请求与短期票据模型。
 * 本包不依赖 IdP 类型，只表达注册所需的精确资源实例身份、票据密文和到期调度信息。
 *
 * <p>Resource Server admission request and short-lived ticket models used by DDC producers.
 * This package has no dependency on IdP types; it expresses only the exact Resource instance
 * identity, opaque ticket credential, and expiration schedule required for registration.</p>
 */
@org.springframework.lang.NonNullApi
package top.egon.cola.component.ddc.model.admission;
