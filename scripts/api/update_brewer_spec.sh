#!/usr/bin/env bash
set -euo pipefail

REPO="${BREWER_REPO:-ybird-labs/brewer}"
TAG_INPUT="${1:-latest}"
SPEC_DIR="${SPEC_DIR:-specs/brewer}"
ASSET_NAME="openapi.json"

usage() {
  cat <<'EOF'
Usage:
  scripts/api/update_brewer_spec.sh [latest|api-spec-vX.Y.Z|vX.Y.Z|X.Y.Z]
  scripts/api/update_brewer_spec.sh --check-latest

Environment:
  BREWER_REPO  GitHub repo to read from (default: ybird-labs/brewer)
  SPEC_DIR     Local spec directory (default: specs/brewer)
  GITHUB_TOKEN Optional token for GitHub release asset downloads

Examples:
  scripts/api/update_brewer_spec.sh
  scripts/api/update_brewer_spec.sh latest
  scripts/api/update_brewer_spec.sh api-spec-v0.1.2
  scripts/api/update_brewer_spec.sh 0.1.2
  scripts/api/update_brewer_spec.sh --check-latest
EOF
}

normalize_tag() {
  local input="$1"
  case "$input" in
    latest) echo "latest" ;;
    api-spec-v*) echo "$input" ;;
    v*) echo "api-spec-$input" ;;
    [0-9]*) echo "api-spec-v$input" ;;
    *)
      echo "Unsupported tag '$input'. Expected latest, api-spec-vX.Y.Z, vX.Y.Z, or X.Y.Z." >&2
      exit 2
      ;;
  esac
}

latest_tag() {
  git ls-remote --tags --refs "https://github.com/${REPO}.git" 'refs/tags/api-spec-v*' \
    | awk '{print $2}' \
    | sed 's#refs/tags/##' \
    | sort -V \
    | tail -n 1
}

if [[ "${TAG_INPUT}" == "-h" || "${TAG_INPUT}" == "--help" ]]; then
  usage
  exit 0
fi

if [[ "${TAG_INPUT}" == "--check-latest" ]]; then
  current=""
  [[ -f "${SPEC_DIR}/VERSION" ]] && current="$(tr -d '[:space:]' < "${SPEC_DIR}/VERSION")"
  latest="$(latest_tag)"
  if [[ -z "${latest}" ]]; then
    echo "No api-spec-v* tags found in ${REPO}." >&2
    exit 1
  fi
  if [[ -z "${current}" ]]; then
    echo "No pinned Brewer API spec found at ${SPEC_DIR}/VERSION. Latest is ${latest}." >&2
    exit 1
  fi
  if [[ "${current}" != "${latest}" ]]; then
    echo "Brewer API spec is not latest: pinned=${current}, latest=${latest}." >&2
    echo "Run: just api-spec-update ${latest}" >&2
    exit 1
  fi
  echo "Brewer API spec is up to date (${current})."
  exit 0
fi

TAG="$(normalize_tag "${TAG_INPUT}")"
if [[ "${TAG}" == "latest" ]]; then
  TAG="$(latest_tag)"
fi

if [[ -z "${TAG}" ]]; then
  echo "No api-spec-v* tags found in ${REPO}." >&2
  exit 1
fi

URL="https://github.com/${REPO}/releases/download/${TAG}/${ASSET_NAME}"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "${TMP_DIR}"' EXIT
TMP_SPEC="${TMP_DIR}/${ASSET_NAME}"

curl_args=(-fsSL)
if [[ -n "${GITHUB_TOKEN:-}" ]]; then
  curl_args+=(-H "Authorization: Bearer ${GITHUB_TOKEN}")
fi

printf 'Downloading Brewer API spec %s from %s\n' "${TAG}" "${URL}"
curl "${curl_args[@]}" "${URL}" -o "${TMP_SPEC}"

python3 - "${TMP_SPEC}" <<'PY'
import json
import sys
from pathlib import Path

path = Path(sys.argv[1])
try:
    data = json.loads(path.read_text())
except Exception as exc:
    raise SystemExit(f"Downloaded file is not valid JSON: {exc}")

if "openapi" not in data or "paths" not in data:
    raise SystemExit("Downloaded JSON does not look like an OpenAPI document")

print(f"OpenAPI: {data.get('openapi')}")
print(f"Title: {data.get('info', {}).get('title', 'unknown')}")
print(f"Paths: {len(data.get('paths', {}))}")
PY

mkdir -p "${SPEC_DIR}"
cp "${TMP_SPEC}" "${SPEC_DIR}/${ASSET_NAME}"
printf '%s\n' "${TAG}" > "${SPEC_DIR}/VERSION"

SHA256="$(shasum -a 256 "${SPEC_DIR}/${ASSET_NAME}" | awk '{print $1}')"
python3 - "${SPEC_DIR}/METADATA.json" "${REPO}" "${TAG}" "${URL}" "${SHA256}" <<'PY'
import json
import sys
from datetime import datetime, timezone
from pathlib import Path

out, repo, tag, url, sha = sys.argv[1:]
metadata = {
    "repo": repo,
    "tag": tag,
    "asset": "openapi.json",
    "source_url": url,
    "sha256": sha,
    "updated_at": datetime.now(timezone.utc).isoformat(),
}
Path(out).write_text(json.dumps(metadata, indent=2) + "\n")
PY

printf 'Updated %s from %s\n' "${SPEC_DIR}/${ASSET_NAME}" "${TAG}"
printf 'Pinned version: %s\n' "${TAG}"
printf 'SHA-256: %s\n' "${SHA256}"
