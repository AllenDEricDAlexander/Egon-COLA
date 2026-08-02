package top.egon.cola.platform.idp.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class IdpAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdpAdminApplication.class, args);
    }
}
