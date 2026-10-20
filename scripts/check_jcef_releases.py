#!/usr/bin/env python3
"""
check_jcef_releases.py

Checks upstream JetBrains Maven repository for new stable production releases of JCEF
(org.jetbrains.intellij.deps.jcef:jcef), compares against the local kromium-provider-jcef
bundled version, and outputs release information for GitHub Actions.
"""

import os
import re
import sys
import urllib.request
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path

MAVEN_METADATA_URL = (
    "https://packages.jetbrains.team/maven/p/ij/intellij-dependencies/"
    "org/jetbrains/intellij/deps/jcef/jcef/maven-metadata.xml"
)
JAR_BASE_URL = (
    "https://cache-redirector.jetbrains.com/intellij-dependencies/"
    "org/jetbrains/intellij/deps/jcef/jcef"
)


def get_current_version(repo_root: Path) -> str:
    """Reads the current bundled JCEF version from libs/jcef.jar version.info."""
    jar_path = repo_root / "kromium-provider-jcef" / "libs" / "jcef.jar"
    if not jar_path.exists():
        print(f"Warning: {jar_path} not found, falling back to empty version.", file=sys.stderr)
        return ""

    with zipfile.ZipFile(jar_path) as z:
        for name in z.namelist():
            if name.endswith("version.info"):
                return z.read(name).decode("utf-8", errors="ignore").strip()

    print("Warning: version.info not found inside jcef.jar", file=sys.stderr)
    return ""


def parse_jcef_version(version_str: str):
    """
    Parses a JCEF version string into a comparable tuple:
    (chromium_parts, build_number, original_string).
    
    Example formats:
      150.0.14-g7c1aa68-chromium-150.0.7871.129-api-1.21-263-b16
      144.0.15-g72717cf-chromium-144.0.7559.172-api-1.21-262-b37
    """
    # Extract Chromium milestone / version
    c_match = re.search(r"chromium-(\d+)\.(\d+)\.(\d+)\.(\d+)", version_str)
    if c_match:
        c_parts = tuple(map(int, c_match.groups()))
    else:
        # Fallback: check leading digits
        lead_match = re.match(r"(\d+)\.(\d+)\.(\d+)", version_str)
        c_parts = tuple(map(int, lead_match.groups())) + (0,) if lead_match else (0, 0, 0, 0)

    # Extract build number (e.g. -b16)
    b_match = re.search(r"-b(\d+)", version_str)
    build_num = int(b_match.group(1)) if b_match else 0

    return (c_parts, build_num, version_str)


def is_stable_production(version_str: str) -> bool:
    """
    Filters for stable production releases only.
    Excludes '-dev', '-snapshot', '-preview', '-rc', etc.
    """
    lower = version_str.lower()
    unstable_markers = ["-dev", "-snapshot", "-preview", "-alpha", "-beta", "-rc"]
    return not any(marker in lower for marker in unstable_markers)


def fetch_upstream_versions() -> list:
    """Fetches and parses all version entries from JetBrains Maven repository."""
    req = urllib.request.Request(
        MAVEN_METADATA_URL,
        headers={"User-Agent": "Kromium-JCEF-Release-Monitor/1.0"}
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        xml_content = resp.read()

    root = ET.fromstring(xml_content)
    versions = [
        v.text.strip()
        for v in root.findall(".//version")
        if v.text and is_stable_production(v.text.strip())
    ]
    return versions


def main():
    repo_root = Path(__file__).resolve().parent.parent
    current_version = get_current_version(repo_root)
    print(f"Current Kromium JCEF Version: {current_version or 'unknown'}")

    print(f"Fetching upstream JCEF stable versions from {MAVEN_METADATA_URL}...")
    stable_versions = fetch_upstream_versions()
    if not stable_versions:
        print("Error: No stable versions found upstream!", file=sys.stderr)
        sys.exit(1)

    parsed_versions = sorted(
        [parse_jcef_version(v) for v in stable_versions],
        key=lambda x: (x[0], x[1])
    )
    latest_tuple = parsed_versions[-1]
    latest_version = latest_tuple[2]
    c_parts = latest_tuple[0]
    chromium_ver = f"{c_parts[0]}.{c_parts[1]}.{c_parts[2]}.{c_parts[3]}"
    jar_url = f"{JAR_BASE_URL}/{latest_version}/jcef-{latest_version}.jar"

    print(f"Latest Upstream Stable Version: {latest_version}")
    print(f"Underlying Chromium Version:   {chromium_ver}")
    print(f"Direct JAR Download URL:        {jar_url}")

    current_parsed = parse_jcef_version(current_version) if current_version else ((0, 0, 0, 0), 0, "")
    update_available = (latest_tuple[0], latest_tuple[1]) > (current_parsed[0], current_parsed[1])

    print(f"Update Available: {update_available}")

    # Set outputs for GitHub Actions
    github_output = os.getenv("GITHUB_OUTPUT")
    if github_output:
        with open(github_output, "a", encoding="utf-8") as f:
            f.write(f"update_available={'true' if update_available else 'false'}\n")
            f.write(f"latest_version={latest_version}\n")
            f.write(f"current_version={current_version}\n")
            f.write(f"chromium_version={chromium_ver}\n")
            f.write(f"jar_url={jar_url}\n")

    # Write GitHub Step Summary
    github_summary = os.getenv("GITHUB_STEP_SUMMARY")
    if github_summary:
        with open(github_summary, "a", encoding="utf-8") as f:
            f.write("# 🔍 JCEF Upstream Release Monitor\n\n")
            f.write(f"- **Current Bundled Version**: `{current_version}`\n")
            f.write(f"- **Latest Stable Release**: `{latest_version}`\n")
            f.write(f"- **Chromium Milestone**: `{chromium_ver}`\n")
            f.write(f"- **Update Available**: `{'YES 🚀' if update_available else 'No (Up to date) ✅'}`\n\n")
            if update_available:
                f.write(f"### 📦 Direct Artifact Download\n[Download `jcef-{latest_version}.jar`]({jar_url})\n\n")


if __name__ == "__main__":
    main()
