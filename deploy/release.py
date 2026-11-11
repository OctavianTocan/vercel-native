#!/usr/bin/env python3
"""Android version gate and fail-closed release publisher. Publication uses the pinned S3 SDK."""
import argparse
import datetime
import hashlib
import json
import os
from pathlib import Path
import re
import shlex
import shutil
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[1]
SITE = "https://mymind.octaviantocan.com"
HEADING = re.compile(r"## (\d+\.\d+\.\d+) \((\d+)\), (\d{4}-\d{2}-\d{2})")
DATED = re.compile(r"## (\d{4}-\d{2}-\d{2})")
CHANGELOG = "android/CHANGELOG.md"
CHANGES = "android/changes"


def fail(message):
    raise ValueError(message)


def history(text):
    entries = []
    for line in text.splitlines():
        line = line.rstrip()
        if line.startswith("## "):
            match = HEADING.fullmatch(line)
            if not match:
                fail(f"invalid changelog heading: {line}")
            name, code, date = match.groups()
            if not 0 < int(code) <= 2100000000:
                fail("versionCode must be between 1 and 2100000000")
            entries.append(dict(versionName=name, versionCode=int(code), date=date, notes=[]))
        elif line.startswith("- "):
            if not entries or not line[2:].strip():
                fail("changelog has an empty note or a note before its heading")
            entries[-1]["notes"].append(line[2:].strip())
    if not entries or any(not entry["notes"] for entry in entries):
        fail("changelog needs release entries with notes")
    for newer, older in zip(entries, entries[1:]):
        if newer["versionCode"] <= older["versionCode"] or version(newer["versionName"]) <= version(older["versionName"]):
            fail("changelog versionName and versionCode must both increase")
    return entries


def version(name):
    return tuple(map(int, name.split(".")))


def runtime_path(path):
    if not path.startswith("android/") or path.endswith("/CONTRACTS"):
        return False
    source = re.search(r"/src/([^/]+)/", path)
    runtime_source = source is not None and not source[1].lower().startswith(("test", "androidtest"))
    return (runtime_source or path.endswith((".gradle.kts", ".gradle"))
            or path in {"android/gradle.properties", "android/gradlew", "android/gradlew.bat"}
            or path.startswith("android/gradle/"))


def git(*args):
    return subprocess.run(["git", *args], cwd=ROOT, check=True, capture_output=True).stdout


def change(path, text):
    """Checks one android/changes file (`## YYYY-MM-DD`, then an entry's body) and returns its date and body lines."""
    lines = [line.rstrip() for line in text.splitlines()]
    match = DATED.fullmatch(lines[0]) if lines else None
    if not match:
        fail(f"{path}: the first line must be '## YYYY-MM-DD'; the version is given when the change reaches main")
    if any(line.startswith("## ") for line in lines[1:]):
        fail(f"{path}: one change file holds one entry")
    if not any(line.startswith("- ") and line[2:].strip() for line in lines[1:]):
        fail(f"{path}: the entry needs a '- ' note")
    return match[1], lines[1:]


def sources(rev=None):
    """The changelog text and every change file ({path: text}) of a revision, or of the working tree for None."""
    if rev is None:
        folder = ROOT / CHANGES
        files = {f"{CHANGES}/{item.name}": item.read_text() for item in sorted(folder.glob("*.md"))} if folder.is_dir() else {}
        return (ROOT / CHANGELOG).read_text(), files
    names = [name for name in git("ls-tree", "-r", "--name-only", "-z", rev, "--", CHANGES).decode().split("\0")
             if name.endswith(".md") and name.count("/") == 2]
    return git("show", f"{rev}:{CHANGELOG}").decode(), {name: git("show", f"{rev}:{name}").decode() for name in names}


def codes(text):
    return [int(match[2]) for match in map(HEADING.fullmatch, (line.rstrip() for line in text.splitlines())) if match]


