package top.egon.cola.component.ddc.api.extension;

import top.egon.cola.component.ddc.model.admission.DdcAdmissionRequest;
import top.egon.cola.component.ddc.model.admission.DdcAdmissionTicket;

/**
 * 为 DDC 注册和心跳生产者提供 IdP 短期准入票据的依赖中立端口。
 *
 * <p>Dependency-neutral port supplying short-lived IdP admission tickets to DDC registration and
 * heartbeat producers.</p>
 *
 * <p>DDC 只拥有此接口和中立模型；IdP Starter 提供生产实现，避免 DDC 反向依赖 IdP。</p>
 *
 * <p>DDC owns only this interface and neutral models. IdP Starter supplies the production
 * implementation, avoiding a reverse DDC-to-IdP dependency.</p>
 */
@FunctionalInterface
public interface DdcAdmissionTicketSupplier {

    /**
     * 为精确 Resource 实例身份取得当前可用票据。
     *
     * <p>Gets the currently usable ticket for an exact Resource instance identity.</p>
     *
     * @param request 精确准入请求；exact admission request
     * @return 未过期且绑定到请求身份的票据；unexpired ticket bound to the request identity
     * @throws RuntimeException 无法安全取得票据时抛出并 Fail Closed；when a ticket cannot be
     * safely acquired, failing closed
     */
    DdcAdmissionTicket getTicket(DdcAdmissionRequest request);
}
