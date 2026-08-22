/**
 * DDC 实例标识和元数据贡献扩展点，允许应用补充实例身份与注册元数据。
 * 本包只声明依赖中立接口，不创建身份、不读取环境；认证由 IdP OAuth2 Client 负责。
 *
 * <p>Extension points for DDC instance identity and metadata contribution. This package declares
 * dependency-neutral interfaces only; OAuth2 credentials remain in the IdP integration.</p>
 */
@NonNullApi
package top.egon.cola.component.ddc.api.extension;

import org.springframework.lang.NonNullApi;