def arrivals(rev=None):
    """
    What reached main since the first change file, oldest first: `("change", path)` for a file added to
    android/changes and `("entry", code)` for a numbered entry added to the changelog after that. Empty outside Git.
    """
    found, started = [], False
    try:
        log = git("log", "--first-parent", "--reverse", "--no-renames", "--diff-merges=first-parent", "--name-status",
                  "--format=commit %H", rev or "HEAD", "--", CHANGES, CHANGELOG).decode()
        for block in log.split("commit ")[1:]:
            lines = block.splitlines()
            if not re.fullmatch(r"[0-9a-f]{40,64}", lines[0].strip()):
                continue
            status = [line.split("\t") for line in lines[1:] if "\t" in line]
            added = sorted(item[-1] for item in status if item[0] == "A" and item[-1].startswith(CHANGES + "/") and item[-1].endswith(".md"))
            if started and any(item[-1] == CHANGELOG for item in status):
                sha = lines[0].strip()
                before = set(codes(git("show", f"{sha}^:{CHANGELOG}").decode()))
                found += [("entry", code) for code in reversed(codes(git("show", f"{sha}:{CHANGELOG}").decode())) if code not in before]
            found += [("change", path) for path in added]
            started = started or bool(added)
    except (OSError, subprocess.CalledProcessError):
        pass
    return found


def assemble(rev=None):
    """
    The numbered changelog a build reads: android/CHANGELOG.md with each android/changes file as an entry, numbered in
    the order the changes reached main, so two pull requests never ask for the same number. A numbered entry added to
    the changelog after the first change file keeps its number when it is higher than the one it would be given.
    """
    text, files = sources(rev)
    order = arrivals(rev) if files else []
    lines = text.splitlines()
    starts = [index for index, line in enumerate(lines) if line.startswith("## ")]
    if not starts:
        fail("changelog needs release entries with notes")
    blocks = {}
    for start, end in zip(starts, starts[1:] + [len(lines)]):
        match = HEADING.fullmatch(lines[start].rstrip())
        if not match:
            fail(f"invalid changelog heading: {lines[start]}")
        blocks[int(match[2])] = (match[1], match[3], lines[start + 1:end])
    parsed = {path: change(path, body) for path, body in files.items()}
    events = [item for item in order if (item[1] in parsed if item[0] == "change" else item[1] in blocks)]
    known = {path for kind, path in events if kind == "change"}
    events += [("change", path) for path in sorted(set(parsed) - known, key=lambda path: (parsed[path][0], path))]
    late = {code for kind, code in events if kind == "entry"}
    older = [code for code in blocks if code not in late]
    if not older:
        fail("changelog needs an entry older than the change files")
    name, code = blocks[older[0]][0], older[0]
    numbered = []
    for kind, key in events:
        major, minor, patch = version(name)
        if kind == "entry" and key > code and version(blocks[key][0]) > version(name):
            name, code = blocks[key][0], key
        else:
            name, code = f"{major}.{minor}.{patch + 1}", code + 1
        date, body = (blocks[key][1], blocks[key][2]) if kind == "entry" else parsed[key]
        while body and not body[-1].strip():
            body = body[:-1]
        numbered.append([f"## {name} ({code}), {date}", *body, ""])
    rest = [line for index in older for line in [f"## {blocks[index][0]} ({index}), {blocks[index][1]}", *blocks[index][2]]]
    result = "\n".join([*lines[:starts[0]], *(line for block in reversed(numbered) for line in block), *rest]) + "\n"
    history(result)
    return result


def gate(base, head):
    current = history(assemble(head))[0]
    previous = history(assemble(base))[0]
    gone = sorted(set(sources(base)[1]) - set(sources(head)[1]))
    if gone:
        fail(f"a change file that reached main gives a released version its notes and cannot be removed or renamed: {gone[0]}")
    paths = git("diff", "--name-only", "--no-renames", "-z", base, head).decode().split("\0")
    changed = any(runtime_path(path) for path in paths)
    if current["versionCode"] < previous["versionCode"] or version(current["versionName"]) < version(previous["versionName"]):
        fail("Android changelog versions must never decrease")
    if changed and (current["versionCode"] <= previous["versionCode"] or version(current["versionName"]) <= version(previous["versionName"])):
        fail("Android main source/build changed: add a newer versionName and versionCode by adding a change file to android/changes (docs/android-releases.md)")
    print("Android runtime version increase verified" if changed else "No Android runtime/build change; no version bump required")


