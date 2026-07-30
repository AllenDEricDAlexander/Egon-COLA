package top.egon.cola.component.gateway.test.rpc.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GatewayRpcTestConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                GatewayRpcTestConsumerApplication.class,
                args
        );
    }
}
