package ${package}.facade.teaching;

import ${package}.facade.dto.teaching.AssignUserToClassRequest;
import ${package}.facade.dto.teaching.CreateSchoolClassRequest;
import ${package}.facade.dto.teaching.SchoolClassDTO;

public interface SchoolClassFacade {
    SchoolClassDTO createSchoolClass(CreateSchoolClassRequest request);

    void assignUser(AssignUserToClassRequest request);
}
