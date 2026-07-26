package top.egon.cola.component.gateway.test.webflux;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GatewayWebFluxHttpTestProviderApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                GatewayWebFluxHttpTestProviderApplication.class,
                args
        );
    }
}
