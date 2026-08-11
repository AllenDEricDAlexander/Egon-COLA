package top.egon.cola.platform.idp.starter.state;

import java.util.Optional;

/**
 * 定义按 Resource Server 标识读取当前运行态投影的端口。
 *
 * <p>Defines the port for reading the current runtime projection by Resource Server identifier.</p>
 */
@FunctionalInterface
public interface IdentityResourceServerStateReader {

    /**
     * 读取当前 Resource Server 状态。
     *
     * <p>Reads the current Resource Server state.</p>
     *
     * @param resourceServerId Resource Server 标识；Resource Server identifier
     * @return 运行态投影；不存在时为空；runtime projection, or empty when absent
     */
    Optional<IdentityResourceServerState> read(String resourceServerId);
}
