# Egon-COLA

Egon-COLA 是基于 COLA 分层思想整理的 Java 21 工程脚手架和组件集合。

当前仓库包含：

- `egon-cola-components`：基础组件、starter、BOM 和组件开发脚手架。
- `egon-cola-archetypes`：可直接生成业务工程的 light、service、web 三类 Maven Archetype。
- `cola-samples`：由脚手架生成的示例工程。
- `scripts`：版本调整、本地集成验证、Maven Central 发布说明。

## 技术版本

- JDK 21
- Maven Wrapper 3.9.14
- Spring Boot 3.5.16
- Spring Cloud 2025.0.3
- Spring Cloud Alibaba 2025.1.0.0
- Spring AI 1.1.8
- Spring AI Alibaba 1.1.2.3

## 本地验证

```bash
# 快速验证，等同 Fast CI 的核心构建
./mvnw -V --no-transfer-progress clean install

# 只验证 statemachine 及其上游模块
./mvnw -V --no-transfer-progress -pl egon-cola-components/egon-cola-component-statemachine -am test

# 强验证，等同 Strong CI：默认 Java 21 构建，再用额外 JDK 跑 surefire:test 和 archetype 生成验证
JAVA21_HOME=/path/to/jdk-21 JAVA22_HOME=/path/to/jdk-22 scripts/integration_test
```

CI 说明：

- Fast CI 使用 `.github/workflows/ci.yaml`，在 Ubuntu 和 Windows 的 Java 21 上执行 `clean install`。
- Strong CI 使用 `.github/workflows/ci_by_multiply_java_versions.yaml`，执行 `scripts/integration_test`，要求 `scripts/bash-buddy` 子模块可用。
- 发布 Maven Central 使用 `.github/workflows/publish-maven-central.yml`，详细步骤见 [scripts/maven-deploy.md](scripts/maven-deploy.md)。

## 发布到 Maven Central

```bash
# 整体打包发布 Central
./mvnw -B -ntp -f ./pom.xml -Prelease -DskipTests clean deploy
./mvnw -B -ntp -f egon-cola-components/pom.xml -Prelease -DskipTests clean deploy
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -Prelease -DskipTests clean deploy
```

## 生成工程示例

```bash
# 生成新工程，采用本地仓库中的 Egon-COLA archetype
mkdir -p /tmp/egon-cola-demo
cd /tmp/egon-cola-demo

mvn archetype:'generate' `
    # 去本地仓库找脚手架
    -DarchetypeCatalog='local' `
    # 生成的project groupId
    -DgroupId='top.egon' `
    # 生成的project artifactId
    -DartifactId='family' `
    # 生成的project version
    -Dversion='1.0.0-SNAPSHOT' `
    # 生成的 java project package
    -Dpackage='top.egon.family' `
    # 指定 archetype 的 group
    -DarchetypeGroupId='top.egon' `
    # 指定 archetype 的 artifactId
    -DarchetypeArtifactId='egon-cola-archetype-web' `
    # 指定版本
    -DarchetypeVersion='5.1.2' `
    -DinteractiveMode='false'

# 本地仓库示例
# light archetype
mvn -B archetype:generate \
  -DgroupId='top.egon' \
  -DartifactId='light' \
  -Dversion='1.0.0-SNAPSHOT' \
  -Dpackage='top.egon.light' \
  -DarchetypeArtifactId='egon-cola-archetype-light' \
  -DarchetypeGroupId='top.egon' \
  -DarchetypeVersion='5.1.2' \
  -DarchetypeCatalog='local' \
  -DinteractiveMode='false'     

# service archetype
mvn -B archetype:generate \
  -DgroupId='top.egon' \
  -DartifactId='fable' \
  -Dversion='1.0.0-SNAPSHOT' \
  -Dpackage='top.egon.fable' \
  -DarchetypeArtifactId='egon-cola-archetype-service' \
  -DarchetypeGroupId='top.egon' \
  -DarchetypeVersion='5.1.2' \
  -DarchetypeCatalog='local' \
  -DinteractiveMode='false' 
  
# web archetype
mvn -B archetype:generate \
  -DgroupId='top.egon' \
  -DartifactId='fable-web' \
  -Dversion='1.0.0-SNAPSHOT' \
  -Dpackage='top.egon.fable-web' \
  -DarchetypeArtifactId='egon-cola-archetype-web' \
  -DarchetypeGroupId='top.egon' \
  -DarchetypeVersion='5.1.2' \
  -DarchetypeCatalog='local' \
  -DinteractiveMode='false'
```

项目创建结束后，可以将生成目录直接拷贝到新仓库，IDEA 打开根 `pom.xml` 作为 Maven Project，再添加 Git 仓库并关联远程仓库。

## License

This project is dual-licensed under the MIT License and the GNU General Public License v3.0 or later.

You may choose either license:

- MIT License, see [LICENSE-MIT](./LICENSE-MIT)
- GNU GPL v3.0 or later, see [LICENSE-GPL-3.0-or-later](./LICENSE-GPL-3.0-or-later)
