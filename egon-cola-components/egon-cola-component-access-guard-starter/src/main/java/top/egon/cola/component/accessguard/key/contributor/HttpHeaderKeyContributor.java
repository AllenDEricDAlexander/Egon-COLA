package top.egon.cola.component.accessguard.key.contributor;

import top.egon.cola.component.accessguard.core.GuardInvocation;
import top.egon.cola.component.accessguard.core.plan.KeyConfig;
import top.egon.cola.component.accessguard.key.GuardKeyPart;
import top.egon.cola.component.accessguard.key.GuardKeyResolutionException;

import java.util.ArrayList;
import java.util.List;

public final class HttpHeaderKeyContributor implements GuardKeyContributor {

    @Override
    public String id() {
        return "HTTP_HEADER";
    }

    @Override
    public List<GuardKeyPart> contribute(GuardInvocation invocation, KeyConfig config) {
        Object candidate = invocation.attributes().get(ClientIpKeyContributor.HTTP_REQUEST_ATTRIBUTE);
        List<GuardKeyPart> parts = new ArrayList<>();
        for (int index = 0; index < config.headers().size(); index++) {
            String name = config.headers().get(index);
            String value = HttpRequestAccess.header(candidate, name);
            if (value == null) {
                throw new GuardKeyResolutionException("REQUIRED_HEADER_MISSING");
            }
            parts.add(new GuardKeyPart("header." + name.toLowerCase(java.util.Locale.ROOT), value, index));
        }
        return List.copyOf(parts);
    }
}
