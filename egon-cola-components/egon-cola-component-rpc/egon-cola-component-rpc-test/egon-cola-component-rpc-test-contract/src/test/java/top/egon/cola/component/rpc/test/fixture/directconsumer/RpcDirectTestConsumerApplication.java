package top.egon.cola.component.rpc.test.fixture.directconsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import top.egon.cola.component.rpc.test.contract.proto.EchoResponse;
import top.egon.cola.component.rpc.test.fixture.RpcTestAdmissionConfiguration;

/** One-shot separate-JVM Direct Consumer used only by the opt-in process test. */
@SpringBootApplication
@Import(RpcTestAdmissionConfiguration.class)
public class RpcDirectTestConsumerApplication {

    private RpcDirectTestConsumerApplication() {
    }

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(
                RpcDirectTestConsumerApplication.class,
                args
        );
        try {
            String message = context.getEnvironment().getProperty(
                    "rpc.test.message",
                    "direct-call"
            );
            EchoResponse response = context
                    .getBean(DirectEchoRpcTestClient.class)
                    .echo(message);
            System.out.printf(
                    "RPC_PROCESS_DIRECT_SUCCESS providerId=%s message=%s "
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
