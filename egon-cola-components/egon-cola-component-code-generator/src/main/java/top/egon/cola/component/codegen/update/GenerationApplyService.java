package top.egon.cola.component.codegen.update;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.codegen.model.CodegenPlanBO;
import top.egon.cola.component.codegen.validation.OutputPathValidator;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Re-checks a plan and replaces only files still owned by the generator.
 */
@Slf4j
@RequiredArgsConstructor
public class GenerationApplyService {

    public static final String NO_CHANGE = "NO_CHANGE";

    public static final String APPLIED = "APPLIED";

    public static final String STALE_PLAN = "STALE_PLAN";

    public static final String DESTRUCTIVE_ACTION = "DESTRUCTIVE_ACTION";

    public static final String RECOVERY_REQUIRED = "RECOVERY_REQUIRED";

    public static final String CONFLICT = "CONFLICT";

    @Qualifier("generationStateRepository")
    private final GenerationStateRepository stateRepository;

    @Qualifier("outputPathValidator")
    private final OutputPathValidator paths;

    public Result apply(Path outputRoot, CodegenPlanBO plan, Set<String> acceptedActionIds,
                        String templateFingerprint, String componentFingerprint, int failAfterWrites) {
        if (!outputRoot.toAbsolutePath().normalize().toString().equals(plan.getOutputRootBinding())) {
            return Result.blocked(STALE_PLAN, "output root binding does not match");
        }
        if (!templateFingerprint.equals(plan.getTemplateSetVersion()) || !componentFingerprint.equals(plan.getComponentFingerprint())) {
            return Result.blocked(STALE_PLAN, "template or component fingerprint changed");
        }
        List<CodegenPlanBO.FileChangeBO> writable = new ArrayList<>();
        for (CodegenPlanBO.FileChangeBO file : plan.getFiles()) {
            if ("DELETE".equals(file.getOperation()) || "RENAME".equals(file.getOperation())) {
                if (acceptedActionIds == null || !acceptedActionIds.contains(file.getActionId())) {
                    return Result.blocked(DESTRUCTIVE_ACTION, file.getPath());
                }
            }
            if ("CONFLICT".equals(file.getOperation())) {
                return Result.blocked(CONFLICT, file.getPath());
            }
            if ("ADD".equals(file.getOperation()) || "UPDATE".equals(file.getOperation()) || "DELETE".equals(file.getOperation())) {
                writable.add(file);
            }
        }
        if (writable.isEmpty()) {
            return Result.blocked(NO_CHANGE, "no file changes");
        }
        Path lockPath = outputRoot.resolve(".egon/codegen/apply.lock");
        try {
            Files.createDirectories(lockPath.getParent());
            try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                channel.lock();
                return writeAll(outputRoot, plan, writable, failAfterWrites);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("apply lock failed", exception);
        }
    }

    public Result recover(Path outputRoot, String mode) {
        GenerationStateRepository.StateBO state = stateRepository.load(outputRoot);
        CodegenPlanBO.JournalBO journal = state.getJournal();
        if (journal == null || journal.getPaths() == null) {
            return Result.blocked(NO_CHANGE, "no journal");
        }
        List<String> conflicts = new ArrayList<>();
        for (String relative : journal.getPaths()) {
            Path target = outputRoot.resolve(relative);
            Path backup = backup(outputRoot, relative);
            try {
                byte[] current = Files.exists(target) ? Files.readAllBytes(target) : null;
                Path writtenMarker = outputRoot.resolve(".egon/codegen/journal").resolve(relative + ".written");
                byte[] written = Files.exists(writtenMarker) ? Files.readAllBytes(writtenMarker) : null;
                if ("rollback".equals(mode)) {
                    if (current != null && written != null && !java.util.Arrays.equals(current, written)) {
                        conflicts.add(relative);
                        continue;
                    }
                    if (Files.exists(backup)) {
                        stateRepository.replace(target, Files.readAllBytes(backup));
                    } else if (Files.exists(target)) {
                        Files.delete(target);
                    }
                }
            } catch (IOException exception) {
                throw new IllegalStateException(relative, exception);
            }
        }
        if (!conflicts.isEmpty()) {
            return Result.blocked(CONFLICT, String.join(",", conflicts));
        }
        state.setJournal(CodegenPlanBO.JournalBO.builder().phase("COMMITTED").paths(List.of()).build());
        stateRepository.save(outputRoot, state);
        return Result.blocked(APPLIED, "recovered");
    }