def manifest(path, missing=False):
    if missing and not path.exists():
        return {"files": []}
    try:
        data = json.loads(path.read_text())
        if not isinstance(data, dict) or not isinstance(data.get("files"), list) or any(not isinstance(item, dict) for item in data["files"]):
            fail("manifest must be an object containing a files array of objects")
        for entry in data["files"]:
            if entry.get("platform") == "android":
                if type(entry.get("versionCode")) is not int or entry["versionCode"] <= 0:
                    fail("Android manifest versionCode is invalid")
                if not isinstance(entry.get("versionName"), str) or not re.fullmatch(r"\d+\.\d+\.\d+", entry["versionName"]):
                    fail("Android manifest versionName is invalid")
        return data
    except (OSError, json.JSONDecodeError) as error:
        fail(f"cannot read valid manifest: {error}")


def published(data):
    entries = [entry for entry in data["files"] if entry.get("platform") == "android"]
    return max(entries, key=lambda entry: entry["versionCode"], default=None)


def required(name):
    value = os.environ.get(name, "").strip()
    if not value:
        fail(f"{name} is required")
    return value


def command(args, label, **kwargs):
    result = subprocess.run(args, capture_output=True, **kwargs)
    if result.returncode:
        fail(f"{label} failed (exit {result.returncode}); check its prerequisites")
    return result.stdout


def certificate(value):
    result = value.strip().replace(":", "").lower()
    if not re.fullmatch(r"[0-9a-f]{64}", result):
        fail("ANDROID_RELEASE_CERT_SHA256 must be a SHA-256 certificate fingerprint")
    return result


