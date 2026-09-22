package top.egon.cola.component.codegen.ddl;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.alter.AlterExpression;
import net.sf.jsqlparser.statement.comment.Comment;
import net.sf.jsqlparser.statement.create.index.CreateIndex;
import net.sf.jsqlparser.statement.create.table.CheckConstraint;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.ForeignKeyIndex;
import net.sf.jsqlparser.statement.create.table.Index;
import net.sf.jsqlparser.statement.create.table.NamedConstraint;
import net.sf.jsqlparser.statement.drop.Drop;
import top.egon.cola.component.codegen.model.CodegenSchemaBO;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Quote-aware scan of managed PostgreSQL scripts and the only JSQLParser boundary.
 *
 * <p>Partial index predicates are retained by the scanner because the current parser rejects
 * {@code WHERE}. The parser itself is not replaced.</p>
 */
@Slf4j
public class PostgreDdlAdapter {

    public static final String UNSUPPORTED_DDL = "UNSUPPORTED_DDL";

    private static final Pattern ROLE_CONDITION = Pattern.compile(
            "(?is)current_setting\\(\\s*'egon_migration\\.role'\\s*\\)\\s*=\\s*'([A-Z_]+)'");

    private static final Set<String> LENGTH_TYPES = Set.of(
            "varchar", "char", "character", "character varying", "bpchar");

    public List<DdlChangeBO> parseStatements(String sql, String source) {
        if (sql == null) {
            throw diagnostic(UNSUPPORTED_DDL, source, 1, "SQL source is required");
        }
        List<DdlChangeBO> changes = new ArrayList<>();
        for (Segment segment : split(sql)) {
            String body = stripLeadingComments(segment.text()).trim();
            if (body.isEmpty()) {
                continue;
            }
            if (startsWithKeyword(body, "DO")) {
                changes.addAll(parseRoleBlock(segment, source));
                continue;
            }
            changes.add(translate(segment.withText(body), null, source));
        }
        return List.copyOf(changes);
    }

    private List<DdlChangeBO> parseRoleBlock(Segment segment, String source) {
        String body = dollarBody(segment.text());
        if (body == null || containsKeyword(body, "EXECUTE")) {
            throw diagnostic(UNSUPPORTED_DDL, source, segment.line(),
                    "dynamic or unscoped DO block is not supported");
        }
        int begin = indexOfKeyword(body, "BEGIN");
        if (begin < 0) {
            throw diagnostic(UNSUPPORTED_DDL, source, segment.line(), "DO block has no BEGIN");
        }
        String rest = body.substring(begin + "BEGIN".length());
        if (!startsWithKeyword(stripLeadingComments(rest), "IF")) {
            throw diagnostic(UNSUPPORTED_DDL, source, segment.line(), "DO block is not a role branch");
        }
        rest = stripLeadingComments(rest).substring(2);
        List<DdlChangeBO> changes = new ArrayList<>();
        String role = readRole(rest, "MASTER_DATA", source, segment);
        int thenAt = indexOfKeyword(rest, "THEN");
        rest = rest.substring(thenAt + "THEN".length());
        changes.addAll(readBranch(rest, role, segment, source, true));
        return changes;
    }

