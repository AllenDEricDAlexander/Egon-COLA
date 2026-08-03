package top.egon.cola.component.gateway.test.mcp;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import top.egon.cola.component.gateway.test.mcp.remote.StableRemoteMcpApplication;

import java.net.URI;

final class RemoteMcpFixtureServer implements AutoCloseable {

    private final ConfigurableApplicationContext context;

    private final int port;

    private RemoteMcpFixtureServer(
            ConfigurableApplicationContext context,
            int port) {
        this.context = context;
        this.port = port;
    }

    static RemoteMcpFixtureServer start() {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(
                StableRemoteMcpApplication.class
        ).logStartupInfo(false).run(
                "--server.port=0",
                "--spring.application.name=gateway-test-mcp-remote",
                "--spring.config.name=gateway-mcp-fixture",
                "--spring.main.banner-mode=off",
                "--spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.jdbc."
                        + "DataSourceAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.orm.jpa."
                        + "HibernateJpaAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.flyway."
                        + "FlywayAutoConfiguration,"
                        + "org.redisson.spring.starter."
                        + "RedissonAutoConfigurationV2,"
                        + "org.springframework.boot.autoconfigure.security."
                        + "servlet.SecurityAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.security."
                        + "servlet.UserDetailsServiceAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.security."
                        + "servlet.SecurityFilterAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.security."
                        + "oauth2.resource.servlet."
                        + "OAuth2ResourceServerAutoConfiguration,"
                        + "org.springframework.boot.actuate.autoconfigure."
                        + "security.servlet."
                        + "ManagementWebSecurityAutoConfiguration",
                "--egon.cola.component.id.machine-id=0",
                "--egon.cola.component.ddc.enabled=false",
                "--management.endpoints.enabled-by-default=false"
        );
        int port = ((WebServerApplicationContext) context)
                .getWebServer()
                .getPort();
        return new RemoteMcpFixtureServer(context, port);
    }

    URI stableEndpoint() {
        return URI.create("http://127.0.0.1:" + port + "/remote/stable");
    }

    URI rcEndpoint() {
        return URI.create("http://127.0.0.1:" + port + "/remote/rc");
    }

    @Override
    public void close() {
        context.close();
    }
}
