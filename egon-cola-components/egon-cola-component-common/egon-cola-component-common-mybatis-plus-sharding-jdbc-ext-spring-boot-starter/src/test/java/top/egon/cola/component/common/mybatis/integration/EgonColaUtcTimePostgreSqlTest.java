package top.egon.cola.component.common.mybatis.integration;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import jakarta.validation.Validation;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mybatis.spring.SqlSessionTemplate;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.common.mybatis.handler.EgonColaMetaObjectHandler;
import top.egon.cola.component.common.mybatis.interceptor.EgonColaModelValidationInterceptor;
import top.egon.cola.component.common.mybatis.model.EgonColaIdentifierGenerator;
import top.egon.cola.component.common.mybatis.schema.support.time.TimeTypesPO;
import top.egon.cola.component.common.mybatis.support.TestTenantIdProvider;
import top.egon.cola.component.common.mybatis.support.TestUserIdProvider;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "EGON_MP_PG_MODEL_TEST", matches = "true")
@ResourceLock("JVM_DEFAULT_TIME_ZONE")
class EgonColaUtcTimePostgreSqlTest {

    private static final Instant WINDOW = Instant.parse("2026-10-13T06:07:08.123456Z");
    private static final Instant AUDIT = Instant.parse("2026-10-13T06:07:08.987654321Z");