    private List<DdlChangeBO> readBranch(String rest, String role, Segment segment, String source, boolean master) {
        List<DdlChangeBO> changes = new ArrayList<>();
        int boundary = nextBoundary(rest);
        if (boundary < 0) {
            throw diagnostic(UNSUPPORTED_DDL, source, segment.line(), "role branch is not closed");
        }
        String statements = rest.substring(0, boundary);
        String after = stripLeadingComments(rest.substring(boundary));
        for (Segment inner : split(statements)) {
            String text = stripLeadingComments(inner.text()).trim();
            if (!text.isEmpty()) {
                Segment located = inner.shift(segment.offset(), segment.line());
                changes.add(translate(located.withText(text), role, source));
            }
        }
        if (startsWithKeyword(after, "ELSIF")) {
            if (!master) {
                throw diagnostic(UNSUPPORTED_DDL, source, segment.line(), "only one SHARD branch is supported");
            }
            String elsif = after.substring(5);
            String shardRole = readRole(elsif, "SHARD", source, segment);
            int thenAt = indexOfKeyword(elsif, "THEN");
            return concat(changes, readBranch(elsif.substring(thenAt + 4), shardRole, segment, source, false));
        }
        if (startsWithKeyword(after, "ELSE")) {
            String elseBody = after.substring(4);
            int endIf = indexOfKeyword(elseBody, "END IF");
            if (endIf < 0) {
                throw diagnostic(UNSUPPORTED_DDL, source, segment.line(), "ELSE branch is not closed");
            }
            String elseSql = elseBody.substring(0, endIf).trim();
            if (!elseSql.isEmpty() && !startsWithKeyword(elseSql, "RAISE")) {
                throw diagnostic(UNSUPPORTED_DDL, source, segment.line(), "ELSE branch must only raise");
            }
            if (containsKeyword(elseSql, "CREATE") || containsKeyword(elseSql, "ALTER")
                    || containsKeyword(elseSql, "DROP") || containsKeyword(elseSql, "EXECUTE")) {
                throw diagnostic(UNSUPPORTED_DDL, source, segment.line(), "ELSE branch changes schema");
            }
        }
        return changes;
    }

    private static List<DdlChangeBO> concat(List<DdlChangeBO> left, List<DdlChangeBO> right) {
        List<DdlChangeBO> all = new ArrayList<>(left);
        all.addAll(right);
        return all;
    }

    private String readRole(String sql, String expected, String source, Segment segment) {
        int thenAt = indexOfKeyword(sql, "THEN");
        if (thenAt < 0) {
            throw diagnostic(UNSUPPORTED_DDL, source, segment.line(), "role condition has no THEN");
        }
        var matcher = ROLE_CONDITION.matcher(sql.substring(0, thenAt));
        if (!matcher.find() || !expected.equals(matcher.group(1))) {
            throw diagnostic(UNSUPPORTED_DDL, source, segment.line(),
                    "role branch must test current_setting('egon_migration.role') = '" + expected + "'");
        }
        return expected;
    }

    private DdlChangeBO translate(Segment segment, String role, String source) {
        String sql = segment.text().trim();
        if (startsWithKeyword(sql, "EXECUTE") || startsWithKeyword(sql, "CALL")) {
            throw diagnostic(UNSUPPORTED_DDL, source, segment.line(), "dynamic SQL is not supported");
        }
        String predicate = null;
        String parseable = sql;
        if (startsWithKeyword(sql, "CREATE") && containsKeyword(sql, "INDEX")) {
            int where = lastKeyword(sql, "WHERE");
            if (where >= 0) {
                predicate = sql.substring(where + "WHERE".length()).trim();
                parseable = sql.substring(0, where).trim();
                if (predicate.isEmpty()) {
                    throw diagnostic(UNSUPPORTED_DDL, source, segment.line(), "index predicate is empty");
                }
            }
        }
        Statement statement = parseOne(normalizeWhitespace(parseable), source, segment.line());
        DdlChangeBO change = mapStatement(statement, predicate, source, segment.line());
        if (change.getIndex() != null && sql.toUpperCase(Locale.ROOT).contains(" UNIQUE ")) {
            change.getIndex().setUnique(true);
        }
        change.setRole(role);
        change.setSql(sql);
        change.setSourcePosition(CodegenSchemaBO.SourcePositionBO.builder()
                .file(source)
                .line(segment.line())
                .column(segment.column())
                .offset(segment.offset())
                .build());
        return change;
    }

    private Statement parseOne(String sql, String source, int line) {
        try {
            Statements statements = CCJSqlParserUtil.parseStatements(sql);
            if (statements == null || statements.getStatements() == null || statements.getStatements().size() != 1) {
                throw diagnostic(UNSUPPORTED_DDL, source, line, "statement did not parse as one command");
            }
            return statements.getStatements().get(0);
        } catch (RuntimeException exception) {
            if (exception instanceof DdlParseException) {
                throw exception;
            }
            log.warn("unsupported DDL in {} at line {}", source, line);
            throw diagnostic(UNSUPPORTED_DDL, source, line, "parser rejected statement: " + preview(sql));
        } catch (Exception exception) {
            log.warn("unsupported DDL in {} at line {}", source, line);
            throw diagnostic(UNSUPPORTED_DDL, source, line, "parser rejected statement: " + preview(sql));
        }
    }

