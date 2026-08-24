package ${package}.infrastructure.cache;

public final class OrganizationCacheKey {
    private static final String PREFIX = "student-management-organization:";
    private OrganizationCacheKey() {}

    public static String user(Long id) { return PREFIX + "user:" + id; }
    public static String grade(Long id) { return PREFIX + "grade:" + id; }
    public static String schoolClass(Long gradeId, Long id) {
        return PREFIX + "school-class:" + gradeId + ":" + id;
    }
    public static String command(String operation, String requestId) {
        return PREFIX + "command:" + operation + ":" + requestId;
    }
}
