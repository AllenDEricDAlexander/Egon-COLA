package top.egon.cola.archetype.source.web.infrastructure.teaching.cache;

import top.egon.cola.archetype.source.web.domain.teaching.client.SchoolClassCachePort;
import top.egon.cola.archetype.source.web.domain.teaching.entities.SchoolClass;
import top.egon.cola.archetype.source.web.domain.teaching.vos.SchoolClassId;

import java.util.Optional;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemorySchoolClassCache implements SchoolClassCachePort {
    private final ConcurrentHashMap<String, SchoolClass> values = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<String> evictedKeys = new CopyOnWriteArrayList<>();
    @Override public Optional<SchoolClass> findById(Long gradeId, SchoolClassId id) {
        return Optional.ofNullable(values.get(key(gradeId, id)));
    }
    @Override public void put(SchoolClass value) {
        values.put(key(value.gradeId(), value.id()), value);
    }
    @Override public void evict(Long gradeId, SchoolClassId id) {
        String key = key(gradeId, id);
        values.remove(key);
        evictedKeys.add(key);
    }
    public List<String> evictedKeys() { return List.copyOf(evictedKeys); }
    public void clearObservations() { evictedKeys.clear(); }
    private static String key(Long gradeId, SchoolClassId id) {
        return gradeId + ":" + id.value();
    }
}
