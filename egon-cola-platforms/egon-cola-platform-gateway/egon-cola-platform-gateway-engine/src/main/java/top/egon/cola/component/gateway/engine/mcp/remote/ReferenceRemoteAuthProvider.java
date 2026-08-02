package top.egon.cola.component.gateway.engine.mcp.remote;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.core.mcp.remote.RemoteAuthProvider;

import java.util.Map;
import java.util.Objects;

/**
 * Resolves reviewed auth profiles without accepting an inbound bearer token.
 */
public final class ReferenceRemoteAuthProvider
        implements RemoteAuthProvider {

    private final ProfileResolver profiles;

    private final OAuthTokenClient oauth;

    public ReferenceRemoteAuthProvider(
            ProfileResolver profiles,
            OAuthTokenClient oauth) {
        this.profiles = Objects.requireNonNull(profiles, "profiles");
        this.oauth = Objects.requireNonNull(oauth, "oauth");
    }

    @Override
    public Publisher<OutboundAuthentication> resolve(AuthRequest request) {
        String reference = request.provider().authProfileReference();
        if (reference == null) {
            return Mono.just(new OutboundAuthentication(
                    Map.of(),
                    request.provider().tlsProfileReference()
            ));
        }
        return Mono.from(profiles.resolve(reference, request.context()))
                .flatMap(profile -> switch (profile.type()) {
                    case SECRET_REFERENCE -> Mono.just(
                            authentication(
                                    profile.authorization(),
                                    profile.tlsProfileReference()
                            )
                    );
                    case OAUTH_CLIENT_CREDENTIALS -> token(
                            profile,
                            request.context(),
                            "client_credentials",
                            null
                    );
                    case TOKEN_EXCHANGE -> token(
                            profile,
                            request.context(),
                            "urn:ietf:params:oauth:grant-type:token-exchange",
                            profile.subjectTokenReference()
                    );
                    case MTLS -> Mono.just(new OutboundAuthentication(
                            Map.of(),
                            required(
                                    profile.tlsProfileReference(),
                                    "tlsProfileReference"
                            )
                    ));
                });
    }

    private Mono<OutboundAuthentication> token(
            Profile profile,
            AuthContext context,
            String grantType,
            String subjectTokenReference) {
        OAuthTokenRequest request = new OAuthTokenRequest(
                required(profile.tokenEndpoint(), "tokenEndpoint"),
                required(profile.clientId(), "clientId"),
                required(
                        profile.clientSecretReference(),
                        "clientSecretReference"
                ),
                grantType,
                subjectTokenReference,
                profile.scope(),
                context,
                profile.tlsProfileReference()
        );
        return Mono.from(oauth.acquire(request))
                .map(token -> authentication(
                        token.tokenType() + " " + token.accessToken(),
                        profile.tlsProfileReference()
                ));
    }

    private OutboundAuthentication authentication(
            String authorization,
            String tlsProfileReference) {
        return new OutboundAuthentication(
                Map.of("authorization", required(
                        authorization,
                        "authorization"
                )),
                tlsProfileReference
        );
    }

    @FunctionalInterface
    public interface ProfileResolver {

        Publisher<Profile> resolve(
                String profileReference,
                AuthContext context
        );
    }

    @FunctionalInterface
    public interface OAuthTokenClient {

        Publisher<OAuthToken> acquire(OAuthTokenRequest request);
    }

    public enum ProfileType {
        SECRET_REFERENCE,
        OAUTH_CLIENT_CREDENTIALS,
        TOKEN_EXCHANGE,
        MTLS
    }

    public record Profile(
            ProfileType type,
            String authorization,
            String tokenEndpoint,
            String clientId,
            String clientSecretReference,
            String subjectTokenReference,
            String scope,
            String tlsProfileReference
    ) {

        public Profile {
            type = Objects.requireNonNull(type, "type");
            authorization = optional(authorization);
            tokenEndpoint = optional(tokenEndpoint);
            clientId = optional(clientId);
            clientSecretReference = optional(clientSecretReference);
            subjectTokenReference = optional(subjectTokenReference);
            scope = optional(scope);
            tlsProfileReference = optional(tlsProfileReference);
        }

        public static Profile secretReference(String authorization) {
            return new Profile(
                    ProfileType.SECRET_REFERENCE,
                    authorization,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }
    }

    public record OAuthTokenRequest(
            String tokenEndpoint,
            String clientId,
            String clientSecretReference,
            String grantType,
            String subjectTokenReference,
            String scope,
            AuthContext context,
            String tlsProfileReference
    ) {

        public OAuthTokenRequest {
            tokenEndpoint = required(tokenEndpoint, "tokenEndpoint");
            clientId = required(clientId, "clientId");
            clientSecretReference = required(
                    clientSecretReference,
                    "clientSecretReference"
            );
            grantType = required(grantType, "grantType");
            subjectTokenReference = optional(subjectTokenReference);
            scope = optional(scope);
            context = Objects.requireNonNull(context, "context");
            tlsProfileReference = optional(tlsProfileReference);
        }
    }

    public record OAuthToken(String accessToken, String tokenType) {

        public OAuthToken {
            accessToken = required(accessToken, "accessToken");
            tokenType = tokenType == null || tokenType.isBlank()
                    ? "Bearer"
                    : tokenType.trim();
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "remote auth profile " + field + " is required"
            );
        }
        return value.trim();
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
