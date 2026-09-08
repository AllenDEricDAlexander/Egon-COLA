#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
TEST_ROOT="$(mktemp -d "${TMPDIR:-/tmp}/egon-parent-normalization.XXXXXX")"
trap 'rm -rf "$TEST_ROOT"' EXIT
mkdir -p "$TEST_ROOT/scripts" "$TEST_ROOT/egon-cola-archetypes/definitions/egon-cola-archetype-sample" \
  "$TEST_ROOT/egon-cola-archetypes/source-projects/source-parent/source-parent-common"
cp "$SCRIPT_DIR/generate_archetypes.sh" "$TEST_ROOT/scripts/"

cat >"$TEST_ROOT/mvnw" <<'EOF'
#!/usr/bin/env bash
set -Eeuo pipefail
for argument in "$@"; do
  if [[ "$argument" == *help:evaluate* ]]; then printf '9.9.9\n'; exit 0; fi
  if [[ "$argument" == -DoutputDirectory=* ]]; then output="${argument#*=}"; fi
done
resources="$output/src/main/resources/archetype-resources"
mkdir -p "$resources/source-parent-common/src/main/resources"
printf '<project/>\n' >"$resources/pom.xml"
printf '<project/>\n' >"$resources/source-parent-common/pom.xml"
printf 'value=${APP_VALUE:default}\n' >"$resources/source-parent-common/src/main/resources/application.yml"
EOF
chmod +x "$TEST_ROOT/mvnw"
source_dir="$TEST_ROOT/egon-cola-archetypes/source-projects/source-parent"
definition="$TEST_ROOT/egon-cola-archetypes/definitions/egon-cola-archetype-sample"
cat >"$source_dir/pom.xml" <<'EOF'
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>example.source</groupId>
    <artifactId>local-build-parent</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <relativePath>../../local-build/pom.xml</relativePath>
  </parent>
  <groupId>example.source</groupId>
  <artifactId>source-parent</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <modules><module>source-parent-common</module></modules>
</project>
EOF
cat >"$source_dir/source-parent-common/pom.xml" <<'EOF'
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>example.source</groupId>
    <artifactId>source-parent</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <relativePath>../pom.xml</relativePath>
  </parent>
  <artifactId>source-parent-common</artifactId>
</project>
EOF
cat >"$definition/archetype.properties" <<'EOF'
sourceProject=source-projects/source-parent
sourceGroupId=example.source
sourceArtifactId=source-parent
sourceVersion=0.1.0-SNAPSHOT
sourcePackage=example.source.fixture
targetArtifactId=sample
expectedTopology=common
EOF
cat >"$definition/packaging-pom.xml" <<'EOF'
<project>
  <parent><groupId>top.egon</groupId><artifactId>egon-cola-archetypes-parent</artifactId><version>@rootVersion@</version><relativePath>../../pom.xml</relativePath></parent>
  <artifactId>sample</artifactId><packaging>maven-archetype</packaging>
</project>
EOF
mkdir -p "$definition/src/main/resources/META-INF/maven" "$definition/src/main/javadoc" \
  "$definition/src/test/resources/projects/basic" "$definition/architecture-docs"
printf '<archetype-descriptor/>\n' >"$definition/src/main/resources/META-INF/maven/archetype-metadata.xml"
printf 'true\n' >"$definition/src/main/resources/META-INF/archetype-post-generate.groovy"
printf '# fixture\n' >"$definition/src/main/javadoc/README.md"
printf '# fixture\n' >"$definition/architecture-docs/README.md"
printf 'groupId=it.test\n' >"$definition/src/test/resources/projects/basic/archetype.properties"
printf 'verify\n' >"$definition/src/test/resources/projects/basic/goal.txt"
printf 'true\n' >"$definition/src/test/resources/projects/basic/verify.groovy"
printf 'true // open boundary fixture\n' >"$definition/src/test/resources/projects/basic/open-dependency-boundary.groovy"
source_hash="$(shasum -a 256 "$source_dir/pom.xml")"
bash "$TEST_ROOT/scripts/generate_archetypes.sh" generate >/dev/null
resources="$TEST_ROOT/egon-cola-archetypes/.generated/sample/archetype-resources"
# This must fail against the old generator that preserves the local build parent.
grep -Fq '<artifactId>egon-cola-archetypes-parent</artifactId>' "$resources/pom.xml" || {
  printf 'parent-normalization-test: generated root retained the local build parent\n' >&2; exit 1;
}
grep -Fq '<version>9.9.9</version>' "$resources/pom.xml"
grep -Eq '<relativePath[[:space:]]*/>' "$resources/pom.xml"
! grep -Eq 'local-build|0.1.0-SNAPSHOT|example.source|@rootVersion@' "$resources/pom.xml"
grep -Fq '<relativePath>../pom.xml</relativePath>' "$resources/__rootArtifactId__-common/pom.xml"
grep -Fq '<artifactId>${rootArtifactId}</artifactId>' "$resources/__rootArtifactId__-common/pom.xml"
grep -Fq '<version>${version}</version>' "$resources/__rootArtifactId__-common/pom.xml"
grep -Fq '${symbol_dollar}{APP_VALUE:default}' "$resources/__rootArtifactId__-common/src/main/resources/application.yml"
grep -Fq '<relativePath>../../pom.xml</relativePath>' "$TEST_ROOT/egon-cola-archetypes/.generated/sample/pom.xml"
cmp "$definition/src/test/resources/projects/basic/open-dependency-boundary.groovy" \
  "$TEST_ROOT/egon-cola-archetypes/.generated/sample/src/test/resources/projects/basic/open-dependency-boundary.groovy"
[[ "$source_hash" == "$(shasum -a 256 "$source_dir/pom.xml")" ]]
bash "$TEST_ROOT/scripts/generate_archetypes.sh" check >/dev/null
printf 'parent-normalization-test: released root parent, module parent, sentinels, auxiliary verifier and determinism passed\n'
