package ${package}.application.manage.teaching;

public interface SchoolClassManage {
    SchoolClassView create(String name, String gradeName);
    void assignUser(String userId, String schoolClassId);
}