    private DdlChangeBO mapStatement(Statement statement, String predicate, String source, int line) {
        if (statement instanceof CreateTable createTable) {
            return DdlChangeBO.builder().kind("CREATE_TABLE").table(table(createTable, line)).build();
        }
        if (statement instanceof CreateIndex createIndex) {
            DdlChangeBO change = DdlChangeBO.builder().kind("CREATE_INDEX").index(index(createIndex, predicate)).build();
            change.setTableName(unquote(createIndex.getTable().getName()));
            return change;
        }
        if (statement instanceof Comment comment) {
            return commentChange(comment);
        }
        if (statement instanceof Alter alter) {
            return alterChange(alter, source, line);
        }
        if (statement instanceof Drop drop) {
            return dropChange(drop, source, line);
        }
        String name = statement.getClass().getSimpleName();
        if ("Insert".equals(name) || "Update".equals(name) || "Delete".equals(name)) {
            return DdlChangeBO.builder().kind("DML").build();
        }
        throw diagnostic(UNSUPPORTED_DDL, source, line, "unsupported statement " + name);
    }

    private static CodegenSchemaBO.TableBO table(CreateTable createTable, int line) {
        String name = unquote(createTable.getTable().getName());
        List<CodegenSchemaBO.ColumnBO> columns = new ArrayList<>();
        List<CodegenSchemaBO.ConstraintBO> constraints = new ArrayList<>();
        int ordinal = 1;
        if (createTable.getColumnDefinitions() != null) {
            for (ColumnDefinition definition : createTable.getColumnDefinitions()) {
                columns.add(column(definition, ordinal));
                constraints.addAll(columnConstraints(definition));
                ordinal++;
            }
        }
        List<CodegenSchemaBO.IndexBO> indexes = new ArrayList<>();
        if (createTable.getIndexes() != null) {
            for (Index index : createTable.getIndexes()) {
                if (index instanceof CheckConstraint check) {
                    constraints.add(CodegenSchemaBO.ConstraintBO.builder()
                            .name(blankToNull(check.getName()))
                            .kind("CHECK")
                            .predicate(check.getExpression() == null ? null : check.getExpression().toString())
                            .build());
                } else if (index instanceof ForeignKeyIndex foreignKey) {
                    constraints.add(CodegenSchemaBO.ConstraintBO.builder()
                            .name(blankToNull(foreignKey.getName()))
                            .kind("FOREIGN_KEY")
                            .columns(names(foreignKey.getColumnsNames()))
                            .predicate(foreignKey.getTable() == null ? null : unquote(foreignKey.getTable().getName()))
                            .build());
                } else if (index instanceof NamedConstraint || index != null) {
                    String kind = constraintKind(index);
                    constraints.add(CodegenSchemaBO.ConstraintBO.builder()
                            .name(blankToNull(index.getName()))
                            .kind(kind)
                            .columns(names(index.getColumnsNames()))
                            .build());
                    if ("PRIMARY_KEY".equals(kind) || "UNIQUE".equals(kind)) {
                        indexes.add(CodegenSchemaBO.IndexBO.builder()
                                .name(blankToNull(index.getName()))
                                .unique(true)
                                .columns(names(index.getColumnsNames()))
                                .build());
                    }
                }
            }
        }
        return CodegenSchemaBO.TableBO.builder()
                .schema(createTable.getTable().getSchemaName())
                .logicalName(name)
                .physicalNames(new ArrayList<>(List.of(name)))
                .columns(columns)
                .constraints(constraints)
                .indexes(indexes)
                .build();
    }

