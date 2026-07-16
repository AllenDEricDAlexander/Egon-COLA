package top.egon.cola.component.accessguard.agent;

import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

public final class AgentMethodSignature implements MethodSignature {

    private final Method method;

    public AgentMethodSignature(Method method) {
        this.method = method;
    }

    @Override
    public Class<?> getReturnType() {
        return method.getReturnType();
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public Class<?>[] getParameterTypes() {
        return method.getParameterTypes().clone();
    }

    @Override
    public String[] getParameterNames() {
        return Arrays.stream(method.getParameters()).map(Parameter::getName).toArray(String[]::new);
    }

    @Override
    public Class<?>[] getExceptionTypes() {
        return method.getExceptionTypes().clone();
    }

    @Override
    public String toShortString() {
        return method.getDeclaringClass().getSimpleName() + "." + method.getName() + "(..)";
    }

    @Override
    public String toLongString() {
        return method.toGenericString();
    }

    @Override
    public String getName() {
        return method.getName();
    }

    @Override
    public int getModifiers() {
        return method.getModifiers();
    }

    @Override
    public Class<?> getDeclaringType() {
        return method.getDeclaringClass();
    }

    @Override
    public String getDeclaringTypeName() {
        return method.getDeclaringClass().getName();
    }
}
