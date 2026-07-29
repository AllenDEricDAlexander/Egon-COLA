package top.egon.cola.component.accessguard.key;

import top.egon.cola.component.accessguard.core.GuardInvocation;
import top.egon.cola.component.accessguard.core.plan.KeyConfig;
import top.egon.cola.component.accessguard.key.contributor.GuardKeyContributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CompositeGuardKeyResolver implements GuardKeyResolver {

    private final Map<String, GuardKeyContributor> contributors;
    private final KeyHasher keyHasher;

    public CompositeGuardKeyResolver(List<GuardKeyContributor> contributors, KeyHasher keyHasher) {
        Map<String, GuardKeyContributor> indexed = new LinkedHashMap<>();
        for (GuardKeyContributor contributor : contributors) {
            String id = contributor.id().toUpperCase(Locale.ROOT);
            if (indexed.putIfAbsent(id, contributor) != null) {
                throw new IllegalArgumentException("Duplicate GuardKeyContributor id: " + id);
            }
        }
        this.contributors = Map.copyOf(indexed);
        this.keyHasher = keyHasher;
    }

    @Override
    public GuardKeyResolution resolve(GuardInvocation invocation, KeyConfig config) {
        List<GuardKeyPart> parts = new ArrayList<>();
        for (String configured : config.contributors()) {
            String id = configured.split(":", 2)[0].trim().toUpperCase(Locale.ROOT);
            GuardKeyContributor contributor = contributors.get(id);
            if (contributor == null) {
                throw new GuardKeyResolutionException("UNKNOWN_CONTRIBUTOR");
            }
            parts.addAll(contributor.contribute(invocation, config));
        }
        if (parts.isEmpty()) {
            throw new GuardKeyResolutionException("NO_KEY_PARTS");
        }
        parts.sort(Comparator.comparingInt(GuardKeyPart::order));
        String normalized = parts.stream()
                .map(part -> normalize(part, config.maxPartLength()))
                .collect(java.util.stream.Collectors.joining("|"));
        GuardKeyScope scope = config.contributors().size() == 1
                && "GLOBAL".equalsIgnoreCase(config.contributors().getFirst())
                ? GuardKeyScope.GLOBAL
                : GuardKeyScope.KEY;
        return new GuardKeyResolution(scope, parts, keyHasher.hash(normalized, config.hmacSecret()));
    }

    private static String normalize(GuardKeyPart part, int maxPartLength) {
        String name = part.name().trim();
        String value = part.value().trim();
        if (name.length() > maxPartLength || value.length() > maxPartLength
                || containsControl(name) || containsControl(value)) {
            throw new GuardKeyResolutionException("invalid key part");
        }
        return escape(name) + "=" + escape(value);
    }

    private static boolean containsControl(String value) {
        return value.codePoints().anyMatch(Character::isISOControl);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("|", "\\|")
                .replace("=", "\\=");
    }
}
