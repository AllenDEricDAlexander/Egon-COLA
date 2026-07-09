package ${package}.adapter.teaching.facade.impl;

import ${package}.application.teaching.command.CreateSchoolClassCommand;
import ${package}.application.teaching.command.ScheduleCourseCommand;
import ${package}.application.teaching.manage.SchoolClassManage;
import ${package}.application.teaching.manage.TeachingUseCaseException;
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
                    request.schoolClassId(), request.courseId(), request.startsAt(), request.endsAt(),
                    request.operatorId(), request.requestId())));
        } catch (TeachingUseCaseException exception) {
            throw publicFailure(exception);
        }
    }

    private static SchoolClassDetailDTO toDto(SchoolClassResult result) {
        return new SchoolClassDetailDTO(
                result.id(), result.name(), result.semester(), result.status(), result.scheduleCount());
    }

    private static TeachingFacadeException publicFailure(TeachingUseCaseException exception) {
        return new TeachingFacadeException(exception.getCode(), exception.getMessage());
    }
}
