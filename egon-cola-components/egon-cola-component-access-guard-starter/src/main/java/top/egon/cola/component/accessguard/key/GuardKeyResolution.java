package top.egon.cola.component.accessguard.key;

import java.util.List;
import java.util.Objects;

public record GuardKeyResolution(
        GuardKeyScope scope,
        List<GuardKeyPart> parts,
        String keyHash
) {

    public GuardKeyResolution {
        scope = Objects.requireNonNull(scope, "scope");
        parts = List.copyOf(parts);
        if (keyHash == null || !keyHash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("keyHash must be a lowercase SHA-256 value");
        }
    }

    @Override
    public String toString() {
        return "GuardKeyResolution[scope=" + scope
                + ", partNames=" + parts.stream().map(GuardKeyPart::name).toList()
                + ", keyHash=<redacted>]";
    }
}
