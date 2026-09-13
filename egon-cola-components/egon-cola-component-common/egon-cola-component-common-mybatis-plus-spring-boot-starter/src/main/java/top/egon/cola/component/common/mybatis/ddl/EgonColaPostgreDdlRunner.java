package top.egon.cola.component.common.mybatis.ddl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlManifestBO.ScriptBO;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlResult.StatusEnum;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Synchronous PostgreSQL initialization facade. Each SQL version and its history row share one
 * transaction. A later target failure never erases an earlier target's committed work.
 */
@Slf4j
@RequiredArgsConstructor
public final class EgonColaPostgreDdlRunner {

    private static final int MAX_SCRIPT_BYTES = 10 * 1024 * 1024;
    private static final Pattern NONTRANSACTIONAL = Pattern.compile(
            "(?is)(?:^|;)\\s*(?:BEGIN|START\\s+TRANSACTION|COMMIT|ROLLBACK|ABORT|END|PREPARE\\s+TRANSACTION|VACUUM|DISCARD|"
                    + "(?:CREATE|DROP)\\s+(?:DATABASE|TABLESPACE)|ALTER\\s+SYSTEM)\\b|\\bCONCURRENTLY\\b");

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;
    @Qualifier("egonColaDdlResourceResolver")
    private final ResourcePatternResolver resources;
    @Qualifier("egonColaMybatisPlusClock")
    private final Clock clock;
    @Qualifier("egonColaDdlLockTimeout")
    private final Duration lockTimeout;
    @Qualifier("egonColaDdlStatementTimeout")
    private final Duration statementTimeout;

    public List<EgonColaDdlResult> run(List<EgonColaDdlTargetBO> targets) {
        if (targets == null || targets.isEmpty()) {
            throw new IllegalArgumentException("DDL targets must not be empty");
        }
        requireTimeout(lockTimeout);
        requireTimeout(statementTimeout);
        List<EgonColaDdlTargetBO> ordered = targets.stream().map(validationUtils::validate)
                .sorted(Comparator.comparing(EgonColaDdlTargetBO::alias).thenComparing(EgonColaDdlTargetBO::schema)).toList();
        Map<ScriptBO, byte[]> scripts = loadScripts(ordered);
        Set<String> names = new HashSet<>();
        Map<javax.sql.DataSource, Set<String>> sources = new IdentityHashMap<>();
        for (EgonColaDdlTargetBO target : ordered) {
            if (!names.add(target.alias() + '.' + target.schema())
                    || !sources.computeIfAbsent(target.dataSource(), ignored -> new HashSet<>()).add(target.schema())) {
                throw new IllegalArgumentException("DUPLICATE_DDL_TARGET");
            }
        }
        List<EgonColaDdlResult> results = new ArrayList<>();
        for (EgonColaDdlTargetBO target : ordered) {
            results.addAll(runTarget(target, scripts));
        }
        return List.copyOf(results);
    }

    private Map<ScriptBO, byte[]> loadScripts(List<EgonColaDdlTargetBO> targets) {
        Map<ScriptBO, byte[]> loaded = new HashMap<>();
        for (EgonColaDdlTargetBO target : targets) {
            for (ScriptBO script : target.manifest().scripts()) {
                if (loaded.containsKey(script)) {
                    continue;
                }
                try {
                    Resource[] matches = resources.getResources("classpath*:" + script.path());
                    if (matches.length != 1) {
                        throw new IllegalArgumentException("AMBIGUOUS_SQL_RESOURCE: " + script.path());
                    }
                    byte[] content;
                    try (var input = matches[0].getInputStream()) {
                        content = input.readNBytes(MAX_SCRIPT_BYTES + 1);
                    }
                    if (content.length == 0 || content.length > MAX_SCRIPT_BYTES) {
                        throw new IllegalArgumentException("SQL_RESOURCE_SIZE: " + script.path());
                    }
                    if (!script.sha256().equals(HexFormat.of().formatHex(digest(content)))) {
                        throw new IllegalArgumentException("CHECKSUM_MISMATCH: " + script.path());
                    }
                    verifyTransactionalScript(new String(content, StandardCharsets.UTF_8));
                    loaded.put(script, content);
                } catch (IOException exception) {
                    throw new IllegalStateException("SQL_RESOURCE_UNREADABLE: " + script.path(), exception);
                }
            }
        }
        return loaded;
    }

