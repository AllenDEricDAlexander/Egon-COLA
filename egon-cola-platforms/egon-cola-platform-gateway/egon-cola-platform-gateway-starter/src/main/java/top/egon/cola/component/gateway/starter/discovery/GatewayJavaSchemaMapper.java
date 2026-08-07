package top.egon.cola.component.gateway.starter.discovery;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import top.egon.cola.component.gateway.starter.annotation.GatewaySchemaField;
import top.egon.cola.component.gateway.starter.annotation.GatewaySchemaRequired;
import top.egon.cola.component.gateway.starter.annotation.GatewaySchemaShape;
import top.egon.cola.component.gateway.starter.annotation.GatewaySchemaType;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

final class GatewayJavaSchemaMapper {

    static final String JSON_SCHEMA_2020_12 =
            "https://json-schema.org/draft/2020-12/schema";

    private static final int MAX_DEPTH = 64;

    private static final int MAX_NODES = 4_000;

    private static final int MAX_BYTES = 2 * 1024 * 1024;

    private final ObjectMapper objectMapper;

    GatewayJavaSchemaMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    Map<String, Object> schema(Type type) {
        return schema(objectMapper.getTypeFactory().constructType(type));
    }

    Map<String, Object> schema(JavaType type) {
        return schema(type, null);
    }

    Map<String, Object> schema(
            JavaType type,
            AnnotatedElement annotatedElement) {
        SchemaContext context = new SchemaContext();
        FieldMetadata metadata = annotatedElement == null
                ? emptyMetadata()
                : metadata(annotatedElement);
        Map<String, Object> root = context.node(type, metadata, 0);
        Map<String, Object> result = inlineRoot(root, context.definitions);
        result.put("$schema", JSON_SCHEMA_2020_12);
        if (!context.definitions.isEmpty()) {
            result.put("$defs", context.definitions);
        }
        verifySize(result);
        return result;
    }

    Map<String, Object> declaredSchema(
            JavaType actualType,
            Class<?> declaredClass,
            GatewaySchemaShape declaredShape,
            AnnotatedElement annotatedElement,
            String identity) {
        validateDeclaration(
                actualType,
                declaredClass,
                declaredShape,
                identity
        );
        return schema(actualType, annotatedElement);
    }

    void validateDeclaration(
            JavaType actualType,
            Class<?> declaredClass,
            GatewaySchemaShape declaredShape,
            String identity) {
        GatewaySchemaShape actualShape = shape(actualType);
        GatewaySchemaShape effectiveShape = declaredShape
                == GatewaySchemaShape.AUTO ? actualShape : declaredShape;
        if (effectiveShape != actualShape) {
            throw new IllegalArgumentException(
                    identity + " shape mismatch: declared=" + effectiveShape
                            + ", actual=" + actualShape
            );
        }
        if (effectiveShape == GatewaySchemaShape.VOID) {
            if (declaredClass != Void.class) {
                throw new IllegalArgumentException(
                        identity + " VOID schema must use Void.class"
                );
            }
            return;
        }
        JavaType compared = switch (effectiveShape) {
            case LIST -> actualType.isArrayType()
                    || actualType.isCollectionLikeType()
                    ? actualType.getContentType() : null;
            case MAP -> actualType.isMapLikeType()
                    ? actualType.getContentType() : null;
            default -> actualType;
        };
        if (compared == null || !sameType(compared.getRawClass(), declaredClass)) {
            throw new IllegalArgumentException(
                    identity + " schema mismatch: declared="
                            + declaredClass.getTypeName() + ", actual="
                            + (compared == null
                            ? actualType.toCanonical()
                            : compared.toCanonical())
            );
        }
        if (effectiveShape == GatewaySchemaShape.MAP) {
            JavaType keyType = actualType.getKeyType();
            if (keyType != null && keyType.getRawClass() != String.class) {
                throw new IllegalArgumentException(
                        identity + " map keys must be strings"
                );
            }
        }
    }

    GatewaySchemaShape shape(JavaType type) {
        Class<?> raw = type.getRawClass();
        if (raw == void.class || raw == Void.class) {
            return GatewaySchemaShape.VOID;
        }
        if (type.isArrayType() || type.isCollectionLikeType()) {
            return GatewaySchemaShape.LIST;
        }
        if (type.isMapLikeType()) {
            return GatewaySchemaShape.MAP;
        }
        return scalar(raw) || raw.isEnum()
                ? GatewaySchemaShape.VALUE
                : GatewaySchemaShape.OBJECT;
    }

