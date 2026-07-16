package top.egon.cola.component.bytecode.benchmark;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureFinding;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureRule;
import top.egon.cola.component.bytecode.core.architecture.ArchitectureGraph;
import top.egon.cola.component.bytecode.core.architecture.ArchitectureGraphBuilder;
import top.egon.cola.component.bytecode.core.architecture.DefaultArchitectureRuleContext;
import top.egon.cola.component.bytecode.core.architecture.DefaultLayerResolver;
import top.egon.cola.component.bytecode.core.architecture.LayerMapping;
import top.egon.cola.component.bytecode.core.architecture.rule.BuiltInArchitectureRules;
import top.egon.cola.component.bytecode.core.classfile.AsmClassMetadataReader;
import top.egon.cola.component.bytecode.core.classfile.ClassMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class ArchitectureScanBenchmark {

    @Benchmark
    public ScanResult scanOneThousandClasses(ScanState state) {
        AsmClassMetadataReader reader = new AsmClassMetadataReader();
        List<ClassMetadata> metadata = new ArrayList<>(state.fixtures.size());
        for (ClassFixture fixture : state.fixtures) {
            metadata.add(reader.read(fixture.module, fixture.classBytes));
        }

        ArchitectureGraph graph = new ArchitectureGraphBuilder().build(
                metadata,
                new DefaultLayerResolver(new LayerMapping(Map.of(), Map.of()))
        );
        DefaultArchitectureRuleContext context = new DefaultArchitectureRuleContext(graph);
        List<ArchitectureFinding> findings = new ArrayList<>();
        for (ArchitectureRule rule : BuiltInArchitectureRules.all()) {
            findings.addAll(rule.evaluate(context));
        }
        return new ScanResult(graph.types().size(), graph.dependencies().size(), findings.size());
    }

    @State(Scope.Benchmark)
    public static class ScanState {

        private List<ClassFixture> fixtures;

        @Setup(Level.Trial)
        public void generateFixtures() {
            List<ClassFixture> generated = new ArrayList<>(1_000);
            ArchitectureClassLayer[] layers = ArchitectureClassLayer.values();
            for (int index = 0; index < 1_000; index++) {
                ArchitectureClassLayer layer = layers[index % layers.length];
                String internalName = "benchmark/" + layer.packageName + "/Generated" + index;
                generated.add(new ClassFixture(
                        "benchmark-" + layer.moduleSuffix,
                        generateClass(internalName, index)
                ));
            }
            fixtures = List.copyOf(generated);
        }

        private byte[] generateClass(String internalName, int index) {
            ClassWriter writer = new ClassWriter(0);
            writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
                    internalName, null, "java/lang/Object", null);
            writer.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL,
                    "identifier", "J", null, (long) index).visitEnd();

            MethodVisitor constructor = writer.visitMethod(
                    Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
            constructor.visitCode();
            constructor.visitVarInsn(Opcodes.ALOAD, 0);
            constructor.visitMethodInsn(Opcodes.INVOKESPECIAL,
                    "java/lang/Object", "<init>", "()V", false);
            constructor.visitInsn(Opcodes.RETURN);
            constructor.visitMaxs(1, 1);
            constructor.visitEnd();

            MethodVisitor method = writer.visitMethod(
                    Opcodes.ACC_PUBLIC, "identifier", "()J", null, null);
            method.visitCode();
            method.visitVarInsn(Opcodes.ALOAD, 0);
            method.visitFieldInsn(Opcodes.GETFIELD, internalName, "identifier", "J");
            method.visitInsn(Opcodes.LRETURN);
            method.visitMaxs(2, 1);
            method.visitEnd();
            writer.visitEnd();
            return writer.toByteArray();
        }
    }

    public record ScanResult(int classCount, int dependencyCount, int findingCount) {
    }

    private record ClassFixture(String module, byte[] classBytes) {
    }

    private enum ArchitectureClassLayer {
        DOMAIN("domain", "domain"),
        APPLICATION("application", "application"),
        INFRASTRUCTURE("infrastructure", "infrastructure"),
        ADAPTER("adapter", "adapter"),
        FACADE("facade", "facade"),
        STARTER("starter", "starter"),
        COMMON("common", "common");

        private final String packageName;
        private final String moduleSuffix;

        ArchitectureClassLayer(String packageName, String moduleSuffix) {
            this.packageName = packageName;
            this.moduleSuffix = moduleSuffix;
        }
    }
}
