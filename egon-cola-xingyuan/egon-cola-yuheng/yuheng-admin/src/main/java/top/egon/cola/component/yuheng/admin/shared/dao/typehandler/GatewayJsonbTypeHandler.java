package top.egon.cola.component.yuheng.admin.shared.dao.typehandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * 中文说明：{@code GatewayJsonbTypeHandler} 是 MyBatis 的 jsonb 列绑定器，负责把声明的 JSON 载体（Jackson 节点或结构化集合）编码为 PostgreSQL {@code jsonb}，不做 DTO/PO 的业务往返映射。
 * English summary: {@code GatewayJsonbTypeHandler} is the MyBatis binding for {@code jsonb} columns: it encodes the declared JSON carrier (a Jackson node or a structured collection) into PostgreSQL {@code jsonb} and performs no DTO/PO business roundtrip.
 *
 * 用法 / Usage: 由 MyBatis 实例化，因此不加 Spring 或数据 Lombok 注解；在 resultMap 或 {@code @TableField(typeHandler = ...)} 上显式引用，Java 类型为 {@code String} 时按已编码 JSON 文本原样读写。/ Instantiated by MyBatis, so it carries no Spring or data-Lombok annotation; reference it explicitly from a resultMap or {@code @TableField(typeHandler = ...)}, where a {@code String} carrier is read and written as the already-encoded JSON text.
 */
public class GatewayJsonbTypeHandler extends BaseTypeHandler<Object> {

    /**
     * 中文说明：保存 Jackson 序列化器，禁止 default typing，只承载列值的 JSON 编解码。
     * English summary: Holds the Jackson serializer used for the column value only; default typing stays disabled.
     *
     * 用法 / Usage: 由本类型内部使用；/ Used internally by this handler and never exposed to callers.
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 中文说明：保存列声明的 Java 类型，用于反序列化时构造精确的 Jackson 目标类型。
     * English summary: Holds the Java type declared for the column, used to build the exact Jackson target type on read.
     *
     * 用法 / Usage: 由 MyBatis 通过带 {@code Class} 参数的构造器注入；/ Supplied by MyBatis through the {@code Class} constructor.
     */
    private final Class<?> rawType;

    /**
     * 中文说明：保存与 {@code rawType} 对应的 Jackson 类型，缺省为 {@code Object}。
     * English summary: Holds the Jackson type resolved from {@code rawType}, defaulting to {@code Object}.
     *
     * 用法 / Usage: 仅在读取列值时使用。/ Used only while reading a column value.
     */
    private final JavaType javaType;

    /**
     * 中文说明：创建未声明 Java 类型的处理器，此时 JSON 按通用结构读取。
     * English summary: Creates the handler without a declared Java type, so JSON is read as a generic structure.
     *
     * 用法 / Usage: 供 MyBatis 在无法解析属性类型时回退使用。/ The fallback MyBatis uses when no property type is resolvable.
     */
    public GatewayJsonbTypeHandler() {
        this(Object.class);
    }

    /**
     * 中文说明：按列声明的 Java 类型创建处理器。
     * English summary: Creates the handler for the Java type declared on the column.
     *
     * 用法 / Usage: 由 MyBatis 反射调用；/ Invoked reflectively by MyBatis with the resolved property type.
     * @param type 列的 Java 类型 / the Java type declared for the column.
     */
    public GatewayJsonbTypeHandler(Class<?> type) {
        this.rawType = type == null ? Object.class : type;
        this.javaType = OBJECT_MAPPER.getTypeFactory().constructType(this.rawType);
    }

    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, Object parameter,
                                    JdbcType jdbcType) throws SQLException {
        // Types.OTHER keeps the driver from sending character varying, so PostgreSQL infers jsonb
        // from the target column instead of rejecting an untyped string.
        statement.setObject(index, encode(parameter), Types.OTHER);
    }

    @Override
    public Object getNullableResult(ResultSet resultSet, String columnName) throws SQLException {
        return decode(resultSet.getString(columnName));
    }

    @Override
    public Object getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException {
        return decode(resultSet.getString(columnIndex));
    }

    @Override
    public Object getNullableResult(CallableStatement statement, int columnIndex) throws SQLException {
        return decode(statement.getString(columnIndex));
    }

    private String encode(Object parameter) throws SQLException {
        if (parameter instanceof String text) {
            return text;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(parameter);
        } catch (JsonProcessingException failure) {
            throw new SQLException("JSONB_COLUMN_ENCODING_FAILED", failure);
        }
    }

    private Object decode(String json) throws SQLException {
        if (json == null || String.class.equals(rawType)) {
            return json;
        }
        try {
            return OBJECT_MAPPER.readValue(json, javaType);
        } catch (JsonProcessingException failure) {
            throw new SQLException("JSONB_COLUMN_DECODING_FAILED", failure);
        }
    }
}
