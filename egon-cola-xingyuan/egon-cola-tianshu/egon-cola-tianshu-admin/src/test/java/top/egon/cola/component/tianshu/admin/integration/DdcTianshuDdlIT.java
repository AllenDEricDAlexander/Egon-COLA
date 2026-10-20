package top.egon.cola.component.tianshu.admin.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlManifestBO;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlResult;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlTargetBO;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlTargetBO.RoleEnum;
import top.egon.cola.component.common.mybatis.ddl.EgonColaPostgreDdlRunner;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcTianshuDdlIT {

    private static final String MANIFEST_RESOURCE = "db/egon-mp/repository-manifest.json";
    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void blankSchemaInitializesAndReentersWhileDriftIsRejectedAndUnknownCommitIsVerified() throws Exception {
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("TIANSHU_AI_PERSISTENCE_IT_ENABLED")),
                "requires an explicitly configured disposable PostgreSQL DDL target");
        PGSimpleDataSource dataSource = dataSource();
        String schema = "tianshu_it_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        boolean schemaCreated = false;
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA \"" + schema + "\"");
            schemaCreated = true;
        }

        try {
            dataSource.setCurrentSchema(schema);
            EgonColaDdlManifestBO manifest = manifest();
            EgonColaDdlTargetBO target = new EgonColaDdlTargetBO("tianshu-it", schema, RoleEnum.MASTER_DATA,
                    commitUnknownAfterServerCommit(dataSource), manifest, routeFingerprint());
            List<EgonColaDdlResult> initialized = runner().run(List.of(target));
            assertThat(initialized).hasSize(manifest.scripts().size() - manifest.startingIndex(null))
                    .allSatisfy(result -> assertThat(result.status()).isEqualTo(EgonColaDdlResult.StatusEnum.APPLIED));

            EgonColaDdlTargetBO normalTarget = new EgonColaDdlTargetBO("tianshu-it", schema,
                    RoleEnum.MASTER_DATA, dataSource, manifest, routeFingerprint());
            assertThat(runner().run(List.of(normalTarget)))
                    .allSatisfy(result -> assertThat(result.status()).isEqualTo(EgonColaDdlResult.StatusEnum.SKIPPED));

            EgonColaDdlManifestBO.ScriptBO lastScript = manifest.scripts().getLast();
            try (Connection connection = dataSource.getConnection(); var update = connection.prepareStatement(
                    "UPDATE ddl_history SET checksum = ? WHERE script = ? AND type = 'SQL'")) {
                update.setString(1, "0".repeat(64));
                update.setString(2, lastScript.path());
                assertThat(update.executeUpdate()).isEqualTo(1);
            }
            assertThatThrownBy(() -> runner().run(List.of(normalTarget)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DDL_TARGET_FAILED");
            try (Connection connection = dataSource.getConnection(); var update = connection.prepareStatement(
                    "UPDATE ddl_history SET checksum = ? WHERE script = ? AND type = 'SQL'")) {
                update.setString(1, lastScript.sha256());
                update.setString(2, lastScript.path());
                assertThat(update.executeUpdate()).isEqualTo(1);
            }
        } finally {
            dataSource.setCurrentSchema("public");
            if (schemaCreated) {
                try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
                    statement.execute("DROP SCHEMA \"" + schema + "\" CASCADE");
                }
            }
        }
    }

    private static EgonColaPostgreDdlRunner runner() {
        return new EgonColaPostgreDdlRunner(new ValidationUtils(VALIDATOR_FACTORY.getValidator()),
                new PathMatchingResourcePatternResolver(), Clock.systemUTC(), Duration.ofSeconds(10),
                Duration.ofSeconds(30));
    }

    private static EgonColaDdlManifestBO manifest() throws Exception {
        try (var input = DdcTianshuDdlIT.class.getClassLoader().getResourceAsStream(MANIFEST_RESOURCE)) {
            if (input == null) throw new IllegalStateException("Missing Tianshu DDL manifest resource");
            return new ObjectMapper().readValue(input, EgonColaDdlManifestBO.class);
        }
    }

    private static PGSimpleDataSource dataSource() {
        String url = requiredEnvironment("TIANSHU_AI_PERSISTENCE_IT_DDL_JDBC_URL");
        if (!url.startsWith("jdbc:postgresql:")) {
            throw new IllegalStateException("TIANSHU_AI_PERSISTENCE_IT_DDL_JDBC_URL must target PostgreSQL");
        }
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(url);
        dataSource.setUser(requiredEnvironment("TIANSHU_AI_PERSISTENCE_IT_DDL_DB_USERNAME"));
        dataSource.setPassword(requiredEnvironment("TIANSHU_AI_PERSISTENCE_IT_DDL_DB_PASSWORD"));
        return dataSource;
    }

    private static String routeFingerprint() {
        String value = System.getenv("TIANSHU_AI_PERSISTENCE_IT_DDL_ROUTE_FINGERPRINT");
        return value == null || value.isBlank() ? "a".repeat(64) : value;
    }

    private static DataSource commitUnknownAfterServerCommit(DataSource delegate) {
        AtomicBoolean failNextCommit = new AtomicBoolean(true);
        return (DataSource) Proxy.newProxyInstance(DataSource.class.getClassLoader(),
                new Class<?>[]{DataSource.class}, (proxy, method, args) -> {
                    Object result = invoke(delegate, method, args);
                    if ("getConnection".equals(method.getName()) && result instanceof Connection connection) {
                        return Proxy.newProxyInstance(Connection.class.getClassLoader(),
                                new Class<?>[]{Connection.class}, (connectionProxy, connectionMethod, connectionArgs) -> {
                                    if ("commit".equals(connectionMethod.getName())
                                            && failNextCommit.compareAndSet(true, false)) {
                                        connection.commit();
                                        throw new SQLException("connection lost after commit", "08006");
                                    }
                                    return invoke(connection, connectionMethod, connectionArgs);
                                });
                    }
                    return result;
                });
    }

    private static Object invoke(Object target, java.lang.reflect.Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException failure) {
            throw failure.getCause();
        }
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException(name + " is required for this IT");
        return value;
    }
}
