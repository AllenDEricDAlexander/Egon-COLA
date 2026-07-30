#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
readonly SCRIPT_DIR
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd -P)"
readonly PROJECT_ROOT
readonly MAVEN_WRAPPER="$PROJECT_ROOT/mvnw"
readonly VERSIONS_PLUGIN="org.codehaus.mojo:versions-maven-plugin:2.16.2:set"

BACKUP_DIR=''
ROLLBACK_REQUIRED=false

usage() {
    printf 'Usage: %s <new-version>\n' "${0##*/}"
    printf 'Example: %s 5.3.0-SNAPSHOT\n' "${0##*/}"
}

die() {
    printf 'Error: %s\n' "$*" >&2
    exit 1
}

read_project_version() {
    "$MAVEN_WRAPPER" \
        -q \
        -N \
        -f "$PROJECT_ROOT/pom.xml" \
        help:evaluate \
        -Dexpression=project.version \
        -DforceStdout \
        -Dstyle.color=never
}

escape_sed_pattern() {
    printf '%s\n' "$1" | sed 's/[][\\.^$*|]/\\&/g'
}

find_project_poms() {
    find "$PROJECT_ROOT" \
        -type d \( -name .git -o -name .worktrees -o -name target \) -prune -o \
        -type f -name pom.xml -print0
}

find_archetype_template_poms() {
    local archetypes_dir="$PROJECT_ROOT/egon-cola-archetypes"

    [[ -d "$archetypes_dir" ]] || return 0

    find "$archetypes_dir" \
        -type d -name target -prune -o \
        -type f -path '*/src/main/resources/archetype-resources/*' -name pom.xml -print0
}

verify_archetype_pom_versions() {
    local expected_version="$1"
    local pom_file
    local version_tag
    local actual_version

    while IFS= read -r -d '' pom_file; do
        while IFS= read -r version_tag; do
            [[ -n "$version_tag" ]] || continue
            actual_version="${version_tag#<egon-cola.version>}"
            actual_version="${actual_version%</egon-cola.version>}"
            [[ "$actual_version" == "$expected_version" ]] || \
                die "$pom_file uses egon-cola.version $actual_version; expected $expected_version"
        done < <(grep -Eo '<egon-cola\.version>[^<]*</egon-cola\.version>' "$pom_file" || true)
    done < <(find_archetype_template_poms)
}

backup_versioned_files() {
    local source_file
    local relative_path
    local backup_file

    BACKUP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/egon-cola-version-backup.XXXXXX")"

    while IFS= read -r -d '' source_file; do
        relative_path="${source_file#"$PROJECT_ROOT"/}"
        backup_file="$BACKUP_DIR/original/$relative_path"
        mkdir -p "$(dirname "$backup_file")"
        cp -p "$source_file" "$backup_file"
    done < <(
        find_project_poms
        find_readme_files
        find_ddc_readme_files
    )

    ROLLBACK_REQUIRED=true
}

restore_versioned_files() {
    local backup_file
    local relative_path
    local restore_failed=false

    while IFS= read -r -d '' backup_file; do
        relative_path="${backup_file#"$BACKUP_DIR/original"/}"
        if ! cp -p "$backup_file" "$PROJECT_ROOT/$relative_path"; then
            printf 'Error: failed to restore %s.\n' "$relative_path" >&2
            restore_failed=true
        fi
    done < <(find "$BACKUP_DIR/original" -type f -print0)

    [[ "$restore_failed" == false ]]
}

cleanup_on_exit() {
    local status=$?
    local preserve_backup=false

    trap - EXIT HUP INT TERM

    if [[ "$ROLLBACK_REQUIRED" == true ]]; then
        if restore_versioned_files; then
            printf 'Restored version-managed files after the version update failed.\n' >&2
        else
            printf 'Error: failed to restore one or more version-managed files. Original backup preserved at %s.\n' \
                "$BACKUP_DIR" >&2
            status=1
            preserve_backup=true
        fi
    fi

    if [[ "$preserve_backup" == false && -n "$BACKUP_DIR" && -d "$BACKUP_DIR" ]]; then
        rm -rf "$BACKUP_DIR"
    fi
    exit "$status"
}

update_archetype_pom_versions() {
    local current_version="$1"
    local new_version="$2"
    local current_tag="<egon-cola.version>$current_version</egon-cola.version>"
    local new_tag="<egon-cola.version>$new_version</egon-cola.version>"
    local escaped_current_tag
    local pom_file
    local temp_file
    local updated_count=0

    escaped_current_tag="$(escape_sed_pattern "$current_tag")"

    while IFS= read -r -d '' pom_file; do
        if ! grep -Fq -- "$current_tag" "$pom_file"; then
            continue
        fi

        temp_file="$BACKUP_DIR/updated/${pom_file#"$PROJECT_ROOT"/}"
        mkdir -p "$(dirname "$temp_file")"
        sed "s|$escaped_current_tag|$new_tag|g" "$pom_file" > "$temp_file"
        cp "$temp_file" "$pom_file"
        updated_count=$((updated_count + 1))
    done < <(find_archetype_template_poms)

    printf 'Updated %d archetype template POM(s).\n' "$updated_count"
}

find_readme_files() {
    local readme

    for readme in "$PROJECT_ROOT/README.md" "$PROJECT_ROOT/README.zh-CN.md"; do
        [[ -f "$readme" ]] && printf '%s\0' "$readme"
    done
}

find_ddc_readme_files() {
    local ddc_dir="$PROJECT_ROOT/egon-cola-platforms/egon-cola-platform-dynamic-config-center"
    local readme

    for readme in "$ddc_dir/README.md" "$ddc_dir/README.zh-CN.md"; do
        [[ -f "$readme" ]] && printf '%s\0' "$readme"
    done
}

