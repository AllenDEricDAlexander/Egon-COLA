package top.egon.cola.platform.tianquan.jianshen.gateway.performance;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import top.egon.cola.component.yuheng.contract.protocol.AccessZone;
import top.egon.cola.component.yuheng.contract.protocol.GatewayProtocol;
import top.egon.cola.component.yuheng.core.context.GatewayPrincipal;
import top.egon.cola.component.yuheng.core.security.AuthorizationDecision;
import top.egon.cola.component.yuheng.core.security.GatewayAuthContext;
import top.egon.cola.component.yuheng.core.security.SecurityDecision;
import top.egon.cola.platform.tianquan.jianshen.gateway.security.Rbac3BizAppScopeAuthorizationProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayHotPathBudgetTest {

    private static final int DECISION_COUNT = 1_000;

    @Test
    void oneGatewayDecisionPerRequestAndNoAdminOrDatabaseClientInHotPath()
            throws Exception {
        AtomicInteger calls = new AtomicInteger();
        var provider = new Rbac3BizAppScopeAuthorizationProvider(context -> {
            calls.incrementAndGet();
            return AuthorizationDecision.allow();
        });

        for (int index = 0; index < DECISION_COUNT; index++) {
            assertEquals(SecurityDecision.ALLOW,
                    Mono.from(provider.authorize(context())).block().decision());
        }
        assertEquals(DECISION_COUNT, calls.get());

        Path source = Path.of(System.getProperty("basedir")).resolve("src/main/java");
        try (var files = Files.walk(source)) {
            List<String> violations = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> lines(path).stream())
                    .filter(line -> List.of(
                                    "EntityManager", "JdbcTemplate", "WebClient",
                                    "RestClient", "tianquan-jianshen-admin",
                                    "operationMapping(", "permissions()")
                            .stream().anyMatch(line::contains))
                    .toList();
            assertEquals(List.of(), violations);
        }
    }

    @Test
    void calibratedEnvironmentMayEnforceReactiveDecisionBudget() {
        Assumptions.assumeTrue(Boolean.getBoolean("tianquan-jianshen.performance.enforce"));
        var provider = new Rbac3BizAppScopeAuthorizationProvider(
                ignored -> AuthorizationDecision.allow());

        Instant started = Instant.now();
        for (int index = 0; index < DECISION_COUNT; index++) {
            Mono.from(provider.authorize(context())).block();
        }

        assertTrue(Duration.between(started, Instant.now())
                .compareTo(Duration.ofSeconds(3)) < 0);
    }

    private static GatewayAuthContext context() {
        return new GatewayAuthContext(
                AccessZone.INTERNAL, GatewayProtocol.HTTP, "operation", "route",
                "policy", "/payments", "GET", Set.of("bearer"),
                new GatewayPrincipal(
                        "user", "USER", "tenant", null, true,
                        Map.of()),
                "127.0.0.1", "trace", "request",
                Instant.parse("2026-07-30T08:00:05Z"), "release");
    }

    private static List<String> lines(Path path) {
        try {
            return Files.readAllLines(path);
        } catch (java.io.IOException error) {
            throw new IllegalStateException(error);
        }
    }
}
