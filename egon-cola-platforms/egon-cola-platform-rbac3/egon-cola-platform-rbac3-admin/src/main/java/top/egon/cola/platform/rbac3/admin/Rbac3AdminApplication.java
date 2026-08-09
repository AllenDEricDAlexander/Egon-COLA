package top.egon.cola.platform.rbac3.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.WebApplicationType;
import org.springframework.core.env.MapPropertySource;
import top.egon.cola.platform.rbac3.admin.bootstrap.cli.Rbac3PlatformAdminBootstrapCli;

import java.util.Map;

@SpringBootApplication
@ConfigurationPropertiesScan
public class Rbac3AdminApplication {

    public static void main(String[] args) {
        if (isBootstrapCommand(args)) {
            SpringApplication application = new SpringApplication(Rbac3AdminApplication.class);
            application.setWebApplicationType(WebApplicationType.NONE);
            application.addInitializers(context -> context.getEnvironment()
                    .getPropertySources()
                    .addFirst(new MapPropertySource(
                            "rbac3BootstrapRuntime",
                            bootstrapRuntimeProperties())));
            try (var context = application.run(args)) {
                context.getBean(Rbac3PlatformAdminBootstrapCli.class)
                        .run(args, System.in);
            }
            return;
        }
        SpringApplication.run(Rbac3AdminApplication.class, args);
    }

    static boolean isBootstrapCommand(String[] args) {
        return args.length > 0 && "bootstrap-platform-admin".equals(args[0]);
    }

    static Map<String, Object> bootstrapRuntimeProperties() {
        return Map.of(
                "egon.cola.component.ddc.enabled", false,
                "egon.cola.component.gateway.reporting.enabled", false,
                "egon.cola.component.ddc.registry.http.enabled", false,
                "egon.cola.component.transactional-outbox.polling.enabled", false,
                "management.endpoint.health.validate-group-membership", false);
    }
}
