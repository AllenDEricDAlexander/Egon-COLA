package top.egon.cola.component.bytecode.starter;

import org.springframework.beans.factory.DisposableBean;
import top.egon.cola.component.bytecode.bridge.BytecodeRuntimeDispatcher;
import top.egon.cola.component.bytecode.bridge.DispatcherRegistration;
import top.egon.cola.component.bytecode.bridge.DispatcherRegistry;

import java.util.Objects;

public final class BytecodeRuntimeRegistrar implements DisposableBean {

    private final DispatcherRegistration registration;

    public BytecodeRuntimeRegistrar(
            ClassLoader applicationLoader,
            String runtimeVersion,
            BytecodeRuntimeDispatcher dispatcher
    ) {
        this.registration = DispatcherRegistry.register(
                Objects.requireNonNull(applicationLoader, "applicationLoader"),
                runtimeVersion,
                dispatcher
        );
    }

    @Override
    public void destroy() {
        registration.close();
    }
}
