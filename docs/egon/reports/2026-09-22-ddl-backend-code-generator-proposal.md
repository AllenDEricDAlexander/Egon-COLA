# Egon-COLA DDL 后端代码生成器方案

| Field | Value |
| --- | --- |
| Status | Review |
| Revision | 3 — FreeMarker 与执行契约完善 |
| Updated | 2026-09-22 15:47 Asia/Shanghai |
| Approval | 用户已认可总体方向并明确批准 FreeMarker；本修订和实施 Plan 待审核，不授权开始编码 |
| Related Plans | [FreeMarker 实施 Plan](../plan/2026-09-22-15-47-ddl-code-generator-freemarker-implementation.md) |

本文件作为本轮唯一有效设计依据（可识别的 legacy Spec），不另复制一份同内容 Spec。
- 日期：2026-09-22。
- 范围：PostgreSQL DDL → Egon-COLA 后端基础 CRUD 与可选模板；支持 Light 单体、Web、Service，排除 Agent 和前端。
- 原则：生成代码符合当前规范，复用 Components、Archetypes 与实际 Platform 契约；除已批准的 FreeMarker 外不自行添加外部依赖，不重写 MP Repository 已实现的 CRUD。

## 1. 建议与参考取舍

建议新增一个独立的开发工具组件 `egon-cola-component-code-generator`，提供可被 skill 调用的 CLI。工具读取 DDL/版本清单，建立规范化逻辑表模型，按项目类型与产物范围渲染本项目维护的模板，再通过生成记录安全更新文件。业务应用不依赖生成器，服务启动不触发生成。

| 参考 | 采用的思想 | 不照搬的部分 |
| --- | --- | --- |
| MyBatis-Plus Generator | 按表选择、包/输出路径配置、自定义文件与模板、类型映射 | 不添加其 Generator 依赖，不生成 IService/ServiceImpl，不覆盖 Egon 的 Repository 规范 |
| 若依 Generator | 预览、字段配置、结构同步与生成配置持久化 | 不引入若依运行模块、数据库配置表、后台页面、权限菜单或前端模板；同步不等于覆盖人工代码 |
| 当前 Egon Archetypes | 三种项目的领域优先结构、端口归属、Mapper/XML、转换和 Bean 风格 | 旧注解、旧业务唯一约束、业务专属逻辑不是通用模板的合规依据 |

