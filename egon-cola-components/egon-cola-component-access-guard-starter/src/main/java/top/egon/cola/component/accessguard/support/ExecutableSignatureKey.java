package top.egon.cola.component.accessguard.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.util.Arrays;
import java.util.List;

public record ExecutableSignatureKey(
        String className,
        String executableName,
        List<String> parameterTypes
) {

    public static ExecutableSignatureKey from(Executable executable) {
        return new ExecutableSignatureKey(
                executable.getDeclaringClass().getName(),
                executable instanceof Constructor<?> ? "<init>" : executable.getName(),
                Arrays.stream(executable.getParameterTypes()).map(Class::getName).toList()
        );
    }

    public String asString() {
        return className + "#" + executableName + "(" + String.join(",", parameterTypes) + ")";
    }
}
