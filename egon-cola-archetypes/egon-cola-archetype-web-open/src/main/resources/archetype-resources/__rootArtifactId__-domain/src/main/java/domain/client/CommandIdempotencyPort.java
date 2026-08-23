package ${package}.domain.client;

public interface CommandIdempotencyPort {
    boolean claim(String operation, String requestId);
    void release(String operation, String requestId);
}
