#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd -P)"
ARCHETYPES_ROOT="${PROJECT_ROOT}/egon-cola-archetypes"
SOURCE_ROOT="${ARCHETYPES_ROOT}/source-projects"
DEFINITIONS_ROOT="${ARCHETYPES_ROOT}/definitions"
MAVEN_WRAPPER="${PROJECT_ROOT}/mvnw"
GENERATED_ROOT="${ARCHETYPES_ROOT}/.generated"
LOCK_DIR="${ARCHETYPES_ROOT}/.generated.lock"
readonly SCRIPT_DIR PROJECT_ROOT ARCHETYPES_ROOT SOURCE_ROOT DEFINITIONS_ROOT MAVEN_WRAPPER GENERATED_ROOT LOCK_DIR

STAGING_ROOT=''
BACKUP_ROOT=''
BACKUP_MOVED=false
LOCK_HELD=false
MANIFESTS=()
MODULE_SUFFIXES=()
TARGET_ARTIFACT_IDS=()
ROOT_VERSION=''
CURRENT_DEFINITION_ROOT=''
CURRENT_PACKAGING_POM=''

usage() {
  printf 'Usage: %s <generate|check>\n' "${0##*/}"
}

die() {
  printf 'generate-archetypes: %s\n' "$*" >&2
  exit 1
}

cleanup_on_exit() {
  local exit_code=$?
  trap - EXIT HUP INT TERM

  if [[ -n "$STAGING_ROOT" && -e "$STAGING_ROOT" ]]; then
    rm -rf -- "$STAGING_ROOT"
  fi

  if [[ "$BACKUP_MOVED" == true && ! -e "$GENERATED_ROOT" && -e "$BACKUP_ROOT" ]]; then
    mv -- "$BACKUP_ROOT" "$GENERATED_ROOT" || exit_code=1
    BACKUP_MOVED=false
    BACKUP_ROOT=''
  fi

  if [[ -n "$BACKUP_ROOT" && -e "$BACKUP_ROOT" ]]; then
    rm -rf -- "$BACKUP_ROOT"
  fi

  if [[ "$LOCK_HELD" == true && -d "$LOCK_DIR" ]]; then
    rm -f -- "$LOCK_DIR/owner"
    rmdir "$LOCK_DIR" || exit_code=1
  fi
  exit "$exit_code"
}

trap cleanup_on_exit EXIT
trap 'exit 129' HUP
trap 'exit 130' INT
trap 'exit 143' TERM

