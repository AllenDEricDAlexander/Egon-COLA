/**
 * DDC 实例标识、元数据贡献和准入票据供应扩展点，允许应用补充实例身份与认证信息。
 * 本包只声明依赖中立接口，不创建身份、不读取环境；实现位于相应集成 Starter。
 *
 * <p>Extension points for DDC instance identity, metadata contribution, and admission-ticket
 * supply. This package declares dependency-neutral interfaces only; implementations belong to
 * the corresponding integration starters.</p>
 */
@NonNullApi
package top.egon.cola.component.ddc.api.extension;

import org.springframework.lang.NonNullApi;
