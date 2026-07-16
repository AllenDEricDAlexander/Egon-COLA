package top.egon.cola.component.bytecode.core.cache;

import top.egon.cola.component.bytecode.core.classfile.ClassMetadata;

public record ClassMetadataCacheEntry(
        ClassMetadataCacheKey key,
        ClassMetadata metadata
) {
}
