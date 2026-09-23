package top.egon.cola.component.codegen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.egon.cola.component.codegen.model.CodegenPlanBO;
import top.egon.cola.component.codegen.update.GenerationApplyService;
import top.egon.cola.component.codegen.update.GenerationPlanService;
import top.egon.cola.component.codegen.update.GenerationStateRepository;
import top.egon.cola.component.codegen.validation.OutputPathValidator;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerationUpdateTest {

    private final GenerationStateRepository states = new GenerationStateRepository();

    private final OutputPathValidator paths = new OutputPathValidator();

    private final GenerationPlanService plans = new GenerationPlanService(states, paths);

    private final GenerationApplyService apply = new GenerationApplyService(states, paths);

    @Test
    void repeatedApplyIsStableAndPartialSelectionPreservesCustomFiles(@TempDir Path root) throws Exception {
        CodegenPlanBO first = plan(root, List.of(file("src/OrdersPO.java", "po", "v1"), file("src/OrdersDAO.java", "dao", "v1")),
                "schema-1", "template-1", "component-1", List.of());
        assertEquals(GenerationApplyService.APPLIED, applyPlan(root, first, Set.of(), current(root, first), -1).status());
        assertEquals(GenerationApplyService.NO_CHANGE, apply.recover(root, "rollback").status());
        byte[] po = Files.readAllBytes(root.resolve("src/OrdersPO.java"));
        byte[] dao = Files.readAllBytes(root.resolve("src/OrdersDAO.java"));
        CodegenPlanBO second = plan(root, List.of(file("src/OrdersPO.java", "po", "v1"), file("src/OrdersDAO.java", "dao", "v1")),
                "schema-1", "template-1", "component-1", List.of());
        assertEquals(GenerationApplyService.NO_CHANGE, applyPlan(root, second, Set.of(), current(root, second), -1).status());
        assertArrayEquals(po, Files.readAllBytes(root.resolve("src/OrdersPO.java")));
        assertArrayEquals(dao, Files.readAllBytes(root.resolve("src/OrdersDAO.java")));

        Files.writeString(root.resolve("src/OrdersDAO.java"), "human dao");
        CodegenPlanBO mapperOnly = plan(root, List.of(file("src/OrdersDAO.xml", "mapper-xml", "<mapper/>")),
                "schema-2", "template-1", "component-1", List.of("dao"));
        assertEquals(GenerationApplyService.APPLIED, applyPlan(root, mapperOnly, Set.of(), current(root, mapperOnly), -1).status());
        assertEquals("human dao", Files.readString(root.resolve("src/OrdersDAO.java")));
        assertTrue(mapperOnly.getPendingImpacts().stream().anyMatch(impact -> "dao".equals(impact.getArtifact())));
        GenerationStateRepository.StateBO state = states.load(root);
        assertTrue(state.getArtifacts().stream().filter(artifact -> "src/OrdersDAO.java".equals(artifact.getPath()))
                .allMatch(artifact -> "schema-1".equals(artifact.getLastGeneratedInput())));
    }

    @Test
    void failedWriteRollsBackAndPreservesLaterHumanEdits(@TempDir Path root) throws Exception {
        CodegenPlanBO plan = plan(root, List.of(file("src/A.java", "po", "A"), file("src/B.java", "dao", "B")),
                "schema", "template", "component", List.of());
        assertEquals(GenerationApplyService.RECOVERY_REQUIRED, applyPlan(root, plan, Set.of(), current(root, plan), 1).status());
        assertTrue(Files.exists(root.resolve("src/A.java")));
        assertFalse(Files.exists(root.resolve("src/B.java")));
        Files.writeString(root.resolve("src/A.java"), "human after crash");
        assertEquals(GenerationApplyService.CONFLICT, apply.recover(root, "rollback").status());
        assertEquals("human after crash", Files.readString(root.resolve("src/A.java")));
    }

    @Test
    void rejectsEscapeStalePlansAndUnapprovedDestruction(@TempDir Path root) throws Exception {
        Path outside = Files.createTempDirectory("egon-outside");
        Files.createDirectories(root.resolve("nested"));
        Files.createSymbolicLink(root.resolve("nested/escape"), outside);
        OutputPathValidator.PathValidationException escaped = org.junit.jupiter.api.Assertions.assertThrows(
                OutputPathValidator.PathValidationException.class, () -> paths.check(root, "nested/escape/file.txt"));
        assertEquals(OutputPathValidator.PATH_ESCAPE, escaped.getCode());

        CodegenPlanBO plan = plan(root, List.of(file("src/A.java", "po", "A")), "schema", "template", "component", List.of());
        var changedTemplate = current(root, plan);
        changedTemplate = new GenerationApplyService.CurrentInputs(changedTemplate.inputFingerprint(),
                changedTemplate.configFingerprint(), "other-template", changedTemplate.componentFingerprint(), changedTemplate.renderedFiles());
        assertEquals(GenerationApplyService.STALE_PLAN, applyPlan(root, plan, Set.of(), changedTemplate, -1).status());
        plan.getFiles().get(0).setOperation("DELETE");
        assertEquals(GenerationApplyService.STALE_PLAN, applyPlan(root, plan, Set.of(), current(root, plan), -1).status());
        plan.getFiles().get(0).setOperation("ADD");
        plan.getFiles().get(0).setCandidatePath("../evil.txt");
        assertEquals(GenerationApplyService.STALE_PLAN, applyPlan(root, plan, Set.of(), current(root, plan), -1).status());
    }

    @Test
    void refusesUnmanagedAndLateCreatedFiles(@TempDir Path root) throws Exception {
        Path target = root.resolve("src/OrdersPO.java");
        Files.createDirectories(target.getParent());
        Files.writeString(target, "human code");
        CodegenPlanBO unmanaged = plan(root, List.of(file("src/OrdersPO.java", "po", "generated")),
                "schema", "template", "component", List.of());
        assertEquals("CONFLICT", unmanaged.getFiles().get(0).getOperation());
        assertEquals(GenerationApplyService.CONFLICT, applyPlan(root, unmanaged, Set.of(), current(root, unmanaged), -1).status());
        assertEquals("human code", Files.readString(target));

        Files.write(target, Files.readAllBytes(root.resolve(unmanaged.getFiles().get(0).getCandidatePath())));
        CodegenPlanBO identicalUnmanaged = plan(root, List.of(file("src/OrdersPO.java", "po", "generated")),
                "schema", "template", "component", List.of());
        assertEquals("CONFLICT", identicalUnmanaged.getFiles().get(0).getOperation());
        assertEquals(GenerationApplyService.CONFLICT,
                applyPlan(root, identicalUnmanaged, Set.of(), current(root, identicalUnmanaged), -1).status());

        Files.delete(target);
        CodegenPlanBO plannedAdd = plan(root, List.of(file("src/OrdersPO.java", "po", "generated")),
                "schema", "template", "component", List.of());
        Files.writeString(target, "created after plan");
        assertEquals(GenerationApplyService.CONFLICT, applyPlan(root, plannedAdd, Set.of(), current(root, plannedAdd), -1).status());
        assertEquals("created after plan", Files.readString(target));
    }

    @Test
    void validatesEveryTargetBeforeWritingAny(@TempDir Path root) throws Exception {
        CodegenPlanBO plan = plan(root, List.of(file("src/A.java", "po", "A"), file("src/B.java", "dao", "B")),
                "schema", "template", "component", List.of());
        Path second = root.resolve("src/B.java");
        Files.createDirectories(second.getParent());
        Files.writeString(second, "human B");
        assertEquals(GenerationApplyService.CONFLICT, applyPlan(root, plan, Set.of(), current(root, plan), -1).status());
        assertFalse(Files.exists(root.resolve("src/A.java")));
        assertEquals("human B", Files.readString(second));
    }

    @Test
    void staleStateOrMissingOwnedFileCannotBeRecreated(@TempDir Path root) throws Exception {
        CodegenPlanBO first = plan(root, List.of(file("src/A.java", "po", "A")),
                "schema", "template", "component", List.of());
        CodegenPlanBO second = plan(root, List.of(file("src/B.java", "dao", "B")),
                "schema", "template", "component", List.of());
        assertEquals(GenerationApplyService.APPLIED, applyPlan(root, first, Set.of(), current(root, first), -1).status());
        assertEquals(GenerationApplyService.STALE_PLAN, applyPlan(root, second, Set.of(), current(root, second), -1).status());
        assertFalse(Files.exists(root.resolve("src/B.java")));

        Files.delete(root.resolve("src/A.java"));
        CodegenPlanBO missing = plan(root, List.of(file("src/A.java", "po", "A")),
                "schema", "template", "component", List.of());
        assertEquals("CONFLICT", missing.getFiles().get(0).getOperation());
        assertEquals(GenerationApplyService.CONFLICT, applyPlan(root, missing, Set.of(), current(root, missing), -1).status());
        assertFalse(Files.exists(root.resolve("src/A.java")));
    }

    @Test
    void recoveryHandlesJournalWrittenBeforeTargetReplacement(@TempDir Path root) throws Exception {
        CodegenPlanBO plan = plan(root, List.of(file("src/A.java", "po", "A")),
                "schema", "template", "component", List.of());
        Path marker = root.resolve(".egon/codegen/journal/src/A.java.written");
        states.replace(marker, Files.readAllBytes(root.resolve(plan.getFiles().get(0).getCandidatePath())));
        GenerationStateRepository.StateBO state = states.load(root);
        state.setJournal(CodegenPlanBO.JournalBO.builder().phase("WRITING")
                .planId(plan.getPlanId()).paths(List.of("src/A.java")).build());
        states.save(root, state);

        assertEquals(GenerationApplyService.APPLIED, apply.recover(root, "rollback").status());
        assertFalse(Files.exists(root.resolve("src/A.java")));
        assertEquals(GenerationApplyService.NO_CHANGE, apply.recover(root, "rollback").status());
    }

    private static GenerationApplyService.CurrentInputs current(Path root, CodegenPlanBO plan) throws Exception {
        List<GenerationPlanService.RenderedFile> rendered = new ArrayList<>();
        for (CodegenPlanBO.FileChangeBO file : plan.getFiles()) {
            if (file.getCandidatePath() != null && !file.getCandidatePath().startsWith("../")) {
                rendered.add(new GenerationPlanService.RenderedFile(file.getPath(), file.getArtifact(), file.getTable(),
                        Files.readAllBytes(root.resolve(file.getCandidatePath()))));
            }
        }
        return new GenerationApplyService.CurrentInputs(plan.getInputFingerprint(), plan.getConfigFingerprint(),
                plan.getTemplateSetVersion(), plan.getComponentFingerprint(), rendered);
    }

    private GenerationApplyService.Result applyPlan(Path root, CodegenPlanBO plan, Set<String> actions,
                                                    GenerationApplyService.CurrentInputs current, int failAfterWrites) {
        return apply.apply(root, plan, actions, () -> current, failAfterWrites);
    }

    private CodegenPlanBO plan(Path root, List<GenerationPlanService.RenderedFile> requested,
                               String schema, String template, String component, List<String> pending) {
        return plans.plan(root, requested, top.egon.cola.component.codegen.model.CodegenProfileEnum.LIGHT,
                schema, "test-configuration", null, template, component, pending);
    }

    private static GenerationPlanService.RenderedFile file(String path, String artifact, String body) {
        return new GenerationPlanService.RenderedFile(path, artifact, "orders", body.getBytes(StandardCharsets.UTF_8));
    }
}
