/**
 * DDC Starter 自动装配使用的类型安全配置属性，包括总体配置与 ACK 投递策略。
 * 本包只绑定本地启动和连接参数，不承载远程业务配置；自动装配依赖这些属性，其他实现按需读取。
 *
 * <p>Type-safe configuration properties used by DDC Starter automatic configuration, including
 * overall settings and acknowledgement delivery policy. These properties bind local bootstrap and
 * connectivity controls only, never remote business configuration.</p>
 */
@NonNullApi
package top.egon.cola.component.ddc.autoconfigure.properties;

import org.springframework.lang.NonNullApi;
