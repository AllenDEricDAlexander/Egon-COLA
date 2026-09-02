package top.egon.cola.archetype.source.serviceopen.domain.course.entities;

import top.egon.cola.archetype.source.serviceopen.domain.common.EvaluationDomainErrorCode;
import top.egon.cola.archetype.source.serviceopen.domain.common.EvaluationDomainException;
import top.egon.cola.archetype.source.serviceopen.domain.course.enums.CourseStatus;
import top.egon.cola.archetype.source.serviceopen.domain.course.vos.CourseCode;

public class Course {

    private Long id;

    private CourseCode code;

    private String name;

    private int credit;

    private CourseStatus status;

    public static Course create(Long id, CourseCode code, String name, int credit) {
        if (id == null || id <= 0 || code == null || isBlank(name) || credit < 1) {
            throw new EvaluationDomainException(
                    EvaluationDomainErrorCode.VALIDATION_FAILED,
                    "course id, code, name and positive credit are required");
        }
        Course course = new Course();
        course.setId(id);
        course.setCode(code);
        course.setName(name.trim());
        course.setCredit(credit);
        course.setStatus(CourseStatus.ACTIVE);
        return course;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public CourseCode getCode() {
        return code;
    }

    public void setCode(CourseCode code) {
        this.code = code;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCredit() {
        return credit;
    }

    public void setCredit(int credit) {
        this.credit = credit;
    }

    public CourseStatus getStatus() {
        return status;
    }

    public void setStatus(CourseStatus status) {
        this.status = status;
    }

    public boolean isActive() {
        return status == CourseStatus.ACTIVE;
    }
}
