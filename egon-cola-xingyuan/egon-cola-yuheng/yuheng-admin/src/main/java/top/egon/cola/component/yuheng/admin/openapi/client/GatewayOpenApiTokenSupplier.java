package top.egon.cola.component.yuheng.admin.openapi.client;

import java.net.URI;

/**
 * Narrow token boundary for Provider OpenAPI reads.
 *
 * <p>The implementation delegates to the existing Tianquan-Shoubing SERVICE-token client;
 * this port keeps OAuth credentials and registration details out of the
 * network client.</p>
 */
@FunctionalInterface
public interface GatewayOpenApiTokenSupplier {

    /**
     * Obtains a token for one exact Provider Resource URI and scope.
     *
     * @param resourceUri trusted Provider Resource URI
     * @param scope least-privilege OpenAPI read scope
     * @return opaque bearer token
     */
    String tokenFor(URI resourceUri, String scope);
}