参考依据：[MyBatis-Plus 配置](https://baomidou.com/reference/new-code-generator-configuration/)、[MyBatis-Plus 生成器](https://baomidou.com/guides/new-code-generator/)、[若依后台手册：代码生成](https://doc.ruoyi.vip/ruoyi/document/htsc.html)。以上只作为设计参考，不把参考项目引入依赖图。

## 2. 当前仓库证据与约束

以下路径相对仓库根，准确 API 在实施前再复核：

| 能力 | 当前证据 | 生成器决策 |
| --- | --- | --- |
| MP-SDJ | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter` | 用户简称映射到这个真实工件，不新增别名 Starter |
| Repository | 上述模块 `extension/EgonColaRepository.java`、`EgonColaIRepository.java` | save、updateById、removeById、批处理、getById 等复用基类；多个方法 final，禁止生成覆盖实现 |
| Mapper 契约 | 上述模块 `extension/EgonColaMapper.java` | XML 必须满足 selectActiveById、selectActiveByIds、deleteVersionedById |
| DDL 历史 | 上述模块 `ddl/EgonColaDdlManifestBO.java`、`EgonColaPostgreDdlRunner.java` | 复用版本/路径/SHA-256 与不可变前缀规则；生成器只读取，不执行 Runner |
| DDL 现状 | `egon-cola-archetypes/source-projects/egon-cola-source-web/egon-cola-source-web-infrastructure/src/main/resources/db/egon-mp/` | 初始化文件含 DO 与 MASTER_DATA/SHARD 分支，分表不是独立业务模型 |
| 模板参照 | 同源工程 `infrastructure/user/{dao,po,repo,converter}`、`resources/mybatis/mapper/user/UserDAO.xml` | 提取结构规范，不复制 User 业务逻辑 |
| 业务端口 | 同源工程 `domain/user/service/UserDomainService.java`、`application/user/manage` | Domain 只暴露业务对象；Infrastructure 实现端口，Application 编排 |
| 解析依赖 | MP-SDJ POM 已声明 mybatis-plus-jsqlparser 与 ShardingSphere PostgreSQL parser | 在现有依赖内部验证并封装 DDL 解析能力，不新增 parser 工件；目前尚未验证完整 DDL 覆盖 |
| 生成器/模板引擎 | 未发现现有后端 CRUD 生成器模块及 Velocity/FreeMarker 声明 | 本轮用户已批准 FreeMarker；采用内部组件封装 FreeMarker，不引入其他模板引擎 |
| Platform | 当前仓库平台工程位于 `egon-cola-xingyuan` | “platforms”按实际模块定位；只使用选中项目已经接入的契约，不虚构目录或通用平台依赖 |

DDL Manifest 当前代码中仍有名为 `family` 的序列化字段。生成器对外配置使用 `projectType`，内部兼容读取这个既有字段；本方案不改其持久化契约，也不把该名称引入新产品文档/配置。

## 3. 首版范围与项目类型

首版支持 native Light/Web/Service。当前依赖限制下不默认增加 Open 公共栈变体；若需要复用已有 Open 项目，须单独确认模板和已批准依赖。这里的“单体”明确映射 Light 单模块，不会把传统三层项目自动改造成 DDD。

| projectType | 物理布局 | 允许的后端入口 |
| --- | --- | --- |
| `light` | 单 Maven 模块，领域优先包，启动包 start | 已有 HTTP 体系的 Controller，或内部 Application 用例 |
| `web` | common、facade、domain、application、infrastructure、adapter、starter | 已有 HTTP 体系的 Controller，按已定义契约接入其他适配器 |
| `service` | 同样七模块；保留服务型边界 | Domain/Application CRUD；可对接已有 Facade，不生成 HTTP Controller |
| `agent` | 不支持 | 返回 UNSUPPORTED_PROJECT_TYPE |

传统三层既有项目保持现状：首版工具对不匹配 Light/Web/Service 的结构明确阻断，不能移动包来“适配”工具；后续可以独立补三层模板，不与 Light 混同。

首版聚焦单逻辑表的创建、详情、列表/分页、按版本更新、逻辑删除。复用基类已有批处理，但不自动对外开放批量操作。跨聚合联动、复杂 JOIN、树形业务、审批、工作流、多租户授权模型推断、GraphQL、新 Proto/RPC 契约生成、前端、Agent 均不纳入自动推导范围。

## 4. 模块与设计

建议新增内部模块及入口（以下均为拟议路径，并非当前可运行工具）：

```text
egon-cola-components/egon-cola-component-code-generator/
  pom.xml
  src/main/java/top/egon/cola/component/codegen/
    cli/         参数读取、退出码、调用流程
    ddl/         DDL 输入适配、版本回放与逻辑 Schema
    model/       配置、表/字段、计划/差异对象
    profile/     light/web/service 的固定目录与职责映射
    template/    模板元数据、上下文与渲染
    update/      文件基线、冲突判定、计划应用/恢复
    validation/  依赖、命名、范围和规范检查
  src/main/resources/templates/backend/
    common/      PO、DAO、XML、Repository、转换等共享模板
    light/       Light 路径及入口差异
    web/         Web 路径及入口差异
    service/     Service 路径及入口差异
  src/test/      DDL、模板产物、重生成与编译验收
scripts/egon-codegen.sh
```

只建一个生成器模块，不拆 core/plugin/server 多层框架。模板目录映射允许直接数据表驱动，首版不引入插件扫描或外部模板执行。

采用两处有实际用途的模式：

- **Adapter**：把现有 MP-SDJ 解析/DDL 清单转换为稳定的生成器 Schema 模型，隔离解析器 AST 和框架版本。现有 Runner 不是 Schema 提取器，不能假装直接调用 Runner 就得到表模型。
- **Strategy**：Light/Web/Service 输出布局策略，隔离真正不同的模块路径与允许入口，避免每个模板散落项目类型分支。固定三种策略即可，不建通用插件系统。

模板引擎采用 Apache FreeMarker `org.freemarker:freemarker:2.3.35`，由内部 `FreeMarkerTemplateService` 封装。模板使用 UTF-8 `.ftl`，循环/条件由 FTL 表达；字段类型、路由、CRUD 权限及可写策略先在 Java 中确定。引擎只属于工具，生成代码不引用 FreeMarker。

使用单个完成初始化后不再修改的 Configuration，classpath 白名单模板，只传 Map/List/标量组成的只读渲染模型。首版不接收外部模板/URL，不暴露 BeanFactory、ClassLoader、文件/网络对象；不承诺运行不可信模板的通用沙箱。

配置固定：`new Configuration(Configuration.VERSION_2_3_35)`、`setClassLoaderForTemplateLoading(..., "templates/backend")`、`setDefaultEncoding("UTF-8")`、`setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER)`、`setLogTemplateExceptions(false)`、`setWrapUncheckedExceptions(true)`、`setAPIBuiltinEnabled(false)`、`setNewBuiltinClassResolver(TemplateClassResolver.ALLOWS_NOTHING_RESOLVER)`；禁止用默认空值掩盖缺失必需字段，渲染异常必须保留模板与位置并停止应用。

使用方括号指令与方括号插值：`Configuration.SQUARE_BRACKET_TAG_SYNTAX` 和 `Configuration.SQUARE_BRACKET_INTERPOLATION_SYNTAX`，例如 `[#list fields as field]`、`[=field.javaName]`。因此 MyBatis `#{id}`、`${...}` 与 Spring/Archetype 占位符可以原样保留。Java/XML 均设为 PlainTextOutputFormat，关闭 HTML 自动转义；标识符先校验，Java 字符串/注释及 XML 属性分别显式转义，禁止二次 eval/interpret。

固定 Locale.ROOT、UTC、确定性数值文本、LF/import 顺序；不在产物中写时间戳、随机值或本机绝对路径。测试未定义变量、语法错、Unicode、注释中的 FTL 字符、Java 泛型、MyBatis 绑定、模板升级导致的 STALE_PLAN，以及重复渲染零差异。

官方依据：[FreeMarker 下载与坐标](https://freemarker.apache.org/freemarkerdownload.html)、[替代语法](https://freemarker.apache.org/docs/dgui_misc_alternativesyntax.html)、[错误处理](https://freemarker.apache.org/docs/pgui_config_errorhandling.html)。

## 5. 依赖边界

分开检查工具本身和产物：

1. 生成器直接依赖使用现有内部 Common Core、MP-SDJ、Common Test（测试范围）与已批准的 org.freemarker:freemarker；新增内部依赖也列入实施方案并由用户批准，不能自行修改 POM。
2. 复用它们已批准提供的 Jackson、校验、SQL 解析等能力，只有 FreeMarker 是本轮已批准的新增第三方工件；不引入 MyBatis-Plus Generator、若依、Velocity、JavaParser、Picocli 或额外日志实现。
3. DDL 解析先对现有 PostgreSQL parser 能力做针对性验证，再由内部 Adapter 封装；不把解析器 AST 暴露到生成代码。若现有解析器不能满足首版承诺，先阻断并提交内部扩展/依赖引入方案，不能临时换库。
4. 产物引用目标工程已具备的 Components、Facade/Platform 契约及其框架 API；不可因为工具运行时有某个依赖，就假设目标模块也有。
5. BOM 中存在版本只是管理信息，不代表目标模块已获得依赖，也不代表批准新增。生成器输出 dependency-report，绝不自动编辑 POM、追加 annotationProcessor 或下载工具。
6. 模板发现依赖缺口时不输出“看起来可编译”的占位替代类。错误必须指出工件/类、受影响产物和候选引入方案。

“无外部依赖”的最新有效含义是：FreeMarker 只进入开发工具依赖，产物不引入新外部依赖；其他外部生成器/运行时仍不允许；Spring/Lombok/Jackson 等现有脚手架基础不会被重新实现。若希望连现有框架类型都不使用，会与 Egon 当前规范矛盾，不是本方案的含义。

## 6. DDL 输入与适应变化

### 6.1 两种离线输入

- `schema`：完整目标 Schema DDL；修改后与上次 Schema 快照比较。适合维护单份结构定义。
- `manifest`：MP-SDJ 有序 SQL 与 Manifest；验证每份 SHA-256、版本顺序和已见历史前缀，离线回放新增版本计算当前目标 Schema。旧版本内容漂移直接阻断，不能通过更新生成记录洗掉差异。

都不运行 SQL、不调用 DDL Runner、不启动数据库。真实数据库元数据读取可以以后作为单独授权的只读能力，首版不依赖临时 H2/容器“执行一下 DDL”。只有增量 ALTER 而没有基线时返回 MISSING_SCHEMA_BASELINE。

### 6.2 首版必须支持的 DDL 子集

- CREATE TABLE：字段、类型、长度/精度、NULL、默认值、主键、外键/检查约束/唯一约束。
- COMMENT ON TABLE/COLUMN：注释；可生成已有约束能表达的长度/必填信息，不把评论文本直接推断成权限或业务规则。
- CREATE/DROP INDEX：索引列、唯一、部分索引谓词；支持检查业务列 + deleted_at 和有效行唯一性。
- ALTER TABLE：增/删/重命名列、重命名表、类型/精度/NULL/default 变更、约束变更；DROP TABLE 进入破坏性变更计划，不能自动删文件。
- 当前受管脚本的 `DO ... BEGIN IF current_setting('egon_migration.role') = 'MASTER_DATA' ... ELSIF ... 'SHARD' ... END IF; END`：只识别经过测试的固定角色分支形态，保留角色归属，并解析其中静态 DDL；不是执行 PL/pgSQL。
- 分片：读取现有 MP-SDJ 路由/物理表配置，建立 `logicalTable -> physicalTargets` 映射并核对结构一致；例如 school_classes_0/1 只生成一个逻辑实体。不按 `_数字` 后缀猜表族，也不把 ddl_history/outbox 内部表当业务表自动生成。

动态 EXECUTE、循环造表、过程计算字段、无法判定的 IF、未知 PostgreSQL 类型/域/表达式等不能跳过后继续成功。必须返回文件/行/语句/不支持语法及解决方案；可以由用户提供等价完整 Schema 输入，但不能自动截取 DO 内所有 CREATE 并当作无条件结果。含 DML 的历史：只允许明确声明不影响结构的普通数据语句作为非结构步骤记录；涉及 CALL/动态 SQL/触发器等潜在结构副作用时阻断。

首版要以三个实际 native 脚手架的 DDL 作为解析输入验收；这只是解析覆盖。其现有旧唯一键如不符合新规范，规范校验应失败并报告修正需求，不能为让模板测试通过而放宽规则或改旧 migration。

### 6.3 中间模型与信息边界

规范化 Schema 记录：来源版本/hash、schema/逻辑表/物理目标、角色、列及顺序、类型/NULL/default、约束索引、注释、基础字段映射、路由键和来源位置。生成业务元数据另外配置，不能改变 DDL 事实：领域名、类名、暴露字段、列表筛选/排序、枚举映射、敏感字段、接口路径和契约归属。

只凭 DDL 无法确定“哪个角色可以删除”“状态转换是否允许”“是否发消息”或某数值是否枚举。缺少这些信息时生成持久化模板；请求涉及这些能力的上层模板则提示缺少配置，不伪造业务。

## 7. 生成范围与目录

规范化产物名称建议如下，CLI 同时输出模板清单供 skill 查询：

| artifacts | 产物 | 约束 |
| --- | --- | --- |
| `pojo` / `po` | `*PO` | pojo 默认仅指 PO；不是无差别生成所有载体 |
| `dao` | `*DAO.java` Java Mapper 接口 | 继承 EgonColaMapper，不另造重复 Mapper 接口 |
| `mapper-xml`（`mapper` 别名） | `*DAO.xml` | 独立可选；namespace 指向准确 DAO |
| `repo` | `*Repository` | 继承 EgonColaRepository，只补装配与具名扩展 |
| `converter` | MapStruct 转换 | BaseConverter / BaseForwardConverter，输入输出类型必须已存在或在本次选中 |
| `command` / `query` / `result` | CRUD 请求/查询/结果载体 | 由已配置的字段可写/可读策略控制，不照搬全表 |
| `domain-service` / `domain-impl` | 业务端口及基础 CRUD 实现 | 实现位于 Infrastructure，端口不泄露 PO/MP 泛型 |
| `application` | Manage 及实现 | 事务与简单 CRUD 编排，复用 Domain 端口 |
| `controller` | 已有 HTTP 技术栈的 CRUD 入口 | 仅 Light/Web；不能为 Service 生成 HTTP |

预置 `persistence-crud = po,dao,mapper-xml,repo`；`backend-crud` 在此基础上包含必要转换、领域/应用与载体，Light/Web 可按配置包含 Controller，Service 默认止于 Application 对外可调用方法。已有 Facade 接入只按已有契约显式配置，不推断新 Proto、RPC 或额外协议依赖。

**范围永远只收窄授权**：单独 `repo` 时校验引用的 DAO/PO 已存在；不存在则报缺失依赖类型，不自动增加产物。允许生成片段式组合，但必须标明外部前置条件；不能称缺依赖的输出为可编译成果。

`outputRoot` 指定项目/导出根，按 `modulePaths` 和 `basePackage + domain + layer` 决定实际路径。允许独立导出目录和逐产物输出子目录；写入既有项目时模块必须存在，不自动创建 Maven 模块或调整依赖方向。拒绝路径越界、符号链接逃逸、重复路径/类名/Bean 名/XML statement 冲突，不写入 Archetype `.generated`。

## 8. CRUD 模板必须满足的规范

- PO 普通 class，继承 EgonModel，不重复声明 ID、tenant、审计、deletedAt、version。字段与基类类型不一致直接阻断；保留明确 MyBatis-Plus 映射。
- Builder 依据父类链选择：EgonModel PO 的继承构建用兼容 SuperBuilder；独立载体按构造目标用 Builder。相等性按父类状态设置 callSuper，record 只用于已明确的不可变值对象。
- 枚举只有配置语义明确时生成；存储码 @EnumValue，前端码 @JsonValue，核对未知值与反序列化策略。
- Bean 名和 Qualifier 根据领域/模块稳定生成并查重，检查 lombok.config 的构造器注解传播；不自动改 lombok.config。缺少必需传播配置时列出阻断项。
- 入参使用 Jakarta 原生/自定义注解、Valid、Validated 与分组。ValidateUtils/ValidationUtils 只作通用手工触发，不能生成一个大工具类堆业务校验。
- save、updateById、removeById 等使用基类实现，不在 Repository 覆盖 final 方法。按用户提交的 version 更新/删除；不能为了方便重新读取最新版并吞掉乐观锁冲突。更新载体只允许业务可写字段，租户和审计由组件上下文负责。
- Mapper XML 生成必需的三条基类契约语句、结果映射和显式列清单。deleteVersionedById 保留 `#{et...}`、`#{MP_OPTLOCK_VERSION_ORIGINAL}`、有效行、审计和版本递增；租户条件由当前 interceptor 契约安全补充并测试，不能重复/矛盾或绕过租户拦截。
- 分页/筛选用具名 XML + 白名单字段/操作符和稳定排序，限制 pageSize；不暴露任意 SQL 列名、`${requestSort}` 或 Wrapper 给外部。MP 本身虽有通用 list/page，生成器仍遵守本项目显式查询规范。
- 路由键是每个 CRUD 入口的必要条件，按逻辑表配置生成并校验；涉及租户以外的分片根键不能仅用 ID 基础方法强行查写。广播表默认只生成 Query，不生成未经授权的写入；复杂/联合主键与 EgonModel ID 不兼容时阻断。
- 完整基础 CRUD：创建返回生成标识/约定结果，详情不存在按当前错误契约处理，更新/删除零影响映射冲突或不存在，页查询稳定排序。更新首版使用明确的完整可写字段语义；PATCH 三态/null 清空只在约定明确时支持，不能偷用 null=忽略推断。
- 创建 Command 排除服务器管理字段；有数据库默认值时不能给未提交字段生成会覆盖默认的 Java 初值。生成前核对 MP insert/updateStrategy 与 required 规则，不能把所有数据库 NOT NULL 都变成前端必填。
- 业务唯一键 + deleted_at、有效行 NULL 约束由 DDL 校验保证；生成器不改数据库。并发重复由 DB 约束兜底并映射当前错误契约，前置 exists 不代替唯一约束。
- 不默认加缓存和事件。选中缓存/事件扩展时必须复用已有组件与配置；Event 必须有实际 Outbox/MQ 路由与一致性要求，无传输能力则阻断。Query 不产生业务事件。首版不自动根据所有 CRUD 推导事件。

## 9. DDL 变化后的重生成

建议保存 `.egon/codegen/` 工具状态：配置、规范化 Schema 快照、每个文件的模板/组件版本、输入指纹、最后生成内容 hash、所依赖的表/列和路径。可审核的配置/manifest/快照纳入版本控制；临时候选文件、备份和锁按工程惯例忽略，不保存凭据或绝对本机路径。

| 变化 | 默认处理 |
| --- | --- |
| 同输入重复生成 | NO_CHANGE；不触碰文件时间或写入噪声 |
| 新表/新列 | 展示新增字段与受影响产物；只生成所选范围且通过规范/依赖门槛 |
| 注释变化 | 只刷新依赖该注释的未改动自有文件 |
| 字段类型/长度/NULL/default 变化 | 列出 PO、约束、Command/Result、XML 与接口影响，要求明确兼容决策后应用 |
| 重命名 | DDL RENAME 或显式 rename mapping；不能猜测“删 A 加 B”就是改名 |
| 删除列/表 | 标为破坏性变更；不自动删已有文件，显式确认具体路径与消费者处置 |
| 仅索引变化 | 更新 Schema 检查和状态，没影响模板时不重写 Java |
| 人工改过文件 | CONFLICT，输出候选 diff，保持原文件；首版不做 Java/XML 自动合并 |
| 未被管理的同名文件 | 不接管、不覆写；用户决定迁移为受管文件或选择别名/目录 |
| 组件/模板版本变化 | 显式计划模板迁移，重跑兼容检查，不能伪装成仅 DDL 字段变动 |

判定使用“上次生成版本、磁盘版本、新生成版本”三方信息，但首版只自动替换 `diskHash == lastGeneratedHash` 的文件。不能用语言模型把冲突文件重新写一遍绕过保护。业务方法放在既有扩展/独立业务文件中；不强行新增 Base/Generated 继承层。若用户编辑了生成文件，它立即受到冲突保护。

部分范围生成必须保留**每个产物自己的同步版本**：本次只生成 PO，不能把 DAO/XML 也标记成已同步最新 DDL。全局 latestObservedSchema 与各产物 lastGeneratedInput 分开，未选中消费者列入 pending impact。

Plan 生成后，在 apply 前再次核对 DDL/config/template/组件指纹及文件 hash，漂移则返回 STALE_PLAN。多文件写入使用临时目录、目标锁、写入日志和可恢复的逐文件替换；不能声称跨目录天然原子。预检失败不改代码；中途故障停止并提供仅针对本次写入的恢复记录，状态清单只能在对应文件成功后推进。

## 10. 拟议配置与命令

以下是接口方案，当前仓库没有这些可执行命令：

```json
{
  "projectType": "web",
  "basePackage": "com.example.order",
  "domain": "order",
  "outputRoot": "/workspace/order-service",
  "modulePaths": {
    "domain": "order-service-domain",
    "application": "order-service-application",
    "infrastructure": "order-service-infrastructure",
    "adapter": "order-service-adapter"
  },
  "input": {
    "mode": "manifest",
    "resourceRoot": "order-service-infrastructure/src/main/resources",
    "manifest": "db/egon-mp/repository-manifest.json"
  },
  "logicalTables": ["orders"],
  "artifacts": ["po", "dao", "mapper-xml", "repo"],
  "existingTypeMappings": {},
  "fieldPolicies": {},
  "events": {"enabled": false}
}
```

```text
egon-codegen templates                         # 机器可读模板/项目类型/版本能力清单
egon-codegen plan --config codegen.json         # DDL 差异、范围、依赖、冲突、候选输出
egon-codegen apply --plan <plan-id>              # 复核 hash 后应用获准范围
egon-codegen check --config codegen.json        # 只检查漂移/规范，不改文件
```

实际脚本名与启动 classpath 在实施时固定并验证；不引入 CLI 框架或 Maven 插件来解决参数解析。提供 JSON 报告 + 简短人类摘要，skill 优先读摘要、错误行和差异，不把所有模板源码反复载入上下文。错误至少区分：配置、语法、规范、缺依赖、缺类型、文件冲突、破坏性变更、过期计划、应用失败。

## 11. Skill 接入

本轮已修改三个 coding skill 的入口和引用文件：

- Spec：必须确认 DDL 来源、项目类型、生成范围、模板与自定义业务界限、依赖可用性；不再为常规模板逐行拟造实现。
- Plan：生成配置、真实命令、输入/输出与组件版本、变更预期及验证写入步骤；缺依赖先阻断并提交引入方案，必须得到用户明确批准。
- Execute：生成器实现并可用后，以工具生成已支持的模板。模型审核 diff、补自定义业务及测试，不重复手写这些模板。只读模板清单/摘要和相关片段，降低 token 使用。
- 当前只有方案，skill 明确要求先检查工具可用性，不能执行虚构命令，也不能声称已经具备生成能力。工具缺失时阻断模板生成环节，设计工作仍可推进。

批准实现生成器不等于批准任意新增依赖。每个新增/升级内部或外部工件、构建插件和处理器都需要明确范围授权；已批准的精确方案不重复询问。

## 12. 实施顺序与验收

每个逻辑任务一个聚焦提交；此处只是推荐迭代顺序，正式实施前再确认，不在本轮改生成器生产代码。

1. **依赖与 DDL 能力验证**：现有 parser 处理真实 native DDL、固定 DO 分支和目标 ALTER 子集；列出不支持语法。验证失败先提交解决方案，不能自行换库。
2. **离线 Schema 与变化模型**：完整 DDL/Manifest 两种输入、历史 checksum、角色/逻辑表归并、类型映射和 schema diff；不执行 SQL。
3. **持久化 CRUD 模板**：三种布局的 PO/DAO/XML/Repo、任意产物范围和外部已有类型引用；编译并验证继承 CRUD 的真实路径。
4. **安全重生成**：文件基线、三方判定、部分产物状态、冲突/重命名/删除、STALE_PLAN、部分写入恢复、确定性。
5. **基础上层 CRUD**：按当前端口/事务/转换结构生成 Domain/Application/载体及可选 Light/Web Controller；Service 不生成 HTTP；缺契约/权限/依赖要阻断。
6. **CLI 与 skill 实际联调**：真实工具发现 → plan → 范围校验 → apply → compile/check；模板版本/组件升级兼容和 token 用量对比。不用假成功命令替代。

验收至少包括：

- 三种项目的 persistence-crud 和 backend-crud（Service 无 HTTP）生成后编译；单独 po、dao、mapper-xml、repo 对已有前置类型编译，不生成未选择文件。
- 创建/详情/页查询/更新/软删：租户隔离、版本冲突、零影响、基础字段不重复、必需 XML ID 和绑定参数、路由键约束、分页排序白名单。
- 固定 role DO 脚本解析、物理分表归并、ALTER 增删改名/类型/NULL/default、单文件 snapshot 修改和 Manifest 历史漂移。
- 未知语法无写入；旧业务唯一约束不合规失败；数据库 default 与请求必填规则不混淆。
- 连续两次相同生成零 diff；人工改 Java/XML 不丢；只生成 PO 后 XML 仍有 pending impact；旧 plan 不能覆盖新改动；中途失败可恢复。
- 不改 POM、不升级依赖、不下载未批准的外部工具、不连接数据库、不写 DDL、不生成前端/Agent/未请求的运行组件。
- 编译与隔离测试不是 PostgreSQL/ShardingSphere/MQ 运行验收；需要真实环境的项目由用户独立授权，不自动启动。

## 13. 审核时需要确认的选择

本方案推荐值为：**native Light/Web/Service；离线 PostgreSQL DDL；优先 persistence-crud 再补上层基础 CRUD；一个内部 CLI 组件；FreeMarker 受控模板；只自动更新未人工修改的受管文件；新增依赖一律先明确审批。**

主要成本在 DDL 语义与安全重生成，不在几份 Java 模板。首版不承诺理解任意 PostgreSQL 程序块，也不承诺自动合并人工业务代码；遇到这些情况必须给出可操作的阻断信息，保证输出真实合规。


## 14. 本轮完善后的执行契约

### 14.1 依赖授权、版本和工具生命周期

FreeMarker 是用户最终明确选择，取代上一条 Velocity 选型；仅授权用于新增 `egon-cola-component-code-generator`。版本在 Components 父 POM dependencyManagement 中固定为 2.3.35；不修改目标项目 POM，不让生成器成为应用运行时 Starter。官方说明 FreeMarker 除 Java 外没有必需依赖；实施时仍检查实际 dependency:tree，禁止追加无关日志或模板工具。Velocity 不进入有效依赖清单。
生成器使用普通 Java main 与明确构造装配，不启动 Spring Boot，也不实例化 MP DataSource/DDL 自动配置。依赖 MP-SDJ 只是读取现有模型/解析能力；不打开数据库连接。应用模板遵循 native Archetype，工具宿主沿用 Components 功能包布局，不给 CLI 强套 Controller/DDD 应用层。

### 14.2 DDL 解析路线冻结

选用 MP-SDJ 当前已依赖的 JSQLParser，由 `PostgreDdlAdapter` 隔离 AST；入口使用当前依赖提供的 `CCJSqlParserUtil.parseStatements`。Step 2 首先针对仓库现有版本和实际三种 native SQL 做能力测试。普通 CREATE/ALTER/INDEX/COMMENT 转为 Schema 变更；DO 的固定角色分支由 quote/comment/dollar-quote 感知扫描器识别后交给解析器。未知过程分支、动态 SQL、损失类型/约束信息必须错误退出，不切换 parser、不删约束以制造成功。如当前 AST 确实无法表达承诺的语法，停止该步骤并提交能力扩展方案。

数据库语法能力验证是编码步骤的 RED/GREEN 前置门槛，不伪称本轮已运行。schema/manifest 两种输入都保存来源位置；系统表忽略清单默认仅 ddl_history 与已登记组件表，不能按名字模糊屏蔽业务表。

### 14.3 上层模板与最小数据契约

`backend-crud` 必须包含其真实编译前置：`domain-model` 生成 Domain 的 `*BO`（业务字段、id、version、必要路由键），`domain-query` 生成 Domain 的只读筛选契约；它们不包含 MP/PO。Application Command/Query/Result 独立；只有本次确实跨边界时生成 MapStruct 转换，不从 DDL 自动创建聚合根/值对象。

基础调用：Controller → Manage → DomainService → Infrastructure DomainServiceImpl → Repository → DAO/XML。Result 页复用 Common PageSlice/PageQuery/现有分页包装；具体泛型和 Builder 是否适用以当前组件签名为准。DomainServiceImpl 负责 PO 与 Domain 的转换，Application 不导入 PO/Repository 实现；Controller 不绕过 Manage。首版不新建 Facade RPC 方法、GraphQL 或 MQ。

HTTP 预置请求是 POST collection 创建、GET id 详情、GET collection 分页、PUT id 完整可写字段更新、DELETE id + expectedVersion 逻辑删除。路径基于显式 `api.basePath`，ID 在 JSON/HTTP 层为正十进制 String，内部 Long。沿用目标项目现有认证/错误契约；请求不能提供 tenant/audit。生成 Controller 前必须配置现有 exception/身份边界符号并验证存在，不生成匿名安全默认值，也不自动添加注解依赖。生成业务操作不承诺新建幂等协议或 Exactly Once；需要这些行为时使用既有明确契约或退出该模板范围。

基础可写策略必须显式配置 create/update/result/filter/sort 字段集合。按版本删除调用 `removeById(PO)`，不得调用会重新读取最新版的重载来忽略调用者版本；更新加载保留审计/租户元数据，再绑定调用者 expectedVersion 和允许字段，检查影响数。按 ID 的基类方法只用于已证明可以安全路由的表，否则要求显式路由查询与受守卫写路径；不生成遗漏分片键的方法。

### 14.4 计划格式、状态与应用

生成器 `plan` 保存 JSON 格式 `formatVersion=1`，字段固定包含 planId、projectType、outputRootBinding、inputFingerprint、configFingerprint、templateSetVersion、componentFingerprint、files、schemaChanges、pendingImpacts、diagnostics。files 每项含相对路径、artifact、logicalTable、operation、expectedDiskHash、previousGeneratedHash、candidateHash、candidateRelativePath。planId 是规范化计划内容的 SHA-256，不含生成时刻。

输出目录绑定属于本地计划，不写进可复用模板产物；apply 必须用同一真实根目录，不允许搬移计划后写入其他项目。快照/配置中只保留相对路径或显式本地配置值，不含数据库凭据。能力清单/诊断输出 stdout JSON；日志/进度 stderr；禁止输出全部源码作为默认报告。

退出码：0 成功/无变化，2 配置或不支持语法，3 缺依赖/类型/规范不合格，4 人工文件冲突，5 破坏性变更待选择，6 STALE_PLAN，7 IO/中断恢复所需。check 无差异为0，发现非冲突的待同步差异为8。错误保留稳定 code、文件/行、artifact、预期/实际摘要；不混入秘密或 SQL 数据值。

apply 默认拒绝破坏性操作。plan 产生的 actionId 可在复核后用 `--accept-action actionId` 精确确认删除/改名/契约变化；这只是机器接口，不代替 skill 获取相应用户授权。只接受计划中列出的 action，不支持全局 --force。模板/输入/代码变化后旧授权 action 随 STALE_PLAN 失效。

目录内 file lock + 同文件系统临时写入与替换；首次根目录不存在时只创建获准根路径，不能先写不明位置。journal 记录 PREPARED、WRITING、COMMITTED/RECOVERY_REQUIRED；崩溃后只允许检查后 resume 或 rollback 本轮文件。回滚若发现用户在中断后改过文件，保留并报告冲突，不覆盖。状态成功推进以实际文件哈希为准。

### 14.5 工具命令与分发边界

入口固定 `scripts/egon-codegen.sh`，Java 主类 `top.egon.cola.component.codegen.cli.CodegenCommand`；支持 templates、plan、apply、check、recover。launcher 读取显式 `EGON_CODEGEN_CLASSPATH`，不在调用时自动 Maven install、解析远端依赖或下载 JAR。首次构建/组装 classpath 的命令在实施文档单独列出并由正常开发流程执行。

首版不新增 Maven plugin、fat-JAR/shade 插件或远程模板仓库。工具无依赖/模块产物时返回 BLOCKED_TOOLING 和准确缺失文件，不能替 skill 手写模板。已通过现有构建依赖解析的 runtimeClasspath 必须包括 FreeMarker，生成的业务应用 classpath 不包括它。

### 14.6 验证证据分类

本轮仅完成设计、源代码/POM读取、FreeMarker 官方文档核对和 Plan 静态检查。实施期间依次证明：解析覆盖 → 模板 golden/语义检查 → 真实 JavaCompiler + Lombok/MapStruct → 三种源项目临时副本 Maven 编译及结构校验 → CLI 重生成和冲突恢复。不得把第一层测试当作后几层已通过，也不得为运行验收启动数据库、Broker 或应用。
