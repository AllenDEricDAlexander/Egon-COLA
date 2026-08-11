package top.egon.cola.component.rpc.test.fixture.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import top.egon.cola.component.rpc.test.fixture.RpcTestAdmissionConfiguration;
import top.egon.cola.component.rpc.test.contract.proto.EchoResponse;

@SpringBootApplication
@Import(RpcTestAdmissionConfiguration.class)
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
                    .getBean(EchoRpcTestClient.class)
                    .echo(message);
            System.out.printf(
                    "RPC_PROCESS_SUCCESS providerId=%s message=%s "
                            + "invocationId=%s traceId=%s%n",
                    response.getProviderId(),
                    response.getMessage(),
                    response.getInvocationId(),
                    response.getTraceId()
            );
        } finally {
            context.close();
        }
    }
}
