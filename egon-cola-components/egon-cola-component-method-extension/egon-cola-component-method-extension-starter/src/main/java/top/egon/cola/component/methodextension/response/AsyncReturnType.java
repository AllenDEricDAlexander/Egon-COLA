package top.egon.cola.component.methodextension.response;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

public record AsyncReturnType(Kind kind, Type payloadType) {

    public static AsyncReturnType from(Method method) {
        Class<?> returnType = method.getReturnType();
        Kind kind;
        if (returnType == CompletableFuture.class) {
            kind = Kind.COMPLETABLE_FUTURE;
        } else if (returnType == CompletionStage.class) {
            kind = Kind.COMPLETION_STAGE;
        } else if (returnType == Future.class) {
            kind = Kind.FUTURE;
        } else if (Future.class.isAssignableFrom(returnType)
                || CompletionStage.class.isAssignableFrom(returnType)) {
            kind = Kind.UNSUPPORTED_CONCRETE_FUTURE;
        } else {
            return new AsyncReturnType(Kind.NONE, method.getGenericReturnType());
        }
        Type genericReturnType = method.getGenericReturnType();
        Type payloadType = genericReturnType instanceof ParameterizedType parameterizedType
                ? parameterizedType.getActualTypeArguments()[0] : Object.class;
        return new AsyncReturnType(kind, payloadType);
    }

    public boolean supported() {
        return kind == Kind.FUTURE
                || kind == Kind.COMPLETION_STAGE
                || kind == Kind.COMPLETABLE_FUTURE;
    }

    public enum Kind {
        NONE,
        FUTURE,
        COMPLETION_STAGE,
        COMPLETABLE_FUTURE,
        UNSUPPORTED_CONCRETE_FUTURE
    }
}