trim() {
  local value="$1"
  value="${value#"${value%%[![:space:]]*}"}"
  value="${value%"${value##*[![:space:]]}"}"
  printf '%s' "$value"
}

escape_sed_pattern() {
  printf '%s' "$1" | sed 's/[.[\*^$()+?{|\\]/\\&/g'
}

escape_sed_replacement() {
  printf '%s' "$1" | sed 's/[&|\\]/\\&/g'
}

hash_file() {
  local file="$1"
  if command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$file" | awk '{print $1}'
    return
  fi
  command -v sha256sum >/dev/null 2>&1 || die 'neither shasum nor sha256sum is available'
  sha256sum "$file" | awk '{print $1}'
}

file_mode() {
  local file="$1"
  stat -f '%Lp' "$file" 2>/dev/null || stat -c '%a' "$file"
}

hash_tree() {
  local tree="$1" file relative mode
  [[ -d "$tree" ]] || die "hash input does not exist: $tree"
  while IFS= read -r file; do
    relative="${file#"$tree"/}"
    mode="$(file_mode "$file")"
    printf '%s  %s  %s\n' "$(hash_file "$file")" "$mode" "$relative"
  done < <(find "$tree" -type f -print | LC_ALL=C sort)
}

hash_product_tree() {
  local tree="$1" file relative mode
  [[ -d "$tree" ]] || die "hash input does not exist: $tree"
  while IFS= read -r file; do
    relative="${file#"$tree"/}"
    [[ "$relative" != generation-manifest.sha256 ]] || continue
    mode="$(file_mode "$file")"
    printf '%s  %s  %s\n' "$(hash_file "$file")" "$mode" "$relative"
  done < <(
    find "$tree" -type f \
      ! -path '*/target/*' \
      -print | LC_ALL=C sort
  )
}

hash_source_tree() {
  local tree="$1" file relative mode
  [[ -d "$tree" ]] || die "hash input does not exist: $tree"
  while IFS= read -r file; do
    relative="${file#"$tree"/}"
    mode="$(file_mode "$file")"
    printf '%s  %s  %s\n' "$(hash_file "$file")" "$mode" "$relative"
  done < <(
    find "$tree" -type f \
      ! -path '*/target/*' \
      ! -path '*/.git/*' \
      ! -path '*/.idea/*' \
      -print | LC_ALL=C sort
  )
}

discover_manifests() {
  MANIFESTS=()
  TARGET_ARTIFACT_IDS=()
  [[ -d "$DEFINITIONS_ROOT" ]] || die "definitions root is missing: $DEFINITIONS_ROOT"
  while IFS= read -r definition; do
    [[ -n "$definition" ]] || continue
    manifest="$definition/archetype.properties"
    [[ -f "$manifest" ]] || die "archetype.properties is missing: $definition"
    [[ -f "$definition/packaging-pom.xml" ]] \
      || die "packaging-pom.xml is missing: $definition"
    MANIFESTS+=("$manifest")
  done < <(find "$DEFINITIONS_ROOT" -mindepth 1 -maxdepth 1 -type d \
    -name 'egon-cola-archetype-*' -print | LC_ALL=C sort)
  ((${#MANIFESTS[@]} > 0)) || die 'no archetype manifests found'
  local manifest target
  for manifest in "${MANIFESTS[@]}"; do
    parse_manifest "$manifest"
    target="$MF_TARGET_ARTIFACT_ID"
    if ((${#TARGET_ARTIFACT_IDS[@]} > 0)); then
      for existing_target in "${TARGET_ARTIFACT_IDS[@]}"; do
        [[ "$existing_target" != "$target" ]] \
          || die "duplicate targetArtifactId ${target} across definitions"
      done
    fi
    TARGET_ARTIFACT_IDS+=("$target")
  done
}

parse_manifest() {
  local manifest="$1" line key value
  MF_SOURCE_PROJECT=''
  MF_SOURCE_GROUP_ID=''
  MF_SOURCE_ARTIFACT_ID=''
  MF_SOURCE_VERSION=''
  MF_SOURCE_PACKAGE=''
  MF_TARGET_ARTIFACT_ID=''
  MF_EXPECTED_TOPOLOGY=''
  local seen_source_project=0 seen_source_group=0 seen_source_artifact=0 seen_source_version=0
  local seen_source_package=0 seen_target_artifact=0 seen_topology=0

  while IFS= read -r line || [[ -n "$line" ]]; do
    line="${line%$'\r'}"
    line="$(trim "$line")"
    [[ -z "$line" || "${line:0:1}" == '#' ]] && continue
    [[ "$line" == *=* ]] || die "invalid manifest line in $manifest: $line"
    key="$(trim "${line%%=*}")"
    value="$(trim "${line#*=}")"
    case "$key" in
      sourceProject)
        ((seen_source_project == 0)) || die "duplicate manifest field sourceProject in $manifest"
        MF_SOURCE_PROJECT="$value"; seen_source_project=1 ;;
      sourceGroupId)
        ((seen_source_group == 0)) || die "duplicate manifest field sourceGroupId in $manifest"
        MF_SOURCE_GROUP_ID="$value"; seen_source_group=1 ;;
      sourceArtifactId)
        ((seen_source_artifact == 0)) || die "duplicate manifest field sourceArtifactId in $manifest"
        MF_SOURCE_ARTIFACT_ID="$value"; seen_source_artifact=1 ;;
      sourceVersion)
        ((seen_source_version == 0)) || die "duplicate manifest field sourceVersion in $manifest"
        MF_SOURCE_VERSION="$value"; seen_source_version=1 ;;
      sourcePackage)
        ((seen_source_package == 0)) || die "duplicate manifest field sourcePackage in $manifest"
        MF_SOURCE_PACKAGE="$value"; seen_source_package=1 ;;
      targetArtifactId)
        ((seen_target_artifact == 0)) || die "duplicate manifest field targetArtifactId in $manifest"
        MF_TARGET_ARTIFACT_ID="$value"; seen_target_artifact=1 ;;
      expectedTopology)
        ((seen_topology == 0)) || die "duplicate manifest field expectedTopology in $manifest"
        MF_EXPECTED_TOPOLOGY="$value"; seen_topology=1 ;;
      *) die "unknown manifest field $key in $manifest" ;;
    esac
  done <"$manifest"

  [[ "$seen_source_project" == 1 && -n "$MF_SOURCE_PROJECT" ]] || die "missing sourceProject in $manifest"
  [[ "$seen_source_group" == 1 && "$MF_SOURCE_GROUP_ID" =~ ^[A-Za-z0-9._-]+$ ]] || die "invalid sourceGroupId in $manifest"
  [[ "$seen_source_artifact" == 1 && "$MF_SOURCE_ARTIFACT_ID" =~ ^[A-Za-z0-9][A-Za-z0-9.-]*$ ]] || die "invalid sourceArtifactId in $manifest"
  [[ "$seen_source_version" == 1 && "$MF_SOURCE_VERSION" =~ ^[A-Za-z0-9][A-Za-z0-9._+-]*$ ]] || die "invalid sourceVersion in $manifest"
  [[ "$seen_source_package" == 1 && "$MF_SOURCE_PACKAGE" =~ ^[A-Za-z][A-Za-z0-9_.]*$ ]] || die "invalid sourcePackage in $manifest"
  [[ "$seen_target_artifact" == 1 && "$MF_TARGET_ARTIFACT_ID" =~ ^[A-Za-z0-9][A-Za-z0-9.-]*$ ]] || die "invalid targetArtifactId in $manifest"
  [[ "$seen_topology" == 1 && -n "$MF_EXPECTED_TOPOLOGY" ]] || die "missing expectedTopology in $manifest"
  [[ "$MF_SOURCE_PROJECT" != /* ]] || die "absolute sourceProject in $manifest"
  [[ "$MF_SOURCE_PROJECT" != *'|'* && "$MF_SOURCE_PROJECT" != *$'\n'* ]] || die "invalid sourceProject in $manifest"

  local package_root source_candidate source_dir package_pom definition_root source_pom
  definition_root="$(cd "$(dirname "$manifest")" && pwd -P)"
  package_root="$ARCHETYPES_ROOT"
  source_candidate="$package_root/$MF_SOURCE_PROJECT"
  [[ -d "$source_candidate" ]] || die "sourceProject does not exist: $MF_SOURCE_PROJECT ($manifest)"
  source_dir="$(cd "$source_candidate" && pwd -P)"
  case "$source_dir/" in
    "$SOURCE_ROOT"/*) ;;
    *) die "sourceProject escapes source-projects: $MF_SOURCE_PROJECT ($manifest)" ;;
  esac
  [[ -f "$source_dir/pom.xml" ]] || die "source POM is missing: $source_dir/pom.xml"
  grep -Fq "<artifactId>${MF_SOURCE_ARTIFACT_ID}</artifactId>" "$source_dir/pom.xml" \
    || die "sourceArtifactId ${MF_SOURCE_ARTIFACT_ID} does not match source POM $source_dir/pom.xml"
  package_pom="$definition_root/packaging-pom.xml"
  grep -Fq "<artifactId>${MF_TARGET_ARTIFACT_ID}</artifactId>" "$package_pom" \
    || die "targetArtifactId ${MF_TARGET_ARTIFACT_ID} does not match packaging POM $package_pom"
  grep -Fq '<packaging>maven-archetype</packaging>' "$package_pom" \
    || die "packaging POM is not a maven-archetype: $package_pom"

  MODULE_SUFFIXES=()
  if [[ "$MF_EXPECTED_TOPOLOGY" != root ]]; then
    local suffix
    IFS=',' read -r -a MODULE_SUFFIXES <<<"$MF_EXPECTED_TOPOLOGY"
    ((${#MODULE_SUFFIXES[@]} > 0)) || die "expectedTopology is empty in $manifest"
    for suffix in "${MODULE_SUFFIXES[@]}"; do
      [[ "$suffix" =~ ^[A-Za-z0-9][A-Za-z0-9.-]*$ ]] || die "invalid module suffix $suffix in $manifest"
      source_pom="$source_dir/${MF_SOURCE_ARTIFACT_ID}-${suffix}/pom.xml"
      [[ -f "$source_pom" ]] || die "source module POM missing: $source_pom"
    done
  fi

  CURRENT_MANIFEST="$manifest"
  CURRENT_PACKAGE_ROOT="$package_root"
  CURRENT_SOURCE_DIR="$source_dir"
  CURRENT_DEFINITION_ROOT="$definition_root"
  CURRENT_PACKAGING_POM="$package_pom"
}

normalize_text_file() {
  local file="$1" pattern replacement temp
  LC_ALL=C grep -Iq . "$file" || return 0
  temp="${file}.tmp.$$"
  pattern="$(escape_sed_pattern "$MF_SOURCE_GROUP_ID")"
  replacement="$(escape_sed_replacement '${groupId}')"
  sed "s|$pattern|$replacement|g" "$file" >"$temp"
  pattern="$(escape_sed_pattern "$MF_SOURCE_VERSION")"
  replacement="$(escape_sed_replacement '${version}')"
  sed "s|$pattern|$replacement|g" "$temp" >"${temp}.2"
  mv -- "${temp}.2" "$temp"
  pattern="$(escape_sed_pattern "$MF_SOURCE_PACKAGE")"
  replacement="$(escape_sed_replacement '${package}')"
  sed "s|$pattern|$replacement|g" "$temp" >"${temp}.2"
  mv -- "${temp}.2" "$temp"
  pattern="$(escape_sed_pattern "$MF_SOURCE_ARTIFACT_ID")"
  if [[ "$MF_EXPECTED_TOPOLOGY" == root ]]; then
    replacement="$(escape_sed_replacement '${artifactId}')"
  else
    replacement="$(escape_sed_replacement '${rootArtifactId}')"
  fi
  sed "s|$pattern|$replacement|g" "$temp" >"${temp}.2"
  mv -- "${temp}.2" "$file"
  rm -f -- "$temp"
}

normalize_text_tree() {
  local file
  while IFS= read -r file; do
    normalize_text_file "$file"
  done < <(find "$1" -type f -print | LC_ALL=C sort)
}

escape_velocity_file() {
  local file="$1" resources="$2" relative temp
  relative="${file#"$resources"/}"
  case "$relative" in
    __gitignore__|.gitattributes|mvnw|mvnw.cmd|Jenkinsfile|.mvn/wrapper/maven-wrapper.properties|deploy/container/README.md|deploy/compose/*)
      return 0
      ;;
  esac
  LC_ALL=C grep -Iq . "$file" || return 0
  temp="${file}.velocity.tmp.$$"
  awk '
    function allowed_token(token) {
      return token == "artifactId" || token == "groupId" || token == "version" ||
             token == "package" || token == "packageInPathFormat" ||
             token == "rootArtifactId" || token == "symbol_pound" ||
             token == "symbol_dollar" || token == "symbol_escape"
    }
    function escape_dollar(line, out, i, j, depth, token, ch, len) {
      out = ""
      i = 1
      len = length(line)
      while (i <= len) {
        ch = substr(line, i, 1)
        if (ch != "$") {
          out = out ch
          i++
          continue
        }
        if (i < len && substr(line, i + 1, 1) == "{") {
          j = i + 2
          depth = 1
          while (j <= len && depth > 0) {
            ch = substr(line, j, 1)
            if (ch == "{") {
              depth++
            } else if (ch == "}") {
              depth--
            }
            j++
          }
          if (depth == 0) {
            token = substr(line, i + 2, (j - 1) - (i + 2))
            if (allowed_token(token)) {
              out = out substr(line, i, j - i)
              i = j
              continue
            }
          }
        }
        out = out "${symbol_dollar}"
        need_dollar = 1
        i++
      }
      return out
    }
    function escape_pound(line, out, i, len, ch) {
      out = ""
      i = 1
      len = length(line)
      while (i <= len) {
        ch = substr(line, i, 1)
        if (ch == "#" && i < len && substr(line, i + 1, 1) == "#") {
          out = out "${symbol_pound}${symbol_pound}"
          need_pound = 1
          i += 2
        } else {
          out = out ch
          i++
        }
      }
      return out
    }
    {
      lines[++count] = $0
      if ($0 ~ /^#set[[:space:]]*\(/) {
        if ($0 ~ /symbol_dollar/) {
          has_dollar = 1
        }
        if ($0 ~ /symbol_pound/) {
          has_pound = 1
        }
        next
      }
      lines[count] = escape_dollar(lines[count])
      lines[count] = escape_pound(lines[count])
    }
    END {
      if (need_pound && !has_pound) {
        print "#set( $symbol_pound = '\''#'\'' )"
      }
      if (need_dollar && !has_dollar) {
        print "#set( $symbol_dollar = '\''$'\'' )"
      }
      for (i = 1; i <= count; i++) {
        print lines[i]
      }
    }
  ' "$file" >"$temp"
  mv -- "$temp" "$file"
}

escape_velocity_tree() {
  local resources="$1" file
  while IFS= read -r file; do
    escape_velocity_file "$file" "$resources"
  done < <(find "$resources" -type f -print | LC_ALL=C sort)
}

normalize_first_root_artifact() {
  local file="$1" temp="$1.tmp.$$"
  awk -v replacement='${rootArtifactId}-parent' '
    BEGIN { changed = 0 }
    {
      if (!changed && index($0, "<artifactId>${rootArtifactId}</artifactId>") > 0) {
        sub(/<artifactId>\$\{rootArtifactId\}<\/artifactId>/,
            "<artifactId>" replacement "</artifactId>")
        changed = 1
      }
      print
    }
  ' "$file" >"$temp"
  mv -- "$temp" "$file"
}

rename_module_directories() {
  local suffix source_dir target_dir
  for suffix in "${MODULE_SUFFIXES[@]}"; do
    source_dir="$1/${MF_SOURCE_ARTIFACT_ID}-${suffix}"
    target_dir="$1/__rootArtifactId__-${suffix}"
    if [[ -d "$source_dir" && ! -e "$target_dir" ]]; then
      mv -- "$source_dir" "$target_dir"
    fi
    [[ -d "$target_dir" ]] || die "generated module directory missing: ${MF_SOURCE_ARTIFACT_ID}-${suffix}"
  done
}

overlay_source_poms() {
  local resources="$1" source_pom target_pom suffix target_dir
  cp -p "$CURRENT_SOURCE_DIR/pom.xml" "$resources/pom.xml"
  if [[ "$MF_EXPECTED_TOPOLOGY" != root ]]; then
    normalize_first_root_artifact "$resources/pom.xml"
    for suffix in "${MODULE_SUFFIXES[@]}"; do
      source_pom="$CURRENT_SOURCE_DIR/${MF_SOURCE_ARTIFACT_ID}-${suffix}/pom.xml"
      target_dir="$resources/__rootArtifactId__-${suffix}"
      target_pom="$target_dir/pom.xml"
      [[ -f "$source_pom" ]] || die "source module POM missing: $source_pom"
      cp -p "$source_pom" "$target_pom"
      normalize_first_root_artifact "$target_pom"
    done
  fi
}

remove_forbidden_generated_paths() {
  local resources="$1" forbidden
  for forbidden in .git .idea target; do
    while IFS= read -r path; do
      [[ -n "$path" ]] || continue
      rm -rf -- "$path"
    done < <(find "$resources" -type d -name "$forbidden" -print | LC_ALL=C sort -r)
  done
  while IFS= read -r path; do
    rm -f -- "$path"
  done < <(find "$resources" -type f -name archetype.properties -print | LC_ALL=C sort)
}

validate_no_source_sentinels() {
  local resources="$1" file value
  for value in "$MF_SOURCE_GROUP_ID" "$MF_SOURCE_VERSION" "$MF_SOURCE_PACKAGE" "$MF_SOURCE_ARTIFACT_ID"; do
    while IFS= read -r file; do
      LC_ALL=C grep -Iq . "$file" || continue
      if grep -Fq -- "$value" "$file"; then
        die "source sentinel remains in generated resources: $value ($file)"
      fi
    done < <(find "$resources" -type f -print | LC_ALL=C sort)
  done
}

validate_topology() {
  local resources="$1" suffix
  [[ -f "$resources/pom.xml" ]] || die "generated root POM missing: $resources/pom.xml"
  if [[ "$MF_EXPECTED_TOPOLOGY" != root ]]; then
    for suffix in "${MODULE_SUFFIXES[@]}"; do
      [[ -f "$resources/__rootArtifactId__-${suffix}/pom.xml" ]] \
        || die "generated topology missing POM for module ${suffix}"
    done
  fi
}

normalize_generated_product() {
  local output_root="$1" resources="$1/src/main/resources/archetype-resources"
  [[ -d "$resources" ]] || die "create-from-project did not produce archetype-resources: $resources"
  remove_forbidden_generated_paths "$resources"
  if [[ -f "$CURRENT_SOURCE_DIR/.gitignore" ]]; then
    cp -p "$CURRENT_SOURCE_DIR/.gitignore" "$resources/__gitignore__"
  fi
  if [[ "$MF_EXPECTED_TOPOLOGY" != root ]]; then
    rename_module_directories "$resources"
  fi
  normalize_text_tree "$resources"
  overlay_source_poms "$resources"
  normalize_text_tree "$resources"
  escape_velocity_tree "$resources"
  validate_no_source_sentinels "$resources"
  validate_topology "$resources"
}

copy_curated_assets() {
  local product_root="$1" definition="$CURRENT_DEFINITION_ROOT" asset
  for asset in \
    "$definition/src/main/resources/META-INF/maven/archetype-metadata.xml" \
    "$definition/src/main/resources/META-INF/archetype-post-generate.groovy" \
    "$definition/src/main/javadoc/README.md" \
    "$definition/src/test/resources/projects/basic/archetype.properties" \
    "$definition/src/test/resources/projects/basic/goal.txt" \
    "$definition/src/test/resources/projects/basic/verify.groovy"; do
    [[ -f "$asset" ]] || die "curated file is missing: $asset"
  done
  [[ -d "$definition/architecture-docs" ]] || die "curated architecture-docs is missing: $definition"
  find "$definition/architecture-docs" -type f -print -quit | grep -q . \
    || die "curated architecture-docs is empty: $definition"
  if grep -R -Fq -- "$PROJECT_ROOT" "$definition/src/main/javadoc" "$definition/architecture-docs"; then
    die "curated documentation contains a local absolute path: $definition"
  fi

  mkdir -p "$product_root/src/main/resources/META-INF/maven" \
    "$product_root/src/main/javadoc" \
    "$product_root/src/test/resources/projects/basic" \
    "$product_root/architecture-docs"
  cp -p "$definition/src/main/resources/META-INF/maven/archetype-metadata.xml" \
    "$product_root/src/main/resources/META-INF/maven/archetype-metadata.xml"
  cp -p "$definition/src/main/resources/META-INF/archetype-post-generate.groovy" \
    "$product_root/src/main/resources/META-INF/archetype-post-generate.groovy"
  cp -p "$definition/src/main/javadoc/README.md" "$product_root/src/main/javadoc/README.md"
  for asset in archetype.properties goal.txt verify.groovy; do
    cp -p "$definition/src/test/resources/projects/basic/$asset" \
      "$product_root/src/test/resources/projects/basic/$asset"
  done
  while IFS= read -r asset; do
    cp -p "$asset" "$product_root/architecture-docs/$(basename "$asset")"
  done < <(find "$definition/architecture-docs" -type f -print | LC_ALL=C sort)
}

compose_child_module() {
  local product_root="$1" temp
  [[ -f "$CURRENT_PACKAGING_POM" ]] || die "packaging POM is missing: $CURRENT_PACKAGING_POM"
  cp -p "$CURRENT_PACKAGING_POM" "$product_root/pom.xml"
  temp="$product_root/pom.xml.tmp.$$"
  sed "s|@rootVersion@|$(escape_sed_replacement "$ROOT_VERSION")|g" \
    "$product_root/pom.xml" >"$temp"
  mv -- "$temp" "$product_root/pom.xml"
  grep -Fq '<packaging>maven-archetype</packaging>' "$product_root/pom.xml" \
    || die "generated child is not a maven-archetype: $product_root/pom.xml"
  grep -Fq '@rootVersion@' "$product_root/pom.xml" \
    && die "rootVersion token remains in generated child: $product_root/pom.xml"
  copy_curated_assets "$product_root"
}

resolve_root_version() {
  local evaluated
  evaluated="$($MAVEN_WRAPPER -q -N help:evaluate -Dexpression=project.version -DforceStdout 2>/dev/null \
    | tail -n 1 | tr -d '\r')"
  [[ -n "$evaluated" && "$evaluated" != *'${'* && "$evaluated" =~ ^[A-Za-z0-9][A-Za-z0-9._+-]*$ ]] \
    || die "unable to resolve a concrete archetypes parent version"
  ROOT_VERSION="$evaluated"
}

write_generated_aggregator() {
  local target
  cat >"$STAGING_ROOT/pom.xml" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>top.egon</groupId>
        <artifactId>egon-cola-archetypes-parent</artifactId>
        <version>${ROOT_VERSION}</version>
        <relativePath>../pom.xml</relativePath>
    </parent>
    <artifactId>egon-cola-generated-archetypes-reactor</artifactId>
    <packaging>pom</packaging>
    <name>egon-cola-generated-archetypes-reactor</name>
    <description>Generated Maven Archetype publishing reactor</description>
    <properties>
        <archetype.preflight.skip>true</archetype.preflight.skip>
    </properties>
    <modules>
EOF
  while IFS= read -r target; do
    [[ -n "$target" ]] || continue
    printf '        <module>%s</module>\n' "$target" >>"$STAGING_ROOT/pom.xml"
  done < <(printf '%s\n' "${TARGET_ARTIFACT_IDS[@]}" | LC_ALL=C sort)
  cat >>"$STAGING_ROOT/pom.xml" <<'EOF'
    </modules>
    <build>
        <plugins>
            <plugin>
                <artifactId>maven-deploy-plugin</artifactId>
                <configuration><skip>true</skip></configuration>
            </plugin>
        </plugins>
    </build>
</project>
EOF
}

acquire_lock() {
  if ! mkdir "$LOCK_DIR" 2>/dev/null; then
    die "generation lock is held: $LOCK_DIR"
  fi
  LOCK_HELD=true
  printf 'pid=%s\n' "$$" >"$LOCK_DIR/owner"
}

write_source_snapshot() {
  local snapshot="$1" manifest
  : >"$snapshot"
  for manifest in "${MANIFESTS[@]}"; do
    parse_manifest "$manifest"
    printf 'manifest=%s\n' "${manifest#"$PROJECT_ROOT"/}" >>"$snapshot"
    hash_source_tree "$CURRENT_SOURCE_DIR" >>"$snapshot"
  done
  LC_ALL=C sort -o "$snapshot" "$snapshot"
}

assert_source_snapshot_unchanged() {
  local before="$1" after="$STAGING_ROOT/source.snapshot.after"
  write_source_snapshot "$after"
  cmp -s "$before" "$after" \
    || die 'source files changed during generation; rerun from a stable worktree'
}

write_plugin_properties() {
  local file="$1"
  printf 'rootArtifactId=%s\n' "$MF_SOURCE_ARTIFACT_ID" >"$file"
}

generate_one() {
  local manifest="$1" product_root plugin_output plugin_properties plugin_log resources manifest_hash
  parse_manifest "$manifest"
  product_root="$STAGING_ROOT/$MF_TARGET_ARTIFACT_ID"
  mkdir -p "$product_root"
  plugin_output="$product_root/plugin-output"
  plugin_properties="$product_root/create-from-project.properties"
  plugin_log="$product_root/create-from-project.log"
  write_plugin_properties "$plugin_properties"

  "$MAVEN_WRAPPER" -B -ntp -f "$CURRENT_SOURCE_DIR/pom.xml" \
    org.apache.maven.plugins:maven-archetype-plugin:3.4.1:create-from-project \
    -Dinteractive=false \
    -DpackageName="$MF_SOURCE_PACKAGE" \
    -DpropertyFile="$plugin_properties" \
    -DoutputDirectory="$plugin_output" >"$plugin_log" 2>&1 \
    || {
      tail -n 40 "$plugin_log" >&2 || true
      die "create-from-project failed for ${MF_TARGET_ARTIFACT_ID}"
    }

  resources="$plugin_output/src/main/resources/archetype-resources"
  [[ -d "$resources" ]] || die "generated resources missing for ${MF_TARGET_ARTIFACT_ID}"
  normalize_generated_product "$plugin_output"
  mv -- "$resources" "$product_root/archetype-resources"
  rm -rf -- "$plugin_output" "$plugin_properties" "$plugin_log"
  compose_child_module "$product_root"
  manifest_hash="$product_root/generation-manifest.sha256"
  {
    printf 'generator=maven-archetype-plugin:3.4.1:create-from-project\n'
    printf 'rootVersion=%s\n' "$ROOT_VERSION"
    printf 'sourceProject=%s\n' "${MF_SOURCE_PROJECT}"
    printf 'sourceGroupId=%s\n' "${MF_SOURCE_GROUP_ID}"
    printf 'sourceArtifactId=%s\n' "${MF_SOURCE_ARTIFACT_ID}"
    printf 'sourceVersion=%s\n' "${MF_SOURCE_VERSION}"
    printf 'sourcePackage=%s\n' "${MF_SOURCE_PACKAGE}"
    printf 'targetArtifactId=%s\n' "${MF_TARGET_ARTIFACT_ID}"
    printf 'expectedTopology=%s\n' "${MF_EXPECTED_TOPOLOGY}"
    printf '[product]\n'
    hash_product_tree "$product_root"
  } >"$manifest_hash"
  printf 'generated %s (%s files)\n' "$MF_TARGET_ARTIFACT_ID" \
    "$(find "$product_root" -type f ! -name generation-manifest.sha256 | wc -l | tr -d ' ')"
}

generate_all() {
  local manifest
  for manifest in "${MANIFESTS[@]}"; do
    generate_one "$manifest"
  done
}

validate_generated_set() {
  local manifest product_dir expected_modules actual_modules
  [[ -f "$STAGING_ROOT/pom.xml" ]] || die 'generated aggregator POM is missing'
  for manifest in "${MANIFESTS[@]}"; do
    parse_manifest "$manifest"
    product_dir="$STAGING_ROOT/$MF_TARGET_ARTIFACT_ID"
    [[ -d "$product_dir/archetype-resources" ]] || die "staged target missing: $MF_TARGET_ARTIFACT_ID"
    [[ -s "$product_dir/generation-manifest.sha256" ]] || die "generation manifest missing: $MF_TARGET_ARTIFACT_ID"
    [[ -f "$product_dir/pom.xml" ]] || die "generated child POM missing: $MF_TARGET_ARTIFACT_ID"
    grep -Fq '<packaging>maven-archetype</packaging>' "$product_dir/pom.xml" \
      || die "generated child packaging is invalid: $MF_TARGET_ARTIFACT_ID"
    grep -Fq "<artifactId>${MF_TARGET_ARTIFACT_ID}</artifactId>" "$product_dir/pom.xml" \
      || die "generated child artifactId is invalid: $MF_TARGET_ARTIFACT_ID"
    grep -Fq '@rootVersion@' "$product_dir/pom.xml" \
      && die "generated child contains unresolved rootVersion token: $MF_TARGET_ARTIFACT_ID"
    for curated in \
      "$product_dir/src/main/resources/META-INF/maven/archetype-metadata.xml" \
      "$product_dir/src/main/resources/META-INF/archetype-post-generate.groovy" \
      "$product_dir/src/main/javadoc/README.md" \
      "$product_dir/src/test/resources/projects/basic/archetype.properties" \
      "$product_dir/src/test/resources/projects/basic/goal.txt" \
      "$product_dir/src/test/resources/projects/basic/verify.groovy"; do
      [[ -f "$curated" ]] || die "generated curated file is missing: $curated"
    done
    [[ -d "$product_dir/architecture-docs" ]] || die "generated architecture-docs is missing: $MF_TARGET_ARTIFACT_ID"
  done
  expected_modules="$(printf '%s\n' "${TARGET_ARTIFACT_IDS[@]}" | LC_ALL=C sort)"
  actual_modules="$(sed -n 's/^[[:space:]]*<module>\([^<]*\)<\/module>[[:space:]]*$/\1/p' "$STAGING_ROOT/pom.xml")"
  [[ "$expected_modules" == "$actual_modules" ]] \
    || die 'generated aggregator module set does not match definitions'
}

swap_staging_into_place() {
  local candidate
  if [[ -e "$GENERATED_ROOT" ]]; then
    candidate="$(mktemp -d "${ARCHETYPES_ROOT}/.generated.previous.XXXXXX")"
    rmdir "$candidate"
    mv -- "$GENERATED_ROOT" "$candidate" || die 'failed to move the previous generated set'
    BACKUP_ROOT="$candidate"
    BACKUP_MOVED=true
  fi
  mv -- "$STAGING_ROOT" "$GENERATED_ROOT" || die 'failed to publish the generated set atomically'
  STAGING_ROOT=''
  if [[ "$BACKUP_MOVED" == true ]]; then
    rm -rf -- "$BACKUP_ROOT"
    BACKUP_ROOT=''
    BACKUP_MOVED=false
  fi
}

compare_generated_set() {
  local manifest product staging_hash generated_hash
  [[ -d "$GENERATED_ROOT" ]] || die 'generated workspace is missing; run generate first'
  cmp -s "$STAGING_ROOT/pom.xml" "$GENERATED_ROOT/pom.xml" \
    || die 'generated aggregator is not deterministic'
  for manifest in "${MANIFESTS[@]}"; do
    parse_manifest "$manifest"
    product="$MF_TARGET_ARTIFACT_ID"
    staging_hash="$STAGING_ROOT/${product}.product.hash"
    generated_hash="$STAGING_ROOT/${product}.generated.hash"
    hash_product_tree "$STAGING_ROOT/$product" >"$staging_hash"
    hash_product_tree "$GENERATED_ROOT/$product" >"$generated_hash"
    cmp -s "$staging_hash" "$generated_hash" \
      || die "generated resources are not deterministic for ${product}"
    cmp -s "$STAGING_ROOT/$product/generation-manifest.sha256" "$GENERATED_ROOT/$product/generation-manifest.sha256" \
      || die "generated provenance differs for ${product}"
  done
}

run_pipeline() {
  local mode="$1"
  discover_manifests
  resolve_root_version
  acquire_lock
  STAGING_ROOT="$(mktemp -d "${ARCHETYPES_ROOT}/.generated.staging.XXXXXX")"
  write_source_snapshot "$STAGING_ROOT/source.snapshot.before"
  generate_all
  write_generated_aggregator
  validate_generated_set
  assert_source_snapshot_unchanged "$STAGING_ROOT/source.snapshot.before"
  if [[ "$mode" == generate ]]; then
    swap_staging_into_place
    printf 'generated set published atomically at %s\n' "$GENERATED_ROOT"
  else
    compare_generated_set
    printf 'generated set is deterministic and matches current workspace\n'
  fi
}

main() {
  [[ $# -eq 1 ]] || {
    usage >&2
    exit 2
  }
  case "$1" in
    generate|check) run_pipeline "$1" ;;
    *) usage >&2; exit 2 ;;
  esac
}

main "$@"
