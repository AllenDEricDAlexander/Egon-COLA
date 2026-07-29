package top.egon.cola.component.accessguard.key.contributor;

import top.egon.cola.component.accessguard.core.GuardInvocation;
import top.egon.cola.component.accessguard.core.plan.KeyConfig;
import top.egon.cola.component.accessguard.key.GuardKeyPart;
import top.egon.cola.component.accessguard.key.GuardKeyResolutionException;

import java.security.Principal;
import java.util.List;

public final class PrincipalKeyContributor implements GuardKeyContributor {

    public static final String PRINCIPAL_ATTRIBUTE = "accessGuard.principal";

    @Override
    public String id() {
        return "PRINCIPAL";
    }

    @Override
    public List<GuardKeyPart> contribute(GuardInvocation invocation, KeyConfig config) {
        Object principal = invocation.attributes().get(PRINCIPAL_ATTRIBUTE);
        if (principal == null) {
            throw new GuardKeyResolutionException("PRINCIPAL_MISSING");
        }
        String name = principal instanceof Principal typed ? typed.getName() : String.valueOf(principal);
        return List.of(new GuardKeyPart("principal", name, 0));
    }
}
