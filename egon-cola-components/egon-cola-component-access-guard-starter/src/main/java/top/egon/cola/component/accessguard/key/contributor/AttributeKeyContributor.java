package top.egon.cola.component.accessguard.key.contributor;

import top.egon.cola.component.accessguard.core.GuardInvocation;
import top.egon.cola.component.accessguard.core.plan.KeyConfig;
import top.egon.cola.component.accessguard.key.GuardKeyPart;
import top.egon.cola.component.accessguard.key.GuardKeyResolutionException;

import java.util.ArrayList;
import java.util.List;

public final class AttributeKeyContributor implements GuardKeyContributor {

    @Override
    public String id() {
        return "ATTRIBUTE";
    }

    @Override
    public List<GuardKeyPart> contribute(GuardInvocation invocation, KeyConfig config) {
        List<String> names = config.contributors().stream()
                .filter(value -> value.regionMatches(true, 0, "ATTRIBUTE:", 0, "ATTRIBUTE:".length()))
                .map(value -> value.substring("ATTRIBUTE:".length()).trim())
                .filter(value -> !value.isEmpty())
                .toList();
        List<GuardKeyPart> parts = new ArrayList<>();
        for (int index = 0; index < names.size(); index++) {
            String name = names.get(index);
            Object value = invocation.attributes().get(name);
            if (value == null) {
                throw new GuardKeyResolutionException("REQUIRED_ATTRIBUTE_MISSING");
            }
            parts.add(new GuardKeyPart(name, String.valueOf(value), index));
        }
        return List.copyOf(parts);
    }
}
