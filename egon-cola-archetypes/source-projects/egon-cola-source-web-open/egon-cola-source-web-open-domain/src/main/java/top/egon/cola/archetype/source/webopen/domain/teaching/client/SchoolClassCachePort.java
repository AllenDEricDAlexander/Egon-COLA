package top.egon.cola.archetype.source.webopen.domain.teaching.client;

import top.egon.cola.archetype.source.webopen.domain.teaching.entities.SchoolClass;
import top.egon.cola.archetype.source.webopen.domain.teaching.vos.SchoolClassId;

import java.util.Optional;

public interface SchoolClassCachePort {
    Optional<SchoolClass> findById(Long gradeId, SchoolClassId id);
    void put(SchoolClass schoolClass);
    void evict(Long gradeId, SchoolClassId id);
}
