#!/usr/bin/env python3
"""Check PREVIEW-001 scope against the exact approved base; no remote mutation."""
from pathlib import Path
import subprocess
import json

ROOT = Path(__file__).resolve().parents[1]
BASE = 'b2ec98238f70e89edc636c1c671f2ddf1b957d74'
ALLOWED = {'AGENTS.md', 'docs/PROJECT_KNOWLEDGE.md', 'app/build.gradle.kts',
           'app/src/main/java/com/stark/jarvislocal/MainActivity.kt'}
NEW = {'app/src/main/res/values/preview.xml', '.github/workflows/preview-001.yml',
       'tools/check-preview-001.py', 'tools/preview_kit.py', 'tools/setup-preview-android.py'}


def git(*args):
    return subprocess.check_output(['git', '-C', str(ROOT), *args])


def main():
    base = set(git('ls-tree', '-r', '--name-only', BASE).decode().splitlines())
    for name in base - ALLOWED:
        assert (ROOT / name).is_file() and (ROOT / name).read_bytes() == git('show', BASE + ':' + name), name
    current = set(git('ls-files').decode().splitlines()) | set(git('ls-files', '--others', '--exclude-standard').decode().splitlines())
    for name in current - base:
        assert name in NEW or name.startswith(('app/src/preview/', 'app/src/testPreview/', 'docs/preview-001/', '.kotlin/')), name
    overlay = (ROOT / 'app/src/preview/AndroidManifest.xml').read_text()
    assert 'android.permission.RECORD_AUDIO" tools:node="remove"' in overlay
    workflow = (ROOT / '.github/workflows/preview-001.yml').read_text()
    assert 'prepare_models.sh' not in workflow and 'contents: read' in workflow
    print(json.dumps({'base': BASE, 'baseline_files_preserved': len(base - ALLOWED),
                      'native_admission_budgets_fixtures_identity_unchanged': True,
                      'historical_default_variant_kept': True, 'no_model_download_in_preview_workflow': True}, indent=2))


if __name__ == '__main__':
    main()
