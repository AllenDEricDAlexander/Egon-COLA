> 本文件是 `SKILL.md` 的全中文审核镜像，不是 Codex 自动发现的运行入口。修改任何规则时，必须同步更新英文运行版与本文件。

# EGON Coding Plan 分步执行

## 目的

严格按照一份已获准执行的 coding Plan 逐 Step 落地。每次只完成一个 Step；只有该 Step 已通过验证并形成独立提交后，才能开始下一个 Step。全部 Step 完成后，必须脱离 Plan 的“已完成”结论，重新对照最终生效的 Spec 审核实际仓库，并如实报告所有未满足、部分满足或仅缺少运行时验证的要求。

Plan 决定实现顺序，最终生效的 Spec 决定实现是否正确。Plan 全部执行完，不等于实现一定满足 Spec。

## 资源完整性预检

把本 `SKILL.md` 所在目录解析为 `<skill-root>`。读取内置 Reference 或修改代码前运行：

```bash
python3 <skill-root>/scripts/validate_skill_resources.py
```

必须用解析后的绝对目录替换 `<skill-root>`。内置资源使用相对 Skill 根的 `references/` 或 `scripts/` 路径。预检报告缺失、越界、歧义或本地链接失效时，停止执行并报告准确诊断，修复/重装 Skill；不能用虚构或不完整 Checklist 继续。

## 用户强制 Java 规则——逐字规范源

以下规则逐字保留，在编码、Step、提交和最终审核门禁中都属于强制要求。具体检查契约见 `references/user-mandated-java-rules.zh-CN.md`。

```text
1 类名规范，必须以 java的pojo规范命名。以dao po bo vo dto query command event等结尾
2 每层之间必须被 springboot-validation 校验，复用的对象 validation 要分组校验，ValidatorUtils使用 libphonenumber进行规范化校验或者validation原生注解，若非必要，不要自己写。
3 实体类规范：复杂对象使用java类并使用Lombok进行@Data\@NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor\@RequiredArgsConstructor\@Builder\@Accessors(chain = true)修饰。简单对象使用Java Record。使用MapStruct、MapStructPlus 进行转换，egon-cola-component-common-core有通用的convertor，必须继承实现这个。如果是不可变对象，使用@Value注释修饰。record场景Record 构造器很适合做数据规范化，推荐使用紧凑构造器，Record 可以作为局部类，在方法内部定义临时数据结构。
4业务类必须使用@Slf4j注解注入log对象。如果业务类被spring管理，必须指定名称，如果是单例的情况下，参考@Service("userService")。如果需要依赖注入，必须@RequiredArgsConstructor进行修饰，不要代码中写。且属性必须被@qualify修饰。
5 工具类只允许使用jdk原生、Apache Commons(commons-lang3、commons-collections4、commons-io、commons-text、commons-codec、commons-beanutils)、Guava。针对Tika按需引入。
6 json 使用SpringBoot-JackSon 对外交互层的实体类必须按需被jackson注解修饰。
7 springboot 多环境配置文件，必须保持配置一致，但值不一定一致。
9 复杂业务必须引入设计模式，不允许硬编码
10 日期相关的必须使用java.time下的实体类，不允许使用java.util下的
11 plan中必须确认代码结构，分层结构或者egon-cola-archetype，只允许这两种代码结构规范。&#x20;
```

禁止翻译、重新编号、纠错、缩写、概括或弱化该区块。`references/user-mandated-java-rules.zh-CN.md` 只把原始拼写映射为准确代码符号，不能修改原文。

## 进入条件与执行授权

修改代码前必须：

1. 确认一份准确的 Plan 路径，并完整阅读该 Plan。
2. 解析并完整阅读 `Implements Spec` 指向的主 Spec，以及 `Effective Specs` 中的全部文档、修订、替代和依赖关系。
3. 阅读当前仓库及目标目录适用的全部 `AGENTS.md` 和仓库规则。
4. 确认 Plan 修订版本和仓库基线；针对下一 Step 重新核实真实路径、符号、迁移版本、消费者和验证命令。
5. 确认执行授权满足以下任一条件：
   - 通常情况下，Plan 状态为 `Ready`，主 Spec 状态为 `Accepted` 或 `Implemented`；或
   - 用户在当前对话中明确授权执行这些准确版本的 Plan 与 Spec。
