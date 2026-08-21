# Egon COLA Common MyBatis-Plus Spring Boot Starter

这个按需引入的 Starter 基于官方 mybatis-plus-spring-boot3-starter 与 mybatis-plus-jsqlparser，版本固定为 3.5.16，提供 Egon COLA 仓储层统一能力：

- EgonModel<M> ActiveRecord 实体基类和七个公共持久化字段；
- 零声明方法的 EgonColaMapper<T>；
- 显式重写并增强官方 57 个方法的 EgonColaIService<T> / EgonColaServiceImpl<M,T>；
- TenantID Guard、BlockAttack、TenantLine、乐观锁、分页有序链；
- 权威审计填充，以及一个 MyBatis 参数/结果 Model 校验拦截器；
- 默认 MDC Provider，并为未来 SecurityContext Provider 保留替换点。

Starter 不拥有业务表、Flyway 迁移、分库分表拓扑、HTTP 接口和业务 Service；这些由采用方负责。

## 引入

先导入 Components BOM，再声明具体 Starter：

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>top.egon</groupId>
                <artifactId>egon-cola-components-bom</artifactId>
                <version>the-bom-version</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <dependency>
            <groupId>top.egon</groupId>
            <artifactId>egon-cola-component-common-mybatis-plus-spring-boot-starter</artifactId>
        </dependency>
    </dependencies>

应用需要提供 DataSource、Jakarta Validator（正常的 Boot Validation 自动配置即可）和 Mapper 扫描。Starter 通过 AutoConfiguration.imports 发现，不需要组件扫描。

## Model、Mapper 和技术 Service

所有持久化实体继承 EgonModel：

    @TableName("order_record")
    public class OrderModel extends EgonModel<OrderModel> {
        @NotBlank
        private String title;
        // 这里只放业务字段
    }

EgonModel 精确定义以下字段：

| Java 属性 | 物理列 | 类型 | 职责 |
|---|---|---|---|
| id | id | Long | MP ASSIGN_ID，审计 Handler 不生成 |
| tenantId | tenant_id | Long | 当前租户/分片键 |
| createUserId | create_user_id | String | insert 审计身份 |
| createTime | create_time | Instant | insert 审计时间 |
| updateUserId | update_user_id | String | insert/update 审计身份 |
| updateTime | update_time | Instant | insert/update 审计时间 |
| isDeleted | is_deleted | Boolean | MP TableLogic，活动值 0、逻辑删除值 1 |

六个非 ID 字段通过 Persisted 校验组持久时必须非空。业务子类可以使用 NotBlank 等 Jakarta 简单约束；跨记录、状态、权限、远程规则仍放在业务 Service。

消费者按官方方式声明 Mapper 和技术 Service：

    @Mapper
    public interface OrderMapper extends EgonColaMapper<OrderModel> {
    }

    public interface OrderRepository extends EgonColaIService<OrderModel> {
    }

    @Service
    public class OrderRepositoryImpl
            extends EgonColaServiceImpl<OrderMapper, OrderModel>
            implements OrderRepository {
    }

EgonColaMapper 不声明租户查询方法，也不提供自定义 SQL Injector；官方 BaseMapper 语句就是正常 CRUD 的完整表面。EgonColaIService / EgonColaServiceImpl 保留官方 57 个方法形状（list/count/id/Optional/map/obj/page/chain/batch 等），在内部增加上下文、参数、Model、分页、Wrapper 和事务保护。

## TenantID 与审计上下文

默认适配器读取 SLF4J MDC：

    MDC.put("tenantId", "11");
    MDC.put("userId", "operator-11");

tenantId 可以是任意非空 Long，零、负数都有效。缺少或无法解析的文本在 JDBC 前失败；userId 必须是非空白字符串。每次操作按当前线程解析，不在静态状态中缓存。

消费者可以提供一个 EgonColaTenantIdProvider 或 EgonColaUserIdProvider Bean 替换 MDC 默认实现，这正是未来接入 SecurityContext 的扩展点。

EgonColaMetaObjectHandler 具有权威填充语义：

- insert 覆盖 tenantId、createUserId、createTime、updateUserId、updateTime、isDeleted=false；
- update 覆盖 tenantId、updateUserId、updateTime；
- update 不改变 id、创建审计字段和 isDeleted；
- insert 的两个时间使用同一次 Clock.instant()。

自定义 Handler 必须继承 EgonColaMetaObjectHandler；公共填充方法是 final，只能通过 protected 后置钩子补充其他技术字段。无关的 MetaObjectHandler 会触发启动合同失败。

## 隔离与 SQL 安全

Starter 启用时的有序链为：

1. EgonColaTenantIdGuardInnerInterceptor（100）
2. BlockAttackInnerInterceptor（200，可配置）
3. TenantLineInnerInterceptor（300）
4. OptimisticLockerInnerInterceptor（400，可配置）
5. PaginationInnerInterceptor（500，可配置）

