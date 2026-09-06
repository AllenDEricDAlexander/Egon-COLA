# Archetype 生成 Reactor、Spring 治理与 Flyway Baseline 实施计划

| Field              | Value                                                                                                                                                |
|--------------------|------------------------------------------------------------------------------------------------------------------------------------------------------|
| Document           | 2026-09-03-13-38-archetype-generated-reactor-implementation.md                                                                                       |
| Template Version   | 4                                                                                                                                                    |
| Status             | Review                                                                                                                                               |
| Created            | 2026-09-03 13:38 CST                                                                                                                                 |
| Updated            | 2026-09-03 13:38 CST                                                                                                                                 |
| Owner              | Egon-COLA maintainer                                                                                                                                 |
| Repository         | Egon-COLA                                                                                                                                            |
| Scope              | 根及一级 Maven Parent、六个正常 source projects、Light/Service/Web Flyway Baseline、Archetype definitions 与完整 generated reactor、CI 和 Maven Central 发布入口         |
| Source Requirement | 确认实施新的两阶段 Archetype 结构，删除旧 Maven 包装模块，统一 Spring Boot Parent/BOM，并按 schema 角色收敛 Flyway Baseline                                                       |
| Baseline Revision  | main at 9566c96796df73aa28e4a20de63c12379dcfdfeb; preserve modified egon-cola-platforms/egon-cola-platform-admin-web-shared/tsconfig.app.tsbuildinfo |
| Implements Spec    | [Archetype 两阶段生成、Spring 依赖治理与 Flyway 收敛设计](../spec/2026-09-03-11-04-archetype-generated-reactor-spring-flyway-design.md)                             |
| Spec Status        | Accepted                                                                                                                                             |
| Spec Revision      | Updated 2026-09-03 13:38 CST at the current uncommitted decision-sync revision over main 9566c96796df73aa28e4a20de63c12379dcfdfeb                    |
| Effective Specs    | [Archetype 两阶段生成、Spring 依赖治理与 Flyway 收敛设计](../spec/2026-09-03-11-04-archetype-generated-reactor-spring-flyway-design.md)                             |
| Depends On Plans   | [已实施的两阶段源码生成计划](2026-09-01-11-23-archetype-two-stage-source-generation-implementation.md)                                                            |
| Supersedes         | None                                                                                                                                                 |
| Superseded By      | None                                                                                                                                                 |
| Related Plans      | None                                                                                                                                                 |

## 1. Summary

本 Plan 实施唯一有效 Spec，共七个顺序 Step。当前仓库已经具备六个正常 source projects、固定
maven-archetype-plugin 3.4.1 的生成器、原子 staging/check 和发布前置脚本；本 Plan 不重复这些已完成工作，
而是在其上完成四个剩余结果：统一 Spring 依赖管理、为三个 legacy 脚手架增加每角色一个 Flyway B baseline、
把旧包装模块的非业务合同迁入 definitions 并生成完整 Maven Reactor、切换 CI/Central 到新 Reactor 后移除旧
Maven module。

完成证据由 Spring effective-POM 静态门禁、三组 B-only 对 V-only schema parity、生成器 fixture、
六 Archetype integration-test、生成消费者 verify、发布附件 allowlist 和根 Reactor dry-run 组成。每个 Step
单独验证并只提交自己的路径；不启动服务、浏览器、Docker、真实数据库或真实 Maven Central 发布。

## 2. Target Spec and Effective Design

### 2.1 Primary target

-
Path: [Archetype 两阶段生成、Spring 依赖治理与 Flyway 收敛设计](../spec/2026-09-03-11-04-archetype-generated-reactor-spring-flyway-design.md)
- Status: Accepted
- Revision: 2026-09-03 13:38 CST；repository baseline main 9566c96796df73aa28e4a20de63c12379dcfdfeb。
- Approval evidence: 用户在 2026-09-03 回复“确认”并显式调用本 Plan skill；该确认承接上一轮列出的三项选择，即每个 schema
  角色一个 baseline、旧包装历史 SQL 原路径只读保留、Open 手工 SQL 不纳入 Flyway。

### 2.2 Effective Spec set

| Role    | Spec/link                                                                            | Status/revision                        | Effective sections | Why included                                      |
|---------|--------------------------------------------------------------------------------------|----------------------------------------|--------------------|---------------------------------------------------|
| Primary | [当前设计](../spec/2026-09-03-11-04-archetype-generated-reactor-spring-flyway-design.md) | Accepted, Updated 2026-09-03 13:38 CST | 全文 §1 至 §20        | 唯一规范目标，包含最新 Spring、generated Reactor、Flyway 和发布决策 |

### 2.3 Superseded or excluded content

2026-08-27 Spec及其 2026-09-01 Plan仅解释当前已存在的 source-projects、generator和旧模块外接
.generated resource 的由来，不属于本 Plan 的规范需求集。当前 Spec §5 的 DEC-001 至 DEC-008覆盖本次范围；
旧 Plan 中“保留六个旧 Maven packaging module”的过渡终态不再作为目标。

## 3. Effective Requirements and Acceptance

| Requirement | Source Spec section                                                                     | Effective statement                        | Observable acceptance                                 | Implementation impact         |
|-------------|-----------------------------------------------------------------------------------------|--------------------------------------------|-------------------------------------------------------|-------------------------------|
| REQ-001     | [Spec §4](../spec/2026-09-03-11-04-archetype-generated-reactor-spring-flyway-design.md) | 六个正常 Maven source project 是业务事实源           | 六个 root可导入和 verify，无 Archetype source token           | Steps 1, 5, 7                 |
| REQ-002     | Spec §4                                                                                 | 六个旧 Archetype Maven包装模块退出                  | parent modules无旧目录，旧目录无 pom和非历史内容                     | Step 6                        |
| REQ-003     | Spec §4                                                                                 | 非业务打包合同进入 definitions且不是 Maven module      | 六定义均含 manifest、packaging POM、metadata、IT、javadoc和架构文档 | Step 5                        |
| REQ-004     | Spec §4                                                                                 | 固定 plugin 3.4.1从源码生成                       | generator日志和fixture只出现3.4.1 create-from-project       | Step 5                        |
| REQ-005     | Spec §4                                                                                 | .generated包含 aggregator和六个完整 child         | 独立 Maven invocation可解析并 integration-test              | Steps 5, 6                    |
| REQ-006     | Spec §4                                                                                 | .generated忽略入库且不可手改                        | git ls-files为空；check识别漂移                              | Steps 5, 7                    |
| REQ-007     | Spec §4                                                                                 | inventory只从 definitions动态发现                | definitions/source/generated三者一一对应                    | Step 5                        |
| REQ-008     | Spec §4                                                                                 | 全套生成加锁、staging、原子替换和失败清理                   | 注入失败和并发fixture保持旧完整集合                                 | Step 5                        |
| REQ-009     | Spec §4                                                                                 | 六个公开 GAV和生成拓扑兼容                            | 六个 basic IT及消费者 verify通过                              | Steps 5, 6, 7                 |
| REQ-010     | Spec §4                                                                                 | source projects不发布Central                  | artifact allowlist不存在内部 source GAV                    | Steps 6, 7                    |
| REQ-011     | Spec §4                                                                                 | 根 Reactor仍形成一个 Central bundle              | 只有一次最终 root deploy且含六 public GAV                      | Step 7                        |
| REQ-012     | Spec §4                                                                                 | 根 Parent统一 Boot BOM 3.5.16和Boot plugin版本   | effective POM管理来源唯一                                   | Step 1                        |
| REQ-013     | Spec §4                                                                                 | 六 source roots保留 Boot Parent并删除重复 Boot BOM | parent仍为3.5.16，dependencyManagement无Boot BOM import   | Step 1                        |
| REQ-014     | Spec §4                                                                                 | 删除与Boot BOM相同的局部版本，保留差异例外                  | Flyway 11.15.0/PostgreSQL 42.7.8在根有账本且未静默改变           | Step 1                        |
| REQ-015     | Spec §4                                                                                 | 相关 source roots使用 Springdoc BOM 2.8.17     | Light/Web及Open对应POM无dependency内Springdoc版本            | Step 1                        |
| REQ-016     | Spec §4                                                                                 | Flyway必须保持 master-data与shard隔离             | 每role一个B baseline且B/V schema parity                   | Steps 2, 3, 4                 |
| REQ-017     | Spec §4                                                                                 | 二十四个现有 V migration路径和字节不变                  | baseline SHA-256 inventory逐项通过                        | Steps 2, 3, 4, 6              |
| REQ-018     | Spec §4                                                                                 | Flyway收敛方案必须先获确认                           | Plan记录DEC-005/006确认并只实施B baseline                     | Steps 2, 3, 4                 |
| REQ-019     | Spec §4                                                                                 | Open手工SQL不纳入Flyway                         | Open中无B/V Flyway新增，manual SQL hash不变                  | Steps 5, 6, 7                 |
| REQ-020     | Spec §4                                                                                 | 任一发布前Gate失败不得deploy                        | release test证明错误路径在deploy前退出                          | Every Step, especially Step 7 |

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

1. 先收敛 Maven Parent/BOM，因为 definitions 中的 packaging POM和 generated effective POM必须继承稳定版本来源。
2. 分 Light、Service、Web 三个独立 Step增加 B baseline。每个 Step先改约定/parity测试形成RED，再增加两份最终态SQL使其GREEN；三个数据库角色集互不写同一路径。
3. definitions和完整 .generated Reactor必须在旧模块仍可作为逐文件基线时建立，先复制非业务合同，再改generator，避免删除后失去metadata/IT权威来源。
4. generated Reactor可完成六个IT后，再切换 archetypes parent profile并删除旧 module的POM、业务模板和非历史内容；二十四个旧V路径不操作。
5. 最后改变deploy wrapper、workflow和文档，使所有入口只消费 generated profile，并在一次root deploy前执行全部Gate。

### 4.2 Test-first strategy

| Behavior             | RED point before implementation                                                        | Minimum GREEN                                   | Refactor/wiring allowed       |
|----------------------|----------------------------------------------------------------------------------------|-------------------------------------------------|-------------------------------|
| Spring管理唯一来源         | 新shell contract对当前三次Boot BOM和六个重复source BOM失败                                          | root BOM、child继承、source保留Parent                 | 删除精确匹配局部版本并加effective POM检查   |
| Light baseline       | convention/parity新增B文件与B-only/V-only断言后缺文件失败                                           | 两个Light B SQL声明最终schema                         | 只整理测试helper，不改V/YAML/migrator |
| Service baseline     | 同上，缺Evaluation两个B SQL失败                                                                | master 1表、shard 8表最终DDL                         | 只复用现有H2 fixture               |
| Web baseline         | 同上，缺Organization两个B SQL失败                                                              | master 6表和种子、shard 4表最终DDL                      | 保持FK/unique/index parity      |
| 完整 generated Reactor | generator fixture先要求definitions、child POM、META-INF、IT和aggregator，当前只得resource fragment | definitions加compose逻辑                           | 保留现有lock/staging/hash函数       |
| 旧module退出            | package test先断言default modules仅facade且generated profile含aggregator，当前失败                | parent profile切换并删除非历史旧内容                       | 只读V archive断言                 |
| Release切换            | 新release fixture对旧路径和无profile命令失败                                                      | wrapper/workflow使用definitions和generated profile | 文档与README同步                   |

### 4.3 Sequential and parallel boundaries

| Step   | Depends on | May run in parallel with                      | Must not overlap with                  | Reason                    |
|--------|------------|-----------------------------------------------|----------------------------------------|---------------------------|
| Step 1 | None       | None                                          | root及一级Parent、六source root POM         | 统一版本来源是后续 generated POM基线 |
| Step 2 | Step 1     | Step 3, Step 4仅在不同worktree/agent且不共享测试helper时 | Light migration/tests                  | 独立schema族，但本执行模式仍逐Step提交  |
| Step 3 | Step 1     | Step 2, Step 4同上                              | Service infrastructure migration/tests | 独立schema族                 |
| Step 4 | Step 1     | Step 2, Step 3同上                              | Web infrastructure migration/tests     | 独立schema族                 |
| Step 5 | Steps 2-4  | None                                          | definitions、generator及generator tests  | 生成结果必须包含已完成baseline       |
| Step 6 | Step 5     | None                                          | archetypes parent与六旧目录                 | 删除前必须已有可验证替代              |
| Step 7 | Step 6     | None                                          | release scripts/workflows/docs         | 入口只可指向最终结构                |

### 4.4 Commit boundaries

七个 Step各自产生一个语义提交。Step 2至4按项目分开，便于单独验证/回滚SQL；Step 5只新增definitions并扩展
generator，不删除旧模块；Step 6只做Reactor cutover和旧内容退出；Step 7只修改入口和文档。执行时使用
git add -- 加入每Step明确路径，绝不广泛stage，也不提交现有tsconfig.app.tsbuildinfo或本Plan之外的文档变化。

### 4.5 Spec Simplicity and Implementation-necessity Audit

| Spec element              | Spec necessity verdict/section | Current repository evidence  | Direct/reuse alternative              | Interaction/implementation cost   | Plan decision                        |
|---------------------------|--------------------------------|------------------------------|---------------------------------------|-----------------------------------|--------------------------------------|
| root Boot BOM             | Spec §7.0 Add                  | 三个一级parent重复import           | 维持重复管理                                | 1个root import、3个child删除           | Implement Step 1                     |
| application Boot Parent   | Spec DEC-004 Keep              | 六source root已直接继承3.5.16      | 改继承Egon parent会失去Boot parent defaults | 不增加调用，仅删除重复BOM                    | Keep Step 1                          |
| Flyway B baseline         | Spec §11 Selected              | 每role两份V且Flyway 11支持B prefix | 改删V或自研JavaMigration                   | 6个SQL、3组parity测试，无production Java | Implement Steps 2-4                  |
| definitions               | Spec §7.0 Add                  | 当前合同仍在六旧Maven module         | 继续保留旧module                           | 约42个小合同文件，消除业务双源                  | Implement Step 5                     |
| full generated aggregator | Spec §7.0 Add                  | 当前.generated只有resources+hash | 逐模块手工Maven调用                          | 1个派生aggregator和6 child POM        | Implement Step 5                     |
| check wrapper             | Spec CLI-002                   | generator已有check subcommand  | 新写第二套check实现                          | 1行exec wrapper避免重复逻辑              | Implement Step 5                     |
| release wrapper           | Spec CLI-003                   | scripts/maven-deploy.sh已存在   | 新增publish_maven_central.sh            | 重复入口无独立价值                         | Reuse current path via PLAN-CLAR-001 |
| custom migration engine   | Spec §13 Rejected              | B baseline原生实现新旧环境分流         | JavaMigration/预处理器                    | 新Bean、方言、失败面                      | Do not implement                     |
| new API/model/cache/page  | Spec §§9-12 N/A/Unchanged      | 无产品交互变化                      | 复用现有合同                                | 零新调用和状态                           | Do not implement                     |

