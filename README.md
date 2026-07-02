Egon-COLA

自定义脚手架。

基于 
- JDK21
- Spring Boot 3.5.16
- Spring Cloud 2025.0.3
- Spring Cloud Alibaba 2025.1.0.0
- Spring AI 1.1.8
- Spring AI Alibaba 1.1.2.3
- others

```bash
# 整体打包发布central
./mvnw -B -ntp -f ./pom.xml -Prelease -DskipTests clean deploy
./mvnw -B -ntp -f egon-cola-components/pom.xml -Prelease -DskipTests clean deploy
./mvnw -B -ntp -f egon-cola-archetypes/pom.xml -Prelease -DskipTests clean deploy

# 生成新工程，采用云端仓库的cola 示例
cd family

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
    -DarchetypeVersion='5.1.1' `
    -DinteractiveMode='false'

# 本地仓库示例
# lite archtype 
mvn -B archetype:generate \
  -DgroupId='top.egon' \
  -DartifactId='light' \
  -Dversion='1.0.0-SNAPSHOT' \
  -Dpackage='top.egon.light' \
  -DarchetypeArtifactId='egon-cola-archetype-light' \
  -DarchetypeGroupId='top.egon' \
  -DarchetypeVersion='5.1.1' \
  -DarchetypeCatalog='local' \
  -DinteractiveMode='false'     

# service archtype    
mvn -B archetype:generate \
  -DgroupId='top.egon' \
  -DartifactId='fable' \
  -Dversion='1.0.0-SNAPSHOT' \
  -Dpackage='top.egon.fable' \
  -DarchetypeArtifactId='egon-cola-archetype-service' \
  -DarchetypeGroupId='top.egon' \
  -DarchetypeVersion='5.1.1' \
  -DarchetypeCatalog='local' \
  -DinteractiveMode='false' 
  
# web archtype    
mvn -B archetype:generate \
  -DgroupId='top.egon' \
  -DartifactId='fable-web' \
  -Dversion='1.0.0-SNAPSHOT' \
  -Dpackage='top.egon.fable-web' \
  -DarchetypeArtifactId='egon-cola-archetype-web' \
  -DarchetypeGroupId='top.egon' \
  -DarchetypeVersion='5.1.1' \
  -DarchetypeCatalog='local' \
  -DinteractiveMode='false'
```

项目创建结束后，.\cola-samples\family\ 直接拷贝走，idea 打开根pom.xml as project。添加git仓库，关联远程仓库。

## License

This project is dual-licensed under the MIT License and the GNU General Public License v3.0 or later.

You may choose either license:

- MIT License, see [LICENSE-MIT](./LICENSE-MIT)
- GNU GPL v3.0 or later, see [LICENSE-GPL-3.0-or-later](./LICENSE-GPL-3.0-or-later)
