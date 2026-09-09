# Archetype native / Open dependency migration — execution and conformance audit

**Verdict: PARTIAL — Spec requirements are unmet or unverified.**

七个 Plan Step 均已实现、验证并分别提交。最终七产品集成测试通过，850 项生成项目测试无失败、错误或跳过；release profile 验证通过，21 个 main/source/javadoc 包完整。最终审计保留一项任务基线已有的旧 SQL 归档检查失败，以及尚未执行的运行时验收。

| 字段 | 审计值 |
| --- | --- |
| Audit HEAD | b9d0d6f8ba0b0bf4520bf8dac8e5bf4b97ed4ea1 |
| Branch | main |
| Plan | [2026-09-08-03-30-archetype-native-open-dependency-migration.md](/Users/mario/SelfProject/Egon-COLA/docs/egon/plan/2026-09-08-03-30-archetype-native-open-dependency-migration.md) |
| Plan revision | Ready 输入；包含 PLAN-CLAR-001..014；执行状态以本报告 Step 表为准 |
| Audit time | 2026-09-08T20:44:41+08:00 |
| 执行方式 | 主代理直接完成；未使用子代理 |
| 证据目录 | /var/folders/gb/vwfj36c909xgtbr0jl267d880000gp/T/egon-native-migration-p2k5o_1_ |

## 1. Effective Specs and interpretation

| Spec | 生效范围 | 文件最后提交 | SHA-256 |
| --- | --- | --- | --- |
| [2026-09-07-16-52-archetype-native-open-dependency-governance.md](/Users/mario/SelfProject/Egon-COLA/docs/egon/spec/2026-09-07-16-52-archetype-native-open-dependency-governance.md) | §3–§20（primary） | afc980095ed6eda21456910841af2074eea4032e | 875b45e3f1185298966127b7878649a86ca40212316ab9fe0317afdd9db5bdbe |
| [2026-09-03-11-04-archetype-generated-reactor-spring-flyway-design.md](/Users/mario/SelfProject/Egon-COLA/docs/egon/spec/2026-09-03-11-04-archetype-generated-reactor-spring-flyway-design.md) | §7、§8、§14、§16、§19、§20 | 31d28c92c5775865aefebea1962c5f8040360220 | e33f0576707d32545874bffdf8f7eace931297e098bc171a4b6dfea7734fae79 |
| [2026-08-23-16-43-open-source-archetype-family.md](/Users/mario/SelfProject/Egon-COLA/docs/egon/spec/2026-08-23-16-43-open-source-archetype-family.md) | §3、§7、§9、§16 | 27447b6aa513ba5c21e4fc17b6e6e01ead20b4ca | f18240e00c570752b0681b1b5618ce4019ddfa7b8c32df67ed72b52b18695b3b |

Primary Spec 的 parent/BOM、native transport 与 Common allowlist 决策覆盖前驱 Spec 的直接 Boot-parent/原 native 冻结说明。前驱中的“六产品”适用于既有六个产品；本次既有 Agent 一并纳入七产品门禁。旧数据库、Open wire、业务 API 和 Agent workflow 的兼容边界继续生效。Spec-only 阶段“不实施代码”的限制已由用户明确授权执行此 Plan 覆盖。

## 2. Step completion and commits

| Step | 任务 | 状态 | 提交 | 验证 |
| --- | --- | --- | --- | --- |
| 1 | Establish parent and dependency ownership | Committed | d6e9e17c1d3330eec3b624bb27b17a40a9dc1636 | 7 个 effective POM；owner/version-bump RED/GREEN、回滚夹具通过 |
| 2 | Add native unary Protobuf contracts | Committed | 530ccb7e06122f6048b75b393099d6194f5b21e9 | 133 项 focused + 156 项扩展测试通过；31 unary operation |
| 3 | Migrate native light provider and Tianshu/API Doc wiring | Committed | c97234bae444043ecc468a4a9c7b4d04ccfc1412 | 207 项 Light 回归；架构扫描与 native 配置/HTTP 检查通过 |
| 4 | Migrate native service/web RPC, Tianshu and API Doc | Committed | a39db1d89c7d59f209eee49019a7dc1c4afe0de4 | 1,100 项相关 reactor 回归；Service/Web 246/343 类架构检查通过 |
| 5 | Enforce open compatibility and Agent minimal dependencies | Committed | d144a464f7f6e07a19ac5307fd414cbcfd3221f5 | 697 项 Open/Agent 回归；源声明与 runtime tree 边界通过 |
| 6 | Normalize generated parent and sentinel handling | Committed | d2db5deb6216b56270327e7df7d55e6ba39eb75a | parent/alias、sentinel、锁、确定性、失败保留和 signal cleanup 夹具通过 |
| 7 | Update definitions, regenerate and close release gates | Committed | b9d0d6f8ba0b0bf4520bf8dac8e5bf4b97ed4ea1 | 3,081 项 source 回归；850 项 generated 测试；七产品 IT、release verify、21 附件及隔离解析通过 |

每个 Step 的 17 项 Manual Checks 和 10 条 Literal Rules 已在提交前单独执行；详细逐行证据保存在对应 `step-XX.json`。最终审计的状态见第 7/8 节，不用提交前的 PASS 掩盖后来发现的旧归档问题。

| Step | 提交前 Manual Checks | 提交前 Literal Rules | 证据 |
| --- | --- | --- | --- |
| 1 | 17 行：PASS 或有范围证据的 N/A | 10 行：PASS 或有范围证据的 N/A | [step-01.json](/var/folders/gb/vwfj36c909xgtbr0jl267d880000gp/T/egon-native-migration-p2k5o_1_/step-01.json) |
| 2 | 17 行：PASS 或有范围证据的 N/A | 10 行：PASS 或有范围证据的 N/A | [step-02.json](/var/folders/gb/vwfj36c909xgtbr0jl267d880000gp/T/egon-native-migration-p2k5o_1_/step-02.json) |
| 3 | 17 行：PASS 或有范围证据的 N/A | 10 行：PASS 或有范围证据的 N/A | [step-03.json](/var/folders/gb/vwfj36c909xgtbr0jl267d880000gp/T/egon-native-migration-p2k5o_1_/step-03.json) |
| 4 | 17 行：PASS 或有范围证据的 N/A | 10 行：PASS 或有范围证据的 N/A | [step-04.json](/var/folders/gb/vwfj36c909xgtbr0jl267d880000gp/T/egon-native-migration-p2k5o_1_/step-04.json) |
| 5 | 17 行：PASS 或有范围证据的 N/A | 10 行：PASS 或有范围证据的 N/A | [step-05.json](/var/folders/gb/vwfj36c909xgtbr0jl267d880000gp/T/egon-native-migration-p2k5o_1_/step-05.json) |
| 6 | 17 行：PASS 或有范围证据的 N/A | 10 行：PASS 或有范围证据的 N/A | [step-06.json](/var/folders/gb/vwfj36c909xgtbr0jl267d880000gp/T/egon-native-migration-p2k5o_1_/step-06.json) |
| 7 | 17 行：PASS 或有范围证据的 N/A | 10 行：PASS 或有范围证据的 N/A | [step-07.json](/var/folders/gb/vwfj36c909xgtbr0jl267d880000gp/T/egon-native-migration-p2k5o_1_/step-07.json) |

### Corrective commits and approved scope clarifications

| 归属 | 提交 | 修正及证据 |
| --- | --- | --- |
| 1 correction | afc980095ed6eda21456910841af2074eea4032e | Boot parent disables standard Maven resource delimiters; restore library runtime-version filtering and explicitly retain archetype template protection；[] |
| 1 correction | 0e66ccfb3ca925cacdeecd45f73d46229622f7d2 | Preserve library Shade configuration after Boot parent adoption；['PLAN-CLAR-012: Boot default transformer merged invalid resource into ManifestResourceTransformer; root-only restoration of explicit module shading'] |
| 2 correction | 26121b707dd15f8d96b3e53463d19fdee7ff8b05 | compiler-generated mapping class is not a business Facade implementation; exact class configuration preserves all business Adapter checks；[] |
| 3 correction | 4da6f0edf4c4f98b28f1f4a5cea979fc52368ecd | Require generated Compose Tianshu application identity；['PLAN-CLAR-013: explicit environment identity avoids Velocity tokens in unfiltered Compose'] |
| 3 correction | d8df6793d83b99d67f2d978c5466f264722cd519 | Tianshu HTTP registration must resolve effective server.port rather than separately reading SERVER_PORT; preserves Maven/JVM runtime port overrides；[] |
| 4 correction | 6557635695c3d726b58490dba220c02977da07cc | Require generated Compose Tianshu application identity；['PLAN-CLAR-013: explicit environment identity avoids Velocity tokens in unfiltered Compose'] |
| 4 correction | 8b0587fab5cf700e451b0eada690596c4c634bac | Complete package documentation for native adapters；['PLAN-CLAR-011: routine repair under existing authorization; no behavior/test requirement changed'] |
| 6 correction | ec2487700ef55395debdb24ccb0e70ee965f1d16 | Normalize Maven parentArtifactId alias before Velocity escaping；['PLAN-CLAR-014: preserve source test assertions and normalize plugin alias in existing pipeline'] |

另有执行准备/澄清文档提交：`a9be951d`（Accepted Spec/Ready Plan）、`6f0ea7f07`（六个 Config Data 解密文件）、`75165e179`（Light MVC 切片接线）。历史提交未 amend、squash、reset 或重写。

### Exact committed file scopes

<details>
<summary>1 — d6e9e17c1 — Establish parent and dependency ownership</summary>

