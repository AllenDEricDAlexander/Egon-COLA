package top.egon.cola.component.gateway.test.rpc.provider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GatewayRpcTestProviderApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                GatewayRpcTestProviderApplication.class,
                args
        );
    }
}
