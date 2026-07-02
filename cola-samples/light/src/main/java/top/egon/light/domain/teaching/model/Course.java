package top.egon.light.domain.teaching.model;

public class Course {
    private final String id;
    private final String name;
    private final String description;

    private Course(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public static Course create(String id, String name, String description) {
        return new Course(id, name, description);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