```text
docs/egon/plan/2026-09-08-03-30-archetype-native-open-dependency-migration.md
docs/egon/spec/2026-09-07-16-52-archetype-native-open-dependency-governance.md
egon-cola-archetypes/pom.xml
egon-cola-archetypes/source-projects/egon-cola-source-agent/egon-cola-source-agent-infrastructure/pom.xml
egon-cola-archetypes/source-projects/egon-cola-source-agent/pom.xml
egon-cola-archetypes/source-projects/egon-cola-source-light-open/pom.xml
egon-cola-archetypes/source-projects/egon-cola-source-light/pom.xml
egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-adapter/pom.xml
egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-domain/pom.xml
egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-facade/pom.xml
egon-cola-archetypes/source-projects/egon-cola-source-service-open/egon-cola-source-service-open-infrastructure/pom.xml
egon-cola-archetypes/source-projects/egon-cola-source-service-open/pom.xml
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/pom.xml
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-domain/pom.xml
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/pom.xml
egon-cola-archetypes/source-projects/egon-cola-source-service/pom.xml
egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-adapter/pom.xml
egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-domain/pom.xml
egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-facade/pom.xml
egon-cola-archetypes/source-projects/egon-cola-source-web-open/egon-cola-source-web-open-infrastructure/pom.xml
egon-cola-archetypes/source-projects/egon-cola-source-web-open/pom.xml
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/pom.xml
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-domain/pom.xml
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/pom.xml
egon-cola-archetypes/source-projects/egon-cola-source-web/pom.xml
egon-cola-components/egon-cola-components-bom/pom.xml
egon-cola-components/pom.xml
pom.xml
scripts/bump_cola_version.sh
scripts/check-archetype-dependency-ownership.py
scripts/test-bump-cola-version.sh
```

</details>

<details>
<summary>2 — 530ccb7e0 — Add native unary Protobuf contracts</summary>

```text
egon-cola-archetypes/egon-cola-evaluation-facade/pom.xml
egon-cola-archetypes/egon-cola-evaluation-facade/src/main/java/top/egon/cola/evaluation/facade/rpc/CourseRpcService.java
egon-cola-archetypes/egon-cola-evaluation-facade/src/main/java/top/egon/cola/evaluation/facade/rpc/EvaluationRpcConverter.java
egon-cola-archetypes/egon-cola-evaluation-facade/src/main/java/top/egon/cola/evaluation/facade/rpc/ExamRpcService.java
egon-cola-archetypes/egon-cola-evaluation-facade/src/main/java/top/egon/cola/evaluation/facade/rpc/ScoreRpcService.java
egon-cola-archetypes/egon-cola-evaluation-facade/src/main/proto/evaluation_facade.proto
egon-cola-archetypes/egon-cola-evaluation-facade/src/test/java/top/egon/cola/evaluation/facade/NativeEvaluationRpcContractTest.java
egon-cola-archetypes/egon-cola-evaluation-facade/src/test/java/top/egon/cola/evaluation/facade/NativeEvaluationRpcMappingTest.java
egon-cola-archetypes/egon-cola-organization-facade/pom.xml
egon-cola-archetypes/egon-cola-organization-facade/src/main/java/top/egon/cola/organization/facade/rpc/GradeRpcService.java
egon-cola-archetypes/egon-cola-organization-facade/src/main/java/top/egon/cola/organization/facade/rpc/OrganizationRpcConverter.java
egon-cola-archetypes/egon-cola-organization-facade/src/main/java/top/egon/cola/organization/facade/rpc/PermissionRpcService.java
egon-cola-archetypes/egon-cola-organization-facade/src/main/java/top/egon/cola/organization/facade/rpc/RoleRpcService.java
egon-cola-archetypes/egon-cola-organization-facade/src/main/java/top/egon/cola/organization/facade/rpc/SchoolClassRpcService.java
egon-cola-archetypes/egon-cola-organization-facade/src/main/java/top/egon/cola/organization/facade/rpc/UserRpcService.java
egon-cola-archetypes/egon-cola-organization-facade/src/main/proto/organization_facade.proto
egon-cola-archetypes/egon-cola-organization-facade/src/test/java/top/egon/cola/organization/facade/NativeOrganizationRpcContractTest.java
egon-cola-archetypes/egon-cola-organization-facade/src/test/java/top/egon/cola/organization/facade/NativeOrganizationRpcMappingTest.java
egon-cola-archetypes/source-projects/egon-cola-source-light/pom.xml
egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/rpc/CourseRpcService.java
egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/rpc/LightRpcConverter.java
egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/rpc/PermissionRpcService.java
egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/rpc/SchoolClassRpcService.java
egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/rpc/UserRpcService.java
egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/proto/teaching_user_facade.proto
egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/facade/NativeRpcContractTest.java
egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/facade/NativeRpcMappingTest.java
```

</details>

<details>
<summary>3 — c97234bae — Migrate native light provider and Tianshu/API Doc wiring</summary>

```text
egon-cola-archetypes/source-projects/egon-cola-source-light/README.md
egon-cola-archetypes/source-projects/egon-cola-source-light/README.zh-CN.md
egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/compose/compose.docker.prod.yaml
egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/compose/compose.docker.yaml
egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/compose/compose.nerdctl.prod.yaml
egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/compose/compose.nerdctl.yaml
egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/compose/compose.podman.prod.yaml
egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/compose/compose.podman.yaml
egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/container/README.md
egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/env/.env.example
egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/env/.env.prod.example
egon-cola-archetypes/source-projects/egon-cola-source-light/pom.xml
egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/teaching/rpc/CourseRpcProvider.java
egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/teaching/rpc/SchoolClassRpcProvider.java
egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/teaching/rpc/package-info.java
egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/user/rpc/PermissionRpcProvider.java
egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/user/rpc/UserRpcProvider.java
egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/adapter/user/rpc/package-info.java
egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/rpc/NativeRpcValidationGroup.java
egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/rpc/RpcIdQuery.java
egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/teaching/dto/CreateCourseDTO.java
egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/teaching/dto/CreateSchoolClassDTO.java
egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/teaching/dto/ScheduleCourseDTO.java
egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/user/dto/AssignRoleDTO.java
egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/user/dto/CreateUserDTO.java
egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/user/dto/GrantPermissionDTO.java
egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/start/StudentManagementApplication.java
egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/start/config/NativeHttpSecurityConfiguration.java
egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/start/config/NativeRpcConfiguration.java
egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/start/config/OpenApiConfig.java
egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/start/config/encryption/ConfigDecryptEnvironmentPostProcessor.java
egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application-dev.yml
egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application-prod.yml
egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application-test.yml
egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application.yml
egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/bootstrap-dev.yml
egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/bootstrap-prod.yml
egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/bootstrap-test.yml
egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/bootstrap.yml
egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/NativeRpcProviderTest.java
egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/teaching/controller/CourseControllerTest.java
egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/teaching/controller/SchoolClassControllerTest.java
egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/teaching/rpc/CourseRpcProviderTest.java
egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/teaching/rpc/SchoolClassRpcProviderTest.java
egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/user/controller/PermissionControllerTest.java
egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/user/controller/RoleControllerTest.java
egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/user/controller/UserControllerTest.java
egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/user/rpc/PermissionRpcProviderTest.java
egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/adapter/user/rpc/UserRpcProviderTest.java
egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/start/StudentManagementApplicationTest.java
egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/start/config/NativeHttpCompatibilityTest.java
egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/start/config/NativeRpcConfigurationTest.java
egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/start/config/RuntimeConfigurationTest.java
egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/start/config/encryption/ConfigDecryptEnvironmentPostProcessorTest.java
scripts/check-native-archetype-boundaries.py
```

</details>

<details>
<summary>4 — a39db1d89 — Migrate native service/web RPC, Tianshu and API Doc</summary>

