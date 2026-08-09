/**
 * DDC 对外端口与扩展点的组织包，只用于归类客户端、扩展、刷新和注册接口。
 * 本包不保存实现或数据对象；实现依赖这里的端口，数据由 {@code model} 提供。
 *
 * <p>Organizational package for public DDC ports and extension points covering clients,
 * extensions, refresh, and registry contracts. It contains no implementations or data objects;
 * implementations depend on these ports and exchange types from {@code model}.</p>
 */
@NonNullApi
package top.egon.cola.component.ddc.api;

import org.springframework.lang.NonNullApi;
