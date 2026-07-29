package top.egon.cola.component.accessguard.support;

import java.lang.reflect.Method;
import java.util.List;

public record MethodSignatureKey(String className, String methodName, List<String> parameterTypes) {

    public static MethodSignatureKey from(Method method) {
        ExecutableSignatureKey key = ExecutableSignatureKey.from(method);
        return new MethodSignatureKey(
                key.className(),
                key.executableName(),
                key.parameterTypes()
        );
    }

    public String asString() {
        return className + "#" + methodName + "(" + String.join(",", parameterTypes) + ")";
    }
}
