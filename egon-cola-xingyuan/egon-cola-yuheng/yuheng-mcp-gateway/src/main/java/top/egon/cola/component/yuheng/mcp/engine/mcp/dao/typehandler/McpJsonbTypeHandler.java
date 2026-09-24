package top.egon.cola.component.yuheng.mcp.engine.mcp.dao.typehandler;

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
 * 中文说明：{@code McpJsonbTypeHandler} 是数据面进程自有的 jsonb 列绑定器，只把声明的 JSON 载体（Jackson 节点或
 * 结构化集合）编码为 PostgreSQL {@code jsonb}，不做 DTO/PO 的业务往返映射。数据面不得依赖控制面模块，因此本类是
 * 控制面同名处理器的对偶实现，逐字沿用其 {@code Types.OTHER} 与编解码语义。
 * English summary: {@code McpJsonbTypeHandler} is the data-plane process' own binding for {@code jsonb} columns: it encodes
 * the declared JSON carrier (a Jackson node or a structured collection) into PostgreSQL {@code jsonb} and performs no
 * DTO/PO business roundtrip. The data plane must not depend on the control plane, so this class is the dual of the
 * control-plane handler of the same purpose and keeps its {@code Types.OTHER} and codec semantics verbatim.
 *
 * 用法 / Usage: 由 MyBatis 实例化，因此不加 Spring 或数据 Lombok 注解；在 resultMap 或 {@code @TableField(typeHandler = ...)}
 * 上显式引用，Java 类型为 {@code String} 时按已编码 JSON 文本原样读写。/ Instantiated by MyBatis, so it carries no Spring or
 * data-Lombok annotation; reference it explicitly from a resultMap or {@code @TableField(typeHandler = ...)}, where a
 * {@code String} carrier is read and written as the already-encoded JSON text.
 */
public class McpJsonbTypeHandler extends BaseTypeHandler<Object> {

    /** 中文说明：保存仅供列值编解码使用的 Jackson 映射器，禁用 default typing。 English summary: Holds the Jackson mapper used for the column value only; default typing stays disabled. */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 中文说明：保存列声明的 Java 类型，用于反序列化时构造精确的 Jackson 目标类型。 English summary: Holds the Java type declared for the column, used to build the exact Jackson target type on read. */
    private final Class<?> rawType;

    /** 中文说明：保存与 {@code rawType} 对应的 Jackson 类型，缺省为 {@code Object}。 English summary: Holds the Jackson type resolved from {@code rawType}, defaulting to {@code Object}. */
    private final JavaType javaType;

    /**
     * 中文说明：创建未声明 Java 类型的处理器，此时 JSON 按通用结构读取。
     * English summary: Creates the handler without a declared Java type, so JSON is read as a generic structure.
     *
     * 用法 / Usage: 供 MyBatis 在无法解析属性类型时回退使用。/ The fallback MyBatis uses when no property type is resolvable.
     */
    public McpJsonbTypeHandler() {
        this(Object.class);
    }

    /**
     * 中文说明：按列声明的 Java 类型创建处理器。
     * English summary: Creates the handler for the Java type declared on the column.
     *
     * 用法 / Usage: 由 MyBatis 反射调用；/ Invoked reflectively by MyBatis with the resolved property type.
     * @param type 列的 Java 类型 / the Java type declared for the column.
     */
    public McpJsonbTypeHandler(Class<?> type) {
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
