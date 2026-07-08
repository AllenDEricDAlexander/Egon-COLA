package top.egon.cola.component.accessguard.key;

import org.aspectj.lang.ProceedingJoinPoint;
import top.egon.cola.component.accessguard.config.AccessGuardRule;

import java.util.List;

public class CompositeAccessKeyResolver implements AccessKeyResolver {

    private final List<AccessKeyResolver> resolvers;

    public CompositeAccessKeyResolver(List<AccessKeyResolver> resolvers) {
        this.resolvers = List.copyOf(resolvers);
    }

    @Override
    public AccessKeyResolution resolve(ProceedingJoinPoint joinPoint, AccessGuardRule rule) {
        for (AccessKeyResolver resolver : resolvers) {
            AccessKeyResolution resolution = resolver.resolve(joinPoint, rule);
            if (resolution != null) {
                return resolution;
            }
        }
        return null;
    }
}