```text
egon-cola-archetypes/egon-cola-organization-facade/src/main/java/top/egon/cola/organization/facade/rpc/RpcIdQuery.java
egon-cola-archetypes/egon-cola-organization-facade/src/main/java/top/egon/cola/organization/facade/rpc/RpcSchoolClassQuery.java
egon-cola-archetypes/source-projects/egon-cola-source-service/README.md
egon-cola-archetypes/source-projects/egon-cola-source-service/README.zh-CN.md
egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/compose/compose.docker.prod.yaml
egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/compose/compose.docker.yaml
egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/compose/compose.nerdctl.prod.yaml
egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/compose/compose.nerdctl.yaml
egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/compose/compose.podman.prod.yaml
egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/compose/compose.podman.yaml
egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/container/README.md
egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/env/.env.example
egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/env/.env.prod.example
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/pom.xml
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/main/java/top/egon/cola/archetype/source/service/adapter/config/NativeRpcConfiguration.java
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/main/java/top/egon/cola/archetype/source/service/adapter/course/facade/impl/CourseFacadeImpl.java
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/main/java/top/egon/cola/archetype/source/service/adapter/course/rpc/CourseRpcProvider.java
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/main/java/top/egon/cola/archetype/source/service/adapter/exam/facade/impl/ExamFacadeImpl.java
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/main/java/top/egon/cola/archetype/source/service/adapter/exam/facade/impl/ScoreFacadeImpl.java
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/main/java/top/egon/cola/archetype/source/service/adapter/exam/rpc/ExamRpcProvider.java
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/main/java/top/egon/cola/archetype/source/service/adapter/exam/rpc/ScoreRpcProvider.java
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/test/java/top/egon/cola/archetype/source/service/adapter/NativeServiceRpcProviderTest.java
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/test/java/top/egon/cola/archetype/source/service/adapter/rpc/EvaluationDubboTripleIntegrationTest.java
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/test/java/top/egon/cola/archetype/source/service/adapter/rpc/package-info.java
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/pom.xml
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/java/top/egon/cola/archetype/source/service/infrastructure/client/organization/DubboOrganizationDirectoryClient.java
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/java/top/egon/cola/archetype/source/service/infrastructure/client/organization/NativeOrganizationDirectoryClient.java
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/java/top/egon/cola/archetype/source/service/infrastructure/client/organization/NativeOrganizationRpcConfiguration.java
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/java/top/egon/cola/archetype/source/service/infrastructure/client/organization/NativeOrganizationRpcProperties.java
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/java/top/egon/cola/archetype/source/service/infrastructure/client/organization/OrganizationClientFailureMapper.java
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/java/top/egon/cola/archetype/source/service/infrastructure/client/organization/OrganizationDirectoryConverter.java
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/test/java/top/egon/cola/archetype/source/service/infrastructure/client/organization/DubboOrganizationDirectoryClientTest.java
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/test/java/top/egon/cola/archetype/source/service/infrastructure/client/organization/NativeOrganizationDirectoryClientTest.java
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/pom.xml
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/java/top/egon/cola/archetype/source/service/starter/EvaluationServiceApplication.java
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/java/top/egon/cola/archetype/source/service/starter/config/NativeHttpSecurityConfiguration.java
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/java/top/egon/cola/archetype/source/service/starter/config/encryption/ConfigDecryptEnvironmentPostProcessor.java
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application-dev.yml
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application-prod.yml
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application-test.yml
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/application.yml
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/bootstrap-dev.yml
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/bootstrap-prod.yml
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/bootstrap-test.yml
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/main/resources/bootstrap.yml
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/test/java/top/egon/cola/archetype/source/service/starter/EvaluationDataSourceModeTest.java
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/test/java/top/egon/cola/archetype/source/service/starter/EvaluationExternalFreeContextTest.java
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/test/java/top/egon/cola/archetype/source/service/starter/NativeServiceConfigurationTest.java
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-starter/src/test/java/top/egon/cola/archetype/source/service/starter/config/encryption/ConfigDecryptEnvironmentPostProcessorTest.java
egon-cola-archetypes/source-projects/egon-cola-source-service/pom.xml
egon-cola-archetypes/source-projects/egon-cola-source-web/README.md
egon-cola-archetypes/source-projects/egon-cola-source-web/README.zh-CN.md
egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/compose/compose.docker.prod.yaml
egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/compose/compose.docker.yaml
egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/compose/compose.nerdctl.prod.yaml
egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/compose/compose.nerdctl.yaml
egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/compose/compose.podman.prod.yaml
egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/compose/compose.podman.yaml
egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/container/README.md
egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/env/.env.example
egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/env/.env.prod.example
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/pom.xml
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/src/main/java/top/egon/cola/archetype/source/web/adapter/config/NativeRpcConfiguration.java
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/src/main/java/top/egon/cola/archetype/source/web/adapter/facade/impl/OrganizationFacadeSupport.java
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/src/main/java/top/egon/cola/archetype/source/web/adapter/facade/impl/OrganizationRpcContextDTO.java
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/src/main/java/top/egon/cola/archetype/source/web/adapter/facade/impl/OrganizationRpcContextInterceptor.java
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/src/main/java/top/egon/cola/archetype/source/web/adapter/teaching/rpc/SchoolClassRpcProvider.java
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/src/main/java/top/egon/cola/archetype/source/web/adapter/user/rpc/UserRpcProvider.java
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/src/test/java/top/egon/cola/archetype/source/web/adapter/NativeOrganizationRpcContextTest.java
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/src/test/java/top/egon/cola/archetype/source/web/adapter/NativeOrganizationRpcProviderTest.java
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/src/test/java/top/egon/cola/archetype/source/web/adapter/OrganizationDubboProviderConfigurationTest.java
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/pom.xml
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/main/java/top/egon/cola/archetype/source/web/infrastructure/client/evaluation/DubboEvaluationQueryClient.java
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/main/java/top/egon/cola/archetype/source/web/infrastructure/client/evaluation/EvaluationClientFailureMapper.java
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/main/java/top/egon/cola/archetype/source/web/infrastructure/client/evaluation/EvaluationQueryConverter.java
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/main/java/top/egon/cola/archetype/source/web/infrastructure/client/evaluation/NativeEvaluationQueryClient.java
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/main/java/top/egon/cola/archetype/source/web/infrastructure/client/evaluation/NativeEvaluationRpcConfiguration.java
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/main/java/top/egon/cola/archetype/source/web/infrastructure/client/evaluation/NativeEvaluationRpcProperties.java
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/test/java/top/egon/cola/archetype/source/web/infrastructure/client/evaluation/DubboEvaluationQueryClientTest.java
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/test/java/top/egon/cola/archetype/source/web/infrastructure/client/evaluation/NativeEvaluationQueryClientTest.java
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/pom.xml
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/java/top/egon/cola/archetype/source/web/starter/OrganizationApplication.java
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/java/top/egon/cola/archetype/source/web/starter/config/NativeHttpSecurityConfiguration.java
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/java/top/egon/cola/archetype/source/web/starter/config/OrganizationSwaggerConfig.java
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/java/top/egon/cola/archetype/source/web/starter/config/encryption/ConfigDecryptEnvironmentPostProcessor.java
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application-dev.yml
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application-prod.yml
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application-test.yml
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/application.yml
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/bootstrap-dev.yml
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/bootstrap-prod.yml
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/bootstrap-test.yml
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/main/resources/bootstrap.yml
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/test/java/top/egon/cola/archetype/source/web/starter/NativeHttpCompatibilityTest.java
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/test/java/top/egon/cola/archetype/source/web/starter/NativeWebConfigurationTest.java
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/test/java/top/egon/cola/archetype/source/web/starter/OrganizationApplicationTest.java
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/test/java/top/egon/cola/archetype/source/web/starter/OrganizationDataSourceModeTest.java
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-starter/src/test/java/top/egon/cola/archetype/source/web/starter/config/encryption/ConfigDecryptEnvironmentPostProcessorTest.java
egon-cola-archetypes/source-projects/egon-cola-source-web/pom.xml
```

</details>

<details>
<summary>5 — d144a464f — Enforce open compatibility and Agent minimal dependencies</summary>

```text
egon-cola-archetypes/definitions/egon-cola-archetype-agent/src/test/resources/projects/basic/verify.groovy
egon-cola-archetypes/definitions/egon-cola-archetype-light-open/src/test/resources/projects/basic/open-dependency-boundary.groovy
scripts/check-archetype-family-boundaries.py
```

</details>

<details>
<summary>6 — d2db5deb6 — Normalize generated parent and sentinel handling</summary>

```text
scripts/generate_archetypes.sh
scripts/test-generate-archetypes.sh
scripts/test-generated-parent-normalization.sh
```

</details>

<details>
<summary>7 — b9d0d6f8b — Update definitions, regenerate and close release gates</summary>

```text
egon-cola-archetypes/definitions/egon-cola-archetype-agent/architecture-docs/agent-multi-module-architecture.md
egon-cola-archetypes/definitions/egon-cola-archetype-agent/src/main/javadoc/README.md
egon-cola-archetypes/definitions/egon-cola-archetype-agent/src/test/resources/projects/basic/verify.groovy
egon-cola-archetypes/definitions/egon-cola-archetype-light-open/architecture-docs/large-monolith-light-domain-architecture.md
egon-cola-archetypes/definitions/egon-cola-archetype-light-open/src/main/javadoc/README.md
egon-cola-archetypes/definitions/egon-cola-archetype-light-open/src/test/resources/projects/basic/verify.groovy
egon-cola-archetypes/definitions/egon-cola-archetype-light/architecture-docs/large-monolith-light-domain-architecture.md
egon-cola-archetypes/definitions/egon-cola-archetype-light/src/main/javadoc/README.md
egon-cola-archetypes/definitions/egon-cola-archetype-light/src/main/resources/META-INF/maven/archetype-metadata.xml
egon-cola-archetypes/definitions/egon-cola-archetype-light/src/test/resources/projects/basic/verify.groovy
egon-cola-archetypes/definitions/egon-cola-archetype-service-open/architecture-docs/student-management-service-only-rpc-mq-architecture.md
egon-cola-archetypes/definitions/egon-cola-archetype-service-open/src/main/javadoc/README.md
egon-cola-archetypes/definitions/egon-cola-archetype-service-open/src/test/resources/projects/basic/verify.groovy
egon-cola-archetypes/definitions/egon-cola-archetype-service/architecture-docs/student-management-service-only-rpc-mq-architecture.md
egon-cola-archetypes/definitions/egon-cola-archetype-service/src/main/javadoc/README.md
egon-cola-archetypes/definitions/egon-cola-archetype-service/src/test/resources/projects/basic/verify.groovy
egon-cola-archetypes/definitions/egon-cola-archetype-web-open/architecture-docs/multi-project-multi-module-architecture.md
egon-cola-archetypes/definitions/egon-cola-archetype-web-open/src/main/javadoc/README.md
egon-cola-archetypes/definitions/egon-cola-archetype-web-open/src/test/resources/projects/basic/verify.groovy
egon-cola-archetypes/definitions/egon-cola-archetype-web/architecture-docs/multi-project-multi-module-architecture.md
egon-cola-archetypes/definitions/egon-cola-archetype-web/src/main/javadoc/README.md
egon-cola-archetypes/definitions/egon-cola-archetype-web/src/test/resources/projects/basic/verify.groovy
```

</details>

<details>
<summary>1 correction — afc980095 — Boot parent disables standard Maven resource delimiters; restore library runtime-version filtering and explicitly retain archetype template protection</summary>

```text
docs/egon/plan/2026-09-08-03-30-archetype-native-open-dependency-migration.md
docs/egon/spec/2026-09-07-16-52-archetype-native-open-dependency-governance.md
egon-cola-archetypes/pom.xml
pom.xml
```

</details>

<details>
<summary>1 correction — 0e66ccfb3 — Preserve library Shade configuration after Boot parent adoption</summary>

```text
docs/egon/plan/2026-09-08-03-30-archetype-native-open-dependency-migration.md
pom.xml
```

</details>

<details>
<summary>2 correction — 26121b707 — compiler-generated mapping class is not a business Facade implementation; exact class configuration preserves all business Adapter checks</summary>

```text
docs/egon/plan/2026-09-08-03-30-archetype-native-open-dependency-migration.md
docs/egon/spec/2026-09-07-16-52-archetype-native-open-dependency-governance.md
egon-cola-archetypes/source-projects/egon-cola-source-light/pom.xml
```

