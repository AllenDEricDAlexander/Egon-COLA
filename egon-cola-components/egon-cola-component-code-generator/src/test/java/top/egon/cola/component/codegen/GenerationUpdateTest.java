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
        CodegenPlanBO first = plans.plan(root, List.of(file("src/OrdersPO.java", "po", "v1"), file("src/OrdersDAO.java", "dao", "v1")),
                "schema-1", "template-1", "component-1", List.of());
        assertEquals(GenerationApplyService.APPLIED, apply.apply(root, first, Set.of(), "template-1", "component-1", -1).status());
        byte[] po = Files.readAllBytes(root.resolve("src/OrdersPO.java"));
        byte[] dao = Files.readAllBytes(root.resolve("src/OrdersDAO.java"));
        CodegenPlanBO second = plans.plan(root, List.of(file("src/OrdersPO.java", "po", "v1"), file("src/OrdersDAO.java", "dao", "v1")),
                "schema-1", "template-1", "component-1", List.of());
        assertEquals(GenerationApplyService.NO_CHANGE, apply.apply(root, second, Set.of(), "template-1", "component-1", -1).status());
        assertArrayEquals(po, Files.readAllBytes(root.resolve("src/OrdersPO.java")));
        assertArrayEquals(dao, Files.readAllBytes(root.resolve("src/OrdersDAO.java")));

        Files.writeString(root.resolve("src/OrdersDAO.java"), "human dao");
        CodegenPlanBO mapperOnly = plans.plan(root, List.of(file("src/OrdersDAO.xml", "mapper-xml", "<mapper/>")),
                "schema-2", "template-1", "component-1", List.of("dao"));
        assertEquals(GenerationApplyService.APPLIED, apply.apply(root, mapperOnly, Set.of(), "template-1", "component-1", -1).status());
        assertEquals("human dao", Files.readString(root.resolve("src/OrdersDAO.java")));
        assertTrue(mapperOnly.getPendingImpacts().stream().anyMatch(impact -> "dao".equals(impact.getArtifact())));
        GenerationStateRepository.StateBO state = states.load(root);
        assertTrue(state.getArtifacts().stream().filter(artifact -> "src/OrdersDAO.java".equals(artifact.getPath()))
                .allMatch(artifact -> "schema-1".equals(artifact.getLastGeneratedInput())));
    }

    @Test
    void failedWriteRollsBackAndPreservesLaterHumanEdits(@TempDir Path root) throws Exception {
        CodegenPlanBO plan = plans.plan(root, List.of(file("src/A.java", "po", "A"), file("src/B.java", "dao", "B")),
                "schema", "template", "component", List.of());
        assertEquals(GenerationApplyService.RECOVERY_REQUIRED, apply.apply(root, plan, Set.of(), "template", "component", 1).status());
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

        CodegenPlanBO plan = plans.plan(root, List.of(file("src/A.java", "po", "A")), "schema", "template", "component", List.of());
        assertEquals(GenerationApplyService.STALE_PLAN, apply.apply(root, plan, Set.of(), "other-template", "component", -1).status());
        plan.getFiles().get(0).setOperation("DELETE");
        assertEquals(GenerationApplyService.DESTRUCTIVE_ACTION, apply.apply(root, plan, Set.of(), "template", "component", -1).status());
        plan.getFiles().get(0).setOperation("ADD");
        plan.getFiles().get(0).setCandidatePath("../evil.txt");
        assertEquals(OutputPathValidator.PATH_ESCAPE, apply.apply(root, plan, Set.of(), "template", "component", -1).status());
    }

    private static GenerationPlanService.RenderedFile file(String path, String artifact, String body) {
        return new GenerationPlanService.RenderedFile(path, artifact, "orders", body.getBytes(StandardCharsets.UTF_8));
    }
}
