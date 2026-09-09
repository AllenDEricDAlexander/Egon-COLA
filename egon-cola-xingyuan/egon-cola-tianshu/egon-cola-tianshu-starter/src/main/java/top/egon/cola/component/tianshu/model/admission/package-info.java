/**
 * Tianshu 准入模型包，承载注册凭据相关的共享模型；凭据获取由 Tianquan-Shoubing OAuth2 Client 负责。
 * 本包只保留协议模型，不执行凭据获取或注册流程。
 *
 * Tianshu registration credentials are carried by the shared registry/config request models.
 * Tianquan-Shoubing OAuth2 Client owns acquisition; this legacy model package intentionally has no Java types.
 *
 * <p>Registration credentials are opaque SERVICE tokens acquired through the Tianquan-Shoubing OAuth2 Client.</p>
 */
@org.springframework.lang.NonNullApi
package top.egon.cola.component.tianshu.model.admission;
