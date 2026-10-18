#!/usr/bin/env python3
"""Documentary two-file overlay; reconstruct in memory, never apply to upstream."""
import argparse
import difflib
import hashlib
import importlib.util
import json
from pathlib import Path
import subprocess
import sys
sys.dont_write_bytecode = True
HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
FOLDER = 'library/src/main/kotlin/dev/ffmpegkit/llama/'
GRADLE = 'library/build.gradle.kts'
MEASURED = FOLDER + 'LlamaMeasured.kt'
OLD_SHA = '329fc1959a2ac990f84e7b63f61239b75dd63d13b478b1f974f37b9c4a3ef97b'
HELPER = '''/** Sole FD owner: no borrowed stream, no retry of an ambiguous close. */
private inline fun <T> withOwnedDescriptor(path: String, flags: Int, mode: Int,
    block: (java.io.FileDescriptor) -> T): T {
    val fd = Os.open(path, flags, mode)
    var primary: Throwable? = null
    try { return block(fd) }
    catch (failure: Throwable) { primary = failure; throw failure }
    finally {
        try { Os.close(fd) }
        catch (close: Throwable) {
            if (primary == null) throw close
            if (primary !== close) primary.addSuppressed(close)
        }
    }
}

'''
def sha(b): return hashlib.sha256(b).hexdigest()
def module(name,path):
    spec=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m

def inputs(upstream):
    review=module('library_review',ROOT/'docs/alpha1-003a/review.py')
    patch=(ROOT/'docs/alpha1-003a/PATCH-LIBRARY-A-VALIDER.patch').read_bytes()
    assert sha(patch)==OLD_SHA
    manifest=json.loads((ROOT/'docs/alpha1-003a/PATCH_FILES.json').read_text())
    original={};candidate={}
    for name,body in review.parts(patch.decode()):
        original[name]='' if manifest[name]['base_sha256'] is None else (upstream/name).read_text()
        assert (None if not original[name] else sha(original[name].encode()))==manifest[name]['base_sha256']
        candidate[name]=review.reconstruct(original[name],body)
        assert sha(candidate[name].encode())==manifest[name]['proposed_sha256']
    assert len(candidate)==7
    return original,candidate

def replace(text,old,new):
    assert text.count(old)==1,old
    return text.replace(old,new)

