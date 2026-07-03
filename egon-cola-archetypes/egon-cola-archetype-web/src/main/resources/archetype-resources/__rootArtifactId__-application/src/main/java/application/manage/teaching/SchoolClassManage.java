package ${package}.application.manage.teaching;

import ${package}.domain.entities.teaching.SchoolClass;

public interface SchoolClassManage {
    SchoolClass create(String name, String gradeName);

    void assignUser(String userId, String schoolClassId);
}
