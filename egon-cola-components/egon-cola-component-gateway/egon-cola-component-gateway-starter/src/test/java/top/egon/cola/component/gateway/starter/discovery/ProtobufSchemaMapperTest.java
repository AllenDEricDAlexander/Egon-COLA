package top.egon.cola.component.gateway.starter.discovery;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.component.gateway.starter.annotation.GatewaySchemaField;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProtobufSchemaMapperTest {

    private final ProtobufSchemaMapper mapper = new ProtobufSchemaMapper();

    @Test
    void expandsNestedRepeatedAndEnumFieldsWithDocumentation()
            throws Exception {
        GatewayOperation operation = operation("documented");

        Map<String, Object> schema = mapper.schema(
                requestDescriptor(),
                operation.requestSchemaFields()
        );

        assertThat(schema)
                .containsEntry("type", "object")
                .containsEntry("messageType", "shop.v1.CreateOrderRequest");
        Map<String, Object> properties = map(schema.get("properties"));
        assertThat(map(properties.get("customerId")))
                .containsEntry("type", "string")
                .containsEntry("protobufType", "STRING")
                .containsEntry("protobufName", "customer_id")
                .containsEntry("fieldNumber", 1)
                .containsEntry("description", "客户编号");

        Map<String, Object> sku = map(properties.get("sku"));
        assertThat(sku)
                .containsEntry("type", "array")
                .containsEntry("description", "商品 SKU 列表");
        assertThat(map(sku.get("items")))
                .containsEntry("type", "string")
                .containsEntry("protobufType", "STRING");

        Map<String, Object> deliveryAddress = map(
                properties.get("deliveryAddress")
        );
        assertThat(deliveryAddress)
                .containsEntry("type", "object")
                .containsEntry("messageType", "shop.v1.Address")
                .containsEntry("description", "配送地址");
        assertThat(map(map(deliveryAddress.get("properties"))
                .get("province")))
                .containsEntry("type", "string")
                .containsEntry("description", "配送省份");

        Map<String, Object> state = map(properties.get("state"));
        assertThat(state)
                .containsEntry("type", "string")
                .containsEntry("protobufType", "ENUM")
                .containsEntry("description", "订单状态");
        assertThat(state.get("enum"))
                .isEqualTo(List.of("ORDER_STATE_UNSPECIFIED", "CREATED"));
    }

    @Test
    void rejectsDocumentationForAnUnknownFieldPath() throws Exception {
        GatewayOperation operation = operation("unknown");

        assertThatThrownBy(() -> mapper.schema(
                requestDescriptor(),
                operation.requestSchemaFields()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missingField")
                .hasMessageContaining("CreateOrderRequest");
    }

    private GatewayOperation operation(String name) throws Exception {
        Method method = DocumentedOperations.class.getDeclaredMethod(name);
        return method.getAnnotation(GatewayOperation.class);
    }

    private Descriptors.Descriptor requestDescriptor() throws Exception {
        DescriptorProtos.DescriptorProto address =
                DescriptorProtos.DescriptorProto.newBuilder()
                        .setName("Address")
                        .addField(field(
                                "province",
                                1,
                                DescriptorProtos.FieldDescriptorProto.Type
                                        .TYPE_STRING
                        ))
                        .addField(field(
                                "city",
                                2,
                                DescriptorProtos.FieldDescriptorProto.Type
                                        .TYPE_STRING
                        ))
                        .build();
        DescriptorProtos.EnumDescriptorProto state =
                DescriptorProtos.EnumDescriptorProto.newBuilder()
                        .setName("OrderState")
                        .addValue(enumValue("ORDER_STATE_UNSPECIFIED", 0))
                        .addValue(enumValue("CREATED", 1))
                        .build();
        DescriptorProtos.DescriptorProto request =
                DescriptorProtos.DescriptorProto.newBuilder()
                        .setName("CreateOrderRequest")
                        .addField(field(
                                "customer_id",
                                1,
                                DescriptorProtos.FieldDescriptorProto.Type
                                        .TYPE_STRING
                        ))
                        .addField(DescriptorProtos.FieldDescriptorProto
                                .newBuilder()
                                .setName("sku")
                                .setNumber(2)
                                .setType(DescriptorProtos
                                        .FieldDescriptorProto.Type.TYPE_STRING)
                                .setLabel(DescriptorProtos
                                        .FieldDescriptorProto.Label
                                        .LABEL_REPEATED))
                        .addField(DescriptorProtos.FieldDescriptorProto
                                .newBuilder()
                                .setName("delivery_address")
                                .setNumber(3)
                                .setType(DescriptorProtos
                                        .FieldDescriptorProto.Type.TYPE_MESSAGE)
                                .setTypeName(".shop.v1.Address"))
                        .addField(DescriptorProtos.FieldDescriptorProto
                                .newBuilder()
                                .setName("state")
                                .setNumber(4)
                                .setType(DescriptorProtos
                                        .FieldDescriptorProto.Type.TYPE_ENUM)
                                .setTypeName(".shop.v1.OrderState"))
                        .build();
        DescriptorProtos.FileDescriptorProto file =
                DescriptorProtos.FileDescriptorProto.newBuilder()
                        .setName("order.proto")
                        .setPackage("shop.v1")
                        .setSyntax("proto3")
                        .addMessageType(address)
                        .addMessageType(request)
                        .addEnumType(state)
                        .build();
        return Descriptors.FileDescriptor.buildFrom(
                file,
                new Descriptors.FileDescriptor[0]
        ).findMessageTypeByName("CreateOrderRequest");
    }

    private DescriptorProtos.FieldDescriptorProto field(
            String name,
            int number,
            DescriptorProtos.FieldDescriptorProto.Type type) {
        return DescriptorProtos.FieldDescriptorProto.newBuilder()
                .setName(name)
                .setNumber(number)
                .setType(type)
                .build();
    }

    private DescriptorProtos.EnumValueDescriptorProto enumValue(
            String name,
            int number) {
        return DescriptorProtos.EnumValueDescriptorProto.newBuilder()
                .setName(name)
                .setNumber(number)
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    private static final class DocumentedOperations {

        @GatewayOperation(requestSchemaFields = {
                @GatewaySchemaField(
                                path = "customerId",
                                description = "客户编号"
                        ),
                @GatewaySchemaField(
                                path = "sku",
                                description = "商品 SKU 列表"
                        ),
                @GatewaySchemaField(
                                path = "deliveryAddress",
                                description = "配送地址"
                        ),
                @GatewaySchemaField(
                                path = "deliveryAddress.province",
                                description = "配送省份"
                        ),
                @GatewaySchemaField(
                                path = "state",
                                description = "订单状态"
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
