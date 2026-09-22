package top.egon.cola.component.codegen.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.codegen.validation.CodegenGroups;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * External generator request. Nested types keep one lifecycle instead of one file per field.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder(alphabetic = true)
public class CodegenConfigBO {

    @NotNull(groups = CodegenGroups.Plan.class)
    private Integer configVersion;

    @NotNull(groups = CodegenGroups.Plan.class)
    @JsonProperty("projectType")
    private CodegenProfileEnum profile;

    private String basePackage;

    private String domain;

    @NotBlank(groups = {CodegenGroups.Plan.class, CodegenGroups.Apply.class})
    private String outputRoot;

    @NotNull(groups = CodegenGroups.Plan.class)
    @Valid
    private InputBO input;

    @JsonProperty("modulePaths")
    @Builder.Default
    private Map<String, String> roots = new LinkedHashMap<>();

    @JsonProperty("logicalTables")
    @Builder.Default
    private List<String> tables = new ArrayList<>();

    @NotEmpty(groups = CodegenGroups.Plan.class)
    @Builder.Default
    private List<String> artifacts = new ArrayList<>();

    @JsonProperty("existingTypeMappings")
    @Builder.Default
    private Map<String, String> existingTypes = new LinkedHashMap<>();

    @Valid
    private FieldPoliciesBO fieldPolicies;

    @Valid
    private ApiContractBO apiContract;

    @Valid
    private EventsBO events;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonPropertyOrder(alphabetic = true)
    public static class InputBO {

        @NotBlank(groups = CodegenGroups.Plan.class)
        private String mode;

        private String resourceRoot;

        @Builder.Default
        private List<String> schemaFiles = new ArrayList<>();

        private String manifest;

        InputBO snapshot() {
            return InputBO.builder()
                    .mode(mode)
                    .resourceRoot(resourceRoot)
                    .schemaFiles(copyList(schemaFiles))
                    .manifest(manifest)
                    .build();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonPropertyOrder(alphabetic = true)
    public static class FieldPoliciesBO {

        @Builder.Default
        private List<String> create = new ArrayList<>();

        @Builder.Default
        private List<String> update = new ArrayList<>();

        @Builder.Default
        private List<String> result = new ArrayList<>();

        @Builder.Default
        private List<String> filter = new ArrayList<>();

        @Builder.Default
        private List<String> sort = new ArrayList<>();

        FieldPoliciesBO snapshot() {
            return FieldPoliciesBO.builder()
                    .create(copyList(create))
                    .update(copyList(update))
                    .result(copyList(result))
                    .filter(copyList(filter))
                    .sort(copyList(sort))
                    .build();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonPropertyOrder(alphabetic = true)
    public static class ApiContractBO {

        private String existingErrorMapper;

        private String contextSymbol;

        private String basePath;

        ApiContractBO snapshot() {
            return ApiContractBO.builder()
                    .existingErrorMapper(existingErrorMapper)
                    .contextSymbol(contextSymbol)
                    .basePath(basePath)
                    .build();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonPropertyOrder(alphabetic = true)
    public static class EventsBO {

        private boolean enabled;

        EventsBO snapshot() {
            return EventsBO.builder().enabled(enabled).build();
        }
    }

    /**
     * Returns a structural copy. Callers cannot mutate the original lists through the copy.
     *
     * @return detached config
     */
    public CodegenConfigBO snapshot() {
        return CodegenConfigBO.builder()
                .configVersion(configVersion)
                .profile(profile)
                .basePackage(basePackage)
                .domain(domain)
                .outputRoot(outputRoot)
                .input(input == null ? null : input.snapshot())
                .roots(copyMap(roots))
                .tables(copyList(tables))
                .artifacts(copyList(artifacts))
                .existingTypes(copyMap(existingTypes))
                .fieldPolicies(fieldPolicies == null ? null : fieldPolicies.snapshot())
                .apiContract(apiContract == null ? null : apiContract.snapshot())
                .events(events == null ? null : events.snapshot())
                .build();
    }

    static <T> List<T> copyList(List<T> source) {
        return source == null ? new ArrayList<>() : new ArrayList<>(source);
    }

    static <K, V> Map<K, V> copyMap(Map<K, V> source) {
        return source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
    }
}