6. 使用已安装 `egon-coding-writing-plan` skill 提供的结构校验器，对准确 Plan 启用 Strict Mode 校验。
7. 检查 `git status`、当前分支与 HEAD、暂存改动和未跟踪文件，保留所有无关工作。
8. Java 工作完整读取 `references/user-mandated-java-rules.zh-CN.md` 和 `references/java-spring-egon-coding-standards.zh-CN.md`。确认实际项目 Tree 是既有传统分层或准确已选 Egon-COLA Archetype；增加代码/依赖前重新验证 Plan 的 Spring/Starter/Egon/模块能力复用账本。

出现以下任一情况时，停止实现并找用户确认：Plan 目标不明确、缺少授权、有效 Spec 互相冲突、重大决策未关闭、Plan 结构无效，或者仓库漂移改变了架构、行为、契约、数据、安全、迁移、兼容性或 Step 的文件归属。

除非用户明确要求，不得启动服务、浏览器、数据库、本地技术栈或长时间运行的运行时测试。Plan 要求的源码检查、编译、静态检查以及聚焦或模块测试可以执行。

## Step 状态机

每个 Plan Step 必须严格遵循：

```text
待处理（Pending） -> 执行中（In Progress） -> 已验证（Verified） -> 已提交（Committed）
                         |                         |
                         +------> 阻塞（Blocked） <+
```

- `Pending`：尚未开始修改该 Step 拥有的文件。
- `In Progress`：只允许修改当前 Step 声明的文件。
- `Verified`：该 Step 的全部验证产生了新鲜的成功证据，且 diff 复核通过。
- `Committed`：已经形成一个路径受限、语义明确的提交，并核实了提交哈希与范围。
- `Blocked`：在获批 Plan 范围内无法安全达到 `Verified` 或 `Committed`。

**Step N 未达到 `Committed` 前，绝不能开始 Step N+1。** 只有测试通过、代码写完或已经暂存，都不算完成。

## 不可协商的执行规则

1. 严格按照 Plan 的 Step 顺序执行，不能提前修改后续 Step，也不能把多个 Step 合并为一次工作区改动或一次提交。
2. 同一时间最多只能有一个 Step 处于 `In Progress`。
3. 遵循当前 Step 规定的文件顺序、操作、真实符号、伪代码、需求和验证门禁，不能在执行阶段自行重新设计。
4. Plan 规定 RED/GREEN 的行为变更必须测试优先：先写聚焦测试并观察预期失败，再实现满足要求的最小代码，最后在持续通过测试的前提下重构或接线。
5. 只能修改当前 Step 声明的文件及其明确声明的生成产物。如果必须增加未声明的文件、公开契约、迁移、依赖或行为，则属于 Plan 漂移，必须停止并申请修订 Plan/Spec。
6. 保留所有无关的已修改或已暂存工作。禁止使用 `git add -A`、`git add .` 等宽泛暂存方式，只能明确暂存当前 Step 的路径。
7. 不得覆盖、回退、格式化、暂存或提交其他人的无关修改。无关修改与当前 Step 文件重叠时必须停止并报告。
8. 运行当前 Step 明确要求的验证，并补充能够证明该 Step 内部完整性的最小编译、静态或回归检查。
9. 必须阅读完整命令输出和退出码。进程句柄丢失、日志不完整、超时、测试跳过，或仓库规则视为失败的警告，都不能算通过。
10. 提交前根据当前 Step 要求和有效 Spec 审查 diff，移除意外文件、调试输出、秘密信息、生成噪声和无关重构。
11. 每个已验证 Step 必须立即形成独立、路径受限、语义明确的提交；Plan 建议的提交信息与最终 diff 及仓库规范一致时优先采用。
12. 禁止用空提交伪造 Step 完成。如果 Step 在执行前就已实现，或执行后没有语义 diff，应判定为仓库/Plan 漂移并停止请求指导。
13. 提交后核实提交哈希、文件列表、diff 摘要、验证证据以及剩余工作区状态，记录 Step 与提交的对应关系后才能继续。
14. 不得自动 amend、squash、reset 或改写已经形成的提交历史。如果后续 Step 暴露了早期提交的缺陷，应暂停推进，以最小的独立纠正提交归属到原 Step，重新执行受影响验证并报告偏差。
15. 禁止修改任何既有且不可变的 Flyway 迁移，只能创建获批 Plan/Spec 指定的新迁移文件。
16. 不得静默跳过、重排、合并、拆分或扩大 Step。任何实质性的执行顺序变化都必须获得用户批准。
17. 每个 Coding Step 都有阻断型 Manual Check。锁定 Step 时，从 `references/java-spring-egon-coding-standards.zh-CN.md` 枚举全部适用稳定 `MC-*`；提交前逐项用具体 Diff/路径/符号/命令证据人工校验。`MC-SCOPE-001` 与 `MC-TEST-001` 始终适用。
18. 只要任一适用 Manual Check 为 `FAIL`、`BLOCKED`、`UNKNOWN`、缺失、无证据或例外未关闭，Step 就不能到 `Verified`，也不能按完成提交。只有关注点真实不在 Step 范围时才允许有证据的 `N/A`。
19. 触达 Java 代码必须执行语义后缀、每个受影响交接的 Jakarta/Spring Validation 与 Group、获批规范化、准确 Record/`@Value`/复杂类完整 Lombok 分类、MapStruct/MapStructPlus 与强制 Egon `BaseConverter`、`@Slf4j`、显式 Bean 名、带 Qualifier 的 Lombok 构造注入、获准工具、Jackson、`java.time`、多环境配置一致性，以及每个 Complex 业务 Flow 的强制获批模式；不能借机大范围清理。
20. 必须逐字执行 `references/user-mandated-java-rules.zh-CN.md`。锁定 Step 和提交前分别执行 Rule 1、2、3、4、5、6、7、9、10、11，Rule 11 始终适用。禁止把规则压缩成泛化 Manual Check、改成建议或因测试通过而豁免。
21. 复杂对象必须保留完整 Lombok 基线；每个新增受影响 Converter 必须使用 MapStruct/MapStructPlus 加 Egon `BaseConverter`；构造器/框架/Converter 冲突会阻断。每个受影响层间交接必须校验，每个 Complex 业务 Flow 必须实现获批模式，不能直接分支。

