# DDL 后端生成器 FreeMarker 实施 Plan

| Field | Value |
| --- | --- |
| Document | `2026-09-22-15-47-ddl-code-generator-freemarker-implementation.md` |
| Template Version | `4` |
| Status | `Review` |
| Created | `2026-09-22 15:47 Asia/Shanghai` |
| Updated | `2026-09-22 15:47 Asia/Shanghai` |
| Owner | Mario |
| Repository | Egon-COLA |
| Scope | 内部 code-generator 工具、native Light/Web/Service 后端模板与 skill 接线 |
| Source Requirement | 用户要求完善方案并写 Plan；最终纠正为 FreeMarker，明确替代 Velocity |
| Baseline Revision | `78675cf732d8d0b1c4cf982d5e21b9202b7e232a`；沿用本任务未提交 docs/skills，不纳入未来代码提交 |
| Implements Spec | [生成器设计 R3](../reports/2026-09-22-ddl-backend-code-generator-proposal.md) |
| Spec Status | `Review` |
| Spec Revision | R3 — FreeMarker 与执行契约完善；本轮方案头部 Updated |
| Effective Specs | [生成器设计 R3](../reports/2026-09-22-ddl-backend-code-generator-proposal.md) |
| Depends On Plans | `None` |
| Supersedes | `None` |
| Superseded By | `None` |
| Related Plans | `None` |

## 1. Summary

实现一个普通 Java CLI 工具组件，使用获准 FreeMarker 2.3.35，把离线 PostgreSQL DDL/MP-SDJ Manifest 转为规范化逻辑 Schema，按明确产物与目标项目布局生成后端 CRUD。八个依次执行的逻辑任务，每个最多一个聚焦提交；当前只完成 Plan，不创建模块、不修改 POM、不安装依赖、不运行应用。

生成器不是数据库迁移器，不连接数据库；生成工程不依赖 FreeMarker。DDL/模板/磁盘基线发生变化时先生成可审核计划，再应用获准变化，保留人工代码和未选中产物状态。已有 basic CRUD 由 EgonColaRepository 承担，模板只生成必要边界与查询SQL。

## 2. Target Spec and Effective Design

### 2.1 Primary target

- Path: [生成器设计 R3](../reports/2026-09-22-ddl-backend-code-generator-proposal.md)，本轮唯一设计依据，按可识别 legacy Spec 进入规划。
- Status: Review；用户同意总体方向并明确选择 FreeMarker，完善后的文件与 Plan 尚未批准执行。
- Revision: R3。最终“切换到 FreeMarker，不是 Velocity”优先；先前 Velocity/JDK文本替换方案已失效。
- Approval evidence: 用户要求“完善一下方案，然后开始写plan”，足以授权本 Review Plan；不据此标 Ready 或启动实现。

### 2.2 Effective Spec set

| Role | Spec/link | Status/revision | Effective sections | Why included |
| --- | --- | --- | --- | --- |
| Primary | [生成器设计](../reports/2026-09-22-ddl-backend-code-generator-proposal.md) | Review / R3 | §1–14 | 覆盖范围、API/文件状态、依赖例外与验收 |

### 2.3 Superseded or excluded content

排除 Velocity、JDK自制模板引擎、Agent/Open/传统三层模板、前端、平台代码改造、新RPC/GraphQL/MQ接口、自动运行DDL、自动升级依赖。旧项目中的人工源码、历史 SQL、已有生成器以外的未提交工作均不是本 Plan 的改动目标。

## 3. Effective Requirements and Acceptance

以下 PLAN-REQ 是对既有方案陈述的追踪别名，不新增产品需求。

| Requirement | Source Spec section | Effective statement | Observable acceptance | Implementation impact |
| --- | --- | --- | --- | --- |
| `PLAN-REQ-001` | §3、§7 | native Light/Web/Service，精确目录与产物选择；无 Agent/前端/Open 隐式适配 | 三种布局与单产物测试，Service 拒绝 Controller，未选路径不变 | 对应 Step 与文件见§5/§10 |
| `PLAN-REQ-002` | §5、§14.1 | 只新增已批准 FreeMarker，工具与业务依赖隔离 | 依赖清单除 FreeMarker/已列内部组件无新增外部工件，目标 POM 不变 | 对应 Step 与文件见§5/§10 |
| `PLAN-REQ-003` | §6、§14.2 | 离线 schema/manifest、固定 role DO、版本不可变前缀及逻辑分片模型 | 实际 native DDL 解析；未知语法与历史漂移失败，不运行 SQL | 对应 Step 与文件见§5/§10 |
| `PLAN-REQ-004` | §4、§14.1 | FreeMarker 2.3.35、受控 .ftl、方括号语法与确定性 | 绑定符原样、缺字段失败、UTF-8/LF 与重复输出一致 | 对应 Step 与文件见§5/§10 |
| `PLAN-REQ-005` | §7、§8 | PO/DAO/XML/Repo 符合 MP-SDJ，继承 CRUD、显式查询 | 必需 XML 方法可执行、class/Builder/租户/version/路由/唯一键核对 | 对应 Step 与文件见§5/§10 |
| `PLAN-REQ-006` | §8、§14.3 | Domain/Application 基础 CRUD 与可选 HTTP，真实依赖与字段策略 | 生成代码编译、版本冲突、字段暴露、分页/错误/认证边界有测试 | 对应 Step 与文件见§5/§10 |
| `PLAN-REQ-007` | §9、§14.4 | Schema diff、文件三方判定、局部产物版本与兼容影响 | 人工文件不覆盖，部分生成不把未选消费者标成同步 | 对应 Step 与文件见§5/§10 |
| `PLAN-REQ-008` | §9、§14.4 | 计划复核、路径保护、锁与日志恢复、精确破坏性授权 | 过期计划失败，写入故障可恢复，恢复不覆盖新人工编辑 | 对应 Step 与文件见§5/§10 |
| `PLAN-REQ-009` | §10、§14.5 | CLI JSON/stdout、stderr 日志、既定退出码与离线 launcher | templates/plan/apply/check/recover 协议测试、无自动下载或服务启动 | 对应 Step 与文件见§5/§10 |
| `PLAN-REQ-010` | §11 | Spec/Plan/Execute 调用实际生成器，模型只写配置和自定义逻辑 | 工具发现、缺依赖阻断、已批准 FreeMarker 不重复询问 | 对应 Step 与文件见§5/§10 |
| `PLAN-REQ-011` | §12、§14.6 | 解析、模板、编译、真实源项目临时副本和安全重生成分层验收 | 每个证据分层独立，未运行的 PostgreSQL/MQ 不冒充通过 | 对应 Step 与文件见§5/§10 |

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

先固定 module/config/Schema/Plan 类型，再证明实际 DDL 子集，再实现 FreeMarker 渲染。以解析模型和渲染上下文为前置完成持久化与上层模板，再添加有内容可比较的 plan/apply 状态，最后接 CLI 与真实源工程验收。单个步骤的失败停在该步骤；解析失败不能继续产出错误模板。

### 4.2 Test-first strategy

每一步先添加能证明新行为缺失的测试，再写实现。Step 1 的聚合/模块POM是运行新测试必需的编译前置，不算行为实现；必要方法只建立签名并抛 UnsupportedOperationException，让测试因行为缺失 RED，不把 javac/依赖下载失败叫作 RED。后续步骤遵循同一原则，不先补完生产实现再追加断言。

### 4.3 Sequential and parallel boundaries

全部串行执行，无子任务并行授权。Step N 依赖前一 Step 的验证与提交，模板/元数据多处共享，不能并行编辑同一工具模块。

### 4.4 Commit boundaries

每 Step 一个逻辑任务、最多一个提交；提交范围为该 Step 的准确文件，不提交 target/临时生成产物。用户未要求本轮提交文档，因此本轮不提交。未来执行前记录新的 HEAD 和 dirty 路径，不能把本轮已存在文档/skill变更一并打进代码提交。

### 4.5 Spec Simplicity and Implementation-necessity Audit

| Spec element | Spec necessity verdict/section | Current repository evidence | Direct/reuse alternative | Interaction/implementation cost | Plan decision |
| --- | --- | --- | --- | --- | --- |
| 独立工具模块 | §4、§14.1 | Components 既有功能模块结构，未发现该CLI | 不把生成器塞进应用启动 | 一个main，无服务部署 | Implement |
| FreeMarker | §4、§14.1 用户最终明确选择 | 当前目标无引擎声明；官方2.3.35有方括号语法 | 不自研模板DSL | 唯一批准外部工件，工具scope | Implement |
| DDL Adapter | §6、§14.2 | MP-SDJ已持有JSQLParser，Runner没有Schema导出 | 不执行SQL/不导入外部生成器 | 一个AST隔离边界+固定分支识别 | Implement |
| 文件manifest与journal | §9、§14.4 | 仓库生成脚本已有原子暂存/hash意识 | 不强行Java AST合并 | 可恢复文件状态，没有数据库表 | Implement |
| 基础CRUD | §8 | EgonColaRepository已有final方法 | 继承组件，只补named SQL与契约装配 | 无重复ServiceImpl CRUD引擎 | Already exists / templates only |
| Facade协议、缓存、Event | §3、§8 | 当前都需要业务配置和外部依赖 | 不从DDL猜测 | 避免虚构新协议/权限/部署 | Excluded |

### 4.6 Change-unit Dependency Matrix

| Change unit | Requirements | Proof/RED point | Compile/runtime prerequisites | Produces | Consumers/unblocks | Owning Step |
| --- | --- | --- | --- | --- | --- | --- |
| 工具模块、依赖与生成请求契约 | PLAN-REQ-001, PLAN-REQ-002 | CodegenContractTest | 现有Components/Java21 | 工具编译基线、FreeMarker 唯一外部新增和可验证请求契约。 | 下一Step及CLI验收 | Step 1 |
| 离线 DDL 与 Manifest 解析 | PLAN-REQ-003 | PostgreDdlAdapterTest | Step 1 已验证提交 | 完整输入与增量历史均得到可追踪 Schema，角色分支和物理表正确归并。 | 下一Step及CLI验收 | Step 2 |
| FreeMarker 渲染边界 | PLAN-REQ-002, PLAN-REQ-004 | FreeMarkerTemplateServiceTest | Step 2 已验证提交 | 获准引擎按方括号语法稳定渲染，模板失败不会产生可应用代码。 | 下一Step及CLI验收 | Step 3 |
| 布局与持久化四件套 | PLAN-REQ-001, PLAN-REQ-005 | PersistenceTemplateTest | Step 3 已验证提交 | 只生成选中的 PO/DAO/XML/Repo，保留 MP-SDJ真实CRUD契约。 | 下一Step及CLI验收 | Step 4 |
| 领域与应用基础 CRUD 模板 | PLAN-REQ-006 | BackendCrudTemplateTest | Step 4 已验证提交 | 在真实领域端口边界生成完整基础CRUD；仅Light/Web具备已配置HTTP入口。 | 下一Step及CLI验收 | Step 5 |
| 生成计划、冲突与安全应用 | PLAN-REQ-007, PLAN-REQ-008 | GenerationUpdateTest | Step 5 已验证提交 | 三方文件判定、逐产物版本和可恢复应用闭合，不覆盖人工修改。 | 下一Step及CLI验收 | Step 6 |
| CLI、启动脚本与工具说明 | PLAN-REQ-009 | CodegenCommandTest | Step 6 已验证提交 | 真实工具入口返回结构化摘要和稳定退出码，无自动下载或服务启动。 | 下一Step及CLI验收 | Step 7 |
| 生成产物验收与 skill 实际接线 | PLAN-REQ-010, PLAN-REQ-011 | CodegenAcceptanceTest | Step 7 已验证提交 | 模板编译、三种真实源工程临时副本与skill全链路验证，保留运行证明边界。 | 下一Step及CLI验收 | Step 8 |

### 4.7 Java, Spring, and Egon-COLA Implementation Standards

| Concern | Current repository evidence | Effective Spec decision | Planned implementation consequence | Owning Steps/checks |
| --- | --- | --- | --- | --- |
| Architecture profile | source-projects/light、web、service 各POM与领域目录 | §3、§14.1/3 | 产物严格选定native profile；工具沿用Components功能包，非新应用架构 | 1–8 / MC-ARCH-001 |
| Reuse/capability | EgonModel、EgonColaRepository/Mapper、Common Converter/PageSlice、MP-SDJ parser | §2、§5 | 不复制CRUD、不引入另一个ORM；MP依赖只作为工具读契约 | 1–5 / MC-REUSE-001 |
| Models/Validation | Common BaseConverter、ValidationUtils、native lombok.config | §8 | class + Builder/SuperBuilder按父类选择，跨边界注解与分组，生成转换符合Common契约 | 1、4、5 |
| Scope/security | 用户禁止未授权新增依赖/自动启动 | §5、§14 | 工具不启Spring、不执行DDL；只写所选路径 | 全部 |

