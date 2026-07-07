package top.egon.cola.component.common.model.query;

import java.io.Serial;
import java.io.Serializable;

/**
 * Common sorting query fragment.
 */
public class SortQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String sortBy;

    private String sortDirection;

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }
}
