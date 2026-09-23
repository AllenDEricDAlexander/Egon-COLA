#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd -P)"
readonly PROJECT_ROOT
readonly LINKER="${PROJECT_ROOT}/scripts/link-skills.sh"
readonly SOURCE_ROOT="${PROJECT_ROOT}/.agents/skills"
FIXTURE_ROOT="$(mktemp -d "${TMPDIR:-/tmp}/egon-link-skills-test.XXXXXX")"
readonly FIXTURE_ROOT

cleanup() {
  rm -rf -- "$FIXTURE_ROOT"
}

trap cleanup EXIT HUP INT TERM

die() {
  printf 'link-skills-test: %s\n' "$*" >&2
  exit 1
}

check() {
  # check <what it asserts> <got> <want>
  [[ "$2" == "$3" ]] && return 0
  printf 'link-skills-test: %s\n  got:  %s\n  want: %s\n' "$1" "$2" "$3" >&2
  exit 1
}

g() {
  git -c user.name=fixture -c user.email=fixture@egon.top -c commit.gpgsign=false "$@"
}

run_in() {
  # The linker resolves the target repository from the target path, never from
  # the caller's cwd; running it from the real repository keeps the case where
  # `git rev-parse --git-dir` answers relatively under test.
  local cwd="$1"
  shift
  set +e
  (cd "$cwd" && "$LINKER" "$@" >"$FIXTURE_ROOT/out" 2>&1)
  local rc=$?
  set -e
  printf '%s' "$rc"
}

run() {
  run_in "$PROJECT_ROOT" "$@"
}

logged() {
  grep -c -- "$1" "$FIXTURE_ROOT/out" || true
}

link_count() {
  find "$1" -mindepth 1 -maxdepth 1 -type l | wc -l | tr -d ' '
}

is_link() {
  [[ -L "$1" ]] && printf 'symlink\n' || printf 'plain\n'
}

resolve_link() {
  [[ -f "$1/SKILL.md" ]] && printf 'yes\n' || printf 'no\n'
}

declare -a SKILLS=()

