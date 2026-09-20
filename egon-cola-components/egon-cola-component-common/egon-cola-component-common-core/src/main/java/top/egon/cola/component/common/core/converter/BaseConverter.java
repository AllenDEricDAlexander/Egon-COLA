package top.egon.cola.component.common.core.converter;

import java.util.Collections;
import java.util.List;

/**
 * Bidirectional object converter contract for one-to-one and list conversions.
 *
 * @param <S> source type
 * @param <T> target type
 */
public interface BaseConverter<S, T> extends BaseForwardConverter<S, T> {

    T toTarget(S source);

    S toSource(T target);

    default List<S> toSourceList(List<T> targets) {
        if (targets == null || targets.isEmpty()) {
            return Collections.emptyList();
        }
        return targets.stream().map(this::toSource).toList();
    }
}
