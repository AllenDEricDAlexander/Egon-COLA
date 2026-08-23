package ${package}.adapter.teaching.facade.impl;

import ${package}.application.teaching.command.CreateSchoolClassCommand;
import ${package}.application.teaching.command.ScheduleCourseCommand;
import ${package}.application.teaching.manage.SchoolClassManage;
import ${package}.application.teaching.manage.TeachingUseCaseException;
import ${package}.application.teaching.query.GetSchoolClassQuery;
import ${package}.application.teaching.result.SchoolClassResult;
import ${package}.facade.teaching.SchoolClassFacade;
import ${package}.facade.teaching.dto.CreateSchoolClassDTO;
import ${package}.facade.teaching.dto.ScheduleCourseDTO;
import ${package}.facade.teaching.dto.SchoolClassDetailDTO;
import ${package}.facade.teaching.exceptions.TeachingFacadeException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SchoolClassFacadeImpl implements SchoolClassFacade {
    private final SchoolClassManage schoolClassManage;

    @Override
    public SchoolClassDetailDTO createSchoolClass(CreateSchoolClassDTO request) {
        try {
            return toDto(schoolClassManage.create(new CreateSchoolClassCommand(
                    request.name(), request.semester(), request.operatorId(), request.requestId())));
        } catch (TeachingUseCaseException exception) {
            throw publicFailure(exception);
        }
    }

    @Override
    public SchoolClassDetailDTO scheduleCourse(ScheduleCourseDTO request) {
        try {
            return toDto(schoolClassManage.schedule(new ScheduleCourseCommand(
                    parseId(request.schoolClassId(), "schoolClassId"), parseId(request.courseId(), "courseId"),
                    request.startsAt(), request.endsAt(),
                    request.operatorId(), request.requestId())));
        } catch (TeachingUseCaseException exception) {
            throw publicFailure(exception);
        }
    }

    @Override
    public SchoolClassDetailDTO getSchoolClass(String schoolClassId) {
        try {
            return toDto(schoolClassManage.get(new GetSchoolClassQuery(parseId(schoolClassId, "schoolClassId"))));
        } catch (TeachingUseCaseException exception) {
            throw publicFailure(exception);
        }
    }

    private static SchoolClassDetailDTO toDto(SchoolClassResult result) {
        return new SchoolClassDetailDTO(
                Long.toString(result.id()), result.name(), result.semester(), result.status(), result.scheduleCount());
    }

    private static long parseId(String value, String field) {
        try {
            long id = Long.parseLong(value);
            if (id <= 0) throw new NumberFormatException(field);
            return id;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(field + " must be a positive decimal Long", exception);
        }
    }

    private static TeachingFacadeException publicFailure(TeachingUseCaseException exception) {
        return new TeachingFacadeException(exception.getCode(), exception.getMessage());
    }
}
