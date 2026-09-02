package top.egon.cola.archetype.source.webopen.infrastructure.teaching.cache;

import top.egon.cola.archetype.source.webopen.domain.teaching.client.GradeCachePort;
import top.egon.cola.archetype.source.webopen.domain.teaching.entities.Grade;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryGradeCache implements GradeCachePort {
    private final ConcurrentHashMap<Long, Grade> values = new ConcurrentHashMap<>();
    @Override public Optional<Grade> findById(Long id) { return Optional.ofNullable(values.get(id)); }
    @Override public void put(Grade grade) { values.put(grade.id(), grade); }
    @Override public void evict(Long id) { values.remove(id); }
}
