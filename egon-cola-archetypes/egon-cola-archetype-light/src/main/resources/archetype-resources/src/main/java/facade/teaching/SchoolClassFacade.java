package ${package}.facade.teaching;

import ${package}.facade.teaching.dto.CreateSchoolClassDTO;
import ${package}.facade.teaching.dto.ScheduleCourseDTO;
import ${package}.facade.teaching.dto.SchoolClassDetailDTO;

public interface SchoolClassFacade {
    SchoolClassDetailDTO createSchoolClass(CreateSchoolClassDTO request);

    SchoolClassDetailDTO scheduleCourse(ScheduleCourseDTO request);
}
