set shell := ["bash", "-cu"]

# Show available recipes
_default:
    just --list

# Download the pinned Brewer OpenAPI spec. Defaults to latest api-spec-v* release.
api-spec-update tag="latest":
    scripts/api/update_brewer_spec.sh {{tag}}

# Print the currently pinned Brewer API spec version.
api-spec-version:
    @if [ -f specs/brewer/VERSION ]; then cat specs/brewer/VERSION; else echo "No Brewer API spec pinned yet"; exit 1; fi

# Fail if this repo is not pinned to the latest Brewer api-spec-v* release.
api-spec-check-latest:
    scripts/api/update_brewer_spec.sh --check-latest

# Show a short summary of the pinned Brewer OpenAPI spec.
api-spec-summary:
    @python3 -c "import json; from pathlib import Path; p=Path('specs/brewer/openapi.json'); assert p.exists(), 'No spec found. Run: just api-spec-update'; data=json.loads(p.read_text()); v=Path('specs/brewer/VERSION').read_text().strip() if Path('specs/brewer/VERSION').exists() else 'unknown'; print(f'Pinned: {v}'); print(f'OpenAPI: {data.get(\"openapi\")}'); print(f'Title: {data.get(\"info\", {}).get(\"title\", \"unknown\")}'); print(f'API version: {data.get(\"info\", {}).get(\"version\", \"unknown\")}'); print(f'Paths: {len(data.get(\"paths\", {}))}'); [print(f'  {path}') for path in sorted(data.get('paths', {}))]"
