package top.egon.cola.component.rpc.test.fixture.provider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import top.egon.cola.component.rpc.test.fixture.RpcTestAdmissionConfiguration;

@SpringBootApplication
@Import(RpcTestAdmissionConfiguration.class)
public class RpcTestProviderApplication {

    public static void main(String[] args) {
        SpringApplication.run(RpcTestProviderApplication.class, args);
    }
}