</details>

<details>
<summary>3 correction — 4da6f0edf — Require generated Compose Tianshu application identity</summary>

```text
docs/egon/plan/2026-09-08-03-30-archetype-native-open-dependency-migration.md
egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/compose/compose.docker.prod.yaml
egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/compose/compose.docker.yaml
egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/compose/compose.nerdctl.prod.yaml
egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/compose/compose.nerdctl.yaml
egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/compose/compose.podman.prod.yaml
egon-cola-archetypes/source-projects/egon-cola-source-light/deploy/compose/compose.podman.yaml
```

</details>

<details>
<summary>3 correction — d8df6793d — Tianshu HTTP registration must resolve effective server.port rather than separately reading SERVER_PORT; preserves Maven/JVM runtime port overrides</summary>

```text
egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application-dev.yml
egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application-prod.yml
egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application-test.yml
egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/application.yml
egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/start/config/NativeRpcConfigurationTest.java
```

</details>

<details>
<summary>4 correction — 655763569 — Require generated Compose Tianshu application identity</summary>

```text
egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/compose/compose.docker.prod.yaml
egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/compose/compose.docker.yaml
egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/compose/compose.nerdctl.prod.yaml
egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/compose/compose.nerdctl.yaml
egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/compose/compose.podman.prod.yaml
egon-cola-archetypes/source-projects/egon-cola-source-service/deploy/compose/compose.podman.yaml
egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/compose/compose.docker.prod.yaml
egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/compose/compose.docker.yaml
egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/compose/compose.nerdctl.prod.yaml
egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/compose/compose.nerdctl.yaml
egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/compose/compose.podman.prod.yaml
egon-cola-archetypes/source-projects/egon-cola-source-web/deploy/compose/compose.podman.yaml
```

</details>

<details>
<summary>4 correction — 8b0587fab — Complete package documentation for native adapters</summary>

```text
docs/egon/plan/2026-09-08-03-30-archetype-native-open-dependency-migration.md
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/main/java/top/egon/cola/archetype/source/service/adapter/config/package-info.java
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/main/java/top/egon/cola/archetype/source/service/adapter/course/rpc/package-info.java
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/main/java/top/egon/cola/archetype/source/service/adapter/exam/rpc/package-info.java
egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-adapter/src/test/java/top/egon/cola/archetype/source/service/adapter/package-info.java
egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-adapter/src/main/java/top/egon/cola/archetype/source/web/adapter/config/package-info.java
```

</details>

<details>
<summary>6 correction — ec2487700 — Normalize Maven parentArtifactId alias before Velocity escaping</summary>

```text
docs/egon/plan/2026-09-08-03-30-archetype-native-open-dependency-migration.md
scripts/generate_archetypes.sh
scripts/test-generated-parent-normalization.sh
```

</details>

## 3. Validation results and evidence boundaries

| Gate | 实际命令 | 结果 | 输出 |
| --- | --- | --- | --- |
| Source reactor | `./mvnw -B -ntp -f egon-cola-archetypes/source-projects/pom.xml test` | PASS：3,081 tests；0 failures/errors/skips | [step-07-source-test.log](/var/folders/gb/vwfj36c909xgtbr0jl267d880000gp/T/egon-native-migration-p2k5o_1_/step-07-source-test.log) |
| Source local install | `./mvnw -B -ntp -f egon-cola-archetypes/source-projects/pom.xml -DskipTests install` | PASS：仅 artifact 前置，不作为测试通过证据 | [step-07-source-install-after-shade.log](/var/folders/gb/vwfj36c909xgtbr0jl267d880000gp/T/egon-native-migration-p2k5o_1_/step-07-source-install-after-shade.log) |
| Generator fixture | `bash scripts/test-generate-archetypes.sh` | PASS；含锁、原子失败保留、确定性 | [step-07-generator-fixtures.log](/var/folders/gb/vwfj36c909xgtbr0jl267d880000gp/T/egon-native-migration-p2k5o_1_/step-07-generator-fixtures.log) |
| Parent/alias fixture | `bash scripts/test-generated-parent-normalization.sh` | PASS；真实 alias RED 后 GREEN | [step-06-alias-green.log](/var/folders/gb/vwfj36c909xgtbr0jl267d880000gp/T/egon-native-migration-p2k5o_1_/step-06-alias-green.log) |
| Release fixture | `bash scripts/test-archetype-release.sh` | PASS；mock workflow/deploy failure contracts | [step-07-release-fixtures.log](/var/folders/gb/vwfj36c909xgtbr0jl267d880000gp/T/egon-native-migration-p2k5o_1_/step-07-release-fixtures.log) |
| Canonical generate | `bash scripts/generate_archetypes.sh generate` | PASS：七产品原子生成 | [step-07-generate-final.log](/var/folders/gb/vwfj36c909xgtbr0jl267d880000gp/T/egon-native-migration-p2k5o_1_/step-07-generate-final.log) |
| Canonical check | `bash scripts/generate_archetypes.sh check` | PASS：七产品路径/字节/mode 与当前输入一致 | [step-07-check-final.log](/var/folders/gb/vwfj36c909xgtbr0jl267d880000gp/T/egon-native-migration-p2k5o_1_/step-07-check-final.log) |
| Generated IT | `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -Pgenerated-archetypes clean integration-test` | PASS：七产品及每个 Published parent/runtime marker 实际执行 | [step-07-generated-it-final.log](/var/folders/gb/vwfj36c909xgtbr0jl267d880000gp/T/egon-native-migration-p2k5o_1_/step-07-generated-it-final.log) |
| Release shape | `./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -Pgenerated-archetypes -Prelease -Dgpg.skip=true verify` | PASS：测试继续执行；仅跳过签名；21 个附件完整 | [step-07-release-shape.log](/var/folders/gb/vwfj36c909xgtbr0jl267d880000gp/T/egon-native-migration-p2k5o_1_/step-07-release-shape.log) |
| Owner/runtime/static | `ownership --effective-poms；native light/service/web；family --runtime-trees` | PASS：5 BaseConverter、12 个 record、70 个 authored main Java 路径、31 operation | [final-audit.log](/var/folders/gb/vwfj36c909xgtbr0jl267d880000gp/T/egon-native-migration-p2k5o_1_/final-audit.log) |
| Extra legacy archive gate | `bash scripts/test-generate-archetypes.sh package reactor` | FAIL：exit 1；12 旧归档路径在任务基线已全部缺失 | [final-legacy-reactor-gate.log](/var/folders/gb/vwfj36c909xgtbr0jl267d880000gp/T/egon-native-migration-p2k5o_1_/final-legacy-reactor-gate.log) |
| Workflow static gate | `bash scripts/test-generate-archetypes.sh release` | PASS：exit 0 | [final-workflow-gate.log](/var/folders/gb/vwfj36c909xgtbr0jl267d880000gp/T/egon-native-migration-p2k5o_1_/final-workflow-gate.log) |

Spec 和 Plan 的 `--strict` 校验均通过。最终 `git diff --check` 通过。源码、生成消费者以及 release-profile 的测试证明均限于对应命令执行时的代码；3,081 项 source 测试发生在两个无关并发提交之前，不能宣称覆盖那些后来提交的 RBAC/前端修改。当前 archetype 输入与依赖修改均由最终 generated/release 测试验证。

| Generated family | Tests | Failures | Errors | Skipped |
| --- | --- | --- | --- | --- |
| light | 207 | 0 | 0 | 0 |
| service | 128 | 0 | 0 | 0 |
| web | 144 | 0 | 0 | 0 |
| light-open | 130 | 0 | 0 | 0 |
| service-open | 105 | 0 | 0 | 0 |
| web-open | 105 | 0 | 0 | 0 |
| agent | 31 | 0 | 0 | 0 |

隔离解析使用一个初始为空的 `top.egon` 仓库命名空间，83 个本次 Egon artifact 由 Maven 重新安装；外部依赖复用已有缓存，因此不是“所有外部缓存均为空”的网络下载测试。七个 source root 的 offline effective POM 成功，七个实际生成消费者的 root/child POM 复制到仓库外后整体 `validate` 成功。最终消费者 POM 哈希与隔离校验输入一致。该证据不证明这些新产物已经远程发布。

证据：[isolated-artifacts.json](/var/folders/gb/vwfj36c909xgtbr0jl267d880000gp/T/egon-native-migration-p2k5o_1_/isolated-artifacts.json)、[isolated-consumer-pom-hashes.json](/var/folders/gb/vwfj36c909xgtbr0jl267d880000gp/T/egon-native-migration-p2k5o_1_/isolated-consumer-pom-hashes.json)、[step-07-release-artifacts.json](/var/folders/gb/vwfj36c909xgtbr0jl267d880000gp/T/egon-native-migration-p2k5o_1_/step-07-release-artifacts.json)。

### Failed attempts and their resolution

| 失败 | 处置 | 最终状态 |
| --- | --- | --- |
| 移除 Cloud 后配置解密器仍依赖旧 Bootstrap API | 六文件按已授权范围改为 Config Data ORDER+1；保留密钥、导入、优先级断言 | 已修复并回归 |
| Light 原 MVC 切片返回403、生成 Mapper 被 ARCH-010误判 | 导入兼容安全链；仅精确分类编译器生成的 LightRpcConverterImpl，真实业务 Facade 负例继续拒绝 | 已修复并回归 |
| HTTP registration 固定8080；Boot parent 使版本资源保留占位符；Shade transformer 合并失败 | 分别修正有效 server.port、库资源过滤/模板保护、显式模块 Shade 继承 | 独立归属原 Step 提交；最终 build/IT/release 通过 |
| 首轮 generated IT：Light verifier、Service/Web parentArtifactId 未展开 | 修正生成 alias 和 Compose identity；更新实际配置 prefix、JUnit nested report、相对目录扫描、Service return 顺序及精确 Mapper 路径 | 最终七产品 IT 与 release verify 均通过 |
| 原生配置早期测试曾尝试 localhost Redis PING 并得到 NOAUTH | 排除 stock Redisson 自动配置；最终测试验证无其客户端，未启动 Redis 服务 | 历史尝试保留在 Step3 证据；最终配置测试通过 |
| 最终扩展归档审计失败 | 核对同一脚本路径要求及任务 baseline Git tree；未新增/恢复/改写历史 SQL | 仍未闭合；见 A-LEGACY-001 |

