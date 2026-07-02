package top.egon.fable.domain.entities.course;

import top.egon.fable.common.constants.ErrorCodes;
import top.egon.fable.common.exception.BizException;
import top.egon.fable.domain.enums.CourseStatus;

public class Course {

    private String id;

    private String name;

    private int credit;

    private CourseStatus status;

    public static Course create(String id, String name, int credit) {
        if (isBlank(name)) {
            throw new BizException("course name must not be blank");
        }
        if (credit < 1) {
            throw new BizException("course credit must be greater than 0");
        }
        Course course = new Course();
        course.setId(id);
        course.setName(name);
        course.setCredit(credit);
        course.setStatus(CourseStatus.ACTIVE);
        return course;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
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
}