## 单个 Step 的执行流程

每个 Step 开始和结束时都必须读取并使用 `references/step-gate-checklist.md`。

### 1. 锁定 Step

- 记录 Step 编号与标题、需求编号、依赖、声明路径、预期 RED/GREEN 行为、验证命令、回滚点和建议提交。
- 用 `git rev-parse HEAD` 记录本 Step 的基线。
- 确认所有依赖都已经由更早的提交哈希提供。
- 确认 Step 路径不会覆盖无关工作。
- 把 Plan 的适用 `MC-*` 写入 Step Manual Check 表；当前证据新暴露关注点时补充 ID，并在编辑前记录架构/复用基线。
- 把全部十个原始 `Rule N` 写入 Literal Rule Gate；编辑前记录适用性/证据，Rule 11 始终为 `Applicable`。

### 2. 重新验证当前仓库

- 编辑前重新打开真实文件和符号，不能只依赖 Plan 中的伪代码。
- 确认 Plan 的实现方向仍符合当前 API、消费者、语言/框架风格和迁移序列。
- 语义漂移必须作为阻塞处理；只有 `Plan Clarification` 已允许或仓库惯例唯一明确的机械性局部细节，才能自行补齐。
- Java Step 重新检查准确架构、Spring/Egon/模块候选、依赖、`lombok.config`、Converter/Validator、全部环境 Profile 和相关命名/模型/Bean 惯例。

### 3. 按文件顺序执行

- 按顺序完成声明的 `CREATE`、`MODIFY`、`DELETE`、`RENAME` 或 `GENERATED` 操作。
- 行为变更在 RED 点运行聚焦测试，并确认失败原因是目标能力缺失，而不是语法、夹具、依赖或环境错误。
- 只实现使测试转绿并满足 Spec 所需的最小行为。
- 保持现有项目风格、注释/注解、模块边界、公开兼容性和无关行为不变。

### 4. 验证 Step

- 运行 Step 的准确聚焦命令，并确认观察到其客观预期结果。
- 运行当前 Step 和仓库要求的编译、lint/格式、Mapper/XML/Schema、模块或跨模块检查。
- 对 Step 路径运行 `git diff --check`。
- 重读当前 Step 的需求和相关 Spec 章节，逐项确认本 Step 承担的行为、错误路径、字段、状态、权限、迁移、UI 状态和测试义务已经落实。
- 使用 `references/step-gate-checklist.md` 逐项执行全部适用 Manual Check，分别记录证据与结论；关闭失败，否则把 Step 标为阻断。
- 针对最终 Diff 重新执行每个 Literal Rule Gate 行；Test/Static Search 只能补充，不能替代逐条人工复核。