不存在fetch-then-forward API、caller-supplied安全上下文、新cache/job/page、重复model/mapper或第三种架构。
Pipeline通过现有shell函数实现，不创建仪式性Java Strategy/Factory。

### 4.6 Change-unit Dependency Matrix

| Change unit                 | Requirements                                        | Proof/RED point                  | Compile/runtime prerequisites | Produces                                 | Consumers/unblocks         | Owning Step |
|-----------------------------|-----------------------------------------------------|----------------------------------|-------------------------------|------------------------------------------|----------------------------|-------------|
| Maven版本治理                   | REQ-012至REQ-015                                     | spring dependency shell contract | root/child POM当前可解析           | 单一Boot管理源                                | generated POM和全量build      | Step 1      |
| Light B baseline            | REQ-016至REQ-018                                     | Light convention/parity RED      | Step 1 Flyway解析               | 2个B SQL                                  | generator与Light consumer   | Step 2      |
| Service B baseline          | REQ-016至REQ-018                                     | Service convention/parity RED    | Step 1                        | 2个B SQL                                  | generator与Service consumer | Step 3      |
| Web B baseline              | REQ-016至REQ-018                                     | Web convention/parity RED        | Step 1                        | 2个B SQL                                  | generator与Web consumer     | Step 4      |
| definitions/full generation | REQ-003至REQ-009                                     | generator fixture RED            | Steps 2-4                     | 完整.generated reactor                     | Maven profile cutover      | Step 5      |
| Reactor cutover             | REQ-002, REQ-005, REQ-009, REQ-010, REQ-017         | package/archive contract RED     | Step 5                        | default facade reactor+generated profile | release entry              | Step 6      |
| Release/CI contract         | REQ-001, REQ-006, REQ-009至REQ-011, REQ-019, REQ-020 | release fixture RED              | Step 6                        | one-bundle gated workflow                | Central operator           | Step 7      |

### 4.7 Java, Spring, and Egon-COLA Implementation Standards

| Concern                            | Current repository evidence                                     | Effective Spec decision                                         | Planned implementation consequence          | Owning Steps/checks                 |
|------------------------------------|-----------------------------------------------------------------|-----------------------------------------------------------------|---------------------------------------------|-------------------------------------|
| Architecture profile               | six exact Light/Service/Web/Open source trees and verify.groovy | Spec §6.1 preserves exact variants                              | 不新增/移动业务Java，generated verifier逐族保持依赖方向     | All Steps; MC-ARCH-001              |
| Reuse/capability                   | Boot Parent/BOM、existing generator、Flyway 11、existing H2 tests  | Spec DEC-002至DEC-007                                            | 复用Maven/Flyway能力，不写Java生成器或migrator         | Steps 1-7; MC-REUSE-001, MC-DEP-001 |
| Naming/model/validation/conversion | no new production Java types or handoffs                        | Spec §6.2 Rules 1-3/6 unchanged                                 | 测试类保留行为后缀；业务POJO/Validation/Converter不动     | Steps 2-4; N/A business checks      |
| Bean/logging/util/JSON/time/config | migrator/YAML保持字节与key结构；shell只用现有工具                             | Spec Rules 4-7/10                                               | 不改Bean、JSON/time；配置key parity静态验证           | Steps 1-7; MC-CONFIG-001            |
| Business variation/pattern         | complexity位于build pipeline                                      | Spec §13 selects Pipeline/staging, rejects Java pattern classes | 复用discover/compose/validate/atomic swap函数阶段 | Step 5; MC-PATTERN-001              |

#### Capability reuse ledger

| Need               | Candidates inspected                 | Exact evidence                              | Fit/gap                               | Decision             | Added dependency/custom code | Owning Step/check       |
|--------------------|--------------------------------------|---------------------------------------------|---------------------------------------|----------------------|------------------------------|-------------------------|
| Spring版本管理         | Boot Parent、spring-boot-dependencies | root及3个一级parent、6 source root POM           | 完全满足                                  | 根BOM+应用Parent        | None                         | Step 1; MC-REUSE-001    |
| Springdoc版本族       | springdoc-openapi-bom                | platform POM已有2.8.17 BOM                    | 完全满足                                  | 复用BOM到4 source roots | None                         | Step 1; MC-DEP-001      |
| Baseline migration | Flyway B migration                   | Flyway 11.15.0和官方baseline文档                 | 满足新环境累计起点并兼容既有V                       | 每role一个B文件           | None                         | Steps 2-4; MC-REUSE-001 |
| migration验证        | existing H2 PostgreSQL mode tests    | three migration test packages               | 可比较B fresh schema与复制V-only schema     | 扩展现有测试               | None                         | Steps 2-4; MC-TEST-001  |
| Archetype转换        | maven-archetype-plugin 3.4.1         | scripts/generate_archetypes.sh generate_one | 已满足内容转换，缺完整module compose             | 扩展shell composition  | None                         | Step 5; MC-REUSE-001    |
| drift check        | existing check mode                  | run_pipeline and compare_generated_set      | 已满足核心比较，缺独立CLI path                   | thin exec wrapper    | None                         | Step 5; MC-DEP-001      |
| 发布编排               | scripts/maven-deploy.sh              | current run_preflight and optional deploy   | 已满足opt-in/guard，路径需切generated profile | 原位修改                 | None                         | Step 7; MC-SCOPE-001    |

### 4.8 User-mandated Java Rule Implementation Matrix

| Literal rule | Spec source       | Repository evidence                        | Exact files and order                      | Pseudocode obligations                                        | Validation gate                    | Steps        | Status/blocker                     |
|--------------|-------------------|--------------------------------------------|--------------------------------------------|---------------------------------------------------------------|------------------------------------|--------------|------------------------------------|
| Rule 1       | Spec §6.2/§10     | 无新增production Java type；只改3族测试类            | convention/parity tests before B SQL       | 测试method命名表达baseline/parity行为，不创建Data/Info/Param/Bean carrier | compile+type inventory             | Steps 2-4    | N/A for production; PASS for tests |
| Rule 2       | Spec §6.2/§9/§10  | 业务layer handoff不变                          | no production boundary files               | 不增删Valid/Validated/group/ValidationUtils                      | generated consumer tests           | Steps 5-7    | N/A                                |
| Rule 3       | Spec §6.2/§10     | 无model/converter改动                         | no production model files                  | 不创建record/class/converter，不绕过BaseConverter                    | git diff Java inventory            | Steps 2-4    | N/A                                |
| Rule 4       | Spec §6.2/§7/§13  | 无business Bean改动                           | no Bean/lombok.config files                | 不改Slf4j、Bean name、RequiredArgsConstructor、Qualifier           | git diff Java inventory            | All Steps    | N/A                                |
| Rule 5       | Spec §6.2/§15     | shell使用Bash、find、grep、sed、cmp、Maven        | test scripts before implementation scripts | 不引入未批准utility/dependency                                      | bash -n and dependency diff        | Steps 1, 5-7 | PASS                               |
| Rule 6       | Spec §6.2/§9/§10  | 外部JSON合同不变                                 | no DTO/API files                           | 不改Jackson annotation/stack                                    | consumer contract regression       | Steps 5-7    | N/A                                |
| Rule 7       | Spec §6.2/§15     | master/shard YAML locations和profile key已存在 | tests, B SQL; YAML files remain untouched  | B/V共处原location，所有环境key集合不变                                    | profile key and hash gates         | Steps 2-4, 7 | PASS                               |
| Rule 9       | Spec §6.2/§13     | generator已有pipeline函数与atomic staging       | generator tests then script                | discover/convert/compose/validate/swap，不以硬编码六分支实现             | injected failure/concurrency tests | Step 5       | PASS                               |
| Rule 10      | Spec §6.2/§10/§15 | 无Java时间字段改动                                | no model/time files                        | 测试不新增Date/Calendar/SimpleDateFormat                           | forbidden import search            | Steps 2-4    | N/A                                |
| Rule 11      | Spec §6.1/§6.2/§8 | exact six Archetype profiles/verifiers     | every Step file tree                       | 仅改变build/resource来源，业务树不混入biz结构或第三层次                          | six architecture verifiers         | Every Step   | PASS                               |

## 5. Change File Tree

```text
pom.xml                                                        MODIFY
scripts
├── test-spring-dependency-management.sh                       CREATE
├── generate_archetypes.sh                                     MODIFY
├── check_archetypes.sh                                        CREATE
├── test-generate-archetypes.sh                                MODIFY in Steps 5 and 6 by distinct functions
├── test-archetype-release.sh                                  CREATE
├── maven-deploy.sh                                            MODIFY
└── maven-deploy.md                                            MODIFY
egon-cola-components/pom.xml                                   MODIFY
egon-cola-platforms/pom.xml                                    MODIFY
egon-cola-archetypes
├── pom.xml                                                    MODIFY in Steps 1 and 6 by distinct sections
├── source-projects
│   ├── six source root pom.xml                                MODIFY
│   ├── egon-cola-source-light
│   │   ├── two migration tests                               MODIFY
│   │   └── two B baseline SQL                                CREATE
│   ├── egon-cola-source-service
│   │   └── infrastructure: two tests plus two B SQL          MODIFY/CREATE
│   └── egon-cola-source-web
│       └── infrastructure: three tests plus two B SQL        MODIFY/CREATE
├── definitions
│   └── six public artifact definitions                       CREATE
├── .generated
│   ├── pom.xml                                                GENERATED
│   └── six complete maven-archetype children                 GENERATED
└── six old egon-cola-archetype directories
    ├── immutable twelve V SQL archive paths                   KEEP, no file operation
    └── every other tracked file                              DELETE
.github/workflows
├── ci.yaml                                                    MODIFY
├── ci_java_compatibility.yaml                                 MODIFY
└── publish-maven-central.yml                                  MODIFY
README.md                                                      MODIFY
README.zh-CN.md                                                MODIFY
```

| Operation | Path                                                  | Current evidence/symbol              | Final symbols/state                                  | Responsibility                    | Step | Requirements                      | Validation owner     |
|-----------|-------------------------------------------------------|--------------------------------------|------------------------------------------------------|-----------------------------------|------|-----------------------------------|----------------------|
| CREATE    | scripts/test-spring-dependency-management.sh          | absent                               | repository-wide POM policy assertions                | Spring RED/GREEN gate             | 1    | REQ-012至REQ-015                   | shell+effective POM  |
| MODIFY    | pom.xml                                               | no Boot BOM                          | root Boot properties/BOM/plugin versions             | shared Maven management           | 1    | REQ-012, REQ-014                  | effective POM        |
| MODIFY    | egon-cola-{components,platforms,archetypes}/pom.xml   | each imports Boot BOM                | inherit root management; retain domain-specific BOMs | child parent cleanup              | 1    | REQ-012, REQ-014                  | policy test          |
| MODIFY    | six source root pom.xml                               | Boot Parent plus duplicate Boot BOM  | Parent only; Springdoc BOM where used                | application dependency management | 1    | REQ-013, REQ-015                  | policy/source verify |
| MODIFY    | three Flyway convention/parity test groups            | expect only four V files/fresh chain | assert one B per role and B/V schema parity          | migration RED/GREEN               | 2-4  | REQ-016至REQ-018                   | targeted JUnit       |
| CREATE    | six B baseline SQL paths                              | absent                               | cumulative final schema per role                     | fresh-environment baseline        | 2-4  | REQ-016至REQ-018                   | H2 parity            |
| CREATE    | scripts/check_archetypes.sh                           | absent; check is subcommand          | thin exec wrapper                                    | CLI-002                           | 5    | REQ-006, REQ-020                  | generator unit       |
| MODIFY    | scripts/test-generate-archetypes.sh                   | tests resource fragments/old modules | tests definitions and full reactor                   | generation contract               | 5, 6 | REQ-003至REQ-009                   | shell fixture        |
| CREATE    | egon-cola-archetypes/definitions/six-definition-trees | absent; assets in old modules        | non-Maven packaging contracts                        | packaging truth                   | 5    | REQ-003, REQ-007                  | definition parity    |
| MODIFY    | scripts/generate_archetypes.sh                        | outputs resources+hash               | composes aggregator and complete child modules       | CLI-001                           | 5    | REQ-004至REQ-008                   | generator tests      |
| GENERATED | egon-cola-archetypes/.generated/complete-reactor      | ignored resource fragments           | ignored aggregator+six children                      | publishable derived output        | 5    | REQ-005, REQ-006                  | Maven IT             |
| MODIFY    | egon-cola-archetypes/pom.xml                          | default six old modules              | default facades; generated-archetypes profile        | Reactor cutover                   | 6    | REQ-002, REQ-005, REQ-010         | Maven validate       |
| DELETE    | six-old-archetype-module-non-flyway-path-set          | old POM/business template/contracts  | absent; immutable SQL exceptions remain              | remove duplicate modules          | 6    | REQ-002, REQ-017                  | archive/static gate  |
| CREATE    | scripts/test-archetype-release.sh                     | absent                               | wrapper/workflow/artifact path contract              | release RED/GREEN                 | 7    | REQ-009至REQ-011, REQ-020          | shell fixture        |
| MODIFY    | scripts/maven-deploy.sh                               | builds old default modules           | explicit generated profile and allowlist             | CLI-003                           | 7    | REQ-010, REQ-011, REQ-020         | dry-run              |
| MODIFY    | three workflow files                                  | discover old manifests/POM paths     | definitions inventory and generated target paths     | CI/Central gates                  | 7    | REQ-006, REQ-009至REQ-011, REQ-020 | static workflow test |
| MODIFY    | scripts/maven-deploy.md, README.md, README.zh-CN.md   | show old module tree/commands        | source/definitions/generated lifecycle               | operator/consumer docs            | 7    | REQ-001, REQ-009, REQ-019         | doc search           |

## 6. Prerequisites, Constraints, and Plan Clarifications

### 6.1 Repository and worktree baseline

- Branch/commit: main at 9566c96796df73aa28e4a20de63c12379dcfdfeb。
- 现有非本任务改动：egon-cola-platforms/egon-cola-platform-admin-web-shared/tsconfig.app.tsbuildinfo；所有Step禁止stage、修改或回退它。
- 当前 source-projects、generator、tests、release wrapper是已提交基础，不将其错误标记为待创建。
- .generated已被.gitignore覆盖，任何Step都不得git add -f。
- 每Step提交前执行git diff --check并使用精确Commit paths；发现新的并发改动则暂停重核重叠文件。
- 仓库未发现额外AGENTS.md文件；采用当前任务提供的主代理规则，尤其是不可修改既有Flyway migration和每Step单独提交。

### 6.2 Build, test, and environment prerequisites

