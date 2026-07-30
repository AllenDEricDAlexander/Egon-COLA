package top.egon.cola.component.gateway.engine.http;

import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.core.http.NormalizedHttpRequest;
import top.egon.cola.component.gateway.core.route.HttpRouteMatch;
import top.egon.cola.component.gateway.core.security.TrustedIdentity;

import java.util.Objects;
import java.util.Set;

public interface GatewayHttpSecurityProcessor {

    Mono<Outcome> authorize(
            AccessZone accessZone,
            GatewayInboundHttpRequest request,
            NormalizedHttpRequest normalized,
            HttpRouteMatch route,
            String traceId);

    record Outcome(
            TrustedIdentity trustedIdentity,
            Set<String> fieldsToRemove
    ) {

        public Outcome {
            trustedIdentity = Objects.requireNonNull(
                    trustedIdentity,
                    "trustedIdentity"
            );
            fieldsToRemove = Set.copyOf(Objects.requireNonNull(
                    fieldsToRemove,
                    "fieldsToRemove"
            ));
        }

        public static Outcome anonymous() {
            return new Outcome(TrustedIdentity.empty(), Set.of());
        }
    }
}
