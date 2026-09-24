package top.egon.cola.component.yuheng.admin.reporting.repository.impl;


/**
 * 中文说明：{@code GatewayDefinitionOperationRow} 是 {@code repository.impl} 包内的不可变行投影辅助载体，保存原 JDBC 查询在 {@code gateway_operation} 与
 * {@code gateway_operation_definition} 聚合后得到的临时状态（操作 id、所属接口分组、来源类型、当前定义指向、定义摘要与最大定义版本）；它从原
 * {@code repository.jdbc} 包迁移而来，职责与字段完全保持，只是不再由 {@code RowMapper} 直接构造，而由 MyBatis-Plus 受守卫单表读取在内存中组装。
 * English summary: {@code GatewayDefinitionOperationRow} is an immutable row-projection helper kept inside the {@code repository.impl} package; it holds the temporary state the legacy JDBC query aggregated from {@code gateway_operation} joined with {@code gateway_operation_definition} (operation id, owning interface group, source type, current-definition pointer, definition digest and maximum definition version). It moves here from {@code repository.jdbc} with identical fields and responsibilities, but is now assembled in memory from guarded MyBatis-Plus single-table reads instead of a JDBC {@code RowMapper}.
 *
 * 用法 / Usage: 仅作为存储实现内部的查询/树组装临时状态，在同一门面方法内产生并消费；不得跨越公开端口、不得作为 JSON 载体返回。/ Use it only as internal query/tree assembly state inside a persistence facade, produced and consumed within the same method; it must never cross the public port or be returned as a JSON carrier.
 */
public final class GatewayDefinitionOperationRow {

    /**
     * 中文说明：{@code gateway_operation} 行的不透明字符串主键，等价于原 SQL 的 {@code o.id} 投影。
     * English summary: Opaque string primary key of the {@code gateway_operation} row, equivalent to the original {@code o.id} projection.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDefinitionOperationRow} 的构造与访问器使用；/ Access it through the constructor and accessor of {@code GatewayDefinitionOperationRow}.
     */
    private final String id;

    /**
     * 中文说明：操作当前所属接口分组 id，等价于原 SQL 的 {@code o.interface_group_id}，用于上报路径的分组冲突判定。
     * English summary: Identifier of the interface group currently owning the operation, the original {@code o.interface_group_id}, used for the group-conflict decision on the report path.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDefinitionOperationRow} 的构造与访问器使用；/ Access it through the constructor and accessor of {@code GatewayDefinitionOperationRow}.
     */
    private final String interfaceGroupId;

    /**
     * 中文说明：操作来源类型的原 wire 字符串，等价于原 SQL 的 {@code o.source_type}，用于来源迁移与冲突判定。
     * English summary: Source-type wire string of the operation, the original {@code o.source_type}, used for source migration and conflict decisions.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDefinitionOperationRow} 的构造与访问器使用；/ Access it through the constructor and accessor of {@code GatewayDefinitionOperationRow}.
     */
    private final String sourceType;

    /**
     * 中文说明：操作当前定义指向 id，等价于原 SQL 的 {@code o.current_definition_id}，可为空表示尚未指向任何定义。
     * English summary: Current-definition pointer of the operation, the original {@code o.current_definition_id}, nullable while no definition is pointed at.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDefinitionOperationRow} 的构造与访问器使用；/ Access it through the constructor and accessor of {@code GatewayDefinitionOperationRow}.
     */
    private final String currentDefinitionId;

    /**
     * 中文说明：当前定义的 {@code definition_sha256} 摘要，等价于原 SQL 经 {@code LEFT JOIN gateway_operation_definition d ON d.id = o.current_definition_id} 得到的 {@code d.definition_sha256}；无当前定义时为空。
     * English summary: {@code definition_sha256} digest of the current definition, produced by the original {@code LEFT JOIN gateway_operation_definition d ON d.id = o.current_definition_id}; null when there is no current definition.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDefinitionOperationRow} 的构造与访问器使用；/ Access it through the constructor and accessor of {@code GatewayDefinitionOperationRow}.
     */
    private final String definitionSha256;

