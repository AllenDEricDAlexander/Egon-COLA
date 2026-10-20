#!/usr/bin/env python3
"""Private standard-library implementation for the isolated Xingyuan command wrapper."""

def validate_manifest(argv):
    import hashlib, json, os, re, stat, sys, uuid
    from pathlib import Path

    def reject(code):
        sys.exit(code)

    def keys(value, allowed, required=None):
        if not isinstance(value, dict) or set(value) - set(allowed) or set(allowed if required is None else required) - set(value):
            reject(2)

    def text(value, maximum=4096):
        if not isinstance(value, str) or not value or len(value) > maximum or any(ord(c) < 32 for c in value):
            reject(2)

    def integer(value, low, high):
        if type(value) is not int or not low <= value <= high:
            reject(2)

    def absolute(value):
        text(value)
        path = Path(value)
        if not path.is_absolute() or str(path) != value or '..' in path.parts or '.' in path.parts:
            reject(3)
        if any(parent.is_symlink() for parent in (path, *path.parents)):
            reject(3)
        return path

    def unique_object(pairs):
        result = {}
        for key, value in pairs:
            if key in result:
                raise ValueError('duplicate key')
            result[key] = value
        return result

    file = absolute(argv[1])
    try:
        info = file.lstat()
        if not stat.S_ISREG(info.st_mode) or info.st_uid != os.getuid() or stat.S_IMODE(info.st_mode) != 0o600:
            reject(3)
        if info.st_size > 128 * 1024:
            reject(2)
        fd = os.open(file, os.O_RDONLY | os.O_NOFOLLOW)
        with os.fdopen(fd, 'rb') as stream:
            contents = stream.read(128 * 1024 + 1)
        if len(contents) > 128 * 1024:
            reject(2)
        after = file.lstat()
        if (info.st_ino, info.st_size, info.st_mtime_ns) != (after.st_ino, after.st_size, after.st_mtime_ns):
            reject(3)
        document = json.loads(contents, object_pairs_hook=unique_object,
                              parse_constant=lambda value: (_ for _ in ()).throw(ValueError('nonfinite')))
    except (ValueError, UnicodeError):
        reject(2)
    except OSError:
        reject(4)
    fields = ('schemaVersion', 'runId', 'repoRoot', 'runtimeRoot', 'publicOrigin', 'postgres', 'redis',
              'databases', 'platformStoragePartitionId', 'tenantCodes', 'userRecipe', 'authorizationRecipe',
              'processes', 'artifacts', 'secretFiles', 'deadlines', 'catalogFiles', 'policyFiles')
    keys(document, fields)
    integer(document['schemaVersion'], 1, 1)
    text(document['runId'], 36)
    try:
        if str(uuid.UUID(document['runId'])) != document['runId']:
            reject(2)
    except ValueError:
        reject(2)
    repo = absolute(document['repoRoot'])
    root = absolute(document['runtimeRoot'])
    if str(repo) != argv[2] or root != repo / ('target/xingyuan-integration-' + document['runId']):
        reject(3)
    if not file.is_relative_to(root):
        reject(3)
    if root.exists():
        root_info = root.lstat()
        if not stat.S_ISDIR(root_info.st_mode) or root_info.st_uid != os.getuid() or stat.S_IMODE(root_info.st_mode) != 0o700:
            reject(3)
    if document['publicOrigin'] != 'https://127.0.0.1:28443':
        reject(3)
    keys(document['postgres'], ('endpoint', 'maintenanceDatabase', 'credentialFile'))
    keys(document['redis'], ('endpoint', 'database', 'credentialFile'))
    if document['postgres']['endpoint'] != '127.0.0.1:5432' or document['postgres']['maintenanceDatabase'] != 'postgres':
        reject(3)
    if document['redis']['endpoint'] != 'redis://127.0.0.1:6379':
        reject(3)
    integer(document['redis']['database'], 1, 15)
    absolute(document['postgres']['credentialFile'])
    absolute(document['redis']['credentialFile'])
    roles = ('idp', 'rbac', 'tianshu', 'yuheng', 'llm', 'light', 'web', 'service', 'agent')
    if not isinstance(document['databases'], list) or not 9 <= len(document['databases']) <= 12:
        reject(2)
    seen = set()
    for target in document['databases']:
        keys(target, ('role', 'name'))
        if target['role'] not in roles or target['name'] != 'xingyuan_it_' + target['role'] + '_20261011' or target['role'] in seen:
            reject(3)
        seen.add(target['role'])
    if seen != set(roles):
        reject(3)
    partition = document['platformStoragePartitionId']
    text(partition, 19)
    if not re.fullmatch(r'[1-9][0-9]{0,18}', partition) or int(partition) > 9223372036854775807:
        reject(2)
    if document['tenantCodes'] != ['default', 'tenant-b'] or document['userRecipe'] != 'local-two-tenant-users-v1' or document['authorizationRecipe'] != 'local-explicit-role-fields-v1':
        reject(3)
    keys(document['deadlines'], ('startupSeconds', 'operationSeconds', 'httpSeconds'))
    for name, maximum in (('startupSeconds', 300), ('operationSeconds', 900), ('httpSeconds', 30)):
        integer(document['deadlines'][name], 1, maximum)
    for name in ('processes', 'catalogFiles', 'policyFiles'):
        if not isinstance(document[name], list) or len(document[name]) > 32:
            reject(2)
    for name in ('artifacts', 'secretFiles'):
        if not isinstance(document[name], dict) or len(document[name]) > 32:
            reject(2)
    processes = {
        'nginx': ('NGINX', 28443, []),
        'shoubing': ('JAVA', 28120, [28122]),
        'jianshen': ('JAVA', 28130, []),
        'tianshu': ('JAVA', 28150, [29080]),
        'yuheng': ('JAVA', 28140, []),
        'biz': ('JAVA', 28180, [28182]),
        'mcp': ('JAVA', 28185, [28186]),
        'llm': ('JAVA', 28190, []),
        'edge': ('JAVA', 28200, []),
        'portal-web': ('NODE', 28125, []),
        'shoubing-web': ('NODE', 28121, []),
        'jianshen-web': ('NODE', 28131, []),
        'yuheng-web': ('NODE', 28141, []),
        'tianshu-web': ('NODE', 28152, []),
        'native-light': ('JAVA', 28310, [29310]),
        'native-web': ('JAVA', 28311, [29311]),
        'native-service': ('JAVA', 28312, [29312]),
        'native-agent': ('JAVA', 28313, [])}
    seen_processes, seen_ports, seen_artifacts = set(), set(), set()
    for process in document['processes']:
        keys(process, ('id', 'artifactRole', 'kind', 'port', 'additionalPorts'),
             ('id', 'artifactRole', 'kind', 'port'))
        for field in ('id', 'artifactRole', 'kind'):
            text(process[field], 128)
        integer(process['port'], 1, 65535)
        additional = process.get('additionalPorts', [])
        if not isinstance(additional, list) or len(additional) > 2:
            reject(2)
        for port in additional:
            integer(port, 1, 65535)
        identity = process['id']
        if (identity not in processes or identity in seen_processes or process['artifactRole'] != identity
                or (process['kind'], process['port'], additional) != processes[identity]
                or process['artifactRole'] in seen_artifacts):
            reject(3)
        for port in [process['port'], *additional]:
            if port in seen_ports:
                reject(3)
            seen_ports.add(port)
        seen_processes.add(identity)
        seen_artifacts.add(process['artifactRole'])

    def task_file(value, directory):
        path = absolute(value)
        if not path.is_relative_to(directory) or path == directory:
            reject(3)
        for parent in (path.parent, *path.parent.parents):
            if parent == root.parent:
                break
            if parent.exists():
                metadata = parent.lstat()
                if not stat.S_ISDIR(metadata.st_mode) or metadata.st_uid != os.getuid() or stat.S_IMODE(metadata.st_mode) != 0o700:
                    reject(3)
        if path.exists():
            metadata = path.lstat()
            if not stat.S_ISREG(metadata.st_mode) or metadata.st_uid != os.getuid() or stat.S_IMODE(metadata.st_mode) != 0o600:
                reject(3)
        return path

    result = task_file(argv[3], root / 'evidence')
    for value in argv[4:6]:
        if value:
            reference = task_file(value, root / 'secrets')
            if reference == result or reference == file:
                reject(3)
    for reference in document['catalogFiles'] + document['policyFiles']:
        keys(reference, ('path', 'sha256'))
        if not absolute(reference['path']).is_relative_to(root):
            reject(3)
        if not isinstance(reference['sha256'], str) or not re.fullmatch(r'[0-9a-f]{64}', reference['sha256']):
            reject(2)
    for reference in document['artifacts'].values():
        keys(reference, ('path', 'sha256', 'buildId'))
        if not absolute(reference['path']).is_relative_to(repo):
            reject(3)
        if not isinstance(reference['sha256'], str) or not re.fullmatch(r'[0-9a-f]{64}', reference['sha256']):
            reject(2)
        text(reference['buildId'], 128)
    for name, value in document['secretFiles'].items():
        text(name, 64)
        if not absolute(value).is_relative_to(root / 'secrets'):
            reject(3)


