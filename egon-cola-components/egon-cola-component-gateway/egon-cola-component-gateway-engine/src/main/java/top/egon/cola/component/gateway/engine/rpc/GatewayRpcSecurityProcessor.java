package top.egon.cola.component.gateway.engine.rpc;

import io.grpc.Deadline;
import io.grpc.Metadata;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.core.security.TrustedIdentity;

import java.util.Objects;
import java.util.Set;

public interface GatewayRpcSecurityProcessor {

    Mono<Outcome> authorize(
            RuntimeRpcRoute route,
            Metadata inboundMetadata,
            String traceId,
            Deadline deadline);

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