| Concern        | Exact command/source                                                                | Required state                                      | Validation boundary                       |
|----------------|-------------------------------------------------------------------------------------|-----------------------------------------------------|-------------------------------------------|
| Java/Maven     | ./mvnw -version                                                                     | Java 21，repository Maven Wrapper可执行                 | local build only                          |
| Skill          | python3 .agents/skills/egon-coding-writing-plan/scripts/validate_skill_resources.py | PASS                                                | documentation preflight                   |
| Source reactor | ./mvnw -B -ntp -f egon-cola-archetypes/source-projects/pom.xml clean install        | all source modules green或准确隔离既有AccessGuard baseline | no services                               |
| Generator      | scripts/generate_archetypes.sh generate/check                                       | fixed plugin 3.4.1 and ignored output               | local filesystem/Maven                    |
| Flyway test DB | existing H2 PostgreSQL mode fixtures                                                | in-memory unique database names                     | not real PostgreSQL                       |
| Release shape  | scripts/maven-deploy.sh --dry-run                                                   | no deploy, GPG signature validity not claimed       | no Central write                          |
| Real release   | protected workflow with secrets                                                     | user separately initiates                           | explicitly outside this Plan-writing turn |

### 6.3 Immutable constraints and approved decisions

- 二十四个既有 V migration全部保持原路径、原字节、原文件名；本 Plan只新增六个B migration。
- 每个legacy project有master-data与shard两个独立role，因此每项目两个B文件；不合并物理数据源。
- B版本等于对应role当前最高V版本，官方语义允许同版本B/V共存；新环境选B，已有环境忽略B。
- 旧包装目录中的十二个V文件只读保留但所在目录不再有pom，不进入Maven Reactor。
- Open手工SQL完全不改，不引入Flyway/Liquibase。
- Flyway 11.15.0与PostgreSQL 42.7.8作为根级显式兼容例外保留；本Plan不进行依赖版本升级。
- 六public GAV、metadata、post-generate、basic IT、module topology和consumer命令不变。
- 实现/验证不启动服务、浏览器、Docker、外部数据库、中间件或真实Central。

### 6.4 Plan Clarifications

| ID            | Small implementation inference                                 | Repository evidence                                                 | Why semantics are unchanged | Impact if wrong                 |
|---------------|----------------------------------------------------------------|---------------------------------------------------------------------|-----------------------------|---------------------------------|
| PLAN-CLAR-001 | CLI-003继续使用scripts/maven-deploy.sh，不新增publish_maven_central.sh | 当前脚本已实现dry-run、opt-in publish和release guard；Spec §9称release wrapper | 只保留现有入口路径，不改变CLI语义          | 若团队要求新文件名会产生兼容wrapper           |
| PLAN-CLAR-002 | definitions中的公开child POM命名packaging-pom.xml并用rootVersion token | 旧child POM是打包合同，bump脚本已只更新source egon版本                             | generator展开token可避免第二版本事实源  | token未替换会被generated POM gate捕获  |
| PLAN-CLAR-003 | B/V parity测试通过复制旧V到临时filesystem location建立V-only链              | 现有测试使用H2且旧V不可改；Flyway fresh location会自动选B                           | 仅测试隔离方式，不改变runtime配置        | H2方言不足时追加用户授权的PostgreSQL验证而不改设计 |

## 7. Ordered File-by-file Implementation Steps

每个Step都必须先完成对应RED证据，再完成GREEN和Step级验证，最后仅提交声明路径。

### Step 1 — 统一 Spring Boot Parent、BOM 与例外版本来源

- Requirements: REQ-001, REQ-012, REQ-013, REQ-014, REQ-015, REQ-020
- Dependencies: None
- Baseline state: root无Boot BOM；components、platforms、archetypes各自import同一Boot BOM；六source roots既继承Boot
  Parent又重复import；source-web Springdoc仍为2.8.13。
- Observable outcome: 非应用模块从root继承Boot BOM，六source roots只使用Boot Parent，四个HTTP source roots统一Springdoc
  BOM 2.8.17，差异版本有单一根级账本。
- End state: repository-wide policy script为GREEN；未改任何Java、配置key或dependency artifact集合。
- Test-first gate: Required — 新policy script首先对当前重复Boot BOM、source重复import、source-web 2.8.13和多处例外属性返回非零。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-UTIL-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 5, Rule 7, Rule 11
- Ordered files:

#### File 1 — `CREATE scripts/test-spring-dependency-management.sh`

- Purpose: 以仓库级静态合同证明全部Spring相关POM都有且只有一个被批准的版本来源。
- Symbols: assertions
  root_boot_bom_once、child_parents_do_not_reimport、source_roots_keep_boot_parent_only、springdoc_bom_2_8_17、documented_flyway_postgresql_exceptions、isolated_it_allowlist。
- Repository evidence: 当前无同类脚本；scripts/test-generate-archetypes.sh和test-bump-cola-version.sh使用set -Eeuo
  pipefail、fixture、fail/assert helper风格。
- Dependencies and consumers: 读取root、components、platforms、archetypes、六source root和两个bytecode src/it POM；由Step
  1及最终CI调用。
- Why now: 先建立可重复RED合同，防止凭肉眼删除版本导致隐式降级。
- Contract/signature changes: 新CLI无参数，成功exit 0，违规exit 1并打印module、artifact和违反的policy。
- Input/output and state mapping: tracked POM集合映射为parent/BOM/version-owner inventory；不写POM、target或local
  repository。
- Error and edge behavior: 缺POM、重复import、未知独立POM、Springdoc dependency自带version、例外版本重复声明都fail；两个src/it
  fixture必须显式allowlist且固定3.5.16。
- Standards impact:
  MC-REUSE-001、MC-DEP-001、MC-UTIL-001、MC-CONFIG-001、MC-SCOPE-001、MC-TEST-001；只用Bash、find、grep、sed和Maven现有能力。
- Literal rule enforcement: Rule 5 仅使用批准的原生工具；Rule 7 不改profile key但检查POM版本结构一致；Rule 11
  扫描不改变六种Archetype包结构。
- Implementation pseudocode:

```bash
collect every tracked pom.xml containing Spring, Spring Boot, Springdoc, Flyway or PostgreSQL coordinates
assert root has spring.boot.version=3.5.16 and exactly one spring-boot-dependencies import
assert components/platforms/archetypes inherit root and contain no local Boot BOM import
for each six source root assert parent is spring-boot-starter-parent:3.5.16 and Boot BOM import count is zero
for each Light/Web HTTP root assert springdoc-openapi-bom:2.8.17 exists and Springdoc dependencies have no version
assert flyway.version=11.15.0 and postgresql.version=42.7.8 are declared only by the approved root exception ledger
allow only the two bytecode src/it fixture POMs to remain self-contained; print PASS only when every assertion holds
```

- Verification contribution: RED/GREEN entry for TEST-003和TEST-004，覆盖全POM inventory而非抽样模块。
- After this file: 脚本可执行但在当前POM布局上按预期RED，未改变构建行为。

#### File 2 — `MODIFY pom.xml,egon-cola-components/pom.xml,egon-cola-platforms/pom.xml,egon-cola-archetypes/pom.xml`

- Purpose: 建立非应用模块的根级Boot BOM、plugin版本和Flyway/PostgreSQL例外来源。
- Symbols: root properties spring.boot.version、springdoc.version、flyway.version、postgresql.version、lombok.version；root
  dependencyManagement spring-boot-dependencies；三个child Parent properties/dependencyManagement cleanup。
- Repository evidence: root当前只管理release
  plugins；三个child都继承root且重复spring.boot.version/BOM；components/platforms还重复与Boot
  3.5.16相同的Micrometer/JUnit/Lombok版本。
- Dependencies and consumers: 所有components/platforms/archetypes children通过Maven parent继承；source roots不继承root，File
  3单独处理。
- Why now: policy test已固定唯一来源，先修共享父级再处理独立应用根。
- Contract/signature changes: artifact GAV/module list不变；effective dependency versions除明确source-web
  Springdoc外保持；Flyway/PostgreSQL覆盖值不变。
- Input/output and state mapping: root property/BOM流向三一级Parent和所有后代；child特有MyBatis、Springdoc、gRPC等BOM继续由原owner管理。
- Error and edge behavior: 不删除plugin annotationProcessor所需的root property；若effective POM出现unresolved
  version或版本变化，停止并恢复到该POM，不以新增局部version修补。
- Standards impact: MC-ARCH-001、MC-REUSE-001、MC-DEP-001、MC-CONFIG-001、MC-SCOPE-001；无新dependency，仅改变已有版本ownership。
- Literal rule enforcement: Rule 5不引入utility；Rule 7 POM不是环境配置且不改application profiles；Rule 11 parent变化不改COLA
  module方向。
- Implementation pseudocode:

```xml
root.properties = {spring.boot.version:3.5.16, springdoc.version:2.8.17,
                   flyway.version:11.15.0, postgresql.version:42.7.8, lombok.version:1.18.46}
root.dependencyManagement imports spring-boot-dependencies at spring.boot.version exactly once
components/platforms/archetypes delete local spring.boot.version and their Boot BOM import
components/platforms use inherited flyway/postgresql/lombok properties; remove only local versions proven equal to Boot BOM
retain platform springdoc BOM and all non-Spring domain BOMs; keep existing public GAV, modules and release plugin behavior
compare effective versions before/after and reject any change outside source-web Springdoc 2.8.13 to 2.8.17
```

- Verification contribution: Maven effective-POM输出证明REQ-012/014，policy脚本证明无重复owner。
- After this file: root lineage模块已GREEN；六source roots仍因重复BOM让File 1脚本保持RED。

#### File 3 —
`MODIFY egon-cola-archetypes/source-projects/egon-cola-source-{light,light-open,service,service-open,web,web-open}/pom.xml`

- Purpose: 保留应用Boot Parent并清理六个重复Boot BOM；在使用Springdoc的四个root引入统一BOM。
- Symbols: parent spring-boot-starter-parent 3.5.16；删除dependencyManagement中的spring-boot-dependencies；Light/Light
  Open/Web/Web Open添加springdoc-openapi-bom 2.8.17；删除具体Springdoc dependency version。
- Repository evidence: 六POM均直接继承Boot
  Parent并再次import同版本BOM；light/light-open/web-open为2.8.17，web为2.8.13；service族不声明Springdoc。
- Dependencies and consumers: 各source root的child modules继承dependencyManagement；generator把这些POM转换为Archetype
  consumer POM。
- Why now: root lineage完成后单独处理不继承仓库root的Boot应用，避免错误改成双parent。
- Contract/signature changes: 保持source internal GAV、module list、Spring Cloud/Alibaba/Dubbo
  BOM和egon-cola.version；仅统一Springdoc patch并移除重复Boot import。
- Input/output and state mapping: Boot Parent提供Spring/Jackson/Logback/JUnit/Lombok版本；Springdoc
  BOM提供相关starter；explicit ShardingSphere/Egon版本保持。
- Error and edge behavior: service roots不得误加Springdoc；Open禁用Flyway合同不变；任何generated POM残留source
  sentinel或重复BOM由Step 5/final gate失败。
- Standards impact: MC-ARCH-001、MC-REUSE-001、MC-DEP-001、MC-CONFIG-001、MC-SCOPE-001、MC-TEST-001；不改Java或environment
  YAML。
- Literal rule enforcement: Rule 5 仅复用既有BOM；Rule 7 六套profile文件完全不动；Rule 11 各source root保留exact
  Light/Service/Web/Open module tree。
- Implementation pseudocode:

```xml
for each source root keep parent org.springframework.boot:spring-boot-starter-parent:3.5.16 with empty relativePath
remove only the spring-boot-dependencies import because the parent already owns dependency management
for light, light-open, web and web-open import org.springdoc:springdoc-openapi-bom:2.8.17
remove version elements from springdoc-openapi-starter-webmvc-api and webmvc-ui dependencies
for service and service-open assert no Springdoc BOM or dependency is introduced
preserve all other BOM order, internal source coordinates, modules, plugin configuration and Open no-Flyway exclusions
```

- Verification contribution: TEST-001、TEST-003、TEST-004及后续source reactor compile。
- After this file: policy脚本GREEN，所有Spring相关module均归入root lineage、Boot application parent或显式isolated IT
  allowlist。

- Validation working directory: /Users/mario/SelfProject/Egon-COLA
- Verification command: bash -n scripts/test-spring-dependency-management.sh &&
  ./scripts/test-spring-dependency-management.sh && ./mvnw -B -ntp -N help:effective-pom
  -Doutput=target/root-effective-pom.xml && ./mvnw -B -ntp -f egon-cola-archetypes/source-projects/pom.xml -DskipTests
  validate
- Expected result: shell gate和Maven命令exit 0；根Boot BOM唯一；六source仅Boot Parent；四Springdoc
  roots解析2.8.17；无非批准dependency version变化。
- Failure returns to: File 1若inventory/allowlist错误；File 2若root lineage effective POM错误；File 3若独立source管理错误。
- Completion criteria: REQ-012至REQ-015全部有静态/effective POM证据，REQ-001 source reactor仍可解析。
- Rollback: 仅回退本Step POM和新测试脚本；不触碰SQL、generated目录或tsbuildinfo。
- Commit paths: scripts/test-spring-dependency-management.sh;
  pom.xml,egon-cola-components/pom.xml,egon-cola-platforms/pom.xml,egon-cola-archetypes/pom.xml;
  egon-cola-archetypes/source-projects/egon-cola-source-{light,light-open,service,service-open,web,web-open}/pom.xml
- Commit: build(dependencies): centralize Spring Boot version management

### Step 2 — 为 Light 增加 master-data 与 shard 累计 Baseline

- Requirements: REQ-016, REQ-017, REQ-018, REQ-020
- Dependencies: Step 1
- Baseline state: Light两个location各有V20260726初始化和V20260825迁移；fresh H2通过完整V链形成最终schema；无B migration。
- Observable outcome: fresh环境每role只执行一个B baseline并得到与旧V链相同schema，已有V环境忽略B且validate成功。
- End state: 新增两个Light B SQL；四个V SQL、YAML location和PhysicalDataSourceFlywayMigrator字节不变。
- Test-first gate: Required — 先让convention/parity测试要求两个B文件、fresh B rank和B/V schema equality，当前因B缺失RED。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-UTIL-001, MC-CONFIG-001, MC-SCOPE-001,
  MC-TEST-001
- Literal Rules: Rule 1, Rule 5, Rule 7, Rule 10, Rule 11
- Ordered files:

#### File 1 —
`MODIFY egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/infrastructure/migration/{FlywayMigrationConventionTest.java,LogicalSchemaParityTest.java}`

- Purpose: 在添加SQL前定义Light baseline命名、每角色数量、新旧路径和最终schema等价合同。
- Symbols: BASELINE_MIGRATION
  pattern；shouldKeepVersionedHistoryAndDeclareOneBaselinePerRole；shouldBuildEquivalentSchemaFromBaselineAndVersionedHistory；copyVersionedMigrationsToTempLocation。
- Repository evidence: 两个现有JUnit 5类已枚举四个V文件、读取classpath并用H2 PostgreSQL mode执行Flyway。
- Dependencies and consumers: 测试读取同module main resources；使用JUnit、AssertJ、Flyway、JDK Files和现有H2 test依赖。
- Why now: 测试先表达缺失B行为，SQL实现不能先于可观察合同。
- Contract/signature changes: production无变化；test helper接收location和@TempDir，分别构造default B-enabled与临时V-only
  Flyway。
- Input/output and state mapping: classpath role files映射到B/V inventory；B-only fresh schema和copied V-only
  schema都映射为table/column/index/seed快照。
