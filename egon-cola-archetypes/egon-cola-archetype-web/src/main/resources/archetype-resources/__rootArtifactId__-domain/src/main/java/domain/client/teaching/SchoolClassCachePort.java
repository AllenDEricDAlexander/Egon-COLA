package ${package}.domain.client.teaching;

import ${package}.domain.entities.teaching.SchoolClass;
import ${package}.domain.vos.teaching.SchoolClassId;

import java.util.Optional;

public interface SchoolClassCachePort {
    Optional<SchoolClass> findById(SchoolClassId id);
    void put(SchoolClass schoolClass);
    void evict(SchoolClassId id);
}