任何失败门禁都会使 Step 保持 `In Progress` 或变为 `Blocked`，不得作为已完成进行提交。

### 5. 复核并提交 Step

- 检查 `git diff -- <Step 路径>` 和 `git status --short`。
- 确认即将提交的只有当前 Step 拥有的路径。
- 明确暂存路径，然后检查暂存区的 `--check`、`--stat` 和 `--name-only`。
- 只提交这些路径；如果暂存区已有其他工作，必须使用路径受限提交将其留在原处。
- 记录提交哈希，并检查 `git show --stat --oneline <hash>` 和已提交文件列表。
- 确认无关的已暂存、未暂存和未跟踪工作仍被保留。

只有到此才能把 Step 标记为 `Committed` 并开始下一 Step。

## 失败与阻塞处理

出现以下情况时，停止当前 Step 并报告证据：

- Plan 或 Spec 不明确、互相矛盾、未获授权或已发生重大过时；
- 必需文件、符号或消费者不存在，或已被不同语义的修改占用；
- 当前 Step 需要未声明的契约、表/列、迁移、依赖、页面、权限或架构调整；
- 验证失败且无法在当前 Step 获批范围内修复；
- 需要但无法获得凭证、权限、外部服务或运行时状态；
- 其他改动与当前 Step 文件重叠；
- 无法形成安全的路径受限提交。

不能把阻塞项标为完成，不能跳到后续 Step，也不能创建误导性提交。报告必须包含受影响 Step、证据、Spec/Plan 影响、安全选项和建议下一步。

## 提交契约

每个实现 Step 在进入下一 Step 前，至少必须产生一个非空、语义明确的提交。

| 证据 | 必填值 |
| --- | --- |
| Step | 编号和 Plan 中的准确标题 |
| Requirements | 来源需求编号 |
| Baseline | 本 Step 修改前的提交 |
| Commit | 结果提交的完整或短哈希 |
| Paths | 准确的已提交文件列表 |
| Validation | 实际执行的命令和观察结果 |
| Manual Check | 全部稳定 ID 为 `PASS` 或有证据的 `N/A`，没有未关闭行 |
| Literal Rules | Rule 1、2、3、4、5、6、7、9、10、11 分别为 `PASS` 或有证据的 `N/A`，Rule 11 为 `PASS` |
| Deviations | `None`、已批准的澄清或纠正提交说明 |

必须使用路径受限的暂存与提交，不能因为无关工作已经暂存就把它带入提交。除非用户另行授权，不得 push、创建 PR、merge 或 release。

## 最终 Spec 一致性审核

全部 Plan Step 提交后，读取 `references/final-spec-audit.md` 并进行一次全新审核，不能只依赖 Plan 自带的追踪矩阵。

1. 重新解析最终生效的全部 Spec 和准确修订版本。
2. 提取每一项有效需求、验收标准、非目标、接口、模型/Schema 规则、UI 行为、测试义务、非功能约束、迁移、兼容性、发布和回滚要求。
3. 将每项要求映射到具体证据：提交、路径/符号和验证/测试结果。
4. 执行 Plan 中安全且已获授权的最终源码、静态、模块或完整回归命令；不得自动启动运行时系统。
5. 每项要求只能归入以下一种状态：
   - `Satisfied`：实现与要求的非运行时证据均已证明满足。
   - `Partial`：只实现或证明了部分内容。
   - `Not satisfied`：实现遗漏或违反要求。
   - `Runtime unverified`：源码/模块证据存在，但 Spec 还要求用户控制的真实运行环境验证，而该验证未执行。
6. 审核非目标和范围边界，确认没有意外增加行为、依赖、迁移或无关重构。
7. 确认每个 Plan Step 都有已核验提交，且没有静默遗漏计划文件或验证门禁。
8. 针对最终 Tree 和 Delivery Commit 重新执行十个原始 Literal Rule 与全部 17 项 Manual Check。适用行必须 `PASS`，每个 `N/A` 要有证据与原因，Rule 11 和 `MC-BLOCKER-001` 必须通过并与全部发现一致。
9. 报告全部 `Partial`、`Not satisfied`、`Runtime unverified`、失败/阻断 Literal Rule、Manual Check 和静默例外尝试及其证据、影响和建议下一步。