- Error and edge behavior: B数量非一、版本不等于role最高V、旧V hash变化、B/V schema差异、master出现shard表或反向出现均fail。
- Standards impact: MC-NAME-001 测试行为类名清晰；MC-UTIL-001 只用JDK；MC-CONFIG-001 验证location不变；MC-TEST-001
  覆盖fresh/existing。
- Literal rule enforcement: Rule 1 不创建POJO carrier；Rule 5 用JDK Files；Rule 7 验证原location；Rule 10 不引入旧日期API；Rule
  11 保留Light单module测试路径。
- Implementation pseudocode:

```java
@Test shouldKeepVersionedHistoryAndDeclareOneBaselinePerRole(@TempDir Path temp) {
    list classpath db/migration and assert exact four immutable V names plus two approved B names
    group by master-data/shard and assert each role has exactly one B whose version equals that role highest V
    assert each SQL header has change, scope and compatibility comments and no unfinished marker
}
@Test shouldBuildEquivalentSchemaFromBaselineAndVersionedHistory(@TempDir Path temp) {
    baselineSchema = migrate fresh database with normal classpath location and assert history selected B
    copy only existing V files byte-for-byte into temp filesystem role location and migrate second database
    assert normalized tables, columns, indexes, constraints and seed rows equal; load normal location on V database and validate B is ignored
}
```

- Verification contribution: TEST-016、TEST-017、TEST-018及immutable history evidence。
- After this file: targeted tests compile and fail only because两个approved B resources不存在。

#### File 2 —
`CREATE egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/db/migration/sharding/{master-data/B20260825_001__baseline_light_master_data_schema.sql,shard/B20260825_002__baseline_light_sharded_schema.sql}`

- Purpose: 用两个直接最终态DDL分别初始化Light master-data和shard新环境。
- Symbols: master tables users、roles、permissions、user_roles、role_permissions、courses；shard tables
  school_classes_0/1、class_course_schedules_0/1及最终索引约束。
- Repository evidence: 同role的V20260726和V20260825文件定义初态与迁移终态；LogicalSchemaParityTest列出八个逻辑表。
- Dependencies and consumers: Flyway从现有classpath locations发现B；source运行和generated consumer都消费；旧V用于已有schema。
- Why now: RED测试已固定最终结构和兼容语义，新增SQL是最小GREEN。
- Contract/signature changes: 不改table/column/index业务合同；新文件prefix B且version与role最高V相同。
- Input/output and state mapping: 空master schema直接产生Long
  id、tenant_id及EgonModel字段；空shard直接产生Long路由/关联字段和两套物理分表；无历史数据转换。
- Error and edge behavior: 只支持empty new environment；不用IF EXISTS掩盖错误；任一DDL失败由Flyway事务失败关闭；不包含ALTER旧schema路径。
- Standards impact: MC-REUSE-001 使用Flyway原生B；MC-CONFIG-001 原location；MC-SCOPE-001 只新增文件；MC-TEST-001 由B/V
  parity证明。
- Literal rule enforcement: Rule 5无utility；Rule 7配置不变；Rule 10 PostgreSQL timestamp语义保持原final schema；Rule
  11资源位于Light现有infrastructure migration路径。
- Implementation pseudocode:

```sql
-- master baseline: create six final-form tables directly with BIGINT ids, tenant_id and audit/deletion columns
-- declare the same active uniqueness and lookup indexes produced by the two existing master V migrations
-- shard baseline: create school_classes_0/1 and class_course_schedules_0/1 directly in their final tenant model
-- declare identical final nullability, key columns and indexes for suffix 0 and suffix 1
-- do not copy transitional UUID columns, migration defaults or DROP/ALTER statements from the V upgrade script
-- keep B20260825_001 and B20260825_002 aligned to each role's current highest V version
```

- Verification contribution: 使Light baseline inventory、fresh B migration、V-only migration和schema parity测试GREEN。
- After this file: Light每role有一个B cumulative起点和两个不可变V history；runtime Java/YAML未改。

- Validation working directory: /Users/mario/SelfProject/Egon-COLA
- Verification command: ./mvnw -B -ntp -f egon-cola-archetypes/source-projects/egon-cola-source-light/pom.xml
  -Dtest=FlywayMigrationConventionTest,LogicalSchemaParityTest test
- Expected result: named tests exit 0；fresh history选择B；V-only临时链与B schema完全等价；四个V hash与Step前inventory一致。
- Failure returns to: File 1若fixture/normalization错；File 2若DDL、version、constraint、seed或schema parity错；任何V
  diff立即停止并回退本Step。
- Completion criteria: Light master/shard baseline和历史兼容有自动证据且无既有migration/config/Java改动。
- Rollback: 删除本Step两个新B文件并回退两个测试文件；既有V不操作。
- Commit paths:
  egon-cola-archetypes/source-projects/egon-cola-source-light/src/test/java/top/egon/cola/archetype/source/light/infrastructure/migration/{FlywayMigrationConventionTest.java,LogicalSchemaParityTest.java};
  egon-cola-archetypes/source-projects/egon-cola-source-light/src/main/resources/db/migration/sharding/{master-data/B20260825_001__baseline_light_master_data_schema.sql,shard/B20260825_002__baseline_light_sharded_schema.sql}
- Commit: feat(archetype-light): add cumulative Flyway baselines

### Step 3 — 为 Service 增加 master-data 与 shard 累计 Baseline

- Requirements: REQ-016, REQ-017, REQ-018, REQ-020
- Dependencies: Step 1
- Baseline state: Service infrastructure两个role各有init和EgonModel migration；EvaluationMigrationTest验证master一表、shard八表。
- Observable outcome: Evaluation fresh环境按role执行单个B，schema与旧V链一致，既有V history继续validate。
- End state: 新增两个Evaluation B SQL；四个V、datasource YAML和migrator完全不改。
- Test-first gate: Required — convention/Evaluation tests先要求B inventory和B/V parity，当前缺B导致RED。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-UTIL-001, MC-CONFIG-001, MC-SCOPE-001,
  MC-TEST-001
- Literal Rules: Rule 1, Rule 5, Rule 7, Rule 10, Rule 11
- Ordered files:

#### File 1 —
`MODIFY egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/test/java/top/egon/cola/archetype/source/service/infrastructure/migration/{FlywayMigrationConventionTest.java,EvaluationMigrationTest.java}`

- Purpose: 固定Evaluation两个B文件、V历史保留、role隔离和新旧schema等价。
- Symbols:
  shouldKeepVersionedHistoryAndDeclareOneBaselinePerRole；shouldInitializeEquivalentMasterAndShardSchemasFromBaselineAndVersionedHistory；copyVersionedMigrations。
- Repository evidence: convention类当前exact四V；EvaluationMigrationTest已用H2/Flyway分别迁master和shard并断言表数。
- Dependencies and consumers: infrastructure test classpath、Flyway、H2、JUnit 5；不需要starter或外部facade运行。
- Why now: 先形成RED，阻止凭SQL手工检查宣称等价。
- Contract/signature changes: test-only；表数断言升级为table/column/index/constraint snapshot，并检查B/V history状态。
- Input/output and state mapping: master B/V均产生evaluation_course；shard
  B/V均产生course_schedule、exam、exam_paper、score的0/1最终物理表。
- Error and edge behavior: master不得出现shard表；shard不得出现evaluation_course；B数量/版本错误、未知历史变更或snapshot差异立即fail。
- Standards impact: MC-NAME-001测试名语义明确；MC-UTIL-001 JDK Files；MC-CONFIG-001 role location不变；MC-TEST-001覆盖隔离。
- Literal rule enforcement: Rule 1 无carrier；Rule 5 不引入测试utility库；Rule 7 不改profiles；Rule 10 无Java旧日期API；Rule
  11 位于Service infrastructure测试边界。
- Implementation pseudocode:

```java
@Test shouldInitializeEquivalentMasterAndShardSchemasFromBaselineAndVersionedHistory(@TempDir Path temp) {
    assert exact four immutable V files and approved B20260825_001/B20260825_002 files
    masterFromB = migrate normal master-data location on fresh H2 and record schema/history
    shardFromB = migrate normal shard location on fresh H2 and record schema/history
    copy only master V and shard V files to separate temporary filesystem locations and migrate V-only databases
    assert master B equals master V with one business table and shard B equals shard V with eight business tables
    re-open V databases with normal locations, migrate and validate; assert baseline migrations are ignored
}
```

- Verification contribution: TEST-016至TEST-018对Service的RED/GREEN和role隔离。
- After this file: tests因两个B resources缺失RED，旧四V仍可单独运行。

#### File 2 —
`CREATE egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/resources/db/migration/sharding/{master-data/B20260825_001__baseline_evaluation_master_data_schema.sql,shard/B20260825_002__baseline_evaluation_sharded_schema.sql}`

- Purpose: 直接建立Evaluation最终master与shard schema，避免新工程重放过渡ALTER链。
- Symbols: master evaluation_course；shard
  evaluation_course_schedule_0/1、evaluation_exam_0/1、evaluation_exam_paper_0/1、evaluation_score_0/1及最终indexes/constraints。
- Repository evidence: 两对现有V SQL和EvaluationMigrationTest给出终态表名/数量；旧migration注释明确脚手架空表前提。
- Dependencies and consumers: existing Flyway locations、source service starter、generated Service archetype。
- Why now: 测试已定义两role准确终态，SQL只实现已批准baseline。
- Contract/signature changes: 无runtime Java/API变化；B版本与当前role最高V一致且不替代V文件。
- Input/output and state mapping:
  空master直建evaluation_course终态；空shard直建八物理表及tenant/audit/delete字段；所有route和relation列类型按旧V终态。
- Error and edge behavior: 不创建跨库FK；不接受非空既有schema；重复对象/权限错误由Flyway失败；不含迁移期UPDATE/ALTER。
- Standards impact: MC-REUSE-001、MC-CONFIG-001、MC-SCOPE-001、MC-TEST-001；无需新依赖或Java Bean。
- Literal rule enforcement: Rule 5 无额外工具；Rule 7 同location；Rule 10 时间列保持原终态；Rule 11 只在Service
  infrastructure资源层。
- Implementation pseudocode:

```sql
-- master B baseline creates evaluation_course directly with BIGINT id, tenant and full EgonModel audit fields
-- recreate current tenant-scoped active unique course-code index and tenant/create lookup index
-- shard B baseline creates the eight evaluation_*_0/1 final physical tables with BIGINT routing keys
-- add the same exam-paper and score active unique indexes plus score lookup indexes as V-chain final state
-- omit cross-database foreign keys and every transitional UUID conversion, rename, update or drop statement
-- preserve exact 0/1 physical-table symmetry so ShardingSphere logical tables remain unchanged
```

- Verification contribution: Service fresh B、V-only、existing V+B validate和schema parity全部GREEN。
- After this file: Service每role一个cumulative B；四V和运行配置不变。

- Validation working directory: /Users/mario/SelfProject/Egon-COLA
- Verification command: ./mvnw -B -ntp -f egon-cola-archetypes/source-projects/egon-cola-source-service/pom.xml -pl
  egon-cola-source-service-infrastructure -am -Dtest=FlywayMigrationConventionTest,EvaluationMigrationTest
  -Dsurefire.failIfNoSpecifiedTests=false test
- Expected result: targeted tests exit 0；master/shard B/V snapshots分别一致；existing V+B validate成功；旧V hash无变化。
- Failure returns to: File 1测试fixture或File 2终态DDL；跨role对象或任一old V diff直接停止。
- Completion criteria: Evaluation两role baseline满足确认方案并保持Service COLA边界。
- Rollback: 删除两个新B并回退两个测试；不修改旧V。
- Commit paths:
  egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/test/java/top/egon/cola/archetype/source/service/infrastructure/migration/{FlywayMigrationConventionTest.java,EvaluationMigrationTest.java};
  egon-cola-archetypes/source-projects/egon-cola-source-service/egon-cola-source-service-infrastructure/src/main/resources/db/migration/sharding/{master-data/B20260825_001__baseline_evaluation_master_data_schema.sql,shard/B20260825_002__baseline_evaluation_sharded_schema.sql}
- Commit: feat(archetype-service): add cumulative Flyway baselines

### Step 4 — 为 Web 增加 master-data 与 shard 累计 Baseline

- Requirements: REQ-016, REQ-017, REQ-018, REQ-020
- Dependencies: Step 1
- Baseline state: Web infrastructure有四个Organization V
  migrations；现有测试验证master六表与两个seed、shard四表、逻辑schema及grade/class复合FK。
- Observable outcome: fresh Organization环境按role执行一个B并与旧V链生成完全相同的schema、索引、约束和seed。
- End state: 新增两个Web B SQL；四V、YAML、migrator、HTTP/RPC合同不变。
- Test-first gate: Required — convention、migration、parity测试先要求两个B及B/V等价，当前因B缺失RED。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-UTIL-001, MC-CONFIG-001, MC-SCOPE-001,
  MC-TEST-001
- Literal Rules: Rule 1, Rule 5, Rule 7, Rule 10, Rule 11
- Ordered files:

#### File 1 —
`MODIFY egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/test/java/top/egon/cola/archetype/source/web/infrastructure/{migration/FlywayMigrationConventionTest.java,OrganizationFlywayMigrationTest.java,migration/LogicalSchemaParityTest.java}`

- Purpose: 定义Organization baseline数量、命名、fresh/existing行为和全部结构语义。
- Symbols:
  shouldKeepVersionedHistoryAndDeclareOneBaselinePerRole；migratesEquivalentFreshMasterAndShardSchemasFromBaseline；shouldMatchVersionedHistorySchema；existingVHistoryIgnoresBaseline。
- Repository evidence: 三个现有JUnit类覆盖文件命名、table/seed、逻辑列parity和复合FK拒绝行为。
- Dependencies and consumers: Web infrastructure test runtime、H2 PostgreSQL mode、Flyway、AssertJ；不启动Web starter。
- Why now: 先固定最复杂的seed/FK/index终态，再写B SQL，避免只以表数量判断成功。
- Contract/signature changes: test-only helper增加schema snapshot、index/constraint metadata、seed rows和V-only临时location。
- Input/output and state mapping: master
  B/V均得到users/roles/permissions/user_roles/role_permissions/grades和相同1001/2001种子；shard
  B/V得到四物理表与复合grade/class关联。
- Error and edge behavior: seed缺失/重复、id或tenant错误、cross-role表、nullable/type/index/FK差异、旧V byte变化均fail。
- Standards impact: MC-NAME-001测试命名语义化；MC-UTIL-001 JDK helper；MC-CONFIG-001保持locations；MC-TEST-001包含约束负例。
- Literal rule enforcement: Rule 1不创建业务carrier；Rule 5使用JDK/JUnit既有依赖；Rule 7 profile不动；Rule
  10无旧Java日期API；Rule 11保持Web infrastructure位置。
- Implementation pseudocode:

