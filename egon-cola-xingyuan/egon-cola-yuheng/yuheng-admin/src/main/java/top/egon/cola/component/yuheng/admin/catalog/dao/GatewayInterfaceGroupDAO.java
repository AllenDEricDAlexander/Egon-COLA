package top.egon.cola.component.yuheng.admin.catalog.dao;

import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.yuheng.admin.catalog.domain.po.GatewayInterfaceGroupPO;

/**
 * gateway_interface_group 的 MyBatis-Plus Mapper，复用 EgonColaMapper 的租户内活跃读取与版本化软删除。
 * MyBatis-Plus mapper for gateway_interface_group; inherits tenant-scoped active reads and versioned soft-delete.
 * 用法 / Usage: 仅声明 Spec §11.2 访问路径所需的具名类型化查询，通用 CRUD 一律经对应持久化仓储调用。
 */
public interface GatewayInterfaceGroupDAO extends EgonColaMapper<GatewayInterfaceGroupPO> {
}
