package ${package}.domain.teaching.vos;

public record CourseCode(String value) {
    public CourseCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("course code must not be blank");
        }
    }
}
