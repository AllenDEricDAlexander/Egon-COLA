package top.egon.cola.component.rpc.provider.binding;

import com.google.protobuf.Message;
import top.egon.cola.component.rpc.contract.descriptor.RpcMethodDescriptor;

import java.lang.reflect.InvocationTargetException;

public record RpcProviderMethodBinding(
        RpcProviderBinding provider,
        RpcMethodDescriptor method
) {

    public Object invoke(Message request) throws Throwable {
        try {
            var javaMethod = method.javaMethod();
            if (!javaMethod.canAccess(provider.bean())
                    && !javaMethod.trySetAccessible()) {
                throw new IllegalAccessException(
                        "RPC provider method is not accessible: "
                                + javaMethod);
            }
            return javaMethod.invoke(provider.bean(), request);
        } catch (InvocationTargetException exception) {
            throw exception.getTargetException();
        }
    }
}
