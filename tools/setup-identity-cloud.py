#!/usr/bin/env python3
"""Provision pinned developer tools only. Uses inherited proxy/TLS; never downloads a model/APK."""
import hashlib
import os
from pathlib import Path
import shutil
import subprocess
import tarfile
import urllib.parse
import urllib.request
import zipfile

ROOT=Path(os.environ.get('JARVIS_TOOL_ROOT','/workspace/.jarvis-core-tools'))
ARTIFACTS=[
 ('gradle.zip','https://downloads.gradle.org/distributions/gradle-8.10.2-bin.zip','31c55713e40233a8303827ceb42ca48a47267a0ad4bab9177123121e71524c26'),
 ('jdk17.tar.gz','https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.16%2B8/OpenJDK17U-jdk_x64_linux_hotspot_17.0.16_8.tar.gz','166774efcf0f722f2ee18eba0039de2d685b350ee14d7b69e6f83437dafd2af1'),
 ('android-tools.zip','https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip','2d2d50857e4eb553af5a6dc3ad507a17adf43d115264b1afc116f95c92e5e258')]

def main():
    ROOT.mkdir(parents=True,exist_ok=True)
    for name,url,expected in ARTIFACTS:
        path=ROOT/name
        if not path.exists():
            partial=ROOT/(name+'.part')
            with urllib.request.urlopen(url,timeout=60) as src,partial.open('wb') as dst:
                shutil.copyfileobj(src,dst,1024*1024)
            if hashlib.sha256(partial.read_bytes()).hexdigest()!=expected:raise RuntimeError('Checksum failed: '+name)
            partial.replace(path)
        if hashlib.sha256(path.read_bytes()).hexdigest()!=expected:raise RuntimeError('Checksum failed: '+name)
    if not (ROOT/'gradle-8.10.2/bin/gradle').exists():
        with zipfile.ZipFile(ROOT/'gradle.zip') as z:z.extractall(ROOT)
    (ROOT/'gradle-8.10.2/bin/gradle').chmod(0o755)
    if not (ROOT/'jdk-17.0.16+8/bin/javac').exists():
        with tarfile.open(ROOT/'jdk17.tar.gz') as t:t.extractall(ROOT,filter='data')
    sdk=ROOT/'android-sdk';dest=sdk/'cmdline-tools/verified'
    if not (dest/'bin/sdkmanager').exists():
        dest.mkdir(parents=True,exist_ok=True)
        with zipfile.ZipFile(ROOT/'android-tools.zip') as z:
            for info in z.infolist():
                if info.filename.startswith('cmdline-tools/'):
                    info.filename=info.filename.removeprefix('cmdline-tools/')
                    if info.filename:z.extract(info,dest)
    for script in (dest/'bin').iterdir():script.chmod(0o755)
    for name in ('user-home','gradle-home'):(ROOT/name).mkdir(exist_ok=True)
    trust=ROOT/'cacerts'
    if not trust.exists():shutil.copyfile('/etc/ssl/certs/java/cacerts',trust)
    keytool=ROOT/'jdk-17.0.16+8/bin/keytool'
    for cert in Path('/usr/local/share/ca-certificates').glob('*.crt'):
        alias='jarvis-ca-'+hashlib.sha256(cert.read_bytes()).hexdigest()[:16]
        query=[str(keytool),'-keystore',str(trust),'-storepass','changeit','-alias',alias]
        if subprocess.run(query+['-list'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL).returncode:
            subprocess.run(query+['-importcert','-noprompt','-file',str(cert)],check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
    env=os.environ.copy();env['JAVA_HOME']=str(ROOT/'jdk-17.0.16+8')
    env['JAVA_TOOL_OPTIONS']=env.get('JAVA_TOOL_OPTIONS','')+f' -Duser.home={ROOT}/user-home -Djavax.net.ssl.trustStore={trust}'
    proxy=urllib.parse.urlparse(env.get('HTTPS_PROXY') or env.get('HTTP_PROXY',''))
    if not proxy.hostname or not proxy.port:raise RuntimeError('Configured session proxy required')
    cmd=[str(dest/'bin/sdkmanager'),'--sdk_root='+str(sdk),'--proxy=http','--proxy_host='+proxy.hostname,'--proxy_port='+str(proxy.port),
         'platforms;android-35','build-tools;34.0.0','build-tools;35.0.0']
    subprocess.run(cmd,input='y\n'*100,text=True,env=env,check=True)
    repo=Path(__file__).resolve().parents[1];local=repo/'local.properties'
    if not local.exists():local.write_text('sdk.dir='+str(sdk)+'\n')
    print('Pinned Java17/Gradle8.10.2/SDK35 ready; no model or APK installed.')
if __name__=='__main__':main()
