package top.egon.cola.platform.rbac3.admin.directory.domain;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.directory.service.DirectorySnapshotProcessor;
import top.egon.cola.platform.rbac3.admin.directory.domain.po.OrgUnitPO;
import top.egon.cola.platform.rbac3.admin.directory.domain.po.PositionPO;
import top.egon.cola.platform.rbac3.admin.directory.domain.po.UserPositionSnapshotPO;
import top.egon.cola.platform.rbac3.admin.identity.domain.po.UserPO;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import top.egon.cola.platform.rbac3.admin.directory.repository.jpa.DirectorySnapshotMaterializer;

/**
     * 类型 `UserPositionKey` 位于 `DirectorySnapshotMaterializer` 内，是记录类型，用于承载 `User Position Key` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `UserPositionKey` is a record inside `DirectorySnapshotMaterializer` and carries the responsibility, state, or contract for `User Position Key`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `UserPositionKey` 作为 `DirectorySnapshotMaterializer` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `UserPositionKey` as the responsibility boundary of `DirectorySnapshotMaterializer`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param userId 记录组件 `userId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `userId` carries constructor data whose meaning is defined by the record contract.
     * @param positionId 记录组件 `positionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `positionId` carries constructor data whose meaning is defined by the record contract.
     */
    public record UserPositionKey(/**
 * 字段 `userId` 表示 `UserPositionKey` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `UserPositionKey` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `userId` 时应保持 `UserPositionKey` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `userId`, preserve `UserPositionKey`'s lifecycle, immutability, and thread-safety constraints.
 */ Long userId, /**
 * 字段 `positionId` 表示 `UserPositionKey` 中与 `position Id` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `positionId` stores the `position Id`-related state, dependency, configuration, or result of `UserPositionKey` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `positionId` 时应保持 `UserPositionKey` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `positionId`, preserve `UserPositionKey`'s lifecycle, immutability, and thread-safety constraints.
 */ Long positionId) {
    }
