/**
 * DDC 配置拉取、发布、注册、心跳、确认和变更事件模型。
 * 本包只表达线上协议数据，不计算校验和、不解析 YAML，也不执行刷新；相关能力位于 {@code format} 和 {@code service}。
 *
 * <p>Protocol models for DDC configuration retrieval, publication, registration, heartbeat,
 * acknowledgement, and change events. Checksum calculation, YAML parsing, and refresh execution
 * are excluded and belong to {@code format} and {@code service}.</p>
 */
@NonNullApi
package top.egon.cola.component.ddc.model.config;

import org.springframework.lang.NonNullApi;
