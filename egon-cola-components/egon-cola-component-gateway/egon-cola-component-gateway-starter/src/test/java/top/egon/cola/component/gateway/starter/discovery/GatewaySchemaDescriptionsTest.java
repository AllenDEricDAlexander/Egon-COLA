package top.egon.cola.component.gateway.starter.discovery;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.component.gateway.starter.annotation.GatewaySchemaField;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewaySchemaDescriptionsTest {

    @Test
    void addsDescriptionsToNestedFieldsWithoutMutatingSource() throws Exception {
        Map<String, Object> source = Map.of(
                "type", "object",
                "properties", Map.of(
                        "customerId", Map.of("type", "string"),
                        "address", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "city", Map.of("type", "string")
                                )
                        )
                )
        );

        Map<String, Object> result = GatewaySchemaDescriptions.apply(
                source,
                documentation("documented"),
                "CreateOrderRequest"
        );

        assertThat(map(properties(result).get("customerId")))
                .containsEntry("description", "客户编号");
        assertThat(map(properties(map(properties(result).get("address")))
                .get("city")))
                .containsEntry("description", "城市");
        assertThat(map(properties(source).get("customerId")))
                .doesNotContainKey("description");
    }

    @Test
    void rejectsUnknownFieldPath() throws Exception {
        assertThatThrownBy(() -> GatewaySchemaDescriptions.apply(
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "customerId", Map.of("type", "string")
                        )
                ),
                documentation("unknown"),
                "CreateOrderRequest"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missingField")
                .hasMessageContaining("CreateOrderRequest");
    }

    private GatewaySchemaField[] documentation(String methodName)
            throws Exception {
        Method method = Operations.class.getDeclaredMethod(methodName);
        return method.getAnnotation(GatewayOperation.class)
                .requestSchemaFields();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> properties(Map<String, Object> schema) {
        return (Map<String, Object>) schema.get("properties");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    private static final class Operations {

        @GatewayOperation(requestSchemaFields = {
                @GatewaySchemaField(
                        path = "customerId",
                        description = "客户编号"
                ),
                @GatewaySchemaField(
                        path = "address.city",
                        description = "城市"
                )
        })
        void documented() {
        }

        @GatewayOperation(requestSchemaFields = {
                @GatewaySchemaField(
                        path = "missingField",
                        description = "不存在"
                )
        })
        void unknown() {
        }
    }
}