    private Result writeAll(Path outputRoot, CodegenPlanBO plan, List<CodegenPlanBO.FileChangeBO> writable, int failAfterWrites) {
        GenerationStateRepository.StateBO state = stateRepository.load(outputRoot);
        List<String> written = new ArrayList<>();
        state.setJournal(CodegenPlanBO.JournalBO.builder().phase("PREPARED").planId(plan.getPlanId()).paths(written).build());
        stateRepository.save(outputRoot, state);
        int count = 0;
        try {
            for (CodegenPlanBO.FileChangeBO file : writable) {
                paths.check(outputRoot, file.getPath());
                if (file.getCandidatePath() == null || !file.getCandidatePath().startsWith(".egon/codegen/candidates/" + plan.getPlanId() + "/")) {
                    return Result.blocked(OutputPathValidator.PATH_ESCAPE, file.getCandidatePath());
                }
                paths.check(outputRoot, file.getCandidatePath());
                Path target = outputRoot.resolve(file.getPath());
                byte[] disk = Files.exists(target) ? Files.readAllBytes(target) : null;
                String diskHash = disk == null ? "" : GenerationPlanService.sha256(disk);
                if (file.isExpectedDiskHashPresent() && !diskHash.equals(file.getExpectedDiskHash())) {
                    return Result.blocked(CONFLICT, file.getPath());
                }
                if (disk != null) {
                    stateRepository.replace(backup(outputRoot, file.getPath()), disk);
                }
                byte[] candidate = Files.readAllBytes(outputRoot.resolve(file.getCandidatePath()));
                if (!GenerationPlanService.sha256(candidate).equals(file.getCandidateHash())) {
                    return Result.blocked(STALE_PLAN, file.getPath());
                }
                stateRepository.replace(target, candidate);
                stateRepository.replace(outputRoot.resolve(".egon/codegen/journal").resolve(file.getPath() + ".written"), candidate);
                written.add(file.getPath());
                count++;
                state.setJournal(CodegenPlanBO.JournalBO.builder().phase("WRITING").planId(plan.getPlanId()).paths(new ArrayList<>(written)).build());
                stateRepository.save(outputRoot, state);
                if (failAfterWrites >= 0 && count >= failAfterWrites) {
                    state.setJournal(CodegenPlanBO.JournalBO.builder().phase(RECOVERY_REQUIRED).planId(plan.getPlanId())
                            .paths(new ArrayList<>(written)).build());
                    stateRepository.save(outputRoot, state);
                    return Result.blocked(RECOVERY_REQUIRED, file.getPath());
                }
            }
            Map<String, CodegenPlanBO.ArtifactStateBO> artifacts = new LinkedHashMap<>();
            for (CodegenPlanBO.ArtifactStateBO existing : state.getArtifacts()) {
                artifacts.put(existing.getPath(), existing);
            }
            for (CodegenPlanBO.FileChangeBO file : writable) {
                artifacts.put(file.getPath(), CodegenPlanBO.ArtifactStateBO.builder()
                        .path(file.getPath())
                        .artifact(file.getArtifact())
                        .logicalTable(file.getTable())
                        .lastGeneratedInput(plan.getInputFingerprint())
                        .previousGeneratedHash(file.getCandidateHash())
                        .previousGeneratedHashPresent(true)
                        .selected(true)
                        .build());
            }
            state.setArtifacts(new ArrayList<>(artifacts.values()));
            state.setLatestObservedSchema(plan.getLatestObservedSchema());
            state.setJournal(CodegenPlanBO.JournalBO.builder().phase("COMMITTED").planId(plan.getPlanId()).paths(written).build());
            stateRepository.save(outputRoot, state);
            log.info("applied {} files", written.size());
            return Result.blocked(APPLIED, "applied");
        } catch (IOException exception) {
            throw new IllegalStateException("apply failed", exception);
        }
    }

    private static Path backup(Path outputRoot, String relative) {
        return outputRoot.resolve(".egon/codegen/backup").resolve(relative);
    }

    public record Result(String status, String detail) {
        static Result blocked(String status, String detail) {
            return new Result(status, detail);
        }
    }
}