    private static CodegenSchemaBO.ColumnBO column(ColumnDefinition definition, int ordinal) {
        ColDataType dataType = definition.getColDataType();
        List<String> specs = definition.getColumnSpecs() == null ? List.of() : definition.getColumnSpecs();
        String typeName = dataType == null ? null : dataType.getDataType();
        List<String> inlineArguments = List.of();
        if (typeName != null) {
            java.util.regex.Matcher typed = java.util.regex.Pattern
                    .compile("(?i)^([A-Z ]+?)\\s*\\((\\d+)(?:\\s*,\\s*(\\d+))?\\)$")
                    .matcher(typeName.trim());
            if (typed.matches()) {
                typeName = typed.group(1).trim();
                inlineArguments = typed.group(3) == null
                        ? List.of(typed.group(2))
                        : List.of(typed.group(2), typed.group(3));
            }
        }
        if (startsWith(specs, "WITH", "TIME", "ZONE")) {
            typeName = typeName + " WITH TIME ZONE";
            specs = specs.subList(3, specs.size());
        }
        Integer length = null;
        Integer precision = null;
        Integer scale = null;
        List<String> args = dataType == null || dataType.getArgumentsStringList() == null
                || dataType.getArgumentsStringList().isEmpty()
                ? inlineArguments : dataType.getArgumentsStringList();
        if (!args.isEmpty()) {
            if (args.size() >= 2) {
                precision = integer(args.get(0));
                scale = integer(args.get(1));
            } else if (typeName != null && LENGTH_TYPES.contains(typeName.toLowerCase(Locale.ROOT))) {
                length = integer(args.get(0));
            } else {
                precision = integer(args.get(0));
            }
        }
        boolean notNull = hasSequence(specs, "NOT", "NULL") || hasSequence(specs, "PRIMARY", "KEY");
        String defaultExpression = defaultExpression(specs);
        return CodegenSchemaBO.ColumnBO.builder()
                .name(unquote(definition.getColumnName()))
                .ordinal(ordinal)
                .sqlType(typeName == null ? null : typeName.toLowerCase(Locale.ROOT))
                .length(length)
                .precision(precision)
                .scale(scale)
                .nullable(!notNull)
                .defaultExpression(defaultExpression)
                .build();
    }

    private static List<CodegenSchemaBO.ConstraintBO> columnConstraints(ColumnDefinition definition) {
        List<String> specs = definition.getColumnSpecs() == null ? List.of() : definition.getColumnSpecs();
        List<CodegenSchemaBO.ConstraintBO> constraints = new ArrayList<>();
        if (hasSequence(specs, "PRIMARY", "KEY")) {
            constraints.add(CodegenSchemaBO.ConstraintBO.builder()
                    .kind("PRIMARY_KEY")
                    .columns(List.of(unquote(definition.getColumnName())))
                    .build());
        }
        if (hasSequence(specs, "UNIQUE")) {
            constraints.add(CodegenSchemaBO.ConstraintBO.builder()
                    .kind("UNIQUE")
                    .columns(List.of(unquote(definition.getColumnName())))
                    .build());
        }
        return constraints;
    }

    private static CodegenSchemaBO.IndexBO index(CreateIndex createIndex, String predicate) {
        Index index = createIndex.getIndex();
        boolean unique = index != null && "UNIQUE".equalsIgnoreCase(index.getType());
        return CodegenSchemaBO.IndexBO.builder()
                .name(index == null ? null : blankToNull(index.getName()))
                .unique(unique)
                .columns(index == null ? List.of() : names(index.getColumnsNames()))
                .predicate(predicate)
                .build();
    }

    private static DdlChangeBO commentChange(Comment comment) {
        if (comment.getColumn() != null) {
            String table = comment.getColumn().getTable() == null
                    ? null : unquote(comment.getColumn().getTable().getName());
            return DdlChangeBO.builder()
                    .kind("COMMENT_COLUMN")
                    .tableName(table)
                    .column(unquote(comment.getColumn().getColumnName()))
                    .expression(comment.getComment() == null ? null : comment.getComment().getValue())
                    .build();
        }
        return DdlChangeBO.builder()
                .kind("COMMENT_TABLE")
                .tableName(comment.getTable() == null ? null : unquote(comment.getTable().getName()))
                .expression(comment.getComment() == null ? null : comment.getComment().getValue())
                .build();
    }

