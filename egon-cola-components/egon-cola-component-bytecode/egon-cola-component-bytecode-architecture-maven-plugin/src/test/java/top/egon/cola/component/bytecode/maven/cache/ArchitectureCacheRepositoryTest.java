package top.egon.cola.component.bytecode.maven.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.egon.cola.component.bytecode.core.cache.ClassMetadataCacheEntry;
import top.egon.cola.component.bytecode.core.cache.ClassMetadataCacheKey;
import top.egon.cola.component.bytecode.core.classfile.ClassMetadata;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureCacheRepositoryTest {

    @TempDir
    Path directory;

    @Test
    void contentAndConfigurationControlHitsAndCorruptionIsRecovered() throws Exception {
        byte[] bytes = {1, 2, 3};
        ClassMetadataCacheKey key = ClassMetadataCacheKey.create(bytes, "schema-1", "9.9.1", "config-a");
        ClassMetadataCacheKey same = ClassMetadataCacheKey.create(bytes, "schema-1", "9.9.1", "config-a");
        ClassMetadataCacheKey byteChanged = ClassMetadataCacheKey.create(
                new byte[]{1, 2, 4}, "schema-1", "9.9.1", "config-a");
        ClassMetadataCacheKey configChanged = ClassMetadataCacheKey.create(
                bytes, "schema-1", "9.9.1", "config-b");
        ClassMetadataCacheKey schemaChanged = ClassMetadataCacheKey.create(
                bytes, "schema-2", "9.9.1", "config-a");
        ArchitectureCacheRepository repository = new ArchitectureCacheRepository(
                directory, new ObjectMapper(), ignored -> { });
        ClassMetadata metadata = new ClassMetadata(
                "sample", "sample.Type", "java.lang.Object", Set.of(), Set.of(), false, List.of());

        repository.store(new ClassMetadataCacheEntry(key, metadata));

        assertEquals(metadata, repository.find(same).orElseThrow().metadata());
        assertNotEquals(key, byteChanged);
        assertNotEquals(key, configChanged);
        assertNotEquals(key, schemaChanged);
        assertTrue(repository.find(byteChanged).isEmpty());

        Files.writeString(repository.pathFor(key), "not-json");
        assertTrue(repository.find(key).isEmpty());
        assertTrue(Files.notExists(repository.pathFor(key)));
    }
}
