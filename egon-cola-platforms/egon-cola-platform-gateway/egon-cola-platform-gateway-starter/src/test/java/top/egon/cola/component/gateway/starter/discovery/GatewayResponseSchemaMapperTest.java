package top.egon.cola.component.gateway.starter.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.pojo.PageResultRecord;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.component.gateway.starter.annotation.GatewayResponseSchema;
import top.egon.cola.component.gateway.starter.annotation.GatewaySchemaShape;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayResponseSchemaMapperTest {

    private final GatewayResponseSchemaMapper mapper =
            new GatewayResponseSchemaMapper(new ObjectMapper());

    @Test
    void mapsAllResultRecordPayloadShapesAndNullability() throws Exception {
        assertResultPayload("objectResult", "object");
        assertResultPayload("listResult", "array");
        assertResultPayload("mapResult", "object");
        assertResultPayload("valueResult", "integer");
    }

    @Test
    void mapsCompletePageResultWithNonNullRecordsAndPage() throws Exception {
        Map<String, Object> schema = schema("pageResult");
        Map<String, Object> properties = map(schema.get("properties"));

        assertThat(schema.get("required"))
                .isEqualTo(List.copyOf(properties.keySet()));
        assertThat(map(properties.get("records")))
                .containsEntry("type", "array")
                .doesNotContainKey("anyOf");
        assertThat(map(properties.get("page")))
                .containsKey("$ref")
                .doesNotContainKey("anyOf");
        assertThat(schema.toString())
                .contains("pageNo")
                .contains("pageSize")
                .contains("hasNext");
    }

    private void assertResultPayload(String methodName, String expectedType)
            throws Exception {
        Map<String, Object> schema = schema(methodName);
        Map<String, Object> data = map(map(schema.get("properties")).get("data"));
        List<?> branches = (List<?>) data.get("anyOf");

        assertThat(schema.get("required"))
                .isEqualTo(List.copyOf(map(schema.get("properties")).keySet()));
        assertThat(resolve(map(branches.getFirst()), schema))
                .containsEntry("type", expectedType);
        assertThat(map(branches.get(1))).containsEntry("type", "null");
    }

    private Map<String, Object> resolve(
            Map<String, Object> value,
            Map<String, Object> root) {
        Object reference = value.get("$ref");
        if (!(reference instanceof String path)) {
            return value;
        }
        return map(map(root.get("$defs")).get(
                path.substring("#/$defs/".length())
        ));
    }

    private Map<String, Object> schema(String methodName) throws Exception {
        Method method = Fixtures.class.getDeclaredMethod(methodName);
        return mapper.schema(
                method,
                method.getAnnotation(GatewayOperation.class)
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    private static final class Fixtures {

        @GatewayOperation(
                registerMcp = true,
                responseSchema = @GatewayResponseSchema(
                        wrapper = ResultRecord.class,
                        payloadField = "data",
                        schema = Payload.class,
                        shape = GatewaySchemaShape.OBJECT
                )
        )
        static ResultRecord<Payload> objectResult() {
            return null;
        }

        @GatewayOperation(
                registerMcp = true,
                responseSchema = @GatewayResponseSchema(
                        wrapper = ResultRecord.class,
                        payloadField = "data",
                        schema = Payload.class,
                        shape = GatewaySchemaShape.LIST
                )
        )
        static ResultRecord<List<Payload>> listResult() {
            return null;
        }

        @GatewayOperation(
                registerMcp = true,
                responseSchema = @GatewayResponseSchema(
                        wrapper = ResultRecord.class,
                        payloadField = "data",
                        schema = Payload.class,
                        shape = GatewaySchemaShape.MAP
                )
        )
        static ResultRecord<Map<String, Payload>> mapResult() {
            return null;
        }

        @GatewayOperation(
                registerMcp = true,
                responseSchema = @GatewayResponseSchema(
                        wrapper = ResultRecord.class,
                        payloadField = "data",
                        schema = Long.class,
                        shape = GatewaySchemaShape.VALUE
                )
        )
        static ResultRecord<Long> valueResult() {
            return null;
        }

        @GatewayOperation(
                registerMcp = true,
                responseSchema = @GatewayResponseSchema(
                        wrapper = PageResultRecord.class,
                        payloadField = "records",
                        schema = Payload.class,
                        shape = GatewaySchemaShape.LIST
                )
        )
        static PageResultRecord<Payload> pageResult() {
            return null;
        }
    }

    private record Payload(String id) {
    }
}
