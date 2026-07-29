package top.egon.cola.component.accessguard.key;

public record GuardKeyPart(String name, String value, int order) {

    public GuardKeyPart {
        if (name == null || name.isBlank()) {
            throw new GuardKeyResolutionException("INVALID_PART_NAME");
        }
        if (value == null) {
            throw new GuardKeyResolutionException("REQUIRED_PART_MISSING");
        }
        name = name.trim();
    }

    @Override
    public String toString() {
        return "GuardKeyPart[name=" + name + ", value=<redacted>, order=" + order + "]";
    }
}
