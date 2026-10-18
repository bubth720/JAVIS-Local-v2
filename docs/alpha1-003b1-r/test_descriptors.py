#!/usr/bin/env python3
"""Test the proposed helper on JVM with fake Os, not Android FD/native behavior."""
import argparse
from pathlib import Path
import subprocess
import sys
import tempfile
sys.dont_write_bytecode=True
import proposal
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--tools',type=Path,required=True);p.add_argument('--scratch',type=Path,required=True)
a=p.parse_args();a.scratch.mkdir(parents=True,exist_ok=True)
assert not a.scratch.resolve().is_relative_to(proposal.ROOT)
w=Path(tempfile.mkdtemp(prefix='descriptor-double-',dir=a.scratch))
s=(proposal.HERE/'DescriptorChecks.kt.fixture').read_text().replace('// EXACT_HELPER_INSERTED_HERE',proposal.HELPER)
(w/'Checks.kt').write_text(s)
cache=a.tools/'gradle-proxy-home/caches/modules-2/files-2.1'
def jar(g,n,v):return next((cache/g/n/v).rglob('*.jar'))
compiler=[jar('org.jetbrains.kotlin',n,'2.0.21') for n in ['kotlin-compiler-embeddable','kotlin-stdlib','kotlin-script-runtime','kotlin-daemon-embeddable']]
compiler += [jar('org.jetbrains.kotlin','kotlin-reflect','1.6.10'),jar('org.jetbrains','annotations','13.0'),jar('org.jetbrains.kotlinx','kotlinx-coroutines-core-jvm','1.9.0')]
compiler+=list((cache/'org.jetbrains.intellij.deps/trove4j').rglob('*.jar'))
std=jar('org.jetbrains.kotlin','kotlin-stdlib','2.0.21');java=a.tools/'jdk-17.0.16+8/bin/java'
subprocess.run([str(java),'-cp',':'.join(map(str,compiler)),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-jvm-target','11','-classpath',str(std),'-d',str(w/'checks.jar'),str(w/'Checks.kt')],check=True)
subprocess.run([str(java),'-cp',str(w/'checks.jar')+':'+str(std),'dev.ffmpegkit.llama.ChecksKt'],check=True)
