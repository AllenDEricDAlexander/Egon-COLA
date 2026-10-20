#!/usr/bin/env bash

set -Eeuo pipefail
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
repo_root="$(cd "$script_dir/../.." && pwd -P)"

python3 - "$repo_root" <<'PYTEST'
import copy, datetime, hashlib, json, os, shlex, shutil, subprocess, sys, tempfile, time, uuid
from pathlib import Path

source = Path(sys.argv[1])
with tempfile.TemporaryDirectory(prefix="xingyuan-isolated-contract-") as directory:
    root = Path(directory).resolve() / "repo"
    scripts = root / "scripts/unified-xingyuan"
    (scripts / "lib").mkdir(parents=True)
    shutil.copy2(source / "scripts/unified-xingyuan/isolated-integration.sh", scripts)
    shutil.copy2(source / "scripts/unified-xingyuan/lib/isolated-integration.sh", scripts / "lib")
    shutil.copy2(source / "scripts/unified-xingyuan/lib/isolated_integration.py", scripts / "lib")
    run_id = str(uuid.uuid4())
    task = root / ("target/xingyuan-integration-" + run_id)
    (task / "secrets").mkdir(parents=True, mode=0o700)
    (task / "evidence").mkdir(mode=0o700)
    task.chmod(0o700)
    names = {role: "xingyuan_it_" + role + "_20261011" for role in
             ("idp", "rbac", "tianshu", "yuheng", "llm", "light", "web", "service", "agent")}
    manifest = {
        "schemaVersion": 1, "runId": run_id, "repoRoot": str(root), "runtimeRoot": str(task),
        "publicOrigin": "https://127.0.0.1:28443",
        "postgres": {"endpoint": "127.0.0.1:5432", "maintenanceDatabase": "postgres",
                     "credentialFile": str(task / "secrets/postgres.json")},
        "redis": {"endpoint": "redis://127.0.0.1:6379", "database": 15,
                  "credentialFile": str(task / "secrets/redis.json")},
        "databases": [{"role": role, "name": name} for role, name in names.items()],
        "platformStoragePartitionId": "900000000000000001", "tenantCodes": ["default", "tenant-b"],
        "userRecipe": "local-two-tenant-users-v1", "authorizationRecipe": "local-explicit-role-fields-v1",
        "processes": [], "artifacts": {}, "secretFiles": {},
        "deadlines": {"startupSeconds": 300, "operationSeconds": 900, "httpSeconds": 30},
        "catalogFiles": [], "policyFiles": []}
    fake_bin = root / "fake-bin"
    fake_bin.mkdir()
    calls = root / "foreign-writer-calls"
    for name in ("psql", "createdb", "redis-cli", "nginx", "java", "node", "kill", "openssl", "lsof", "npm"):
        fake = fake_bin / name
        fake.write_text("#!/usr/bin/env bash\nprintf '%s\\n' '" + name + "' >> \"$XINGYUAN_FAKE_CALLS\"\nexit 95\n")
        fake.chmod(0o755)
    wrapper = root / "mvnw"
    wrapper.write_text("#!/usr/bin/env bash\nprintf '%s\\n' mvnw >> \"$XINGYUAN_FAKE_CALLS\"\nexit 95\n")
    wrapper.chmod(0o755)
    env = dict(os.environ, PATH=str(fake_bin) + os.pathsep + os.environ["PATH"],
               XINGYUAN_ISOLATED_INTEGRATION="true", XINGYUAN_FAKE_CALLS=str(calls))
    command = ["bash", str(scripts / "isolated-integration.sh")]
    failures = []
    def invoke(label, expected, content=None, extra=(), operation="prepare", live=True, mode=0o600, result_file=None):
        file = task / "intent.json"
        file.unlink(missing_ok=True)
        file.write_text(json.dumps(manifest if content is None else content))
        file.chmod(mode)
        current = dict(env)
        if not live: current.pop("XINGYUAN_ISOLATED_INTEGRATION", None)
        args = [operation, "--manifest", str(file), "--result", str(task / "evidence/result.json" if result_file is None else result_file), *extra]
        result = subprocess.run(command + args, env=current, cwd=root, capture_output=True, text=True, timeout=15)
        if result.returncode != expected:
            failures.append(label + ": expected exit " + str(expected) + ", got " + str(result.returncode))
        assert "fixture-private-value" not in result.stdout + result.stderr
    private_module = [sys.executable, str(scripts / "lib/isolated_integration.py")]
    for mode, arguments, expected_exit, opted_in in (
            ("unknown-mode", [], 2, True),
            ("prepare", [str(task / "intent.json"), str(task / "evidence/direct.json"), "false"], 3, False),
            ("validate", [str(task / "intent.json"), str(root / "foreign-repository"), str(task / "evidence/direct.json"), "", ""], 3, True)):
        module_env = dict(env)
        if not opted_in:
            module_env.pop("XINGYUAN_ISOLATED_INTEGRATION", None)
        direct = subprocess.run(private_module + [mode, *arguments], env=module_env, cwd=root, capture_output=True, text=True, timeout=15)
        if direct.returncode != expected_exit:
            failures.append("private module entry bypassed its mode/repository/opt-in guard")
    assert not calls.exists()
    invoke("unknown operation", 2, operation="drop-db")
    invoke("unknown flag", 2, extra=("--force",))
    invoke("duplicate flag", 2, extra=("--manifest", str(task / "intent.json")))
    invoke("missing opt-in", 3, live=False)
    commands = {
        "prepare": (), "provision-identity": (), "start-control-plane": (),
        "provision-authorization": ("--phase", "platform"),
        "start-consumers": ("--token-index", str(task / "secrets/token-index.json")),
        "authorize-cli": ("--tenant-code", "default", "--resource-key", "tianshu", "--user-key", "admin",
                          "--output-file", str(task / "secrets/token.json")),
        "verify": ("--case-set", "full", "--token-index", str(task / "secrets/token-index.json")),
        "status": (), "stop": ()}
    for operation, extra in commands.items():
        invoke("compiled command " + operation, 4, extra=extra, operation=operation,
               result_file=task / ("evidence/command-" + operation + ".json"))
    default_environment=dict(env)
    default_environment.pop("XINGYUAN_ISOLATED_INTEGRATION",None)
    disabled_bridge=subprocess.run([sys.executable,"-I",str(scripts/"lib/isolated_integration.py"),"formal-revoke",
                                   str(task/"missing-manifest.json"),str(task/"evidence/formal-revocation.json"),"a"*64,str(root)],
                                  env=default_environment,capture_output=True,text=True,timeout=5)
    if disabled_bridge.returncode!=3 or calls.exists():
        failures.append("private formal revocation reached a facility without explicit opt-in")
    invoke("native phase requires token index", 2, extra=("--phase", "native"), operation="provision-authorization")
    invoke("unknown phase", 2, extra=("--phase", "grant-all"), operation="provision-authorization")
    invoke("verify requires full case set", 2, extra=("--case-set", "skip-security"), operation="verify")
    invoke("status rejects resume", 2, extra=("--resume",), operation="status")
    invoke("wrong command rejects selector", 2, extra=("--user-key", "admin"), operation="prepare")
    foreign = copy.deepcopy(manifest); foreign["databases"][0]["name"] = "egon_cola"
    invoke("foreign database", 3, foreign)
    duplicate = copy.deepcopy(manifest); duplicate["databases"][1] = duplicate["databases"][0]
    invoke("duplicate role", 3, duplicate)
    endpoint = copy.deepcopy(manifest); endpoint["postgres"]["endpoint"] = "192.0.2.1:5432"
    invoke("foreign endpoint", 3, endpoint)
    escaping = copy.deepcopy(manifest); escaping["runtimeRoot"] = str(root / "target/../old-runtime")
    invoke("escaping runtime root", 3, escaping)
    unknown = copy.deepcopy(manifest); unknown["rawToken"] = "fixture-private-value"
    invoke("unknown JSON field", 2, unknown)
    wrong = copy.deepcopy(manifest); wrong["schemaVersion"] = True
    invoke("boolean schema coercion", 2, wrong)
    invoke("world-readable manifest", 3, mode=0o644)
    process = {"id": "shoubing", "artifactRole": "shoubing", "kind": "JAVA", "port": 28120, "additionalPorts": [28122]}
    wrong_port = copy.deepcopy(manifest); wrong_port["processes"] = [dict(process, port=18120)]
    invoke("retired process port", 3, wrong_port)
    wrong_kind = copy.deepcopy(manifest); wrong_kind["processes"] = [dict(process, kind="NODE")]
    invoke("wrong process kind", 3, wrong_kind)
    wrong_role = copy.deepcopy(manifest); wrong_role["processes"] = [dict(process, artifactRole="foreign-jar")]
    invoke("foreign process artifact role", 3, wrong_role)
    unknown_process = copy.deepcopy(manifest); unknown_process["processes"] = [dict(process, id="foreign-process")]
    invoke("unknown process id", 3, unknown_process)
    duplicate_process = copy.deepcopy(manifest); duplicate_process["processes"] = [process, process]
    invoke("duplicate process id", 3, duplicate_process)
    extra_process = copy.deepcopy(manifest); extra_process["processes"] = [dict(process, exec="untrusted command")]
    invoke("untrusted exec field", 2, extra_process)
    invoke("escaping result", 3, result_file=root / "foreign-result.json")
    invoke("escaping token index", 3, operation="start-consumers", extra=("--token-index", str(root / "foreign-token-index.json")))
    invoke("escaping token output", 3, operation="authorize-cli",
           extra=("--tenant-code", "default", "--resource-key", "tianshu", "--user-key", "admin",
                  "--output-file", str(root / "foreign-token.json")))
    assert not calls.exists(), "Rejected inputs invoked a foreign writer"
    # This recorder represents the declared owner command, never an external JVM/database.
    owner_manifest = copy.deepcopy(manifest)
    policy_directory = task / "policies"
    policy_directory.mkdir(mode=0o700)
    policy = policy_directory / "identity-provisioning.json"
    policy.write_text("{}"); policy.chmod(0o600)
    owner_manifest["policyFiles"] = [{"path": str(policy), "sha256": hashlib.sha256(policy.read_bytes()).hexdigest()}]
    artifact = root / "built-owner.jar"
    artifact.write_bytes(b"controlled-built-owner-artifact")
    owner_manifest["artifacts"] = {"shoubing": {"path": str(artifact), "sha256": hashlib.sha256(artifact.read_bytes()).hexdigest(), "buildId": "controlled-build"}}
    for role, value in (("oauth-kek", b"k" * 32), ("admin-password", b"controlled-password-123456"), ("viewer-password", b"controlled-password-234567"), ("provisioner-password", b"controlled-password-345678")):
        file = task / "secrets" / (role + ".txt")
        file.write_bytes(value); file.chmod(0o600)
        owner_manifest["secretFiles"][role] = str(file)
    intent_file = task / "intent.json"
    intent_file.write_text(json.dumps(owner_manifest)); intent_file.chmod(0o600)
    digest = hashlib.sha256(intent_file.read_bytes()).hexdigest()
    def private_json(file, value):
        file.write_text(json.dumps(value)); file.chmod(0o600)
    private_json(task / "evidence/db-idp.json", {"runId": run_id, "manifestSha256": digest, "role": "idp",
                 "databaseName": names["idp"], "oid": 42, "ownerComment": "xingyuan-it:" + run_id + ":" + digest,
                 "databaseOwner": "controlled-owner"})
    private_json(task / "evidence/prepared.json", {"schemaVersion": 1, "operationId": "CLI-001", "runId": run_id,
                 "manifestSha256": digest, "status": "COMMITTED", "counts": {"databaseCount": 9}})
    wrapper.write_text('''#!/usr/bin/env python3
import hashlib, json, os, sys
from pathlib import Path
options = {value.split('=', 1)[0]: value.split('=', 1)[1] for value in sys.argv if value.startswith('-D') and '=' in value}
assert options['-Dtest'] == 'IsolatedOAuthProvisioningIT#provisionIdentity'
assert options['-Dxingyuan.isolated.resume'] == 'false'
assert os.environ['XINGYUAN_ISOLATED_INTEGRATION'] == 'true'
assert not {'JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', 'MAVEN_OPTS'} & os.environ.keys()
Path(os.environ['XINGYUAN_FAKE_CALLS']).write_text('owner-command-once\\n')
manifest = Path(options['-Dxingyuan.isolated.manifest-file'])
intent = json.loads(manifest.read_text())
root = Path(intent['runtimeRoot'])
digest = hashlib.sha256(manifest.read_bytes()).hexdigest()
if os.environ.get('XINGYUAN_FAKE_OUTCOME') == 'unknown':
    print('XINGYUAN_COMMIT_UNKNOWN')
    sys.exit(1)
result = {'schemaVersion': 1, 'operationId': 'CLI-002', 'runId': intent['runId'], 'manifestSha256': digest,
          'status': 'COMMITTED', 'counts': {'tenantCount': 2}, 'error': None,
          'steps': [{'stepId': stage, 'state': 'COMMITTED'} for stage in
                    ('IDENTITY', 'REALMS', 'SECURITY', 'MANAGEMENT_CLIENTS', 'RESOURCES', 'CLIENTS', 'GRANTS')]}
output = Path(options['-Dxingyuan.isolated.result-file'])
output.write_text(json.dumps(result)); output.chmod(0o600)
registry = root / 'secrets/runtime-registry.json'
registry.write_text(json.dumps({'runId': intent['runId'], 'manifestSha256': digest, 'subjects': {'admin': '101', 'viewer': '102'}, 'provisioningSubject': '103'})); registry.chmod(0o600)
''')
    wrapper.chmod(0o755)
    owner_result = task / "evidence/identity-result.json"
    owner_args = command + ["provision-identity", "--manifest", str(intent_file), "--result", str(owner_result)]
    response = subprocess.run(owner_args, env=env, cwd=root, capture_output=True, text=True, timeout=15)
    if response.returncode != 0 or response.stdout.strip() != "CLI-002 COMMITTED evidence/identity-result.json":
        failures.append("owner command did not publish the verified completion contract")
    canonical_identity = task / "evidence/identity.json"
    if not canonical_identity.exists() or canonical_identity.read_bytes() != owner_result.read_bytes():
        failures.append("identity command did not preserve its verified canonical phase receipt")
    assert calls.read_text() == "owner-command-once\n"
    assert "controlled-password" not in response.stdout + response.stderr
    calls.unlink()
    owner_result.unlink()
    canonical_identity.unlink()
    (task / "evidence/provision-identity-owner.log").unlink()
    provisioner_password = Path(owner_manifest["secretFiles"]["provisioner-password"])
    provisioner_password.write_bytes(b"short")
    response = subprocess.run(owner_args, env=env, cwd=root, capture_output=True, text=True, timeout=15)
    if response.returncode != 2 or calls.exists():
        failures.append("invalid provisioner credential reached the identity owner")
    for output in (calls,owner_result,canonical_identity,task / "evidence/provision-identity-owner.log"):
        output.unlink(missing_ok=True)
    provisioner_password.write_bytes(b"controlled-password-345678")
    provisioner_password.chmod(0o600)
    unknown_env = dict(env, XINGYUAN_FAKE_OUTCOME="unknown")
    response = subprocess.run(owner_args, env=unknown_env, cwd=root, capture_output=True, text=True, timeout=15)
    if response.returncode != 9:
        failures.append("unknown owner commit did not freeze with exit9")
    assert calls.read_text() == "owner-command-once\n"
    assert "XINGYUAN_COMMIT_UNKNOWN" not in response.stdout
    calls.unlink()
    (task / "evidence/provision-identity-owner.log").unlink()
    wrong_receipt = json.loads((task / "evidence/db-idp.json").read_text())
    wrong_receipt["ownerComment"] = "foreign-owner"
    private_json(task / "evidence/db-idp.json", wrong_receipt)
    response = subprocess.run(owner_args, env=env, cwd=root, capture_output=True, text=True, timeout=15)
    if response.returncode != 3 or calls.exists():
        failures.append("foreign ownership reached the owner command")
    # Preparation must reject an existing database before generating keys or acquiring Redis ownership.
    collision = copy.deepcopy(owner_manifest)
    collision["processes"] = [{"id": "shoubing", "artifactRole": "shoubing", "kind": "JAVA", "port": 28120, "additionalPorts": [28122]}]
    for role in ("oauth-kek", "audit-hmac", "edge-session", "registry-hmac", "admin-password", "viewer-password", "provisioner-password", "tls-ca-key", "tls-ca-cert",
                 "tls-server-key", "tls-server-cert", "tls-keystore", "tls-keystore-password"):
        collision["secretFiles"][role] = str(task / "secrets" / (role + ".private"))
    postgres_password = task / "secrets/postgres.json"
    postgres_password.write_text("fixture-private-value"); postgres_password.chmod(0o600)
    redis_password = task / "secrets/redis.json"
    redis_password.write_text("fixture-private-value"); redis_password.chmod(0o600)
    psql = fake_bin / "psql"
    psql.write_text("#!/usr/bin/env python3\nimport json, os, sys\nfrom pathlib import Path\nquery = sys.stdin.read()\nassert 'CREATE DATABASE' not in query and 'COMMENT ON DATABASE' not in query\nassert os.environ['PGPASSWORD'] == 'fixture-private-value'\nPath(os.environ['XINGYUAN_FAKE_CALLS']).write_text('psql-read-only\\n')\nprint(json.dumps([{'databaseName': 'xingyuan_it_idp_20261011', 'oid': 42,\n                   'databaseOwner': 'foreign-owner', 'ownerComment': 'foreign-run'}]))\n")
    psql.chmod(0o755)
    collision_env = dict(env, PGUSER="controlled-owner")
    intent_file.write_text(json.dumps(collision)); intent_file.chmod(0o600)
    response = subprocess.run(command + ["prepare", "--manifest", str(intent_file), "--result", str(task / "evidence/collision.json")],
                              env=collision_env, cwd=root, capture_output=True, text=True, timeout=15)
    if response.returncode != 3:
        failures.append("existing database must be rejected before any preparation mutation: got exit " + str(response.returncode))
    if calls.exists():
        assert calls.read_text() == "psql-read-only\n"
        calls.unlink()
    assert "fixture-private-value" not in response.stdout + response.stderr
    # These recorders exercise the complete preparation algorithm without a DB, Redis, crypto process or socket.
    for name, contents in (
        ("psql", r"""#!/usr/bin/env python3
import json, os, re, sys
from pathlib import Path
state = Path(os.environ['XINGYUAN_FAKE_STATE'])
rows = json.loads(state.read_text()) if state.exists() else []
query = sys.stdin.read()
assert os.environ['PGPASSWORD'] == 'fixture-private-value'
with Path(os.environ['XINGYUAN_FAKE_CALLS']).open('a') as log:
    log.write('psql-' + ('create' if query.startswith('CREATE') else 'comment' if query.startswith('COMMENT') else 'read') + '\n')
if query.startswith('CREATE DATABASE'):
    assert (state.parent / 'evidence/original-databases.json').is_file(), 'Original database identities must be recorded before CREATE'
    assert (state.parent / 'evidence/original-nginx.json').is_file(), 'Original nginx must be recorded before CREATE'
    assert (state.parent / 'evidence/original-redis.json').is_file(), 'Original Redis must be recorded before CREATE'
    name = re.search(r'CREATE DATABASE "([a-z0-9_]+)"', query)[1]
    assert name.startswith('xingyuan_it_') and not any(row['databaseName'] == name for row in rows)
    rows.append({'databaseName': name, 'oid': 100 + len(rows), 'databaseOwner': 'controlled-owner', 'ownerComment': None})
    state.write_text(json.dumps(rows))
    if os.environ.get('XINGYUAN_FAKE_OUTCOME') == 'create-unknown': sys.exit(1)
elif query.startswith('COMMENT ON DATABASE'):
    name, comment = re.search(r'COMMENT ON DATABASE "([a-z0-9_]+)" IS \'([^\']+)\'', query).groups()
    next(row for row in rows if row['databaseName'] == name)['ownerComment'] = comment
    state.write_text(json.dumps(rows))
    if os.environ.get('XINGYUAN_FAKE_OUTCOME') == 'comment-unknown': sys.exit(1)
elif 'XINGYUAN_ORIGINAL_DATABASES' in query:
    changed = os.environ.get('XINGYUAN_FAKE_OUTCOME') == 'original-db-changed' and bool(rows)
    print(json.dumps([{'databaseName':'controlled-original','oid':999 if changed else 41,'databaseOwner':'controlled-original-owner'}]))
else:
    assert query.startswith('SELECT coalesce')
    assert "shobj_description(oid, 'pg_database')" in query
    print(json.dumps(rows))
"""),
        ("redis-cli", r"""#!/usr/bin/env python3
import json, os, sys
from pathlib import Path
args = sys.argv[1:]
assert os.environ['REDISCLI_AUTH'] == 'fixture-private-value'
assert not any(value in args for value in ('FLUSHDB', 'FLUSHALL', 'CONFIG', 'ACL'))
if '--json' in args:
    if 'INFO' in args:
        print(json.dumps('run_id:'+'a'*40+'\r\n' if args[-1]=='server' else '# Keyspace\r\ndb0:keys=1,expires=0,avg_ttl=0\r\n'))
    elif 'TIME' in args:
        print(json.dumps(['1792022400','0']))
    else:
        assert 'EVAL_RO' in args and args[-2:] == ['0','0']
        assert args[args.index('-n')+1]=='0'
        print(json.dumps(['0',[['b'*40,'string','c'*40,-1,12]]]))
    sys.exit(0)
file = Path(os.environ['XINGYUAN_FAKE_STATE'] + '.redis')
with Path(os.environ['XINGYUAN_FAKE_CALLS']).open('a') as log: log.write('redis-read-or-claim\n')
if os.environ.get('XINGYUAN_FAKE_OUTCOME') == 'foreign-redis':
    print('0')
elif 'EVAL' in args:
    marker = args[-2]
    if not file.exists(): file.write_text(marker); print('1')
    elif args[-1] == 'true' and file.read_text() == marker: print('2')
    else: print('0')
else:
    assert args[-2:] == ['GET', 'xingyuan-it:owner']
    print(file.read_text() if file.exists() else '')
"""),
        ("openssl", r"""#!/usr/bin/env python3
import os, sys
from pathlib import Path
args = sys.argv[1:]
with Path(os.environ['XINGYUAN_FAKE_CALLS']).open('a') as log: log.write(('openssl-generate-fixture' if '-keyout' in args or '-out' in args else 'openssl-verify-fixture') + '\n')
assert 'fixture-private-value' not in args
for option in ('-keyout', '-out'):
    if option in args:
        path = Path(args[args.index(option) + 1])
        assert not path.exists()
        path.write_bytes(b'controlled-certificate-or-key-fixture')
        path.chmod(0o600)
if '-pubkey' in args or '-pubout' in args: print('controlled-public-key')
"""),
        ("lsof", r"""#!/usr/bin/env python3
import os, sys
if '-Fpn' in sys.argv:
    print('p501\nn127.0.0.1:8080')
    sys.exit(0)
if os.environ.get('XINGYUAN_FAKE_OUTCOME') == 'occupied-port':
    print('123456')
    sys.exit(0)
sys.exit(1)
"""),
    ):
        fake = fake_bin / name
        fake.write_text(contents); fake.chmod(0o755)

    def preparation_fixture():
        value = copy.deepcopy(manifest)
        value["runId"] = str(uuid.uuid4())
        current_task = root / ("target/xingyuan-integration-" + value["runId"])
        for directory in (current_task, current_task / "secrets", current_task / "evidence"):
            directory.mkdir(mode=0o700, parents=True, exist_ok=True)
        value["runtimeRoot"] = str(current_task)
        for role in ("postgres", "redis"):
            file = current_task / "secrets" / (role + ".password")
            file.write_text("fixture-private-value"); file.chmod(0o600)
            value[role]["credentialFile"] = str(file)
        value["secretFiles"] = {role: str(current_task / "secrets" / (role + ".private")) for role in collision["secretFiles"]}
        value["processes"] = collision["processes"]
        value["artifacts"] = collision["artifacts"]
        value["policyFiles"] = []
        file = current_task / "intent.json"
        file.write_text(json.dumps(value)); file.chmod(0o600)
        current_env = dict(env, PGUSER="controlled-owner", XINGYUAN_FAKE_STATE=str(current_task / "fake-database-state.json"))
        return value, current_task, file, current_env

    preparation_ps=fake_bin/"ps"
    preparation_ps.write_text("#!/usr/bin/env python3\nimport os,sys\nif sys.argv[1:]==['-axo','pid=,uid=,lstart=,comm=']:\n print('501 '+str(os.getuid())+' Thu Oct 15 00:00:00 2026 /controlled/nginx')\nelif '-p' in sys.argv and sys.argv[sys.argv.index('-p')+1]=='501':\n print('Thu Oct 15 00:00:00 2026 S 501 nginx: master process /controlled/nginx')\nelse: sys.exit(1)\n")
    preparation_ps.chmod(0o755)

    def prepare_run(label, expected, current, current_task, file, current_env, resume=False):
        args = command + ["prepare", "--manifest", str(file), "--result", str(current_task / "evidence/prepare.json")]
        if resume: args.append("--resume")
        completed = subprocess.run(args, env=current_env, cwd=root, capture_output=True, text=True, timeout=15)
        if completed.returncode != expected:
            failures.append(label + ": expected exit " + str(expected) + ", got " + str(completed.returncode))
        assert "fixture-private-value" not in completed.stdout + completed.stderr
        return completed

    current, current_task, file, current_env = preparation_fixture()
    response = prepare_run("complete controlled preparation", 0, current, current_task, file, current_env)
    if response.returncode == 0:
        completed = json.loads((current_task / "evidence/prepare.json").read_text())
        assert completed["status"] == "COMMITTED" and completed["operationId"] == "CLI-001"
        assert completed["counts"] == {"databaseCount": 9, "tenantCount": 0, "clientCount": 0, "catalogCount": 0, "liveCaseCount": 0}
        assert completed["error"] is None and len(completed["targets"]) == 9
        assert len(list((current_task / "evidence").glob("db-*.json"))) == 9
        assert not list((current_task / "secrets").glob(".tls-*"))
        before = {path: path.read_bytes() for path in current_task.rglob("*") if path.is_file()}
        calls.unlink()
        prepare_run("same owner resume", 0, current, current_task, file, current_env, resume=True)
        assert all(path.read_bytes() == content for path, content in before.items()), "Resume changed a committed checkpoint or secret"
        assert "psql-create" not in calls.read_text() and "openssl-generate" not in calls.read_text()
        calls.unlink()
        idp_receipt=current_task/'evidence/db-idp.json'
        idp_bytes=idp_receipt.read_bytes();idp_receipt.unlink()
        prepare_run('same owner resume restores only its missing receipt',0,current,current_task,file,current_env,resume=True)
        assert idp_receipt.read_bytes()==idp_bytes
        assert 'psql-create' not in calls.read_text() and 'psql-comment' not in calls.read_text() and 'openssl-generate' not in calls.read_text()
        calls.unlink()
        preserved=(current_task/"evidence/original-databases.json").read_bytes()
        (current_task/"evidence/original-databases.json").unlink()
        prepare_run("resume cannot reconstruct a missing original before-state",9,current,current_task,file,current_env,resume=True)
        (current_task/"evidence/original-databases.json").write_bytes(preserved)
        (current_task/"evidence/original-databases.json").chmod(0o600)
        calls.unlink(missing_ok=True)
        nginx_checkpoint=(current_task/"evidence/original-nginx.json").read_bytes()
        (current_task/"evidence/original-nginx.json").unlink()
        prepare_run("resume cannot invent the original nginx state",9,current,current_task,file,current_env,resume=True)
        (current_task/"evidence/original-nginx.json").write_bytes(nginx_checkpoint)
        (current_task/"evidence/original-nginx.json").chmod(0o600)
        redis_checkpoint=(current_task/"evidence/original-redis.json").read_bytes()
        (current_task/"evidence/original-redis.json").unlink()
        prepare_run("resume cannot invent the original Redis state",9,current,current_task,file,current_env,resume=True)
        (current_task/"evidence/original-redis.json").write_bytes(redis_checkpoint)
        (current_task/"evidence/original-redis.json").chmod(0o600)
        calls.unlink(missing_ok=True)
        prepare_run("existing database requires resume", 5, current, current_task, file, current_env)
        assert not calls.exists()

    calls.unlink(missing_ok=True)
    current,current_task,file,current_env=preparation_fixture()
    # A previous claim may precede the very first database CREATE; it is already task state.
    partial_marker='xingyuan-it:'+current['runId']+':'+hashlib.sha256(file.read_bytes()).hexdigest()
    Path(current_env['XINGYUAN_FAKE_STATE']+'.redis').write_text(partial_marker)
    prepare_run('Redis-only task state cannot invent an original baseline',9,current,current_task,file,current_env,resume=True)
    assert not (current_task/'evidence/original-databases.json').exists()
    assert 'psql-create' not in calls.read_text()

    calls.unlink(missing_ok=True)
    current,current_task,file,current_env=preparation_fixture()
    current_env["XINGYUAN_FAKE_OUTCOME"]="original-db-changed"
    prepare_run("an original database replacement cannot commit preparation",3,current,current_task,file,current_env)
    assert len(json.loads(Path(current_env["XINGYUAN_FAKE_STATE"]).read_text()))==9
    assert not (current_task/"evidence/prepared.json").exists()
    assert json.loads((current_task/"evidence/original-databases.json").read_text())["databases"]["controlled-original"]["oid"]==41

    for outcome, expected in (("occupied-port", 3), ("foreign-redis", 3), ("create-unknown", 9)):
        calls.unlink(missing_ok=True)
        current, current_task, file, current_env = preparation_fixture()
        current_env["XINGYUAN_FAKE_OUTCOME"] = outcome
        prepare_run(outcome, expected, current, current_task, file, current_env)
        recorded = calls.read_text() if calls.exists() else ""
        if outcome != "create-unknown":
            assert "psql-create" not in recorded and "openssl-generate" not in recorded
        else:
            assert recorded.count("psql-create") == 1 and "psql-comment" not in recorded
            assert not (current_task / "evidence/db-idp.json").exists()
            current_env.pop("XINGYUAN_FAKE_OUTCOME")
            prepare_run("unknown commit freezes ownerless target", 3, current, current_task, file, current_env, resume=True)
            assert calls.read_text().count("psql-create") == 1
    calls.unlink(missing_ok=True)
    current,current_task,file,current_env=preparation_fixture()
    current_env['XINGYUAN_FAKE_OUTCOME']='comment-unknown'
    prepare_run('committed ownership comment with unknown return preserves the target',9,current,current_task,file,current_env)
    original_fact=json.loads(Path(current_env['XINGYUAN_FAKE_STATE']).read_text())[0]
    assert original_fact['ownerComment']=='xingyuan-it:'+current['runId']+':'+hashlib.sha256(file.read_bytes()).hexdigest()
    assert not (current_task/'evidence/db-idp.json').exists()
    assert calls.read_text().count('psql-create')==1 and calls.read_text().count('psql-comment')==1
    current_env.pop('XINGYUAN_FAKE_OUTCOME')
    prepare_run('missing receipt still requires explicit resume',3,current,current_task,file,current_env)
    assert calls.read_text().count('psql-create')==1 and calls.read_text().count('psql-comment')==1
    prepare_run('explicit resume reconciles committed ownership before continuing',0,current,current_task,file,current_env,resume=True)
    restored=json.loads((current_task/'evidence/db-idp.json').read_text())
    assert all(restored[key]==value for key,value in original_fact.items())
    assert calls.read_text().count('psql-create')==9 and calls.read_text().count('psql-comment')==9
    assert len(json.loads(Path(current_env['XINGYUAN_FAKE_STATE']).read_text()))==9
    calls.unlink(missing_ok=True)
    # A matching PID/start time/task marker is insufficient when the actual executable artifact differs.
    current, current_task, file, current_env = preparation_fixture()
    actual_command = "java -Dxingyuan.isolated.run-id=" + current["runId"] + " -jar " + str(root / "foreign-artifact.jar")
    record = {"schemaVersion": 1, "runId": current["runId"], "manifestSha256": hashlib.sha256(file.read_bytes()).hexdigest(),
              "processes": [{"id": "shoubing", "pid": 1234567, "startInstant": "2026-10-14T09:00:00Z",
                             "artifactSha256": current["artifacts"]["shoubing"]["sha256"],
                             "commandSha256": hashlib.sha256(actual_command.encode()).hexdigest()}]}
    private_json(current_task / "evidence/processes.json", record)
    ps = fake_bin / "ps"
    ps.write_text(r"""#!/usr/bin/env python3
import os, sys
assert sys.argv[1:] == ['-p', '1234567', '-o', 'lstart=', '-o', 'stat=', '-o', 'command=']
print('Wed Oct 14 09:00:00 2026 S ' + os.environ['XINGYUAN_FAKE_PS_COMMAND'])
""")
    ps.chmod(0o755)
    current_env["XINGYUAN_FAKE_PS_COMMAND"] = actual_command
    response = subprocess.run(command + ["status", "--manifest", str(file), "--result", str(current_task / "evidence/pid-state.json")],
                              env=current_env, cwd=root, capture_output=True, text=True, timeout=15)
    if response.returncode != 10:
        failures.append("PID receipt cannot substitute an unrelated executable artifact: got exit " + str(response.returncode))
    ps.write_text(r"""#!/usr/bin/env python3
import json, os, sys
from pathlib import Path
args = sys.argv[1:]
assert len(args) == 8 and args[0] == '-p' and args[2:] == ['-o', 'lstart=', '-o', 'stat=', '-o', 'command=']
pid = args[1]
terminated = Path(os.environ['XINGYUAN_FAKE_TERMINATED'])
if terminated.exists() and pid in json.loads(terminated.read_text()): sys.exit(1)
values = json.loads(os.environ['XINGYUAN_FAKE_PS_ROWS'])
if pid not in values: sys.exit(1)
value = values[pid]
print(value['start'] + ' ' + value['state'] + ' ' + value['command'])
""")
    signal = fake_bin / "kill"
    signal.write_text(r"""#!/usr/bin/env python3
import json, os, sys
from pathlib import Path
assert sys.argv[1] == '-TERM' and sys.argv[2] in ('1234567', '2345678') and len(sys.argv) == 3
file = Path(os.environ['XINGYUAN_FAKE_TERMINATED'])
values = json.loads(file.read_text()) if file.exists() else []
values.append(sys.argv[2]); file.write_text(json.dumps(values))
""")
    signal.chmod(0o755)

    def lifecycle_fixture():
        value, task_root, manifest_path, process_env = preparation_fixture()
        process_env["XINGYUAN_FAKE_TERMINATED"] = str(task_root / "controlled-terminated.json")
        actual = shlex.join(["java", "-Dxingyuan.isolated.run-id=" + value["runId"], "-jar", value["artifacts"]["shoubing"]["path"]])
        row = {"id": "shoubing", "pid": 1234567, "startInstant": "2026-10-14T09:00:00Z",
               "artifactSha256": value["artifacts"]["shoubing"]["sha256"], "commandSha256": hashlib.sha256(actual.encode()).hexdigest()}
        facts = {"1234567": {"start": "Wed Oct 14 09:00:00 2026", "state": "S", "command": actual}}
        process_env["XINGYUAN_FAKE_PS_ROWS"] = json.dumps(facts)
        receipt = {"schemaVersion": 1, "runId": value["runId"], "manifestSha256": hashlib.sha256(manifest_path.read_bytes()).hexdigest(), "processes": [row]}
        private_json(task_root / "evidence/processes.json", receipt)
        return value, task_root, manifest_path, process_env, receipt, facts

    def lifecycle_run(label, expected, task_root, manifest_path, process_env, operation="stop"):
        result = task_root / ("evidence/" + operation + ".json")
        response = subprocess.run(command + [operation, "--manifest", str(manifest_path), "--result", str(result)],
                                  env=process_env, cwd=root, capture_output=True, text=True, timeout=15)
        if response.returncode != expected:
            failures.append(label + ": expected exit " + str(expected) + ", got " + str(response.returncode))
        assert "fixture-private-value" not in response.stdout + response.stderr
        return response

    current, current_task, file, current_env, record, facts = lifecycle_fixture()
    status_env = dict(current_env); status_env.pop("XINGYUAN_ISOLATED_INTEGRATION")
    response = lifecycle_run("status is read-only without mutation opt-in", 0, current_task, file, status_env, operation="status")
    if response.returncode == 0:
        assert json.loads((current_task / "evidence/status.json").read_text())["status"] == "RUNNING"
    original_receipt = (current_task / "evidence/processes.json").read_bytes()
    originals = {path: path.read_bytes() for path in (current_task / "secrets").iterdir()}
    response = lifecycle_run("only the owned process receives TERM", 0, current_task, file, current_env)
    if response.returncode == 0:
        assert json.loads((current_task / "controlled-terminated.json").read_text()) == ["1234567"]
        assert json.loads((current_task / "evidence/stop.json").read_text())["status"] == "STOPPED"
        assert (current_task / "evidence/processes.json").read_bytes() == original_receipt
        assert all(path.read_bytes() == value for path, value in originals.items())
    for fault, expected in (("START", 10), ("COMMAND", 10), ("RUN", 3), ("PID", 3), ("DUPLICATE", 3)):
        current, current_task, file, current_env, record, facts = lifecycle_fixture()
        if fault == "START": record["processes"][0]["startInstant"] = "2026-10-14T08:00:00Z"
        elif fault == "COMMAND": record["processes"][0]["commandSha256"] = "0" * 64
        elif fault == "RUN": record["runId"] = str(uuid.uuid4())
        elif fault == "PID": record["processes"][0]["pid"] = True
        else: record["processes"].append(record["processes"][0])
        private_json(current_task / "evidence/processes.json", record)
        lifecycle_run("foreign process " + fault, expected, current_task, file, current_env)
        assert not (current_task / "controlled-terminated.json").exists(), "Rejected identity received a signal"

    current, current_task, file, current_env, record, facts = lifecycle_fixture()
    current["processes"].append({"id": "jianshen", "artifactRole": "jianshen", "kind": "JAVA", "port": 28130})
    current["artifacts"]["jianshen"] = current["artifacts"]["shoubing"]
    private_json(file, current)
    record["manifestSha256"] = hashlib.sha256(file.read_bytes()).hexdigest()
    second = dict(record["processes"][0], id="jianshen", pid=2345678, startInstant="2026-10-14T08:00:00Z")
    record["processes"].append(second)
    facts["2345678"] = facts["1234567"]
    current_env["XINGYUAN_FAKE_PS_ROWS"] = json.dumps(facts)
    private_json(current_task / "evidence/processes.json", record)
    lifecycle_run("all process identities are checked before the first TERM", 10, current_task, file, current_env)
    assert not (current_task / "controlled-terminated.json").exists()
    # The preceding synthetic application ledger used a ps stub; CLI children now use actual PID/group introspection.
    ps.unlink()
    # PUBLIC CLI dispatch is mocked at the declared Playwright owner boundary, never at a token endpoint.
    current, current_task, file, current_env = preparation_fixture()
    cli_registry = {"schemaVersion": 1, "runId": current["runId"], "manifestSha256": hashlib.sha256(file.read_bytes()).hexdigest(),
                    "tenantIds": {"default": "1001", "tenant-b": "1002"}, "subjects": {"admin": "101", "viewer": "102"},
                    "cliResources": [{"resourceKey": "tianshu", "resourceServerId": "resource-tianshu",
                                      "clientIds": {"default": "cli-a", "tenant-b": "cli-b"}, "readPath": "/api/v1/tianshu/registry/services/page"}],
                    "clients": [{"clientId": "cli-a", "clientPk": 9223372036854775000, "realmCode": "default", "clientType": "PUBLIC",
                                 "purpose": "USER", "status": "ACTIVE", "refreshTokenTtlSeconds": 0,
                                 "issuer": current["publicOrigin"] + "/tenants/1001", "resourceUris": ["https://resources.example/tianshu"],
                                 "allowedScopes": ["openid", "profile", "TIANSHU_READ"]}]}
    private_json(current_task / "secrets/runtime-registry.json", cli_registry)
    for name, operation, state in (("control-plane.json", "CLI-003", "RUNNING"), ("authorization-platform.json", "CLI-004", "COMMITTED")):
        private_json(current_task / "evidence" / name, {"schemaVersion": 1, "operationId": operation, "status": state,
                     "runId": current["runId"], "manifestSha256": cli_registry["manifestSha256"]})
    Path(current["secretFiles"]["tls-ca-cert"]).write_text("controlled-certificate-fixture")
    Path(current["secretFiles"]["tls-ca-cert"]).chmod(0o600)
    package = root / "egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/node_modules/@playwright/test/package.json"
    package.parent.mkdir(parents=True)
    package.write_text('{"version":"1.62.0"}')
    npm = fake_bin / "npm"
    npm.write_text(r"""#!/usr/bin/env python3
import datetime, hashlib, json, os, sys
from pathlib import Path
assert sys.argv[1:] == ['exec', '--', 'playwright', 'test', '--config', 'playwright.integration.config.ts', '--grep', 'CLI-006:', '--workers', '1']
assert not {'NODE_OPTIONS', 'NODE_TLS_REJECT_UNAUTHORIZED', 'DEBUG', 'PWDEBUG'} & os.environ.keys()
assert os.environ['XINGYUAN_ISOLATED_INTEGRATION'] == 'true'
manifest = json.loads(Path(os.environ['XINGYUAN_INTEGRATION_MANIFEST']).read_text())
root = Path(manifest['runtimeRoot'])
registry = json.loads((root / 'secrets/runtime-registry.json').read_text())
client = registry['clients'][0]
output = Path(os.environ['XINGYUAN_CLI_OUTPUT_FILE'])
assert not output.exists()
scope_only=os.environ.get('XINGYUAN_CLI_SCOPE_MODE')=='IDENTITY_ONLY'
assert os.environ.get('XINGYUAN_CLI_SCOPE_MODE') in (None,'IDENTITY_ONLY')
data = b'controlled-scope-input-not-a-JWT' if scope_only else b'controlled-formal-input-not-a-JWT'
output.write_bytes(data); output.chmod(0o600)
expiry = datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(seconds=-1 if os.environ.get('XINGYUAN_FAKE_OUTCOME') == 'expired-cli' else 300)
index = {'schemaVersion': 1, 'runId': registry['runId'], 'manifestSha256': registry['manifestSha256'],
         'tokens': [{'tenantCode': 'default', 'tenantId': '1001', 'resourceKey': 'tianshu', 'userKey': 'admin',
                     'clientId': client['clientId'], 'clientPk': str(client['clientPk']), 'issuer': client['issuer'],
                     'resourceUri': client['resourceUris'][0], 'scopes': ['openid'] if scope_only and os.environ.get('XINGYUAN_FAKE_OUTCOME')!='widen-scope-probe' else client['allowedScopes'],
                     'tokenFile': str(output), 'tokenSha256': hashlib.sha256(data).hexdigest(),
                     'probeStatus':None,'expiresAt': expiry.isoformat().replace('+00:00','Z')}]}
if scope_only:
    file=Path(str(output)+'.metadata.json')
    file.write_text(json.dumps({'schemaVersion':1,'runId':registry['runId'],'manifestSha256':registry['manifestSha256'],'scope':'IDENTITY_ONLY_PUBLIC_CODE','token':index['tokens'][0]})); file.chmod(0o600)
    if os.environ.get('XINGYUAN_FAKE_OUTCOME')=='change-canonical-index':
        (root/'secrets/token-index.json').write_text(json.dumps(index))
else:
    file = root / 'secrets/token-index.json'; file.write_text(json.dumps(index)); file.chmod(0o600)
result = {'schemaVersion': 1, 'operationId': 'CLI-006', 'runId': registry['runId'],
          'manifestSha256': registry['manifestSha256'], 'status': 'COMMITTED', 'error': None}
file = Path(os.environ['XINGYUAN_CLI_RESULT_FILE']); file.write_text(json.dumps(result)); file.chmod(0o600)
Path(os.environ['XINGYUAN_FAKE_CALLS']).write_text('public-owner-once\n')
""")
    npm.chmod(0o755)
    def formal_cli(label, expected, suffix):
        response = subprocess.run(command + ["authorize-cli", "--manifest", str(file), "--result", str(current_task / ("evidence/cli-" + suffix + ".json")),
                     "--tenant-code", "default", "--resource-key", "tianshu", "--user-key", "admin",
                     "--output-file", str(current_task / ("secrets/cli-" + suffix + ".at"))],
                     env=current_env, cwd=root, capture_output=True, text=True, timeout=15)
        if response.returncode != expected:
            failures.append(label + ": expected exit " + str(expected) + ", got " + str(response.returncode))
        assert "controlled-formal-input" not in response.stdout + response.stderr
        return response
    formal_cli("formal PUBLIC owner dispatch", 0, "positive")
    assert calls.read_text() == "public-owner-once\n"
    calls.unlink()
    cli_registry["clients"][0]["clientType"] = "CONFIDENTIAL"
    private_json(current_task / "secrets/runtime-registry.json", cli_registry)
    formal_cli("confidential BFF client cannot be a PUBLIC input", 3, "confidential")
    assert not calls.exists()
    cli_registry["clients"][0]["clientType"] = "PUBLIC"
    private_json(current_task / "secrets/runtime-registry.json", cli_registry)
    current_env["XINGYUAN_FAKE_OUTCOME"] = "expired-cli"
    formal_cli("expired owner output is not accepted", 6, "expired")
    assert (current_task / "secrets/cli-expired.at").exists(), "Partial owner output must be retained"
    calls.unlink()

    current_env.pop('XINGYUAN_FAKE_OUTCOME',None)
    canonical_scope_before=(current_task/'secrets/token-index.json').read_bytes()
    for scope_fault,expected in ((None,0),('widen-scope-probe',7),('change-canonical-index',3)):
        current_env['XINGYUAN_FAKE_OUTCOME']=scope_fault or 'scope-probe-ok'
        suffix=scope_fault or 'identity-only'; output=current_task/('secrets/scope-'+suffix+'.at'); result=current_task/('evidence/scope-'+suffix+'.json')
        response=subprocess.run([sys.executable,'-I',str(scripts/'lib/isolated_integration.py'),'authorize-scope-probe',str(file),str(result),
                                'default','tianshu','admin',str(output),str(root)],env=current_env,cwd=root,capture_output=True,text=True,timeout=15)
        if response.returncode!=expected: failures.append('identity-only scope producer '+str(scope_fault)+' expected '+str(expected)+' got '+str(response.returncode))
        if 'controlled-scope-input' in response.stdout+response.stderr: failures.append('scope producer printed token bytes')
        if scope_fault!='change-canonical-index' and (current_task/'secrets/token-index.json').read_bytes()!=canonical_scope_before:
            failures.append('scope probe replaced the exact full-scope canonical index')
        (current_task/'secrets/token-index.json').write_bytes(canonical_scope_before)
        calls.unlink(missing_ok=True)
    current_env.pop('XINGYUAN_FAKE_OUTCOME',None)

    response=subprocess.run([sys.executable,'-I',str(scripts/'lib/isolated_integration.py'),'authorize-scope-probe',str(file),str(current_task/'evidence/scope-viewer.json'),
                            'default','tianshu','viewer',str(current_task/'secrets/scope-viewer.at'),str(root)],env=current_env,cwd=root,capture_output=True,text=True,timeout=15)
    if response.returncode!=3 or calls.exists(): failures.append('identity-only probe accepted an unreviewed viewer action or reached the browser')
    current_env['XINGYUAN_CLI_SCOPE_MODE']='IDENTITY_ONLY'
    formal_cli('public CLI ignores a private scope-mode environment override',0,'scope-environment-override')
    public_after=json.loads((current_task/'secrets/token-index.json').read_bytes())
    if public_after['tokens'][0]['scopes']!=cli_registry['clients'][0]['allowedScopes']:
        failures.append('public CLI weakened the canonical exact-scope input contract')
    current_env.pop('XINGYUAN_CLI_SCOPE_MODE',None); calls.unlink(missing_ok=True)

    # Import the actual dedicated config with discovery semantics: no browser or web server is constructed.
    # A frozen but occupied control-plane port must stop before any Java/bootstrap call.
    control_run = str(uuid.uuid4())
    control_root = root / ("target/xingyuan-integration-" + control_run)
    for relative in ("", "evidence", "secrets", "profiles", "nginx"):
        (control_root / relative).mkdir(parents=True, exist_ok=True)
        (control_root / relative).chmod(0o700)
    control_manifest = copy.deepcopy(manifest)
    control_manifest.update(runId=control_run, runtimeRoot=str(control_root))
    control_manifest["postgres"]["credentialFile"] = str(control_root / "secrets/postgres.txt")
    control_manifest["redis"]["credentialFile"] = str(control_root / "secrets/redis.txt")
    control_manifest["processes"] = []
    control_manifest["artifacts"] = {}
    for name, kind, port, extra in (("shoubing", "JAVA", 28120, [28122]), ("jianshen", "JAVA", 28130, []),
                                    ("tianshu", "JAVA", 28150, [29080]), ("yuheng", "JAVA", 28140, []),
                                    ("edge", "JAVA", 28200, []), ("nginx", "NGINX", 28443, [])):
        artifact_file = control_root / "nginx/nginx.conf" if name == "nginx" else root / ("controlled-" + name + ".jar")
        artifact_file.write_bytes(("controlled-" + name).encode())
        artifact_file.chmod(0o600)
        control_manifest["processes"].append({"id": name, "artifactRole": name, "kind": kind, "port": port, "additionalPorts": extra})
        control_manifest["artifacts"][name] = {"path": str(artifact_file), "sha256": hashlib.sha256(artifact_file.read_bytes()).hexdigest(), "buildId": "controlled-control"}
    control_intent = control_root / "intent.json"
    private_json(control_intent, control_manifest)
    control_digest = hashlib.sha256(control_intent.read_bytes()).hexdigest()
    for filename, operation in (("prepared.json", "CLI-001"), ("identity.json", "CLI-002")):
        private_json(control_root / "evidence" / filename, {"schemaVersion": 1, "operationId": operation, "runId": control_run,
                     "manifestSha256": control_digest, "status": "COMMITTED"})
    (fake_bin / "lsof").write_text("#!/usr/bin/env bash\nprintf '%s\n' preflight-lsof >> \"$XINGYUAN_FAKE_CALLS\"\nprintf '%s\n' 70001\n")
    (fake_bin / "lsof").chmod(0o755)
    control_calls = root / "controlled-start-calls"
    response = subprocess.run(command + ["start-control-plane", "--manifest", str(control_intent), "--result", str(control_root / "evidence/control-result.json")],
                              env=dict(env, XINGYUAN_FAKE_CALLS=str(control_calls)), cwd=root, capture_output=True, text=True, timeout=15)
    if response.returncode != 3 or not control_calls.exists() or control_calls.read_text() != "preflight-lsof\n":
        failures.append("occupied control-plane port did not stop before bootstrap/Java startup")

    # Narrow process/JVM/HTTPS transports exercise lifecycle ordering without starting a service or socket.
    import importlib.util
    module_spec = importlib.util.spec_from_file_location("controlled_isolated_runtime", scripts / "lib/isolated_integration.py")
    runtime_module = importlib.util.module_from_spec(module_spec)
    module_spec.loader.exec_module(runtime_module)
    # Actual files/ZIP/XML are inspected here; no generation, Maven, JVM or network is substituted into a live claim.
    import zipfile
    build_workspace = root / '.runtime/xingyuan-plan-execution' / ('step17-native-build-' + str(uuid.uuid4()))
    build_workspace.mkdir(parents=True, mode=0o700)
    build_snapshot = build_workspace / 'source'
    build_snapshot.mkdir(mode=0o700)
    build_started = time.time_ns() - 1000000000
    def build_file(path, content):
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content)
        return path
    build_reports = {}
    for family in ('light', 'web', 'service', 'agent'):
        definition = build_snapshot / 'egon-cola-archetypes/definitions' / ('egon-cola-archetype-' + family)
        build_file(definition / 'src/test/resources/projects/basic/archetype.properties',
                   'groupId=reviewed.it\nartifactId=reviewed-' + family + '\nversion=0.1-SNAPSHOT\npackage=reviewed.pkg\n')
        product = build_snapshot / 'egon-cola-archetypes/.generated' / ('egon-cola-archetype-' + family)
        build_file(product / 'generation-manifest.sha256', family + '-manifest')
        project = product / 'target/test-classes/projects/basic/project' / ('reviewed-' + family)
        build_file(project / 'pom.xml', '<project/>')
        build_file(project / 'build.log', 'controlled original command output')
        jar = project / 'target' / ('reviewed-' + family + '-0.1-SNAPSHOT.jar')
        jar.parent.mkdir(parents=True, exist_ok=True)
        with zipfile.ZipFile(jar, 'w') as archive:
            archive.writestr('META-INF/MANIFEST.MF', 'Manifest-Version: 1.0\r\nMain-Class: org.springframework.boot.loader.launch.JarLauncher\r\nStart-Class: reviewed.pkg.Application\r\n\r\n')
            archive.writestr('BOOT-INF/classes/reviewed/pkg/Application.class', b'controlled-class-reference')
        required = ['NativeSecurityContractTest', 'ExplicitConstructorInjectionTest']
        if family in ('light', 'web'): required.append('NativeGraphQlSecurityContractTest')
        if family == 'agent': required.append('NativeAgentSecurityContractTest')
        for name in required:
            classname = 'reviewed.pkg.' + name
            report = build_file(project / 'target/surefire-reports' / ('TEST-' + classname + '.xml'),
                                '<testsuite name="' + classname + '" tests="1" failures="0" errors="0" skipped="0"><testcase classname="' + classname + '" name="executed"/></testsuite>')
            build_reports[(family, name)] = report
    build_products = runtime_module.collect_native_build_products(build_snapshot, build_started)
    if set(build_products) != {'native-light', 'native-web', 'native-service', 'native-agent'}:
        failures.append('native build collector omitted a required generated family')
    report = build_reports[('light', 'NativeSecurityContractTest')]
    original_report = report.read_text()
    for fault in ('skip', 'missing', 'stale', 'zero'):
        report.write_text(original_report)
        if fault == 'skip': report.write_text(original_report.replace('skipped="0"', 'skipped="1"').replace('</testcase>', '<skipped/></testcase>'))
        elif fault == 'missing': report.unlink()
        elif fault == 'stale': os.utime(report, ns=(build_started - 1, build_started - 1))
        else: report.write_text(original_report.replace('tests="1"', 'tests="0"').replace('<testcase classname="reviewed.pkg.NativeSecurityContractTest" name="executed"/>', ''))
        try:
            runtime_module.collect_native_build_products(build_snapshot, build_started)
            failures.append('native build collector accepted ' + fault + ' actual security execution')
        except SystemExit as rejected:
            if rejected.code != 8: failures.append('native build collector returned wrong ' + fault + ' code')
        report.write_text(original_report)
        os.utime(report, None)
    service_container=build_reports[('service','NativeSecurityContractTest')]
    service_original=service_container.read_text()
    service_container.write_text('<testsuite name="reviewed.pkg.NativeSecurityContractTest" tests="0" failures="0" errors="0" skipped="0"/>')
    nested_report=build_file(service_container.parent/'TEST-reviewed.pkg.NativeSecurityContractTest$ActualNestedChain.xml',
               '<testsuite name="reviewed.pkg.NativeSecurityContractTest$ActualNestedChain" tests="1" failures="0" errors="0" skipped="0"><testcase classname="reviewed.pkg.NativeSecurityContractTest$ActualNestedChain" name="executed"/></testsuite>')
    try:
        runtime_module.collect_native_build_products(build_snapshot,build_started)
    except SystemExit:
        failures.append('native build collector rejected an empty JUnit parent with genuinely executed nested security tests')
    service_container.write_text(service_original);nested_report.unlink()
    fingerprint_root = build_snapshot / 'fingerprint'
    tracked = build_file(fingerprint_root / 'source.java', 'source-one')
    # Exercise the producer's actual final call at its declared private workspace path.
    # The payload is explicitly CONTROLLED and can never satisfy the native provenance reader.
    import ast
    native_publisher_source = scripts / "lib/isolated_integration.py"
    native_builder = next(node for node in ast.parse(native_publisher_source.read_text()).body
                          if isinstance(node, ast.FunctionDef) and node.name == "build_native_provenance")
    native_publication = copy.deepcopy(native_builder.body[-1])
    assert isinstance(native_publication, ast.Expr) and isinstance(native_publication.value, ast.Call)
    assert native_publication.value.func.id in {"publish_task_json", "publish_private_file"}
    if isinstance(native_publication.value.args[2], ast.Dict):
        native_publication.value.args[2] = ast.Name(id="result", ctx=ast.Load())
    native_publication_code = compile(ast.fix_missing_locations(ast.Module(body=[native_publication], type_ignores=[])), str(native_publisher_source), "exec")
    native_publication_root = root / "controlled-native-publication"; native_publication_root.mkdir(mode=0o700)
    native_publication_path = native_publication_root / "native-build-provenance.json"
    native_payload = {"scope": "CONTROLLED_PUBLICATION_CONTRACT_ONLY", "status": "CONTROLLED", "marker": 1}
    native_publication_env = dict(runtime_module.__dict__, workspace=native_publication_root, result_file=native_publication_path, result=native_payload, json=json)
    try:
        exec(native_publication_code, native_publication_env)
        native_original = native_publication_path.read_bytes()
        assert json.loads(native_original) == native_payload and (native_publication_path.stat().st_mode & 0o777) == 0o600
        native_publication_env["result"] = dict(native_payload, marker=2)
        try:
            exec(native_publication_code, native_publication_env)
            raise AssertionError("changed native result accepted")
        except SystemExit as rejected:
            assert rejected.code == 5
        assert native_publication_path.read_bytes() == native_original
        native_foreign = root / "foreign-native-result.json"; native_publication_env["result_file"] = native_foreign
        try:
            exec(native_publication_code, native_publication_env)
            raise AssertionError("foreign native result accepted")
        except SystemExit as rejected:
            assert rejected.code == 3
        assert not native_foreign.exists()
    except (SystemExit, AssertionError) as rejected:
        failures.append("native producer private publication contract failed: " + str(rejected))

    initial_fingerprint = runtime_module.native_build_tree_fingerprint(fingerprint_root)
    mcp_review = {'schemaVersion':1,'runId':run_id,'processId':'mcp','tenantKeys':['default','tenant-b'],
                  'applicationCode':'yuheng-admin','businessCode':'controlled-platform',
                  'permissionCode':'mcp:xingyuan-it-'+run_id+':resource:scopes:read',
                  'resourceCode':'yuheng-admin.mcp-task-'+run_id.replace('-',''),
                  'roleCode':'XINGYUAN_MCP_'+run_id.replace('-','').upper()+'_ADMIN','userKey':'admin','approved':True}
    if runtime_module.validate_mcp_task_review(manifest,mcp_review) != mcp_review:
        failures.append('exact reviewed task MCP authorization was not retained')
    for field,value in [('schemaVersion',True),('runId',str(uuid.uuid4())),('processId','biz'),('tenantKeys',['default']),
                        ('applicationCode','mock-backend'),('businessCode','../foreign'),('permissionCode','mcp:*'),
                        ('resourceCode','foreign'),('roleCode','XINGYUAN_ADMIN'),('userKey','viewer'),('approved',False),('extra',True)]:
        changed=copy.deepcopy(mcp_review); changed[field]=value
        try:
            runtime_module.validate_mcp_task_review(manifest,changed)
            failures.append('MCP task review accepted broadened '+field)
        except SystemExit as rejected:
            if rejected.code != 3: failures.append('MCP task review returned wrong unsafe code for '+field)
    build_file(fingerprint_root / 'target/output', 'ignored-output')
    if runtime_module.native_build_tree_fingerprint(fingerprint_root) != initial_fingerprint:
        failures.append('native source fingerprint included Maven outputs')
    tracked.chmod(0o755)
    if runtime_module.native_build_tree_fingerprint(fingerprint_root) == initial_fingerprint:
        failures.append('native source fingerprint ignored executable mode changes')
    tracked.chmod(0o600); tracked.write_text('source-two')
    if runtime_module.native_build_tree_fingerprint(fingerprint_root) == initial_fingerprint:
        failures.append('native source fingerprint ignored content changes')
    (fingerprint_root / 'link').symlink_to(tracked)
    try:
        runtime_module.native_build_tree_fingerprint(fingerprint_root)
        failures.append('native build fingerprint followed a foreign symbolic link')
    except SystemExit as rejected:
        if rejected.code != 3: failures.append('native build fingerprint returned wrong link code')
    disabled_build = subprocess.run(private_module + ['native-build-provenance', str(root), str(task / 'evidence/forbidden-build.json'), str(policy)],
                                    env=default_environment, capture_output=True, text=True, timeout=5)
    if disabled_build.returncode != 3 or calls.exists():
        failures.append('native build entry reached a command without explicit opt-in')
    build_inputs = ('egon-cola-archetypes/source-projects', 'egon-cola-archetypes/definitions',
                    'egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter')
    for family in ('light', 'web', 'service'):
        build_file(build_snapshot / ('egon-cola-archetypes/source-projects/egon-cola-source-' + family + '-open') / 'pom.xml', '<project/>')
        build_file(build_snapshot / ('egon-cola-archetypes/.generated/egon-cola-archetype-' + family + '-open') / 'generation-manifest.sha256', family + '-open')
    build_file(build_snapshot / build_inputs[2] / 'pom.xml', '<project/>')
    build_stages = ['export', 'current-dependencies', 'generate-first', 'check-first', 'generate-second', 'check-second', 'source-install', 'resolved-peer-facades', 'actual-all-seven']
    build_commands = []
    for stage in build_stages:
        log = build_file(build_workspace / (stage + '.log'), 'controlled-' + stage); log.chmod(0o600)
        build_commands.append({'stage': stage, 'exitCode': 0, 'logSha256': runtime_module.task_artifact_sha256(log)})
    build_proof = {'schemaVersion': 1, 'scope': 'CURRENT_GENERATED_NATIVE_BUILD_ONLY', 'status': 'PASSED', 'sourceHead': 'a'*40,
                   'workspace': str(build_workspace), 'sourceInputs': {name:runtime_module.native_build_tree_fingerprint(build_snapshot/name) for name in build_inputs},
                   'generatedFamilies': {family:runtime_module.native_build_tree_fingerprint(build_snapshot/'egon-cola-archetypes/.generated'/('egon-cola-archetype-'+family))
                                         for family in ('light','web','service','agent','light-open','web-open','service-open')},
                   'openSources': {family:runtime_module.native_build_tree_fingerprint(build_snapshot/('egon-cola-archetypes/source-projects/egon-cola-source-'+family+'-open'))
                                   for family in ('light','web','service')},
                   'commands': build_commands, 'artifacts': runtime_module.collect_native_build_products(build_snapshot, build_started),
                   'startedNs': build_started, 'completedAt': datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00','Z')}
    build_manifest = copy.deepcopy(owner_manifest)
    build_manifest['artifacts'] = {role:{'path':item['path'],'sha256':item['sha256'],'buildId':'controlled-build'} for role,item in build_proof['artifacts'].items()}
    build_policy = task/'policies/native-build-provenance.json'
    def publish_build_fixture(value):
        private_json(build_workspace/build_policy.name, value); private_json(build_policy,value)
        build_manifest['policyFiles'] = [{'path':str(build_policy),'sha256':runtime_module.task_artifact_sha256(build_policy)}]
    original_build_run = subprocess.run
    build_git_commands = []
    def build_git_only(arguments, **keywords):
        if arguments[0] != 'git': raise AssertionError('build reader reached a non-git process')
        build_git_commands.append(arguments)
        return type('GitResult',(),{'returncode':0})()
    try:
        subprocess.run = build_git_only
        publish_build_fixture(build_proof)
        if runtime_module.read_native_build_provenance(build_manifest) != build_proof or len(build_git_commands)!=2:
            failures.append('native build reader did not inspect the actual frozen products and source ancestry')
        for fault in ('status','head','source','open','generation','command','missing-agent','time','artifact','policy-sha'):
            changed = copy.deepcopy(build_proof)
            if fault == 'status': changed['status']='PARTIAL'
            elif fault == 'head': changed['sourceHead']='invented'
            elif fault == 'source': changed['sourceInputs'][build_inputs[2]]['sha256']='b'*64
            elif fault == 'open': changed['openSources'].pop('light')
            elif fault == 'generation': changed['generatedFamilies']['web-open']['sha256']='b'*64
            elif fault == 'command': changed['commands'][-1]['exitCode']=1
            elif fault == 'missing-agent': changed['artifacts'].pop('native-agent')
            elif fault == 'time': changed['completedAt']='2000-01-01T00:00:00Z'
            publish_build_fixture(changed)
            selected_manifest=copy.deepcopy(build_manifest)
            if fault == 'artifact': selected_manifest['artifacts']['native-light']['path']=str(root/'foreign.jar')
            if fault == 'policy-sha': selected_manifest['policyFiles'][0]['sha256']='b'*64
            try:
                runtime_module.read_native_build_provenance(selected_manifest)
                failures.append('native build reader accepted '+fault)
            except SystemExit as rejected:
                if rejected.code!=3: failures.append('native build reader returned wrong '+fault+' code')
    finally:
        subprocess.run=original_build_run
    print('PASS native build ZIP/XML/source-mode/provenance guards with controlled files; no Maven or runtime')
    original_nginx_fixture={"501":{"pid":501,"uid":os.getuid(),"processGroup":501,"startInstant":"2026-10-15T00:00:00Z","commandSha256":"a"*64,"listenPorts":[8080]}}
    for fault,current_nginx in (("gone",{}),("port changed",{"501":{**original_nginx_fixture["501"],"listenPorts":[8081]}})):
        try:
            runtime_module.require_original_nginx(original_nginx_fixture,current_nginx)
            failures.append("original nginx preservation accepted "+fault)
        except SystemExit as rejected:
            if rejected.code!=3: failures.append("original nginx "+fault+" returned wrong exit")
    runtime_module.require_original_nginx(original_nginx_fixture,{**original_nginx_fixture,"502":{**original_nginx_fixture["501"],"pid":502}})
    for field,value in (("pid",True),("uid",-1),("listenPorts",[8080,8080]),("commandSha256","bad")):
        invalid=copy.deepcopy(original_nginx_fixture);invalid["501"][field]=value
        try:
            runtime_module.require_original_nginx(invalid,invalid)
            failures.append("original nginx accepted invalid "+field)
        except SystemExit as rejected:
            if rejected.code!=3: failures.append("invalid original nginx returned wrong unsafe exit")
    old_database={"operator-original":{"databaseName":"operator-original","oid":41,"databaseOwner":"operator-owner"}}
    runtime_module.require_original_databases(old_database,copy.deepcopy(old_database))
    runtime_module.require_original_databases(old_database,{**old_database,"another-database":{"databaseName":"another-database","oid":42,"databaseOwner":"another-owner"}})
    for label,changed in (("deleted",{}),("replaced",{"operator-original":{**old_database["operator-original"],"oid":99}})):
        try:
            runtime_module.require_original_databases(old_database,changed)
            failures.append("preservation accepted an original database being "+label)
        except SystemExit as rejected:
            if rejected.code!=3: failures.append("database preservation returned wrong unsafe exit")
    for field,value in (("oid",True),("databaseOwner",""),("databaseName","another")):
        invalid=copy.deepcopy(old_database); invalid["operator-original"][field]=value
        try:
            runtime_module.require_original_databases(invalid,invalid)
            failures.append("original database baseline accepted invalid "+field)
        except SystemExit as rejected:
            if rejected.code!=3: failures.append("invalid original baseline returned wrong exit")
    from unittest.mock import patch
    import types
    # Fixed task Boot variation: package only controlled class bytes; substitute all process transports.
    import io, zipfile
    probe_run = str(uuid.uuid4()); probe_root = root / ('target/xingyuan-integration-' + probe_run)
    for relative in ('', 'evidence', 'profiles', 'secrets', 'policies'):
        (probe_root / relative).mkdir(parents=True, exist_ok=True); (probe_root / relative).chmod(0o700)
    probe_package = Path('top/egon/cola/platform/tianquan/shoubing/admin/integration')
    probe_module = root / 'egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin'
    probe_source = probe_module / 'src/test/java' / probe_package / 'XingyuanRuntimeProbeInitializer.java'
    probe_source.parent.mkdir(parents=True, exist_ok=True); probe_source.write_text('controlled source evidence only')
    probe_classes = probe_module / 'target/test-classes' / probe_package
    probe_classes.mkdir(parents=True, exist_ok=True)
    initializer_class = probe_classes / 'XingyuanRuntimeProbeInitializer.class'
    initializer_class.write_bytes(b'\xca\xfe\xba\xbe\0\0\0Acontrolled-not-executable')
    try:
        runtime_module.package_runtime_probe_fixture(root, probe_root)
        failures.append('runtime fixture packaged without its generated mapper class')
    except SystemExit as rejected:
        if rejected.code != 4: failures.append('missing compiled fixture returned wrong exit')
    for name in ('XingyuanRuntimeProbeEdgeBindingConverterImpl.class', 'XingyuanRuntimeProbeInitializer$CommandDTO.class', 'UnrelatedTest.class'):
        (probe_classes / name).write_bytes(initializer_class.read_bytes())
    fixture_build = runtime_module.package_runtime_probe_fixture(root, probe_root)
    fixture_file = probe_root / 'artifacts/task-runtime-probe.jar'
    fixture_bytes = fixture_file.read_bytes()
    if runtime_module.package_runtime_probe_fixture(root, probe_root) != fixture_build or fixture_file.read_bytes() != fixture_bytes:
        failures.append('runtime fixture packaging is not byte-stable')
    if any('Test' in name for name in fixture_build['entries']) or len(fixture_build['entries']) != 4:
        failures.append('thin fixture included an unrelated test or omitted its fixed factories entry')
    with zipfile.ZipFile(io.BytesIO(fixture_bytes)) as original_fixture:
        fixture_entries = {name:original_fixture.read(name) for name in original_fixture.namelist()}
    for wrong_name,wrong_value in (('META-INF/spring.factories',b'foreign.Initializer\n'),('foreign/OtherTest.class',initializer_class.read_bytes())):
        invalid=io.BytesIO()
        with zipfile.ZipFile(invalid,'w') as jar:
            for name,value in {**fixture_entries,wrong_name:wrong_value}.items(): jar.writestr(name,value)
        try:
            runtime_module.runtime_probe_fixture_entries(invalid.getvalue())
            failures.append('runtime fixture accepted foreign entry/factories')
        except SystemExit as rejected:
            if rejected.code != 3: failures.append('foreign fixture entry returned wrong exit')
    original_probe_source=probe_source.read_bytes(); probe_source.write_text('changed source evidence')
    try:
        runtime_module.package_runtime_probe_fixture(root,probe_root)
        failures.append('runtime fixture overwrote differing build provenance')
    except SystemExit as rejected:
        if rejected.code!=5: failures.append('changed fixture provenance returned wrong exit')
    probe_source.write_bytes(original_probe_source)
    stale_root = root / ('target/xingyuan-integration-' + str(uuid.uuid4()))
    stale_root.mkdir(mode=0o700)
    try:
        runtime_module.package_runtime_probe_fixture(root, stale_root)
        failures.append('runtime fixture labeled stale compiled classes with current source')
    except SystemExit as rejected:
        if rejected.code != 4: failures.append('stale compiled fixture returned wrong exit')
    source_time = probe_source.stat().st_mtime_ns
    for compiled in probe_classes.glob('*.class'):
        os.utime(compiled, ns=(source_time + 1000000, source_time + 1000000))
    probe_boot=root/'controlled-properties-launcher.jar'
    def write_probe_boot(start_class='controlled.OriginalMain',launcher=True):
        with zipfile.ZipFile(probe_boot,'w') as jar:
            jar.writestr('META-INF/MANIFEST.MF','Manifest-Version: 1.0\r\nStart-Class: '+start_class+'\r\n\r\n')
            jar.writestr('BOOT-INF/classes/controlled/OriginalMain.class',initializer_class.read_bytes())
            if launcher: jar.writestr('org/springframework/boot/loader/launch/PropertiesLauncher.class',initializer_class.read_bytes())
    write_probe_boot(); original_probe_boot=probe_boot.read_bytes()
    probe_manifest=copy.deepcopy(manifest); probe_manifest.update(runId=probe_run,runtimeRoot=str(probe_root))
    probe_manifest['processes']=[{'id':'edge','artifactRole':'edge','kind':'JAVA','port':28200,'additionalPorts':[]}]
    probe_manifest['artifacts']={'edge':{'path':str(probe_boot),'sha256':hashlib.sha256(original_probe_boot).hexdigest(),'buildId':'controlled'}}
    probe_manifest['policyFiles']=[{'path':str(fixture_file),'sha256':fixture_build['fixtureSha256']}]
    probe_manifest_file=probe_root/'intent-custom-name.json'; private_json(probe_manifest_file,probe_manifest)
    probe_digest=hashlib.sha256(probe_manifest_file.read_bytes()).hexdigest()
    probe_configuration=runtime_module.runtime_probe_configuration(probe_manifest,'edge',probe_manifest_file,probe_digest)
    if probe_configuration['startClass']!='controlled.OriginalMain' or runtime_module.runtime_probe_configuration(probe_manifest,'native-agent') is not None:
        failures.append('runtime fixture lost the original main or applied to an unapproved role')
    probe_source.write_text('uncompiled changed runtime observer')
    try:
        runtime_module.runtime_probe_configuration(probe_manifest,'edge',probe_manifest_file,probe_digest)
        failures.append('runtime launch accepted a fixture for different current source')
    except SystemExit as rejected:
        if rejected.code != 3: failures.append('changed launch source returned wrong exit')
    probe_source.write_bytes(original_probe_source)
    light_probe_manifest=copy.deepcopy(probe_manifest)
    light_probe_manifest['processes']=[{'id':'native-light','artifactRole':'native-light','kind':'JAVA','port':28310,'additionalPorts':[29310]}]
    light_probe_manifest['artifacts']={'native-light':copy.deepcopy(probe_manifest['artifacts']['edge'])}
    light_probe_file=probe_root/'light-probe-intent.json'; private_json(light_probe_file,light_probe_manifest)
    light_configuration=runtime_module.runtime_probe_configuration(light_probe_manifest,'native-light',light_probe_file,hashlib.sha256(light_probe_file.read_bytes()).hexdigest())
    if not isinstance(light_configuration,dict) or light_configuration.get('sha256')!=fixture_build['fixtureSha256']:
        failures.append('approved Light observation did not require the same frozen runtime fixture')
    without_fixture=copy.deepcopy(probe_manifest); without_fixture['policyFiles']=[]
    if runtime_module.runtime_probe_configuration(without_fixture,'edge') is not None:
        failures.append('unregistered fixture changed the default java-jar launch')
    budget_probe=copy.deepcopy(probe_manifest); budget_probe['deadlines']={key:1 for key in budget_probe['deadlines']}
    runtime_module.runtime_probe_configuration(budget_probe,'edge',probe_manifest_file,probe_digest)
    for fault in ('hash','role-policy','duplicate-policy','manifest','budget'):
        wrong=copy.deepcopy(probe_manifest); expected_hash=probe_digest
        if fault=='hash': wrong['policyFiles'][0]['sha256']='0'*64
        elif fault=='role-policy': wrong['policyFiles'][0]['path']=str(probe_root/'policies/task-runtime-probe.jar')
        elif fault=='duplicate-policy': wrong['policyFiles'].append(wrong['policyFiles'][0])
        elif fault=='manifest': expected_hash='0'*64
        else: wrong['deadlines']['httpSeconds']=99999
        try:
            runtime_module.runtime_probe_configuration(wrong,'edge',probe_manifest_file,expected_hash)
            failures.append('runtime fixture accepted '+fault)
        except SystemExit as rejected:
            if rejected.code!=3: failures.append('runtime fixture '+fault+' returned wrong exit')
    for start_class,launcher in (('../Foreign',True),('controlled.OriginalMain',False)):
        write_probe_boot(start_class,launcher)
        wrong=copy.deepcopy(probe_manifest); wrong['artifacts']['edge']['sha256']=hashlib.sha256(probe_boot.read_bytes()).hexdigest()
        private_json(probe_manifest_file,wrong)
        try:
            runtime_module.runtime_probe_configuration(wrong,'edge',probe_manifest_file,hashlib.sha256(probe_manifest_file.read_bytes()).hexdigest())
            failures.append('runtime fixture accepted missing PropertiesLauncher/invalid Start-Class')
        except SystemExit as rejected:
            if rejected.code!=3: failures.append('invalid original Boot layout returned wrong exit')
    probe_boot.write_bytes(original_probe_boot); private_json(probe_manifest_file,probe_manifest)
    probe_argv=['/controlled/java',*runtime_module.runtime_probe_java_arguments(probe_manifest,'edge',probe_configuration)]
    def observe_probe(arguments):
        text=shlex.join(arguments)
        record={'id':'edge','pid':1234567,'startInstant':'2026-10-15T00:00:00Z','commandSha256':hashlib.sha256(text.encode()).hexdigest()}
        with patch('subprocess.run',return_value=types.SimpleNamespace(returncode=0,stdout='Thu Oct 15 00:00:00 2026 S '+text+'\n',stderr='')):
            return runtime_module.observe_owned_process(probe_manifest,record)
    if observe_probe(probe_argv)!='RUNNING': failures.append('fixed owned Boot variation was not recognized')
    for fault in ('classpath','main','loader','role','fixture','run','agent'):
        wrong=list(probe_argv)
        if fault=='classpath': wrong[wrong.index('-cp')+1]=str(root/'foreign.jar')
        elif fault=='main': wrong[wrong.index('org.springframework.boot.loader.launch.PropertiesLauncher')]='foreign.Main'
        elif fault=='agent': wrong.insert(1,'-javaagent:/foreign.jar')
        else:
            prefix={'loader':'-Dloader.path=','role':'-Dxingyuan.isolated.runtime-probe.role=','fixture':'-Dxingyuan.isolated.runtime-probe.fixture-sha256=','run':'-Dxingyuan.isolated.run-id='}[fault]
            index=next(i for i,value in enumerate(wrong) if value.startswith(prefix)); wrong[index]=prefix+'foreign'
        try:
            observe_probe(wrong); failures.append('owned JVM observation accepted arbitrary '+fault)
        except SystemExit as rejected:
            if rejected.code!=10: failures.append('owned JVM '+fault+' returned wrong identity rejection')
    password_file=probe_root/'secrets/tls-password'; password_file.write_text('controlled-private-value'); password_file.chmod(0o600)
    probe_manifest['secretFiles']={'tls-keystore-password':str(password_file)}; private_json(probe_manifest_file,probe_manifest)
    probe_digest=hashlib.sha256(probe_manifest_file.read_bytes()).hexdigest()
    profile_file=probe_root/'profiles/edge.properties'; profile_file.write_text('controlled=true\n');profile_file.chmod(0o600)
    private_json(probe_root/'evidence/profiles-ready.json',{'runId':probe_run,'manifestSha256':probe_digest,'profiles':{'edge':{'path':str(profile_file),'sha256':hashlib.sha256(profile_file.read_bytes()).hexdigest()}}})
    with patch('shutil.which',return_value='/controlled/java'),patch('subprocess.Popen',return_value=types.SimpleNamespace(pid=1234567)) as launched, \
         patch.object(runtime_module,'capture_owned_process',return_value={'id':'edge','pid':1234567}):
        runtime_module.launch_owned_runtime(probe_manifest,probe_digest,'edge',probe_root/'secrets/task.p12',probe_manifest_file)
        if launched.call_args.args[0]!=probe_argv: failures.append('runtime launcher did not use the sole fixed Boot argv')
    if probe_boot.read_bytes()!=original_probe_boot: failures.append('runtime fixture modified the original Boot JAR')
    audit_digest=hashlib.sha256(control_intent.read_bytes()).hexdigest()
    private_json(control_root/"evidence/original-databases.json",{"schemaVersion":1,"runId":control_run,"manifestSha256":audit_digest,"status":"CAPTURED",
                 "observedAt":datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00','Z'),"databases":old_database})
    private_json(control_root/"evidence/db-idp.json",{"runId":control_run,"manifestSha256":audit_digest,"role":"idp","databaseName":"xingyuan_it_idp_20261011","databaseOwner":"controlled-owner"})
    audit_credential=Path(control_manifest["postgres"]["credentialFile"])
    audit_credential.write_text("controlled-audit-password");audit_credential.chmod(0o600)
    audit_directory=control_root/"evidence"/("verify-"+str(uuid.uuid4()));audit_directory.mkdir(mode=0o700)
    def controlled_original_query(argv,**options):
        assert argv[:4]==['psql','-X','--no-password','-A'] and argv[argv.index('-d')+1]=='postgres'
        assert argv[argv.index('-h')+1]=='127.0.0.1' and argv[argv.index('-p')+1]=='5432'
        assert options['input'].startswith('SELECT ') and 'XINGYUAN_ORIGINAL_DATABASES' in options['input']
        assert options['env']['PGPASSWORD']=='controlled-audit-password' and not {'PGSERVICE','PGOPTIONS','PGHOST'}&options['env'].keys()
        assert 'controlled-audit-password' not in str(argv)
        return types.SimpleNamespace(returncode=0,stdout=json.dumps(list(old_database.values())),stderr='')
    with patch.dict(os.environ,{'PGSERVICE':'foreign','PGHOST':'foreign','PGOPTIONS':'-c search_path=foreign'}),patch.object(subprocess,'run',side_effect=controlled_original_query):
        if runtime_module.verify_original_databases(control_manifest,audit_digest,audit_directory)['originalDatabaseCount']!=1:
            failures.append('original database maintenance audit lost the original identities')
    def controlled_nginx_query(argv,**options):
        if argv==['ps','-axo','pid=,uid=,lstart=,comm=']:
            return types.SimpleNamespace(returncode=0,stdout='501 '+str(os.getuid())+' Thu Oct 15 00:00:00 2026 /controlled/nginx\n502 0 Thu Oct 15 00:00:00 2026 /controlled/unrelated\n503 -2 Thu Oct 15 00:00:00 2026 /usr/libexec/dhcp6d\n',stderr='')
        if argv[0]=='ps':
            assert argv[1:3]==['-p','501']
            return types.SimpleNamespace(returncode=0,stdout='Thu Oct 15 00:00:00 2026 S 501 nginx: master process /controlled/nginx\n',stderr='')
        assert argv==['lsof','-nP','-a','-p','501','-iTCP','-sTCP:LISTEN','-Fpn']
        return types.SimpleNamespace(returncode=0,stdout='p501\nn127.0.0.1:8443\nn*:8080\n',stderr='')
    with patch.object(subprocess,'run',side_effect=controlled_nginx_query):
        observed=runtime_module.original_nginx_facts(control_manifest)
        assert observed['501']['listenPorts']==[8080,8443] and len(observed)==1
        private_json(control_root/'evidence/original-nginx.json',{'schemaVersion':1,'runId':control_run,'manifestSha256':audit_digest,'status':'CAPTURED',
                     'observedAt':datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00','Z'),'processes':observed})
        if runtime_module.verify_original_nginx(control_manifest,audit_digest,audit_directory)['originalProcessCount']!=1:
            failures.append('original nginx read-only observer lost its original process')
    def unreadable_nginx(argv,**options):
        response=controlled_nginx_query(argv,**options)
        if argv[0]=='lsof': return types.SimpleNamespace(returncode=1,stdout='',stderr='permission denied')
        return response
    with patch.object(subprocess,'run',side_effect=unreadable_nginx):
        try:
            runtime_module.original_nginx_facts(control_manifest)
            failures.append('unreadable original nginx was treated as absent')
        except SystemExit as rejected:
            if rejected.code!=6: failures.append('unreadable nginx returned wrong dependency exit')
    def negative_nginx_uid(argv,**options):
        response=controlled_nginx_query(argv,**options)
        if argv==['ps','-axo','pid=,uid=,lstart=,comm=']:
            response.stdout=response.stdout.replace('501 '+str(os.getuid())+' ', '501 -2 ', 1)
        return response
    with patch.object(subprocess,'run',side_effect=negative_nginx_uid):
        try:
            runtime_module.original_nginx_facts(control_manifest)
            failures.append('original nginx accepted a negative owner uid')
        except SystemExit as rejected:
            if rejected.code!=6: failures.append('negative nginx uid returned wrong dependency exit')
    redis_fixture={'serverReference':'a'*64,'observedAtMs':1000,'databases':{'0':{
        'b'*40:{'type':'string','valueSha1':'c'*40,'expiresAtMs':-1},
        'd'*40:{'type':'hash','valueSha1':'e'*40,'expiresAtMs':2000}}}}
    expired_redis=copy.deepcopy(redis_fixture);expired_redis['observedAtMs']=2000
    del expired_redis['databases']['0']['d'*40]
    runtime_module.require_original_redis(redis_fixture,expired_redis)
    extra_redis=copy.deepcopy(redis_fixture);extra_redis['databases']['2']={'f'*40:{'type':'set','valueSha1':'f'*40,'expiresAtMs':-1}}
    runtime_module.require_original_redis(redis_fixture,extra_redis)
    for label in ('persistent-missing','early-expiry','value-changed','ttl-changed','server-changed','clock-reversed','invalid-type','invalid-expiry'):
        changed=copy.deepcopy(redis_fixture)
        if label=='persistent-missing': del changed['databases']['0']['b'*40]
        elif label=='early-expiry': del changed['databases']['0']['d'*40]
        elif label=='value-changed': changed['databases']['0']['b'*40]['valueSha1']='f'*40
        elif label=='ttl-changed': changed['databases']['0']['d'*40]['expiresAtMs']=2001
        elif label=='server-changed': changed['serverReference']='f'*64
        elif label=='clock-reversed': changed['observedAtMs']=999
        elif label=='invalid-type': changed['databases']['0']['b'*40]['type']=True
        else: changed['databases']['0']['b'*40]['expiresAtMs']=True
        try:
            runtime_module.require_original_redis(redis_fixture,changed)
            failures.append('Redis preservation accepted '+label)
        except SystemExit as rejected:
            if rejected.code!=3: failures.append('Redis '+label+' returned wrong unsafe exit')
    redis_credential=Path(control_manifest['redis']['credentialFile'])
    redis_credential.write_text('controlled-redis-password');redis_credential.chmod(0o600)
    redis_fault={}
    redis_queries=[]
    def controlled_redis_query(argv,**options):
        assert argv[:9]==['redis-cli','-h','127.0.0.1','-p','6379','-n',argv[6],'-e','--no-auth-warning']
        assert argv[9]=='--json' and 'controlled-redis-password' not in str(argv)
        assert options['env']['REDISCLI_AUTH']=='controlled-redis-password'
        assert 'REDISCLI_USER' not in options['env'] and 'REDISCLI_HOST' not in options['env']
        redis_queries.append(argv[10])
        if argv[10]=='INFO':
            value='run_id:'+'a'*40+'\r\n' if argv[-1]=='server' else '# Keyspace\r\ndb0:keys=2,expires=1,avg_ttl=1000\r\ndb'+str(control_manifest['redis']['database'])+':keys=1,expires=0,avg_ttl=0\r\n'
            if redis_fault.get('raw-info') or redis_fault.get('raw-info-invalid') or redis_fault.get('raw-info-duplicate'):
                if argv[-1]=='server':
                    value='# Server\r\n'+value
                    if redis_fault.get('raw-info-invalid'): value=value.replace('run_id:', 'other_id:')
                    if redis_fault.get('raw-info-duplicate'): value+='run_id:'+'b'*40+'\r\n'
                return types.SimpleNamespace(returncode=0,stdout=value.encode(),stderr=b'')
        elif argv[10]=='TIME': value=['1792022400','0']
        else:
            assert argv[10]=='EVAL_RO' and argv[6]=='0' and argv[-2]=='0'
            assert "redis.call('SCAN'" in argv[11] and "redis.call('PEXPIRETIME'" in argv[11]
            assert 'memory>4194304' in argv[11] and '#value>4194304' in argv[11] and 'bytes>33554432' in argv[11]
            assert not any("redis.call('"+command in argv[11] for command in ('SET','DEL','EXPIRE','FLUSH','CONFIG','ACL'))
            value=['0', [['b'*40,'string','c'*40,-1,12],['d'*40,'hash','e'*40,1792022401000,24]]]
            if redis_fault.get('repeat'): value=['7',value[1]]
            if redis_fault.get('within-key-limit'): value[1][0][4]=4194304
            if redis_fault.get('large'): value[1][0][4]=4194305
            if redis_fault.get('byte-budget'): value[1]=[[format(index,'040x'),'string','c'*40,-1,4194304] for index in range(9)]
            if redis_fault.get('overflow'): value[1]=[[format(index,'040x'),'string','c'*40,-1,12] for index in range(513)]
            if redis_fault.get('invalid'): value[1][0][3]=True
        return types.SimpleNamespace(returncode=1 if redis_fault.get('unreadable') else 0,stdout=json.dumps(value).encode(),stderr=b'')
    with patch.dict(os.environ,{'REDISCLI_USER':'foreign','REDISCLI_HOST':'foreign'}),patch.object(subprocess,'run',side_effect=controlled_redis_query):
        observed=runtime_module.original_redis_facts(control_manifest)
        assert set(observed['databases'])=={'0'} and len(observed['databases']['0'])==2
        assert redis_queries==['INFO','INFO','EVAL_RO','TIME','INFO']
        private_json(control_root/'evidence/original-redis.json',{'schemaVersion':1,'runId':control_run,'manifestSha256':audit_digest,
                     'status':'CAPTURED','snapshot':observed})
        proof=runtime_module.verify_original_redis(control_manifest,audit_digest,audit_directory)
        assert proof['originalKeyCount']==2 and proof['scope']=='ORIGINAL_REDIS_SAMPLED_KEY_VALUES_ONLY'
        redis_fault['raw-info']=True
        raw_observed=runtime_module.original_redis_facts(control_manifest)
        assert raw_observed==observed
        redis_fault.clear();redis_fault['within-key-limit']=True
        assert runtime_module.original_redis_facts(control_manifest)==observed
        redis_fault.clear()
        for fault in ('repeat','large','byte-budget','overflow','unreadable','invalid','raw-info-invalid','raw-info-duplicate'):
            redis_fault.clear();redis_fault[fault]=True
            try:
                runtime_module.original_redis_facts(control_manifest)
                failures.append('Redis observer accepted '+fault)
            except SystemExit as rejected:
                if rejected.code!=(3 if fault=='invalid' else 6): failures.append('Redis observer '+fault+' returned wrong exit')
    # Real bounded CPU-only processes verify cancellation across the separate CLI browser process group.
    owned_bin=root/"owned-cli-bin"
    owned_bin.mkdir()
    owned_npm=owned_bin/"npm"
    owned_npm.write_text("#!/usr/bin/env python3\nimport os,time\nfrom pathlib import Path\np=Path(os.environ['XINGYUAN_OWNED_PID_FILE'])\np.write_text(str(os.getpid()))\np.chmod(0o600)\ntime.sleep(5)\n")
    owned_npm.chmod(0o755)
    controller_code=r'''
import hashlib,importlib.util,json,os,sys
from pathlib import Path
spec=importlib.util.spec_from_file_location("owned_cli_runtime",sys.argv[1])
module=importlib.util.module_from_spec(spec); spec.loader.exec_module(module)
manifest=json.loads(Path(sys.argv[2]).read_text())
manifest["deadlines"]["operationSeconds"]=int(sys.argv[4])
log=Path(sys.argv[3])
fd=os.open(log,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
sys.exit(module.run_authorization_browser(manifest,hashlib.sha256(Path(sys.argv[2]).read_bytes()).hexdigest(),sys.argv[5],dict(os.environ),fd,log))
'''
    for mode,seconds in (("signal",30),("timeout",1)):
        pid_file=control_root/"evidence"/("owned-cli-"+mode+".pid")
        child_log=control_root/"evidence"/("owned-cli-"+mode+".log")
        owned_environment=dict(os.environ,PATH=str(owned_bin)+os.pathsep+os.environ["PATH"],XINGYUAN_OWNED_PID_FILE=str(pid_file))
        controller=subprocess.Popen([sys.executable,"-I","-c",controller_code,str(scripts/"lib/isolated_integration.py"),str(control_intent),str(child_log),str(seconds),str(root)],
                                    env=owned_environment,stdin=subprocess.DEVNULL,stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True,start_new_session=True)
        try:
            if mode=="signal":
                bound=time.monotonic()+5
                while (not child_log.with_suffix(".process.json").exists() or not pid_file.exists()) and controller.poll() is None and time.monotonic()<bound:
                    time.sleep(0.02)
                if not child_log.with_suffix(".process.json").exists(): failures.append("owned CLI child did not record its actual birth")
                controller.terminate()
            stdout,stderr=controller.communicate(timeout=10)
            if controller.returncode!=9: failures.append("owned CLI "+mode+" did not preserve unknown commit: "+str(controller.returncode)+" "+stderr[-300:])
            if pid_file.exists():
                try:
                    os.kill(int(pid_file.read_text()),0)
                    failures.append("owned CLI "+mode+" left its detached CPU child alive")
                except ProcessLookupError: pass
            else: failures.append("owned CLI "+mode+" did not execute the controlled CPU child")
            receipt=json.loads(child_log.with_suffix(".process.json").read_text())
            if receipt.get("state")!="EXITED" or receipt.get("exitCode") is None:
                failures.append("owned CLI "+mode+" did not record terminal process state")
        finally:
            if controller.poll() is None:
                controller.terminate(); controller.wait(timeout=10)
    # Validate task pid/log/TLS confinement without invoking nginx or opening a listener.
    nginx_test_root = control_root / "nginx"
    nginx_test_root.mkdir(mode=0o700, exist_ok=True)
    nginx_file = nginx_test_root / "nginx.conf"
    nginx_before = nginx_file.read_bytes()
    nginx_secrets = {name: str(control_root / "secrets" / name) for name in
                     ("tls-server-cert", "tls-server-key", "tls-ca-cert")}
    nginx_guard_manifest = dict(control_manifest, secretFiles=nginx_secrets)
    def nginx_guard_config(pid=None, log_variable="$uri"):
        return (f'pid "{pid or nginx_test_root / "nginx.pid"}";\n'
                f'error_log "{control_root / "evidence/nginx-error.log"}" crit;\n'
                'events {} http { log_format isolated_safe "$request_method ' + log_variable + ' $status $request_id";\n'
                f'access_log "{control_root / "evidence/nginx-access.log"}" isolated_safe;\n'
                'server { listen 127.0.0.1:28443 ssl;\n'
                f'ssl_certificate "{nginx_secrets["tls-server-cert"]}";\n'
                f'ssl_certificate_key "{nginx_secrets["tls-server-key"]}";\n'
                'location / { proxy_pass https://127.0.0.1:28200; proxy_ssl_verify on;\n'
                f'proxy_ssl_trusted_certificate "{nginx_secrets["tls-ca-cert"]}"; }} }} }}')
    for label, content, expected in (("safe", nginx_guard_config(), 0),
                                     ("foreign pid", nginx_guard_config("/tmp/existing-nginx.pid"), 3),
                                     ("query in access log", nginx_guard_config(log_variable="$request_uri"), 3),
                                     ("hidden system include", nginx_guard_config() + "\ninclude /etc/nginx/nginx.conf;", 3),
                                     ("disabled upstream verification", nginx_guard_config().replace("proxy_ssl_verify on", "proxy_ssl_verify off"), 3),
                                     ("foreign upstream", nginx_guard_config().replace("127.0.0.1:28200", "127.0.0.1:18180"), 3),
                                     ("public listener", nginx_guard_config().replace("127.0.0.1:28443", "0.0.0.0:28443"), 3)):
        nginx_file.write_text(content)
        nginx_file.chmod(0o600)
        nginx_guard_manifest["artifacts"] = {"nginx": {"path": str(nginx_file), "sha256": hashlib.sha256(nginx_file.read_bytes()).hexdigest(), "buildId": "controlled-nginx"}}
        try:
            runtime_module.validate_task_nginx_configuration(nginx_guard_manifest)
            actual = 0
        except SystemExit as rejected:
            actual = rejected.code
        if actual != expected:
            failures.append("nginx confinement " + label + " expected " + str(expected) + " got " + str(actual))
    fault_map=nginx_test_root/'revocation-fault.map'; fault_map.write_bytes(b'default 0;\n'); fault_map.chmod(0o600)
    fault_config=nginx_guard_config().replace('http {', 'http { map $uri $xingyuan_task_revocation_fault { include "'+str(fault_map)+'"; }', 1).replace(
            'server {', 'server { if ($xingyuan_task_revocation_fault) { return 503; }', 1)
    for label,content,map_bytes,expected in (
            ('bounded revocation fault map',fault_config,b'default 0;\n',0),
            ('foreign fault include',fault_config.replace(str(fault_map),'/etc/nginx/foreign.map'),b'default 0;\n',3),
            ('wrong fault selector',fault_config.replace('map $uri','map $request_uri'),b'default 0;\n',3),
            ('inline fault rules',fault_config.replace('include "','default 1; include "'),b'default 0;\n',3),
            ('foreign fault response',fault_config.replace('return 503;','return 200;'),b'default 0;\n',3),
            ('global endpoint fault',fault_config,b'default 1;\n',3)):
        nginx_file.write_text(content); fault_map.write_bytes(map_bytes)
        nginx_guard_manifest['artifacts']['nginx']['sha256']=hashlib.sha256(nginx_file.read_bytes()).hexdigest()
        try:
            runtime_module.validate_task_nginx_configuration(nginx_guard_manifest); actual=0
        except SystemExit as rejected:
            actual=rejected.code
        if actual!=expected: failures.append('task revocation fault '+label+' expected '+str(expected)+' got '+str(actual))
    fault_map.write_bytes(b'default 0;\n')
    nginx_file.write_text(fault_config)
    fault_manifest=copy.deepcopy(nginx_guard_manifest)
    fault_manifest['artifacts']['nginx']['sha256']=hashlib.sha256(nginx_file.read_bytes()).hexdigest()
    fault_intent=control_root/'fault-intent.json'; private_json(fault_intent,fault_manifest)
    fault_digest=hashlib.sha256(fault_intent.read_bytes()).hexdigest()
    prior_fault_registry=(control_root/'secrets/runtime-registry.json').read_bytes() if (control_root/'secrets/runtime-registry.json').exists() else None
    private_json(control_root/'secrets/runtime-registry.json',{'schemaVersion':1,'runId':control_run,'manifestSha256':fault_digest,
                 'tenantIds':{'default':'1001','tenant-b':'1002'},'realms':{'platform':{'issuer':fault_manifest['publicOrigin']+'/platform'},
                 'default':{'issuer':fault_manifest['publicOrigin']+'/tenants/1001'},'tenant-b':{'issuer':fault_manifest['publicOrigin']+'/tenants/1002'}}})
    prior_fault_ledger=(control_root/'evidence/processes.json').read_bytes() if (control_root/'evidence/processes.json').exists() else None
    private_json(control_root/'evidence/processes.json',{'schemaVersion':1,'runId':control_run,'manifestSha256':fault_digest,'processes':[
                 {'id':'nginx','pid':900001,'startInstant':'2026-10-15T00:00:00Z','artifactSha256':fault_manifest['artifacts']['nginx']['sha256'],'commandSha256':'d'*64}]})
    def fault_signal(arguments,**kwargs):
        assert arguments==['kill','-HUP','900001'], 'fault injection signalled a foreign process or used another command'
        return types.SimpleNamespace(returncode=0,stdout=b'',stderr=b'')
    def fault_probe(intent,digest,enabled):
        assert digest==fault_digest
        return {'issuerCount':3,'revocationStatus':503 if enabled else 401,'metadataStatus':200}
    try:
        with patch.dict(os.environ,{'XINGYUAN_ISOLATED_INTEGRATION':'true'}),patch.object(runtime_module,'observe_owned_process',return_value='RUNNING'), \
             patch('subprocess.run',side_effect=fault_signal) as signalled,patch.object(runtime_module,'probe_task_revocation_fault',side_effect=fault_probe):
            for mode in ('on','off'):
                result=control_root/('evidence/fault-'+mode+'.json')
                try:
                    runtime_module.set_task_revocation_fault(['controlled',str(fault_intent),str(result),mode])
                except SystemExit as rejected:
                    failures.append('owned AS fault '+mode+' returned '+str(rejected.code)); continue
                outcome=json.loads(result.read_bytes())
                if outcome.get('status')!='COMMITTED' or outcome.get('counts',{}).get('liveCaseCount')!=0:
                    failures.append('owned AS fault result claimed a live case or lost its committed state')
            if signalled.call_count!=2 or fault_map.read_bytes()!=b'default 0;\n':
                failures.append('owned AS fault did not preserve the config and restore the exact OFF map')
            retained_state=(control_root/'evidence/revocation-fault-state.json').read_bytes()
            signal_count=signalled.call_count
            with patch.object(runtime_module,'observe_owned_process',side_effect=SystemExit(10)):
                try:
                    runtime_module.set_task_revocation_fault(['controlled',str(fault_intent),str(control_root/'evidence/fault-foreign-pid.json'),'on'])
                    failures.append('AS fault accepted a foreign nginx process')
                except SystemExit as rejected:
                    if rejected.code!=10: failures.append('AS fault foreign process returned wrong exit')
            if signalled.call_count!=signal_count or fault_map.read_bytes()!=b'default 0;\n' or (control_root/'evidence/revocation-fault-state.json').read_bytes()!=retained_state:
                failures.append('foreign nginx fault changed state, map or sent a signal')
            unknown=control_root/'evidence/fault-unknown.json'
            with patch.object(runtime_module,'probe_task_revocation_fault',side_effect=SystemExit(6)):
                try:
                    runtime_module.set_task_revocation_fault(['controlled',str(fault_intent),str(unknown),'on'])
                    failures.append('AS fault treated an unconfirmed reload as committed')
                except SystemExit as rejected:
                    if rejected.code!=9: failures.append('unconfirmed AS fault did not return commit-unknown')
            if json.loads(unknown.read_bytes()).get('status')!='UNKNOWN_COMMIT' or fault_map.read_bytes()==b'default 0;\n':
                failures.append('unknown fault lost the actual changed file or outcome evidence')
            signal_count=signalled.call_count
            try:
                runtime_module.set_task_revocation_fault(['controlled',str(fault_intent),str(control_root/'evidence/fault-blind-retry.json'),'on'])
                failures.append('unknown AS fault was retried blindly')
            except SystemExit as rejected:
                if rejected.code!=9: failures.append('unknown AS fault retry returned wrong exit')
            if signalled.call_count!=signal_count: failures.append('unknown AS fault retried nginx reload')
            runtime_module.set_task_revocation_fault(['controlled',str(fault_intent),str(control_root/'evidence/fault-explicit-off.json'),'off'])
            if fault_map.read_bytes()!=b'default 0;\n': failures.append('explicit fault cleanup did not restore OFF')
    finally:
        if prior_fault_registry is None: (control_root/'secrets/runtime-registry.json').unlink(missing_ok=True)
        else: (control_root/'secrets/runtime-registry.json').write_bytes(prior_fault_registry)
        if prior_fault_ledger is None: (control_root/'evidence/processes.json').unlink(missing_ok=True)
        else: (control_root/'evidence/processes.json').write_bytes(prior_fault_ledger)
    nginx_file.write_bytes(nginx_before)
    # Queue observations use only the task profile's canonical keys and original read-only Redis metadata.
    queue_root = root / ('target/xingyuan-integration-' + str(uuid.uuid4()))
    for relative in ('', 'evidence', 'profiles', 'secrets'):
        (queue_root / relative).mkdir(mode=0o700)
    queue_manifest = copy.deepcopy(manifest); queue_manifest.update(runId=queue_root.name.removeprefix('xingyuan-integration-'), runtimeRoot=str(queue_root))
    queue_digest = 'c' * 64; queue_namespace = 'xingyuan-it:' + queue_manifest['runId'] + ':bff:'
    queue_profile = queue_root / 'profiles/edge.properties'
    queue_bytes = ('egon.yuheng.edge.logout.stream=' + (queue_namespace + 'logout:v1').replace(':','\\:') + '\n'
                   + 'egon.yuheng.edge.logout.consumer-group=' + (queue_namespace + 'revocation:v1').replace(':','\\:') + '\n').encode('ascii')
    def queue_inputs(contents):
        queue_profile.write_bytes(contents); queue_profile.chmod(0o600)
        private_json(queue_root/'evidence/profiles-ready.json', {'schemaVersion':1,'runId':queue_manifest['runId'], 'manifestSha256':queue_digest,
                     'profiles':{'edge':{'path':str(queue_profile),'sha256':hashlib.sha256(contents).hexdigest()}}})
    queue_inputs(queue_bytes)
    private_json(queue_root/'evidence/processes.json', {'processes':[{'id':'edge'}]})
    queue_marker='xingyuan-it:'+queue_manifest['runId']+':'+queue_digest
    def queue_query(arguments,**kwargs):
        if kwargs['env'].get('REDISCLI_AUTH')!='controlled-queue-password' or 'controlled-queue-password' in arguments:
            failures.append('queue observer exposed the Redis credential or omitted protected authentication')
        if arguments[-2:]==['GET','xingyuan-it:owner']: value=queue_marker
        elif 'EVAL_RO' in arguments and arguments[-2:]==[queue_namespace+'logout:v1',queue_namespace+'revocation:v1']:
            if any(word in arguments[arguments.index('EVAL_RO')+1] for word in ('XRANGE','XREAD','XACK','XDEL','payload')):
                failures.append('queue observer read/mutated a payload rather than fixed counts')
            value=[2,1]
        else:
            failures.append('queue observer accepted an arbitrary Redis command/key'); value=None
        return types.SimpleNamespace(returncode=0,stdout=json.dumps(value).encode(),stderr=b'')
    with patch.object(runtime_module,'validate_owned_process_record'),patch.object(runtime_module,'observe_owned_process',return_value='RUNNING'), \
         patch.object(runtime_module,'task_credential',return_value='controlled-queue-password') as queue_credential,patch('subprocess.run',side_effect=queue_query) as queue_transport:
        queue_counts=runtime_module.read_task_logout_queue(queue_manifest,queue_digest)
        if queue_counts.get('queuedCount')!=2 or queue_counts.get('pendingCount')!=1 or len(queue_counts)!=5:
            failures.append('queue observer lost its fixed count-only result')
        for fault in ('foreign-stream','duplicate-stream','wrong-hash'):
            altered=queue_bytes.replace((queue_namespace+'logout:v1').replace(':','\\:').encode(),b'foreign') if fault=='foreign-stream' else queue_bytes
            if fault=='duplicate-stream': altered+=b'egon.yuheng.edge.logout.stream=foreign\n'
            queue_inputs(altered)
            if fault=='wrong-hash': queue_profile.write_bytes(queue_bytes+b'changed=true\n')
            credential_calls=queue_credential.call_count; transport_calls=queue_transport.call_count
            try:
                runtime_module.read_task_logout_queue(queue_manifest,queue_digest)
                failures.append('queue observer accepted '+fault)
            except SystemExit as rejected:
                if rejected.code!=3: failures.append('queue profile '+fault+' returned wrong exit')
            if credential_calls!=queue_credential.call_count or transport_calls!=queue_transport.call_count:
                failures.append('invalid queue profile reached credential/Redis I/O')
        queue_inputs(queue_bytes)
        for invalid in ([-1,0],[1,2],[1.5,0],[True,0],[100001,0]):
            def invalid_queue(arguments,**kwargs):
                return types.SimpleNamespace(returncode=0,stdout=json.dumps(queue_marker if arguments[-2:]==['GET','xingyuan-it:owner'] else invalid).encode(),stderr=b'')
            with patch('subprocess.run',side_effect=invalid_queue):
                try:
                    runtime_module.read_task_logout_queue(queue_manifest,queue_digest)
                    failures.append('queue observer accepted malformed count facts')
                except SystemExit as rejected:
                    if rejected.code!=6: failures.append('invalid queue count returned wrong exit')
        with patch('subprocess.run',return_value=types.SimpleNamespace(returncode=0,stdout=b'"foreign-owner"',stderr=b'')) as wrong_owner:
            try:
                runtime_module.read_task_logout_queue(queue_manifest,queue_digest)
                failures.append('queue observer read a foreign namespace owner')
            except SystemExit as rejected:
                if rejected.code!=3 or wrong_owner.call_count!=1: failures.append('queue owner guard did not precede metadata read')
    # The actual installed Vite loader resolves a controlled config; it never starts a server.
    frontend_project = root / "controlled-frontend"
    frontend_project.mkdir(mode=0o700)
    (frontend_project / "node_modules").mkdir(mode=0o700)
    installed_vite = source / "egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/node_modules/vite"
    (frontend_project / "node_modules/vite").symlink_to(installed_vite, target_is_directory=True)
    (frontend_project / "package.json").write_text('{"type":"module"}')
    (frontend_project / ".env").write_text("VITE_CONTROLLED_OLD_ENV=must-not-load\n")
    frontend_env = control_root / "profiles/frontend-env"
    frontend_env.mkdir(mode=0o700, exist_ok=True)
    for secret in ("tls-server-cert", "tls-server-key"):
        path = Path(nginx_secrets[secret])
        path.write_text("controlled-configuration-bytes-only")
        path.chmod(0o600)
    original_frontend = ("import {loadEnv} from " + json.dumps((installed_vite / "dist/node/index.js").as_uri()) + ";\n"
                         "export default ({mode}) => ({server:{proxy:{'/api':{target:'http://127.0.0.1:18180'}}},"
                         "define:{'controlled.env':JSON.stringify(loadEnv(mode,process.cwd(),'VITE_').VITE_CONTROLLED_OLD_ENV ?? 'isolated')}})\n")
    (frontend_project / "vite.config.ts").write_text(original_frontend)
    generated_vite = control_root / "profiles/controlled.vite.config.mjs"
    generated_vite.write_bytes(runtime_module.render_frontend_vite_configuration(frontend_project, control_root, 28141,
                              "/apps/yuheng/", {"VITE_BASE_PATH":"/apps/yuheng/", "VITE_OAUTH_REGISTRATION_ID":"controlled-real-registration"}, nginx_secrets))
    generated_vite.chmod(0o600)
    inspect_vite = ("import assert from 'node:assert/strict'; const {default:c}=await import(process.argv[1]);"
                    "assert.equal(c.define['controlled.env'],JSON.stringify('isolated'));"
                    "assert.equal(c.server.host,'127.0.0.1');assert.equal(c.server.port,28141);"
                    "assert.equal(c.base,'/apps/yuheng/');assert.deepEqual(c.server.proxy,{});"
                    "assert.equal(c.server.strictPort,true);assert.equal(c.envDir,process.argv[2]);"
                    "assert.ok(Buffer.isBuffer(c.server.https.key));")
    frontend_check = subprocess.run(["node", "--input-type=module", "-e", inspect_vite, generated_vite.as_uri(), str(frontend_env)],
                                    cwd=frontend_project, env=runtime_module.isolated_runtime_environment(), capture_output=True, text=True, timeout=30)
    if frontend_check.returncode:
        failures.append("actual Vite configuration isolation failed: " + frontend_check.stderr[-2000:])
    (frontend_project / "vite.config.ts").write_text("export default {resolve:{alias:{'@egon-cola/xingyuan-admin-web-shared':'/foreign/source'}}}\n")
    frontend_alias = subprocess.run(["node", "--input-type=module", "-e", "await import(process.argv[1])", generated_vite.as_uri()],
                                    cwd=frontend_project, env=runtime_module.isolated_runtime_environment(), capture_output=True, text=True, timeout=30)
    if frontend_alias.returncode == 0 or "XINGYUAN_SOURCE_ALIAS_FORBIDDEN" not in frontend_alias.stderr:
        failures.append("frontend shared-package source alias was not rejected")
    # Actual package/config provenance with a controlled identity registry; no app is started.
    frontend_manifest = copy.deepcopy(control_manifest)
    frontend_manifest["repoRoot"] = str(source)
    frontend_manifest["secretFiles"] = nginx_secrets
    frontend_locations = {"portal-web":"egon-cola-xingyuan/egon-cola-xingyuan-admin-portal",
                          "shoubing-web":"egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web",
                          "jianshen-web":"egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web",
                          "yuheng-web":"egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web",
                          "tianshu-web":"egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web"}
    frontend_clients, frontend_aliases = [], {}
    for offset, (role, relative) in enumerate(frontend_locations.items()):
        package = source / relative / "package.json"
        frontend_manifest["artifacts"][role] = {"path":str(package), "sha256":hashlib.sha256(package.read_bytes()).hexdigest(), "buildId":"controlled-frontend"}
        frontend_manifest["processes"].append({"id":role,"artifactRole":role,"kind":"NODE","port":28121+offset,"additionalPorts":[]})
        alias = {"portal-web":"edge-ui","shoubing-web":"edge-shoubing","jianshen-web":"edge-jianshen","yuheng-web":"edge-yuheng","tianshu-web":"edge-tianshu"}[role]
        issuer_registrations = {}
        for tenant in ("1001", "1002"):
            issuer = frontend_manifest["publicOrigin"] + "/tenants/" + tenant
            registration = alias + "-actual-" + tenant
            issuer_registrations[issuer] = registration
            frontend_clients.append({"registrationId":registration,"issuer":issuer,"clientType":"CONFIDENTIAL","purpose":"USER","status":"ACTIVE","alias":alias})
        frontend_aliases[alias] = {"defaultRegistrationId":alias+"-actual-1001","issuerRegistrationIds":issuer_registrations}
    old_control_registry = (control_root / "secrets/runtime-registry.json").read_bytes() if (control_root / "secrets/runtime-registry.json").exists() else None
    private_json(control_root / "secrets/runtime-registry.json", {"schemaVersion":1,"runId":control_run,"manifestSha256":control_digest,
                 "publicOrigin":frontend_manifest["publicOrigin"],"tenantIds":{"default":"1001","tenant-b":"1002"},"aliases":frontend_aliases,"clients":frontend_clients})
    frontend_receipt = runtime_module.prepare_frontend_profiles(frontend_manifest, control_digest)
    if set(frontend_receipt["profiles"]) != set(frontend_locations):
        failures.append("frontend generator did not publish five task profiles")
    sandbox_source = source / frontend_locations["portal-web"] / "public/wujie-empty.html"
    if (control_root / "portal-public/wujie-empty.html").read_bytes() != sandbox_source.read_bytes():
        failures.append("frontend generator did not preserve the same-origin sandbox document")
    if runtime_module.prepare_frontend_profiles(frontend_manifest, control_digest) != frontend_receipt:
        failures.append("same frontend facts changed immutable output")
    actual_config_probe = ("import assert from 'node:assert/strict';const {default:c}=await import(process.argv[1]);"
                           "assert.equal(c.root,process.argv[2]);assert.equal(c.server.host,'127.0.0.1');"
                           "assert.equal(c.server.strictPort,true);assert.deepEqual(c.server.proxy,{});"
                           "assert.ok(c.plugins.flat(Infinity).length>=2);assert.ok(Buffer.isBuffer(c.server.https.key));")
    for role, relative in frontend_locations.items():
        probe = subprocess.run(["node", "--input-type=module", "-e", actual_config_probe,
                                Path(frontend_receipt["profiles"][role]["path"]).as_uri(), str(source / relative)],
                                cwd=source / relative, env=runtime_module.isolated_runtime_environment(), capture_output=True, text=True, timeout=30)
        if probe.returncode:
            failures.append("actual frontend Vite config " + role + " failed: " + probe.stderr[-2000:])
    from unittest.mock import patch
    from types import SimpleNamespace
    with patch("subprocess.Popen", return_value=SimpleNamespace(pid=71111)) as node_transport, \
         patch.object(runtime_module, "capture_owned_process", return_value={"id":"yuheng-web","pid":71111}):
        runtime_module.launch_owned_runtime(frontend_manifest, control_digest, "yuheng-web", control_root / "unused.p12")
        arguments, keywords = node_transport.call_args
        expected_project = source / frontend_locations["yuheng-web"]
        if (arguments[0][1] != str(expected_project / "node_modules/vite/bin/vite.js")
                or keywords["cwd"] != str(expected_project) or "NODE_OPTIONS" in keywords["env"]
                or keywords["env"].get("NODE_EXTRA_CA_CERTS") != nginx_secrets["tls-ca-cert"]):
            failures.append("Node task launch did not preserve the isolated invocation contract")
    portal_manifest = json.loads((control_root / "portal-public/portal-manifest/isolated.json").read_text())
    if portal_manifest["tianshu"]["url"] != "https://127.0.0.1:28443/apps/tianshu/":
        failures.append("Portal manifest did not retain the canonical same-origin mount")
    if portal_manifest["tianquan-jianshen"]["standaloneUrl"] != "https://127.0.0.1:28443/apps/tianquan-jianshen/iam/roles":
        failures.append("Portal standalone entry did not use the actual Jianshen IAM roles route")
    if old_control_registry is None:
        (control_root / "secrets/runtime-registry.json").unlink()
    else:
        (control_root / "secrets/runtime-registry.json").write_bytes(old_control_registry)
    # Formal-input preflight only: controlled opaque files never claim JWT/server acceptance.
    consumer_manifest = copy.deepcopy(control_manifest)
    consumer_run = str(uuid.uuid4())
    consumer_root = root / ("target/xingyuan-integration-" + consumer_run)
    for directory in (consumer_root, consumer_root / "secrets", consumer_root / "evidence"):
        directory.mkdir(mode=0o700)
    consumer_manifest.update(runId=consumer_run, runtimeRoot=str(consumer_root))
    consumer_digest = "f" * 64
    formal_clients, formal_tokens = [], []
    formal_scopes = ["openid", "TIANSHU_READ", "TIANSHU_WRITE"]
    for tenant, tenant_id in (("default", "1001"), ("tenant-b", "1002")):
        client_id, client_pk = "formal-" + tenant, "900719925474099" + ("3" if tenant == "default" else "4")
        issuer = consumer_manifest["publicOrigin"] + "/tenants/" + tenant_id
        formal_clients.append({"clientId":client_id,"clientPk":int(client_pk),"realmCode":tenant,"issuer":issuer,
                              "clientType":"PUBLIC","purpose":"USER","status":"ACTIVE","refreshTokenTtlSeconds":0,
                              "allowedScopes":formal_scopes,"resourceUris":["https://127.0.0.1:28150"]})
        for user in ("admin", "viewer"):
            token_file = consumer_root / "secrets" / (tenant + "-" + user + ".at")
            token_file.write_text("controlled-opaque-formal-input"); token_file.chmod(0o600)
            formal_tokens.append({"tenantCode":tenant,"tenantId":tenant_id,"resourceKey":"tianshu","userKey":user,
                                  "clientId":client_id,"clientPk":client_pk,"issuer":issuer,"resourceUri":"https://127.0.0.1:28150",
                                  "scopes":formal_scopes,"tokenFile":str(token_file),"probeStatus":200,
                                  "tokenSha256":hashlib.sha256(token_file.read_bytes()).hexdigest(),"expiresAt":"2099-01-01T00:00:00Z"})
    formal_registry = {"schemaVersion":1,"runId":consumer_run,"manifestSha256":consumer_digest,"publicOrigin":consumer_manifest["publicOrigin"],
                       "tenantIds":{"default":"1001","tenant-b":"1002"},"clients":formal_clients,
                       "resources":[{"resourceServerId":"resource-tianshu","resourceUri":"https://127.0.0.1:28150","status":"ACTIVE"}],
                       "cliResources":[{"resourceKey":"tianshu","resourceServerId":"resource-tianshu","clientIds":{"default":"formal-default","tenant-b":"formal-tenant-b"},"readPath":"/api/v1/tianshu/bizs"}]}
    private_json(consumer_root / "secrets/runtime-registry.json", formal_registry)
    formal_index = consumer_root / "secrets/token-index.json"
    formal_index_body = {"schemaVersion":1,"runId":consumer_run,"manifestSha256":consumer_digest,"tokens":formal_tokens}
    private_json(formal_index, formal_index_body)
    if len(runtime_module.validate_formal_token_index(consumer_manifest, consumer_digest, formal_index)) != 4:
        failures.append("formal input preflight did not bind both tenants and both users")
    unprobed=copy.deepcopy(formal_index_body)
    for value in unprobed["tokens"]: value["probeStatus"]=None
    private_json(formal_index,unprobed)
    try:
        if len(runtime_module.validate_formal_token_index(consumer_manifest,consumer_digest,formal_index))!=4:
            failures.append("AS-verified unprobed inputs were not retained as inputs only")
    except SystemExit as rejected:
        failures.append("AS-verified input required an unstarted downstream: "+str(rejected.code))
    private_json(formal_index,formal_index_body)
    for fault, expected in (("expired", 6), ("foreign tenant", 3), ("service client", 3), ("missing input", 4)):
        changed = copy.deepcopy(formal_index_body)
        changed_registry = copy.deepcopy(formal_registry)
        if fault == "expired": changed["tokens"][0]["expiresAt"] = "2000-01-01T00:00:00Z"
        elif fault == "foreign tenant": changed["tokens"][0]["tenantId"] = "1002"
        elif fault == "service client": changed_registry["clients"][0].update(clientType="CONFIDENTIAL", purpose="SERVICE")
        else: changed["tokens"].pop()
        private_json(formal_index, changed)
        private_json(consumer_root / "secrets/runtime-registry.json", changed_registry)
        try:
            runtime_module.validate_formal_token_index(consumer_manifest, consumer_digest, formal_index)
            failures.append("formal input " + fault + " was accepted")
        except SystemExit as rejected:
            if rejected.code != expected: failures.append("formal input " + fault + " returned " + str(rejected.code))
    private_json(formal_index, formal_index_body)
    private_json(consumer_root / "secrets/runtime-registry.json", formal_registry)
    # Consumer orchestration substitutes transports only; it cannot produce a real integration pass.
    (consumer_root / "profiles").mkdir(mode=0o700)
    consumer_roles = (("biz","JAVA",28180,[28182]),("mcp","JAVA",28185,[28186]),("llm","JAVA",28190,[]),
                      ("portal-web","NODE",28125,[]),("shoubing-web","NODE",28121,[]),("jianshen-web","NODE",28131,[]),
                      ("yuheng-web","NODE",28141,[]),("tianshu-web","NODE",28152,[]),
                      ("native-light","JAVA",28310,[29310]),("native-web","JAVA",28311,[29311]),
                      ("native-service","JAVA",28312,[29312]),("native-agent","JAVA",28313,[]))
    for name, kind, port, extra in consumer_roles:
        artifact = root / ("consumer-" + name + ".jar")
        artifact.write_text("controlled-consumer-" + name)
        consumer_manifest["artifacts"][name] = {"path":str(artifact),"sha256":hashlib.sha256(artifact.read_bytes()).hexdigest(),"buildId":"controlled-consumer"}
        consumer_manifest["processes"].append({"id":name,"artifactRole":name,"kind":kind,"port":port,"additionalPorts":extra})
    consumer_intent = consumer_root / "intent.json"
    private_json(consumer_intent, consumer_manifest)
    consumer_digest = hashlib.sha256(consumer_intent.read_bytes()).hexdigest()
    formal_registry["manifestSha256"] = consumer_digest
    formal_index_body["manifestSha256"] = consumer_digest
    private_json(consumer_root / "secrets/runtime-registry.json", formal_registry)
    private_json(formal_index, formal_index_body)
    consumer_record = {"schemaVersion":1,"runId":consumer_run,"manifestSha256":consumer_digest,"processes":[
        {"id":item["id"],"pid":72000+i,"startInstant":"2026-10-14T00:00:00Z","commandSha256":"e"*64,
         "artifactSha256":consumer_manifest["artifacts"][item["id"]]["sha256"]}
        for i,item in enumerate(control_manifest["processes"])]}
    private_json(consumer_root / "evidence/processes.json", consumer_record)
    profile_refs = {}
    for process in consumer_manifest["processes"]:
        if process["kind"] == "JAVA":
            file = consumer_root / "profiles" / (process["id"] + ".properties")
            file.write_text("controlled=true\n"); file.chmod(0o600)
            profile_refs[process["id"]] = {"path":str(file),"sha256":hashlib.sha256(file.read_bytes()).hexdigest()}
    private_json(consumer_root / "evidence/profiles-ready.json", {"schemaVersion":1,"runId":consumer_run,"manifestSha256":consumer_digest,"profiles":profile_refs})
    for filename, operation, state, step in (("prepared.json","CLI-001","COMMITTED","PREPARE"),("identity.json","CLI-002","COMMITTED","IDENTITY"),
                                           ("control-plane.json","CLI-003","RUNNING","CONTROL_PLANE"),
                                           ("authorization-platform.json","CLI-004","COMMITTED","PLATFORM_AUTHORIZATION"),
                                           ("authorization-native.json","CLI-004","COMMITTED","NATIVE_AUTHORIZATION")):
        private_json(consumer_root / "evidence" / filename, {"schemaVersion":1,"operationId":operation,"runId":consumer_run,"manifestSha256":consumer_digest,
                     "status":state,"completedAt":"2026-10-14T00:00:00Z","targets":[{"targetKey":p["id"],"identity":str(p["pid"]),"version":p["startInstant"],"state":"RUNNING"} for p in consumer_record["processes"]],
                     "counts":{"databaseCount":1 if operation == "CLI-002" else 9,"tenantCount":2,"clientCount":2,"catalogCount":4,"liveCaseCount":0},
                     "steps":[{"stepId":step,"state":state,"evidence":"evidence/controlled-owner.json"}],"error":None})
    consumer_calls = []
    consumer_originals = {name:getattr(runtime_module,name) for name in ("prepare_frontend_profiles","ensure_task_truststore","launch_owned_runtime","observe_owned_process","wait_owned_https","read_native_build_provenance","prepare_gateway_publication")}
    def consumer_launch(intent,digest,name,truststore,manifest_file=None):
        consumer_calls.append(("launch",name))
        return {"id":name,"pid":73000+len(consumer_calls),"startInstant":"2026-10-14T00:00:00Z","commandSha256":"a"*64,
                "artifactSha256":intent["artifacts"][name]["sha256"]}
    def consumer_wait(*arguments,**keywords):
        consumer_calls.append(("https",arguments[2],keywords))
        return {"status":"UP"}
    def consumer_frontends(*args):
        consumer_calls.append(("profiles",None))
        return {"profiles":{role:{} for role in frontend_locations}}
    runtime_module.prepare_frontend_profiles = consumer_frontends
    runtime_module.ensure_task_truststore = lambda *args: consumer_root / "secrets/controlled.p12"
    runtime_module.launch_owned_runtime = consumer_launch
    runtime_module.observe_owned_process = lambda *args: "RUNNING"
    runtime_module.wait_owned_https = consumer_wait
    runtime_module.prepare_gateway_publication = lambda *args: consumer_calls.append(("gateway-publication",None)) or {"scope":"CONTROLLED_GATEWAY_OWNER_TRANSPORT_ONLY"}
    consumer_native_receipt = (consumer_root / "evidence/authorization-native.json").read_bytes()
    try:
        original_consumer_path = os.environ.get("PATH","")
        os.environ["PATH"] = env["PATH"]
        (fake_bin / "lsof").write_text("#!/usr/bin/env bash\nexit 1\n")
        consumer_initial_files = {path:path.read_bytes() for path in consumer_root.rglob('*') if path.is_file()}
        try:
            runtime_module.start_consumers(["controlled",str(consumer_intent),str(consumer_root / "evidence/missing-build-proof.json"),str(formal_index)])
            failures.append("consumer startup accepted native JARs without current generation/build provenance")
        except SystemExit as rejected:
            if rejected.code != 4: failures.append("missing native build provenance returned wrong exit")
        if consumer_calls: failures.append("consumer startup reached transports before native build provenance")
        for path in consumer_root.rglob('*'):
            if path.is_file() and path not in consumer_initial_files: path.unlink()
        for path,content in consumer_initial_files.items(): path.write_bytes(content); path.chmod(0o600)
        consumer_calls.clear()
        # Remaining orchestration checks use substituted build and network transports only.
        runtime_module.read_native_build_provenance = lambda *args: {"scope":"CONTROLLED_BUILD_TRANSPORT_ONLY"}
        (consumer_root / "evidence/authorization-native.json").unlink()
        try:
            runtime_module.start_consumers(["controlled",str(consumer_intent),str(consumer_root / "evidence/missing-native.json"),str(formal_index)])
            failures.append("consumer startup accepted missing native authorization")
        except SystemExit as rejected:
            if rejected.code != 4: failures.append("missing native receipt returned wrong exit")
        if consumer_calls: failures.append("consumer was launched before native authorization")
        (consumer_root / "evidence/authorization-native.json").write_bytes(consumer_native_receipt)
        (consumer_root / "evidence/authorization-native.json").chmod(0o600)
        try:
            runtime_module.start_consumers(["controlled",str(consumer_intent),str(consumer_root / "evidence/consumer-result.json"),str(formal_index)])
            first_launches = len([call for call in consumer_calls if call[0] == "launch"])
            if first_launches != 12: failures.append("consumer startup did not capture twelve owned processes")
            if len(json.loads((consumer_root/'evidence/processes.json').read_text())['processes'])!=18:
                failures.append('default runtime coverage did not retain all eighteen declared processes')
            probe_urls = [call[1] for call in consumer_calls if call[0] == "https"]
            gateway_position=next((index for index,call in enumerate(consumer_calls) if call[0]=='gateway-publication'),-1)
            if gateway_position<0 or len([call for call in consumer_calls[:gateway_position] if call[0]=='launch'])!=12:
                failures.append('consumer final readiness advanced before all providers and original gateway owner publication')
            if any(call[0]=='https' and not call[1].endswith('/actuator/health/liveness') for call in consumer_calls[:gateway_position]):
                failures.append('consumer required engine readiness before the real route publication phase')
            for gateway_port in (28182,28186):
                for suffix in ("/actuator/health/liveness","/actuator/health","/actuator/health/readiness"):
                    if "https://127.0.0.1:" + str(gateway_port) + suffix not in probe_urls:
                        failures.append("gateway health probe missed its declared management port")
            if any(url.startswith(("https://127.0.0.1:28180/actuator/","https://127.0.0.1:28185/actuator/")) for url in probe_urls):
                failures.append("gateway health probe used the public business listener")
            if any(call[1] in {"shoubing","nginx","tianshu","jianshen","yuheng","edge"} for call in consumer_calls if call[0] == "launch"):
                failures.append("consumer startup restarted the existing control plane")
            runtime_module.start_consumers(["controlled",str(consumer_intent),str(consumer_root / "evidence/consumer-again.json"),str(formal_index)])
            if len([call for call in consumer_calls if call[0] == "launch"]) != first_launches:
                failures.append("consumer retry restarted an owned alive process")
            if len([call for call in consumer_calls if call[0] == "https" and call[2].get("html")]) < 5:
                failures.append("consumer startup did not require the five real HTML mounts")
            # The accepted Agent gap changes only exact runtime/profile coverage; all other roles stay required.
            consumer_saved={path:path.read_bytes() for path in consumer_root.rglob('*') if path.is_file()}
            calls_before_exclusion=list(consumer_calls)
            try:
                policies=consumer_root/'policies'; policies.mkdir(exist_ok=True); policies.chmod(0o700)
                deferred=policies/'agent-runtime-deferred.json'
                accepted={'schemaVersion':1,'runId':consumer_run,'role':'native-agent','status':'UNVERIFIED',
                          'reasonCode':'AGENT_RUNTIME_CONFIGURATION_NOT_PROVIDED','acceptedOn':'2026-10-15'}
                private_json(deferred,accepted)
                excluded_manifest=copy.deepcopy(consumer_manifest)
                policy_reference={'path':str(deferred),'sha256':hashlib.sha256(deferred.read_bytes()).hexdigest()}
                excluded_manifest['policyFiles'].append(policy_reference)
                exclusions={'native-agent':policy_reference['sha256']}
                if runtime_module.runtime_profile_exclusions(excluded_manifest)!=exclusions:
                    failures.append('Agent exclusion did not bind the accepted policy SHA')
                for field,value in (('schemaVersion',True),('runId','foreign'),('role','native-web'),('status','PASSED'),
                                    ('reasonCode','OTHER'),('acceptedOn','2026-10-16'),('unknown','secret')):
                    private_json(deferred,{**accepted,field:value}); policy_reference['sha256']=hashlib.sha256(deferred.read_bytes()).hexdigest()
                    try:
                        runtime_module.runtime_profile_exclusions(excluded_manifest); failures.append('Agent exclusion accepted invalid '+field)
                    except SystemExit as rejected:
                        if rejected.code!=3: failures.append('Agent exclusion '+field+' returned wrong exit')
                private_json(deferred,accepted);policy_reference['sha256']=exclusions['native-agent']
                for policy_files in ([{**policy_reference,'sha256':'0'*64}],[policy_reference,policy_reference],
                                     [{**policy_reference,'path':str(consumer_root/'foreign/agent-runtime-deferred.json')}]):
                    wrong=copy.deepcopy(excluded_manifest);wrong['policyFiles']=policy_files
                    try:
                        runtime_module.runtime_profile_exclusions(wrong);failures.append('Agent exclusion accepted wrong hash/path/duplicate')
                    except SystemExit as rejected:
                        if rejected.code!=3:failures.append('Agent policy binding returned wrong exit')
                private_json(consumer_intent,excluded_manifest);excluded_digest=hashlib.sha256(consumer_intent.read_bytes()).hexdigest()
                for path in (consumer_root/'evidence').glob('*.json'):
                    value=json.loads(path.read_text())
                    if isinstance(value,dict) and 'manifestSha256' in value:
                        value['manifestSha256']=excluded_digest;private_json(path,value)
                for path in (consumer_root/'secrets/runtime-registry.json',formal_index):
                    value=json.loads(path.read_text());value['manifestSha256']=excluded_digest;private_json(path,value)
                excluded_record=copy.deepcopy(consumer_record);excluded_record['manifestSha256']=excluded_digest
                private_json(consumer_root/'evidence/processes.json',excluded_record)
                eleven_profiles={key:value for key,value in profile_refs.items() if key!='native-agent'}
                excluded_profiles={'schemaVersion':1,'runId':consumer_run,'manifestSha256':excluded_digest,'profiles':eleven_profiles,'runtimeExclusions':exclusions}
                private_json(consumer_root/'evidence/profiles-ready.json',excluded_profiles)
                consumer_calls.clear()
                runtime_module.start_consumers(['controlled',str(consumer_intent),str(consumer_root/'evidence/agent-deferred-consumers.json'),str(formal_index)])
                owned=json.loads((consumer_root/'evidence/processes.json').read_text())['processes']
                if len(owned)!=17 or any(value['id']=='native-agent' for value in owned) or len([call for call in consumer_calls if call[0]=='launch'])!=11:
                    failures.append('Agent exclusion did not produce exactly seventeen owned processes and eleven consumer launches')
                for fault in ('missing-other-profile','missing-exclusion-hash','agent-still-profiled'):
                    wrong=copy.deepcopy(excluded_profiles)
                    if fault=='missing-other-profile':wrong['profiles'].pop('native-web')
                    elif fault=='missing-exclusion-hash':wrong['runtimeExclusions']={}
                    else:wrong['profiles']['native-agent']=profile_refs['native-agent']
                    private_json(consumer_root/'evidence/profiles-ready.json',wrong)
                    try:
                        runtime_module.start_consumers(['controlled',str(consumer_intent),str(consumer_root/'evidence'/('excluded-'+fault+'.json')),str(formal_index)])
                        failures.append('Agent exclusion relaxed exact profile coverage for '+fault)
                    except SystemExit as rejected:
                        if rejected.code!=3:failures.append('Agent profile coverage '+fault+' returned wrong exit')
            finally:
                for path in consumer_root.rglob('*'):
                    if path.is_file() and path not in consumer_saved:path.unlink()
                for path,value in consumer_saved.items():path.write_bytes(value);path.chmod(0o600)
                consumer_calls[:]=calls_before_exclusion
            def consumer_not_ready(*arguments,**keywords):
                raise SystemExit(6)
            runtime_module.wait_owned_https = consumer_not_ready
            try:
                runtime_module.start_consumers(["controlled",str(consumer_intent),str(consumer_root / "evidence/consumer-unready.json"),str(formal_index)])
                failures.append("consumer unready transport was reported as RUNNING")
            except SystemExit as unavailable:
                if unavailable.code != 6: failures.append("consumer unready transport returned wrong exit")
            if len([call for call in consumer_calls if call[0] == "launch"]) != first_launches:
                failures.append("consumer readiness failure restarted a live process")
            if (consumer_root / "evidence/consumer-unready.json").exists():
                failures.append("consumer readiness failure published a success result")
        except SystemExit as rejected:
            failures.append("consumer lifecycle source is incomplete: " + str(rejected.code))
    finally:
        os.environ["PATH"] = original_consumer_path
        for name,value in consumer_originals.items(): setattr(runtime_module,name,value)
    lifecycle_root = root / ('target/xingyuan-integration-' + str(uuid.uuid4()))
    for folder in (lifecycle_root, lifecycle_root/'evidence', lifecycle_root/'secrets', lifecycle_root/'policies'):
        folder.mkdir(parents=True, exist_ok=True, mode=0o700); folder.chmod(0o700)
    lifecycle_manifest = copy.deepcopy(consumer_manifest)
    lifecycle_manifest.update(runtimeRoot=str(lifecycle_root), runId=lifecycle_root.name.removeprefix('xingyuan-integration-'))
    lifecycle_key=lifecycle_root/'secrets/owned.key'; lifecycle_key.write_bytes(b'k'*32); lifecycle_key.chmod(0o600)
    lifecycle_manifest['secretFiles']={'oauth-kek':str(lifecycle_key)}
    lifecycle_manifest['policyFiles']=[]
    lifecycle_file=lifecycle_root/'intent.json';private_json(lifecycle_file,lifecycle_manifest)
    lifecycle_digest=runtime_module.task_artifact_sha256(lifecycle_file)
    lifecycle_record=copy.deepcopy(consumer_record)
    lifecycle_record.update(runId=lifecycle_manifest['runId'],manifestSha256=lifecycle_digest)
    lifecycle_record['processes']=[{'id':item['id'],'pid':80000+index,'startInstant':'2026-10-15T00:00:00Z','commandSha256':'a'*64,
                                  'artifactSha256':lifecycle_manifest['artifacts'][item['id']]['sha256']} for index,item in enumerate(lifecycle_manifest['processes'])]
    private_json(lifecycle_root/'evidence/processes.json',lifecycle_record)
    for filename in ('original-databases.json','original-nginx.json','original-redis.json'):
        private_json(lifecycle_root/'evidence'/filename,{'scope':'CONTROLLED_ORIGINAL_TRANSPORT_ONLY'})
    for item in lifecycle_manifest['databases']:
        private_json(lifecycle_root/'evidence'/('db-'+item['role']+'.json'),{'scope':'CONTROLLED_RECEIPT_BYTES_ONLY'})
    lifecycle_functions={name:getattr(runtime_module,name) for name in ('process_state','observe_owned_process','observe_owned_database_metadata',
                         'read_original_database_baseline','read_original_nginx_baseline','read_original_redis_baseline',
                         'verify_original_databases','verify_original_nginx','verify_original_redis')}
    lifecycle_state={'running':True,'stops':0,'damage':False}
    def lifecycle_stop(arguments):
        if arguments[1]!='stop': raise AssertionError('lifecycle selected an unreviewed process operation')
        lifecycle_state['stops']+=1;lifecycle_state['running']=False
        private_json(Path(arguments[3]),{'schemaVersion':1,'operationId':'CLI-009','runId':lifecycle_manifest['runId'],
                     'manifestSha256':lifecycle_digest,'status':'STOPPED','error':None,
                     'targets':[{'targetKey':item['id'],'state':'STOPPED'} for item in lifecycle_record['processes']]})
        if lifecycle_state['damage']: lifecycle_key.write_bytes(b'x'*32)
    def lifecycle_preserved(kind):
        def observe(intent,digest,attempt):
            private_json(Path(attempt)/('original-'+kind+'-preserved.json'),{'scope':'CONTROLLED_PRESERVATION_TRANSPORT_ONLY'})
        return observe
    try:
        runtime_module.process_state=lifecycle_stop
        runtime_module.observe_owned_process=lambda *args:'RUNNING' if lifecycle_state['running'] else 'STOPPED'
        runtime_module.observe_owned_database_metadata=lambda *args:{'controlled-new-database':{'oid':71}}
        for name in ('read_original_database_baseline','read_original_nginx_baseline','read_original_redis_baseline'):
            setattr(runtime_module,name,lambda *args:{})
        for kind in ('databases','nginx','redis'):
            setattr(runtime_module,'verify_original_'+kind,lifecycle_preserved(kind))
        for stopped in (False,True):
            observed_attempt=lifecycle_root/'evidence'/('verify-'+str(uuid.uuid4())); observed_attempt.mkdir(mode=0o700)
            private_json(observed_attempt/'intent.json',{'runId':lifecycle_manifest['runId'],'manifestSha256':lifecycle_digest,'caseSet':'full'})
            lifecycle_state.update(running=not stopped,stops=0,damage=False)
            observed_result=observed_attempt/'resource-live-guards.json'
            try:
                runtime_module.observe_live_guards(['controlled',str(lifecycle_file),str(observed_result)])
                observed=json.loads(observed_result.read_text())
                if stopped or observed.get('scope')!='OWNED_RUNNING_COHORT_AND_ORIGINAL_FACILITY_GUARDS_ONLY' or observed.get('status')!='PASSED' or len(observed.get('proofs',[]))!=3:
                    failures.append('live guard failed to bind its three controlled facility observations')
            except SystemExit as rejected:
                if not stopped or rejected.code!=6 or observed_result.exists():failures.append('live guard accepted stopped/foreign cohort or wrong result')
            if lifecycle_state['stops']!=0:failures.append('read-only live guard stopped an owned process')
        for fault in ('missing-cases','retained','changed-secret'):
            attempt=lifecycle_root/'evidence'/('verify-'+str(uuid.uuid4()));attempt.mkdir(mode=0o700)
            private_json(attempt/'intent.json',{'runId':lifecycle_manifest['runId'],'manifestSha256':lifecycle_digest,'caseSet':'full'})
            private_json(attempt/'controlled-domain.json',{'scope':'CONTROLLED_CASE_TRANSPORT_ONLY'})
            proof={'path':str((attempt/'controlled-domain.json').relative_to(lifecycle_root)),'sha256':runtime_module.task_artifact_sha256(attempt/'controlled-domain.json')}
            private_json(attempt/'junit-native.json',{'classname':'top.egon.cola.component.yuheng.test.integration.XingyuanNativeAuthenticationIT',
                         'methods':['verifyNativeAuthentication'],'testCount':1,'reportSha256':'a'*64,'startedNs':time.time_ns()-1000000000})
            if fault!='missing-cases':
                for number in range(1,25):
                    if number==23:continue
                    runner='native' if 11<=number<=15 else 'resource'
                    folder=attempt/'cases'/runner;folder.mkdir(parents=True,exist_ok=True,mode=0o700);folder.parent.chmod(0o700)
                    case_id='LIVE-'+str(number).zfill(3)
                    private_json(folder/(case_id+'.json'),{'schemaVersion':1,'runId':lifecycle_manifest['runId'],'manifestSha256':lifecycle_digest,
                                 'caseId':case_id,'runner':runner,'status':'PASSED','executedAt':datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00','Z'),
                                 'testClass':'top.egon.cola.component.yuheng.test.integration.'+('XingyuanNativeAuthenticationIT' if runner=='native' else 'XingyuanResourceAuthorizationIT'),
                                 'testMethod':'verifyNativeAuthentication' if runner=='native' else 'verifyResourceAuthorization','proofs':[proof]})
            lifecycle_state.update(running=True,stops=0,damage=fault=='changed-secret')
            target=attempt/'resource-owned-stop-preservation.json'
            try:
                runtime_module.stop_owned_and_observe_preservation(['controlled',str(lifecycle_file),str(target)])
                if fault!='retained':failures.append('owned stop accepted '+fault)
                else:
                    observed=json.loads(target.read_text())
                    if observed['retainedCaseCount']!=23 or observed['ownedProcessCount']!=18 or observed['retainedSecretFileCount']!=1:
                        failures.append('owned stop lost its exact retained cohort/file facts')
            except SystemExit as rejected:
                if fault=='retained' or rejected.code!=(4 if fault=='missing-cases' else 3):failures.append('owned stop returned wrong '+fault+' code '+str(rejected.code))
            if lifecycle_state['stops']!=(0 if fault=='missing-cases' else 1):failures.append('owned stop crossed its case preflight boundary')
            if (lifecycle_root/'evidence/processes.json').read_bytes()!=json.dumps(lifecycle_record).encode():failures.append('owned stop replaced the frozen process record')
            lifecycle_key.write_bytes(b'k'*32)
    finally:
        for name,function in lifecycle_functions.items():setattr(runtime_module,name,function)
    print('PASS owned-stop case/file retention guards with substituted process/facility transports; no real stop')
    metadata_manifest=copy.deepcopy(lifecycle_manifest)
    pg_credential=lifecycle_root/'secrets/metadata-password';pg_credential.write_text('controlled-metadata-password');pg_credential.chmod(0o600)
    metadata_manifest['postgres']['credentialFile']=str(pg_credential)
    metadata_rows=[]
    for index,item in enumerate(metadata_manifest['databases']):
        row={'databaseName':item['name'],'oid':1001+index,'databaseOwner':'controlled-owner',
             'ownerComment':'xingyuan-it:'+metadata_manifest['runId']+':'+lifecycle_digest}
        metadata_rows.append(row)
        private_json(lifecycle_root/'evidence'/('db-'+item['role']+'.json'),dict(row,role=item['role'],runId=metadata_manifest['runId'],manifestSha256=lifecycle_digest))
    metadata_calls=[];metadata_run=subprocess.run
    def metadata_psql_only(arguments,**keywords):
        if arguments[0]!='psql' or not keywords['input'].startswith('SELECT ') or any(word in keywords['input'] for word in ('DROP ','UPDATE ','DELETE ')):
            raise AssertionError('task database observer selected a mutation or an unexpected process')
        metadata_calls.append(arguments)
        return type('MetadataResult',(),{'returncode':0,'stdout':json.dumps(metadata_rows)})()
    try:
        subprocess.run=metadata_psql_only
        observed=runtime_module.observe_owned_database_metadata(metadata_manifest,lifecycle_digest)
        if len(observed)!=9:failures.append('task metadata observer omitted a prepared database')
        metadata_original=copy.deepcopy(metadata_rows)
        for fault in ('missing','foreign','oid','owner','comment','duplicate','boolean'):
            metadata_rows[:]=copy.deepcopy(metadata_original)
            if fault=='missing':metadata_rows.pop()
            elif fault=='foreign':metadata_rows[0]['databaseName']='original_user_database'
            elif fault=='oid':metadata_rows[0]['oid']+=100
            elif fault=='owner':metadata_rows[0]['databaseOwner']='other-owner'
            elif fault=='comment':metadata_rows[0]['ownerComment']='foreign-task'
            elif fault=='duplicate':metadata_rows[-1]=metadata_rows[0]
            else:metadata_rows[0]['oid']=True
            try:
                runtime_module.observe_owned_database_metadata(metadata_manifest,lifecycle_digest)
                failures.append('task metadata observer accepted '+fault)
            except SystemExit as rejected:
                if rejected.code!=3:failures.append('task metadata observer returned wrong '+fault+' exit')
        before=len(metadata_calls)
        foreign=copy.deepcopy(metadata_manifest);foreign['databases'][0]['name']='egon_cola'
        try:
            runtime_module.observe_owned_database_metadata(foreign,lifecycle_digest)
            failures.append('task metadata observer accepted a foreign target before SQL')
        except SystemExit as rejected:
            if rejected.code!=3:failures.append('task metadata target preflight returned wrong exit')
        if len(metadata_calls)!=before:failures.append('foreign task metadata target reached SQL')
    finally:
        subprocess.run=metadata_run
    print('PASS exact task database OID/owner/comment/receipt and SELECT-only guards; no PostgreSQL connection')
    # Exercise the actual health helper with only TLS/HTTP transports substituted; no listener/socket.
    import email.message, urllib.error
    ca_file = Path(nginx_secrets["tls-ca-cert"])
    ca_file.write_text("controlled-ca-transport-only"); ca_file.chmod(0o600)
    probe_manifest = dict(control_manifest, secretFiles=nginx_secrets)
    class ProbeResponse:
        status = 200
        def __init__(self,url,data,media="application/json"):
            self.url,self.data = url,data
            self.headers = email.message.Message(); self.headers["Content-Type"] = media
        def __enter__(self): return self
        def __exit__(self,*args): return False
        def geturl(self): return self.url
        def read(self,*args): return self.data
    probe_url = "https://127.0.0.1:28120/actuator/health/readiness"
    probe_opener = SimpleNamespace(open=None)
    from unittest.mock import Mock
    probe_opener.open = Mock(side_effect=[urllib.error.HTTPError(probe_url,401,"controlled",None,None),
                                          ProbeResponse(probe_url,b'{"status":"DOWN"}'),ProbeResponse(probe_url,b'{"status":"UP"}')])
    tls_transport = SimpleNamespace(load_cert_chain=Mock())
    with patch("ssl.create_default_context",return_value=tls_transport), patch("urllib.request.build_opener",return_value=probe_opener), \
         patch.object(runtime_module,"observe_owned_process",return_value="RUNNING"), patch("time.sleep",return_value=None):
        status = runtime_module.wait_owned_https(probe_manifest,{"id":"shoubing"},probe_url,ready=True)
        if status != {"status":"UP"} or probe_opener.open.call_count != 3 or tls_transport.load_cert_chain.call_count != 1:
            failures.append("HTTP401 or DOWN was accepted as real readiness UP")
        html_url = "https://127.0.0.1:28443/apps/yuheng/"
        probe_opener.open = Mock(return_value=ProbeResponse(html_url,b'<html>login</html>',"text/html"))
        try:
            runtime_module.wait_owned_https(probe_manifest,{"id":"nginx"},html_url,html=True)
            failures.append("foreign login HTML was accepted as a Vite mount")
        except SystemExit as rejected:
            if rejected.code != 8: failures.append("foreign HTML returned wrong rejection")
        probe_opener.open = Mock(return_value=ProbeResponse(html_url,b'<html><script src="/apps/yuheng/@vite/client"></script></html>',"text/html"))
        runtime_module.wait_owned_https(probe_manifest,{"id":"nginx"},html_url,html=True)
        if tls_transport.load_cert_chain.call_count != 1:
            failures.append("public origin received the task monitor client certificate")
        management_manifest=dict(probe_manifest,processes=[*probe_manifest['processes'],
                                 {'id':'biz','port':28180,'additionalPorts':[28182]},
                                 {'id':'mcp','port':28185,'additionalPorts':[28186]}])
        for role,port in (('biz',28182),('mcp',28186)):
            url='https://127.0.0.1:'+str(port)+'/actuator/health/readiness'
            probe_opener.open=Mock(return_value=ProbeResponse(url,b'{"status":"UP"}'))
            assert runtime_module.wait_owned_https(management_manifest,{'id':role},url,ready=True)=={'status':'UP'}
            assert probe_opener.open.call_count==1
        for port in (28122,28183):
            probe_opener.open=Mock()
            try:
                runtime_module.wait_owned_https(management_manifest,{'id':'biz'},'https://127.0.0.1:'+str(port)+'/actuator/health/readiness',ready=True)
                failures.append('HTTPS observer accepted an undeclared or RPC-only port')
            except SystemExit as rejected:
                if rejected.code!=3: failures.append('undeclared HTTPS management port returned wrong rejection')
            assert probe_opener.open.call_count==0
    # Fresh XML evidence is mandatory; stale, skipped or zero-selected reports never prove a live run.
    import time
    junit_file = root / "controlled-junit.xml"
    junit_class = "controlled.OwnedLiveIT"
    junit_methods = {"actualFirst", "actualSecond"}
    def junit_document(skipped=False, duplicate=False, missing=False):
        names = ["actualFirst", "actualFirst" if duplicate else "actualSecond"]
        if missing: names.pop()
        cases = ''.join('<testcase classname="' + junit_class + '" name="' + name + '">' + ('<skipped/>' if skipped else '') + '</testcase>' for name in names)
        return '<testsuite name="' + junit_class + '" tests="' + str(len(names)) + '" failures="0" errors="0" skipped="' + ('2' if skipped else '0') + '">' + cases + '</testsuite>'
    junit_file.write_text(junit_document())
    started_ns = time.time_ns() - 1_000_000_000
    proof = runtime_module.read_fresh_junit_report(junit_file, junit_class, junit_methods, started_ns)
    if proof.get("methods") != sorted(junit_methods): failures.append("fresh actual test methods were not captured")
    for fault in ("skip", "duplicate", "missing", "stale", "entity"):
        junit_file.write_text(junit_document(skipped=fault=="skip", duplicate=fault=="duplicate", missing=fault=="missing"))
        if fault == "entity": junit_file.write_text('<!DOCTYPE x [<!ENTITY x "unsafe">]>' + junit_document())
        if fault == "stale": os.utime(junit_file, ns=(started_ns-10_000_000_000,started_ns-10_000_000_000))
        try:
            runtime_module.read_fresh_junit_report(junit_file, junit_class, junit_methods, started_ns)
            failures.append("JUnit " + fault + " report was accepted")
        except SystemExit as rejected:
            if rejected.code != 8: failures.append("JUnit " + fault + " returned wrong verification exit")
    # Validation command builders are file-only; a short Java source-mode probe checks real @argfile parsing.
    verification_intent = copy.deepcopy(consumer_manifest)
    verification_intent["secretFiles"] = {"tls-ca-cert":str(consumer_root / "secrets/verification-ca.pem"),
                                          "tls-keystore-password":str(consumer_root / "secrets/verification-store.password")}
    store_password = 'controlled store "quoted" \"password'
    Path(verification_intent["secretFiles"]["tls-keystore-password"]).write_text(store_password)
    Path(verification_intent["secretFiles"]["tls-keystore-password"]).chmod(0o600)
    truststore = consumer_root / "secrets/java-truststore.p12"
    truststore.write_bytes(b"controlled-argument-reference-only"); truststore.chmod(0o600)
    command_manifest = consumer_root / "verification-intent.json"
    private_json(command_manifest,verification_intent)
    verification_digest = hashlib.sha256(command_manifest.read_bytes()).hexdigest()
    timing_registry={"schemaVersion":1,"runId":consumer_run,"manifestSha256":verification_digest,"clients":[
        {"realmCode":realm,"alias":alias,"accessTokenTtlSeconds":300}
        for realm in ("default","tenant-b") for alias in ("edge-ui","edge-shoubing","edge-jianshen","edge-yuheng","edge-tianshu")]}
    if runtime_module.verification_time_plan(verification_intent,verification_digest,timing_registry)["bffRefreshWaitSeconds"]!=482:
        failures.append("verification timing did not use the real four-business-family refresh windows")
    impossible=copy.deepcopy(timing_registry)
    for item in impossible["clients"]: item["accessTokenTtlSeconds"]=600
    try:
        runtime_module.verification_time_plan(verification_intent,verification_digest,impossible)
        failures.append("impossible A/B BFF wait was accepted inside the 900-second operation")
    except SystemExit as rejected:
        if rejected.code!=2: failures.append("impossible time plan returned wrong input exit")
    for fault,expected in (("duplicate",3),("missing",3),("boolean ttl",2),("foreign run",3)):
        changed=copy.deepcopy(timing_registry)
        if fault=="duplicate": changed["clients"].append(copy.deepcopy(changed["clients"][1]))
        elif fault=="missing": changed["clients"].pop(1)
        elif fault=="boolean ttl": changed["clients"][1]["accessTokenTtlSeconds"]=True
        else: changed["runId"]=str(uuid.uuid4())
        try:
            runtime_module.verification_time_plan(verification_intent,verification_digest,changed)
            failures.append("verification time plan accepted "+fault)
        except SystemExit as rejected:
            if rejected.code!=expected: failures.append("time plan "+fault+" returned wrong exit")
    for role in ("idp","rbac"):
        private_json(consumer_root / "evidence" / ("db-"+role+".json"),{"runId":consumer_run,"manifestSha256":verification_digest,
                     "role":role,"databaseName":"xingyuan_it_"+role+"_20261011","oid":51 if role=="idp" else 52})
    attempt = consumer_root / "evidence" / ("verify-"+str(uuid.uuid4()))
    attempt.mkdir(mode=0o700)
    with patch.dict(os.environ,{"JAVA_TOOL_OPTIONS":"-Dforeign=true","MAVEN_ARGS":"-DskipTests=true","NODE_OPTIONS":"--require /foreign"}):
        oracle_command = runtime_module.verification_runner_command(command_manifest,verification_intent,verification_digest,attempt,"oracle",truststore)
    if ("-Dtest=IsolatedOAuthProvisioningIT#observeBffFamilies" not in oracle_command["argv"]
            or "-DskipTests=false" not in oracle_command["argv"] or "-Dmaven.test.skip=false" not in oracle_command["argv"]
            or any(key in oracle_command["environment"] for key in ("JAVA_TOOL_OPTIONS","MAVEN_ARGS","NODE_OPTIONS"))
            or store_password in " ".join(oracle_command["argv"]) or store_password in str(oracle_command["environment"])):
        failures.append("oracle command leaked a credential or accepted ambient execution overrides")
    outbox_command = runtime_module.verification_runner_command(command_manifest,verification_intent,verification_digest,attempt,"outbox",truststore)
    if "-Dtest=OutboxTransactionRollbackIT#jpaRoleResourceAndMpOutbox*" not in outbox_command["argv"]:
        failures.append("full verification did not select the three planned PG producer methods")
    code_command=runtime_module.formal_input_command(command_manifest,verification_intent,verification_digest,attempt,0,("default","controlled-resource","admin"))
    if ("authorize-cli" not in code_command["argv"] or code_command["stage"]!="formal-input-000"
            or not Path(code_command["output"]).is_relative_to(consumer_root/"secrets")):
        failures.append("formal Code reacquisition did not use the fixed original CLI006 operation")
    try:
        runtime_module.formal_input_command(command_manifest,verification_intent,verification_digest,attempt,0,("default","controlled-resource","service"))
        failures.append("formal Code reacquisition accepted a machine identity as a USER input")
    except SystemExit as rejected:
        if rejected.code!=3: failures.append("formal Code identity returned wrong unsafe exit")
    try:
        cache_command = runtime_module.verification_runner_command(command_manifest,verification_intent,verification_digest,attempt,"service-cache",truststore)
        if "-Dtest=XingyuanNativeAuthenticationIT#configuredNativeServiceClientsRenewThroughTheRealAsWithSingleFlight" not in cache_command["argv"]:
            failures.append("SERVICE cache observation did not select its actual opt-in case")
    except SystemExit:
        failures.append("SERVICE cache observation has no fixed parallel runner")
    audit_command=runtime_module.verification_runner_command(command_manifest,verification_intent,verification_digest,attempt,'platform-http-audit',truststore)
    if '-Dtest=IsolatedOAuthProvisioningIT#observePlatformHttpAudits' not in audit_command['argv']:
        failures.append('platform HTTP audit bridge selected a replacement owner')
    gateway_command=runtime_module.verification_runner_command(command_manifest,verification_intent,verification_digest,consumer_root/'evidence','gateway-owner',truststore)
    if '-Dtest=XingyuanResourceAuthorizationIT#publishTaskGatewayGroupAndProtectedReadRouteThroughOriginalOwners' not in gateway_command['argv']:
        failures.append('gateway publication did not use the fixed original owner API test')
    mcp_command=runtime_module.verification_runner_command(command_manifest,verification_intent,verification_digest,attempt,'mcp-owner',truststore)
    if '-Dtest=IsolatedAuthorizationProvisioningIT#provisionTaskMcpAuthorization' not in mcp_command['argv'] or mcp_command['module'] != 'egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin':
        failures.append('MCP authorization selected a replacement authority owner')
    mcp_audit_command=runtime_module.verification_runner_command(command_manifest,verification_intent,verification_digest,attempt,'mcp-audit',truststore)
    if '-Dtest=IsolatedOAuthProvisioningIT#observeMcpHttpAudits' not in mcp_audit_command['argv']:
        failures.append('MCP audit selected a replacement signed receipt observer')
    try:
        revoke_command = runtime_module.verification_runner_command(command_manifest,verification_intent,verification_digest,attempt,"formal-revoke",truststore,"a"*64)
        if ("-Dtest=IsolatedOAuthProvisioningIT#revokeFormalFamily" not in revoke_command["argv"]
                or "-Dxingyuan.isolated.revocation-token-sha256="+"a"*64 not in revoke_command["argv"]):
            failures.append("formal revocation did not select the exact owner and token digest")
    except (SystemExit, TypeError):
        failures.append("formal family owner has no bounded fixed command bridge")
    service_command=runtime_module.verification_runner_command(command_manifest,verification_intent,verification_digest,attempt,'service-revoke',truststore,'a'*64)
    if '-Dtest=IsolatedOAuthProvisioningIT#revokeServiceAccessToken' not in service_command['argv'] or '-Dxingyuan.isolated.revocation-token-sha256='+'a'*64 not in service_command['argv']:
        failures.append('SERVICE revocation did not select the original access-token owner')
    for invalid_digest in (None, "a"*63, "A"*64, "../foreign"):
        try:
            runtime_module.verification_runner_command(command_manifest,verification_intent,verification_digest,attempt,"formal-revoke",truststore,invalid_digest)
            failures.append("formal revocation accepted an invalid token digest")
        except SystemExit as rejected:
            if rejected.code != 2: failures.append("formal revocation digest returned wrong rejection")
    try:
        runtime_module.verification_runner_command(command_manifest,verification_intent,verification_digest,attempt,"arbitrary-selector",truststore)
        failures.append("verification accepted an arbitrary Maven selector")
    except SystemExit as rejected:
        if rejected.code != 2: failures.append("unknown verification selector returned wrong exit")
    argument_probe = attempt / "VerifyArguments.java"
    argument_probe.write_text('class VerifyArguments { public static void main(String[] args) { if (!'
                              +json.dumps(store_password)+'.equals(System.getProperty("javax.net.ssl.trustStorePassword"))) System.exit(8); }}')
    argument_probe.chmod(0o600)
    parsed = subprocess.run(["java","@"+str(attempt / "verification.jvm.args"),str(argument_probe)],cwd=attempt,
                            env=runtime_module.isolated_runtime_environment(),capture_output=True,text=True,timeout=30)
    if parsed.returncode: failures.append("real JDK did not preserve the opaque JVM argument-file value")
    # The actual private adapter is exercised with a controlled AS owner, never a database or application.
    import datetime
    bridge_token={"tokenSha256":"a"*64,"clientPk":"51","tenantId":"101","issuer":"https://127.0.0.1:28443/tenants/101",
                  "resourceUri":"urn:controlled:resource"}
    bridge_state={"calls":0,"fault":None}
    bridge_class="top.egon.cola.platform.tianquan.shoubing.admin.integration.IsolatedOAuthProvisioningIT"
    class ControlledFamilyProcess:
        def wait(self,timeout=None): return 0
        def poll(self): return 0
    def controlled_family_start(value,checksum,directory,stage,command,owned_children=None):
        bridge_state["calls"]+=1
        started=time.time_ns()
        request_file=directory/("formal-revocation-request-"+"a"*64+".json")
        receipt=json.loads(request_file.read_text())
        receipt.update(status="CONFIRMED",denyApplied=True,recordedAt=datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00','Z'))
        if bridge_state["fault"]=="foreign confirmation": receipt["clientPk"]="999"
        private_json(directory/("formal-revocation-"+"a"*64+".json"),receipt)
        report=Path(value["repoRoot"])/command["module"]/"target/surefire-reports"/("TEST-"+bridge_class+".xml")
        report.parent.mkdir(parents=True,exist_ok=True)
        report.write_text('<testsuite name="'+bridge_class+'" tests="1" failures="0" errors="0" skipped="'+('1' if bridge_state["fault"]=="skipped owner" else '0')+'">'
                          +'<testcase classname="'+bridge_class+'" name="revokeFormalFamily">'+('<skipped/>' if bridge_state["fault"]=="skipped owner" else '')+'</testcase></testsuite>')
        entry={"process":ControlledFamilyProcess(),"startedNs":started}
        if owned_children is not None: owned_children.append(entry)
        return entry
    with patch.object(runtime_module,"validate_formal_token_index",return_value={("default","controlled","admin"):bridge_token}), \
         patch.object(runtime_module,"ensure_task_truststore",return_value=truststore), \
         patch.object(runtime_module,"start_verification_process",side_effect=controlled_family_start), \
         patch.object(runtime_module,"capture_verification_process"), patch.object(runtime_module,"stop_verification_process"):
        for fault,expected in ((None,None),("foreign request",3),("foreign confirmation",8),("skipped owner",8)):
            bridge_attempt=consumer_root/"evidence"/("verify-"+str(uuid.uuid4()))
            bridge_attempt.mkdir(mode=0o700)
            bridge_state["fault"]=fault
            request={"schemaVersion":1,"runId":consumer_run,"manifestSha256":verification_digest,"status":"READY","baselineStatus":200,
                     **bridge_token,"authorizationReference":"b"*64,"recordedAt":datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00','Z')}
            if fault=="foreign request": request["clientPk"]="999"
            private_json(bridge_attempt/("formal-revocation-request-"+"a"*64+".json"),request)
            calls_before=bridge_state["calls"]
            try:
                runtime_module.revoke_formal_family(["controlled",str(command_manifest),str(bridge_attempt/("formal-revocation-"+"a"*64+".json")),"a"*64,str(verification_intent["repoRoot"])])
                if expected is not None: failures.append("formal bridge accepted "+str(fault))
            except SystemExit as rejected:
                if rejected.code!=expected: failures.append("formal bridge "+str(fault)+" returned wrong exit "+str(rejected.code))
            if fault=="foreign request" and bridge_state["calls"]!=calls_before:
                failures.append("foreign formal request reached the owner")
    # Gateway API/JUnit production is substituted here; this is not a route publication or runtime case.
    gateway_class='top.egon.cola.component.yuheng.test.integration.XingyuanResourceAuthorizationIT'
    gateway_module=root/'egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-suite'
    gateway_source=gateway_module/'src/test/java'/Path(*gateway_class.split('.')).with_suffix('.java')
    gateway_source.parent.mkdir(parents=True,exist_ok=True)
    gateway_source_original=gateway_source.read_bytes() if gateway_source.exists() else None
    gateway_marker=b'class ControlledGatewaySource { void publishTaskGatewayGroupAndProtectedReadRouteThroughOriginalOwners() {} }'
    gateway_source.write_bytes(gateway_marker)
    gateway_report=gateway_module/'target/surefire-reports'/('TEST-'+gateway_class+'.xml');gateway_report.parent.mkdir(parents=True,exist_ok=True)
    gateway_state={'fault':None,'calls':0}
    class ControlledGatewayProcess:
        def wait(self,timeout=None):return 1 if gateway_state['fault']=='unknown' else 0
        def poll(self):return 0
    def controlled_gateway_start(intent,digest,directory,stage,command,owned_children=None):
        assert stage.startswith('gateway-owner-') and len(stage)==len('gateway-owner-')+32
        assert '-Dtest=XingyuanResourceAuthorizationIT#publishTaskGatewayGroupAndProtectedReadRouteThroughOriginalOwners' in command['argv']
        gateway_state['calls']+=1;started=time.time_ns()
        if gateway_state['fault']=='unknown':private_json(directory/'gateway-publication-running.json',{'status':'RUNNING'})
        else:
            private_json(directory/'gateway-publication.json',{'schemaVersion':1,'runId':intent['runId'],
                         'manifestSha256':'foreign' if gateway_state['fault']=='foreign' else digest,
                         'scope':'ORIGINAL_TASK_GATEWAY_OWNER_PUBLICATION_ONLY','status':'COMMITTED'})
        if gateway_state['fault']!='stale':
            skipped=gateway_state['fault']=='skip'
            gateway_report.write_text('<testsuite name="'+gateway_class+'" tests="1" failures="0" errors="0" skipped="'+('1' if skipped else '0')+'">'
                                      +'<testcase classname="'+gateway_class+'" name="publishTaskGatewayGroupAndProtectedReadRouteThroughOriginalOwners">'
                                      +('<skipped/>' if skipped else '')+'</testcase></testsuite>')
        if gateway_state['fault']=='source':gateway_source.write_bytes(gateway_marker+b'// changed')
        entry={'process':ControlledGatewayProcess(),'startedNs':started}
        if owned_children is not None:owned_children.append(entry)
        return entry
    gateway_ready=consumer_root/'evidence/gateway-publication.json';gateway_pending=consumer_root/'evidence/gateway-publication-running.json'
    try:
        with patch.object(runtime_module,'start_verification_process',side_effect=controlled_gateway_start), \
             patch.object(runtime_module,'capture_verification_process'),patch.object(runtime_module,'stop_verification_process'):
            for fault,expected in ((None,None),('skip',8),('foreign',8),('source',8),('stale',8),('unknown',9)):
                gateway_source.write_bytes(gateway_marker);gateway_ready.unlink(missing_ok=True);gateway_pending.unlink(missing_ok=True)
                gateway_state['fault']=fault
                try:
                    observed=runtime_module.prepare_gateway_publication(command_manifest,verification_intent,verification_digest,truststore)
                    if expected is not None:failures.append('gateway owner bridge accepted '+str(fault))
                    elif observed['scope']!='ORIGINAL_GATEWAY_OWNER_EXECUTION_ONLY' or observed['execution']['testCount']!=1:
                        failures.append('gateway owner bridge lost its actual fixed execution binding')
                except SystemExit as rejected:
                    if rejected.code!=expected:failures.append('gateway owner bridge returned wrong '+str(fault)+' exit '+str(rejected.code))
    finally:
        gateway_ready.unlink(missing_ok=True);gateway_pending.unlink(missing_ok=True)
        if gateway_source_original is None:gateway_source.unlink()
        else:gateway_source.write_bytes(gateway_source_original)
    print('PASS fixed gateway owner bridge fresh-execution/source/unknown guards; simulated API/JUnit transport only')
    # The actual private audit bridge observes only a substituted owner/report transport, never a database.
    audit_owner_source=root/'egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/integration/IsolatedOAuthProvisioningIT.java'
    audit_owner_source.parent.mkdir(parents=True,exist_ok=True)
    audit_source_before=audit_owner_source.read_bytes() if audit_owner_source.exists() else None
    audit_owner_source.write_text('class Controlled { void observePlatformHttpAudits() {} }')
    try:
        audit_contracts=(
            ('platform-http-audit','observePlatformHttpAudits','resource-platform-http-boundaries.json','FOUR_PLATFORM_DIRECT_HTTP_AUTHENTICATION_AND_COUNTERS',8,
             'protocol-platform-http-audits.json','FOUR_PLATFORM_HTTP_PG_STREAM_SIGNED_RECEIPTS_ONLY','PLATFORM_HTTP_AUDIT_OWNER_EXECUTION_ONLY',80,96,runtime_module.observe_platform_http_audits,'observations'),
            ('native-userinfo-audit','observeNativeUserInfoAudits','resource-native-userinfo.json','NATIVE_USERINFO_HTTP_REQUESTS_ONLY',2,
             'protocol-native-userinfo-audits.json','NATIVE_USERINFO_PG_STREAM_SIGNED_RECEIPTS_ONLY','NATIVE_USERINFO_AUDIT_OWNER_EXECUTION_ONLY',6,6,runtime_module.observe_native_userinfo_audits,'observations'),
            ('native-rpc-audit','observeNativeRpcAudits','native-rpc-contract-probe.json','STANDALONE_NOT_FOUND_PROTOCOL',4,
             'protocol-native-rpc-audits.json','NATIVE_RPC_PG_STREAM_SIGNED_RECEIPTS_ONLY','NATIVE_RPC_AUDIT_OWNER_EXECUTION_ONLY',16,20,runtime_module.observe_native_rpc_audits,'proofs'))
        for audit_stage,audit_method,audit_input,audit_input_scope,audit_observations,audit_proof,audit_proof_scope,audit_execution_scope,audit_requests,audit_events,audit_bridge,audit_input_field in audit_contracts:
            audit_state={'calls':0,'stops':0,'fault':None}
            audit_class='top.egon.cola.platform.tianquan.shoubing.admin.integration.IsolatedOAuthProvisioningIT'
            def audit_owner_start(intent,digest,directory,stage,command,children=None):
                assert stage==audit_stage and '-Dtest=IsolatedOAuthProvisioningIT#'+audit_method in command['argv']
                audit_state['calls']+=1; started=time.time_ns(); source_data=(directory/audit_input).read_bytes()
                outcome={'schemaVersion':1,'runId':intent['runId'],'manifestSha256':digest,'status':'PASSED','scope':audit_proof_scope,
                         'sourceSha256':hashlib.sha256(source_data).hexdigest(),'requestCount':audit_requests-1 if audit_state['fault']=='count' else audit_requests,
                         'storedEventCount':audit_events,'spoolAbsentAfterReceiptCount':audit_events,'proofs':[{}]*audit_requests,'recordedAt':datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00','Z')}
                private_json(directory/audit_proof,outcome)
                report=root/'egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/target/surefire-reports'/('TEST-'+audit_class+'.xml')
                report.parent.mkdir(parents=True,exist_ok=True)
                skipped=audit_state['fault']=='skip'
                report.write_text('<testsuite name="'+audit_class+'" tests="1" failures="0" errors="0" skipped="'+('1' if skipped else '0')+'">'
                                 +'<testcase classname="'+audit_class+'" name="'+audit_method+'">'+('<skipped/>' if skipped else '')+'</testcase></testsuite>')
                if audit_state['fault']=='source-drift': audit_owner_source.write_text('changed while owner ran')
                entry={'stage':stage,'startedNs':started,'process':types.SimpleNamespace(wait=lambda **kwargs:0,poll=lambda:0)}
                if children is not None: children.append(entry)
                return entry
            def audit_owner_stop(*args): audit_state['stops']+=1
            with patch.object(runtime_module,'ensure_task_truststore',return_value=truststore),patch.object(runtime_module,'start_verification_process',side_effect=audit_owner_start), \
                 patch.object(runtime_module,'capture_verification_process'),patch.object(runtime_module,'stop_verification_process',side_effect=audit_owner_stop):
                for fault,expected in ((None,None),('count',8),('skip',8),('source-drift',3),('foreign-request',3)):
                    audit_owner_source.write_text('class Controlled { void observePlatformHttpAudits() {} }')
                    audit_attempt=consumer_root/'evidence'/('verify-'+str(uuid.uuid4())); audit_attempt.mkdir(mode=0o700)
                    request={'schemaVersion':1,'runId':consumer_run,'manifestSha256':verification_digest,'status':'PASSED',
                             'scope':audit_input_scope,audit_input_field:[{}]*audit_observations}
                    if fault=='foreign-request': request['manifestSha256']='0'*64
                    private_json(audit_attempt/audit_input,request)
                    audit_state['fault']=fault; calls=audit_state['calls']; stops=audit_state['stops']
                    result=audit_attempt/(audit_stage+'-owner.json')
                    try:
                        audit_bridge(['controlled',str(command_manifest),str(result)])
                        if expected is not None: failures.append('audit bridge accepted '+str(fault))
                    except SystemExit as rejected:
                        if rejected.code!=expected: failures.append('audit bridge '+str(fault)+' returned wrong exit '+str(rejected.code))
                    if fault=='foreign-request':
                        if audit_state['calls']!=calls: failures.append('foreign HTTP audit request reached the owner')
                    elif audit_state['stops']!=stops+1: failures.append('audit bridge did not settle its owned child handle')
                    if expected is None:
                        observed=json.loads(result.read_bytes())
                        if observed.get('scope')!=audit_execution_scope or observed.get('execution',{}).get('testCount')!=1:
                            failures.append('audit bridge lost its original fresh executed method proof')
                    elif result.exists(): failures.append('failed HTTP audit bridge published success')
    finally:
        if audit_source_before is None: audit_owner_source.unlink(missing_ok=True)
        else: audit_owner_source.write_bytes(audit_source_before)
    # SERVICE access-token ownership is separate from the PUBLIC authorization-code family bridge.
    svc_state={'calls':0,'fault':None,'stops':0}
    svc_hash='a'*64
    def service_owner_start(intent,digest,directory,stage,command,children=None):
        assert stage=='service-revoke-'+svc_hash and '-Dtest=IsolatedOAuthProvisioningIT#revokeServiceAccessToken' in command['argv']
        svc_state['calls']+=1; started=time.time_ns(); request=json.loads((directory/('service-revocation-request-'+svc_hash+'.json')).read_bytes())
        confirmed=dict(request,status='CONFIRMED',denyApplied=svc_state['fault']!='queued',recordedAt=datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00','Z'))
        if svc_state['fault']=='unknown': private_json(directory/('service-revocation-running-'+svc_hash+'.json'),dict(request,status='RUNNING'))
        else: private_json(directory/('service-revocation-'+svc_hash+'.json'),confirmed)
        report=root/'egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/target/surefire-reports'/('TEST-'+audit_class+'.xml')
        report.parent.mkdir(parents=True,exist_ok=True); skipped=svc_state['fault']=='skip'
        report.write_text('<testsuite name="'+audit_class+'" tests="1" failures="0" errors="0" skipped="'+('1' if skipped else '0')+'">'
                         +'<testcase classname="'+audit_class+'" name="revokeServiceAccessToken">'+('<skipped/>' if skipped else '')+'</testcase></testsuite>')
        entry={'stage':stage,'startedNs':started,'process':types.SimpleNamespace(wait=lambda **kwargs:1 if svc_state['fault']=='unknown' else 0,poll=lambda:0)}
        if children is not None: children.append(entry)
        return entry
    def service_owner_stop(*args): svc_state['stops']+=1
    with patch.object(runtime_module,'ensure_task_truststore',return_value=truststore),patch.object(runtime_module,'start_verification_process',side_effect=service_owner_start), \
         patch.object(runtime_module,'capture_verification_process'),patch.object(runtime_module,'stop_verification_process',side_effect=service_owner_stop):
        for fault,expected in ((None,None),('queued',8),('skip',8),('unknown',9),('foreign',3)):
            svc_attempt=consumer_root/'evidence'/('verify-'+str(uuid.uuid4())); svc_attempt.mkdir(mode=0o700)
            now=datetime.datetime.now(datetime.timezone.utc)
            request={'schemaVersion':1,'runId':consumer_run,'manifestSha256':verification_digest,'status':'READY','baselineRpcStatus':'OK',
                     'consumerRole':'native-web','providerRole':'native-service','authorizationId':'71','authorizationReference':hashlib.sha256(b'71').hexdigest(),
                     'tokenSha256':svc_hash,'recordedAt':now.isoformat().replace('+00:00','Z'),'expiresAt':(now+datetime.timedelta(seconds=120)).isoformat().replace('+00:00','Z')}
            if fault=='foreign': request['manifestSha256']='0'*64
            private_json(svc_attempt/('service-revocation-request-'+svc_hash+'.json'),request)
            svc_state['fault']=fault; previous_calls=svc_state['calls']; previous_stops=svc_state['stops']
            try:
                runtime_module.revoke_service_access(['controlled',str(command_manifest),str(svc_attempt/('service-revocation-'+svc_hash+'.json')),svc_hash,str(root)])
                if expected is not None: failures.append('SERVICE bridge accepted '+str(fault))
            except SystemExit as rejected:
                if rejected.code!=expected: failures.append('SERVICE bridge '+str(fault)+' returned wrong exit '+str(rejected.code))
            if fault=='foreign':
                if svc_state['calls']!=previous_calls: failures.append('foreign SERVICE request reached the owner')
            elif svc_state['stops']!=previous_stops+1: failures.append('SERVICE bridge did not settle its exact owned child')
    # A real bounded CPU-only child validates PID/group ownership; no application, socket or service is started.
    short_command={"argv":[sys.executable,"-I","-c","import time; time.sleep(5)"],"cwd":str(root),"environment":runtime_module.isolated_runtime_environment()}
    with patch('subprocess.Popen',return_value=types.SimpleNamespace(pid=900002)) as declared_short_transport:
        for declared_stage in ('field-owner','field-consumer','platform-http-audit','native-userinfo-audit','native-rpc-audit'):
            try:
                entry=runtime_module.start_verification_process(verification_intent,verification_digest,attempt,declared_stage,short_command)
                if entry.get('stage')!=declared_stage or entry['process'].pid!=900002:
                    failures.append('declared short stage lost its creation handle')
            except SystemExit as rejected:
                failures.append('required fixed short stage rejected before transport: '+declared_stage+' exit '+str(rejected.code))
        before_calls=declared_short_transport.call_count
        try:
            runtime_module.start_verification_process(verification_intent,verification_digest,attempt,'field-owner/foreign',short_command)
            failures.append('short stage accepted an arbitrary path')
        except SystemExit as rejected:
            if rejected.code!=3: failures.append('foreign short stage returned wrong unsafe exit')
        if before_calls!=declared_short_transport.call_count: failures.append('foreign short stage reached process transport')
    short_entry=runtime_module.start_verification_process(verification_intent,verification_digest,attempt,"native",short_command)
    try:
        runtime_module.capture_verification_process(verification_intent,verification_digest,attempt,short_entry)
        original_identity=copy.deepcopy(short_entry["record"])
        short_entry["record"]["commandSha256"]="0"*64
        try:
            runtime_module.stop_verification_process(verification_intent,short_entry)
            failures.append("short-child stop accepted a different command identity")
        except SystemExit as rejected:
            if rejected.code!=10: failures.append("short-child identity mismatch returned wrong exit")
        if short_entry["process"].poll() is not None: failures.append("a rejected short-child identity was signalled")
        short_entry["record"]=original_identity
        runtime_module.stop_verification_process(verification_intent,short_entry)
        if short_entry["process"].poll() is None: failures.append("the exact owned short child did not stop")
    finally:
        if short_entry["process"].poll() is None:
            short_entry["record"]=original_identity
            runtime_module.stop_verification_process(verification_intent,short_entry)
    # Case-envelope checks only; these controlled files never count as runtime acceptance.
    import datetime
    (attempt / "cases").mkdir(mode=0o700)
    (attempt / "cases/resource").mkdir(mode=0o700)
    effect=attempt / "case-effect.json"
    private_json(effect,{"schemaVersion":1,"controlledTransportOnly":True})
    case_file=attempt / "cases/resource/LIVE-006.json"
    case_started=time.time_ns()-1_000_000_000
    case_record={"schemaVersion":1,"runId":consumer_run,"manifestSha256":verification_digest,"caseId":"LIVE-006","runner":"resource","status":"PASSED",
                 "executedAt":datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00','Z'),"testClass":"controlled.OwnedLiveIT","testMethod":"actualFirst",
                 "proofs":[{"path":str(effect.relative_to(consumer_root)),"sha256":hashlib.sha256(effect.read_bytes()).hexdigest()}]}
    private_json(case_file,case_record)
    executed={"resource":{"classname":"controlled.OwnedLiveIT","methods":["actualFirst"],"testCount":1,"reportSha256":"d"*64,"startedNs":case_started}}
    if runtime_module.read_live_case_evidence(verification_intent,verification_digest,attempt,"LIVE-006","resource",executed)["proofCount"]!=1:
        failures.append("case proof did not bind the executed method and hashed effect")
    for fault,expected in (("wrong method",8),("wrong run",8),("foreign proof",3)):
        changed=copy.deepcopy(case_record)
        if fault=="wrong method": changed["testMethod"]="notExecuted"
        elif fault=="wrong run": changed["runId"]=str(uuid.uuid4())
        else: changed["proofs"][0]["path"]="secrets/formal.at"
        private_json(case_file,changed)
        try:
            runtime_module.read_live_case_evidence(verification_intent,verification_digest,attempt,"LIVE-006","resource",executed)
            failures.append("case "+fault+" evidence was accepted")
        except SystemExit as rejected:
            if rejected.code!=expected: failures.append("case "+fault+" returned wrong rejection")
    # Only the user's accepted Agent runtime input gap may remain explicitly unverified.
    # These are transport fixtures, not evidence that any LIVE case actually ran.
    (attempt / "cases/native").mkdir(mode=0o700)
    agent_deferral={"reasonCode":"AGENT_RUNTIME_CONFIGURATION_NOT_PROVIDED","acceptedOn":"2026-10-15","scope":"AGENT_RUNTIME_ONLY",
                    "sourceBuildCase":"LIVE-012","preservedObligations":["SOURCE_INTEGRATION","SIX_LAYER_ARCHITECTURE","SECURITY_DEFAULT_TESTS","GENERATED_BUILD"]}
    source_case=copy.deepcopy(case_record)
    source_case.update(caseId="LIVE-012",runner="native",testClass="controlled.NativeIT",testMethod="verifyNativeAuthentication")
    source_case_file=attempt/"cases/native/LIVE-012.json"
    private_json(source_case_file,source_case)
    executed["native"]={"classname":"controlled.NativeIT","methods":["verifyNativeAuthentication"],"testCount":1,"reportSha256":"e"*64,"startedNs":case_started}
    deferred_case=copy.deepcopy(source_case)
    deferred_case.update(caseId="LIVE-015",status="UNVERIFIED",deferred=agent_deferral,
                         proofs=[*source_case["proofs"],{"path":str(source_case_file.relative_to(consumer_root)),"sha256":hashlib.sha256(source_case_file.read_bytes()).hexdigest()}])
    deferred_file=attempt/"cases/native/LIVE-015.json"
    private_json(deferred_file,deferred_case)
    try:
        observed=runtime_module.read_live_case_evidence(verification_intent,verification_digest,attempt,"LIVE-015","native",executed)
        if observed.get("status")!="UNVERIFIED" or observed.get("deferred")!=agent_deferral:
            failures.append("accepted Agent runtime input gap lost its explicit unverified obligations")
    except SystemExit as rejected:
        failures.append("accepted Agent runtime input gap was rejected with "+str(rejected.code))
    for fault in ("skipped","wrong case","wrong reason","wrong acceptance","weakened obligations","wrong run","wrong manifest","missing build proof","wrong method"):
        changed=copy.deepcopy(deferred_case); selected_case="LIVE-015"
        if fault=="skipped": changed["status"]="SKIPPED"
        elif fault=="wrong case": changed["caseId"]="LIVE-014"; selected_case="LIVE-014"
        elif fault=="wrong reason": changed["deferred"]["reasonCode"]="ANY_DEPENDENCY_UNAVAILABLE"
        elif fault=="wrong acceptance": changed["deferred"]["acceptedOn"]="2026-10-14"
        elif fault=="weakened obligations": changed["deferred"]["preservedObligations"].remove("SECURITY_DEFAULT_TESTS")
        elif fault=="wrong run": changed["runId"]=str(uuid.uuid4())
        elif fault=="wrong manifest": changed["manifestSha256"]="0"*64
        elif fault=="missing build proof": changed["proofs"]=changed["proofs"][:1]
        else: changed["testMethod"]="notExecuted"
        private_json(attempt/"cases/native"/(selected_case+".json"),changed)
        try:
            runtime_module.read_live_case_evidence(verification_intent,verification_digest,attempt,selected_case,"native",executed)
            failures.append("Agent runtime deferral accepted "+fault)
        except SystemExit as rejected:
            if rejected.code!=8: failures.append("Agent runtime "+fault+" returned wrong rejection")
    private_json(deferred_file,deferred_case)
    weakened_source=copy.deepcopy(source_case); weakened_source["status"]="SKIPPED"
    private_json(source_case_file,weakened_source)
    deferred_case["proofs"][-1]["sha256"]=hashlib.sha256(source_case_file.read_bytes()).hexdigest()
    private_json(deferred_file,deferred_case)
    try:
        runtime_module.read_live_case_evidence(verification_intent,verification_digest,attempt,"LIVE-015","native",executed)
        failures.append("Agent runtime deferral waived the source/build case")
    except SystemExit as rejected:
        if rejected.code!=8: failures.append("unverified Agent source/build returned wrong rejection")
    # The parent pipeline is exercised with explicit process/owner transports only, never real LIVE assertions.
    source_specs=[("egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin","top.egon.cola.platform.tianquan.shoubing.admin.integration.IsolatedOAuthProvisioningIT",["observeBffFamilies"]),
                  ("egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin","top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.OutboxTransactionRollbackIT",["jpaRoleResourceAndMpOutboxRollback","jpaRoleResourceAndMpOutboxAppendFault","jpaRoleResourceAndMpOutboxCommit"]),
                  ("egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-suite","top.egon.cola.component.yuheng.test.integration.XingyuanNativeAuthenticationIT",["verifyNativeAuthentication"]),
                  ("egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-suite","top.egon.cola.component.yuheng.test.integration.XingyuanResourceAuthorizationIT",["verifyResourceAuthorization"]),
                  ("egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-suite","top.egon.cola.component.yuheng.test.integration.XingyuanNativeAuthenticationIT",["configuredNativeServiceClientsRenewThroughTheRealAsWithSingleFlight"]),
                  ("egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin","top.egon.cola.platform.tianquan.jianshen.admin.integration.IsolatedAuthorizationProvisioningIT",["observeNativeFieldRules"]),
                  ("egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-suite","top.egon.cola.component.yuheng.test.integration.XingyuanFieldAuthorizationIT",["nativeFieldRulesControlWritesAcrossRealOwnerEpochsAndTenants"]) ]
    source_methods={}
    for module,classname,methods in source_specs:
        file=root/module/"src/test/java"/Path(*classname.split('.')).with_suffix('.java')
        file.parent.mkdir(parents=True,exist_ok=True)
        source_methods.setdefault(file,set()).update(methods)
    for file,methods in source_methods.items():
        file.write_text('class Controlled { '+''.join('void '+name+'(){}' for name in sorted(methods))+' }')
    pw_source=root/"egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/e2e/integration/xingyuan-identity-integration.spec.ts"
    pw_source.parent.mkdir(parents=True,exist_ok=True)
    pw_source.write_text("test('LIVE-controlled-parent', async()=>{})")
    for relative in ("egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/playwright.integration.config.ts",
                     "egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-suite/src/test/java/top/egon/cola/component/yuheng/test/tianquan/shoubing/UnifiedIdentityLiveClient.java"):
        metadata_source=root/relative
        metadata_source.parent.mkdir(parents=True,exist_ok=True)
        metadata_source.write_text("// controlled source provenance only; no application or browser is run\n")
    pipeline_manifest=copy.deepcopy(verification_intent)
    pipeline_file=consumer_root/"pipeline-intent.json"
    private_json(pipeline_file,pipeline_manifest)
    pipeline_digest=hashlib.sha256(pipeline_file.read_bytes()).hexdigest()
    private_json(consumer_root/"evidence/original-databases.json",{"schemaVersion":1,"runId":consumer_run,"manifestSha256":pipeline_digest,
                 "status":"CAPTURED","observedAt":datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00','Z'),"databases":old_database})
    private_json(consumer_root/"evidence/original-nginx.json",{"schemaVersion":1,"runId":consumer_run,"manifestSha256":pipeline_digest,
                 "status":"CAPTURED","observedAt":datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00','Z'),"processes":original_nginx_fixture})
    private_json(consumer_root/"evidence/original-redis.json",{"schemaVersion":1,"runId":consumer_run,"manifestSha256":pipeline_digest,
                 "status":"CAPTURED","snapshot":redis_fixture})
    pipeline_registry=copy.deepcopy(timing_registry)
    pipeline_registry["manifestSha256"]=pipeline_digest
    private_json(consumer_root/"secrets/runtime-registry.json",pipeline_registry)
    pipeline_ledger=json.loads((consumer_root/"evidence/processes.json").read_text())
    pipeline_ledger["manifestSha256"]=pipeline_digest
    private_json(consumer_root/"evidence/processes.json",pipeline_ledger)
    pipeline_calls=[]
    class TerminalHandle:
        def __init__(self,code=0): self.code=code
        def poll(self): return self.code
    def controlled_validation_start(intent,digest,directory,stage,command,owned=None):
        started=time.time_ns()
        pipeline_calls.append(stage)
        entry={"process":TerminalHandle(),"record":None,"stage":stage,"startedNs":started,"runId":consumer_run,"manifestSha256":digest}
        if owned is not None: owned.append(entry)
        def marker_file(name,status,**extra):
            private_json(directory/name,{"schemaVersion":1,"runId":consumer_run,"manifestSha256":digest,"status":status,**extra})
        if stage.startswith("formal-input-"):
            if pipeline_state.get("cliUnknown") and stage=="formal-input-001":
                entry["process"]=TerminalHandle(9)
                return entry
            args=command["argv"]
            operation=args.index("authorize-cli")
            result_file=Path(args[operation+2])
            tenant,resource,user=args[operation+3:operation+6]
            output=Path(args[operation+6])
            body=json.loads(formal_index.read_text())
            selected=[value for value in body["tokens"] if (value["tenantCode"],value["resourceKey"],value["userKey"])==(tenant,resource,user)]
            assert len(selected)==1
            material=("controlled-fresh-input-"+directory.name+stage).encode()
            if pipeline_state.get("reuseCliToken"): material=Path(selected[0]["tokenFile"]).read_bytes()
            output.write_bytes(material); output.chmod(0o600)
            selected[0].update(tokenFile=str(output),tokenSha256=hashlib.sha256(material).hexdigest(),probeStatus=None,
                               expiresAt=(datetime.datetime.now(datetime.timezone.utc)+datetime.timedelta(seconds=600)).isoformat().replace('+00:00','Z'))
            if pipeline_state.get("wrongCliTenant"): selected[0]["tenantId"]="9999"
            if pipeline_state.get("widenCliScope"): selected[0]["scopes"]=[*selected[0]["scopes"],"unreviewed:scope"]
            body.update(runId=consumer_run,manifestSha256=digest)
            private_json(formal_index,body)
            private_json(result_file,{"schemaVersion":1,"operationId":"CLI-006","runId":consumer_run,"manifestSha256":digest,"status":"COMMITTED",
                         "completedAt":datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00','Z'),
                         "targets":[{"targetKey":resource,"identity":"PUBLIC_USER","version":"1","state":"COMMITTED"}],
                         "counts":{"databaseCount":0,"tenantCount":1,"clientCount":1,"catalogCount":0,"liveCaseCount":0},"steps":[],"error":None})
            return entry
        if stage=="oracle":
            marker_file("oracle-READY.json","READY",phase="READY",proofs=[])
            marker_file("oracle-COMPLETE.json","PASSED",phase="COMPLETE",proofs=[])
            for realm in ("TENANT_A","TENANT_B"):
                for phase,status,count in (("REFRESH","PASSED",4),("STEP_UP","PASSED",1),("HOLD","READY",5),("REPLAY","PASSED",5)):
                    marker_file("oracle-"+realm+"_"+phase+".json",status,phase=realm+"_"+phase,proofs=[{}]*count)
            for phase,status in (("HOLD","READY"),("REPLAY","PASSED")):
                marker_file("oracle-TENANT_A_FAULT_"+phase+".json",status,phase="TENANT_A_FAULT_"+phase,proofs=[{}]*5)
        if stage=="browser":
            marker_file("browser-AB_BROWSER.json","PASSED")
            marker_file("browser-BROWSER_EXECUTION.json","PASSED",titleReference=hashlib.sha256(b"LIVE-controlled-parent").hexdigest(),expectedStatus="passed",actualStatus="passed",retry=0,executedTestCount=1)
            marker_file("browser-AS_REVOCATION_PENDING.json","PASSED",responseStatus=503,remoteState="PENDING",localLoggedOut=True,
                        durableIntentCount=5,lateCallbackObserved=True,lateCallbackSessionStatus=401,proofs=[{}]*3)
            if not pipeline_state.get('omitFaultRecovery'):
                marker_file("browser-AS_REVOCATION_RECOVERED.json","PASSED",queuedCount=0,pendingCount=0,sessionStatus=401,proofs=[{}]*5)
        else:
            module,classname,methods=source_specs[{"oracle":0,"outbox":1,"native":2,"resource":3,"service-cache":4,"field-owner":5,"field-consumer":6}[stage]]
            report=root/module/"target/surefire-reports"/("TEST-"+classname+".xml")
            report.parent.mkdir(parents=True,exist_ok=True)
            skipped=stage=="service-cache" and pipeline_state.get("skipCache")
            report.write_text('<testsuite name="'+classname+'" tests="'+str(len(methods))+'" failures="0" errors="0" skipped="'+('1' if skipped else '0')+'">'
                              +''.join('<testcase classname="'+classname+'" name="'+method+'">'+('<skipped/>' if skipped else '')+'</testcase>' for method in methods)+'</testsuite>')
            if stage=="field-owner":
                marker_file("native-field-owner-ready.json", "READY")
            if stage=="service-cache":
                if not pipeline_state.get("omitCacheReady"):
                    marker_file("native-service-cache-ready.json","READY",phase="INITIAL_VERIFIED",requestCount=4,profileCount=4)
                marker_file("native-service-cache.json","PASSED",clock="SYSTEM_UTC",proofs=[{"role":role} for role in ("native-light","native-web","native-service","native-agent")])
            if stage in ("resource","native"):
                (directory/"cases").mkdir(mode=0o700,exist_ok=True)
                (directory/"cases"/stage).mkdir(mode=0o700,exist_ok=True)
                proof=directory/(stage+"-controlled-effect.json"); private_json(proof,{"controlledTransportOnly":True})
                for number in range(1,25):
                    if (11<=number<=15)!=(stage=="native"): continue
                    case_id="LIVE-"+str(number).zfill(3)
                    if pipeline_state.get("omitCase")==case_id: continue
                    case={"schemaVersion":1,"runId":consumer_run,"manifestSha256":digest,"caseId":case_id,"runner":stage,"status":"PASSED",
                                 "executedAt":datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00','Z'),"testClass":classname,"testMethod":methods[0],
                                 "proofs":[{"path":str(proof.relative_to(consumer_root)),"sha256":hashlib.sha256(proof.read_bytes()).hexdigest()}]}
                    if pipeline_state.get("deferAgent") and case_id=="LIVE-015":
                        case.update(status="UNVERIFIED",deferred=copy.deepcopy(agent_deferral))
                        build_case=directory/"cases/native/LIVE-012.json"
                        if build_case.exists():
                            case["proofs"].append({"path":str(build_case.relative_to(consumer_root)),"sha256":hashlib.sha256(build_case.read_bytes()).hexdigest()})
                    private_json(directory/"cases"/stage/(case_id+".json"),case)
        if stage=="resource" and not pipeline_state.get("leaveRunning"): pipeline_state["stopped"]=True
        return entry
    pipeline_state={"stopped":False}
    def controlled_formal_index(*args):
        values=json.loads(formal_index.read_text())["tokens"]
        return {(value["tenantCode"],value["resourceKey"],value["userKey"]):copy.deepcopy(value) for value in values}
    def controlled_database_preservation(intent,digest,directory):
        assert pipeline_state["stopped"], "The final database audit must follow the real owned-stop phase"
        pipeline_calls.append("database-preservation")
        if pipeline_state.get("originalDrift"): raise SystemExit(3)
        proof={"schemaVersion":1,"runId":consumer_run,"manifestSha256":digest,"status":"PASSED","scope":"ORIGINAL_DATABASE_IDENTITIES_ONLY","originalDatabaseCount":1}
        private_json(directory/"original-databases-preserved.json",proof)
        return proof
    def controlled_nginx_preservation(intent,digest,directory):
        assert pipeline_state["stopped"]
        pipeline_calls.append("nginx-preservation")
        if pipeline_state.get("nginxDrift"): raise SystemExit(3)
        proof={"schemaVersion":1,"runId":consumer_run,"manifestSha256":digest,"status":"PASSED","scope":"ORIGINAL_NGINX_IDENTITIES_AND_PORTS_ONLY"}
        private_json(directory/"original-nginx-preserved.json",proof)
        return proof
    def controlled_redis_preservation(intent,digest,directory):
        assert pipeline_state["stopped"]
        pipeline_calls.append("redis-preservation")
        if pipeline_state.get("redisDrift"): raise SystemExit(3)
        if pipeline_state.get("redisBeforeTamper"):
            altered=json.loads((consumer_root/"evidence/original-redis.json").read_text())
            altered['snapshot']['observedAtMs']+=1
            private_json(consumer_root/"evidence/original-redis.json",altered)
        proof={"schemaVersion":1,"runId":consumer_run,"manifestSha256":digest,"status":"PASSED","scope":"ORIGINAL_REDIS_SAMPLED_KEY_VALUES_ONLY"}
        private_json(directory/"original-redis-preserved.json",proof)
        return proof
    pipeline_replacements={"require_operation_receipt":lambda *args: {},"validate_formal_token_index":controlled_formal_index,
                           "verify_original_databases":controlled_database_preservation,
                           "verify_original_nginx":controlled_nginx_preservation,
                           "verify_original_redis":controlled_redis_preservation,
                           "ensure_task_truststore":lambda *args: truststore,"verification_runner_command":lambda *args: {},
                           "start_verification_process":controlled_validation_start,"capture_verification_process":lambda *args: None,
                           "observe_owned_process":lambda *args: "STOPPED" if pipeline_state["stopped"] else "RUNNING"}
    pipeline_replacements['validate_task_nginx_configuration']=lambda *args: not pipeline_state.get('missingFaultMap')
    (consumer_root/'nginx').mkdir(mode=0o700,exist_ok=True)
    (consumer_root/'nginx/revocation-fault.map').write_bytes(b'default 0;\n')
    (consumer_root/'nginx/revocation-fault.map').chmod(0o600)
    pipeline_originals={name:getattr(runtime_module,name) for name in pipeline_replacements}
    try:
        for name,value in pipeline_replacements.items(): setattr(runtime_module,name,value)
        pipeline_state.update(stopped=False,missingFaultMap=True)
        try:
            runtime_module.verify_full_integration(["controlled",str(pipeline_file),str(consumer_root/"evidence/pipeline-no-fault-map.json"),str(formal_index),"full"])
            failures.append('full pipeline started before the approved AS fault map was ready')
        except SystemExit as rejected:
            if rejected.code!=4: failures.append('missing AS fault map returned wrong input exit')
        if pipeline_calls: failures.append('missing AS fault map started a validation child')
        pipeline_state.clear(); pipeline_state.update(stopped=False)
        too_slow=copy.deepcopy(pipeline_registry)
        for item in too_slow["clients"]: item["accessTokenTtlSeconds"]=600
        private_json(consumer_root/"secrets/runtime-registry.json",too_slow)
        try:
            runtime_module.verify_full_integration(["controlled",str(pipeline_file),str(consumer_root/"evidence/pipeline-impossible-time.json"),str(formal_index),"full"])
            failures.append("full pipeline started an impossible real wait plan")
        except SystemExit as rejected:
            if rejected.code!=2: failures.append("full impossible time plan returned wrong input exit")
        if pipeline_calls: failures.append("impossible time plan created a validation child")
        private_json(consumer_root/"secrets/runtime-registry.json",pipeline_registry)
        try:
            runtime_module.verify_full_integration(["controlled",str(pipeline_file),str(consumer_root/"evidence/pipeline-result.json"),str(formal_index),"full"])
        except SystemExit as rejected:
            import traceback
            locations=[frame.name+":"+str(frame.lineno) for frame in traceback.extract_tb(rejected.__traceback__)]
            failures.append("controlled parent rejected "+str(rejected.code)+" at "+"/".join(locations))
        pipeline_result=json.loads((consumer_root/"evidence/pipeline-result.json").read_text()) if (consumer_root/"evidence/pipeline-result.json").exists() else {"counts":{"liveCaseCount":0},"status":"MISSING"}
        expected_calls=["service-cache","oracle","browser",*["formal-input-"+str(value).zfill(3) for value in range(4)],"outbox","field-owner","field-consumer","native","resource","database-preservation","nginx-preservation","redis-preservation"]
        if pipeline_calls!=expected_calls or pipeline_result["counts"]["liveCaseCount"]!=24 or pipeline_result["status"]!="PASSED":
            failures.append("controlled verify pipeline did not enforce the complete required sequence")
        if not any(value["stepId"]=="FORMAL_CODE_INPUTS" and value["state"]=="PASSED" for value in pipeline_result["steps"]):
            failures.append("formal Code reacquisition was hidden from the full result")
        pipeline_state.clear(); pipeline_state.update(stopped=False,deferAgent=True)
        pipeline_calls.clear()
        deferred_result_file=consumer_root/"evidence/pipeline-agent-unverified.json"
        try:
            runtime_module.verify_full_integration(["controlled",str(pipeline_file),str(deferred_result_file),str(formal_index),"full"])
            failures.append("Agent runtime deferral returned full success")
        except SystemExit as rejected:
            if rejected.code!=7: failures.append("accepted Agent runtime gap did not return PARTIAL exit 7")
        deferred_result=json.loads(deferred_result_file.read_text())
        if deferred_result["status"]!="PARTIAL" or deferred_result["counts"]["liveCaseCount"]!=23 or pipeline_calls!=expected_calls:
            failures.append("Agent runtime gap did not retain 23 passed cases and all preservation checks")
        if deferred_result["steps"][-1]["state"]!="PARTIAL" or deferred_result["error"]["code"]!="XINGYUAN_PARTIAL":
            failures.append("Agent runtime gap was hidden from the public partial result")
        if deferred_result["status"]=="PARTIAL":
            matrix_file=consumer_root/deferred_result["steps"][-1]["evidence"]
            matrix=json.loads(matrix_file.read_text())
            if (matrix.get("status")!="PARTIAL" or len(matrix.get("cases",[]))!=24
                    or [case["caseId"] for case in matrix["cases"] if case.get("status")=="UNVERIFIED"]!=["LIVE-015"]
                    or sum(case.get("status")=="PASSED" for case in matrix["cases"])!=23):
                failures.append("private Agent matrix lost its precise unverified case")
            if json.loads((consumer_root/"evidence/full.json").read_text())["status"]!="PARTIAL":
                failures.append("Agent partial outcome left a misleading successful full receipt")
        pipeline_state.clear(); pipeline_state.update(stopped=False,deferAgent=True,omitCase="LIVE-012")
        pipeline_calls.clear()
        try:
            runtime_module.verify_full_integration(["controlled",str(pipeline_file),str(consumer_root/"evidence/pipeline-agent-missing-build.json"),str(formal_index),"full"])
            failures.append("Agent runtime gap waived source/build evidence")
        except SystemExit as rejected:
            if rejected.code!=4: failures.append("Agent missing source/build returned wrong unavailable exit")
        native_source=root/source_specs[2][0]/"src/test/java"/Path(*source_specs[2][1].split('.')).with_suffix('.java')
        native_source_bytes=native_source.read_bytes()
        resource_source=root/source_specs[3][0]/"src/test/java"/Path(*source_specs[3][1].split('.')).with_suffix('.java')
        resource_source_bytes=resource_source.read_bytes()
        native_source.write_text("class Controlled { void configuredNativeServiceClientsRenewThroughTheRealAsWithSingleFlight(){} }")
        resource_source.write_text("class Controlled {}")
        pipeline_state.clear(); pipeline_state.update(stopped=False,deferAgent=True)
        pipeline_calls.clear()
        try:
            runtime_module.verify_full_integration(["controlled",str(pipeline_file),str(consumer_root/"evidence/pipeline-agent-missing-runner.json"),str(formal_index),"full"])
            failures.append("Agent runtime gap treated a missing native composite as passed")
        except SystemExit as rejected:
            if rejected.code!=4: failures.append("Agent missing native runner returned wrong unavailable exit")
        finally:
            native_source.write_bytes(native_source_bytes)
            resource_source.write_bytes(resource_source_bytes)
        if pipeline_calls: failures.append("Agent runtime gap started children without the native composite")
        missing_runner_result=consumer_root/"evidence/pipeline-agent-missing-runner.json"
        if not missing_runner_result.exists():
            failures.append("missing composite runners did not publish a bounded failure result")
        else:
            failure_result=json.loads(missing_runner_result.read_bytes())
            if (failure_result.get("status")!="FAILED" or failure_result.get("error")!={"code":"XINGYUAN_INPUT_UNAVAILABLE",
                    "message":"Required verification source is missing: native, resource", "retryable":False,"stage":"SOURCE_READINESS"}
                    or failure_result.get("runId")!=consumer_run or failure_result.get("manifestSha256")!=pipeline_digest
                    or failure_result.get("counts")!={"databaseCount":0,"tenantCount":0,"clientCount":0,"catalogCount":0,"liveCaseCount":0}
                    or failure_result.get("targets")!=[] or failure_result.get("steps")!=[]
                    or missing_runner_result.stat().st_mode & 0o777!=0o600):
                failures.append("missing runner result did not retain all source gaps and zero executed cases")
        preserved_redis_baseline=(consumer_root/"evidence/original-redis.json").read_bytes()
        for fault in ("originalDrift","nginxDrift","redisDrift","redisBeforeTamper"):
            pipeline_state.clear();pipeline_state.update(stopped=False,**{fault:True})
            pipeline_calls.clear()
            try:
                runtime_module.verify_full_integration(["controlled",str(pipeline_file),str(consumer_root/"evidence"/("pipeline-"+fault+".json")),str(formal_index),"full"])
                failures.append("full verification accepted original facility drift "+fault)
            except SystemExit as rejected:
                if rejected.code!=3: failures.append("original facility drift returned wrong unsafe exit")
            (consumer_root/"evidence/original-redis.json").write_bytes(preserved_redis_baseline)
        pipeline_state.clear()
        for fault in ("omitCacheReady","skipCache"):
            pipeline_state.clear(); pipeline_state.update(stopped=False,**{fault:True})
            pipeline_calls.clear()
            try:
                runtime_module.verify_full_integration(["controlled",str(pipeline_file),str(consumer_root/"evidence"/("pipeline-"+fault+".json")),str(formal_index),"full"])
                failures.append("full verification accepted SERVICE cache "+fault)
            except SystemExit as rejected:
                if rejected.code!=8: failures.append("SERVICE cache "+fault+" returned wrong exit")
            if "outbox" in pipeline_calls or "resource" in pipeline_calls or fault=="omitCacheReady" and "oracle" in pipeline_calls:
                failures.append("failed SERVICE cache gate advanced later acceptance stages")
        pipeline_state.clear()
        pipeline_state.update(stopped=False,omitFaultRecovery=True)
        pipeline_calls.clear()
        try:
            runtime_module.verify_full_integration(["controlled",str(pipeline_file),str(consumer_root/"evidence/pipeline-fault-not-recovered.json"),str(formal_index),"full"])
            failures.append('full verification accepted an AS fault without durable queue recovery')
        except SystemExit as rejected:
            if rejected.code!=8: failures.append('missing AS fault recovery returned wrong exit')
        if 'outbox' in pipeline_calls or 'resource' in pipeline_calls:
            failures.append('missing AS fault recovery advanced the real matrix')
        pipeline_state.clear()
        pipeline_state.update(stopped=False,cliUnknown=True)
        pipeline_calls.clear()
        try:
            runtime_module.verify_full_integration(["controlled",str(pipeline_file),str(consumer_root/"evidence/pipeline-cli-unknown.json"),str(formal_index),"full"])
            failures.append("unknown formal Code commit continued the matrix")
        except SystemExit as rejected:
            if rejected.code!=9: failures.append("unknown formal Code commit returned wrong exit")
        unknown_result=json.loads((consumer_root/"evidence/pipeline-cli-unknown.json").read_text())
        if unknown_result["status"]!="UNKNOWN_COMMIT" or "outbox" in pipeline_calls:
            failures.append("unknown formal Code input was treated as a completed phase")
        for fault in ("wrongCliTenant","widenCliScope","reuseCliToken"):
            retained_index=formal_index.read_bytes()
            pipeline_state.clear(); pipeline_state.update(stopped=False,**{fault:True})
            pipeline_calls.clear()
            try:
                runtime_module.verify_full_integration(["controlled",str(pipeline_file),str(consumer_root/"evidence"/("pipeline-"+fault+".json")),str(formal_index),"full"])
                failures.append("formal Code input accepted "+fault)
            except SystemExit as rejected:
                if rejected.code!=8: failures.append("formal Code "+fault+" returned wrong exit")
            if "outbox" in pipeline_calls: failures.append("mismatched formal Code tuple reached later acceptance")
            formal_index.write_bytes(retained_index)
        pipeline_state.clear()
        pipeline_state.update(stopped=False,omitCase="LIVE-024")
        pipeline_calls.clear()
        try:
            runtime_module.verify_full_integration(["controlled",str(pipeline_file),str(consumer_root/"evidence/pipeline-missing-case.json"),str(formal_index),"full"])
            failures.append("verify published PASSED with a missing required case")
        except SystemExit as rejected:
            if rejected.code!=4: failures.append("missing case file returned wrong unavailable exit")
        missing_result=json.loads((consumer_root/"evidence/pipeline-missing-case.json").read_text())
        if missing_result["status"]!="FAILED" or missing_result["counts"]["liveCaseCount"]!=23:
            failures.append("missing required case was counted as a full successful matrix")
        pipeline_state.update(stopped=False,omitCase=None,leaveRunning=True)
        try:
            runtime_module.verify_full_integration(["controlled",str(pipeline_file),str(consumer_root/"evidence/pipeline-not-stopped.json"),str(formal_index),"full"])
            failures.append("verify accepted a claimed preservation case while application PIDs were still running")
        except SystemExit as rejected:
            if rejected.code!=8: failures.append("unconfirmed final stop returned wrong verification exit")
        stop_result=json.loads((consumer_root/"evidence/pipeline-not-stopped.json").read_text())
        if stop_result["status"]!="FAILED": failures.append("unconfirmed final stop published PASSED")
    finally:
        for name,value in pipeline_originals.items(): setattr(runtime_module,name,value)
    lifecycle_calls = []
    original_transports = {name: getattr(runtime_module, name) for name in ("run_task_fixture", "ensure_task_truststore", "launch_owned_runtime", "observe_owned_process", "wait_owned_https", "validate_task_nginx_configuration")}
    def controlled_fixture(manifest_file, intent, digest, stage):
        lifecycle_calls.append("fixture:" + stage)
        if stage == "profiles":
            return {"profiles": {name: {} for name in ("shoubing", "jianshen", "tianshu", "yuheng", "edge")}}
        if stage == "control-clients":
            return {"membershipCount": 6, "catalogCount": 4}
        return {"status": "COMMITTED"}
    def controlled_launch(intent, digest, name, truststore, manifest_file=None):
        lifecycle_calls.append("launch:" + name)
        return {"id": name, "pid": 71000 + len(lifecycle_calls), "startInstant": "2026-10-14T00:00:00Z",
                "artifactSha256": intent["artifacts"][name]["sha256"], "commandSha256": "d" * 64}
    def controlled_wait(*arguments, **keywords):
        lifecycle_calls.append("https:" + arguments[2])
    runtime_module.validate_task_nginx_configuration = lambda *args: None
    runtime_module.run_task_fixture = controlled_fixture
    runtime_module.ensure_task_truststore = lambda *args: control_root / "secrets/controlled-truststore.p12"
    runtime_module.launch_owned_runtime = controlled_launch
    runtime_module.observe_owned_process = lambda *args: "RUNNING"
    runtime_module.wait_owned_https = controlled_wait
    private_json(control_root / "secrets/runtime-registry.json", {"runId": control_run, "manifestSha256": control_digest,
                 "tenantIds": {"default": "1001", "tenant-b": "1002"}, "clients": []})
    (fake_bin / "lsof").write_text("#!/usr/bin/env bash\nexit 1\n")
    try:
        original_path = os.environ.get("PATH", "")
        os.environ["PATH"] = env["PATH"]
        runtime_module.start_control_plane(["controlled", str(control_intent), str(control_root / "evidence/controlled-start.json")])
        result = json.loads((control_root / "evidence/control-plane.json").read_text())
        assert result["status"] == "RUNNING" and len(result["targets"]) == 6
        assert lifecycle_calls.index("fixture:control-clients") < lifecycle_calls.index("launch:jianshen")
        assert len([value for value in lifecycle_calls if value.startswith("launch:")]) == 6
        runtime_module.start_control_plane(["controlled", str(control_intent), str(control_root / "evidence/controlled-start-again.json")])
        assert len([value for value in lifecycle_calls if value.startswith("launch:")]) == 6, "an owned running process was restarted"
        def transient_unavailable(*args, **kwargs):
            raise SystemExit(6)
        runtime_module.wait_owned_https = transient_unavailable
        try:
            runtime_module.start_control_plane(["controlled", str(control_intent), str(control_root / "evidence/controlled-start-unavailable.json")])
            failures.append("transient HTTPS unavailability was reported as a successful control plane")
        except SystemExit as stopped:
            assert stopped.code == 6
        assert len([value for value in lifecycle_calls if value.startswith("launch:")]) == 6, "a transient probe restarted an alive process"
    finally:
        os.environ["PATH"] = original_path
        for name, value in original_transports.items():
            setattr(runtime_module, name, value)

    # This is a transport recorder only; no authorization service or database is simulated as a live pass.
    platform_run = str(uuid.uuid4())
    platform_root = root / ("target/xingyuan-integration-" + platform_run)
    for relative in ("", "evidence", "secrets", "profiles", "policies"):
        (platform_root / relative).mkdir(parents=True, exist_ok=True)
        (platform_root / relative).chmod(0o700)
    platform_manifest = copy.deepcopy(manifest)
    platform_manifest.update(runId=platform_run, runtimeRoot=str(platform_root))
    platform_manifest["postgres"]["credentialFile"] = str(platform_root / "secrets/postgres.txt")
    platform_manifest["redis"]["credentialFile"] = str(platform_root / "secrets/redis.txt")
    platform_artifact = root / "controlled-platform.jar"
    platform_artifact.write_bytes(b"controlled-platform-artifact")
    platform_manifest["artifacts"] = {"jianshen": {"path": str(platform_artifact), "sha256": hashlib.sha256(platform_artifact.read_bytes()).hexdigest(), "buildId": "controlled-platform"}}
    platform_settings = platform_root / "policies/maven-settings.xml"
    platform_settings.write_text("<settings/>"); platform_settings.chmod(0o600)
    platform_manifest["policyFiles"] = [{"path": str(platform_settings), "sha256": hashlib.sha256(platform_settings.read_bytes()).hexdigest()}]
    platform_intent = platform_root / "intent.json"
    private_json(platform_intent, platform_manifest)
    platform_digest = hashlib.sha256(platform_intent.read_bytes()).hexdigest()
    for filename, operation, status in (("prepared.json", "CLI-001", "COMMITTED"), ("identity.json", "CLI-002", "COMMITTED"), ("control-plane.json", "CLI-003", "RUNNING")):
        private_json(platform_root / "evidence" / filename, {"schemaVersion": 1, "operationId": operation, "runId": platform_run,
                     "manifestSha256": platform_digest, "status": status})
    private_json(platform_root / "evidence/db-rbac.json", {"runId": platform_run, "manifestSha256": platform_digest,
                 "role": "rbac", "databaseName": names["rbac"], "oid": 52, "databaseOwner": "controlled-platform-owner",
                 "ownerComment": "xingyuan-it:" + platform_run + ":" + platform_digest})
    private_json(platform_root / "secrets/runtime-registry.json", {"schemaVersion": 1, "runId": platform_run,
                 "manifestSha256": platform_digest, "tenantIds": {"default": "1001", "tenant-b": "1002"}})
    wrapper.write_text('''#!/usr/bin/env python3
import datetime, hashlib, json, os, sys
from pathlib import Path
options = {value.split('=', 1)[0]: value.split('=', 1)[1] for value in sys.argv if value.startswith('-D') and '=' in value}
assert options['-Dtest'] == 'IsolatedAuthorizationProvisioningIT#provisionPlatformAuthorization'
assert options['-Duser.timezone'] == 'UTC'
assert Path(sys.argv[sys.argv.index('-s') + 1]).name == 'maven-settings.xml'
assert os.environ['XINGYUAN_ISOLATED_INTEGRATION'] == 'true'
assert not {'JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', 'MAVEN_OPTS'} & os.environ.keys()
Path(os.environ['XINGYUAN_FAKE_CALLS']).write_text('controlled-platform-owner-once\\n')
manifest = Path(options['-Dxingyuan.isolated.manifest-file'])
intent = json.loads(manifest.read_text())
root = Path(intent['runtimeRoot'])
expiry = datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(seconds=-1 if os.environ.get('XINGYUAN_FAKE_PLATFORM_EXPIRED') else 300)
snapshots = {}
for index, tenant in enumerate(('1001', '1002')):
    snapshots[tenant] = {'tenantId': tenant, 'rbacUserId': str(5001 + index), 'authVersion': 3, 'policyVersion': 4,
                         'selectedRoleIds': [str(6001 + index * 4 + i) for i in range(4)], 'snapshotChecksum': 'c' * 64,
                         'expiresAt': expiry.isoformat().replace('+00:00', 'Z')}
value = {'schemaVersion': 1, 'runId': intent['runId'], 'manifestSha256': hashlib.sha256(manifest.read_bytes()).hexdigest(),
         'databaseOid': 52, 'phase': 'platform', 'status': 'COMMITTED', 'commandSha256': 'a' * 64,
         'recordedAt': datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00', 'Z'), 'snapshots': snapshots}
path = root / 'evidence/authorization-platform-owner.json'
path.write_text(json.dumps(value)); path.chmod(0o600)
''')
    wrapper.chmod(0o755)
    platform_calls = root / "controlled-platform-owner-calls"
    platform_environment = dict(env, XINGYUAN_FAKE_CALLS=str(platform_calls))
    platform_result = platform_root / "evidence/platform-result.json"
    platform_arguments = command + ["provision-authorization", "--phase", "platform", "--manifest", str(platform_intent), "--result", str(platform_result)]
    response = subprocess.run(platform_arguments, env=platform_environment, cwd=root, capture_output=True, text=True, timeout=15)
    if response.returncode != 0 or response.stdout.strip() != "CLI-004 COMMITTED evidence/platform-result.json":
        failures.append("platform owner callback was not dispatched and verified")
    else:
        canonical_platform = platform_root / "evidence/authorization-platform.json"
        assert canonical_platform.read_bytes() == platform_result.read_bytes()
        assert json.loads(platform_result.read_text())["counts"]["tenantCount"] == 2
        assert "5001" not in platform_result.read_text()
        retained_platform = canonical_platform.read_bytes()
        platform_calls.unlink()
        repeated = subprocess.run(platform_arguments, env=platform_environment, cwd=root, capture_output=True, text=True, timeout=15)
        if repeated.returncode != 5 or platform_calls.exists():
            failures.append("a committed platform invocation repeated owner writes without explicit resume")
        expired = subprocess.run(platform_arguments + ["--resume"], env=dict(platform_environment, XINGYUAN_FAKE_PLATFORM_EXPIRED="true"),
                                 cwd=root, capture_output=True, text=True, timeout=15)
        if expired.returncode != 7 or canonical_platform.read_bytes() != retained_platform:
            failures.append("expired platform proofs replaced the committed phase receipt")
    assert "controlled-private-value" not in response.stdout + response.stderr

    # The native parent invokes one reviewed owner; its four snapshots are required for public COMMITTED.
    native_run = str(uuid.uuid4())
    native_root = root / ("target/xingyuan-integration-" + native_run)
    for relative in ("", "evidence", "secrets", "profiles", "policies"):
        (native_root / relative).mkdir(parents=True, exist_ok=True)
        (native_root / relative).chmod(0o700)
    native_manifest = copy.deepcopy(platform_manifest)
    native_manifest.update(runId=native_run, runtimeRoot=str(native_root))
    native_manifest["postgres"]["credentialFile"] = str(native_root / "secrets/postgres.txt")
    native_manifest["redis"]["credentialFile"] = str(native_root / "secrets/redis.txt")
    native_manifest["processes"] = [{"id":"tianshu","artifactRole":"tianshu","kind":"JAVA","port":28150,"additionalPorts":[29080]}]
    native_manifest["artifacts"]["tianshu"] = copy.deepcopy(native_manifest["artifacts"]["jianshen"])
    native_settings = native_root / "policies/maven-settings.xml"
    native_settings.write_text("<settings/>"); native_settings.chmod(0o600)
    native_manifest["policyFiles"] = [{"path":str(native_settings),"sha256":hashlib.sha256(native_settings.read_bytes()).hexdigest()}]
    native_intent = native_root / "intent.json"
    private_json(native_intent, native_manifest)
    native_digest = hashlib.sha256(native_intent.read_bytes()).hexdigest()
    for filename, operation, state, step in (("prepared.json","CLI-001","COMMITTED","PREPARE"),
            ("identity.json","CLI-002","COMMITTED","IDENTITY"),("control-plane.json","CLI-003","RUNNING","CONTROL_PLANE"),
            ("authorization-platform.json","CLI-004","COMMITTED","PLATFORM_AUTHORIZATION")):
        private_json(native_root / "evidence" / filename, {"schemaVersion":1,"operationId":operation,"runId":native_run,
                     "manifestSha256":native_digest,"status":state,"completedAt":"2026-10-14T00:00:00Z","targets":[],
                     "counts":{"databaseCount":1 if operation=="CLI-002" else 9,"tenantCount":2,"clientCount":0,"catalogCount":4,"liveCaseCount":0},
                     "steps":[{"stepId":step,"state":state,"evidence":"evidence/controlled-owner.json"}],"error":None})
    private_json(native_root / "evidence/db-rbac.json", {"runId":native_run,"manifestSha256":native_digest,"role":"rbac",
                 "databaseName":names["rbac"],"oid":53,"databaseOwner":"controlled-native-owner","ownerComment":"xingyuan-it:"+native_run+":"+native_digest})
    native_registry = copy.deepcopy(formal_registry)
    native_registry.update(runId=native_run,manifestSha256=native_digest,publicOrigin=native_manifest["publicOrigin"])
    native_registry["resources"][0]["rbacApplicationCode"] = "tianshu-admin"
    native_registry["subjects"] = {"admin":"controlled-admin","viewer":"controlled-viewer"}
    native_registry["provisioningSubject"] = "controlled-provisioner"
    native_registry_file = native_root / "secrets/runtime-registry.json"
    private_json(native_registry_file,native_registry)
    native_token = copy.deepcopy(formal_tokens[0])
    native_token_file = native_root / "secrets/default-admin-tianshu.at"
    native_token_file.write_text("controlled-native-private-input"); native_token_file.chmod(0o600)
    native_token.update(tokenFile=str(native_token_file),tokenSha256=hashlib.sha256(native_token_file.read_bytes()).hexdigest())
    native_index = native_root / "secrets/token-index.json"
    native_index_body = {"schemaVersion":1,"runId":native_run,"manifestSha256":native_digest,"tokens":[native_token]}
    private_json(native_index,native_index_body)
    native_calls = root / "controlled-native-owner-calls"
    native_environment = dict(env,XINGYUAN_FAKE_CALLS=str(native_calls))
    wrapper.write_text(r'''#!/usr/bin/env python3
import datetime, hashlib, json, os, sys
from pathlib import Path
options = {value.split('=',1)[0]:value.split('=',1)[1] for value in sys.argv if value.startswith('-D') and '=' in value}
assert options['-Dtest']=='IsolatedAuthorizationProvisioningIT#provisionNativeAuthorization'
assert options['-Duser.timezone']=='UTC'
assert not {'JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','_JAVA_OPTIONS','MAVEN_OPTS','MAVEN_ARGS'} & os.environ.keys()
assert os.environ['XINGYUAN_ISOLATED_INTEGRATION']=='true'
assert 'controlled-native-private-input' not in ' '.join(sys.argv)
Path(os.environ['XINGYUAN_FAKE_CALLS']).write_text('controlled-native-owner-once\n')
manifest=Path(options['-Dxingyuan.isolated.manifest-file']); intent=json.loads(manifest.read_text()); root=Path(intent['runtimeRoot'])
mode=os.environ.get('XINGYUAN_FAKE_NATIVE_FAULT','')
if mode=='unknown-commit':
    print('XINGYUAN_COMMIT_UNKNOWN'); sys.exit(1)
now=datetime.datetime.now(datetime.timezone.utc)
header={'schemaVersion':1,'runId':intent['runId'],'manifestSha256':hashlib.sha256(manifest.read_bytes()).hexdigest(),
        'databaseOid':53,'status':'COMMITTED','recordedAt':now.isoformat().replace('+00:00','Z')}
actors={'1001':{'adminUserId':'5101','viewerUserId':'5102','provisionerUserId':'5103'},
        '1002':{'adminUserId':'5201','viewerUserId':'5202','provisionerUserId':'5203'}}
parts={'actorSha256':('native-actors.json',dict(header,actors=actors)),
       'catalogReviewSha256':('native-catalog-review.json',dict(header)),
       'assignmentSha256':('native-role-assignments.json',dict(header,scope='NATIVE_ROLE_ASSIGNMENTS_ONLY'))}
value=dict(header,phase='native',registrySha256=hashlib.sha256((root/'secrets/runtime-registry.json').read_bytes()).hexdigest())
for key,(name,document) in parts.items():
    path=root/'evidence'/name; path.write_text(json.dumps(document)); path.chmod(0o600)
    value[key]=hashlib.sha256(path.read_bytes()).hexdigest()
snapshots={}
for index,tenant in enumerate(('1001','1002')):
    for user in ('admin','viewer'):
        snapshots[tenant+':'+user]={'tenantId':tenant,'rbacUserId':actors[tenant][user+'UserId'],'authVersion':8,'policyVersion':12,
            'selectedRoleIds':[str(6101+index*20+(10 if user=='viewer' else 0)+i) for i in range(8 if user=='admin' else 4)],
            'snapshotChecksum':'d'*64,'expiresAt':(now+datetime.timedelta(seconds=300)).isoformat().replace('+00:00','Z')}
value['snapshots']=snapshots
if mode=='partial': value['status']='RUNNING'
elif mode=='wrong-hash': value['assignmentSha256']='0'*64
elif mode=='missing-snapshot': del snapshots['1002:viewer']
elif mode=='wrong-actor': snapshots['1001:admin']['rbacUserId']='5103'
elif mode=='duplicate-role': snapshots['1001:admin']['selectedRoleIds'][-1]=snapshots['1001:admin']['selectedRoleIds'][0]
elif mode=='platform-only': snapshots['1001:admin']['selectedRoleIds']=snapshots['1001:admin']['selectedRoleIds'][:4]
elif mode=='expired': snapshots['1001:admin']['expiresAt']='2000-01-01T00:00:00Z'
elif mode=='non-utc': value['recordedAt']=now.isoformat()
elif mode=='foreign-owner': value['databaseOid']=999
elif mode=='extra-field': value['token']='controlled-native-private-input'
elif mode=='changed-registry':
    path=root/'secrets/runtime-registry.json'; path.write_text(path.read_text()+'\n')
path=root/'evidence/authorization-native-owner.json'
if mode=='missing-owner': path.unlink(missing_ok=True)
else: path.write_text(json.dumps(value)); path.chmod(0o600)
''')
    wrapper.chmod(0o755)
    native_result = native_root / "evidence/native-result.json"
    native_arguments = command+["provision-authorization","--phase","native","--manifest",str(native_intent),
                              "--result",str(native_result),"--token-index",str(native_index)]
    response = subprocess.run(native_arguments,env=native_environment,cwd=root,capture_output=True,text=True,timeout=15)
    if response.returncode != 0 or response.stdout.strip() != "CLI-004 COMMITTED evidence/native-result.json":
        failures.append("native owner callback was not dispatched with one formal Tianshu input and four verified snapshots")
    else:
        canonical_native = native_root / "evidence/authorization-native.json"
        retained_native = canonical_native.read_bytes()
        result = json.loads(retained_native)
        if retained_native != native_result.read_bytes() or len(result["targets"]) != 4 or result["steps"] != [
                {"stepId":"NATIVE_AUTHORIZATION","state":"COMMITTED","evidence":"evidence/authorization-native-owner.json"}]:
            failures.append("native complete owner was projected into an incomplete or wrong-phase public receipt")
        if any(secret in retained_native.decode() for secret in ("5101","controlled-native-private-input","controlled-admin")):
            failures.append("native public receipt exposed private owner or bearer material")
        native_calls.unlink()
        repeated=subprocess.run(native_arguments,env=native_environment,cwd=root,capture_output=True,text=True,timeout=15)
        if repeated.returncode!=5 or native_calls.exists(): failures.append("native owner repeated without explicit resume")
        resumed=subprocess.run(native_arguments+["--resume"],env=native_environment,cwd=root,capture_output=True,text=True,timeout=15)
        if resumed.returncode!=0: failures.append("native explicit resume did not revalidate its original owner")
        retained_native=canonical_native.read_bytes()
        for fault,expected in (("partial",7),("wrong-hash",7),("missing-snapshot",7),("wrong-actor",7),("duplicate-role",7),
                ("platform-only",7),("expired",7),("non-utc",7),("foreign-owner",7),("extra-field",7),("missing-owner",4),
                ("changed-registry",3),("unknown-commit",9)):
            outcome=subprocess.run(native_arguments+["--resume"],env=dict(native_environment,XINGYUAN_FAKE_NATIVE_FAULT=fault),
                                   cwd=root,capture_output=True,text=True,timeout=15)
            if outcome.returncode!=expected or canonical_native.read_bytes()!=retained_native:
                failures.append("native "+fault+" replaced a confirmed result or returned "+str(outcome.returncode))
            private_json(native_registry_file,native_registry)
            assert "controlled-native-private-input" not in outcome.stdout+outcome.stderr
        for fault,expected in (("missing-platform",4),("wrong-platform-phase",5),("expired-token",6),("service-token",3),("wrong-token-hash",3)):
            native_calls.unlink(missing_ok=True)
            platform_path=native_root/"evidence/authorization-platform.json"; platform_bytes=platform_path.read_bytes()
            changed_index=copy.deepcopy(native_index_body); changed_registry=copy.deepcopy(native_registry)
            if fault=="missing-platform": platform_path.unlink()
            elif fault=="wrong-platform-phase":
                changed=json.loads(platform_bytes); changed["steps"][0]["stepId"]="NATIVE_AUTHORIZATION"; private_json(platform_path,changed)
            elif fault=="expired-token": changed_index["tokens"][0]["expiresAt"]="2000-01-01T00:00:00Z"
            elif fault=="service-token": changed_registry["clients"][0].update(clientType="CONFIDENTIAL",purpose="SERVICE")
            else: changed_index["tokens"][0]["tokenSha256"]="0"*64
            private_json(native_index,changed_index); private_json(native_registry_file,changed_registry)
            rejected=subprocess.run(native_arguments+["--resume"],env=native_environment,cwd=root,capture_output=True,text=True,timeout=15)
            if rejected.returncode!=expected or native_calls.exists(): failures.append("native "+fault+" reached the owner or returned "+str(rejected.returncode))
            platform_path.write_bytes(platform_bytes); platform_path.chmod(0o600)
            private_json(native_index,native_index_body); private_json(native_registry_file,native_registry)
    assert "controlled-native-private-input" not in response.stdout+response.stderr

    node = shutil.which("node")
    if node is None:
        failures.append("existing Node runtime is unavailable for browser input guards")
    else:
        program = r"""
import assert from 'node:assert/strict'
import { mkdtempSync, realpathSync, writeFileSync, chmodSync, symlinkSync, mkdirSync, rmSync } from 'node:fs'
import { join } from 'node:path'
import { tmpdir } from 'node:os'
import { createHash } from 'node:crypto'
process.argv.push('--list')
const { bffRefreshWaitMilliseconds, readIntegrationJson, readPrivateFile, loadIntegrationInputs, runtimeProbeInputs, canonicalSessionProof } = await import('./playwright.integration.config.ts')
const businessAliases = ['edge-shoubing', 'edge-jianshen', 'edge-yuheng', 'edge-tianshu']
const refreshClients = ['default', 'tenant-b'].flatMap(realmCode => [
  { realmCode, alias: 'edge-ui', accessTokenTtlSeconds: 600 },
  ...businessAliases.map(alias => ({ realmCode, alias, accessTokenTtlSeconds: realmCode === 'default' ? 300 : 600 })),
])
assert.equal(bffRefreshWaitMilliseconds(refreshClients, 'default'), 241000)
assert.equal(bffRefreshWaitMilliseconds(refreshClients, 'tenant-b'), 541000)
assert.throws(() => bffRefreshWaitMilliseconds(refreshClients.filter(value => value.alias !== 'edge-tianshu'), 'default'), { message: 'XINGYUAN_TARGET_UNSAFE' })
assert.throws(() => bffRefreshWaitMilliseconds([...refreshClients, refreshClients[1]], 'default'), { message: 'XINGYUAN_TARGET_UNSAFE' })
assert.equal(readIntegrationJson(Buffer.from('{"clientPk":9223372036854775807}')).clientPk, '9223372036854775807')
const boundedRegistryMetadata = Buffer.from(JSON.stringify({ metadata: '目录'.repeat(100000) }))
assert.equal(readIntegrationJson(boundedRegistryMetadata).metadata.length, 200000)
assert.equal(readIntegrationJson(Buffer.from('{"metadata":"目录😀","clientPk":9223372036854775807}')).metadata, '目录😀')
for (const json of ['{"id":1,"id":2}', '{} {}', '{"number":NaN}', '{"number":Infinity}']) {
  assert.throws(() => readIntegrationJson(Buffer.from(json)), { message: 'XINGYUAN_INPUT_INVALID' })
}
delete process.env.DEBUG
delete process.env.PWDEBUG
delete process.env.NODE_TLS_REJECT_UNAUTHORIZED
delete process.env.XINGYUAN_ISOLATED_INTEGRATION
assert.throws(() => loadIntegrationInputs(), { message: 'XINGYUAN_TARGET_UNSAFE' })
process.env.XINGYUAN_ISOLATED_INTEGRATION = 'true'
delete process.env.XINGYUAN_INTEGRATION_MANIFEST
assert.throws(() => loadIntegrationInputs(), { message: 'XINGYUAN_INPUT_UNAVAILABLE' })
const directory = mkdtempSync(join(realpathSync(tmpdir()), 'xingyuan-browser-guards-'))
try {
  const artifacts = join(directory, 'artifacts'); mkdirSync(artifacts, { mode: 0o700 })
  const fixture = join(artifacts, 'task-runtime-probe.jar'); writeFileSync(fixture, 'controlled-file-hash-only', { mode: 0o600 })
  const fixtureSha256 = createHash('sha256').update('controlled-file-hash-only').digest('hex')
  const fixtureManifest = { policyFiles: [{ path: fixture, sha256: fixtureSha256 }], artifacts: { edge: { sha256: 'd'.repeat(64) } } }
  assert.deepEqual(runtimeProbeInputs(fixtureManifest, directory), { fixtureSha256, artifactSha256: 'd'.repeat(64) })
  assert.equal(runtimeProbeInputs({ ...fixtureManifest, policyFiles: [] }, directory), undefined)
  for (const policyFiles of [[{ path: fixture, sha256: '0'.repeat(64) }], [...fixtureManifest.policyFiles, ...fixtureManifest.policyFiles],
    [{ path: join(directory, 'foreign/task-runtime-probe.jar'), sha256: fixtureSha256 }]]) {
    assert.throws(() => runtimeProbeInputs({ ...fixtureManifest, policyFiles }, directory), { message: 'XINGYUAN_TARGET_UNSAFE' })
  }
  const expected = { runId: 'controlled-run', manifestSha256: 'a'.repeat(64), fixtureSha256, artifactSha256: 'd'.repeat(64), nonce: 'controlled-nonce',
    issuer: 'https://127.0.0.1:28443/tenants/1001', tenantId: '1001', issuedAt: '2026-10-15T00:00:00Z', expiresAt: '2026-10-15T00:01:00Z' }
  const result = { bindingReference: 'b'.repeat(64), issuer: expected.issuer, tenantId: expected.tenantId, generation: 1,
    originalCreatedAt: '2026-10-14T23:00:00Z', absoluteExpiresAt: '2026-10-15T01:00:00Z' }
  const envelope = { schemaVersion: 1, runId: expected.runId, manifestSha256: expected.manifestSha256, role: 'edge', fixtureSha256,
    artifactSha256: expected.artifactSha256, nonce: expected.nonce, operation: 'EDGE_BINDING', status: 'COMPLETED', completedAt: '2026-10-15T00:00:30Z', result }
  assert.deepEqual(canonicalSessionProof(envelope, expected), result)
  for (const changed of [{ runId: 'foreign' }, { manifestSha256: '0'.repeat(64) }, { fixtureSha256: '0'.repeat(64) }, { artifactSha256: '0'.repeat(64) },
    { nonce: 'foreign' }, { role: 'native-web' }, { completedAt: '2026-10-15T00:01:01Z' }, { completedAt: '2026-10-14T23:59:59Z' },
    { completedAt: '2026-10-15T00:00:30+00:00' }, { token: 'unexpected-secret' }]) {
    assert.throws(() => canonicalSessionProof({ ...envelope, ...changed }, expected), { message: 'XINGYUAN_VERIFICATION_FAILED' })
  }
  for (const changed of [{ issuer: 'https://foreign' }, { tenantId: '1002' }, { generation: 0 }, { generation: Number.MAX_SAFE_INTEGER + 1 },
    { originalCreatedAt: '2026-10-15T00:00:31Z' }, { absoluteExpiresAt: '2026-10-15T00:00:30Z' }, { bindingReference: 'invalid' }, { cookie: 'unexpected-secret' }]) {
    assert.throws(() => canonicalSessionProof({ ...envelope, result: { ...result, ...changed } }, expected), { message: 'XINGYUAN_VERIFICATION_FAILED' })
  }
  const file = join(directory, 'input.json')
  writeFileSync(file, '{}', { mode: 0o600, flag: 'wx' })
  assert.equal(readPrivateFile(file, directory).toString(), '{}')
  chmodSync(file, 0o644)
  assert.throws(() => readPrivateFile(file, directory), { message: 'XINGYUAN_TARGET_UNSAFE' })
  chmodSync(file, 0o600)
  assert.throws(() => readPrivateFile(file, join(directory, 'foreign-root')), { message: 'XINGYUAN_TARGET_UNSAFE' })
  const link = join(directory, 'link.json')
  symlinkSync(file, link)
  assert.throws(() => readPrivateFile(link, directory), { message: 'XINGYUAN_TARGET_UNSAFE' })
  writeFileSync(file, 'oversized')
  assert.throws(() => readPrivateFile(file, directory, 4), { message: 'XINGYUAN_INPUT_INVALID' })
} finally {
  rmSync(directory, { recursive: true })
}
console.log('PASS actual browser config strict JSON, exact Long, opt-in and private file guards; no browser')
"""
        config_env = dict(os.environ)
        config_env.pop("XINGYUAN_INTEGRATION_MANIFEST", None)
        config_env.pop("XINGYUAN_ISOLATED_INTEGRATION", None)
        checked = subprocess.run([node, '--input-type=module', '-e', program],
                                 cwd=source / 'egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web',
                                 env=config_env, capture_output=True, text=True, timeout=15)
        if checked.returncode != 0:
            failures.append("actual browser input guards failed: " + checked.stderr[-3000:])
        else:
            print(checked.stdout.strip())
    if failures:
        print("\n".join(failures), file=sys.stderr)
        sys.exit(1)
    print("PASS isolated CLI guards, owner dispatch and controlled preparation/recovery; zero external facilities")
PYTEST
