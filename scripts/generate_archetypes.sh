#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd -P)"
ARCHETYPES_ROOT="${PROJECT_ROOT}/egon-cola-archetypes"
SOURCE_ROOT="${ARCHETYPES_ROOT}/source-projects"
MAVEN_WRAPPER="${PROJECT_ROOT}/mvnw"
GENERATED_ROOT="${ARCHETYPES_ROOT}/.generated"
LOCK_DIR="${ARCHETYPES_ROOT}/.generated.lock"
readonly SCRIPT_DIR PROJECT_ROOT ARCHETYPES_ROOT SOURCE_ROOT MAVEN_WRAPPER GENERATED_ROOT LOCK_DIR

STAGING_ROOT=''
BACKUP_ROOT=''
BACKUP_MOVED=false
LOCK_HELD=false
MANIFESTS=()
MODULE_SUFFIXES=()

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

read_module_names() {
  sed -n 's/^[[:space:]]*<module>\([^<]*\)<\/module>[[:space:]]*$/\1/p' \
    "$ARCHETYPES_ROOT/pom.xml"
}

archetype_module_count() {
  local module count=0
  while IFS= read -r module; do
    [[ -n "$module" ]] || continue
    if [[ -f "$ARCHETYPES_ROOT/$module/pom.xml" ]] \
      && grep -Fq '<packaging>maven-archetype</packaging>' "$ARCHETYPES_ROOT/$module/pom.xml"; then
      count=$((count + 1))
    fi
  done < <(read_module_names)
  printf '%s' "$count"
}

discover_manifests() {
  MANIFESTS=()
  while IFS= read -r manifest; do
    [[ -n "$manifest" ]] && MANIFESTS+=("$manifest")
  done < <(find "$ARCHETYPES_ROOT" -type f -path '*/src/main/archetype/archetype.properties' -print | LC_ALL=C sort)
  ((${#MANIFESTS[@]} > 0)) || die 'no archetype manifests found'
  local expected_count
  expected_count="$(archetype_module_count)"
  [[ "${#MANIFESTS[@]}" -eq "$expected_count" ]] \
    || die "manifest count ${#MANIFESTS[@]} does not match maven-archetype module count ${expected_count}"
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

  local package_root source_candidate source_dir package_pom
  package_root="$(cd "$(dirname "$manifest")/../../.." && pwd -P)"
  source_candidate="$package_root/$MF_SOURCE_PROJECT"
  [[ -d "$source_candidate" ]] || die "sourceProject does not exist: $MF_SOURCE_PROJECT ($manifest)"
  source_dir="$(cd "$source_candidate" && pwd -P)"
  case "$source_dir/" in
    "$SOURCE_ROOT"/*) ;;
    *) die "sourceProject escapes source-projects: $MF_SOURCE_PROJECT ($manifest)" ;;
  esac
  [[ -f "$source_dir/pom.xml" ]] || die "source POM is missing: $source_dir/pom.xml"
  package_pom="$package_root/pom.xml"
  grep -Fq "<artifactId>${MF_TARGET_ARTIFACT_ID}</artifactId>" "$package_pom" \
    || die "targetArtifactId ${MF_TARGET_ARTIFACT_ID} does not match package POM $package_pom"

  MODULE_SUFFIXES=()
  if [[ "$MF_EXPECTED_TOPOLOGY" != root ]]; then
    local suffix
    IFS=',' read -r -a MODULE_SUFFIXES <<<"$MF_EXPECTED_TOPOLOGY"
    ((${#MODULE_SUFFIXES[@]} > 0)) || die "expectedTopology is empty in $manifest"
    for suffix in "${MODULE_SUFFIXES[@]}"; do
      [[ "$suffix" =~ ^[A-Za-z0-9][A-Za-z0-9.-]*$ ]] || die "invalid module suffix $suffix in $manifest"
    done
  fi

  CURRENT_MANIFEST="$manifest"
  CURRENT_PACKAGE_ROOT="$package_root"
  CURRENT_SOURCE_DIR="$source_dir"
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
  validate_no_source_sentinels "$resources"
  validate_topology "$resources"
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
  manifest_hash="$product_root/generation-manifest.sha256"
  {
    printf 'generator=maven-archetype-plugin:3.4.1:create-from-project\n'
    printf 'sourceProject=%s\n' "${MF_SOURCE_PROJECT}"
    printf 'sourceGroupId=%s\n' "${MF_SOURCE_GROUP_ID}"
    printf 'sourceArtifactId=%s\n' "${MF_SOURCE_ARTIFACT_ID}"
    printf 'sourceVersion=%s\n' "${MF_SOURCE_VERSION}"
    printf 'sourcePackage=%s\n' "${MF_SOURCE_PACKAGE}"
    printf 'targetArtifactId=%s\n' "${MF_TARGET_ARTIFACT_ID}"
    printf 'expectedTopology=%s\n' "${MF_EXPECTED_TOPOLOGY}"
    printf '[resources]\n'
    hash_tree "$product_root/archetype-resources"
  } >"$manifest_hash"
  printf 'generated %s (%s files)\n' "$MF_TARGET_ARTIFACT_ID" \
    "$(find "$product_root/archetype-resources" -type f | wc -l | tr -d ' ')"
}

generate_all() {
  local manifest
  for manifest in "${MANIFESTS[@]}"; do
    generate_one "$manifest"
  done
}

validate_generated_set() {
  local manifest product_dir
  for manifest in "${MANIFESTS[@]}"; do
    parse_manifest "$manifest"
    product_dir="$STAGING_ROOT/$MF_TARGET_ARTIFACT_ID"
    [[ -d "$product_dir/archetype-resources" ]] || die "staged target missing: $MF_TARGET_ARTIFACT_ID"
    [[ -s "$product_dir/generation-manifest.sha256" ]] || die "generation manifest missing: $MF_TARGET_ARTIFACT_ID"
  done
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
  local manifest product
  [[ -d "$GENERATED_ROOT" ]] || die 'generated workspace is missing; run generate first'
  for manifest in "${MANIFESTS[@]}"; do
    parse_manifest "$manifest"
    product="$MF_TARGET_ARTIFACT_ID"
    diff -ruN "$STAGING_ROOT/$product/archetype-resources" "$GENERATED_ROOT/$product/archetype-resources" \
      >/dev/null || die "generated resources are not deterministic for ${product}"
    cmp -s "$STAGING_ROOT/$product/generation-manifest.sha256" "$GENERATED_ROOT/$product/generation-manifest.sha256" \
      || die "generated provenance differs for ${product}"
  done
}

run_pipeline() {
  local mode="$1"
  discover_manifests
  acquire_lock
  STAGING_ROOT="$(mktemp -d "${ARCHETYPES_ROOT}/.generated.staging.XXXXXX")"
  write_source_snapshot "$STAGING_ROOT/source.snapshot.before"
  generate_all
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
