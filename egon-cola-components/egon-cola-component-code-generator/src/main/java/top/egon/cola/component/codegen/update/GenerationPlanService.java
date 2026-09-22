package top.egon.cola.component.codegen.update;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.codegen.model.CodegenPlanBO;
import top.egon.cola.component.codegen.validation.OutputPathValidator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a reviewable plan. It stages candidates and does not replace project files.
 */
@Slf4j
@RequiredArgsConstructor
public class GenerationPlanService {

    public static final String CONFLICT = "CONFLICT";

    @Qualifier("generationStateRepository")
    private final GenerationStateRepository stateRepository;

    @Qualifier("outputPathValidator")
    private final OutputPathValidator paths;

    public CodegenPlanBO plan(Path outputRoot, List<RenderedFile> requested, String schemaFingerprint,
                              String templateFingerprint, String componentFingerprint, List<String> pendingArtifacts) {
        GenerationStateRepository.StateBO state = stateRepository.load(outputRoot);
        Map<String, CodegenPlanBO.ArtifactStateBO> previous = new LinkedHashMap<>();
        for (CodegenPlanBO.ArtifactStateBO artifact : state.getArtifacts()) {
            previous.put(artifact.getPath(), artifact);
        }
        List<CodegenPlanBO.FileChangeBO> files = new ArrayList<>();
        for (RenderedFile rendered : requested) {
            paths.check(outputRoot, rendered.relativePath());
            byte[] disk = readIfExists(outputRoot.resolve(rendered.relativePath()));
            String diskHash = disk == null ? "" : sha256(disk);
            CodegenPlanBO.ArtifactStateBO prior = previous.get(rendered.relativePath());
            String previousHash = prior == null ? "" : prior.getPreviousGeneratedHash();
            boolean previousPresent = prior != null && prior.isPreviousGeneratedHashPresent();
            String candidateHash = sha256(rendered.bytes());
            String operation;
            if (disk != null && previousPresent && !diskHash.equals(previousHash)) {
                operation = CONFLICT;
            } else if (disk != null && diskHash.equals(candidateHash)) {
                operation = "NO_CHANGE";
            } else if (disk == null) {
                operation = "ADD";
            } else {
                operation = "UPDATE";
            }
            String candidateRelative = ".egon/codegen/candidates/pending/" + rendered.relativePath();
            files.add(CodegenPlanBO.FileChangeBO.builder()
                    .path(rendered.relativePath())
                    .artifact(rendered.artifact())
                    .table(rendered.table())
                    .operation(operation)
                    .expectedDiskHash(diskHash)
                    .expectedDiskHashPresent(disk != null)
                    .previousGeneratedHash(previousHash)
                    .previousGeneratedHashPresent(previousPresent)
                    .candidateHash(candidateHash)
                    .candidateHashPresent(true)
                    .candidatePath(candidateRelative)
                    .build());
        }
        files.sort(Comparator.comparing(CodegenPlanBO.FileChangeBO::getPath));
        List<CodegenPlanBO.PendingImpactBO> impacts = new ArrayList<>();
        if (pendingArtifacts != null) {
            for (String artifact : pendingArtifacts) {
                impacts.add(CodegenPlanBO.PendingImpactBO.builder()
                        .artifact(artifact)
                        .reason("selected generation did not include this artifact")
                        .build());
            }
        }
        CodegenPlanBO plan = CodegenPlanBO.builder()
                .formatVersion(1)
                .profile(null)
                .outputRootBinding(outputRoot.toAbsolutePath().normalize().toString())
                .inputFingerprint(schemaFingerprint)
                .templateSetVersion(templateFingerprint)
                .componentFingerprint(componentFingerprint)
                .files(files)
                .pendingImpacts(impacts)
                .latestObservedSchema(schemaFingerprint)
                .build();
        plan.setPlanId(planId(plan));
        for (int index = 0; index < files.size(); index++) {
            RenderedFile rendered = find(requested, files.get(index).getPath());
            String candidateRelative = ".egon/codegen/candidates/" + plan.getPlanId() + "/" + rendered.relativePath();
            files.get(index).setCandidatePath(candidateRelative);
            files.get(index).setActionId(sha256((plan.getPlanId() + ":" + rendered.relativePath()).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            try {
                Path candidate = outputRoot.resolve(candidateRelative);
                paths.check(outputRoot, candidateRelative);
                stateRepository.replace(candidate, rendered.bytes());
            } catch (IOException exception) {
                throw new IllegalStateException("candidate could not be staged", exception);
            }
        }
        log.info("planned {} files for {}", files.size(), outputRoot.getFileName());
        return plan;
    }

    private static RenderedFile find(List<RenderedFile> requested, String path) {
        for (RenderedFile rendered : requested) {
            if (rendered.relativePath().equals(path)) {
                return rendered;
            }
        }
        throw new IllegalArgumentException(path);
    }

    static String planId(CodegenPlanBO plan) {
        StringBuilder canonical = new StringBuilder();
        canonical.append(plan.getFormatVersion()).append('|')
                .append(plan.getOutputRootBinding()).append('|')
                .append(plan.getInputFingerprint()).append('|')
                .append(plan.getTemplateSetVersion()).append('|')
                .append(plan.getComponentFingerprint()).append('|');
        for (CodegenPlanBO.FileChangeBO file : plan.getFiles()) {
            canonical.append(file.getPath()).append('|').append(file.getOperation()).append('|')
                    .append(file.getCandidateHash()).append('|').append(file.getExpectedDiskHash()).append('\n');
        }
        return sha256(canonical.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static byte[] readIfExists(Path path) {
        try {
            return Files.exists(path) ? Files.readAllBytes(path) : null;
        } catch (IOException exception) {
            throw new IllegalStateException(path.toString(), exception);
        }
    }

    public record RenderedFile(String relativePath, String artifact, String table, byte[] bytes) {
    }
}
