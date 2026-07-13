package ${package}.infrastructure.teaching.cache;

import ${package}.domain.teaching.client.SchoolClassCachePort;
import ${package}.domain.teaching.entities.SchoolClass;
import ${package}.domain.teaching.vos.SchoolClassId;

import java.util.Optional;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemorySchoolClassCache implements SchoolClassCachePort {
    private final ConcurrentHashMap<SchoolClassId, SchoolClass> values = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<SchoolClassId> evictedKeys = new CopyOnWriteArrayList<>();
    @Override public Optional<SchoolClass> findById(SchoolClassId id) { return Optional.ofNullable(values.get(id)); }
    @Override public void put(SchoolClass value) { values.put(value.id(), value); }
    @Override public void evict(SchoolClassId id) {
        values.remove(id);
        evictedKeys.add(id);
    }
    public List<SchoolClassId> evictedKeys() { return List.copyOf(evictedKeys); }
    public void clearObservations() { evictedKeys.clear(); }
}