```java
@Test shouldMatchVersionedHistorySchema(@TempDir Path temp) {
    assert four immutable V names plus exactly B20260825_003 master and B20260825_004 shard
    migrate normal master and shard locations on fresh databases and assert baseline rows selected
    copy only V files to temp role locations, migrate V-only databases and capture columns, indexes, constraints and seeds
    assert master snapshots and rows equal; assert shard snapshots equal and suffix 0/1 remain symmetric
    execute mismatched grade membership insert and assert SQLException for both B and V schema variants
    re-open V databases with normal locations, migrate/validate and assert B entries remain ignored
}
```

- Verification contribution: TEST-016至TEST-018，包含Web seed和referential integrity。
- After this file: 测试在两个B资源缺失处RED；原有V链测试语义仍保留。

#### File 2 —
`CREATE egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/main/resources/db/migration/sharding/{master-data/B20260825_003__baseline_organization_master_data_schema.sql,shard/B20260825_004__baseline_organization_sharded_schema.sql}`

- Purpose: 直接建立Organization最终master与shard schema及确定性初始权限种子。
- Symbols: master six tables和tenant-scoped indexes、roles id 1001、permissions id 2001；shard
  school_classes_0/1、school_class_users_0/1及复合FK/index。
- Repository evidence: 当前两组V SQL、OrganizationFlywayMigrationTest和LogicalSchemaParityTest给出全部终态约束。
- Dependencies and consumers: existing role locations、Web source starter、generated Web archetype；不影响Open Web manual
  SQL。
- Why now: RED测试已经定义结构和数据终态，B SQL提供最小fresh-path实现。
- Contract/signature changes: B prefix加同role最高版本；不改表/列/seed公共语义。
- Input/output and state mapping: 空master直建Long/tenant/EgonModel六表并插入两个已知seed；空shard直建四个Long/tenant最终表及复合关联。
- Error and edge behavior: seed主键/唯一冲突使迁移失败；不以ON CONFLICT隐藏异常；不建跨库FK；不接受已有非Flyway对象。
- Standards impact: MC-REUSE-001、MC-CONFIG-001、MC-SCOPE-001、MC-TEST-001；仅新增SQL，保持Open exclusion。
- Literal rule enforcement: Rule 5 无utility；Rule 7 同role location；Rule 10 时间列类型保持最终V定义；Rule 11 只在Web
  infrastructure。
- Implementation pseudocode:

```sql
-- master B baseline creates users, roles, permissions, user_roles, role_permissions and grades in final BIGINT tenant model
-- create every active tenant-scoped unique and lookup index from the current V-chain final state
-- insert the existing role 1001 and permission 2001 rows with the same code, name, status, tenant and audit values
-- shard B baseline creates school_classes_0/1 and school_class_users_0/1 with final tenant and audit columns
-- add current tenant/grade/name uniqueness plus composite class membership foreign keys for both suffixes
-- exclude every transitional UUID update, column rename, ALTER or cross-master foreign key
```

- Verification contribution: 使Web baseline、schema parity、seed和FK tests GREEN。
- After this file: Web每role一个B起点，旧V/external contracts/Open manual SQL均未变。

- Validation working directory: /Users/mario/SelfProject/Egon-COLA
- Verification command: ./mvnw -B -ntp -f egon-cola-archetypes/source-projects/egon-cola-source-web/pom.xml -pl
  egon-cola-source-web-infrastructure -am
  -Dtest=FlywayMigrationConventionTest,OrganizationFlywayMigrationTest,LogicalSchemaParityTest
  -Dsurefire.failIfNoSpecifiedTests=false test
- Expected result: named tests exit 0；B/V master及shard schema一致；seed一致；复合FK负例一致；旧V hash不变。
- Failure returns to: File 1测试snapshot/fixture或File 2 DDL/seed/constraint；Open tree或旧V出现diff立即回退。
- Completion criteria: Organization baseline安全闭合，三个legacy source project均已具备两role B migration。
- Rollback: 删除两个新B并回退三个测试；旧V和Open SQL不操作。
- Commit paths:
  egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/test/java/top/egon/cola/archetype/source/web/infrastructure/{migration/FlywayMigrationConventionTest.java,OrganizationFlywayMigrationTest.java,migration/LogicalSchemaParityTest.java};
  egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/main/resources/db/migration/sharding/{master-data/B20260825_003__baseline_organization_master_data_schema.sql,shard/B20260825_004__baseline_organization_sharded_schema.sql}
- Commit: feat(archetype-web): add cumulative Flyway baselines

### Step 5 — 外置六份 definitions 并生成完整 Maven Archetype Reactor

- Requirements: REQ-001, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-016, REQ-017, REQ-018,
  REQ-019, REQ-020
- Dependencies: Steps 2, 3, 4
- Baseline state: generator从六旧module内manifest发现source并只输出archetype-resources与hash；metadata、child
  POM、IT和javadoc仍在旧module。
- Observable outcome: definitions成为非业务打包事实源，CLI-001原子生成一个aggregator和六个完整maven-archetype
  child，CLI-002可独立check。
- End state: .generated可直接被Maven解析和IT；旧modules暂时保留作parity基线，尚未退出parent Reactor。
- Test-first gate: Required — generator fixture先改为definitions输入并断言完整child/aggregator/check
  wrapper；当前generator因旧discovery和缺compose输出RED。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-UTIL-001, MC-CONFIG-001, MC-PATTERN-001,
  MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 1, Rule 5, Rule 7, Rule 9, Rule 11
- Ordered files:

#### File 1 — `MODIFY scripts/test-generate-archetypes.sh`

- Purpose: 将fixture合同从旧package module discovery升级为definitions和完整generated Reactor。
- Symbols:
  setup_definition_fixture、test_definition_validation、test_complete_child_layout、test_generated_aggregator、test_check_wrapper、test_atomic_full_reactor_failure、test_legacy_sql_hash_boundary。
- Repository evidence: 当前脚本已有fake mvnw、manifest validation、failure
  injection、lock/signal/determinism及package/release modes。
- Dependencies and consumers: copy production generator和新check wrapper到temp repo；fixture创建三个definitions/source
  roots；由Step 5和CI调用。
- Why now: 在复制合同和改generator前先建立RED，确保完整module不是事后人工检查。
- Contract/signature changes: unit mode预期definitions path；generation mode要求generated parent/children；package
  mode将在Step 6再切新profile断言。
- Input/output and state mapping: definition manifest+packaging POM+curated resources+fake plugin
  resources映射为child标准Maven tree；sorted target IDs映射aggregator modules。
- Error and edge behavior: missing/extra definition、artifact mismatch、rootVersion token未替换、curated
  file缺失、child重复、aggregate partial、check mutation全部fail。
- Standards impact: MC-REUSE-001 复用现有fixture；MC-UTIL-001 只用shell；MC-PATTERN-001 验证pipeline phases；MC-TEST-001
  覆盖原子集合。
- Literal rule enforcement: Rule 1 无Java carrier；Rule 5 只用允许工具；Rule 7 不改Spring profiles；Rule 9 数据驱动无六分支；Rule
  11 逐definition校验exact topology。
- Implementation pseudocode:

```bash
fixture creates definitions/package-a..c with archetype.properties, packaging-pom.xml, META-INF, IT and javadoc inputs
first assertion calls current generator and expects failure because it still scans old module manifests
after GREEN assert .generated/pom.xml lists package-a,b,c once in sorted order and is deploy-skipped
for each child assert pom.xml packaging=maven-archetype, resources, META-INF, post script, javadoc and basic IT exist
inject failure on child two and assert previous complete parent plus every old child hash is unchanged
invoke check_archetypes.sh and assert it delegates check without changing .generated; reject tracked/generated source mutation
```

- Verification contribution: TEST-005至TEST-009及CLI-001/CLI-002合同。
- After this file: 新unit fixture在当前generator上RED，旧测试helper仍可复用。

#### File 2 — `CREATE scripts/check_archetypes.sh`

- Purpose: 提供Spec CLI-002要求的独立稳定入口，同时保持check实现只有一份。
- Symbols: repository-root resolution and exec generate_archetypes.sh check。
- Repository evidence: generator main已支持唯一参数generate或check；新增第二实现会重复lock/discovery/compare逻辑。
- Dependencies and consumers: scripts/generate_archetypes.sh；CI、maven-deploy.sh和maintainer。
- Why now: File 1已定义thin-wrapper合同，先建立入口再修改producer。
- Contract/signature changes: 无参数；任何参数返回usage code 2；成功/错误完全传播generator exit code和stdout/stderr。
- Input/output and state mapping: current checkout传给generator check；不改变generated、source或definitions。
- Error and edge behavior: generator缺失/不可执行、参数非空或check失败都非零；使用exec确保signal/exit code不被吞。
- Standards impact: MC-REUSE-001、MC-DEP-001、MC-UTIL-001、MC-SCOPE-001、MC-TEST-001；无新工具和状态。
- Literal rule enforcement: Rule 5 仅Bash/JDK外无依赖；Rule 7 无配置变化；Rule 11 不接触业务树。
- Implementation pseudocode:

```bash
set -Eeuo pipefail
resolve SCRIPT_DIR using BASH_SOURCE and derive GENERATOR=${SCRIPT_DIR}/generate_archetypes.sh
if argument count is not zero then print Usage: scripts/check_archetypes.sh to stderr and exit 2
if generator is not executable then print exact missing path and exit 1
exec "${GENERATOR}" check so stdout, stderr, signal and exit code remain authoritative
```

- Verification contribution: CLI-002 unit、drift和read-only断言。
- After this file: wrapper存在；在generator改造前仍按旧input合同工作且完整fixture继续RED。

#### File 3 —
`CREATE egon-cola-archetypes/definitions/egon-cola-archetype-{light,light-open,service,service-open,web,web-open}/{archetype.properties,packaging-pom.xml}`

- Purpose: 为每个public artifact声明source/topology和generated child Maven打包合同。
- Symbols: manifest keys
  sourceProject、sourceGroupId、sourceArtifactId、sourceVersion、sourcePackage、targetArtifactId、expectedTopology；packaging
  POM rootVersion token和maven-archetype packaging。
- Repository evidence: 当前六旧manifest和POM提供确切GAV、test facade dependencies、metadata及plugin declarations。
- Dependencies and consumers: generator唯一读取；definitions本身不列入任何Maven modules。
- Why now: tests和wrapper已定，先建立数据输入再改discovery/composition。
- Contract/signature changes: sourceProject改为相对archetypes root的source-projects路径；generated child parent
  relativePath为../../pom.xml；public artifactId/name/description不变。
- Input/output and state mapping: 一definition映射一个source和一个generated child；rootVersion
  token映射当前egon-cola-archetypes parent version；service/web legacy test facade dependencies保持。
- Error and edge behavior: unknown/missing/duplicate key、path escape、artifactId不一致、未替换token、duplicate
  target或topology缺module都fail。
- Standards impact: MC-ARCH-001、MC-REUSE-001、MC-CONFIG-001、MC-PATTERN-001、MC-SCOPE-001；properties驱动变体，不新增Java。
- Literal rule enforcement: Rule 5无新library；Rule 7 manifest不是Spring profile；Rule 9避免hard-coded case per
  family；Rule 11 exact六variant topology。
- Implementation pseudocode:

```properties
sourceProject=source-projects/egon-cola-source-service
sourceGroupId=top.egon.internal.archetype.source
sourceArtifactId=egon-cola-source-service
sourceVersion=0.1.0-SNAPSHOT
sourcePackage=top.egon.cola.archetype.source.service
targetArtifactId=egon-cola-archetype-service
expectedTopology=common,domain,application,infrastructure,adapter,starter
packaging-pom parent=top.egon:egon-cola-archetypes-parent:@rootVersion@ relativePath=../../pom.xml
packaging=maven-archetype; preserve service/web test-scoped facade dependencies and inherited enforcer/jar plugins
```

- Verification contribution: definition inventory、GAV、topology和generated effective POM assertions。
- After this file: 六definitions的identity/POM合同存在，但curated assets尚未完整，generator仍RED。

#### File 4 —
`CREATE egon-cola-archetypes/definitions/egon-cola-archetype-{light,light-open,service,service-open,web,web-open}/{src/main/resources/META-INF,src/main/javadoc,src/test/resources/projects/basic,architecture-docs}`

- Purpose: 从旧module逐文件迁入所有不可由create-from-project推导的发布、IT和说明合同。
- Symbols:
  META-INF/maven/archetype-metadata.xml、META-INF/archetype-post-generate.groovy、README.md、archetype.properties、goal.txt、verify.groovy及三类architecture
  markdown。
- Repository evidence: 每个旧module现有对应文件；metadata fileSets和module sets不同；post
  script恢复mvnw权限；verify.groovy含逐族架构和source sentinel断言。
- Dependencies and consumers: generator复制到generated child；Maven Archetype lifecycle读取metadata/IT；javadoc Jar
  execution读取README；用户阅读architecture docs。
- Why now: identity合同已存在，随后generator可一次校验每份definition完整性。
- Contract/signature changes: 内容先逐字复制，路径owner从旧Maven module改为definition；不改变consumer输入、error或generated
  project assertions。
- Input/output and state mapping: old curated file
  SHA-256映射definition同名文件；architecture根文档映射definition/architecture-docs下原文件名。
- Error and edge behavior: 任一hash差异在本Step禁止；不得复制archetype-resources业务树或旧Flyway
  archive；不得把本地绝对路径写入README。
- Standards impact: MC-ARCH-001、MC-CONFIG-001、MC-SCOPE-001、MC-TEST-001；既有verifier原样保留所有Java/config合同。
- Literal rule enforcement: Rule 1测试脚本不改业务type；Rule 7 verifier继续检查profiles；Rule 11
  metadata和architecture按exact variant分开。
- Implementation pseudocode:

```text
for each six old module identify only META-INF metadata/post script, javadoc README and basic IT three files
copy those files byte-for-byte into the matching definition conventional src tree
copy Light pair monolith architecture doc, Service pair service-only doc and Web pair multi-module doc into architecture-docs
assert definition contains no archetype-resources directory, Java source, generated output or db/migration SQL
record before/after SHA-256 pairs and fail when any curated contract byte differs
leave every old file present until Step 6 cutover succeeds
```

- Verification contribution: metadata/post/IT/javadoc parity和REQ-003 single ownership准备。
- After this file: 六definitions完整；旧module仍有相同副本作为临时过渡。

#### File 5 — `MODIFY scripts/generate_archetypes.sh`

- Purpose: 改为从definitions发现并在同一staging内组合完整aggregator/children，再原子发布。
- Symbols:
  DEFINITIONS_ROOT；discover_definitions；parse_manifest；resolve_root_version；compose_child_module；write_generated_aggregator；validate_generated_reactor；compare_generated_set。
- Repository evidence: 当前脚本已有安全path解析、fixed plugin goal、normalization、source snapshot、lock、staging、atomic
  backup/restore和determinism compare。
- Dependencies and consumers: Files 2-4 definitions/wrapper；root mvnw；Step 6 generated profile；CI/release。
- Why now: tests和所有input合同已确定，能做最小producer改变。
- Contract/signature changes: generate/check命令不变；manifest discovery path改变；output从resource fragment升级为完整Maven
  tree；日志加入aggregator count/fingerprint。
