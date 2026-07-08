package top.egon.cola.component.accessguard.circuitbreaker;

import top.egon.cola.component.accessguard.config.AccessGuardRule;

import java.util.concurrent.ExecutorService;

public interface TimeoutTaskExecutorProvider {

    ExecutorService getExecutor(AccessGuardRule rule);
}
