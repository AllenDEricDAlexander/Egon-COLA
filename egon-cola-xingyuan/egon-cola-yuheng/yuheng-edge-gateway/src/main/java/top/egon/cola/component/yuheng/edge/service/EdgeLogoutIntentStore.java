package top.egon.cola.component.yuheng.edge.service;

import reactor.core.publisher.Mono;

import java.util.List;

public interface EdgeLogoutIntentStore {
    default Mono<String> enqueue(EdgeLogoutRevocationIntent intent) {
        return enqueueAll(List.of(intent)).map(ids -> ids.getFirst());
    }

    Mono<List<String>> enqueueAll(List<EdgeLogoutRevocationIntent> intents);

    Mono<List<PendingIntent>> pollNewAndClaimStale();

    Mono<Void> acknowledge(String messageId);

    record PendingIntent(String messageId, EdgeLogoutRevocationIntent intent) {
        public PendingIntent {
            if (messageId == null || messageId.isBlank() || intent == null) {
                throw new IllegalArgumentException("Edge pending logout intent is invalid");
            }
        }
    }
}
