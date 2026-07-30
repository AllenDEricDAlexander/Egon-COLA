package top.egon.cola.platform.rbac3.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class Rbac3AdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(Rbac3AdminApplication.class, args);
    }
}
