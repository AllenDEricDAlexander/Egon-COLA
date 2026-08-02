package top.egon.cola.platform.idp.admin.token.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.idp.admin.token.domain.IdentitySigningKeyEntity;
import top.egon.cola.platform.idp.admin.token.infrastructure.IdentitySigningKeyRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class SigningKeyAdminService {

    private static final Set<String> PRIVATE_JWK_FIELDS = Set.of(
            "d", "p", "q", "dp", "dq", "qi", "oth"
    );

    private final IdentitySigningKeyRepository keys;
    private final SigningKeyRuntime runtime;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public SigningKeyAdminService(
            IdentitySigningKeyRepository keys,
            SigningKeyRuntime runtime,
            ObjectMapper objectMapper
    ) {
        this(keys, runtime, objectMapper, Clock.systemUTC());
    }

    SigningKeyAdminService(
            IdentitySigningKeyRepository keys,
            SigningKeyRuntime runtime,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.keys = Objects.requireNonNull(keys, "keys");
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Transactional(readOnly = true)
    public List<SigningKeyView> list() {
        return keys.findAll().stream()
                .sorted(Comparator.comparing(IdentitySigningKeyEntity::getKid))
                .map(this::view)
                .toList();
    }

    @Transactional
    public SigningKeyView publish(PublishSigningKeyCommand command) {
        Objects.requireNonNull(command, "command");
        if (keys.existsById(command.kid())) {
            throw new IllegalStateException("signing key already exists");
        }
        String encryptedPrivateKey = encrypted(command.encryptedPrivateKey());
        String publicJwk = publicJwk(command.kid(), command.publicJwk());
        IdentitySigningKeyEntity key = keys.save(
                IdentitySigningKeyEntity.published(
                        command.kid(),
                        encryptedPrivateKey,
                        publicJwk,
                        clock.instant()
                )
        );
        return view(key);
    }

    @Transactional
    public SigningKeyView activate(String kid, long expectedVersion) {
        IdentitySigningKeyEntity target = key(kid);
        if (target.getVersion() != expectedVersion) {
            throw new IllegalStateException("stale signing key version");
        }
        runtime.activate(target.getKid());
        Instant now = clock.instant();
        for (IdentitySigningKeyEntity active
                : keys.findByStatus(IdentitySigningKeyEntity.Status.ACTIVE)) {
            if (!active.getKid().equals(target.getKid())) {
                active.retire(active.getVersion(), now);
                keys.save(active);
            }
        }
        target.activate(expectedVersion, now);
        keys.save(target);
        return view(target);
    }

    @Transactional
    public SigningKeyView retire(String kid, long expectedVersion) {
        IdentitySigningKeyEntity target = key(kid);
        if (runtime.isServing(target.getKid())) {
            throw new IllegalStateException(
                    "serving signing key cannot be retired"
            );
        }
        target.retire(expectedVersion, clock.instant());
        keys.save(target);
        return view(target);
    }

    private SigningKeyView view(IdentitySigningKeyEntity key) {
        return new SigningKeyView(
                key.getKid(),
                key.getAlgorithm(),
                key.getPublicJwk(),
                key.getStatus().name(),
                runtime.isServing(key.getKid()),
                key.getActivatedAt(),
                key.getRetiredAt(),
                key.getVersion(),
                key.getCreatedAt(),
                key.getUpdatedAt()
        );
    }

    private IdentitySigningKeyEntity key(String kid) {
        if (kid == null || kid.isBlank() || !kid.equals(kid.trim())) {
            throw new IllegalArgumentException("kid is required");
        }
        return keys.findById(kid).orElseThrow(() ->
                new IllegalArgumentException("signing key was not found"));
    }

    private String publicJwk(String kid, String value) {
        try {
            JsonNode root = objectMapper.readTree(value);
            if (root == null
                    || !root.isObject()
                    || !"RSA".equals(text(root, "kty"))
                    || !kid.equals(text(root, "kid"))
                    || text(root, "n") == null
                    || text(root, "e") == null
                    || PRIVATE_JWK_FIELDS.stream().anyMatch(root::has)) {
                throw new IllegalArgumentException("invalid public JWK");
            }
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("invalid public JWK", exception);
        }
    }

    private static String text(JsonNode root, String field) {
        JsonNode value = root.get(field);
        return value == null || !value.isTextual() || value.textValue().isBlank()
                ? null
                : value.textValue();
    }

    private static String encrypted(String value) {
        if (value == null
                || value.isBlank()
                || value.length() > 65_536
                || !value.startsWith("kms:")) {
            throw new IllegalArgumentException(
                    "encrypted private key must use a KMS envelope"
            );
        }
        return value;
    }

    public record PublishSigningKeyCommand(
            String kid,
            String encryptedPrivateKey,
            String publicJwk
    ) {
    }

    public record SigningKeyView(
            String kid,
            String algorithm,
            String publicJwk,
            String status,
            boolean runtimeServing,
            Instant activatedAt,
            Instant retiredAt,
            long version,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
