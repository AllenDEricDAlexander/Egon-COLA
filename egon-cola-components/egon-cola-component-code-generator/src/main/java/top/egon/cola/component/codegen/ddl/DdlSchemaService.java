package top.egon.cola.component.codegen.ddl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.codegen.ddl.PostgreDdlAdapter.DdlChangeBO;
import top.egon.cola.component.codegen.ddl.PostgreDdlAdapter.DdlParseException;
import top.egon.cola.component.codegen.model.CodegenConfigBO;
import top.egon.cola.component.codegen.model.CodegenSchemaBO;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlManifestBO;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Replays offline schema or manifest input into one normalized schema.
 * It never opens a connection and never calls the DDL runner.
 */
@Slf4j
@RequiredArgsConstructor
public class DdlSchemaService {

    public static final String CHECKSUM_DRIFT = "CHECKSUM_DRIFT";

    public static final String MISSING_SCHEMA_BASELINE = "MISSING_SCHEMA_BASELINE";

    public static final String PHYSICAL_SCHEMA_MISMATCH = "PHYSICAL_SCHEMA_MISMATCH";

    private static final Set<String> TECHNICAL_TABLES = Set.of("ddl_history");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Qualifier("postgreDdlAdapter")
    private final PostgreDdlAdapter adapter;

    public CodegenSchemaBO read(CodegenConfigBO config) {
        return read(config, Map.of(), List.of());
    }

    public CodegenSchemaBO read(
            CodegenConfigBO config,
            Map<String, List<String>> logicalPhysicalMappings,
            List<String> observedChecksumPrefix) {
        if (config == null || config.getInput() == null || config.getInput().getMode() == null) {
            throw new DdlParseException(PostgreDdlAdapter.UNSUPPORTED_DDL, null, 1, "DDL input is required");
        }
        LoadedInput loaded = load(config.getInput(), observedChecksumPrefix);
        List<CodegenSchemaBO.TableBO> tables = new ArrayList<>();
        for (LoadedScript script : loaded.scripts()) {
            for (DdlChangeBO change : adapter.parseStatements(script.sql(), script.source())) {
                apply(tables, change);
            }
        }
        tables.removeIf(table -> TECHNICAL_TABLES.contains(table.getLogicalName().toLowerCase(java.util.Locale.ROOT)));
        List<CodegenSchemaBO.TableBO> merged = merge(tables, logicalPhysicalMappings);
        return CodegenSchemaBO.builder()
                .inputFingerprint(loaded.fingerprint())
                .versionChecksumPrefix(loaded.prefix())
                .tables(merged)
                .build()
                .snapshot();
    }

    private void apply(List<CodegenSchemaBO.TableBO> tables, DdlChangeBO change) {
        switch (change.getKind()) {
            case "CREATE_TABLE" -> {
                CodegenSchemaBO.TableBO table = change.getTable();
                table.setRole(change.getRole());
                tables.add(table);
            }
            case "CREATE_INDEX" -> require(tables, change.getTableName()).getIndexes().add(change.getIndex());
            case "DROP_INDEX" -> dropIndex(tables, change.getIndexName());
            case "DROP_TABLE" -> tables.removeIf(table -> sameTable(table, change.getTableName()));
            case "COMMENT_COLUMN" -> column(require(tables, change.getTableName()), change.getColumn())
                    .setComment(change.getExpression());
            case "COMMENT_TABLE" -> require(tables, change.getTableName());
            case "ADD_COLUMN" -> addColumn(require(tables, change.getTableName()), change.getColumnDefinition());
            case "DROP_COLUMN" -> require(tables, change.getTableName()).getColumns()
                    .removeIf(column -> change.getColumn().equals(column.getName()));
            case "RENAME_COLUMN" -> renameColumn(require(tables, change.getTableName()), change.getColumn(), change.getNewName());
            case "RENAME_TABLE" -> renameTable(require(tables, change.getTableName()), change.getNewName());
            case "SET_NULLABLE" -> column(require(tables, change.getTableName()), change.getColumn())
                    .setNullable(change.getNullable());
            case "SET_DEFAULT" -> column(require(tables, change.getTableName()), change.getColumn())
                    .setDefaultExpression(change.getExpression());
            case "SET_TYPE" -> copyType(column(require(tables, change.getTableName()), change.getColumnDefinition().getName()),
                    change.getColumnDefinition());
            case "DML" -> log.debug("ignoring non-structural statement");
            default -> throw new DdlParseException(PostgreDdlAdapter.UNSUPPORTED_DDL, source(change), line(change),
                    "cannot apply " + change.getKind());
        }
    }

