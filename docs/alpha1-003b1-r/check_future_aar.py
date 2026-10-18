#!/usr/bin/env python3
"""Inspect consumer rules only; never load JNI or build/install anything."""
import argparse
from pathlib import Path
import zipfile

REQUIRED = {'-keep class dev.ffmpegkit.llama.LlamaJNI { *; }',
            '-keepclasseswithmembernames class * { native <methods>; }',
            '-keep public class dev.ffmpegkit.llama.** { public *; }'}
def verify(path):
    with zipfile.ZipFile(path) as z:
        if 'proguard.txt' not in z.namelist():raise ValueError('consumer proguard.txt absent')
        lines={s.strip() for s in z.read('proguard.txt').decode().splitlines() if s.strip() and not s.lstrip().startswith('#')}
        if not REQUIRED <= lines:raise ValueError('consumer JNI/API rules missing or changed')
        return 'PASS: consumer JNI/API rules packaged'
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('aar',type=Path);a=p.parse_args()
    print(verify(a.aar))
