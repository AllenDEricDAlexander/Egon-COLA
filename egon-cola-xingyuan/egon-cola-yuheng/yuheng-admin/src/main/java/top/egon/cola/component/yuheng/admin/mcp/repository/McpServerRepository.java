package top.egon.cola.component.yuheng.admin.mcp.repository;


import java.util.List;
import java.util.Optional;

import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.mcp.domain.bo.McpServerBO;

/**
 * 中文说明：{@code McpServerRepository} 是 gateway_mcp_server 的业务端口，只声明当前调用方真正需要的命名方法，
 * 不再继承 Spring Data/JPA 的泛型 CRUD（旧的 {@code saveAndFlush}/{@code flush} 托管实体脏检查已失去消费者），
 * 也不外泄 MyBatis-Plus 行模型。
 * English summary: {@code McpServerRepository} is the business port for {@code gateway_mcp_server}; it declares only the named
 * methods current consumers need, no longer inherits Spring Data/JPA generic CRUD (the former {@code saveAndFlush} and
 * {@code flush} managed-entity dirty checking lost every consumer), and leaks no MyBatis-Plus row model.
 *
 * 用法 / Usage: 由 {@code MpMcpServerRepository} 以受守卫的 MP 写入实现，保存返回权威业务载体，业务 {@code revision} 的 CAS
 * 与递增、以及软删除都须在仓储内完成。
 * {@code MpMcpServerRepository} implements it through guarded MyBatis-Plus writes, returning the authoritative carrier and performing
 * the business {@code revision} compare-and-set, its increment and the soft delete inside the repository.
 */
@Validated
public interface McpServerRepository {

    /**
     * 中文说明：执行 按组列服务器 操作；等价于旧端口上的同名派生查询，按组键读取未删除的服务器并按 serverCode 升序返回。
     * English summary: Executes the list servers by group operation; the equivalent of the same-named derived query on the former
     * port, reading non-deleted servers of one group ordered ascending by serverCode.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpServerRepository.findAllByGatewayGroupIdAndDeletedFalseOrderByServerCode(...)}。
     * 调用方应准备合法参数并处理返回值或异常。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 组内未删除的服务器载体；returns the group's non-deleted server carriers.
     */
    List<McpServerBO> findAllByGatewayGroupIdAndDeletedFalseOrderByServerCode(
            String gatewayGroupId);

    /**
     * 中文说明：执行 按主键读服务器 操作；等价于旧端口上的同名派生查询，只返回未删除的行。
     * English summary: Executes the read server by identifier operation; the equivalent of the same-named derived query on the former
     * port, returning only a non-deleted row.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpServerRepository.findByIdAndDeletedFalse(...)}。
     * @param id 参数 id；parameter id。
     * @return 返回 命中的服务器载体；returns the matched server carrier.
     */
    Optional<McpServerBO> findByIdAndDeletedFalse(String id);

    /**
     * 中文说明：执行 保存服务器 操作；替代旧的 {@code saveAndFlush} 与托管实体脏检查，按业务 {@code revision} 做 CAS 并递增，
     * 载体标记删除时改为软删，返回携带权威 revision 与审计投影的业务载体。
     * English summary: Executes the save server operation; it replaces the former {@code saveAndFlush} and managed-entity dirty
     * checking, performing the compare-and-set and increment on the business {@code revision}, soft-deleting when the carrier is
     * marked deleted and returning the carrier carrying the authoritative revision and audit projections.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code server = McpServerRepository.save(server)}；写后必须采用返回值，忽略返回值即为丢写。
     * The write must be re-assigned through the return value; discarding it is a lost write.
     * @param server 参数 服务器载体；parameter server carrier。
     * @return 返回 权威服务器载体；returns the authoritative server carrier.
     */
    McpServerBO save(McpServerBO server);
}
