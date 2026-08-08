package top.egon.cola.component.gateway.starter.discovery.schema;

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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Maps Jackson-resolved Java types and Gateway field metadata to JSON Schema
 * documents while enforcing declaration, recursion, and size safety rules.
 * 中文说明：将 Java 类型、网关字段注解和 Bean Validation 约束统一投影为受安全限制的 JSON Schema。
 */
public final class GatewayJavaSchemaMapper {

    /** JSON Schema dialect URI emitted by generated documents. 生成文档使用的 JSON Schema 方言 URI。 */
    public static final String JSON_SCHEMA_2020_12 =
            "https://json-schema.org/draft/2020-12/schema";

    /** Maximum recursive type depth accepted during schema generation. Schema 生成允许的最大递归深度。 */
    private static final int MAX_DEPTH = 64;

    /** Maximum number of schema nodes accepted during one generation. 单次生成允许访问的最大节点数。 */
    private static final int MAX_NODES = 4_000;

    /** Maximum UTF-8 serialized size of a generated schema document. 生成文档序列化后的最大 UTF-8 字节数。 */
    private static final int MAX_BYTES = 2 * 1024 * 1024;

    /** Jackson mapper used for type construction, introspection, and JSON parsing. 用于类型构造、反射和 JSON 解析。 */
    private final ObjectMapper objectMapper;

    /**
     * Creates a Java schema mapper.
     * 中文说明：保存用于解析 Gateway 模型和示例的 Jackson 配置。
     *
     * @param objectMapper Jackson mapper configured for Gateway model types
     */
    public GatewayJavaSchemaMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Generates a JSON Schema document for a reflective Java type.
     * 中文说明：将反射 Type 转为 Jackson JavaType 后生成完整 Schema 文档。
     *
     * @param type reflective Java type
     * @return generated JSON Schema document
     */
    public Map<String, Object> schema(Type type) {
        return schema(objectMapper.getTypeFactory().constructType(type));
    }

    /**
     * Generates a JSON Schema document for a Jackson Java type.
     * 中文说明：从已解析的 JavaType 自动推导对象、数组、映射和标量形状。
     *
     * @param type resolved Jackson type
     * @return generated JSON Schema document
     */
    public Map<String, Object> schema(JavaType type) {
        return schema(type, null);
    }

