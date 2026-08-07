package top.egon.cola.component.gateway.starter.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Descriptors;
import top.egon.cola.component.gateway.contract.schema.proto.GatewayRequiredOption;
import top.egon.cola.component.gateway.contract.schema.proto.GatewaySchemaFieldOption;
import top.egon.cola.component.gateway.contract.schema.proto.SchemaOptions;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ProtobufSchemaMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    Map<String, Object> schema(Descriptors.Descriptor descriptor) {
        Context context = new Context();
        Map<String, Object> root = context.message(descriptor);
        Object reference = root.get("$ref");
        Map<String, Object> result;
        if (reference instanceof String value
                && value.startsWith("#/$defs/")) {
            String key = value.substring("#/$defs/".length());
            result = copyMap(map(context.definitions.get(key)));
        } else {
            result = copyMap(root);
        }
        result.put("$schema", GatewayJavaSchemaMapper.JSON_SCHEMA_2020_12);
        if (!context.definitions.isEmpty()) {
            result.put("$defs", context.definitions);
        }
        return result;
    }

    private final class Context {

        private final Map<String, Object> definitions = new LinkedHashMap<>();

        private final Map<String, String> keys = new LinkedHashMap<>();

        private Map<String, Object> message(
                Descriptors.Descriptor descriptor) {
            Map<String, Object> wellKnown = wellKnown(descriptor);
            if (wellKnown != null) {
                return wellKnown;
            }
            String key = keys.computeIfAbsent(
                    descriptor.getFullName(),
                    ignored -> definitionKey(descriptor)
            );
            if (definitions.containsKey(key)) {
                return reference(key);
            }
            Map<String, Object> definition = new LinkedHashMap<>();
            definitions.put(key, definition);
            Map<String, Object> properties = new LinkedHashMap<>();
            List<String> required = new ArrayList<>();
            for (Descriptors.FieldDescriptor field : descriptor.getFields()) {
                properties.put(field.getJsonName(), field(field));
                if (required(field)) {
                    required.add(field.getJsonName());
                }
            }
            definition.put("type", "object");
            definition.put("messageType", descriptor.getFullName());
            definition.put("properties", properties);
            if (!required.isEmpty()) {
                definition.put("required", required);
            }
            definition.put("additionalProperties", false);
            addOneOf(definition, descriptor);
            return reference(key);
        }

        private Map<String, Object> field(
                Descriptors.FieldDescriptor field) {
            Map<String, Object> result;
            if (field.isMapField()) {
                Descriptors.FieldDescriptor keyField = field.getMessageType()
                        .findFieldByName("key");
                if (keyField.getJavaType()
                        != Descriptors.FieldDescriptor.JavaType.STRING) {
                    throw new IllegalArgumentException(
                            "protobuf map key is not a string: "
                                    + field.getFullName()
                    );
                }
                Descriptors.FieldDescriptor valueField = field.getMessageType()
                        .findFieldByName("value");
                result = type("object");
                result.put("additionalProperties", value(valueField));
            } else {
                Map<String, Object> value = value(field);
                if (field.isRepeated()) {
                    result = type("array");
                    result.put("items", value);
                } else {
                    result = copyMap(value);
                }
            }
            result.put("protobufName", field.getName());
            result.put("protobufType", field.getType().name());
            result.put("fieldNumber", field.getNumber());
            if (field.getContainingOneof() != null) {
                result.put(
                        "protobufOneof",
                        field.getContainingOneof().getName()
                );
            }
            applyOption(result, field);
            return result;
        }

        private Map<String, Object> value(
                Descriptors.FieldDescriptor field) {
            return switch (field.getJavaType()) {
                case BOOLEAN -> type("boolean");
                case BYTE_STRING -> formatted("string", "byte");
                case DOUBLE -> formatted("number", "double");
                case FLOAT -> formatted("number", "float");
                case INT -> formatted("integer", switch (field.getType()) {
                    case UINT32, FIXED32 -> "uint32";
                    default -> "int32";
                });
                case LONG -> formatted("integer", switch (field.getType()) {
                    case UINT64, FIXED64 -> "uint64";
                    default -> "int64";
                });
                case ENUM -> enumSchema(field.getEnumType());
                case MESSAGE -> message(field.getMessageType());
                case STRING -> type("string");
            };
        }

        private boolean required(Descriptors.FieldDescriptor field) {
            GatewaySchemaFieldOption option = option(field);
            if (option != null
                    && option.getRequired()
                    == GatewayRequiredOption.GATEWAY_OPTIONAL
                    && field.isRequired()) {
                throw new IllegalArgumentException(
                        "protobuf Gateway Option cannot weaken required field: "
                                + field.getFullName()
                );
            }
            return field.isRequired() || option != null
                    && option.getRequired()
                    == GatewayRequiredOption.GATEWAY_REQUIRED;
        }

        private void applyOption(
                Map<String, Object> schema,
                Descriptors.FieldDescriptor field) {
            GatewaySchemaFieldOption option = option(field);
            if (option == null) {
                return;
            }
            if (!option.getDescription().isBlank()) {
                schema.put("description", option.getDescription().trim());
            }
            if (!option.getFormat().isBlank()) {
                schema.put("format", option.getFormat().trim());
            }
            if (!option.getExample().isBlank()) {
                schema.put(
                        "example",
                        parseExample(option.getExample(), schema, field)
                );
            }
        }

        private GatewaySchemaFieldOption option(
                Descriptors.FieldDescriptor field) {
            return field.getOptions().hasExtension(SchemaOptions.gatewaySchema)
                    ? field.getOptions().getExtension(
                    SchemaOptions.gatewaySchema
            ) : null;
        }

        private void addOneOf(
                Map<String, Object> definition,
                Descriptors.Descriptor descriptor) {
            List<Map<String, Object>> groups = new ArrayList<>();
            for (Descriptors.OneofDescriptor oneof
                    : descriptor.getRealOneofs()) {
                List<Map<String, Object>> branches = oneof.getFields().stream()
                        .map(field -> Map.<String, Object>of(
                                "required",
                                List.of(field.getJsonName())
                        ))
                        .toList();
                Map<String, Object> group = new LinkedHashMap<>();
                group.put("oneOf", branches);
                group.put("x-protobuf-oneof", oneof.getName());
                groups.add(group);
            }
            if (!groups.isEmpty()) {
                definition.put("allOf", groups);
            }
        }

        private String definitionKey(Descriptors.Descriptor descriptor) {
            return descriptor.getFullName().replace('.', '_');
        }
    }

    private Map<String, Object> wellKnown(
            Descriptors.Descriptor descriptor) {
        return switch (descriptor.getFullName()) {
            case "google.protobuf.Timestamp" -> formatted(
                    "string",
                    "date-time"
            );
            case "google.protobuf.Duration" -> formatted(
                    "string",
                    "duration"
            );
            case "google.protobuf.FieldMask" -> type("string");
            case "google.protobuf.Empty" -> object();
            case "google.protobuf.Struct" -> {
                Map<String, Object> result = type("object");
                result.put("additionalProperties", Map.of());
                yield result;
            }
            case "google.protobuf.ListValue" -> {
                Map<String, Object> result = type("array");
                result.put("items", Map.of());
                yield result;
            }
            case "google.protobuf.Value", "google.protobuf.Any" ->
                    new LinkedHashMap<>();
            case "google.protobuf.StringValue" -> type("string");
            case "google.protobuf.BoolValue" -> type("boolean");
            case "google.protobuf.Int32Value",
                 "google.protobuf.UInt32Value" -> formatted(
                    "integer",
                    "int32"
            );
            case "google.protobuf.Int64Value",
                 "google.protobuf.UInt64Value" -> formatted(
                    "integer",
                    "int64"
            );
            case "google.protobuf.FloatValue" -> formatted(
                    "number",
                    "float"
            );
            case "google.protobuf.DoubleValue" -> formatted(
                    "number",
                    "double"
            );
            case "google.protobuf.BytesValue" -> formatted("string", "byte");
            default -> null;
        };
    }

    private Object parseExample(
            String value,
            Map<String, Object> schema,
            Descriptors.FieldDescriptor field) {
        try {
            String type = String.valueOf(schema.get("type"));
            return switch (type) {
                case "string" -> {
                    Object values = schema.get("enum");
                    if (values instanceof Collection<?> collection
                            && !collection.contains(value)) {
                        throw new IllegalArgumentException();
                    }
                    yield value;
                }
                case "integer" -> new BigInteger(value);
                case "number" -> new BigDecimal(value);
                case "boolean" -> {
                    if (!"true".equals(value) && !"false".equals(value)) {
                        throw new IllegalArgumentException();
                    }
                    yield Boolean.valueOf(value);
                }
                case "array" -> requireExampleType(
                        objectMapper.readValue(value, Object.class),
                        List.class
                );
                case "object" -> requireExampleType(
                        objectMapper.readValue(value, Object.class),
                        Map.class
                );
                default -> throw new IllegalArgumentException();
            };
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "invalid protobuf Gateway Option example for "
                            + field.getFullName() + ": " + value,
                    exception
            );
        }
    }

    private Object requireExampleType(Object value, Class<?> expected) {
        if (!expected.isInstance(value)) {
            throw new IllegalArgumentException();
        }
        return value;
    }

    private Map<String, Object> enumSchema(
            Descriptors.EnumDescriptor descriptor) {
        Map<String, Object> result = type("string");
        result.put(
                "enum",
                descriptor.getValues().stream()
                        .map(Descriptors.EnumValueDescriptor::getName)
                        .toList()
        );
        result.put("enumType", descriptor.getFullName());
        return result;
    }

    private Map<String, Object> object() {
        Map<String, Object> result = type("object");
        result.put("properties", Map.of());
        result.put("additionalProperties", false);
        return result;
    }

    private Map<String, Object> formatted(String type, String format) {
        Map<String, Object> result = type(type);
        result.put("format", format);
        return result;
    }

    private Map<String, Object> type(String type) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", type);
        return result;
    }

    private Map<String, Object> reference(String key) {
        return new LinkedHashMap<>(Map.of("$ref", "#/$defs/" + key));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    private Map<String, Object> copyMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(
                String.valueOf(key),
                value
        ));
        return result;
    }
}
