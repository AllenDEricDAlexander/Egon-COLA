package top.egon.cola.component.yuheng.core.security;

import top.egon.cola.component.yuheng.contract.protocol.GatewayProtocol;

import java.util.Set;

public interface GatewayIdentityMapper {

    String mapperId();

    Set<GatewayProtocol> supportedProtocols();

    TrustedIdentity map(GatewayAuthContext context);
}
