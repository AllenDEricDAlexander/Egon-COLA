package top.egon.cola.archetype.source.lightopen.adapter.teaching.facade.impl;

import top.egon.cola.archetype.source.lightopen.application.teaching.command.CreateSchoolClassCommand;
import top.egon.cola.archetype.source.lightopen.application.teaching.command.ScheduleCourseCommand;
import top.egon.cola.archetype.source.lightopen.application.teaching.manage.SchoolClassManage;
import top.egon.cola.archetype.source.lightopen.application.teaching.manage.TeachingUseCaseException;
import top.egon.cola.archetype.source.lightopen.application.teaching.query.GetSchoolClassQuery;
import top.egon.cola.archetype.source.lightopen.application.teaching.result.SchoolClassResult;
import top.egon.cola.archetype.source.lightopen.facade.teaching.SchoolClassFacade;
import top.egon.cola.archetype.source.lightopen.facade.teaching.dto.CreateSchoolClassDTO;
import top.egon.cola.archetype.source.lightopen.facade.teaching.dto.ScheduleCourseDTO;
import top.egon.cola.archetype.source.lightopen.facade.teaching.dto.SchoolClassDetailDTO;
import top.egon.cola.archetype.source.lightopen.facade.teaching.exceptions.TeachingFacadeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("schoolClassFacadeImpl")
@RequiredArgsConstructor
@Slf4j
public class SchoolClassFacadeImpl implements SchoolClassFacade {
    @Qualifier("schoolClassManageImpl")
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

    @Override
    public SchoolClassDetailDTO getSchoolClass(Long schoolClassId) {
        try {
            return toDto(schoolClassManage.get(new GetSchoolClassQuery(schoolClassId)));
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
