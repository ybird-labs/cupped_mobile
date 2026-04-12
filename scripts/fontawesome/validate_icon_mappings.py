#!/usr/bin/env python3

from collections import Counter
import json
import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MANIFEST = ROOT / "scripts" / "fontawesome" / "icons.json"
SWIFT_FILE = ROOT / "iosApp" / "iosApp" / "DesignSystem" / "Icons" / "AppIcon.swift"
ANDROID_FILE = (
    ROOT
    / "composeApp"
    / "src"
    / "androidMain"
    / "kotlin"
    / "cafe"
    / "cupped"
    / "app"
    / "designsystem"
    / "icons"
    / "AppIcon.kt"
)


def lower_camel_to_pascal(value: str) -> str:
    return value[0].upper() + value[1:]


def parse_manifest() -> list[dict]:
    with MANIFEST.open("r", encoding="utf-8") as handle:
        return json.load(handle)["icons"]


def parse_swift_cases(source: str) -> list[str]:
    return re.findall(r"^\s*case\s+([a-zA-Z][a-zA-Z0-9]*)$", source, flags=re.MULTILINE)


def parse_swift_switch(source: str, property_name: str) -> dict[str, str]:
    pattern = rf"var {property_name}: String \{{(?P<body>.*?)^\s*\}}"
    match = re.search(pattern, source, flags=re.MULTILINE | re.DOTALL)
    if not match:
        raise ValueError(f"Could not find Swift property: {property_name}")

    mappings: dict[str, str] = {}
    for cases_blob, value in re.findall(
        r"case\s+([^:]+):\s+\"([^\"]+)\"",
        match.group("body"),
        flags=re.MULTILINE,
    ):
        case_names = [item.strip().removeprefix(".") for item in cases_blob.split(",")]
        for case_name in case_names:
            mappings[case_name] = value
    return mappings


def parse_android_entries(source: str) -> dict[str, tuple[str, str]]:
    entries: dict[str, tuple[str, str]] = {}
    for name, drawable, label in re.findall(
        r"^\s*([A-Z][A-Za-z0-9]*)\(R\.drawable\.([a-z0-9_]+), \"([^\"]+)\"\),$",
        source,
        flags=re.MULTILINE,
    ):
        entries[name] = (drawable, label)
    return entries


def find_manifest_duplicates(manifest: list[dict]) -> list[str]:
    errors: list[str] = []
    collision_fields = (
        ("appName", "Duplicate appName"),
        ("iosAssetName", "Duplicate iosAssetName"),
        ("androidDrawableName", "Duplicate androidDrawableName"),
        ("accessibilityLabel", "Duplicate accessibilityLabel"),
    )

    for field_name, error_prefix in collision_fields:
        counts = Counter(icon[field_name] for icon in manifest)
        for value, count in counts.items():
            if count > 1:
                errors.append(f"{error_prefix}: {value}")

    return errors


def main() -> int:
    manifest = parse_manifest()
    errors: list[str] = []
    errors.extend(find_manifest_duplicates(manifest))

    if errors:
        print("Font Awesome icon mapping validation failed:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1

    swift_source = SWIFT_FILE.read_text(encoding="utf-8")
    android_source = ANDROID_FILE.read_text(encoding="utf-8")

    manifest_names = [icon["appName"] for icon in manifest]
    expected_swift_cases = manifest_names
    actual_swift_cases = parse_swift_cases(swift_source)

    missing_swift_cases = [name for name in expected_swift_cases if name not in actual_swift_cases]
    extra_swift_cases = [name for name in actual_swift_cases if name not in expected_swift_cases]
    if missing_swift_cases:
        errors.append(f"Swift enum missing cases: {', '.join(missing_swift_cases)}")
    if extra_swift_cases:
        errors.append(f"Swift enum has extra cases: {', '.join(extra_swift_cases)}")

    swift_assets = parse_swift_switch(swift_source, "assetName")
    swift_labels = parse_swift_switch(swift_source, "accessibilityLabel")
    for icon in manifest:
        name = icon["appName"]
        expected_asset = icon["iosAssetName"]
        expected_label = icon["accessibilityLabel"]
        if swift_assets.get(name) != expected_asset:
            errors.append(
                f"Swift asset mismatch for {name}: expected {expected_asset}, got {swift_assets.get(name)}"
            )
        if swift_labels.get(name) != expected_label:
            errors.append(
                f"Swift accessibility label mismatch for {name}: expected {expected_label}, got {swift_labels.get(name)}"
            )

    android_entries = parse_android_entries(android_source)
    expected_android_names = [lower_camel_to_pascal(icon["appName"]) for icon in manifest]
    actual_android_names = list(android_entries.keys())
    missing_android_entries = [name for name in expected_android_names if name not in actual_android_names]
    extra_android_entries = [name for name in actual_android_names if name not in expected_android_names]
    if missing_android_entries:
        errors.append(f"Android enum missing entries: {', '.join(missing_android_entries)}")
    if extra_android_entries:
        errors.append(f"Android enum has extra entries: {', '.join(extra_android_entries)}")

    for icon in manifest:
        name = lower_camel_to_pascal(icon["appName"])
        expected_drawable = icon["androidDrawableName"]
        expected_label = icon["accessibilityLabel"]
        actual_drawable, actual_label = android_entries.get(name, (None, None))
        if actual_drawable != expected_drawable:
            errors.append(
                f"Android drawable mismatch for {name}: expected {expected_drawable}, got {actual_drawable}"
            )
        if actual_label != expected_label:
            errors.append(
                f"Android label mismatch for {name}: expected {expected_label}, got {actual_label}"
            )

    if errors:
        print("Font Awesome icon mapping validation failed:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1

    print("Font Awesome icon mappings are in sync.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
