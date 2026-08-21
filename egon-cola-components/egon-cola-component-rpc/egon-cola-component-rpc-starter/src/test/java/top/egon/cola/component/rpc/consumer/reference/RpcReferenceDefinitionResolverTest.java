package top.egon.cola.component.rpc.consumer.reference;

import com.google.protobuf.StringValue;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import top.egon.cola.component.rpc.annotation.EgonRpcDirectReference;
import top.egon.cola.component.rpc.annotation.EgonRpcReference;
import top.egon.cola.component.rpc.annotation.FailStrategy;
import top.egon.cola.component.rpc.annotation.LoadBalance;
import top.egon.cola.component.rpc.config.EgonRpcProperties;
import top.egon.cola.component.rpc.context.identity.RpcProcessIdentity;
import top.egon.cola.component.rpc.contract.descriptor.RpcContractDescriptor;
import top.egon.cola.component.rpc.contract.validation.RpcContractValidator;
import top.egon.cola.component.rpc.consumer.loadbalance.RpcLoadBalanceKeyResolver;
import top.egon.cola.component.rpc.exception.EgonRpcErrorCode;
import top.egon.cola.component.rpc.exception.EgonRpcException;
import top.egon.cola.component.rpc.support.RpcProviderTestFixtures.EchoContract;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RpcReferenceDefinitionResolverTest {

    private final RpcContractDescriptor descriptor =
            new RpcContractValidator().validate(EchoContract.class);

    @Test
    void resolvesDirectIdentityAndCommonPolicyOnce() throws Exception {
        ApplicationContext context = mock(ApplicationContext.class);
        RpcLoadBalanceKeyResolver resolver = ignored -> "stable-key";
        when(context.getBean("hash-key")).thenReturn(resolver);
        when(context.getBean("fallback")).thenReturn(mock(EchoContract.class));
        EgonRpcProperties properties = new EgonRpcProperties();
        properties.getConsumer().setMaxRetries(4);
        RpcReferenceDefinitionResolver definitions = new RpcReferenceDefinitionResolver(
                properties,
                new RpcProcessIdentity("consumer", "test", "127.0.0.1", 1, "i-1"),
                context);

        RpcReferenceDefinition definition = definitions.resolve(
                Holder.class.getDeclaredField("direct"), descriptor);

        assertThat(definition.mode()).isEqualTo(RpcReferenceMode.DIRECT);
        assertThat(definition.directQuery().bizCode()).isEqualTo("commerce");
        assertThat(definition.directQuery().appCode()).isEqualTo("orders");
        assertThat(definition.directQuery().env()).isEqualTo("test");
        RpcReferencePolicy policy = definition.policyFor(
                EchoContract.class.getMethod("echo", StringValue.class));
        assertThat(policy.timeoutMs()).isEqualTo(400);
        assertThat(policy.retries()).isEqualTo(2);
        assertThat(policy.loadBalance()).isEqualTo(LoadBalance.CONSISTENT_HASH);
        assertThat(policy.failStrategy()).isEqualTo(FailStrategy.LOCAL_FALLBACK);
        assertThat(policy.fallbackBean()).isEqualTo("fallback");
        assertThat(policy.keyResolver()).isSameAs(resolver);
    }

    @Test
    void rejectsConsistentHashWithoutNamedResolver() {
        RpcReferenceDefinitionResolver definitions = new RpcReferenceDefinitionResolver(
                new EgonRpcProperties(),
                new RpcProcessIdentity("consumer", "test", "127.0.0.1", 1, "i-1"));

        assertThatThrownBy(() -> definitions.resolve(
                Holder.class.getDeclaredField("gateway"), descriptor))
                .isInstanceOfSatisfying(EgonRpcException.class, error -> {
                    assertThat(error.getCode()).isEqualTo(EgonRpcErrorCode.RPC_INVALID_CONTRACT);
                    assertThat(error.getMessage()).contains("bean");
                    assertThat(error.getMessage()).doesNotContain("authorization");
                });
    }

    @Test
    void rejectsTwoReferenceAnnotations() {
        RpcReferenceDefinitionResolver definitions = new RpcReferenceDefinitionResolver(
                new EgonRpcProperties(),
                new RpcProcessIdentity("consumer", "test", "127.0.0.1", 1, "i-1"));

        assertThatThrownBy(() -> definitions.resolve(
                Holder.class.getDeclaredField("both"), descriptor))
                .isInstanceOf(EgonRpcException.class)
                .hasMessageContaining("exactly one");
    }

    static final class Holder {

        @EgonRpcDirectReference(
                bizCode = "commerce",
                appCode = "orders",
                timeoutMs = 400,
                retries = 2,
                loadBalance = LoadBalance.CONSISTENT_HASH,
                fallbackBean = "fallback",
                failStrategy = FailStrategy.LOCAL_FALLBACK,
                loadBalanceKeyResolver = "hash-key")
        private EchoContract direct;

        @EgonRpcReference(loadBalance = LoadBalance.CONSISTENT_HASH)
        private EchoContract gateway;

        @EgonRpcReference
        @EgonRpcDirectReference(bizCode = "commerce", appCode = "orders")
        private EchoContract both;
    }
}