    private LoadedInput load(CodegenConfigBO.InputBO input, List<String> observedChecksumPrefix) {
        if ("schema".equals(input.getMode())) {
            List<LoadedScript> scripts = new ArrayList<>();
            StringBuilder fingerprint = new StringBuilder();
            for (String schemaFile : input.getSchemaFiles()) {
                Path path = resolve(input.getResourceRoot(), schemaFile);
                byte[] bytes = readBytes(path);
                String hash = sha256(bytes);
                fingerprint.append(hash).append('\n');
                scripts.add(new LoadedScript(path.toString(), new String(bytes, java.nio.charset.StandardCharsets.UTF_8)));
            }
            return new LoadedInput(scripts, fingerprint.toString().trim(), fingerprint.toString().trim());
        }
        if (!"manifest".equals(input.getMode())) {
            throw new DdlParseException(PostgreDdlAdapter.UNSUPPORTED_DDL, input.getManifest(), 1, "unsupported input mode");
        }
        Path manifestPath = resolve(input.getResourceRoot(), input.getManifest());
        EgonColaDdlManifestBO manifest = readManifest(manifestPath);
        List<String> observed = observedChecksumPrefix == null ? List.of() : observedChecksumPrefix;
        if (observed.size() > manifest.scripts().size()) {
            throw new DdlParseException(CHECKSUM_DRIFT, manifestPath.toString(), 1, "observed checksum prefix is longer than history");
        }
        List<LoadedScript> scripts = new ArrayList<>();
        StringBuilder prefix = new StringBuilder();
        for (int index = 0; index < manifest.scripts().size(); index++) {
            EgonColaDdlManifestBO.ScriptBO script = manifest.scripts().get(index);
            Path path = resolve(input.getResourceRoot(), script.path());
            byte[] bytes = readBytes(path);
            String actual = sha256(bytes);
            if (!actual.equals(script.sha256())) {
                throw new DdlParseException(CHECKSUM_DRIFT, path.toString(), 1, "script bytes do not match the manifest checksum");
            }
            if (index < observed.size() && !observed.get(index).equals(script.sha256())) {
                throw new DdlParseException(CHECKSUM_DRIFT, path.toString(), 1, "immutable checksum prefix changed");
            }
            prefix.append(script.version()).append(':').append(script.sha256()).append('\n');
            scripts.add(new LoadedScript(path.toString(), new String(bytes, java.nio.charset.StandardCharsets.UTF_8)));
        }
        String prefixText = prefix.toString().trim();
        return new LoadedInput(scripts, sha256(prefixText.getBytes(java.nio.charset.StandardCharsets.UTF_8)), prefixText);
    }

    private static EgonColaDdlManifestBO readManifest(Path path) {
        try {
            JsonNode root = MAPPER.readTree(readBytes(path));
            List<EgonColaDdlManifestBO.ScriptBO> scripts = new ArrayList<>();
            for (JsonNode script : root.path("scripts")) {
                scripts.add(new EgonColaDdlManifestBO.ScriptBO(
                        text(script, "version"), text(script, "path"), text(script, "sha256")));
            }
            return new EgonColaDdlManifestBO(text(root, "family"), scripts);
        } catch (IllegalArgumentException exception) {
            throw new DdlParseException(CHECKSUM_DRIFT, path.toString(), 1, "manifest contract was rejected");
        } catch (IOException exception) {
            throw new DdlParseException(CHECKSUM_DRIFT, path.toString(), 1, "manifest could not be read");
        }
    }