    private List<EgonColaDdlResult> runTarget(EgonColaDdlTargetBO target, Map<ScriptBO, byte[]> scripts) {
        List<EgonColaDdlResult> results = new ArrayList<>();
        try (Connection connection = target.dataSource().getConnection()) {
            if (!connection.getAutoCommit()) {
                throw new IllegalStateException("DDL_REQUIRES_OWN_CONNECTION");
            }
            connection.setAutoCommit(false);
            String path = "validation";
            try {
                validatePrimary(connection, target);
                configureAndLock(connection, target);
                int installed = installedPrefix(connection, target);
                boolean committed = false;
                for (int index = 0; index < target.manifest().scripts().size(); index++) {
                    ScriptBO script = target.manifest().scripts().get(index);
                    path = script.path();
                    Instant start = clock.instant();
                    if (committed) {
                        configureAndLock(connection, target);
                        installed = installedPrefix(connection, target);
                        committed = false;
                    }
                    StatusEnum status = StatusEnum.SKIPPED;
                    if (index >= installed) {
                        executePostgreSqlScript(connection, scripts.get(script));
                        insertHistory(connection, target, script, elapsed(start));
                        try {
                            connection.commit();
                        } catch (SQLException commitFailure) {
                            rollback(connection, commitFailure);
                            if (!confirmCommit(target, index, commitFailure)) {
                                throw new IllegalStateException("DDL_COMMIT_UNKNOWN: " + path, commitFailure);
                            }
                        }
                        committed = true;
                        status = StatusEnum.APPLIED;
                    }
                    results.add(validationUtils.validate(new EgonColaDdlResult(target.alias(), target.schema(),
                            script.version(), script.sha256(), status, elapsed(start))));
                }
                if (!committed) {
                    connection.rollback(); // Release a read-only verification lock without a fake migration commit.
                }
                return results;
            } catch (SQLException | RuntimeException failure) {
                rollback(connection, failure);
                throw new IllegalStateException("DDL_TARGET_FAILED: " + target.alias() + '/' + target.schema()
                        + " script=" + path + " SQLState=" + sqlState(failure) + " reason="
                        + (failure instanceof IllegalStateException ? failure.getMessage() : failure.getClass().getSimpleName()), failure);
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("DDL_CONNECTION_FAILED: " + target.alias() + " SQLState=" + failure.getSQLState(), failure);
        }
    }

    private void validatePrimary(Connection connection, EgonColaDdlTargetBO target) throws SQLException {
        if (!"PostgreSQL".equals(connection.getMetaData().getDatabaseProductName())) {
            throw new IllegalStateException("POSTGRESQL_REQUIRED");
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT current_schema(), pg_is_in_recovery(), current_setting('transaction_read_only')::boolean")) {
            statement.setQueryTimeout((int) Math.max(1, statementTimeout.toSeconds()));
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next() || !target.schema().equals(row.getString(1))) {
                    throw new IllegalStateException("SCHEMA_MISMATCH");
                }
                if (row.getBoolean(2) || row.getBoolean(3) || connection.isReadOnly()) {
                    throw new IllegalStateException("PRIMARY_REQUIRED");
                }
            }
        }
    }

    private void configureAndLock(Connection connection, EgonColaDdlTargetBO target) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT set_config('lock_timeout', ?, true), set_config('statement_timeout', ?, true), "
                        + "set_config('egon_migration.role', ?, true), set_config('search_path', ?, true)")) {
            statement.setString(1, lockTimeout.toMillis() + "ms");
            statement.setString(2, statementTimeout.toMillis() + "ms");
            statement.setString(3, target.role().name());
            statement.setString(4, '"' + target.schema() + "\",pg_catalog");
            statement.execute();
        }
        try (PreparedStatement statement = connection.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
            statement.setLong(1, ByteBuffer.wrap(digest(("egon-ddl:" + target.schema()).getBytes(StandardCharsets.UTF_8))).getLong());
            statement.execute();
        }
    }

    private int installedPrefix(Connection connection, EgonColaDdlTargetBO target) throws SQLException {
        List<String> relations = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT c.relname FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace n ON n.oid=c.relnamespace "
                        + "WHERE n.nspname=? AND c.relkind IN ('r','p','v','m','S','f') ORDER BY c.relname")) {
            statement.setString(1, target.schema());
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    relations.add(rows.getString(1));
                }
            }
        }
        if (!relations.contains("ddl_history")) {
            if (!relations.isEmpty()) {
                throw new IllegalStateException("REBUILD_REQUIRED");
            }
            return 0;
        }
        int count = 0;
        try (PreparedStatement statement = connection.prepareStatement("SELECT script,type,version,checksum,route_fingerprint,tenant_id FROM "
                + target.getDdlGenerator().getDdlHistory() + " ORDER BY version")) {
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    if (count >= target.manifest().scripts().size()) {
                        throw new IllegalStateException("MANIFEST_PREFIX_MISMATCH");
                    }
                    ScriptBO script = target.manifest().scripts().get(count);
                    if (!script.path().equals(rows.getString(1)) || !"SQL".equals(rows.getString(2))
                            || !script.version().equals(rows.getString(3)) || rows.getLong(6) != 0) {
                        throw new IllegalStateException("MANIFEST_PREFIX_MISMATCH");
                    }
                    if (!script.sha256().equals(rows.getString(4))) {
                        throw new IllegalStateException("CHECKSUM_MISMATCH");
                    }
                    if (!target.routeFingerprint().equals(rows.getString(5))) {
                        throw new IllegalStateException("ROUTE_FINGERPRINT_MISMATCH");
                    }
                    count++;
                }
            }
        }
        if (count == 0) {
            throw new IllegalStateException("REBUILD_REQUIRED: unmanaged empty ddl_history");
        }
        return count;
    }

    private void insertHistory(Connection connection, EgonColaDdlTargetBO target, ScriptBO script, Duration elapsed) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO " + target.getDdlGenerator().getDdlHistory()
                + " (tenant_id,script,type,version,checksum,installed_on,execution_ms,route_fingerprint) "
                + "VALUES (0,?,?,?,?,CURRENT_TIMESTAMP,?,?)")) {
            statement.setString(1, script.path());
            statement.setString(2, "SQL");
            statement.setString(3, script.version());
            statement.setString(4, script.sha256());
            statement.setLong(5, elapsed.toMillis());
            statement.setString(6, target.routeFingerprint());
            if (statement.executeUpdate() != 1) {
                throw new IllegalStateException("DDL_HISTORY_WRITE_FAILED");
            }
        }
    }

    private boolean confirmCommit(EgonColaDdlTargetBO target, int index, SQLException original) {
        try (Connection confirmation = target.dataSource().getConnection()) {
            confirmation.setAutoCommit(false);
            try {
                validatePrimary(confirmation, target);
                configureAndLock(confirmation, target);
                return installedPrefix(confirmation, target) > index;
            } finally {
                confirmation.rollback();
            }
        } catch (SQLException | RuntimeException failure) {
            original.addSuppressed(failure);
            return false;
        }
    }

    private Duration elapsed(Instant start) {
        Duration duration = Duration.between(start, clock.instant());
        return duration.isNegative() ? Duration.ZERO : duration;
    }

    private static void rollback(Connection connection, Throwable failure) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
        }
    }

    private static String sqlState(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql) {
                return sql.getSQLState();
            }
        }
        return "none";
    }

    private static byte[] digest(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("JDK must provide SHA-256", failure);
        }
    }

    private static void requireTimeout(Duration timeout) {
        if (timeout == null || timeout.compareTo(Duration.ofMillis(1)) < 0 || timeout.compareTo(Duration.ofMillis(Integer.MAX_VALUE)) > 0) {
            throw new IllegalArgumentException("DDL timeout must be positive and bounded in milliseconds");
        }
    }

    static List<String> splitPostgreSqlStatements(String sql) {
        List<String> statements = new ArrayList<>();
        int start = 0;
        int blockDepth = 0;
        String dollarQuote = null;
        char quote = 0;
        boolean lineComment = false;
        for (int index = 0; index < sql.length(); index++) {
            char current = sql.charAt(index);
            char next = index + 1 < sql.length() ? sql.charAt(index + 1) : 0;
            if (lineComment) {
                if (current == '\n') { lineComment = false; }
                continue;
            }
            if (blockDepth > 0) {
                if (current == '/' && next == '*') { blockDepth++; index++; }
                else if (current == '*' && next == '/') { blockDepth--; index++; }
                continue;
            }
            if (dollarQuote != null) {
                if (sql.startsWith(dollarQuote, index)) {
                    index += dollarQuote.length() - 1;
                    dollarQuote = null;
                }
                continue;
            }
            if (quote != 0) {
                if (current == quote) {
                    if (next == quote) { index++; }
                    else { quote = 0; }
                } else if (current == '\\' && quote == '\'' && next != 0) {
                    index++;
                }
                continue;
            }
            if (current == '-' && next == '-') { lineComment = true; index++; continue; }
            if (current == '/' && next == '*') { blockDepth = 1; index++; continue; }
            if (current == '\'' || current == '"') { quote = current; continue; }
            if (current == '$' && (index == 0 || !isPostgreSqlIdentifierPart(sql.charAt(index - 1)))) {
                int delimiterEnd = dollarDelimiterEnd(sql, index);
                if (delimiterEnd >= 0) {
                    dollarQuote = sql.substring(index, delimiterEnd + 1);
                    index = delimiterEnd;
                    continue;
                }
            }
            if (current == ';') {
                addStatement(statements, sql.substring(start, index));
                start = index + 1;
            }
        }
        if (quote != 0 || blockDepth != 0 || dollarQuote != null) {
            throw new IllegalArgumentException("UNTERMINATED_POSTGRESQL_SQL_BODY");
        }
        addStatement(statements, sql.substring(start));
        return List.copyOf(statements);
    }

    private static boolean containsSql(String statement) {
        for (int index = 0; index < statement.length();) {
            char current = statement.charAt(index);
            char next = index + 1 < statement.length() ? statement.charAt(index + 1) : 0;
            if (Character.isWhitespace(current) || current == ';') { index++; }
            else if (current == '-' && next == '-') {
                int end = statement.indexOf('\n', index + 2);
                index = end < 0 ? statement.length() : end + 1;
            } else if (current == '/' && next == '*') {
                int depth = 1;
                index += 2;
                while (index < statement.length() && depth > 0) {
                    if (statement.startsWith("/*", index)) { depth++; index += 2; }
                    else if (statement.startsWith("*/", index)) { depth--; index += 2; }
                    else { index++; }
                }
            } else { return true; }
        }
        return false;
    }

    private static void executePostgreSqlScript(Connection connection, byte[] content) throws SQLException {
        String sql = new String(content, StandardCharsets.UTF_8);
        try (Statement statement = connection.createStatement()) {
            for (String command : splitPostgreSqlStatements(sql)) {
                statement.execute(command);
            }
        }
    }

    private static int dollarDelimiterEnd(String sql, int start) {
        int index = start + 1;
        if (index < sql.length() && sql.charAt(index) == '$') { return index; }
        if (index >= sql.length() || !isPostgreSqlIdentifierStart(sql.charAt(index))) { return -1; }
        for (index++; index < sql.length(); index++) {
            char current = sql.charAt(index);
            if (current == '$') { return index; }
            if (!isPostgreSqlIdentifierPart(current)) { return -1; }
        }
        return -1;
    }

    private static boolean isPostgreSqlIdentifierStart(char value) {
        return value == '_' || value >= 'a' && value <= 'z' || value >= 'A' && value <= 'Z';
    }

    private static boolean isPostgreSqlIdentifierPart(char value) {
        return isPostgreSqlIdentifierStart(value) || value >= '0' && value <= '9' || value == '$';
    }

    private static void addStatement(List<String> statements, String source) {
        String command = source.trim();
        if (!command.isEmpty() && containsSql(command)) { statements.add(command); }
    }

    private static void verifyTransactionalScript(String sql) {
        // Mask quoted bodies and comments before looking for top-level transaction control. PostgreSQL
        // itself prohibits transaction control inside a DO body executed within our explicit transaction.
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < sql.length();) {
            char current = sql.charAt(i);
            if (sql.startsWith("--", i)) {
                int end = sql.indexOf('\n', i + 2);
                i = end < 0 ? sql.length() : end;
                code.append(' ');
            } else if (sql.startsWith("/*", i)) {
                int depth = 1;
                i += 2;
                while (i < sql.length() && depth > 0) {
                    if (sql.startsWith("/*", i)) { depth++; i += 2; }
                    else if (sql.startsWith("*/", i)) { depth--; i += 2; }
                    else { i++; }
                }
                if (depth != 0) { throw new IllegalArgumentException("UNTERMINATED_SQL_COMMENT"); }
                code.append(' ');
            } else if (current == '\'' || current == '"') {
                char quote = current;
                boolean closed = false;
                for (i++; i < sql.length(); i++) {
                    if (sql.charAt(i) == quote) {
                        if (i + 1 < sql.length() && sql.charAt(i + 1) == quote) { i++; }
                        else { i++; closed = true; break; }
                    }
                }
                if (!closed) { throw new IllegalArgumentException("UNTERMINATED_SQL_LITERAL"); }
                code.append(' ');
            } else if (current == '$' && sql.substring(i).matches("(?s)^\\$(?:[a-zA-Z_][a-zA-Z0-9_]*)?\\$.*")) {
                int delimiterEnd = sql.indexOf('$', i + 1);
                String delimiter = sql.substring(i, delimiterEnd + 1);
                int end = sql.indexOf(delimiter, delimiterEnd + 1);
                if (end < 0) { throw new IllegalArgumentException("UNTERMINATED_SQL_BODY"); }
                i = end + delimiter.length();
                code.append(' ');
            } else {
                code.append(current);
                i++;
            }
        }
        if (NONTRANSACTIONAL.matcher(code).find()) {
            throw new IllegalArgumentException("NONTRANSACTIONAL_SQL");
        }
    }
}
