/**
 * DDC 实例标识与元数据贡献扩展点，允许应用补充实例身份信息。
 * 本包只声明扩展接口，不创建身份、不读取环境；默认实现位于 {@code service.lifecycle}。
 *
 * <p>Extension points for DDC instance identity and metadata contribution. This package declares
 * extension interfaces only; identity creation and environment access belong to default
 * implementations in {@code service.lifecycle}.</p>
 */
@NonNullApi
package top.egon.cola.component.ddc.api.extension;

import org.springframework.lang.NonNullApi;
