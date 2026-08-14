package top.egon.cola.component.gateway.core.security;

/**
 * Classifies the trust boundary of a published Gateway route.
 */
public enum GatewayRouteSecurityType {
    PUBLIC_PROTOCOL,
    IDENTITY_PROTECTED,
    BUSINESS_PROTECTED
}
