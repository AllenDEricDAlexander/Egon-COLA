package top.egon.cola.component.gateway.test.mcp.remote;

import org.springframework.boot.SpringApplication;

import java.util.Map;

/**
 * Alternate entry point for running only the RC-labelled fixture process.
 */
public final class RcRemoteMcpApplication {

    private RcRemoteMcpApplication() {
    }

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(
                StableRemoteMcpApplication.class
        );
        application.setDefaultProperties(Map.of(
                "spring.application.name",
                "gateway-test-mcp-remote-rc",
                "server.port",
                "18152"
        ));
        application.run(args);
    }
}