    @Test
    void existingMyBatisInstantBindingSurvivesJvmAndSessionZones() throws Exception {
        // TimeZone is used only to simulate the JVM environment; all model values use java.time.
        java.util.TimeZone original = java.util.TimeZone.getDefault();
        PGSimpleDataSource admin = source();
        String schema = "egon_utc_" + UUID.randomUUID().toString().replace("-", "");
        JdbcTemplate adminJdbc = new JdbcTemplate(admin);
        adminJdbc.execute("CREATE SCHEMA " + schema);
        TestTenantIdProvider tenant = new TestTenantIdProvider();
        TestUserIdProvider user = new TestUserIdProvider();
        try (var validators = Validation.buildDefaultValidatorFactory()) {
            PGSimpleDataSource scoped = source();
            scoped.setCurrentSchema(schema);
            JdbcTemplate jdbc = new JdbcTemplate(scoped);
            jdbc.execute("""
                    CREATE TABLE time_types (
                        id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL,
                        create_user_id VARCHAR(128) NOT NULL, update_user_id VARCHAR(128) NOT NULL,
                        create_time TIMESTAMPTZ(6) NOT NULL, update_time TIMESTAMPTZ(6) NOT NULL,
                        deleted_at TIMESTAMP(6) WITHOUT TIME ZONE, version BIGINT NOT NULL,
                        occurred_at TIMESTAMPTZ(6), offset_at TIMESTAMPTZ(6),
                        local_at TIMESTAMP(6) WITHOUT TIME ZONE, pure_date DATE, local_time TIME(6) WITHOUT TIME ZONE
                    )
                    """);
            tenant.set(41L);
            user.set("utc-fixture");
            SnowflakeIdGenerator.initialize(0L, Duration.ofMillis(5));
            GlobalConfig global = new GlobalConfig();
            global.setDbConfig(new GlobalConfig.DbConfig());
            global.setIdentifierGenerator(new EgonColaIdentifierGenerator());
            global.setMetaObjectHandler(new EgonColaMetaObjectHandler(user, Clock.fixed(AUDIT, ZoneOffset.UTC)));
            MybatisConfiguration configuration = new MybatisConfiguration();
            configuration.setMapUnderscoreToCamelCase(true);
            MybatisPlusInterceptor plugins = new MybatisPlusInterceptor();
            plugins.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
                @Override
                public Expression getTenantId() { return new LongValue(EgonColaTenantIdProvider.currentTenantId()); }
            }));
            plugins.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
            MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
            factory.setConfiguration(configuration);
            factory.setGlobalConfig(global);
            factory.setDataSource(scoped);
            factory.setPlugins(plugins, new EgonColaModelValidationInterceptor(validators.getValidator(),
                    new EgonColaMybatisPlusProperties()));
            var sessionFactory = factory.getObject();
            sessionFactory.getConfiguration().addMapper(UtcTimeMapper.class);
            UtcTimeMapper mapper = new SqlSessionTemplate(sessionFactory).getMapper(UtcTimeMapper.class);
            List<String> zones = List.of("UTC", "Asia/Shanghai", "America/New_York");
            for (int index = 0; index < zones.size(); index++) {
                java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(zones.get(index)));
                scoped.setOptions("-c TimeZone=" + zones.get(index));
                TimeTypesPO row = new TimeTypesPO().setOccurredAt(WINDOW)
                        .setOffsetAt(WINDOW.atOffset(ZoneOffset.ofHours(8)))
                        .setLocalAt(LocalDateTime.of(2026, 10, 13, 14, 7, 8, 123456000))
                        .setPureDate(LocalDate.of(2026, 10, 13)).setLocalTime(LocalTime.of(14, 7, 8, 123456000));
                assertThat(mapper.insert(row)).isEqualTo(1);
                // SqlSessionTemplate opens another connection outside a transaction, using these session options.
                scoped.setOptions("-c TimeZone=" + zones.get((index + 1) % zones.size()));
                TimeTypesPO loaded = mapper.selectById(row.getId());
                assertThat(loaded.getOccurredAt()).isEqualTo(WINDOW);
                assertThat(loaded.getOffsetAt().toInstant()).isEqualTo(WINDOW);
                assertThat(loaded.getLocalAt()).isEqualTo(row.getLocalAt());
                assertThat(loaded.getPureDate()).isEqualTo(row.getPureDate());
                assertThat(loaded.getLocalTime()).isEqualTo(row.getLocalTime());
                assertThat(loaded.getCreateTime()).isEqualTo(AUDIT.truncatedTo(java.time.temporal.ChronoUnit.MICROS));
                assertThat(loaded.getUpdateTime()).isEqualTo(loaded.getCreateTime());
                assertThat(loaded.getDeletedAt()).isNull();
                assertThat(loaded.getVersion()).isZero();
                assertThat(loaded.getTenantId()).isEqualTo(41L);
            }
            assertThat(jdbc.queryForObject("""
                    SELECT data_type FROM information_schema.columns
                    WHERE table_schema=? AND table_name='time_types' AND column_name='occurred_at'
                    """, String.class, schema)).isEqualTo("timestamp with time zone");
            assertThat(jdbc.queryForObject("""
                    SELECT datetime_precision FROM information_schema.columns
                    WHERE table_schema=? AND table_name='time_types' AND column_name='occurred_at'
                    """, Integer.class, schema)).isEqualTo(6);
            assertThat(jdbc.queryForObject("""
                    SELECT data_type FROM information_schema.columns
                    WHERE table_schema=? AND table_name='time_types' AND column_name='deleted_at'
                    """, String.class, schema)).isEqualTo("timestamp without time zone");
        } finally {
            java.util.TimeZone.setDefault(original);
            tenant.clear();
            user.clear();
            // Delete only this explicitly enabled fixture's own UUID schema.
            adminJdbc.execute("DROP SCHEMA " + schema + " CASCADE");
        }
    }

    @Test
    void utcTemporalDefaultsSurviveJvmAndSessionZones() throws Exception {
        java.util.TimeZone original = java.util.TimeZone.getDefault();
        try (var connection = source().getConnection()) {
            connection.setAutoCommit(false);
            try (var sql = connection.createStatement()) {
                sql.execute("""
                        CREATE TEMPORARY TABLE egon_utc_defaults (
                            occurred_at TIMESTAMPTZ(6) DEFAULT CURRENT_TIMESTAMP(6),
                            legacy_at TIMESTAMP(6) WITHOUT TIME ZONE DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
                            utc_date DATE DEFAULT ((CURRENT_TIMESTAMP AT TIME ZONE 'UTC')::date),
                            utc_time TIME(6) WITHOUT TIME ZONE DEFAULT ((CURRENT_TIMESTAMP AT TIME ZONE 'UTC')::time(6)),
                            fixed_at TIMESTAMPTZ(6) DEFAULT '2026-10-13T00:30:00.123456Z',
                            pure_date DATE DEFAULT DATE '2026-10-13',
                            pure_time TIME(6) DEFAULT TIME '06:07:08.123456',
                            starts_at TIMESTAMPTZ(6), ends_at TIMESTAMPTZ(6),
                            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE DEFAULT NULL
                        ) ON COMMIT DROP
                        """);
                Instant transactionTime;
                try (var rows = sql.executeQuery("SELECT CURRENT_TIMESTAMP(6)")) {
                    assertThat(rows.next()).isTrue();
                    transactionTime = rows.getObject(1, java.time.OffsetDateTime.class).toInstant();
                }
                LocalDateTime utc = LocalDateTime.ofInstant(transactionTime, ZoneOffset.UTC);
                for (String zone : List.of("UTC", "Asia/Shanghai", "America/New_York", "Pacific/Kiritimati")) {
                    java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(zone));
                    // Values are the fixed fixture whitelist, never external SQL input.
                    sql.execute("SET LOCAL TIME ZONE '" + zone + "'");
                    try (var rows = sql.executeQuery("INSERT INTO egon_utc_defaults DEFAULT VALUES RETURNING *")) {
                        assertThat(rows.next()).isTrue();
                        assertThat(rows.getObject("occurred_at", java.time.OffsetDateTime.class).toInstant()).isEqualTo(transactionTime);
                        assertThat(rows.getObject("legacy_at", LocalDateTime.class)).isEqualTo(utc);
                        assertThat(rows.getObject("utc_date", LocalDate.class)).isEqualTo(utc.toLocalDate());
                        assertThat(rows.getObject("utc_time", LocalTime.class)).isEqualTo(utc.toLocalTime());
                        assertThat(rows.getObject("fixed_at", java.time.OffsetDateTime.class).toInstant())
                                .isEqualTo(Instant.parse("2026-10-13T00:30:00.123456Z"));
                        assertThat(rows.getObject("pure_date", LocalDate.class)).isEqualTo(LocalDate.of(2026, 10, 13));
                        assertThat(rows.getObject("pure_time", LocalTime.class)).isEqualTo(LocalTime.of(6, 7, 8, 123456000));
                        assertThat(rows.getObject("starts_at")).isNull();
                        assertThat(rows.getObject("ends_at")).isNull();
                        assertThat(rows.getObject("deleted_at")).isNull();
                    }
                }
            } finally {
                connection.rollback();
            }
        } finally {
            java.util.TimeZone.setDefault(original);
        }
    }

    private static PGSimpleDataSource source() {
        PGSimpleDataSource source = new PGSimpleDataSource();
        source.setURL(System.getenv("EGON_MP_PG_URL"));
        source.setUser(System.getenv("EGON_MP_PG_USER"));
        source.setPassword(System.getenv("EGON_MP_PG_PASSWORD"));
        return source;
    }

    public interface UtcTimeMapper extends EgonColaMapper<TimeTypesPO> {
    }
}