    private Map<String, Object> inlineRoot(
            Map<String, Object> root,
            Map<String, Object> definitions) {
        Object reference = root.get("$ref");
        if (!(reference instanceof String value)
                || !value.startsWith("#/$defs/")) {
            return copyMap(root);
        }
        Object definition = definitions.get(value.substring("#/$defs/".length()));
        if (!(definition instanceof Map<?, ?> map)) {
            throw new IllegalStateException("unresolved Java schema ref " + value);
        }
        return copyMap(map);
    }

    private void verifySize(Map<String, Object> schema) {
        try {
            int bytes = objectMapper.writeValueAsString(schema)
                    .getBytes(StandardCharsets.UTF_8).length;
            if (bytes > MAX_BYTES) {
                throw new IllegalArgumentException(
                        "gateway Java schema exceeds byte limit " + MAX_BYTES
                );
            }
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "gateway Java schema cannot be serialized",
                    exception
            );
        }
    }

    private final class SchemaContext {

        private final Map<String, Object> definitions = new LinkedHashMap<>();

        private final Map<String, String> keys = new HashMap<>();

        private int nodes;

        private Map<String, Object> node(
                JavaType sourceType,
                FieldMetadata metadata,
                int depth) {
            if (depth > MAX_DEPTH || ++nodes > MAX_NODES) {
                throw new IllegalArgumentException(
                        "gateway Java schema exceeds safety limits"
                );
            }
            if (sourceType.getRawClass() == Optional.class) {
                JavaType content = metadata.implementationType(
                        requireContent(sourceType, "Optional")
                );
                return nullable(node(
                        content,
                        metadata.withoutImplementation(),
                        depth + 1
                ));
            }
            JavaType type = metadata.implementationType(sourceType);
            Class<?> raw = type.getRawClass();
            Map<String, Object> result;
            if (raw == void.class || raw == Void.class) {
                result = type("null");
            } else if (raw.isEnum()) {
                result = type("string");
                result.put(
                        "enum",
                        Arrays.stream(raw.getEnumConstants())
                                .map(String::valueOf)
                                .toList()
                );
            } else if (raw == String.class || raw == Character.class
                    || raw == char.class) {
                result = type("string");
            } else if (raw == UUID.class) {
                result = formatted("string", "uuid");
            } else if (raw == LocalDate.class) {
                result = formatted("string", "date");
            } else if (Set.of(
                    Instant.class,
                    LocalDateTime.class,
                    OffsetDateTime.class,
                    ZonedDateTime.class
            ).contains(raw)) {
                result = formatted("string", "date-time");
            } else if (raw == boolean.class || raw == Boolean.class) {
                result = type("boolean");
            } else if (integer(raw)) {
                result = type("integer");
                String format = integerFormat(raw);
                if (format != null) {
                    result.put("format", format);
                }
            } else if (number(raw)) {
                result = type("number");
            } else if (type.isArrayType()) {
                result = array(node(
                        requireContent(type, "array"),
                        emptyMetadata(),
                        depth + 1
                ));
            } else if (type.isCollectionLikeType()
                    || Collection.class.isAssignableFrom(raw)) {
                result = array(node(
                        requireContent(type, "collection"),
                        emptyMetadata(),
                        depth + 1
                ));
            } else if (type.isMapLikeType()
                    || Map.class.isAssignableFrom(raw)) {
                JavaType keyType = type.getKeyType();
                if (keyType != null && keyType.getRawClass() != String.class
                        && keyType.getRawClass() != Object.class) {
                    throw new IllegalArgumentException(
                            "gateway schema map keys must be strings: "
                                    + type.toCanonical()
                    );
                }
                result = type("object");
                result.put(
                        "additionalProperties",
                        node(
                                requireContent(type, "map"),
                                emptyMetadata(),
                                depth + 1
                        )
                );
            } else if (raw == Object.class) {
                throw new IllegalArgumentException(
                        "gateway schema type is incomplete: "
                                + type.toCanonical()
                );
            } else {
                result = object(type, depth);
            }
            metadata.applyTypeOverride(result, type);
            metadata.applyConstraints(result, type);
            metadata.applyDocumentation(result, type);
            return result;
        }

        private Map<String, Object> object(JavaType type, int depth) {
            String canonical = type.toCanonical();
            String key = definitionKey(type);
            if (definitions.containsKey(key)) {
                return reference(key);
            }
            Map<String, Object> definition = new LinkedHashMap<>();
            definitions.put(key, definition);
            BeanDescription bean = objectMapper.getSerializationConfig()
                    .introspect(type);
            Map<String, Object> properties = new LinkedHashMap<>();
            List<String> required = new ArrayList<>();
            for (BeanPropertyDefinition property : bean.findProperties()) {
                if (!property.couldSerialize()) {
                    continue;
                }
                FieldMetadata metadata = metadata(
                        property,
                        type.getRawClass()
                );
                JavaType propertyType = property.getPrimaryType();
                Map<String, Object> propertySchema = node(
                        propertyType,
                        metadata,
                        depth + 1
                );
                properties.put(property.getName(), propertySchema);
                if (metadata.required(property)) {
                    required.add(property.getName());
                }
            }
            definition.put("type", "object");
            definition.put("properties", properties);
            if (!required.isEmpty()) {
                definition.put("required", required);
            }
            definition.put("additionalProperties", false);
            keys.put(canonical, key);
            return reference(key);
        }

        private String definitionKey(JavaType type) {
            String canonical = type.toCanonical();
            String known = keys.get(canonical);
            if (known != null) {
                return known;
            }
            String base = type.getRawClass().getSimpleName();
            if (base.isBlank()) {
                base = "Schema";
            }
            String stable = base + "__" + shortHash(canonical);
            keys.put(canonical, stable);
            return stable;
        }
    }

    private final class FieldMetadata {

        private final GatewaySchemaField gateway;

        private final NotNull notNull;
        private final NotBlank notBlank;
        private final NotEmpty notEmpty;
        private final Size size;
        private final Min min;
        private final Max max;
        private final DecimalMin decimalMin;
        private final DecimalMax decimalMax;
        private final Pattern pattern;
        private final Positive positive;
        private final PositiveOrZero positiveOrZero;
        private final Email email;

        private final boolean implementationEnabled;

        private FieldMetadata(
                GatewaySchemaField gateway,
                AnnotationLookup lookup) {
            this.gateway = gateway;
            this.notNull = annotation(lookup, NotNull.class);
            this.notBlank = annotation(lookup, NotBlank.class);
            this.notEmpty = annotation(lookup, NotEmpty.class);
            this.size = annotation(lookup, Size.class);
            this.min = annotation(lookup, Min.class);
            this.max = annotation(lookup, Max.class);
            this.decimalMin = annotation(lookup, DecimalMin.class);
            this.decimalMax = annotation(lookup, DecimalMax.class);
            this.pattern = annotation(lookup, Pattern.class);
            this.positive = annotation(lookup, Positive.class);
            this.positiveOrZero = annotation(lookup, PositiveOrZero.class);
            this.email = annotation(lookup, Email.class);
            this.implementationEnabled = true;
        }

        private FieldMetadata(FieldMetadata source) {
            this.gateway = source.gateway;
            this.notNull = source.notNull;
            this.notBlank = source.notBlank;
            this.notEmpty = source.notEmpty;
            this.size = source.size;
            this.min = source.min;
            this.max = source.max;
            this.decimalMin = source.decimalMin;
            this.decimalMax = source.decimalMax;
            this.pattern = source.pattern;
            this.positive = source.positive;
            this.positiveOrZero = source.positiveOrZero;
            this.email = source.email;
            this.implementationEnabled = false;
        }

        private FieldMetadata withoutImplementation() {
            return new FieldMetadata(this);
        }

        private JavaType implementationType(JavaType source) {
            if (!implementationEnabled || gateway == null
                    || gateway.implementation() == Void.class) {
                return source;
            }
            Class<?> implementation = gateway.implementation();
            if (source.isCollectionLikeType()) {
                JavaType content = source.getContentType();
                validateImplementationTarget(content, implementation);
                return objectMapper.getTypeFactory().constructCollectionLikeType(
                        source.getRawClass(),
                        implementation
                );
            }
            if (source.isMapLikeType()) {
                JavaType content = source.getContentType();
                validateImplementationTarget(content, implementation);
                return objectMapper.getTypeFactory().constructMapLikeType(
                        source.getRawClass(),
                        source.getKeyType(),
                        objectMapper.constructType(implementation)
                );
            }
            validateImplementationTarget(source, implementation);
            return objectMapper.constructType(implementation);
        }

        private void validateImplementationTarget(
                JavaType source,
                Class<?> implementation) {
            Class<?> declared = source.getRawClass();
            if (!declared.isAssignableFrom(implementation)
                    && declared != Object.class) {
                throw new IllegalArgumentException(
                        "gateway schema implementation "
                                + implementation.getName()
                                + " is incompatible with "
                                + source.toCanonical()
                );
            }
            if (declared != Object.class && !declared.isInterface()
                    && !java.lang.reflect.Modifier.isAbstract(
                    declared.getModifiers()
            )) {
                throw new IllegalArgumentException(
                        "gateway schema implementation is redundant for "
                                + source.toCanonical()
                );
            }
        }

        private boolean required(BeanPropertyDefinition property) {
            boolean constrained = notNull != null || notBlank != null
                    || notEmpty != null || property.isRequired();
            if (gateway != null
                    && gateway.required() == GatewaySchemaRequired.OPTIONAL
                    && constrained) {
                throw new IllegalArgumentException(
                        "GatewaySchemaField OPTIONAL conflicts with required "
                                + "constraint on " + property.getName()
                );
            }
            return constrained || gateway != null
                    && gateway.required() == GatewaySchemaRequired.REQUIRED;
        }

        private void applyTypeOverride(
                Map<String, Object> schema,
                JavaType actualType) {
            if (gateway == null || gateway.type() == GatewaySchemaType.AUTO) {
                return;
            }
            String declared = switch (gateway.type()) {
                case STRING -> "string";
                case INTEGER -> "integer";
                case NUMBER -> "number";
                case BOOLEAN -> "boolean";
                case OBJECT, MAP -> "object";
                case ARRAY -> "array";
                case AUTO -> throw new IllegalStateException();
            };
            String actual = String.valueOf(schema.get("type"));
            if (schema.containsKey("$ref")) {
                actual = "object";
            }
            boolean numericWidening = "number".equals(declared)
                    && "integer".equals(actual);
            if (!declared.equals(actual) && !numericWidening) {
                throw new IllegalArgumentException(
                        "GatewaySchemaField type " + gateway.type()
                                + " is incompatible with "
                                + actualType.toCanonical()
                );
            }
            schema.put("type", declared);
        }

        private void applyConstraints(
                Map<String, Object> schema,
                JavaType type) {
            String schemaType = String.valueOf(schema.get("type"));
            if (notBlank != null) {
                requireSchemaType(schemaType, "string", "NotBlank", type);
                schema.put("minLength", 1);
            }
            if (notEmpty != null) {
                switch (schemaType) {
                    case "string" -> schema.put("minLength", 1);
                    case "array" -> schema.put("minItems", 1);
                    case "object" -> schema.put("minProperties", 1);
                    default -> throw incompatible("NotEmpty", type);
                }
            }
            if (size != null) {
                switch (schemaType) {
                    case "string" -> limits(
                            schema,
                            "minLength",
                            "maxLength",
                            size.min(),
                            size.max()
                    );
                    case "array" -> limits(
                            schema,
                            "minItems",
                            "maxItems",
                            size.min(),
                            size.max()
                    );
                    case "object" -> limits(
                            schema,
                            "minProperties",
                            "maxProperties",
                            size.min(),
                            size.max()
                    );
                    default -> throw incompatible("Size", type);
                }
            }
            if (min != null) {
                requireNumber(schemaType, "Min", type);
                schema.put("minimum", min.value());
            }
            if (max != null) {
                requireNumber(schemaType, "Max", type);
                schema.put("maximum", max.value());
            }
            if (decimalMin != null) {
                requireNumber(schemaType, "DecimalMin", type);
                schema.put(
                        decimalMin.inclusive()
                                ? "minimum" : "exclusiveMinimum",
                        new BigDecimal(decimalMin.value())
                );
            }
            if (decimalMax != null) {
                requireNumber(schemaType, "DecimalMax", type);
                schema.put(
                        decimalMax.inclusive()
                                ? "maximum" : "exclusiveMaximum",
                        new BigDecimal(decimalMax.value())
                );
            }
            if (pattern != null) {
                requireSchemaType(schemaType, "string", "Pattern", type);
                schema.put("pattern", pattern.regexp());
            }
            if (positive != null) {
                requireNumber(schemaType, "Positive", type);
                schema.put("exclusiveMinimum", 0);
            }
            if (positiveOrZero != null) {
                requireNumber(schemaType, "PositiveOrZero", type);
                schema.put("minimum", 0);
            }
            if (email != null) {
                requireSchemaType(schemaType, "string", "Email", type);
                schema.put("format", "email");
            }
        }

        private void applyDocumentation(
                Map<String, Object> schema,
                JavaType type) {
            if (gateway == null) {
                return;
            }
            if (!gateway.description().isBlank()) {
                schema.put("description", gateway.description().trim());
            }
            if (!gateway.format().isBlank()) {
                schema.put("format", gateway.format().trim());
            }
            if (!gateway.example().isBlank()) {
                schema.put(
                        "example",
                        parseExample(gateway.example(), schema, type)
                );
            }
        }
    }

    @FunctionalInterface
    private interface AnnotationLookup {

        Annotation get(Class<? extends Annotation> annotationType);
    }

    private FieldMetadata emptyMetadata() {
        return new FieldMetadata(null, annotationType -> null);
    }

    private FieldMetadata metadata(AnnotatedElement element) {
        return new FieldMetadata(
                element.getAnnotation(GatewaySchemaField.class),
                element::getAnnotation
        );
    }

    private FieldMetadata metadata(
            BeanPropertyDefinition property,
            Class<?> rawType) {
        List<AnnotationLookup> lookups = new ArrayList<>();
        add(lookups, property.getField());
        add(lookups, property.getGetter());
        add(lookups, property.getSetter());
        add(lookups, property.getConstructorParameter());
        if (rawType.isRecord()) {
            Arrays.stream(rawType.getRecordComponents())
                    .filter(component -> component.getName()
                            .equals(property.getInternalName()))
                    .findFirst()
                    .ifPresent(component -> lookups.add(
                            component::getAnnotation
                    ));
        }
        String identity = rawType.getName() + "." + property.getName();
        AnnotationLookup merged = annotationType -> merge(
                lookups,
                annotationType,
                identity
        );
        return new FieldMetadata(
                annotation(merged, GatewaySchemaField.class),
                merged
        );
    }

    private void add(
            List<AnnotationLookup> lookups,
            AnnotatedMember member) {
        if (member != null) {
            lookups.add(member::getAnnotation);
        }
    }

    private Annotation merge(
            List<AnnotationLookup> lookups,
            Class<? extends Annotation> annotationType,
            String identity) {
        Annotation result = null;
        for (AnnotationLookup lookup : lookups) {
            Annotation candidate = lookup.get(annotationType);
            if (candidate == null) {
                continue;
            }
            if (result != null && !result.equals(candidate)) {
                throw new IllegalArgumentException(
                        "conflicting " + annotationType.getSimpleName()
                                + " declarations for " + identity
                );
            }
            result = candidate;
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private <A extends Annotation> A annotation(
            AnnotationLookup lookup,
            Class<A> annotationType) {
        return (A) lookup.get(annotationType);
    }

    private Object parseExample(
            String value,
            Map<String, Object> schema,
            JavaType type) {
        String schemaType = schema.containsKey("$ref")
                ? "object" : String.valueOf(schema.get("type"));
        try {
            return switch (schemaType) {
                case "string" -> validateStringExample(value, schema);
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
                        List.class,
                        type
                );
                case "object" -> requireExampleType(
                        objectMapper.readValue(value, Object.class),
                        Map.class,
                        type
                );
                default -> throw new IllegalArgumentException();
            };
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "invalid GatewaySchemaField example for "
                            + type.toCanonical() + ": " + value,
                    exception
            );
        }
    }

    private String validateStringExample(
            String value,
            Map<String, Object> schema) {
        Object format = schema.get("format");
        if ("uuid".equals(format)) {
            UUID.fromString(value);
        } else if ("date".equals(format)) {
            LocalDate.parse(value);
        } else if ("date-time".equals(format)) {
            Instant.parse(value);
        }
        Object values = schema.get("enum");
        if (values instanceof Collection<?> collection
                && !collection.contains(value)) {
            throw new IllegalArgumentException("example is not an enum value");
        }
        return value;
    }

    private Object requireExampleType(
            Object value,
            Class<?> expected,
            JavaType type) {
        if (!expected.isInstance(value)) {
            throw new IllegalArgumentException(
                    "example does not match " + type.toCanonical()
            );
        }
        return value;
    }

    private void requireNumber(
            String actual,
            String constraint,
            JavaType type) {
        if (!"integer".equals(actual) && !"number".equals(actual)) {
            throw incompatible(constraint, type);
        }
    }

    private void requireSchemaType(
            String actual,
            String expected,
            String constraint,
            JavaType type) {
        if (!expected.equals(actual)) {
            throw incompatible(constraint, type);
        }
    }

    private IllegalArgumentException incompatible(
            String constraint,
            JavaType type) {
        return new IllegalArgumentException(
                constraint + " is incompatible with " + type.toCanonical()
        );
    }

    private void limits(
            Map<String, Object> schema,
            String minName,
            String maxName,
            int min,
            int max) {
        if (min > 0) {
            schema.put(minName, min);
        }
        if (max < Integer.MAX_VALUE) {
            schema.put(maxName, max);
        }
    }

    private JavaType requireContent(JavaType type, String kind) {
        JavaType content = type.getContentType();
        if (content == null && type.containedTypeCount() > 0) {
            content = type.containedType(0);
        }
        if (content == null || content.getRawClass() == Object.class) {
            throw new IllegalArgumentException(
                    "gateway schema " + kind + " type is incomplete: "
                            + type.toCanonical()
            );
        }
        return content;
    }

    private static Map<String, Object> nullable(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("anyOf", List.of(source, Map.of("type", "null")));
        return result;
    }

    private static Map<String, Object> reference(String key) {
        return new LinkedHashMap<>(Map.of("$ref", "#/$defs/" + key));
    }

    private static Map<String, Object> array(Map<String, Object> items) {
        Map<String, Object> result = type("array");
        result.put("items", items);
        return result;
    }

    private static Map<String, Object> formatted(
            String type,
            String format) {
        Map<String, Object> result = type(type);
        result.put("format", format);
        return result;
    }

    private static Map<String, Object> type(String type) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", type);
        return result;
    }

    private static boolean sameType(Class<?> left, Class<?> right) {
        return boxed(left).equals(boxed(right));
    }

    private static Class<?> boxed(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        return Array.get(Array.newInstance(type, 1), 0).getClass();
    }

    private static boolean scalar(Class<?> raw) {
        return raw == String.class || raw == Character.class
                || raw == char.class || raw == UUID.class
                || raw == LocalDate.class || raw == Instant.class
                || raw == LocalDateTime.class || raw == OffsetDateTime.class
                || raw == ZonedDateTime.class || raw == boolean.class
                || raw == Boolean.class || integer(raw) || number(raw);
    }

    private static boolean integer(Class<?> raw) {
        return raw == byte.class || raw == Byte.class
                || raw == short.class || raw == Short.class
                || raw == int.class || raw == Integer.class
                || raw == long.class || raw == Long.class
                || raw == BigInteger.class;
    }

    private static boolean number(Class<?> raw) {
        return raw == float.class || raw == Float.class
                || raw == double.class || raw == Double.class
                || raw == BigDecimal.class;
    }

    private static String integerFormat(Class<?> raw) {
        if (raw == byte.class || raw == Byte.class
                || raw == short.class || raw == Short.class
                || raw == int.class || raw == Integer.class) {
            return "int32";
        }
        if (raw == long.class || raw == Long.class) {
            return "int64";
        }
        return null;
    }

    private static String shortHash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(12);
            for (int index = 0; index < 6; index++) {
                result.append(String.format("%02x", digest[index]));
            }
            return result.toString();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> copyMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(
                String.valueOf(key),
                copyValue(value)
        ));
        return result;
    }

    private static Object copyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return copyMap(map);
        }
        if (value instanceof List<?> list) {
            return list.stream().map(GatewayJavaSchemaMapper::copyValue).toList();
        }
        return value;
    }
}
