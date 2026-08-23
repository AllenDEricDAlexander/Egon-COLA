package ${package}.domain.teaching.client;

import ${package}.domain.teaching.entities.SchoolClass;
import ${package}.domain.teaching.vos.SchoolClassId;

import java.util.Optional;

public interface SchoolClassCachePort {
    Optional<SchoolClass> findById(String gradeId, SchoolClassId id);
    void put(SchoolClass schoolClass);
    void evict(String gradeId, SchoolClassId id);
}
