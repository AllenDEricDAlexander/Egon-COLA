package top.egon.cola.archetype.source.agent.infrastructure.knowledge.repo.po;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Binds a {@code String} field to a PostgreSQL {@code jsonb} column.
 *
 * <p>The driver sends a plain {@code setString} as {@code character varying}, which PostgreSQL
 * refuses to assign to jsonb; declaring the value as {@code Types.OTHER} lets the server infer the
 * column type instead. Reads stay plain text, so the field holds the raw JSON document.
 */
public class JsonbStringTypeHandler extends BaseTypeHandler<String> {

    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, String parameter,
                                    JdbcType jdbcType) throws SQLException {
        statement.setObject(index, parameter, Types.OTHER);
    }

    @Override
    public String getNullableResult(ResultSet resultSet, String columnName) throws SQLException {
        return resultSet.getString(columnName);
    }

    @Override
    public String getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException {
        return resultSet.getString(columnIndex);
    }

    @Override
    public String getNullableResult(CallableStatement statement, int columnIndex) throws SQLException {
        return statement.getString(columnIndex);
    }
}
