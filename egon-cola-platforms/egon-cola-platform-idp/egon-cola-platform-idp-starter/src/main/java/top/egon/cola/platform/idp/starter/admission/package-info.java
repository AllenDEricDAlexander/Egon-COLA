/**
 * Resource Server 启动时向 IdP 认证机器身份、申请短期 Admission Ticket 并安全续签的适配层。
 * 本包只从 owner-only 外部文件读取私钥，不上传、记录或写入动态配置；DDC 通过中立 SPI
 * 消费缓存票据。
 *
 * <p>Adapters that authenticate machine identity to IdP during Resource Server startup, acquire
 * short-lived Admission Tickets, and renew them safely. This package reads private keys only from
 * owner-only external files and never uploads, logs, or writes them to dynamic configuration; DDC
 * consumes cached tickets through its neutral SPI.</p>
 */
@org.springframework.lang.NonNullApi
package top.egon.cola.platform.idp.starter.admission;
