package top.egon.cola.component.gateway.starter.discovery;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.starter.annotation.GatewaySchemaField;
import top.egon.cola.component.gateway.starter.annotation.GatewaySchemaRequired;
import top.egon.cola.component.gateway.starter.discovery.schema.GatewayJavaSchemaMapper;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayJavaSchemaMapperTest {

    private final GatewayJavaSchemaMapper mapper =
            new GatewayJavaSchemaMapper(new ObjectMapper());

    @Test
    void preservesNestedGenericsJacksonNamesValidationAndMetadata()
            throws Exception {
        Method method = Fixtures.class.getDeclaredMethod("aggregate");

        Map<String, Object> schema = mapper.schema(
                method.getGenericReturnType()
        );

        assertThat(schema).containsEntry("type", "object");
        Map<String, Object> properties = map(schema.get("properties"));
        assertThat(properties).containsKeys(
                "request_id",
                "lines",
                "amounts",
                "state",
                "active",
                "createdOn",
                "createdAt"
        ).doesNotContainKey("secret");
        assertThat(map(properties.get("request_id")))
                .containsEntry("type", "string")
                .containsEntry("format", "uuid")
                .containsEntry("description", "请求 ID")
                .containsEntry("example", "3d594650-3436-4d8d-8bd4-22d6f65f58a2");
        assertThat(map(properties.get("lines")))
                .containsEntry("type", "array")
                .containsEntry("minItems", 1);
        assertThat(map(properties.get("amounts")))
                .containsEntry("type", "object")
                .containsKey("additionalProperties");
        assertThat(map(properties.get("createdOn")))
                .containsEntry("format", "date");
        assertThat(map(properties.get("createdAt")))
                .containsEntry("format", "date-time");
        assertThat(schema).containsKey("$defs");
    }

    @Test
    void emitsFiniteLocalReferencesForSelfAndMutualRecursion()
            throws Exception {
        Map<String, Object> schema = mapper.schema(
                Fixtures.class.getDeclaredMethod("tree")
                        .getGenericReturnType()
        );

        assertThat(schema.toString())
                .contains("#/$defs/")
                .doesNotContain("truncated");
        assertThat(schema).containsKey("$defs");
    }

    @Test
    void rejectsRequiredWeakeningAndInvalidExamples() throws Exception {
        assertThatThrownBy(() -> mapper.schema(
                InvalidFixtures.class.getDeclaredMethod("optionalRequired")
                        .getGenericReturnType()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("OPTIONAL")
                .hasMessageContaining("required");
        assertThatThrownBy(() -> mapper.schema(
                InvalidFixtures.class.getDeclaredMethod("invalidExample")
                        .getGenericReturnType()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("example");
    }

    @Test
    void appliesImplementationToOptionalAbstractPayload() throws Exception {
        Map<String, Object> schema = mapper.schema(
                Fixtures.class.getDeclaredMethod("optionalPet")
                        .getGenericReturnType()
        );

        Map<String, Object> pet = map(map(schema.get("properties")).get("pet"));
        assertThat(pet).containsKey("anyOf");
        assertThat(pet.toString())
                .contains("#/$defs/")
                .contains("宠物");
    }

    @Test
    void allowsArbitraryJsonOnlyAtAnExplicitDynamicMapBoundary()
            throws Exception {
        Map<String, Object> schema = mapper.schema(
                Fixtures.class.getDeclaredMethod("dynamicDocument")
                        .getGenericReturnType()
        );

        Map<String, Object> payload = map(
                map(schema.get("properties")).get("payload")
        );
        assertThat(payload).containsEntry("type", "object");
        assertThat(map(payload.get("additionalProperties"))).isEmpty();

        assertThatThrownBy(() -> mapper.schema(
                InvalidFixtures.class.getDeclaredMethod("erasedDocument")
                        .getGenericReturnType()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("map type is incomplete");
    }

    @Test
    void rejectsSchemasBeyondTheDepthSafetyLimit() {
        com.fasterxml.jackson.databind.JavaType type =
                new ObjectMapper().constructType(String.class);
        for (int index = 0; index < 70; index++) {
            type = new ObjectMapper().getTypeFactory()
                    .constructCollectionType(List.class, type);
        }

        com.fasterxml.jackson.databind.JavaType deepType = type;
        assertThatThrownBy(() -> mapper.schema(deepType))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("safety limits");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    private static final class Fixtures {

        static Aggregate aggregate() {
            return null;
        }

        static Node tree() {
            return null;
        }

        static OptionalPet optionalPet() {
            return null;
        }

        static DynamicDocument dynamicDocument() {
            return null;
        }
    }

    private static final class InvalidFixtures {

        static OptionalRequired optionalRequired() {
            return null;
        }

        static InvalidExample invalidExample() {
            return null;
        }

        static ErasedDocument erasedDocument() {
            return null;
        }
    }

    private record Aggregate(
            @JsonProperty("request_id")
            @GatewaySchemaField(
                    description = "请求 ID",
                    example = "3d594650-3436-4d8d-8bd4-22d6f65f58a2"
            )
            UUID requestId,
            @GatewaySchemaField(description = "订单行")
            @NotEmpty
            List<Line> lines,
            @GatewaySchemaField(description = "按币种汇总")
            Map<String, Money> amounts,
            State state,
            boolean active,
            LocalDate createdOn,
            Instant createdAt,
            @JsonIgnore String secret
    ) {
    }

    private record Line(
            @NotBlank
            @Size(max = 40)
            @Pattern(regexp = "L-[0-9]+")
            String id,
            @Min(1) @Max(999) int quantity
    ) {
    }

    private record Money(BigDecimal amount, String currency) {
    }

    private enum State {
        CREATED,
        PAID
    }

    private record Node(String name, List<Node> children, Peer peer) {
    }

    private record Peer(Node owner) {
    }

    private interface Pet {
    }

    private record Cat(String name) implements Pet {
    }

    private record OptionalPet(
            @GatewaySchemaField(
                    description = "宠物",
                    implementation = Cat.class
            )
            Optional<Pet> pet
    ) {
    }

    private record DynamicDocument(
            @GatewaySchemaField(allowArbitraryJson = true)
            Map<String, Object> payload
    ) {
    }

    private record ErasedDocument(Map<String, Object> payload) {
    }

    private record OptionalRequired(
            @NotBlank
            @GatewaySchemaField(required = GatewaySchemaRequired.OPTIONAL)
            String value
    ) {
    }

    private record InvalidExample(
            @GatewaySchemaField(example = "not-a-number")
            int value
    ) {
    }
}
