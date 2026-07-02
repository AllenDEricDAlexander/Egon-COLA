# Egon-COLA Maven Central 发布操作说明

Egon-COLA 发布到 Maven Central 使用 Sonatype Central Portal，不再使用旧 OSSRH Staging 流程。

## 0. 前置准备

在 `/Users/mario/.m2/settings.xml` 中配置 Central Portal User Token：

```xml
<servers>
    <server>
        <id>central</id>
        <username>${env.CENTRAL_USERNAME}</username>
        <password>${env.CENTRAL_PASSWORD}</password>
    </server>
</servers>
```

并确认本地发布 profile 通过环境变量注入 GPG 配置：

```xml
<profiles>
    <profile>
        <id>central-publishing</id>
        <properties>
            <gpg.executable>gpg</gpg.executable>
            <gpg.passphrase>${env.GPG_PASSPHRASE}</gpg.passphrase>
            <gpg.homedir>/Users/mario/.gnupg/</gpg.homedir>
        </properties>
    </profile>
</profiles>
<activeProfiles>
    <activeProfile>central-publishing</activeProfile>
</activeProfiles>
```

本地环境变量：

```bash
export CENTRAL_USERNAME="Central Portal token username"
export CENTRAL_PASSWORD="Central Portal token password"
export GPG_PASSPHRASE="GPG key passphrase"
```

GitHub Actions Secrets：

```text
CENTRAL_USERNAME
CENTRAL_PASSWORD
GPG_PRIVATE_KEY
GPG_PASSPHRASE
```

## 1. 本地验证

不执行真实发布：

```bash
bash ./mvnw -B -ntp -DskipTests validate
bash ./mvnw -B -ntp -f egon-cola-components/pom.xml -Prelease -DskipTests verify
bash ./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -Prelease -DskipTests verify
```

如果只想验证 profile 绑定但不签名：

```bash
bash ./mvnw -B -ntp -f egon-cola-components/pom.xml -Prelease -DskipTests -Dgpg.skip=true verify
bash ./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -Prelease -DskipTests -Dgpg.skip=true verify
```

验证 deploy 生命周期但不上传到 Central：

```bash
bash ./mvnw -B -ntp -N -Prelease -DskipTests -DskipPublishing=true clean deploy
bash ./mvnw -B -ntp -N -f egon-cola-components/pom.xml -Prelease -DskipTests -DskipPublishing=true clean deploy
bash ./mvnw -B -ntp -N -f egon-cola-archetypes/pom.xml -Prelease -DskipTests -DskipPublishing=true clean deploy
bash ./mvnw -B -ntp -f egon-cola-components/pom.xml -Prelease -DskipTests -DskipPublishing=true clean deploy
bash ./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -Prelease -DskipTests -DskipPublishing=true clean deploy
```

注意：`-Dgpg.skip=true` 只能用于本地 verify，不能用于真实 deploy。真实发布必须生成 `.asc` 签名文件。

## 2. 发布父 POM

三个父 POM 都支持独立发布。第一次发布时建议按依赖顺序执行：先发布 aggregation parent，再发布 components parent 和 archetypes parent。

```bash
bash ./mvnw -B -ntp -N -Prelease -DskipTests clean deploy
bash ./mvnw -B -ntp -N -f egon-cola-components/pom.xml -Prelease -DskipTests clean deploy
bash ./mvnw -B -ntp -N -f egon-cola-archetypes/pom.xml -Prelease -DskipTests clean deploy
```

## 3. 发布 Components

确认版本号不是 `SNAPSHOT`，然后执行：

```bash
bash ./mvnw -B -ntp -f egon-cola-components/pom.xml -Prelease -DskipTests clean deploy
```

## 4. 发布 Archetypes

建议先发布 components，等待 Maven Central 可解析后再发布 archetypes：

```bash
bash ./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -Prelease -DskipTests clean deploy
```

## 5. GitHub Actions 手动发布

使用 `.github/workflows/publish-maven-central.yml` 的 `workflow_dispatch` 手动触发。

可选目标：

```text
egon-cola-aggregation-parent
egon-cola-components-parent
egon-cola-archetypes-parent
egon-cola-components
egon-cola-archetypes
all
```

## 6. 常见失败

- `401 Unauthorized`：`central` server id 缺失、Central Token 错误、或 Secret 未注入。
- `403 Forbidden`：`top.egon` namespace 未验证，或版本已经发布过。
- `repository element was not specified`：没有启用 `-Prelease`，或 release profile 没有加载；Central Portal release 发布必须走 `central-publishing-maven-plugin`，不能走默认 `maven-deploy-plugin` 的 release repository。
- Missing Signature：GPG 私钥或 `GPG_PASSPHRASE` 不可用。
- Missing Signature 且命令里有 `-Dgpg.skip=true`：这是本地跳过签名参数，不可用于真实发布。
- Missing Sources/Javadocs：`-Prelease` 未生效。
