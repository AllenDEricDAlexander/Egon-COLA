package top.egon.cola.component.gateway.test.http;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GatewayHttpTestProviderApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                GatewayHttpTestProviderApplication.class,
                args
        );
    }
}
