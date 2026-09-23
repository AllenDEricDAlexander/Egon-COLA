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
import java.util.LinkedHashSet;
import java.util.function.Supplier;

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
                        Supplier<CurrentInputs> currentSupplier, int failAfterWrites) {
        if (!GenerationPlanService.hasValidId(plan) || currentSupplier == null) {
            return Result.blocked(STALE_PLAN, "plan format or content changed");
        }
        if (!outputRoot.toAbsolutePath().normalize().toString().equals(plan.getOutputRootBinding())) {
            return Result.blocked(STALE_PLAN, "output root binding does not match");
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
        Path lockPath = outputRoot.resolve(".egon/codegen/apply.lock");
        try {
            Files.createDirectories(lockPath.getParent());
            try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                channel.lock();
                CurrentInputs current;
                try {
                    current = currentSupplier.get();
                } catch (RuntimeException failure) {
                    return Result.blocked(STALE_PLAN, "generation inputs could not be revalidated");
                }
                if (current == null || !java.util.Objects.equals(current.inputFingerprint(), plan.getInputFingerprint())
                        || !java.util.Objects.equals(current.configFingerprint(), plan.getConfigFingerprint())
                        || !java.util.Objects.equals(current.templateFingerprint(), plan.getTemplateSetVersion())
                        || !java.util.Objects.equals(current.componentFingerprint(), plan.getComponentFingerprint())) {
                    return Result.blocked(STALE_PLAN, "generation inputs changed");
                }
                Result preflight = preflight(outputRoot, plan, current);
                if (preflight != null) {
                    return preflight;
                }
                if (writable.isEmpty()) {
                    return Result.blocked(NO_CHANGE, "no file changes");
                }
                return writeAll(outputRoot, plan, writable, failAfterWrites);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("apply lock failed", exception);
        }
    }

    private Result preflight(Path outputRoot, CodegenPlanBO plan, CurrentInputs current) throws IOException {
        GenerationStateRepository.StateBO state = stateRepository.load(outputRoot);
        if (!stateRepository.fingerprint(state).equals(plan.getStateFingerprint())) {
            return Result.blocked(STALE_PLAN, "generation state changed");
        }
        if (state.getJournal() != null && RECOVERY_REQUIRED.equals(state.getJournal().getPhase())) {
            return Result.blocked(RECOVERY_REQUIRED, "recover the interrupted generation first");
        }
        Map<String, GenerationPlanService.RenderedFile> rendered = new LinkedHashMap<>();
        for (GenerationPlanService.RenderedFile file : current.renderedFiles()) {
            if (rendered.putIfAbsent(file.relativePath(), file) != null) {
                return Result.blocked(STALE_PLAN, "duplicate rendered path");
            }
        }
        Map<String, CodegenPlanBO.ArtifactStateBO> owned = new LinkedHashMap<>();
        for (CodegenPlanBO.ArtifactStateBO artifact : state.getArtifacts()) {
            owned.put(artifact.getPath(), artifact);
        }
        Set<String> planned = new LinkedHashSet<>();
        for (CodegenPlanBO.FileChangeBO file : plan.getFiles()) {
            if (!planned.add(file.getPath())) {
                return Result.blocked(STALE_PLAN, "duplicate planned path");
            }
            paths.check(outputRoot, file.getPath());
            GenerationPlanService.RenderedFile candidate = rendered.get(file.getPath());
            if (candidate == null || !java.util.Objects.equals(candidate.artifact(), file.getArtifact())
                    || !java.util.Objects.equals(candidate.table(), file.getTable())
                    || !GenerationPlanService.sha256(candidate.bytes()).equals(file.getCandidateHash())) {
                return Result.blocked(STALE_PLAN, "rendered output changed: " + file.getPath());
            }
            CodegenPlanBO.ArtifactStateBO prior = owned.get(file.getPath());
            boolean priorPresent = prior != null && prior.isPreviousGeneratedHashPresent();
            if (priorPresent != file.isPreviousGeneratedHashPresent()
                    || (priorPresent && !java.util.Objects.equals(prior.getPreviousGeneratedHash(), file.getPreviousGeneratedHash()))) {
                return Result.blocked(STALE_PLAN, "file ownership changed: " + file.getPath());
            }
            Path target = outputRoot.resolve(file.getPath());
            boolean exists = Files.exists(target);
            if (exists != file.isExpectedDiskHashPresent()) {
                return Result.blocked(CONFLICT, "file presence changed: " + file.getPath());
            }
            if (exists && !GenerationPlanService.sha256(Files.readAllBytes(target)).equals(file.getExpectedDiskHash())) {
                return Result.blocked(CONFLICT, "file content changed: " + file.getPath());
            }
            if (exists && !priorPresent) {
                return Result.blocked(CONFLICT, "unmanaged file: " + file.getPath());
            }
        }
        return planned.equals(rendered.keySet()) ? null : Result.blocked(STALE_PLAN, "selected output set changed");
    }

    public Result recover(Path outputRoot, String mode) {
        Path lockPath = outputRoot.resolve(".egon/codegen/apply.lock");
        try {
            Files.createDirectories(lockPath.getParent());
            try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                channel.lock();
                return recoverLocked(outputRoot, mode);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("recovery lock failed", exception);
        }
    }

    private Result recoverLocked(Path outputRoot, String mode) {
        if (!"rollback".equals(mode)) {
            return Result.blocked(CONFLICT, "unsupported recovery mode");
        }
        GenerationStateRepository.StateBO state = stateRepository.load(outputRoot);
        CodegenPlanBO.JournalBO journal = state.getJournal();
        if (journal == null || journal.getPaths() == null) {
            return Result.blocked(NO_CHANGE, "no journal");
        }
        if (!RECOVERY_REQUIRED.equals(journal.getPhase()) && !"WRITING".equals(journal.getPhase())) {
            return Result.blocked(NO_CHANGE, "there is no interrupted generation");
        }
        List<String> conflicts = new ArrayList<>();
        for (String relative : journal.getPaths()) {
            paths.check(outputRoot, relative);
            paths.check(outputRoot, ".egon/codegen/backup/" + relative);
            paths.check(outputRoot, ".egon/codegen/journal/" + relative + ".written");
            Path target = outputRoot.resolve(relative);
            Path backup = backup(outputRoot, relative);
            try {
                byte[] current = Files.exists(target) ? Files.readAllBytes(target) : null;
                Path writtenMarker = outputRoot.resolve(".egon/codegen/journal").resolve(relative + ".written");
                byte[] written = Files.exists(writtenMarker) ? Files.readAllBytes(writtenMarker) : null;
                if (written == null || (current == null && Files.exists(backup))) {
                    conflicts.add(relative);
                    continue;
                }
                if ("rollback".equals(mode)) {
                    if (current != null && written != null && !java.util.Arrays.equals(current, written)
                            && (!Files.exists(backup) || !java.util.Arrays.equals(current, Files.readAllBytes(backup)))) {
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
                if ((disk != null) != file.isExpectedDiskHashPresent()
                        || (disk != null && !diskHash.equals(file.getExpectedDiskHash()))) {
                    return interrupted(state, outputRoot, plan, written, CONFLICT, file.getPath());
                }
                if (disk != null) {
                    stateRepository.replace(backup(outputRoot, file.getPath()), disk);
                }
                byte[] candidate = Files.readAllBytes(outputRoot.resolve(file.getCandidatePath()));
                if (!GenerationPlanService.sha256(candidate).equals(file.getCandidateHash())) {
                    return interrupted(state, outputRoot, plan, written, STALE_PLAN, file.getPath());
                }
                stateRepository.replace(outputRoot.resolve(".egon/codegen/journal").resolve(file.getPath() + ".written"), candidate);
                written.add(file.getPath());
                state.setJournal(CodegenPlanBO.JournalBO.builder().phase("WRITING").planId(plan.getPlanId()).paths(new ArrayList<>(written)).build());
                stateRepository.save(outputRoot, state);
                stateRepository.replace(target, candidate);
                count++;
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
            state.setVersionChecksumPrefix(plan.getVersionChecksumPrefix());
            state.setJournal(CodegenPlanBO.JournalBO.builder().phase("COMMITTED").planId(plan.getPlanId()).paths(written).build());
            stateRepository.save(outputRoot, state);
            log.info("applied {} files", written.size());
            return Result.blocked(APPLIED, "applied");
        } catch (IOException exception) {
            throw new IllegalStateException("apply failed", exception);
        }
    }

    private Result interrupted(GenerationStateRepository.StateBO state, Path outputRoot, CodegenPlanBO plan,
                               List<String> written, String status, String path) {
        state.setJournal(CodegenPlanBO.JournalBO.builder()
                .phase(written.isEmpty() ? "COMMITTED" : RECOVERY_REQUIRED)
                .planId(plan.getPlanId()).paths(new ArrayList<>(written)).build());
        stateRepository.save(outputRoot, state);
        return Result.blocked(written.isEmpty() ? status : RECOVERY_REQUIRED, path);
    }

    private static Path backup(Path outputRoot, String relative) {
        return outputRoot.resolve(".egon/codegen/backup").resolve(relative);
    }

    public record Result(String status, String detail) {
        static Result blocked(String status, String detail) {
            return new Result(status, detail);
        }
    }

    public record CurrentInputs(String inputFingerprint, String configFingerprint, String templateFingerprint,
                                String componentFingerprint, List<GenerationPlanService.RenderedFile> renderedFiles) {
    }
}
