#!/usr/bin/env python3
"""Android version gate and fail-closed release publisher. Uses only the Python standard library."""
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
APP_ID = "com.octaviantocan.vercelnative"
PREFIX = "/download/vercel/"
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




def main():
    parser = argparse.ArgumentParser()
    subs = parser.add_subparsers(dest="command", required=True)
    subs.add_parser("version")
    subs.add_parser("changelog")
    check = subs.add_parser("gate")
    check.add_argument("--base", default="")
    check.add_argument("--head", default="HEAD")
    args = parser.parse_args()
    if args.command == "version":
        print(json.dumps(history(assemble())[0]))
    elif args.command == "changelog":
        print(assemble())
    elif not args.base or set(args.base) == {"0"}:
        history(assemble(args.head))
        print("Initial release metadata verified")
    else:
        gate(args.base, args.head)

if __name__ == "__main__":
    main()
