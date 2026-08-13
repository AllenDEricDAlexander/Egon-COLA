package top.egon.cola.platform.rbac3.admin.identity.repository.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.shared.repository.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.auth.service.PasswordIdentityAuthenticator;
import top.egon.cola.platform.rbac3.admin.tenant.domain.po.TenantPO;
import top.egon.cola.platform.rbac3.admin.identity.domain.po.ExternalIdentityPO;
import top.egon.cola.platform.rbac3.admin.identity.domain.po.UserCredentialPO;
import top.egon.cola.platform.rbac3.admin.identity.domain.po.UserPO;
import top.egon.cola.platform.rbac3.admin.identity.service.IdentityMappingFacade;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import top.egon.cola.platform.rbac3.admin.tenant.domain.enums.TenantStatusEnum;
import top.egon.cola.platform.rbac3.admin.identity.domain.enums.UserCredentialStatusEnum;
import top.egon.cola.platform.rbac3.admin.identity.domain.enums.UserStatusEnum;
import top.egon.cola.platform.rbac3.admin.auth.domain.vo.PasswordCredentialVO;

/**
     * 类型 `CredentialRow` 位于 `IdentityRepositories` 内，是记录类型，用于承载 `Credential Row` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `CredentialRow` is a record inside `IdentityRepositories` and carries the responsibility, state, or contract for `Credential Row`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `CredentialRow` 作为 `IdentityRepositories` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `CredentialRow` as the responsibility boundary of `IdentityRepositories`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenant 记录组件 `tenant` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenant` carries constructor data whose meaning is defined by the record contract.
     * @param user 记录组件 `user` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `user` carries constructor data whose meaning is defined by the record contract.
     * @param credential 记录组件 `credential` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `credential` carries constructor data whose meaning is defined by the record contract.
     */
    record CredentialRow(
            /**
             * 字段 `tenant` 表示 `CredentialRow` 中与 `tenant` 相关的状态、依赖、配置或结果（声明类型 `TenantPO`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenant` stores the `tenant`-related state, dependency, configuration, or result of `CredentialRow` (declared type `TenantPO`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenant` 时应保持 `CredentialRow` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenant`, preserve `CredentialRow`'s lifecycle, immutability, and thread-safety constraints.
             */
            TenantPO tenant,
            /**
             * 字段 `user` 表示 `CredentialRow` 中与 `user` 相关的状态、依赖、配置或结果（声明类型 `UserPO`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `user` stores the `user`-related state, dependency, configuration, or result of `CredentialRow` (declared type `UserPO`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `user` 时应保持 `CredentialRow` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `user`, preserve `CredentialRow`'s lifecycle, immutability, and thread-safety constraints.
             */
            UserPO user,
            /**
             * 字段 `credential` 表示 `CredentialRow` 中与 `credential` 相关的状态、依赖、配置或结果（声明类型 `UserCredentialPO`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `credential` stores the `credential`-related state, dependency, configuration, or result of `CredentialRow` (declared type `UserCredentialPO`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `credential` 时应保持 `CredentialRow` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `credential`, preserve `CredentialRow`'s lifecycle, immutability, and thread-safety constraints.
             */
            UserCredentialPO credential
    ) {

        /**
         * 方法 `toPasswordCredential` 按照 `CredentialRow` 的职责处理输入，完成 `to Password Credential` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `toPasswordCredential` processes its inputs according to `CredentialRow`'s responsibility, performs the `to Password Credential` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `toPasswordCredential` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `toPasswordCredential`, then continue the business flow using its result, exception, or side effect.
         *
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        PasswordCredentialVO toPasswordCredential() {
            boolean active = tenant.getStatus() == TenantStatusEnum.ACTIVE
                    && user.getStatus() == UserStatusEnum.ACTIVE
                    && credential.getStatus() != UserCredentialStatusEnum.DISABLED
                    && credential.getStatus() != UserCredentialStatusEnum.EXPIRED;
            return new PasswordCredentialVO(
                    tenant.getCode(),
                    user.getNormalizedUsername(),
                    user.getId().toString(),
                    credential.getPasswordHash(),
                    credential.getFailedAttempts(),
                    credential.getLockedUntil(),
                    active);
        }
    }
