package top.egon.cola.component.yuheng.admin.reporting.repository.impl;


import top.egon.cola.component.yuheng.admin.reporting.domain.bo.GatewayStoredReportBO;
import top.egon.cola.component.yuheng.contract.reporting.GatewayInterfaceDefinitionReportResult;

import java.util.ArrayList;
import java.util.List;


/**
 * 中文说明：{@code GatewayMutableStoredReport} 是定义集入库过程中的可变累计器，只在门面实现内部使用：
 * 逐操作累加「新建」「更新」计数并收集每个操作的变更引用，最后冻结为不可变的 {@code GatewayStoredReportBO} 端口载体，
 * 从而让端口签名不暴露可变状态。计数与引用顺序严格跟随上报树的业务域→实体域→分组→操作遍历顺序，与原实现一致。
 * English summary: {@code GatewayMutableStoredReport} is the mutable accumulator used only inside the facade implementation while a
 * definition set is ingested: it adds the created and updated counters per operation, collects one change reference per operation and
 * finally freezes into the immutable {@code GatewayStoredReportBO} port carrier, so no mutable state reaches the port signature. The
 * counters and the reference order follow the business-domain → entity-domain → group → operation traversal exactly as before.
 *
 * 用法 / Usage: 由 {@code MpGatewayDefinitionReportRepository#ingest} 在单次上报内创建、传递给每个操作的存储步骤并立即冻结；
 * 不得跨请求共享或作为字段缓存。/ Create it inside a single {@code MpGatewayDefinitionReportRepository#ingest} call, pass it to each
 * operation store step and freeze it immediately; never share it across requests or cache it in a field.
 */
public final class GatewayMutableStoredReport {

    /**
     * 中文说明：本次上报新建的操作数量。
     * English summary: The number of operations created by this report.
     *
     * 用法 / Usage: 由 {@code MpGatewayDefinitionReportRepository} 的存储步骤自增；/ Incremented by the storage step of {@code MpGatewayDefinitionReportRepository}.
     */
    int created;

    /**
     * 中文说明：本次上报更新的操作数量。
     * English summary: The number of operations updated by this report.
     *
     * 用法 / Usage: 由 {@code MpGatewayDefinitionReportRepository} 的存储步骤自增；/ Incremented by the storage step of {@code MpGatewayDefinitionReportRepository}.
     */
    int updated;

    /**
     * 中文说明：按遍历顺序累积的操作变更引用（{@code CREATED}/{@code UNCHANGED}/{@code UPDATED}）。
     * English summary: The operation change references ({@code CREATED}/{@code UNCHANGED}/{@code UPDATED}) accumulated in traversal order.
     *
     * 用法 / Usage: 由存储步骤追加并在 {@link #freeze()} 时复制；/ Appended by the storage step and copied on {@link #freeze()}.
     */
    final List<
            GatewayInterfaceDefinitionReportResult.OperationRef> refs =
            new ArrayList<>();

    /**
     * 中文说明：执行 freeze 操作；把累计的计数与引用快照为不可变端口载体。
     * English summary: Executes the freeze operation; snapshots the accumulated counters and references into the immutable port carrier.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayMutableStoredReport.freeze()}。
     * @return 返回入库上报载体；returns the stored-report carrier.
     */
    GatewayStoredReportBO freeze() {
        return new GatewayStoredReportBO(created, updated, List.copyOf(refs));
    }
}
