package top.egon.cola.platform.rbac3.admin.runtime.domain.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RBucket;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;
import top.egon.cola.platform.rbac3.admin.activation.service.RoleActivationFacade;
import top.egon.cola.platform.rbac3.admin.authorization.service.AuthorizationDecisionService;
import top.egon.cola.platform.rbac3.admin.runtime.service.SessionSnapshotProjector;
import top.egon.cola.platform.rbac3.contract.authorization.SessionAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.core.runtime.Rbac3RuntimeKeyFactory;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.activation.repository.RoleActivationRuntimeRepository;
import top.egon.cola.platform.rbac3.admin.activation.domain.vo.RuntimePublicationVO;
import top.egon.cola.platform.rbac3.admin.authorization.repository.AuthorizationSnapshotRepository;
import top.egon.cola.platform.rbac3.admin.authorization.repository.FenceVerifier;
import top.egon.cola.platform.rbac3.admin.authorization.domain.vo.SnapshotRecordVO;
import top.egon.cola.platform.rbac3.admin.runtime.repository.redis.RedisAuthorizationRuntimeRepository;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.SessionSnapshotProjectionVO;

/**
     * 类型 `PublishCommandDTO` 位于 `RedisAuthorizationRuntimeStore` 内，是记录类型，用于承载 `Publish Command` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `PublishCommandDTO` is a record inside `RedisAuthorizationRuntimeStore` and carries the responsibility, state, or contract for `Publish Command`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `PublishCommandDTO` 作为 `RedisAuthorizationRuntimeStore` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `PublishCommandDTO` as the responsibility boundary of `RedisAuthorizationRuntimeStore`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param userId 记录组件 `userId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `userId` carries constructor data whose meaning is defined by the record contract.
     * @param sessionId 记录组件 `sessionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionId` carries constructor data whose meaning is defined by the record contract.
     * @param authVersion 记录组件 `authVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authVersion` carries constructor data whose meaning is defined by the record contract.
     * @param sessionVersion 记录组件 `sessionVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionVersion` carries constructor data whose meaning is defined by the record contract.
     * @param policyVersion 记录组件 `policyVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyVersion` carries constructor data whose meaning is defined by the record contract.
     * @param projection 记录组件 `projection` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `projection` carries constructor data whose meaning is defined by the record contract.
     */
    public record PublishCommandDTO(
            /**
             * 字段 `tenantId` 表示 `PublishCommandDTO` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `PublishCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `PublishCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `PublishCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `userId` 表示 `PublishCommandDTO` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `PublishCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `userId` 时应保持 `PublishCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `userId`, preserve `PublishCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String userId,
            /**
             * 字段 `sessionId` 表示 `PublishCommandDTO` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `PublishCommandDTO` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `PublishCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `PublishCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            String sessionId,
            /**
             * 字段 `authVersion` 表示 `PublishCommandDTO` 中与 `auth Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authVersion` stores the `auth Version`-related state, dependency, configuration, or result of `PublishCommandDTO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authVersion` 时应保持 `PublishCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authVersion`, preserve `PublishCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            long authVersion,
            /**
             * 字段 `sessionVersion` 表示 `PublishCommandDTO` 中与 `session Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionVersion` stores the `session Version`-related state, dependency, configuration, or result of `PublishCommandDTO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionVersion` 时应保持 `PublishCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionVersion`, preserve `PublishCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            long sessionVersion,
            /**
             * 字段 `policyVersion` 表示 `PublishCommandDTO` 中与 `policy Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyVersion` stores the `policy Version`-related state, dependency, configuration, or result of `PublishCommandDTO` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyVersion` 时应保持 `PublishCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyVersion`, preserve `PublishCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            long policyVersion,
            /**
             * 字段 `projection` 表示 `PublishCommandDTO` 中与 `projection` 相关的状态、依赖、配置或结果（声明类型 `SessionSnapshotProjectionVO`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `projection` stores the `projection`-related state, dependency, configuration, or result of `PublishCommandDTO` (declared type `SessionSnapshotProjectionVO`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `projection` 时应保持 `PublishCommandDTO` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `projection`, preserve `PublishCommandDTO`'s lifecycle, immutability, and thread-safety constraints.
             */
            SessionSnapshotProjectionVO projection
    ) {
    }