    /**
     * Generates a JSON Schema document and applies metadata from an annotated
     * source element when supplied.
     * 中文说明：根节点会附加字段注解、校验约束及可复用定义，并检查文档大小。
     *
     * @param type resolved Java type
     * @param annotatedElement metadata source, or {@code null}
     * @return generated JSON Schema document
     * @throws IllegalArgumentException if the type, metadata, constraints, or
     *         resulting schema violates Gateway safety rules
     */
    public Map<String, Object> schema(
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

    /**
     * Validates an explicit schema declaration and generates the schema for the
     * actual Java type.
     * 中文说明：先验证显式类和形状与实际 Java 类型一致，再按实际类型生成文档。
     *
     * @param actualType resolved Java type
     * @param declaredClass explicitly declared schema class
     * @param declaredShape explicitly declared schema shape
     * @param annotatedElement metadata source, or {@code null}
     * @param identity declaration identity used in validation errors
     * @return generated schema document
     * @throws IllegalArgumentException if declaration and Java type differ
     */
    public Map<String, Object> declaredSchema(
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

    /**
     * Validates an explicit schema class and shape against a resolved Java type.
     * 中文说明：列表比较元素类型、映射检查字符串键，void 仅允许声明 Void.class。
     *
     * @param actualType resolved Java type
     * @param declaredClass explicitly declared schema class
     * @param declaredShape explicitly declared schema shape
     * @param identity declaration identity used in validation errors
     * @throws IllegalArgumentException if shape, element type, map key type, or
     *         void declaration is incompatible
     */
    public void validateDeclaration(
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

    /**
     * Classifies a resolved Java type into a Gateway schema shape.
     * 中文说明：void、列表、映射、标量和对象分别归类为对应 GatewaySchemaShape。
     *
     * @param type resolved Java type
     * @return matching Gateway schema shape
     */
    public GatewaySchemaShape shape(JavaType type) {
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

    /**
     * Replaces a root definition reference with a mutable copy of its target.
     * 中文说明：根节点若只是 $ref，则展开其定义副本，确保调用方可以安全修改结果。
     *
     * @param root generated root schema or reference
     * @param definitions generated definitions
     * @return mutable inlined root schema
     * @throws IllegalStateException if a root reference cannot be resolved
     */
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

    /**
     * Verifies the serialized UTF-8 size of a generated schema.
     * 中文说明：Schema 序列化失败或超过 2 MiB 限制时立即拒绝生成结果。
     *
     * @param schema schema document to verify
     * @throws IllegalArgumentException if serialization fails or the byte limit
     *         is exceeded
     */
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

    /** Maintains definitions, stable keys, and safety counters for one mapping. 维护一次映射的定义、稳定键和安全计数器。 */
    private final class SchemaContext {

        /** Reusable definitions indexed by stable generated keys. 按稳定生成键索引的可复用定义。 */
        private final Map<String, Object> definitions = new LinkedHashMap<>();

        /** Generated definition keys indexed by canonical Java type. 按 Java 规范类型名索引定义键。 */
        private final Map<String, String> keys = new HashMap<>();

        /** Number of schema nodes visited in this mapping operation. 当前映射已访问的 Schema 节点数。 */
        private int nodes;

        /**
         * Maps one Java type node and applies its field metadata.
         * 中文说明：递归处理 Optional、集合、映射和对象节点，并在每层应用字段元数据。
         *
         * @param sourceType source Java type
         * @param metadata field metadata to apply
         * @param depth current recursive depth
         * @return generated schema node
         * @throws IllegalArgumentException if safety limits, generic type
         *         completeness, or metadata compatibility checks fail
         */
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

        /**
         * Introspects a serializable object type into a reusable definition.
         * 中文说明：通过 Jackson 序列化属性生成对象定义，递归类型使用稳定引用避免重复展开。
         *
         * @param type object type to introspect
         * @param depth current recursive depth
         * @return reference to the generated or existing definition
         */
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

        /**
         * Returns a stable collision-resistant definition key for a Java type.
         * 中文说明：键由简单类名和规范类型名摘要组成，兼顾可读性与碰撞规避。
         *
         * @param type Java type to identify
         * @return stable definition key
         */
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

    /**
     * Aggregates Gateway and Bean Validation metadata for one schema field and
     * applies it to generated schema nodes.
     * 中文说明：集中保存字段注解和校验约束，避免在各类节点分支中重复处理。
     */
    private final class FieldMetadata {

        /** Gateway-specific field declaration, or {@code null}. 网关字段声明，不存在时为 {@code null}。 */
        private final GatewaySchemaField gateway;

        /** Not-null constraint, or {@code null}. 非空约束，不存在时为 {@code null}。 */
        private final NotNull notNull;
        /** Not-blank constraint, or {@code null}. 非空白约束，不存在时为 {@code null}。 */
        private final NotBlank notBlank;
        /** Not-empty constraint, or {@code null}. 非空集合或字符串约束，不存在时为 {@code null}。 */
        private final NotEmpty notEmpty;
        /** Size constraint, or {@code null}. 长度或数量约束，不存在时为 {@code null}。 */
        private final Size size;
        /** Integral minimum constraint, or {@code null}. 整数最小值约束，不存在时为 {@code null}。 */
        private final Min min;
        /** Integral maximum constraint, or {@code null}. 整数最大值约束，不存在时为 {@code null}。 */
        private final Max max;
        /** Decimal minimum constraint, or {@code null}. 小数最小值约束，不存在时为 {@code null}。 */
        private final DecimalMin decimalMin;
        /** Decimal maximum constraint, or {@code null}. 小数最大值约束，不存在时为 {@code null}。 */
        private final DecimalMax decimalMax;
        /** Regular-expression constraint, or {@code null}. 正则表达式约束，不存在时为 {@code null}。 */
        private final Pattern pattern;
        /** Strictly-positive constraint, or {@code null}. 严格正数约束，不存在时为 {@code null}。 */
        private final Positive positive;
        /** Non-negative constraint, or {@code null}. 非负数约束，不存在时为 {@code null}。 */
        private final PositiveOrZero positiveOrZero;
        /** Email-format constraint, or {@code null}. 邮箱格式约束，不存在时为 {@code null}。 */
        private final Email email;

        /** Whether a declared concrete implementation may replace the source type. 是否允许具体实现替换源类型。 */
        private final boolean implementationEnabled;

        /**
         * Creates metadata from a Gateway declaration and annotation lookup.
         * 中文说明：从字段声明及成员注解查找器收集所有可用约束。
         *
         * @param gateway Gateway field declaration, or {@code null}
         * @param lookup lookup used to resolve Bean Validation annotations
         */
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

        /**
         * Copies metadata while disabling another implementation substitution.
         * 中文说明：复制约束信息，同时关闭下一层实现类型替换，防止 Optional 递归重复替换。
         *
         * @param source metadata to copy
         */
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

        /**
         * Creates a copy that cannot reapply a declared implementation type.
         * 中文说明：返回禁止再次应用 implementation 的元数据副本。
         *
         * @return metadata copy with implementation substitution disabled
         */
        private FieldMetadata withoutImplementation() {
            return new FieldMetadata(this);
        }

        /**
         * Applies a declared concrete implementation to a scalar or container
         * content type.
         * 中文说明：对接口、抽象类、集合元素或映射值应用兼容的具体实现类型。
         *
         * @param source declared source type
         * @return effective type used for schema generation
         * @throws IllegalArgumentException if the implementation is
         *         incompatible or redundant
         */
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

        /**
         * Validates that an implementation can legally specialize a source
         * type.
         * 中文说明：实现类必须可赋值给源类型，且不能为已经具体化的源类重复声明。
         *
         * @param source source type being specialized
         * @param implementation declared implementation class
         * @throws IllegalArgumentException if assignment is invalid or the
         *         source type is already concrete
         */
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

        /**
         * Resolves requiredness from Jackson, Bean Validation, and Gateway
         * metadata.
         * 中文说明：必填性由 Jackson 属性状态、非空约束和 GatewaySchemaField 共同决定。
         *
         * @param property Jackson property being mapped
         * @return {@code true} when the property is required
         * @throws IllegalArgumentException if an optional declaration conflicts
         *         with a required constraint
         */
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

        /**
         * Validates and applies an explicit Gateway JSON Schema type override.
         * 中文说明：允许整数向 number 宽化，其余类型覆盖必须与实际 Schema 类型相容。
         *
         * @param schema generated schema node to update
         * @param actualType resolved Java type
         * @throws IllegalArgumentException if the override is incompatible
         */
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

        /**
         * Projects supported Bean Validation annotations into JSON Schema
         * constraints.
         * 中文说明：把长度、范围、正数、正则和邮箱等 Jakarta 校验约束转换为 Schema 关键字。
         *
         * @param schema generated schema node to update
         * @param type resolved Java type
         * @throws IllegalArgumentException if a constraint is incompatible with
         *         the generated schema type
         */
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

        /**
         * Applies Gateway description, format, and typed example metadata.
         * 中文说明：写入网关描述、格式和经过类型校验的示例值。
         *
         * @param schema generated schema node to update
         * @param type resolved Java type
         * @throws IllegalArgumentException if an example is invalid
         */
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

    /** Resolves a requested annotation from one metadata source. 从单个元数据来源解析指定注解。 */
    @FunctionalInterface
    private interface AnnotationLookup {

        /**
         * Looks up an annotation by type.
         * 中文说明：按注解类型读取成员上的声明，不存在时返回 null。
         *
         * @param annotationType annotation type to resolve
         * @return resolved annotation, or {@code null}
         */
        Annotation get(Class<? extends Annotation> annotationType);
    }

    /**
     * Creates metadata with no Gateway or validation annotations.
     * 中文说明：用于数组、集合和映射内容等没有独立注解来源的节点。
     *
     * @return empty metadata
     */
    private FieldMetadata emptyMetadata() {
        return new FieldMetadata(null, annotationType -> null);
    }

    /**
     * Reads field metadata directly from one annotated element.
     * 中文说明：从方法参数、字段或 record component 直接读取 Gateway 与校验注解。
     *
     * @param element metadata source
     * @return resolved field metadata
     */
    private FieldMetadata metadata(AnnotatedElement element) {
        return new FieldMetadata(
                element.getAnnotation(GatewaySchemaField.class),
                element::getAnnotation
        );
    }

    /**
     * Merges metadata from all Jackson members representing one property,
     * including its record component when applicable.
     * 中文说明：合并字段、访问器、构造参数和 record component，并拒绝冲突声明。
     *
     * @param property Jackson property definition
     * @param rawType declaring raw class
     * @return merged field metadata
     * @throws IllegalArgumentException if equivalent members declare
     *         conflicting annotation values
     */
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

    /**
     * Adds a Jackson member as an annotation lookup when it exists.
     * 中文说明：仅将实际存在的 Jackson 成员加入查找列表。
     *
     * @param lookups lookup collection to update
     * @param member Jackson member, or {@code null}
     */
    private void add(
            List<AnnotationLookup> lookups,
            AnnotatedMember member) {
        if (member != null) {
            lookups.add(member::getAnnotation);
        }
    }

    /**
     * Merges one annotation type from multiple property metadata sources.
     * 中文说明：多个成员声明相同注解时必须完全相等，否则报告属性级冲突。
     *
     * @param lookups metadata sources
     * @param annotationType annotation type to merge
     * @param identity property identity used in errors
     * @return common annotation value, or {@code null} when absent
     * @throws IllegalArgumentException if sources contain different values
     */
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

    /**
     * Resolves and casts an annotation from a lookup.
     * 中文说明：以泛型方式返回查找到的注解实例，缺失时保持 null。
     *
     * @param lookup annotation lookup
     * @param annotationType requested annotation type
     * @param <A> annotation type
     * @return resolved annotation, or {@code null}
     */
    @SuppressWarnings("unchecked")
    private <A extends Annotation> A annotation(
            AnnotationLookup lookup,
            Class<A> annotationType) {
        return (A) lookup.get(annotationType);
    }

    /**
     * Parses and validates a Gateway field example according to its generated
     * schema type.
     * 中文说明：示例会按 Schema 类型解析，并额外校验格式、枚举和容器类型。
     *
     * @param value textual example
     * @param schema generated schema node
     * @param type resolved Java type used in errors
     * @return typed example value
     * @throws IllegalArgumentException if the example is malformed or has an
     *         incompatible type or format
     */
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

    /**
     * Validates formatted and enumerated string examples.
     * 中文说明：检查字符串是否属于枚举，并支持 uuid、date 和 date-time 格式校验。
     *
     * @param value string example
     * @param schema generated string schema
     * @return validated input value
     * @throws IllegalArgumentException if format or enum validation fails
     */
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

    /**
     * Ensures a parsed structured example has the expected container type.
     * 中文说明：数组或对象示例必须解析为声明的容器类型。
     *
     * @param value parsed example
     * @param expected expected container class
     * @param type resolved Java type used in errors
     * @return validated value
     * @throws IllegalArgumentException if the value has a different type
     */
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

    /**
     * Requires a generated schema type to be numeric.
     * 中文说明：只有 integer 或 number 节点可以应用数值范围约束。
     *
     * @param actual generated schema type
     * @param constraint constraint name used in errors
     * @param type resolved Java type
     * @throws IllegalArgumentException if the schema is not numeric
     */
    private void requireNumber(
            String actual,
            String constraint,
            JavaType type) {
        if (!"integer".equals(actual) && !"number".equals(actual)) {
            throw incompatible(constraint, type);
        }
    }

    /**
     * Requires an exact generated schema type for a constraint.
     * 中文说明：字符串、数组等约束只能作用于其对应的 Schema 节点类型。
     *
     * @param actual generated schema type
     * @param expected required schema type
     * @param constraint constraint name used in errors
     * @param type resolved Java type
     * @throws IllegalArgumentException if the types differ
     */
    private void requireSchemaType(
            String actual,
            String expected,
            String constraint,
            JavaType type) {
        if (!expected.equals(actual)) {
            throw incompatible(constraint, type);
        }
    }

    /**
     * Creates a constraint incompatibility exception.
     * 中文说明：统一报告约束名称与实际 Java 类型，便于定位注解误用。
     *
     * @param constraint incompatible constraint name
     * @param type resolved Java type
     * @return qualified exception
     */
    private IllegalArgumentException incompatible(
            String constraint,
            JavaType type) {
        return new IllegalArgumentException(
                constraint + " is incompatible with " + type.toCanonical()
        );
    }

    /**
     * Applies finite lower and upper size limits to a schema.
     * 中文说明：仅写入有效的最小值和非默认最大值，避免产生冗余关键字。
     *
     * @param schema schema node to update
     * @param minName lower-bound keyword
     * @param maxName upper-bound keyword
     * @param min configured lower bound
     * @param max configured upper bound
     */
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

    /**
     * Resolves a complete generic content type for a container.
     * 中文说明：集合、映射或 Optional 缺少泛型内容时拒绝生成不完整 Schema。
     *
     * @param type container type
     * @param kind container description used in errors
     * @return resolved non-Object content type
     * @throws IllegalArgumentException if generic content is absent or erased
     */
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

    /**
     * Wraps a schema in an alternative that also accepts JSON null.
     * 中文说明：将原 Schema 与 null 分支组合，表达 Optional 或可空载荷。
     *
     * @param source non-null schema
     * @return nullable schema
     */
    private static Map<String, Object> nullable(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("anyOf", List.of(source, Map.of("type", "null")));
        return result;
    }

    /**
     * Builds a local definition reference.
     * 中文说明：生成指向当前文档 $defs 的本地引用节点。
     *
     * @param key definition key
     * @return local reference schema
     */
    private static Map<String, Object> reference(String key) {
        return new LinkedHashMap<>(Map.of("$ref", "#/$defs/" + key));
    }

    /**
     * Builds an array schema for an item schema.
     * 中文说明：创建数组节点并把给定 Schema 作为 items。
     *
     * @param items item schema
     * @return array schema
     */
    private static Map<String, Object> array(Map<String, Object> items) {
        Map<String, Object> result = type("array");
        result.put("items", items);
        return result;
    }

    /**
     * Builds a typed schema with a format keyword.
     * 中文说明：在基础类型上附加 JSON Schema format 信息。
     *
     * @param type JSON Schema type
     * @param format JSON Schema format
     * @return formatted schema
     */
    private static Map<String, Object> formatted(
            String type,
            String format) {
        Map<String, Object> result = type(type);
        result.put("format", format);
        return result;
    }

    /**
     * Builds a schema containing only a type keyword.
     * 中文说明：创建后续约束处理可继续扩展的最小类型节点。
     *
     * @param type JSON Schema type
     * @return typed schema
     */
    private static Map<String, Object> type(String type) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", type);
        return result;
    }

    /**
     * Compares classes after normalizing primitive types to wrappers.
     * 中文说明：基本类型和包装类型按同一逻辑值类型比较。
     *
     * @param left first class
     * @param right second class
     * @return {@code true} when both represent the same logical type
     */
    private static boolean sameType(Class<?> left, Class<?> right) {
        return boxed(left).equals(boxed(right));
    }

    /**
     * Converts a primitive class to its wrapper class.
     * 中文说明：将基本类型标准化为包装类，其他类型原样返回。
     *
     * @param type class to normalize
     * @return wrapper class for a primitive, or the original class otherwise
     */
    private static Class<?> boxed(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        return Array.get(Array.newInstance(type, 1), 0).getClass();
    }

    /**
     * Determines whether a raw Java class maps to a scalar schema.
     * 中文说明：识别字符串、数字、布尔、字符、UUID 和时间类型等标量类。
     *
     * @param raw raw Java class
     * @return {@code true} for supported scalar classes
     */
    private static boolean scalar(Class<?> raw) {
        return raw == String.class || raw == Character.class
                || raw == char.class || raw == UUID.class
                || raw == LocalDate.class || raw == Instant.class
                || raw == LocalDateTime.class || raw == OffsetDateTime.class
                || raw == ZonedDateTime.class || raw == boolean.class
                || raw == Boolean.class || integer(raw) || number(raw);
    }

    /**
     * Determines whether a raw Java class maps to an integer schema.
     * 中文说明：识别 byte、short、int、long 及大整数类型。
     *
     * @param raw raw Java class
     * @return {@code true} for supported integral classes
     */
    private static boolean integer(Class<?> raw) {
        return raw == byte.class || raw == Byte.class
                || raw == short.class || raw == Short.class
                || raw == int.class || raw == Integer.class
                || raw == long.class || raw == Long.class
                || raw == BigInteger.class;
    }

    /**
     * Determines whether a raw Java class maps to a non-integral number schema.
     * 中文说明：识别浮点和 BigDecimal 等非整数数值类型。
     *
     * @param raw raw Java class
     * @return {@code true} for supported decimal classes
     */
    private static boolean number(Class<?> raw) {
        return raw == float.class || raw == Float.class
                || raw == double.class || raw == Double.class
                || raw == BigDecimal.class;
    }

    /**
     * Resolves the JSON Schema integer format for a Java integral class.
     * 中文说明：为 int、long 等整数类型补充对应的位宽格式。
     *
     * @param raw integral Java class
     * @return {@code int32}, {@code int64}, or {@code null}
     */
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

    /**
     * Creates a deterministic twelve-character SHA-256 prefix.
     * 中文说明：对规范类型名取稳定摘要，作为定义键的短后缀。
     *
     * @param value value to hash
     * @return lowercase hexadecimal hash prefix
     * @throws IllegalStateException if SHA-256 is unavailable
     */
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

    /**
     * Creates a mutable deep copy of a schema map.
     * 中文说明：递归复制 Schema 容器，避免修改时影响原始定义。
     *
     * @param source source map
     * @return recursively copied string-keyed map
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> copyMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(
                String.valueOf(key),
                copyValue(value)
        ));
        return result;
    }

    /**
     * Recursively copies nested schema maps and lists.
     * 中文说明：Map 和 List 会继续深拷贝，其他标量值直接返回。
     *
     * @param value schema value to copy
     * @return copied container or original scalar value
     */
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
