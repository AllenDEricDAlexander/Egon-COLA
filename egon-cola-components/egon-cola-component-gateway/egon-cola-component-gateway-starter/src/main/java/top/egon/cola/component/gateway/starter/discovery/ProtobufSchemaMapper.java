package top.egon.cola.component.gateway.starter.discovery;

import com.google.protobuf.Descriptors;
import top.egon.cola.component.gateway.starter.annotation.GatewaySchemaField;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ProtobufSchemaMapper {

    private static final int MAX_DEPTH = 6;

    Map<String, Object> schema(
            Descriptors.Descriptor descriptor,
            GatewaySchemaField[] documentation) {
        DocumentationIndex descriptions = new DocumentationIndex(
                documentation
        );
        Map<String, Object> schema = messageSchema(
                descriptor,
                "",
                0,
                new LinkedHashSet<>(),
                descriptions
        );
        descriptions.verifyAllFieldsExist(descriptor.getFullName());
        return schema;
    }

    private Map<String, Object> messageSchema(
            Descriptors.Descriptor descriptor,
            String path,
            int depth,
            Set<String> ancestors,
            DocumentationIndex descriptions) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "object");
        result.put("messageType", descriptor.getFullName());
        if (depth > MAX_DEPTH
                || ancestors.contains(descriptor.getFullName())) {
            result.put("$ref", descriptor.getFullName());
            result.put("truncated", true);
            return result;
        }

        Set<String> nestedAncestors = new LinkedHashSet<>(ancestors);
        nestedAncestors.add(descriptor.getFullName());
        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        for (Descriptors.FieldDescriptor field : descriptor.getFields()) {
            String fieldPath = childPath(path, field.getJsonName());
            properties.put(
                    field.getJsonName(),
                    fieldSchema(
                            field,
                            fieldPath,
                            depth,
                            nestedAncestors,
                            descriptions
                    )
            );
            if (field.isRequired()) {
                required.add(field.getJsonName());
            }
        }
        result.put("properties", properties);
        if (!required.isEmpty()) {
            result.put("required", required);
        }
        return result;
    }

    private Map<String, Object> fieldSchema(
            Descriptors.FieldDescriptor field,
            String path,
            int depth,
            Set<String> ancestors,
            DocumentationIndex descriptions) {
        if (field.isMapField()) {
            Descriptors.FieldDescriptor valueField = field.getMessageType()
                    .findFieldByName("value");
            Map<String, Object> additionalProperties = valueSchema(
                    valueField,
                    path,
                    depth + 1,
                    ancestors,
                    descriptions
            );
            addTechnicalMetadata(additionalProperties, valueField);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("type", "object");
            result.put("additionalProperties", additionalProperties);
            addTechnicalMetadata(result, field);
            descriptions.addDescription(path, result);
            return result;
        }
        Map<String, Object> valueSchema = valueSchema(
                field,
                path,
                depth,
                ancestors,
                descriptions
        );
        addTechnicalMetadata(valueSchema, field);
        if (field.isRepeated()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("type", "array");
            result.put("items", valueSchema);
            addTechnicalMetadata(result, field);
            descriptions.addDescription(path, result);
            return result;
        }
        descriptions.addDescription(path, valueSchema);
        return valueSchema;
    }

    private Map<String, Object> valueSchema(
            Descriptors.FieldDescriptor field,
            String path,
            int depth,
            Set<String> ancestors,
            DocumentationIndex descriptions) {
        Map<String, Object> result;
        switch (field.getJavaType()) {
            case BOOLEAN -> result = type("boolean");
            case BYTE_STRING -> {
                result = type("string");
                result.put("format", "byte");
            }
            case DOUBLE -> {
                result = type("number");
                result.put("format", "double");
            }
            case FLOAT -> {
                result = type("number");
                result.put("format", "float");
            }
            case INT -> {
                result = type("integer");
                result.put("format", integerFormat(field));
            }
            case LONG -> {
                result = type("integer");
                result.put("format", longFormat(field));
            }
            case ENUM -> {
                result = type("string");
                result.put(
                        "enum",
                        field.getEnumType().getValues().stream()
                                .map(Descriptors.EnumValueDescriptor::getName)
                                .toList()
                );
                result.put("enumType", field.getEnumType().getFullName());
            }
            case MESSAGE -> result = messageSchema(
                    field.getMessageType(),
                    path,
                    depth + 1,
                    ancestors,
                    descriptions
            );
            case STRING -> result = type("string");
            default -> result = type("object");
        }
        return result;
    }

    private Map<String, Object> type(String type) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", type);
        return result;
    }

    private String integerFormat(Descriptors.FieldDescriptor field) {
        return switch (field.getType()) {
            case UINT32, FIXED32 -> "uint32";
            default -> "int32";
        };
    }

    private String longFormat(Descriptors.FieldDescriptor field) {
        return switch (field.getType()) {
            case UINT64, FIXED64 -> "uint64";
            default -> "int64";
        };
    }

    private void addTechnicalMetadata(
            Map<String, Object> schema,
            Descriptors.FieldDescriptor field) {
        schema.put("protobufType", field.getType().name());
        schema.put("protobufName", field.getName());
        schema.put("fieldNumber", field.getNumber());
    }

    private String childPath(String parent, String child) {
        return parent.isEmpty() ? child : parent + "." + child;
    }

    private static final class DocumentationIndex {

        private final Map<String, String> descriptions =
                new LinkedHashMap<>();

        private final Set<String> consumed = new HashSet<>();

        private DocumentationIndex(GatewaySchemaField[] documentation) {
            for (GatewaySchemaField field : documentation) {
                String path = field.path().trim();
                String description = field.description().trim();
                if (path.isEmpty()) {
                    throw new IllegalArgumentException(
                            "gateway schema field path must not be blank"
                    );
                }
                if (description.isEmpty()) {
                    throw new IllegalArgumentException(
                            "gateway schema field description must not be "
                                    + "blank: " + path
                    );
                }
                String previous = descriptions.putIfAbsent(
                        path,
                        description
                );
                if (previous != null) {
                    throw new IllegalArgumentException(
                            "duplicate gateway schema field path: " + path
                    );
                }
            }
        }

        private void addDescription(
                String path,
                Map<String, Object> schema) {
            String description = descriptions.get(path);
            if (description != null) {
                schema.put("description", description);
                consumed.add(path);
            }
        }

        private void verifyAllFieldsExist(String messageType) {
            List<String> unknown = descriptions.keySet().stream()
                    .filter(path -> !consumed.contains(path))
                    .toList();
            if (!unknown.isEmpty()) {
                throw new IllegalArgumentException(
                        "gateway schema field path does not exist in "
                                + messageType + ": " + unknown
                );
            }
        }
    }
}
