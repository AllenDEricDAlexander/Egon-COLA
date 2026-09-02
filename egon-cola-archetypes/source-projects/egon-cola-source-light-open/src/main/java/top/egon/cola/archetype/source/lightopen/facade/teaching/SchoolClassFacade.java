package top.egon.cola.archetype.source.lightopen.facade.teaching;

import top.egon.cola.archetype.source.lightopen.facade.teaching.dto.CreateSchoolClassDTO;
import top.egon.cola.archetype.source.lightopen.facade.teaching.dto.ScheduleCourseDTO;
import top.egon.cola.archetype.source.lightopen.facade.teaching.dto.SchoolClassDetailDTO;

public interface SchoolClassFacade {
    SchoolClassDetailDTO createSchoolClass(CreateSchoolClassDTO request);

    SchoolClassDetailDTO scheduleCourse(ScheduleCourseDTO request);

    SchoolClassDetailDTO getSchoolClass(Long schoolClassId);

}