Service/Web 的 `-DskipTests verify` 曾仅用于生成诊断所需 jar/架构报告；这些命令不算测试 GREEN。最终结论使用之后完整重建的真实 IT 与 release verify。

## 4. Primary Spec conformance

| ID | 要求 | 实现/验证证据 | 状态 |
| --- | --- | --- | --- |
| Primary REQ-001 | Native RPC/Tianshu/API Doc owner 与接线 | Steps1–4,7；native starter POM、配置/Provider/Client；native scanner、850 generated tests | Satisfied |
| Primary REQ-002 | Open 外部体系与现有合同 | Steps1,5,7；Open root/local facade；697 focused 与 Open generated IT | Satisfied |
| Primary REQ-003 | Agent 最小依赖、Spring AI/ADK/Agent Flow | Steps1,5,7；实际 runtime tree/BOOT-INF/lib；Agent31 tests | Satisfied |
| Primary REQ-004 | Components BOM 统一版本 | Step1；Components BOM/archetypes parent；七 isolated effective POM | Satisfied |
| Primary REQ-005 | ShardingSphere 5.5.3 owner 在 archetype parent | Step1；源无 local owner；6 非 Agent packaged libraries 均5.5.3 | Satisfied |
| Primary REQ-006 | Commons Lang 3.20.0 通过 Common Core/BOM | Step1；root Boot 同名属性桥接与 BOM 值相等；七 packaged libraries | Satisfied |
| Primary REQ-007 | 具体 parent、empty relativePath、独立解析 | Steps1,6,7；83 artifacts 新装；7 source/7 仓库外消费者模型校验 | Satisfied |
| Primary REQ-008 | Native 源码、配置、测试、Compose、verifier 同步 | Steps3,4,7及 corrective commits；18 Compose marker gate、native scans | Satisfied |
| Primary REQ-009 | Open 所需 Common/ID/MP/DTP 保留 | Steps1,5,7；源代码哈希、source声明/runtime guard、Open生成回归 | Satisfied |
| Primary REQ-010 | 拓扑、变量、锁、原子替换与确定性 | Steps6,7；parent+alias RED/GREEN、全套 fixture、最终 generate/check | Satisfied |
| Primary REQ-011 | 31个 unary contract 与原 facade 语义 | Steps2–4,7；3 proto、12RpcService、5BaseConverter、31operation及映射/错误/context测试 | Satisfied |

### Unary operation inventory

| ID | Family / service.method | Business delegate | Request → response | 状态 |
| --- | --- | --- | --- | --- |
| RPC-001 | light / Course.CreateCourse | CourseFacade.createCourse | CreateCourseRpcRequest → CourseRpcResponse | Satisfied：contract/provider/mapping suites |
| RPC-002 | light / Course.GetCourse | CourseFacade.getCourse | GetCourseRpcRequest → CourseRpcResponse | Satisfied：contract/provider/mapping suites |
| RPC-003 | light / SchoolClass.CreateSchoolClass | SchoolClassFacade.createSchoolClass | CreateSchoolClassRpcRequest → SchoolClassRpcResponse | Satisfied：contract/provider/mapping suites |
| RPC-004 | light / SchoolClass.ScheduleCourse | SchoolClassFacade.scheduleCourse | ScheduleCourseRpcRequest → SchoolClassRpcResponse | Satisfied：contract/provider/mapping suites |
| RPC-005 | light / SchoolClass.GetSchoolClass | SchoolClassFacade.getSchoolClass | GetSchoolClassRpcRequest → SchoolClassRpcResponse | Satisfied：contract/provider/mapping suites |
| RPC-006 | light / User.CreateUser | UserFacade.createUser | CreateUserRpcRequest → UserRpcResponse | Satisfied：contract/provider/mapping suites |
| RPC-007 | light / User.AssignRole | UserFacade.assignRole | AssignRoleRpcRequest → UserRpcResponse | Satisfied：contract/provider/mapping suites |
| RPC-008 | light / User.GetUser | UserFacade.getUser | GetUserRpcRequest → UserRpcResponse | Satisfied：contract/provider/mapping suites |
| RPC-009 | light / Permission.GrantPermission | PermissionFacade.grantPermission | GrantPermissionRpcRequest → PermissionRpcResponse | Satisfied：contract/provider/mapping suites |
| RPC-010 | light / Permission.GetUserPermissions | PermissionFacade.getUserPermissions | GetUserPermissionsRpcRequest → PermissionListRpcResponse | Satisfied：contract/provider/mapping suites |
| RPC-011 | organization / User.CreateUser | UserFacade.createUser | CreateUserRpcRequest → UserRpcResponse | Satisfied：contract/provider/mapping suites |
| RPC-012 | organization / User.GetUser | UserFacade.getUser | GetUserRpcRequest → UserRpcResponse | Satisfied：contract/provider/mapping suites |
| RPC-013 | organization / Role.AssignRole | RoleFacade.assignRole | AssignRoleRpcRequest → RpcResponse | Satisfied：contract/provider/mapping suites |
| RPC-014 | organization / Permission.GrantPermission | PermissionFacade.grantPermission | GrantPermissionRpcRequest → RpcResponse | Satisfied：contract/provider/mapping suites |
| RPC-015 | organization / Permission.GetPermissionTree | PermissionFacade.getPermissionTree | GetPermissionTreeRpcRequest → PermissionTreeRpcResponse | Satisfied：contract/provider/mapping suites |
| RPC-016 | organization / Grade.CreateGrade | GradeFacade.createGrade | CreateGradeRpcRequest → GradeRpcResponse | Satisfied：contract/provider/mapping suites |
| RPC-017 | organization / Grade.GetGrade | GradeFacade.getGrade | GetGradeRpcRequest → GradeRpcResponse | Satisfied：contract/provider/mapping suites |
| RPC-018 | organization / SchoolClass.CreateSchoolClass | SchoolClassFacade.createSchoolClass | CreateSchoolClassRpcRequest → SchoolClassRpcResponse | Satisfied：contract/provider/mapping suites |
| RPC-019 | organization / SchoolClass.GetSchoolClass | SchoolClassFacade.getSchoolClass | GetSchoolClassRpcRequest → SchoolClassRpcResponse | Satisfied：contract/provider/mapping suites |
| RPC-020 | organization / SchoolClass.AssignUser | SchoolClassFacade.assignUser | AssignUserRpcRequest → RpcResponse | Satisfied：contract/provider/mapping suites |
| RPC-021 | evaluation / Course.CreateCourse | CourseFacade.create | CreateCourseRpcRequest → CourseRpcResponse | Satisfied：contract/provider/mapping suites |
| RPC-022 | evaluation / Course.ScheduleCourse | CourseFacade.scheduleCourse | ScheduleCourseRpcRequest → CourseScheduleRpcResponse | Satisfied：contract/provider/mapping suites |
| RPC-023 | evaluation / Course.GetCourse | CourseFacade.getCourse | GetCourseRpcRequest → CourseRpcResponse | Satisfied：contract/provider/mapping suites |
| RPC-024 | evaluation / Course.PageCourses | CourseFacade.pageCourses | PageCourseRpcRequest → PageCourseRpcResponse | Satisfied：contract/provider/mapping suites |
| RPC-025 | evaluation / Exam.CreateExam | ExamFacade.createExam | CreateExamRpcRequest → ExamRpcResponse | Satisfied：contract/provider/mapping suites |
| RPC-026 | evaluation / Exam.AttachPaper | ExamFacade.attachPaper | AttachExamPaperRpcRequest → ExamPaperRpcResponse | Satisfied：contract/provider/mapping suites |
| RPC-027 | evaluation / Exam.PublishExam | ExamFacade.publishExam | PublishExamRpcRequest → ExamRpcResponse | Satisfied：contract/provider/mapping suites |
| RPC-028 | evaluation / Exam.GetExam | ExamFacade.getExam | GetExamRpcRequest → ExamRpcResponse | Satisfied：contract/provider/mapping suites |
| RPC-029 | evaluation / Score.RecordScore | ScoreFacade.recordScore | RecordScoreRpcRequest → ScoreRpcResponse | Satisfied：contract/provider/mapping suites |
| RPC-030 | evaluation / Score.GetScore | ScoreFacade.getScore | GetScoreRpcRequest → ScoreRpcResponse | Satisfied：contract/provider/mapping suites |
| RPC-031 | evaluation / Score.PageScores | ScoreFacade.pageScores | PageScoreRpcRequest → PageScoreRpcResponse | Satisfied：contract/provider/mapping suites |

### Model, mapping and handoff audit

