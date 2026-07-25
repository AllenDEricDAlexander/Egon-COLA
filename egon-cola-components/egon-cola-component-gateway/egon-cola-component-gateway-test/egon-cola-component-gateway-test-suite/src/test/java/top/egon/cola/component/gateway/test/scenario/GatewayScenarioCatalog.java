package top.egon.cola.component.gateway.test.scenario;

import java.util.List;

public final class GatewayScenarioCatalog {

    private GatewayScenarioCatalog() {
    }

    public static List<Scenario> all() {
        return List.of(
                scenario("starter-report", "Starter", "Admin Catalog"),
                scenario("provider-discovery", "DDC Registry", "Engine"),
                scenario("http-forward", "HTTP Client", "HTTP Provider"),
                scenario("rpc-forward", "RPC Consumer", "RPC Provider"),
                scenario("http-to-rpc", "HTTP Client", "RPC Provider"),
                scenario("load-balance", "Engine", "Two Providers"),
                scenario("rule-release", "Admin", "Engine ACK and LKG"),
                scenario("traffic-governance", "Policy", "Engine"),
                scenario("security-extension", "Security SPI", "Engine"),
                scenario("trace-kafka", "Engine", "Kafka and Admin"),
                scenario("failure-recovery", "Fault Injector", "Topology"),
                scenario(
                        "rpc-slot-invariant",
                        "Zero One Two Slots",
                        "RPC Consumer"
                )
        );
    }

    private static Scenario scenario(
            String code,
            String source,
            String target) {
        return new Scenario(code, source, target, true);
    }

    public record Scenario(
            String code,
            String source,
            String target,
            boolean realProcessRequired
    ) {
    }
}
