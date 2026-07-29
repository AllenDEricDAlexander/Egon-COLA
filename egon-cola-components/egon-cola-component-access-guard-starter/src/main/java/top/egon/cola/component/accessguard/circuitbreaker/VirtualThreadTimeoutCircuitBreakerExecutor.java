package top.egon.cola.component.accessguard.circuitbreaker;

import top.egon.cola.component.accessguard.reject.RejectResponseInvoker;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VirtualThreadTimeoutCircuitBreakerExecutor extends ThreadPoolTimeoutCircuitBreakerExecutor {

    private static final ExecutorService VIRTUAL_THREAD_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    public VirtualThreadTimeoutCircuitBreakerExecutor(RejectResponseInvoker rejectResponseInvoker) {
        super(VIRTUAL_THREAD_EXECUTOR, rejectResponseInvoker);
    }
}
