package top.egon.cola.platform.idp.admin.token.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.idp.admin.token.domain.IdentitySigningKeyEntity;
import top.egon.cola.platform.idp.admin.token.infrastructure.IdentitySigningKeyRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SigningKeyAdminServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-08-02T00:00:00Z");

    private final IdentitySigningKeyRepository keys =
            mock(IdentitySigningKeyRepository.class);
    private final SigningKeyRuntime runtime = mock(SigningKeyRuntime.class);
    private SigningKeyAdminService service;

    @BeforeEach
    void setUp() {
        service = new SigningKeyAdminService(
                keys,
                runtime,
                new ObjectMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void publishesMetadataWithoutReturningEncryptedPrivateMaterial() {
        when(keys.existsById("key-2026-08")).thenReturn(false);
        when(keys.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SigningKeyAdminService.SigningKeyView published = service.publish(
                new SigningKeyAdminService.PublishSigningKeyCommand(
                        "key-2026-08",
                        "kms:v1:encrypted-value",
                        "{\"kty\":\"RSA\",\"kid\":\"key-2026-08\",\"n\":\"abc\",\"e\":\"AQAB\"}"
                )
        );

        assertThat(published.kid()).isEqualTo("key-2026-08");
        assertThat(published.publicJwk()).contains("\"kty\":\"RSA\"");
        assertThat(published.toString())
                .doesNotContain("encrypted-value", "encryptedPrivateKey");
    }

    @Test
    void activationMustSucceedInRuntimeBeforeDatabaseStateChanges() {
        IdentitySigningKeyEntity key = IdentitySigningKeyEntity.published(
                "key-2026-08",
                "kms:v1:encrypted-value",
                "{\"kty\":\"RSA\",\"kid\":\"key-2026-08\",\"n\":\"abc\",\"e\":\"AQAB\"}",
                NOW
        );
        when(keys.findById("key-2026-08")).thenReturn(Optional.of(key));
        when(keys.findByStatus(IdentitySigningKeyEntity.Status.ACTIVE))
                .thenReturn(List.of());
        when(keys.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(runtime.isServing("key-2026-08")).thenReturn(true);

        SigningKeyAdminService.SigningKeyView active =
                service.activate("key-2026-08", 0L);

        verify(runtime).activate("key-2026-08");
        assertThat(active.status()).isEqualTo("ACTIVE");
        assertThat(active.runtimeServing()).isTrue();

        assertThatThrownBy(() -> service.retire("key-2026-08", 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("serving");
    }
}