def revise(before):
    after=dict(before)
    after[GRADLE]=replace(before[GRADLE],'consumerProguardFiles("proguard-rules.pro")','consumerProguardFiles("src/proguard-rules.pro")')
    s=before[MEASURED].replace('import java.io.FileInputStream\n','').replace('import java.io.FileOutputStream\n','')
    s=replace(s,'/** Private filesystem ownership',HELPER+'/** Private filesystem ownership')
    s=replace(s,'''        val sourceFd = Os.open(sourcePath, OsConstants.O_RDONLY or OsConstants.O_NOFOLLOW or OsConstants.O_CLOEXEC, 0)
        FileInputStream(sourceFd).use { input ->
            val original = Os.fstat(input.fd)''','''        withOwnedDescriptor(sourcePath, OsConstants.O_RDONLY or OsConstants.O_NOFOLLOW or OsConstants.O_CLOEXEC, 0) { sourceFd ->
            val original = Os.fstat(sourceFd)''')
    s=replace(s,'''            val outFd = Os.open(file.path, OsConstants.O_WRONLY or OsConstants.O_CREAT or
                OsConstants.O_EXCL or OsConstants.O_NOFOLLOW or OsConstants.O_CLOEXEC, 0x180) // 0600
            created = true // retained by OwnerSlot before the file was created
            FileOutputStream(outFd).use { output ->
                val own = Os.fstat(output.fd)''','''            withOwnedDescriptor(file.path, OsConstants.O_WRONLY or OsConstants.O_CREAT or
                OsConstants.O_EXCL or OsConstants.O_NOFOLLOW or OsConstants.O_CLOEXEC, 0x180) { outFd -> // 0600
                created = true // retained by OwnerSlot before the file was created
                val own = Os.fstat(outFd)''')
    s=replace(s,'val count = input.read(buffer, 0, wanted)','val count = Os.read(sourceFd, buffer, 0, wanted)')
    s=replace(s,'                        output.write(buffer, 0, count)','''                        var written = 0
                        while (written < count) {
                            checkCancelled()
                            val accepted = Os.write(outFd, buffer, written, count - written)
                            check(accepted > 0 && accepted <= count - written) { "SNAPSHOT_WRITE_STALLED" }
                            written += accepted
                        }''')
    s=replace(s,'check(input.read() == -1)', 'check(Os.read(sourceFd, buffer, 0, 1) == 0)')
    s=s.replace('Os.fstat(input.fd)','Os.fstat(sourceFd)').replace('Os.fchmod(output.fd,','Os.fchmod(outFd,').replace('output.fd.sync()','Os.fsync(outFd)')
    s=replace(s,'''        val fd = Os.open(root.path, OsConstants.O_RDONLY or OsConstants.O_NOFOLLOW, 0)
        try {''','''        withOwnedDescriptor(root.path, OsConstants.O_RDONLY or OsConstants.O_NOFOLLOW or OsConstants.O_CLOEXEC, 0) { fd ->''')
    s=replace(s,'        } finally { Os.close(fd) }','        }')
    after[MEASURED]=s
    assert {n for n in before if before[n]!=after[n]}=={GRADLE,MEASURED}
    # This region contains all admissions/native quarantine code and MUST stay identical.
    assert s[:s.index(HELPER)]==before[MEASURED][:before[MEASURED].index('/** Private filesystem ownership')].replace('import java.io.FileInputStream\n','').replace('import java.io.FileOutputStream\n','')
    return after

def diff(before,after):
    chunks=[]
    for name in sorted(before):
        if before[name]==after[name]:continue
        chunks.append(f'diff --git a/{name} b/{name}\n')
        if not before[name]:chunks.append('new file mode 100644\n')
        chunks.extend(difflib.unified_diff(before[name].splitlines(True),after[name].splitlines(True),
             fromfile='a/'+name if before[name] else '/dev/null',tofile='b/'+name))
    return ''.join(chunks)

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--upstream',type=Path,required=True);p.add_argument('--write-documents',action='store_true');args=p.parse_args()
    original,before=inputs(args.upstream);after=revise(before);patch=diff(before,after)
    # Check reduced overlay against unchanged B1 laboratory source (dry run only).
    lab=ROOT.parent/'alpha1-003b1-isolated/candidate-a'
    if lab.exists():
        for n in before: assert (lab/n).read_text()==before[n],n
        subprocess.run(['git','apply','--check','-'],cwd=lab,input=patch,text=True,check=True)
    # Check composed tree vs pristine pinned upstream without writing any candidate source.
    subprocess.run(['git','apply','--check','-'],cwd=args.upstream,input=diff(original,after),text=True,check=True)
    data={'overlay_sha256':sha(patch.encode()),'base_patch_sha256':OLD_SHA,
      'files':{n:{'b1_sha256':sha(before[n].encode()),'proposed_sha256':sha(after[n].encode())} for n in [GRADLE,MEASURED]},
      'composed_dry_check':'PASS','overlay_dry_check':'PASS' if lab.exists() else 'NOT_EXECUTED','applied':False}
    if args.write_documents:
        (HERE/'PATCH-CORRECTIONS-A-VALIDER.patch').write_text(patch)
        (HERE/'PATCH.sha256').write_text(data['overlay_sha256']+'  PATCH-CORRECTIONS-A-VALIDER.patch\n')
        (HERE/'PATCH-FILES.json').write_text(json.dumps(data,indent=2)+'\n')
    print(json.dumps(data,indent=2))
if __name__=='__main__':main()