    private static DdlChangeBO alterChange(Alter alter, String source, int line) {
        if (alter.getAlterExpressions() == null || alter.getAlterExpressions().size() != 1) {
            throw diagnostic(UNSUPPORTED_DDL, source, line, "alter must contain one action");
        }
        AlterExpression expression = alter.getAlterExpressions().get(0);
        String table = unquote(alter.getTable().getName());
        return switch (expression.getOperation()) {
            case ADD -> addColumn(table, expression, source, line);
            case DROP -> DdlChangeBO.builder().kind("DROP_COLUMN").tableName(table)
                    .column(unquote(expression.getColumnName())).build();
            case RENAME -> DdlChangeBO.builder().kind("RENAME_COLUMN").tableName(table)
                    .column(unquote(expression.getColumnOldName()))
                    .newName(unquote(expression.getColumnName())).build();
            case RENAME_TABLE -> DdlChangeBO.builder().kind("RENAME_TABLE").tableName(table)
                    .newName(unquote(expression.getNewTableName())).build();
            case ALTER -> alterColumn(table, expression, source, line);
            default -> throw diagnostic(UNSUPPORTED_DDL, source, line,
                    "unsupported alter " + expression.getOperation());
        };
    }

    private static DdlChangeBO addColumn(String table, AlterExpression expression, String source, int line) {
        if (expression.getColDataTypeList() == null || expression.getColDataTypeList().size() != 1) {
            throw diagnostic(UNSUPPORTED_DDL, source, line, "ADD COLUMN lost its type");
        }
        ColumnDefinition definition = expression.getColDataTypeList().get(0);
        return DdlChangeBO.builder().kind("ADD_COLUMN").tableName(table).columnDefinition(column(definition, 0)).build();
    }

    private static DdlChangeBO alterColumn(String table, AlterExpression expression, String source, int line) {
        if (expression.getColumnSetDefaultList() != null && !expression.getColumnSetDefaultList().isEmpty()) {
            AlterExpression.ColumnSetDefault setDefault = expression.getColumnSetDefaultList().get(0);
            return DdlChangeBO.builder().kind("SET_DEFAULT").tableName(table)
                    .column(unquote(setDefault.getColumnName()))
                    .expression(setDefault.getDefaultValue())
                    .build();
        }
        if (expression.getColDataTypeList() == null || expression.getColDataTypeList().isEmpty()) {
            throw diagnostic(UNSUPPORTED_DDL, source, line, "alter column lost its target");
        }
        ColumnDefinition definition = expression.getColDataTypeList().get(0);
        List<String> specs = definition.getColumnSpecs() == null ? List.of() : definition.getColumnSpecs();
        if (hasSequence(specs, "DROP", "NOT", "NULL")) {
            return DdlChangeBO.builder().kind("SET_NULLABLE").tableName(table)
                    .column(unquote(definition.getColumnName())).nullable(true).build();
        }
        if (hasSequence(specs, "NOT", "NULL")) {
            return DdlChangeBO.builder().kind("SET_NULLABLE").tableName(table)
                    .column(unquote(definition.getColumnName())).nullable(false).build();
        }
        if (definition.getColDataType() != null && definition.getColDataType().getDataType() != null
                && !"SET".equalsIgnoreCase(definition.getColDataType().getDataType())) {
            return DdlChangeBO.builder().kind("SET_TYPE").tableName(table)
                    .columnDefinition(column(definition, 0)).build();
        }
        throw diagnostic(UNSUPPORTED_DDL, source, line, "alter column action is not supported");
    }

    private static DdlChangeBO dropChange(Drop drop, String source, int line) {
        String type = drop.getType() == null ? "" : drop.getType().toUpperCase(Locale.ROOT);
        String name = drop.getName() == null ? null : unquote(drop.getName().getName());
        if ("INDEX".equals(type)) {
            return DdlChangeBO.builder().kind("DROP_INDEX").indexName(name).build();
        }
        if ("TABLE".equals(type)) {
            return DdlChangeBO.builder().kind("DROP_TABLE").tableName(name).build();
        }
        throw diagnostic(UNSUPPORTED_DDL, source, line, "unsupported drop " + type);
    }

