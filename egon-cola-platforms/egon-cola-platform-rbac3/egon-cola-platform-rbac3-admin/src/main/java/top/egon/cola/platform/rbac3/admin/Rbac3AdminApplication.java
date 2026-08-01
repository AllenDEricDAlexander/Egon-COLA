package top.egon.cola.platform.rbac3.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.WebApplicationType;
import top.egon.cola.platform.rbac3.admin.bootstrap.cli.Rbac3PlatformAdminBootstrapCli;

@SpringBootApplication
@ConfigurationPropertiesScan
public class Rbac3AdminApplication {

    public static void main(String[] args) {
        if (isBootstrapCommand(args)) {
            SpringApplication application = new SpringApplication(Rbac3AdminApplication.class);
            application.setWebApplicationType(WebApplicationType.NONE);
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
}
