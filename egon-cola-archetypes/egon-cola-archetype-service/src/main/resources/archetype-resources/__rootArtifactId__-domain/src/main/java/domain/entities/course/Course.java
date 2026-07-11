#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.entities.course;

import ${package}.common.constants.ErrorCodes;
import ${package}.common.exception.BizException;
import ${package}.domain.common.EvaluationDomainErrorCode;
import ${package}.domain.common.EvaluationDomainException;
import ${package}.domain.enums.CourseStatus;
import ${package}.domain.vos.course.CourseCode;

public class Course {

    private String id;

    private CourseCode code;

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

    public static Course create(String id, CourseCode code, String name, int credit) {
        if (isBlank(id) || code == null || isBlank(name) || credit < 1) {
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
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
