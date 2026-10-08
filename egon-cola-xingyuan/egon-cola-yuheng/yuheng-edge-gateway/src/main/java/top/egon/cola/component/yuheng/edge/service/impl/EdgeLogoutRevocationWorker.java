package top.egon.cola.component.yuheng.edge.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.egon.cola.component.yuheng.edge.service.EdgeLogoutIntentStore;
import top.egon.cola.component.yuheng.edge.service.EdgeLogoutService;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component("edgeLogoutRevocationWorker")
@RequiredArgsConstructor
public class EdgeLogoutRevocationWorker {
    @Qualifier("edgeLogoutIntentStore")
    private final EdgeLogoutIntentStore intentStore;
    @Qualifier("edgeLogoutService")
    private final EdgeLogoutService logoutService;
    private final AtomicBoolean running = new AtomicBoolean();

    @Scheduled(fixedDelayString = "${egon.yuheng.edge.logout.poll-interval:5s}")
    public void retryPending() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        intentStore.pollNewAndClaimStale()
                .flatMapMany(Flux::fromIterable)
                .flatMap(pending -> logoutService.process(pending)
                        .onErrorResume(exception -> {
                            log.warn("BFF revocation intent remains pending; intentId={}, cause={}",
                                    pending.intent().intentId(),
                                    exception.getClass().getSimpleName());
                            return Mono.empty();
                        }), 4)
                .then()
                .doFinally(ignored -> running.set(false))
                .doOnError(exception -> log.error("BFF logout retry poll failed; cause={}",
                        exception.getClass().getSimpleName()))
                .subscribe();
    }
}
