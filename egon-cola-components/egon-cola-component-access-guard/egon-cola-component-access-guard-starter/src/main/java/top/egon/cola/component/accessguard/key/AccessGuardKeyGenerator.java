package top.egon.cola.component.accessguard.key;

import top.egon.cola.component.accessguard.support.SensitiveValueHasher;

public class AccessGuardKeyGenerator {

    private final SensitiveValueHasher hasher;

    public AccessGuardKeyGenerator() {
        this(new SensitiveValueHasher());
    }

    public AccessGuardKeyGenerator(SensitiveValueHasher hasher) {
        this.hasher = hasher;
    }

    public AccessKeyResolution generate(String rawKey) {
        String normalizedKey = rawKey == null ? "" : rawKey.trim();
        return new AccessKeyResolution(rawKey, normalizedKey, hasher.sha256(normalizedKey));
    }
}