seed_source_skills() {
  local skill_md parent
  [[ -d "$SOURCE_ROOT" ]] || die "skill source directory is missing: $SOURCE_ROOT"
  while IFS= read -r skill_md; do
    parent="${skill_md%/*}"
    SKILLS+=("${parent##*/}")
  done < <(find "$SOURCE_ROOT" -mindepth 2 -maxdepth 2 -name SKILL.md \
    -not -path '*/node_modules/*' -print | sort)
  ((${#SKILLS[@]} > 0)) || die "no skill directory with SKILL.md under $SOURCE_ROOT"
}

# A fresh project without git gets one link per skill and nothing else.
fixture_plain_dir() {
  local root="$FIXTURE_ROOT/plain" rc
  mkdir -p "$root"

  rc="$(run "$root")"
  check 'no-git target exits 0' "$rc" 0
  check 'no-git target links every skill' "$(link_count "$root/.agents/skills")" "${#SKILLS[@]}"
  check 'linked skill resolves' "$(resolve_link "$root/.agents/skills/${SKILLS[0]}")" 'yes'
  check 'only .agents was created' "$(ls -A "$root" | tr '\n' ' ')" '.agents '

  rc="$(run "$root")"
  check 'rerun exits 0' "$rc" 0
  check 'rerun adds no links' "$(link_count "$root/.agents/skills")" "${#SKILLS[@]}"
  check 'rerun reports every skill unchanged' "$(logged '^unchanged')" "${#SKILLS[@]}"
}

# A repository holding a committed copy of one skill: dry-run must not write,
# the real run must replace it with a link, exclude it, and stay idempotent.
fixture_committed_copy() {
  local name="${SKILLS[0]}"
  local root="$FIXTURE_ROOT/committed" rc tracked deleted
  mkdir -p "$root/.agents/skills"
  cp -R -- "$SOURCE_ROOT/$name" "$root/.agents/skills/$name"
  (cd "$root" && g init -q && g add .agents && g commit -qm 'fixture: skill copy')

  rc="$(run --dry-run "$root")"
  check 'dry-run exits 0' "$rc" 0
  check 'dry-run plans one replacement' "$(logged '^would-replace')" 1
  check 'dry-run plans the remaining links' "$(logged '^would-link')" "$((${#SKILLS[@]} - 1))"
  check 'dry-run leaves the copy alone' "$(is_link "$root/.agents/skills/$name")" 'plain'
  check 'dry-run writes no exclude entry' \
    "$(grep -cxF -- ".agents/skills/$name" "$root/.git/info/exclude" || true)" 0

  rc="$(run "$root")"
  check 'real run exits 0' "$rc" 0
  check 'committed copy became a link' "$(is_link "$root/.agents/skills/$name")" 'symlink'
  check 'replacement resolves to the real skill' "$(resolve_link "$root/.agents/skills/$name")" 'yes'
  check 'link path recorded in exclude once' \
    "$(grep -cxF -- ".agents/skills/$name" "$root/.git/info/exclude")" 1

  tracked="$(cd "$root" && g ls-files ".agents/skills/$name" | wc -l | tr -d ' ')"
  ((tracked > 0)) || die 'fixture copy is not tracked'
  deleted="$(cd "$root" && g status --porcelain | grep -c '^ D' || true)"
  check 'removed copy stays visible as deletions' "$deleted" "$tracked"
  check 'the links themselves leak no untracked files' \
    "$(cd "$root" && g status --porcelain | grep -c '^??' || true)" 0

  rc="$(run "$root")"
  check 'rerun still exits 0' "$rc" 0
  check 'rerun reports every skill unchanged' "$(logged '^unchanged')" "${#SKILLS[@]}"
  check 'rerun does not duplicate the exclude entry' \
    "$(grep -cxF -- ".agents/skills/$name" "$root/.git/info/exclude")" 1

  (cd "$root" && g add -A .agents && g commit -qm 'fixture: switch to linked skills')
  check 'committing the switch cleans the tree' \
    "$(cd "$root" && g status --porcelain | wc -l | tr -d ' ')" 0
}

# An untracked or dirty copy is work the script may not throw away silently.
fixture_untracked_copy() {
  local name="${SKILLS[0]}"
  local root="$FIXTURE_ROOT/untracked" rc
  mkdir -p "$root/.agents/skills/$name"
  printf '# stub\n' >"$root/.agents/skills/$name/SKILL.md"
  (cd "$root" && g init -q && g commit -qm 'fixture: empty' --allow-empty)

  rc="$(run "$root")"
  check 'untracked copy exits with skips' "$rc" 2
  check 'untracked copy is reported blocked' "$(logged '^blocked')" 1
  check 'untracked copy survives' "$([[ -d "$root/.agents/skills/$name" && ! -L "$root/.agents/skills/$name" ]] && printf 'dir\n' || printf 'gone\n')" 'dir'

  rc="$(run --force "$root")"
  check '--force exits 0' "$rc" 0
  check '--force replaces the untracked copy' "$(is_link "$root/.agents/skills/$name")" 'symlink'
}

fixture_dirty_copy() {
  local name="${SKILLS[0]}"
  local root="$FIXTURE_ROOT/dirty" rc
  mkdir -p "$root/.agents/skills/$name"
  cp -- "$SOURCE_ROOT/$name/SKILL.md" "$root/.agents/skills/$name/SKILL.md"
  (cd "$root" && g init -q && g add . && g commit -qm 'fixture: committed skill')
  printf 'local edit\n' >>"$root/.agents/skills/$name/SKILL.md"

  rc="$(run "$root")"
  check 'dirty copy exits with skips' "$rc" 2
  check 'dirty copy keeps its uncommitted edit' \
    "$(grep -c 'local edit' "$root/.agents/skills/$name/SKILL.md")" 1
}

# Pre-existing links: the right one is left alone, a stale one is repointed.
fixture_existing_links() {
  local name="${SKILLS[0]}"
  local root="$FIXTURE_ROOT/links" elsewhere rc
  mkdir -p "$root/.agents/skills"
  ln -s "$SOURCE_ROOT/$name" "$root/.agents/skills/$name"

  rc="$(run "$root")"
  check 'correct link exits 0' "$rc" 0
  check 'correct link reported unchanged' "$(logged '^unchanged')" 1
  check 'correct link is untouched' "$(readlink "$root/.agents/skills/$name")" "$SOURCE_ROOT/$name"

  mkdir -p "$FIXTURE_ROOT/elsewhere/$name"
  touch "$FIXTURE_ROOT/elsewhere/$name/SKILL.md"
  elsewhere="$FIXTURE_ROOT/elsewhere/$name"
  rm -- "$root/.agents/skills/$name"
  ln -s "$elsewhere" "$root/.agents/skills/$name"

  rc="$(run "$root")"
  check 'stale link exits 0' "$rc" 0
  check 'stale link is repointed' "$(readlink "$root/.agents/skills/$name")" "$SOURCE_ROOT/$name"
  check 'stale link reported relinked' "$(logged '^relinked')" 1
}

# Something that is not a skill occupies the path: only --force may remove it.
fixture_occupant() {
  local name="${SKILLS[0]}"
  local root="$FIXTURE_ROOT/occupant" rc
  mkdir -p "$root/.agents/skills"
  printf 'not-a-skill\n' >"$root/.agents/skills/$name"

  rc="$(run "$root")"
  check 'non-skill occupant exits with skips' "$rc" 2
  check 'non-skill occupant survives' "$(cat "$root/.agents/skills/$name")" 'not-a-skill'

  rc="$(run --force "$root")"
  check '--force replaces non-skill occupant' "$(is_link "$root/.agents/skills/$name")" 'symlink'
}

fixture_refusals() {
  local parent="$FIXTURE_ROOT/parent" rc
  mkdir -p "$parent/child"
  (cd "$parent" && g init -q && g commit -qm 'fixture: outer repo' --allow-empty)

  check 'relative path is refused' "$(run relative/path)" 1
  check 'missing target is refused' "$(run "$FIXTURE_ROOT/nope")" 1
  check 'self-link is refused' "$(run "$PROJECT_ROOT")" 1
  check 'self-link explains itself' "$(logged 'refusing to link it into itself')" 1
  check 'subdirectory of another repo is refused' "$(run "$parent/child")" 1
  check 'refusal names the borrowed git dir' "$(logged 'not the root of its own git repository')" 1
  check 'no argument is refused' "$(run)" 1
  check 'unknown option is refused' "$(run --nope "$FIXTURE_ROOT/plain")" 1

  mkdir -p "$FIXTURE_ROOT/fileblock/.agents"
  touch "$FIXTURE_ROOT/fileblock/.agents/skills"
  check 'skills path occupied by a file is refused' "$(run "$FIXTURE_ROOT/fileblock")" 1
}

fixture_cwd_independence() {
  local root="$FIXTURE_ROOT/foreign-cwd" rc
  mkdir -p "$root"
  (cd "$root" && g init -q && g commit -qm 'fixture: bare' --allow-empty)

  rc="$(run_in / "$root")"
  check 'runs from a directory outside any repo' "$rc" 0
  check 'links every skill from that cwd' "$(link_count "$root/.agents/skills")" "${#SKILLS[@]}"
  check 'exclude entries landed in the target, not the caller' \
    "$(grep -c '^\.agents/skills/' "$root/.git/info/exclude")" "${#SKILLS[@]}"
}

seed_source_skills
fixture_plain_dir
fixture_committed_copy
fixture_untracked_copy
fixture_dirty_copy
fixture_existing_links
fixture_occupant
fixture_refusals
fixture_cwd_independence

printf '%s\n' 'link skills fixture: PASS'
