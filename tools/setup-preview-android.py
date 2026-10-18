#!/usr/bin/env python3
"""Pinned Android command-line tooling for CI, no models/native build/device installation."""
import argparse
import hashlib
import os
from pathlib import Path
import subprocess
import urllib.parse
import urllib.request
import zipfile

URL = 'https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip'
SHA = '2d2d50857e4eb553af5a6dc3ad507a17adf43d115264b1afc116f95c92e5e258'


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--sdk-root', type=Path, default=Path(os.environ.get('RUNNER_TEMP', '/tmp')) / 'jarvis-preview-sdk')
    args = p.parse_args()
    root = args.sdk_root.resolve()
    tools = root / 'cmdline-tools/verified'
    manager = tools / 'bin/sdkmanager'
    root.mkdir(parents=True, exist_ok=True)
    if not manager.exists():
        archive = root / 'verified-commandlinetools.zip'
        if not archive.exists():
            with urllib.request.urlopen(URL, timeout=60) as source, archive.open('xb') as target:
                while block := source.read(1024 * 1024):
                    target.write(block)
        assert hashlib.sha256(archive.read_bytes()).hexdigest() == SHA, 'SDK tooling checksum mismatch'
        with zipfile.ZipFile(archive) as zipped:
            for entry in zipped.infolist():
                relative = Path(entry.filename).relative_to('cmdline-tools')
                assert '..' not in relative.parts and not relative.is_absolute()
                dest = tools / relative
                if entry.is_dir():
                    dest.mkdir(parents=True, exist_ok=True)
                else:
                    dest.parent.mkdir(parents=True, exist_ok=True)
                    dest.write_bytes(zipped.read(entry))
        for executable in (tools / 'bin').iterdir():
            executable.chmod(0o755)
    cmd = [str(manager), '--sdk_root=' + str(root)]
    proxy = urllib.parse.urlparse(os.environ.get('HTTPS_PROXY') or os.environ.get('HTTP_PROXY', ''))
    if proxy.hostname and proxy.port:
        cmd += ['--proxy=http', '--proxy_host=' + proxy.hostname, '--proxy_port=' + str(proxy.port)]
    cmd += ['platforms;android-35', 'build-tools;35.0.0']
    result = subprocess.run(cmd, input='y\n' * 100, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    if result.returncode:
        detail = result.stdout[-3500:]
        print(detail)
        if os.environ.get('GITHUB_ACTIONS'):
            escaped = detail.replace('%', '%25').replace('\r', '%0D').replace('\n', '%0A')
            print('::error title=Android SDK preparation failed::' + escaped)
        raise SystemExit(result.returncode)
    assert (root / 'platforms/android-35/android.jar').is_file()
    assert (root / 'build-tools/35.0.0/aapt').is_file()
    if os.environ.get('GITHUB_ENV'):
        with open(os.environ['GITHUB_ENV'], 'a') as stream:
            stream.write(f'ANDROID_HOME={root}\nANDROID_SDK_ROOT={root}\n')
        with open(os.environ['GITHUB_PATH'], 'a') as stream:
            stream.write(str(tools / 'bin') + '\n')
    print('Verified Android SDK35 / build-tools35.0.0 ready; no model, emulator or APK installed.')


if __name__ == '__main__':
    main()