| Family | 模型映射 | 字段清单 | Converter/test | 状态 |
| --- | --- | --- | --- | --- |
| light | CreateCourseDTO ↔ CreateCourseRpcRequest | code, name, operatorId, requestId | LightRpcConverter / NativeRpcMappingTest | Satisfied |
| light | CreateSchoolClassDTO ↔ CreateSchoolClassRpcRequest | name, semester, operatorId, requestId | LightRpcConverter / NativeRpcMappingTest | Satisfied |
| light | ScheduleCourseDTO ↔ ScheduleCourseRpcRequest | schoolClassId, courseId, startsAt, endsAt, operatorId, requestId | LightRpcConverter / NativeRpcMappingTest | Satisfied |
| light | CreateUserDTO ↔ CreateUserRpcRequest | externalId, name, email, operatorId, requestId | LightRpcConverter / NativeRpcMappingTest | Satisfied |
| light | AssignRoleDTO ↔ AssignRoleRpcRequest | userId, roleCode, operatorId, requestId | LightRpcConverter / NativeRpcMappingTest | Satisfied |
| light | GrantPermissionDTO ↔ GrantPermissionRpcRequest | roleCode, permissionCode, operatorId, requestId | LightRpcConverter / NativeRpcMappingTest | Satisfied |
| light | CourseDTO ↔ CourseResponse | id, code, name, status | LightRpcConverter / NativeRpcMappingTest | Satisfied |
| light | SchoolClassDetailDTO ↔ SchoolClassResponse | id, name, semester, status, scheduleCount | LightRpcConverter / NativeRpcMappingTest | Satisfied |
| light | UserDetailDTO ↔ UserResponse | id, name, email, status | LightRpcConverter / NativeRpcMappingTest | Satisfied |
| light | PermissionDTO ↔ PermissionResponse | roleCode, permissionCode, status | LightRpcConverter / NativeRpcMappingTest | Satisfied |
| light | PermissionDetailDTO ↔ PermissionDetailResponse | code, name, children | LightRpcConverter / NativeRpcMappingTest | Satisfied |
| light | scalar/query ↔ GetCourseRpcRequest | courseId | Provider/Client scalar/default validation | Satisfied |
| light | scalar/query ↔ GetSchoolClassRpcRequest | schoolClassId | Provider/Client scalar/default validation | Satisfied |
| light | scalar/query ↔ GetUserRpcRequest | userId | Provider/Client scalar/default validation | Satisfied |
| light | scalar/query ↔ GetUserPermissionsRpcRequest | userId | Provider/Client scalar/default validation | Satisfied |
| organization | CreateUserDTO ↔ CreateUserRpcRequest | name, email | OrganizationRpcConverter / NativeOrganizationRpcMappingTest | Satisfied |
| organization | AssignRoleDTO ↔ AssignRoleRpcRequest | userId, roleCode | OrganizationRpcConverter / NativeOrganizationRpcMappingTest | Satisfied |
| organization | GrantPermissionDTO ↔ GrantPermissionRpcRequest | roleCode, permissionCode | OrganizationRpcConverter / NativeOrganizationRpcMappingTest | Satisfied |
| organization | CreateGradeDTO ↔ CreateGradeRpcRequest | code, name | OrganizationRpcConverter / NativeOrganizationRpcMappingTest | Satisfied |
| organization | CreateSchoolClassDTO ↔ CreateSchoolClassRpcRequest | name, gradeCode | OrganizationRpcConverter / NativeOrganizationRpcMappingTest | Satisfied |
| organization | AssignUserToClassDTO ↔ AssignUserRpcRequest | gradeId, userId, schoolClassId | OrganizationRpcConverter / NativeOrganizationRpcMappingTest | Satisfied |
| organization | UserDetailDTO ↔ UserResponse | id, name, email, status, roleCodes | OrganizationRpcConverter / NativeOrganizationRpcMappingTest | Satisfied |
| organization | PermissionTreeDTO ↔ PermissionTreeResponse | userId, permissionCodes | OrganizationRpcConverter / NativeOrganizationRpcMappingTest | Satisfied |
| organization | GradeDetailDTO ↔ GradeResponse | id, code, name, status | OrganizationRpcConverter / NativeOrganizationRpcMappingTest | Satisfied |
| organization | SchoolClassDetailDTO ↔ SchoolClassResponse | id, name, gradeCode, gradeName, status, userIds | OrganizationRpcConverter / NativeOrganizationRpcMappingTest | Satisfied |
| organization | scalar/query ↔ GetUserRpcRequest | userId | Provider/Client scalar/default validation | Satisfied |
| organization | scalar/query ↔ GetPermissionTreeRpcRequest | userId | Provider/Client scalar/default validation | Satisfied |
| organization | scalar/query ↔ GetGradeRpcRequest | gradeId | Provider/Client scalar/default validation | Satisfied |
| organization | scalar/query ↔ GetSchoolClassRpcRequest | gradeId, schoolClassId | Provider/Client scalar/default validation | Satisfied |
| evaluation | CreateCourseRequest ↔ CreateCourseRpcRequest | code, name, credit | EvaluationRpcConverter / NativeEvaluationRpcMappingTest | Satisfied |
| evaluation | ScheduleCourseRequest ↔ ScheduleCourseRpcRequest | courseId, classId, startsAt, endsAt | EvaluationRpcConverter / NativeEvaluationRpcMappingTest | Satisfied |
| evaluation | GetCourseRequest ↔ GetCourseRpcRequest | courseId | EvaluationRpcConverter / NativeEvaluationRpcMappingTest | Satisfied |
| evaluation | PageCourseRequest ↔ PageCourseRpcRequest | currentPage, pageSize | EvaluationRpcConverter / NativeEvaluationRpcMappingTest | Satisfied |
| evaluation | CreateExamRequest ↔ CreateExamRpcRequest | courseId, title, startsAt, endsAt | EvaluationRpcConverter / NativeEvaluationRpcMappingTest | Satisfied |
| evaluation | AttachExamPaperRequest ↔ AttachExamPaperRpcRequest | examId, title, totalPoints | EvaluationRpcConverter / NativeEvaluationRpcMappingTest | Satisfied |
| evaluation | PublishExamRequest ↔ PublishExamRpcRequest | examId | EvaluationRpcConverter / NativeEvaluationRpcMappingTest | Satisfied |
| evaluation | GetExamRequest ↔ GetExamRpcRequest | examId | EvaluationRpcConverter / NativeEvaluationRpcMappingTest | Satisfied |
| evaluation | RecordScoreRequest ↔ RecordScoreRpcRequest | examId, studentId, points | EvaluationRpcConverter / NativeEvaluationRpcMappingTest | Satisfied |
| evaluation | GetScoreRequest ↔ GetScoreRpcRequest | examId, scoreId | EvaluationRpcConverter / NativeEvaluationRpcMappingTest | Satisfied |
| evaluation | PageScoreRequest ↔ PageScoreRpcRequest | examId, currentPage, pageSize | EvaluationRpcConverter / NativeEvaluationRpcMappingTest | Satisfied |
| evaluation | CourseResponse ↔ CourseResponse | id, code, name, credit, status | EvaluationRpcConverter / NativeEvaluationRpcMappingTest | Satisfied |
| evaluation | CourseScheduleResponse ↔ CourseScheduleResponse | id, courseId, classId, startsAt, endsAt, status | EvaluationRpcConverter / NativeEvaluationRpcMappingTest | Satisfied |
| evaluation | ExamResponse ↔ ExamResponse | id, courseId, title, startsAt, endsAt, status | EvaluationRpcConverter / NativeEvaluationRpcMappingTest | Satisfied |
| evaluation | ExamPaperResponse ↔ ExamPaperResponse | id, examId, title, totalPoints, status | EvaluationRpcConverter / NativeEvaluationRpcMappingTest | Satisfied |
| evaluation | ScoreResponse ↔ ScoreResponse | id, examId, courseId, studentId, points, status | EvaluationRpcConverter / NativeEvaluationRpcMappingTest | Satisfied |
| evaluation | CourseResponse> ↔ PageCourseResponse | records, currentPage, totalPages, pageSize, totalCount | EvaluationRpcConverter / NativeEvaluationRpcMappingTest | Satisfied |
| evaluation | ScoreResponse> ↔ PageScoreResponse | records, currentPage, totalPages, pageSize, totalCount | EvaluationRpcConverter / NativeEvaluationRpcMappingTest | Satisfied |

另外逐项核对：成功/失败 envelope 的 code/message/traceId、nullable data、列表顺序及分页字段；默认/非法请求由 Jakarta Validation 拒绝；Light 复用 DTO 使用 native input/output groups；Service/Web 复用原共享 DTO 默认约束及正 ID Query。两个 domain client 保留失败分类和裁剪后的错误信息，成功 DTO 在交给 domain 前验证。Organization 四个 metadata 字段沿 scoped gRPC Context 传递，原 HTTP Holder 优先、SYSTEM/default actor/trace fallback 与 finally 清理保留。

5 个 MapStruct/BaseConverter：
- [EvaluationRpcConverter.java](/Users/mario/SelfProject/Egon-COLA/egon-cola-archetypes/egon-cola-evaluation-facade/src/main/java/top/egon/cola/evaluation/facade/rpc/EvaluationRpcConverter.java)
- [OrganizationRpcConverter.java](/Users/mario/SelfProject/Egon-COLA/egon-cola-archetypes/egon-cola-organization-facade/src/main/java/top/egon/cola/organization/facade/rpc/OrganizationRpcConverter.java)
- [LightRpcConverter.java](/Users/mario/SelfProject/Egon-COLA/egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/java/top/egon/cola/archetype/source/light/facade/rpc/LightRpcConverter.java)
- [OrganizationDirectoryConverter.java](/Users/mario/SelfProject/Egon-COLA/egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/java/top/egon/cola/archetype/source/service/infrastructure/client/organization/OrganizationDirectoryConverter.java)
- [EvaluationQueryConverter.java](/Users/mario/SelfProject/Egon-COLA/egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/main/java/top/egon/cola/archetype/source/web/infrastructure/client/evaluation/EvaluationQueryConverter.java)

### Cross-cutting and non-goal audit

| ID/范围 | 要求与证据 | 状态 | 剩余行动 |
| --- | --- | --- | --- |
| NFR-REPRO | owner/effective POM、原子生成/hash/lock 夹具、最终 check | Satisfied | None |
| NFR-FAILURE | 缺 parent/依赖/合约/invalid input 明确失败；transport/business error 单测 | Satisfied | None |
| NFR-COMPAT | HTTP/OAS/GraphQL/MQ 回归；Open Java/proto/SQL 哈希；无数据库变更 | Satisfied | None |
| NFR-RUNTIME（primary §15） | native discovery/跨进程连接、Tianshu/Tianquan-Shoubing/TLS 仅有源码及隔离测试证据 | Runtime unverified | 用户部署后验收连接、证书、HMAC、SERVICE token、目标 biz/app/env/group/version |
| NFR-SECURITY（primary §15） | 静态未引入凭据输出；配置示例无真实凭据 | Runtime unverified | 验收实际运行日志脱敏及 Tianquan-Shoubing/文档治理权限 |
| NFR-OPERABILITY（primary §15） | 复用组件健康/注册信号；未检验真实失败/恢复及运维告警 | Runtime unverified | 验收真实注册、健康、失败恢复与状态可观测性 |
| Database/schema | 本次无任何 SQL commit；82现存SQL及133native domain文件哈希不变 | Satisfied | 旧归档缺失另列 A-LEGACY-001 |
| Frontend | 本任务无页面改动；并发前端/RBAC提交排除 | Satisfied | None |
| Runtime/publish prohibition | 未启动业务项目/外部基础设施；未 push/PR/deploy/Central/sign | Satisfied | 运行/发布由用户另行发起 |

