package top.egon.cola.component.accessguard.key;

import top.egon.cola.component.accessguard.config.AccessGuardRule;

import java.lang.reflect.Executable;

public interface ExecutableAccessKeyResolver {

    AccessKeyResolution resolve(Executable executable, Object[] arguments, AccessGuardRule rule);
}
