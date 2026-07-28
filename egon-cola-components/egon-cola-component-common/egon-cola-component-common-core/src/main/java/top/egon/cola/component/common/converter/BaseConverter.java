package top.egon.cola.component.common.converter;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Base object converter contract for one-to-one and list conversions.
 *
 * @param <S> source type
 * @param <T> target type
 */
public interface BaseConverter<S, T> {

    T toTarget(S source);

    S toSource(T target);

    default List<T> toTargetList(List<S> sources) {
        if (sources == null || sources.isEmpty()) {
            return Collections.emptyList();
        }
        return sources.stream().map(this::toTarget).toList();
    }

    default List<S> toSourceList(List<T> targets) {
        if (targets == null || targets.isEmpty()) {
            return Collections.emptyList();
        }
        return targets.stream().map(this::toSource).toList();
    }

    default String map(Date date) {
        return date == null ? null : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }

    default Date map(String date) {
        try {
            return date == null ? null : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(date);
        } catch (Exception e) {
            return null;
        }
    }
}
