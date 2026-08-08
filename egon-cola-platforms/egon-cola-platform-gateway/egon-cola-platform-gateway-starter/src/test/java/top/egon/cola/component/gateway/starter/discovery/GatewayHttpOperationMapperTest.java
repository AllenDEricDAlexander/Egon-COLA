package top.egon.cola.component.gateway.starter.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;
import reactor.core.publisher.Flux;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReport;
import top.egon.cola.component.gateway.starter.GatewayReportingProperties;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.component.gateway.starter.annotation.GatewayRequestLocation;
import top.egon.cola.component.gateway.starter.annotation.GatewayRequestSchemaField;
import top.egon.cola.component.gateway.starter.annotation.GatewayResponseSchema;
import top.egon.cola.component.gateway.starter.annotation.GatewaySchemaField;
import top.egon.cola.component.gateway.starter.annotation.GatewaySchemaShape;
import top.egon.cola.component.gateway.starter.discovery.http.GatewayHttpOperationMapper;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayHttpOperationMapperTest {

    @Test
    void groupsPathQueryHeaderAndRequestBodyWithoutFlattening()
            throws Exception {
        GatewayInterfaceDefinitionReport.Operation operation = operation(
                Contract.class.getDeclaredMethod(
                        "update",
                        String.class,
                        boolean.class,
                        String.class,
                        Command.class
                ),
                "/orders/{orderId}"
        );

        Map<String, Object> request = operation.requestSchema();
        assertThat(request)
                .containsEntry("x-egon-schema-model", "gateway-operation-request/v2")
                .containsEntry("type", "object");
        assertThat(map(request.get("properties")))
                .containsOnlyKeys("path", "query", "header", "body");
        assertThat(operation.responseSchema())
                .containsEntry("x-egon-schema-model", "gateway-operation-response/v2")
                .containsEntry("type", "object");
    }

    @Test
    void expandsComplexModelAttributeIntoQueryProperties() throws Exception {
        GatewayInterfaceDefinitionReport.Operation operation = operation(
                Contract.class.getDeclaredMethod("search", Query.class),
                "/orders"
        );

        Map<String, Object> query = map(map(operation.requestSchema()
                .get("properties")).get("query"));
        assertThat(map(query.get("properties")))
                .containsKeys("customerId", "statuses");
    }

    @Test
    void rejectsIncompleteManagedDeclarations() throws Exception {
        assertThatThrownBy(() -> operation(
                Contract.class.getDeclaredMethod(
                        "incomplete",
                        String.class,
                        Command.class
                ),
                "/orders/{orderId}"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requestSchemaFields")
                .hasMessageContaining("BODY");
    }

    @Test
    void mapsBodyListMapAndValueShapes() throws Exception {
        assertThat(bodySchema("listBody", List.class))
                .containsEntry("type", "array")
                .containsKey("items");
        assertThat(bodySchema("mapBody", Map.class))
                .containsEntry("type", "object")
                .containsKey("additionalProperties");
        assertThat(bodySchema("valueBody", String.class))
                .containsEntry("type", "string");
    }

    @Test
    void rejectsDuplicateWrongAndUnknownDeclarations() throws Exception {
        assertThatThrownBy(() -> operation(
                Contract.class.getDeclaredMethod("duplicateBody", Command.class),
                "/orders"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate requestSchemaFields");
        assertThatThrownBy(() -> operation(
                Contract.class.getDeclaredMethod("wrongName", String.class),
                "/orders"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing QUERY id");
        assertThatThrownBy(() -> operation(
                Contract.class.getDeclaredMethod("wrongShape", String.class),
                "/orders"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("shape mismatch");
        assertThatThrownBy(() -> operation(
                Contract.class.getDeclaredMethod("unknownDeclaration"),
                "/orders"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("declares unknown QUERY ghost");
    }

    @Test
    void rejectsInvalidBodyAndExpandedQueryComposition() throws Exception {
        assertThatThrownBy(() -> operation(
                Contract.class.getDeclaredMethod(
                        "twoBodies",
                        Command.class,
                        Command.class
                ),
                "/orders"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("multiple request bodies");
        assertThatThrownBy(() -> operation(
                Contract.class.getDeclaredMethod("modelAsBody", Query.class),
                "/orders"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing QUERY");
        assertThatThrownBy(() -> operation(
                Contract.class.getDeclaredMethod(
                        "queryCollision",
                        FirstQuery.class,
                        SecondQuery.class
                ),
                "/orders"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expanded query property collision")
                .hasMessageContaining("value");
    }

    @Test
    void rejectsManagedBusinessHeadersCookiesPartsAndStreaming()
            throws Exception {
        assertThatThrownBy(() -> operation(
                Contract.class.getDeclaredMethod("businessHeader", String.class),
                "/orders"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required HEADER parameter")
                .hasMessageContaining("X-Tenant");
        assertThatThrownBy(() -> operation(
                Contract.class.getDeclaredMethod("businessCookie", String.class),
                "/orders"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required COOKIE parameter")
                .hasMessageContaining("SESSION");
        assertThatThrownBy(() -> operation(
                Contract.class.getDeclaredMethod("part", String.class),
                "/orders"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("multipart operations are unsupported");
        assertThatThrownBy(() -> operation(
                Contract.class.getDeclaredMethod("streaming"),
                "/orders"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("streaming operations are unsupported");
    }

    private Map<String, Object> bodySchema(
            String methodName,
            Class<?> parameterType) throws Exception {
        GatewayInterfaceDefinitionReport.Operation operation = operation(
                Contract.class.getDeclaredMethod(methodName, parameterType),
                "/orders"
        );
        return map(map(operation.requestSchema().get("properties")).get("body"));
    }

    private GatewayInterfaceDefinitionReport.Operation operation(
            Method method,
            String path) {
        GatewayReportingProperties properties = new GatewayReportingProperties();
        properties.setBizCode("commerce");
        properties.setApplicationCode("orders");
        properties.setEnv("test");
        properties.setNamespace("default");
        properties.setArtifactVersion("1.0.0");
        GatewayHttpOperationMapper mapper = new GatewayHttpOperationMapper(
                properties,
                new ObjectMapper()
        );
        return mapper.group(
                Contract.class,
                List.of(new GatewayHttpOperationMapper.Mapping(
                        new HandlerMethod(new Contract(), method),
                        Set.of(path),
                        Set.of("PUT"),
                        Set.of("application/json"),
                        Set.of("application/json")
                ))
        ).interfaceGroup().operations().getFirst();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    @GatewayInterfaceGroup(
            businessDomainCode = "trade",
            businessDomainName = "交易域",
            entityDomainCode = "order",
            entityDomainName = "订单",
            code = "orders",
            name = "订单",
            mcpServerCode = "trade-mcp"
    )
    private static final class Contract {

        @PutMapping
        @GatewayOperation(
                registerMcp = true,
                mcpName = "order_update",
                requestSchemaFields = {
                        @GatewayRequestSchemaField(
                                location = GatewayRequestLocation.PATH,
                                name = "orderId",
                                schema = String.class,
                                shape = GatewaySchemaShape.VALUE
                        ),
                        @GatewayRequestSchemaField(
                                location = GatewayRequestLocation.QUERY,
                                name = "notify",
                                schema = Boolean.class,
                                shape = GatewaySchemaShape.VALUE
                        ),
                        @GatewayRequestSchemaField(
                                location = GatewayRequestLocation.HEADER,
                                name = "Authorization",
                                schema = String.class,
                                shape = GatewaySchemaShape.VALUE
                        ),
                        @GatewayRequestSchemaField(
                                location = GatewayRequestLocation.BODY,
                                schema = Command.class,
                                shape = GatewaySchemaShape.OBJECT
                        )
                },
                responseSchema = @GatewayResponseSchema(
                        schema = View.class,
                        shape = GatewaySchemaShape.OBJECT
                )
        )
        View update(
                @PathVariable
                @GatewaySchemaField(description = "订单 ID") String orderId,
                @RequestParam(name = "notify", defaultValue = "false")
                boolean notify,
                @RequestHeader("Authorization") String authorization,
                @RequestBody Command command) {
            return null;
        }

        @PutMapping
        @GatewayOperation(
                registerMcp = true,
                mcpName = "order_search",
                requestSchemaFields = @GatewayRequestSchemaField(
                        location = GatewayRequestLocation.QUERY,
                        schema = Query.class,
                        shape = GatewaySchemaShape.OBJECT,
                        expanded = true
                ),
                responseSchema = @GatewayResponseSchema(
                        schema = View.class,
                        shape = GatewaySchemaShape.LIST
                )
        )
        List<View> search(@ModelAttribute Query query) {
            return List.of();
        }

        @PutMapping
        @GatewayOperation(
                registerMcp = true,
                mcpName = "order_incomplete",
                requestSchemaFields = @GatewayRequestSchemaField(
                        location = GatewayRequestLocation.PATH,
                        name = "orderId",
                        schema = String.class,
                        shape = GatewaySchemaShape.VALUE
                ),
                responseSchema = @GatewayResponseSchema(
                        schema = View.class,
                        shape = GatewaySchemaShape.OBJECT
                )
        )
        View incomplete(
                @PathVariable("orderId") String orderId,
                @RequestBody Command command) {
            return null;
        }

        @PutMapping
        @GatewayOperation(
                registerMcp = true,
                mcpName = "order_list_body",
                requestSchemaFields = @GatewayRequestSchemaField(
                        location = GatewayRequestLocation.BODY,
                        schema = Command.class,
                        shape = GatewaySchemaShape.LIST
                ),
                responseSchema = @GatewayResponseSchema(
                        schema = View.class,
                        shape = GatewaySchemaShape.OBJECT
                )
        )
        View listBody(@RequestBody List<Command> body) {
            return null;
        }

        @PutMapping
        @GatewayOperation(
                registerMcp = true,
                mcpName = "order_map_body",
                requestSchemaFields = @GatewayRequestSchemaField(
                        location = GatewayRequestLocation.BODY,
                        schema = Command.class,
                        shape = GatewaySchemaShape.MAP
                ),
                responseSchema = @GatewayResponseSchema(
                        schema = View.class,
                        shape = GatewaySchemaShape.OBJECT
                )
        )
        View mapBody(@RequestBody Map<String, Command> body) {
            return null;
        }

        @PutMapping
        @GatewayOperation(
                registerMcp = true,
                mcpName = "order_value_body",
                requestSchemaFields = @GatewayRequestSchemaField(
                        location = GatewayRequestLocation.BODY,
                        schema = String.class,
                        shape = GatewaySchemaShape.VALUE
                ),
                responseSchema = @GatewayResponseSchema(
                        schema = View.class,
                        shape = GatewaySchemaShape.OBJECT
                )
        )
        View valueBody(@RequestBody String body) {
            return null;
        }

        @PutMapping
        @GatewayOperation(
                registerMcp = true,
                mcpName = "duplicate_body",
                requestSchemaFields = {
                        @GatewayRequestSchemaField(
                                location = GatewayRequestLocation.BODY,
                                schema = Command.class,
                                shape = GatewaySchemaShape.OBJECT
                        ),
                        @GatewayRequestSchemaField(
                                location = GatewayRequestLocation.BODY,
                                schema = Command.class,
                                shape = GatewaySchemaShape.OBJECT
                        )
                },
                responseSchema = @GatewayResponseSchema(
                        schema = View.class,
                        shape = GatewaySchemaShape.OBJECT
                )
        )
        View duplicateBody(@RequestBody Command body) {
            return null;
        }

        @PutMapping
        @GatewayOperation(
                registerMcp = true,
                mcpName = "wrong_name",
                requestSchemaFields = @GatewayRequestSchemaField(
                        location = GatewayRequestLocation.QUERY,
                        name = "other",
                        schema = String.class,
                        shape = GatewaySchemaShape.VALUE
                ),
                responseSchema = @GatewayResponseSchema(
                        schema = View.class,
                        shape = GatewaySchemaShape.OBJECT
                )
        )
        View wrongName(@RequestParam("id") String id) {
            return null;
        }

        @PutMapping
        @GatewayOperation(
                registerMcp = true,
                mcpName = "wrong_shape",
                requestSchemaFields = @GatewayRequestSchemaField(
                        location = GatewayRequestLocation.QUERY,
                        name = "id",
                        schema = String.class,
                        shape = GatewaySchemaShape.OBJECT
                ),
                responseSchema = @GatewayResponseSchema(
                        schema = View.class,
                        shape = GatewaySchemaShape.OBJECT
                )
        )
        View wrongShape(@RequestParam("id") String id) {
            return null;
        }

        @PutMapping
        @GatewayOperation(
                registerMcp = true,
                mcpName = "unknown_declaration",
                requestSchemaFields = @GatewayRequestSchemaField(
                        location = GatewayRequestLocation.QUERY,
                        name = "ghost",
                        schema = String.class,
                        shape = GatewaySchemaShape.VALUE
                ),
                responseSchema = @GatewayResponseSchema(
                        schema = View.class,
                        shape = GatewaySchemaShape.OBJECT
                )
        )
        View unknownDeclaration() {
            return null;
        }

        @PutMapping
        @GatewayOperation(
                registerMcp = true,
                mcpName = "two_bodies",
                requestSchemaFields = @GatewayRequestSchemaField(
                        location = GatewayRequestLocation.BODY,
                        schema = Command.class,
                        shape = GatewaySchemaShape.OBJECT
                ),
                responseSchema = @GatewayResponseSchema(
                        schema = View.class,
                        shape = GatewaySchemaShape.OBJECT
                )
        )
        View twoBodies(
                @RequestBody Command first,
                @RequestBody Command second) {
            return null;
        }

        @PutMapping
        @GatewayOperation(
                registerMcp = true,
                mcpName = "model_as_body",
                requestSchemaFields = @GatewayRequestSchemaField(
                        location = GatewayRequestLocation.BODY,
                        schema = Query.class,
                        shape = GatewaySchemaShape.OBJECT
                ),
                responseSchema = @GatewayResponseSchema(
                        schema = View.class,
                        shape = GatewaySchemaShape.OBJECT
                )
        )
        View modelAsBody(@ModelAttribute Query query) {
            return null;
        }

        @PutMapping
        @GatewayOperation(
                registerMcp = true,
                mcpName = "query_collision",
                requestSchemaFields = {
                        @GatewayRequestSchemaField(
                                location = GatewayRequestLocation.QUERY,
                                schema = FirstQuery.class,
                                shape = GatewaySchemaShape.OBJECT,
                                expanded = true
                        ),
                        @GatewayRequestSchemaField(
                                location = GatewayRequestLocation.QUERY,
                                schema = SecondQuery.class,
                                shape = GatewaySchemaShape.OBJECT,
                                expanded = true
                        )
                },
                responseSchema = @GatewayResponseSchema(
                        schema = View.class,
                        shape = GatewaySchemaShape.OBJECT
                )
        )
        View queryCollision(
                @ModelAttribute FirstQuery first,
                @ModelAttribute SecondQuery second) {
            return null;
        }

        @PutMapping
        @GatewayOperation(
                registerMcp = true,
                mcpName = "business_header",
                requestSchemaFields = @GatewayRequestSchemaField(
                        location = GatewayRequestLocation.HEADER,
                        name = "X-Tenant",
                        schema = String.class,
                        shape = GatewaySchemaShape.VALUE
                ),
                responseSchema = @GatewayResponseSchema(
                        schema = View.class,
                        shape = GatewaySchemaShape.OBJECT
                )
        )
        View businessHeader(@RequestHeader("X-Tenant") String tenant) {
            return null;
        }

        @PutMapping
        @GatewayOperation(
                registerMcp = true,
                mcpName = "business_cookie",
                requestSchemaFields = @GatewayRequestSchemaField(
                        location = GatewayRequestLocation.COOKIE,
                        name = "SESSION",
                        schema = String.class,
                        shape = GatewaySchemaShape.VALUE
                ),
                responseSchema = @GatewayResponseSchema(
                        schema = View.class,
                        shape = GatewaySchemaShape.OBJECT
                )
        )
        View businessCookie(@CookieValue("SESSION") String session) {
            return null;
        }

        @PutMapping
        @GatewayOperation(
                registerMcp = true,
                mcpName = "part",
                requestSchemaFields = @GatewayRequestSchemaField(
                        location = GatewayRequestLocation.PART,
                        name = "file",
                        schema = String.class,
                        shape = GatewaySchemaShape.VALUE
                ),
                responseSchema = @GatewayResponseSchema(
                        schema = View.class,
                        shape = GatewaySchemaShape.OBJECT
                )
        )
        View part(@RequestPart("file") String file) {
            return null;
        }

        @PutMapping(produces = "text/event-stream")
        @GatewayOperation(
                registerMcp = true,
                mcpName = "streaming",
                responseSchema = @GatewayResponseSchema(
                        schema = View.class,
                        shape = GatewaySchemaShape.LIST
                )
        )
        Flux<View> streaming() {
            return Flux.empty();
        }
    }

    private record Command(String reason, List<String> lines) {
    }

    private record Query(String customerId, List<String> statuses) {
    }

    private record FirstQuery(String value) {
    }

    private record SecondQuery(String value) {
    }

    private record View(String id, Map<String, String> attributes) {
    }
}