最终审核发现缺口时，不得静默追加未计划修复；应先报告并等待用户批准纠正 Plan/Step。

## 最终报告契约

完成报告必须包含：

- Plan 路径/修订和全部有效 Spec 路径/修订；
- Step 表格：状态、提交哈希、提交路径和验证证据；
- 最终验证命令及真实结果；
- 覆盖每项需求的 Spec 一致性矩阵；
- 包含全部稳定 `MC-*` 的最终 Manual Check 矩阵，逐项写 Applicability、Status、Evidence、Finding 与 Required action/exception；
- 包含 Rule 1、2、3、4、5、6、7、9、10、11 的最终 Literal Rule 矩阵，逐项写 Applicability、Status、Diff Evidence、Validation Evidence、Finding 与 Action；
- 明确列出的未满足、部分满足和运行时未验证要求；
- 已批准偏差和纠正提交；
- 剩余工作区状态，以及无关工作得到保留的确认；
- 明确说明哪些运行时、数据库、浏览器、部署、push、PR 或 release 操作没有执行；
- 一个最终结论：
  - `PASS — Implementation conforms to the effective Specs`
  - `PARTIAL — Spec requirements are unmet or unverified`
  - `BLOCKED — Final verification could not be completed`

只要任一有效要求为 `Partial`、`Not satisfied`、缺少强制运行时证据，或任一 Literal Rule/Manual Check 缺失、未通过、被弱化或无证据，就绝不能声称完全完成。最终 PASS 要求全部 Spec 要求满足、全部适用逐字规则通过、Rule 11 通过且全部适用 Manual Check 通过。

## 常见错误

| 错误 | 必须采取的纠正措施 |
| --- | --- |
| 未提交当前 Step 就提前修改多个 Step | 仅安全回退未经批准的后续 Step 改动，先完成并提交当前 Step |
| 测试通过但未提交就开始 Step N+1 | 停止，核验并提交 Step N |
| 提交了无关的已暂存文件 | 使用路径受限提交并核对提交文件列表 |
| 为 Step 创建空提交 | 停止并报告 Plan/基线漂移 |
| 把 Plan 执行完成当作满足 Spec | 独立运行最终 Spec 审核 |
| 最终审核中直接修复未计划的 Spec 缺口 | 报告缺口并申请纠正 Plan/Step |
| 用单测/模块测试宣称真实运行验收通过 | 将对应要求标记为 `Runtime unverified` |
| 后续工作暴露缺陷后改写早期提交 | 保留历史，创建归属明确的纠正提交并报告 |
| Manual Check 失败、缺失或未知仍提交 Step | 保持 `In Progress`/`Blocked`，逐项补证据并关闭后再提交 |
| 没有当前 Spring/Egon/模块复用证明就新增依赖/Helper | 停止，重建复用账本；复用现有能力，或返回 Spec/Plan 批准缺口 |
| 执行中创建混合分包 Tree | 停止；保持已有传统或准确 Archetype 形态，并申请结构性 Plan/Spec 修订 |
| 最终审核用一句话概括 Manual Check | 针对最终 Commit/Tree 逐项重新执行并记录全部稳定 ID |
| 用一般编码规范摘要替代准确编号规则 | 恢复逐字源，逐项重新执行全部原始 Rule |
| 测试通过但复杂类缺完整 Lombok 基线，或 Converter 绕过 `BaseConverter` | 保持 Step 阻断；编译/Test 成功不能豁免 Rule 3 |
| 只校验 Controller 输入 | 提交前增加并验证每个受影响层间交接的 Validation/Group |
| Complex 业务仍为 `if/else` 或 `switch` | 提交前实现获批模式参与者和测试 |
| 自动启动项目 | 除非用户明确要求，否则把运行测试留给用户 |

## Skill 维护

修改本 skill 时，必须运行 `scripts/test_user_mandated_java_rules.py`、`scripts/test_validate_skill_resources.py` 与 `scripts/validate_skill_resources.py`，使用 `references/acceptance-scenarios.md` 复核，并确认逐字源、英文运行入口、中文审核镜像、规范、检查清单和元数据仍然表达同一套执行契约。
