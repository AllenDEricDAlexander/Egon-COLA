package top.egon.cola.component.common.model.page;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Slice model for pagination scenarios where total count is not available.
 */
public class PageSlice<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<T> records = Collections.emptyList();

    private boolean hasNext;

    public static <T> PageSlice<T> of(List<T> records, boolean hasNext) {
        PageSlice<T> slice = new PageSlice<>();
        slice.records = records == null ? Collections.emptyList() : new ArrayList<>(records);
        slice.hasNext = hasNext;
        return slice;
    }

    public List<T> getRecords() {
        return records;
    }

    public boolean isHasNext() {
        return hasNext;
    }
}