    /**
     * 中文说明：该操作全部历史定义版本的最大值，等价于原 SQL 的 {@code COALESCE(MAX(all_d.definition_version), 0)}，供追加下一版本时递增使用。
     * English summary: Maximum definition version across all historical definitions of the operation, the original {@code COALESCE(MAX(all_d.definition_version), 0)}, used to derive the next appended version.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDefinitionOperationRow} 的构造与访问器使用；/ Access it through the constructor and accessor of {@code GatewayDefinitionOperationRow}.
     */
    private final long maxVersion;

    /**
     * 中文说明：创建 {@code GatewayDefinitionOperationRow} 实例；参数顺序与原 JDBC 投影列（{@code id, interface_group_id, source_type, current_definition_id, definition_sha256, max_version}）逐项一致，保证迁移期间既有调用方可原位构造。
     * English summary: Creates a {@code GatewayDefinitionOperationRow}; the parameter order matches the original projection columns {@code id, interface_group_id, source_type, current_definition_id, definition_sha256, max_version} one by one so existing callers keep constructing it in place during the migration.
     *
     * 用法 / Usage: 由存储实现在读取投影后调用；/ Called by the persistence implementation right after the projection read.
     * @param id 参数 操作 id；parameter operation id.
     * @param interfaceGroupId 参数 接口分组 id；parameter interface group id.
     * @param sourceType 参数 来源类型；parameter source type.
     * @param currentDefinitionId 参数 当前定义 id；parameter current definition id.
     * @param definitionSha256 参数 定义摘要；parameter definition sha256.
     * @param maxVersion 参数 最大定义版本；parameter maximum definition version.
     */
    public GatewayDefinitionOperationRow(
            String id,
            String interfaceGroupId,
            String sourceType,
            String currentDefinitionId,
            String definitionSha256,
            long maxVersion) {
        this.id = id;
        this.interfaceGroupId = interfaceGroupId;
        this.sourceType = sourceType;
        this.currentDefinitionId = currentDefinitionId;
        this.definitionSha256 = definitionSha256;
        this.maxVersion = maxVersion;
    }

    /**
     * 中文说明：返回操作 id 投影，访问器命名与原 record 分量保持一致。
     * English summary: Returns the operation id projection; accessor names stay identical to the original record components.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDefinitionOperationRow.id()}。
     * @return 返回操作 id；returns the operation id.
     */
    public String id() {
        return id;
    }

    /**
     * 中文说明：返回接口分组 id 投影，访问器命名与原 record 分量保持一致。
     * English summary: Returns the interface group id projection; accessor names stay identical to the original record components.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDefinitionOperationRow.interfaceGroupId()}。
     * @return 返回接口分组 id；returns the interface group id.
     */
    public String interfaceGroupId() {
        return interfaceGroupId;
    }

    /**
     * 中文说明：返回来源类型投影，访问器命名与原 record 分量保持一致。
     * English summary: Returns the source type projection; accessor names stay identical to the original record components.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDefinitionOperationRow.sourceType()}。
     * @return 返回来源类型；returns the source type.
     */
    public String sourceType() {
        return sourceType;
    }

    /**
     * 中文说明：返回当前定义指向投影，访问器命名与原 record 分量保持一致。
     * English summary: Returns the current-definition pointer projection; accessor names stay identical to the original record components.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDefinitionOperationRow.currentDefinitionId()}。
     * @return 返回当前定义 id；returns the current definition id.
     */
    public String currentDefinitionId() {
        return currentDefinitionId;
    }

    /**
     * 中文说明：返回当前定义摘要投影，访问器命名与原 record 分量保持一致。
     * English summary: Returns the current definition digest projection; accessor names stay identical to the original record components.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDefinitionOperationRow.definitionSha256()}。
     * @return 返回定义摘要；returns the definition sha256.
     */
    public String definitionSha256() {
        return definitionSha256;
    }

    /**
     * 中文说明：返回最大定义版本投影，访问器命名与原 record 分量保持一致。
     * English summary: Returns the maximum definition version projection; accessor names stay identical to the original record components.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDefinitionOperationRow.maxVersion()}。
     * @return 返回最大定义版本；returns the maximum definition version.
     */
    public long maxVersion() {
        return maxVersion;
    }
}
