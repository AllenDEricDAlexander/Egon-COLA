package top.egon.cola.component.common.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 树结构工具，支持将扁平列表组装为树。
 */
public final class Trees {

    private Trees() {
    }

    public static <T, ID> List<T> build(Collection<T> nodes,
                                        Function<T, ID> idGetter,
                                        Function<T, ID> parentIdGetter,
                                        BiConsumer<T, List<T>> childrenSetter) {
        if (Collections2.isEmpty(nodes)) {
            return List.of();
        }
        Map<ID, T> nodeMap = new LinkedHashMap<>();
        Map<ID, List<T>> childrenMap = new LinkedHashMap<>();
        for (T node : nodes) {
            ID id = idGetter.apply(node);
            nodeMap.put(id, node);
            childrenMap.putIfAbsent(id, new ArrayList<>());
        }
        List<T> roots = new ArrayList<>();
        for (T node : nodes) {
            ID parentId = parentIdGetter.apply(node);
            if (parentId == null || !nodeMap.containsKey(parentId) || Objects.equals(idGetter.apply(node), parentId)) {
                roots.add(node);
                continue;
            }
            childrenMap.computeIfAbsent(parentId, key -> new ArrayList<>()).add(node);
        }
        for (T node : nodes) {
            childrenSetter.accept(node, childrenMap.getOrDefault(idGetter.apply(node), List.of()));
        }
        return roots;
    }
}
