package top.egon.cola.component.yuheng.admin.shared.dao.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * 中文说明：{@code GatewayVectorTypeHandler} 是 pgvector {@code vector} 列的 MyBatis 绑定器，Java 侧向量表示为精确长度的 {@code float[]}，写入前校验非空长度与每个分量的有限性，只借助 JDK JDBC 与 pgvector 文本字面量，不引入第二个 PostgreSQL 驱动或 pgvector-java 依赖。
 * English summary: {@code GatewayVectorTypeHandler} binds a pgvector {@code vector} column to an exactly sized {@code float[]}: it validates a non-empty length and a finite value per component before binding, and reads back exactly the components the server returned, using only JDK JDBC plus the pgvector text literal so no second driver or pgvector-java dependency appears.
 *
 * 用法 / Usage: 由 MyBatis 实例化，因此不加 Spring 或数据 Lombok 注解；在 resultMap 或 {@code @TableField(typeHandler = ...)} 上显式引用，绑定值形如 {@code [1.0,2.0,3.0]} 并以 {@code Types.OTHER} 发送。/ Instantiated by MyBatis, so it carries no Spring or data-Lombok annotation; reference it explicitly from a resultMap or {@code @TableField(typeHandler = ...)} - the bound value is a {@code [1.0,2.0,3.0]} literal sent as {@code Types.OTHER}.
 */
public class GatewayVectorTypeHandler extends BaseTypeHandler<float[]> {

    /**
     * 中文说明：表示写入被拒绝时使用的错误标识，向量长度为空。
     * English summary: Error code raised when a binding is rejected because the vector carries no component.
     *
     * 用法 / Usage: 随 {@link IllegalArgumentException} 抛出，供调用方与测试断言。/ Thrown inside an {@link IllegalArgumentException} for callers and tests to assert on.
     */
    private static final String EMPTY_VECTOR = "EMBEDDING_VECTOR_EMPTY";

    /**
     * 中文说明：表示写入被拒绝时使用的错误标识，向量分量不是有限数值。
     * English summary: Error code raised when a binding is rejected because a component is not finite.
     *
     * 用法 / Usage: 随 {@link IllegalArgumentException} 抛出，供调用方与测试断言。/ Thrown inside an {@link IllegalArgumentException} for callers and tests to assert on.
     */
    private static final String NON_FINITE_VECTOR = "EMBEDDING_VECTOR_NOT_FINITE";

    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, float[] parameter,
                                    JdbcType jdbcType) throws SQLException {
        // Types.OTHER lets PostgreSQL infer the vector domain from the target column instead of
        // sending a character varying the server cannot assign to it.
        statement.setObject(index, toLiteral(parameter), Types.OTHER);
    }

    @Override
    public float[] getNullableResult(ResultSet resultSet, String columnName) throws SQLException {
        return fromLiteral(resultSet.getString(columnName));
    }

    @Override
    public float[] getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException {
        return fromLiteral(resultSet.getString(columnIndex));
    }

    @Override
    public float[] getNullableResult(CallableStatement statement, int columnIndex) throws SQLException {
        return fromLiteral(statement.getString(columnIndex));
    }

    private static String toLiteral(float[] vector) {
        requireNonEmpty(vector);
        StringBuilder literal = new StringBuilder(vector.length * 8 + 2);
        literal.append('[');
        for (int index = 0; index < vector.length; index++) {
            float component = vector[index];
            if (!Float.isFinite(component)) {
                throw new IllegalArgumentException(NON_FINITE_VECTOR);
            }
            if (index > 0) {
                literal.append(',');
            }
            literal.append(component);
        }
        return literal.append(']').toString();
    }

    private static float[] fromLiteral(String text) throws SQLException {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        if (trimmed.length() < 2 || trimmed.charAt(0) != '['
                || trimmed.charAt(trimmed.length() - 1) != ']') {
            throw new SQLException("EMBEDDING_VECTOR_LITERAL_MALFORMED");
        }
        String body = trimmed.substring(1, trimmed.length() - 1).trim();
        String[] tokens = body.isEmpty() ? new String[0] : body.split(",", -1);
        requireNonEmpty(tokens.length);
        float[] vector = new float[tokens.length];
        for (int index = 0; index < tokens.length; index++) {
            float component;
            try {
                component = Float.parseFloat(tokens[index].trim());
            } catch (NumberFormatException failure) {
                throw new SQLException("EMBEDDING_VECTOR_LITERAL_MALFORMED", failure);
            }
            if (!Float.isFinite(component)) {
                throw new SQLException(NON_FINITE_VECTOR);
            }
            vector[index] = component;
        }
        return vector;
    }

    private static void requireNonEmpty(float[] vector) {
        if (vector.length == 0) {
            throw new IllegalArgumentException(EMPTY_VECTOR);
        }
    }

    private static void requireNonEmpty(int count) {
        if (count == 0) {
            throw new IllegalArgumentException(EMPTY_VECTOR);
        }
    }
}
