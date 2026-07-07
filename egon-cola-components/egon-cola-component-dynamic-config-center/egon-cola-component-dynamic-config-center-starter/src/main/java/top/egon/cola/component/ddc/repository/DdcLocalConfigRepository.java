package top.egon.cola.component.ddc.repository;

import top.egon.cola.component.ddc.model.vo.DdcFieldBinding;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class DdcLocalConfigRepository {

    private final ConcurrentMap<String, CopyOnWriteArrayList<DdcFieldBinding>> bindings = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, Long> versions = new ConcurrentHashMap<>();

    public void addBinding(String key, DdcFieldBinding binding) {
        bindings.computeIfAbsent(key, ignored -> new CopyOnWriteArrayList<>()).add(binding);
    }

    public List<DdcFieldBinding> bindings(String key) {
        List<DdcFieldBinding> current = bindings.get(key);
        return current == null ? Collections.emptyList() : List.copyOf(current);
    }

    public Long version(String key) {
        return versions.get(key);
    }

    public void updateVersion(String key, long version) {
        versions.put(key, version);
    }
}
