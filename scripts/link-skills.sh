#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_NAME="${0##*/}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
readonly SCRIPT_DIR
readonly REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd -P)"
readonly SOURCE_ROOT="$REPO_ROOT/.agents/skills"

DRY_RUN=false
FORCE=false
PRUNE=false
declare -a TARGETS=()
declare -a SKILL_NAMES=()

usage() {
    printf '%s\n' \
        "Usage: $SCRIPT_NAME [--dry-run] [--force] [--prune] <absolute-project-dir>..." \
        "" \
        "Link every skill of $SOURCE_ROOT into other projects, one symlink" \
        "per skill at <project>/.agents/skills/<name>." \
        "" \
        "Options:" \
        "  --dry-run  Report what would happen; touch nothing." \
        "  --force    Also discard copies that are untracked or dirty." \
        "  --prune    Also remove links into this repository whose skill was" \
        "             renamed or deleted upstream, with their exclude entries." \
        "  -h, --help Show this help." \
        "" \
        "A same-name real directory is replaced by the symlink when the" \
        "target repository tracks it and reports it clean, because git can" \
        "restore it. Anything else is left alone unless --force is given." \
        "Every path this script links is added to that repository's" \
        ".git/info/exclude so git status stays clean." \
        "" \
        "The target must be the root of its own repository. A linked git" \
        "worktree, or a directory that only sits inside another repository," \
        "is refused rather than written into the wrong exclude file. Links" \
        "pointing somewhere other than this repository are never pruned." \
        "" \
        "Exit codes: 0 everything requested is linked, 2 finished with" \
        "skips, 1 usage or hard error."
}

die() {
    printf 'Error: %s\n' "$*" >&2
    exit 1
}

log() {
    printf '%-8s %s\n' "$1" "$2"
}

# Absolute .git directory of the repository governing $1, or nothing when the
# target is not inside one. `git -C` walks parents, so the caller must compare
# the result against the target before trusting it.
git_dir_of() {
    local target="$1" git_dir
    git_dir="$(git -C "$target" rev-parse --git-dir 2>/dev/null)" || return 0
    [[ "$git_dir" == /* ]] || git_dir="$target/$git_dir"
    (cd "$git_dir" && pwd -P)
}

# A path listed here is untracked-on-purpose: git still shows no conflict,
# and a later `git add .agents/skills` in the target records the switch.
mark_excluded() {
    local exclude="$1" entry="$2"
    [[ -n "$exclude" ]] || return 0
    [[ -f "$exclude" ]] || : >"$exclude"
    grep -qxF "$entry" "$exclude" 2>/dev/null && return 0
    printf '%s\n' "$entry" >>"$exclude"
}

# The inverse of mark_excluded, for links that no longer exist.
unmark_excluded() {
    local exclude="$1" entry="$2" tmp
    [[ -n "$exclude" && -f "$exclude" ]] || return 0
    grep -qxF "$entry" "$exclude" 2>/dev/null || return 0
    tmp="$exclude.$$.tmp"
    grep -vxF "$entry" "$exclude" >"$tmp" || true
    mv -f -- "$tmp" "$exclude" || die "failed to rewrite $exclude"
}

is_skill_name() {
    local want="$1" name
    for name in "${SKILL_NAMES[@]}"; do
        [[ "$name" == "$want" ]] && return 0
    done
    return 1
}

# Decide whether deleting an existing real copy is reversible:
#   replace - tracked and clean, `git restore` brings it back
#   force   - untracked, dirty, or not a skill directory at all
classify() {
    local target="$1" rel="$2" tracked modified
    tracked="$(git -C "$target" ls-files -- "$rel" 2>/dev/null)" || tracked=''
    [[ -n "$tracked" ]] || {
        printf 'force\n'
        return 0
    }
    modified="$(git -C "$target" status --porcelain --no-renames -- "$rel" 2>/dev/null)" || modified=''
    [[ -n "$modified" ]] || {
        printf 'replace\n'
        return 0
    }
    printf 'force\n'
}

link_one() {
    local target="$1" exclude="$2" name="$3" src dest rel state action

    src="$SOURCE_ROOT/$name"
    dest="$target/.agents/skills/$name"
    rel=".agents/skills/$name"

    if [[ -L "$dest" ]]; then
        if [[ "$(readlink "$dest")" == "$src" ]]; then
            log unchanged "$rel"
            return 0
        fi
        if $DRY_RUN; then
            log would-relink "$rel -> $src"
        else
            ln -nfs "$src" "$dest" || die "failed to relink $dest"
            mark_excluded "$exclude" "$rel"
            log relinked "$rel -> $src"
        fi
        LINKED=$((LINKED + 1))
        return 0
    fi

    action='link'
    if [[ -e "$dest" ]]; then
        if [[ -d "$dest" && -f "$dest/SKILL.md" ]]; then
            state="$(classify "$target" "$rel")"
        else
            state='force'
        fi
        if [[ "$state" == 'force' ]] && ! $FORCE; then
            log blocked "$rel (untracked, dirty or not a skill; --force to discard)"
            SKIPPED=$((SKIPPED + 1))
            return 0
        fi
        action='replace'
    fi

    if $DRY_RUN; then
        if [[ "$action" == 'replace' ]]; then
            log would-replace "$rel (real copy -> symlink)"
        else
            log would-link "$rel -> $src"
        fi
        LINKED=$((LINKED + 1))
        return 0
    fi

    if [[ "$action" == 'replace' ]]; then
        rm -rf "$dest" || die "failed to remove $dest"
        REPLACED=$((REPLACED + 1))
    fi
    ln -s "$src" "$dest" || die "failed to link $dest"
    mark_excluded "$exclude" "$rel"
    if [[ "$action" == 'replace' ]]; then
        log replaced "$rel (real copy -> symlink)"
    else
        log linked "$rel -> $src"
    fi
    LINKED=$((LINKED + 1))
}

# Skills this repository renamed or dropped leave dead links behind in every
# project that consumed them, which discovery reports as a missing skill.
# Only links whose recorded target is inside SOURCE_ROOT are candidates.
prune_stale_links() {
    local target="$1" exclude="$2" link name dest rel

    [[ -d "$target/.agents/skills" ]] || return 0
    while IFS= read -r link; do
        [[ -n "$link" ]] || continue
        dest="$(readlink "$link")"
        [[ "$dest" == "$SOURCE_ROOT"/* ]] || continue
        name="${link##*/}"
        is_skill_name "$name" && continue

        rel=".agents/skills/$name"
        if $DRY_RUN; then
            log would-prune "$rel -> $dest"
        else
            rm -- "$link" || die "failed to remove $link"
            unmark_excluded "$exclude" "$rel"
            log pruned "$rel -> $dest"
        fi
        PRUNED=$((PRUNED + 1))
    done < <(find "$target/.agents/skills" -mindepth 1 -maxdepth 1 -type l | sort)
}

link_target() {
    local target="$1" git_dir exclude name

    git_dir="$(git_dir_of "$target")"
    if [[ -z "$git_dir" ]]; then
        exclude=''
        log notice "$target has no git repository; links stay unversioned"
    elif [[ "$git_dir" == "$target/.git" ]]; then
        exclude="$git_dir/info/exclude"
    else
        die "$target is not the root of its own git repository ($git_dir); refusing to write another repository's exclude file"
    fi

    if [[ ! -d "$target/.agents/skills" ]]; then
        if $DRY_RUN; then
            log would-create "$target/.agents/skills"
        else
            mkdir -p "$target/.agents/skills" || die "failed to create $target/.agents/skills"
        fi
    fi

    for name in "${SKILL_NAMES[@]}"; do
        link_one "$target" "$exclude" "$name"
    done
    if $PRUNE; then
        prune_stale_links "$target" "$exclude"
    fi
}

while (($#)); do
    case "$1" in
        --dry-run) DRY_RUN=true ;;
        --force) FORCE=true ;;
        --prune) PRUNE=true ;;
        -h | --help)
            usage
            exit 0
            ;;
        -*) die "unknown option $1 (see --help)" ;;
        *) TARGETS+=("$1") ;;
    esac
    shift
