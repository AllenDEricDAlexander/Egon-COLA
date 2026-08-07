package top.egon.cola.component.gateway.starter.discovery;

import com.google.protobuf.Empty;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.component.gateway.starter.annotation.GatewayRequestLocation;
import top.egon.cola.component.gateway.starter.annotation.GatewayRequestSchemaField;
import top.egon.cola.component.gateway.starter.annotation.GatewayResponseSchema;
import top.egon.cola.component.gateway.starter.annotation.GatewaySchemaShape;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class RpcGatewayDefinitionContributorTest {

    @Test
    void rpcContractDeclaresExactlyOneMessageAndExplicitEnvelope()
            throws Exception {
        Method method = Contract.class.getDeclaredMethod("lookup", Empty.class);
        GatewayOperation operation = method.getAnnotation(GatewayOperation.class);

        assertThat(operation.requestSchemaFields()).singleElement()
                .satisfies(field -> {
                    assertThat(field.location())
                            .isEqualTo(GatewayRequestLocation.RPC_MESSAGE);
                    assertThat(field.schema()).isEqualTo(Empty.class);
                    assertThat(field.shape()).isEqualTo(
                            GatewaySchemaShape.OBJECT
                    );
                });
        assertThat(operation.responseSchema().wrapper())
                .isEqualTo(Void.class);
        assertThat(operation.responseSchema().schema())
                .isEqualTo(Empty.class);
    }

    @GatewayInterfaceGroup(
            businessDomainCode = "trade",
            businessDomainName = "交易域",
            entityDomainCode = "order",
            entityDomainName = "订单",
            code = "rpc-orders",
            name = "RPC 订单",
            mcpServerCode = "trade-mcp"
    )
    private interface Contract {

        @EgonRpcMethod(name = "Lookup", idempotent = true)
        @GatewayOperation(
                idempotent = true,
                registerMcp = true,
                mcpName = "rpc_order_lookup",
                requestSchemaFields = @GatewayRequestSchemaField(
                        location = GatewayRequestLocation.RPC_MESSAGE,
                        schema = Empty.class,
                        shape = GatewaySchemaShape.OBJECT
                ),
                responseSchema = @GatewayResponseSchema(
                        schema = Empty.class,
                        shape = GatewaySchemaShape.OBJECT
                )
        )
        Empty lookup(Empty request);
    }
}
