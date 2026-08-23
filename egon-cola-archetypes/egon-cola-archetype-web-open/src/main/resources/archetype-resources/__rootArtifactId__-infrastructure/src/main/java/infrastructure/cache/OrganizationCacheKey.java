package ${package}.infrastructure.cache;

public final class OrganizationCacheKey {
    private static final String PREFIX = "student-management-organization:";
    private OrganizationCacheKey() {}

    public static String user(String id) { return PREFIX + "user:" + id; }
    public static String grade(String id) { return PREFIX + "grade:" + id; }
    public static String schoolClass(String gradeId, String id) {
        return PREFIX + "school-class:" + gradeId + ":" + id;
    }
    public static String command(String operation, String requestId) {
        return PREFIX + "command:" + operation + ":" + requestId;
    }
}