Guard 校验显式 tenant_id 条件，拒绝空值/不一致值，拒绝调用方修改 tenant_id 或 is_deleted，无法安全解析的 SQL 直接失败。配置的全局表才可以精确忽略 TenantLine。TenantLine 为官方 SELECT/INSERT/UPDATE/DELETE 追加当前非空 tenant_id。

AR、EgonColaIService、EgonColaMapper、Wrapper、链式 Wrapper 和直接 Mapper 语句都经过同一条链。Service 的空或无条件写 Wrapper 在 SQL 前失败；官方逻辑删除 SQL 可以修改 is_deleted，普通业务 Wrapper 不可以。

Starter 从不声明 ISqlInjector Bean，官方 MP 默认 Injector 继续提供标准语句。

## 配置

    egon:
      cola:
        component:
          mybatis-plus:
            enabled: true
            tenant-id:
              mdc-key: tenantId
              ignored-tables: []
            audit:
              user-id-mdc-key: userId
            pagination:
              enabled: true
              max-page-size: 500
              overflow: false
            batch:
              default-size: 1000
              max-chunk-size: 1000
              max-collection-size: 10000
            block-attack:
              enabled: true
            optimistic-locker:
              enabled: true
            meta-fill:
              enabled: true

enabled=false 关闭完整 Egon COLA 链。即使关闭分页 SQL 拦截器，Service 仍会校验页参数。Starter 会校验最终 outer interceptor 的成员和顺序，缺少隔离、填充或 Model 校验能力时启动失败，而不是静默降级。

## 分层校验与转换

    Controller DTO（@Valid）
            -> BaseConverter<DTO, PO>
    业务 Service PO（Jakarta 字段规则 + 复杂规则）
            -> BaseConverter<PO, Model>
    仓储 Model（EgonModel + 业务约束 + tenant/persisted 分组）
            -> MP fill 后的 MyBatis ParameterHandler
    数据库行 -> MyBatis ResultSetHandler -> loaded Model 校验

common-core 提供对象、属性、候选值和 group 的实例化 ValidationUtils。Starter 的 EgonColaModelValidationUtils 负责 INSERT、UPDATE、DELETE、QUERY、LOADED 分组及 tenant 一致性。EgonColaModelValidationInterceptor 会递归校验参数/结果中的 Model、集合、数组、Map、分页和 Wrapper 实体，并防止循环引用。

Converter 必须显式映射业务字段，不能把 id、tenantId、创建/更新时间、用户 ID 或 isDeleted 从 DTO/PO 复制到 Model；这些字段属于仓储填充和数据库结果边界。

## 采用方表结构与迁移

映射 EgonModel 的每张采用方表都需要七个公共列为非空持久状态，并补充业务列：

    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    create_user_id VARCHAR(128) NOT NULL,
    create_time TIMESTAMP NOT NULL,
    update_user_id VARCHAR(128) NOT NULL,
    update_time TIMESTAMP NOT NULL,
    is_deleted BOOLEAN NOT NULL

请按查询负载为 tenant_id 与 is_deleted 建索引。已有数据必须先回填并验证，再部署继承 EgonModel 的实体；Starter 不创建或修改生产表。迁移、回填、回滚和历史数据清理由采用方按自身规范执行。

## 限制与失败行为

- 页大小必须在 1..max-page-size（默认上限 500）；
- batch chunk 与集合上限可配置，并在首条 JDBC 前校验；
- batch 方法具有事务语义，入口捕获一个 tenant 快照；上下文变化或数据库失败会整体回滚；
- 上下文缺失、显式租户不一致、Model 无效、不安全 Wrapper、受保护列修改和不支持 SQL 都 fail closed；
- 旧版本号更新保持官方 false/零行结果；
- 违反持久化或业务约束的历史行在结果返回前失败，不返回半合法 Model。

## 验证边界

模块测试使用嵌入式 H2，不启动应用服务器或外部数据库：

    ./mvnw -B -ntp -f egon-cola-components/pom.xml \
      -pl egon-cola-component-common-mybatis-plus-spring-boot-starter -am test

测试覆盖公开 API、自动配置、官方默认语句、tenant SQL、逻辑删除、填充、校验、AR、事务、转换边界和线程隔离；不等同于采用方真实数据库方言、生产索引、SecurityContext 映射、DataSource 路由或在线分库分表拓扑证明。

## 关闭与回滚

不使用 Egon COLA 链时移除具体 Starter，或设置 egon.cola.component.mybatis-plus.enabled=false。采用方应先独立回滚表结构/数据，再移除依赖；在所有相关 Model 移除前保留公共列。平台回滚按实现提交的逆序进行，采用方迁移与平台依赖回滚分别负责。
