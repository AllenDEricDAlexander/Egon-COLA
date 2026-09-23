package top.egon.cola.component.codegen.update;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import top.egon.cola.component.codegen.model.CodegenPlanBO;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Local generation state. This is not a MyBatis repository and does not open a database.
 */
@Slf4j
public class GenerationStateRepository {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .setSerializationInclusion(JsonInclude.Include.ALWAYS);

    public StateBO load(Path outputRoot) {
        Path state = stateFile(outputRoot);
        if (!Files.exists(state)) {
            return StateBO.builder().formatVersion(1).artifacts(new ArrayList<>()).build();
        }
        try {
            StateBO loaded = MAPPER.readValue(state.toFile(), StateBO.class);
            if (loaded.getFormatVersion() == null || loaded.getFormatVersion() != 1) {
                throw new IllegalStateException("unsupported generation state format");
            }
            if (loaded.getArtifacts() == null) {
                loaded.setArtifacts(new ArrayList<>());
            }
            return loaded;
        } catch (IOException exception) {
            throw new IllegalStateException("generation state is unreadable", exception);
        }
    }

    public void save(Path outputRoot, StateBO state) {
        try {
            Path file = stateFile(outputRoot);
            Files.createDirectories(file.getParent());
            replace(file, MAPPER.writeValueAsBytes(state));
        } catch (IOException exception) {
            throw new IllegalStateException("generation state could not be saved", exception);
        }
    }

    public String fingerprint(StateBO state) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(MAPPER.writeValueAsBytes(state)));
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("generation state fingerprint failed", exception);
        }
    }

    public Path candidate(Path outputRoot, String planId, String relativePath) {
        return outputRoot.resolve(".egon/codegen/candidates").resolve(planId).resolve(relativePath).normalize();
    }

    public void replace(Path target, byte[] bytes) throws IOException {
        Files.createDirectories(target.getParent());
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        Files.write(temporary, bytes);
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    public static Path stateFile(Path outputRoot) {
        return outputRoot.resolve(".egon/codegen/state.json");
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    public static class StateBO {

        private Integer formatVersion;

        private String latestObservedSchema;

        private String versionChecksumPrefix;

        @Builder.Default
        private List<CodegenPlanBO.ArtifactStateBO> artifacts = new ArrayList<>();

        private CodegenPlanBO.JournalBO journal;
    }
}
