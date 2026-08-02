package top.egon.cola.component.gateway.test.mcp.remote;

import java.util.List;

/**
 * Stable fixture names shared by contract tests and local bootstrap scripts.
 */
public final class McpRemoteFixtureCatalog {

    private static final Fixture STABLE = new Fixture(
            "STABLE_2025_11_25",
            List.of("remote_echo", "remote_failure"),
            List.of("remote_text", "remote_blob", "remote_order"),
            List.of("remote_summary"),
            List.of("remote_dashboard")
    );

    private static final Fixture RC = new Fixture(
            "RC_2026_07_28",
            List.of("remote_echo", "remote_failure"),
            List.of("remote_text", "remote_blob", "remote_order"),
            List.of("remote_summary"),
            List.of("remote_dashboard")
    );

    private McpRemoteFixtureCatalog() {
    }

    public static Operation httpOperation() {
        return new Operation("HTTP", "mcp.fixture.echo");
    }

    public static Operation rpcOperation() {
        return new Operation("RPC", "gateway.rpc.test.echo");
    }

    public static Fixture stable() {
        return STABLE;
    }

    public static Fixture rc() {
        return RC;
    }

    public record Operation(String protocol, String operationKey) {
    }

    public record Fixture(
            String dialect,
            List<String> tools,
            List<String> resources,
            List<String> prompts,
            List<String> apps) {

        public Fixture {
            tools = List.copyOf(tools);
            resources = List.copyOf(resources);
            prompts = List.copyOf(prompts);
            apps = List.copyOf(apps);
        }
    }
}
