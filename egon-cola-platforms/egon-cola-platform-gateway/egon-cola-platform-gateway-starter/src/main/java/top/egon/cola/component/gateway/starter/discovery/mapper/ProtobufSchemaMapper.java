package top.egon.cola.component.gateway.starter.discovery.mapper;

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

/**
 * Converts Protobuf message descriptors to JSON Schema documents used by
 * Gateway RPC discovery.
 * 中文说明：以 Protobuf 描述符为唯一输入，生成 RPC 发现使用的 JSON Schema 及递归定义。
 */
public final class ProtobufSchemaMapper {

    /** Mapper used to parse JSON examples declared in Protobuf field options. 用于解析字段选项中的 JSON 示例。 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Generates a JSON Schema document for a Protobuf message descriptor.
     * 中文说明：根消息引用会被展开，递归消息则保留在 $defs 中并通过引用复用。
     *
     * @param descriptor root message descriptor
     * @return generated JSON Schema with reusable definitions
     * @throws IllegalArgumentException if descriptor options or map fields are
     *         incompatible with Gateway schema rules
     */
    public Map<String, Object> schema(Descriptors.Descriptor descriptor) {
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

    /** Maintains recursive definitions while mapping one descriptor graph. 维护一次描述符图遍历中的递归定义。 */
    private final class Context {

        /** Definitions indexed by stable Protobuf message keys. 按稳定键索引 Protobuf 消息定义。 */
        private final Map<String, Object> definitions = new LinkedHashMap<>();

        /** Definition keys indexed by fully qualified Protobuf message name. 按消息全限定名索引定义键。 */
        private final Map<String, String> keys = new LinkedHashMap<>();

        /**
         * Maps a message descriptor to a well-known schema or definition
         * reference.
         * 中文说明：先识别 Google well-known 类型，普通消息则创建可递归引用的对象定义。
         *
         * @param descriptor message descriptor to map
         * @return schema fragment or definition reference
         */
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

        /**
         * Maps a Protobuf field, including repetition, map, oneof, and custom
         * option metadata.
         * 中文说明：重复字段生成数组，map 字段生成 additionalProperties，并保留 Protobuf 元数据。
         *
         * @param field field descriptor to map
         * @return field schema
         * @throws IllegalArgumentException if a map key is not a string
         */
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

        /**
         * Maps the scalar or aggregate value type of a Protobuf field.
         * 中文说明：标量、枚举和嵌套消息分别映射为 JSON 基础类型、枚举或定义引用。
         *
         * @param field field descriptor whose value type is mapped
         * @return value schema
         */
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

        /**
         * Resolves whether a field is required by Protobuf or Gateway options.
         * 中文说明：Gateway_OPTIONAL 不能削弱 Protobuf 原生 required 字段的约束。
         *
         * @param field field descriptor to inspect
         * @return {@code true} when the field must be present
         * @throws IllegalArgumentException if an option weakens a Protobuf
         *         required field
         */
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

        /**
         * Applies Gateway description, format, and example options to a field
         * schema.
         * 中文说明：字段选项中的描述、格式和示例会写入生成的 Schema 节点。
         *
         * @param schema field schema to update
         * @param field source field descriptor
         */
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

        /**
         * Reads the Gateway schema extension from a field descriptor.
         * 中文说明：未声明扩展选项时返回 null，避免为普通字段添加额外元数据。
         *
         * @param field field descriptor to inspect
         * @return configured option, or {@code null} when absent
         */
        private GatewaySchemaFieldOption option(
                Descriptors.FieldDescriptor field) {
            return field.getOptions().hasExtension(SchemaOptions.gatewaySchema)
                    ? field.getOptions().getExtension(
                    SchemaOptions.gatewaySchema
            ) : null;
        }

        /**
         * Adds JSON Schema alternatives for each real Protobuf oneof group.
         * 中文说明：每个 oneof 分支通过 required 条件表达互斥选择关系。
         *
         * @param definition message definition to update
         * @param descriptor message descriptor containing oneof groups
         */
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

        /**
         * Creates the stable definition key for a Protobuf message.
         * 中文说明：将全限定消息名中的点替换为下划线，得到可用于 $defs 的稳定键。
         *
         * @param descriptor message descriptor
         * @return definition-safe key derived from the full message name
         */
        private String definitionKey(Descriptors.Descriptor descriptor) {
            return descriptor.getFullName().replace('.', '_');
        }
    }

    /**
     * Maps supported Google well-known message types to their JSON forms.
     * 中文说明：时间、持续时间、包装类型、Struct、ListValue 等按约定 JSON 形态直接映射。
     *
     * @param descriptor descriptor to inspect
     * @return mapped schema, or {@code null} for a regular message
     */
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

    /**
     * Parses and validates a field-option example against its generated schema.
     * 中文说明：示例按生成节点的 type 解析为正确的数字、布尔、数组或对象，并校验枚举值。
     *
     * @param value textual example value
     * @param schema generated field schema
     * @param field source field descriptor
     * @return typed example suitable for JSON Schema output
     * @throws IllegalArgumentException if the value does not match the schema
     */
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

    /**
     * Ensures a parsed structured example has the expected container type.
     * 中文说明：数组示例必须解析为 List，对象示例必须解析为 Map。
     *
     * @param value parsed example value
     * @param expected expected container class
     * @return the validated value
     * @throws IllegalArgumentException if the value has a different type
     */
    private Object requireExampleType(Object value, Class<?> expected) {
        if (!expected.isInstance(value)) {
            throw new IllegalArgumentException();
        }
        return value;
    }

    /**
     * Builds a string enumeration schema from a Protobuf enum descriptor.
     * 中文说明：枚举值使用 Protobuf 符号名称输出，并记录枚举的全限定类型名。
     *
     * @param descriptor enum descriptor
     * @return enumeration schema
     */
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

    /**
     * Builds a closed empty-object schema.
     * 中文说明：空消息禁止附加属性，明确表示没有可传输字段。
     *
     * @return empty-object schema
     */
    private Map<String, Object> object() {
        Map<String, Object> result = type("object");
        result.put("properties", Map.of());
        result.put("additionalProperties", false);
        return result;
    }

    /**
     * Builds a typed schema with a format keyword.
     * 中文说明：在基础 JSON 类型上附加 date-time、整数宽度等格式信息。
     *
     * @param type JSON Schema type
     * @param format JSON Schema format
     * @return formatted schema
     */
    private Map<String, Object> formatted(String type, String format) {
        Map<String, Object> result = type(type);
        result.put("format", format);
        return result;
    }

    /**
     * Builds a schema containing only a JSON Schema type keyword.
     * 中文说明：创建仅包含 type 的最小 Schema 节点，供其他映射方法继续补充。
     *
     * @param type JSON Schema type
     * @return typed schema
     */
    private Map<String, Object> type(String type) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", type);
        return result;
    }

    /**
     * Builds a local definition reference.
     * 中文说明：生成指向当前文档 $defs 的本地引用。
     *
     * @param key definition key
     * @return local reference schema
     */
    private Map<String, Object> reference(String key) {
        return new LinkedHashMap<>(Map.of("$ref", "#/$defs/" + key));
    }

    /**
     * Casts a known schema value to a string-keyed map.
     * 中文说明：调用方已确认值是 Schema 映射，因此这里只做受控类型转换。
     *
     * @param value map value to cast
     * @return cast map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    /**
     * Creates a mutable shallow copy with stringified keys.
     * 中文说明：复制顶层内容并统一键类型，便于后续加入 Protobuf 元数据。
     *
     * @param source map to copy
     * @return mutable string-keyed copy
     */
    private Map<String, Object> copyMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(
                String.valueOf(key),
                value
        ));
        return result;
    }
}
