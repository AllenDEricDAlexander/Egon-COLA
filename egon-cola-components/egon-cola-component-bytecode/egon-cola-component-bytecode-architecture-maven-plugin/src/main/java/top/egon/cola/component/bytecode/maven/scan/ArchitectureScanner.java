package top.egon.cola.component.bytecode.maven.scan;

import com.fasterxml.jackson.databind.ObjectMapper;
import top.egon.cola.component.bytecode.core.architecture.ArchitectureGraph;
import top.egon.cola.component.bytecode.core.architecture.ArchitectureGraphBuilder;
import top.egon.cola.component.bytecode.core.cache.ClassMetadataCacheEntry;
import top.egon.cola.component.bytecode.core.cache.ClassMetadataCacheKey;
import top.egon.cola.component.bytecode.core.classfile.AsmClassMetadataReader;
import top.egon.cola.component.bytecode.core.classfile.ClassMetadata;
import top.egon.cola.component.bytecode.core.classfile.ClassMetadataReader;
import top.egon.cola.component.bytecode.maven.cache.ArchitectureCacheRepository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public final class ArchitectureScanner {

    public static final String PARSER_SCHEMA = "architecture-metadata-v1";
    public static final String ASM_BASELINE = "9.9.1";

    private final ObjectMapper objectMapper;
    private final Consumer<String> debug;
    private final ClassMetadataReader metadataReader;

    public ArchitectureScanner(ObjectMapper objectMapper, Consumer<String> debug) {
        this(objectMapper, debug, new AsmClassMetadataReader());
    }

    ArchitectureScanner(
            ObjectMapper objectMapper,
            Consumer<String> debug,
            ClassMetadataReader metadataReader
    ) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.debug = Objects.requireNonNull(debug, "debug");
        this.metadataReader = Objects.requireNonNull(metadataReader, "metadataReader");
    }

    public ArchitectureScanResult scan(ScanRequest request) throws IOException {
        ArchitectureCacheRepository cache = new ArchitectureCacheRepository(
                request.cacheDirectory(), objectMapper, debug);
        List<ClassMetadata> metadata = new ArrayList<>();
        int[] cacheHits = {0};
        for (ScanInput input : request.inputs()) {
            if (Files.notExists(input.path())) {
                debug.accept("Skipping absent architecture scan input " + input.path());
            } else if (Files.isDirectory(input.path())) {
                scanDirectory(input, request, cache, metadata, cacheHits);
            } else if (input.path().toString().endsWith(".jar")) {
                scanJar(input, request, cache, metadata, cacheHits);
            }
        }
        ArchitectureGraph graph = new ArchitectureGraphBuilder().build(metadata, request.layerResolver());
        return new ArchitectureScanResult(graph, metadata.size() - cacheHits[0], cacheHits[0]);
    }

    private void scanDirectory(
            ScanInput input,
            ScanRequest request,
            ArchitectureCacheRepository cache,
            List<ClassMetadata> metadata,
            int[] cacheHits
    ) throws IOException {
        try (Stream<Path> paths = Files.walk(input.path())) {
            List<Path> classFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".class"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
            for (Path classFile : classFiles) {
                read(input.module(), Files.readAllBytes(classFile), request, cache, metadata, cacheHits);
            }
        }
    }

    private void scanJar(
            ScanInput input,
            ScanRequest request,
            ArchitectureCacheRepository cache,
            List<ClassMetadata> metadata,
            int[] cacheHits
    ) throws IOException {
        try (JarFile jar = new JarFile(input.path().toFile())) {
            List<JarEntry> entries = jar.stream()
                    .filter(entry -> !entry.isDirectory() && entry.getName().endsWith(".class"))
                    .sorted(Comparator.comparing(JarEntry::getName))
                    .toList();
            for (JarEntry entry : entries) {
                try (InputStream stream = jar.getInputStream(entry)) {
                    read(input.module(), stream.readAllBytes(), request, cache, metadata, cacheHits);
                }
            }
        }
    }

    private void read(
            String module,
            byte[] bytes,
            ScanRequest request,
            ArchitectureCacheRepository cache,
            List<ClassMetadata> metadata,
            int[] cacheHits
    ) throws IOException {
        ClassMetadataCacheKey key = ClassMetadataCacheKey.create(
                bytes, PARSER_SCHEMA, ASM_BASELINE,
                request.scanConfigurationDigest() + "\u001f" + module);
        if (request.cacheEnabled()) {
            var cached = cache.find(key);
            if (cached.isPresent()) {
                metadata.add(cached.get().metadata());
                cacheHits[0]++;
                return;
            }
        }
        ClassMetadata parsed;
        try {
            parsed = metadataReader.read(module, bytes);
        } catch (RuntimeException exception) {
            throw new IOException("Unable to parse class file for module " + module, exception);
        }
        metadata.add(parsed);
        if (request.cacheEnabled()) {
            cache.store(new ClassMetadataCacheEntry(key, parsed));
        }
    }
}
