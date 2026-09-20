package top.egon.cola.component.outbox.store;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import top.egon.cola.component.outbox.api.OutboxReceipt;
import top.egon.cola.component.outbox.exception.OutboxIdempotencyConflictException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Freezes the long primary key contract for the Postgres outbox store: the JDBC {@code id} column is
 * bound explicitly by the caller, while the {@code message_id} return path and the
 * ON CONFLICT/idempotency resolution keep their previous shape.
 *
 * <p>No database is involved. Every assertion is made on the captured JDBC parameters and SQL text,
 * and the static Snowflake entry is reached reflectively so a missing contract fails as an
 * assertion instead of a compile error.</p>
 */
class OutboxLongPrimaryKeyTest {

    private static final String INSERT_MARKER = "insert into egon_cola_outbox_message";
    private static final String RESOLVE_MARKER = "select message_id, idempotency_key";
    private static final int MACHINE_ID = 733;
    private static final Duration CLOCK_BACKWARD = Duration.ofMillis(5);

    @BeforeAll
    static void bindTheProcessWideEngine() {
        initializeStaticSnowflakeFacade();
    }

    @Test
    void enqueueBindsTheLongIdAsTheFirstInsertParameter() {
        RecordingJdbcTemplate jdbcTemplate = new RecordingJdbcTemplate(
                sql -> List.of(Map.of("message_id", "message-1")));
        NewOutboxRecord record = record();

        OutboxReceipt receipt = newStore(jdbcTemplate).enqueue(record);

        Invocation insert = jdbcTemplate.only(INSERT_MARKER);
        assertThat(normalize(insert.sql()))
                .contains("( id, message_id, idempotency_key, message_fingerprint, channel, destination,")
                .contains("'PENDING', 0,")
                .contains("coalesce(?, clock_timestamp())")
                .contains("on conflict do nothing")
                .contains("returning message_id");
        assertThat(insert.args())
                .as("the explicit id binding grows the parameter list by exactly one position")
                .hasSize(13)
                .filteredOn(value -> value instanceof Long).hasSize(1);
        assertThat((Long) insert.args().getFirst()).isPositive();
        assertThat(insert.args().subList(1, insert.args().size()))
                .containsExactly(
                        "message-1",
                        "idempotency-1",
                        "fingerprint-1",
                        "RABBITMQ",
                        "exchange/routing-key",
                        "{\"orderId\":1}",
                        "application/json",
                        "v1",
                        "{}",
                        "trace-1",
                        3,
                        Timestamp.from(record.availableAt()));
        assertThat(normalize(insert.sql()).chars().filter(character -> character == '?').count())
                .as("every placeholder keeps a bound parameter")
                .isEqualTo(insert.args().size());
        assertThat(receipt).isEqualTo(new OutboxReceipt(record.messageId(), record.idempotencyKey(), true));
    }

    @Test
    void repeatedEnqueueUsesOneMonotonicStaticEngineAndKeepsTheMachineSegment() {
        RecordingJdbcTemplate jdbcTemplate = new RecordingJdbcTemplate(
                sql -> List.of(Map.of("message_id", "message-1")));
        PostgresqlJdbcOutboxStore store = newStore(jdbcTemplate);

        store.enqueue(record());
        store.enqueue(record("message-2", "idempotency-2", "fingerprint-2"));

        long first = boundId(jdbcTemplate, 0);
        long second = boundId(jdbcTemplate, 1);

        assertThat(second).isGreaterThan(first);
        assertThat((first >>> 12) & 1023L).isEqualTo(MACHINE_ID);
        assertThat((second >>> 12) & 1023L).isEqualTo(MACHINE_ID);
    }

    @Test
    void conflictingInsertStillResolvesTheExistingMessageIdAndIdempotencyKey() {
        RecordingJdbcTemplate jdbcTemplate = new RecordingJdbcTemplate(sql -> {
            if (sql.contains(INSERT_MARKER)) {
                return List.of();
            }
            return List.of(Map.of(
                    "message_id", "message-1",
                    "idempotency_key", "idempotency-1",
                    "message_fingerprint", "fingerprint-1"));
        });

        OutboxReceipt receipt = newStore(jdbcTemplate).enqueue(record());

        assertThat(jdbcTemplate.only(INSERT_MARKER).args())
                .filteredOn(value -> value instanceof Long).hasSize(1);
        assertThat(jdbcTemplate.only(RESOLVE_MARKER).args())
                .containsExactly("message-1", "idempotency-1", "idempotency-1");
        assertThat(receipt).isEqualTo(new OutboxReceipt("message-1", "idempotency-1", false));
    }

    @Test
    void anAmbiguousExistingRowIsNeverReportedAsIdempotentSuccess() {
        RecordingJdbcTemplate jdbcTemplate = new RecordingJdbcTemplate(sql -> {
            if (sql.contains(INSERT_MARKER)) {
                return List.of();
            }
            return List.of(
                    Map.of("message_id", "message-1", "idempotency_key", "idempotency-1",
                            "message_fingerprint", "fingerprint-1"),
                    Map.of("message_id", "message-2", "idempotency_key", "idempotency-1",
                            "message_fingerprint", "fingerprint-1"));
        });

        PostgresqlJdbcOutboxStore store = newStore(jdbcTemplate);

        assertThatThrownBy(() -> store.enqueue(record()))
                .isInstanceOf(OutboxIdempotencyConflictException.class);
        assertThat(jdbcTemplate.invocations()).hasSize(2);
    }

    @Test
    void theHistoricalIdentityDeclarationStaysUntouchedInV1() throws Exception {
        String sql = new ClassPathResource(
                "db/transactional-outbox/postgresql/V1__create_transactional_outbox_schema.sql")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(normalize(sql))
                .contains("id bigint generated by default as identity primary key");
    }

