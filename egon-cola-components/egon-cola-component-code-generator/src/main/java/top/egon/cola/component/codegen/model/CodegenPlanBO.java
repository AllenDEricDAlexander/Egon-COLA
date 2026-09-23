package top.egon.cola.component.codegen.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.codegen.validation.CodegenGroups;

import java.util.ArrayList;
import java.util.List;

/**
 * Machine contract shared by plan and apply. Version 2 binds all generation inputs.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder(alphabetic = true)
public class CodegenPlanBO {

    public static final int FORMAT_VERSION = 2;

    @NotNull(groups = CodegenGroups.Apply.class)
    private Integer formatVersion;

    @NotBlank(groups = CodegenGroups.Apply.class)
    private String planId;

    @JsonProperty("projectType")
    private CodegenProfileEnum profile;

    @NotBlank(groups = CodegenGroups.Apply.class)
    private String outputRootBinding;

    private String inputFingerprint;

    private String configFingerprint;

    private String stateFingerprint;

    private String templateSetVersion;

    private String componentFingerprint;

    @Builder.Default
    private List<FileChangeBO> files = new ArrayList<>();

    @Builder.Default
    private List<SchemaChangeBO> schemaChanges = new ArrayList<>();

    @Builder.Default
    private List<PendingImpactBO> pendingImpacts = new ArrayList<>();

    @Builder.Default
    private List<DiagnosticBO> diagnostics = new ArrayList<>();

    @Builder.Default
    private List<ArtifactStateBO> artifactStates = new ArrayList<>();

    private JournalBO journal;

    private String latestObservedSchema;

    private String versionChecksumPrefix;

    @JsonIgnore
    public boolean supportedFormat() {
        return formatVersion != null && formatVersion == FORMAT_VERSION;
    }

    public CodegenPlanBO snapshot() {
        List<FileChangeBO> copiedFiles = new ArrayList<>();
        if (files != null) {
            for (FileChangeBO file : files) {
                copiedFiles.add(file == null ? null : file.snapshot());
            }
        }
        List<SchemaChangeBO> copiedChanges = new ArrayList<>();
        if (schemaChanges != null) {
            for (SchemaChangeBO change : schemaChanges) {
                copiedChanges.add(change == null ? null : change.snapshot());
            }
        }
        List<PendingImpactBO> copiedImpacts = new ArrayList<>();
        if (pendingImpacts != null) {
            for (PendingImpactBO impact : pendingImpacts) {
                copiedImpacts.add(impact == null ? null : impact.snapshot());
            }
        }
        List<DiagnosticBO> copiedDiagnostics = new ArrayList<>();
        if (diagnostics != null) {
            for (DiagnosticBO diagnostic : diagnostics) {
                copiedDiagnostics.add(diagnostic == null ? null : diagnostic.snapshot());
            }
        }
        List<ArtifactStateBO> copiedStates = new ArrayList<>();
        if (artifactStates != null) {
            for (ArtifactStateBO state : artifactStates) {
                copiedStates.add(state == null ? null : state.snapshot());
            }
        }
        return CodegenPlanBO.builder()
                .formatVersion(formatVersion)
                .planId(planId)
                .profile(profile)
                .outputRootBinding(outputRootBinding)
                .inputFingerprint(inputFingerprint)
                .configFingerprint(configFingerprint)
                .stateFingerprint(stateFingerprint)
                .templateSetVersion(templateSetVersion)
                .componentFingerprint(componentFingerprint)
                .files(copiedFiles)
                .schemaChanges(copiedChanges)
                .pendingImpacts(copiedImpacts)
                .diagnostics(copiedDiagnostics)
                .artifactStates(copiedStates)
                .journal(journal == null ? null : journal.snapshot())
                .latestObservedSchema(latestObservedSchema)
                .versionChecksumPrefix(versionChecksumPrefix)
                .build();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonPropertyOrder(alphabetic = true)
    public static class FileChangeBO {

        private String actionId;

        private String path;

        private String artifact;

        @JsonProperty("logicalTable")
        private String table;

        private String operation;

        private String expectedDiskHash;

        private boolean expectedDiskHashPresent;

        private String previousGeneratedHash;

        private boolean previousGeneratedHashPresent;

        private String candidateHash;

        private boolean candidateHashPresent;

        @JsonProperty("candidateRelativePath")
        private String candidatePath;

        FileChangeBO snapshot() {
            return FileChangeBO.builder()
                    .actionId(actionId)
                    .path(path)
                    .artifact(artifact)
                    .table(table)
                    .operation(operation)
                    .expectedDiskHash(expectedDiskHash)
                    .expectedDiskHashPresent(expectedDiskHashPresent)
                    .previousGeneratedHash(previousGeneratedHash)
                    .previousGeneratedHashPresent(previousGeneratedHashPresent)
                    .candidateHash(candidateHash)
                    .candidateHashPresent(candidateHashPresent)
                    .candidatePath(candidatePath)
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
    public static class SchemaChangeBO {

        private String kind;

        private String logicalTable;

        private String column;

        private String beforeValue;

        private String afterValue;

        private boolean destructive;

        SchemaChangeBO snapshot() {
            return SchemaChangeBO.builder()
                    .kind(kind)
                    .logicalTable(logicalTable)
                    .column(column)
                    .beforeValue(beforeValue)
                    .afterValue(afterValue)
                    .destructive(destructive)
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
    public static class PendingImpactBO {

        private String artifact;

        private String logicalTable;

        private String path;

        private String reason;

        PendingImpactBO snapshot() {
            return PendingImpactBO.builder()
                    .artifact(artifact)
                    .logicalTable(logicalTable)
                    .path(path)
                    .reason(reason)
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
    public static class DiagnosticBO {

        private String code;

        private String jsonPointer;

        private String file;

        private Integer line;

        private String artifact;

        private String expected;

        private String actual;

        private String message;

        public static DiagnosticBO of(String code, String jsonPointer, String message) {
            return DiagnosticBO.builder()
                    .code(code)
                    .jsonPointer(jsonPointer)
                    .message(message)
                    .build();
        }

        DiagnosticBO snapshot() {
            return DiagnosticBO.builder()
                    .code(code)
                    .jsonPointer(jsonPointer)
                    .file(file)
                    .line(line)
                    .artifact(artifact)
                    .expected(expected)
                    .actual(actual)
                    .message(message)
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
    public static class ArtifactStateBO {

        private String path;

        private String artifact;

        private String logicalTable;

        private String lastGeneratedInput;

        private String previousGeneratedHash;

        private boolean previousGeneratedHashPresent;

        private boolean selected;

        ArtifactStateBO snapshot() {
            return ArtifactStateBO.builder()
                    .path(path)
                    .artifact(artifact)
                    .logicalTable(logicalTable)
                    .lastGeneratedInput(lastGeneratedInput)
                    .previousGeneratedHash(previousGeneratedHash)
                    .previousGeneratedHashPresent(previousGeneratedHashPresent)
                    .selected(selected)
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
    public static class JournalBO {

        private String phase;

        private String planId;

        @Builder.Default
        private List<String> paths = new ArrayList<>();

        JournalBO snapshot() {
            return JournalBO.builder()
                    .phase(phase)
                    .planId(planId)
                    .paths(CodegenConfigBO.copyList(paths))
                    .build();
        }
    }
}