done

((${#TARGETS[@]} > 0)) || {
    usage >&2
    exit 1
}

[[ -d "$SOURCE_ROOT" ]] || die "skill source directory is missing: $SOURCE_ROOT"

while IFS= read -r skill_md; do
    parent="${skill_md%/*}"
    SKILL_NAMES+=("${parent##*/}")
done < <(
    find "$SOURCE_ROOT" -mindepth 2 -maxdepth 2 -name SKILL.md \
        -not -path '*/node_modules/*' -print | sort
)
((${#SKILL_NAMES[@]} > 0)) || die "no skill directory with SKILL.md under $SOURCE_ROOT"

LINKED=0
SKIPPED=0
REPLACED=0
PRUNED=0

for raw in "${TARGETS[@]}"; do
    case "$raw" in
        /*) ;;
        *) die "target must be an absolute path, got: $raw" ;;
    esac
    [[ -d "$raw" ]] || die "target directory is missing: $raw"

    target="$(cd "$raw" && pwd -P)"
    [[ "$target" != "$REPO_ROOT" ]] ||
        die "$target is the skill source repository; refusing to link it into itself"

    if [[ -e "$target/.agents" && ! -d "$target/.agents" ]]; then
        die "$target/.agents exists but is not a directory"
    fi
    if [[ -e "$target/.agents/skills" && ! -d "$target/.agents/skills" ]]; then
        die "$target/.agents/skills exists but is not a directory"
    fi

    printf '\n== %s ==\n' "$target"
    link_target "$target"
done

printf '\n'
if $DRY_RUN; then
    printf 'dry-run: %d link(s), %d prune(s), %d skip(s) would result\n' \
        "$LINKED" "$PRUNED" "$SKIPPED"
else
    printf '%d link(s) in place (%d replaced a real copy), %d pruned, %d skip(s)\n' \
        "$LINKED" "$REPLACED" "$PRUNED" "$SKIPPED"
    if ((REPLACED > 0)); then
        printf 'note: each replaced copy still shows as a deletion until committed;\n'
        printf '      the symlinks themselves are hidden by .git/info/exclude\n'
    fi
fi

((SKIPPED == 0)) || exit 2
exit 0
