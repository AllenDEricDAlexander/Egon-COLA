package top.egon.cola.component.common.model.query;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Common time range query fragment.
 */
public class TimeRangeQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
}
