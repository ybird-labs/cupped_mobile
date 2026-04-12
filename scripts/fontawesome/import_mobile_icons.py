#!/usr/bin/env python3

import json
import shutil
import sys
from xml.dom import minidom
from pathlib import Path

import defusedxml.ElementTree as SafeET
import xml.etree.ElementTree as XmlET

ROOT = Path(__file__).resolve().parents[2]
KIT_ROOT = ROOT / ".vendor" / "fontawesome" / "kit"
MANIFEST = ROOT / "scripts" / "fontawesome" / "icons.json"
IOS_ASSETS_ROOT = ROOT / "iosApp" / "iosApp" / "Assets.xcassets" / "FontAwesome"
ANDROID_DRAWABLE_ROOT = ROOT / "composeApp" / "src" / "androidMain" / "res" / "drawable"
ANDROID_VECTOR_NS = "http://schemas.android.com/apk/res/android"
ANDROID_ICON_DP = 24.0


def load_manifest() -> dict:
    with MANIFEST.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def ensure_dir(path: Path) -> None:
    path.mkdir(parents=True, exist_ok=True)


def ensure_ios_catalog_root() -> None:
    ensure_dir(IOS_ASSETS_ROOT)
    contents = {
        "info": {
            "author": "xcode",
            "version": 1
        }
    }
    with (IOS_ASSETS_ROOT / "Contents.json").open("w", encoding="utf-8") as handle:
        json.dump(contents, handle, indent=2)
        handle.write("\n")


def find_svg(icon: dict, default_style: str) -> Path:
    style = icon.get("faStyle", default_style)
    svg_path = KIT_ROOT / "svgs" / style / f"{icon['faName']}.svg"
    if not svg_path.exists():
        raise FileNotFoundError(f"Missing SVG for {icon['appName']}: {svg_path}")
    return svg_path


def write_ios_asset(svg_path: Path, asset_name: str) -> None:
    imageset_dir = IOS_ASSETS_ROOT / f"{asset_name}.imageset"
    ensure_dir(imageset_dir)
    shutil.copy2(svg_path, imageset_dir / f"{asset_name}.svg")
    contents = {
        "images": [
            {
                "idiom": "universal",
                "filename": f"{asset_name}.svg"
            }
        ],
        "info": {
            "author": "xcode",
            "version": 1
        },
        "properties": {
            "preserves-vector-representation": True,
            "template-rendering-intent": "template"
        }
    }
    with (imageset_dir / "Contents.json").open("w", encoding="utf-8") as handle:
        json.dump(contents, handle, indent=2)
        handle.write("\n")


def android_attr(name: str) -> str:
    return f"{{{ANDROID_VECTOR_NS}}}{name}"


def parse_viewbox(svg_root) -> tuple[str, str]:
    view_box = svg_root.attrib.get("viewBox")
    if not view_box:
        raise ValueError("SVG is missing viewBox")
    _, _, width, height = view_box.split()
    return width, height


def format_dp(value: float) -> str:
    if value.is_integer():
        return f"{int(value)}dp"
    return f"{value:.2f}".rstrip("0").rstrip(".") + "dp"


def scaled_android_size(width: str, height: str) -> tuple[str, str]:
    viewport_width = float(width)
    viewport_height = float(height)
    if viewport_width <= 0 or viewport_height <= 0:
        raise ValueError(f"Invalid viewBox dimensions: {width}x{height}")

    # Keep the SVG viewport untouched and scale only the intrinsic drawable size.
    # This mirrors how native asset pipelines preserve icon geometry while fitting
    # the longest side into the design system's 24dp icon box.
    if viewport_width >= viewport_height:
        scaled_width = ANDROID_ICON_DP
        scaled_height = ANDROID_ICON_DP * viewport_height / viewport_width
    else:
        scaled_width = ANDROID_ICON_DP * viewport_width / viewport_height
        scaled_height = ANDROID_ICON_DP

    return format_dp(scaled_width), format_dp(scaled_height)


def collect_paths(node) -> list[tuple[str, str]]:
    paths: list[tuple[str, str]] = []
    for element in node.iter():
        if element.tag.endswith("path"):
            path_data = element.attrib.get("d")
            if not path_data:
                continue
            fill = element.attrib.get("fill", "currentColor")
            if fill in ("none", "transparent"):
                continue
            if fill == "currentColor":
                fill = "#FF000000"
            elif fill.startswith("#") and len(fill) == 7:
                fill = "#FF" + fill[1:]
            paths.append((path_data, fill))
    if not paths:
        raise ValueError("No drawable paths found in SVG")
    return paths


def write_android_vector(svg_path: Path, drawable_name: str) -> None:
    svg_root = SafeET.parse(svg_path).getroot()
    width, height = parse_viewbox(svg_root)
    android_width, android_height = scaled_android_size(width, height)
    XmlET.register_namespace("android", ANDROID_VECTOR_NS)
    vector = XmlET.Element(
        "vector",
        {
            android_attr("width"): android_width,
            android_attr("height"): android_height,
            android_attr("viewportWidth"): width,
            android_attr("viewportHeight"): height,
        },
    )
    for path_data, fill in collect_paths(svg_root):
        XmlET.SubElement(
            vector,
            "path",
            {
                android_attr("fillColor"): fill,
                android_attr("pathData"): path_data,
            },
        )
    rough_xml = XmlET.tostring(vector, encoding="unicode")
    xml = minidom.parseString(rough_xml).toprettyxml(indent="  ", encoding="utf-8")
    out_path = ANDROID_DRAWABLE_ROOT / f"{drawable_name}.xml"
    with out_path.open("w", encoding="utf-8") as handle:
        handle.write(xml.decode("utf-8"))


def main() -> int:
    if not KIT_ROOT.exists():
        print(f"Kit directory not found: {KIT_ROOT}", file=sys.stderr)
        return 1

    manifest = load_manifest()
    default_style = manifest.get("defaultStyle", "solid")
    ensure_ios_catalog_root()
    ensure_dir(ANDROID_DRAWABLE_ROOT)

    generated = []
    for icon in manifest["icons"]:
        svg_path = find_svg(icon, default_style)
        write_ios_asset(svg_path, icon["iosAssetName"])
        write_android_vector(svg_path, icon["androidDrawableName"])
        generated.append(icon["appName"])

    print(f"Generated {len(generated)} icons")
    for app_name in generated:
        print(f"- {app_name}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