def provision_identity(argv):
    import fcntl, hashlib, json, os, re, stat, subprocess, sys, tempfile
    from pathlib import Path

    def reject(code):
        sys.exit(code)

    def protected(path, root=None, maximum=128 * 1024):
        if not path.is_absolute() or str(path) != str(path.absolute()) or '..' in path.parts:
            reject(3)
        if root is not None and (not path.is_relative_to(root) or path == root):
            reject(3)
        if any(parent.is_symlink() for parent in (path, *path.parents)):
            reject(3)
        try:
            before = path.lstat()
            if not stat.S_ISREG(before.st_mode) or before.st_uid != os.getuid() or stat.S_IMODE(before.st_mode) != 0o600:
                reject(3)
            if before.st_size > maximum:
                reject(2)
            with os.fdopen(os.open(path, os.O_RDONLY | os.O_NOFOLLOW), 'rb') as stream:
                data = stream.read(maximum + 1)
            after = path.lstat()
            if len(data) > maximum:
                reject(2)
            if (before.st_ino, before.st_size, before.st_mtime_ns) != (after.st_ino, after.st_size, after.st_mtime_ns):
                reject(3)
            return data
        except OSError:
            reject(4)

    def unique(pairs):
        values = {}
        for key, value in pairs:
            if key in values:
                reject(2)
            values[key] = value
        return values

    def document(data):
        try:
            return json.loads(data, object_pairs_hook=unique,
                              parse_constant=lambda value: (_ for _ in ()).throw(ValueError()))
        except (ValueError, UnicodeError):
            reject(2)

    manifest_file, result_file = Path(argv[1]), Path(argv[2])
    resume, repository = argv[3], Path(argv[4])
    contents = protected(manifest_file)
    manifest = document(contents)
    digest = hashlib.sha256(contents).hexdigest()
    root = Path(manifest['runtimeRoot'])
    if root != repository / ('target/xingyuan-integration-' + manifest['runId']):
        reject(3)
    receipt_file = root / 'evidence/db-idp.json'
    receipt = document(protected(receipt_file, root / 'evidence'))
    if (receipt.get('runId') != manifest['runId'] or receipt.get('manifestSha256') != digest
            or receipt.get('role') != 'idp' or receipt.get('databaseName') != 'xingyuan_it_idp_20261011'
            or type(receipt.get('oid')) is not int or receipt['oid'] <= 0
            or receipt.get('ownerComment') != 'xingyuan-it:' + manifest['runId'] + ':' + digest
            or not isinstance(receipt.get('databaseOwner'), str) or not receipt['databaseOwner']):
        reject(3)
    prepared_file = root / 'evidence/prepared.json'
    prepared = document(protected(prepared_file, root / 'evidence'))
    if (prepared.get('schemaVersion') != 1 or prepared.get('operationId') != 'CLI-001'
            or prepared.get('runId') != manifest['runId'] or prepared.get('manifestSha256') != digest
            or prepared.get('status') != 'COMMITTED' or prepared.get('counts', {}).get('databaseCount') != 9):
        reject(4)
    for role, size in (('oauth-kek', 32), ('admin-password', 4096), ('viewer-password', 4096), ('provisioner-password', 4096)):
        reference = manifest['secretFiles'].get(role)
        if reference is None:
            reject(4)
        data = protected(Path(reference), root / 'secrets', size)
        if role == 'oauth-kek' and len(data) != 32 or role.endswith('-password') and len(data.rstrip(b'\r\n')) < 12:
            reject(2)
    recipes = [value for value in manifest['policyFiles'] if Path(value['path']).name == 'identity-provisioning.json']
    if len(recipes) != 1:
        reject(4)
    recipe_data = protected(Path(recipes[0]['path']), root, 2 * 1024 * 1024)
    if hashlib.sha256(recipe_data).hexdigest() != recipes[0]['sha256']:
        reject(3)
    for reference in manifest['artifacts'].values():
        path = Path(reference['path'])
        if not path.is_absolute() or not path.is_relative_to(repository) or any(parent.is_symlink() for parent in (path, *path.parents)):
            reject(3)
        if not path.is_file():
            reject(4)
        if hashlib.sha256(path.read_bytes()).hexdigest() != reference['sha256']:
            reject(3)
    if not manifest['artifacts']:
        reject(4)
    lock_file = root / 'evidence/operation.lock'
    if lock_file.exists():
        protected(lock_file, root / 'evidence', 1024)
    try:
        lock_fd = os.open(lock_file, os.O_RDWR | os.O_CREAT | os.O_NOFOLLOW, 0o600)
        fcntl.flock(lock_fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
    except (OSError, BlockingIOError):
        reject(5)
    canonical_identity = root / 'evidence/identity.json'
    canonical_bytes = protected(canonical_identity, root / 'evidence') if canonical_identity.exists() else None
    if canonical_bytes is not None:
        previous_identity = document(canonical_bytes)
        if (resume != 'true' or previous_identity.get('operationId') != 'CLI-002'
                or previous_identity.get('runId') != manifest['runId'] or previous_identity.get('manifestSha256') != digest
                or previous_identity.get('status') != 'COMMITTED'):
            reject(5)
    if result_file.exists() and resume != 'true':
        reject(5)
    log = root / 'evidence/provision-identity-owner.log'
    try:
        fd = os.open(log, os.O_WRONLY | os.O_CREAT | os.O_EXCL | os.O_NOFOLLOW, 0o600)
    except FileExistsError:
        if resume != 'true':
            reject(5)
        protected(log, root / 'evidence', 16 * 1024 * 1024)
        fd = os.open(log, os.O_WRONLY | os.O_APPEND | os.O_NOFOLLOW)
    args = [str(repository / 'mvnw'), '-B', '-ntp', '-o', '-Duser.timezone=UTC', '-pl',
            'egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin', '-am',
            '-Dtest=IsolatedOAuthProvisioningIT#provisionIdentity', '-Dsurefire.failIfNoSpecifiedTests=false',
            '-Dxingyuan.isolated.manifest-file=' + str(manifest_file), '-Dxingyuan.isolated.receipt-file=' + str(receipt_file),
            '-Dxingyuan.isolated.repository-root=' + str(repository), '-Dxingyuan.isolated.result-file=' + str(result_file),
            '-Dxingyuan.isolated.resume=' + resume, 'test']
    settings = [reference for reference in manifest['policyFiles'] if Path(reference['path']).name == 'maven-settings.xml']
    if len(settings) > 1:
        reject(2)
    if settings:
        if Path(settings[0]['path']) != root / 'policies/maven-settings.xml':
            reject(3)
        if hashlib.sha256(protected(Path(settings[0]['path']), root, 2 * 1024 * 1024)).hexdigest() != settings[0]['sha256']:
            reject(3)
        args[1:1] = ['-s', settings[0]['path']]
    environment = dict(os.environ, XINGYUAN_ISOLATED_INTEGRATION='true')
    for name in ('JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', '_JAVA_OPTIONS', 'MAVEN_OPTS', 'MAVEN_ARGS'):
        environment.pop(name, None)
    try:
        with os.fdopen(fd, 'ab') as output:
            outcome = subprocess.run(args, cwd=repository, env=environment, stdout=output, stderr=subprocess.STDOUT,
                                     stdin=subprocess.DEVNULL, timeout=manifest['deadlines']['operationSeconds'])
    except (OSError, subprocess.TimeoutExpired):
        reject(6)
    if outcome.returncode != 0:
        text = protected(log, root / 'evidence', 16 * 1024 * 1024).decode('utf-8', errors='replace')
        exits = {'XINGYUAN_INPUT_INVALID': 2, 'XINGYUAN_TARGET_UNSAFE': 3, 'XINGYUAN_INPUT_UNAVAILABLE': 4,
                 'XINGYUAN_STATE_CONFLICT': 5, 'XINGYUAN_DEPENDENCY_UNAVAILABLE': 6, 'XINGYUAN_PARTIAL': 7,
                 'XINGYUAN_COMMIT_UNKNOWN': 9}
        found = [value for key, value in exits.items() if key in text]
        reject(9 if 9 in found else found[-1] if found else 6)
    result = document(protected(result_file, root / 'evidence'))
    if (result.get('schemaVersion') != 1 or result.get('operationId') != 'CLI-002' or result.get('runId') != manifest['runId']
            or result.get('manifestSha256') != digest or result.get('status') != 'COMMITTED'
            or result.get('counts', {}).get('tenantCount') != 2 or result.get('error') is not None):
        reject(7)
    required = {'IDENTITY', 'REALMS', 'SECURITY', 'MANAGEMENT_CLIENTS', 'RESOURCES', 'CLIENTS', 'GRANTS'}
    if {value.get('stepId') for value in result.get('steps', []) if value.get('state') == 'COMMITTED'} != required:
        reject(7)
    registry = document(protected(root / 'secrets/runtime-registry.json', root / 'secrets', 2 * 1024 * 1024))
    if registry.get('runId') != manifest['runId'] or registry.get('manifestSha256') != digest:
        reject(9)
    subjects = registry.get('subjects')
    provisioner = registry.get('provisioningSubject')
    if (not isinstance(subjects, dict) or set(subjects) != {'admin', 'viewer'}
            or not isinstance(provisioner, str) or not re.fullmatch(r'[A-Za-z0-9._:-]{1,128}', provisioner)
            or any(not isinstance(value, str) or not value.strip() or len(value) > 200 for value in subjects.values())
            or len(set(subjects.values())) != 2 or provisioner in subjects.values()):
        reject(9)
    verified_result = protected(result_file, root / 'evidence')
    if canonical_identity != result_file:
        if canonical_bytes is not None:
            if canonical_bytes != verified_result:
                reject(5)
        else:
            fd, staged = tempfile.mkstemp(prefix='.identity-', suffix='.json', dir=canonical_identity.parent)
            try:
                with os.fdopen(fd, 'wb') as output:
                    output.write(verified_result)
                    output.flush()
                    os.fsync(output.fileno())
                os.link(staged, canonical_identity, follow_symlinks=False)
                directory_fd = os.open(canonical_identity.parent, os.O_RDONLY)
                try:
                    os.fsync(directory_fd)
                finally:
                    os.close(directory_fd)
            except FileExistsError:
                reject(5)
            finally:
                os.unlink(staged)
    print('CLI-002 COMMITTED ' + str(result_file.relative_to(root)))


def original_database_facts(manifest, query):
    """Read identities of non-task PostgreSQL databases without entering their schemas."""
    import sys
    names=[value['name'] for value in manifest['databases']]
    rows=decode_task_json(query("SELECT coalesce(json_agg(json_build_object('databaseName',datname,'oid',oid::bigint,'databaseOwner',pg_get_userbyid(datdba)) ORDER BY datname),'[]'::json)::text /* XINGYUAN_ORIGINAL_DATABASES */ FROM pg_database WHERE datname NOT IN ("
                               +','.join("'"+name+"'" for name in names)+");").encode('utf-8'))
    if not isinstance(rows,list) or len(rows)>4096:
        sys.exit(6)
    result={}
    oids=set()
    for value in rows:
        if (not isinstance(value,dict) or set(value)!={'databaseName','oid','databaseOwner'}
                or not isinstance(value['databaseName'],str) or not 1<=len(value['databaseName'])<=256
                or value['databaseName'] in names or value['databaseName'] in result
                or type(value['oid']) is not int or value['oid']<=0 or value['oid'] in oids
                or not isinstance(value['databaseOwner'],str) or not 1<=len(value['databaseOwner'])<=256):
            sys.exit(3)
        result[value['databaseName']]=value
        oids.add(value['oid'])
    return dict(sorted(result.items()))


def require_original_databases(baseline, current):
    import sys
    if not isinstance(baseline,dict) or not isinstance(current,dict):
        sys.exit(3)
    for facts in (baseline,current):
        oids=set()
        for name,value in facts.items():
            if (not isinstance(name,str) or not 1<=len(name)<=256 or not isinstance(value,dict)
                    or set(value)!={'databaseName','oid','databaseOwner'} or value['databaseName']!=name
                    or type(value['oid']) is not int or value['oid']<=0 or value['oid'] in oids
                    or not isinstance(value['databaseOwner'],str) or not 1<=len(value['databaseOwner'])<=256):
                sys.exit(3)
            oids.add(value['oid'])
    if any(current.get(name)!=value for name,value in baseline.items()):
        sys.exit(3)


def read_original_database_baseline(manifest, digest):
    import datetime
    import sys
    from pathlib import Path
    root=Path(manifest['runtimeRoot'])
    value=decode_task_json(read_task_file(root/'evidence/original-databases.json',root/'evidence'))
    if (not isinstance(value,dict) or set(value)!={'schemaVersion','runId','manifestSha256','status','observedAt','databases'}
            or type(value.get('schemaVersion')) is not int or value['schemaVersion']!=1 or value.get('runId')!=manifest['runId']
            or value.get('manifestSha256')!=digest or value.get('status')!='CAPTURED' or not isinstance(value.get('databases'),dict)):
        sys.exit(3)
    try:
        observed=datetime.datetime.fromisoformat(value['observedAt'].replace('Z','+00:00'))
        if (not value['observedAt'].endswith('Z') or observed.tzinfo is None or observed.utcoffset()!=datetime.timedelta()
                or observed>datetime.datetime.now(datetime.timezone.utc)):
            sys.exit(3)
    except (ValueError,TypeError,AttributeError):
        sys.exit(3)
    require_original_databases(value['databases'],value['databases'])
    return value


def verify_original_databases(manifest, digest, evidence):
    """Reobserve original database identities through the prepared task's exact maintenance endpoint."""
    import datetime
    import hashlib
    import subprocess
    import sys
    from pathlib import Path
    root=Path(manifest['runtimeRoot'])
    baseline=read_original_database_baseline(manifest,digest)
    receipt=decode_task_json(read_task_file(root/'evidence/db-idp.json',root/'evidence'))
    if (not isinstance(receipt,dict) or receipt.get('runId')!=manifest['runId'] or receipt.get('manifestSha256')!=digest or receipt.get('role')!='idp'
            or receipt.get('databaseName')!='xingyuan_it_idp_20261011' or not isinstance(receipt.get('databaseOwner'),str)
            or not receipt['databaseOwner'] or len(receipt['databaseOwner'])>63 or any(ord(ch)<32 for ch in receipt['databaseOwner'])):
        sys.exit(3)
    environment=isolated_runtime_environment()
    environment.update(PGPASSWORD=task_credential(Path(manifest['postgres']['credentialFile'])),PGCONNECT_TIMEOUT='5')
    def query(sql):
        try:
            result=subprocess.run(['psql','-X','--no-password','-A','-t','-v','ON_ERROR_STOP=1','-h','127.0.0.1','-p','5432',
                                   '-U',receipt['databaseOwner'],'-d','postgres'],input=sql,text=True,
                                  stdout=subprocess.PIPE,stderr=subprocess.PIPE,env=environment,timeout=manifest['deadlines']['httpSeconds'])
        except (OSError,subprocess.TimeoutExpired):
            sys.exit(6)
        if result.returncode or len(result.stdout)>4*1024*1024:
            sys.exit(6)
        return result.stdout.strip()
    current=original_database_facts(manifest,query)
    require_original_databases(baseline['databases'],current)
    proof={'schemaVersion':1,'runId':manifest['runId'],'manifestSha256':digest,'status':'PASSED','scope':'ORIGINAL_DATABASE_IDENTITIES_ONLY',
           'baselineSha256':hashlib.sha256(read_task_file(root/'evidence/original-databases.json',root/'evidence')).hexdigest(),
           'originalDatabaseCount':len(baseline['databases']),'observedAt':datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00','Z')}
    publish_task_json(Path(evidence)/'original-databases-preserved.json',root,proof)
    return proof


def require_original_nginx(baseline, current):
    import datetime
    import re
    import sys
    if not isinstance(baseline,dict) or not isinstance(current,dict):
        sys.exit(3)
    for facts in (baseline,current):
        for pid,value in facts.items():
            if (not isinstance(value,dict) or set(value)!={'pid','uid','processGroup','startInstant','commandSha256','listenPorts'}
                    or type(value['pid']) is not int or value['pid']<=0 or str(value['pid'])!=pid
                    or type(value['uid']) is not int or value['uid']<0 or type(value['processGroup']) is not int or value['processGroup']<=0
                    or not isinstance(value['commandSha256'],str) or not re.fullmatch(r'[0-9a-f]{64}',value['commandSha256'])
                    or not isinstance(value['listenPorts'],list) or any(type(port) is not int or not 1<=port<=65535 for port in value['listenPorts'])
                    or value['listenPorts']!=sorted(set(value['listenPorts']))):
                sys.exit(3)
            try:
                started=datetime.datetime.fromisoformat(value['startInstant'].replace('Z','+00:00'))
                if not value['startInstant'].endswith('Z') or started.tzinfo is None or started.utcoffset()!=datetime.timedelta():
                    sys.exit(3)
            except (ValueError,TypeError,AttributeError):
                sys.exit(3)
    if any(current.get(pid)!=value for pid,value in baseline.items()):
        sys.exit(3)


def original_nginx_facts(manifest):
    """Observe existing nginx processes and listener ports, with no nginx control operation."""
    import datetime
    import os
    import re
    import subprocess
    import sys
    from pathlib import Path
    environment=isolated_runtime_environment()
    environment.update(LC_ALL='C',TZ='UTC')
    try:
        inventory=subprocess.run(['ps','-axo','pid=,uid=,lstart=,comm='],stdin=subprocess.DEVNULL,stdout=subprocess.PIPE,stderr=subprocess.PIPE,
                                 text=True,env=environment,timeout=manifest['deadlines']['httpSeconds'])
    except (OSError,subprocess.TimeoutExpired):
        sys.exit(6)
    if inventory.returncode or inventory.stderr.strip() or len(inventory.stdout)>4*1024*1024:
        sys.exit(6)
    result={}
    for line in inventory.stdout.splitlines():
        if not line.strip(): continue
        fields=line.strip().split(None,7)
        if len(fields)!=8:
            sys.exit(6)
        if Path(fields[7]).name!='nginx': continue
        if not fields[0].isdigit() or not fields[1].isdigit():
            sys.exit(6)
        pid,uid=int(fields[0]),int(fields[1])
        identity=short_process_fingerprint(pid,manifest['deadlines']['httpSeconds'])
        if identity is None: sys.exit(6)
        try:
            started=datetime.datetime.strptime(' '.join(fields[2:7]),'%a %b %d %H:%M:%S %Y').replace(tzinfo=datetime.timezone.utc).isoformat().replace('+00:00','Z')
        except ValueError:
            sys.exit(6)
        if identity['startInstant']!=started: sys.exit(3)
        try:
            ports=subprocess.run(['lsof','-nP','-a','-p',str(pid),'-iTCP','-sTCP:LISTEN','-Fpn'],stdin=subprocess.DEVNULL,
                                 stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True,env=environment,timeout=manifest['deadlines']['httpSeconds'])
        except (OSError,subprocess.TimeoutExpired):
            sys.exit(6)
        if ports.returncode not in (0,1) or ports.stderr.strip() or len(ports.stdout)>128*1024:
            sys.exit(6)
        listeners=set()
        for field in ports.stdout.splitlines():
            if field.startswith('p'):
                if field!='p'+str(pid): sys.exit(3)
            elif field.startswith('n'):
                match=re.search(r':([0-9]{1,5})(?: \(LISTEN\))?$',field[1:])
                if match is None or not 1<=int(match.group(1))<=65535: sys.exit(6)
                listeners.add(int(match.group(1)))
            elif field.strip(): sys.exit(6)
        if not listeners and uid!=os.getuid(): sys.exit(6)
        result[str(pid)]={'pid':pid,'uid':uid,'processGroup':identity['processGroup'],'startInstant':started,
                          'commandSha256':identity['commandSha256'],'listenPorts':sorted(listeners)}
    require_original_nginx(result,result)
    return result


def read_original_nginx_baseline(manifest,digest):
    import datetime
    import sys
    from pathlib import Path
    root=Path(manifest['runtimeRoot'])
    value=decode_task_json(read_task_file(root/'evidence/original-nginx.json',root/'evidence'))
    if (not isinstance(value,dict) or set(value)!={'schemaVersion','runId','manifestSha256','status','observedAt','processes'}
            or type(value.get('schemaVersion')) is not int or value['schemaVersion']!=1 or value.get('runId')!=manifest['runId']
            or value.get('manifestSha256')!=digest or value.get('status')!='CAPTURED'):
        sys.exit(3)
    try:
        when=datetime.datetime.fromisoformat(value['observedAt'].replace('Z','+00:00'))
        if not value['observedAt'].endswith('Z') or when.tzinfo is None or when.utcoffset()!=datetime.timedelta() or when>datetime.datetime.now(datetime.timezone.utc):
            sys.exit(3)
    except (ValueError,TypeError,AttributeError):
        sys.exit(3)
    require_original_nginx(value.get('processes'),value.get('processes'))
    return value


def verify_original_nginx(manifest,digest,evidence):
    import datetime
    import hashlib
    from pathlib import Path
    root=Path(manifest['runtimeRoot'])
    before=read_task_file(root/'evidence/original-nginx.json',root/'evidence')
    baseline=read_original_nginx_baseline(manifest,digest)
    require_original_nginx(baseline['processes'],original_nginx_facts(manifest))
    if read_task_file(root/'evidence/original-nginx.json',root/'evidence')!=before:
        raise SystemExit(3)
    proof={'schemaVersion':1,'runId':manifest['runId'],'manifestSha256':digest,'status':'PASSED','scope':'ORIGINAL_NGINX_IDENTITIES_AND_PORTS_ONLY',
           'baselineSha256':hashlib.sha256(before).hexdigest(),'originalProcessCount':len(baseline['processes']),
           'observedAt':datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00','Z')}
    publish_task_json(Path(evidence)/'original-nginx-preserved.json',root,proof)
    return proof


def require_original_redis(baseline,current):
    """Compare sampled values; only an elapsed original expiry permits a missing key."""
    import re
    import sys
    for facts in (baseline,current):
        if (not isinstance(facts,dict) or set(facts)!={'serverReference','observedAtMs','databases'}
                or not isinstance(facts['serverReference'],str) or not re.fullmatch(r'[0-9a-f]{64}',facts['serverReference'])
                or type(facts['observedAtMs']) is not int or facts['observedAtMs']<=0 or not isinstance(facts['databases'],dict)):
            sys.exit(3)
        count=0
        for database,keys in facts['databases'].items():
            if not isinstance(database,str) or not re.fullmatch(r'0|[1-9][0-9]{0,4}',database) or not isinstance(keys,dict):
                sys.exit(3)
            count+=len(keys)
            for key,value in keys.items():
                if (not isinstance(key,str) or not re.fullmatch(r'[0-9a-f]{40}',key) or not isinstance(value,dict)
                        or set(value)!={'type','valueSha1','expiresAtMs'} or not isinstance(value['type'],str)
                        or not re.fullmatch(r'[A-Za-z0-9_-]{1,64}',value['type']) or value['type']=='none'
                        or not isinstance(value['valueSha1'],str) or not re.fullmatch(r'[0-9a-f]{40}',value['valueSha1'])
                        or type(value['expiresAtMs']) is not int or value['expiresAtMs'] not in (-1,) and value['expiresAtMs']<=0):
                    sys.exit(3)
        if count>512 or len(facts['databases'])>256: sys.exit(6)
    if baseline['serverReference']!=current['serverReference'] or current['observedAtMs']<baseline['observedAtMs']:
        sys.exit(3)
    for database,keys in baseline['databases'].items():
        for key,value in keys.items():
            observed=current['databases'].get(database,{}).get(key)
            if observed!=value and not (observed is None and value['expiresAtMs']!=-1 and value['expiresAtMs']<=current['observedAtMs']):
                sys.exit(3)


def original_redis_facts(manifest):
    """Read non-task databases in bounded pages; raw keys and DUMP bytes stay in Redis."""
    import hashlib
    import re
    import subprocess
    import sys
    import time
    from pathlib import Path
    environment=isolated_runtime_environment()
    environment['REDISCLI_AUTH']=task_credential(Path(manifest['redis']['credentialFile']))
    deadline=time.monotonic()+manifest['deadlines']['operationSeconds']
    def query(database,*arguments):
        remaining=deadline-time.monotonic()
        if remaining<=0: sys.exit(6)
        try:
            result=subprocess.run(['redis-cli','-h','127.0.0.1','-p','6379','-n',str(database),'-e','--no-auth-warning','--json',*arguments],
                                  stdin=subprocess.DEVNULL,stdout=subprocess.PIPE,stderr=subprocess.PIPE,env=environment,
                                  timeout=min(remaining,manifest['deadlines']['httpSeconds']))
        except (OSError,subprocess.TimeoutExpired):
            sys.exit(6)
        if result.returncode or result.stderr.strip() or len(result.stdout)>128*1024: sys.exit(6)
        if arguments in (('INFO','server'),('INFO','keyspace')) and result.stdout.startswith(b'# '):
            try:
                info=result.stdout.decode('utf-8')
            except UnicodeError:
                sys.exit(6)
            if not info.splitlines() or info.splitlines()[0]!='# '+arguments[1].capitalize() or '\0' in info:
                sys.exit(6)
            return info
        return decode_task_json(result.stdout)
    def server_reference():
        info=query(0,'INFO','server')
        if not isinstance(info,str): sys.exit(6)
        identifiers=re.findall(r'^run_id:([0-9a-f]{40})\r?$',info,re.MULTILINE)
        if len(identifiers)!=1: sys.exit(6)
        return hashlib.sha256(identifiers[0].encode('ascii')).hexdigest()
    identity=server_reference()
    keyspace=query(0,'INFO','keyspace')
    if not isinstance(keyspace,str): sys.exit(6)
    databases=set()
    for line in keyspace.splitlines():
        if not line or line.startswith('#'): continue
        match=re.fullmatch(r'db(0|[1-9][0-9]{0,4}):keys=([0-9]+),expires=([0-9]+),avg_ttl=([0-9]+)(?:,subexpiry=([0-9]+))?',line)
        if match is None or int(match[1]) in databases: sys.exit(6)
        databases.add(int(match[1]))
    if len(databases)>256: sys.exit(6)
    # Redis SHA1 is an equality fingerprint, never an authorization/signature primitive.
    script="""local page=redis.call('SCAN',ARGV[1],'COUNT',16)
local rows={}
if #page[2]>512 then return redis.error_reply('snapshot page limit') end
local bytes=0
local cardinality={list='LLEN',set='SCARD',zset='ZCARD',hash='HLEN',stream='XLEN'}
for _,key in ipairs(page[2]) do
  local expiry=redis.call('PEXPIRETIME',key)
  if expiry~=-2 then
    local kind=redis.call('TYPE',key).ok
    if kind~='string' and not cardinality[kind] then return redis.error_reply('snapshot unsupported type') end
    if cardinality[kind] and redis.call(cardinality[kind],key)>4096 then return redis.error_reply('snapshot collection limit') end
    local memory=redis.call('MEMORY','USAGE',key,'SAMPLES',0)
    if not memory or memory>4194304 then return redis.error_reply('snapshot memory limit') end
    local value=redis.call('DUMP',key)
    if value then
      bytes=bytes+#value
      if #value>4194304 or bytes>33554432 then return redis.error_reply('snapshot value limit') end
      table.insert(rows,{redis.sha1hex(key),kind,redis.sha1hex(value),expiry,#value})
    end
  end
end
return {page[1],rows}"""
    snapshot={}
    total_bytes=0
    count=0
    for database in sorted(databases-{manifest['redis']['database']}):
        cursor='0'
        seen=set()
        keys={}
        for _ in range(256):
            page=query(database,'EVAL_RO',script,'0',cursor)
            if (not isinstance(page,list) or len(page)!=2 or not isinstance(page[0],str) or not re.fullmatch(r'0|[1-9][0-9]{0,19}',page[0])
                    or not isinstance(page[1],list) or len(page[1])>512): sys.exit(6)
            for row in page[1]:
                if (not isinstance(row,list) or len(row)!=5 or type(row[4]) is not int or not 0<row[4]<=4194304
                        or not isinstance(row[0],str)): sys.exit(6)
                value={'type':row[1],'valueSha1':row[2],'expiresAtMs':row[3]}
                if row[0] in keys and keys[row[0]]!=value: sys.exit(3)
                total_bytes+=row[4]
                if row[0] not in keys:
                    count+=1
                keys[row[0]]=value
                if count>512 or total_bytes>33554432: sys.exit(6)
            cursor=page[0]
            if cursor=='0': break
            if cursor in seen: sys.exit(6)
            seen.add(cursor)
        else:
            sys.exit(6)
        snapshot[str(database)]=dict(sorted(keys.items()))
    observed=query(0,'TIME')
    if (not isinstance(observed,list) or len(observed)!=2 or any(not isinstance(part,str) or not part.isdigit() or len(part)>16 for part in observed)
            or not 0<=int(observed[1])<1000000): sys.exit(6)
    if server_reference()!=identity: sys.exit(3)
    facts={'serverReference':identity,'observedAtMs':int(observed[0])*1000+int(observed[1])//1000,'databases':snapshot}
    require_original_redis(facts,facts)
    return facts


def read_original_redis_baseline(manifest,digest):
    import sys
    from pathlib import Path
    root=Path(manifest['runtimeRoot'])
    value=decode_task_json(read_task_file(root/'evidence/original-redis.json',root/'evidence'))
    if (not isinstance(value,dict) or set(value)!={'schemaVersion','runId','manifestSha256','status','snapshot'}
            or type(value['schemaVersion']) is not int or value['schemaVersion']!=1 or value['runId']!=manifest['runId']
            or value['manifestSha256']!=digest or value['status']!='CAPTURED'):
        sys.exit(3)
    require_original_redis(value['snapshot'],value['snapshot'])
    if str(manifest['redis']['database']) in value['snapshot']['databases']: sys.exit(3)
    return value


def verify_original_redis(manifest,digest,evidence):
    import datetime
    import hashlib
    from pathlib import Path
    root=Path(manifest['runtimeRoot'])
    before=read_task_file(root/'evidence/original-redis.json',root/'evidence')
    baseline=read_original_redis_baseline(manifest,digest)
    current=original_redis_facts(manifest)
    require_original_redis(baseline['snapshot'],current)
    if read_task_file(root/'evidence/original-redis.json',root/'evidence')!=before: raise SystemExit(3)
    proof={'schemaVersion':1,'runId':manifest['runId'],'manifestSha256':digest,'status':'PASSED','scope':'ORIGINAL_REDIS_SAMPLED_KEY_VALUES_ONLY',
           'baselineSha256':hashlib.sha256(before).hexdigest(),'originalKeyCount':sum(map(len,baseline['snapshot']['databases'].values())),
           'observedAt':datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00','Z')}
    publish_task_json(Path(evidence)/'original-redis-preserved.json',root,proof)
    return proof


def prepare(argv):
    import base64, datetime, fcntl, hashlib, json, os, re, secrets, shutil, stat, subprocess, sys
    from pathlib import Path

    manifest_file, result_file, resume = Path(argv[1]), Path(argv[2]), argv[3] == 'true'

    def reject(code):
        sys.exit(code)

    def protected(path, maximum=128 * 1024):
        if not path.is_absolute() or path != Path(os.path.normpath(path)) or any(p.is_symlink() for p in (path, *path.parents)):
            reject(3)
        try:
            before = path.lstat()
            if not stat.S_ISREG(before.st_mode) or before.st_uid != os.getuid() or stat.S_IMODE(before.st_mode) != 0o600:
                reject(3)
            if before.st_size > maximum:
                reject(2)
            with os.fdopen(os.open(path, os.O_RDONLY | os.O_NOFOLLOW), 'rb') as stream:
                data = stream.read(maximum + 1)
            after = path.lstat()
            if len(data) > maximum:
                reject(2)
            if (before.st_ino, before.st_size, before.st_mtime_ns) != (after.st_ino, after.st_size, after.st_mtime_ns):
                reject(3)
            return data
        except OSError:
            reject(4)

    def unique(pairs):
        result = {}
        for key, value in pairs:
            if key in result:
                reject(2)
            result[key] = value
        return result

    def decode(data):
        try:
            return json.loads(data, object_pairs_hook=unique,
                              parse_constant=lambda value: (_ for _ in ()).throw(ValueError()))
        except (ValueError, UnicodeError):
            reject(2)

    manifest_bytes = protected(manifest_file)
    manifest = decode(manifest_bytes)
    root = Path(manifest['runtimeRoot'])
    digest = hashlib.sha256(manifest_bytes).hexdigest()
    owner_marker = 'xingyuan-it:' + manifest['runId'] + ':' + digest

    def unchanged():
        if hashlib.sha256(protected(manifest_file)).hexdigest() != digest:
            reject(3)

    def private_directory(path):
        if not path.is_relative_to(root) or any(p.is_symlink() for p in (path, *path.parents)):
            reject(3)
        try:
            path.mkdir(mode=0o700, parents=True, exist_ok=True)
            for parent in (path, *path.parents):
                if parent == root.parent:
                    break
                info = parent.lstat()
                if not stat.S_ISDIR(info.st_mode) or info.st_uid != os.getuid() or stat.S_IMODE(info.st_mode) != 0o700:
                    reject(3)
        except OSError:
            reject(4)

    for directory in (root, root / 'evidence', root / 'secrets', root / 'profiles'):
        private_directory(directory)

    # A task-local advisory lock never authorizes another database or a changed intent.
    lock_file = root / 'evidence/operation.lock'
    if lock_file.exists():
        protected(lock_file)
    try:
        lock_fd = os.open(lock_file, os.O_RDWR | os.O_CREAT | os.O_NOFOLLOW, 0o600)
        fcntl.flock(lock_fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
    except OSError:
        reject(5)

    for executable in ('psql', 'redis-cli', 'openssl', 'lsof'):
        if shutil.which(executable) is None:
            reject(4)
    if not manifest['processes'] or not manifest['artifacts']:
        reject(4)
    for process in manifest['processes']:
        if process['artifactRole'] not in manifest['artifacts']:
            reject(4)
    for reference in manifest['artifacts'].values():
        path = Path(reference['path'])
        if any(p.is_symlink() for p in (path, *path.parents)) or not path.is_file():
            reject(4)
        if hashlib.sha256(path.read_bytes()).hexdigest() != reference['sha256']:
            reject(3)
    for reference in manifest['catalogFiles'] + manifest['policyFiles']:
        if hashlib.sha256(protected(Path(reference['path']), 2 * 1024 * 1024)).hexdigest() != reference['sha256']:
            reject(3)
    if any(process['id'].startswith('native-') for process in manifest['processes']):
        read_native_build_provenance(manifest)
    required_secrets = {'oauth-kek', 'audit-hmac', 'edge-session', 'registry-hmac', 'admin-password', 'viewer-password', 'provisioner-password',
                        'tls-ca-key', 'tls-ca-cert', 'tls-server-key', 'tls-server-cert', 'tls-keystore', 'tls-keystore-password'}
    if not required_secrets.issubset(manifest['secretFiles']):
        reject(4)
    secret_paths = {key: Path(value) for key, value in manifest['secretFiles'].items()}
    if len(set(secret_paths.values())) != len(secret_paths):
        reject(3)
    for path in secret_paths.values():
        if not path.is_relative_to(root / 'secrets') or path.parent != root / 'secrets':
            reject(3)
        if path.exists():
            protected(path, 2 * 1024 * 1024)
    if result_file.exists() and not resume:
        reject(5)

    def credential(path):
        value = protected(path, 4096)
        if value.endswith(b'\r\n'):
            value = value[:-2]
        elif value.endswith(b'\n'):
            value = value[:-1]
        try:
            text = value.decode('utf-8')
        except UnicodeError:
            reject(2)
        if not text or '\0' in text:
            reject(2)
        return text

    pg_user = os.environ.get('PGUSER', '')
    if not pg_user or len(pg_user) > 63 or any(ord(value) < 32 for value in pg_user):
        reject(4)
    pg_password = credential(Path(manifest['postgres']['credentialFile']))
    redis_password = credential(Path(manifest['redis']['credentialFile']))
    pg_environment = {key: value for key, value in os.environ.items() if not key.startswith('PG')}
    pg_environment.update(PGPASSWORD=pg_password, PGCONNECT_TIMEOUT='5')
    redis_environment = dict(os.environ, REDISCLI_AUTH=redis_password)

    # Neither libpq defaults nor an inherited service/options file may choose the target.
    def psql(sql, mutation=False):
        unchanged()
        args = ['psql', '-X', '--no-password', '-A', '-t', '-v', 'ON_ERROR_STOP=1',
                '-h', '127.0.0.1', '-p', '5432', '-U', pg_user, '-d', 'postgres']
        try:
            completed = subprocess.run(args, input=sql, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                       env=pg_environment, timeout=manifest['deadlines']['httpSeconds'])
        except (OSError, subprocess.TimeoutExpired):
            reject(9 if mutation else 6)
        if completed.returncode != 0:
            reject(9 if mutation else 6)
        return completed.stdout.strip()

    names = [entry['name'] for entry in manifest['databases']]
    query = """SELECT coalesce(json_agg(json_build_object(
        'databaseName', datname, 'oid', oid::bigint, 'databaseOwner', pg_get_userbyid(datdba),
        'ownerComment', shobj_description(oid, 'pg_database'))), '[]'::json)::text
        FROM pg_database WHERE datname IN (""" + ','.join("'" + name + "'" for name in names) + ');'

    def database_facts():
        rows = decode(psql(query))
        if not isinstance(rows, list):
            reject(6)
        result = {}
        for row in rows:
            if (not isinstance(row, dict) or set(row) != {'databaseName', 'oid', 'databaseOwner', 'ownerComment'}
                    or row['databaseName'] not in names or row['databaseName'] in result
                    or type(row['oid']) is not int or row['oid'] <= 0):
                reject(3)
            result[row['databaseName']] = row
        return result

    def redis(*arguments):
        unchanged()
        try:
            result = subprocess.run(['redis-cli', '-h', '127.0.0.1', '-p', '6379', '-n', str(manifest['redis']['database']),
                                     '--no-auth-warning', '--raw', *arguments], env=redis_environment,
                                    stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True,
                                    timeout=manifest['deadlines']['httpSeconds'])
        except (OSError, subprocess.TimeoutExpired):
            reject(6)
        if result.returncode != 0:
            reject(6)
        return result.stdout.strip()

    existing = database_facts()
    if existing and not resume:
        reject(3)
    prior_redis_owner=redis('GET','xingyuan-it:owner')
    prior_task_facts=bool(existing) or prior_redis_owner==owner_marker
    original_file=root/'evidence/original-databases.json'
    current_original=original_database_facts(manifest,psql)
    if original_file.exists():
        if not resume:
            reject(5)
        original=read_original_database_baseline(manifest,digest)
        require_original_databases(original['databases'],current_original)
    else:
        if prior_task_facts:
            reject(9)
        original={'schemaVersion':1,'runId':manifest['runId'],'manifestSha256':digest,'status':'CAPTURED',
                  'observedAt':datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00','Z'),'databases':current_original}
        publish_task_json(original_file,root,original)
    nginx_file=root/'evidence/original-nginx.json'
    current_nginx=original_nginx_facts(manifest)
    if nginx_file.exists():
        if not resume: reject(5)
        nginx_original=read_original_nginx_baseline(manifest,digest)
        require_original_nginx(nginx_original['processes'],current_nginx)
    else:
        if prior_task_facts: reject(9)
        nginx_original={'schemaVersion':1,'runId':manifest['runId'],'manifestSha256':digest,'status':'CAPTURED',
                        'observedAt':datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00','Z'),'processes':current_nginx}
        publish_task_json(nginx_file,root,nginx_original)
    redis_file=root/'evidence/original-redis.json'
    current_redis=original_redis_facts(manifest)
    if redis_file.exists():
        if not resume: reject(5)
        redis_original=read_original_redis_baseline(manifest,digest)
        require_original_redis(redis_original['snapshot'],current_redis)
    else:
        if prior_task_facts: reject(9)
        redis_original={'schemaVersion':1,'runId':manifest['runId'],'manifestSha256':digest,'status':'CAPTURED','snapshot':current_redis}
        publish_task_json(redis_file,root,redis_original)
    recover_receipts=set()
    for target in manifest['databases']:
        receipt_file = root / ('evidence/db-' + target['role'] + '.json')
        fact = existing.get(target['name'])
        if fact is None:
            if receipt_file.exists():
                reject(3)
            continue
        if fact['databaseOwner'] != pg_user or fact['ownerComment'] != owner_marker:
            reject(3)
        if not receipt_file.exists():
            if not resume or receipt_file.is_symlink():
                reject(3)
            recover_receipts.add(target['name'])
            continue
        receipt = decode(protected(receipt_file))
        expected = dict(fact, role=target['role'], runId=manifest['runId'], manifestSha256=digest)
        if receipt != expected:
            reject(3)

    # Occupied ports belong to their existing owner; preparation never kills them.
    for process in manifest['processes']:
        for port in [process['port'], *process.get('additionalPorts', [])]:
            try:
                probe = subprocess.run(['lsof', '-nP', '-iTCP:' + str(port), '-sTCP:LISTEN', '-t'],
                                       stdin=subprocess.DEVNULL, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                       timeout=manifest['deadlines']['httpSeconds'])
            except (OSError, subprocess.TimeoutExpired):
                reject(6)
            if probe.returncode == 0 and probe.stdout.strip():
                reject(3)
            if probe.returncode != 1 or probe.stdout.strip():
                reject(6)

    claim = """if redis.call('DBSIZE') == 0 then
      redis.call('SET', KEYS[1], ARGV[1]); return 1
    elseif ARGV[2] == 'true' and redis.call('GET', KEYS[1]) == ARGV[1] then
      return 2
    else return 0 end"""
    if redis('EVAL', claim, '1', 'xingyuan-it:owner', owner_marker, str(resume).lower()) not in ('1', '2'):
        reject(3)

    def publish(path, data):
        private_directory(path.parent)
        if path.exists():
            if resume and protected(path, 2 * 1024 * 1024) == data:
                return
            reject(5)
        temporary = path.parent / ('.' + path.name + '-' + secrets.token_hex(12) + '.tmp')
        try:
            fd = os.open(temporary, os.O_WRONLY | os.O_CREAT | os.O_EXCL | os.O_NOFOLLOW, 0o600)
            with os.fdopen(fd, 'wb') as stream:
                stream.write(data)
                stream.flush()
                os.fsync(stream.fileno())
            # link is atomic and cannot replace an existing checkpoint or credential.
            os.link(temporary, path, follow_symlinks=False)
            temporary.unlink()
            directory = os.open(path.parent, os.O_RDONLY)
            try:
                os.fsync(directory)
            finally:
                os.close(directory)
        except OSError:
            reject(7)
        finally:
            if temporary.exists():
                temporary.unlink()

    def publish_json(path, value):
        publish(path, (json.dumps(value, sort_keys=True, separators=(',', ':')) + '\n').encode('utf-8'))

    receipts = []
    for target in manifest['databases']:
        unchanged()
        if redis('GET', 'xingyuan-it:owner') != owner_marker:
            reject(3)
        fact = existing.get(target['name'])
        if fact is None:
            # All names are the independently validated frozen allowlist, never arbitrary SQL input.
            quoted_user = '"' + pg_user.replace('"', '""') + '"'
            psql('CREATE DATABASE "' + target['name'] + '" OWNER ' + quoted_user + ';', mutation=True)
            psql('COMMENT ON DATABASE "' + target['name'] + '" IS \'' + owner_marker + '\';', mutation=True)
            fact = database_facts().get(target['name'])
            if fact is None or fact['databaseOwner'] != pg_user or fact['ownerComment'] != owner_marker:
                reject(9)
        elif target['name'] in recover_receipts and database_facts().get(target['name']) != fact:
            reject(3)
        receipt = dict(fact, role=target['role'], runId=manifest['runId'], manifestSha256=digest)
        publish_json(root / ('evidence/db-' + target['role'] + '.json'), receipt)
        receipts.append(receipt)

    for role in ('oauth-kek', 'audit-hmac', 'edge-session', 'registry-hmac'):
        path = secret_paths[role]
        if path.exists():
            if not resume or len(protected(path, 32)) != 32:
                reject(5)
        else:
            publish(path, secrets.token_bytes(32))
    for role in ('admin-password', 'viewer-password', 'provisioner-password', 'tls-keystore-password'):
        path = secret_paths[role]
        if path.exists():
            if not resume or len(credential(path)) < 12:
                reject(5)
        else:
            publish(path, secrets.token_urlsafe(36).encode('ascii'))

    def openssl(*arguments):
        unchanged()
        try:
            result = subprocess.run(['openssl', *map(str, arguments)], stdin=subprocess.DEVNULL,
                                    stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                    timeout=manifest['deadlines']['operationSeconds'])
        except (OSError, subprocess.TimeoutExpired):
            reject(7)
        if result.returncode != 0:
            reject(7)
        return result.stdout

    tls_roles = ('tls-ca-key', 'tls-ca-cert', 'tls-server-key', 'tls-server-cert', 'tls-keystore')
    present = [secret_paths[role].exists() for role in tls_roles]
    if any(present) and not all(present):
        reject(9)
    if all(present) and not resume:
        reject(5)
    if not any(present):
        staging = root / 'secrets' / ('.tls-' + secrets.token_hex(16))
        private_directory(staging)
        staged = {role: staging / role for role in tls_roles}
        extensions = staging / 'server-extensions.cnf'
        request = staging / 'server-request.csr'
        publish(extensions, b'basicConstraints=critical,CA:FALSE\nkeyUsage=critical,digitalSignature,keyEncipherment\nextendedKeyUsage=serverAuth,clientAuth\nsubjectAltName=IP:127.0.0.1,DNS:localhost\n')
        openssl('req', '-x509', '-newkey', 'rsa:3072', '-nodes', '-sha256', '-days', '7',
                '-subj', '/CN=Xingyuan Isolated Integration CA', '-addext', 'basicConstraints=critical,CA:TRUE',
                '-addext', 'keyUsage=critical,keyCertSign,cRLSign', '-keyout', staged['tls-ca-key'], '-out', staged['tls-ca-cert'])
        openssl('req', '-new', '-newkey', 'rsa:3072', '-nodes', '-sha256', '-subj', '/CN=127.0.0.1',
                '-keyout', staged['tls-server-key'], '-out', request)
        openssl('x509', '-req', '-in', request, '-CA', staged['tls-ca-cert'], '-CAkey', staged['tls-ca-key'],
                '-set_serial', '0x' + secrets.token_hex(16), '-days', '7', '-sha256', '-extfile', extensions, '-out', staged['tls-server-cert'])
        openssl('pkcs12', '-export', '-inkey', staged['tls-server-key'], '-in', staged['tls-server-cert'],
                '-certfile', staged['tls-ca-cert'], '-name', 'xingyuan-it', '-out', staged['tls-keystore'],
                '-passout', 'file:' + str(secret_paths['tls-keystore-password']))
        for role in tls_roles:
            publish(secret_paths[role], protected(staged[role], 2 * 1024 * 1024))
        for path in staging.iterdir():
            path.unlink()
        staging.rmdir()
    for role in tls_roles:
        protected(secret_paths[role], 2 * 1024 * 1024)
    openssl('verify', '-CAfile', secret_paths['tls-ca-cert'], '-verify_ip', '127.0.0.1', secret_paths['tls-server-cert'])
    certificate_key = openssl('x509', '-in', secret_paths['tls-server-cert'], '-pubkey', '-noout')
    private_key = openssl('pkey', '-in', secret_paths['tls-server-key'], '-pubout')
    if certificate_key != private_key:
        reject(3)
    openssl('pkcs12', '-in', secret_paths['tls-keystore'], '-passin', 'file:' + str(secret_paths['tls-keystore-password']), '-noout')

    def property_value(value):
        return str(value).replace('\\', '\\\\').replace('\n', '\\n').replace('\r', '\\r').replace('\t', '\\t').replace(' ', '\\ ')

    # Only static transport facts exist here; identity-derived registrations are published by their owner later.
    for process in manifest['processes']:
        if process['kind'] != 'JAVA':
            continue
        values = {'server.address': '127.0.0.1', 'server.port': process['port'], 'server.ssl.enabled': 'true',
                  'server.ssl.key-store': 'file:' + str(secret_paths['tls-keystore']), 'server.ssl.key-store-type': 'PKCS12',
                  'server.ssl.key-store-password': credential(secret_paths['tls-keystore-password']),
                  'spring.jpa.properties.hibernate.jdbc.time_zone': 'UTC', 'spring.jackson.time-zone': 'UTC'}
        publish(root / ('profiles/' + process['id'] + '-transport.properties'),
                ''.join(key + '=' + property_value(value) + '\n' for key, value in sorted(values.items())).encode('utf-8'))

    if 'nginx' in manifest['artifacts']:
        fault_map = root / 'nginx/revocation-fault.map'
        if not fault_map.exists() and (root / 'evidence/revocation-fault-state.json').exists():
            reject(9)
        if not fault_map.exists():
            publish_private_file(fault_map, root, task_revocation_fault_contents(manifest, digest, False))

    require_original_databases(original['databases'],original_database_facts(manifest,psql))
    require_original_nginx(nginx_original['processes'],original_nginx_facts(manifest))
    require_original_redis(redis_original['snapshot'],original_redis_facts(manifest))
    unchanged()
    result = {'schemaVersion': 1, 'operationId': 'CLI-001', 'runId': manifest['runId'], 'manifestSha256': digest,
              'status': 'COMMITTED', 'completedAt': datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00', 'Z'),
              'targets': [{'targetKey': value['role'], 'identity': str(value['oid']), 'version': digest, 'state': 'COMMITTED'} for value in receipts],
              'counts': {'databaseCount': 9, 'tenantCount': 0, 'clientCount': 0, 'catalogCount': 0, 'liveCaseCount': 0},
              'steps': [{'stepId': 'PREPARE_TARGETS', 'state': 'COMMITTED', 'evidence': 'evidence/prepared.json'}], 'error': None}
    prepared_file = root / 'evidence/prepared.json'
    if prepared_file.exists():
        previous = decode(protected(prepared_file))
        if not resume or previous.get('runId') != manifest['runId'] or previous.get('manifestSha256') != digest or previous.get('status') != 'COMMITTED':
            reject(5)
        result = previous
    else:
        publish_json(prepared_file, result)
    if result_file != prepared_file:
        publish_json(result_file, result)
    print('CLI-001 COMMITTED ' + str(result_file.relative_to(root)))


def validate_owned_process_record(manifest, digest, record, result_file=None, allow_empty=False):
    """Validate the shared process-ledger contract and artifact provenance before observation or mutation."""
    import datetime
    import hashlib
    import os
    import re
    import sys
    from pathlib import Path

    def reject(code):
        sys.exit(code)

    if (not isinstance(record, dict) or set(record) != {'schemaVersion', 'runId', 'manifestSha256', 'processes'}
            or type(record['schemaVersion']) is not int or record['schemaVersion'] != 1
            or record['runId'] != manifest['runId'] or record['manifestSha256'] != digest):
        reject(3)
    if not isinstance(record['processes'], list) or not (0 if allow_empty else 1) <= len(record['processes']) <= 32:
        reject(2)
    if result_file is not None and result_file.exists():
        reject(5)
    expected = {value['id']: value for value in manifest['processes']}
    seen_ids, seen_pids = set(), set()
    fields = {'id', 'pid', 'startInstant', 'artifactSha256', 'commandSha256'}
    for process in record['processes']:
        if not isinstance(process, dict) or set(process) != fields:
            reject(2)
        if (process['id'] not in expected or process['id'] in seen_ids
                or type(process['pid']) is not int or process['pid'] <= 1 or process['pid'] == os.getpid()
                or process['pid'] in seen_pids):
            reject(3)
        for key in ('artifactSha256', 'commandSha256'):
            if not isinstance(process[key], str) or not re.fullmatch(r'[0-9a-f]{64}', process[key]):
                reject(2)
        reference = manifest['artifacts'].get(expected[process['id']]['artifactRole'])
        if reference is None or reference['sha256'] != process['artifactSha256']:
            reject(10)
        artifact = Path(reference['path'])
        if any(p.is_symlink() for p in (artifact, *artifact.parents)) or not artifact.is_file():
            reject(10)
        if hashlib.sha256(artifact.read_bytes()).hexdigest() != process['artifactSha256']:
            reject(10)
        try:
            started = datetime.datetime.fromisoformat(process['startInstant'].replace('Z', '+00:00'))
            if started.tzinfo is None or started.utcoffset() != datetime.timedelta() or not process['startInstant'].endswith('Z'):
                reject(2)
        except (ValueError, TypeError, AttributeError):
            reject(2)
        seen_ids.add(process['id'])
        seen_pids.add(process['pid'])

    return {value['id']: value for value in manifest['processes']}


def observe_owned_process(manifest, process):
    """Inspect one already-recorded owned process without acquiring a second operation lock."""
    import datetime
    import hashlib
    import locale
    import os
    import shlex
    import subprocess
    import sys
    from pathlib import Path

    def reject(code):
        sys.exit(code)

    locale.setlocale(locale.LC_TIME, 'C')
    process_environment = dict(os.environ, LC_ALL='C', TZ='UTC')
    root = Path(manifest['runtimeRoot'])
    expected = {value['id']: value for value in manifest['processes']}
    try:
        observed = subprocess.run(['ps', '-p', str(process['pid']), '-o', 'lstart=', '-o', 'stat=', '-o', 'command='],
                                  stdin=subprocess.DEVNULL, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                  text=True, env=process_environment, timeout=manifest['deadlines']['httpSeconds'])
    except (OSError, subprocess.TimeoutExpired):
        reject(6)
    if observed.returncode == 1 and not observed.stdout.strip() and not observed.stderr.strip():
        return 'STOPPED'
    if observed.returncode != 0 or len(observed.stdout) > 64 * 1024:
        reject(6)
    lines = observed.stdout.strip().splitlines()
    if len(lines) != 1:
        reject(10)
    columns = lines[0].split(None, 6)
    if len(columns) != 7:
        reject(10)
    try:
        started = datetime.datetime.strptime(' '.join(columns[:5]), '%a %b %d %H:%M:%S %Y').replace(tzinfo=datetime.timezone.utc)
    except ValueError:
        reject(10)
    actual_start = started.isoformat().replace('+00:00', 'Z')
    if actual_start != process['startInstant']:
        reject(10)
    if columns[5].startswith('Z'):
        return 'STOPPED'
    if hashlib.sha256(columns[6].encode('utf-8')).hexdigest() != process['commandSha256']:
        reject(10)
    try:
        arguments = shlex.split(columns[6])
    except ValueError:
        reject(10)
    declared = expected[process['id']]
    artifact = Path(manifest['artifacts'][declared['artifactRole']]['path'])
    if declared['kind'] == 'JAVA':
        if not arguments or Path(arguments[0]).name != 'java':
            reject(10)
        if '-cp' in arguments:
            prefix = '-Dxingyuan.isolated.runtime-probe.manifest-file='
            files = [value.removeprefix(prefix) for value in arguments if value.startswith(prefix)]
            if len(files) != 1:
                reject(10)
            try:
                probe = runtime_probe_configuration(manifest, process['id'], files[0])
            except SystemExit:
                reject(10)
            if probe is None or arguments[1:] != runtime_probe_java_arguments(manifest, process['id'], probe):
                reject(10)
        else:
            selected = process['id'] in ('edge', 'native-light', 'native-web', 'native-service', 'shoubing', 'jianshen', 'tianshu', 'yuheng', 'llm') and any(
                isinstance(value, dict) and Path(value.get('path', '')).name == 'task-runtime-probe.jar'
                for value in manifest.get('policyFiles', []))
            if (selected or '-jar' not in arguments or arguments.index('-jar') + 1 >= len(arguments)
                    or arguments[arguments.index('-jar') + 1] != str(artifact)
                    or '-Dxingyuan.isolated.run-id=' + manifest['runId'] not in arguments
                    or any(value in ('-classpath', '--class-path') or value.startswith(('-javaagent', '-agentlib', '-agentpath', '-Dloader.',
                           '-Dcontext.initializer.classes=', '-Dxingyuan.isolated.runtime-probe.')) for value in arguments)):
                reject(10)
    elif declared['kind'] == 'NODE':
        configuration = str(root / ('profiles/' + process['id'] + '.vite.config.mjs'))
        entry = str(artifact.parent / 'node_modules/vite/bin/vite.js')
        if (len(arguments) < 3 or Path(arguments[0]).name not in ('node', 'nodejs')
                or arguments[1] != entry or str(artifact.parent) not in arguments
                or '--config' not in arguments or arguments.index('--config') + 1 >= len(arguments)
                or arguments[arguments.index('--config') + 1] != configuration):
            reject(10)
    else:
        if arguments[:3] == ['nginx:', 'master', 'process']:
            arguments = arguments[3:]
        if (not arguments or Path(arguments[0]).name != 'nginx'
                or '-p' not in arguments or arguments.index('-p') + 1 >= len(arguments)
                or Path(arguments[arguments.index('-p') + 1]) != root / 'nginx'
                or '-c' not in arguments or arguments.index('-c') + 1 >= len(arguments)
                or arguments[arguments.index('-c') + 1] != str(artifact)):
            reject(10)
    return 'RUNNING'


def process_state(argv):
    import datetime, fcntl, hashlib, json, locale, os, re, secrets, shlex, stat, subprocess, sys, time
    from pathlib import Path

    operation, manifest_file, result_file = argv[1], Path(argv[2]), Path(argv[3])

    def reject(code):
        sys.exit(code)

    def protected(path):
        if not path.is_absolute() or any(p.is_symlink() for p in (path, *path.parents)):
            reject(3)
        try:
            before = path.lstat()
            if not stat.S_ISREG(before.st_mode) or before.st_uid != os.getuid() or stat.S_IMODE(before.st_mode) != 0o600:
                reject(3)
            if before.st_size > 128 * 1024:
                reject(2)
            with os.fdopen(os.open(path, os.O_RDONLY | os.O_NOFOLLOW), 'rb') as stream:
                data = stream.read(128 * 1024 + 1)
            after = path.lstat()
            if len(data) > 128 * 1024:
                reject(2)
            if (before.st_ino, before.st_size, before.st_mtime_ns) != (after.st_ino, after.st_size, after.st_mtime_ns):
                reject(3)
            return data
        except OSError:
            reject(4)

    def unique(pairs):
        value = {}
        for key, item in pairs:
            if key in value:
                reject(2)
            value[key] = item
        return value

    def decode(data):
        try:
            return json.loads(data, object_pairs_hook=unique,
                              parse_constant=lambda value: (_ for _ in ()).throw(ValueError()))
        except (ValueError, UnicodeError):
            reject(2)

    manifest_bytes = protected(manifest_file)
    manifest = decode(manifest_bytes)
    digest = hashlib.sha256(manifest_bytes).hexdigest()
    root = Path(manifest['runtimeRoot'])
    lock_file = root / 'evidence/operation.lock'
    if lock_file.exists():
        protected(lock_file)
    try:
        lock_fd = os.open(lock_file, os.O_RDWR | os.O_CREAT | os.O_NOFOLLOW, 0o600)
        fcntl.flock(lock_fd, (fcntl.LOCK_EX if operation == 'stop' else fcntl.LOCK_SH) | fcntl.LOCK_NB)
    except OSError:
        reject(5)
    record_file = root / 'evidence/processes.json'
    record_bytes = protected(record_file)
    record = decode(record_bytes)
    expected = validate_owned_process_record(manifest, digest, record, result_file)

    locale.setlocale(locale.LC_TIME, 'C')
    process_environment = dict(os.environ, LC_ALL='C', TZ='UTC')

    def observe(process):
        return observe_owned_process(manifest, process)

    # Inspect every receipt before the first signal; a bad second row cannot cause a partial foreign stop.
    observations = {value['id']: observe(value) for value in record['processes']}
    if operation == 'stop':
        if os.environ.get('XINGYUAN_ISOLATED_INTEGRATION') != 'true':
            reject(3)
        for process in record['processes']:
            if hashlib.sha256(protected(manifest_file)).hexdigest() != digest or protected(record_file) != record_bytes:
                reject(3)
            if observations[process['id']] == 'STOPPED':
                continue
            reference = manifest['artifacts'][expected[process['id']]['artifactRole']]
            artifact = Path(reference['path'])
            if (any(p.is_symlink() for p in (artifact, *artifact.parents)) or not artifact.is_file()
                    or hashlib.sha256(artifact.read_bytes()).hexdigest() != process['artifactSha256']):
                reject(10)
            if observe(process) == 'STOPPED':
                observations[process['id']] = 'STOPPED'
                continue
            try:
                signal = subprocess.run(['kill', '-TERM', str(process['pid'])], stdin=subprocess.DEVNULL,
                                        stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=5)
            except (OSError, subprocess.TimeoutExpired):
                reject(7)
            if signal.returncode != 0 and observe(process) != 'STOPPED':
                reject(10)
        deadline = time.monotonic() + min(manifest['deadlines']['startupSeconds'], manifest['deadlines']['operationSeconds'])
        while True:
            observations = {value['id']: observe(value) for value in record['processes']}
            if all(value == 'STOPPED' for value in observations.values()):
                break
            if time.monotonic() >= deadline:
                reject(7)
            time.sleep(min(0.5, max(0, deadline - time.monotonic())))

    state = 'STOPPED' if all(value == 'STOPPED' for value in observations.values()) else 'RUNNING'
    result = {'schemaVersion': 1, 'operationId': 'CLI-009' if operation == 'stop' else 'CLI-008',
              'runId': manifest['runId'], 'manifestSha256': digest, 'status': state,
              'completedAt': datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00', 'Z'),
              'targets': [{'targetKey': value['id'], 'identity': str(value['pid']), 'version': value['startInstant'], 'state': observations[value['id']]}
                          for value in record['processes']],
              'counts': {'databaseCount': 0, 'tenantCount': 0, 'clientCount': 0, 'catalogCount': 0, 'liveCaseCount': 0},
              'steps': [], 'error': None}
    parent = result_file.parent
    if not parent.is_dir() or any(p.is_symlink() for p in (parent, *parent.parents)):
        reject(3)
    temporary = parent / ('.' + result_file.name + '-' + secrets.token_hex(12) + '.tmp')
    try:
        fd = os.open(temporary, os.O_WRONLY | os.O_CREAT | os.O_EXCL | os.O_NOFOLLOW, 0o600)
        with os.fdopen(fd, 'wb') as output:
            output.write((json.dumps(result, sort_keys=True, separators=(',', ':')) + '\n').encode('utf-8'))
            output.flush()
            os.fsync(output.fileno())
        os.link(temporary, result_file, follow_symlinks=False)
        temporary.unlink()
        directory = os.open(parent, os.O_RDONLY)
        try:
            os.fsync(directory)
        finally:
            os.close(directory)
    except OSError:
        reject(7)
    finally:
        if temporary.exists():
            temporary.unlink()
    print(result['operationId'] + ' ' + state + ' ' + str(result_file.relative_to(root)))


def run_authorization_browser(manifest, digest, workspace, environment, log_fd, log_file):
    """Own the fixed CLI006 browser process even when its outer verifier is cancelled."""
    import datetime
    import os
    import signal
    import subprocess
    import sys
    import time
    from pathlib import Path

    root,log_file=Path(manifest['runtimeRoot']),Path(log_file)
    receipt=log_file.with_suffix('.process.json')
    process=None
    previous=None
    handlers={value:signal.getsignal(value) for value in (signal.SIGTERM,signal.SIGINT)}
    def cancel(signum,frame):
        sys.exit(9)
    for value in handlers:
        signal.signal(value,cancel)
    try:
        with os.fdopen(log_fd,'wb') as output:
            started=time.time_ns()
            process=subprocess.Popen(['npm','exec','--','playwright','test','--config','playwright.integration.config.ts','--grep','CLI-006:','--workers','1'],
                                     cwd=workspace,env=environment,stdin=subprocess.DEVNULL,stdout=output,stderr=subprocess.STDOUT,start_new_session=True)
            identity=short_process_fingerprint(process.pid,manifest['deadlines']['httpSeconds']) if process.poll() is None else None
            record={'schemaVersion':1,'runId':manifest['runId'],'manifestSha256':digest,'stage':'CLI006_BROWSER',
                    'pid':process.pid,'startedNs':started,'state':'STARTED','identity':identity,'logFile':str(log_file.relative_to(root))}
            publish_task_json(receipt,root,record)
            previous=read_task_file(receipt,root/'evidence')
            try:
                return process.wait(timeout=manifest['deadlines']['operationSeconds'])
            except subprocess.TimeoutExpired:
                sys.exit(9)
    except OSError:
        sys.exit(6)
    finally:
        for value in handlers:
            signal.signal(value,signal.SIG_IGN)
        try:
            unknown=False
            if process is not None and process.poll() is None:
                # An unreaped creation handle cannot refer to a reused PID; npm may legitimately change its title.
                try:
                    if os.getpgid(process.pid)!=process.pid:
                        sys.exit(10)
                    os.killpg(process.pid,signal.SIGTERM)
                    process.wait(timeout=5)
                except ProcessLookupError:
                    if process.poll() is None: unknown=True
                except subprocess.TimeoutExpired:
                    unknown=True
            if process is not None and previous is not None:
                record.update(state='UNKNOWN_COMMIT' if unknown else 'EXITED',exitCode=process.poll(),
                              completedAt=datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00','Z'))
                publish_task_json(receipt,root,record,previous)
            if unknown:
                sys.exit(9)
        finally:
            for value,handler in handlers.items():
                signal.signal(value,handler)


def authorize_scope_probe(argv):
    authorize_cli(argv, scope_probe=True)


def authorize_cli(argv, scope_probe=False):
    import datetime, fcntl, hashlib, json, os, re, stat, sys, uuid
    from pathlib import Path

    manifest_file, result_file = Path(argv[1]), Path(argv[2])
    tenant, resource, user, output_file, repository = argv[3], argv[4], argv[5], Path(argv[6]), Path(argv[7])

    def reject(code):
        sys.exit(code)

    def protected(path, maximum=128 * 1024):
        if not path.is_absolute() or any(parent.is_symlink() for parent in (path, *path.parents)):
            reject(3)
        try:
            before = path.lstat()
            if not stat.S_ISREG(before.st_mode) or before.st_uid != os.getuid() or stat.S_IMODE(before.st_mode) != 0o600:
                reject(3)
            if before.st_size > maximum:
                reject(2)
            with os.fdopen(os.open(path, os.O_RDONLY | os.O_NOFOLLOW), 'rb') as source:
                value = source.read(maximum + 1)
            after = path.lstat()
            if len(value) > maximum:
                reject(2)
            if (before.st_ino, before.st_size, before.st_mtime_ns) != (after.st_ino, after.st_size, after.st_mtime_ns):
                reject(3)
            return value
        except OSError:
            reject(4)

    def unique(pairs):
        result = {}
        for key, value in pairs:
            if key in result:
                reject(2)
            result[key] = value
        return result

    def decode(value):
        try:
            return json.loads(value, object_pairs_hook=unique,
                              parse_constant=lambda value: (_ for _ in ()).throw(ValueError()))
        except (ValueError, UnicodeError):
            reject(2)

    contents = protected(manifest_file)
    manifest = decode(contents)
    root = Path(manifest['runtimeRoot'])
    digest = hashlib.sha256(contents).hexdigest()
    if tenant not in ('default', 'tenant-b') or user not in ('admin', 'viewer') or not re.fullmatch(r'[a-z][a-z0-9-]{2,63}', resource):
        reject(2)
    if type(scope_probe) is not bool or scope_probe and user != 'admin':
        reject(3)
    if (not output_file.is_relative_to(root / 'secrets') or output_file in
            (root / 'secrets/runtime-registry.json', root / 'secrets/token-index.json')):
        reject(3)
    if output_file.exists() or result_file.exists():
        reject(5)
    canonical_index = root / 'secrets/token-index.json'
    canonical_before = protected(canonical_index) if scope_probe else None
    metadata_file = Path(str(output_file) + '.metadata.json')
    if scope_probe and metadata_file.exists():
        reject(5)
    registry = decode(protected(root / 'secrets/runtime-registry.json', 2 * 1024 * 1024))
    if not isinstance(registry, dict):
        reject(2)
    if registry.get('runId') != manifest['runId'] or registry.get('manifestSha256') != digest:
        reject(3)
    if not isinstance(registry.get('cliResources'), list) or not isinstance(registry.get('clients'), list):
        reject(4)
    if any(not isinstance(value, dict) for value in registry['cliResources'] + registry['clients']):
        reject(2)
    bindings = [value for value in registry['cliResources'] if value.get('resourceKey') == resource]
    if len(bindings) != 1:
        reject(3)
    client_id = bindings[0].get('clientIds', {}).get(tenant)
    clients = [value for value in registry['clients'] if value.get('clientId') == client_id and value.get('realmCode') == tenant]
    if (len(clients) != 1 or clients[0].get('clientType') != 'PUBLIC' or clients[0].get('purpose') != 'USER'
            or clients[0].get('status') != 'ACTIVE' or clients[0].get('refreshTokenTtlSeconds') != 0):
        reject(3)
    if scope_probe and (not isinstance(clients[0].get('allowedScopes'), list) or 'openid' not in clients[0]['allowedScopes']):
        reject(4)
    for name, operation in (('control-plane.json', 'CLI-003'), ('authorization-platform.json', 'CLI-004')):
        receipt = decode(protected(root / 'evidence' / name))
        if not isinstance(receipt, dict):
            reject(2)
        if (receipt.get('runId') != manifest['runId'] or receipt.get('manifestSha256') != digest
                or receipt.get('operationId') != operation or receipt.get('status') not in ('COMMITTED', 'RUNNING')):
            reject(3)
    for reference in manifest['policyFiles'] + manifest['catalogFiles']:
        if hashlib.sha256(protected(Path(reference['path']), 2 * 1024 * 1024)).hexdigest() != reference['sha256']:
            reject(3)
    ca = manifest['secretFiles'].get('tls-ca-cert')
    if ca is None:
        reject(4)
    protected(Path(ca))
    workspace = repository / 'egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web'
    package = workspace / 'node_modules/@playwright/test/package.json'
    try:
        if decode(package.read_bytes()).get('version') != '1.62.0':
            reject(4)
    except OSError:
        reject(4)
    lock_file = root / 'evidence/operation.lock'
    if lock_file.exists():
        protected(lock_file)
    try:
        lock_fd = os.open(lock_file, os.O_RDWR | os.O_CREAT | os.O_NOFOLLOW, 0o600)
        fcntl.flock(lock_fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
    except OSError:
        reject(5)
    log_file = root / ('evidence/cli-' + tenant + '-' + resource + '-' + user + '-' + str(uuid.uuid4()) + '.log')
    try:
        log_fd = os.open(log_file, os.O_WRONLY | os.O_CREAT | os.O_EXCL | os.O_NOFOLLOW, 0o600)
    except FileExistsError:
        reject(5)
    environment = dict(os.environ, XINGYUAN_ISOLATED_INTEGRATION='true', XINGYUAN_INTEGRATION_MANIFEST=str(manifest_file),
                       NODE_EXTRA_CA_CERTS=ca, XINGYUAN_CLI_TENANT_CODE=tenant, XINGYUAN_CLI_RESOURCE_KEY=resource,
                       XINGYUAN_CLI_USER_KEY=user, XINGYUAN_CLI_OUTPUT_FILE=str(output_file), XINGYUAN_CLI_RESULT_FILE=str(result_file))
    for name in ('NODE_OPTIONS', 'NODE_TLS_REJECT_UNAUTHORIZED', 'DEBUG', 'PWDEBUG'):
        environment.pop(name, None)
    environment.pop('XINGYUAN_CLI_SCOPE_MODE', None)
    if scope_probe:
        environment['XINGYUAN_CLI_SCOPE_MODE'] = 'IDENTITY_ONLY'
    outcome = run_authorization_browser(manifest,digest,workspace,environment,log_fd,log_file)
    if outcome != 0:
        if output_file.exists():
            reject(7)
        report = protected(log_file, 4 * 1024 * 1024).decode('utf-8', errors='replace')
        outcomes = {'XINGYUAN_INPUT_INVALID': 2, 'XINGYUAN_TARGET_UNSAFE': 3, 'XINGYUAN_INPUT_UNAVAILABLE': 4,
                    'XINGYUAN_STATE_CONFLICT': 5, 'XINGYUAN_DEPENDENCY_UNAVAILABLE': 6, 'XINGYUAN_PARTIAL': 7}
        matched = [value for key, value in outcomes.items() if key in report]
        reject(matched[-1] if matched else 8)
    result = decode(protected(result_file))
    if not isinstance(result, dict):
        reject(2)
    if (result.get('operationId') != 'CLI-006' or result.get('status') != 'COMMITTED'
            or result.get('runId') != manifest['runId'] or result.get('manifestSha256') != digest or result.get('error') is not None):
        reject(7)
    index = decode(protected(metadata_file if scope_probe else canonical_index))
    if not isinstance(index, dict):
        reject(2)
    if index.get('runId') != manifest['runId'] or index.get('manifestSha256') != digest:
        reject(7)
    if scope_probe:
        if (set(index) != {'schemaVersion','runId','manifestSha256','scope','token'} or type(index.get('schemaVersion')) is not int or index['schemaVersion'] != 1
                or index.get('scope') != 'IDENTITY_ONLY_PUBLIC_CODE' or not isinstance(index.get('token'),dict)
                or protected(canonical_index) != canonical_before):
            reject(3)
        indexed_tokens = [index['token']]
    else:
        indexed_tokens = index.get('tokens')
    if not isinstance(indexed_tokens,list):
        reject(7)
    if any(not isinstance(value, dict) for value in indexed_tokens):
        reject(2)
    tokens = [value for value in indexed_tokens if value.get('tenantCode') == tenant and value.get('resourceKey') == resource and value.get('userKey') == user]
    if (len(tokens) != 1 or tokens[0].get('clientId') != client_id or tokens[0].get('tokenFile') != str(output_file)
            or hashlib.sha256(protected(output_file)).hexdigest() != tokens[0].get('tokenSha256')):
        reject(7)
    binding = tokens[0]
    if (binding.get('tenantId') != registry.get('tenantIds', {}).get(tenant)
            or binding.get('issuer') != clients[0].get('issuer')
            or binding.get('clientPk') != str(clients[0].get('clientPk'))
            or clients[0].get('resourceUris') != [binding.get('resourceUri')]
            or not isinstance(binding.get('scopes'), list)
            or sorted(binding['scopes']) != (['openid'] if scope_probe else sorted(clients[0].get('allowedScopes', [])))):
        reject(7)
    try:
        value = binding.get('expiresAt')
        if not isinstance(value, str) or not value.endswith('Z'):
            reject(7)
        expiry = datetime.datetime.fromisoformat(value.replace('Z', '+00:00'))
        if expiry <= datetime.datetime.now(datetime.timezone.utc):
            reject(6)
    except (ValueError, TypeError):
        reject(7)
    if hashlib.sha256(protected(manifest_file)).hexdigest() != digest:
        reject(3)
    print('CLI-006 COMMITTED ' + str(result_file.relative_to(root)))


def provision_authorization_platform(argv):
    provision_authorization(argv, 'platform')


def provision_authorization_native(argv):
    provision_authorization(argv, 'native')


def provision_authorization(argv, phase):
    import datetime, fcntl, hashlib, json, os, re, signal, stat, subprocess, sys, tempfile
    from pathlib import Path

    manifest_file, result_file, resume, repository = Path(argv[1]), Path(argv[2]), argv[3] == 'true', Path(argv[-1])
    if argv[3] not in ('true', 'false') or phase not in ('platform', 'native'):
        sys.exit(2)

    def reject(code):
        sys.exit(code)

    def protected(path, root=None, maximum=128 * 1024):
        if not path.is_absolute() or path != Path(os.path.normpath(path)) or any(p.is_symlink() for p in (path, *path.parents)):
            reject(3)
        if root is not None and (path == root or not path.is_relative_to(root)):
            reject(3)
        try:
            before = path.lstat()
            if not stat.S_ISREG(before.st_mode) or before.st_uid != os.getuid() or stat.S_IMODE(before.st_mode) != 0o600:
                reject(3)
            if before.st_size > maximum:
                reject(2)
            if root is not None:
                for directory in path.parents:
                    if not directory.is_relative_to(root):
                        break
                    directory_info = directory.lstat()
                    if not stat.S_ISDIR(directory_info.st_mode) or directory_info.st_uid != os.getuid() or stat.S_IMODE(directory_info.st_mode) != 0o700:
                        reject(3)
            with os.fdopen(os.open(path, os.O_RDONLY | os.O_NOFOLLOW), 'rb') as stream:
                data = stream.read(maximum + 1)
            after = path.lstat()
            if len(data) > maximum or (before.st_dev, before.st_ino, before.st_size, before.st_mtime_ns) != (after.st_dev, after.st_ino, after.st_size, after.st_mtime_ns):
                reject(3)
            return data
        except OSError:
            reject(4)

    def decode(data):
        def pairs(values):
            output = {}
            for key, value in values:
                if key in output:
                    reject(2)
                output[key] = value
            return output
        try:
            return json.loads(data.decode('utf-8'), object_pairs_hook=pairs, parse_constant=lambda _: reject(2))
        except (ValueError, UnicodeError):
            reject(2)

    manifest_bytes = protected(manifest_file)
    manifest = decode(manifest_bytes)
    digest = hashlib.sha256(manifest_bytes).hexdigest()
    root = Path(manifest['runtimeRoot'])
    for directory in (root, root / 'evidence', root / 'secrets', root / 'profiles'):
        try:
            info = directory.lstat()
            if not stat.S_ISDIR(info.st_mode) or info.st_uid != os.getuid() or stat.S_IMODE(info.st_mode) != 0o700:
                reject(3)
        except OSError:
            reject(4)
    lock_path = root / 'evidence/operation.lock'
    if lock_path.exists():
        protected(lock_path, root / 'evidence', 1024)
    try:
        lock_fd = os.open(lock_path, os.O_RDWR | os.O_CREAT | os.O_NOFOLLOW, 0o600)
        fcntl.flock(lock_fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
    except (OSError, BlockingIOError):
        reject(5)
    for relative, operation, state in (('prepared.json', 'CLI-001', 'COMMITTED'), ('identity.json', 'CLI-002', 'COMMITTED'), ('control-plane.json', 'CLI-003', 'RUNNING')):
        value = decode(protected(root / 'evidence' / relative, root / 'evidence'))
        if (type(value.get('schemaVersion')) is not int or value.get('schemaVersion') != 1 or value.get('operationId') != operation or value.get('status') != state
                or value.get('runId') != manifest['runId'] or value.get('manifestSha256') != digest):
            reject(5)
    receipt_file = root / 'evidence/db-rbac.json'
    receipt = decode(protected(receipt_file, root / 'evidence'))
    if (receipt.get('runId') != manifest['runId'] or receipt.get('manifestSha256') != digest or receipt.get('role') != 'rbac'
            or receipt.get('databaseName') != 'xingyuan_it_rbac_20261011' or type(receipt.get('oid')) is not int or receipt['oid'] <= 0
            or receipt.get('ownerComment') != 'xingyuan-it:' + manifest['runId'] + ':' + digest):
        reject(3)
    for reference in manifest['artifacts'].values():
        artifact = Path(reference['path'])
        if not artifact.is_file() or any(path.is_symlink() for path in (artifact, *artifact.parents)) or hashlib.sha256(artifact.read_bytes()).hexdigest() != reference['sha256']:
            reject(3)
    if not manifest['artifacts']:
        reject(4)
    for reference in manifest['policyFiles'] + (manifest['catalogFiles'] if phase == 'native' else []):
        if hashlib.sha256(protected(Path(reference['path']), root, 2 * 1024 * 1024)).hexdigest() != reference['sha256']:
            reject(3)
    registry_file = root / 'secrets/runtime-registry.json'
    registry_bytes = protected(registry_file, root / 'secrets', 2 * 1024 * 1024)
    registry = decode(registry_bytes)
    if registry.get('runId') != manifest['runId'] or registry.get('manifestSha256') != digest:
        reject(3)
    tenants = registry.get('tenantIds')
    if (not isinstance(tenants, dict) or set(tenants) != {'default', 'tenant-b'}
            or any(not isinstance(value, str) or not re.fullmatch(r'[1-9][0-9]{0,18}', value) for value in tenants.values())
            or any(int(value) > 9223372036854775807 for value in tenants.values()) or len(set(tenants.values())) != 2):
        reject(2)
    frozen_native_inputs = {}
    if phase == 'native':
        for filename, operation, state, step in (('control-plane.json', 'CLI-003', 'RUNNING', 'CONTROL_PLANE'),
                                               ('authorization-platform.json', 'CLI-004', 'COMMITTED', 'PLATFORM_AUTHORIZATION')):
            require_operation_receipt(manifest, digest, filename, operation, state, step)
            path = root / 'evidence' / filename
            frozen_native_inputs[path] = protected(path, root / 'evidence')
        native_input = validate_native_authorization_input(manifest, digest, Path(argv[4]), registry)
        frozen_native_inputs[Path(argv[4])] = protected(Path(argv[4]), root / 'secrets')
        frozen_native_inputs[registry_file] = registry_bytes
    canonical = root / ('evidence/authorization-' + phase + '.json')
    previous_results = {}
    for destination in set((canonical, result_file)):
        if destination.exists():
            previous_results[destination] = protected(destination, root / 'evidence')
            old = decode(previous_results[destination])
            if (not resume or old.get('runId') != manifest['runId'] or old.get('manifestSha256') != digest
                    or old.get('operationId') != 'CLI-004' or old.get('status') != 'COMMITTED'):
                reject(5)
            if phase == 'native' and old.get('steps') != [{'stepId': 'NATIVE_AUTHORIZATION', 'state': 'COMMITTED', 'evidence': 'evidence/authorization-native-owner.json'}]:
                reject(5)
    log_path = root / ('evidence/provision-authorization-' + phase + '-owner.log')
    if log_path.exists():
        if not resume:
            reject(5)
        protected(log_path, root / 'evidence', 16 * 1024 * 1024)
        descriptor = os.open(log_path, os.O_WRONLY | os.O_APPEND | os.O_NOFOLLOW)
    else:
        descriptor = os.open(log_path, os.O_WRONLY | os.O_CREAT | os.O_EXCL | os.O_NOFOLLOW, 0o600)
    arguments = [str(repository / 'mvnw'), '-B', '-ntp', '-o', '-Duser.timezone=UTC', '-pl',
                 'egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin', '-am',
                 '-Dtest=IsolatedAuthorizationProvisioningIT#' + ('provisionPlatformAuthorization' if phase == 'platform' else 'provisionNativeAuthorization'), '-Dsurefire.failIfNoSpecifiedTests=false',
                 '-Dxingyuan.isolated.repository-root=' + str(repository), '-Dxingyuan.isolated.manifest-file=' + str(manifest_file),
                 '-Dxingyuan.isolated.receipt-file=' + str(receipt_file), '-Dxingyuan.isolated.resume=' + str(resume).lower(), 'test']
    settings = [reference for reference in manifest['policyFiles'] if Path(reference['path']).name == 'maven-settings.xml']
    if len(settings) > 1:
        reject(2)
    if settings:
        if Path(settings[0]['path']) != root / 'policies/maven-settings.xml':
            reject(3)
        arguments[1:1] = ['-s', settings[0]['path']]
    environment = dict(os.environ, XINGYUAN_ISOLATED_INTEGRATION='true')
    for key in ('JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', '_JAVA_OPTIONS', 'MAVEN_OPTS', 'MAVEN_ARGS'):
        environment.pop(key, None)
    try:
        with os.fdopen(descriptor, 'ab') as output:
            child = subprocess.Popen(arguments, cwd=repository, env=environment, stdin=subprocess.DEVNULL,
                                     stdout=output, stderr=subprocess.STDOUT, start_new_session=True)
            try:
                returncode = child.wait(timeout=manifest['deadlines']['operationSeconds'])
            except subprocess.TimeoutExpired:
                if child.poll() is None:
                    os.killpg(child.pid, signal.SIGTERM)
                try:
                    child.wait(timeout=min(10, manifest['deadlines']['startupSeconds']))
                except subprocess.TimeoutExpired:
                    pass
                reject(9)
    except OSError:
        reject(6)
    if returncode:
        log = protected(log_path, root / 'evidence', 16 * 1024 * 1024).decode('utf-8', errors='replace')
        failures = {'XINGYUAN_INPUT_INVALID': 2, 'XINGYUAN_TARGET_UNSAFE': 3, 'XINGYUAN_INPUT_UNAVAILABLE': 4,
                    'XINGYUAN_STATE_CONFLICT': 5, 'XINGYUAN_DEPENDENCY_UNAVAILABLE': 6, 'XINGYUAN_PARTIAL': 7, 'XINGYUAN_COMMIT_UNKNOWN': 9}
        found = [code for key, code in failures.items() if key in log]
        reject(9 if 9 in found else found[-1] if found else 6)
    if protected(manifest_file) != manifest_bytes:
        reject(3)
    if phase == 'native':
        if any(protected(path, root, 2 * 1024 * 1024) != contents for path, contents in frozen_native_inputs.items()):
            reject(3)
        if validate_native_authorization_input(manifest, digest, Path(argv[4]), registry) != native_input:
            reject(3)
    owner_file = root / ('evidence/authorization-' + phase + '-owner.json')
    owner = decode(protected(owner_file, root / 'evidence'))
    expected_snapshots = {tenant: (tenant, 4) for tenant in tenants.values()} if phase == 'platform' else {
        tenant + ':' + user: (tenant, 8 if user == 'admin' else 4) for tenant in tenants.values() for user in ('admin', 'viewer')}
    if not isinstance(owner, dict):
        reject(7)
    if (type(owner.get('schemaVersion')) is not int or owner.get('schemaVersion') != 1 or owner.get('runId') != manifest['runId'] or owner.get('manifestSha256') != digest
            or type(owner.get('databaseOid')) is not int or owner.get('databaseOid') != receipt['oid'] or owner.get('phase') != phase or owner.get('status') != 'COMMITTED'
            or phase == 'platform' and (not isinstance(owner.get('commandSha256'), str) or not re.fullmatch(r'[0-9a-f]{64}', owner['commandSha256']))
            or not isinstance(owner.get('snapshots'), dict) or set(owner['snapshots']) != set(expected_snapshots)):
        reject(7)
    now = datetime.datetime.now(datetime.timezone.utc)
    if phase == 'native':
        validate_native_authorization_owner(manifest, digest, receipt['oid'], registry_bytes, owner, now)
    for key, snapshot in owner['snapshots'].items():
        tenant, role_count = expected_snapshots[key]
        if not isinstance(snapshot, dict) or not isinstance(snapshot.get('expiresAt'), str):
            reject(7)
        try:
            expiry = datetime.datetime.fromisoformat(snapshot['expiresAt'].replace('Z', '+00:00'))
        except (KeyError, ValueError, TypeError):
            reject(7)
        roles = snapshot.get('selectedRoleIds')
        if (snapshot.get('tenantId') != tenant or not isinstance(snapshot.get('rbacUserId'), str)
                or not re.fullmatch(r'[1-9][0-9]{0,18}', snapshot['rbacUserId']) or int(snapshot['rbacUserId']) > 9223372036854775807
                or any(type(snapshot.get(key)) is not int or not 1 <= snapshot[key] <= 9223372036854775807 for key in ('authVersion', 'policyVersion'))
                or not isinstance(roles, list) or len(roles) != role_count
                or any(not isinstance(role, str) or not re.fullmatch(r'[1-9][0-9]{0,18}', role) or int(role) > 9223372036854775807 for role in roles) or len(set(roles)) != role_count
                or not isinstance(snapshot.get('snapshotChecksum'), str) or not re.fullmatch(r'[0-9a-f]{64}', snapshot['snapshotChecksum'])
                or not snapshot['expiresAt'].endswith('Z') or expiry.tzinfo is None or expiry.utcoffset() != datetime.timedelta() or expiry <= now):
            reject(7)
    targets = [(code + ':platform-snapshot', tenant) for code, tenant in sorted(tenants.items())] if phase == 'platform' else [
        (code + ':' + user + ':native-snapshot', tenant + ':' + user) for code, tenant in sorted(tenants.items()) for user in ('admin', 'viewer')]
    result = {'schemaVersion': 1, 'operationId': 'CLI-004', 'runId': manifest['runId'], 'manifestSha256': digest,
              'status': 'COMMITTED', 'completedAt': now.isoformat().replace('+00:00', 'Z'),
              'targets': [{'targetKey': target, 'identity': 'USER_SNAPSHOT',
                           'version': str(owner['snapshots'][key]['authVersion']) + ':' + str(owner['snapshots'][key]['policyVersion']), 'state': 'COMMITTED'}
                          for target, key in targets],
              'counts': {'databaseCount': 9, 'tenantCount': 2, 'clientCount': 0, 'catalogCount': 4, 'liveCaseCount': 0},
              'steps': [{'stepId': phase.upper() + '_AUTHORIZATION', 'state': 'COMMITTED', 'evidence': str(owner_file.relative_to(root))}], 'error': None}
    if phase == 'native':
        for destination in set((canonical, result_file)):
            publish_task_json(destination, root, result, previous_results.get(destination))
        print('CLI-004 COMMITTED ' + str(result_file.relative_to(root)))
        return
    encoded = (json.dumps(result, sort_keys=True, separators=(',', ':')) + '\n').encode('utf-8')
    for destination in set((canonical, result_file)):
        if destination.exists():
            protected(destination, root / 'evidence')
        descriptor, temporary = tempfile.mkstemp(prefix='.' + phase + '-', suffix='.json', dir=destination.parent)
        try:
            with os.fdopen(descriptor, 'wb') as output:
                output.write(encoded)
                output.flush()
                os.fsync(output.fileno())
            os.replace(temporary, destination)
            directory = os.open(destination.parent, os.O_RDONLY)
            try:
                os.fsync(directory)
            finally:
                os.close(directory)
        finally:
            if os.path.exists(temporary):
                os.unlink(temporary)
    print('CLI-004 COMMITTED ' + str(result_file.relative_to(root)))



def validate_native_authorization_input(manifest, digest, token_index, registry):
    """The native metadata owner needs one formal Tianshu USER input before consumers exist."""
    import sys

    resources = registry.get('resources')
    bindings = registry.get('cliResources')
    if not isinstance(resources, list) or not isinstance(bindings, list):
        sys.exit(2)
    selected = [value for value in resources if isinstance(value, dict) and value.get('rbacApplicationCode') == 'tianshu-admin']
    if len(selected) != 1 or selected[0].get('status') != 'ACTIVE':
        sys.exit(3)
    matches = [value for value in bindings if isinstance(value, dict) and value.get('resourceServerId') == selected[0].get('resourceServerId')]
    if len(matches) != 1 or not isinstance(matches[0].get('resourceKey'), str):
        sys.exit(3)
    identity = ('default', matches[0]['resourceKey'], 'admin')
    inputs = validate_formal_token_index(manifest, digest, token_index, {identity})
    selected_input = inputs[identity]
    if not {'TIANSHU_READ', 'TIANSHU_WRITE'} <= set(selected_input['scopes']):
        sys.exit(3)
    return selected_input


def validate_native_authorization_owner(manifest, digest, database_oid, registry_bytes, owner, now):
    """Bind the complete parent proof to its actual private receipts, never a partial role-preparation scope."""
    import datetime
    import hashlib
    import re
    import sys
    from pathlib import Path

    fields = {'schemaVersion', 'runId', 'manifestSha256', 'databaseOid', 'phase', 'registrySha256', 'actorSha256',
              'catalogReviewSha256', 'assignmentSha256', 'status', 'recordedAt', 'snapshots'}
    if set(owner) != fields or owner['registrySha256'] != hashlib.sha256(registry_bytes).hexdigest():
        sys.exit(7)
    root = Path(manifest['runtimeRoot'])
    parts = {}
    for key, filename in (('actorSha256', 'native-actors.json'), ('catalogReviewSha256', 'native-catalog-review.json'),
                          ('assignmentSha256', 'native-role-assignments.json')):
        contents = read_task_file(root / 'evidence' / filename, root / 'evidence')
        part = decode_task_json(contents)
        if (owner[key] != hashlib.sha256(contents).hexdigest() or not isinstance(part, dict)
                or type(part.get('schemaVersion')) is not int or part['schemaVersion'] != 1 or part.get('runId') != manifest['runId']
                or part.get('manifestSha256') != digest or type(part.get('databaseOid')) is not int or part['databaseOid'] != database_oid
                or part.get('status') != 'COMMITTED'):
            sys.exit(7)
        parts[key] = part
    if parts['assignmentSha256'].get('scope') != 'NATIVE_ROLE_ASSIGNMENTS_ONLY':
        sys.exit(7)
    for value in [owner, *parts.values()]:
        timestamp = value.get('recordedAt')
        try:
            instant = datetime.datetime.fromisoformat(timestamp.replace('Z', '+00:00'))
            if not timestamp.endswith('Z') or instant.tzinfo is None or instant.utcoffset() != datetime.timedelta() or instant > now:
                sys.exit(7)
        except (ValueError, TypeError, AttributeError):
            sys.exit(7)
    tenants = decode_task_json(registry_bytes)['tenantIds']
    actors = parts['actorSha256'].get('actors')
    if not isinstance(actors, dict) or set(actors) != set(tenants.values()):
        sys.exit(7)
    seen = set()
    for tenant, actor in actors.items():
        if not isinstance(actor, dict) or set(actor) != {'adminUserId', 'viewerUserId', 'provisionerUserId'}:
            sys.exit(7)
        for value in actor.values():
            if not isinstance(value, str) or not re.fullmatch(r'[1-9][0-9]{0,18}', value) or int(value) > 9223372036854775807 or value in seen:
                sys.exit(7)
            seen.add(value)
        for user in ('admin', 'viewer'):
            snapshot = owner['snapshots'].get(tenant + ':' + user)
            if (not isinstance(snapshot, dict) or set(snapshot) != {'tenantId', 'rbacUserId', 'authVersion', 'policyVersion', 'selectedRoleIds', 'snapshotChecksum', 'expiresAt'}
                    or snapshot['rbacUserId'] != actor[user + 'UserId']):
                sys.exit(7)


def read_task_file(path, root=None, maximum=128 * 1024):
    import os
    import stat
    import sys
    from pathlib import Path

    path = Path(path)
    if (not path.is_absolute() or path != Path(os.path.normpath(path))
            or any(value.is_symlink() for value in (path, *path.parents))
            or root is not None and (path == root or not path.is_relative_to(root))):
        sys.exit(3)
    try:
        before = path.lstat()
        if not stat.S_ISREG(before.st_mode) or before.st_uid != os.getuid() or stat.S_IMODE(before.st_mode) != 0o600:
            sys.exit(3)
        if before.st_size > maximum:
            sys.exit(2)
        if root is not None:
            for directory in path.parents:
                if not directory.is_relative_to(root):
                    break
                meta = directory.lstat()
                if not stat.S_ISDIR(meta.st_mode) or meta.st_uid != os.getuid() or stat.S_IMODE(meta.st_mode) != 0o700:
                    sys.exit(3)
        with os.fdopen(os.open(path, os.O_RDONLY | os.O_NOFOLLOW), 'rb') as source:
            content = source.read(maximum + 1)
        after = path.lstat()
        if len(content) > maximum:
            sys.exit(2)
        if (before.st_dev, before.st_ino, before.st_size, before.st_mtime_ns) != (after.st_dev, after.st_ino, after.st_size, after.st_mtime_ns):
            sys.exit(3)
        return content
    except OSError:
        sys.exit(4)


def decode_task_json(content):
    import json
    import sys

    def pairs(values):
        result = {}
        for key, value in values:
            if key in result:
                sys.exit(2)
            result[key] = value
        return result

    try:
        return json.loads(content.decode('utf-8'), object_pairs_hook=pairs, parse_constant=lambda _: sys.exit(2))
    except (ValueError, UnicodeError):
        sys.exit(2)


def publish_task_json(path, root, value, expected=None):
    import json
    import os
    import stat
    import sys
    import tempfile
    from pathlib import Path

    path = Path(path)
    if not path.is_relative_to(root / 'evidence') or any(p.is_symlink() for p in (path, *path.parents)):
        sys.exit(3)
    parent = path.parent.lstat()
    if not stat.S_ISDIR(parent.st_mode) or parent.st_uid != os.getuid() or stat.S_IMODE(parent.st_mode) != 0o700:
        sys.exit(3)
    if expected is None and path.exists() or expected is not None and read_task_file(path, root / 'evidence') != expected:
        sys.exit(5)
    encoded = (json.dumps(value, sort_keys=True, separators=(',', ':'), allow_nan=False) + '\n').encode('utf-8')
    if len(encoded) > 128 * 1024:
        sys.exit(2)
    descriptor, temporary = tempfile.mkstemp(prefix='.' + path.name + '-', suffix='.tmp', dir=path.parent)
    try:
        with os.fdopen(descriptor, 'wb') as output:
            output.write(encoded)
            output.flush()
            os.fsync(output.fileno())
        if expected is None:
            os.link(temporary, path, follow_symlinks=False)
        else:
            os.replace(temporary, path)
        directory = os.open(path.parent, os.O_RDONLY)
        try:
            os.fsync(directory)
        finally:
            os.close(directory)
    except FileExistsError:
        sys.exit(5)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)


def run_task_fixture(manifest_file, manifest, digest, stage):
    import hashlib
    import os
    import signal
    import subprocess
    import sys
    import uuid
    from pathlib import Path

    choices = {
        'profiles': ('egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin',
                     'IsolatedOAuthProvisioningIT#generateRuntimeProfiles', 'idp', 'profiles-ready.json'),
        'tianshu-catalog': ('egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin',
                           'IsolatedTianshuCatalogProvisioningIT#provisionPlatformCatalog', 'tianshu', 'tianshu-platform-catalog.json'),
        'control-clients': ('egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin',
                            'IsolatedAuthorizationProvisioningIT#verifyControlPlaneClients', 'rbac', 'control-plane-clients.json')}
    if stage not in choices:
        sys.exit(2)
    module, selector, database, receipt_name = choices[stage]
    root = Path(manifest['runtimeRoot'])
    repository = Path(manifest['repoRoot'])
    receipt_file = root / 'evidence' / ('db-' + database + '.json')
    receipt = decode_task_json(read_task_file(receipt_file, root / 'evidence'))
    if receipt.get('runId') != manifest['runId'] or receipt.get('manifestSha256') != digest or receipt.get('role') != database:
        sys.exit(3)
    arguments = [str(repository / 'mvnw'), '-B', '-ntp', '-o', '-Duser.timezone=UTC', '-pl', module, '-am',
                 '-Dtest=' + selector, '-Dsurefire.failIfNoSpecifiedTests=false',
                 '-Dxingyuan.isolated.repository-root=' + str(repository), '-Dxingyuan.isolated.manifest-file=' + str(manifest_file),
                 '-Dxingyuan.isolated.receipt-file=' + str(receipt_file),
                 '-Dxingyuan.isolated.resume=' + str((root / 'evidence' / receipt_name).exists()).lower(), 'test']
    settings = [value for value in manifest['policyFiles'] if Path(value['path']) == root / 'policies/maven-settings.xml']
    if len(settings) > 1:
        sys.exit(2)
    if settings:
        if hashlib.sha256(read_task_file(Path(settings[0]['path']), root, 2 * 1024 * 1024)).hexdigest() != settings[0]['sha256']:
            sys.exit(3)
        arguments[1:1] = ['-s', settings[0]['path']]
    environment = dict(os.environ, XINGYUAN_ISOLATED_INTEGRATION='true')
    for key in ('JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', '_JAVA_OPTIONS', 'MAVEN_OPTS', 'MAVEN_ARGS'):
        environment.pop(key, None)
    log_path = root / 'evidence' / (stage + '-owner-' + uuid.uuid4().hex + '.log')
    descriptor = os.open(log_path, os.O_WRONLY | os.O_CREAT | os.O_EXCL | os.O_NOFOLLOW, 0o600)
    try:
        with os.fdopen(descriptor, 'ab') as log:
            child = subprocess.Popen(arguments, cwd=repository, env=environment, stdout=log, stderr=subprocess.STDOUT,
                                     stdin=subprocess.DEVNULL, start_new_session=True)
            try:
                returncode = child.wait(timeout=manifest['deadlines']['operationSeconds'])
            except subprocess.TimeoutExpired:
                if child.poll() is None:
                    os.killpg(child.pid, signal.SIGTERM)
                try:
                    child.wait(timeout=10)
                except subprocess.TimeoutExpired:
                    pass
                sys.exit(9)
    except OSError:
        sys.exit(6)
    if returncode:
        output = read_task_file(log_path, root / 'evidence', 16 * 1024 * 1024).decode('utf-8', errors='replace')
        failures = {'XINGYUAN_INPUT_INVALID': 2, 'XINGYUAN_TARGET_UNSAFE': 3, 'XINGYUAN_INPUT_UNAVAILABLE': 4,
                    'XINGYUAN_STATE_CONFLICT': 5, 'XINGYUAN_DEPENDENCY_UNAVAILABLE': 6, 'XINGYUAN_PARTIAL': 7, 'XINGYUAN_COMMIT_UNKNOWN': 9}
        found = [code for message, code in failures.items() if message in output]
        sys.exit(9 if 9 in found else found[-1] if found else 6)
    result = decode_task_json(read_task_file(root / 'evidence' / receipt_name, root / 'evidence'))
    if result.get('runId') != manifest['runId'] or result.get('manifestSha256') != digest:
        sys.exit(7)
    if stage == 'tianshu-catalog' and result.get('status') != 'COMMITTED' or stage == 'control-clients' and result.get('status') != 'READY':
        sys.exit(7)
    return result


def task_artifact_sha256(path):
    import hashlib
    import sys
    from pathlib import Path

    path = Path(path)
    if not path.is_file() or any(value.is_symlink() for value in (path, *path.parents)):
        sys.exit(3)
    digest = hashlib.sha256()
    with path.open('rb') as source:
        for block in iter(lambda: source.read(64 * 1024), b''):
            digest.update(block)
    return digest.hexdigest()


def native_build_tree_fingerprint(directory):
    """Bind source/product bytes and modes; Maven outputs are never source inputs."""
    import hashlib
    import json
    import stat
    import sys
    from pathlib import Path

    directory = Path(directory)
    if not directory.is_dir() or any(path.is_symlink() for path in (directory, *directory.parents)):
        sys.exit(3)
    files = []
    for path in sorted(directory.rglob('*')):
        relative = path.relative_to(directory)
        if 'target' in relative.parts or '.git' in relative.parts or path.name == '.flattened-pom.xml':
            continue
        if path.is_symlink():
            sys.exit(3)
        if path.is_dir():
            continue
        info = path.stat()
        if not stat.S_ISREG(info.st_mode) or len(files) >= 100000:
            sys.exit(3)
        files.append([relative.as_posix(), stat.S_IMODE(info.st_mode), task_artifact_sha256(path)])
    if not files:
        sys.exit(4)
    return {'sha256': hashlib.sha256(json.dumps(files, separators=(',', ':')).encode()).hexdigest(), 'fileCount': len(files)}


def collect_native_build_products(snapshot, started_ns):
    """Read the original all-seven verifier outputs, including each native security suite."""
    import re
    import sys
    import xml.etree.ElementTree as ET
    import zipfile
    from pathlib import Path

    snapshot = Path(snapshot)
    products = {}
    for family in ('light', 'web', 'service', 'agent'):
        definition = snapshot / 'egon-cola-archetypes/definitions' / ('egon-cola-archetype-' + family)
        properties = {}
        for line in (definition / 'src/test/resources/projects/basic/archetype.properties').read_text().splitlines():
            if line and not line.startswith('#'):
                key, value = line.split('=', 1)
                properties[key.strip()] = value.strip()
        if any(not re.fullmatch(r'[A-Za-z0-9_.-]{1,128}', properties.get(key, '')) for key in ('groupId', 'artifactId', 'version', 'package')):
            sys.exit(3)
        generated = snapshot / 'egon-cola-archetypes/.generated' / ('egon-cola-archetype-' + family)
        project = generated / 'target/test-classes/projects/basic/project' / properties['artifactId']
        build_log = project / 'build.log'
        if not build_log.is_file() or build_log.stat().st_mtime_ns < started_ns:
            sys.exit(8)
        candidates = []
        for jar in project.rglob('target/*.jar'):
            if jar.name.endswith(('-sources.jar', '-javadoc.jar', '-tests.jar')):
                continue
            before_sha = task_artifact_sha256(jar)
            with zipfile.ZipFile(jar) as archive:
                if len(archive.namelist()) != len(set(archive.namelist())):
                    sys.exit(3)
                try:
                    metadata = archive.read('META-INF/MANIFEST.MF').decode('utf-8')
                except KeyError:
                    continue
                main = [line.removeprefix('Main-Class: ').strip() for line in metadata.splitlines() if line.startswith('Main-Class: ')]
                start = [line.removeprefix('Start-Class: ').strip() for line in metadata.splitlines() if line.startswith('Start-Class: ')]
                if main == ['org.springframework.boot.loader.launch.JarLauncher'] and len(start) == 1 and start[0].startswith(properties['package'] + '.'):
                    if (jar.stat().st_mtime_ns < started_ns or 'BOOT-INF/classes/' + start[0].replace('.', '/') + '.class' not in archive.namelist()
                            or task_artifact_sha256(jar) != before_sha):
                        sys.exit(8)
                    candidates.append((jar, start[0]))
        if len(candidates) != 1:
            sys.exit(8)
        jar, start_class = candidates[0]
        required = {'NativeSecurityContractTest', 'ExplicitConstructorInjectionTest'}
        if family in ('light', 'web'):
            required.add('NativeGraphQlSecurityContractTest')
        if family == 'agent':
            required.add('NativeAgentSecurityContractTest')
        suites = []
        for report in project.rglob('target/surefire-reports/TEST-*.xml'):
            suite = ET.parse(report).getroot()
            classname = suite.get('name', '')
            if classname.rsplit('.', 1)[-1].split('$', 1)[0] not in required:
                continue
            cases = list(suite.findall('testcase'))
            if (report.stat().st_mtime_ns < started_ns or not classname.startswith(properties['package'] + '.')
                    or int(suite.get('tests', '0')) != len(cases)
                    or any(int(suite.get(key, '-1')) != 0 for key in ('failures', 'errors', 'skipped'))
                    or any(case.find(key) is not None for case in cases for key in ('failure', 'error', 'skipped'))):
                sys.exit(8)
            if not cases:
                continue
            suites.append({'classname': classname, 'testCount': len(cases), 'path': str(report.relative_to(snapshot)), 'reportSha256': task_artifact_sha256(report)})
        if {suite['classname'].rsplit('.', 1)[-1].split('$', 1)[0] for suite in suites} != required:
            sys.exit(8)
        products['native-' + family] = {'path': str(jar), 'sha256': task_artifact_sha256(jar), 'startClass': start_class,
                                      'consumer': {key: properties[key] for key in ('groupId', 'artifactId', 'version', 'package')},
                                      'project': native_build_tree_fingerprint(project), 'securitySuites': suites,
                                      'generationManifestSha256': task_artifact_sha256(generated / 'generation-manifest.sha256'),
                                      'buildLogSha256': task_artifact_sha256(build_log)}
    return products


def build_native_provenance(argv):
    """Explicit, offline source/build operation. It never connects to runtime facilities."""
    import datetime
    import json
    import os
    import re
    import signal
    import subprocess
    import sys
    import tarfile
    import time
    from pathlib import Path

    repository, result_file, settings = map(Path, argv[1:4])
    if repository != Path(__file__).resolve().parents[3] or os.environ.get('XINGYUAN_ISOLATED_INTEGRATION') != 'true':
        sys.exit(3)
    workspace = result_file.parent
    if (workspace.parent != repository / '.runtime/xingyuan-plan-execution'
            or not re.fullmatch(r'step17-native-build-[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}', workspace.name)
            or result_file.name != 'native-build-provenance.json' or workspace.exists()
            or not settings.is_relative_to(repository / '.runtime')
            or any(path.is_symlink() for path in (*workspace.parents, settings, *settings.parents))):
        sys.exit(3)
    settings_bytes = read_task_file(settings, maximum=2 * 1024 * 1024)
    workspace.mkdir(mode=0o700)
    environment = isolated_runtime_environment()
    # All nested Maven invocations, including the original generator/verifiers, inherit offline settings.
    # A fixed no-whitespace copy avoids Maven's environment argument word splitting.
    if any(character.isspace() for character in str(workspace)):
        sys.exit(3)
    settings_copy = workspace / 'maven-settings.xml'
    publish_private_file(settings_copy, workspace, settings_bytes)
    environment['MAVEN_ARGS'] = '-o -s ' + str(settings_copy)
    environment['TZ'] = 'UTC'
    started_ns = time.time_ns()
    deadline = time.monotonic() + 7200
    commands = []

    def run(name, arguments, cwd):
        remaining = int(deadline - time.monotonic())
        if remaining <= 0:
            sys.exit(7)
        log = workspace / (name + '.log')
        fd = os.open(log, os.O_WRONLY | os.O_CREAT | os.O_EXCL | os.O_NOFOLLOW, 0o600)
        entry = None
        owner = {'runId': workspace.name, 'deadlines': {'httpSeconds': 5, 'startupSeconds': 10}}
        previous_handlers = {value: signal.getsignal(value) for value in (signal.SIGTERM, signal.SIGINT)}
        def cancel(signum, frame):
            sys.exit(7)
        try:
            for value in previous_handlers:
                signal.signal(value, cancel)
            with os.fdopen(fd, 'wb') as output:
                process = subprocess.Popen(arguments, cwd=cwd, env=environment, stdin=subprocess.DEVNULL,
                                           stdout=output, stderr=subprocess.STDOUT, start_new_session=True)
                entry = {'runId': workspace.name, 'process': process, 'record': short_process_fingerprint(process.pid, 5)}
                code = process.wait(timeout=remaining)
                output.flush()
                os.fsync(output.fileno())
        except subprocess.TimeoutExpired:
            sys.exit(7)
        finally:
            try:
                if entry is not None:
                    stop_verification_process(owner, entry)
            finally:
                for value, handler in previous_handlers.items():
                    signal.signal(value, handler)
        if code != 0:
            sys.exit(8)
        commands.append({'stage': name, 'exitCode': 0, 'logSha256': task_artifact_sha256(log)})

    head = subprocess.run(['git', 'rev-parse', 'HEAD'], cwd=repository, check=True, capture_output=True, text=True, timeout=10).stdout.strip()
    if not re.fullmatch(r'[0-9a-f]{40}', head):
        sys.exit(3)
    archive = workspace / 'source.tar'
    run('export', ['git', 'archive', '--format=tar', '--output=' + str(archive), head], repository)
    snapshot = workspace / 'source'
    snapshot.mkdir(mode=0o700)
    with tarfile.open(archive) as source:
        for member in source.getmembers():
            relative = Path(member.name)
            if member.issym() and member.name == '.claude/skills' and member.linkname == '../.agents/skills':
                continue
            if relative.is_absolute() or '..' in relative.parts or not (member.isfile() or member.isdir()):
                sys.exit(3)
        # Agent-skill links are unrelated to the Maven export and are never followed or copied.
        source.extractall(snapshot, filter=lambda member, destination: None if member.name == '.claude/skills' else tarfile.data_filter(member, destination))
    inputs = {name: native_build_tree_fingerprint(snapshot / name) for name in (
        'egon-cola-archetypes/source-projects', 'egon-cola-archetypes/definitions',
        'egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter')}
    opens = {family: native_build_tree_fingerprint(snapshot / ('egon-cola-archetypes/source-projects/egon-cola-source-' + family + '-open'))
             for family in ('light', 'web', 'service')}
    modules = ','.join(('egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter',
                       'egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-starter',
                       'egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-tianshu-adapter'))
    run('current-dependencies', ['./mvnw', '-B', '-ntp', '-pl', modules, '-am', '-DskipTests', 'install'], snapshot)
    generated = snapshot / 'egon-cola-archetypes/.generated'
    run('generate-first', ['bash', 'scripts/generate_archetypes.sh', 'generate'], snapshot)
    run('check-first', ['bash', 'scripts/generate_archetypes.sh', 'check'], snapshot)
    first = {family: native_build_tree_fingerprint(generated / ('egon-cola-archetype-' + family))
             for family in ('light', 'web', 'service', 'agent', 'light-open', 'web-open', 'service-open')}
    run('generate-second', ['bash', 'scripts/generate_archetypes.sh', 'generate'], snapshot)
    run('check-second', ['bash', 'scripts/generate_archetypes.sh', 'check'], snapshot)
    if any(native_build_tree_fingerprint(generated / ('egon-cola-archetype-' + family)) != proof for family, proof in first.items()):
        sys.exit(8)
    run('source-install', ['./mvnw', '-B', '-ntp', '-f', 'egon-cola-archetypes/source-projects/pom.xml', '-DskipTests', 'install'], snapshot)
    run('resolved-peer-facades', ['./mvnw', '-B', '-ntp', '-f', 'egon-cola-archetypes/source-projects/pom.xml',
                                   '-Ppublish-resolved-facade', '-pl', 'egon-cola-source-web/egon-cola-source-web-facade,egon-cola-source-service/egon-cola-source-service-facade,egon-cola-source-web-open/egon-cola-source-web-open-facade,egon-cola-source-service-open/egon-cola-source-service-open-facade',
                                   '-am', '-DskipTests', 'install'], snapshot)
    run('actual-all-seven', ['./mvnw', '-B', '-ntp', '-f', 'egon-cola-archetypes/pom.xml', '-Pgenerated-archetypes',
                             '-DskipTests=false', 'integration-test'], snapshot)
    products = collect_native_build_products(snapshot, started_ns)
    if any(native_build_tree_fingerprint(snapshot / name) != proof for name, proof in inputs.items()):
        sys.exit(8)
    if any(native_build_tree_fingerprint(snapshot / ('egon-cola-archetypes/source-projects/egon-cola-source-' + family + '-open')) != proof
           or native_build_tree_fingerprint(generated / ('egon-cola-archetype-' + family + '-open')) != first[family + '-open'] for family, proof in opens.items()):
        sys.exit(8)
    current_head = subprocess.run(['git', 'rev-parse', 'HEAD'], cwd=repository, check=True, capture_output=True, text=True, timeout=10).stdout.strip()
    if current_head != head:
        sys.exit(3)
    result = {'schemaVersion': 1, 'scope': 'CURRENT_GENERATED_NATIVE_BUILD_ONLY', 'status': 'PASSED',
                      'sourceHead': head, 'workspace': str(workspace), 'sourceInputs': inputs, 'generatedFamilies': first, 'openSources': opens,
                      'commands': commands, 'artifacts': products, 'startedNs': started_ns,
                      'completedAt': datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00', 'Z')}
    publish_private_file(result_file, workspace,
                         (json.dumps(result, ensure_ascii=False, sort_keys=True, indent=2, allow_nan=False) + '\n').encode('utf-8'))


def read_native_build_provenance(manifest):
    """Require the actual generated projects and current owner outputs before native startup."""
    import datetime
    import hashlib
    import re
    import subprocess
    import sys
    from pathlib import Path

    root, repository = Path(manifest['runtimeRoot']), Path(manifest['repoRoot'])
    file = root / 'policies/native-build-provenance.json'
    references = [item for item in manifest['policyFiles'] if Path(item['path']).name == file.name]
    if not references:
        sys.exit(4)
    raw = read_task_file(file, root / 'policies', 2 * 1024 * 1024)
    if len(references) != 1 or references[0]['path'] != str(file) or references[0]['sha256'] != hashlib.sha256(raw).hexdigest():
        sys.exit(3)
    proof = decode_task_json(raw)
    fields = {'schemaVersion', 'scope', 'status', 'sourceHead', 'workspace', 'sourceInputs', 'generatedFamilies',
              'openSources', 'commands', 'artifacts', 'startedNs', 'completedAt'}
    if (not isinstance(proof, dict) or set(proof) != fields or type(proof['schemaVersion']) is not int or proof['schemaVersion'] != 1
            or proof['status'] != 'PASSED' or proof['scope'] != 'CURRENT_GENERATED_NATIVE_BUILD_ONLY'
            or not isinstance(proof['sourceHead'], str) or not re.fullmatch(r'[0-9a-f]{40}', proof['sourceHead'])
            or type(proof['startedNs']) is not int or proof['startedNs'] <= 0):
        sys.exit(3)
    workspace = Path(proof['workspace'])
    if (workspace.parent != repository / '.runtime/xingyuan-plan-execution'
            or not re.fullmatch(r'step17-native-build-[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}', workspace.name)):
        sys.exit(3)
    if read_task_file(workspace / file.name, workspace, 2 * 1024 * 1024) != raw:
        sys.exit(3)
    try:
        completed = datetime.datetime.fromisoformat(proof['completedAt'].replace('Z', '+00:00'))
        if (not proof['completedAt'].endswith('Z') or completed.utcoffset() != datetime.timedelta()
                or completed.timestamp() * 1000000000 < proof['startedNs'] or completed > datetime.datetime.now(datetime.timezone.utc)):
            sys.exit(3)
    except (TypeError, ValueError, AttributeError):
        sys.exit(3)
    stages = ['export', 'current-dependencies', 'generate-first', 'check-first', 'generate-second', 'check-second', 'source-install', 'resolved-peer-facades', 'actual-all-seven']
    if (not isinstance(proof['commands'], list) or len(proof['commands']) != len(stages)
            or any(not isinstance(item, dict) or set(item) != {'stage', 'exitCode', 'logSha256'}
                   or item['stage'] != stage or type(item['exitCode']) is not int or item['exitCode'] != 0
                   or item['logSha256'] != task_artifact_sha256(workspace / (stage + '.log')) for item, stage in zip(proof['commands'], stages))):
        sys.exit(3)
    inputs = ('egon-cola-archetypes/source-projects', 'egon-cola-archetypes/definitions',
              'egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter')
    snapshot = workspace / 'source'
    if not isinstance(proof['sourceInputs'], dict) or set(proof['sourceInputs']) != set(inputs):
        sys.exit(3)
    for name in inputs:
        if native_build_tree_fingerprint(snapshot / name) != proof['sourceInputs'][name]:
            sys.exit(3)
    # Step17's own later commit is allowed; changes to build inputs require a new real build.
    for command in (['git', 'merge-base', '--is-ancestor', proof['sourceHead'], 'HEAD'],
                    ['git', 'diff', '--quiet', proof['sourceHead'], '--', *inputs, 'scripts/generate_archetypes.sh', 'pom.xml']):
        checked = subprocess.run(command, cwd=repository, stdin=subprocess.DEVNULL, capture_output=True, timeout=10)
        if checked.returncode != 0:
            sys.exit(3)
    families = ('light', 'web', 'service', 'agent', 'light-open', 'web-open', 'service-open')
    if not isinstance(proof['generatedFamilies'], dict) or set(proof['generatedFamilies']) != set(families):
        sys.exit(3)
    for family in families:
        if native_build_tree_fingerprint(snapshot / 'egon-cola-archetypes/.generated' / ('egon-cola-archetype-' + family)) != proof['generatedFamilies'][family]:
            sys.exit(3)
    if not isinstance(proof['openSources'], dict) or set(proof['openSources']) != {'light', 'web', 'service'}:
        sys.exit(3)
    for family in proof['openSources']:
        if native_build_tree_fingerprint(snapshot / ('egon-cola-archetypes/source-projects/egon-cola-source-' + family + '-open')) != proof['openSources'][family]:
            sys.exit(3)
    products = collect_native_build_products(snapshot, proof['startedNs'])
    if products != proof['artifacts']:
        sys.exit(3)
    for role, product in products.items():
        reference = manifest['artifacts'].get(role)
        if not isinstance(reference, dict) or any(reference.get(key) != product[key] for key in ('path', 'sha256')):
            sys.exit(3)
    return proof


def observe_native_build(argv):
    """Copy only revalidated source/build facts into the current verification attempt."""
    import datetime
    import hashlib
    import re
    import sys
    from pathlib import Path

    manifest_file, result_file = map(Path, argv[1:3])
    raw = read_task_file(manifest_file)
    manifest = decode_task_json(raw)
    root, attempt = Path(manifest['runtimeRoot']), result_file.parent
    if (result_file.name != 'native-generated-build.json' or attempt.parent != root / 'evidence'
            or not re.fullmatch(r'verify-[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}', attempt.name)
            or result_file.exists()):
        sys.exit(3)
    proof = read_native_build_provenance(manifest)
    source = read_task_file(root / 'policies/native-build-provenance.json', root / 'policies', 2 * 1024 * 1024)
    if read_task_file(manifest_file) != raw:
        sys.exit(3)
    publish_task_json(result_file, root, {'schemaVersion': 1, 'runId': manifest['runId'], 'manifestSha256': hashlib.sha256(raw).hexdigest(),
                      'scope': 'FOUR_CURRENT_GENERATED_NATIVE_BUILD_AND_OPEN_PRESERVATION', 'status': 'PASSED',
                      'sourceProvenanceSha256': hashlib.sha256(source).hexdigest(), 'sourceBuild': proof,
                      'recordedAt': datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00', 'Z')})


def stop_owned_and_observe_preservation(argv):
    """Last full-run action: stop the original cohort, then reobserve retained facilities/files."""
    import datetime
    import hashlib
    import re
    import sys
    from pathlib import Path

    manifest_file, result_file = map(Path, argv[1:3])
    manifest_bytes = read_task_file(manifest_file)
    manifest = decode_task_json(manifest_bytes)
    digest = hashlib.sha256(manifest_bytes).hexdigest()
    root, attempt = Path(manifest['runtimeRoot']), result_file.parent
    if (result_file.name != 'resource-owned-stop-preservation.json' or attempt.parent != root / 'evidence'
            or not re.fullmatch(r'verify-[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}', attempt.name)
            or result_file.exists()):
        sys.exit(3)
    intent = decode_task_json(read_task_file(attempt / 'intent.json', attempt))
    if not isinstance(intent, dict) or intent.get('runId') != manifest['runId'] or intent.get('manifestSha256') != digest or intent.get('caseSet') != 'full':
        sys.exit(3)
    # Resource's enclosing JUnit method is still running. These receipts are pre-stop inputs;
    # the parent validates their actual completed JUnit execution after this method returns.
    case_bytes, proof_hashes = {}, {}
    exclusions = runtime_profile_exclusions(manifest)
    for number in range(1, 25):
        if number == 23:
            continue
        case_id = 'LIVE-' + str(number).zfill(3)
        runner = 'native' if 11 <= number <= 15 else 'resource'
        file = attempt / 'cases' / runner / (case_id + '.json')
        raw = read_task_file(file, attempt)
        case = decode_task_json(raw)
        expected_status = 'UNVERIFIED' if number == 15 and 'native-agent' in exclusions else 'PASSED'
        fields = {'schemaVersion', 'runId', 'manifestSha256', 'caseId', 'runner', 'status', 'executedAt', 'testClass', 'testMethod', 'proofs'}
        if expected_status == 'UNVERIFIED':
            fields.add('deferred')
        if (not isinstance(case, dict) or type(case.get('schemaVersion')) is not int or case['schemaVersion'] != 1
                or set(case) != fields
                or case.get('runId') != manifest['runId'] or case.get('manifestSha256') != digest or case.get('caseId') != case_id
                or case.get('runner') != runner or case.get('status') != expected_status
                or case.get('testClass') != 'top.egon.cola.component.yuheng.test.integration.' + ('XingyuanNativeAuthenticationIT' if runner == 'native' else 'XingyuanResourceAuthorizationIT')
                or case.get('testMethod') != ('verifyNativeAuthentication' if runner == 'native' else 'verifyResourceAuthorization')
                or not isinstance(case.get('proofs'), list) or not 1 <= len(case['proofs']) <= 32):
            sys.exit(8)
        if runner == 'native':
            execution = decode_task_json(read_task_file(attempt / 'junit-native.json', attempt))
            read_live_case_evidence(manifest, digest, attempt, case_id, runner, {'native': execution})
        seen = set()
        for proof in case['proofs']:
            if (not isinstance(proof, dict) or set(proof) != {'path', 'sha256'} or not isinstance(proof['path'], str)
                    or not isinstance(proof['sha256'], str) or not re.fullmatch(r'[0-9a-f]{64}', proof['sha256'])):
                sys.exit(8)
            relative = Path(proof['path'])
            target = root / relative
            if relative.is_absolute() or '..' in relative.parts or str(relative) != proof['path'] or not target.is_relative_to(attempt) or target == file or target in seen:
                sys.exit(3)
            if hashlib.sha256(read_task_file(target, attempt)).hexdigest() != proof['sha256']:
                sys.exit(8)
            seen.add(target)
            proof_hashes[target] = proof['sha256']
        case_bytes[file] = raw
    record_file = root / 'evidence/processes.json'
    record_bytes = read_task_file(record_file, root / 'evidence')
    record = decode_task_json(record_bytes)
    validate_owned_process_record(manifest, digest, record)
    expected = {item['id'] for item in manifest['processes']} - set(exclusions)
    if ({item['id'] for item in record['processes']} != expected
            or any(observe_owned_process(manifest, item) != 'RUNNING' for item in record['processes'])):
        sys.exit(6)
    originals = {name: read_task_file(root / 'evidence' / name, root / 'evidence') for name in (
        'original-databases.json', 'original-nginx.json', 'original-redis.json')}
    read_original_database_baseline(manifest, digest)
    read_original_nginx_baseline(manifest, digest)
    read_original_redis_baseline(manifest, digest)
    secrets = {name: hashlib.sha256(read_task_file(Path(path), root / 'secrets', 2 * 1024 * 1024)).hexdigest()
               for name, path in manifest['secretFiles'].items()}
    database_receipts = {target['role']: read_task_file(root / ('evidence/db-' + target['role'] + '.json'), root / 'evidence')
                         for target in manifest['databases']}
    databases = observe_owned_database_metadata(manifest, digest)
    if read_task_file(manifest_file) != manifest_bytes or read_task_file(record_file, root / 'evidence') != record_bytes:
        sys.exit(3)
    stop_file = attempt / 'owned-stop.json'
    process_state([__file__, 'stop', str(manifest_file), str(stop_file)])
    stopped = decode_task_json(read_task_file(stop_file, attempt))
    if (stopped.get('operationId') != 'CLI-009' or stopped.get('runId') != manifest['runId'] or stopped.get('manifestSha256') != digest
            or stopped.get('status') != 'STOPPED' or stopped.get('error') is not None or len(stopped.get('targets', [])) != len(expected)
            or any(item.get('state') != 'STOPPED' for item in stopped['targets'])
            or any(observe_owned_process(manifest, item) != 'STOPPED' for item in record['processes'])):
        sys.exit(8)
    preservation = attempt / 'owned-preservation'
    preservation.mkdir(mode=0o700)
    verify_original_databases(manifest, digest, preservation)
    verify_original_nginx(manifest, digest, preservation)
    verify_original_redis(manifest, digest, preservation)
    if observe_owned_database_metadata(manifest, digest) != databases:
        sys.exit(3)
    if (read_task_file(manifest_file) != manifest_bytes or read_task_file(record_file, root / 'evidence') != record_bytes
            or any(read_task_file(root / 'evidence' / name, root / 'evidence') != raw for name, raw in originals.items())
            or any(read_task_file(file, attempt) != raw for file, raw in case_bytes.items())
            or any(hashlib.sha256(read_task_file(file, attempt)).hexdigest() != checksum for file, checksum in proof_hashes.items())
            or any(hashlib.sha256(read_task_file(Path(manifest['secretFiles'][name]), root / 'secrets', 2 * 1024 * 1024)).hexdigest() != checksum
                   for name, checksum in secrets.items())
            or any(read_task_file(root / ('evidence/db-' + role + '.json'), root / 'evidence') != raw for role, raw in database_receipts.items())):
        sys.exit(3)
    proofs = []
    for filename in ('owned-stop.json', 'owned-preservation/original-databases-preserved.json',
                     'owned-preservation/original-nginx-preserved.json', 'owned-preservation/original-redis-preserved.json'):
        file = attempt / filename
        proofs.append({'path': str(file.relative_to(root)), 'sha256': hashlib.sha256(read_task_file(file, attempt)).hexdigest()})
    publish_task_json(result_file, root, {'schemaVersion': 1, 'runId': manifest['runId'], 'manifestSha256': digest,
                      'scope': 'ORIGINAL_OWNED_COHORT_STOP_AND_BOUNDED_PRESERVATION_ONLY', 'status': 'PASSED',
                      'ownedProcessCount': len(expected), 'retainedCaseCount': len(case_bytes), 'retainedSecretFileCount': len(secrets),
                      'retainedDatabaseReceiptCount': len(database_receipts), 'proofs': proofs,
                      'retainedDatabaseIdentityCount': len(databases),
                      'recordedAt': datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00', 'Z')})


def observe_live_guards(argv):
    """Read the owned cohort and original facilities before full-case mutations; never stop or write a database."""
    import datetime
    import hashlib
    import re
    import sys
    from pathlib import Path

    manifest_file, result_file = map(Path, argv[1:3]); raw = read_task_file(manifest_file); manifest = decode_task_json(raw)
    digest = hashlib.sha256(raw).hexdigest(); root, attempt = Path(manifest['runtimeRoot']), result_file.parent
    if (result_file.name != 'resource-live-guards.json' or attempt.parent != root/'evidence'
            or not re.fullmatch(r'verify-[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}',attempt.name) or result_file.exists()):
        sys.exit(3)
    intent = decode_task_json(read_task_file(attempt/'intent.json',attempt))
    if intent.get('runId') != manifest['runId'] or intent.get('manifestSha256') != digest or intent.get('caseSet') != 'full':
        sys.exit(3)
    record_file = root/'evidence/processes.json'; record_raw = read_task_file(record_file,root/'evidence'); record = decode_task_json(record_raw)
    validate_owned_process_record(manifest,digest,record)
    exclusions = runtime_profile_exclusions(manifest)
    expected = {value['id'] for value in manifest['processes']} - set(exclusions)
    if {value['id'] for value in record['processes']} != expected or any(observe_owned_process(manifest,value) != 'RUNNING' for value in record['processes']):
        sys.exit(6)
    before = observe_owned_database_metadata(manifest,digest)
    preservation=attempt/'guard-preservation'; preservation.mkdir(mode=0o700)
    verify_original_databases(manifest,digest,preservation); verify_original_nginx(manifest,digest,preservation); verify_original_redis(manifest,digest,preservation)
    if (observe_owned_database_metadata(manifest,digest) != before or read_task_file(manifest_file) != raw
            or read_task_file(record_file,root/'evidence') != record_raw or any(observe_owned_process(manifest,value) != 'RUNNING' for value in record['processes'])):
        sys.exit(3)
    proofs=[]
    for name in ('original-databases-preserved.json','original-nginx-preserved.json','original-redis-preserved.json'):
        file=preservation/name; proofs.append({'path':str(file.relative_to(root)),'sha256':hashlib.sha256(read_task_file(file,preservation)).hexdigest()})
    publish_task_json(result_file,root,{'schemaVersion':1,'runId':manifest['runId'],'manifestSha256':digest,'status':'PASSED',
                    'scope':'OWNED_RUNNING_COHORT_AND_ORIGINAL_FACILITY_GUARDS_ONLY','ownedProcessCount':len(expected),'ownedDatabaseCount':len(before),
                    'proofs':proofs,'recordedAt':datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00','Z')})


def observe_owned_database_metadata(manifest, digest):
    """Read only the nine prepared task database identities, never their user schemas."""
    import subprocess
    import sys
    from pathlib import Path

    roles = ('idp', 'rbac', 'tianshu', 'yuheng', 'llm', 'light', 'web', 'service', 'agent')
    expected = {role: 'xingyuan_it_' + role + '_20261011' for role in roles}
    targets = manifest['databases']
    if len(targets) != len(roles) or {item.get('role'): item.get('name') for item in targets} != expected:
        sys.exit(3)
    root = Path(manifest['runtimeRoot'])
    receipts = {role: decode_task_json(read_task_file(root / ('evidence/db-' + role + '.json'), root / 'evidence')) for role in roles}
    owner = receipts['idp'].get('databaseOwner') if isinstance(receipts['idp'], dict) else None
    if not isinstance(owner, str) or not 1 <= len(owner) <= 63 or any(ord(character) < 32 for character in owner):
        sys.exit(3)
    for role, name in expected.items():
        receipt = receipts[role]
        if (not isinstance(receipt, dict) or set(receipt) != {'databaseName', 'oid', 'databaseOwner', 'ownerComment', 'role', 'runId', 'manifestSha256'}
                or receipt.get('role') != role or receipt.get('databaseName') != name or type(receipt.get('oid')) is not int or receipt['oid'] <= 0
                or receipt.get('runId') != manifest['runId'] or receipt.get('manifestSha256') != digest
                or receipt.get('databaseOwner') != owner or receipt.get('ownerComment') != 'xingyuan-it:' + manifest['runId'] + ':' + digest):
            sys.exit(3)
    environment = isolated_runtime_environment()
    environment.update(PGPASSWORD=task_credential(Path(manifest['postgres']['credentialFile'])), PGCONNECT_TIMEOUT='5')
    sql = ("SELECT coalesce(json_agg(json_build_object('databaseName',datname,'oid',oid::bigint,'databaseOwner',pg_get_userbyid(datdba),"
           "'ownerComment',shobj_description(oid,'pg_database')) ORDER BY datname),'[]'::json)::text FROM pg_database WHERE datname IN ("
           + ','.join("'" + name + "'" for name in expected.values()) + ');')
    try:
        result = subprocess.run(['psql', '-X', '--no-password', '-A', '-t', '-v', 'ON_ERROR_STOP=1', '-h', '127.0.0.1', '-p', '5432',
                                 '-U', owner, '-d', 'postgres'], input=sql, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                env=environment, timeout=manifest['deadlines']['httpSeconds'])
    except (OSError, subprocess.TimeoutExpired):
        sys.exit(6)
    if result.returncode or len(result.stdout) > 128 * 1024:
        sys.exit(6)
    rows = decode_task_json(result.stdout.encode('utf-8'))
    if not isinstance(rows, list) or len(rows) != len(roles):
        sys.exit(3)
    actual, oids = {}, set()
    for row in rows:
        if (not isinstance(row, dict) or set(row) != {'databaseName', 'oid', 'databaseOwner', 'ownerComment'}
                or row.get('databaseName') not in expected.values() or row['databaseName'] in actual
                or type(row.get('oid')) is not int or row['oid'] <= 0 or row['oid'] in oids
                or row.get('databaseOwner') != owner or row.get('ownerComment') != 'xingyuan-it:' + manifest['runId'] + ':' + digest):
            sys.exit(3)
        actual[row['databaseName']] = row
        oids.add(row['oid'])
    for role, name in expected.items():
        receipt = receipts[role]
        if (not isinstance(receipt, dict) or receipt.get('runId') != manifest['runId'] or receipt.get('manifestSha256') != digest
                or receipt.get('role') != role or any(receipt.get(key) != value for key, value in actual[name].items())):
            sys.exit(3)
    return dict(sorted(actual.items()))


def publish_private_file(path, root, content):
    import os
    import stat
    import sys
    import tempfile

    if path == root or not path.is_relative_to(root) or any(value.is_symlink() for value in (path, *path.parents)):
        sys.exit(3)
    for directory in path.parents:
        if not directory.is_relative_to(root):
            break
        info = directory.lstat()
        if not stat.S_ISDIR(info.st_mode) or info.st_uid != os.getuid() or stat.S_IMODE(info.st_mode) != 0o700:
            sys.exit(3)
    if path.exists():
        if read_task_file(path, root, max(128 * 1024, len(content))) != content:
            sys.exit(5)
        return
    descriptor, temporary = tempfile.mkstemp(prefix='.' + path.name + '-', suffix='.tmp', dir=path.parent)
    try:
        with os.fdopen(descriptor, 'wb') as output:
            output.write(content)
            output.flush()
            os.fsync(output.fileno())
        os.link(temporary, path, follow_symlinks=False)
        directory = os.open(path.parent, os.O_RDONLY)
        try:
            os.fsync(directory)
        finally:
            os.close(directory)
    except FileExistsError:
        sys.exit(5)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)


def task_credential(path, root=None):
    import sys
    try:
        value = read_task_file(path, root, 4096).decode('utf-8')
    except UnicodeError:
        sys.exit(2)
    if value.endswith('\r\n'):
        value = value[:-2]
    elif value.endswith('\n'):
        value = value[:-1]
    if not value or '\0' in value:
        sys.exit(2)
    return value


def ensure_task_truststore(manifest, digest):
    import hashlib
    import os
    import re
    import shutil
    import ssl
    import subprocess
    import sys
    import uuid
    from pathlib import Path

    root = Path(manifest['runtimeRoot'])
    keytool = shutil.which('keytool')
    if keytool is None:
        sys.exit(4)
    try:
        ca = Path(manifest['secretFiles']['tls-ca-cert'])
        password_file = Path(manifest['secretFiles']['tls-keystore-password'])
    except KeyError:
        sys.exit(4)
    ca_bytes = read_task_file(ca, root / 'secrets', 128 * 1024)
    task_credential(password_file, root / 'secrets')
    try:
        ca_der = ssl.PEM_cert_to_DER_cert(ca_bytes.decode('ascii'))
    except (ValueError, UnicodeError):
        sys.exit(3)
    store = root / 'secrets/java-truststore.p12'
    receipt_file = root / 'evidence/java-truststore.json'
    if store.exists() != receipt_file.exists():
        sys.exit(9)

    def run(*arguments):
        try:
            result = subprocess.run([keytool, *map(str, arguments)], stdin=subprocess.DEVNULL,
                                    stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=manifest['deadlines']['httpSeconds'])
        except (OSError, subprocess.TimeoutExpired):
            sys.exit(6)
        if result.returncode or len(result.stdout) > 2 * 1024 * 1024:
            sys.exit(6)
        return result.stdout

    if not store.exists():
        staged = root / 'secrets' / ('.java-trust-' + uuid.uuid4().hex + '.p12')
        run('-importcert', '-noprompt', '-alias', 'xingyuan-task-ca', '-file', ca, '-keystore', staged,
            '-storetype', 'PKCS12', '-storepass:file', password_file)
        content = read_task_file(staged, root / 'secrets', 2 * 1024 * 1024)
        publish_private_file(store, root, content)
        staged.unlink()
        publish_task_json(receipt_file, root, {'schemaVersion': 1, 'runId': manifest['runId'], 'manifestSha256': digest,
                          'caSha256': hashlib.sha256(ca_der).hexdigest(), 'storeSha256': hashlib.sha256(content).hexdigest()})
    receipt = decode_task_json(read_task_file(receipt_file, root / 'evidence'))
    if (receipt.get('runId') != manifest['runId'] or receipt.get('manifestSha256') != digest
            or receipt.get('caSha256') != hashlib.sha256(ca_der).hexdigest()
            or receipt.get('storeSha256') != hashlib.sha256(read_task_file(store, root / 'secrets', 2 * 1024 * 1024)).hexdigest()):
        sys.exit(3)
    exported = run('-exportcert', '-rfc', '-alias', 'xingyuan-task-ca', '-keystore', store, '-storetype', 'PKCS12', '-storepass:file', password_file)
    matches = re.findall(b'-----BEGIN CERTIFICATE-----.*?-----END CERTIFICATE-----', exported, re.S)
    if len(matches) != 1 or ssl.PEM_cert_to_DER_cert(matches[0].decode('ascii')) != ca_der:
        sys.exit(3)
    return store


def isolated_runtime_environment():
    import os
    allowed = {'PATH', 'HOME', 'JAVA_HOME', 'TMPDIR', 'LANG', 'LC_ALL'}
    environment = {key: value for key, value in os.environ.items() if key in allowed}
    environment['TZ'] = 'UTC'
    return environment


def capture_owned_process(manifest, digest, role, pid):
    import datetime
    import hashlib
    import os
    import subprocess
    import sys
    import time

    declared = next(value for value in manifest['processes'] if value['id'] == role)
    environment = dict(os.environ, LC_ALL='C', TZ='UTC')
    previous = None
    deadline = time.monotonic() + min(10, manifest['deadlines']['startupSeconds'])
    while time.monotonic() < deadline:
        try:
            value = subprocess.run(['ps', '-p', str(pid), '-o', 'lstart=', '-o', 'stat=', '-o', 'command='],
                                   stdin=subprocess.DEVNULL, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                   text=True, env=environment, timeout=manifest['deadlines']['httpSeconds'])
        except (OSError, subprocess.TimeoutExpired):
            sys.exit(6)
        if value.returncode == 1 and not value.stdout.strip() and not value.stderr.strip():
            sys.exit(6)
        if value.returncode or len(value.stdout) > 64 * 1024 or len(value.stdout.strip().splitlines()) != 1:
            sys.exit(6)
        columns = value.stdout.strip().split(None, 6)
        if len(columns) != 7 or columns[5].startswith('Z'):
            sys.exit(6)
        if previous != value.stdout:
            previous = value.stdout
            time.sleep(0.1)
            continue
        try:
            start = datetime.datetime.strptime(' '.join(columns[:5]), '%a %b %d %H:%M:%S %Y').replace(tzinfo=datetime.timezone.utc)
        except ValueError:
            sys.exit(10)
        record = {'id': role, 'pid': pid, 'startInstant': start.isoformat().replace('+00:00', 'Z'),
                  'artifactSha256': manifest['artifacts'][declared['artifactRole']]['sha256'],
                  'commandSha256': hashlib.sha256(columns[6].encode('utf-8')).hexdigest()}
        validate_owned_process_record(manifest, digest, {'schemaVersion': 1, 'runId': manifest['runId'],
                                      'manifestSha256': digest, 'processes': [record]})
        if observe_owned_process(manifest, record) != 'RUNNING':
            sys.exit(6)
        return record
    sys.exit(6)


def prepare_frontend_profiles(manifest, digest):
    """Publish five provenance-bound configs and the existing Portal manifest shape without running Node."""
    import hashlib
    import json
    import os
    import re
    import sys
    from pathlib import Path

    root, repository = Path(manifest['runtimeRoot']), Path(manifest['repoRoot'])
    if not {'tls-server-cert', 'tls-server-key', 'tls-ca-cert'} <= manifest['secretFiles'].keys():
        sys.exit(4)
    registry_bytes = read_task_file(root / 'secrets/runtime-registry.json', root / 'secrets', 2 * 1024 * 1024)
    registry = decode_task_json(registry_bytes)
    if (registry.get('runId') != manifest['runId'] or registry.get('manifestSha256') != digest
            or registry.get('publicOrigin') != manifest['publicOrigin']):
        sys.exit(3)
    roles = {'portal-web': ('edge-ui', '/', None, None),
             'shoubing-web': ('edge-shoubing', '/apps/tianquan-shoubing/', 'tianquan-shoubing', '/overview'),
             'jianshen-web': ('edge-jianshen', '/apps/tianquan-jianshen/', 'tianquan-jianshen', '/iam/roles'),
             'yuheng-web': ('edge-yuheng', '/apps/yuheng/', 'yuheng', '/dashboard'),
             'tianshu-web': ('edge-tianshu', '/apps/tianshu/', 'tianshu', '/registry')}
    declared = {item['id']: item for item in manifest['processes']}
    if not roles.keys() <= declared.keys() or not roles.keys() <= manifest['artifacts'].keys():
        sys.exit(4)
    for directory in (root / 'profiles/frontend-env', root / 'portal-public', root / 'portal-public/portal-manifest'):
        if not directory.exists():
            directory.mkdir(mode=0o700)
        metadata = directory.lstat()
        if directory.is_symlink() or not directory.is_dir() or metadata.st_uid != os.getuid() or metadata.st_mode & 0o777 != 0o700:
            sys.exit(3)
    outputs, profiles, sources, children = {}, {}, {}, {}
    host_contract = Path(manifest['artifacts']['portal-web']['path']).parent / 'src/manifest/loader.ts'
    host_source = host_contract.read_text(encoding='utf-8')
    host_versions = re.findall(r"export const PORTAL_HOST_VERSION = '([0-9]+\.[0-9]+\.[0-9]+)'", host_source)
    if len(host_versions) != 1:
        sys.exit(4)
    host_version = host_versions[0]
    sources[str(host_contract)] = task_artifact_sha256(host_contract)
    sandbox_source = host_contract.parent.parent.parent / 'public/wujie-empty.html'
    sources[str(sandbox_source)] = task_artifact_sha256(sandbox_source)
    sandbox_bytes = sandbox_source.read_bytes()
    origin = manifest['publicOrigin']
    for role, (alias, mount, platform, landing) in roles.items():
        process, artifact = declared[role], manifest['artifacts'][role]
        package = Path(artifact['path'])
        if (process['kind'] != 'NODE' or process['artifactRole'] != role or package.name != 'package.json'
                or not package.is_relative_to(repository) or task_artifact_sha256(package) != artifact['sha256']):
            sys.exit(3)
        project = package.parent
        package_bytes = package.read_bytes()
        if len(package_bytes) > 128 * 1024 or hashlib.sha256(package_bytes).hexdigest() != artifact['sha256']:
            sys.exit(3)
        metadata = decode_task_json(package_bytes)
        version = metadata.get('version')
        if not isinstance(version, str) or not re.fullmatch(r'[0-9]+\.[0-9]+\.[0-9]+(?:-[A-Za-z0-9.-]+)?', version):
            sys.exit(4)
        shared = project / 'node_modules/@egon-cola/xingyuan-admin-web-shared/package.json'
        shared_sha = task_artifact_sha256(shared)
        installed = decode_task_json(shared.read_bytes())
        if installed.get('name') != '@egon-cola/admin-web-shared' or installed.get('version') != '0.2.5':
            sys.exit(4)
        source_configuration = project / 'vite.config.ts'
        for dependency in (package, shared, source_configuration, project / 'node_modules/vite/bin/vite.js', project / 'node_modules/vite/dist/node/index.js'):
            sources[str(dependency)] = task_artifact_sha256(dependency)
        if sources[str(shared)] != shared_sha:
            sys.exit(3)
        binding = registry.get('aliases', {}).get(alias)
        if not isinstance(binding, dict) or not isinstance(binding.get('issuerRegistrationIds'), dict) or len(binding['issuerRegistrationIds']) != 2:
            sys.exit(3)
        for tenant in ('default', 'tenant-b'):
            issuer = origin + '/tenants/' + str(registry.get('tenantIds', {}).get(tenant))
            registration = binding['issuerRegistrationIds'].get(issuer)
            clients = [value for value in registry.get('clients', []) if value.get('registrationId') == registration and value.get('issuer') == issuer]
            if (not isinstance(registration, str) or len(clients) != 1 or clients[0].get('clientType') != 'CONFIDENTIAL'
                    or clients[0].get('purpose') != 'USER' or clients[0].get('status') != 'ACTIVE' or clients[0].get('alias') != alias):
                sys.exit(3)
        if binding.get('defaultRegistrationId') != binding['issuerRegistrationIds'].get(origin + '/tenants/' + str(registry['tenantIds']['default'])):
            sys.exit(3)
        environment = {'VITE_BASE_PATH': mount, 'VITE_YUHENG_ORIGIN': origin, 'VITE_OAUTH_REGISTRATION_ID': alias}
        if role == 'jianshen-web':
            environment['VITE_TIANQUAN_JIANSHEN_API_BASE'] = origin
        if role == 'portal-web':
            environment['VITE_PORTAL_ENV'] = 'isolated'
            for name, key in (('TIANQUAN_SHOUBING', 'tianquan-shoubing'), ('TIANQUAN_JIANSHEN', 'tianquan-jianshen'), ('YUHENG', 'yuheng'), ('TIANSHU', 'tianshu')):
                environment['VITE_' + name + '_ADMIN_WEB_URL'] = origin + '/apps/' + key
        else:
            children[platform] = {'key': platform, 'displayName': {'tianquan-shoubing': '身份与安全', 'tianquan-jianshen': '权限治理', 'yuheng': 'API 网关', 'tianshu': '配置中心'}[platform], 'url': origin + mount,
                                  'standaloneUrl': origin + mount.rstrip('/') + landing, 'version': version, 'contractVersion': 'platform-1',
                                  'compatibleHostRange': '>=' + host_version + ' <' + str(int(host_version.split('.')[0]) + 1) + '.0.0', 'requiredCapabilities': []}
        output = root / 'profiles' / (role + '.vite.config.mjs')
        content = render_frontend_vite_configuration(project, root, process['port'], mount, environment, manifest['secretFiles'],
                                                     root / 'portal-public' if role == 'portal-web' else None)
        outputs[output] = content
        profiles[role] = {'path': str(output), 'sha256': hashlib.sha256(content).hexdigest()}
    for path, checksum in sources.items():
        if task_artifact_sha256(path) != checksum:
            sys.exit(3)
    for path, content in outputs.items():
        publish_private_file(path, root, content)
    public_manifest = root / 'portal-public/portal-manifest/isolated.json'
    public_bytes = (json.dumps(children, ensure_ascii=True, sort_keys=True, separators=(',', ':')) + '\n').encode('utf-8')
    publish_private_file(public_manifest, root, public_bytes)
    publish_private_file(root / 'portal-public/wujie-empty.html', root, sandbox_bytes)
    receipt = {'schemaVersion': 1, 'runId': manifest['runId'], 'manifestSha256': digest,
               'registrySha256': hashlib.sha256(registry_bytes).hexdigest(), 'profiles': profiles, 'sourceFiles': sources,
               'portalManifest': {'path': str(public_manifest), 'sha256': hashlib.sha256(public_bytes).hexdigest()}}
    publish_private_file(root / 'evidence/frontend-profiles-ready.json', root,
                         (json.dumps(receipt, sort_keys=True, separators=(',', ':')) + '\n').encode('utf-8'))
    return receipt


def render_frontend_vite_configuration(project, root, port, base_path, environment, secret_files, public_directory=None):
    """Reuse the installed Vite loader and original plugins with a task-only environment and HTTPS listener."""
    import json
    import re
    import sys
    from pathlib import Path

    project, root = Path(project), Path(root)
    if (not project.is_absolute() or not root.is_absolute() or type(port) is not int or not 1 <= port <= 65535
            or not isinstance(base_path, str) or not re.fullmatch(r'/(?:[a-z0-9-]+/)*', base_path)
            or not isinstance(environment, dict) or environment.get('VITE_BASE_PATH') != base_path
            or any(not re.fullmatch(r'VITE_[A-Z0-9_]+', key) or not isinstance(value, str)
                   or any(ord(character) < 32 for character in value) for key, value in environment.items())):
        sys.exit(3)
    quote = lambda value: json.dumps(value, ensure_ascii=True)
    api = (project / 'node_modules/vite/dist/node/index.js').as_uri()
    configuration = project / 'vite.config.ts'
    isolated_environment = root / 'profiles/frontend-env'
    lines = [
        "import {readFileSync} from 'node:fs'",
        "import {loadConfigFromFile, mergeConfig} from " + quote(api),
        "const project = " + quote(str(project)),
        "const envDir = " + quote(str(isolated_environment)),
        "Object.assign(process.env, " + quote(environment) + ")",
        "const previousDirectory = process.cwd()",
        "let loaded",
        "try {",
        "  process.chdir(envDir)",
        "  loaded = await loadConfigFromFile({command:'serve', mode:'isolated'}, " + quote(str(configuration)) + ", project)",
        "} finally { process.chdir(previousDirectory) }",
        "if (!loaded) throw new Error('XINGYUAN_INPUT_UNAVAILABLE')",
        "const config = mergeConfig(loaded.config, {",
        "  root: project, envDir, base: " + quote(base_path) + ",",
        "  server: {host:'127.0.0.1', port:" + str(port) + ", strictPort:true, hmr:false,",
        "    https: {cert:readFileSync(" + quote(secret_files['tls-server-cert']) + "), key:readFileSync(" + quote(secret_files['tls-server-key']) + ")}},",
        "})",
        "config.server.proxy = {}",
    ]
    if public_directory is not None:
        lines.append("config.publicDir = " + quote(str(public_directory)))
    lines.extend([
        "const aliases = config.resolve?.alias ?? {}",
        "const sharedPackages = ['@egon-cola/admin-web-shared','@egon-cola/xingyuan-admin-web-shared']",
        "for (const entry of Array.isArray(aliases) ? aliases : Object.entries(aliases).map(([find,replacement]) => ({find,replacement}))) {",
        "  if (sharedPackages.some(name => typeof entry.find === 'string' ? name === entry.find || name.startsWith(entry.find + '/') : entry.find.test(name))) {",
        "    throw new Error('XINGYUAN_SOURCE_ALIAS_FORBIDDEN')",
        "  }",
        "}",
        "export default config",
    ])
    return ('\n'.join(lines) + '\n').encode('utf-8')


def task_revocation_fault_contents(manifest, digest, enabled):
    """The only injectable HTTP fault is the three actual task AS revocation paths."""
    import sys
    from pathlib import Path

    if type(enabled) is not bool:
        sys.exit(2)
    if not enabled:
        return b'default 0;\n'
    root = Path(manifest['runtimeRoot'])
    registry = decode_task_json(read_task_file(root / 'secrets/runtime-registry.json', root / 'secrets', 2 * 1024 * 1024))
    if (not isinstance(registry, dict) or type(registry.get('schemaVersion')) is not int or registry['schemaVersion'] != 1
            or registry.get('runId') != manifest['runId'] or digest is None or registry.get('manifestSha256') != digest
            or not isinstance(registry.get('tenantIds'), dict) or set(registry['tenantIds']) != {'default', 'tenant-b'}):
        sys.exit(3)
    import re
    ids = [registry['tenantIds'][realm] for realm in ('default', 'tenant-b')]
    if (any(not isinstance(value, str) or not re.fullmatch(r'[1-9][0-9]{0,18}', value) or int(value) > 9223372036854775807 for value in ids)
            or len(set(ids)) != 2 or not isinstance(registry.get('realms'), dict)
            or any(not isinstance(registry['realms'].get(realm),dict) for realm in ('platform','default','tenant-b'))
            or registry['realms'].get('platform', {}).get('issuer') != manifest['publicOrigin'] + '/platform'
            or any(registry['realms'].get(realm, {}).get('issuer') != manifest['publicOrigin'] + '/tenants/' + registry['tenantIds'][realm]
                   for realm in ('default', 'tenant-b'))):
        sys.exit(3)
    return ('default 0;\n/platform/oauth2/revoke 1;\n' + ''.join('/tenants/' + tenant + '/oauth2/revoke 1;\n' for tenant in ids)).encode('ascii')


def validate_task_nginx_configuration(manifest, digest=None):
    """Confine the frozen standalone proxy to this task before nginx or any bootstrap is invoked."""
    import re
    import shlex
    import sys
    from pathlib import Path

    root = Path(manifest['runtimeRoot'])
    reference = manifest['artifacts'].get('nginx')
    configuration = root / 'nginx/nginx.conf'
    if (not isinstance(reference, dict) or reference.get('path') != str(configuration)
            or task_artifact_sha256(configuration) != reference.get('sha256')):
        sys.exit(3)
    try:
        lexer = shlex.shlex(read_task_file(configuration, root, 128 * 1024).decode('utf-8'), posix=True, punctuation_chars='{};')
        lexer.whitespace_split = True
        lexer.commenters = '#'
        directives, current, depth = [], [], 0
        contexts, directive_contexts, blocks = [], [], []
        for token in lexer:
            if token and set(token) <= set('{};'):
                for delimiter in token:
                    if delimiter == ';':
                        if not current:
                            sys.exit(3)
                        directives.append(current)
                        directive_contexts.append(tuple(contexts))
                        current = []
                    elif delimiter == '{':
                        if not current:
                            sys.exit(3)
                        depth += 1
                        blocks.append((tuple(contexts), tuple(current)))
                        contexts.append(tuple(current))
                        current = []
                    else:
                        if current or depth <= 0:
                            sys.exit(3)
                        depth -= 1
                        contexts.pop()
            else:
                current.append(token)
        if current or depth:
            sys.exit(3)
    except (UnicodeError, ValueError):
        sys.exit(3)
    grouped = {}
    for directive in directives:
        grouped.setdefault(directive[0], []).append(directive[1:])
    if (any(name in grouped for name in ('load_module', 'perl_require', 'js_import'))
            or grouped.get('pid') != [[str(root / 'nginx/nginx.pid')]]
            or grouped.get('error_log') != [[str(root / 'evidence/nginx-error.log'), 'crit']]
            or grouped.get('access_log') != [[str(root / 'evidence/nginx-access.log'), 'isolated_safe']]):
        sys.exit(3)
    fault_map = root / 'nginx/revocation-fault.map'
    fault_header = ('map', '$uri', '$xingyuan_task_revocation_fault')
    fault_if = ('if', '($xingyuan_task_revocation_fault)')
    has_fault = 'include' in grouped
    if has_fault:
        if (grouped['include'] != [[str(fault_map)]]
                or [context for context, header in blocks if header == fault_header] != [(('http',),)]
                or [context for context, header in blocks if header == fault_if] != [(('http',), ('server',))]
                or [(context, directive) for context, directive in zip(directive_contexts, directives) if context and context[-1] == fault_header]
                   != [((('http',), fault_header), ['include', str(fault_map)])]
                or [(context, directive) for context, directive in zip(directive_contexts, directives) if context and context[-1] == fault_if]
                   != [((('http',), ('server',), fault_if), ['return', '503'])]):
            sys.exit(3)
        content = read_task_file(fault_map, root / 'nginx', 4096)
        if content != b'default 0;\n':
            if (not re.fullmatch(rb'default 0;\n/platform/oauth2/revoke 1;\n/tenants/[1-9][0-9]{0,18}/oauth2/revoke 1;\n/tenants/[1-9][0-9]{0,18}/oauth2/revoke 1;\n', content)
                    or content != task_revocation_fault_contents(manifest, digest, True)):
                sys.exit(3)
    if (any('$xingyuan_task_revocation_fault' in token and header not in (fault_header, fault_if) for _, header in blocks for token in header)
            or not has_fault and any('$xingyuan_task_revocation_fault' in token for _, header in blocks for token in header)):
        sys.exit(3)
    formats = grouped.get('log_format', [])
    if len(formats) != 1 or len(formats[0]) < 2 or formats[0][0] != 'isolated_safe':
        sys.exit(3)
    variables = set(re.findall(r'\$[A-Za-z_][A-Za-z0-9_]*|\$\{[^}]*\}', ' '.join(formats[0][1:])))
    if not {'$request_method', '$uri', '$status'} <= variables or not variables <= {
            '$request_method', '$uri', '$status', '$request_id', '$time_iso8601'}:
        sys.exit(3)
    expected_files = {'ssl_certificate': 'tls-server-cert', 'ssl_certificate_key': 'tls-server-key',
                      'proxy_ssl_trusted_certificate': 'tls-ca-cert', 'proxy_ssl_certificate': 'tls-server-cert',
                      'proxy_ssl_certificate_key': 'tls-server-key'}
    for directive, secret in expected_files.items():
        rows = grouped.get(directive, [])
        if directive in ('ssl_certificate', 'ssl_certificate_key', 'proxy_ssl_trusted_certificate') and not rows:
            sys.exit(3)
        if any(row != [manifest['secretFiles'].get(secret)] for row in rows):
            sys.exit(3)
    if any(not row or row[0] != '127.0.0.1:28443' or 'ssl' not in row for row in grouped.get('listen', [])):
        sys.exit(3)
    if not grouped.get('listen') or not grouped.get('proxy_pass') or not grouped.get('proxy_ssl_verify'):
        sys.exit(3)
    if any(row != ['on'] for row in grouped['proxy_ssl_verify']):
        sys.exit(3)
    from urllib.parse import urlsplit
    ports = {item['port'] for item in manifest['processes'] if item['kind'] in ('JAVA', 'NODE')}
    for row in grouped['proxy_pass']:
        try:
            target = urlsplit(row[0]) if len(row) == 1 else None
            if (target is None or target.scheme != 'https' or target.hostname != '127.0.0.1' or target.port not in ports
                    or target.username is not None or target.password is not None or target.query or target.fragment or '$' in row[0]):
                sys.exit(3)
        except ValueError:
            sys.exit(3)
    return has_fault


def set_task_revocation_fault(argv):
    import datetime
    import fcntl
    import hashlib
    import os
    import stat
    import subprocess
    import sys
    import tempfile
    import uuid
    from pathlib import Path

    if argv[3] not in ('on', 'off'):
        sys.exit(2)
    if os.environ.get('XINGYUAN_ISOLATED_INTEGRATION') != 'true':
        sys.exit(3)
    manifest_file, result_file = Path(argv[1]), Path(argv[2])
    manifest_bytes = read_task_file(manifest_file)
    manifest = decode_task_json(manifest_bytes)
    root = Path(manifest['runtimeRoot']); digest = hashlib.sha256(manifest_bytes).hexdigest(); enabled = argv[3] == 'on'
    if result_file.exists():
        sys.exit(5)
    if (not result_file.is_absolute() or result_file != Path(os.path.normpath(result_file)) or not result_file.is_relative_to(root / 'evidence')
            or any(path.is_symlink() for path in (result_file, *result_file.parents))):
        sys.exit(3)
    parent = result_file.parent.lstat()
    if not stat.S_ISDIR(parent.st_mode) or parent.st_uid != os.getuid() or stat.S_IMODE(parent.st_mode) != 0o700:
        sys.exit(3)
    if not validate_task_nginx_configuration(manifest, digest):
        sys.exit(4)
    record_file = root / 'evidence/processes.json'; record_bytes = read_task_file(record_file, root / 'evidence')
    record = decode_task_json(record_bytes); validate_owned_process_record(manifest, digest, record)
    owners = [value for value in record['processes'] if value['id'] == 'nginx']
    if len(owners) != 1 or observe_owned_process(manifest, owners[0]) != 'RUNNING':
        sys.exit(10)
    owner = owners[0]
    fault_file = root / 'nginx/revocation-fault.map'; before = read_task_file(fault_file, root / 'nginx', 4096)
    desired = task_revocation_fault_contents(manifest, digest, enabled)
    state_file = root / 'evidence/revocation-fault-state.json'
    old_state = read_task_file(state_file, root / 'evidence') if state_file.exists() else None
    if old_state is not None:
        previous = decode_task_json(old_state)
        if (not isinstance(previous, dict) or type(previous.get('schemaVersion')) is not int or previous['schemaVersion']!=1
                or previous.get('scope') != 'TASK_AS_REVOCATION_ENDPOINT_ONLY' or previous.get('runId') != manifest['runId']
                or previous.get('manifestSha256') != digest or previous.get('status') not in ('RUNNING', 'COMMITTED')
                or previous.get('pid')!=owner['pid'] or previous.get('startInstant')!=owner['startInstant'] or previous.get('commandSha256')!=owner['commandSha256']):
            sys.exit(3)
        if previous['status'] != 'COMMITTED' and enabled:
            sys.exit(9)
    lock_file = root / 'evidence/revocation-fault.lock'
    if lock_file.exists():
        read_task_file(lock_file, root / 'evidence', 1024)
    lock = os.open(lock_file, os.O_WRONLY | os.O_CREAT | os.O_NOFOLLOW, 0o600)
    try:
        fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
    except OSError:
        os.close(lock); sys.exit(5)
    started = False
    def stable():
        if read_task_file(manifest_file) != manifest_bytes or read_task_file(record_file, root / 'evidence') != record_bytes:
            sys.exit(3)
        if observe_owned_process(manifest, owner) != 'RUNNING':
            sys.exit(10)
    def result(status, error=None):
        publish_task_json(result_file, root, {'schemaVersion':1,'operationId':'CLI-007','runId':manifest['runId'],'manifestSha256':digest,
            'status':status,'completedAt':datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00','Z'),
            'targets':[{'targetKey':'nginx','identity':str(owner['pid']),'version':owner['startInstant'],'state':'RUNNING' if error is None else 'UNREADY'}],
            'counts':{'databaseCount':0,'tenantCount':0,'clientCount':0,'catalogCount':0,'liveCaseCount':0},
            'steps':[{'stepId':'TASK_AS_REVOCATION_FAULT_' + argv[3].upper(),'state':status,'evidence':'evidence/revocation-fault-state.json'}], 'error':error})
    try:
        stable()
        state={'schemaVersion':1,'runId':manifest['runId'],'manifestSha256':digest,'scope':'TASK_AS_REVOCATION_ENDPOINT_ONLY','nonce':str(uuid.uuid4()),
               'mode':argv[3],'status':'RUNNING','pid':owner['pid'],'startInstant':owner['startInstant'],'commandSha256':owner['commandSha256'],
               'beforeSha256':hashlib.sha256(before).hexdigest(),'afterSha256':hashlib.sha256(desired).hexdigest(),
               'recordedAt':datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00','Z')}
        publish_task_json(state_file, root, state, old_state); started = True
        running = read_task_file(state_file, root / 'evidence')
        if before != desired:
            directory = fault_file.parent.lstat()
            descriptor, temporary = tempfile.mkstemp(prefix='.revocation-fault-',suffix='.tmp',dir=fault_file.parent)
            try:
                with os.fdopen(descriptor,'wb') as stream:
                    stream.write(desired); stream.flush(); os.fsync(stream.fileno())
                stable()
                current = fault_file.parent.lstat()
                if (directory.st_dev,directory.st_ino)!=(current.st_dev,current.st_ino) or read_task_file(fault_file,root / 'nginx',4096)!=before:
                    sys.exit(3)
                os.replace(temporary,fault_file)
                parent_fd=os.open(fault_file.parent,os.O_RDONLY | os.O_NOFOLLOW)
                try: os.fsync(parent_fd)
                finally: os.close(parent_fd)
            finally:
                if os.path.exists(temporary): os.unlink(temporary)
            stable()
            signal=subprocess.run(['kill','-HUP',str(owner['pid'])],stdin=subprocess.DEVNULL,stdout=subprocess.PIPE,stderr=subprocess.PIPE,
                                  timeout=min(5,manifest['deadlines']['httpSeconds']))
            if signal.returncode:
                sys.exit(9)
        observed=probe_task_revocation_fault(manifest,digest,enabled)
        stable()
        if read_task_file(fault_file,root / 'nginx',4096)!=desired:
            sys.exit(9)
        state.update(status='COMMITTED',observed=observed,recordedAt=datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00','Z'))
        publish_task_json(state_file,root,state,running)
        result('COMMITTED')
    except (OSError, subprocess.TimeoutExpired, SystemExit) as failure:
        if started:
            try:
                if not result_file.exists():
                    result('UNKNOWN_COMMIT',{'code':'XINGYUAN_COMMIT_UNKNOWN','message':'Task revocation fault outcome is unconfirmed','retryable':False,'stage':'TASK_AS_REVOCATION_FAULT'})
            finally: sys.exit(9)
        if isinstance(failure,SystemExit): raise
        sys.exit(6)
    finally:
        os.close(lock)


def probe_task_revocation_fault(manifest, digest, enabled):
    import ssl
    import sys
    import time
    import urllib.error
    import urllib.request
    from pathlib import Path

    root = Path(manifest['runtimeRoot']); ca=Path(manifest['secretFiles']['tls-ca-cert'])
    read_task_file(ca,root / 'secrets',128*1024)
    try: context=ssl.create_default_context(cafile=str(ca))
    except (OSError,ssl.SSLError): sys.exit(6)
    class NoRedirect(urllib.request.HTTPRedirectHandler):
        def redirect_request(self, request, response, code, message, headers, destination): return None
    opener=urllib.request.build_opener(urllib.request.HTTPSHandler(context=context),NoRedirect())
    paths=[line.split(' ',1)[0] for line in task_revocation_fault_contents(manifest,digest,True).decode('ascii').splitlines()[1:]]
    deadline=time.monotonic()+manifest['deadlines']['httpSeconds']
    def request(path,method):
        remaining=deadline-time.monotonic()
        if remaining<=0: sys.exit(6)
        try:
            try: response=opener.open(urllib.request.Request(manifest['publicOrigin']+path,data=b'' if method=='POST' else None,method=method),timeout=remaining)
            except urllib.error.HTTPError as error: response=error
            try:
                body=response.read(64*1024+1)
                if len(body)>64*1024 or response.headers.get('Location') is not None: sys.exit(8)
                if time.monotonic()>deadline: sys.exit(6)
                return response.code,body
            finally: response.close()
        except (urllib.error.URLError,OSError,TimeoutError): sys.exit(6)
    statuses=[request(path,'POST')[0] for path in paths]
    if any(code!=503 if enabled else code not in (400,401,403,405) for code in statuses): sys.exit(8)
    code,body=request('/platform/.well-known/openid-configuration','GET')
    metadata=decode_task_json(body)
    if code!=200 or not isinstance(metadata,dict) or metadata.get('issuer')!=manifest['publicOrigin']+'/platform': sys.exit(8)
    return {'issuerCount':3,'revocationStatus':503 if enabled else statuses[0],'metadataStatus':200}


def runtime_profile_exclusions(manifest):
    """Only the accepted, SHA-frozen Agent configuration gap may exclude a runtime process."""
    import hashlib
    import sys
    from pathlib import Path

    root = Path(manifest['runtimeRoot'])
    path = root / 'policies/agent-runtime-deferred.json'
    candidates = [value for value in manifest.get('policyFiles', [])
                  if isinstance(value, dict) and Path(value.get('path', '')).name == path.name]
    if not candidates:
        if path.exists() or path.is_symlink():
            sys.exit(3)
        return {}
    if len(candidates) != 1 or candidates[0].get('path') != str(path):
        sys.exit(3)
    contents = read_task_file(path, root / 'policies')
    checksum = hashlib.sha256(contents).hexdigest()
    expected = {'schemaVersion': 1, 'runId': manifest['runId'], 'role': 'native-agent', 'status': 'UNVERIFIED',
                'reasonCode': 'AGENT_RUNTIME_CONFIGURATION_NOT_PROVIDED', 'acceptedOn': '2026-10-15'}
    policy = decode_task_json(contents)
    if (not isinstance(policy, dict) or set(policy) != set(expected) or policy != expected
            or type(policy.get('schemaVersion')) is not int or candidates[0].get('sha256') != checksum):
        sys.exit(3)
    return {'native-agent': checksum}


def runtime_probe_fixture_entries(contents):
    """Accept only the reviewed initializer, its nested classes and one generated mapper."""
    import hashlib
    import io
    import re
    import sys
    import zipfile

    package = 'top/egon/cola/platform/tianquan/shoubing/admin/integration/'
    initializer = 'XingyuanRuntimeProbeInitializer'
    mapper = 'XingyuanRuntimeProbeEdgeBindingConverterImpl.class'
    factories = (b'org.springframework.context.ApplicationContextInitializer='
                 b'top.egon.cola.platform.tianquan.shoubing.admin.integration.XingyuanRuntimeProbeInitializer\n')
    expected = {package + initializer + '.class', package + mapper, 'META-INF/spring.factories'}
    try:
        with zipfile.ZipFile(io.BytesIO(contents)) as fixture:
            names = fixture.namelist()
            if (len(names) != len(set(names)) or not expected <= set(names) or len(names) > 64
                    or sum(item.file_size for item in fixture.infolist()) > 2 * 1024 * 1024):
                sys.exit(3)
            entries = {}
            for item in fixture.infolist():
                name = item.filename
                if (item.is_dir() or item.file_size > 2 * 1024 * 1024 or item.flag_bits & 1
                        or name not in expected and not re.fullmatch(re.escape(package + initializer) + r'\$[A-Za-z0-9_$]+\.class', name)
                        or 'Test' in name):
                    sys.exit(3)
                value = fixture.read(item)
                if name == 'META-INF/spring.factories':
                    if value != factories:
                        sys.exit(3)
                elif len(value) < 8 or value[:4] != b'\xca\xfe\xba\xbe':
                    sys.exit(3)
                entries[name] = hashlib.sha256(value).hexdigest()
            return entries
    except (OSError, ValueError, zipfile.BadZipFile, RuntimeError):
        sys.exit(3)


def read_task_logout_queue(manifest, digest):
    """Read only counts from the fixed, bound task Edge queue; never read an encrypted intent payload."""
    import hashlib
    import subprocess
    import sys
    from pathlib import Path

    root = Path(manifest['runtimeRoot'])
    profile = root / 'profiles/edge.properties'
    profile_bytes = read_task_file(profile, root / 'profiles', 2 * 1024 * 1024)
    profile_sha = hashlib.sha256(profile_bytes).hexdigest()
    ready = decode_task_json(read_task_file(root / 'evidence/profiles-ready.json', root / 'evidence'))
    if (not isinstance(ready, dict) or type(ready.get('schemaVersion')) is not int or ready['schemaVersion'] != 1
            or ready.get('runId') != manifest['runId'] or ready.get('manifestSha256') != digest
            or not isinstance(ready.get('profiles'), dict) or ready['profiles'].get('edge') != {'path': str(profile), 'sha256': profile_sha}):
        sys.exit(3)
    namespace = 'xingyuan-it:' + manifest['runId'] + ':bff:'
    stream, group = namespace + 'logout:v1', namespace + 'revocation:v1'
    # The original materializer binds/validates EdgeOAuthProperties and emits canonical JDK Properties bytes.
    # Accept only those exact two encoded entries; this observer does not implement a second Properties parser.
    lines = profile_bytes.splitlines()
    for key, value in (('egon.yuheng.edge.logout.stream', stream), ('egon.yuheng.edge.logout.consumer-group', group)):
        encoded = key.encode('ascii') + b'=' + value.replace(':', '\\:').encode('ascii')
        if lines.count(encoded) != 1 or sum(line.startswith(key.encode('ascii') + b'=') for line in lines) != 1:
            sys.exit(3)
    record = decode_task_json(read_task_file(root / 'evidence/processes.json', root / 'evidence'))
    validate_owned_process_record(manifest, digest, record)
    owners = [value for value in record['processes'] if value['id'] == 'edge']
    if len(owners) != 1 or observe_owned_process(manifest, owners[0]) != 'RUNNING':
        sys.exit(10)
    environment = isolated_runtime_environment()
    environment['REDISCLI_AUTH'] = task_credential(Path(manifest['redis']['credentialFile']))
    def query(*arguments):
        try:
            result = subprocess.run(['redis-cli', '-h', '127.0.0.1', '-p', '6379', '-n', str(manifest['redis']['database']),
                                     '-e', '--no-auth-warning', '--json', *arguments], stdin=subprocess.DEVNULL,
                                    stdout=subprocess.PIPE, stderr=subprocess.PIPE, env=environment,
                                    timeout=manifest['deadlines']['httpSeconds'])
        except (OSError, subprocess.TimeoutExpired):
            sys.exit(6)
        if result.returncode or result.stderr.strip() or len(result.stdout) > 1024:
            sys.exit(6)
        return decode_task_json(result.stdout)
    marker = 'xingyuan-it:' + manifest['runId'] + ':' + digest
    if query('GET', 'xingyuan-it:owner') != marker:
        sys.exit(3)
    script = "local p=redis.call('XPENDING',KEYS[1],ARGV[1]); return {redis.call('XLEN',KEYS[1]),p[1]}"
    observed = query('EVAL_RO', script, '1', stream, group)
    if (not isinstance(observed, list) or len(observed) != 2 or any(type(value) is not int or not 0 <= value <= 100000 for value in observed)
            or observed[1] > observed[0]):
        sys.exit(6)
    if query('GET', 'xingyuan-it:owner') != marker or hashlib.sha256(read_task_file(profile, root / 'profiles', 2 * 1024 * 1024)).hexdigest() != profile_sha:
        sys.exit(3)
    if observe_owned_process(manifest, owners[0]) != 'RUNNING':
        sys.exit(10)
    return {'queuedCount': observed[0], 'pendingCount': observed[1], 'profileSha256': profile_sha,
            'streamReference': hashlib.sha256(stream.encode('ascii')).hexdigest(), 'groupReference': hashlib.sha256(group.encode('ascii')).hexdigest()}


def observe_task_logout_queue(argv):
    import datetime
    import hashlib
    import os
    import stat
    import sys
    from pathlib import Path

    manifest_file, result_file = Path(argv[1]), Path(argv[2])
    raw = read_task_file(manifest_file); manifest = decode_task_json(raw)
    root = Path(manifest['runtimeRoot']); digest = hashlib.sha256(raw).hexdigest()
    if (not result_file.is_absolute() or result_file != Path(os.path.normpath(result_file)) or not result_file.is_relative_to(root / 'evidence')
            or any(path.is_symlink() for path in (result_file, *result_file.parents))):
        sys.exit(3)
    if result_file.exists():
        sys.exit(5)
    parent = result_file.parent.lstat()
    if not stat.S_ISDIR(parent.st_mode) or parent.st_uid != os.getuid() or stat.S_IMODE(parent.st_mode) != 0o700:
        sys.exit(3)
    counts = read_task_logout_queue(manifest, digest)
    if read_task_file(manifest_file) != raw:
        sys.exit(3)
    publish_task_json(result_file, root, {'schemaVersion': 1, 'runId': manifest['runId'], 'manifestSha256': digest,
                      'scope': 'TASK_EDGE_LOGOUT_QUEUE_COUNTS_ONLY', 'status': 'OBSERVED',
                      'recordedAt': datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00', 'Z'), **counts})


def package_runtime_probe_fixture(repository, runtime_root):
    """Assemble the compiled task fixture before freezing the existing manifest policy list."""
    import hashlib
    import io
    import json
    import os
    import re
    import stat
    import sys
    import uuid
    import zipfile
    from pathlib import Path

    repository, root = Path(repository), Path(runtime_root)
    if (not repository.is_absolute() or not root.is_absolute() or repository != repository.resolve()
            or root.parent != repository / 'target' or not re.fullmatch(r'xingyuan-integration-[0-9a-f-]{36}', root.name)
            or any(path.is_symlink() for path in (root, *root.parents))):
        sys.exit(3)
    try:
        if str(uuid.UUID(root.name.removeprefix('xingyuan-integration-'))) != root.name.removeprefix('xingyuan-integration-'):
            sys.exit(3)
    except ValueError:
        sys.exit(3)
    package = Path('top/egon/cola/platform/tianquan/shoubing/admin/integration')
    module = repository / 'egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin'
    source = module / 'src/test/java' / package / 'XingyuanRuntimeProbeInitializer.java'
    classes = module / 'target/test-classes' / package
    names = ['XingyuanRuntimeProbeInitializer.class', 'XingyuanRuntimeProbeEdgeBindingConverterImpl.class']
    names += sorted(path.name for path in classes.glob('XingyuanRuntimeProbeInitializer$*.class'))
    if not source.is_file() or any(not (classes / name).is_file() for name in names):
        sys.exit(4)
    source_sha = task_artifact_sha256(source)
    proof_file = root / 'evidence/runtime-probe-fixture-build.json'
    if proof_file.exists():
        previous = decode_task_json(read_task_file(proof_file, root / 'evidence'))
        if not isinstance(previous, dict) or previous.get('source') != {'path': str(source), 'sha256': source_sha}:
            sys.exit(5)
    source_modified = source.stat().st_mtime_ns
    if any((classes / name).stat().st_mtime_ns < source_modified for name in names):
        sys.exit(4)
    entries = {}
    for name in names:
        path = classes / name
        checksum = task_artifact_sha256(path)
        content = path.read_bytes()
        if len(content) > 2 * 1024 * 1024 or hashlib.sha256(content).hexdigest() != checksum:
            sys.exit(3)
        entries[(package / name).as_posix()] = content
    entries['META-INF/spring.factories'] = (b'org.springframework.context.ApplicationContextInitializer='
            b'top.egon.cola.platform.tianquan.shoubing.admin.integration.XingyuanRuntimeProbeInitializer\n')
    encoded = io.BytesIO()
    with zipfile.ZipFile(encoded, 'w', compression=zipfile.ZIP_STORED) as jar:
        for name, content in sorted(entries.items()):
            item = zipfile.ZipInfo(name, (1980, 1, 1, 0, 0, 0)); item.external_attr = 0o100600 << 16
            jar.writestr(item, content)
    contents = encoded.getvalue()
    if len(contents) > 2 * 1024 * 1024:
        sys.exit(2)
    entry_hashes = runtime_probe_fixture_entries(contents)
    proof = {'schemaVersion': 1, 'runId': root.name.removeprefix('xingyuan-integration-'), 'scope': 'TASK_RUNTIME_PROBE_BUILD_ONLY',
             'source': {'path': str(source), 'sha256': source_sha},
             'fixtureSha256': hashlib.sha256(contents).hexdigest(), 'entries': entry_hashes}
    for directory in (root, root / 'artifacts', root / 'evidence'):
        if not directory.exists():
            if directory == root:
                sys.exit(4)
            directory.mkdir(mode=0o700)
        metadata = directory.lstat()
        if not stat.S_ISDIR(metadata.st_mode) or metadata.st_uid != os.getuid() or stat.S_IMODE(metadata.st_mode) != 0o700:
            sys.exit(3)
    if source_sha != task_artifact_sha256(source) or source.stat().st_mtime_ns != source_modified:
        sys.exit(3)
    publish_private_file(root / 'artifacts/task-runtime-probe.jar', root, contents)
    publish_private_file(root / 'evidence/runtime-probe-fixture-build.json', root,
                         (json.dumps(proof, sort_keys=True, separators=(',', ':')) + '\n').encode())
    return proof


def runtime_probe_configuration(manifest, role, manifest_file=None, digest=None):
    """Resolve the sole permitted Boot launcher variation without accepting an arbitrary main or classpath."""
    import hashlib
    import re
    import sys
    import zipfile
    from pathlib import Path

    if role not in ('edge', 'native-light', 'native-web', 'native-service', 'shoubing', 'jianshen', 'tianshu', 'yuheng', 'llm'):
        return None
    root = Path(manifest['runtimeRoot']); fixture = root / 'artifacts/task-runtime-probe.jar'
    references = [item for item in manifest.get('policyFiles', [])
                  if isinstance(item, dict) and Path(item.get('path', '')).name == fixture.name]
    if not references:
        return None
    if (len(references) != 1 or references[0].get('path') != str(fixture)
            or not re.fullmatch(r'[0-9a-f]{64}', str(references[0].get('sha256', '')))):
        sys.exit(3)
    contents = read_task_file(fixture, root / 'artifacts', 2 * 1024 * 1024)
    checksum = hashlib.sha256(contents).hexdigest()
    if checksum != references[0]['sha256']:
        sys.exit(3)
    entries = runtime_probe_fixture_entries(contents)
    proof = decode_task_json(read_task_file(root / 'evidence/runtime-probe-fixture-build.json', root / 'evidence'))
    source = str(Path(manifest['repoRoot']) / 'egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/integration/XingyuanRuntimeProbeInitializer.java')
    if (not isinstance(proof, dict) or set(proof) != {'schemaVersion', 'runId', 'scope', 'source', 'fixtureSha256', 'entries'}
            or type(proof.get('schemaVersion')) is not int or proof['schemaVersion'] != 1 or proof.get('runId') != manifest['runId']
            or proof.get('scope') != 'TASK_RUNTIME_PROBE_BUILD_ONLY' or proof.get('fixtureSha256') != checksum or proof.get('entries') != entries
            or not isinstance(proof.get('source'), dict) or set(proof['source']) != {'path', 'sha256'}
            or proof['source']['path'] != source or not isinstance(proof['source']['sha256'], str)
            or not re.fullmatch(r'[0-9a-f]{64}', proof['source']['sha256'])
            or task_artifact_sha256(source) != proof['source']['sha256']):
        sys.exit(3)
    if manifest_file is None:
        sys.exit(4)
    manifest_file = Path(manifest_file)
    original_bytes = read_task_file(manifest_file, root)
    original = decode_task_json(original_bytes)
    if digest is not None and hashlib.sha256(original_bytes).hexdigest() != digest:
        sys.exit(3)
    if (not isinstance(original, dict) or set(original) != set(manifest)
            or any(original[key] != value for key, value in manifest.items() if key != 'deadlines')
            or not isinstance(original.get('deadlines'), dict) or set(original['deadlines']) != set(manifest['deadlines'])
            or any(type(value) is not int or type(original['deadlines'][key]) is not int or not 0 < value <= original['deadlines'][key]
                   for key, value in manifest['deadlines'].items())):
        sys.exit(3)
    reference = manifest['artifacts'][role]
    if task_artifact_sha256(reference['path']) != reference['sha256']:
        sys.exit(3)
    try:
        with zipfile.ZipFile(reference['path']) as jar:
            names = jar.namelist()
            if (names.count('org/springframework/boot/loader/launch/PropertiesLauncher.class') != 1
                    or names.count('META-INF/MANIFEST.MF') != 1):
                sys.exit(3)
            raw = jar.read('META-INF/MANIFEST.MF')
            if len(raw) > 128 * 1024:
                sys.exit(2)
            fields = {}; previous = None
            for line in raw.decode('utf-8').replace('\r\n', '\n').split('\n'):
                if not line:
                    break
                if line.startswith(' ') and previous:
                    fields[previous] += line[1:]; continue
                key, separator, value = line.partition(': ')
                if not separator or key in fields:
                    sys.exit(3)
                fields[key] = value; previous = key
            main = fields.get('Start-Class', '')
            if (not re.fullmatch(r'[A-Za-z_$][A-Za-z0-9_$]*(?:\.[A-Za-z_$][A-Za-z0-9_$]*)+', main)
                    or names.count('BOOT-INF/classes/' + main.replace('.', '/') + '.class') != 1):
                sys.exit(3)
    except (OSError, ValueError, UnicodeError, zipfile.BadZipFile, KeyError):
        sys.exit(3)
    return {'fixture': str(fixture), 'sha256': checksum, 'manifestFile': str(manifest_file), 'startClass': main}


def runtime_probe_java_arguments(manifest, role, configuration):
    from pathlib import Path

    root = Path(manifest['runtimeRoot'])
    return ['-Dxingyuan.isolated.run-id=' + manifest['runId'], '@' + str(root / 'profiles' / (role + '.jvm.args')),
            '-Dloader.path=' + configuration['fixture'], '-Dxingyuan.isolated.runtime-probe.enabled=true',
            '-Dxingyuan.isolated.runtime-probe.manifest-file=' + configuration['manifestFile'],
            '-Dxingyuan.isolated.runtime-probe.role=' + role,
            '-Dxingyuan.isolated.runtime-probe.fixture-sha256=' + configuration['sha256'],
            '-cp', manifest['artifacts'][role]['path'], 'org.springframework.boot.loader.launch.PropertiesLauncher',
            '--spring.config.location=' + (root / 'profiles' / (role + '.properties')).as_uri()]


def launch_owned_runtime(manifest, digest, role, truststore, manifest_file=None):
    import hashlib
    import os
    import shutil
    import subprocess
    import sys
    import uuid
    from pathlib import Path

    root = Path(manifest['runtimeRoot'])
    if role in runtime_profile_exclusions(manifest):
        sys.exit(4)
    declared = next(value for value in manifest['processes'] if value['id'] == role)
    reference = manifest['artifacts'][declared['artifactRole']]
    if task_artifact_sha256(reference['path']) != reference['sha256']:
        sys.exit(3)
    if declared['kind'] == 'JAVA':
        java = shutil.which('java')
        if java is None:
            sys.exit(4)
        password = task_credential(Path(manifest['secretFiles']['tls-keystore-password']), root / 'secrets')
        def quote(value):
            return '"' + value.replace('\\', '\\\\').replace('"', '\\"').replace('\n', '\\n').replace('\r', '\\r').replace('\t', '\\t') + '"'
        options = ['-Xms64m', '-Xmx768m', '-XX:MaxDirectMemorySize=256m',
                   '-Duser.timezone=UTC', '-Djavax.net.ssl.trustStore=' + str(truststore),
                   '-Djavax.net.ssl.trustStoreType=PKCS12', '-Djavax.net.ssl.trustStorePassword=' + password]
        args_file = root / 'profiles' / (role + '.jvm.args')
        publish_private_file(args_file, root, ('\n'.join(quote(value) for value in options) + '\n').encode('utf-8'))
        profile = root / 'profiles' / (role + '.properties')
        proof = decode_task_json(read_task_file(root / 'evidence/profiles-ready.json', root / 'evidence'))
        binding = proof.get('profiles', {}).get(role)
        if (proof.get('runId') != manifest['runId'] or proof.get('manifestSha256') != digest or not isinstance(binding, dict)
                or binding.get('path') != str(profile) or hashlib.sha256(read_task_file(profile, root / 'profiles', 2 * 1024 * 1024)).hexdigest() != binding.get('sha256')):
            sys.exit(3)
        probe = runtime_probe_configuration(manifest, role, manifest_file, digest)
        arguments = ([java, *runtime_probe_java_arguments(manifest, role, probe)] if probe is not None else
                     [java, '-Dxingyuan.isolated.run-id=' + manifest['runId'], '@' + str(args_file), '-jar', reference['path'],
                      '--spring.config.location=' + profile.as_uri()])
    elif declared['kind'] == 'NODE':
        node = shutil.which('node')
        project = Path(reference['path']).parent
        proof = decode_task_json(read_task_file(root / 'evidence/frontend-profiles-ready.json', root / 'evidence'))
        binding = proof.get('profiles', {}).get(role)
        configuration = root / 'profiles' / (role + '.vite.config.mjs')
        if (node is None or not isinstance(binding, dict) or proof.get('runId') != manifest['runId']
                or proof.get('manifestSha256') != digest or binding.get('path') != str(configuration)
                or hashlib.sha256(read_task_file(configuration, root / 'profiles', 128 * 1024)).hexdigest() != binding.get('sha256')):
            sys.exit(3)
        for path, checksum in proof.get('sourceFiles', {}).items():
            if task_artifact_sha256(path) != checksum:
                sys.exit(3)
        arguments = [node, str(project / 'node_modules/vite/bin/vite.js'), str(project), '--config', str(configuration),
                     '--host', '127.0.0.1', '--port', str(declared['port']), '--strictPort', '--mode', 'isolated']
    elif declared['kind'] == 'NGINX':
        validate_task_nginx_configuration(manifest)
        nginx = shutil.which('nginx')
        if nginx is None or Path(reference['path']) != root / 'nginx/nginx.conf':
            sys.exit(4)
        arguments = [nginx, '-p', str(root / 'nginx'), '-c', reference['path'], '-g', 'daemon off;']
        try:
            tested = subprocess.run([nginx, '-t', '-p', str(root / 'nginx'), '-c', reference['path']], stdin=subprocess.DEVNULL,
                                    stdout=subprocess.PIPE, stderr=subprocess.PIPE, env=isolated_runtime_environment(), timeout=manifest['deadlines']['httpSeconds'])
        except (OSError, subprocess.TimeoutExpired):
            sys.exit(6)
        if tested.returncode:
            sys.exit(6)
    else:
        sys.exit(2)
    log = root / 'evidence' / (role + '-runtime-' + uuid.uuid4().hex + '.log')
    descriptor = os.open(log, os.O_WRONLY | os.O_CREAT | os.O_EXCL | os.O_NOFOLLOW, 0o600)
    try:
        with os.fdopen(descriptor, 'ab') as output:
            environment = isolated_runtime_environment()
            working_directory = manifest['repoRoot']
            if declared['kind'] == 'NODE':
                working_directory = str(project)
                environment['NODE_EXTRA_CA_CERTS'] = manifest['secretFiles']['tls-ca-cert']
            child = subprocess.Popen(arguments, cwd=working_directory, env=environment,
                                     stdin=subprocess.DEVNULL, stdout=output, stderr=subprocess.STDOUT, start_new_session=True)
    except OSError:
        sys.exit(6)
    return capture_owned_process(manifest, digest, role, child.pid)


def wait_owned_https(manifest, record, url, metadata_issuer=None, jwks=False, ready=False, html=False):
    import ssl
    import sys
    import time
    import urllib.error
    import urllib.request
    from pathlib import Path

    root = Path(manifest['runtimeRoot'])
    ca = Path(manifest['secretFiles']['tls-ca-cert'])
    read_task_file(ca, root / 'secrets', 128 * 1024)
    context = ssl.create_default_context(cafile=str(ca))
    from urllib.parse import urlsplit
    target = urlsplit(url)
    allowed_ports = {port for value in manifest['processes'] for port in (value['port'], runtime_management_port(value))}
    if target.scheme != 'https' or target.hostname != '127.0.0.1' or target.port not in allowed_ports:
        sys.exit(3)
    if target.port != 28443:
        certificate = Path(manifest['secretFiles']['tls-server-cert'])
        key = Path(manifest['secretFiles']['tls-server-key'])
        read_task_file(certificate, root / 'secrets', 128 * 1024)
        read_task_file(key, root / 'secrets', 128 * 1024)
        context.load_cert_chain(certfile=str(certificate), keyfile=str(key))
    class NoRedirect(urllib.request.HTTPRedirectHandler):
        def redirect_request(self, request, response, code, message, headers, target):
            return None
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}), urllib.request.HTTPSHandler(context=context), NoRedirect())
    deadline = time.monotonic() + manifest['deadlines']['startupSeconds']
    while time.monotonic() < deadline:
        if observe_owned_process(manifest, record) != 'RUNNING':
            sys.exit(6)
        try:
            with opener.open(url, timeout=manifest['deadlines']['httpSeconds']) as response:
                if response.geturl() != url:
                    sys.exit(3)
                if (ready or html or jwks or metadata_issuer is not None) and response.status != 200:
                    sys.exit(8)
                data = response.read(128 * 1024 + 1)
                if len(data) > 128 * 1024:
                    sys.exit(6)
                if html:
                    media_type = response.headers.get_content_type()
                    vite_client = (target.path.rstrip('/') + '/@vite/client').encode('utf-8')
                    if media_type != 'text/html' or b'<html' not in data.lower() or vite_client not in data:
                        sys.exit(8)
                    return
                if metadata_issuer is None and not jwks and not ready:
                    return
                document = decode_task_json(data)
                if ready:
                    if not isinstance(document, dict) or document.get('status') != 'UP':
                        time.sleep(min(1, max(0, deadline - time.monotonic())))
                        continue
                    return document
                if jwks:
                    import base64
                    import binascii
                    keys = document.get('keys')
                    if not isinstance(keys, list) or not 1 <= len(keys) <= 16:
                        sys.exit(8)
                    for key in keys:
                        if (not isinstance(key, dict) or key.get('kty') != 'RSA' or key.get('alg') != 'RS256' or key.get('use') != 'sig'
                                or not isinstance(key.get('kid'), str) or not key['kid'] or len(key['kid']) > 128
                                or not isinstance(key.get('n'), str) or len(key['n']) > 16384 or not isinstance(key.get('e'), str)
                                or any(name in key for name in ('d', 'p', 'q', 'dp', 'dq', 'qi', 'oth'))):
                            sys.exit(8)
                        try:
                            modulus = int.from_bytes(base64.urlsafe_b64decode(key['n'] + '=' * (-len(key['n']) % 4)), 'big')
                            exponent = int.from_bytes(base64.urlsafe_b64decode(key['e'] + '=' * (-len(key['e']) % 4)), 'big')
                        except (ValueError, binascii.Error):
                            sys.exit(8)
                        if modulus.bit_length() < 3072 or exponent < 3:
                            sys.exit(8)
                    return document
                if (document.get('issuer') != metadata_issuer or document.get('token_endpoint') != metadata_issuer + '/oauth2/token'
                        or document.get('authorization_endpoint') != metadata_issuer + '/oauth2/authorize'
                        or document.get('jwks_uri') != metadata_issuer + '/.well-known/jwks.json'
                        or document.get('userinfo_endpoint') != metadata_issuer + '/userinfo'):
                    sys.exit(8)
                return document
        except urllib.error.HTTPError as unavailable:
            try:
                if metadata_issuer is None and not jwks and not ready and not html and unavailable.code in (401, 403, 404, 503):
                    return
            finally:
                unavailable.close()
        except (urllib.error.URLError, OSError, TimeoutError):
            pass
        time.sleep(min(1, max(0, deadline - time.monotonic())))
    sys.exit(6)


def validate_formal_token_index(manifest, digest, token_index, required_inputs=None):
    """Check formal input provenance for each required resource/tenant/user; servers still validate the actual bearer."""
    import datetime
    import hashlib
    import re
    import sys
    from pathlib import Path
    from urllib.parse import urlsplit

    root = Path(manifest['runtimeRoot'])
    if Path(token_index) != root / 'secrets/token-index.json':
        sys.exit(3)
    registry = decode_task_json(read_task_file(root / 'secrets/runtime-registry.json', root / 'secrets', 2 * 1024 * 1024))
    index = decode_task_json(read_task_file(Path(token_index), root / 'secrets'))
    if not isinstance(index, dict) or set(index) != {'schemaVersion', 'runId', 'manifestSha256', 'tokens'}:
        sys.exit(2)
    for document in (registry, index):
        if (not isinstance(document, dict) or type(document.get('schemaVersion')) is not int or document['schemaVersion'] != 1
                or document.get('runId') != manifest['runId'] or document.get('manifestSha256') != digest):
            sys.exit(3)
    if registry.get('publicOrigin') != manifest['publicOrigin']:
        sys.exit(3)
    tenants = registry.get('tenantIds')
    if not isinstance(tenants, dict) or set(tenants) != {'default', 'tenant-b'}:
        sys.exit(3)
    for tenant in tenants.values():
        if not isinstance(tenant, str) or not re.fullmatch(r'[1-9][0-9]{0,18}', tenant) or int(tenant) > 9223372036854775807:
            sys.exit(3)
    if len(set(tenants.values())) != 2:
        sys.exit(3)
    for name, limit in (('clients', 192), ('resources', 64), ('cliResources', 32)):
        values = registry.get(name)
        if not isinstance(values, list) or not 1 <= len(values) <= limit or any(not isinstance(value, dict) for value in values):
            sys.exit(2)
    tokens = index.get('tokens')
    if not isinstance(tokens, list) or len(tokens) > 192:
        sys.exit(2)
    fields = {'tenantCode','tenantId','resourceKey','userKey','clientId','clientPk','issuer','resourceUri','scopes','tokenFile','probeStatus','tokenSha256','expiresAt'}
    indexed = {}
    for token in tokens:
        if not isinstance(token, dict) or set(token) != fields:
            sys.exit(2)
        identity = (token['tenantCode'], token['resourceKey'], token['userKey'])
        if any(not isinstance(value, str) for value in identity) or identity in indexed:
            sys.exit(2)
        indexed[identity] = token
    required, resource_keys = {}, set()
    ports = {value['port'] for value in manifest['processes']}
    for binding in registry['cliResources']:
        key = binding.get('resourceKey')
        if not isinstance(key, str) or not re.fullmatch(r'[a-z][a-z0-9-]{2,63}', key) or key in resource_keys:
            sys.exit(2)
        resource_keys.add(key)
        if not isinstance(binding.get('clientIds'), dict) or set(binding['clientIds']) != set(tenants):
            sys.exit(2)
        resources = [value for value in registry['resources'] if value.get('resourceServerId') == binding.get('resourceServerId')]
        if len(resources) != 1 or resources[0].get('status') != 'ACTIVE':
            sys.exit(3)
        uri = resources[0].get('resourceUri')
        if not isinstance(uri, str) or len(uri) > 2048 or any(ord(character) < 32 for character in uri):
            sys.exit(2)
        try:
            target = urlsplit(uri) if isinstance(uri, str) else None
            if (target is None or target.scheme != 'https' or target.hostname != '127.0.0.1' or target.port not in ports
                    or target.username is not None or target.password is not None or target.query or target.fragment):
                sys.exit(3)
        except ValueError:
            sys.exit(3)
        for tenant, tenant_id in tenants.items():
            client_id = binding.get('clientIds', {}).get(tenant)
            issuer = manifest['publicOrigin'] + '/tenants/' + tenant_id
            clients = [value for value in registry['clients'] if value.get('clientId') == client_id and value.get('realmCode') == tenant]
            if not isinstance(client_id, str) or len(clients) != 1:
                sys.exit(3)
            client = clients[0]
            client_pk = client.get('clientPk')
            if (type(client_pk) not in (int, str) or not re.fullmatch(r'[1-9][0-9]{0,18}', str(client_pk)) or int(client_pk) > 9223372036854775807
                    or client.get('clientType') != 'PUBLIC' or client.get('purpose') != 'USER' or client.get('status') != 'ACTIVE'
                    or type(client.get('refreshTokenTtlSeconds')) is not int or client['refreshTokenTtlSeconds'] != 0
                    or client.get('issuer') != issuer or client.get('resourceUris') != [uri]):
                sys.exit(3)
            approved_scopes = client.get('allowedScopes')
            if (not isinstance(approved_scopes, list) or not 1 <= len(approved_scopes) <= 64
                    or any(not isinstance(scope, str) or not re.fullmatch(r'[A-Za-z0-9._:-]{1,128}', scope) for scope in approved_scopes)
                    or len(set(approved_scopes)) != len(approved_scopes) or 'offline_access' in approved_scopes):
                sys.exit(3)
            for user in ('admin', 'viewer'):
                identity = (tenant, key, user)
                token = indexed.get(identity)
                if token is None:
                    if required_inputs is None or identity in required_inputs:
                        sys.exit(4)
                    continue
                scopes = token['scopes']
                if (token['tenantId'] != tenant_id or token['clientId'] != client_id or token['clientPk'] != str(client_pk)
                        or token['issuer'] != issuer or token['resourceUri'] != uri
                        or (token['probeStatus'] is not None and (type(token['probeStatus']) is not int
                            or token['probeStatus'] not in (200, 403))) or not isinstance(scopes, list)
                        or any(not isinstance(scope, str) for scope in scopes) or len(scopes) != len(approved_scopes)
                        or set(scopes) != set(approved_scopes) or not isinstance(token['tokenSha256'], str)
                        or not re.fullmatch(r'[0-9a-f]{64}', token['tokenSha256'])):
                    sys.exit(3)
                try:
                    expiry = datetime.datetime.fromisoformat(token['expiresAt'].replace('Z', '+00:00'))
                    if not token['expiresAt'].endswith('Z') or expiry.tzinfo is None or expiry.utcoffset() != datetime.timedelta():
                        sys.exit(2)
                    if expiry <= datetime.datetime.now(datetime.timezone.utc):
                        sys.exit(6)
                except (ValueError, AttributeError, TypeError):
                    sys.exit(2)
                if (not isinstance(token['tokenFile'], str) or len(token['tokenFile']) > 4096
                        or any(ord(character) < 32 for character in token['tokenFile']) or token['tokenFile'] in manifest['secretFiles'].values()):
                    sys.exit(3)
                data = read_task_file(Path(token['tokenFile']), root / 'secrets')
                if not data or hashlib.sha256(data).hexdigest() != token['tokenSha256']:
                    sys.exit(3)
                required[identity] = token
    if not set(indexed) <= set(required):
        sys.exit(3)
    if required_inputs is not None and not required_inputs <= set(required):
        sys.exit(4)
    return required


def require_operation_receipt(manifest, digest, filename, operation, state, required_step=None):
    """Consume the canonical public result without confusing two phases of CLI004."""
    import datetime
    import re
    import sys
    from pathlib import Path

    root = Path(manifest['runtimeRoot'])
    receipt = decode_task_json(read_task_file(root / 'evidence' / filename, root / 'evidence'))
    fields = {'schemaVersion','operationId','runId','manifestSha256','status','completedAt','targets','counts','steps','error'}
    if not isinstance(receipt, dict) or set(receipt) != fields:
        sys.exit(2)
    if (type(receipt['schemaVersion']) is not int or receipt['schemaVersion'] != 1 or receipt['operationId'] != operation
            or receipt['runId'] != manifest['runId'] or receipt['manifestSha256'] != digest
            or receipt['status'] != state or receipt['error'] is not None):
        sys.exit(5)
    try:
        instant = datetime.datetime.fromisoformat(receipt['completedAt'].replace('Z', '+00:00'))
        if not receipt['completedAt'].endswith('Z') or instant.tzinfo is None or instant.utcoffset() != datetime.timedelta():
            sys.exit(2)
    except (ValueError, TypeError, AttributeError):
        sys.exit(2)
    limits = {'databaseCount':12,'tenantCount':2,'clientCount':192,'catalogCount':32,'liveCaseCount':128}
    counts = receipt['counts']
    if (not isinstance(counts, dict) or set(counts) != set(limits)
            or any(type(counts[key]) is not int or not 0 <= counts[key] <= maximum for key,maximum in limits.items())
            or counts['databaseCount'] != (1 if operation == 'CLI-002' else 9)):
        sys.exit(2)
    states = {'COMMITTED','RUNNING','READY','UNREADY','STOPPED','BLOCKED','UNKNOWN_COMMIT','PARTIAL','FAILED','PASSED'}
    if not isinstance(receipt['targets'], list) or len(receipt['targets']) > 32:
        sys.exit(2)
    seen_targets = set()
    for target in receipt['targets']:
        if (not isinstance(target, dict) or set(target) != {'targetKey','identity','version','state'}
                or any(not isinstance(target[key], str) or not target[key] or len(target[key]) > 256
                       or any(ord(character) < 32 for character in target[key]) for key in target)
                or target['state'] not in states or target['targetKey'] in seen_targets):
            sys.exit(2)
        seen_targets.add(target['targetKey'])
    steps = receipt['steps']
    if not isinstance(steps, list) or not 1 <= len(steps) <= 32:
        sys.exit(2)
    seen = set()
    for step in steps:
        if (not isinstance(step, dict) or set(step) != {'stepId','state','evidence'} or not isinstance(step['stepId'], str)
                or not re.fullmatch(r'[A-Z][A-Z0-9_]{0,63}', step['stepId']) or step['stepId'] in seen or not isinstance(step['state'],str) or step['state'] not in states
                or not isinstance(step['evidence'], str) or len(step['evidence']) > 4096):
            sys.exit(2)
        path = Path(step['evidence'])
        if path.is_absolute() or not path.parts or path.parts[0] != 'evidence' or '..' in path.parts or any(ord(c) < 32 for c in str(path)):
            sys.exit(3)
        seen.add(step['stepId'])
    if required_step is not None and not any(step['stepId'] == required_step and step['state'] == state for step in steps):
        sys.exit(5)
    return receipt


def runtime_management_port(process):
    port = process.get('port')
    if process.get('id') in ('biz', 'mcp'):
        additional = process.get('additionalPorts')
        if not isinstance(additional, list) or len(additional) != 1 or additional[0] == port:
            sys.exit(3)
        port = additional[0]
    if type(port) is not int or not 1 <= port <= 65535:
        sys.exit(3)
    return port


def prepare_gateway_publication(manifest_file, manifest, digest, truststore):
    """Invoke the original management APIs through one fixed, actually executed owner test."""
    import datetime
    import hashlib
    import signal
    import subprocess
    import sys
    import uuid
    from pathlib import Path

    root, repository = Path(manifest['runtimeRoot']), Path(manifest['repoRoot'])
    raw = read_task_file(Path(manifest_file))
    if hashlib.sha256(raw).hexdigest() != digest:
        sys.exit(3)
    attempt = root / 'evidence'
    module = repository / 'egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-suite'
    classname = 'top.egon.cola.component.yuheng.test.integration.XingyuanResourceAuthorizationIT'
    method = 'publishTaskGatewayGroupAndProtectedReadRouteThroughOriginalOwners'
    source = module / 'src/test/java' / Path(*classname.split('.')).with_suffix('.java')
    if not source.is_file() or method + '(' not in source.read_text():
        sys.exit(4)
    source_sha = task_artifact_sha256(source)
    report = module / 'target/surefire-reports' / ('TEST-' + classname + '.xml')
    previous = None
    if report.exists():
        info = report.stat()
        previous = (info.st_dev, info.st_ino, info.st_mtime_ns, info.st_size, task_artifact_sha256(report))
    command = verification_runner_command(Path(manifest_file), manifest, digest, attempt, 'gateway-owner', truststore)
    nonce = uuid.uuid4().hex
    children = []
    handlers = {value: signal.getsignal(value) for value in (signal.SIGTERM, signal.SIGINT)}
    def cancel(signum, frame):
        sys.exit(9)
    try:
        for value in handlers:
            signal.signal(value, cancel)
        entry = start_verification_process(manifest, digest, attempt, 'gateway-owner-' + nonce, command, children)
        capture_verification_process(manifest, digest, attempt, entry)
        try:
            code = entry['process'].wait(timeout=manifest['deadlines']['operationSeconds'])
        except subprocess.TimeoutExpired:
            sys.exit(9)
        ready_file = attempt / 'gateway-publication.json'
        if code != 0:
            sys.exit(9 if (attempt / 'gateway-publication-running.json').exists() and not ready_file.exists() else 8)
        execution = read_fresh_junit_report(report, classname, {method}, entry['startedNs'], previous)
        owner_bytes = read_task_file(ready_file, attempt)
        owner = decode_task_json(owner_bytes)
        if (not isinstance(owner, dict) or owner.get('status') != 'COMMITTED'
                or owner.get('scope') != 'ORIGINAL_TASK_GATEWAY_OWNER_PUBLICATION_ONLY'
                or owner.get('runId') != manifest['runId'] or owner.get('manifestSha256') != digest
                or read_task_file(Path(manifest_file)) != raw or task_artifact_sha256(source) != source_sha):
            sys.exit(8)
        proof = {'schemaVersion': 1, 'runId': manifest['runId'], 'manifestSha256': digest, 'status': 'PASSED',
                 'scope': 'ORIGINAL_GATEWAY_OWNER_EXECUTION_ONLY', 'sourceSha256': source_sha, 'execution': execution,
                 'ownerProof': {'path': str(ready_file.relative_to(root)), 'sha256': hashlib.sha256(owner_bytes).hexdigest()},
                 'completedAt': datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00', 'Z')}
        publish_task_json(attempt / ('gateway-publication-execution-' + nonce + '.json'), root, proof)
        return proof
    finally:
        try:
            for entry in children:
                stop_verification_process(manifest, entry)
        finally:
            for value, handler in handlers.items():
                signal.signal(value, handler)


def start_consumers(argv):
    import datetime
    import fcntl
    import hashlib
    import os
    import subprocess
    import sys
    import time
    import uuid
    from pathlib import Path

    manifest_file, result_file, token_index = map(Path, argv[1:4])
    manifest_bytes = read_task_file(manifest_file)
    manifest = decode_task_json(manifest_bytes)
    digest = hashlib.sha256(manifest_bytes).hexdigest()
    root = Path(manifest['runtimeRoot'])
    operation_deadline = time.monotonic() + manifest['deadlines']['operationSeconds']
    controls = ('shoubing','nginx','tianshu','jianshen','yuheng','edge')
    consumers = ('biz','mcp','llm','portal-web','shoubing-web','jianshen-web','yuheng-web','tianshu-web',
                 'native-light','native-web','native-service','native-agent')
    frontends = {'portal-web':'/','shoubing-web':'/apps/tianquan-shoubing/','jianshen-web':'/apps/tianquan-jianshen/',
                 'yuheng-web':'/apps/yuheng/','tianshu-web':'/apps/tianshu/'}
    declared = {item['id']:item for item in manifest['processes']}
    if set(declared) != set(controls + consumers) or not set(declared) <= manifest['artifacts'].keys():
        sys.exit(4)
    exclusions = runtime_profile_exclusions(manifest)
    consumers = tuple(role for role in consumers if role not in exclusions)
    if result_file.exists():
        sys.exit(5)

    def budget():
        remaining = int(operation_deadline - time.monotonic())
        if remaining < 1:
            sys.exit(7)
        return dict(manifest, deadlines={key:min(value,remaining) for key,value in manifest['deadlines'].items()})

    lock_path = root / 'evidence/operation.lock'
    if lock_path.exists():
        read_task_file(lock_path, root / 'evidence', 1024)
    try:
        lock_fd = os.open(lock_path, os.O_RDWR | os.O_CREAT | os.O_NOFOLLOW, 0o600)
        fcntl.flock(lock_fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
    except OSError:
        sys.exit(5)
    try:
        for filename,operation,state,step in (('prepared.json','CLI-001','COMMITTED',None),('identity.json','CLI-002','COMMITTED',None),
                ('control-plane.json','CLI-003','RUNNING','CONTROL_PLANE'),
                ('authorization-platform.json','CLI-004','COMMITTED','PLATFORM_AUTHORIZATION'),
                ('authorization-native.json','CLI-004','COMMITTED','NATIVE_AUTHORIZATION')):
            prerequisite = require_operation_receipt(manifest,digest,filename,operation,state,step)
            if filename == 'control-plane.json':
                control_receipt = prerequisite
        read_native_build_provenance(manifest)
        formal_inputs = validate_formal_token_index(manifest,digest,token_index)
        record_file = root / 'evidence/processes.json'
        record_bytes = read_task_file(record_file,root / 'evidence')
        record = decode_task_json(record_bytes)
        validate_owned_process_record(manifest,digest,record)
        observed = {item['id']:observe_owned_process(manifest,item) for item in record['processes']}
        if set(observed) & set(exclusions):
            sys.exit(3)
        if any(observed.get(role) != 'RUNNING' for role in controls):
            sys.exit(6)
        recorded_controls = {value['id']:value for value in record['processes'] if value['id'] in controls}
        targets = {value['targetKey']:value for value in control_receipt['targets']}
        if set(targets) != set(controls) or any(targets[role]['identity'] != str(recorded_controls[role]['pid'])
                or targets[role]['version'] != recorded_controls[role]['startInstant'] or targets[role]['state'] != 'RUNNING' for role in controls):
            sys.exit(3)
        for role in consumers:
            if observed.get(role) == 'RUNNING':
                continue
            for port in (declared[role]['port'],*declared[role].get('additionalPorts',[])):
                try:
                    occupied = subprocess.run(['lsof','-nP','-iTCP:' + str(port),'-sTCP:LISTEN','-t'],stdin=subprocess.DEVNULL,
                                              stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=budget()['deadlines']['httpSeconds'])
                except (OSError,subprocess.TimeoutExpired):
                    sys.exit(6)
                if occupied.returncode == 0 and occupied.stdout.strip():
                    sys.exit(3)
                if occupied.returncode not in (0,1) or occupied.stderr.strip():
                    sys.exit(6)
        profiles = decode_task_json(read_task_file(root / 'evidence/profiles-ready.json',root / 'evidence'))
        if (type(profiles.get('schemaVersion')) is not int or profiles['schemaVersion'] != 1 or profiles.get('runId') != manifest['runId']
                or profiles.get('manifestSha256') != digest or not isinstance(profiles.get('profiles'),dict)
                or profiles.get('runtimeExclusions', {}) != exclusions
                or set(profiles['profiles']) != {role for role, process in declared.items() if process['kind'] == 'JAVA' and role not in exclusions}):
            sys.exit(3)
        for role,process in declared.items():
            reference = manifest['artifacts'][role]
            if task_artifact_sha256(reference['path']) != reference['sha256']:
                sys.exit(3)
            if process['kind'] == 'JAVA' and role not in exclusions:
                file = root / 'profiles' / (role + '.properties')
                binding = profiles['profiles'].get(role)
                if not isinstance(binding,dict) or binding.get('path') != str(file) or hashlib.sha256(read_task_file(file,root / 'profiles',2 * 1024 * 1024)).hexdigest() != binding.get('sha256'):
                    sys.exit(3)
        frontend_profiles = prepare_frontend_profiles(budget(),digest)
        if set(frontend_profiles.get('profiles',{})) != set(frontends):
            sys.exit(7)
        truststore = ensure_task_truststore(budget(),digest)
        running = {item['id']:item for item in record['processes'] if observed[item['id']] == 'RUNNING'}
        if any(state == 'STOPPED' for state in observed.values()):
            publish_task_json(root / 'evidence' / ('processes-history-' + uuid.uuid4().hex + '.json'),root,record)

        def save_record():
            nonlocal record_bytes
            updated = {'schemaVersion':1,'runId':manifest['runId'],'manifestSha256':digest,'processes':list(running.values())}
            validate_owned_process_record(manifest,digest,updated)
            publish_task_json(record_file,root,updated,record_bytes)
            record_bytes = read_task_file(record_file,root / 'evidence')

        for role in consumers:
            if role not in running:
                if read_task_file(manifest_file) != manifest_bytes:
                    sys.exit(3)
                running[role] = launch_owned_runtime(budget(),digest,role,truststore,manifest_file)
                save_record()
            elif observe_owned_process(manifest,running[role]) != 'RUNNING':
                sys.exit(6)
            if role not in frontends:
                wait_owned_https(budget(),running[role],'https://127.0.0.1:' + str(runtime_management_port(declared[role])) + '/actuator/health/liveness')
        prepare_gateway_publication(manifest_file, budget(), digest, truststore)
        # All real providers may now register their original leases/definitions before the required readiness checks.
        for role in controls + consumers:
            if declared[role]['kind'] == 'JAVA':
                address = 'https://127.0.0.1:' + str(runtime_management_port(declared[role]))
                wait_owned_https(budget(),running[role],address + '/actuator/health',ready=True)
                wait_owned_https(budget(),running[role],address + '/actuator/health/readiness',ready=True)
        for role,mount in frontends.items():
            if observe_owned_process(manifest,running[role]) != 'RUNNING':
                sys.exit(6)
            wait_owned_https(budget(),running['nginx'],manifest['publicOrigin'] + mount,html=True)
        validate_formal_token_index(manifest,digest,token_index)
        if runtime_profile_exclusions(manifest) != exclusions:
            sys.exit(3)
        for item in running.values():
            if observe_owned_process(manifest,item) != 'RUNNING':
                sys.exit(6)
        if read_task_file(manifest_file) != manifest_bytes:
            sys.exit(3)
        result = {'schemaVersion':1,'operationId':'CLI-005','runId':manifest['runId'],'manifestSha256':digest,'status':'RUNNING',
                  'completedAt':datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00','Z'),
                  'targets':[{'targetKey':role,'identity':str(running[role]['pid']),'version':running[role]['startInstant'],'state':'RUNNING'} for role in controls+consumers],
                  'counts':{'databaseCount':9,'tenantCount':2,'clientCount':len({(item['issuer'],item['clientPk']) for item in formal_inputs.values()}),'catalogCount':4,'liveCaseCount':0},
                  'steps':[{'stepId':'CONSUMERS','state':'RUNNING','evidence':'evidence/processes.json'},
                           {'stepId':'FRONTEND_CONFIG','state':'COMMITTED','evidence':'evidence/frontend-profiles-ready.json'},
                           {'stepId':'APPLICATION_READINESS','state':'READY','evidence':'evidence/processes.json'}],'error':None}
        for destination in {root / 'evidence/consumers.json',result_file}:
            previous = read_task_file(destination,root / 'evidence') if destination.exists() else None
            if previous is not None:
                prior = decode_task_json(previous)
                if prior.get('runId') != manifest['runId'] or prior.get('manifestSha256') != digest or prior.get('operationId') != 'CLI-005':
                    sys.exit(5)
            publish_task_json(destination,root,result,previous)
        print('CLI-005 RUNNING ' + str(result_file.relative_to(root)))
    finally:
        os.close(lock_fd)


def start_control_plane(argv):
    import fcntl
    import hashlib
    import os
    import subprocess
    import sys
    import time
    from pathlib import Path

    manifest_file, result_file = Path(argv[1]), Path(argv[2])
    manifest_bytes = read_task_file(manifest_file)
    manifest = decode_task_json(manifest_bytes)
    digest = hashlib.sha256(manifest_bytes).hexdigest()
    root = Path(manifest['runtimeRoot'])
    operation_deadline = time.monotonic() + manifest['deadlines']['operationSeconds']

    def budget():
        remaining = int(operation_deadline - time.monotonic())
        if remaining < 1:
            sys.exit(7)
        deadlines = {key: min(value, remaining) for key, value in manifest['deadlines'].items()}
        return dict(manifest, deadlines=deadlines)

    required = {'shoubing', 'nginx', 'tianshu', 'jianshen', 'yuheng', 'edge'}
    declared = {process['id']: process for process in manifest['processes']}
    if not required <= declared.keys() or not required <= manifest['artifacts'].keys():
        sys.exit(4)
    for filename, operation in (('prepared.json', 'CLI-001'), ('identity.json', 'CLI-002')):
        receipt = decode_task_json(read_task_file(root / 'evidence' / filename, root / 'evidence'))
        if (type(receipt.get('schemaVersion')) is not int or receipt['schemaVersion'] != 1
                or receipt.get('operationId') != operation or receipt.get('status') != 'COMMITTED'
                or receipt.get('runId') != manifest['runId'] or receipt.get('manifestSha256') != digest):
            sys.exit(5)
    lock_path = root / 'evidence/operation.lock'
    if lock_path.exists():
        read_task_file(lock_path, root / 'evidence', 1024)
    try:
        lock_fd = os.open(lock_path, os.O_RDWR | os.O_CREAT | os.O_NOFOLLOW, 0o600)
        fcntl.flock(lock_fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
    except OSError:
        sys.exit(5)
    try:
        record_file = root / 'evidence/processes.json'
        record = decode_task_json(read_task_file(record_file, root / 'evidence')) if record_file.exists() else {
            'schemaVersion': 1, 'runId': manifest['runId'], 'manifestSha256': digest, 'processes': []}
        validate_owned_process_record(manifest, digest, record, allow_empty=True)
        observed = {process['id']: observe_owned_process(manifest, process) for process in record['processes']}
        for role in ('shoubing', 'nginx', 'tianshu', 'jianshen', 'yuheng', 'edge'):
            if observed.get(role) == 'RUNNING':
                continue
            process = declared[role]
            for port in (process['port'], *process.get('additionalPorts', [])):
                try:
                    status = subprocess.run(['lsof', '-nP', '-iTCP:' + str(port), '-sTCP:LISTEN', '-t'],
                                            stdin=subprocess.DEVNULL, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                            timeout=budget()['deadlines']['httpSeconds'])
                except (OSError, subprocess.TimeoutExpired):
                    sys.exit(4)
                if status.returncode == 0 and status.stdout.strip():
                    sys.exit(3)
                if status.returncode not in (0, 1) or status.stderr.strip():
                    sys.exit(6)
        for role in required:
            reference = manifest['artifacts'][role]
            if task_artifact_sha256(reference['path']) != reference['sha256']:
                sys.exit(3)
        validate_task_nginx_configuration(manifest)
        profiles = run_task_fixture(manifest_file, budget(), digest, 'profiles')
        if not (required - {'nginx'}) <= profiles.get('profiles', {}).keys():
            sys.exit(7)
        run_task_fixture(manifest_file, budget(), digest, 'tianshu-catalog')
        truststore = ensure_task_truststore(budget(), digest)
        registry = decode_task_json(read_task_file(root / 'secrets/runtime-registry.json', root / 'secrets', 2 * 1024 * 1024))
        if registry.get('runId') != manifest['runId'] or registry.get('manifestSha256') != digest:
            sys.exit(3)
        if result_file.exists():
            sys.exit(5)
        record_bytes = read_task_file(record_file, root / 'evidence') if record_file.exists() else None
        running = {item['id']: item for item in record['processes'] if observed[item['id']] == 'RUNNING'}
        if any(value == 'STOPPED' for value in observed.values()):
            import uuid
            publish_task_json(root / 'evidence' / ('processes-history-' + uuid.uuid4().hex + '.json'), root, record)

        def save_record():
            nonlocal record_bytes
            updated = {'schemaVersion': 1, 'runId': manifest['runId'], 'manifestSha256': digest, 'processes': list(running.values())}
            validate_owned_process_record(manifest, digest, updated)
            publish_task_json(record_file, root, updated, record_bytes)
            record_bytes = read_task_file(record_file, root / 'evidence')

        def ensure(role):
            if role not in running:
                if read_task_file(manifest_file) != manifest_bytes:
                    sys.exit(3)
                running[role] = launch_owned_runtime(budget(), digest, role, truststore, manifest_file)
                save_record()
            elif observe_owned_process(manifest, running[role]) != 'RUNNING':
                sys.exit(6)
            return running[role]

        authorization = ensure('shoubing')
        platform_issuer = manifest['publicOrigin'] + '/platform'
        wait_owned_https(budget(), authorization, 'https://127.0.0.1:28120/platform/.well-known/openid-configuration', platform_issuer)
        proxy = ensure('nginx')
        wait_owned_https(budget(), proxy, platform_issuer + '/.well-known/openid-configuration', platform_issuer)
        wait_owned_https(budget(), proxy, platform_issuer + '/.well-known/jwks.json', jwks=True)
        tianshu = ensure('tianshu')
        wait_owned_https(budget(), tianshu, 'https://127.0.0.1:28150/actuator/health/liveness')
        # The original read-only clients also initialize the already-owned JA technical schema before its main starts.
        clients = run_task_fixture(manifest_file, budget(), digest, 'control-clients')
        if clients.get('membershipCount') != 6 or clients.get('catalogCount') != 4:
            sys.exit(7)
        for role in ('jianshen', 'yuheng', 'edge'):
            current = ensure(role)
            wait_owned_https(budget(), current, 'https://127.0.0.1:' + str(runtime_management_port(declared[role])) + '/actuator/health/liveness')
        for tenant in ('default', 'tenant-b'):
            actual = registry.get('tenantIds', {}).get(tenant)
            if not isinstance(actual, str):
                sys.exit(3)
            issuer = manifest['publicOrigin'] + '/tenants/' + actual
            wait_owned_https(budget(), proxy, issuer + '/.well-known/openid-configuration', issuer)
            wait_owned_https(budget(), proxy, issuer + '/.well-known/jwks.json', jwks=True)
        import datetime
        now = datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00', 'Z')
        result = {'schemaVersion': 1, 'operationId': 'CLI-003', 'runId': manifest['runId'], 'manifestSha256': digest,
                  'status': 'RUNNING', 'completedAt': now,
                  'targets': [{'targetKey': role, 'identity': str(running[role]['pid']), 'version': running[role]['startInstant'], 'state': 'RUNNING'}
                              for role in ('shoubing', 'nginx', 'tianshu', 'jianshen', 'yuheng', 'edge')],
                  'counts': {'databaseCount': 9, 'tenantCount': 2, 'clientCount': len(registry.get('clients', [])), 'catalogCount': 4, 'liveCaseCount': 0},
                  'steps': [{'stepId': 'CONTROL_PLANE', 'state': 'RUNNING', 'evidence': 'evidence/control-plane-clients.json'}], 'error': None}
        for destination in set((root / 'evidence/control-plane.json', result_file)):
            previous = read_task_file(destination, root / 'evidence') if destination.exists() else None
            if previous is not None:
                old = decode_task_json(previous)
                if old.get('runId') != manifest['runId'] or old.get('manifestSha256') != digest or old.get('operationId') != 'CLI-003':
                    sys.exit(5)
            publish_task_json(destination, root, result, previous)
        print('CLI-003 RUNNING ' + str(result_file.relative_to(root)))
    finally:
        os.close(lock_fd)


def short_process_fingerprint(pid, timeout):
    """Inspect a transient validation child independently of the long-running application ledger."""
    import datetime
    import hashlib
    import locale
    import os
    import subprocess
    import sys

    locale.setlocale(locale.LC_TIME,'C')
    try:
        result = subprocess.run(['ps','-p',str(pid),'-o','lstart=','-o','stat=','-o','pgid=','-o','command='],
                                stdin=subprocess.DEVNULL,stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True,
                                env=dict(os.environ,LC_ALL='C',TZ='UTC'),timeout=timeout)
    except (OSError,subprocess.TimeoutExpired):
        sys.exit(6)
    if result.returncode == 1 and not result.stdout.strip() and not result.stderr.strip():
        return None
    if result.returncode or len(result.stdout)>64*1024 or len(result.stdout.strip().splitlines())!=1:
        sys.exit(6)
    columns = result.stdout.strip().split(None,7)
    if len(columns)!=8 or not columns[6].isdigit():
        sys.exit(10)
    try:
        started = datetime.datetime.strptime(' '.join(columns[:5]),'%a %b %d %H:%M:%S %Y').replace(tzinfo=datetime.timezone.utc)
    except ValueError:
        sys.exit(10)
    if columns[5].startswith('Z'):
        return None
    return {'pid':pid,'processGroup':int(columns[6]),'startInstant':started.isoformat().replace('+00:00','Z'),
            'commandSha256':hashlib.sha256(columns[7].encode('utf-8')).hexdigest()}


def start_verification_process(manifest, digest, attempt, stage, command, owned_children=None):
    import os
    import re
    import subprocess
    import sys
    import time
    import uuid
    from pathlib import Path

    root, attempt = Path(manifest['runtimeRoot']),Path(attempt)
    if (stage not in {'oracle','browser','outbox','resource','native','compat','formal-revoke','service-cache','field-owner','field-consumer','platform-http-audit','native-userinfo-audit','native-rpc-audit','mcp-audit','mcp-owner'}
            and not re.fullmatch(r'formal-input-[0-9]{3}|service-revoke-[0-9a-f]{64}|gateway-owner-[0-9a-f]{32}',stage)) or (attempt.parent != root/'evidence'
            and not ((stage in {'formal-revoke','platform-http-audit','native-userinfo-audit','native-rpc-audit','mcp-audit','mcp-owner'} or re.fullmatch(r'service-revoke-[0-9a-f]{64}|gateway-owner-[0-9a-f]{32}',stage)) and attempt == root/'evidence')):
        sys.exit(3)
    if not isinstance(command,dict) or not isinstance(command.get('argv'),list) or not command['argv'] or any(not isinstance(value,str) for value in command['argv']):
        sys.exit(2)
    if (attempt/(stage+'-process.json')).exists():
        sys.exit(5)
    log_file = attempt/(stage+'-'+uuid.uuid4().hex+'.log')
    descriptor = os.open(log_file,os.O_WRONLY|os.O_CREAT|os.O_EXCL|os.O_NOFOLLOW,0o600)
    started_ns = time.time_ns()
    try:
        with os.fdopen(descriptor,'ab') as output:
            process = subprocess.Popen(command['argv'],cwd=command['cwd'],env=command['environment'],stdin=subprocess.DEVNULL,
                                       stdout=output,stderr=subprocess.STDOUT,start_new_session=True)
    except OSError:
        sys.exit(6)
    entry = {'process':process,'record':None,'stage':stage,'startedNs':started_ns,'logFile':str(log_file),
             'runId':manifest['runId'],'manifestSha256':digest}
    if owned_children is not None:
        owned_children.append(entry)
    publish_task_json(attempt/(stage+'-process.json'),root,{'schemaVersion':1,'runId':manifest['runId'],'manifestSha256':digest,
                      'stage':stage,'pid':process.pid,'startedNs':started_ns,'state':'STARTED','identity':None,
                      'logFile':str(log_file.relative_to(root))})
    return entry


def capture_verification_process(manifest,digest,attempt,entry):
    import sys
    import time
    from pathlib import Path
    root,attempt=Path(manifest['runtimeRoot']),Path(attempt)
    process,stage=entry['process'],entry['stage']
    if entry.get('runId')!=manifest['runId'] or entry.get('manifestSha256')!=digest:
        sys.exit(3)
    previous = None
    deadline = time.monotonic()+min(10,manifest['deadlines']['startupSeconds'])
    while time.monotonic()<deadline:
        if process.poll() is not None:
            return entry
        current = short_process_fingerprint(process.pid,manifest['deadlines']['httpSeconds'])
        if current is None:
            if process.poll() is not None:
                return entry
            sys.exit(6)
        if current['processGroup'] != process.pid:
            sys.exit(10)
        if current==previous:
            entry['record']=current
            birth=attempt/(stage+'-process.json')
            previous_bytes=read_task_file(birth,root/'evidence')
            receipt=decode_task_json(previous_bytes)
            receipt.update(state='RUNNING',identity=current)
            publish_task_json(birth,root,receipt,previous_bytes)
            return entry
        previous=current
        time.sleep(0.1)
    sys.exit(6)


def stop_verification_process(manifest, entry):
    import os
    import signal
    import subprocess
    import sys

    process,record=entry['process'],entry.get('record')
    if process.poll() is not None:
        return
    if entry.get('runId')!=manifest['runId']:
        sys.exit(3)
    if record is None:
        # This is the unreaped creation handle, never a PID reconstructed from an old receipt.
        record=short_process_fingerprint(process.pid,manifest['deadlines']['httpSeconds'])
        if record is None:
            if process.poll() is not None:
                return
            sys.exit(9)
        entry['record']=record
    observed=short_process_fingerprint(process.pid,manifest['deadlines']['httpSeconds'])
    if process.poll() is not None:
        return
    if observed!=record or observed['processGroup']!=process.pid:
        sys.exit(10)
    try:
        if os.getpgid(process.pid)!=process.pid:
            sys.exit(10)
    except ProcessLookupError:
        if process.poll() is not None:
            return
        sys.exit(6)
    try:
        os.killpg(process.pid,signal.SIGTERM)
        process.wait(timeout=min(10,manifest['deadlines']['startupSeconds']))
    except (OSError,subprocess.TimeoutExpired):
        sys.exit(7)


def formal_input_command(manifest_file, manifest, digest, attempt, ordinal, identity):
    import os
    import re
    import sys
    from pathlib import Path
    tenant,resource,user=identity
    root,repository,attempt=Path(manifest['runtimeRoot']),Path(manifest['repoRoot']),Path(attempt)
    if (tenant not in ('default','tenant-b') or user not in ('admin','viewer') or not isinstance(resource,str)
            or not re.fullmatch(r'[a-z][a-z0-9-]{2,63}',resource) or type(ordinal) is not int or not 0<=ordinal<128
            or attempt.parent!=root/'evidence' or not re.fullmatch(r'verify-[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}',attempt.name)
            or attempt.is_symlink() or not attempt.is_dir() or attempt.stat().st_uid!=os.getuid() or attempt.stat().st_mode&0o777!=0o700):
        sys.exit(3)
    stage='formal-input-'+str(ordinal).zfill(3)
    output=root/'secrets'/(attempt.name+'-'+str(ordinal).zfill(3)+'.at')
    result=attempt/(stage+'-result.json')
    if output.exists() or result.exists():
        sys.exit(5)
    environment=isolated_runtime_environment()
    environment.update(XINGYUAN_ISOLATED_INTEGRATION='true',XINGYUAN_VERIFICATION_EVIDENCE_DIRECTORY=str(attempt))
    return {'argv':[sys.executable,'-I',str(repository/'scripts/unified-xingyuan/lib/isolated_integration.py'),'authorize-cli',
                    str(manifest_file),str(result),tenant,resource,user,str(output),str(repository)],
            'cwd':str(repository),'environment':environment,'stage':stage,'output':str(output),'result':str(result)}


def read_formal_input_result(manifest, digest, command, expected, started_ns):
    """Bind a new CLI006 Code result to the unchanged authorization tuple and its private output."""
    import datetime
    import hashlib
    import sys
    from pathlib import Path
    root=Path(manifest['runtimeRoot'])
    result_file,output=Path(command['result']),Path(command['output'])
    raw=read_task_file(result_file,root/'evidence')
    result=decode_task_json(raw)
    if (not isinstance(result,dict) or set(result)!={'schemaVersion','operationId','runId','manifestSha256','status','completedAt','targets','counts','steps','error'}
            or type(result.get('schemaVersion')) is not int or result['schemaVersion']!=1 or result.get('operationId')!='CLI-006'
            or result.get('runId')!=manifest['runId'] or result.get('manifestSha256')!=digest or result.get('status')!='COMMITTED' or result.get('error') is not None):
        sys.exit(8)
    counts={'databaseCount':0,'tenantCount':1,'clientCount':1,'catalogCount':0,'liveCaseCount':0}
    if (result.get('counts')!=counts or any(type(value) is not int for value in result['counts'].values())
            or result.get('targets')!=[{'targetKey':expected['resourceKey'],'identity':'PUBLIC_USER','version':'1','state':'COMMITTED'}]
            or result.get('steps')!=[] or result_file.stat().st_mtime_ns<started_ns):
        sys.exit(8)
    try:
        completed=datetime.datetime.fromisoformat(result['completedAt'].replace('Z','+00:00'))
        if (not result['completedAt'].endswith('Z') or completed.tzinfo is None or completed.utcoffset()!=datetime.timedelta()
                or completed.timestamp()*1_000_000_000<started_ns or completed>datetime.datetime.now(datetime.timezone.utc)+datetime.timedelta(seconds=5)):
            sys.exit(8)
    except (ValueError,TypeError,AttributeError):
        sys.exit(8)
    index=decode_task_json(read_task_file(root/'secrets/token-index.json',root/'secrets'))
    if (not isinstance(index,dict) or set(index)!={'schemaVersion','runId','manifestSha256','tokens'}
            or type(index.get('schemaVersion')) is not int or index['schemaVersion']!=1 or index.get('runId')!=manifest['runId']
            or index.get('manifestSha256')!=digest or not isinstance(index.get('tokens'),list) or len(index['tokens'])>192
            or any(not isinstance(value,dict) for value in index['tokens'])):
        sys.exit(8)
    selected=[value for value in index['tokens'] if all(value.get(key)==expected[key] for key in ('tenantCode','resourceKey','userKey'))]
    if len(selected)!=1:
        sys.exit(8)
    token=selected[0]
    immutable={'tenantCode','tenantId','resourceKey','userKey','clientId','clientPk','issuer','resourceUri'}
    if (set(token)!=immutable|{'scopes','tokenFile','probeStatus','tokenSha256','expiresAt'}
            or any(token[key]!=expected[key] for key in immutable) or token['probeStatus'] is not None
            or not isinstance(token['scopes'],list) or any(not isinstance(scope,str) for scope in token['scopes'])
            or sorted(token['scopes'])!=sorted(expected['scopes']) or token['tokenFile']!=str(output)
            or token['tokenSha256']==expected['tokenSha256'] or str(output)==expected['tokenFile']):
        sys.exit(8)
    material=read_task_file(output,root/'secrets')
    if not material or output.stat().st_mtime_ns<started_ns or hashlib.sha256(material).hexdigest()!=token['tokenSha256']:
        sys.exit(8)
    try:
        expiry=datetime.datetime.fromisoformat(token['expiresAt'].replace('Z','+00:00'))
        if (not token['expiresAt'].endswith('Z') or expiry.tzinfo is None or expiry.utcoffset()!=datetime.timedelta()
                or expiry<=datetime.datetime.now(datetime.timezone.utc)):
            sys.exit(6)
    except (ValueError,TypeError,AttributeError):
        sys.exit(8)
    return {'tenantCode':expected['tenantCode'],'resourceKey':expected['resourceKey'],'userKey':expected['userKey'],
            'result':str(result_file.relative_to(root)),'resultSha256':hashlib.sha256(raw).hexdigest(),
            'tokenReference':token['tokenSha256'],'expiresAt':token['expiresAt']}


def verification_runner_command(manifest_file, manifest, digest, attempt, stage, truststore, token_sha256=None):
    """Build only reviewed validation invocations; sensitive JVM values stay in the task argument file."""
    import hashlib
    import os
    import re
    import sys
    from pathlib import Path

    root, repository, attempt = Path(manifest['runtimeRoot']), Path(manifest['repoRoot']), Path(attempt)
    default_evidence = stage in {'formal-revoke','service-revoke','platform-http-audit','native-userinfo-audit','native-rpc-audit','mcp-audit','gateway-owner','mcp-owner'} and attempt == root / 'evidence'
    if (not default_evidence and (attempt.parent != root / 'evidence' or not re.fullmatch(r'verify-[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}',attempt.name))
            or attempt.is_symlink() or not attempt.is_dir() or attempt.stat().st_uid != os.getuid() or attempt.stat().st_mode & 0o777 != 0o700
            or Path(truststore) != root / 'secrets/java-truststore.p12'):
        sys.exit(3)
    read_task_file(Path(truststore),root / 'secrets')
    choices = {
        'oracle':('egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin','IsolatedOAuthProvisioningIT#observeBffFamilies','idp'),
        'outbox':('egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin','OutboxTransactionRollbackIT#jpaRoleResourceAndMpOutbox*','rbac'),
        'field-owner':('egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin','IsolatedAuthorizationProvisioningIT#observeNativeFieldRules','rbac'),
        'field-consumer':('egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-suite','XingyuanFieldAuthorizationIT#nativeFieldRulesControlWritesAcrossRealOwnerEpochsAndTenants',None),
        'formal-revoke':('egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin','IsolatedOAuthProvisioningIT#revokeFormalFamily','idp'),
        'service-revoke':('egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin','IsolatedOAuthProvisioningIT#revokeServiceAccessToken','idp'),
        'platform-http-audit':('egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin','IsolatedOAuthProvisioningIT#observePlatformHttpAudits','idp'),
        'native-userinfo-audit':('egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin','IsolatedOAuthProvisioningIT#observeNativeUserInfoAudits','idp'),
        'native-rpc-audit':('egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin','IsolatedOAuthProvisioningIT#observeNativeRpcAudits','idp'),
        'gateway-owner':('egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-suite','XingyuanResourceAuthorizationIT#publishTaskGatewayGroupAndProtectedReadRouteThroughOriginalOwners',None),
        'mcp-owner':('egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin','IsolatedAuthorizationProvisioningIT#provisionTaskMcpAuthorization','rbac'),
        'mcp-audit':('egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin','IsolatedOAuthProvisioningIT#observeMcpHttpAudits','idp'),
        'service-cache':('egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-suite','XingyuanNativeAuthenticationIT#configuredNativeServiceClientsRenewThroughTheRealAsWithSingleFlight',None),
        'resource':('egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-suite','XingyuanResourceAuthorizationIT#verifyResourceAuthorization',None),
        'native':('egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-suite','XingyuanNativeAuthenticationIT#verifyNativeAuthentication',None)}
    environment = isolated_runtime_environment()
    environment.update(XINGYUAN_ISOLATED_INTEGRATION='true',XINGYUAN_INTEGRATION_MANIFEST=str(manifest_file),
                       XINGYUAN_VERIFICATION_EVIDENCE_DIRECTORY=str(attempt),NODE_EXTRA_CA_CERTS=manifest['secretFiles']['tls-ca-cert'])
    if stage == 'browser':
        workspace = repository / 'egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web'
        package = workspace / 'node_modules/@playwright/test/package.json'
        if not package.is_file():
            sys.exit(4)
        metadata = decode_task_json(package.read_bytes())
        if metadata.get('name') != '@playwright/test' or metadata.get('version') != '1.62.0':
            sys.exit(4)
        return {'argv':['npm','exec','--','playwright','test','--config','playwright.integration.config.ts','--grep','LIVE-','--workers','1'],
                'cwd':str(workspace),'environment':environment}
    if stage not in choices:
        sys.exit(2)
    if stage in {'formal-revoke','service-revoke'} and (not isinstance(token_sha256,str) or not re.fullmatch(r'[0-9a-f]{64}',token_sha256)):
        sys.exit(2)
    if stage not in {'formal-revoke','service-revoke'} and token_sha256 is not None:
        sys.exit(2)
    module,selector,database = choices[stage]
    password = task_credential(Path(manifest['secretFiles']['tls-keystore-password']),root / 'secrets')
    def quote(value):
        return '"' + value.replace('\\','\\\\').replace('"','\\"').replace('\n','\\n').replace('\r','\\r').replace('\t','\\t') + '"'
    options = ['-Duser.timezone=UTC','-Djavax.net.ssl.trustStore=' + str(truststore),'-Djavax.net.ssl.trustStoreType=PKCS12',
               '-Djavax.net.ssl.trustStorePassword=' + password]
    argument_file = attempt / (stage+'.jvm.args' if stage in {'formal-revoke','service-revoke'} else 'verification.jvm.args')
    publish_private_file(argument_file,root,('\n'.join(quote(option) for option in options) + '\n').encode('utf-8'))
    arguments = [str(repository / 'mvnw'),'-B','-ntp','-o','-Duser.timezone=UTC','-pl',module,'-am','-Dtest=' + selector,
                 '-DskipTests=false','-Dmaven.test.skip=false','-Dsurefire.failIfNoSpecifiedTests=false',
                 '-DargLine=' + quote('@' + str(argument_file)),
                 '-Dxingyuan.isolated.repository-root=' + str(repository),'-Dxingyuan.isolated.manifest-file=' + str(manifest_file),
                 '-Dxingyuan.isolated.evidence-directory=' + str(attempt),'-Dxingyuan.isolated.token-index=' + str(root / 'secrets/token-index.json')]
    if database is not None:
        receipt_file = root / 'evidence' / ('db-' + database + '.json')
        receipt = decode_task_json(read_task_file(receipt_file,root / 'evidence'))
        if (receipt.get('runId') != manifest['runId'] or receipt.get('manifestSha256') != digest or receipt.get('role') != database
                or receipt.get('databaseName') != 'xingyuan_it_' + database + '_20261011' or type(receipt.get('oid')) is not int or receipt['oid'] <= 0):
            sys.exit(3)
        arguments.append('-Dxingyuan.isolated.receipt-file=' + str(receipt_file))
    if stage in {'formal-revoke','service-revoke'}:
        arguments.append('-Dxingyuan.isolated.revocation-token-sha256=' + token_sha256)
    settings = [value for value in manifest['policyFiles'] if Path(value['path']) == root / 'policies/maven-settings.xml']
    if len(settings) > 1:
        sys.exit(2)
    if settings:
        content = read_task_file(Path(settings[0]['path']),root,2 * 1024 * 1024)
        if hashlib.sha256(content).hexdigest() != settings[0]['sha256']:
            sys.exit(3)
        arguments[1:1] = ['-s',settings[0]['path']]
    arguments.append('test')
    return {'argv':arguments,'cwd':str(repository),'environment':environment,'module':module,'selector':selector}


def revoke_formal_family(argv):
    """Private adapter from a proven resource read to the existing AS family owner."""
    import datetime
    import fcntl
    import hashlib
    import os
    import re
    import subprocess
    import sys
    from pathlib import Path

    manifest_file,result_file=map(Path,argv[1:3])
    token_hash=argv[3]
    if not re.fullmatch(r'[0-9a-f]{64}',token_hash):
        sys.exit(2)
    raw=read_task_file(manifest_file)
    manifest=decode_task_json(raw)
    digest=hashlib.sha256(raw).hexdigest()
    root,attempt=Path(manifest['runtimeRoot']),result_file.parent
    if result_file.name!='formal-revocation-'+token_hash+'.json' or result_file.exists():
        sys.exit(5)
    if attempt!=root/'evidence' and (attempt.parent!=root/'evidence'
            or not re.fullmatch(r'verify-[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}',attempt.name)):
        sys.exit(3)
    index=validate_formal_token_index(manifest,digest,root/'secrets/token-index.json')
    selected=[item for item in index.values() if item['tokenSha256']==token_hash]
    if len(selected)!=1:
        sys.exit(3)
    token=selected[0]
    request=decode_task_json(read_task_file(attempt/('formal-revocation-request-'+token_hash+'.json'),root/'evidence'))
    if not isinstance(request,dict):
        sys.exit(2)
    if (request.get('schemaVersion')!=1 or type(request.get('schemaVersion')) is not int or request.get('status')!='READY'
            or request.get('runId')!=manifest['runId'] or request.get('manifestSha256')!=digest
            or type(request.get('baselineStatus')) is not int or request['baselineStatus']!=200
            or request.get('tokenSha256')!=token_hash or request.get('clientPk')!=token['clientPk']
            or request.get('tenantId')!=token['tenantId'] or request.get('issuer')!=token['issuer']
            or request.get('resourceUri')!=token['resourceUri']
            or not isinstance(request.get('authorizationReference'),str) or not re.fullmatch(r'[0-9a-f]{64}',request['authorizationReference'])):
        sys.exit(3)
    try:
        requested_at=datetime.datetime.fromisoformat(request['recordedAt'].replace('Z','+00:00'))
        now=datetime.datetime.now(datetime.timezone.utc)
        if (not request['recordedAt'].endswith('Z') or requested_at.utcoffset()!=datetime.timedelta()
                or not datetime.timedelta()<=now-requested_at<=datetime.timedelta(seconds=manifest['deadlines']['operationSeconds'])):
            sys.exit(3)
    except (ValueError,KeyError,TypeError,AttributeError):
        sys.exit(2)
    lock_path=attempt/'formal-revoke.lock'
    if lock_path.exists():
        read_task_file(lock_path,root/'evidence',1024)
    lock=os.open(lock_path,os.O_RDWR|os.O_CREAT|os.O_NOFOLLOW,0o600)
    children=[]
    import signal
    previous_handlers={value:signal.getsignal(value) for value in (signal.SIGTERM,signal.SIGINT)}
    def cancel(signum,frame):
        sys.exit(9)
    for value in previous_handlers:
        signal.signal(value,cancel)
    try:
        try:
            fcntl.flock(lock,fcntl.LOCK_EX|fcntl.LOCK_NB)
        except OSError:
            sys.exit(5)
        truststore=ensure_task_truststore(manifest,digest)
        command=verification_runner_command(manifest_file,manifest,digest,attempt,'formal-revoke',truststore,token_hash)
        classname='top.egon.cola.platform.tianquan.shoubing.admin.integration.IsolatedOAuthProvisioningIT'
        report=Path(manifest['repoRoot'])/command['module']/'target/surefire-reports'/('TEST-'+classname+'.xml')
        before=None
        if report.exists():
            info=report.stat()
            before=(info.st_dev,info.st_ino,info.st_mtime_ns,info.st_size,task_artifact_sha256(report))
        if read_task_file(manifest_file)!=raw:
            sys.exit(3)
        entry=start_verification_process(manifest,digest,attempt,'formal-revoke',command,children)
        capture_verification_process(manifest,digest,attempt,entry)
        try:
            code=entry['process'].wait(timeout=manifest['deadlines']['operationSeconds'])
        except subprocess.TimeoutExpired:
            sys.exit(9)
        if code!=0:
            sys.exit(9 if (attempt/('formal-revocation-running-'+token_hash+'.json')).exists() else 8)
        proof=read_fresh_junit_report(report,classname,{'revokeFormalFamily'},entry['startedNs'],before)
        confirmed=decode_task_json(read_task_file(result_file,root/'evidence'))
        if (not isinstance(confirmed,dict) or confirmed.get('status')!='CONFIRMED' or confirmed.get('denyApplied') is not True
                or any(confirmed.get(key)!=request[key] for key in ('schemaVersion','runId','manifestSha256','baselineStatus',
                        'tokenSha256','issuer','clientPk','tenantId','resourceUri','authorizationReference'))):
            sys.exit(8)
        try:
            confirmed_at=datetime.datetime.fromisoformat(confirmed['recordedAt'].replace('Z','+00:00'))
            if not confirmed['recordedAt'].endswith('Z') or confirmed_at.utcoffset()!=datetime.timedelta() or confirmed_at<requested_at:
                sys.exit(8)
        except (ValueError,KeyError,TypeError,AttributeError):
            sys.exit(8)
        publish_task_json(attempt/'junit-formal-revoke.json',root,proof)
    finally:
        try:
            for entry in reversed(children):
                stop_verification_process(manifest,entry)
        finally:
            os.close(lock)
            for value,handler in previous_handlers.items():
                signal.signal(value,handler)


def revoke_service_access(argv):
    """Revoke one actual SERVICE token through the original owner; bearer bytes never leave either JVM."""
    import datetime
    import fcntl
    import hashlib
    import os
    import re
    import signal
    import stat
    import subprocess
    import sys
    from pathlib import Path

    manifest_file,result_file=Path(argv[1]),Path(argv[2]); token_hash=argv[3]
    if not re.fullmatch(r'[0-9a-f]{64}',token_hash): sys.exit(2)
    raw=read_task_file(manifest_file); manifest=decode_task_json(raw); digest=hashlib.sha256(raw).hexdigest()
    root,evidence=Path(manifest['runtimeRoot']),result_file.parent
    if (not result_file.is_absolute() or result_file!=Path(os.path.normpath(result_file)) or result_file.name!='service-revocation-'+token_hash+'.json'
            or evidence!=root/'evidence' and (evidence.parent!=root/'evidence' or not re.fullmatch(r'verify-[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}',evidence.name))
            or any(path.is_symlink() for path in (result_file,*result_file.parents))): sys.exit(3)
    metadata=evidence.lstat()
    if not stat.S_ISDIR(metadata.st_mode) or metadata.st_uid!=os.getuid() or stat.S_IMODE(metadata.st_mode)!=0o700: sys.exit(3)
    if result_file.exists(): sys.exit(5)
    request_file=evidence/('service-revocation-request-'+token_hash+'.json'); request_bytes=read_task_file(request_file,evidence)
    request=decode_task_json(request_bytes)
    if (not isinstance(request,dict) or type(request.get('schemaVersion')) is not int or request['schemaVersion']!=1 or request.get('status')!='READY'
            or request.get('runId')!=manifest['runId'] or request.get('manifestSha256')!=digest or request.get('tokenSha256')!=token_hash
            or request.get('baselineRpcStatus')!='OK' or request.get('consumerRole') not in {'native-web','native-service'}
            or request.get('providerRole') not in {'native-web','native-service'} or request['consumerRole']==request['providerRole']
            or not isinstance(request.get('authorizationId'),str) or not re.fullmatch(r'[1-9][0-9]{0,18}',request['authorizationId'])
            or int(request['authorizationId'])>9223372036854775807
            or hashlib.sha256(request['authorizationId'].encode('ascii')).hexdigest()!=request.get('authorizationReference')): sys.exit(3)
    try:
        recorded=datetime.datetime.fromisoformat(request['recordedAt'].replace('Z','+00:00')); expires=datetime.datetime.fromisoformat(request['expiresAt'].replace('Z','+00:00'))
        now=datetime.datetime.now(datetime.timezone.utc)
        if (not request['recordedAt'].endswith('Z') or not request['expiresAt'].endswith('Z') or recorded.utcoffset()!=datetime.timedelta()
                or expires.utcoffset()!=datetime.timedelta() or not datetime.timedelta()<=now-recorded<=datetime.timedelta(seconds=manifest['deadlines']['operationSeconds']) or expires<=now): sys.exit(3)
    except (KeyError,ValueError,TypeError,AttributeError): sys.exit(2)
    lock_file=evidence/'service-revoke.lock'
    if lock_file.exists(): read_task_file(lock_file,root/'evidence',1024)
    lock=os.open(lock_file,os.O_WRONLY|os.O_CREAT|os.O_NOFOLLOW,0o600); children=[]
    previous={value:signal.getsignal(value) for value in (signal.SIGTERM,signal.SIGINT)}
    def cancel(signum,frame): sys.exit(9)
    for value in previous: signal.signal(value,cancel)
    try:
        try: fcntl.flock(lock,fcntl.LOCK_EX|fcntl.LOCK_NB)
        except OSError: sys.exit(5)
        command=verification_runner_command(manifest_file,manifest,digest,evidence,'service-revoke',ensure_task_truststore(manifest,digest),token_hash)
        classname='top.egon.cola.platform.tianquan.shoubing.admin.integration.IsolatedOAuthProvisioningIT'
        report=Path(manifest['repoRoot'])/command['module']/'target/surefire-reports'/('TEST-'+classname+'.xml'); before=None
        if report.exists():
            info=report.stat(); before=(info.st_dev,info.st_ino,info.st_mtime_ns,info.st_size,task_artifact_sha256(report))
        if read_task_file(manifest_file)!=raw or read_task_file(request_file,evidence)!=request_bytes: sys.exit(3)
        entry=start_verification_process(manifest,digest,evidence,'service-revoke-'+token_hash,command,children); capture_verification_process(manifest,digest,evidence,entry)
        try: code=entry['process'].wait(timeout=manifest['deadlines']['operationSeconds'])
        except subprocess.TimeoutExpired: sys.exit(9)
        if code!=0: sys.exit(9 if (evidence/('service-revocation-running-'+token_hash+'.json')).exists() else 8)
        execution=read_fresh_junit_report(report,classname,{'revokeServiceAccessToken'},entry['startedNs'],before)
        confirmed=decode_task_json(read_task_file(result_file,evidence))
        if (not isinstance(confirmed,dict) or confirmed.get('status')!='CONFIRMED' or confirmed.get('denyApplied') is not True
                or any(confirmed.get(key)!=value for key,value in request.items() if key not in {'status','recordedAt','denyApplied'})): sys.exit(8)
        completed=datetime.datetime.fromisoformat(confirmed['recordedAt'].replace('Z','+00:00'))
        if not confirmed['recordedAt'].endswith('Z') or completed<recorded or completed>=expires: sys.exit(8)
        publish_task_json(evidence/('junit-service-revoke-'+token_hash+'.json'),root,execution)
    finally:
        try:
            for entry in reversed(children): stop_verification_process(manifest,entry)
        finally:
            os.close(lock)
            for value,handler in previous.items(): signal.signal(value,handler)


def validate_mcp_task_review(manifest, review):
    import re
    import sys
    import uuid

    fields = {'schemaVersion','runId','processId','tenantKeys','applicationCode','businessCode','permissionCode','resourceCode','roleCode','userKey','approved'}
    run_id = manifest['runId']
    if (not isinstance(review,dict) or set(review) != fields or str(uuid.UUID(run_id)) != run_id
            or type(review.get('schemaVersion')) is not int or review['schemaVersion'] != 1 or review.get('runId') != run_id
            or review.get('processId') != 'mcp' or review.get('tenantKeys') != ['default','tenant-b'] or review.get('applicationCode') != 'yuheng-admin'
            or not isinstance(review.get('businessCode'),str) or not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9._-]{0,127}',review['businessCode'])
            or review.get('permissionCode') != 'mcp:xingyuan-it-'+run_id+':resource:scopes:read'
            or review.get('resourceCode') != 'yuheng-admin.mcp-task-'+run_id.replace('-','')
            or review.get('roleCode') != 'XINGYUAN_MCP_'+run_id.replace('-','').upper()+'_ADMIN'
            or review.get('userKey') != 'admin' or review.get('approved') is not True):
        sys.exit(3)
    return review


def prepare_mcp_authorization(argv):
    """Fixed task-only authority owner; no arbitrary permission, selector, Bean or SQL input."""
    import datetime
    import fcntl
    import hashlib
    import os
    import re
    import signal
    import subprocess
    import sys
    from pathlib import Path

    if os.environ.get('XINGYUAN_ISOLATED_INTEGRATION') != 'true':
        sys.exit(4)
    manifest_file, result_file = map(Path, argv[1:3])
    raw = read_task_file(manifest_file); manifest = decode_task_json(raw); digest = hashlib.sha256(raw).hexdigest()
    root, evidence = Path(manifest['runtimeRoot']), result_file.parent
    if (result_file != evidence/'mcp-authorization-execution.json' or not result_file.is_absolute()
            or evidence != root/'evidence' and (evidence.parent != root/'evidence' or not re.fullmatch(r'verify-[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}',evidence.name))
            or result_file != Path(os.path.normpath(result_file)) or any(path.is_symlink() for path in (result_file,*result_file.parents))):
        sys.exit(3)
    record_raw = read_task_file(root/'evidence/processes.json',root/'evidence'); record = decode_task_json(record_raw)
    validate_owned_process_record(manifest,digest,record)
    selected = [value for value in record['processes'] if value['id'] in {'shoubing','jianshen','tianshu','yuheng','mcp'}]
    if len(selected) != 5 or any(observe_owned_process(manifest,value) != 'RUNNING' for value in selected):
        sys.exit(6)
    publication_file = evidence/'mcp-publication.json'; publication_raw = read_task_file(publication_file,evidence)
    publication = decode_task_json(publication_raw)
    if (publication.get('runId') != manifest['runId'] or publication.get('manifestSha256') != digest or publication.get('status') != 'COMMITTED'
            or publication.get('scope') != 'ORIGINAL_TASK_MCP_OWNER_PUBLICATION_ONLY' or publication.get('serverCode') != 'xingyuan-it-'+manifest['runId']
            or publication.get('rbacApplicationCode') != 'yuheng-admin'):
        sys.exit(3)
    review_file = root/'policies/mcp-authorization.json'; review_raw = read_task_file(review_file,root/'policies')
    refs = [value for value in manifest['policyFiles'] if value['path'] == str(review_file)]
    if len(refs) != 1 or hashlib.sha256(review_raw).hexdigest() != refs[0]['sha256']:
        sys.exit(3)
    validate_mcp_task_review(manifest,decode_task_json(review_raw))
    proof_file = evidence/'mcp-authorization.json'
    if any(value.exists() for value in (result_file,proof_file,evidence/'mcp-authorization-running.json')):
        sys.exit(9)
    source_path = Path(manifest['repoRoot'])/'egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/test/java/top/egon/cola/platform/tianquan/jianshen/admin/integration/IsolatedAuthorizationProvisioningIT.java'
    source_sha = task_artifact_sha256(source_path); children=[]
    lock_path = evidence/'mcp-owner-execution.lock'
    if lock_path.exists():
        read_task_file(lock_path,evidence,1024)
    lock = os.open(lock_path,os.O_WRONLY|os.O_CREAT|os.O_NOFOLLOW,0o600)
    handlers = {value:signal.getsignal(value) for value in (signal.SIGTERM,signal.SIGINT)}
    def cancel(signum,frame):
        sys.exit(9)
    try:
        for value in handlers:
            signal.signal(value,cancel)
        try:
            fcntl.flock(lock,fcntl.LOCK_EX|fcntl.LOCK_NB)
        except OSError:
            sys.exit(5)
        command = verification_runner_command(manifest_file,manifest,digest,evidence,'mcp-owner',ensure_task_truststore(manifest,digest))
        classname = 'top.egon.cola.platform.tianquan.jianshen.admin.integration.IsolatedAuthorizationProvisioningIT'
        report = Path(manifest['repoRoot'])/command['module']/'target/surefire-reports'/('TEST-'+classname+'.xml'); previous=None
        if report.exists():
            info=report.stat(); previous=(info.st_dev,info.st_ino,info.st_mtime_ns,info.st_size,task_artifact_sha256(report))
        entry = start_verification_process(manifest,digest,evidence,'mcp-owner',command,children); capture_verification_process(manifest,digest,evidence,entry)
        try:
            code=entry['process'].wait(timeout=manifest['deadlines']['operationSeconds'])
        except subprocess.TimeoutExpired:
            sys.exit(9)
        if code != 0:
            sys.exit(9 if (evidence/'mcp-authorization-running.json').exists() else 8)
        execution=read_fresh_junit_report(report,classname,{'provisionTaskMcpAuthorization'},entry['startedNs'],previous)
        proof_raw=read_task_file(proof_file,evidence); proof=decode_task_json(proof_raw)
        if (proof.get('schemaVersion') != 1 or type(proof.get('schemaVersion')) is not int or proof.get('runId') != manifest['runId'] or proof.get('manifestSha256') != digest
                or proof.get('status') != 'COMMITTED' or proof.get('scope') != 'MCP_TASK_AUTHORIZATION_OWNER_ONLY'
                or proof.get('publicationSha256') != hashlib.sha256(publication_raw).hexdigest() or proof.get('reviewSha256') != hashlib.sha256(review_raw).hexdigest()
                or proof.get('permissionCode') != 'mcp:xingyuan-it-'+manifest['runId']+':resource:scopes:read'
                or not isinstance(proof.get('tenants'),list) or len(proof['tenants']) != 2 or any(value.get('viewerGranted') is not False for value in proof['tenants'])
                or read_task_file(manifest_file) != raw or read_task_file(publication_file,evidence) != publication_raw or read_task_file(review_file,root/'policies') != review_raw
                or task_artifact_sha256(source_path) != source_sha or read_task_file(root/'evidence/processes.json',root/'evidence') != record_raw):
            sys.exit(8)
        publish_task_json(result_file,root,{'schemaVersion':1,'runId':manifest['runId'],'manifestSha256':digest,'status':'PASSED',
                'scope':'MCP_TASK_AUTHORIZATION_OWNER_EXECUTION_ONLY','sourceSha256':source_sha,'execution':execution,
                'proof':{'path':str(proof_file.relative_to(root)),'sha256':hashlib.sha256(proof_raw).hexdigest()},
                'recordedAt':datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00','Z')})
    finally:
        try:
            for entry in reversed(children):
                stop_verification_process(manifest,entry)
        finally:
            os.close(lock)
            for value,handler in handlers.items():
                signal.signal(value,handler)


def observe_platform_http_audits(argv):
    observe_http_audits(argv, 'platform-http-audit', 'resource-platform-http-boundaries.json',
                        'FOUR_PLATFORM_DIRECT_HTTP_AUTHENTICATION_AND_COUNTERS', 8, 'observePlatformHttpAudits',
                        'protocol-platform-http-audits.json', 'FOUR_PLATFORM_HTTP_PG_STREAM_SIGNED_RECEIPTS_ONLY',
                        'PLATFORM_HTTP_AUDIT_OWNER_EXECUTION_ONLY', 80, 96)


def observe_mcp_audits(argv):
    observe_http_audits(argv, 'mcp-audit', 'resource-mcp-boundaries.json',
                        'TASK_MCP_REAL_RESOURCE_READ_SCOPE_AND_FINE_PERMISSION_ONLY', 2, 'observeMcpHttpAudits',
                        'protocol-mcp-audits.json', 'MCP_HTTP_PG_STREAM_SIGNED_RECEIPTS_ONLY',
                        'MCP_AUDIT_OWNER_EXECUTION_ONLY', 12, 20)


def observe_native_userinfo_audits(argv):
    observe_http_audits(argv, 'native-userinfo-audit', 'resource-native-userinfo.json',
                        'NATIVE_USERINFO_HTTP_REQUESTS_ONLY', 2, 'observeNativeUserInfoAudits',
                        'protocol-native-userinfo-audits.json', 'NATIVE_USERINFO_PG_STREAM_SIGNED_RECEIPTS_ONLY',
                        'NATIVE_USERINFO_AUDIT_OWNER_EXECUTION_ONLY', 6, 6)


def observe_native_rpc_audits(argv):
    observe_http_audits(argv, 'native-rpc-audit', 'native-rpc-contract-probe.json',
                        'STANDALONE_NOT_FOUND_PROTOCOL', 4, 'observeNativeRpcAudits',
                        'protocol-native-rpc-audits.json', 'NATIVE_RPC_PG_STREAM_SIGNED_RECEIPTS_ONLY',
                        'NATIVE_RPC_AUDIT_OWNER_EXECUTION_ONLY', 16, 20, 'proofs')


def observe_http_audits(argv, stage, source_name, source_scope, observation_count, method, proof_name, proof_scope,
                        execution_scope, request_count, event_count, source_field='observations'):
    """Private read-only owner bridge; its execution is never a full LIVE case receipt."""
    import datetime
    import fcntl
    import hashlib
    import os
    import re
    import signal
    import stat
    import subprocess
    import sys
    from pathlib import Path

    manifest_file, result_file = Path(argv[1]), Path(argv[2])
    raw = read_task_file(manifest_file); manifest = decode_task_json(raw); digest = hashlib.sha256(raw).hexdigest()
    root, evidence = Path(manifest['runtimeRoot']), result_file.parent
    if (not result_file.is_absolute() or result_file != Path(os.path.normpath(result_file)) or result_file.name != stage + '-owner.json'
            or evidence != root/'evidence' and (evidence.parent != root/'evidence' or not re.fullmatch(r'verify-[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}',evidence.name))
            or any(path.is_symlink() for path in (result_file,*result_file.parents))):
        sys.exit(3)
    metadata = evidence.lstat()
    if not stat.S_ISDIR(metadata.st_mode) or metadata.st_uid != os.getuid() or stat.S_IMODE(metadata.st_mode) != 0o700:
        sys.exit(3)
    proof_file = evidence/proof_name
    if result_file.exists() or proof_file.exists():
        sys.exit(5)
    source_file = evidence/source_name; source = read_task_file(source_file,evidence)
    source_hash = hashlib.sha256(source).hexdigest(); request = decode_task_json(source)
    if (not isinstance(request,dict) or type(request.get('schemaVersion')) is not int or request['schemaVersion'] != 1
            or request.get('runId') != manifest['runId'] or request.get('manifestSha256') != digest or request.get('status') != 'PASSED'
            or request.get('scope') != source_scope
            or not isinstance(request.get(source_field),list) or len(request[source_field]) != observation_count):
        sys.exit(3)
    source_path = Path(manifest['repoRoot'])/'egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/integration/IsolatedOAuthProvisioningIT.java'
    source_sha = task_artifact_sha256(source_path)
    lock_path = evidence/(stage + '.lock')
    if lock_path.exists():
        read_task_file(lock_path,root/'evidence',1024)
    lock = os.open(lock_path,os.O_WRONLY|os.O_CREAT|os.O_NOFOLLOW,0o600); children=[]
    previous_handlers = {value:signal.getsignal(value) for value in (signal.SIGTERM,signal.SIGINT)}
    def cancel(signum,frame):
        sys.exit(8)
    for value in previous_handlers:
        signal.signal(value,cancel)
    try:
        try:
            fcntl.flock(lock,fcntl.LOCK_EX|fcntl.LOCK_NB)
        except OSError:
            sys.exit(5)
        command = verification_runner_command(manifest_file,manifest,digest,evidence,stage,ensure_task_truststore(manifest,digest))
        classname = 'top.egon.cola.platform.tianquan.shoubing.admin.integration.IsolatedOAuthProvisioningIT'
        report = Path(manifest['repoRoot'])/command['module']/'target/surefire-reports'/('TEST-'+classname+'.xml')
        before = None
        if report.exists():
            info=report.stat(); before=(info.st_dev,info.st_ino,info.st_mtime_ns,info.st_size,task_artifact_sha256(report))
        if read_task_file(manifest_file)!=raw or read_task_file(source_file,evidence)!=source or task_artifact_sha256(source_path)!=source_sha:
            sys.exit(3)
        entry = start_verification_process(manifest,digest,evidence,stage,command,children)
        capture_verification_process(manifest,digest,evidence,entry)
        try:
            code = entry['process'].wait(timeout=manifest['deadlines']['operationSeconds'])
        except subprocess.TimeoutExpired:
            sys.exit(6)
        if code != 0:
            sys.exit(8)
        execution = read_fresh_junit_report(report,classname,{method},entry['startedNs'],before)
        content = read_task_file(proof_file,evidence); proof = decode_task_json(content)
        if (not isinstance(proof,dict) or type(proof.get('schemaVersion')) is not int or proof['schemaVersion']!=1
                or proof.get('runId')!=manifest['runId'] or proof.get('manifestSha256')!=digest or proof.get('status')!='PASSED'
                or proof.get('scope')!=proof_scope or proof.get('sourceSha256')!=source_hash
                or type(proof.get('requestCount')) is not int or proof['requestCount']!=request_count
                or type(proof.get('storedEventCount')) is not int or proof['storedEventCount']!=event_count
                or type(proof.get('spoolAbsentAfterReceiptCount')) is not int or proof['spoolAbsentAfterReceiptCount']!=event_count
                or not isinstance(proof.get('proofs'),list) or len(proof['proofs'])!=request_count):
            sys.exit(8)
        if read_task_file(manifest_file)!=raw or read_task_file(source_file,evidence)!=source or task_artifact_sha256(source_path)!=source_sha:
            sys.exit(3)
        publish_task_json(result_file,root,{'schemaVersion':1,'runId':manifest['runId'],'manifestSha256':digest,'status':'PASSED',
                'scope':execution_scope,'sourceSha256':source_hash,
                'proof':{'path':str(proof_file.relative_to(root)),'sha256':hashlib.sha256(content).hexdigest()},'execution':execution,
                'recordedAt':datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00','Z')})
    finally:
        try:
            for entry in reversed(children):
                stop_verification_process(manifest,entry)
        finally:
            os.close(lock)
            for value,handler in previous_handlers.items():
                signal.signal(value,handler)


def read_fresh_junit_report(path, classname, required_methods, started_ns, previous=None):
    """Require fresh exact JUnit executions, keeping raw XML/output out of the public result."""
    import hashlib
    import os
    import re
    import stat
    import sys
    import xml.etree.ElementTree as xml
    from pathlib import Path

    path = Path(path)
    if (not path.is_absolute() or '..' in path.parts or any(parent.is_symlink() for parent in (path,*path.parents))
            or not isinstance(classname,str) or not re.fullmatch(r'[A-Za-z_$][A-Za-z0-9_$.]{0,511}',classname)
            or type(started_ns) is not int or started_ns <= 0 or not isinstance(required_methods,(set,frozenset))
            or not 1 <= len(required_methods) <= 128 or any(not isinstance(name,str) or not re.fullmatch(r'[A-Za-z_$][A-Za-z0-9_$]{0,255}',name) for name in required_methods)):
        sys.exit(3)
    def identity(value):
        return (value.st_dev,value.st_ino,value.st_mtime_ns,value.st_size)
    try:
        before = path.lstat()
        if (not stat.S_ISREG(before.st_mode) or before.st_uid != os.getuid() or stat.S_IMODE(before.st_mode) & 0o022
                or before.st_size > 4 * 1024 * 1024):
            sys.exit(8)
        with os.fdopen(os.open(path,os.O_RDONLY | os.O_NOFOLLOW),'rb') as source:
            opened = os.fstat(source.fileno())
            content = source.read(4 * 1024 * 1024 + 1)
        after = path.lstat()
        fingerprint = (*identity(after),hashlib.sha256(content).hexdigest())
        if (len(content) > 4 * 1024 * 1024 or identity(before) != identity(opened) or identity(before) != identity(after)
                or after.st_mtime_ns < started_ns or fingerprint == previous):
            sys.exit(8)
        text = content.decode('utf-8-sig')
        if '<!DOCTYPE' in text.upper() or '<!ENTITY' in text.upper():
            sys.exit(8)
        suite = xml.fromstring(text)
    except (OSError,UnicodeError,xml.ParseError):
        sys.exit(8)
    if suite.tag != 'testsuite' or suite.get('name') != classname:
        sys.exit(8)
    for key in ('tests','failures','errors','skipped'):
        if not re.fullmatch(r'[0-9]{1,5}',suite.get(key,'')):
            sys.exit(8)
    if (int(suite.get('tests')) != len(required_methods)
            or any(int(suite.get(key)) for key in ('failures','errors','skipped'))):
        sys.exit(8)
    cases = suite.findall('testcase')
    if len(cases) != len(required_methods):
        sys.exit(8)
    observed = set()
    for case in cases:
        name = case.get('name')
        if (case.get('classname') != classname or name not in required_methods or name in observed
                or any(case.find(kind) is not None for kind in ('skipped','failure','error'))):
            sys.exit(8)
        observed.add(name)
    if observed != required_methods:
        sys.exit(8)
    return {'classname':classname,'methods':sorted(observed),'testCount':len(observed),
            'reportSha256':fingerprint[-1],'mtimeNs':after.st_mtime_ns,'startedNs':started_ns}


def read_live_case_evidence(manifest,digest,attempt,case_id,runner,executions):
    """Link a case's private proof files to this attempt and an actually executed fixed test method."""
    import datetime
    import hashlib
    import re
    import sys
    from pathlib import Path

    root,attempt=Path(manifest['runtimeRoot']),Path(attempt)
    if attempt.parent!=root/'evidence' or not re.fullmatch(r'verify-[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}',attempt.name):
        sys.exit(3)
    if not re.fullmatch(r'LIVE-(00[1-9]|01[0-9]|02[0-4])',case_id) or runner not in {'resource','native','browser','oracle','outbox','lifecycle'}:
        sys.exit(2)
    file=attempt/'cases'/runner/(case_id+'.json')
    raw=read_task_file(file,attempt)
    case=decode_task_json(raw)
    fields={'schemaVersion','runId','manifestSha256','caseId','runner','status','executedAt','testClass','testMethod','proofs'}
    if not isinstance(case,dict):
        sys.exit(8)
    deferred=case.get('status')=='UNVERIFIED'
    if deferred:
        fields.add('deferred')
    if set(case)!=fields:
        sys.exit(8)
    if (type(case['schemaVersion']) is not int or case['schemaVersion']!=1 or case['runId']!=manifest['runId']
            or case['manifestSha256']!=digest or case['caseId']!=case_id or case['runner']!=runner
            or case['status'] not in {'PASSED','UNVERIFIED'}):
        sys.exit(8)
    if deferred and (case_id!='LIVE-015' or runner!='native' or case['deferred']!={
            'reasonCode':'AGENT_RUNTIME_CONFIGURATION_NOT_PROVIDED','acceptedOn':'2026-10-15','scope':'AGENT_RUNTIME_ONLY',
            'sourceBuildCase':'LIVE-012','preservedObligations':['SOURCE_INTEGRATION','SIX_LAYER_ARCHITECTURE','SECURITY_DEFAULT_TESTS','GENERATED_BUILD']}):
        sys.exit(8)
    execution=executions.get(runner)
    if (not isinstance(execution,dict) or execution.get('classname')!=case['testClass'] or case['testMethod'] not in execution.get('methods',[])
            or type(execution.get('testCount')) is not int or execution['testCount']<=0
            or type(execution.get('startedNs')) is not int or execution['startedNs']<=0 or file.stat().st_mtime_ns<execution['startedNs']
            or not isinstance(execution.get('reportSha256'),str) or not re.fullmatch(r'[0-9a-f]{64}',execution['reportSha256'])):
        sys.exit(8)
    try:
        completed=datetime.datetime.fromisoformat(case['executedAt'].replace('Z','+00:00'))
        if (not case['executedAt'].endswith('Z') or completed.tzinfo is None or completed.utcoffset()!=datetime.timedelta()
                or completed.timestamp()*1_000_000_000<execution['startedNs'] or completed>datetime.datetime.now(datetime.timezone.utc)+datetime.timedelta(seconds=5)):
            sys.exit(8)
    except (ValueError,TypeError,AttributeError):
        sys.exit(8)
    proofs=case['proofs']
    if not isinstance(proofs,list) or not 1<=len(proofs)<=32:
        sys.exit(8)
    seen=set()
    for proof in proofs:
        if not isinstance(proof,dict) or set(proof)!={'path','sha256'} or not isinstance(proof['path'],str) or not isinstance(proof['sha256'],str):
            sys.exit(8)
        reference=Path(proof['path'])
        if (reference.is_absolute() or '..' in reference.parts or str(reference)!=proof['path'] or proof['path'] in seen
                or not re.fullmatch(r'[0-9a-f]{64}',proof['sha256'])):
            sys.exit(8)
        target=root/reference
        if not target.is_relative_to(attempt) or target==file:
            sys.exit(3)
        content=read_task_file(target,attempt)
        if not content or hashlib.sha256(content).hexdigest()!=proof['sha256']:
            sys.exit(8)
        seen.add(proof['path'])
    if deferred:
        # The accepted missing runtime inputs do not waive the generated Agent's source/build gate.
        source_build=read_live_case_evidence(manifest,digest,attempt,'LIVE-012','native',executions)
        if {'path':source_build['evidence'],'sha256':source_build['caseSha256']} not in proofs:
            sys.exit(8)
    observed={'caseId':case_id,'runner':runner,'status':case['status'],'caseSha256':hashlib.sha256(raw).hexdigest(),'proofCount':len(proofs),
              'executionSha256':execution['reportSha256'],'evidence':str(file.relative_to(root))}
    if deferred:
        observed['deferred']=case['deferred']
    return observed


def verification_time_plan(manifest, digest, registry):
    """Expose the real sequential BFF wait floor without changing any Client lifetime."""
    import sys
    operation=manifest['deadlines']['operationSeconds']
    if (not isinstance(registry,dict) or type(registry.get('schemaVersion')) is not int or registry['schemaVersion']!=1
            or registry.get('runId')!=manifest['runId'] or registry.get('manifestSha256')!=digest):
        sys.exit(3)
    if (type(operation) is not int or not 1<=operation<=900 or not isinstance(registry.get('clients'),list)
            or len(registry['clients'])>192 or any(not isinstance(value,dict) for value in registry['clients'])):
        sys.exit(2)
    waits={}
    aliases=('edge-shoubing','edge-jianshen','edge-yuheng','edge-tianshu')
    for realm in ('default','tenant-b'):
        lifetimes=[]
        for alias in aliases:
            selected=[value for value in registry['clients'] if value.get('realmCode')==realm and value.get('alias')==alias]
            if len(selected)!=1:
                sys.exit(3)
            lifetime=selected[0].get('accessTokenTtlSeconds')
            if type(lifetime) is not int or not 300<=lifetime<=600:
                sys.exit(2)
            lifetimes.append(lifetime)
        waits[realm]=max(lifetimes)-60+1
    if sum(waits.values()) + 90 >= operation:
        sys.exit(2)
    return {'operationSeconds':operation,'bffRefreshWaitSeconds':sum(waits.values()),'realmRefreshWaitSeconds':waits,'taskFaultRecoverySeconds':90}


def verify_full_integration(argv):
    import datetime
    import fcntl
    import hashlib
    import os
    import re
    import sys
    import time
    import uuid
    from pathlib import Path

    manifest_file,result_file,token_index=map(Path,argv[1:4])
    if argv[4]!='full':
        sys.exit(2)
    manifest_bytes=read_task_file(manifest_file)
    manifest=decode_task_json(manifest_bytes)
    digest=hashlib.sha256(manifest_bytes).hexdigest()
    root,repository=Path(manifest['runtimeRoot']),Path(manifest['repoRoot'])
    if result_file.exists():
        sys.exit(5)
    specs={
        'oracle':('egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin',
                  'top.egon.cola.platform.tianquan.shoubing.admin.integration.IsolatedOAuthProvisioningIT',{'observeBffFamilies'}),
        'outbox':('egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin',
                  'top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.OutboxTransactionRollbackIT',
                  {'jpaRoleResourceAndMpOutboxRollback','jpaRoleResourceAndMpOutboxAppendFault','jpaRoleResourceAndMpOutboxCommit'}),
        'field-owner':('egon-cola-xingyuan/egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin',
                       'top.egon.cola.platform.tianquan.jianshen.admin.integration.IsolatedAuthorizationProvisioningIT',{'observeNativeFieldRules'}),
        'field-consumer':('egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-suite',
                          'top.egon.cola.component.yuheng.test.integration.XingyuanFieldAuthorizationIT',{'nativeFieldRulesControlWritesAcrossRealOwnerEpochsAndTenants'}),
        'native':('egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-suite',
                  'top.egon.cola.component.yuheng.test.integration.XingyuanNativeAuthenticationIT',{'verifyNativeAuthentication'}),
        'resource':('egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-suite',
                    'top.egon.cola.component.yuheng.test.integration.XingyuanResourceAuthorizationIT',{'verifyResourceAuthorization'}),
        'service-cache':('egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-suite',
                    'top.egon.cola.component.yuheng.test.integration.XingyuanNativeAuthenticationIT',{'configuredNativeServiceClientsRenewThroughTheRealAsWithSingleFlight'})}
    source_hashes={}
    missing_sources=[]
    for stage,(module,classname,methods) in specs.items():
        source=repository/module/'src/test/java'/Path(*classname.split('.')).with_suffix('.java')
        if not source.is_file():
            missing_sources.append(stage)
            continue
        text=source.read_text(encoding='utf-8')
        if any(not re.search(r'\b'+method+r'\s*\(',text) for method in methods):
            missing_sources.append(stage)
            continue
        source_hashes[str(source)]=task_artifact_sha256(source)
    browser_source=repository/'egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/e2e/integration/xingyuan-identity-integration.spec.ts'
    browser_text=browser_source.read_text(encoding='utf-8') if browser_source.is_file() else ''
    titles=re.findall(r"test\('([^']*LIVE-[^']*)'",browser_text)
    if len(titles)!=1:
        missing_sources.append('browser')
    else:
        source_hashes[str(browser_source)]=task_artifact_sha256(browser_source)
    for relative in ('scripts/unified-xingyuan/lib/isolated_integration.py',
                     'egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/test/java/top/egon/cola/platform/tianquan/shoubing/admin/integration/XingyuanRuntimeProbeInitializer.java',
                     'egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web/playwright.integration.config.ts',
                     'egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-suite/src/test/java/top/egon/cola/component/yuheng/test/tianquan/shoubing/UnifiedIdentityLiveClient.java'):
        source=repository/relative
        if not source.is_file():
            missing_sources.append('shared-source')
            continue
        source_hashes[str(source)]=task_artifact_sha256(source)
    if missing_sources:
        publish_task_json(result_file,root,{'schemaVersion':1,'operationId':'CLI-007','runId':manifest['runId'],'manifestSha256':digest,
                          'status':'FAILED','completedAt':datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00','Z'),
                          'targets':[],'counts':{'databaseCount':0,'tenantCount':0,'clientCount':0,'catalogCount':0,'liveCaseCount':0},'steps':[],
                          'error':{'code':'XINGYUAN_INPUT_UNAVAILABLE','message':'Required verification source is missing: '+', '.join(dict.fromkeys(missing_sources)),
                                   'retryable':False,'stage':'SOURCE_READINESS'}})
        sys.exit(4)
    if not validate_task_nginx_configuration(manifest,digest):
        sys.exit(4)
    if read_task_file(root/'nginx/revocation-fault.map',root/'nginx',4096) != task_revocation_fault_contents(manifest,digest,False):
        sys.exit(5)
    for filename,operation,state,step in (('prepared.json','CLI-001','COMMITTED',None),('identity.json','CLI-002','COMMITTED',None),
            ('control-plane.json','CLI-003','RUNNING','CONTROL_PLANE'),('authorization-platform.json','CLI-004','COMMITTED','PLATFORM_AUTHORIZATION'),
            ('authorization-native.json','CLI-004','COMMITTED','NATIVE_AUTHORIZATION'),('consumers.json','CLI-005','RUNNING','CONSUMERS')):
        require_operation_receipt(manifest,digest,filename,operation,state,step)
    formal_inputs=validate_formal_token_index(manifest,digest,token_index)
    if not isinstance(formal_inputs,dict) or not 1<=len(formal_inputs)<=128:
        sys.exit(4)
    registry_bytes=read_task_file(root/'secrets/runtime-registry.json',root/'secrets',2*1024*1024)
    time_plan=verification_time_plan(manifest,digest,decode_task_json(registry_bytes))
    original_baseline_bytes=read_task_file(root/'evidence/original-databases.json',root/'evidence')
    read_original_database_baseline(manifest,digest)
    original_nginx_bytes=read_task_file(root/'evidence/original-nginx.json',root/'evidence')
    read_original_nginx_baseline(manifest,digest)
    original_redis_bytes=read_task_file(root/'evidence/original-redis.json',root/'evidence')
    read_original_redis_baseline(manifest,digest)
    record_bytes=read_task_file(root/'evidence/processes.json',root/'evidence')
    record=decode_task_json(record_bytes)
    validate_owned_process_record(manifest,digest,record)
    exclusions=runtime_profile_exclusions(manifest)
    expected_processes={item['id'] for item in manifest['processes']} - set(exclusions)
    if {item['id'] for item in record['processes']}!=expected_processes or any(observe_owned_process(manifest,item)!='RUNNING' for item in record['processes']):
        sys.exit(6)
    lock_path=root/'evidence/verification.lock'
    if lock_path.exists():
        read_task_file(lock_path,root/'evidence',1024)
    lock_fd=os.open(lock_path,os.O_RDWR|os.O_CREAT|os.O_NOFOLLOW,0o600)
    try:
        fcntl.flock(lock_fd,fcntl.LOCK_EX|fcntl.LOCK_NB)
    except OSError:
        os.close(lock_fd)
        sys.exit(5)
    children,confirmed,executions=[],[],{}
    fresh_inputs=[]
    attempt=root/'evidence'/('verify-'+str(uuid.uuid4()))
    try:
        attempt.mkdir(mode=0o700)
        publish_task_json(attempt/'intent.json',root,{'schemaVersion':1,'runId':manifest['runId'],'manifestSha256':digest,
                          'caseSet':'full','sourceFiles':source_hashes,'registrySha256':hashlib.sha256(registry_bytes).hexdigest(),
                          'timePlan':time_plan,
                          'originalDatabaseBaselineSha256':hashlib.sha256(original_baseline_bytes).hexdigest(),
                          'originalNginxBaselineSha256':hashlib.sha256(original_nginx_bytes).hexdigest(),
                          'originalRedisBaselineSha256':hashlib.sha256(original_redis_bytes).hexdigest(),
                          'startedAt':datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00','Z')})
    except BaseException:
        os.close(lock_fd)
        raise
    deadline=time.monotonic()+manifest['deadlines']['operationSeconds']
    current_stage='VERIFY_INPUTS'

    def budget():
        remaining=int(deadline-time.monotonic())
        if remaining<1:
            if current_stage=='FORMAL_CODE_INPUTS' and sum(entry['stage'].startswith('formal-input-') for entry in children)>len(fresh_inputs):
                sys.exit(9)
            sys.exit(7)
        return dict(manifest,deadlines={key:min(value,remaining) for key,value in manifest['deadlines'].items()})

    def stable_sources():
        if read_task_file(manifest_file)!=manifest_bytes or runtime_profile_exclusions(manifest)!=exclusions:
            sys.exit(3)
        if read_task_file(root/'secrets/runtime-registry.json',root/'secrets',2*1024*1024)!=registry_bytes:
            sys.exit(3)
        if read_task_file(root/'evidence/original-databases.json',root/'evidence')!=original_baseline_bytes:
            sys.exit(3)
        if read_task_file(root/'evidence/original-nginx.json',root/'evidence')!=original_nginx_bytes:
            sys.exit(3)
        if read_task_file(root/'evidence/original-redis.json',root/'evidence')!=original_redis_bytes:
            sys.exit(3)
        if any(task_artifact_sha256(path)!=checksum for path,checksum in source_hashes.items()):
            sys.exit(3)

    def report_file(stage):
        module,classname,_=specs[stage]
        return repository/module/'target/surefire-reports'/('TEST-'+classname+'.xml')

    def snapshot_report(stage):
        path=report_file(stage)
        if not path.exists():
            return None
        info=path.stat()
        return (info.st_dev,info.st_ino,info.st_mtime_ns,info.st_size,task_artifact_sha256(path))

    def start(stage,truststore):
        stable_sources()
        command=verification_runner_command(manifest_file,budget(),digest,attempt,stage,truststore)
        before=snapshot_report(stage) if stage in specs else None
        entry=start_verification_process(budget(),digest,attempt,stage,command,children)
        entry['previousReport']=before
        capture_verification_process(budget(),digest,attempt,entry)
        return entry

    def await_exit(entries,cli_input=False):
        while True:
            budget()
            outcomes=[entry['process'].poll() for entry in entries]
            for code in outcomes:
                if code is not None and code!=0:
                    if cli_input and code<0:
                        sys.exit(9)
                    sys.exit(code if cli_input and code in (2,3,4,5,6,7,8,9,10) else 8)
            if all(code is not None for code in outcomes):
                return
            time.sleep(0.2)

    def marker(filename,status,phase=None,proof_count=None,entry=None):
        path=attempt/filename
        if not path.exists():
            return None
        value=decode_task_json(read_task_file(path,attempt))
        if (not isinstance(value,dict) or type(value.get('schemaVersion')) is not int or value['schemaVersion']!=1
                or value.get('runId')!=manifest['runId'] or value.get('manifestSha256')!=digest or value.get('status')!=status
                or phase is not None and value.get('phase')!=phase
                or proof_count is not None and (not isinstance(value.get('proofs'),list) or len(value['proofs'])!=proof_count)
                or entry is not None and path.stat().st_mtime_ns<entry['startedNs']):
            sys.exit(8)
        return value

    def capture_report(stage,entry):
        _,classname,methods=specs[stage]
        proof=read_fresh_junit_report(report_file(stage),classname,methods,entry['startedNs'],entry['previousReport'])
        executions[stage]=proof
        publish_task_json(attempt/('junit-'+stage+'.json'),root,proof)

    def result(status,error=None,publish=True):
        now=datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00','Z')
        value={'schemaVersion':1,'operationId':'CLI-007','runId':manifest['runId'],'manifestSha256':digest,'status':status,'completedAt':now,
               'targets':[{'targetKey':item['id'],'identity':str(item['pid']),'version':item['startInstant'],
                           'state':'UNREADY' if error is not None and status!='PARTIAL' else observe_owned_process(manifest,item)} for item in record['processes']],
               'counts':{'databaseCount':9,'tenantCount':2,'clientCount':0,'catalogCount':4,'liveCaseCount':sum(case['status']=='PASSED' for case in confirmed)},
               'steps':([{'stepId':'FORMAL_CODE_INPUTS','state':'PASSED','evidence':str((attempt/'formal-inputs.json').relative_to(root))}]
                        if (attempt/'formal-inputs.json').exists() and current_stage!='FORMAL_CODE_INPUTS' else [])
                       +[{'stepId':current_stage,'state':'PASSED' if error is None else 'PARTIAL' if status=='PARTIAL' else 'FAILED',
                          'evidence':str((attempt/('case-matrix.json' if current_stage=='FULL_MATRIX' else 'intent.json')).relative_to(root))}],
               'error':error}
        if publish:
            publish_task_json(result_file,root,value)
        return value

    try:
        truststore=ensure_task_truststore(budget(),digest)
        current_stage='SERVICE_CACHE_START'
        service_cache=start('service-cache',truststore)
        initial_cache=None
        while initial_cache is None:
            initial_cache=marker('native-service-cache-ready.json','READY','INITIAL_VERIFIED',entry=service_cache)
            if initial_cache is None:
                if service_cache['process'].poll() is not None:
                    sys.exit(8)
                budget()
                time.sleep(0.2)
        cache_count=initial_cache.get('requestCount')
        if type(cache_count) is not int or not 1<=cache_count<=192 or type(initial_cache.get('profileCount')) is not int or initial_cache['profileCount']!=4-len(exclusions):
            sys.exit(8)
        current_stage='BFF_ORACLE_START'
        oracle=start('oracle',truststore)
        while marker('oracle-READY.json','READY','READY',0,oracle) is None:
            if oracle['process'].poll() is not None:
                sys.exit(8)
            if service_cache['process'].poll() not in (None,0):
                sys.exit(8)
            budget()
            time.sleep(0.2)
        current_stage='BFF_BROWSER'
        browser=start('browser',truststore)
        await_exit([service_cache,oracle,browser])
        capture_report('service-cache',service_cache)
        completed_cache=marker('native-service-cache.json','PASSED',proof_count=cache_count,entry=service_cache)
        if (completed_cache is None or completed_cache.get('clock')!='SYSTEM_UTC'
                or {item.get('role') for item in completed_cache['proofs'] if isinstance(item,dict)}!=({'native-light','native-web','native-service','native-agent'}-set(exclusions))):
            sys.exit(8)
        capture_report('oracle',oracle)
        if marker('browser-AB_BROWSER.json','PASSED',entry=browser) is None:
            sys.exit(8)
        browser_execution=marker('browser-BROWSER_EXECUTION.json','PASSED',entry=browser)
        if (browser_execution is None or browser_execution.get('titleReference')!=hashlib.sha256(titles[0].encode()).hexdigest()
                or browser_execution.get('expectedStatus')!='passed' or browser_execution.get('actualStatus')!='passed'
                or browser_execution.get('retry')!=0 or type(browser_execution.get('executedTestCount')) is not int
                or browser_execution['executedTestCount']!=1):
            sys.exit(8)
        for realm in ('TENANT_A','TENANT_B'):
            for phase,status,count in (('REFRESH','PASSED',4),('STEP_UP','PASSED',1),('HOLD','READY',5),('REPLAY','PASSED',5)):
                if marker('oracle-'+realm+'_'+phase+'.json',status,realm+'_'+phase,count,oracle) is None:
                    sys.exit(8)
        for phase,status in (('HOLD','READY'),('REPLAY','PASSED')):
            if marker('oracle-TENANT_A_FAULT_'+phase+'.json',status,'TENANT_A_FAULT_'+phase,5,oracle) is None:
                sys.exit(8)
        pending = marker('browser-AS_REVOCATION_PENDING.json','PASSED',entry=browser)
        recovered = marker('browser-AS_REVOCATION_RECOVERED.json','PASSED',entry=browser)
        if (pending is None or pending.get('responseStatus') != 503 or pending.get('remoteState') != 'PENDING'
                or pending.get('localLoggedOut') is not True or type(pending.get('durableIntentCount')) is not int or pending['durableIntentCount'] != 5
                or pending.get('lateCallbackObserved') is not True or pending.get('lateCallbackSessionStatus') != 401
                or not isinstance(pending.get('proofs'),list) or len(pending['proofs']) != 3
                or recovered is None or type(recovered.get('queuedCount')) is not int or recovered['queuedCount'] != 0
                or type(recovered.get('pendingCount')) is not int or recovered['pendingCount'] != 0 or recovered.get('sessionStatus') != 401
                or not isinstance(recovered.get('proofs'),list) or len(recovered['proofs']) != 5):
            sys.exit(8)
        if marker('oracle-COMPLETE.json','PASSED','COMPLETE',0,oracle) is None:
            sys.exit(8)
        current_stage='FORMAL_CODE_INPUTS'
        if read_task_file(root/'evidence/processes.json',root/'evidence')!=record_bytes:
            sys.exit(3)
        if any(observe_owned_process(manifest,item)!='RUNNING' for item in record['processes']):
            sys.exit(6)
        progress_file=attempt/'formal-inputs-progress.json'
        previous_progress=None
        commands=[]
        for ordinal,identity in enumerate(sorted(formal_inputs)):
            stable_sources()
            command=formal_input_command(manifest_file,budget(),digest,attempt,ordinal,identity)
            commands.append((identity,command))
            entry=start_verification_process(budget(),digest,attempt,command['stage'],command,children)
            capture_verification_process(budget(),digest,attempt,entry)
            await_exit([entry],cli_input=True)
            fresh_inputs.append(read_formal_input_result(manifest,digest,command,formal_inputs[identity],entry['startedNs']))
            publish_task_json(progress_file,root,{'schemaVersion':1,'runId':manifest['runId'],'manifestSha256':digest,
                              'status':'RUNNING','expectedCount':len(formal_inputs),'completedCount':len(fresh_inputs),'inputs':fresh_inputs},previous_progress)
            previous_progress=read_task_file(progress_file,attempt)
        refreshed=validate_formal_token_index(manifest,digest,token_index)
        if set(refreshed)!=set(formal_inputs):
            sys.exit(5)
        for (identity,command),proof in zip(commands,fresh_inputs):
            if refreshed[identity]['tokenFile']!=command['output'] or refreshed[identity]['tokenSha256']!=proof['tokenReference']:
                sys.exit(5)
        stable_sources()
        if read_task_file(root/'evidence/processes.json',root/'evidence')!=record_bytes:
            sys.exit(3)
        if any(observe_owned_process(manifest,item)!='RUNNING' for item in record['processes']):
            sys.exit(6)
        publish_task_json(attempt/'formal-inputs.json',root,{'schemaVersion':1,'runId':manifest['runId'],'manifestSha256':digest,
                          'status':'COMMITTED','operationId':'CLI-006','completedAt':datetime.datetime.now(datetime.timezone.utc).isoformat().replace('+00:00','Z'),
                          'inputCount':len(fresh_inputs),'inputs':fresh_inputs})
        current_stage='PG_PRODUCER'
        outbox=start('outbox',truststore)
        await_exit([outbox])
        capture_report('outbox',outbox)
        current_stage='FIELD_RULE_OWNER'
        field_owner=start('field-owner',truststore)
        while marker('native-field-owner-ready.json','READY',entry=field_owner) is None:
            if field_owner['process'].poll() is not None:
                sys.exit(8)
            budget()
            time.sleep(0.2)
        current_stage='FIELD_RULE_CONSUMER'
        field_consumer=start('field-consumer',truststore)
        await_exit([field_consumer,field_owner])
        capture_report('field-owner',field_owner)
        capture_report('field-consumer',field_consumer)
        for stage in ('native','resource'):
            current_stage='NATIVE_MATRIX' if stage=='native' else 'RESOURCE_MATRIX'
            validate_formal_token_index(manifest,digest,token_index)
            entry=start(stage,truststore)
            await_exit([entry])
            capture_report(stage,entry)
        current_stage='CASE_EVIDENCE'
        for number in range(1,25):
            case_id='LIVE-'+str(number).zfill(3)
            runner='native' if 11<=number<=15 else 'resource'
            confirmed.append(read_live_case_evidence(manifest,digest,attempt,case_id,runner,executions))
        stable_sources()
        if read_task_file(root/'evidence/processes.json',root/'evidence')!=record_bytes:
            sys.exit(3)
        if any(observe_owned_process(manifest,item)!='STOPPED' for item in record['processes']):
            sys.exit(8)
        current_stage='ORIGINAL_DATABASE_PRESERVATION'
        verify_original_databases(budget(),digest,attempt)
        current_stage='ORIGINAL_NGINX_PRESERVATION'
        verify_original_nginx(budget(),digest,attempt)
        current_stage='ORIGINAL_REDIS_PRESERVATION'
        verify_original_redis(budget(),digest,attempt)
        stable_sources()
        current_stage='FULL_MATRIX'
        status='PARTIAL' if any(case['status']=='UNVERIFIED' for case in confirmed) else 'PASSED'
        publish_task_json(attempt/'case-matrix.json',root,{'schemaVersion':1,'runId':manifest['runId'],'manifestSha256':digest,
                          'status':status,'cases':confirmed,'executions':executions})
        error=({'code':'XINGYUAN_PARTIAL','message':'Agent runtime configuration was not provided; LIVE-015 remains unverified',
                'retryable':False,'stage':current_stage} if status=='PARTIAL' else None)
        value=result(status,error,publish=False)
        previous=read_task_file(root/'evidence/full.json',root/'evidence') if (root/'evidence/full.json').exists() else None
        if previous is not None:
            old=decode_task_json(previous)
            if old.get('runId')!=manifest['runId'] or old.get('manifestSha256')!=digest or old.get('operationId')!='CLI-007':
                sys.exit(5)
        publish_task_json(root/'evidence/full.json',root,value,previous)
        publish_task_json(result_file,root,value)
        print('CLI-007 '+status+' '+str(result_file.relative_to(root)))
        if status=='PARTIAL':
            sys.exit(7)
    except SystemExit as failure:
        codes={2:'XINGYUAN_INPUT_INVALID',3:'XINGYUAN_TARGET_UNSAFE',4:'XINGYUAN_INPUT_UNAVAILABLE',5:'XINGYUAN_STATE_CONFLICT',
               6:'XINGYUAN_DEPENDENCY_UNAVAILABLE',7:'XINGYUAN_PARTIAL',8:'XINGYUAN_VERIFICATION_FAILED',9:'XINGYUAN_COMMIT_UNKNOWN',10:'XINGYUAN_TARGET_UNSAFE'}
        if not result_file.exists():
            result('UNKNOWN_COMMIT' if failure.code==9 else 'FAILED',{'code':codes.get(failure.code,'XINGYUAN_VERIFICATION_FAILED'),
                   'message':'Isolated verification did not finish','retryable':False,'stage':current_stage})
        raise
    finally:
        try:
            for entry in reversed(children):
                if entry['process'].poll() is None:
                    stop_verification_process(manifest,entry)
        finally:
            os.close(lock_fd)


def main():
    import os
    import sys
    from pathlib import Path

    operations = {
        "validate": (validate_manifest, 5),
        "provision-identity": (provision_identity, 4),
        "prepare": (prepare, 3),
        "start-control-plane": (start_control_plane, 2),
        "start-consumers": (start_consumers, 3),
        "verify": (verify_full_integration, 4),
        "process-state": (process_state, 3),
        "authorize-cli": (authorize_cli, 7),
        "authorize-scope-probe": (authorize_scope_probe, 7),
        "authorization-platform": (provision_authorization_platform, 4),
        "authorization-native": (provision_authorization_native, 5),
        "formal-revoke": (revoke_formal_family, 4),
        "service-revoke": (revoke_service_access, 4),
        "revocation-fault": (set_task_revocation_fault, 3),
        "logout-queue-status": (observe_task_logout_queue, 2),
        "observe-platform-http-audits": (observe_platform_http_audits, 2),
        "prepare-mcp-authorization": (prepare_mcp_authorization, 2),
        "observe-mcp-audits": (observe_mcp_audits, 2),
        "observe-native-userinfo-audits": (observe_native_userinfo_audits, 2),
        "observe-native-rpc-audits": (observe_native_rpc_audits, 2),
        "native-build-provenance": (build_native_provenance, 3),
        "observe-native-build": (observe_native_build, 2),
        "owned-stop-preservation": (stop_owned_and_observe_preservation, 2),
        "observe-live-guards": (observe_live_guards, 2),
    }
    if len(sys.argv) < 2 or sys.argv[1] not in operations:
        sys.exit(2)
    mode = sys.argv[1]
    os.umask(0o077)
    function, count = operations[mode]
    arguments = sys.argv[2:]
    if len(arguments) != count:
        sys.exit(2)
    repository = Path(__file__).resolve().parents[3]
    if mode == "validate":
        if Path(arguments[1]) != repository:
            sys.exit(3)
        function([__file__, *arguments])
        return
    if mode == "native-build-provenance":
        function([__file__, *arguments])
        return
    if mode == "process-state" and arguments[0] not in ("status", "stop"):
        sys.exit(2)
    readonly = mode == "process-state" and arguments[0] == "status"
    if not readonly and os.environ.get("XINGYUAN_ISOLATED_INTEGRATION") != "true":
        sys.exit(3)
    if mode in ("provision-identity", "authorize-cli", "authorize-scope-probe", "authorization-platform", "authorization-native", "formal-revoke", "service-revoke") and Path(arguments[-1]) != repository:
        sys.exit(3)
    manifest = arguments[1] if mode == "process-state" else arguments[0]
    result = arguments[2] if mode == "process-state" else arguments[1]
    output = arguments[5] if mode in ("authorize-cli", "authorize-scope-probe") else ""
    token_index = arguments[3] if mode == "authorization-native" else arguments[2] if mode in ("start-consumers","verify") else ""
    validate_manifest([__file__, manifest, str(repository), result, token_index, output])
    function([__file__, *arguments])


if __name__ == "__main__":
    main()
