package top.egon.cola.component.rpc.test.fixture;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.ddc.api.extension.DdcAdmissionTicketSupplier;
import top.egon.cola.component.ddc.model.admission.DdcAdmissionTicket;

import java.net.URI;
import java.time.Instant;

/**
 * Supplies the fixed admission credential accepted by the RPC process test.
 */
@Configuration(proxyBeanMethods = false)
public class RpcTestAdmissionConfiguration {

    /**
     * Creates tickets bound to the exact process identity requested by DDC.
     *
     * @return process-test admission ticket supplier
     */
    @Bean
    public DdcAdmissionTicketSupplier rpcTestAdmissionTicketSupplier() {
        return (bizCode, appCode, environment, instanceId) ->
                new DdcAdmissionTicket(
                        "test-admission-ticket",
                        Instant.now().plusSeconds(300),
                        "rpc-process-test-resource",
                        URI.create("urn:egon:resource:rpc-process-test"),
                        1L,
                        bizCode,
                        appCode,
                        environment,
                        instanceId,
                        "rpc-process-test-credential"
                );
    }
}
