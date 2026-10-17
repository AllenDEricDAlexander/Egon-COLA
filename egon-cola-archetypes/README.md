# Egon-COLA Archetypes

本目录维护七个 Maven Archetype 产品：可直接编译、测试的正常源工程经两段式生成，打包成发布到 Maven Central 的 archetype。本文是维护者入口，说明各类文档放在哪、哪些会随生成工程分发、改完怎么验证。

## 目录结构

| 路径 | 内容 | 是否手改 |
|---|---|---|
| `source-projects/egon-cola-source-<family>/` | 七个正常源工程：Java、资源、测试、模块 POM，以及随生成工程分发的 README | 是，这里是事实来源 |
| `definitions/egon-cola-archetype-<family>/` | archetype 打包描述、`verify.groovy` 集成测试、随生成工程分发的策展文档 | 是 |
| `.generated/` | 由 `scripts/generate_archetypes.sh generate` 派生的打包输入，已被 gitignore | 否，永远不要手改 |
| `docs/` | 维护者文档（见下文），不随生成工程分发 | 是 |

七个 family 为 `light`、`light-open`、`service`、`service-open`、`web`、`web-open`、`agent`。各产品的形态、模块与直接依赖见[架构总览](docs/architecture-overview.md)。

## 维护者文档

| 文档 | 语言 | 内容 |
|---|---|---|
| [docs/architecture-overview.md](docs/architecture-overview.md) | English | 七个产品的形态选择、实际直接模块依赖、层职责、持久化与验证边界 |
| [docs/architecture-diagrams.md](docs/architecture-diagrams.md) | 中文 | 业务层依赖、持久化调用、CQE 事务事件、数据库生命周期四张 Mermaid 图 |
| [docs/coding-standard.md](docs/coding-standard.md) | 中文 | Java、组件复用、校验、软删除唯一键、分布式 DDL 与 CQE 的目标编码规范，以及当前实现与规范的已知差异 |
| [docs/open-archetype-code-style.md](docs/open-archetype-code-style.md) | English | 三个 Open 产品在通用编码规范之外的补充约定 |
| [docs/generated-readme-contract.md](docs/generated-readme-contract.md) | 中文 | 生成工程 README 的统一章节骨架，以及被 `verify.groovy` 断言冻结、改文档时必须保留的子串 |

## 会离开本目录的文档

下列文件不只给本仓库维护者看，修改时要按读者和去向来写：

| 来源 | 去向 | 读者 |
|---|---|---|
| `source-projects/*/README.md`、`README.zh-CN.md` | 生成业务工程的根目录 README，`${rootArtifactId}` 等变量被替换 | 业务工程开发者 |
| `source-projects/*/deploy/container/README.md` | 生成业务工程的容器交付说明（不做 Velocity 转义） | 业务工程开发者 |
| `source-projects/*/**/db/manual/postgresql/README.md` | 生成业务工程的归档 SQL 说明（仅 Open 族） | 业务工程开发者 |
| `definitions/*/src/main/javadoc/README.md` | 打进 archetype 发布制品的 javadoc classifier | archetype 使用者 |
| `definitions/*/architecture-docs/architecture.md` | 复制到 `.generated/` 中 archetype 目录旁，供 `verify.groovy` 读取；**不进入**生成业务工程，也不在发布制品里 | 维护者、集成测试 |

修改前三类文件前先读 [docs/generated-readme-contract.md](docs/generated-readme-contract.md)：README 有统一的章节骨架，且大量子串被集成测试逐字断言，改错一个字就会让 archetype IT 失败。`architecture.md` 同样有被断言的关键字，见该文档最后一节。

## 修改与验证

改源工程或策展文档后，按顺序运行：

```bash
./scripts/generate_archetypes.sh generate
```

```bash
./scripts/generate_archetypes.sh check
```

```bash
python3 scripts/checks/check-archetype-family-boundaries.py
```

```bash
python3 scripts/checks/check-native-archetype-boundaries.py
```

```bash
python3 scripts/checks/check-archetype-dependency-ownership.py
```

```bash
./mvnw -o -f egon-cola-archetypes/pom.xml -Pgenerated-archetypes integration-test -DskipTests=false
```

最后一条会为七族各生成一个业务工程并运行其 `verify.groovy`，耗时较长；只改动单个 family 时可加 `-pl :egon-cola-archetype-<family>` 限定范围。这些命令都不会启动应用、数据库或容器。脚本的完整说明见 [scripts/README.md](../scripts/README.md)。
