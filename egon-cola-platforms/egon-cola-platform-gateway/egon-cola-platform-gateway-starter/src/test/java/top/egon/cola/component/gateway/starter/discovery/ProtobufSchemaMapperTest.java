package top.egon.cola.component.gateway.starter.discovery;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Any;
import com.google.protobuf.AnyProto;
import com.google.protobuf.TimestampProto;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.contract.schema.proto.GatewayRequiredOption;
import top.egon.cola.component.gateway.contract.schema.proto.GatewaySchemaFieldOption;
import top.egon.cola.component.gateway.contract.schema.proto.SchemaOptions;
import top.egon.cola.component.gateway.starter.discovery.mapper.GatewayJavaSchemaMapper;
import top.egon.cola.component.gateway.starter.discovery.mapper.ProtobufSchemaMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProtobufSchemaMapperTest {

    private final ProtobufSchemaMapper mapper = new ProtobufSchemaMapper();

    @Test
    void mapsDescriptorTypesOptionsAndLocalReferences() throws Exception {
        Map<String, Object> schema = mapper.schema(requestDescriptor());

        assertThat(schema)
                .containsEntry("type", "object")
                .containsEntry("messageType", "shop.v1.CreateOrderRequest")
                .containsKey("$defs");
        Map<String, Object> properties = map(schema.get("properties"));
        assertThat(map(properties.get("customerId")))
                .containsEntry("type", "string")
                .containsEntry("protobufType", "STRING")
                .containsEntry("protobufName", "customer_id")
                .containsEntry("fieldNumber", 1)
                .containsEntry("description", "客户编号")
                .containsEntry("example", "C-10001");
        assertThat(schema.get("required"))
                .isEqualTo(List.of("customerId", "sku", "extension"));

        Map<String, Object> sku = map(properties.get("sku"));
        assertThat(sku)
                .containsEntry("type", "array")
                .containsEntry("description", "商品 SKU 列表");
        assertThat(map(sku.get("items"))).containsEntry("type", "string");

        Map<String, Object> address = resolve(
                map(properties.get("deliveryAddress")),
                schema
        );
        assertThat(address)
                .containsEntry("type", "object")
                .containsEntry("messageType", "shop.v1.Address");
        assertThat(map(map(address.get("properties")).get("province")))
                .containsEntry("description", "配送省份");

        Map<String, Object> state = map(properties.get("state"));
        assertThat(state)
                .containsEntry("type", "string")
                .containsEntry("protobufType", "ENUM");
        assertThat(state.get("enum"))
                .isEqualTo(List.of("ORDER_STATE_UNSPECIFIED", "CREATED"));

        Map<String, Object> parent = resolve(
                map(properties.get("parent")),
                schema
        );
        assertThat(parent.toString()).contains("#/$defs/");
        assertThat(schema.toString()).doesNotContain("truncated");

        assertThat(map(properties.get("metadata")))
                .containsEntry("type", "object")
                .containsKey("additionalProperties");
        assertThat(map(properties.get("changedAt")))
                .containsEntry("type", "string")
                .containsEntry("format", "date-time");
        assertThat(map(properties.get("extension")))
                .containsEntry("protobufType", "MESSAGE")
                .containsEntry("description", "扩展消息");
        assertThat(schema.toString())
                .contains("contact")
                .contains("oneOf")
                .contains("email")
                .contains("phone");
    }

    @Test
    void mapsRootWellKnownMessage() {
        Map<String, Object> schema = mapper.schema(Any.getDescriptor());

        assertThat(schema)
                .containsEntry(
                        "$schema",
                        GatewayJavaSchemaMapper.JSON_SCHEMA_2020_12
                );
    }

    private Descriptors.Descriptor requestDescriptor() throws Exception {
        DescriptorProtos.DescriptorProto address =
                DescriptorProtos.DescriptorProto.newBuilder()
                        .setName("Address")
                        .addField(field(
                                "province",
                                1,
                                DescriptorProtos.FieldDescriptorProto.Type
                                        .TYPE_STRING,
                                option("配送省份", "", "")
                        ))
                        .addField(field(
                                "city",
                                2,
                                DescriptorProtos.FieldDescriptorProto.Type
                                        .TYPE_STRING,
                                null
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
                        .addNestedType(DescriptorProtos.DescriptorProto
                                .newBuilder()
                                .setName("MetadataEntry")
                                .setOptions(DescriptorProtos
                                        .MessageOptions.newBuilder()
                                        .setMapEntry(true))
                                .addField(field(
                                        "key",
                                        1,
                                        DescriptorProtos.FieldDescriptorProto
                                                .Type.TYPE_STRING,
                                        null
                                ))
                                .addField(DescriptorProtos
                                        .FieldDescriptorProto.newBuilder()
                                        .setName("value")
                                        .setNumber(2)
                                        .setType(DescriptorProtos
                                                .FieldDescriptorProto.Type
                                                .TYPE_MESSAGE)
                                        .setTypeName(".shop.v1.Address")))
                        .addOneofDecl(DescriptorProtos.OneofDescriptorProto
                                .newBuilder()
                                .setName("contact"))
                        .addField(field(
                                "customer_id",
                                1,
                                DescriptorProtos.FieldDescriptorProto.Type
                                        .TYPE_STRING,
                                option("客户编号", "", "C-10001")
                        ))
                        .addField(DescriptorProtos.FieldDescriptorProto
                                .newBuilder()
                                .setName("sku")
                                .setNumber(2)
                                .setType(DescriptorProtos
                                        .FieldDescriptorProto.Type.TYPE_STRING)
                                .setLabel(DescriptorProtos
                                        .FieldDescriptorProto.Label
                                        .LABEL_REPEATED)
                                .setOptions(options(option(
                                        "商品 SKU 列表",
                                        "",
                                        ""
                                ))))
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
                        .addField(DescriptorProtos.FieldDescriptorProto
                                .newBuilder()
                                .setName("parent")
                                .setNumber(5)
                                .setType(DescriptorProtos
                                        .FieldDescriptorProto.Type.TYPE_MESSAGE)
                                .setTypeName(".shop.v1.CreateOrderRequest"))
                        .addField(DescriptorProtos.FieldDescriptorProto
                                .newBuilder()
                                .setName("metadata")
                                .setNumber(6)
                                .setLabel(DescriptorProtos
                                        .FieldDescriptorProto.Label
                                        .LABEL_REPEATED)
                                .setType(DescriptorProtos
                                        .FieldDescriptorProto.Type.TYPE_MESSAGE)
                                .setTypeName(".shop.v1.CreateOrderRequest.MetadataEntry"))
                        .addField(DescriptorProtos.FieldDescriptorProto
                                .newBuilder()
                                .setName("changed_at")
                                .setNumber(7)
                                .setType(DescriptorProtos
                                        .FieldDescriptorProto.Type.TYPE_MESSAGE)
                                .setTypeName(".google.protobuf.Timestamp"))
                        .addField(DescriptorProtos.FieldDescriptorProto
                                .newBuilder()
                                .setName("extension")
                                .setNumber(8)
                                .setType(DescriptorProtos
                                        .FieldDescriptorProto.Type.TYPE_MESSAGE)
                                .setTypeName(".google.protobuf.Any")
                                .setOptions(options(option(
                                        "扩展消息",
                                        "",
                                        ""
                                ))))
                        .addField(DescriptorProtos.FieldDescriptorProto
                                .newBuilder()
                                .setName("email")
                                .setNumber(9)
                                .setType(DescriptorProtos
                                        .FieldDescriptorProto.Type.TYPE_STRING)
                                .setOneofIndex(0))
                        .addField(DescriptorProtos.FieldDescriptorProto
                                .newBuilder()
                                .setName("phone")
                                .setNumber(10)
                                .setType(DescriptorProtos
                                        .FieldDescriptorProto.Type.TYPE_STRING)
                                .setOneofIndex(0))
                        .build();
        DescriptorProtos.FileDescriptorProto file =
                DescriptorProtos.FileDescriptorProto.newBuilder()
                        .setName("order.proto")
                        .setPackage("shop.v1")
                        .setSyntax("proto3")
                        .addDependency("egon/gateway/schema_options.proto")
                        .addDependency("google/protobuf/timestamp.proto")
                        .addDependency("google/protobuf/any.proto")
                        .addMessageType(address)
                        .addMessageType(request)
                        .addEnumType(state)
                        .build();
        return Descriptors.FileDescriptor.buildFrom(
                file,
                new Descriptors.FileDescriptor[]{
                        SchemaOptions.getDescriptor(),
                        TimestampProto.getDescriptor(),
                        AnyProto.getDescriptor()
                }
        ).findMessageTypeByName("CreateOrderRequest");
    }

    private DescriptorProtos.FieldDescriptorProto field(
            String name,
            int number,
            DescriptorProtos.FieldDescriptorProto.Type type,
            GatewaySchemaFieldOption option) {
        DescriptorProtos.FieldDescriptorProto.Builder builder =
                DescriptorProtos.FieldDescriptorProto.newBuilder()
                        .setName(name)
                        .setNumber(number)
                        .setType(type);
        if (option != null) {
            builder.setOptions(options(option));
        }
        return builder.build();
    }

    private DescriptorProtos.FieldOptions options(
            GatewaySchemaFieldOption option) {
        return DescriptorProtos.FieldOptions.newBuilder()
                .setExtension(SchemaOptions.gatewaySchema, option)
                .build();
    }

    private GatewaySchemaFieldOption option(
            String description,
            String format,
            String example) {
        return GatewaySchemaFieldOption.newBuilder()
                .setDescription(description)
                .setFormat(format)
                .setRequired(GatewayRequiredOption.GATEWAY_REQUIRED)
                .setExample(example)
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

    private Map<String, Object> resolve(
            Map<String, Object> value,
            Map<String, Object> root) {
        String reference = String.valueOf(value.get("$ref"));
        return map(map(root.get("$defs")).get(
                reference.substring("#/$defs/".length())
        ));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }
}
