package top.egon.cola.component.ddc.http.registration;

import java.util.Map;

/**
 * Contributes product-specific identity and metadata to an HTTP service
 * registration without coupling DDC to that product.
 *
 * <p>中文：向 HTTP 服务注册贡献产品特有的版本和元数据，同时避免 DDC
 * 反向依赖具体产品。
 */
public interface DdcHttpRegistrationContributor {

    /**
     * Returns a service version fallback when the HTTP registration property
     * does not declare one explicitly.
     *
     * @return service version fallback, or {@code null}
     */
    default String serviceVersion() {
        return null;
    }

    /**
     * Returns immutable metadata to merge into the DDC registration.
     *
     * @return registration metadata
     */
    default Map<String, String> metadata() {
        return Map.of();
    }
}