| Need | Candidates inspected | Exact evidence | Fit/gap | Decision | Owning Step |
| --- | --- | --- | --- | --- | --- |
| 模板 | FreeMarker/自研/Velocity | 官方FreeMarker下载、替代语法文档；用户最终纠正 | FreeMarker获批，其他选型已排除 | 2.3.35管理在Components父POM | 1、3 |
| DDL | MP-SDJ现有JSQLParser、Runner | MP-SDJ pom.xml、ddl/EgonColaDdlManifestBO.java | AST覆盖待实际测试；不是新增依赖授权 | 先固定支持子集测试，不通过则停止 | 2 |
| JSON | Common cache提供的Jackson | MP-SDJ→cache POM runtime jackson-databind/jsr310 | 工具可复用；目标项目仍需独立核验 | 严格JSON和稳定排序，不装替代库 | 1、6、7 |
| CRUD | EgonColaRepository/EgonColaMapper | extension/*.java与native UserDAO.xml | 基础已有，SQL契约由业务Mapper承担 | 只生成必要模板 | 4、5 |
| 编译/测试 | JavaCompiler、Common Test、mvnw | 当前Java21/处理器配置与脚本 | 无需新增编译器/插件框架 | 本地编译与临时source项目 | 8 |

### 4.8 User-mandated Java Rule Implementation Matrix

| Literal rule | Spec source | Repository evidence | Exact files and order | Pseudocode obligations | Validation gate | Steps | Status/blocker |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | §8、§14 | Components/native source与当前skill规范 | §5精确文件及§7顺序 | BO/Query/Command/Result/DAO等语义名；CLI为明确行为入口 | 各Step聚焦测试与§8验收 | 1–8 | PASS |
| Rule 2 | §8、§14 | Components/native source与当前skill规范 | §5精确文件及§7顺序 | config注解/分组及生成业务@Valid/@Validated，工具手工触发非代理校验 | 各Step聚焦测试与§8验收 | 1–8 | PASS |
| Rule 3 | §8、§14 | Components/native source与当前skill规范 | §5精确文件及§7顺序 | 普通class与父类相适配Builder；公共Converter与类型字段映射 | 各Step聚焦测试与§8验收 | 1–8 | PASS |
| Rule 4 | §8、§14 | Components/native source与当前skill规范 | §5精确文件及§7顺序 | 工具普通Java构造装配；产物显式Bean/Slf4j/RequiredArgsConstructor/Qualifier | 各Step聚焦测试与§8验收 | 1–8 | PASS |
| Rule 5 | §8、§14 | Components/native source与当前skill规范 | §5精确文件及§7顺序 | JDK/已管理工具；FreeMarker是本任务明确批准模板引擎例外 | 各Step聚焦测试与§8验收 | 1–8 | PASS |
| Rule 6 | §8、§14 | Components/native source与当前skill规范 | §5精确文件及§7顺序 | Jackson规范化配置/计划；枚举JSON编码明确，无额外持久化枚举假设 | 各Step聚焦测试与§8验收 | 1–8 | PASS |
| Rule 7 | §8、§14 | Components/native source与当前skill规范 | §5精确文件及§7顺序 | 工具无Spring profiles；不修改业务profile；生成代码缺配置时阻断 | 各Step聚焦测试与§8验收 | 1–8 | PASS |
| Rule 9 | §8、§14 | Components/native source与当前skill规范 | §5精确文件及§7顺序 | Adapter隔离AST、Strategy区分布局；其他直接实现不再加注册框架 | 各Step聚焦测试与§8验收 | 1–8 | PASS |
| Rule 10 | §8、§14 | Components/native source与当前skill规范 | §5精确文件及§7顺序 | java.time、UTC/LF与确定性文本；无生成时刻噪声 | 各Step聚焦测试与§8验收 | 1–8 | PASS |
| Rule 11 | §8、§14 | Components/native source与当前skill规范 | §5精确文件及§7顺序 | 产物native light/web/service保层次；工具是既有Components功能模块形态 | 各Step聚焦测试与§8验收 | 1–8 | PASS |

## 5. Change File Tree

下表列全部文件，每个路径只列一次；路径为仓库根相对路径。模板的输出动态文件不是当前仓库手工新增文件，由manifest记录；模板首轮依次生成支持产物，不复制到 archetype 的 .generated。

| Operation | Path | Current evidence/symbol | Final symbols/state | Responsibility | Step | Requirements | Validation owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| MODIFY | `egon-cola-components/pom.xml` | 现有文件，本轮仅规划 | pom.xml | 登记开发工具模块和获准引擎版本 | Step 1 | PLAN-REQ-001, PLAN-REQ-002 | CodegenContractTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/pom.xml` | 当前不存在；设计§4/14授权新增责任 | pom.xml | 建立普通 jar 工具，不引导 Spring 上下文 | Step 1 | PLAN-REQ-001, PLAN-REQ-002 | CodegenContractTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/CodegenContractTest.java` | 当前不存在；设计§4/14授权新增责任 | CodegenContractTest.java | 先定义配置与授权约束的 RED 测试 | Step 1 | PLAN-REQ-001, PLAN-REQ-002 | CodegenContractTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/model/CodegenConfigBO.java` | 当前不存在；设计§4/14授权新增责任 | CodegenConfigBO.java | 外部配置类型与嵌套字段策略 | Step 1 | PLAN-REQ-001, PLAN-REQ-002 | CodegenContractTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/model/CodegenSchemaBO.java` | 当前不存在；设计§4/14授权新增责任 | CodegenSchemaBO.java | 规范化 Schema 和来源位置 | Step 1 | PLAN-REQ-001, PLAN-REQ-002 | CodegenContractTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/model/CodegenPlanBO.java` | 当前不存在；设计§4/14授权新增责任 | CodegenPlanBO.java | 跨 plan/apply 的机器契约 | Step 1 | PLAN-REQ-001, PLAN-REQ-002 | CodegenContractTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/model/CodegenProfileEnum.java` | 当前不存在；设计§4/14授权新增责任 | CodegenProfileEnum.java | 冻结三类配置值与能力矩阵 | Step 1 | PLAN-REQ-001, PLAN-REQ-002 | CodegenContractTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/validation/CodegenConfigValidator.java` | 当前不存在；设计§4/14授权新增责任 | CodegenConfigValidator.java | 注解驱动与交叉配置校验 | Step 1 | PLAN-REQ-001, PLAN-REQ-002 | CodegenContractTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/validation/CodegenGroups.java` | 当前不存在；设计§4/14授权新增责任 | CodegenGroups.java | 隔离配置与应用校验场景 | Step 1 | PLAN-REQ-001, PLAN-REQ-002 | CodegenContractTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/lombok.config` | 当前不存在；设计§4/14授权新增责任 | lombok.config | 保证工具处理器与构造器注解一致 | Step 1 | PLAN-REQ-001, PLAN-REQ-002 | CodegenContractTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/PostgreDdlAdapterTest.java` | 当前不存在；设计§4/14授权新增责任 | PostgreDdlAdapterTest.java | 冻结支持子集和拒绝边界 | Step 2 | PLAN-REQ-003 | PostgreDdlAdapterTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/ddl/PostgreDdlAdapter.java` | 当前不存在；设计§4/14授权新增责任 | PostgreDdlAdapter.java | 现有 JSQLParser 的唯一 AST 边界 | Step 2 | PLAN-REQ-003 | PostgreDdlAdapterTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/ddl/DdlSchemaService.java` | 当前不存在；设计§4/14授权新增责任 | DdlSchemaService.java | 有序回放和规范化 Schema 构建 | Step 2 | PLAN-REQ-003 | PostgreDdlAdapterTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/test/resources/ddl/schema.sql` | 当前不存在；设计§4/14授权新增责任 | schema.sql | 合规基础表覆盖类型和软删唯一性 | Step 2 | PLAN-REQ-003 | PostgreDdlAdapterTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/test/resources/ddl/changes.sql` | 当前不存在；设计§4/14授权新增责任 | changes.sql | 固定增量变化测试 | Step 2 | PLAN-REQ-003 | PostgreDdlAdapterTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/FreeMarkerTemplateServiceTest.java` | 当前不存在；设计§4/14授权新增责任 | FreeMarkerTemplateServiceTest.java | 定义引擎真实处理行为 | Step 3 | PLAN-REQ-002, PLAN-REQ-004 | FreeMarkerTemplateServiceTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/template/FreeMarkerTemplateService.java` | 当前不存在；设计§4/14授权新增责任 | FreeMarkerTemplateService.java | 封装受控模板读取与渲染 | Step 3 | PLAN-REQ-002, PLAN-REQ-004 | FreeMarkerTemplateServiceTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/template/TemplateContextService.java` | 当前不存在；设计§4/14授权新增责任 | TemplateContextService.java | 语义决策与渲染数据分离 | Step 3 | PLAN-REQ-002, PLAN-REQ-004 | FreeMarkerTemplateServiceTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/catalog.json` | 当前不存在；设计§4/14授权新增责任 | catalog.json | 版本化模板能力清单 | Step 3 | PLAN-REQ-002, PLAN-REQ-004 | FreeMarkerTemplateServiceTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/PersistenceTemplateTest.java` | 当前不存在；设计§4/14授权新增责任 | PersistenceTemplateTest.java | 基础模板 RED 与生成内容编译 | Step 4 | PLAN-REQ-001, PLAN-REQ-005 | PersistenceTemplateTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/profile/ProjectLayoutStrategy.java` | 当前不存在；设计§4/14授权新增责任 | ProjectLayoutStrategy.java | 选择现有三种布局策略 | Step 4 | PLAN-REQ-001, PLAN-REQ-005 | PersistenceTemplateTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/validation/GenerationScopeValidator.java` | 当前不存在；设计§4/14授权新增责任 | GenerationScopeValidator.java | 生成前能力/依赖/Schema统一预检 | Step 4 | PLAN-REQ-001, PLAN-REQ-005 | PersistenceTemplateTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/po.java.ftl` | 当前不存在；设计§4/14授权新增责任 | po.java.ftl | 生成持久化模型 | Step 4 | PLAN-REQ-001, PLAN-REQ-005 | PersistenceTemplateTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/dao.java.ftl` | 当前不存在；设计§4/14授权新增责任 | dao.java.ftl | 生成 Java Mapper 接口 | Step 4 | PLAN-REQ-001, PLAN-REQ-005 | PersistenceTemplateTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/mapper.xml.ftl` | 当前不存在；设计§4/14授权新增责任 | mapper.xml.ftl | 生成显式 SQL 与绑定 | Step 4 | PLAN-REQ-001, PLAN-REQ-005 | PersistenceTemplateTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/repository.java.ftl` | 当前不存在；设计§4/14授权新增责任 | repository.java.ftl | 生成薄 Repository | Step 4 | PLAN-REQ-001, PLAN-REQ-005 | PersistenceTemplateTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/BackendCrudTemplateTest.java` | 当前不存在；设计§4/14授权新增责任 | BackendCrudTemplateTest.java | 先证明所有上层产物闭合 | Step 5 | PLAN-REQ-006 | BackendCrudTemplateTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/domain-model.java.ftl` | 当前不存在；设计§4/14授权新增责任 | domain-model.java.ftl | 业务值载体 | Step 5 | PLAN-REQ-006 | BackendCrudTemplateTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/domain-query.java.ftl` | 当前不存在；设计§4/14授权新增责任 | domain-query.java.ftl | 领域检索契约 | Step 5 | PLAN-REQ-006 | BackendCrudTemplateTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/command.java.ftl` | 当前不存在；设计§4/14授权新增责任 | command.java.ftl | 创建/更新/删除命令 | Step 5 | PLAN-REQ-006 | BackendCrudTemplateTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/query.java.ftl` | 当前不存在；设计§4/14授权新增责任 | query.java.ftl | 应用详情/分页输入 | Step 5 | PLAN-REQ-006 | BackendCrudTemplateTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/result.java.ftl` | 当前不存在；设计§4/14授权新增责任 | result.java.ftl | 公开查询输出 | Step 5 | PLAN-REQ-006 | BackendCrudTemplateTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/converter.java.ftl` | 当前不存在；设计§4/14授权新增责任 | converter.java.ftl | 必需边界转换 | Step 5 | PLAN-REQ-006 | BackendCrudTemplateTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/domain-service.java.ftl` | 当前不存在；设计§4/14授权新增责任 | domain-service.java.ftl | Domain基础端口 | Step 5 | PLAN-REQ-006 | BackendCrudTemplateTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/domain-impl.java.ftl` | 当前不存在；设计§4/14授权新增责任 | domain-impl.java.ftl | Infrastructure领域实现 | Step 5 | PLAN-REQ-006 | BackendCrudTemplateTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/manage.java.ftl` | 当前不存在；设计§4/14授权新增责任 | manage.java.ftl | Application用例接口 | Step 5 | PLAN-REQ-006 | BackendCrudTemplateTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/manage-impl.java.ftl` | 当前不存在；设计§4/14授权新增责任 | manage-impl.java.ftl | Application事务实现 | Step 5 | PLAN-REQ-006 | BackendCrudTemplateTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/controller.java.ftl` | 当前不存在；设计§4/14授权新增责任 | controller.java.ftl | 可选 HTTP CRUD | Step 5 | PLAN-REQ-006 | BackendCrudTemplateTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/GenerationUpdateTest.java` | 当前不存在；设计§4/14授权新增责任 | GenerationUpdateTest.java | 文件行为 RED 与故障注入 | Step 6 | PLAN-REQ-007, PLAN-REQ-008 | GenerationUpdateTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/update/GenerationPlanService.java` | 当前不存在；设计§4/14授权新增责任 | GenerationPlanService.java | Schema+产物影响与候选计划 | Step 6 | PLAN-REQ-007, PLAN-REQ-008 | GenerationUpdateTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/update/GenerationStateRepository.java` | 当前不存在；设计§4/14授权新增责任 | GenerationStateRepository.java | 本地状态文件访问 | Step 6 | PLAN-REQ-007, PLAN-REQ-008 | GenerationUpdateTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/update/GenerationApplyService.java` | 当前不存在；设计§4/14授权新增责任 | GenerationApplyService.java | 计划复核、替换与恢复状态机 | Step 6 | PLAN-REQ-007, PLAN-REQ-008 | GenerationUpdateTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/validation/OutputPathValidator.java` | 当前不存在；设计§4/14授权新增责任 | OutputPathValidator.java | 路径与目录身份检查 | Step 6 | PLAN-REQ-007, PLAN-REQ-008 | GenerationUpdateTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/CodegenCommandTest.java` | 当前不存在；设计§4/14授权新增责任 | CodegenCommandTest.java | 命令参数及退出码 RED | Step 7 | PLAN-REQ-009 | CodegenCommandTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/cli/CodegenCommand.java` | 当前不存在；设计§4/14授权新增责任 | CodegenCommand.java | 普通 main 与显式依赖装配 | Step 7 | PLAN-REQ-009 | CodegenCommandTest |
| CREATE | `scripts/egon-codegen.sh` | 当前不存在；设计§4/14授权新增责任 | egon-codegen.sh | 离线launcher | Step 7 | PLAN-REQ-009 | CodegenCommandTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/README.md` | 当前不存在；设计§4/14授权新增责任 | README.md | 可运行操作说明与依赖声明 | Step 7 | PLAN-REQ-009 | CodegenCommandTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/test/resources/codegen-web.json` | 当前不存在；设计§4/14授权新增责任 | codegen-web.json | 无秘密的完整示例配置 | Step 7 | PLAN-REQ-009 | CodegenCommandTest |
| CREATE | `egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/CodegenAcceptanceTest.java` | 当前不存在；设计§4/14授权新增责任 | CodegenAcceptanceTest.java | 最终行为矩阵 | Step 8 | PLAN-REQ-010, PLAN-REQ-011 | CodegenAcceptanceTest |
| CREATE | `scripts/test-egon-codegen.sh` | 当前不存在；设计§4/14授权新增责任 | test-egon-codegen.sh | 临时源工程编译门槛 | Step 8 | PLAN-REQ-010, PLAN-REQ-011 | CodegenAcceptanceTest |
| MODIFY | `.agents/skills/egon-coding-writing-spec/references/backend-code-generation.md` | 现有文件，本轮仅规划 | backend-code-generation.md | 将方案态路由切换为真实工具工作流 | Step 8 | PLAN-REQ-010, PLAN-REQ-011 | CodegenAcceptanceTest |
| MODIFY | `.agents/skills/egon-coding-writing-plan/references/backend-code-generation.md` | 现有文件，本轮仅规划 | backend-code-generation.md | 将方案态路由切换为真实工具工作流 | Step 8 | PLAN-REQ-010, PLAN-REQ-011 | CodegenAcceptanceTest |
| MODIFY | `.agents/skills/egon-coding-executing-plan/references/backend-code-generation.md` | 现有文件，本轮仅规划 | backend-code-generation.md | 将方案态路由切换为真实工具工作流 | Step 8 | PLAN-REQ-010, PLAN-REQ-011 | CodegenAcceptanceTest |
| MODIFY | `.agents/skills/README-egon-coding-spec-plan.md` | 现有文件，本轮仅规划 | README-egon-coding-spec-plan.md | 更新实际生成流程入口 | Step 8 | PLAN-REQ-010, PLAN-REQ-011 | CodegenAcceptanceTest |

## 6. Prerequisites, Constraints, and Plan Clarifications

### 6.1 Repository and worktree baseline

当前HEAD如元数据；前两轮技能和proposal改动仍在工作树，全部保留。本轮新Plan与方案修订不作为后续步骤自动提交范围。实施开始先重新git status与受影响POM/API核验；发现别人改动重叠需协商，不回滚或覆盖。

### 6.2 Build, test, and environment prerequisites

- Java 21+编译环境按Components release=21；仓库mvnw可用，依赖通过已批准正常构建准备。
- 编码阶段模板引擎只允许 org.freemarker:freemarker:2.3.35；该版本由方案选择，授权来源是用户明确FreeMarker更正。其他新外部依赖仍先阻断。
- 工具内部直接依赖是方案列出的现有组件，未获Plan执行批准前不改POM。若测试所需Common Test不能提供依赖，返回具体缺口，不自动添加test库。
- dependency plugin为开发验证命令，不新增生命周期绑定；首次解析若需要现有私服/环境凭据，只使用既有配置，不记录凭据。
- 不执行DDL、数据库/容器/应用启动、远程推送或发布。本轮仅运行Plan/skill静态验证。

### 6.3 Plan Clarifications

| ID | Evidence | Clarification | Why local and compatible |
| --- | --- | --- | --- |
| PC-001 | 用户最后一条明确改为FreeMarker，官方下载页2.3.35 | 固定core工件与方括号语法 | 落实明确选择，不增加第二引擎 |
| PC-002 | DomainService不能依赖PO/Application | 生成domain-model/domain-query作为backend-crud编译前置 | 方案§14.3已说明真实边界，不造聚合根 |
| PC-003 | 既有Schema/Plan配置有大量同生命周期字段 | 用三个BO的嵌套静态类型保结构 | 不为每个字段建独立文件 |
| PC-004 | Common PageQuery是record不可继承 | 分页Query组合Common PageQuery而非extends | 遵循真实签名，不新增分页框架 |
| PC-005 | final repo CRUD与removeById(id)会加载最新快照 | 按调用者版本使用PO重载 | 保留原始并发语义，不能生成失效的乐观锁 |
| PC-006 | 工具是Components内开发CLI，不是业务应用 | 保持功能包布局；应用架构规则限定生成产物 | 用户已同意独立CLI设计，避免给工具虚构Controller或DDD |

## 7. Ordered File-by-file Implementation Steps

以下命令为未来编码步骤验证，不是本轮已经通过的证明。新测试先建立可编译签名但无行为，观察指定行为RED，再实现GREEN；依赖解析/编译环境失败单独报告。

### Step 1 — 工具模块、依赖与生成请求契约

- Requirements: PLAN-REQ-001, PLAN-REQ-002
- Dependencies: 当前仓库Components构建基线与获准依赖
- Baseline state: 工具模块不存在，POM与skill已有规则已核对。
- Observable outcome: 工具编译基线、FreeMarker 唯一外部新增和可验证请求契约。
- End state: 下列文件完整，CodegenContractTest GREEN；未执行数据库、应用或发布，后续步骤仍待实现。
- Test-first gate: Required — CodegenContractTest 先固定下面伪代码中的失败行为；缺少类型时只补编译契约，RED必须是缺行为断言失败。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11
- Ordered files:

#### File 1 — `MODIFY egon-cola-components/pom.xml`

- Purpose: 登记开发工具模块和获准引擎版本。
- Symbols: `pom.xml`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前 Components 聚合 POM 的 modules、dependencyManagement、Java 21 compiler 配置。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `CodegenContractTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 本Step编译/RED前置，先锁定预期。
- Contract/signature changes: 新增 module 与 freemarker.version=2.3.35 及 dependencyManagement；不添加到业务模块 dependencies。
- Input/output and state mapping: 登记开发工具模块和获准引擎版本按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```text
modules: append egon-cola-component-code-generator to the existing reactor list
<properties> freemarker.version = 2.3.35 </properties>
<dependencyManagement> manage org.freemarker:freemarker using freemarker.version; preserve existing managed versions </dependencyManagement>
```

- Verification contribution: `CodegenContractTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 2 — `CREATE egon-cola-components/egon-cola-component-code-generator/pom.xml`

- Purpose: 建立普通 jar 工具，不引导 Spring 上下文。
- Symbols: `pom.xml`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `CodegenContractTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: 父 top.egon:egon-cola-components 同仓版本；内部 common-core、MP-SDJ、common-test(test)，FreeMarker；继承现有测试/处理器配置。
- Input/output and state mapping: 建立普通 jar 工具，不引导 Spring 上下文按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```text
parent = existing Components parent; packaging = jar; compiler release = inherited 21
compile dependencies = common-core + existing MP-SDJ starter + freemarker managed 2.3.35
test dependencies = common-test; do not add web runtime, generator frameworks, logging bindings or build plugins
```

- Verification contribution: `CodegenContractTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 3 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/CodegenContractTest.java`

- Purpose: 先定义配置与授权约束的 RED 测试。
- Symbols: `CodegenContractTest.java`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `CodegenContractTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: 缺字段、未知产物、重复路径、Agent/Service Controller 均被拒绝；序列化往返稳定。
- Input/output and state mapping: 先定义配置与授权约束的 RED 测试按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```java
assertInvalid(configWithoutOutputRoot(), "CONFIG_REQUIRED")
assertInvalid(config("service", "controller"), "UNSUPPORTED_ARTIFACT")
assertValid(config("web", "repo", existingPojoAndDaoTypes())); assertNoTargetWrites()
assertThrowsUnknownJsonProperty(); assertApprovedExternalCoordinatesEqual(Set.of("org.freemarker:freemarker"))
```

- Verification contribution: `CodegenContractTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 4 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/model/CodegenConfigBO.java`

- Purpose: 外部配置类型与嵌套字段策略。
- Symbols: `CodegenConfigBO.java`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `CodegenContractTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: 普通 class + Data/NoArgs/AllArgs/Accessors/Builder；configVersion、profile、input、roots、tables、artifacts、existingTypes、fieldPolicies、apiContract。
- Input/output and state mapping: 外部配置类型与嵌套字段策略按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Builder
class CodegenConfigBO { @NotBlank String outputRoot; @Valid @NotNull InputBO input; @NotEmpty List<String> artifacts; }
InputBO = mode/resourceRoot/schemaFiles/manifest; apiContract = existingErrorMapper/contextSymbol/basePath
Unknown JSON keys fail; fieldPolicies declare create/update/result/filter/sort; no implicit server-field exposure
```

- Verification contribution: `CodegenContractTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 5 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/model/CodegenSchemaBO.java`

- Purpose: 规范化 Schema 和来源位置。
- Symbols: `CodegenSchemaBO.java`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `CodegenContractTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: 嵌套 TableBO/ColumnBO/ConstraintBO/SourcePositionBO；物理目标、逻辑表和索引 NULL 谓词不能丢。
- Input/output and state mapping: 规范化 Schema 和来源位置按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```java
CodegenSchemaBO holds ordered tables, inputFingerprint and immutable copy of version/checksum prefix
TableBO holds schema/logicalName/role/physicalNames/routeKeys/columns/constraints/indexes
ColumnBO holds sqlType/length/precision/scale/nullable/defaultExpression/comment/sourcePosition
Use class Lombok baseline; snapshot reads create defensive copies; no persistence or Spring annotations
```

- Verification contribution: `CodegenContractTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 6 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/model/CodegenPlanBO.java`

- Purpose: 跨 plan/apply 的机器契约。
- Symbols: `CodegenPlanBO.java`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `CodegenContractTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: formatVersion=1 与方案§14.4全字段；FileChangeBO、ArtifactStateBO、DiagnosticBO、JournalBO 为内部嵌套类型。
- Input/output and state mapping: 跨 plan/apply 的机器契约按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```java
CodegenPlanBO = fingerprints + outputRootBinding + schemaChanges + files + pendingImpacts + diagnostics
FileChangeBO = actionId/path/artifact/table/operation/expectedDiskHash/previousGeneratedHash/candidateHash/candidatePath
ArtifactStateBO keeps per-file lastGeneratedInput; latestObservedSchema never overwrites unselected artifact state
JSON ordering canonical; absent optional value has explicit presence flag; reject unsupported formatVersion
```

- Verification contribution: `CodegenContractTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 7 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/model/CodegenProfileEnum.java`

- Purpose: 冻结三类配置值与能力矩阵。
- Symbols: `CodegenProfileEnum.java`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `CodegenContractTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: LIGHT/WEB/SERVICE；JsonValue 为 light/web/service；不存数据库，不加 EnumValue。
- Input/output and state mapping: 冻结三类配置值与能力矩阵按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```java
enum CodegenProfileEnum { LIGHT("light"), WEB("web"), SERVICE("service"); }
@JsonValue String code(); parse only exact documented lower-case values
reject agent/open/traditional when no implemented profile; service disallows controller; profile choice does not move existing packages
```

- Verification contribution: `CodegenContractTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 8 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/validation/CodegenConfigValidator.java`

- Purpose: 注解驱动与交叉配置校验。
- Symbols: `CodegenConfigValidator.java`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `CodegenContractTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: 使用现有 ValidationUtils 执行注解元数据，局部交叉规则验 roots/artifacts/types；@Slf4j、RequiredArgsConstructor。
- Input/output and state mapping: 注解驱动与交叉配置校验按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```java
validate(config): beanValidator.validate(config, CodegenGroups.Plan.class)
verify known profile and artifacts, project module paths, nonempty explicit field policies for upper CRUD
verify schema or manifest mode has exactly its required inputs; resolve existing type mappings without adding artifacts
return normalized config or diagnostics with JSON pointer/path; do not create output folders
```

- Verification contribution: `CodegenContractTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 9 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/validation/CodegenGroups.java`

- Purpose: 隔离配置与应用校验场景。
- Symbols: `CodegenGroups.java`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `CodegenContractTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: Plan、Apply 分组只承载 marker；嵌套输入 @Valid，跨组需求使用既定约束。
- Input/output and state mapping: 隔离配置与应用校验场景按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```java
interface CodegenGroups { interface Plan {} interface Apply {} }
Plan requires DDL input, profile and output root; Apply requires validated planId and bound output root
Do not weaken required fields via Default group; CLI validates before planning and again before apply
```

- Verification contribution: `CodegenContractTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 10 — `CREATE egon-cola-components/egon-cola-component-code-generator/lombok.config`

- Purpose: 保证工具处理器与构造器注解一致。
- Symbols: `lombok.config`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: egon-cola-archetypes/source-projects/egon-cola-source-web/lombok.config 已使用该传播规则。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `CodegenContractTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: 沿用 native source 的 Qualifier 传播；只改本工具目录。
- Input/output and state mapping: 保证工具处理器与构造器注解一致按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```text
config.stopBubbling = true
lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Qualifier
Keep generated application lombok.config untouched; validator reports missing required propagation instead of repairing it
```

- Verification contribution: `CodegenContractTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-code-generator -am -Dtest=CodegenContractTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: 退出0，目标测试真实被发现且全部通过；无匹配测试跳过不能视为完成。首次RED须记录具体断言，再记录GREEN。
- Failure returns to: 本Step对应失败测试及被测文件；依赖或AST能力不足先阻断，不引入替代库、不削弱规范。
- Completion criteria: 工具编译基线、FreeMarker 唯一外部新增和可验证请求契约。 具备聚焦证据，git diff --check通过且只包含上述文件；Step 1手工检查逐项留证。
- Rollback: 仅撤回尚未提交的本Step自有修改；已提交后使用单独获准纠正任务，不重写历史，不删除别人数据/源码。工具运行写入的恢复由journal契约负责。
- Commit paths: `egon-cola-components/pom.xml`; `egon-cola-components/egon-cola-component-code-generator/pom.xml`; `egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/CodegenContractTest.java`; `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/model/CodegenConfigBO.java`; `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/model/CodegenSchemaBO.java`; `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/model/CodegenPlanBO.java`; `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/model/CodegenProfileEnum.java`; `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/validation/CodegenConfigValidator.java`; `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/validation/CodegenGroups.java`; `egon-cola-components/egon-cola-component-code-generator/lombok.config`
- Commit: `feat(codegen): define generator contracts and approved engine`

### Step 2 — 离线 DDL 与 Manifest 解析

- Requirements: PLAN-REQ-003
- Dependencies: Step 1 已验证并提交；前面步骤能力保留
- Baseline state: 已完成前1步，只新增本步明确行为，不改已验证契约。
- Observable outcome: 完整输入与增量历史均得到可追踪 Schema，角色分支和物理表正确归并。
- End state: 下列文件完整，PostgreDdlAdapterTest GREEN；未执行数据库、应用或发布，后续步骤仍待实现。
- Test-first gate: Required — PostgreDdlAdapterTest 先固定下面伪代码中的失败行为；缺少类型时只补编译契约，RED必须是缺行为断言失败。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/PostgreDdlAdapterTest.java`

- Purpose: 冻结支持子集和拒绝边界。
- Symbols: `PostgreDdlAdapterTest.java`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `PostgreDdlAdapterTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 本Step编译/RED前置，先锁定预期。
- Contract/signature changes: 读取三个 native 源 SQL 内容；解析与规范合规检查分开，不为旧唯一约束改 SQL。
- Input/output and state mapping: 冻结支持子集和拒绝边界按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```java
parse actual light/web/service init resources; assert MASTER_DATA and SHARD tables retain distinct role metadata
apply CREATE, COMMENT, ALTER ADD/DROP/RENAME/TYPE/NULL/DEFAULT, INDEX deltas against a complete baseline
assertUnknownDynamicExecuteFailsWithSourcePosition(); assertHistoryChecksumDriftFailsBeforeRendering()
assertCannotMergeDifferentPhysicalSchemas(); assertNoConnectionOrDdlRunnerIsInvoked()
```

- Verification contribution: `PostgreDdlAdapterTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 2 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/ddl/PostgreDdlAdapter.java`

- Purpose: 现有 JSQLParser 的唯一 AST 边界。
- Symbols: `PostgreDdlAdapter.java`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `PostgreDdlAdapterTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: parseStatements(String,source) → 有类型的结构变更；只用 MP-SDJ 既有解析依赖。
- Input/output and state mapping: 现有 JSQLParser 的唯一 AST 边界按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```java
lex source preserving comments, quoted identifiers and dollar-quoted blocks; retain source offsets
recognize only the fixed current_setting(egon_migration.role) branch grammar; associate static statements with role
for each ordinary statement call CCJSqlParserUtil.parseStatements; translate every supported AST node and index predicate
if unsupported procedure/dynamic SQL/AST loses metadata: emit UNSUPPORTED_DDL at source position; never skip or change parser
```

- Verification contribution: `PostgreDdlAdapterTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 3 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/ddl/DdlSchemaService.java`

- Purpose: 有序回放和规范化 Schema 构建。
- Symbols: `DdlSchemaService.java`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `PostgreDdlAdapterTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: read(CodegenConfigBO) → CodegenSchemaBO；使用 EgonColaDdlManifestBO验证格式、SHA-256前缀。
- Input/output and state mapping: 有序回放和规范化 Schema 构建按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```java
schema input = complete baseline; manifest input = ordered classpath scripts validated with EgonColaDdlManifestBO
verify canonical paths, sha256 bytes and previously observed immutable prefix before replay
apply typed schema changes in order; unresolved ALTER target yields MISSING_SCHEMA_BASELINE
read explicit logical/physical mappings; merge only matching field/constraint semantics, exclude only exact registered technical tables
```

- Verification contribution: `PostgreDdlAdapterTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 4 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/test/resources/ddl/schema.sql`

- Purpose: 合规基础表覆盖类型和软删唯一性。
- Symbols: `schema.sql`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `PostgreDdlAdapterTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: 测试 orders 表带所有 EgonModel 字段、业务列 + deleted_at 联合键与 active guard；不供数据库执行。
- Input/output and state mapping: 合规基础表覆盖类型和软删唯一性按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```text
CREATE TABLE orders (id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, deleted_at TIMESTAMP, version BIGINT NOT NULL DEFAULT 0, ...audits, code VARCHAR(64));
CREATE UNIQUE INDEX orders_lifecycle ON orders(tenant_id, code, deleted_at);
CREATE UNIQUE INDEX orders_active ON orders(tenant_id, code) WHERE deleted_at IS NULL;
COMMENT ON COLUMN orders.code IS '业务代码与 Unicode 转义样本';
```

- Verification contribution: `PostgreDdlAdapterTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 5 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/test/resources/ddl/changes.sql`

- Purpose: 固定增量变化测试。
- Symbols: `changes.sql`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `PostgreDdlAdapterTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: 包括新增列、重命名、NULL/default、唯一索引差异；分例由测试拆分且保持基线。
- Input/output and state mapping: 固定增量变化测试按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```text
ALTER TABLE orders ADD COLUMN note VARCHAR(160);
ALTER TABLE orders RENAME COLUMN code TO order_code;
ALTER TABLE orders ALTER COLUMN note SET NOT NULL;
-- Tests replay supported variants independently and assert destructive impacts instead of writing live schema.
```

- Verification contribution: `PostgreDdlAdapterTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-code-generator -am -Dtest=PostgreDdlAdapterTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: 退出0，目标测试真实被发现且全部通过；无匹配测试跳过不能视为完成。首次RED须记录具体断言，再记录GREEN。
- Failure returns to: 本Step对应失败测试及被测文件；依赖或AST能力不足先阻断，不引入替代库、不削弱规范。
- Completion criteria: 完整输入与增量历史均得到可追踪 Schema，角色分支和物理表正确归并。 具备聚焦证据，git diff --check通过且只包含上述文件；Step 2手工检查逐项留证。
- Rollback: 仅撤回尚未提交的本Step自有修改；已提交后使用单独获准纠正任务，不重写历史，不删除别人数据/源码。工具运行写入的恢复由journal契约负责。
- Commit paths: `egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/PostgreDdlAdapterTest.java`; `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/ddl/PostgreDdlAdapter.java`; `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/ddl/DdlSchemaService.java`; `egon-cola-components/egon-cola-component-code-generator/src/test/resources/ddl/schema.sql`; `egon-cola-components/egon-cola-component-code-generator/src/test/resources/ddl/changes.sql`
- Commit: `feat(codegen): parse managed PostgreSQL DDL offline`

### Step 3 — FreeMarker 渲染边界

- Requirements: PLAN-REQ-002, PLAN-REQ-004
- Dependencies: Step 2 已验证并提交；前面步骤能力保留
- Baseline state: 已完成前2步，只新增本步明确行为，不改已验证契约。
- Observable outcome: 获准引擎按方括号语法稳定渲染，模板失败不会产生可应用代码。
- End state: 下列文件完整，FreeMarkerTemplateServiceTest GREEN；未执行数据库、应用或发布，后续步骤仍待实现。
- Test-first gate: Required — FreeMarkerTemplateServiceTest 先固定下面伪代码中的失败行为；缺少类型时只补编译契约，RED必须是缺行为断言失败。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/FreeMarkerTemplateServiceTest.java`

- Purpose: 定义引擎真实处理行为。
- Symbols: `FreeMarkerTemplateServiceTest.java`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `FreeMarkerTemplateServiceTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 本Step编译/RED前置，先锁定预期。
- Contract/signature changes: 真实 Configuration 单元测试；数据中的 FTL 不递归执行，MyBatis原样。
- Input/output and state mapping: 定义引擎真实处理行为按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```java
render real classpath template with Java identifiers, Unicode and XML escaped values
assertLiteral("#{et.id}"); assertLiteral("${existing.property}"); assertNoDoubleEscaping()
assertUndefinedRequiredVariableThrows(); assertUnknownTemplateThrows(); assertNewAndApiBuiltinsBlocked()
assertBytesEqual(secondRender(), firstRender()); assertMissingModelDoesNotProduceCandidate()
```

- Verification contribution: `FreeMarkerTemplateServiceTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 2 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/template/FreeMarkerTemplateService.java`

- Purpose: 封装受控模板读取与渲染。
- Symbols: `FreeMarkerTemplateService.java`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `FreeMarkerTemplateServiceTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: render(templateId, Map<String,Object>) → UTF-8 byte[]；冻结 Configuration；缓存来自版本化模板。
- Input/output and state mapping: 封装受控模板读取与渲染按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```java
cfg = new Configuration(Configuration.VERSION_2_3_35); classpath loader root = templates/backend
cfg uses square bracket tag/interpolation, RETHROW_HANDLER, API disabled, ALLOWS_NOTHING_RESOLVER, plain-text output
templateId must be declared in catalog; inputs contain read-only maps/lists/scalars only
process once into bounded StringWriter; normalize LF; failures return TEMPLATE_ERROR with template/line, never partial output
```

- Verification contribution: `FreeMarkerTemplateServiceTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 3 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/template/TemplateContextService.java`

- Purpose: 语义决策与渲染数据分离。
- Symbols: `TemplateContextService.java`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `FreeMarkerTemplateServiceTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: context(schema,table,profile,artifact,policies)；类型、字段、import、Bean及转义元数据。
- Input/output and state mapping: 语义决策与渲染数据分离按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```java
derive Java field mapping from SQL precision/time semantics and EgonModel inherited field registry
sort imports deterministically; prevalidate every identifier; escape Java strings/comments and XML attributes separately
apply explicit create/update/result/filter/sort policies; include route/tenant/version constraints and named bind parameters
return unmodifiable map of scalar/render lists; do not expose executable helpers, AST, ClassLoader or Spring context
```

- Verification contribution: `FreeMarkerTemplateServiceTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 4 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/catalog.json`

- Purpose: 版本化模板能力清单。
- Symbols: `catalog.json`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `FreeMarkerTemplateServiceTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: templateSetVersion、模板ID、项目类型、路径模式、前置类型、产物依赖；不自动拓展选中范围。
- Input/output and state mapping: 版本化模板能力清单按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```text
catalog formatVersion = 1; engine = freemarker; engineVersion = 2.3.35
Each entry declares artifact, templateResource, allowedProfiles, targetPathPattern and requiredTypes
Compute template-set digest from catalog plus exact resource bytes; unresolved entry fails startup validation
Do not register event/cache/RPC/Agent templates or dynamically discover external templates
```

- Verification contribution: `FreeMarkerTemplateServiceTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-code-generator -am -Dtest=FreeMarkerTemplateServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: 退出0，目标测试真实被发现且全部通过；无匹配测试跳过不能视为完成。首次RED须记录具体断言，再记录GREEN。
- Failure returns to: 本Step对应失败测试及被测文件；依赖或AST能力不足先阻断，不引入替代库、不削弱规范。
- Completion criteria: 获准引擎按方括号语法稳定渲染，模板失败不会产生可应用代码。 具备聚焦证据，git diff --check通过且只包含上述文件；Step 3手工检查逐项留证。
- Rollback: 仅撤回尚未提交的本Step自有修改；已提交后使用单独获准纠正任务，不重写历史，不删除别人数据/源码。工具运行写入的恢复由journal契约负责。
- Commit paths: `egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/FreeMarkerTemplateServiceTest.java`; `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/template/FreeMarkerTemplateService.java`; `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/template/TemplateContextService.java`; `egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/catalog.json`
- Commit: `feat(codegen): render deterministic FreeMarker templates`

### Step 4 — 布局与持久化四件套

- Requirements: PLAN-REQ-001, PLAN-REQ-005
- Dependencies: Step 3 已验证并提交；前面步骤能力保留
- Baseline state: 已完成前3步，只新增本步明确行为，不改已验证契约。
- Observable outcome: 只生成选中的 PO/DAO/XML/Repo，保留 MP-SDJ真实CRUD契约。
- End state: 下列文件完整，PersistenceTemplateTest GREEN；未执行数据库、应用或发布，后续步骤仍待实现。
- Test-first gate: Required — PersistenceTemplateTest 先固定下面伪代码中的失败行为；缺少类型时只补编译契约，RED必须是缺行为断言失败。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/PersistenceTemplateTest.java`

- Purpose: 基础模板 RED 与生成内容编译。
- Symbols: `PersistenceTemplateTest.java`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `PersistenceTemplateTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 本Step编译/RED前置，先锁定预期。
- Contract/signature changes: 三类布局、单产物、版本删除/SQL绑定、人工路径与依赖不扩展。
- Input/output and state mapping: 基础模板 RED 与生成内容编译按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```java
for profile in light/web/service render selected po,dao,mapper-xml,repo against compliant orders schema
compile generated Java with actual EgonModel/EgonColaRepository and Lombok processor classpath
assert three required XML statement ids, MP_OPTLOCK_VERSION_ORIGINAL and explicit active predicates
assertRepoOnlyRequiresExistingDaoAndPo(); assertNoGeneratedFinalCrudOverride(); assertUnsupportedIdShapeBlocks()
```

- Verification contribution: `PersistenceTemplateTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 2 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/profile/ProjectLayoutStrategy.java`

- Purpose: 选择现有三种布局策略。
- Symbols: `ProjectLayoutStrategy.java`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `PersistenceTemplateTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: 接口输出 artifact→module/path 与允许能力；只三个固定实现，通过内部静态实现避免注册框架。
- Input/output and state mapping: 选择现有三种布局策略按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```java
ProjectLayoutStrategy resolve(CodegenProfileEnum profile) uses fixed Light/Web/Service strategy instances
Light maps infrastructure/domain/application/adapter under one source root; Web/Service maps configured existing modules
Service rejects controller; validate selected modules exist and align actual POM/package evidence
Every resolved relative path must remain under outputRoot; no package migration or extra Maven module creation
```

- Verification contribution: `PersistenceTemplateTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 3 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/validation/GenerationScopeValidator.java`

- Purpose: 生成前能力/依赖/Schema统一预检。
- Symbols: `GenerationScopeValidator.java`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `PersistenceTemplateTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: 校验基础字段、唯一键、类型/Bean重复、依赖和已存在引用；无效时零写入。
- Input/output and state mapping: 生成前能力/依赖/Schema统一预检按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```java
verify common base field types, business+deleted_at key and active NULL guard; old business-only key is diagnostic
inspect target module dependency evidence and required classpath symbols; BOM-only entries are not available dependencies
selected artifacts remain exact; missing referenced types -> MISSING_TYPE with dependent artifact, never expand selection
verify explicit route keys and field allowlists; broadcast table writes, unknown SQL types and composite IDs block unsupported CRUD
```

- Verification contribution: `PersistenceTemplateTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 4 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/po.java.ftl`

- Purpose: 生成持久化模型。
- Symbols: `po.java.ftl`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `PersistenceTemplateTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: *PO extends EgonModel<*PO>；继承字段不重复。
- Input/output and state mapping: 生成持久化模型按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```text
package [=packages.po]; imports use prepared type/annotation list
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @SuperBuilder @EqualsAndHashCode(callSuper=true)
class [=poType] extends EgonModel<[=poType]> { [#list businessFields as field] @TableField("[=field.column]") private [=field.javaType] [=field.javaName]; [/#list] }
Root/empty class constructor conflicts follow verified explicit variant, never invent a base class.
```

- Verification contribution: `PersistenceTemplateTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 5 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/dao.java.ftl`

- Purpose: 生成 Java Mapper 接口。
- Symbols: `dao.java.ftl`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `PersistenceTemplateTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: *DAO extends EgonColaMapper<PO>；具名 query/page 额外方法与XML完全一致。
- Input/output and state mapping: 生成 Java Mapper 接口按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```text
@Mapper interface [=daoType] extends EgonColaMapper<[=poType]> {
  List<[=poType]> selectByQuery(@Param("query") [=domainQueryType] query, @Param("limit") int limit, @Param("offset") long offset);
}
If persistence-only has no query type, omit upper query methods; retain inherited three-method XML contract.
```

- Verification contribution: `PersistenceTemplateTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 6 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/mapper.xml.ftl`

- Purpose: 生成显式 SQL 与绑定。
- Symbols: `mapper.xml.ftl`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `PersistenceTemplateTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: selectActiveById/selectActiveByIds/deleteVersionedById，结果映射、分页/路由扩展。
- Input/output and state mapping: 生成显式 SQL 与绑定按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```text
<mapper namespace="[=daoFqn]"> resultMap enumerates inherited and business columns; no SELECT star
selectActiveById/Ids use literal #{id}/#{ids} bindings and deleted_at IS NULL; empty IDs never produce IN ()
deleteVersionedById binds #{et.id}, #{MP_OPTLOCK_VERSION_ORIGINAL}, audit updates and UTC deleted_at; version increments once
Custom query/page statements emit validated route predicates and fixed sort branches, never raw request SQL interpolation. </mapper>
```

- Verification contribution: `PersistenceTemplateTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 7 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/repository.java.ftl`

- Purpose: 生成薄 Repository。
- Symbols: `repository.java.ftl`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `PersistenceTemplateTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: EgonColaRepository<DAO,PO>；命名Bean、Getter、Qualifier字段装配。
- Input/output and state mapping: 生成薄 Repository按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```text
@Slf4j @Validated @Repository("[=repoBean]") @RequiredArgsConstructor
class [=repoType] extends EgonColaRepository<[=daoType],[=poType]> {
 @Getter @Qualifier("[=daoBean]") final [=daoType] baseMapper; @Getter(PROTECTED) @Qualifier("[=propertiesBean]") final EgonColaMybatisPlusProperties properties;
 named query methods delegate to explicit DAO/XML only; no duplicated save/update/delete or automatic cache/event. }
```

- Verification contribution: `PersistenceTemplateTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-code-generator -am -Dtest=PersistenceTemplateTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: 退出0，目标测试真实被发现且全部通过；无匹配测试跳过不能视为完成。首次RED须记录具体断言，再记录GREEN。
- Failure returns to: 本Step对应失败测试及被测文件；依赖或AST能力不足先阻断，不引入替代库、不削弱规范。
- Completion criteria: 只生成选中的 PO/DAO/XML/Repo，保留 MP-SDJ真实CRUD契约。 具备聚焦证据，git diff --check通过且只包含上述文件；Step 4手工检查逐项留证。
- Rollback: 仅撤回尚未提交的本Step自有修改；已提交后使用单独获准纠正任务，不重写历史，不删除别人数据/源码。工具运行写入的恢复由journal契约负责。
- Commit paths: `egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/PersistenceTemplateTest.java`; `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/profile/ProjectLayoutStrategy.java`; `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/validation/GenerationScopeValidator.java`; `egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/po.java.ftl`; `egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/dao.java.ftl`; `egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/mapper.xml.ftl`; `egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/repository.java.ftl`
- Commit: `feat(codegen): generate native persistence scaffolding`

### Step 5 — 领域与应用基础 CRUD 模板

- Requirements: PLAN-REQ-006
- Dependencies: Step 4 已验证并提交；前面步骤能力保留
- Baseline state: 已完成前4步，只新增本步明确行为，不改已验证契约。
- Observable outcome: 在真实领域端口边界生成完整基础CRUD；仅Light/Web具备已配置HTTP入口。
- End state: 下列文件完整，BackendCrudTemplateTest GREEN；未执行数据库、应用或发布，后续步骤仍待实现。
- Test-first gate: Required — BackendCrudTemplateTest 先固定下面伪代码中的失败行为；缺少类型时只补编译契约，RED必须是缺行为断言失败。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/BackendCrudTemplateTest.java`

- Purpose: 先证明所有上层产物闭合。
- Symbols: `BackendCrudTemplateTest.java`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `BackendCrudTemplateTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 本Step编译/RED前置，先锁定预期。
- Contract/signature changes: 基础类型与转换关系编译、分页边界、版本保留、HTTP领域隔离。
- Input/output and state mapping: 先证明所有上层产物闭合按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```java
render backend-crud for all three profiles and compile with actual MapStruct/Lombok processors
assertDomainImportsNoPoOrMp(); assertInfrastructureImportsNoApplication(); assertControllerUsesManageOnly()
call generated use cases with fake domain service; verify expectedVersion survives update/delete and zero affected rows map through configured error adapter
assertFieldPoliciesExcludeTenantAuditAndSensitiveColumns(); assertServiceHasNoController(); assertGetHasNoWrites()
```

- Verification contribution: `BackendCrudTemplateTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 2 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/domain-model.java.ftl`

- Purpose: 业务值载体。
- Symbols: `domain-model.java.ftl`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `BackendCrudTemplateTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: Domain *BO，业务字段/id/version/route；无 MP 注解。
- Input/output and state mapping: 业务值载体按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```text
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Builder class [=domainBoType] implements existing common contract
Carry only id, version, configured business and routing fields; ordinary entity remains class
Do not synthesize Aggregate/DDD Value Object or persistence annotations from the table definition
```

- Verification contribution: `BackendCrudTemplateTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 3 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/domain-query.java.ftl`

- Purpose: 领域检索契约。
- Symbols: `domain-query.java.ftl`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `BackendCrudTemplateTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: 字段白名单 + Common PageQuery 组合；不继承 record。
- Input/output and state mapping: 领域检索契约按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```text
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Builder class [=domainQueryType]
Compose Common PageQuery and explicit filter/sort values; reject overflow and sort values outside allowlist
No Controller type or infrastructure PO import; stable primary-key tie-breaker is mandatory for paging
```

- Verification contribution: `BackendCrudTemplateTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 4 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/command.java.ftl`

- Purpose: 创建/更新/删除命令。
- Symbols: `command.java.ftl`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `BackendCrudTemplateTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: 分别Create/Update/Delete模板上下文，update/delete包含expectedVersion。
- Input/output and state mapping: 创建/更新/删除命令按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```text
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Builder class [=commandType]
Emit @NotNull/@Size/@Valid/group constraints only for explicitly required client fields; defaults are not frontend requirements
Update/Delete carry id + expectedVersion + required route fields; create excludes server identity/tenant/audit
```

- Verification contribution: `BackendCrudTemplateTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 5 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/query.java.ftl`

- Purpose: 应用详情/分页输入。
- Symbols: `query.java.ftl`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `BackendCrudTemplateTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: DetailQuery/PageQuery按需要生成，不把领域和应用包互相倒置。
- Input/output and state mapping: 应用详情/分页输入按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```text
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Builder class [=queryType]
Detail variant requires positive Long id and route keys; page variant composes Common PageQuery
Reject unsupported filter/sort before calling DomainService; use no wrapper or SQL fragment fields
```

- Verification contribution: `BackendCrudTemplateTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 6 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/result.java.ftl`

- Purpose: 公开查询输出。
- Symbols: `result.java.ftl`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `BackendCrudTemplateTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: *Result class，外部 ID 显式字符串映射，字段清单固定。
- Input/output and state mapping: 公开查询输出按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```text
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Builder class [=resultType]
Result fields follow read exposure policy and Jackson mapping; map Long ID to decimal String at boundary
Never expose PO, tenant/audit internals or sensitive fields by wildcard; reuse existing page response semantics
```

- Verification contribution: `BackendCrudTemplateTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 7 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/converter.java.ftl`

- Purpose: 必需边界转换。
- Symbols: `converter.java.ftl`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `BackendCrudTemplateTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: PO↔Domain 和 Application↔Domain各自MapStruct文件；复用BaseConverter/Forward。
- Input/output and state mapping: 必需边界转换按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```text
@Mapper(componentModel="spring", unmappedTargetPolicy=ERROR) interface [=converterType] extends [=commonConverterContract]
Emit explicit ignored inherited/server fields and allowed business field mappings; version passed from request not latest row
Use named decimal ID conversion with validation, disable Builder in mapping only when verified required; every generated Bean name matches Qualifier
```

- Verification contribution: `BackendCrudTemplateTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 8 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/domain-service.java.ftl`

- Purpose: Domain基础端口。
- Symbols: `domain-service.java.ftl`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `BackendCrudTemplateTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: create/update/delete/detail/query业务契约；不能返回PO/MP类型。
- Input/output and state mapping: Domain基础端口按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```text
@Validated interface [=domainServiceType] { [=domainBoType] create(@Valid [=domainBoType] value); }
Declare detail/update/delete/query using Domain BO/Query and Common PageSlice, with explicit id/version/route preconditions
No generic technical CRUD inheritance, no Application imports; errors use existing configured common/domain hierarchy
```

- Verification contribution: `BackendCrudTemplateTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 9 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/domain-impl.java.ftl`

- Purpose: Infrastructure领域实现。
- Symbols: `domain-impl.java.ftl`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `BackendCrudTemplateTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: 薄编排 Repository+converter，完整更新和版本删除。
- Input/output and state mapping: Infrastructure领域实现按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```text
@Service("[=domainServiceBean]") @Validated @Slf4j @RequiredArgsConstructor class [=domainImplType] implements [=domainServiceType]
create converts whitelisted fields -> repo.save -> authoritative persisted result; failure maps via existing error contract
update loads scoped current metadata, retains tenant/creation fields, applies writable values and caller expectedVersion -> repo.updateById
delete uses version-bearing PO and repo.removeById(PO); never discard expectedVersion by using the ID-only reload path
```

- Verification contribution: `BackendCrudTemplateTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 10 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/manage.java.ftl`

- Purpose: Application用例接口。
- Symbols: `manage.java.ftl`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `BackendCrudTemplateTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: 基础CRUD方法与载体；不泄漏持久层。
- Input/output and state mapping: Application用例接口按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```text
@Validated interface [=manageType] exposes create/update/delete/detail/page methods with @Valid inputs
Use Application Command/Query/Result plus existing page contract; document no Event side effect in v1
Service profile exports these methods for existing adapters without inventing an HTTP/RPC protocol
```

- Verification contribution: `BackendCrudTemplateTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 11 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/manage-impl.java.ftl`

- Purpose: Application事务实现。
- Symbols: `manage-impl.java.ftl`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `BackendCrudTemplateTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: 命名Bean+constructor qualifiers，写事务和查询只读边界。
- Input/output and state mapping: Application事务实现按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```text
@Service("[=manageBean]") @RequiredArgsConstructor @Slf4j @Validated class [=manageImplType]
@Transactional write method validates group, converts to domain values, invokes DomainService, maps result and logs safe outcome
read-only detail/page uses query contract and bounded page semantics; no infrastructure types, datasource setup or template runtime
```

- Verification contribution: `BackendCrudTemplateTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 12 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/controller.java.ftl`

- Purpose: 可选 HTTP CRUD。
- Symbols: `controller.java.ftl`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `BackendCrudTemplateTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: 已配置错误/认证边界；POST/GET/PUT/DELETE，Service禁用。
- Input/output and state mapping: 可选 HTTP CRUD按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```text
@RestController("[=controllerBean]") @RequestMapping("[=apiBasePath]") @RequiredArgsConstructor @Slf4j
POST creates; GET id/details and collection/page are read-only; PUT id binds full editable fields and expectedVersion; DELETE binds expectedVersion
Validate positive decimal String IDs then map to Long, invoke Manage only; use configured project auth/error contracts and no client tenant/audit
Emit route/OpenAPI metadata only when dependency/preflight proves existing stack; do not create security, filter, or exception infrastructure
```

- Verification contribution: `BackendCrudTemplateTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-code-generator -am -Dtest=BackendCrudTemplateTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: 退出0，目标测试真实被发现且全部通过；无匹配测试跳过不能视为完成。首次RED须记录具体断言，再记录GREEN。
- Failure returns to: 本Step对应失败测试及被测文件；依赖或AST能力不足先阻断，不引入替代库、不削弱规范。
- Completion criteria: 在真实领域端口边界生成完整基础CRUD；仅Light/Web具备已配置HTTP入口。 具备聚焦证据，git diff --check通过且只包含上述文件；Step 5手工检查逐项留证。
- Rollback: 仅撤回尚未提交的本Step自有修改；已提交后使用单独获准纠正任务，不重写历史，不删除别人数据/源码。工具运行写入的恢复由journal契约负责。
- Commit paths: `egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/BackendCrudTemplateTest.java`; `egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/domain-model.java.ftl`; `egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/domain-query.java.ftl`; `egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/command.java.ftl`; `egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/query.java.ftl`; `egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/result.java.ftl`; `egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/converter.java.ftl`; `egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/domain-service.java.ftl`; `egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/domain-impl.java.ftl`; `egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/manage.java.ftl`; `egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/manage-impl.java.ftl`; `egon-cola-components/egon-cola-component-code-generator/src/main/resources/templates/backend/common/controller.java.ftl`
- Commit: `feat(codegen): generate layered backend CRUD scaffolding`

### Step 6 — 生成计划、冲突与安全应用

- Requirements: PLAN-REQ-007, PLAN-REQ-008
- Dependencies: Step 5 已验证并提交；前面步骤能力保留
- Baseline state: 已完成前5步，只新增本步明确行为，不改已验证契约。
- Observable outcome: 三方文件判定、逐产物版本和可恢复应用闭合，不覆盖人工修改。
- End state: 下列文件完整，GenerationUpdateTest GREEN；未执行数据库、应用或发布，后续步骤仍待实现。
- Test-first gate: Required — GenerationUpdateTest 先固定下面伪代码中的失败行为；缺少类型时只补编译契约，RED必须是缺行为断言失败。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/GenerationUpdateTest.java`

- Purpose: 文件行为 RED 与故障注入。
- Symbols: `GenerationUpdateTest.java`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `GenerationUpdateTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 本Step编译/RED前置，先锁定预期。
- Contract/signature changes: TempDir、hash漂移、只生成PO、符号链接、错误中断与恢复。
- Input/output and state mapping: 文件行为 RED 与故障注入按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```java
plan -> apply twice yields byte-identical files and NO_CHANGE on second pass
modify DAO manually, request mapper-only update, assert DAO unchanged and pending impacts retained
inject failure after replacing first file; recover only journal-owned paths; new human edits become CONFLICT
reject symlink escape, stale input/template/component fingerprint, unlisted destructive action and arbitrary plan candidate path
```

- Verification contribution: `GenerationUpdateTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 2 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/update/GenerationPlanService.java`

- Purpose: Schema+产物影响与候选计划。
- Symbols: `GenerationPlanService.java`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `GenerationUpdateTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: plan(config,schema,baseline)；组装固定所有状态字段、稳定SHA-256 planId。
- Input/output and state mapping: Schema+产物影响与候选计划按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```java
diff normalized schema and per-artifact lastGeneratedInput; classify rename/type/null/default/drop and record pending consumers
for requested artifact compare baseline hash vs disk hash vs newly rendered hash; unknown/manual files become CONFLICT
stage candidates under authorized state root after preflight; produce relative paths and checksums only
hash canonical plan excluding planId; no wall clock/randomness; plan cannot widen requested artifacts or apply changes
```

- Verification contribution: `GenerationUpdateTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 3 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/update/GenerationStateRepository.java`

- Purpose: 本地状态文件访问。
- Symbols: `GenerationStateRepository.java`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `GenerationUpdateTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: 配置/Schema/逐文件manifest/journal读写，唯一IO边界；非数据库Repo不继承MP。
- Input/output and state mapping: 本地状态文件访问按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```java
load formatVersion 1 state with strict Jackson DTO and normalized relative keys; corruption blocks, never resets silently
write snapshots/manifests via same-filesystem temporary files and replacement; keep previous state until commit
persist latestObservedSchema separately from each artifact lastGeneratedInput and previousGeneratedHash
read/write only allowed .egon/codegen paths; expose candidate/backup/journal operations without permitting arbitrary file access
```

- Verification contribution: `GenerationUpdateTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 4 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/update/GenerationApplyService.java`

- Purpose: 计划复核、替换与恢复状态机。
- Symbols: `GenerationApplyService.java`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `GenerationUpdateTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: apply(plan,acceptedActionIds)/recover(planId,mode)；精确动作授权无force。
- Input/output and state mapping: 计划复核、替换与恢复状态机按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```java
acquire per-real-root file lock; verify current roots, all fingerprints, expectedDiskHash and candidateHash before first write
require exact acceptedActionIds for destructive/contract changes; immutable selected scope and dependencies must still match
journal PREPARED -> WRITING; backup and replace each file on same filesystem, append successful hash; commit only proven artifact states
failure -> RECOVERY_REQUIRED; resume/rollback verifies current hash matches this run, never overwrite a subsequent human modification
```

- Verification contribution: `GenerationUpdateTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 5 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/validation/OutputPathValidator.java`

- Purpose: 路径与目录身份检查。
- Symbols: `OutputPathValidator.java`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `GenerationUpdateTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: real root绑定、文件名冲突、大小写冲突、符号链接和输出越界。
- Input/output and state mapping: 路径与目录身份检查按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```java
resolve real existing ancestor then compare canonical outputRoot binding; refuse symlinked escapes and unexpected parent changes
validate every target/candidate/backup/journal relative path has no traversal and stays within its allowed root
detect normalized duplicate paths, FQN/Bean/mapper-statement collisions before rendering/apply
new root creation only follows approved config, use no recursive deletion or arbitrary filesystem cleanup
```

- Verification contribution: `GenerationUpdateTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-code-generator -am -Dtest=GenerationUpdateTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: 退出0，目标测试真实被发现且全部通过；无匹配测试跳过不能视为完成。首次RED须记录具体断言，再记录GREEN。
- Failure returns to: 本Step对应失败测试及被测文件；依赖或AST能力不足先阻断，不引入替代库、不削弱规范。
- Completion criteria: 三方文件判定、逐产物版本和可恢复应用闭合，不覆盖人工修改。 具备聚焦证据，git diff --check通过且只包含上述文件；Step 6手工检查逐项留证。
- Rollback: 仅撤回尚未提交的本Step自有修改；已提交后使用单独获准纠正任务，不重写历史，不删除别人数据/源码。工具运行写入的恢复由journal契约负责。
- Commit paths: `egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/GenerationUpdateTest.java`; `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/update/GenerationPlanService.java`; `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/update/GenerationStateRepository.java`; `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/update/GenerationApplyService.java`; `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/validation/OutputPathValidator.java`
- Commit: `feat(codegen): apply generation plans without overwriting custom code`

### Step 7 — CLI、启动脚本与工具说明

- Requirements: PLAN-REQ-009
- Dependencies: Step 6 已验证并提交；前面步骤能力保留
- Baseline state: 已完成前6步，只新增本步明确行为，不改已验证契约。
- Observable outcome: 真实工具入口返回结构化摘要和稳定退出码，无自动下载或服务启动。
- End state: 下列文件完整，CodegenCommandTest GREEN；未执行数据库、应用或发布，后续步骤仍待实现。
- Test-first gate: Required — CodegenCommandTest 先固定下面伪代码中的失败行为；缺少类型时只补编译契约，RED必须是缺行为断言失败。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/CodegenCommandTest.java`

- Purpose: 命令参数及退出码 RED。
- Symbols: `CodegenCommandTest.java`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `CodegenCommandTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 本Step编译/RED前置，先锁定预期。
- Contract/signature changes: 内存streams/TempDir覆盖templates/plan/apply/check/recover与未知选项。
- Input/output and state mapping: 命令参数及退出码 RED按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```java
invoke each command with explicit config/state and capture stdout/stderr independently
assert stdout contains one valid JSON object; progress/errors go to stderr; codes follow Spec 14.4
assertNoSpringApplicationOrDatabaseStarts(); assertMissingClasspathFailsBeforeExecution()
assertApplyRequiresExistingPlanAndValidActions(); check drift returns 8 without mutating files
```

- Verification contribution: `CodegenCommandTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 2 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/cli/CodegenCommand.java`

- Purpose: 普通 main 与显式依赖装配。
- Symbols: `CodegenCommand.java`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `CodegenCommandTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: main delegates run(args,out,err)返回int；最终唯一System.exit。
- Input/output and state mapping: 普通 main 与显式依赖装配按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```java
parse fixed command/flag grammar using JDK; reject unknown/duplicate flags and missing operands
construct existing ValidationUtils/Jackson and tool collaborators explicitly; do not bootstrap Spring or MP autoconfiguration
validate config -> read DDL -> scope/dep preflight -> render plan; apply/check/recover call their exact services
write structured diagnostic JSON, return declared code; preserve exceptions as causes without dumping credentials or generated source
```

- Verification contribution: `CodegenCommandTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 3 — `CREATE scripts/egon-codegen.sh`

- Purpose: 离线launcher。
- Symbols: `egon-codegen.sh`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `CodegenCommandTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: EGON_CODEGEN_CLASSPATH显式提供；java主类固定，参数原样数组传递。
- Input/output and state mapping: 离线launcher按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```text
set -euo pipefail
require JAVA executable and nonempty EGON_CODEGEN_CLASSPATH whose entries exist; print BLOCKED_TOOLING on missing files
exec java -cp "$EGON_CODEGEN_CLASSPATH" top.egon.cola.component.codegen.cli.CodegenCommand "$@"
Never run Maven install, curl, package managers or application startup from this script
```

- Verification contribution: `CodegenCommandTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 4 — `CREATE egon-cola-components/egon-cola-component-code-generator/README.md`

- Purpose: 可运行操作说明与依赖声明。
- Symbols: `README.md`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `CodegenCommandTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: 写真实构建/classpath准备、选择范围、DDL变化、人工冲突与recover边界。
- Input/output and state mapping: 可运行操作说明与依赖声明按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```text
Document build via repository mvnw and generator module; classpath prepared explicitly using existing Maven dependency plugin
Show templates -> plan -> diff review -> apply -> check with exact JSON config and exit codes
Explain FreeMarker tool-only dependency, native profiles, immutable DDL, unsupported grammar and manual-file conflicts
State PostgreSQL/MQ runtime unverified and source-copy compile isolation; do not imply wrapper installs tools automatically
```

- Verification contribution: `CodegenCommandTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 5 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/test/resources/codegen-web.json`

- Purpose: 无秘密的完整示例配置。
- Symbols: `codegen-web.json`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `CodegenCommandTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: 当前配置结构与fieldPolicies，orders逻辑表，根目录由测试注入。
- Input/output and state mapping: 无秘密的完整示例配置按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```text
configVersion=1; projectType=web; artifacts=[po,dao,mapper-xml,repo]; input mode=schema
modulePaths explicitly identify existing infrastructure/domain/application/adapter modules; fieldPolicies exclude audit/tenant
test substitutes only outputRoot and resourceRoot inside its TempDir; no hard-coded credentials, live URL or user workspace mutation
```

- Verification contribution: `CodegenCommandTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-code-generator -am -Dtest=CodegenCommandTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: 退出0，目标测试真实被发现且全部通过；无匹配测试跳过不能视为完成。首次RED须记录具体断言，再记录GREEN。
- Failure returns to: 本Step对应失败测试及被测文件；依赖或AST能力不足先阻断，不引入替代库、不削弱规范。
- Completion criteria: 真实工具入口返回结构化摘要和稳定退出码，无自动下载或服务启动。 具备聚焦证据，git diff --check通过且只包含上述文件；Step 7手工检查逐项留证。
- Rollback: 仅撤回尚未提交的本Step自有修改；已提交后使用单独获准纠正任务，不重写历史，不删除别人数据/源码。工具运行写入的恢复由journal契约负责。
- Commit paths: `egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/CodegenCommandTest.java`; `egon-cola-components/egon-cola-component-code-generator/src/main/java/top/egon/cola/component/codegen/cli/CodegenCommand.java`; `scripts/egon-codegen.sh`; `egon-cola-components/egon-cola-component-code-generator/README.md`; `egon-cola-components/egon-cola-component-code-generator/src/test/resources/codegen-web.json`
- Commit: `feat(codegen): expose offline generator CLI`

### Step 8 — 生成产物验收与 skill 实际接线

- Requirements: PLAN-REQ-010, PLAN-REQ-011
- Dependencies: Step 7 已验证并提交；前面步骤能力保留
- Baseline state: 已完成前7步，只新增本步明确行为，不改已验证契约。
- Observable outcome: 模板编译、三种真实源工程临时副本与skill全链路验证，保留运行证明边界。
- End state: 下列文件完整，CodegenAcceptanceTest GREEN；未执行数据库、应用或发布，后续步骤仍待实现。
- Test-first gate: Required — CodegenAcceptanceTest 先固定下面伪代码中的失败行为；缺少类型时只补编译契约，RED必须是缺行为断言失败。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/CodegenAcceptanceTest.java`

- Purpose: 最终行为矩阵。
- Symbols: `CodegenAcceptanceTest.java`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `CodegenAcceptanceTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 本Step编译/RED前置，先锁定预期。
- Contract/signature changes: 真实引擎、Lombok/MapStruct、基类与Mapper映射；不mock模板输出。
- Input/output and state mapping: 最终行为矩阵按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```java
generate compliant orders schema into three temporary source layouts; compile all selected Java with JavaCompiler and real processors
assert generated domain/adapter/infrastructure package edges and no FreeMarker dependency/import in output
exercise CRUD repository/mapping via existing isolated test capabilities; no live PostgreSQL or broker connection
cover repeated generation, partial selection, schema evolution, custom edits, unknown grammar, exact dependency gaps and recovery fault points
```

- Verification contribution: `CodegenAcceptanceTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 2 — `CREATE scripts/test-egon-codegen.sh`

- Purpose: 临时源工程编译门槛。
- Symbols: `test-egon-codegen.sh`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 当前路径不存在；方案§4/§14定义该责任，结构参考 `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` 与 native Web 的对应契约。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `CodegenAcceptanceTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: 参数light/web/service或all；复制正常源码到mktemp，生成新域并执行compile/architecture checks。
- Input/output and state mapping: 临时源工程编译门槛按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```text
create task-owned temporary directory; copy selected source-project excluding target/.git and existing generated caches
run tool against explicit new codegenfixture domain and compliant schema; provide local installed dependencies explicitly
invoke existing mvnw -Ptest compile and selected architecture verification without invoking Spring Boot run
trap deletes only the exact temporary directory created by this test; report baseline failures separately; never alter source-project POM/SQL in workspace
```

- Verification contribution: `CodegenAcceptanceTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 3 — `MODIFY .agents/skills/egon-coding-writing-spec/references/backend-code-generation.md`

- Purpose: 将方案态路由切换为真实工具工作流。
- Symbols: `backend-code-generation.md`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 已有 `.agents/skills/egon-coding-writing-spec/references/backend-code-generation.md`；核对当前内容后只作列出的增量。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `CodegenAcceptanceTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: 已实现命令、配置版本、产物选择及报告定位；保留依赖审批门槛。
- Input/output and state mapping: 将方案态路由切换为真实工具工作流按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```text
Replace proposal-only availability text only after CodegenAcceptanceTest and launcher tests pass
Discover scripts/egon-codegen.sh, verify template/profile/version inventory and required local classpath
For approved template tasks call plan then inspect scope/conflicts and apply only authorized file actions
Read machine summary and diff, never retype generated boilerplate; missing dependency/tool blocks without auto-install
```

- Verification contribution: `CodegenAcceptanceTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 4 — `MODIFY .agents/skills/egon-coding-writing-plan/references/backend-code-generation.md`

- Purpose: 将方案态路由切换为真实工具工作流。
- Symbols: `backend-code-generation.md`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 已有 `.agents/skills/egon-coding-writing-plan/references/backend-code-generation.md`；核对当前内容后只作列出的增量。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `CodegenAcceptanceTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: 已实现命令、配置版本、产物选择及报告定位；保留依赖审批门槛。
- Input/output and state mapping: 将方案态路由切换为真实工具工作流按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```text
Replace proposal-only availability text only after CodegenAcceptanceTest and launcher tests pass
Discover scripts/egon-codegen.sh, verify template/profile/version inventory and required local classpath
For approved template tasks call plan then inspect scope/conflicts and apply only authorized file actions
Read machine summary and diff, never retype generated boilerplate; missing dependency/tool blocks without auto-install
```

- Verification contribution: `CodegenAcceptanceTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 5 — `MODIFY .agents/skills/egon-coding-executing-plan/references/backend-code-generation.md`

- Purpose: 将方案态路由切换为真实工具工作流。
- Symbols: `backend-code-generation.md`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 已有 `.agents/skills/egon-coding-executing-plan/references/backend-code-generation.md`；核对当前内容后只作列出的增量。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `CodegenAcceptanceTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: 已实现命令、配置版本、产物选择及报告定位；保留依赖审批门槛。
- Input/output and state mapping: 将方案态路由切换为真实工具工作流按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```text
Replace proposal-only availability text only after CodegenAcceptanceTest and launcher tests pass
Discover scripts/egon-codegen.sh, verify template/profile/version inventory and required local classpath
For approved template tasks call plan then inspect scope/conflicts and apply only authorized file actions
Read machine summary and diff, never retype generated boilerplate; missing dependency/tool blocks without auto-install
```

- Verification contribution: `CodegenAcceptanceTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

#### File 6 — `MODIFY .agents/skills/README-egon-coding-spec-plan.md`

- Purpose: 更新实际生成流程入口。
- Symbols: `README-egon-coding-spec-plan.md`；具体接口/字段/方法见本文件契约和伪代码，不创建未列明转发层。
- Repository evidence: 已有 `.agents/skills/README-egon-coding-spec-plan.md`；核对当前内容后只作列出的增量。
- Dependencies and consumers: 使用前序Step契约；由本Step的 `CodegenAcceptanceTest`、后续CLI/模板验收消费；依赖仅方案§5清单。
- Why now: 前序文件已限定输入输出；此位置提供后续文件所需能力，最终按本Step命令验证。
- Contract/signature changes: 保留三个skill分工，链接真实tool README；不宣称覆盖未实现模板。
- Input/output and state mapping: 更新实际生成流程入口按方案§6/§8/§14字段定义；只改变此文件或其明确产物，DDL来源、基础字段/默认值、version和路径范围不能丢失。
- Error and edge behavior: 无效输入带稳定诊断返回，失败不修改目标项目；不引入未批准依赖。
- Standards impact: MC-ARCH-001, MC-DEP-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — 遵循本文件契约；不适用的Spring/时间/模型项由§12给出边界，不能自动添框架。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 普通载体class/按父类Builder；仅Spring管理产物要求显式Bean/Qualifier，工具不启动容器；Common转换/注解校验、JSON与java.time按§4.8及文件伪代码应用。
- Implementation pseudocode:

```text
Describe Spec selecting DDL/profile/artifacts, Plan fixing config and required existing types, Execute using verified launcher
List supported native profiles and generator version; Agent/Open/traditional remain unsupported tool profiles
Explain FreeMarker approval scope and token-saving summary workflow; existing manual business code remains user-owned
```

- Verification contribution: `CodegenAcceptanceTest` 验证本文件契约；最终§8补实际产物编译/CLI验收，不以文本匹配代替行为。
- After this file: 此责任可被后序文件调用/渲染；Step测试可能仍RED，直到本Step所有文件完成与聚焦验证GREEN后才可提交。

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-code-generator -am -Dtest=CodegenAcceptanceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: 退出0，目标测试真实被发现且全部通过；无匹配测试跳过不能视为完成。首次RED须记录具体断言，再记录GREEN。
- Failure returns to: 本Step对应失败测试及被测文件；依赖或AST能力不足先阻断，不引入替代库、不削弱规范。
- Completion criteria: 模板编译、三种真实源工程临时副本与skill全链路验证，保留运行证明边界。 具备聚焦证据，git diff --check通过且只包含上述文件；Step 8手工检查逐项留证。
- Rollback: 仅撤回尚未提交的本Step自有修改；已提交后使用单独获准纠正任务，不重写历史，不删除别人数据/源码。工具运行写入的恢复由journal契约负责。
- Commit paths: `egon-cola-components/egon-cola-component-code-generator/src/test/java/top/egon/cola/component/codegen/CodegenAcceptanceTest.java`; `scripts/test-egon-codegen.sh`; `.agents/skills/egon-coding-writing-spec/references/backend-code-generation.md`; `.agents/skills/egon-coding-writing-plan/references/backend-code-generation.md`; `.agents/skills/egon-coding-executing-plan/references/backend-code-generation.md`; `.agents/skills/README-egon-coding-spec-plan.md`
- Commit: `feat(codegen): verify generated projects and connect coding skills`

## 8. Test, Validation, and Quality Gates

| Gate | Working directory / command | Expected result | Failure action / boundary |
| --- | --- | --- | --- |
| 每Step聚焦 | 仓库根，§7各精确mvnw命令 | 目标JUnit测试发现且0失败 | 修本Step；无测试/环境故障不能算GREEN |
| 工具模块完整 | `./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-code-generator -am verify` | 组件依赖与工具全部测试通过 | 若基线依赖失败区分已有问题，禁止顺手修邻近模块 |
| 依赖树 | `./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-code-generator dependency:tree -Dscope=runtime` | 新外部引擎仅FreeMarker2.3.35，实际内部/传递依赖与批准清单相符 | 发现额外引入停止；不能排除必要库来隐瞒问题 |
| 产物真实编译 | CodegenAcceptanceTest 的 JavaCompiler 与 Lombok/MapStruct处理器 | 三类型与各范围产物可编译，Converter生成实现/Qualifier成立 | 不mock处理器或只校验字符串 |
| 临时源项目 | `bash scripts/test-egon-codegen.sh all`，显式已准备classpaths | native light/web/service副本编译与各自架构门槛通过 | 只清本测试临时目录；不得修改原source项目 |
| skill资源 | `python3 .agents/skills/egon-coding-writing-spec/scripts/validate_skill_resources.py`；plan/execute同路径对应脚本 | 新命令与资源引用有效 | 保持工具不可用时阻断分支 |
| Skill回归 | `PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s .agents/skills/egon-coding-writing-plan/scripts -p 'test_*.py'`；spec/execute同样执行 | 既有规则/资源检查全部通过 | 不改测试以规避规范 |
| Diff | `git diff --check`，逐Step explicit paths | 无无关修改、秘密、build产物 | 清理仅本任务临时文件 |

最终验收覆盖PLAN-REQ全部场景，包括未知DDL、非兼容基础字段、旧唯一约束、人工编辑/路径逃逸、版本冲突、局部生成未同步消费者、候选被改、精确破坏性动作与崩溃后再次人工编辑。真实PostgreSQL锁/路由/MQ消息交付不在本轮实现验收承诺，需另行用户授权；本工具不会创建这些服务。

## 9. Migration, Compatibility, Rollout, and Rollback

无业务数据库变更，无新migration，无DDL执行。只新增开发工具和模板，保留历史DDL/Manifest checksum与既有source/archetype业务文件。应用不依赖生成器，引擎升级只改变template/component fingerprint并令旧Plan失效。

模板迁移使用显式计划，不覆盖人工文件。首次接管已有文件必须明确批准具体路径和基线；首版默认拒绝接管。生成状态schema版本不认识则报错，不自动重置。客户端/接口兼容由字段策略与破坏性action报告处理；不能将DROP/rename当普通重新渲染。

工具发布/远程安装/平台集成是单独授权事项；本Plan没有deploy或服务启动命令。回滚代码仅一个逻辑任务的文件；回滚生成行为核对journal和当前文件hash，用户后续更改始终优先保护。

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Source | Steps | Evidence |
| --- | --- | --- | --- |
| PLAN-REQ-001 | §3、§7 | 1, 4 | 三种布局与单产物测试，Service 拒绝 Controller，未选路径不变；对应§7目标测试与§8闭环 |
| PLAN-REQ-002 | §5、§14.1 | 1, 3 | 依赖清单除 FreeMarker/已列内部组件无新增外部工件，目标 POM 不变；对应§7目标测试与§8闭环 |
| PLAN-REQ-003 | §6、§14.2 | 2 | 实际 native DDL 解析；未知语法与历史漂移失败，不运行 SQL；对应§7目标测试与§8闭环 |
| PLAN-REQ-004 | §4、§14.1 | 3 | 绑定符原样、缺字段失败、UTF-8/LF 与重复输出一致；对应§7目标测试与§8闭环 |
| PLAN-REQ-005 | §7、§8 | 4 | 必需 XML 方法可执行、class/Builder/租户/version/路由/唯一键核对；对应§7目标测试与§8闭环 |
| PLAN-REQ-006 | §8、§14.3 | 5 | 生成代码编译、版本冲突、字段暴露、分页/错误/认证边界有测试；对应§7目标测试与§8闭环 |
| PLAN-REQ-007 | §9、§14.4 | 6 | 人工文件不覆盖，部分生成不把未选消费者标成同步；对应§7目标测试与§8闭环 |
| PLAN-REQ-008 | §9、§14.4 | 6 | 过期计划失败，写入故障可恢复，恢复不覆盖新人工编辑；对应§7目标测试与§8闭环 |
| PLAN-REQ-009 | §10、§14.5 | 7 | templates/plan/apply/check/recover 协议测试、无自动下载或服务启动；对应§7目标测试与§8闭环 |
| PLAN-REQ-010 | §11 | 8 | 工具发现、缺依赖阻断、已批准 FreeMarker 不重复询问；对应§7目标测试与§8闭环 |
| PLAN-REQ-011 | §12、§14.6 | 8 | 每个证据分层独立，未运行的 PostgreSQL/MQ 不冒充通过；对应§7目标测试与§8闭环 |

## 11. Risks, Blockers, and User Decisions

| Concern | Evidence | Decision | Closure / execution gate |
| --- | --- | --- | --- |
| 模板引擎 | 用户最后明确FreeMarker，官方2.3.35 | 已关闭选型；Velocity已排除 | 本Plan审核后按Step1引入，不重复询问同项 |
| parser方言覆盖 | 实际role DO、索引/约束复杂，尚未执行 | 设计冻结既有JSQLParser+有限角色分支 | Step2真实测试失败就停止并提具体扩展方案；当前不声称已支持 |
| 新内部模块/布局 | 用户认可其余方向，Components当前结构 | 本Review Plan列出准确依赖和文件 | 待用户审核Plan，不因规划而自动编码 |
| 旧DDL不符合新unique规则 | native旧初始化仍存在单业务列UNIQUE | 解析可成功，合规应失败；不得修改旧DDL | acceptance另用合规测试表，源样本保留失败预期 |
| 完整编译环境 | 本轮仅静态检查，未安装/编译生成器 | 将环境依赖作为显式执行前置 | 无真实目标测试结果不得标完成 |
| 源码人工修改与部分写入 | 重生成必然可能遇到 | 不自动合并，日志可恢复且检查后续编辑 | Step6故障注入和人工修改测试关闭风险 |

没有需要在设计中猜测的引擎/范围选择；剩余是明确定义的实施验证门槛与Plan审核。若执行时证据导致契约/依赖改变，返回方案修订，不在步骤中静默设计新架构。

## 12. Review and Acceptance

### 12.1 Original requirement fidelity

DDL与变化适配、指定路径/模板范围、三种native项目、仅后端基础CRUD、复用MP与平台现有契约、FreeMarker例外和其他依赖阻断都分别追踪。Agent/前端/自动改DDL不在范围。

### 12.2 Plan/Spec consistency

当前方案R3已收敛FreeMarker、数据契约、状态机和退出码；本Plan没有恢复Velocity或新增模板引擎。PLAN-REQ只做追踪别名。proposal为唯一legacy Spec，不创建冗余Spec。

### 12.3 Repository executability

路径与现有POM/API已读取；新增路径由§5明示。不把未来Java方法伪装成现有API，也不把聚焦测试命令写成已通过结果。source-projects业务代码保持不变，只用临时副本验证。

### 12.4 Verification boundary

当前只运行文档与skill静态校验；没有生产代码实现、Maven构建、引擎安装、数据库/应用/浏览器启动。下表PASS表示规划证据完整，不表示实现验收完成。

### 12.5 Blocking Manual Check

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | §3/§4.7/Step4三种native布局及工具宿主边界 | 设计/计划已明确证据与执行要求 | None |
| MC-REUSE-001 | Applicable | PASS | §4.7列出MP、Converter、PageQuery/PageSlice与现有parser | 设计/计划已明确证据与执行要求 | None |
| MC-DEP-001 | Applicable | PASS | §5依赖清单、Step1与最终FreeMarker授权，产物无引擎 | 设计/计划已明确证据与执行要求 | None |
| MC-NAME-001 | Applicable | PASS | Step1 BO及§5所有行为/模板名称，按职责避免类膨胀 | 设计/计划已明确证据与执行要求 | None |
| MC-VALID-001 | Applicable | PASS | Step1分组约束、Step4 preflight、Step5层间注解及负例 | 设计/计划已明确证据与执行要求 | None |
| MC-MODEL-001 | Applicable | PASS | Step1普通class基线、Step4继承PO与Builder分场景 | 设计/计划已明确证据与执行要求 | None |
| MC-CONVERT-001 | Applicable | PASS | Step5 converter.ftl与Common BaseConverter/Forward，真实处理器测试 | 设计/计划已明确证据与执行要求 | None |
| MC-LOG-001 | Applicable | PASS | Step7 JSON stdout/stderr分离，工具/产物日志不泄露秘密 | 设计/计划已明确证据与执行要求 | None |
| MC-BEAN-001 | Applicable | PASS | Step1 lombok.config和Step5明确Bean/Qualifier；CLI不启动Spring | 设计/计划已明确证据与执行要求 | None |
| MC-UTIL-001 | Applicable | PASS | JDK/已有Commons，FreeMarker只做获准引擎，不装外部生成器 | 设计/计划已明确证据与执行要求 | None |
| MC-JSON-001 | Applicable | PASS | §14配置/plan格式对应Step1/6/7严格Jackson和未知字段错误 | 设计/计划已明确证据与执行要求 | None |
| MC-TIME-001 | Applicable | PASS | UTC/java.time/确定性渲染，不记录生成时刻，Step3测试 | 设计/计划已明确证据与执行要求 | None |
| MC-CONFIG-001 | Applicable | PASS | 工具无Spring profile修改；缺业务配置阻断，Step4/5 | 设计/计划已明确证据与执行要求 | None |
| MC-PATTERN-001 | Applicable | PASS | Step2 Adapter与Step4布局Strategy有真实变化点，其余无注册框架 | 设计/计划已明确证据与执行要求 | None |
| MC-SCOPE-001 | Applicable | PASS | §5文件表与baseline dirty边界，只目标路径与本工具，不动数据库 | 设计/计划已明确证据与执行要求 | None |
| MC-TEST-001 | Applicable | PASS | §7 RED/GREEN和§8分层编译/临时源项目门槛完整 | 设计/计划已明确证据与执行要求 | None |
| MC-BLOCKER-001 | Applicable | PASS | §11已关闭选型；仅Plan审核与实施测试未执行，不冒充运行证明 | 设计/计划已明确证据与执行要求 | None |

### 12.6 Final verdict

PASS — Ready for user review

状态为Review，等待用户审核此Plan；不表示获准开始实现。实施时必须重新核对源码/依赖，按步骤验证并形成受限提交，不能用本次文档检查代替任何代码或运行验收。