    private static List<CodegenSchemaBO.TableBO> merge(
            List<CodegenSchemaBO.TableBO> tables, Map<String, List<String>> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            return tables;
        }
        Map<String, CodegenSchemaBO.TableBO> byName = new LinkedHashMap<>();
        for (CodegenSchemaBO.TableBO table : tables) {
            byName.put(table.getLogicalName(), table);
        }
        for (Map.Entry<String, List<String>> mapping : mappings.entrySet()) {
            List<CodegenSchemaBO.TableBO> physicals = new ArrayList<>();
            for (String physicalName : mapping.getValue()) {
                CodegenSchemaBO.TableBO physical = byName.remove(physicalName);
                if (physical == null) {
                    throw new DdlParseException(MISSING_SCHEMA_BASELINE, physicalName, 1,
                            "physical table is not in the replayed schema");
                }
                physicals.add(physical);
            }
            CodegenSchemaBO.TableBO baseline = physicals.get(0);
            for (CodegenSchemaBO.TableBO physical : physicals) {
                if (!sameStructure(baseline, physical)) {
                    throw new DdlParseException(PHYSICAL_SCHEMA_MISMATCH, mapping.getKey(), 1,
                            "physical tables do not share field and constraint semantics");
                }
            }
            CodegenSchemaBO.TableBO merged = CodegenSchemaBO.TableBO.builder()
                    .schema(baseline.getSchema())
                    .logicalName(mapping.getKey())
                    .role(baseline.getRole())
                    .physicalNames(new ArrayList<>(mapping.getValue()))
                    .routeKeys(new ArrayList<>(baseline.getRouteKeys()))
                    .columns(baseline.getColumns())
                    .constraints(baseline.getConstraints())
                    .indexes(baseline.getIndexes())
                    .build();
            byName.put(mapping.getKey(), merged);
        }
        return new ArrayList<>(byName.values());
    }

    private static boolean sameStructure(CodegenSchemaBO.TableBO left, CodegenSchemaBO.TableBO right) {
        if (!Objects.equals(left.getRole(), right.getRole()) || left.getColumns().size() != right.getColumns().size()) {
            return false;
        }
        for (int index = 0; index < left.getColumns().size(); index++) {
            CodegenSchemaBO.ColumnBO first = left.getColumns().get(index);
            CodegenSchemaBO.ColumnBO second = right.getColumns().get(index);
            if (!Objects.equals(first.getName(), second.getName())
                    || !Objects.equals(first.getSqlType(), second.getSqlType())
                    || !Objects.equals(first.getLength(), second.getLength())
                    || !Objects.equals(first.getPrecision(), second.getPrecision())
                    || !Objects.equals(first.getScale(), second.getScale())
                    || !Objects.equals(first.getNullable(), second.getNullable())
                    || !Objects.equals(normalizeDefault(first.getDefaultExpression()), normalizeDefault(second.getDefaultExpression()))) {
                return false;
            }
        }
        return signatures(left.getIndexes()).equals(signatures(right.getIndexes()))
                && constraintSignatures(left.getConstraints()).equals(constraintSignatures(right.getConstraints()));
    }

    private static List<String> signatures(List<CodegenSchemaBO.IndexBO> indexes) {
        List<String> signatures = new ArrayList<>();
        if (indexes != null) {
            for (CodegenSchemaBO.IndexBO index : indexes) {
                signatures.add(index.getUnique() + "|" + index.getColumns() + "|" + index.getPredicate());
            }
        }
        signatures.sort(String::compareTo);
        return signatures;
    }

    private static List<String> constraintSignatures(List<CodegenSchemaBO.ConstraintBO> constraints) {
        List<String> signatures = new ArrayList<>();
        if (constraints != null) {
            for (CodegenSchemaBO.ConstraintBO constraint : constraints) {
                signatures.add(constraint.getKind() + "|" + constraint.getColumns() + "|" + constraint.getPredicate());
            }
        }
        signatures.sort(String::compareTo);
        return signatures;
    }

    private static String normalizeDefault(String expression) {
        return expression == null ? null : expression.replace("'", "").trim();
    }

    private static void addColumn(CodegenSchemaBO.TableBO table, CodegenSchemaBO.ColumnBO column) {
        column.setOrdinal(table.getColumns().size() + 1);
        table.getColumns().add(column);
    }

    private static void copyType(CodegenSchemaBO.ColumnBO target, CodegenSchemaBO.ColumnBO source) {
        target.setSqlType(source.getSqlType());
        target.setLength(source.getLength());
        target.setPrecision(source.getPrecision());
        target.setScale(source.getScale());
    }

    private static void renameColumn(CodegenSchemaBO.TableBO table, String from, String to) {
        column(table, from).setName(to);
        for (CodegenSchemaBO.IndexBO index : table.getIndexes()) {
            replace(index.getColumns(), from, to);
        }
        for (CodegenSchemaBO.ConstraintBO constraint : table.getConstraints()) {
            replace(constraint.getColumns(), from, to);
        }
    }

    private static void replace(List<String> names, String from, String to) {
        if (names == null) {
            return;
        }
        for (int index = 0; index < names.size(); index++) {
            if (from.equals(names.get(index))) {
                names.set(index, to);
            }
        }
    }

    private static void renameTable(CodegenSchemaBO.TableBO table, String newName) {
        String previous = table.getLogicalName();
        table.setLogicalName(newName);
        List<String> physicals = new ArrayList<>();
        for (String physical : table.getPhysicalNames()) {
            physicals.add(physical.equals(previous) ? newName : physical);
        }
        if (physicals.isEmpty()) {
            physicals.add(newName);
        }
        table.setPhysicalNames(physicals);
    }

    private static void dropIndex(List<CodegenSchemaBO.TableBO> tables, String indexName) {
        for (CodegenSchemaBO.TableBO table : tables) {
            table.getIndexes().removeIf(index -> indexName.equals(index.getName()));
        }
    }

    private static CodegenSchemaBO.TableBO require(List<CodegenSchemaBO.TableBO> tables, String name) {
        for (CodegenSchemaBO.TableBO table : tables) {
            if (sameTable(table, name)) {
                return table;
            }
        }
        throw new DdlParseException(MISSING_SCHEMA_BASELINE, name, 1, "ALTER target has no schema baseline");
    }

    private static boolean sameTable(CodegenSchemaBO.TableBO table, String name) {
        return name.equals(table.getLogicalName()) || table.getPhysicalNames().contains(name);
    }

    private static CodegenSchemaBO.ColumnBO column(CodegenSchemaBO.TableBO table, String name) {
        for (CodegenSchemaBO.ColumnBO column : table.getColumns()) {
            if (name.equals(column.getName())) {
                return column;
            }
        }
        throw new DdlParseException(MISSING_SCHEMA_BASELINE, name, 1, "column is not in the baseline");
    }

    private static Path resolve(String root, String location) {
        Path path = Path.of(location);
        if (root == null || root.isBlank() || path.isAbsolute()) {
            return path;
        }
        return Path.of(root).resolve(path);
    }

    private static byte[] readBytes(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException exception) {
            throw new DdlParseException(MISSING_SCHEMA_BASELINE, path.toString(), 1, "SQL input could not be read");
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required", exception);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.asText().isBlank()) {
            throw new IllegalArgumentException(field);
        }
        return value.asText();
    }

    private static String source(DdlChangeBO change) {
        return change.getSourcePosition() == null ? null : change.getSourcePosition().getFile();
    }

    private static int line(DdlChangeBO change) {
        return change.getSourcePosition() == null || change.getSourcePosition().getLine() == null
                ? 1 : change.getSourcePosition().getLine();
    }

    private record LoadedInput(List<LoadedScript> scripts, String fingerprint, String prefix) {
    }

    private record LoadedScript(String source, String sql) {
    }
}