- Input/output and state mapping: source经plugin/normalize得到child resources；definition packaging
  POM/META-INF/IT/javadoc/docs复制到child；sorted target IDs写aggregator modules；rootVersion token替换为Maven求值结果。
- Error and edge behavior: definition/source count mismatch、token残留、curated缺失、child POM
  GAV错误、aggregator多余module、source并发变化或任一plugin失败均不swap；check不写current generated。
- Standards impact: MC-REUSE-001复用原函数；MC-DEP-001无新dependency；MC-UTIL-001 shell
  allowlist；MC-PATTERN-001明确pipeline；MC-TEST-001全失败面。
- Literal rule enforcement: Rule 5只用现有shell/Maven；Rule 7不重写环境YAML；Rule 9以definition数据驱动阶段，无artifactId
  switch；Rule 11 validate_topology保持六profile。
- Implementation pseudocode:

```bash
discover definitions by sorted definitions/egon-cola-archetype-{light,light-open,service,service-open,web,web-open}/archetype.properties and reject undeclared directories
resolve rootVersion once through ./mvnw -q -N help:evaluate and require a nonempty nonexpression value
for each definition parse/validate source then invoke maven-archetype-plugin:3.4.1:create-from-project in staging
normalize resources, copy packaging-pom with rootVersion replacement, overlay META-INF, javadoc, basic IT and architecture docs
write staging/pom.xml with deploy skip and one sorted module element for each target artifact
validate every child packaging/GAV/resource/curated file and full inventory; assert source snapshot unchanged
generate mode atomically swaps the whole reactor; check mode recursively compares full staging tree, modes and provenance
```

- Verification contribution: generator unit/generation/check、atomicity、source immutability和full Maven layout。
- After this file: CLI-001/CLI-002 GREEN；旧module仍为default Reactor，generated Reactor可独立解析。

#### File 6 —
`GENERATED egon-cola-archetypes/.generated/{pom.xml,egon-cola-archetype-{light,light-open,service,service-open,web,web-open}}`

- Purpose: 产生不入库的完整第二阶段Maven Reactor，作为后续IT和deploy唯一public Archetype实现。
- Symbols: private aggregator egon-cola-generated-archetypes-reactor；six public child
  POMs/resources/META-INF/javadoc/IT/docs；generation-manifest.sha256。
- Repository evidence: .gitignore已覆盖root；当前目录已有六resources fragments；Spec §8要求完整reactor。
- Dependencies and consumers: 只由File 5生成；Step 6 profile、Step 7 release/CI消费；用户不得编辑。
- Why now: producer已GREEN，执行一次real generate建立切换候选。
- Contract/signature changes: public child GAV与当前六module相同；aggregator是private且deploy skip；business source
  GAV不发布。
- Input/output and state mapping: six sources+definitions映射six完整children；B/V migrations来自source，Open manual
  SQL原样；hash记录每个tracked input/output。
- Error and edge behavior: generated path若tracked、token残留、缺child、extra child、IT缺失或check diff即不可进入Step
  6；清理可删除整目录重建。
- Standards impact: MC-ARCH-001、MC-CONFIG-001、MC-SCOPE-001、MC-TEST-001；派生物保持exact架构和profiles。
- Literal rule enforcement: Rule 1-7/9/10只通过既有consumer verifier观察，不手改Java；Rule 11由六topology verifier强制。
- Implementation pseudocode:

```text
execute scripts/generate_archetypes.sh generate from repository root
assert generated parent lists exactly six sorted child directories and has deploy skip
for each child inspect effective parent version, maven-archetype packaging and all curated/resource trees
execute scripts/check_archetypes.sh and compare a second candidate byte/mode manifest to current .generated
assert git ls-files for .generated returns zero and git status does not stage any generated artifact
retain the complete generated set only as local input for Step 6 Maven integration tests
```

- Verification contribution: TEST-005至TEST-012以及REQ-005/006。
- After this file: 一个完整ignored generated Reactor可重建、可校验；尚未从root profile发布。

- Validation working directory: /Users/mario/SelfProject/Egon-COLA
- Verification command: bash -n scripts/generate_archetypes.sh scripts/check_archetypes.sh
  scripts/test-generate-archetypes.sh && ./scripts/test-generate-archetypes.sh unit && ./scripts/generate_archetypes.sh
  generate && ./scripts/check_archetypes.sh && test -z "$(git ls-files -- egon-cola-archetypes/.generated)" && ./mvnw -B
  -ntp -f egon-cola-archetypes/.generated/pom.xml -Dgpg.skip=true clean integration-test
- Expected result: shell和Maven exit 0；完整aggregator/six children通过IT；生成两次deterministic；generated无tracked文件；旧V
  hashes不变。
- Failure returns to: File 1合同、Files 3-4 definition、File 5生成逻辑或Step 2-4 source migration；不得先执行Step 6删除。
- Completion criteria: REQ-003至REQ-008有本地可重复证据，六GAV/generated topology兼容。
- Rollback: 回退Files 1-5并删除ignored .generated重建；旧module/source不受影响。
- Commit paths: scripts/test-generate-archetypes.sh; scripts/check_archetypes.sh;
  egon-cola-archetypes/definitions/egon-cola-archetype-{light,light-open,service,service-open,web,web-open}/{archetype.properties,packaging-pom.xml};
  egon-cola-archetypes/definitions/egon-cola-archetype-{light,light-open,service,service-open,web,web-open}/{src/main/resources/META-INF,src/main/javadoc,src/test/resources/projects/basic,architecture-docs};
  scripts/generate_archetypes.sh;
  egon-cola-archetypes/.generated/{pom.xml,egon-cola-archetype-{light,light-open,service,service-open,web,web-open}}
- Commit: build(archetypes): generate complete Maven archetype reactor

### Step 6 — 切换 Archetype Parent 到 generated profile 并移除旧 Maven Modules

- Requirements: REQ-002, REQ-003, REQ-005, REQ-006, REQ-009, REQ-010, REQ-017, REQ-019, REQ-020
- Dependencies: Step 5
- Baseline state: 完整.generated Reactor可独立IT，但egon-cola-archetypes/pom.xml默认仍列六旧module，旧目录仍有POM、curated合同和业务template副本。
- Observable outcome: default archetypes Reactor只聚合两个facade；generated-archetypes profile聚合.generated；旧目录不再是Maven
  module且只剩批准的十二个V SQL archive。
- End state: fresh checkout默认Maven解析不要求.generated；生成后第二次Maven调用可构建六public GAV；不存在旧业务template副本。
- Test-first gate: Required — package/archive mode先断言新profile、无旧POM和旧目录只剩exact V allowlist，当前结构RED。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-UTIL-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 5, Rule 7, Rule 11
- Ordered files:

#### File 1 — `MODIFY scripts/test-generate-archetypes.sh`

- Purpose: 在破坏性删除前锁定default/profile Reactor、旧模块缺席和历史SQL例外合同。
- Symbols:
  test_reactor_cutover_mode、assert_default_modules、assert_generated_profile、assert_legacy_archive_allowlist、assert_no_duplicate_business_template。
- Repository evidence: 当前package mode按旧目录扫描POM/.generated resource wiring；unit/generation modes已在Step
  5适配definitions。
- Dependencies and consumers: 读取archetypes POM、definitions、generated reactor、git tracked旧目录；由Step 6和最终CI执行。
- Why now: 先让新结构assertions在旧modules存在时RED，随后POM切换/删除才能证明GREEN。
- Contract/signature changes: package mode参数改为reactor；不再把旧family目录视为可构建module。
- Input/output and state mapping: default modules集合映射两个facade；profile
  module映射唯一.generated；每个旧legacy目录映射四个immutable V文件，Open旧目录映射空/不存在。
- Error and edge behavior: 旧POM、旧metadata/IT/javadoc/business file残留、任一V缺失/hash变化、profile默认激活或generated
  tracked均fail。
- Standards impact: MC-ARCH-001、MC-CONFIG-001、MC-SCOPE-001、MC-TEST-001；验证删除边界且不修改archive。
- Literal rule enforcement: Rule 5使用git/find/grep；Rule 7检查YAML只来自source/generated；Rule 11 profile生成的六topology保持。
- Implementation pseudocode:

```bash
read default modules from egon-cola-archetypes/pom.xml and assert exactly organization-facade,evaluation-facade
assert profile id generated-archetypes contains one module .generated and is not activeByDefault
assert every generated child is listed once by .generated/pom.xml and every old package pom.xml is absent
for Light/Service/Web old directories compare git tracked files to the exact twelve immutable V SQL allowlist
for three Open old directories assert no tracked file remains; assert no old archetype-resources business Java/POM/config survives
execute the fixture's fresh-checkout default Maven validate without .generated, then execute real profile integration-test
```

- Verification contribution: TEST-002、TEST-009、TEST-010、TEST-016和fresh-checkout parse合同。
- After this file: 新assertions在当前old module list/POM上RED，删除尚未发生。

#### File 2 — `MODIFY egon-cola-archetypes/pom.xml`

- Purpose: 将public Archetype module参与方式改为显式第二阶段profile并调整generated child共享preflight路径。
- Symbols: default modules two facades；profile generated-archetypes module .generated；maven-enforcer require generated
  child resource at project.basedir；release plugin management保持。
- Repository evidence: 当前default modules列两个facade和六旧module；parent已有Archetype 3.4.1、javadoc
  Jar、source/GPG/Central配置。
- Dependencies and consumers: root Reactor、.generated aggregator/children、maven-deploy.sh/workflows；Step 1已处理Boot BOM
  inheritance。
- Why now: generated替代已在Step 5通过独立IT，可安全切Maven入口。
- Contract/signature changes: 默认build不再产出六public GAV；显式-Pgenerated-archetypes才加入完整generated
  Reactor；GAV和release profile不变。
- Input/output and state mapping: fresh checkout default -> parent+facades；generated checkout profile ->
  parent+facades+private aggregator+sixchildren；aggregator deploy skip。
- Error and edge behavior: profile开启而.generated缺失在Maven model解析阶段明确失败；默认模式即使.generated不存在也必须validate；child
  resource缺失在enforcer validate失败。
- Standards impact: MC-ARCH-001、MC-REUSE-001、MC-DEP-001、MC-CONFIG-001、MC-SCOPE-001；只改build graph。
- Literal rule enforcement: Rule 5只用Maven既有插件；Rule 7不改profiles配置文件，此处Maven profile隔离生成阶段；Rule 11
  exact六Archetype仍由generated child保持。
- Implementation pseudocode:

```xml
default modules = [egon-cola-organization-facade, egon-cola-evaluation-facade]
profile id=generated-archetypes has modules=[.generated] and no activeByDefault
shared enforcer for maven-archetype children requires ${project.basedir}/src/main/resources/archetype-resources/pom.xml
keep maven.archetype.version=3.4.1, archetype-packaging extension and source/javadoc/GPG/Central lifecycle ordering
do not add source-projects as a root/archetypes module and do not deploy private generated aggregator
preserve release metadata, distributionManagement and public parent version
```

- Verification contribution: default/profile effective model、generated IT和artifact allowlist。
- After this file: Maven cutover完成；旧module POM仍物理存在但不被聚合，File 3/4继续清理。

#### File 3 —
`DELETE egon-cola-archetypes/egon-cola-archetype-{light,light-open,service,service-open,web,web-open}/{pom.xml,src/main/archetype,src/main/javadoc,src/main/resources/META-INF,src/test,root-architecture-docs,.gitignore}`

- Purpose: 删除已迁入definitions/generated的旧Maven身份和curated副本。
- Symbols: six pom.xml；six manifest trees；six META-INF trees；six javadoc/IT trees；family architecture docs；module-local
  gitignore。
- Repository evidence: Step 5已逐字复制curated合同并生成完整child；Step 6 File 2不再引用旧module。
- Dependencies and consumers: definitions成为contract owner；generated children成为Maven consumer；无remaining
  POM/module引用这些路径。
- Why now: 替代路径和parent cutover均完成后才允许删除，避免不可恢复中间态。
- Contract/signature changes: Git路径删除；公开artifact内容不变；old directories不再被IDE/Maven识别为项目。
- Input/output and state mapping: deleted contract path逐一映射definitions同名owner；deleted POM映射generated
  packaging-pom展开结果；docs映射definition architecture-docs。
- Error and edge behavior: 删除前/后SHA parity必须通过；任何unmapped file或consumer引用停止；严禁递归删除整个legacy目录以免触及V
  archive。
- Standards impact: MC-ARCH-001、MC-SCOPE-001、MC-TEST-001；删除精确path集合，无业务Java重写。
- Literal rule enforcement: Rule 5使用精确Git path list而非宽泛rm；Rule 7 source profiles由generated保留；Rule 11
  generated exact module tree已验证。
- Implementation pseudocode:

```text
build a tracked deletion list containing six POMs and only manifest, META-INF, javadoc, basic IT, architecture docs and module gitignore
for each deleted curated file require a matching definition file with equal SHA-256 before staging deletion
search root POMs/scripts/workflows for references to old module paths and require only archive assertions remain
stage exact deletion paths without recursive removal of legacy directory roots
assert no deleted path is one of the twelve immutable Flyway V files
assert generated child contains the replacement contract before accepting the deletion set
```

- Verification contribution: REQ-002/003和old module absence contract。
- After this file: old dirs不再是Maven modules；legacy dirs只可能包含business template SQL subtree，下一File删除非历史业务副本。

#### File 4 — `DELETE six-old-archetype-module-non-flyway-path-set`

- Purpose: 删除旧archetype-resources内所有业务Java/POM/config/docs/manual SQL副本，同时精确保留十二个legacy V SQL
  archive。
- Symbols: tracked set等于六旧archetype-resources文件减去十二个Spec DEC-006列出的V paths。
- Repository evidence: source-projects拥有全部业务事实源，Step 5 generated round-trip已通过；git ls-files可形成确定性差集。
- Dependencies and consumers: generated resources只来自source；旧POM/resources配置已在Files 2-3移除，故无runtime/compile
  consumer。
- Why now: 这是旧module清理中风险最高的机械删除，必须最后执行并由allowlist保护。
- Contract/signature changes: 只改变repository ownership；consumer生成文件由generated child继续提供；Open manual SQL仍由Open
  source生成。
- Input/output and state mapping: 每个被删relative path必须在对应source/generated tree有等价路径和bytes；十二个V
  archive无replacement要求且保持原地。
- Error and edge behavior: 差集若包含未映射文件、V path、Flyway header或未知root文件则停止；禁止使用指向old module
  root的递归删除命令。
- Standards impact: MC-ARCH-001、MC-CONFIG-001、MC-SCOPE-001、MC-TEST-001；保护config/Flyway边界。
- Literal rule enforcement: Rule 5 用git ls-files和显式allowlist；Rule 7 生成消费者profile keys与source相等；Rule 11
  业务树只从exact source profile生成。
- Implementation pseudocode:

```text
immutable = exact twelve old-module V SQL repository paths captured with SHA-256 before Step
candidates = git ls-files under six old archetype-resources roots minus immutable
for each candidate derive matching source/generated relative path and require semantic or byte parity from Step 5 manifest
abort when candidate is SQL under db/migration, lacks a replacement, or appears in any remaining Maven/resource reference
stage deletion of candidates one exact path at a time; never remove parent legacy directories recursively
after staging assert old tracked set equals immutable exactly and every immutable path/hash equals pre-Step inventory
run generated profile IT to prove all removed consumer content comes from source plus definitions
```

- Verification contribution: no duplicate business source、Open manual SQL preservation、V archive allowlist及generated
  consumer完整性。
- After this file: 三个legacy目录仅剩十二个原路径V archive；三个Open旧目录为空/消失；业务事实源唯一。

- Validation working directory: /Users/mario/SelfProject/Egon-COLA
- Verification command: ./scripts/test-generate-archetypes.sh package reactor && ./mvnw -B -ntp -f
  egon-cola-archetypes/pom.xml validate && ./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -Pgenerated-archetypes clean
  integration-test && ./scripts/check_archetypes.sh
- Expected result: package/archive assertionsGREEN；无.generated时default validate成功；恢复后profile六IT成功；旧tracked文件exact等于十二V
  allowlist且hash未变。
- Failure returns to: File 1 contract、File 2 profile/preflight、File 3 curated mapping或File 4 deletion
  allowlist；任何V异常立即停止并恢复非SQL删除。
- Completion criteria: 旧Maven modules完全退出且业务副本删除，历史SQL例外与完整generated Reactor同时成立。
- Rollback: 回退Step 6四个path sets并重新generate；不修改/移动/删除immutable SQL。
- Commit paths: scripts/test-generate-archetypes.sh; egon-cola-archetypes/pom.xml;
  egon-cola-archetypes/egon-cola-archetype-{light,light-open,service,service-open,web,web-open}/{pom.xml,src/main/archetype,src/main/javadoc,src/main/resources/META-INF,src/test,root-architecture-docs,.gitignore};
  six-old-archetype-module-non-flyway-path-set
- Commit: refactor(archetypes): replace legacy modules with generated reactor

### Step 7 — 将本地、CI 与 Central 发布入口切换到完整 generated Reactor

- Requirements: REQ-001, REQ-006, REQ-009, REQ-010, REQ-011, REQ-012, REQ-013, REQ-014, REQ-015, REQ-017, REQ-019,
  REQ-020
- Dependencies: Step 6
- Baseline state: local generated profile可构建；deploy wrapper和三个workflow仍发现旧manifest/旧module
  target并在无profile的root/archetypes Reactor执行IT/verify/deploy；README仍展示旧目录。
- Observable outcome: 所有维护/CI/release入口按source verify、generate/check、generated profile IT、release shape、artifact
  allowlist、一次opt-in root deploy顺序执行。
- End state: no-write dry-run可完整验证；真实Central仍仅由用户显式触发；文档只展示source/definitions/generated三边界。
- Test-first gate: Required — 新release shell fixture先断言definitions discovery、generated target paths、profile
  order和单deploy，当前入口返回RED。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-UTIL-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001,
  MC-TEST-001, MC-BLOCKER-001
- Literal Rules: Rule 5, Rule 7, Rule 9, Rule 11
- Ordered files:

#### File 1 — `CREATE scripts/test-archetype-release.sh`

- Purpose: 无Central写入地验证deploy wrapper和workflow不再引用旧module并保持Gate顺序。
- Symbols:
  test_supported_targets、test_mandatory_preflight_order、test_generated_profile_on_verify_and_deploy、test_definition_inventory、test_artifact_allowlist_paths、test_failure_never_reaches_deploy。
- Repository evidence: 当前test-generate release mode仅检查workflow出现source/generator字符串，没有验证profile、paths、顺序或一次deploy。
- Dependencies and consumers: temp fake mvnw/generator/check scripts；读取三workflow文本；CI和Step 7执行。
- Why now: 在修改外部写入口前先定义不可达deploy和路径合同。
- Contract/signature changes: new no-arg shell test；不读取secrets、不联网、不调用真实Maven。
- Input/output and state mapping: fake call log映射source/generate/check/profile/shape/deploy顺序；definition target
  IDs映射.generated target artifact paths。
- Error and edge behavior: injected任一阶段失败后log出现deploy即fail；缺profile、旧manifest path、individual target、source
  artifact或多次deploy均fail。
- Standards impact: MC-REUSE-001、MC-UTIL-001、MC-PATTERN-001、MC-SCOPE-001、MC-TEST-001、MC-BLOCKER-001；隔离外部副作用。
- Literal rule enforcement: Rule 5 仅shell原生工具；Rule 7 验证配置Gate不被跳过；Rule 9 按pipeline stage log断言；Rule 11
  六target来自definition topology。
- Implementation pseudocode:

```bash
copy maven-deploy.sh into temp repository with fake mvnw, generator and check wrapper that append exact calls to a log
invoke --dry-run and assert ordered subsequence: source install, generate, check, generated-archetypes integration-test, release shape
inject nonzero at each stage and assert no logged command contains clean deploy
invoke --publish with a fixed non-SNAPSHOT fake version and assert exactly one root command has both release and generated profiles
scan three workflows for definitions manifest discovery and .generated target paths; reject old module manifest/POM paths
assert supported targets are only all, archetypes and list; source-project artifact IDs never appear in expected deployment allowlist
```

- Verification contribution: TEST-013至TEST-015及REQ-020 fail-closed proof。
- After this file: 当前wrapper/workflows在old paths/profile assertions上RED。

#### File 2 — `MODIFY scripts/maven-deploy.sh`

- Purpose: 保留现有CLI-003但把所有preflight和deploy命令切到generated profile及单bundle合同。
- Symbols: list_targets、project_args、run_preflight、generated profile args、artifact allowlist；remove individual Open
  target branches。
- Repository evidence: 当前脚本已有all/archetypes/three Open targets、source install、generate/check、dry-run、non-SNAPSHOT
  guard和optional deploy。
- Dependencies and consumers: source reactor、CLI-001/CLI-002、archetypes generated profile、root release profile、operator。
- Why now: release fixture已固定顺序，Reactor切换已完成。
- Contract/signature changes: all仍default、archetypes仍可dry-run；删除三个individual public publish
  targets防部分发布；--publish仅允许all；--skip-tests不能跳preflight。
- Input/output and state mapping: source verified state映射full generated set；profile artifacts映射allowlist；all
  publish映射single root Central bundle。
- Error and edge behavior: 任何Gate非零立即return；missing generated/profile、SNAPSHOT、archetypes --publish、unknown remote
  status均拒绝；脚本不自动重试deploy。
- Standards impact: MC-REUSE-001、MC-DEP-001、MC-UTIL-001、MC-PATTERN-001、MC-SCOPE-001、MC-TEST-001；复用现有wrapper。
- Literal rule enforcement: Rule 5只调用现有Maven/shell；Rule 7完整source验证保持profile parity；Rule 9阶段顺序显式；Rule
  11 generated profile包含exact六族。
- Implementation pseudocode:

```bash
supported targets = all, archetypes, list; reject --publish unless target is all
run root and archetypes parent bootstrap, then source-projects clean install
invoke generate_archetypes.sh generate followed by check_archetypes.sh and assert .generated is untracked
invoke archetypes/pom.xml -Pgenerated-archetypes clean integration-test
invoke root pom.xml -Pgenerated-archetypes -Prelease -Dgpg.skip=true clean verify and inspect six child artifact shapes
if dry-run print no deploy; if publish require non-SNAPSHOT and execute exactly one root -Pgenerated-archetypes -Prelease clean deploy
propagate nonzero status immediately and print reconciliation guidance rather than retrying an unknown Central outcome
```

- Verification contribution: CLI-003 local ordering、target restrictions和single deploy。
- After this file: local wrapper对新Reactor GREEN；workflow/docs仍旧路径。

#### File 3 — `MODIFY .github/workflows/{ci.yaml,ci_java_compatibility.yaml,publish-maven-central.yml}`

- Purpose: 让普通CI、Java兼容矩阵和受保护Central workflow统一消费definitions/generated profile。
- Symbols: source-to-archetype preflight steps；MANIFESTS definitions discovery；MODULE_DIR .generated child；Maven
  profiles；signed attachment scan；single deploy。
- Repository evidence: 当前三个workflow已包含source/generate基础，但find旧src/main/archetype manifests、检查旧module
  POM/target且Maven命令未启用generated profile。
- Dependencies and consumers: GitHub runners、Maven wrapper、Step 7 Files 1-2脚本；真实secrets仅publish job。
- Why now: local contract已GREEN，workflow可映射同一顺序而不发明第二流程。
- Contract/signature changes: CI内部路径/profile改变；publish input仍all/dry_run/confirm/skip_tests；public artifacts不变。
- Input/output and state mapping: definitions manifest targetArtifactId映射.generated child和consumer GAV；source
  install提供本地Egon deps；profile install/verify提供Archetype artifact。
- Error and edge behavior: inventory mismatch、generated tracked、IT/consumer/attachment/signature缺失均在deploy前fail；publish
  timeout保留deployment id并禁止盲重试。
- Standards impact: MC-ARCH-001、MC-REUSE-001、MC-CONFIG-001、MC-PATTERN-001、MC-SCOPE-001、MC-TEST-001；不新增Action/dependency。
- Literal rule enforcement: Rule 5 复用现有Actions/Maven；Rule 7 六generated consumer配置测试保持；Rule 9 同一pipeline；Rule
  11 动态inventory仍逐族验证exact topology。
- Implementation pseudocode:

```yaml
all workflows install root/archetypes parents, verify source-projects, generate full reactor and invoke check_archetypes.sh
replace manifest search with definitions/egon-cola-archetype-{light,light-open,service,service-open,web,web-open}/archetype.properties and validate six unique targets
run archetypes or root Maven commands with -Pgenerated-archetypes whenever public Archetype children are required
compatibility workflow installs generated artifacts before archetype:generate, then preserves existing Java matrix consumer verify
release shape scans .generated/${TARGET_ARTIFACT_ID}/target for main,sources,javadoc and required signatures
publish workflow retains branch/version/secret guards and performs one root -Pgenerated-archetypes -Prelease clean deploy only
```

- Verification contribution: CI static contract、consumer matrix、release shape和Central unreachable-on-failure。
- After this file: 三workflow都指向最终结构；真实运行仍由GitHub/user控制。

#### File 4 — `MODIFY scripts/maven-deploy.md,README.md,README.zh-CN.md`

- Purpose: 同步维护者、发布者和消费者看到的目录、命令、Spring策略及Flyway baseline语义。
- Symbols: source/definitions/.generated tree；generate/check/profile commands；one-bundle release；Spring Boot管理表；B
  migration说明；Open manual SQL边界。
- Repository evidence: 当前文档仍展示六旧module目录、individual target dry-run和无generated profile的Maven命令。
- Dependencies and consumers: 只引用Files 2-3最终CLI/workflow；不成为构建输入。
- Why now: 代码和workflow终态确定后再写文档，避免记录过渡路径。
- Contract/signature changes: consumer GAV/archetype:generate命令保持；maintainer/release命令更新；明确旧V archive不应编辑。
- Input/output and state mapping: source edits经generate映射public child；Spring Parent/BOM来源映射模块类型；新/旧Flyway环境映射B/V选择。
- Error and edge behavior: 文档禁止手改.generated、禁止发布source GAV、禁止删除archive V、禁止对unknown
  Central状态重发；不承诺未运行live验证。
- Standards impact: MC-CONFIG-001、MC-SCOPE-001、MC-TEST-001；记录Rule 7和release边界。
- Literal rule enforcement: Rule 5 命令只使用仓库wrapper/scripts；Rule 7 说明profile keys不随baseline变化；Rule 11
  目录图只展示exact六Archetype family。
- Implementation pseudocode:

```markdown
replace old six-module tree with source-projects as editable truth, definitions as curated contracts and .generated as ignored reactor
document generate_archetypes.sh generate, check_archetypes.sh and -Pgenerated-archetypes second Maven invocation
state root Boot BOM versus source Boot Parent policy and list Flyway/PostgreSQL compatibility exceptions
explain B migration: one master and one shard baseline per legacy project, old V history immutable, Open manual SQL unchanged
retain all six public archetype:generate examples and GAVs while removing individual publish target instructions
describe dry-run, signed attachment allowlist, single all publish and unknown-status reconciliation without claiming live success
```

- Verification contribution: doc grep确保无旧module构建命令、无错误Spring/Flyway声明，consumer示例仍六个。
- After this file: repository文档与CLI/workflow/Spec一致。

- Validation working directory: /Users/mario/SelfProject/Egon-COLA
- Verification command: bash -n scripts/test-archetype-release.sh scripts/maven-deploy.sh scripts/generate_archetypes.sh
  scripts/check_archetypes.sh && ./scripts/test-archetype-release.sh &&
  ./scripts/test-spring-dependency-management.sh && ./scripts/test-generate-archetypes.sh unit &&
  ./scripts/test-generate-archetypes.sh package reactor && ./scripts/maven-deploy.sh --dry-run && git diff --check
- Expected result: all shell/static gates exit 0；dry-run完成source、generation、six IT和release shape但日志无clean
  deploy/remote write；docs无旧命令。
- Failure returns to: File 1 fixture、File 2 local order、File 3 workflow mapping或File 4文档；构建回归返回对应Step 1/5/6。
- Completion criteria: 所有入口只消费正常source和完整generated Reactor，发布仅all单bundle且任一Gate失败不deploy。
- Rollback: 回退Step 7脚本/workflows/docs；若已发生Central写入不能回滚同版本，只能核对deployment并发新版本forward-fix。
- Commit paths: scripts/test-archetype-release.sh; scripts/maven-deploy.sh;
  .github/workflows/{ci.yaml,ci_java_compatibility.yaml,publish-maven-central.yml};
  scripts/maven-deploy.md,README.md,README.zh-CN.md
- Commit: ci(archetypes): publish generated reactor through one gated bundle

## 8. Test, Validation, and Quality Gates

