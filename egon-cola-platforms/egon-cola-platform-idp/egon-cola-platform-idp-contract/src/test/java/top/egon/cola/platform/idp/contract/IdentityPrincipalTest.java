package top.egon.cola.platform.idp.contract;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IdentityPrincipalTest {

    @Test
    void rejectsBlankStableIdentityClaims() {
        assertThrows(IllegalArgumentException.class, () ->
                new IdentityPrincipal(
                        " ",
                        "tenant-a",
                        "sid-a",
                        "web",
                        "jti-a",
                        0L,
                        Set.of("mock-api"),
                        Instant.EPOCH,
                        Instant.EPOCH.plusSeconds(900)
                ));
    }

    @Test
    void accessIdentityContainsNoAuthorizationFacts() {
        Set<String> components = Arrays.stream(
                        IdentityPrincipal.class.getRecordComponents()
                )
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());

        assertFalse(components.contains("roles"));
        assertFalse(components.contains("permissions"));
        assertFalse(components.contains("authVersion"));
        assertFalse(components.contains("contextVersion"));
        assertFalse(components.contains("policyVersion"));
    }
}
