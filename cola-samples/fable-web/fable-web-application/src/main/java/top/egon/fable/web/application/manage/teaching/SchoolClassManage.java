package top.egon.fable.web.application.manage.teaching;

import top.egon.fable.web.domain.entities.teaching.SchoolClass;

public interface SchoolClassManage {
    SchoolClass create(String name, String gradeName);

    void assignUser(String userId, String schoolClassId);
}
