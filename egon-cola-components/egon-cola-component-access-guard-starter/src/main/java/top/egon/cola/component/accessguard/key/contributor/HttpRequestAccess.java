package top.egon.cola.component.accessguard.key.contributor;

import top.egon.cola.component.accessguard.key.GuardKeyResolutionException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

final class HttpRequestAccess {

    private HttpRequestAccess() {
    }

    static String remoteAddress(Object request) {
        return invoke(request, "getRemoteAddr", new Class<?>[0], new Object[0]);
    }

    static String header(Object request, String name) {
        return invoke(request, "getHeader", new Class<?>[]{String.class}, new Object[]{name});
    }

    private static String invoke(Object request, String methodName, Class<?>[] parameterTypes, Object[] arguments) {
        if (request == null) {
            throw new GuardKeyResolutionException("HTTP_REQUEST_MISSING");
        }
        try {
            Method method = request.getClass().getMethod(methodName, parameterTypes);
            Object value = method.invoke(request, arguments);
            return value == null ? null : String.valueOf(value);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            throw new GuardKeyResolutionException("HTTP_REQUEST_UNREADABLE");
        }
    }
}
