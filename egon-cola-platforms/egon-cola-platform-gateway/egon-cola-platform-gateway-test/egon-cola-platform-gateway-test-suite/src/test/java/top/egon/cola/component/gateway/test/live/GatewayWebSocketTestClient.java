package top.egon.cola.component.gateway.test.live;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

public final class GatewayWebSocketTestClient {

    private final HttpClient httpClient;

    public GatewayWebSocketTestClient() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build());
    }

    GatewayWebSocketTestClient(HttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
    }

    public Transcript exchange(
            URI uri,
            Map<String, String> headers,
            String text,
            byte[] binary,
            byte[] ping,
            Duration timeout) throws Exception {
        RecordingListener listener = new RecordingListener();
        WebSocket.Builder builder = httpClient.newWebSocketBuilder()
                .connectTimeout(timeout);
        headers.forEach(builder::header);
        WebSocket socket = builder.buildAsync(uri, listener)
                .get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        try {
            socket.sendText(text, true)
                    .get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            listener.text.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            socket.sendBinary(ByteBuffer.wrap(binary), true)
                    .get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            listener.binary.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            socket.sendPing(ByteBuffer.wrap(ping))
                    .get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            listener.pong.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "test-complete")
                    .get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            listener.closeCode.get(
                    timeout.toMillis(),
                    TimeUnit.MILLISECONDS
            );
            return listener.transcript();
        } catch (Exception failure) {
            socket.abort();
            throw failure;
        }
    }

    public record Transcript(
            List<String> textFrames,
            List<byte[]> binaryFrames,
            List<byte[]> pongFrames,
            int closeCode,
            String closeReason) {

        public Transcript {
            textFrames = List.copyOf(textFrames);
            binaryFrames = copy(binaryFrames);
            pongFrames = copy(pongFrames);
        }

        private static List<byte[]> copy(List<byte[]> values) {
            return values.stream()
                    .map(value -> Arrays.copyOf(value, value.length))
                    .toList();
        }
    }

    private static final class RecordingListener
            implements WebSocket.Listener {

        private final List<String> textFrames =
                java.util.Collections.synchronizedList(new ArrayList<>());

        private final List<byte[]> binaryFrames =
                java.util.Collections.synchronizedList(new ArrayList<>());

        private final List<byte[]> pongFrames =
                java.util.Collections.synchronizedList(new ArrayList<>());

        private final CompletableFuture<String> text =
                new CompletableFuture<>();

        private final CompletableFuture<byte[]> binary =
                new CompletableFuture<>();

        private final CompletableFuture<byte[]> pong =
                new CompletableFuture<>();

        private final CompletableFuture<Integer> closeCode =
                new CompletableFuture<>();

        private volatile String closeReason;

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(
                WebSocket webSocket,
                CharSequence data,
                boolean last) {
            if (last) {
                String value = data.toString();
                textFrames.add(value);
                text.complete(value);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onBinary(
                WebSocket webSocket,
                ByteBuffer data,
                boolean last) {
            byte[] value = bytes(data);
            if (last) {
                binaryFrames.add(value);
                binary.complete(value);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onPong(
                WebSocket webSocket,
                ByteBuffer message) {
            byte[] value = bytes(message);
            pongFrames.add(value);
            pong.complete(value);
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(
                WebSocket webSocket,
                int statusCode,
                String reason) {
            closeReason = reason;
            closeCode.complete(statusCode);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            text.completeExceptionally(error);
            binary.completeExceptionally(error);
            pong.completeExceptionally(error);
            closeCode.completeExceptionally(error);
        }

        private Transcript transcript() {
            return new Transcript(
                    textFrames,
                    binaryFrames,
                    pongFrames,
                    closeCode.join(),
                    closeReason
            );
        }

        private static byte[] bytes(ByteBuffer source) {
            byte[] value = new byte[source.remaining()];
            source.get(value);
            return value;
        }
    }
}
