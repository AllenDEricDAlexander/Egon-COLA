package top.egon.cola.component.rpc.test.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import top.egon.cola.component.rpc.test.contract.proto.EchoResponse;

@SpringBootApplication
public class RpcTestConsumerApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(
                RpcTestConsumerApplication.class,
                args
        );
        if (!context.getEnvironment().getProperty(
                "rpc.test.run-once",
                Boolean.class,
                false
        )) {
            return;
        }
        try {
            String message = context.getEnvironment().getProperty(
                    "rpc.test.message",
                    "hello"
            );
            EchoResponse response = context
                    .getBean(EchoRpcClient.class)
                    .echo(message);
            System.out.printf(
                    "RPC_PROCESS_SUCCESS providerId=%s message=%s%n",
                    response.getProviderId(),
                    response.getMessage()
            );
        } finally {
            context.close();
        }
    }
}
