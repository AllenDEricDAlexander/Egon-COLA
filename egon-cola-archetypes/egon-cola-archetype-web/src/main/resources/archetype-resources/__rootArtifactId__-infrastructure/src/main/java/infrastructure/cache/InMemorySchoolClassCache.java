package ${package}.infrastructure.cache;

import ${package}.domain.client.teaching.SchoolClassCachePort;
import ${package}.domain.entities.teaching.SchoolClass;
import ${package}.domain.vos.teaching.SchoolClassId;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemorySchoolClassCache implements SchoolClassCachePort {
    private final ConcurrentHashMap<SchoolClassId, SchoolClass> values = new ConcurrentHashMap<>();
    @Override public Optional<SchoolClass> findById(SchoolClassId id) { return Optional.ofNullable(values.get(id)); }
    @Override public void put(SchoolClass value) { values.put(value.id(), value); }
    @Override public void evict(SchoolClassId id) { values.remove(id); }
}
