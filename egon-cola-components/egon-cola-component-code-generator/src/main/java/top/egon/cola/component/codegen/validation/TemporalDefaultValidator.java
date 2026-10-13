package top.egon.cola.component.codegen.validation;

import top.egon.cola.component.codegen.model.CodegenSchemaBO;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.regex.Pattern;

/** Checks the supported temporal DEFAULT expressions without relying on a database session zone. */
public final class TemporalDefaultValidator {

    public static final String TIME_DEFAULT_UNSUPPORTED = "TIME_DEFAULT_UNSUPPORTED";

    private static final String NOW = "(?:CURRENT_TIMESTAMP(?:\\s*\\(\\s*[0-6]\\s*\\))?|"
            + "(?:NOW|TRANSACTION_TIMESTAMP|STATEMENT_TIMESTAMP|CLOCK_TIMESTAMP)\\s*\\(\\s*\\))";
    private static final Pattern UTC_TIME = Pattern.compile(NOW + "\\s+AT\\s+TIME\\s+ZONE\\s+'UTC'", Pattern.CASE_INSENSITIVE);
    private static final Pattern LITERAL = Pattern.compile("(?is)^(?:(DATE|TIME(?:\\s*\\(\\s*[0-6]\\s*\\))?(?:\\s+WITHOUT\\s+TIME\\s+ZONE)?|"
            + "TIMESTAMP(?:\\s*\\(\\s*[0-6]\\s*\\))?(?:\\s+(?:WITH|WITHOUT)\\s+TIME\\s+ZONE)?|TIMESTAMPTZ(?:\\s*\\(\\s*[0-6]\\s*\\))?)\\s+)?"
            + "'([^']*)'(?:\\s*::\\s*(.+))?$");

    private TemporalDefaultValidator() {
    }

    public static boolean supports(CodegenSchemaBO.ColumnBO column) {
        String kind = kind(column.getSqlType());
        String expression = column.getDefaultExpression();
        if (kind == null || expression == null || expression.isBlank() || "NULL".equalsIgnoreCase(expression.trim())) {
            return true;
        }
        expression = unwrap(expression.trim());
        if ("instant".equals(kind) && expression.matches("(?i)" + NOW)) {
            return true;
        }
        if ("timestamp".equals(kind) && UTC_TIME.matcher(expression).matches()) {
            return true;
        }
        if ("date".equals(kind) || "time".equals(kind)) {
            var cast = Pattern.compile("(?is)^(.+)::\\s*(DATE|TIME(?:\\s*\\(\\s*[0-6]\\s*\\))?(?:\\s+WITHOUT\\s+TIME\\s+ZONE)?)$").matcher(expression);
            if (cast.matches() && kind.equals(kind(cast.group(2))) && UTC_TIME.matcher(unwrap(cast.group(1).trim())).matches()) {
                return true;
            }
        }
        var literal = LITERAL.matcher(expression);
        if (!literal.matches() || literal.group(1) != null && !kind.equals(kind(literal.group(1)))
                || literal.group(3) != null && !kind.equals(kind(literal.group(3)))) {
            return false;
        }
        String value = literal.group(2);
        try {
            switch (kind) {
                case "instant" -> {
                    return OffsetDateTime.parse(value.replace(' ', 'T')).getOffset().equals(ZoneOffset.UTC);
                }
                case "timestamp" -> LocalDateTime.parse(value.replace(' ', 'T'));
                case "date" -> LocalDate.parse(value);
                case "time" -> LocalTime.parse(value);
                default -> { return false; }
            }
            return true;
        } catch (DateTimeException invalidLiteral) {
            return false;
        }
    }

    public static void requireUtc(CodegenSchemaBO.ColumnBO column) {
        if (!supports(column)) {
            throw new IllegalArgumentException(TIME_DEFAULT_UNSUPPORTED + ": " + column.getName());
        }
    }

    private static String kind(String type) {
        if (type == null) {
            return null;
        }
        type = type.toLowerCase(Locale.ROOT).replaceAll("\\(\\s*[0-6]\\s*\\)", "").trim().replaceAll("\\s+", " ");
        return switch (type) {
            case "timestamptz", "timestamp with time zone" -> "instant";
            case "timestamp", "timestamp without time zone" -> "timestamp";
            case "date" -> "date";
            case "time", "time without time zone" -> "time";
            default -> null;
        };
    }

    private static String unwrap(String value) {
        while (value.startsWith("(") && value.endsWith(")")) {
            int depth = 0;
            boolean quoted = false;
            boolean outer = true;
            for (int index = 0; index < value.length() - 1; index++) {
                char character = value.charAt(index);
                if (character == '\'') {
                    quoted = !quoted;
                } else if (!quoted && character == '(') {
                    depth++;
                } else if (!quoted && character == ')' && --depth == 0) {
                    outer = false;
                    break;
                }
            }
            if (!outer || quoted || depth != 1) {
                break;
            }
            value = value.substring(1, value.length() - 1).trim();
        }
        return value;
    }
}