API 文档治理保持默认兼容配置。启用平台文档 catalog 前，现有业务 Controller 需要显式、唯一的 `@Operation(operationId=...)`；已验证治理 fixture 的401/403/200和缺 operationId 的拒绝路径，不能据此声称全部旧业务 Controller 已完成治理配置。Native transport/configuration 的替换是已批准的不兼容点；旧 Dubbo wire 调用方不能直接按原协议连接，需配套迁移。

## 5. Inherited Spec audit

| Generated Spec requirement | 生效语义 | 证据 | 状态 |
| --- | --- | --- | --- |
| Generated REQ-001 | 正常 source 作为唯一业务事实源 | 源码/生成回归与 generator source inputs | Satisfied |
| Generated REQ-002 | 旧 Maven 包装模块退出 reactor | 当前 root 仅2facade+generated profile；旧包装不在 reactor | Satisfied |
| Generated REQ-003 | definitions 非业务合同完整 | 7properties/metadata/post/verify/docs，实际IT | Satisfied |
| Generated REQ-004 | 固定 create-from-project 3.4.1 | generator 版本与完整生成日志 | Satisfied |
| Generated REQ-005 | 完整 generated reactor | 11-reactor条目，含7archetype；独立IT通过 | Satisfied |
| Generated REQ-006 | ignored/禁止手改派生物 | git ls-files为空；仅canonical generator/Maven构建 | Satisfied |
| Generated REQ-007 | definition动态清单一一对应 | 7definition/source/product一致 | Satisfied |
| Generated REQ-008 | 锁/staging/原子swap/清理 | unit fixture及两轮最终canonicalcheck | Satisfied |
| Generated REQ-009 | 原6公开GAV/拓扑兼容，Agent扩展 | 既有6+Agent均完整生成850tests | Satisfied |
| Generated REQ-010 | source不进公开发布集 | release shape21archetype附件；发布脚本mock allowlist | Satisfied |
| Generated REQ-011 | 一个root Central bundle | 发布脚本/CI静态及mock单deploy合同；真实publish未执行 | Satisfied |
| Generated REQ-012 | 全局Boot管理可追踪 | 按primary修订为Bootparent继承链；effective POM | Satisfied |
| Generated REQ-013 | 应用Boot入口唯一 | 旧直接Boot-parent要求由primary REQ007覆盖 | Satisfied |
| Generated REQ-014 | 依赖差异不静默漂移 | effective POM/dependency tree与source/generated回归 | Satisfied |
| Generated REQ-015 | Springdoc2.8.17 owner | parent管理与实际Open/runtime包检查 | Satisfied |
| Generated REQ-016 | master/shard迁移角色隔离 | 现有H2/sharding/Flyway用例通过，source/config未变 | Satisfied |
| Generated REQ-017 | 历史SQL原路径/字节 | 现存82SQL不变；旧12归档在本任务baseline已缺失，额外gate失败 | Partial |
| Generated REQ-018 | 历史Flyway决策边界 | 未新增/修改/恢复任何migration，不重开历史单文件决策 | Satisfied |
| Generated REQ-019 | Open人工SQL语义 | Open1173源文件哈希及Open测试通过 | Satisfied |
| Generated REQ-020 | 发布前fail-closed gates | 所有失败均记录、修正后重跑；未执行发布；旧archive失败显式保留 | Satisfied |

| Open Spec requirement | 生效语义 | 证据 | 状态 |
| --- | --- | --- | --- |
| Open REQ-001 | Light Open单模块 | 生成130tests | Satisfied |
| Open REQ-002 | Service Open拓扑 | 生成105tests | Satisfied |
| Open REQ-003 | Web Open拓扑 | 生成105tests | Satisfied |
| Open REQ-004 | 原非目标范围限制 | primary仅覆盖必要parent/native治理；Open业务文件哈希不变 | Satisfied |
| Open REQ-005 | 既有架构/依赖方向 | 原ArchUnit/verifier全部保留并通过 | Satisfied |
| Open REQ-006 | Boot3.5.16/Java21/Wrapper | generated model、runtime jar、wrapper既有断言 | Satisfied |
| Open REQ-007 | Cloud/SCA/Nacos | effective POM/source/runtime guards | Satisfied |
| Open REQ-008 | MyBatis Plus/无JPA | source与verifier保留，MP测试通过 | Satisfied |
| Open REQ-009 | ShardingSphere JDBC5.5.3 | runtime jar及路由测试 | Satisfied |
| Open REQ-010 | Open无Flyway | 源与runtime既有forbidden gate | Satisfied |
| Open REQ-011 | 人工PostgreSQL SQL | SQL哈希/现有manual schema测试 | Satisfied |
| Open REQ-012 | Common必要组件allowlist | primary REQ009修订；ID/Core/MP/DTP保留 | Satisfied |
| Open REQ-013 | DTP及profile行为 | source YAML/Java1173文件哈希、原测试 | Satisfied |
| Open REQ-014 | 21-operation外部Triple/gRPC | 本地facade/IDL哈希，实际Interop/Error/Deadline测试 | Satisfied |
| Open REQ-015 | Gateway外置/Light-Web Springdoc | 原forbidden/positive verifier与实际2.8.17jar | Satisfied |
| Open REQ-016 | 最小有消费者依赖 | family源声明/runtime分离guard | Satisfied |
| Open REQ-017 | 业务HTTP/GraphQL/MQ/domain | source与generated全部旧业务回归 | Satisfied |
| Open REQ-018 | 公开GAV/discovery/release形状 | 三个Open主包/metadata/source/javadoc | Satisfied |
| Open REQ-019 | 不依赖live基础设施测试 | 完整OpenIT；既有隔离H2/本地RPC测试夹具 | Satisfied |
| Open REQ-020 | 不运行项目或真实数据库SQL | 仅Maven/静态/隔离夹具；无外部服务启动 | Satisfied |
| Open REQ-021 | Common Snowflake Long/BIGINT | 源/SQL哈希，原ID/PO/schema测试 | Satisfied |

### Test-obligation coverage

| Spec / tests | 对应实证 | 状态 |
| --- | --- | --- |
| Primary TEST-001 | 7source effective POM +7仓库外生成消费者validate | Satisfied |
| Primary TEST-002 | final native scanner +generated source/runtime marker gates | Satisfied |
| Primary TEST-003 | actual runtime trees、BOM owner/effective模型、packaged libs | Satisfied |
| Primary TEST-004 | NativeConfiguration/HTTP/provider/client测试 | Satisfied |
| Primary TEST-005 | Open Triple/gRPC existing suites | Satisfied |
| Primary TEST-006 | Open source/generated compile与配置/依赖 | Satisfied |
| Primary TEST-007 | Agent runtime guard与31generated tests | Satisfied |
| Primary TEST-008 | canonicalgenerate+fixture | Satisfied |
| Primary TEST-009 | 最终canonicalcheck | Satisfied |
| Primary TEST-010 | 七产品完整IT及releaseverify | Satisfied |
| Primary TEST-011 | 31unary contracts+映射/错误/validation/context | Satisfied |
| Generated TEST-001..005 | 正常source/definitions/model/plugin/拓扑；旧包装reactor退出已核对 | Satisfied |
| Generated TEST-006..009 | 确定性、失败清理、锁、tracked boundary夹具及真实check | Satisfied |
| Generated TEST-010..015 | 生成IT/consumer/roundtrip/21releaseartifacts/发布mock合同 | Satisfied |
| Generated TEST-016 | 当前SQL哈希通过；原12archive路径baseline缺失 | Partial |
| Generated TEST-017..019 | 原migration角色/schema parity与OpenmanualSQL测试 | Satisfied |
| Generated TEST-020 | source/generated/release均通过；额外archive失败baseline证据保留 | Partial：旧archive门禁需单独确认 |

## 6. Open findings, risks and next actions

| ID | 状态 | 证据与影响 | 建议下一步 |
| --- | --- | --- | --- |
| A-LEGACY-001 | Baseline gap / Partial | [final-legacy-baseline.json](/var/folders/gb/vwfj36c909xgtbr0jl267d880000gp/T/egon-native-migration-p2k5o_1_/final-legacy-baseline.json)：12旧archive路径在 `dfd24ce3f` 与当前均不存在；`package reactor` exit1。本次未改SQL，不能宣称旧原路径归档已满足。 | 用户确认这些旧路径应恢复还是前驱Spec/校验应更新；随后以单独 corrective Plan处理。不要伪造SQL、修改校验来隐藏缺失。 |
| A-RUNTIME-001 | Runtime unverified | 未连接真实DDC/Tianquan-Shoubing/TLS或做跨进程native调用；源码/内存gRPC/模型测试不替代部署证明。 | 配置DDC与peer app code、HMAC、TLS和IdP SERVICE token后由用户联调。 |
| A-OPS-001 | Runtime unverified | 未验证实际注册失败/恢复、生产健康信号及日志脱敏。 | 由用户运行并验收健康/注册/恢复与日志。 |
| A-DOC-001 | Enablement prerequisite | 默认业务HTTP兼容；旧Controller未普遍补齐显式operationId。 | 若启用平台文档治理，先补齐业务operationId并验收权限；本次未扩张业务接口改造。 |
| A-PUBLISH-001 | Not performed | 本地5.4.0产物已装配和隔离解析，未确认相同新内容已远程发布；GPG签名跳过。 | 发布前确认版本/远程坐标与archive决策，再显式执行受控发布。 |

