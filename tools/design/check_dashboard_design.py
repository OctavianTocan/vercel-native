"""Check the dashboard's Figma mappings and generate the Kotlin assets they define."""

import argparse
import json
from pathlib import Path
import re
import sys


ROOT = Path(__file__).resolve().parents[2]
MANIFEST = ROOT / "design/dashboard.json"
DASHBOARD = ROOT / "android/app/src/main/kotlin/com/octaviantocan/vercelnative/dashboard"
TOKENS = DASHBOARD / "DashboardTokens.kt"
CASES = DASHBOARD / "DashboardDesignCases.kt"
BEGIN_CASES = "// BEGIN GENERATED DESIGN CASES"
END_CASES = "// END GENERATED DESIGN CASES"
NODE_ID = re.compile(r"\d+:\d+\Z")
RAW_STYLING = re.compile(r"\b\d+(?:\.\d+)?\.(?:dp|sp)\b|\bColor\s*\(\s*0x")


def kotlin_string(value):
    if value is None:
        return "null"
    return json.dumps(value, ensure_ascii=False).replace("$", r"\$")


def render_tokens(tokens):
    colors = tokens["colors"]
    lines = [
        "// Generated from design/dashboard.json by tools/design/check_dashboard_design.py.",
        "package com.octaviantocan.vercelnative.dashboard",
        "",
        "import androidx.compose.ui.graphics.Color",
        "import androidx.compose.ui.unit.dp",
        "import androidx.compose.ui.unit.sp",
        "",
        "internal object DashboardTokens {",
    ]
    for unit, kinds in (("dp", "dimensions"), ("sp", "type")):
        lines.extend(f"    val {token['name']} = {token['value']}.{unit}" for token in tokens[kinds])
    lines.extend(f"    const val {token['name']} = {token['kotlin']}" for token in tokens["numbers"])
    lines.extend(["}", "", "internal data class DashboardPalette("])
    lines.extend(f"    val {token['name']}: Color," for token in colors)
    lines.extend([") {", "    companion object {"])
    for mode in ("dark", "light"):
        if mode == "light":
            lines.append("")
        lines.append(f"        val {mode.title()} = DashboardPalette(")
        for token in colors:
            value = token[mode].upper()
            argb = f"FF{value}" if len(value) == 6 else value[6:] + value[:6]
            lines.append(f"            {token['name']} = Color(0x{argb}),")
        lines.append("        )")
    return "\n".join(lines + ["    }", "}", ""])


def render_cases(cases, current):
    before, separator, saved = current.partition(BEGIN_CASES)
    if not separator or END_CASES not in saved:
        raise ValueError("DashboardDesignCases.kt is missing its generated section")
    _, _, after = saved.partition(END_CASES)
    rows = []
    for case in cases:
        route = case["route"]
        fields = ", ".join(kotlin_string(case[k]) for k in ("id", "title", "group", "symbol"))
        arguments = ", ".join(kotlin_string(route[k]) for k in ("target", "variant", "detail"))
        rows.append(f"    DashboardDesignCase({fields}, DashboardRoute(Destination.{route['destination']}, {arguments})),")
    return f"{before}{BEGIN_CASES}\n" + "\n".join(rows) + f"\n{END_CASES}{after}"


def read_source(path, cache):
    if path not in cache:
        source = (ROOT / path).resolve()
        if not source.is_relative_to(ROOT) or not source.is_file():
            cache[path] = ""
        else:
            cache[path] = source.read_text()
    return cache[path]


def verify_manifest(manifest):
    errors = []
    cases = manifest["cases"]
    ids = [case["id"] for case in cases]
    if len(ids) != manifest["approvedFrameCount"] or len(set(ids)) != len(ids):
        errors.append("Every approved frame must have exactly one preview case.")
    cache = {}
    destinations = read_source(str(DASHBOARD.relative_to(ROOT) / "Dashboard.kt"), cache)
    for case in cases:
        case_id = case["id"]
        path = case["source"]
        source = read_source(path, cache)
        if not NODE_ID.fullmatch(case_id):
            errors.append(f"Invalid Figma node ID: {case_id}")
        if not re.search(rf"\bfun\s+{re.escape(case['symbol'])}\s*\(", source):
            errors.append(f"{case_id}: {case['symbol']} is missing from {path}")
        destination = case["route"]["destination"]
        if not re.search(rf"^\s*{re.escape(destination)}\(", destinations, re.MULTILINE):
            errors.append(f"{case_id}: unknown dashboard destination {destination}")
    for component in manifest["components"]:
        template = read_source(component["template"], cache)
        node = component["figmaId"].replace(":", "-")
        source = read_source(component["source"], cache)
        if (f"node-id={node}" not in template or f"// source={component['source']}" not in template
                or not re.search(rf"\bfun\s+{re.escape(component['name'])}\s*\(", source)):
            errors.append(f"Code Connect mapping disagrees with the code for {component['name']}")
    for path in DASHBOARD.glob("*.kt"):
        if path != TOKENS:
            for index, line in enumerate(path.read_text().splitlines(), 1):
                if RAW_STYLING.search(line):
                    errors.append(f"{path.relative_to(ROOT)}:{index}: use a named dashboard token")
    review = manifest.get("figmaReview")
    if review is not None:
        if set(review["frames"]) != set(ids):
            errors.append("The Figma snapshot must include every approved frame.")
        for case in cases:
            if case.get("libraryComponentId") not in review["components"]:
                errors.append(f"{case['id']}: the snapshot is missing the linked Figma layout.")
        for kind in ("colors", "dimensions", "type"):
            for token in manifest["tokens"][kind]:
                figma_id = token.get("figmaId")
                if figma_id is None and token.get("figmaFrame") in review["frames"]:
                    # Some approved layers still have values set directly on the frame.
                    continue
                variable = review["variables"].get(figma_id, {})
                expected = (
                    {"Dark": token["dark"], "Light": token["light"]}
                    if kind == "colors" else {"Default": token["value"]}
                )
                if variable.get("modes") != expected:
                    errors.append(f"{token['name']}: sync the Compose token to the reviewed Figma value.")
    return errors


