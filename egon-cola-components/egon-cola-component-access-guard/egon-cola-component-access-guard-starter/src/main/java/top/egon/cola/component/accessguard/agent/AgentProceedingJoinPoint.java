package top.egon.cola.component.accessguard.agent;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.SourceLocation;
import org.aspectj.runtime.internal.AroundClosure;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class AgentProceedingJoinPoint implements ProceedingJoinPoint {

    private final Object target;
    private final Method method;
    private final MethodHandle continuation;
    private final Object[] arguments;
    private final AgentMethodSignature signature;
    private final SourceLocation sourceLocation;
    private final StaticPart staticPart;

    public AgentProceedingJoinPoint(
            Object target,
            Method method,
            MethodHandle continuation,
            Object[] arguments
    ) {
        if (!Modifier.isStatic(method.getModifiers()) && target == null) {
            throw new IllegalArgumentException("Instance method requires an Agent target");
        }
        if (Modifier.isStatic(method.getModifiers()) && target != null) {
            throw new IllegalArgumentException("Static method must not have an Agent target");
        }
        this.target = target;
        this.method = method;
        this.continuation = continuation;
        this.arguments = arguments == null ? new Object[0] : arguments.clone();
        this.signature = new AgentMethodSignature(method);
        this.sourceLocation = new AgentSourceLocation(method.getDeclaringClass());
        this.staticPart = new AgentStaticPart(signature, sourceLocation);
    }

    @Override
    public Object proceed() throws Throwable {
        return invoke(arguments);
    }

    @Override
    public Object proceed(Object[] arguments) throws Throwable {
        Object[] invocationArguments = arguments == null ? new Object[0] : arguments.clone();
        if (invocationArguments.length != method.getParameterCount()) {
            throw new IllegalArgumentException("Expected " + method.getParameterCount()
                    + " Agent arguments but received " + invocationArguments.length);
        }
        return invoke(invocationArguments);
    }

    private Object invoke(Object[] invocationArguments) throws Throwable {
        List<Object> values = new ArrayList<>(invocationArguments.length + 1);
        if (!Modifier.isStatic(method.getModifiers())) {
            values.add(target);
        }
        values.addAll(Arrays.asList(invocationArguments));
        return continuation.invokeWithArguments(values);
    }

    @Override
    public void set$AroundClosure(AroundClosure arc) {
    }

    @Override
    public String toShortString() {
        return "execution(" + signature.toShortString() + ")";
    }

    @Override
    public String toLongString() {
        return "method-execution(" + signature.toLongString() + ")";
    }

    @Override
    public Object getThis() {
        return target;
    }

    @Override
    public Object getTarget() {
        return target;
    }

    @Override
    public Object[] getArgs() {
        return arguments.clone();
    }

    @Override
    public Signature getSignature() {
        return signature;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public String getKind() {
        return JoinPoint.METHOD_EXECUTION;
    }

    @Override
    public StaticPart getStaticPart() {
        return staticPart;
    }

    private record AgentSourceLocation(Class<?> withinType) implements SourceLocation {

        @Override
        public Class<?> getWithinType() {
            return withinType;
        }

        @Override
        public String getFileName() {
            return withinType.getSimpleName() + ".java";
        }

        @Override
        public int getLine() {
            return -1;
        }

        @Override
        public int getColumn() {
            return -1;
        }
    }

    private record AgentStaticPart(
            Signature signature,
            SourceLocation sourceLocation
    ) implements StaticPart {

        @Override
        public int getId() {
            return -1;
        }

        @Override
        public String getKind() {
            return JoinPoint.METHOD_EXECUTION;
        }

        @Override
        public Signature getSignature() {
            return signature;
        }

        @Override
        public SourceLocation getSourceLocation() {
            return sourceLocation;
        }

        @Override
        public String toShortString() {
            return "execution(" + signature.toShortString() + ")";
        }

        @Override
        public String toLongString() {
            return "method-execution(" + signature.toLongString() + ")";
        }

        @Override
        public String toString() {
            return toShortString();
        }
    }
}