A-LEGACY-001 的实际错误为 `archetype-generation-test: legacy V archive is missing`；以下12条路径在任务基线和当前均不存在（非本次删除）：

```text
egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/db/migration/sharding/master-data/V20260726_001__init_light_master_data_schema.sql
egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/db/migration/sharding/master-data/V20260825_001__migrate_light_master_data_to_egon_model.sql
egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/db/migration/sharding/shard/V20260726_002__init_light_sharded_schema.sql
egon-cola-archetypes/egon-cola-archetype-light/src/main/resources/archetype-resources/src/main/resources/db/migration/sharding/shard/V20260825_002__migrate_light_sharded_to_tenant_model.sql
egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/migration/sharding/master-data/V20260726_001__init_evaluation_master_data_schema.sql
egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/migration/sharding/master-data/V20260825_001__migrate_evaluation_master_data_to_egon_model.sql
egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/migration/sharding/shard/V20260726_002__init_evaluation_sharded_schema.sql
egon-cola-archetypes/egon-cola-archetype-service/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/migration/sharding/shard/V20260825_002__migrate_evaluation_sharded_to_tenant_model.sql
egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/migration/sharding/master-data/V20260726_001__init_organization_master_data_schema.sql
egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/migration/sharding/master-data/V20260825_003__migrate_organization_master_data_to_egon_model.sql
egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/migration/sharding/shard/V20260726_002__init_organization_sharded_schema.sql
egon-cola-archetypes/egon-cola-archetype-web/src/main/resources/archetype-resources/__rootArtifactId__-infrastructure/src/main/resources/db/migration/sharding/shard/V20260825_004__migrate_organization_sharded_to_tenant_model.sql
```

测试包含隔离 H2 SQL、in-process gRPC，以及既有 Open 端口夹具；这些测试会创建并关闭自己的临时资源。本任务没有启动用户的业务项目、浏览器、Docker/Compose、Tianshu、Tianquan-Shoubing 或数据库服务，也没有连接用户真实数据库执行迁移。未执行 push、PR、merge、deploy 或 Central publish。

## 7. Final Manual Check matrix

| Check ID | Applicability | Status | 独立最终证据 | Finding | Action |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | 7个源/生成拓扑、真实架构报告、850generated tests | 保留选定COLA profile；精确generated mapper分类有证据 | None；运行边界见§6 |
| MC-REUSE-001 | Applicable | PASS | 5BaseConverter；RPC contract/strategy/proxy factories、Tianshu/HTTP/OpenAPI starters | 没有新通用框架或重复业务层 | None；运行边界见§6 |
| MC-DEP-001 | Applicable | PASS | 7isolated effective POM、源/runtime guards、实际BOOT-INF/lib | native/Open/Agent版本与需要分离 | None；运行边界见§6 |
| MC-NAME-001 | Applicable | PASS | 70 authored main Java路径、12record，31RpcService方法 | POJO/角色命名与边界一致 | None；运行边界见§6 |
| MC-VALID-001 | Applicable | PASS | generated provider/client/config suites；Default/native groups、正ID、响应DTO校验 | 受影响handoff有Jakarta/ValidationUtils与负例证据 | None；运行边界见§6 |
| MC-MODEL-001 | Applicable | PASS | 12records及原DTO/PO；Protoc派生类型 | 无新增缺完整Lombok的复杂手写carrier | None；运行边界见§6 |
| MC-CONVERT-001 | Applicable | PASS | 5MapStruct/BaseConverter、47模型映射清单、null/集合/时间/分页测试 | 无手写DTO复制或JSON转换绕过 | None；运行边界见§6 |
| MC-LOG-001 | Applicable | PASS | 新provider/client/interceptor Slf4j；静态未输出真实凭据 | 静态检查通过；运行日志验收A-OPS-001 | None；运行边界见§6 |
| MC-BEAN-001 | Applicable | PASS | named bean、final qualified RequiredArgsConstructor、Lombok复制Qualifier | source/generated context wiring通过 | None；运行边界见§6 |
| MC-UTIL-001 | Applicable | PASS | Java imports、POM及Python/Groovy/Bash工具清单 | 工具allowlist与已有框架能力复用 | None；运行边界见§6 |
| MC-JSON-001 | Applicable | PASS | 未更改公共JSON；HTTP/OAS/GraphQL回归；native为Protobuf | Jackson外部业务兼容 | None；运行边界见§6 |
| MC-TIME-001 | Applicable | PASS | 新增/修改Java无Date/Calendar/SimpleDateFormat；java.time往返与纳秒 | local/UTC/null语义保留 | None；运行边界见§6 |
| MC-CONFIG-001 | Applicable | PASS | 新增native core prefix在各profile一致；18Compose explicit identity及无marker | 原业务profile差异保留；运行配置仍需用户给值 | None；运行边界见§6 |
| MC-PATTERN-001 | Applicable | PASS | Provider/domain-client Adapter；既有Strategy/ProxyFactory；原子生成pipeline | 复杂度由已批准参与者承担，无多余模式层 | None；运行边界见§6 |
| MC-SCOPE-001 | Applicable | PASS | 236交付路径逐commit清单；无SQLcommit；82/133/1173哈希及原5staged保留 | 并发2提交排除；无业务/前端扩张 | None；运行边界见§6 |
| MC-TEST-001 | Applicable | BLOCKED | 必需Plan命令均通过；额外package reactor gate在baseline/current均exit1 | 旧Spec archive证明未闭合，见A-LEGACY-001 | A-LEGACY-001 / 用户决策 |
| MC-BLOCKER-001 | Applicable | BLOCKED | A-LEGACY-001及§15运行时未验证逐项列出 | 实现已交付；不能给无保留最终PASS | A-LEGACY-001 / 用户决策 |

## 8. Final Literal Rule matrix

| Rule | Applicability | Status | Final diff/path evidence | Validation evidence | Finding / action |
| --- | --- | --- | --- | --- | --- |
| Rule 1 | Applicable | PASS | 70 authored main Java路径、12record，31RpcService方法 | Source/generated tests + final-audit.log | 逐条按规范核对；无放宽规则。运行/旧归档缺口另列。 |
| Rule 2 | Applicable | PASS | generated provider/client/config suites；Default/native groups、正ID、响应DTO校验 | Source/generated tests + final-audit.log | 逐条按规范核对；无放宽规则。运行/旧归档缺口另列。 |
| Rule 3 | Applicable | PASS | 12records及原DTO/PO；Protoc派生类型；5MapStruct/BaseConverter、47模型映射清单、null/集合/时间/分页测试 | Source/generated tests + final-audit.log | 逐条按规范核对；无放宽规则。运行/旧归档缺口另列。 |
| Rule 4 | Applicable | PASS | 新provider/client/interceptor Slf4j；静态未输出真实凭据；named bean、final qualified RequiredArgsConstructor、Lombok复制Qualifier | Source/generated tests + final-audit.log | 逐条按规范核对；无放宽规则。运行/旧归档缺口另列。 |
| Rule 5 | Applicable | PASS | Java imports、POM及Python/Groovy/Bash工具清单；7isolated effective POM、源/runtime guards、实际BOOT-INF/lib | Source/generated tests + final-audit.log | 逐条按规范核对；无放宽规则。运行/旧归档缺口另列。 |
| Rule 6 | Applicable | PASS | 未更改公共JSON；HTTP/OAS/GraphQL回归；native为Protobuf | Source/generated tests + final-audit.log | 逐条按规范核对；无放宽规则。运行/旧归档缺口另列。 |
| Rule 7 | Applicable | PASS | 新增native core prefix在各profile一致；18Compose explicit identity及无marker | Source/generated tests + final-audit.log | 逐条按规范核对；无放宽规则。运行/旧归档缺口另列。 |
| Rule 9 | Applicable | PASS | Provider/domain-client Adapter；既有Strategy/ProxyFactory；原子生成pipeline | Source/generated tests + final-audit.log | 逐条按规范核对；无放宽规则。运行/旧归档缺口另列。 |
| Rule 10 | Applicable | PASS | 新增/修改Java无Date/Calendar/SimpleDateFormat；java.time往返与纳秒 | Source/generated tests + final-audit.log | 逐条按规范核对；无放宽规则。运行/旧归档缺口另列。 |
| Rule 11 | Applicable | PASS | 7个源/生成拓扑、真实架构报告、850generated tests | Source/generated tests + final-audit.log | 逐条按规范核对；无放宽规则。运行/旧归档缺口另列。 |

Rule 8 不存在，没有重排或补造规则。Literal Rules 的 PASS 仅证明当前交付代码/配置符合其检查范围；不能抵消最终 Manual TEST/BLOCKER 的归档缺口，也不代表真实部署验收通过。

## 9. Worktree and delivery closure

所有七步提交均为非空、按路径限定的语义提交；修正归属原 Step，未重写历史。`.generated` 仅由 canonical generator 和 Maven 生成，未跟踪或手工编辑。

| 并发提交（非本任务交付） | 说明 |
| --- | --- |
| 04ed6764029fe794ba2a00cd604f66bd71d32b50 | Concurrent external task; excluded from this delivery |
| 99313fb8915965b9abb26aafbafe4e07dd78f14a | Concurrent external task; excluded from this delivery |

审计时仅保留用户原有五个 Agent 暂存文件：`.dockerignore`、`.gitattributes`、`.gitignore`、`mvnw`、`mvnw.cmd`；其 staged binary patch 与开工快照一致。其他任务的已提交前端/RBAC工作未被纳入本次交付提交清单。

完成核对：parent/BOM、31 native RPC、Light/Service/Web迁移、Open/Agent边界、生成器、七产品IT与发布附件均完成。旧归档基线缺口和真实运行/发布验收保持显式未完成状态；本报告不批准发布。
