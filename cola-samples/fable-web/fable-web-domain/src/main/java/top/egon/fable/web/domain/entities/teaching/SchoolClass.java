package top.egon.fable.web.domain.entities.teaching;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SchoolClass {
    private final String id;
    private final String name;
    private final String gradeName;
    private final List<String> userIds;

    private SchoolClass(String id, String name, String gradeName, List<String> userIds) {
        this.id = id;
        this.name = name;
        this.gradeName = gradeName;
        this.userIds = new ArrayList<>(userIds);
    }

    public static SchoolClass create(String id, String name, String gradeName) {
        return new SchoolClass(id, name, gradeName, List.of());
    }

    public static SchoolClass restore(String id, String name, String gradeName, List<String> userIds) {
        return new SchoolClass(id, name, gradeName, userIds);
    }

    public void assignUser(String userId) {
        userIds.add(userId);
    }

    public boolean hasUser(String userId) {
        return userIds.contains(userId);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getGradeName() { return gradeName; }
    public List<String> getUserIds() { return Collections.unmodifiableList(userIds); }
}
