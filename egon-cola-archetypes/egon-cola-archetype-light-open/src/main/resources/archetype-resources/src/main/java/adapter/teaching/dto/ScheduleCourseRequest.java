package ${package}.adapter.teaching.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ScheduleCourseRequest(@NotNull LocalDateTime startsAt, @NotNull LocalDateTime endsAt) {
}
