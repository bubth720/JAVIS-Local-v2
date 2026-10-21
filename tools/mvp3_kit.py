#!/usr/bin/env python3
"""Inspect the signed MVP APK and prepare an Actions artifact directory. No install/download."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = 'com.stark.jarvislocal.mvp003'


def sha(data):
    return hashlib.sha256(data).hexdigest()


def command(*args):
    return subprocess.check_output(list(map(str, args)), text=True, cwd=ROOT)


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--apk', type=Path, default=ROOT / 'app/build/outputs/apk/mvp3/app-mvp3.apk')
    p.add_argument('--out', type=Path, required=True)
    p.add_argument('--sdk', type=Path, required=True)
    p.add_argument('--allow-dirty-local', action='store_true', help='Local audit only; never the CI kit')
    args = p.parse_args()
    build_tools = args.sdk / 'build-tools/35.0.0'
    dirty = bool(command('git', 'status', '--porcelain', '--', '.', ':!.kotlin/', ':!docs/identity-002n/').strip())
    assert not dirty or args.allow_dirty_local, 'Commit before building the downloadable kit'
    permissions = command(build_tools / 'aapt', 'dump', 'permissions', args.apk)
    badging = command(build_tools / 'aapt', 'dump', 'badging', args.apk)
    manifest = command(build_tools / 'aapt', 'dump', 'xmltree', args.apk, 'AndroidManifest.xml')
    certificate = command(build_tools / 'apksigner', 'verify', '--verbose', '--print-certs', args.apk)
    assert "package: name='" + PACKAGE + "'" in badging, "Unexpected APK package"
    assert "sdkVersion:'24'" in badging and "targetSdkVersion:'35'" in badging
    assert "launchable-activity: name='com.stark.jarvislocal.Mvp2Activity'" in badging
    assert "native-code: 'arm64-v8a'" in badging
    for forbidden in ('android.permission.INTERNET', 'android.permission.ACCESS_NETWORK_STATE', 'android.permission.RECORD_AUDIO'):
        assert forbidden not in permissions, forbidden
    # Validate all component exports, not just the launcher / permission names.
    blocks = re.findall(r'^      E: (activity|service|receiver|provider) .*?(?=^      E: |\Z)', manifest, re.M | re.S)
    assert sorted(blocks) == ['activity', 'activity', 'provider', 'receiver'], blocks
    components = []
    for match in re.finditer(r'^      E: (activity|service|receiver|provider) .*?(?=^      E: |\Z)', manifest, re.M | re.S):
        body = match.group(0)
        name = re.search(r'A: android:name[^=]*="([^"]+)"', body).group(1)
        components.append({'type': match.group(1), 'name': name,
                           'exported': bool(re.search(r'android:exported.*0xffffffff', body)),
                           'dump_permission': '"android.permission.DUMP"' in body})
    assert [c['name'] for c in components if c['exported']] == [
        'com.stark.jarvislocal.Mvp2Activity', 'androidx.profileinstaller.ProfileInstallReceiver']
    assert components[-1]['dump_permission']
    assert re.search(r'android:allowBackup.*0x0$', manifest, re.M)
    requested = re.findall(r"uses-permission: name='([^']+)'", permissions)
    assert requested == [PACKAGE + '.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'], requested
    alignment_check = command(build_tools / 'zipalign', '-c', '-P', '16', '-v', '4', args.apk)
    # check_output requires exit status 0; zipalign 35 prints the typo 'succesful'.
    # Do not infer success from localized/human-readable output.
    prior = json.loads((ROOT / 'docs/identity-002t/NATIVE_AUDIT.json').read_text())
    cache = Path(os.environ.get('GRADLE_USER_HOME', str(Path.home() / '.gradle'))) / 'caches/modules-2/files-2.1'
    historical = next((cache / 'dev.ffmpegkit-maintained/llama-android/0.1.1').rglob('*.aar'))
    assert sha(historical.read_bytes()) == 'fdccf04c77de470004d7e54679bb918074ee9dbf31ea75af8c014d19c31e7501'
    with zipfile.ZipFile(historical) as aar:
        historical_native = {Path(n).name: sha(aar.read(n)) for n in aar.namelist() if n.startswith('jni/arm64-v8a/') and n.endswith('.so')}
    assert len(historical_native) == 6
    baseline_native = {item['path']: item['sha256'] for item in json.loads((ROOT / 'docs/mvp-001/LOCAL-APK-INFO.json').read_text())['native_libraries']}
    pdfbox = next((cache / 'com.tom-roush/pdfbox-android/2.0.27.0').rglob('*.aar'))
    assert sha(pdfbox.read_bytes()) == '30277f879cfd571db2a137582c95516a0d4ea6778e945519bc58ca93d57d88c7'
    with zipfile.ZipFile(pdfbox) as pdf:
        expected_assets = {n: sha(pdf.read(n)) for n in pdf.namelist() if n.startswith('assets/') and not n.endswith('/')}
    assert len(expected_assets) == 112
    for name in ('identity/manifest-b.json', 'models/README.txt'):
        expected_assets['assets/' + name] = sha((ROOT / 'app/src/main/assets' / name).read_bytes())
    libraries = []
    with zipfile.ZipFile(args.apk) as archive, tempfile.TemporaryDirectory(prefix='preview-apk-audit-') as temp:
        models = [n for n in archive.namelist() if n.startswith('assets/') and (n.endswith('.gguf') or n.endswith('/whisper.bin'))]
        assert not models, 'No weights may be bundled in MVP-003'
        for name in sorted(n for n in archive.namelist() if n.startswith('lib/') and n.endswith('.so')):
            data = archive.read(name)
            assert sha(data) == baseline_native[name], 'Native binary changed: ' + name
            assert name.startswith('lib/arm64-v8a/') and data[:4] == b'\x7fELF' and int.from_bytes(data[18:20], 'little') == 183
            if Path(name).name in historical_native:
                assert sha(data) == historical_native[Path(name).name]
            if name.endswith(('/libllama_jni.so', '/libllama.so')):
                assert sha(data) == prior['parts_sha256']['jni/arm64-v8a/' + Path(name).name]
            path = Path(temp) / Path(name).name
            path.write_bytes(data)
            elf = command('readelf', '-lW', path)
            align = sorted(set(line.split()[-1] for line in elf.splitlines() if line.strip().startswith('LOAD ')))
            assert align and all(int(value, 16) >= 16384 for value in align), name
            dependencies = re.findall(r'\(NEEDED\).*?\[(.*?)\]', command('readelf', '-dW', path))
            libraries.append({'path': name, 'sha256': sha(data), 'load_segment_alignments': align,
                              'needed': dependencies})
        assert len(libraries) == 7
        actual_assets = {n: sha(archive.read(n)) for n in archive.namelist() if n.startswith('assets/') and not n.endswith('/')}
        assert actual_assets == expected_assets, 'Unexpected or changed APK asset'
    expected_counts = {'historical_app': 263, 'fixture': 36, 'preview_ui': 12, 'mvp': 72, 'mvp2': 41, 'mvp3': 15}
    suites = {}
    for label, directory in (
        ('historical_app', ROOT / 'app/build/test-results/testDebugUnitTest'),
        ('preview_ui', ROOT / 'app/build/test-results/testPreviewUnitTest'),
        ('mvp', ROOT / 'app/build/test-results/testMvpUnitTest'),
        ('mvp2', ROOT / 'app/build/test-results/testMvp2UnitTest'),
        ('mvp3', ROOT / 'app/build/test-results/testMvp3UnitTest'),
        ('fixture', ROOT / 'fixtures/p2-client/build/test-results/testDebugUnitTest')):
        rows = []
        for f in sorted(directory.glob('TEST-*.xml')):
            e = ET.parse(f).getroot()
            rows.append({'class': e.attrib['name'], **{k: int(e.attrib.get(k, 0)) for k in ('tests', 'failures', 'errors', 'skipped')}})
        assert rows and sum(r['tests'] for r in rows) >= expected_counts[label], (label, 'incomplete suite')
        assert not any(r[k] for r in rows for k in ('failures', 'errors', 'skipped')), label
        suites[label] = rows
    lint = {}
    for label, path in (
        ('historical_app', ROOT / 'app/build/reports/lint-results-debug.xml'),
        ('preview', ROOT / 'app/build/reports/lint-results-preview.xml'),
        ('mvp', ROOT / 'app/build/reports/lint-results-mvp.xml'),
        ('mvp2', ROOT / 'app/build/reports/lint-results-mvp2.xml'),
        ('mvp3', ROOT / 'app/build/reports/lint-results-mvp3.xml'),
        ('fixture', ROOT / 'fixtures/p2-client/build/reports/lint-results-debug.xml')):
        issues = ET.parse(path).getroot().findall('issue')
        lint[label] = {severity: sum(e.attrib.get('severity') == severity for e in issues) for severity in ('Error', 'Fatal', 'Warning')}
        assert not lint[label]['Error'] and not lint[label]['Fatal'], label
    native_names = {Path(item['path']).name for item in libraries}
    system_names = {'libc.so', 'libm.so', 'libdl.so', 'liblog.so', 'libandroid.so', 'libz.so', 'libjnigraphics.so'}
    for item in libraries:
        assert set(item['needed']) <= native_names | system_names, item
    args.out.mkdir(parents=True, exist_ok=True)
    assert not any(args.out.iterdir()), 'Use a fresh output directory; never overwrite a delivered kit'
    apk_name = 'JARVIS-MVP-003.apk'
    shutil.copyfile(args.apk, args.out / apk_name)
    for name in ('GUIDE.md', 'REPORT.md', 'AUDIT.md', 'VALIDATION.md', 'DEVICE-QUALIFICATION.md', 'MODEL-PROVENANCE.json', 'EXTENSIONS.md'):
        shutil.copyfile(ROOT / 'docs/mvp-003' / name, args.out / name)
    info = {'source_sha': command('git', 'rev-parse', 'HEAD').strip(), 'source_dirty': dirty,
            'package': PACKAGE, 'label': 'JARVIS MVP 003 diagnostic', 'min_sdk': 24, 'target_sdk': 35,
            'abi': ['arm64-v8a'], 'apk_sha256': sha(args.apk.read_bytes()), 'apk_bytes': args.apk.stat().st_size,
            'certificate_sha256': re.search(r'certificate SHA-256 digest: ([0-9a-f]+)', certificate).group(1),
            'github_run_id': os.environ.get('GITHUB_RUN_ID'), 'github_repository': os.environ.get('GITHUB_REPOSITORY'),
            'classification': 'DIAGNOSTIC_NOT_OPERATIONAL', 'debuggable': True, 'permissions': requested, 'components': components, 'zipalign_16k_passed': True,
            'runtime_network_observed': False, 'model_provenance': 'NO_MODEL_SELECTED_OR_BUNDLED',
            'native_libraries': libraries, 'historical_aar_sha256': sha(historical.read_bytes()), 'bundled_models': models, 'assets_sha256': actual_assets, 'pdfbox_aar_sha256': sha(pdfbox.read_bytes()), 'tests': suites, 'lint': lint,
            'real_gguf_conversation_validated': False, 'Athena_enabled': False, 'whisper_microphone_enabled': False, 'tts_audibility_qualified_here': False,
            'offline_tts': 'manual test and user confirmation required; no physical proof in cloud',
            'core_voice_binder_enabled': False,
            'emulator_tests_executed': False, 'personal_device_installed': False,
            'signing': 'Development debug certificate; no stable update signing identity promised'}
    (args.out / 'LIRE-MOI.txt').write_text('JARVIS MVP 003 diagnostic — SANS MODELE\nAttends une autorisation explicite avant installation sur ton telephone.\nAucune conversation GGUF ni Athena validee.\nGuide telephone : https://github.com/bubth720/JAVIS-Local-v2/blob/' + info['source_sha'] + '/docs/mvp-003/GUIDE.md\nApplication distincte : ' + PACKAGE + '\nNe desinstalle aucune version existante.\n')
    (args.out / 'APK-INFO.json').write_text(json.dumps(info, indent=2, ensure_ascii=False) + '\n')
    for name, text in (('PERMISSIONS.txt', permissions), ('MANIFEST.txt', manifest), ('CERTIFICAT.txt', certificate), ('ZIPALIGN.txt', alignment_check)):
        (args.out / name).write_text(text)
    sums = ''.join(sha(f.read_bytes()) + '  ' + f.name + '\n' for f in sorted(args.out.iterdir()) if f.is_file())
    (args.out / 'SHA256SUMS.txt').write_text(sums)
    summary = f"APK: {apk_name}\nPackage: {PACKAGE}\nCommit: {info['source_sha']}\nSHA-256: {info['apk_sha256']}\nCertificate SHA-256: {info['certificate_sha256']}\nNo model, no native conversation qualification, no physical installation.\n"
    print(summary)
    if os.environ.get('GITHUB_STEP_SUMMARY'):
        with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as stream:
            stream.write('### MVP-003 — kit de laboratoire\n\n```text\n' + summary + '```\n')


if __name__ == '__main__':
    main()