def compare_snapshots(manifest, candidate):
    previous = manifest["figmaReview"]
    key = manifest["figmaFileKey"]
    if candidate.get("figmaFileKey") != key:
        raise ValueError("Snapshot is for a different Figma file")
    if previous is None:
        raise ValueError("The first Figma snapshot needs a visual review before it can become the comparison baseline.")
    cases = {case["id"]: case for case in manifest["cases"]}
    old_frames = previous["frames"]
    new_frames = candidate["frames"]

    def changes(field):
        old = previous[field]
        new = candidate[field]
        return {item for item in old.keys() | new.keys() if old.get(item) != new.get(item)}

    changed_components = changes("components")
    changed_variables = changes("variables")
    changed_frames = changes("frames")

    # Changes to a base variable also affect semantic aliases. Component sets
    # can themselves depend on other components and variables.
    while True:
        previous_count = len(changed_variables)
        for snapshot in (previous, candidate):
            for variable_id, variable in snapshot["variables"].items():
                aliases = {
                    value.get("id") for value in variable["modes"].values()
                    if isinstance(value, dict) and value.get("type") == "VARIABLE_ALIAS"
                }
                if aliases & changed_variables:
                    changed_variables.add(variable_id)
        if previous_count == len(changed_variables):
            break
    while True:
        previous_count = len(changed_components)
        for snapshot in (previous, candidate):
            for component_id, component in snapshot["components"].items():
                if (
                    set(component["components"]) & changed_components
                    or set(component["variables"]) & changed_variables
                ):
                    changed_components.add(component_id)
        if previous_count == len(changed_components):
            break
    found = False
    for kind in ("colors", "dimensions", "type"):
        for token in manifest["tokens"][kind]:
            token_id = token.get("figmaId")
            if token_id not in changed_variables:
                continue
            old_modes = previous["variables"].get(token_id, {}).get("modes", {})
            new_modes = candidate["variables"].get(token_id, {}).get("modes", {})
            modes = sorted(mode for mode in old_modes.keys() | new_modes.keys() if old_modes.get(mode) != new_modes.get(mode))
            mode_label = f" ({', '.join(modes)})" if modes else ""
            print(f"Compose token {token['name']}{mode_label} → {TOKENS.relative_to(ROOT)}")
            found = True
    for frame_id in sorted(old_frames.keys() | new_frames.keys()):
        old_frame, new_frame = old_frames.get(frame_id, {}), new_frames.get(frame_id, {})
        components = set(old_frame.get("components", [])) | set(new_frame.get("components", []))
        variables = set(old_frame.get("variables", [])) | set(new_frame.get("variables", []))
        case = cases.get(frame_id)
        if case and case.get("libraryComponentId"):
            components.add(case["libraryComponentId"])
        reasons = []
        if frame_id in changed_frames:
            reasons.append("layout or instance")
        if components & changed_components:
            reasons.append("component")
        if variables & changed_variables:
            reasons.append("variable")
        if not reasons:
            continue
        found = True
        url = f"https://www.figma.com/design/{key}?node-id={frame_id.replace(':', '-')}"
        code = f"{case['source']} → {case['symbol']} ({case['title']})" if case else "Add a preview case and source mapping"
        print(f"{', '.join(reasons)}: {url} → {code}")
    if not found:
        print("The reviewed frames, components and variables match the Figma snapshot.")
    return found


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    action = parser.add_mutually_exclusive_group()
    action.add_argument("--generate", action="store_true", help="Regenerate the shared Compose tokens and picker cases")
    action.add_argument("--compare", type=Path, help="Report affected screens from a fresh Figma snapshot")
    args = parser.parse_args()
    manifest = json.loads(MANIFEST.read_text())
    if args.compare:
        return int(compare_snapshots(manifest, json.loads(args.compare.read_text())))
    generated = {TOKENS: render_tokens(manifest["tokens"]), CASES: render_cases(manifest["cases"], CASES.read_text())}
    if args.generate:
        for path, content in generated.items():
            path.write_text(content)
    errors = verify_manifest(manifest)
    errors.extend(f"Regenerate {p.relative_to(ROOT)} with --generate" for p, content in generated.items()
                  if not p.is_file() or p.read_text() != content)
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    print(f"Verified {len(manifest['cases'])} frame-to-source mappings and their generated Compose tokens.")
    if manifest["figmaReview"] is None:
        print("The live Figma review baseline is pending.")
    unpublished = [c for c in manifest["components"] if c["status"] != "published"]
    if unpublished:
        print(f"{len(unpublished)} Code Connect templates are waiting to be published.")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (KeyError, ValueError, OSError) as error:
        sys.exit(str(error))
