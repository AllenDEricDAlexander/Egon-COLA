package top.egon.cola.component.accessguard.support;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public record MethodSignatureKey(String className, String methodName, List<String> parameterTypes) {

    public static MethodSignatureKey from(Method method) {
        return new MethodSignatureKey(
                method.getDeclaringClass().getName(),
                method.getName(),
                Arrays.stream(method.getParameterTypes()).map(Class::getName).toList()
        );
    }

    public String asString() {
        return className + "#" + methodName + "(" + String.join(",", parameterTypes) + ")";
    }
}
