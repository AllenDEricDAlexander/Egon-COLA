package top.egon.cola.component.accessguard.api;

import top.egon.cola.component.accessguard.core.GuardOutcome;

public interface AccessGuardClient {

    GuardOutcome evaluate(GuardRequest request);

    <T> T execute(GuardRequest request, GuardedOperation<T> operation) throws Throwable;
}