    private static String defaultExpression(List<String> specs) {
        for (int index = 0; index < specs.size(); index++) {
            if ("DEFAULT".equalsIgnoreCase(specs.get(index))) {
                List<String> tokens = new ArrayList<>();
                for (int cursor = index + 1; cursor < specs.size(); cursor++) {
                    String token = specs.get(cursor);
                    if (Set.of("NOT", "NULL", "PRIMARY", "UNIQUE", "CHECK", "CONSTRAINT", "REFERENCES")
                            .contains(token.toUpperCase(Locale.ROOT))) {
                        break;
                    }
                    tokens.add(token);
                }
                return tokens.isEmpty() ? null : String.join(" ", tokens);
            }
        }
        return null;
    }

    private static String constraintKind(Index index) {
        String type = index.getType() == null ? "" : index.getType().toUpperCase(Locale.ROOT);
        if (type.contains("PRIMARY")) {
            return "PRIMARY_KEY";
        }
        if (type.contains("UNIQUE")) {
            return "UNIQUE";
        }
        return "INDEX";
    }

    private static boolean startsWith(List<String> specs, String... expected) {
        if (specs.size() < expected.length) {
            return false;
        }
        for (int index = 0; index < expected.length; index++) {
            if (!expected[index].equalsIgnoreCase(specs.get(index))) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasSequence(List<String> specs, String... expected) {
        for (int index = 0; index <= specs.size() - expected.length; index++) {
            boolean match = true;
            for (int cursor = 0; cursor < expected.length; cursor++) {
                if (!expected[cursor].equalsIgnoreCase(specs.get(index + cursor))) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return true;
            }
        }
        return false;
    }

    private static Integer integer(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static List<String> names(List<String> columns) {
        if (columns == null) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (String column : columns) {
            names.add(unquote(column));
        }
        return names;
    }

    private static String unquote(String name) {
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1).replace("\"\"", "\"");
        }
        return trimmed;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    static List<Segment> split(String sql) {
        List<Segment> segments = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int line = 1;
        int column = 1;
        int startLine = 1;
        int startColumn = 1;
        int startOffset = 0;
        int paren = 0;
        for (int index = 0; index < sql.length();) {
            char currentChar = sql.charAt(index);
            if (current.isEmpty()) {
                startLine = line;
                startColumn = column;
                startOffset = index;
            }
            if (currentChar == '-' && index + 1 < sql.length() && sql.charAt(index + 1) == '-') {
                int end = sql.indexOf('\n', index);
                if (end < 0) {
                    current.append(sql.substring(index));
                    break;
                }
                current.append(sql, index, end);
                index = end;
                continue;
            }
            if (currentChar == '/' && index + 1 < sql.length() && sql.charAt(index + 1) == '*') {
                int end = sql.indexOf("*/", index + 2);
                if (end < 0) {
                    throw new DdlParseException(UNSUPPORTED_DDL, null, line, "unterminated block comment");
                }
                appendCounted(current, sql.substring(index, end + 2));
                int newlines = countNewlines(sql.substring(index, end + 2));
                line += newlines;
                index = end + 2;
                column = 1;
                continue;
            }
            if (currentChar == '\'') {
                int end = skipQuoted(sql, index, '\'');
                appendCounted(current, sql.substring(index, end));
                line += countNewlines(sql.substring(index, end));
                index = end;
                column = 1;
                continue;
            }
            if (currentChar == '"') {
                int end = skipQuoted(sql, index, '"');
                current.append(sql, index, end);
                index = end;
                column += end - index;
                continue;
            }
            if (currentChar == '$') {
                String tag = dollarTag(sql, index);
                if (tag != null) {
                    int end = sql.indexOf(tag, index + tag.length());
                    if (end < 0) {
                        throw new DdlParseException(UNSUPPORTED_DDL, null, line, "unterminated dollar quote");
                    }
                    end += tag.length();
                    appendCounted(current, sql.substring(index, end));
                    line += countNewlines(sql.substring(index, end));
                    index = end;
                    column = 1;
                    continue;
                }
            }
            if (currentChar == '(') {
                paren++;
            } else if (currentChar == ')' && paren > 0) {
                paren--;
            }
            if (currentChar == ';' && paren == 0) {
                segments.add(new Segment(current.toString(), startLine, startColumn, startOffset));
                current.setLength(0);
                index++;
                column++;
                continue;
            }
            current.append(currentChar);
            if (currentChar == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
            index++;
        }
        if (!current.toString().isBlank()) {
            segments.add(new Segment(current.toString(), startLine, startColumn, startOffset));
        }
        return segments;
    }

    private static void appendCounted(StringBuilder current, String text) {
        current.append(text);
    }

    private static int skipQuoted(String sql, int start, char quote) {
        int index = start + 1;
        while (index < sql.length()) {
            if (sql.charAt(index) == quote) {
                if (index + 1 < sql.length() && sql.charAt(index + 1) == quote) {
                    index += 2;
                    continue;
                }
                return index + 1;
            }
            index++;
        }
        throw new DdlParseException(UNSUPPORTED_DDL, null, 1, "unterminated quoted literal");
    }

    private static String dollarTag(String sql, int start) {
        if (start + 1 >= sql.length() || sql.charAt(start) != '$') {
            return null;
        }
        int end = start + 1;
        if (sql.charAt(end) == '$') {
            return "$$";
        }
        while (end < sql.length() && (Character.isLetterOrDigit(sql.charAt(end)) || sql.charAt(end) == '_')) {
            end++;
        }
        if (end < sql.length() && sql.charAt(end) == '$' && end > start + 1) {
            return sql.substring(start, end + 1);
        }
        return null;
    }

    private static String dollarBody(String statement) {
        String trimmed = stripLeadingComments(statement).trim();
        int dollar = trimmed.indexOf('$');
        if (dollar < 0) {
            return null;
        }
        String tag = dollarTag(trimmed, dollar);
        if (tag == null) {
            return null;
        }
        int bodyStart = dollar + tag.length();
        int bodyEnd = trimmed.lastIndexOf(tag);
        if (bodyEnd <= bodyStart) {
            return null;
        }
        return trimmed.substring(bodyStart, bodyEnd);
    }

    static String stripLeadingComments(String sql) {
        String rest = sql;
        while (true) {
            String trimmed = rest.stripLeading();
            if (trimmed.startsWith("--")) {
                int newline = trimmed.indexOf('\n');
                rest = newline < 0 ? "" : trimmed.substring(newline + 1);
                continue;
            }
            if (trimmed.startsWith("/*")) {
                int end = trimmed.indexOf("*/");
                if (end < 0) {
                    return trimmed;
                }
                rest = trimmed.substring(end + 2);
                continue;
            }
            return trimmed;
        }
    }

    private static boolean startsWithKeyword(String sql, String keyword) {
        String trimmed = stripLeadingComments(sql);
        if (trimmed.length() < keyword.length() || !trimmed.regionMatches(true, 0, keyword, 0, keyword.length())) {
            return false;
        }
        return trimmed.length() == keyword.length()
                || !Character.isLetterOrDigit(trimmed.charAt(keyword.length()));
    }

    private static boolean containsKeyword(String sql, String keyword) {
        return indexOfKeyword(sql, keyword) >= 0;
    }

    private static int indexOfKeyword(String sql, String keyword) {
        int paren = 0;
        for (int index = 0; index < sql.length();) {
            char current = sql.charAt(index);
            if (current == '-' && index + 1 < sql.length() && sql.charAt(index + 1) == '-') {
                int end = sql.indexOf('\n', index);
                index = end < 0 ? sql.length() : end + 1;
                continue;
            }
            if (current == '\'') {
                index = skipQuoted(sql, index, '\'');
                continue;
            }
            if (current == '"') {
                index = skipQuoted(sql, index, '"');
                continue;
            }
            if (current == '$') {
                String tag = dollarTag(sql, index);
                if (tag != null) {
                    int end = sql.indexOf(tag, index + tag.length());
                    index = end < 0 ? sql.length() : end + tag.length();
                    continue;
                }
            }
            if (current == '(') {
                paren++;
                index++;
                continue;
            }
            if (current == ')' && paren > 0) {
                paren--;
                index++;
                continue;
            }
            if (paren == 0 && sql.regionMatches(true, index, keyword, 0, keyword.length())
                    && boundary(sql, index, keyword.length())) {
                return index;
            }
            index++;
        }
        return -1;
    }

    private static int lastKeyword(String sql, String keyword) {
        int found = -1;
        int from = 0;
        while (from < sql.length()) {
            int next = indexOfKeyword(sql.substring(from), keyword);
            if (next < 0) {
                return found;
            }
            found = from + next;
            from = found + keyword.length();
        }
        return found;
    }

    private static int nextBoundary(String sql) {
        int elsif = indexOfKeyword(sql, "ELSIF");
        int elseAt = indexOfKeyword(sql, "ELSE");
        int endIf = indexOfKeyword(sql, "END IF");
        int boundary = -1;
        for (int candidate : new int[] {elsif, elseAt, endIf}) {
            if (candidate >= 0 && (boundary < 0 || candidate < boundary)) {
                boundary = candidate;
            }
        }
        return boundary;
    }

    private static boolean boundary(String sql, int start, int length) {
        boolean before = start == 0 || !Character.isLetterOrDigit(sql.charAt(start - 1));
        int end = start + length;
        boolean after = end >= sql.length() || !Character.isLetterOrDigit(sql.charAt(end));
        return before && after;
    }

    private static int countNewlines(String text) {
        int count = 0;
        for (int index = 0; index < text.length(); index++) {
            if (text.charAt(index) == '\n') {
                count++;
            }
        }
        return count;
    }

    private static DdlParseException diagnostic(String code, String file, int line, String message) {
        return new DdlParseException(code, file, line, message);
    }

    private static String preview(String sql) {
        String flat = sql.replaceAll("\\s+", " ").trim();
        return flat.length() <= 180 ? flat : flat.substring(0, 180);
    }

    private static String normalizeWhitespace(String sql) {
        StringBuilder normalized = new StringBuilder();
        boolean pendingSpace = false;
        for (int index = 0; index < sql.length();) {
            char current = sql.charAt(index);
            if (current == '\'' || current == '"') {
                int end = skipQuoted(sql, index, current);
                appendSpace(normalized, pendingSpace);
                pendingSpace = false;
                normalized.append(sql, index, end);
                index = end;
                continue;
            }
            if (current == '-' && index + 1 < sql.length() && sql.charAt(index + 1) == '-') {
                int end = sql.indexOf('\n', index);
                index = end < 0 ? sql.length() : end + 1;
                pendingSpace = true;
                continue;
            }
            if (current == '/' && index + 1 < sql.length() && sql.charAt(index + 1) == '*') {
                int end = sql.indexOf("*/", index + 2);
                index = end < 0 ? sql.length() : end + 2;
                pendingSpace = true;
                continue;
            }
            if (Character.isWhitespace(current)) {
                pendingSpace = true;
                index++;
                continue;
            }
            appendSpace(normalized, pendingSpace);
            pendingSpace = false;
            normalized.append(current);
            index++;
        }
        return normalized.toString().trim();
    }

    private static void appendSpace(StringBuilder normalized, boolean pendingSpace) {
        if (pendingSpace && !normalized.isEmpty()) {
            normalized.append(' ');
        }
    }

    public static final class DdlParseException extends RuntimeException {

        private final String code;

        private final String file;

        private final int line;

        public DdlParseException(String code, String file, int line, String message) {
            super(message);
            this.code = code;
            this.file = file;
            this.line = line;
        }

        public String getCode() {
            return code;
        }

        public String getFile() {
            return file;
        }

        public int getLine() {
            return line;
        }
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.experimental.Accessors(chain = true)
    @lombok.Builder
    public static final class DdlChangeBO {

        private String kind;

        private String role;

        private String sql;

        private String tableName;

        private String column;

        private String newName;

        private String expression;

        private Boolean nullable;

        private String indexName;

        private top.egon.cola.component.codegen.model.CodegenSchemaBO.TableBO table;

        private top.egon.cola.component.codegen.model.CodegenSchemaBO.ColumnBO columnDefinition;

        private top.egon.cola.component.codegen.model.CodegenSchemaBO.IndexBO index;

        private top.egon.cola.component.codegen.model.CodegenSchemaBO.SourcePositionBO sourcePosition;
    }

    private record Segment(String text, int line, int column, int offset) {

        private Segment withText(String text) {
            return new Segment(text, line, column, offset);
        }

        private Segment shift(int baseOffset, int baseLine) {
            return new Segment(text, baseLine + line - 1, column, baseOffset + offset);
        }
    }
}