    private static long boundId(RecordingJdbcTemplate jdbcTemplate, int enqueueIndex) {
        List<Invocation> inserts = jdbcTemplate.invocations().stream()
                .filter(invocation -> invocation.sql().contains(INSERT_MARKER))
                .toList();
        assertThat(inserts).hasSizeGreaterThan(enqueueIndex);
        return (Long) inserts.get(enqueueIndex).args().getFirst();
    }

    private static PostgresqlJdbcOutboxStore newStore(JdbcTemplate jdbcTemplate) {
        return new PostgresqlJdbcOutboxStore(
                jdbcTemplate,
                null,
                null,
                NoOpTransactionManager.INSTANCE);
    }

    private static NewOutboxRecord record() {
        return record("message-1", "idempotency-1", "fingerprint-1");
    }

    private static NewOutboxRecord record(String messageId, String idempotencyKey, String fingerprint) {
        return new NewOutboxRecord(
                messageId,
                idempotencyKey,
                fingerprint,
                "RABBITMQ",
                "exchange/routing-key",
                "{\"orderId\":1}",
                "application/json",
                "v1",
                "{}",
                "trace-1",
                Instant.parse("2026-09-20T12:00:00Z"),
                3);
    }

    /**
     * The static Snowflake facade belongs to the same Step, so this fixture must not fail to compile
     * while that contract is still missing.
     */
    private static void initializeStaticSnowflakeFacade() {
        try {
            Class<?> facade = Class.forName("top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator");
            Method initialize = Arrays.stream(facade.getDeclaredMethods())
                    .filter(method -> "initialize".equals(method.getName()))
                    .filter(method -> Modifier.isStatic(method.getModifiers()))
                    .findFirst()
                    .orElse(null);
            assertThat(initialize)
                    .as("REQ-014: SnowflakeIdGenerator must expose a static initialize entry")
                    .isNotNull();
            Object machineId = initialize.getParameterTypes()[0] == long.class
                    ? (Object) MACHINE_ID
                    : Long.valueOf(MACHINE_ID);
            initialize.invoke(null, machineId, CLOCK_BACKWARD);
        } catch (ReflectiveOperationException exception) {
            fail("SnowflakeIdGenerator.initialize must be callable reflectively: " + exception, exception);
        }
    }

    private static String normalize(String value) {
        return value.replaceAll("\\s+", " ").trim();
    }

    private record Invocation(String sql, List<Object> args) {
    }

    private static final class RecordingJdbcTemplate extends JdbcTemplate {

        private final List<Invocation> invocations = new ArrayList<>();
        private final Function<String, List<Map<String, String>>> responder;

        private RecordingJdbcTemplate(Function<String, List<Map<String, String>>> responder) {
            this.responder = responder;
        }

        @Override
        public <T> List<T> query(String sql, RowMapper<T> rowMapper, @Nullable Object... args)
                throws DataAccessException {
            invocations.add(new Invocation(sql, new ArrayList<>(Arrays.asList(args))));
            List<T> results = new ArrayList<>();
            for (Map<String, String> row : responder.apply(sql)) {
                try {
                    results.add(rowMapper.mapRow(resultSet(row), 0));
                } catch (SQLException exception) {
                    throw new IllegalStateException("Fixture row mapping failed", exception);
                }
            }
            return results;
        }

        private List<Invocation> invocations() {
            return List.copyOf(invocations);
        }

        private Invocation only(String marker) {
            List<Invocation> matches = invocations.stream()
                    .filter(invocation -> invocation.sql().contains(marker))
                    .toList();
            assertThat(matches)
                    .as("exactly one statement matching %s", marker)
                    .hasSize(1);
            return matches.getFirst();
        }

        private static ResultSet resultSet(Map<String, String> row) {
            InvocationHandler handler = (proxy, method, arguments) -> {
                String name = method.getName();
                if ("getString".equals(name)) {
                    return row.get((String) arguments[0]);
                }
                if ("wasNull".equals(name)) {
                    return false;
                }
                if ("toString".equals(name)) {
                    return "resultSet";
                }
                if ("equals".equals(name)) {
                    return proxy == arguments[0];
                }
                if ("hashCode".equals(name)) {
                    return System.identityHashCode(proxy);
                }
                Class<?> returnType = method.getReturnType();
                return returnType.isPrimitive() ? primitiveDefault(returnType) : null;
            };
            return (ResultSet) Proxy.newProxyInstance(
                    OutboxLongPrimaryKeyTest.class.getClassLoader(),
                    new Class<?>[] {ResultSet.class},
                    handler);
        }

        private static Object primitiveDefault(Class<?> primitiveType) {
            if (boolean.class.equals(primitiveType)) {
                return false;
            }
            if (long.class.equals(primitiveType)) {
                return 0L;
            }
            if (double.class.equals(primitiveType)) {
                return 0d;
            }
            return float.class.equals(primitiveType) ? 0f : 0;
        }
    }

    /** {@code enqueue} must never open a worker transaction, so the manager stays a compile-only seam. */
    private enum NoOpTransactionManager implements PlatformTransactionManager {
        INSTANCE;

        @Override
        public TransactionStatus getTransaction(@Nullable TransactionDefinition definition) {
            throw new UnsupportedOperationException("enqueue must not use a worker transaction");
        }

        @Override
        public void commit(TransactionStatus status) {
            throw new UnsupportedOperationException("enqueue must not use a worker transaction");
        }

        @Override
        public void rollback(TransactionStatus status) {
            throw new UnsupportedOperationException("enqueue must not use a worker transaction");
        }
    }
}