update_readme_archetype_versions() {
    local new_version="$1"
    local readme_file
    local temp_file
    local updated_count=0

    while IFS= read -r -d '' readme_file; do
        if ! grep -Eq -- "-DarchetypeVersion='[^']*'" "$readme_file"; then
            continue
        fi

        temp_file="$BACKUP_DIR/updated/${readme_file#"$PROJECT_ROOT"/}"
        mkdir -p "$(dirname "$temp_file")"
        sed -E "s|-DarchetypeVersion='[^']*'|-DarchetypeVersion='$new_version'|g" \
            "$readme_file" > "$temp_file"
        cp "$temp_file" "$readme_file"
        updated_count=$((updated_count + 1))
    done < <(find_readme_files)

    printf 'Updated %d README file(s).\n' "$updated_count"
}

verify_readme_archetype_versions() {
    local expected_version="$1"
    local readme_file
    local stale

    while IFS= read -r -d '' readme_file; do
        stale="$(grep -Eo -- "-DarchetypeVersion='[^']*'" "$readme_file" \
            | grep -Fv -- "-DarchetypeVersion='$expected_version'" || true)"
        [[ -z "$stale" ]] || \
            die "$readme_file still documents $stale; expected $expected_version"
    done < <(find_readme_files)
}

update_ddc_readme_versions() {
    local current_version="$1"
    local new_version="$2"
    local current_tag="<version>$current_version</version>"
    local new_tag="<version>$new_version</version>"
    local escaped_current_tag
    local readme_file
    local temp_file
    local updated_count=0

    escaped_current_tag="$(escape_sed_pattern "$current_tag")"

    while IFS= read -r -d '' readme_file; do
        if ! grep -Fq -- "$current_tag" "$readme_file"; then
            continue
        fi

        temp_file="$BACKUP_DIR/updated/${readme_file#"$PROJECT_ROOT"/}"
        mkdir -p "$(dirname "$temp_file")"
        sed "s|$escaped_current_tag|$new_tag|g" "$readme_file" > "$temp_file"
        cp "$temp_file" "$readme_file"
        updated_count=$((updated_count + 1))
    done < <(find_ddc_readme_files)

    printf 'Updated %d DDC README file(s).\n' "$updated_count"
}

verify_ddc_readme_versions() {
    local expected_version="$1"
    local expected_tag="<version>$expected_version</version>"
    local readme_file

    while IFS= read -r -d '' readme_file; do
        grep -Fq -- 'egon-cola-platform-dynamic-config-center-starter' "$readme_file" || \
            die "$readme_file does not document the DDC platform Starter"
        grep -Fq -- "$expected_tag" "$readme_file" || \
            die "$readme_file does not document DDC version $expected_version"
    done < <(find_ddc_readme_files)
}

if [[ $# -ne 1 ]]; then
    usage >&2
    exit 2
fi

readonly NEW_VERSION="$1"
[[ "$NEW_VERSION" =~ ^[0-9A-Za-z][0-9A-Za-z._+-]*$ ]] || \
    die "invalid Maven version: $NEW_VERSION"

[[ -f "$PROJECT_ROOT/pom.xml" ]] || die "project root POM not found: $PROJECT_ROOT/pom.xml"
[[ -x "$MAVEN_WRAPPER" ]] || die "Maven wrapper is not executable: $MAVEN_WRAPPER"

CURRENT_VERSION="$(read_project_version)"
readonly CURRENT_VERSION
[[ -n "$CURRENT_VERSION" && "$CURRENT_VERSION" != *$'\n'* ]] || \
    die 'could not determine the current project version'

verify_archetype_pom_versions "$CURRENT_VERSION"
verify_ddc_readme_versions "$CURRENT_VERSION"

if [[ "$CURRENT_VERSION" == "$NEW_VERSION" ]]; then
    printf 'Egon-COLA is already at version %s. No changes made.\n' "$NEW_VERSION"
    exit 0
fi

trap cleanup_on_exit EXIT
trap 'exit 129' HUP
trap 'exit 130' INT
trap 'exit 143' TERM
backup_versioned_files

printf 'Updating Egon-COLA from %s to %s...\n' "$CURRENT_VERSION" "$NEW_VERSION"

"$MAVEN_WRAPPER" \
    -B \
    -ntp \
    -f "$PROJECT_ROOT/pom.xml" \
    "$VERSIONS_PLUGIN" \
    -DgenerateBackupPoms=false \
    -DprocessAllModules=true \
    -DnewVersion="$NEW_VERSION"

update_archetype_pom_versions "$CURRENT_VERSION" "$NEW_VERSION"
verify_archetype_pom_versions "$NEW_VERSION"
update_readme_archetype_versions "$NEW_VERSION"
verify_readme_archetype_versions "$NEW_VERSION"
update_ddc_readme_versions "$CURRENT_VERSION" "$NEW_VERSION"
verify_ddc_readme_versions "$NEW_VERSION"

UPDATED_VERSION="$(read_project_version)"
readonly UPDATED_VERSION
[[ "$UPDATED_VERSION" == "$NEW_VERSION" ]] || \
    die "root POM version is $UPDATED_VERSION after update; expected $NEW_VERSION"

"$MAVEN_WRAPPER" -B -ntp -f "$PROJECT_ROOT/pom.xml" -DskipTests validate

ROLLBACK_REQUIRED=false
rm -rf "$BACKUP_DIR"
BACKUP_DIR=''
trap - EXIT HUP INT TERM

printf 'Egon-COLA POM versions updated to %s.\n' "$NEW_VERSION"