def signing(production):
    required("ANDROID_RELEASE_PASSWORD")
    store = Path(os.environ.get("ANDROID_RELEASE_STORE", str(Path.home() / ".config/vercel-native/android-release.p12")))
    if not store.is_file():
        fail("release keystore is missing; set ANDROID_RELEASE_STORE to the existing production key")
    alias = os.environ.get("ANDROID_RELEASE_ALIAS", "vercel-native")
    der = command(["keytool", "-exportcert", "-alias", alias, "-keystore", str(store), "-storepass:env", "ANDROID_RELEASE_PASSWORD"], "release key/password/alias validation")
    digest = hashlib.sha256(der).hexdigest()
    expected = required("ANDROID_RELEASE_CERT_SHA256") if production else os.environ.get("ANDROID_RELEASE_CERT_SHA256")
    if expected and digest != certificate(expected):
        fail("release keystore certificate does not match ANDROID_RELEASE_CERT_SHA256; keep the existing production key")
    sdk = Path(os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT") or str(Path.home() / "Android/Sdk"))
    tools = sorted((sdk / "build-tools").glob("*/apksigner"), key=lambda path: tuple(int(n) for n in re.findall(r"\d+", path.parent.name)))
    if not tools or not tools[-1].with_name("aapt2").is_file():
        fail("apksigner and aapt2 are required in Android SDK build-tools")
    return tools[-1], digest


def verify_apk(apk, entry, signer, digest):
    output = command([str(signer), "verify", "--print-certs", str(apk)], "APK signature verification").decode()
    certs = re.findall(r"certificate SHA-256 digest: ([0-9a-fA-F:]+)", output)
    if not certs or any(certificate(item) != digest for item in certs):
        fail("APK is not signed with the existing release key")
    output = command([str(signer.with_name("aapt2")), "dump", "badging", str(apk)], "APK version validation").decode()
    match = re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'", output)
    if not match or match.groups() != ("com.octaviantocan.vercelnative", str(entry["versionCode"]), entry["versionName"]):
        fail("APK application ID or version does not match the changelog")


def checksum(path):
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


class Remote:
    def __init__(self, stage):
        self.stage = stage
        self.token = None
        for name in ("CLOUDFLARE_ACCOUNT_ID", "R2_ACCESS_KEY_ID", "R2_SECRET_ACCESS_KEY", "TEST_USER_NAME", "TEST_USER_PASSWORD", "ANDROID_GOOGLE_WEB_CLIENT_ID"):
            required(name)
        import boto3
        from botocore.config import Config
        self.s3 = boto3.client("s3", endpoint_url=f"https://{required('CLOUDFLARE_ACCOUNT_ID')}.r2.cloudflarestorage.com",
            aws_access_key_id=required("R2_ACCESS_KEY_ID"), aws_secret_access_key=required("R2_SECRET_ACCESS_KEY"),
            region_name="auto", config=Config(signature_version="s3v4", retries={"max_attempts": 4}, connect_timeout=15, read_timeout=300))

    def r2(self, action, key, path, if_match=None):
        if action == "get":
            answer = self.s3.get_object(Bucket="vercel-native", Key=key)
            with answer["Body"] as body, Path(path).open("wb") as output:
                shutil.copyfileobj(body, output)
            return answer["ETag"]
        elif action == "put":
            kind = "application/json" if key.endswith(".json") else "application/vnd.android.package-archive"
            with Path(path).open("rb") as source:
                condition = {"IfMatch": if_match} if if_match is not None else {}
                self.s3.put_object(Bucket="vercel-native", Key=key, Body=source, ContentType=kind, **condition)
        else:
            fail("Unsupported release storage operation")

    def request(self, path, output, method="GET", payload=None):
        args = ["curl", "--silent", "--show-error", "--fail", "--connect-timeout", "15", "--max-time", "300", "--request", method, "--output", str(output)]
        if self.token:
            headers = self.stage / "authorization"
            headers.write_text(f"Authorization: Bearer {self.token}\n")
            headers.chmod(0o600)
            args += ["--header", f"@{headers}"]
        if payload is not None:
            args += ["--header", "Content-Type: application/json", "--data-binary", "@-"]
        args += [SITE + path]
        command(args, f"authenticated download request {method} {path}", input=payload)

    def signin(self):
        output = self.stage / "signin.json"
        self.request("/app/v1/auth/password", output, "POST", json.dumps(dict(username=required("TEST_USER_NAME"), password=required("TEST_USER_PASSWORD"), device="android-release-ci")).encode())
        self.token = json.loads(output.read_text()).get("token")
        if not isinstance(self.token, str) or not self.token or re.search(r"[\r\n]", self.token):
            fail("test-user sign-in did not return an app token")
        output.unlink()

    def verify_package(self, entry, signer, digest):
        apk = self.stage / "candidate-hosted.apk"
        self.request(f"/download/vercel/{entry['name']}", apk)
        if apk.stat().st_size != entry["bytes"] or checksum(apk) != entry["sha256"]:
            fail("hosted candidate APK checksum/size mismatch; active feed was not changed")
        verify_apk(apk, entry, signer, digest)

    def verify(self, entry, signer, digest):
        description = self.stage / "hosted-android.json"
        self.request("/download/vercel/android.json", description)
        live = json.loads(description.read_text())
        for field in ("applicationId", "versionCode", "versionName", "bytes", "sha256", "notes", "history", "sourceCommit"):
            if live.get(field) != entry.get(field):
                fail(f"hosted Android {field} does not match the release candidate")
        if live.get("url") != f"/download/vercel/{entry['name']}":
            fail("hosted Android URL does not match the release candidate")
        apk = self.stage / "hosted.apk"
        self.request(live["url"], apk)
        if apk.stat().st_size != entry["bytes"] or checksum(apk) != entry["sha256"]:
            fail("hosted APK does not match the candidate checksum/size")
        verify_apk(apk, entry, signer, digest)

    def close(self):
        if self.token:
            try:
                self.request("/app/v1/auth/signout", self.stage / "signout", "POST")
            except ValueError:
                print("android-release: test-user signout failed", file=sys.stderr)


def publish(local=None, if_needed=False):
    given = os.environ.get("MYMIND_CHANGELOG")
    if not given and not local and sources()[1] and git("rev-parse", "--is-shallow-repository").strip() != b"false":
        fail("a release needs the full Git history to number the change files; fetch it (fetch-depth: 0)")
    text = Path(given).read_text() if given else assemble()
    entries = history(text)
    latest = entries[0]
    with tempfile.TemporaryDirectory(prefix="android-release-") as temporary:
        stage = Path(temporary)
        changelog = stage / "CHANGELOG.md"
        changelog.write_text(text)
        remote = None
        try:
            if local:
                target = Path(local).resolve()
                data = manifest(target / "manifest.json", missing=True)
            else:
                remote = Remote(stage)
                original_etag = remote.r2("get", "manifest.json", stage / "manifest.json")
                data = manifest(stage / "manifest.json")
            previous = published(data)
            equal = previous and latest["versionCode"] == previous["versionCode"]
            if previous and (latest["versionCode"] < previous["versionCode"] or version(latest["versionName"]) < version(previous["versionName"])):
                fail("release is older than the published Android version")
            if previous and (latest["versionCode"] <= previous["versionCode"] or version(latest["versionName"]) <= version(previous["versionName"])) and not (remote and if_needed and equal):
                fail("versionName and versionCode must both be higher than the published Android version")
            signer, digest = signing(production=remote is not None)
            if remote:
                remote.signin()
            if remote and if_needed and equal:
                if any(previous.get(field) != latest[field] for field in ("versionName", "notes")) or previous.get("history") != entries:
                    fail("published version already exists with different release notes/history; bump the version")
                if previous.get("name") != f"vercel-native-{latest['versionName']}.apk":
                    fail("published APK name does not match this version")
                remote.verify(previous, signer, digest)
                print(f"Already published and verified {latest['versionName']} ({latest['versionCode']}); no upload")
                return
            args = ["nice", "-n", "19", "./gradlew", "--no-daemon", "--max-workers=2", *shlex.split(os.environ.get("GRADLE_ARGS", "")), f"-PreleaseChangelog={changelog}"]
            if remote:
                args += [f"-PgoogleWebClientId={required('ANDROID_GOOGLE_WEB_CLIENT_ID')}"]
            # Gradle output is retained on the console; it must never receive passwords as command arguments.
            subprocess.run([*args, ":app:assembleRelease"], cwd=ROOT / "android", check=True)
            source = ROOT / "android/app/build/outputs/apk/release/app-release.apk"
            verify_apk(source, latest, signer, digest)
            name = f"vercel-native-{latest['versionName']}.apk"
            apk = stage / name
            shutil.copyfile(source, apk)
            entry = dict(latest, applicationId="com.octaviantocan.vercelnative", name=name, label="Vercel Native", platform="android", bytes=apk.stat().st_size,
                         sha256=checksum(apk), built=datetime.datetime.now(datetime.timezone.utc).isoformat(timespec="seconds"), history=entries)
            revision = required("ANDROID_RELEASE_REVISION")
            if revision:
                if not re.fullmatch(r"[0-9a-f]{40}", revision):
                    fail("ANDROID_RELEASE_REVISION must be a full Git commit SHA")
                entry["sourceCommit"] = revision
            updated = {**data, "files": [item for item in data["files"] if item.get("platform") != "android"] + [entry]}
            candidate = stage / "candidate-manifest.json"
            candidate.write_text(json.dumps(updated, indent=2) + "\n")
            if remote:
                remote.r2("put", name, apk)
                remote.verify_package(entry, signer, digest)
                latest_etag = remote.r2("get", "manifest.json", stage / "latest-manifest.json")
                if latest_etag != original_etag or manifest(stage / "latest-manifest.json") != data:
                    fail("live manifest changed during publication; refusing to overwrite it, retry after the other publisher finishes")
                backup = stage / "previous-manifest.json"
                backup.write_text(json.dumps(data, indent=2) + "\n")
                remote.r2("put", "previous-manifest.json", backup)
                remote.r2("put", "manifest.json", candidate, if_match=original_etag)
                remote.verify(entry, signer, digest)
                print(f"Published and verified {name} at {SITE}/download")
            else:
                target.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(apk, target / name)
                pending = target / ".android-release-manifest.json"
                shutil.copyfile(candidate, pending)
                pending.replace(target / "manifest.json")
                print(f"Published {name} into {target}")
        finally:
            if remote:
                remote.close()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    commands.add_parser("version", help="print the current release as JSON")
    commands.add_parser("changelog", help="print the numbered changelog a build reads: android/CHANGELOG.md plus android/changes")
    check = commands.add_parser("gate", help="require a new version for Android runtime/build changes")
    check.add_argument("--base", required=True)
    check.add_argument("--head", default="HEAD")
    release = commands.add_parser("publish", help="publish with the existing release key; production needs CI credentials documented in docs/android-releases.md")
    release.add_argument("--local", metavar="DOWNLOADS_DIR")
    release.add_argument("--if-needed", action="store_true", help="verify an identical published release instead of uploading it again")
    args = parser.parse_args()
    try:
        if args.command == "version":
            print(json.dumps(history(assemble())[0]))
        elif args.command == "changelog":
            sys.stdout.write(assemble())
        elif args.command == "gate":
            if not args.base or set(args.base) == {"0"}:
                history(assemble(args.head))
                print("Initial release metadata verified")
            else:
                gate(args.base, args.head)
        else:
            publish(args.local, args.if_needed)
    except (ValueError, OSError, subprocess.CalledProcessError) as error:
        print(f"android-release: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
