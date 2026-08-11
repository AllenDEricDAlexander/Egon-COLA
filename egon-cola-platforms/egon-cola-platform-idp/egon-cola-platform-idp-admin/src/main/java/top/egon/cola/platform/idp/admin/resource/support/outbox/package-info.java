/**
 * Resource Server 生命周期事务事件适配与 DDC 投递边界。
 *
 * <p>本包在 Resource 停用事务中写入不含密钥的稳定事件，并通过仓库标准事务发件箱将
 * 精确 {@code bizCode + appCode + env} 撤销命令可靠投递到 DDC。</p>
 *
 * <p>Transactional Resource Server lifecycle adapters and DDC delivery boundaries. This package
 * writes key-free stable events in the Resource disable transaction and reliably delivers exact
 * {@code bizCode + appCode + env} revocation commands through the repository-standard outbox.</p>
 */
package top.egon.cola.platform.idp.admin.resource.support.outbox;
