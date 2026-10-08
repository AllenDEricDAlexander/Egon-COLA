package top.egon.cola.component.yuheng.edge.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.PendingEntry;
import org.redisson.api.RScript;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.api.stream.StreamCreateGroupArgs;
import org.redisson.api.stream.StreamReadGroupArgs;
import org.redisson.client.RedisException;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import top.egon.cola.component.yuheng.edge.config.EdgeOAuthProperties;
import top.egon.cola.component.yuheng.edge.service.EdgeLogoutIntentStore;
import top.egon.cola.component.yuheng.edge.service.EdgeLogoutRevocationIntent;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component("edgeLogoutIntentStore")
@RequiredArgsConstructor
@Slf4j
public class RedissonEdgeLogoutIntentStore implements EdgeLogoutIntentStore {
    private static final String PURPOSE = "logout-intent";
    private static final String INTENT_ID_FIELD = "intentId";
    private static final String PAYLOAD_FIELD = "payload";
    private static final String BOUNDED_XADD_BATCH = "local n=redis.call('XLEN',KEYS[1]); "
            + "local count=tonumber(ARGV[2]); "
            + "if n+count>tonumber(ARGV[1]) then return false end; "
            + "local ids={}; "
            + "for i=0,count-1 do local p=3+(i*2); "
            + "ids[#ids+1]=redis.call('XADD',KEYS[1],'*','intentId',ARGV[p],'payload',ARGV[p+1]); "
            + "end; return ids";
    private static final String ACK_AND_DELETE = "redis.call('XACK',KEYS[1],ARGV[1],ARGV[2]); "
            + "return redis.call('XDEL',KEYS[1],ARGV[2])";
    private static final int MAX_BATCH_SIZE = 100;

    @Qualifier("redisson")
    private final RedissonClient redisson;
    @Qualifier("edgeOAuthProperties")
    private final EdgeOAuthProperties properties;
    @Qualifier("edgeTokenCipher")
    private final EdgeTokenCipher cipher;
    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;
    private final String consumerName = "edge-logout-" + UUID.randomUUID();

    @PostConstruct
    public void initializeConsumerGroup() {
        RStream<String, String> stream = stream();
        try {
            stream.createGroup(StreamCreateGroupArgs.name(properties.getLogout().getConsumerGroup())
                    .id(StreamMessageId.ALL)
                    .makeStream());
        } catch (RedisException exception) {
            if (exception.getMessage() == null || !exception.getMessage().contains("BUSYGROUP")) {
                throw new IllegalStateException("Edge logout retry stream could not be initialized", exception);
            }
        }
    }

    @Override
    public Mono<List<String>> enqueueAll(List<EdgeLogoutRevocationIntent> intents) {
        if (intents == null || intents.isEmpty()
                || intents.size() > properties.getSession().getMaxAuthorizedClients()) {
            return Mono.error(new IllegalArgumentException("Edge logout intent batch is invalid"));
        }
        return Mono.fromCallable(() -> enqueueNow(intents))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(exception -> log.debug(
                        "Edge logout intent batch enqueue failed; failureType={}",
                        exception.getClass().getSimpleName()));
    }

    @Override
    public Mono<List<PendingIntent>> pollNewAndClaimStale() {
        return Mono.fromCallable(this::pollNow)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(exception -> log.debug(
                        "Edge logout intent poll failed; failureType={}",
                        exception.getClass().getSimpleName()));
    }

    @Override
    public Mono<Void> acknowledge(String messageId) {
        return Mono.fromRunnable(() -> acknowledgeNow(messageId))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    private List<String> enqueueNow(List<EdgeLogoutRevocationIntent> intents) throws Exception {
        List<Object> arguments = new ArrayList<>(2 + intents.size() * 2);
        arguments.add(properties.getLogout().getMaxPending());
        arguments.add(intents.size());
        for (EdgeLogoutRevocationIntent intent : intents) {
            byte[] plaintext = objectMapper.writeValueAsBytes(intent);
            String encrypted = new String(
                    cipher.encrypt(plaintext, PURPOSE, intent.intentId()),
                    StandardCharsets.US_ASCII);
            arguments.add(intent.intentId());
            arguments.add(encrypted);
        }
        Object result = redisson.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE,
                BOUNDED_XADD_BATCH,
                RScript.ReturnType.MULTI,
                List.of(properties.getLogout().getStream()),
                arguments.toArray());
        if (!(result instanceof List<?> resultIds) || resultIds.size() != intents.size()) {
            throw new IllegalStateException("Edge logout retry stream is at capacity");
        }
        return resultIds.stream().map(Object::toString).toList();
    }

    private List<PendingIntent> pollNow() throws Exception {
        RStream<String, String> stream = stream();
        String group = properties.getLogout().getConsumerGroup();
        int batchSize = Math.min(properties.getLogout().getMaxPending(), MAX_BATCH_SIZE);
        Map<StreamMessageId, Map<String, String>> messages = new LinkedHashMap<>(
                stream.readGroup(group, consumerName,
                        StreamReadGroupArgs.neverDelivered().count(batchSize)));

        List<PendingEntry> stale = stream.listPending(
                group,
                StreamMessageId.MIN,
                StreamMessageId.MAX,
                properties.getLogout().getClaimIdle().toMillis(),
                TimeUnit.MILLISECONDS,
                batchSize);
        if (!stale.isEmpty()) {
            StreamMessageId[] staleIds = stale.stream().map(PendingEntry::getId)
                    .toArray(StreamMessageId[]::new);
            messages.putAll(stream.claim(
                    group,
                    consumerName,
                    properties.getLogout().getClaimIdle().toMillis(),
                    TimeUnit.MILLISECONDS,
                    staleIds));
        }

        List<PendingIntent> pending = new ArrayList<>(messages.size());
        for (Map.Entry<StreamMessageId, Map<String, String>> message : messages.entrySet()) {
            String encrypted = message.getValue().get(PAYLOAD_FIELD);
            String intentId = message.getValue().get(INTENT_ID_FIELD);
            if (encrypted == null || intentId == null || intentId.isBlank()) {
                throw new IllegalStateException("Edge logout retry message is malformed");
            }
            byte[] plaintext = cipher.decrypt(
                    encrypted.getBytes(StandardCharsets.US_ASCII),
                    PURPOSE,
                    intentId);
            EdgeLogoutRevocationIntent intent = objectMapper.readValue(
                    plaintext, EdgeLogoutRevocationIntent.class);
            if (!intentId.equals(intent.intentId())) {
                throw new IllegalStateException("Edge logout retry message context is invalid");
            }
            pending.add(new PendingIntent(message.getKey().toString(), intent));
        }
        return List.copyOf(pending);
    }

    private void acknowledgeNow(String messageId) {
        redisson.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE,
                ACK_AND_DELETE,
                RScript.ReturnType.INTEGER,
                List.of(properties.getLogout().getStream()),
                properties.getLogout().getConsumerGroup(),
                messageId);
    }

    private RStream<String, String> stream() {
        return redisson.getStream(properties.getLogout().getStream(), StringCodec.INSTANCE);
    }

}