| Gate/order              | Working directory         | Command or method                                                                                                                                                          | Scope                    | Expected result                                                                                    | Failure returns to            | Requirements/runtime boundary       |
|-------------------------|---------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------|----------------------------------------------------------------------------------------------------|-------------------------------|-------------------------------------|
| 1 RED Spring            | repository root           | bash scripts/test-spring-dependency-management.sh before POM edits                                                                                                         | all Spring-related POMs  | nonzero for duplicate Boot BOM and web 2.8.13                                                      | Step 1 File 1 if wrong reason | REQ-012至REQ-015; static             |
| 2 GREEN Spring          | repository root           | bash scripts/test-spring-dependency-management.sh                                                                                                                          | all POM policy           | exit 0 and exact ownership report                                                                  | Step 1 Files 2-3              | REQ-012至REQ-015; static             |
| 3 RED Light             | source-light root         | targeted FlywayMigrationConventionTest,LogicalSchemaParityTest                                                                                                             | Light migration          | missing B resource assertion                                                                       | Step 2 File 1                 | REQ-016至REQ-018; H2                 |
| 4 GREEN Light           | repository root           | Step 2 exact Maven command                                                                                                                                                 | Light B/V                | exit 0 and schema parity                                                                           | Step 2 File 2                 | REQ-016至REQ-018; H2                 |
| 5 RED Service           | service source root       | targeted convention/Evaluation tests                                                                                                                                       | Service migration        | missing B resource assertion                                                                       | Step 3 File 1                 | REQ-016至REQ-018; H2                 |
| 6 GREEN Service         | repository root           | Step 3 exact Maven command                                                                                                                                                 | Service B/V              | exit 0, one/eight tables and parity                                                                | Step 3 File 2                 | REQ-016至REQ-018; H2                 |
| 7 RED Web               | web source root           | targeted three tests                                                                                                                                                       | Web migration            | missing B resource assertion                                                                       | Step 4 File 1                 | REQ-016至REQ-018; H2                 |
| 8 GREEN Web             | repository root           | Step 4 exact Maven command                                                                                                                                                 | Web B/V                  | exit 0, seed/FK/schema parity                                                                      | Step 4 File 2                 | REQ-016至REQ-018; H2                 |
| 9 RED generator         | repository root           | bash scripts/test-generate-archetypes.sh unit before generator edits                                                                                                       | definitions/full reactor | fails old discovery or missing child layout                                                        | Step 5 File 1                 | REQ-003至REQ-008; isolated fixture   |
| 10 GREEN generator      | repository root           | Step 5 exact shell/Maven chain                                                                                                                                             | generator+generated IT   | exit 0; six complete children                                                                      | Step 5 Files 2-6              | REQ-003至REQ-009; local Maven        |
| 11 RED cutover          | repository root           | bash scripts/test-generate-archetypes.sh package reactor before cutover                                                                                                    | module/archive tree      | old six modules reported                                                                           | Step 6 File 1                 | REQ-002, REQ-017; static            |
| 12 GREEN cutover        | repository root           | Step 6 exact command                                                                                                                                                       | default/profile reactor  | default works without generated; profile IT passes                                                 | Step 6 Files 2-4              | REQ-002至REQ-010; local Maven        |
| 13 RED release          | repository root           | bash scripts/test-archetype-release.sh before entry edits                                                                                                                  | local/workflow release   | old paths/profile/order fail                                                                       | Step 7 File 1                 | REQ-010, REQ-011, REQ-020; fixture  |
| 14 GREEN release        | repository root           | Step 7 exact command                                                                                                                                                       | dry-run release          | exit 0; no deploy command                                                                          | Step 7 Files 2-4              | REQ-001至REQ-020; no remote          |
| 15 Final source         | repository root           | ./mvnw -B -ntp -f egon-cola-archetypes/source-projects/pom.xml clean install                                                                                               | six source trees         | all modules pass or exact unrelated baseline recorded and task stops                               | owning earlier Step           | REQ-001, REQ-012至REQ-019; local     |
| 16 Final generated      | repository root           | ./scripts/generate_archetypes.sh generate && ./scripts/check_archetypes.sh && ./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -Pgenerated-archetypes clean integration-test | six public archetypes    | exit 0 and six basic IT pass                                                                       | Steps 2-6                     | REQ-003至REQ-020; local              |
| 17 Final release shape  | repository root           | ./mvnw -B -ntp -f pom.xml -Pgenerated-archetypes -Prelease -Dgpg.skip=true clean verify                                                                                    | root bundle shape        | exit 0; main/source/javadoc present; no source GAV                                                 | Steps 1, 5-7                  | REQ-009至REQ-020; no Central         |
| 18 Static final         | repository root           | git diff --check && git status --short && git ls-files egon-cola-archetypes/.generated                                                                                     | scope/generated policy   | no whitespace error; generated output list empty; only authorized paths plus preserved tsbuildinfo | owning Step                   | all; static                         |
| 19 User-controlled live | GitHub protected workflow | manually dispatch all publish after Plan execution review                                                                                                                  | GPG and Central          | published deployment id and six resolvable GAVs                                                    | Step 7/operator               | REQ-011; not executed automatically |

Plan编写阶段只运行Spec/Plan validator和静态Git检查；上表Maven/JUnit/fixture命令是未来Step执行门禁，不在本轮声称通过。

## 9. Migration, Compatibility, Rollout, and Rollback

数据库不新增业务schema变更；六个B文件是三个项目、两个物理role的累计最终态入口，属于用户显式批准的六文件
分解。每个B版本等于其role最高V版本，且直接CREATE最终表/约束/index/seed。旧V不修改；fresh database由Flyway
选最新B并忽略低于/等于该baseline所覆盖的V，已有schema history忽略B并继续validate原V。H2 parity是本地证据，
真实PostgreSQL验证若需要由用户另行授权，不能用作删除历史V的理由。

结构 rollout必须按Steps 1至7顺序。Step 5 definitions/generated替代通过前不能删旧内容；Step 6切换后default
fresh checkout无需.generated，release/CI必须先生成再启profile。公开GAV、consumer参数、metadata、module topology
保持兼容；source GAV永不发布。Git级回滚可按每Step精确revert并重生成.generated。数据库B尚未发布时可删新增B回滚；
发布或被数据库应用后不得改B checksum，只能增加新的forward migration。Central发布不可覆盖/删除，同版本未知状态
必须先查询deployment，不能自动重发。

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Effective Spec section | Steps         | Files                            | Tests/gates         | Completion evidence       |
|-------------|------------------------|---------------|----------------------------------|---------------------|---------------------------|
| REQ-001     | Spec §4                | 1, 5, 7       | source POMs, generator, docs     | Gates 2, 15-17      | source可开发且仅一事实源           |
| REQ-002     | Spec §4                | 6             | archetypes POM, old path sets    | Gates 11-12, 18     | 旧POM/业务副本缺席               |
| REQ-003     | Spec §4                | 5, 6          | definitions, old curated deletes | Gates 9-12          | definitions完整且唯一          |
| REQ-004     | Spec §4                | 5             | generator                        | Gates 9-10          | plugin 3.4.1日志            |
| REQ-005     | Spec §4                | 5, 6          | generated reactor, profile       | Gates 10, 12, 16    | aggregator+six child可构建   |
| REQ-006     | Spec §4                | 5, 7          | check wrapper, workflows         | Gates 10, 14, 18    | ignored、deterministic     |
| REQ-007     | Spec §4                | 5             | definitions manifests, generator | Gates 9-10          | 一一动态inventory             |
| REQ-008     | Spec §4                | 5             | generator/tests                  | Gates 9-10          | lock/staging/atomic tests |
| REQ-009     | Spec §4                | 5, 6, 7       | generated contracts/CI           | Gates 10, 12, 16-17 | 六IT和consumer verify       |
| REQ-010     | Spec §4                | 6, 7          | profile/release allowlist        | Gates 12, 14, 17    | 无source artifacts         |
| REQ-011     | Spec §4                | 7             | deploy script/workflow           | Gates 13-14, 19     | exactly one root deploy   |
| REQ-012     | Spec §4                | 1, 7          | root/child POMs                  | Gates 1-2, 17       | root Boot BOM唯一           |
| REQ-013     | Spec §4                | 1, 7          | six source POMs                  | Gates 1-2, 15       | Parent only               |
| REQ-014     | Spec §4                | 1, 7          | root exceptions/child cleanup    | Gates 1-2, 17       | resolved versions未漂移      |
| REQ-015     | Spec §4                | 1, 7          | four source Springdoc BOMs       | Gates 1-2, 15       | Springdoc 2.8.17          |
| REQ-016     | Spec §4/§11            | 2, 3, 4, 5    | six B SQL/tests                  | Gates 3-10, 16      | per-role B/V parity       |
| REQ-017     | Spec §4/§11            | 2, 3, 4, 6, 7 | tests/archive/static gates       | Gates 3-18          | 24 V paths/hashes不变       |
| REQ-018     | Spec §4/§5             | 2, 3, 4, 5    | B files and approved flow        | Gates 3-8           | 只实施已确认B方案                 |
| REQ-019     | Spec §4                | 5, 6, 7       | Open source/generated/docs       | Gates 10, 12, 16    | Open manual SQL不变         |
| REQ-020     | Spec §4                | 1-7           | every Step gate/release wrapper  | Gates 1-19          | 任一失败无deploy               |

## 11. Risks, Blockers, and User Decisions

| ID        | Risk or decision                        | Impacted Steps/files  | Evidence                                       | Owner            | Status/action                                                                   |
|-----------|-----------------------------------------|-----------------------|------------------------------------------------|------------------|---------------------------------------------------------------------------------|
| BLOCK-001 | 每project按role两个B而非跨role一个文件             | Steps 2-4             | Spec DEC-005, user confirmation                | User             | Closed: two role baselines approved                                             |
| BLOCK-002 | old module SQL不能删除导致目录物理保留              | Step 6                | Spec DEC-006, immutable rule                   | User             | Closed: archive-only original paths approved                                    |
| BLOCK-003 | Open manual SQL范围                       | Steps 5-7             | Spec DEC-008                                   | User             | Closed: excluded                                                                |
| RISK-001  | B SQL漏掉V链最终column/index/constraint/seed | Steps 2-4             | current migrations contain init+ALTER chain    | Implementer      | Mitigate with B/V metadata and negative parity tests                            |
| RISK-002  | H2与真实PostgreSQL方言差异                     | Steps 2-4             | tests currently use H2 PostgreSQL mode         | User/Implementer | Local gate only; optional PostgreSQL validation requires explicit authorization |
| RISK-003  | AccessGuardProperties既有全Reactor测试基线可能失败 | Steps 1, 7 final gate | prior source install evidence; current turn未重跑 | Implementer      | Reproduce first; do not edit unrelated module under this Plan                   |
| RISK-004  | generated aggregator意外进入Central         | Steps 5-7             | new private aggregator                         | Implementer      | deploy skip plus artifact allowlist                                             |
| RISK-005  | Central远端状态未知                           | Step 7 live gate      | immutable release semantics                    | Release operator | No automatic retry; reconcile deployment id                                     |
| RISK-006  | definitions复制后contract drift            | Steps 5-6             | current assets differ byfamily                 | Implementer      | initial SHA parity plus generator/IT tests                                      |

无未关闭的Spec或用户决策 blocker。RISK-003若在执行中复现，会阻断当前Step的全量验证并单独报告，但不授权跨scope修复。

## 12. Review and Acceptance

### 12.1 Original requirement fidelity

七Steps覆盖正常源码唯一事实源、旧Maven模块退出、plugin生成完整Archetype Reactor、Central单bundle、
Spring Parent/BOM治理和已确认的per-role Flyway baseline。旧V原路径例外与Open手工SQL排除均按用户确认保留，
没有把“删除旧模块”扩成删除不可变历史。

### 12.2 Spec consistency

Plan逐项实现Accepted Spec DEC-001至DEC-008，没有新增业务API/model/table/page/cache/job/dependency。
PLAN-CLAR-001复用现有release wrapper，PLAN-CLAR-002只消除版本双源，PLAN-CLAR-003只定义测试隔离技术。
Simplicity audit未发现fetch-then-forward或可由直接路径替代的新接口。

### 12.3 Repository executability

所有MODIFY路径和主要symbols已在main 9566c9679复核；CREATE路径当前不存在；DELETE只在generated替代
通过后执行且以Git tracked allowlist保护。每Step有RED、GREEN、working directory、exact command、failure return、
rollback、commit paths和单独commit。existing tsbuildinfo不在任何scope。

### 12.4 Test and release completeness

测试从POM静态策略、三族migration parity、generator fixture、Maven profile IT、consumer verify到release shape
逐层升级。真实PostgreSQL、GPG validity和Central publish明确为用户控制边界；Plan不把H2/静态workflow检查描述为
live成功。

### 12.5 Blocking Manual Check

| Check ID       | Applicability  | Status | Evidence                                                  | Finding                                                                            | Required action/exception |
|----------------|----------------|--------|-----------------------------------------------------------|------------------------------------------------------------------------------------|---------------------------|
| MC-ARCH-001    | Applicable     | PASS   | §4.7, §5, Steps 2-6                                       | exact Light/Service/Web/Open profiles保留，无第三架构                                      | None                      |
| MC-REUSE-001   | Applicable     | PASS   | capability ledger和current generator/Flyway tests          | 复用Boot/Flyway/plugin/existing scripts                                              | None                      |
| MC-DEP-001     | Applicable     | PASS   | Step 1 POM diff design；Steps 2-7无新dependency              | 只改变existing management ownership                                                   | None                      |
| MC-NAME-001    | Not applicable | N/A    | 无新增production Java type；只改existing test类                  | POJO语义命名不受影响                                                                       | Evidence retained         |
| MC-VALID-001   | Not applicable | N/A    | business handoff/API不变                                    | 无Validation/groups/telephone变化                                                     | Evidence retained         |
| MC-MODEL-001   | Not applicable | N/A    | no model/converter files in §5                            | Record/Lombok构造不受影响                                                                | Evidence retained         |
| MC-CONVERT-001 | Not applicable | N/A    | no mapping files or new converter                         | BaseConverter/MapStruct不受影响                                                        | Evidence retained         |
| MC-LOG-001     | Not applicable | N/A    | no concrete business class changes                        | Slf4j规则不受影响                                                                        | Evidence retained         |
| MC-BEAN-001    | Not applicable | N/A    | no Bean/lombok.config changes                             | Bean names/Qualifier/injection不受影响                                                 | Evidence retained         |
| MC-UTIL-001    | Applicable     | PASS   | Steps 1, 5-7 shell pseudocode                             | 仅Bash/JDK/Maven现有工具，无utility dependency                                            | None                      |
| MC-JSON-001    | Not applicable | N/A    | external DTO/API unchanged                                | Spring Boot Jackson合同不变                                                            | Evidence retained         |
| MC-TIME-001    | Not applicable | N/A    | no Java time field/import changes                         | SQL timestamp保持existing final schema                                               | Evidence retained         |
| MC-CONFIG-001  | Applicable     | PASS   | Rule 7 matrix, Steps 2-4 parity and Step 7 consumer gates | B baseline不改location或profile key                                                   | None                      |
| MC-PATTERN-001 | Applicable     | PASS   | Spec §13 and Step 5 pipeline stages                       | complex build flow uses data-driven Pipeline/staging, no hardcoded family branches | None                      |
| MC-SCOPE-001   | Applicable     | PASS   | §5 inventory, Step commit paths, preserved tsbuildinfo    | no unrelated refactor or destructive migration edit                                | None                      |
| MC-TEST-001    | Applicable     | PASS   | §8 Gates 1-19 and per-file contributions                  | every applicable standard/requirement has exact future proof                       | None                      |
| MC-BLOCKER-001 | Applicable     | PASS   | §11 BLOCK-001至003 closed; all rows PASS/N/A               | no unresolved Spec/user/manual blocker                                             | None                      |

### 12.6 Final verdict

PASS — Ready for user review
