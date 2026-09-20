package top.egon.cola.component.common.core.converter;

import java.util.List;

/**
 * One-way projection contract for mappings whose reverse direction cannot rebuild the source.
 *
 * @param <S> source type
 * @param <T> projected target type
 */
public interface BaseForwardConverter<S, T> {

    T toTarget(S source);

    default List<T> toTargetList(List<S> sources) {
        if (sources == null || sources.isEmpty()) {
            return List.of();
        }
        return sources.stream().map(this::toTarget).toList();
    }
}
