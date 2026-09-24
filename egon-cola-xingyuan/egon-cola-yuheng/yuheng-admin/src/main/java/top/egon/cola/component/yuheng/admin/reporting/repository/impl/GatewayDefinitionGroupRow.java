package top.egon.cola.component.yuheng.admin.reporting.repository.impl;


/**
 * 中文说明：{@code GatewayDefinitionGroupRow} 是 {@code repository.impl} 包内的不可变行投影辅助载体，保存原 JDBC 查询在写入前对
 * {@code gateway_interface_group} 已存在行的探测结果（分组 id 与其来源类型），用于来源迁移与冲突判定；它从原 {@code repository.jdbc} 包迁移而来，
 * 字段与职责完全保持，只是不再由 {@code RowMapper} 直接构造，而由 MyBatis-Plus 受守卫单表读取提供。
 * English summary: {@code GatewayDefinitionGroupRow} is an immutable row-projection helper kept inside the {@code repository.impl} package; it holds the pre-write probe of an existing {@code gateway_interface_group} row (its id and source type) that the legacy JDBC query used for source migration and conflict decisions. It moves here from {@code repository.jdbc} with identical fields and responsibilities, but is now fed by guarded MyBatis-Plus single-table reads instead of a JDBC {@code RowMapper}.
 *
 * 用法 / Usage: 仅作为存储实现内部的查询/树组装临时状态，在同一门面方法内产生并消费；不得跨越公开端口、不得作为 JSON 载体返回。/ Use it only as internal query/tree assembly state inside a persistence facade, produced and consumed within the same method; it must never cross the public port or be returned as a JSON carrier.
 */
public final class GatewayDefinitionGroupRow {

    /**
     * 中文说明：已存在接口分组的 id，等价于原 SQL 的 {@code id} 投影。
     * English summary: Identifier of the existing interface group, equivalent to the original {@code id} projection.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDefinitionGroupRow} 的构造与访问器使用；/ Access it through the constructor and accessor of {@code GatewayDefinitionGroupRow}.
     */
    private final String id;

    /**
     * 中文说明：已存在接口分组的来源类型原 wire 字符串，等价于原 SQL 的 {@code source_type} 投影。
     * English summary: Source-type wire string of the existing interface group, equivalent to the original {@code source_type} projection.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDefinitionGroupRow} 的构造与访问器使用；/ Access it through the constructor and accessor of {@code GatewayDefinitionGroupRow}.
     */
    private final String sourceType;

    /**
     * 中文说明：创建 {@code GatewayDefinitionGroupRow} 实例；参数顺序与原 JDBC 投影列（{@code id, source_type}）一致，保证迁移期间既有调用方可原位构造。
     * English summary: Creates a {@code GatewayDefinitionGroupRow}; the parameter order matches the original projection columns {@code id, source_type} so existing callers keep constructing it in place during the migration.
     *
     * 用法 / Usage: 由存储实现在读取投影后调用；/ Called by the persistence implementation right after the projection read.
     * @param id 参数 分组 id；parameter group id.
     * @param sourceType 参数 来源类型；parameter source type.
     */
    public GatewayDefinitionGroupRow(String id, String sourceType) {
        this.id = id;
        this.sourceType = sourceType;
    }

    /**
     * 中文说明：返回分组 id 投影，访问器命名与原 record 分量保持一致。
     * English summary: Returns the group id projection; accessor names stay identical to the original record components.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDefinitionGroupRow.id()}。
     * @return 返回分组 id；returns the group id.
     */
    public String id() {
        return id;
    }

    /**
     * 中文说明：返回来源类型投影，访问器命名与原 record 分量保持一致。
     * English summary: Returns the source type projection; accessor names stay identical to the original record components.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDefinitionGroupRow.sourceType()}。
     * @return 返回来源类型；returns the source type.
     */
    public String sourceType() {
        return sourceType;
    }
}
