#!/usr/bin/env python3
"""Offline/read-only GGUF inspection. No download, JNI, admission, model load or install."""
import argparse
from collections import Counter
import hashlib
import json
import math
import os
from pathlib import Path
import struct

REVISION = '1208e45d782fe18602c5eaf10e5758d5b0f24c03'
EXPECTED_SHA256 = 'b0638f08417a2d3c8652760462eb5407c6e30173cf9608ad0820757a281eea0e'
EXPECTED_BYTES = 396704416
FORMATS = {0:'B',1:'b',2:'H',3:'h',4:'I',5:'i',6:'f',7:'B',10:'Q',11:'q',12:'d'}
TENSOR_TYPES = {0:'F32',1:'F16',2:'Q4_0',3:'Q4_1',6:'Q5_0',7:'Q5_1',8:'Q8_0',
                10:'Q2_K',11:'Q3_K',12:'Q4_K',13:'Q5_K',14:'Q6_K',15:'Q8_K',30:'BF16'}

class Reader:
    def __init__(self, f, limit=64*1024*1024):
        self.f=f;self.limit=limit
    def read(self,n):
        if n<0 or self.f.tell()+n>self.limit:raise ValueError('GGUF metadata exceeds bounded inspection budget')
        b=self.f.read(n)
        if len(b)!=n:raise ValueError('Truncated GGUF metadata')
        return b
    def number(self,fmt):return struct.unpack('<'+fmt,self.read(struct.calcsize('<'+fmt)))[0]
    def string(self):
        n=self.number('Q')
        if n>1024*1024:raise ValueError('GGUF string exceeds inspection limit')
        return self.read(n).decode('utf-8',errors='strict')
    def value(self,kind,array=False):
        if kind==8:return self.string()
        if kind in FORMATS:
            v=self.number(FORMATS[kind])
            if kind==7:
                if v not in (0,1):raise ValueError('Invalid GGUF bool')
                return bool(v)
            if isinstance(v,float) and not math.isfinite(v):raise ValueError('Non-finite GGUF value')
            return v
        if kind==9 and not array:
            subtype=self.number('I');n=self.number('Q')
            if subtype not in FORMATS and subtype!=8:raise ValueError('Unsupported/nested GGUF array')
            if n>1_000_000:raise ValueError('GGUF array exceeds inspection limit')
            start=self.f.tell()
            for _ in range(n):self.value(subtype,array=True)
            end=self.f.tell();self.f.seek(start);d=hashlib.sha256()
            while self.f.tell()<end:d.update(self.read(min(1024*1024,end-self.f.tell())))
            return {'kind':'array','element_type':subtype,'length':n,'serialized_elements_sha256':d.hexdigest()}
        raise ValueError('Unsupported GGUF metadata type')

def inspect_metadata(f,file_size):
    r=Reader(f, min(file_size,64*1024*1024))
    if r.read(4)!=b'GGUF':raise ValueError('GGUF little-endian signature absent')
    version=r.number('I')
    if version not in (2,3):raise ValueError('Unsupported GGUF version')
    tensors=r.number('Q');count=r.number('Q')
    if not 1<=tensors<=100_000 or not 1<=count<=100_000:raise ValueError('Invalid GGUF header counts')
    values={}
    for _ in range(count):
        key=r.string()
        if key in values:raise ValueError('Duplicate GGUF key')
        values[key]=r.value(r.number('I'))
    names=set();types=Counter();offsets=[]
    for _ in range(tensors):
        name=r.string()
        if name in names:raise ValueError('Duplicate tensor name')
        names.add(name);ndim=r.number('I')
        if not 1<=ndim<=4:raise ValueError('Invalid tensor dimension count')
        shape=[r.number('Q') for _ in range(ndim)]
        if any(n==0 or n>2**31 for n in shape):raise ValueError('Invalid tensor shape')
        kind=r.number('I');offset=r.number('Q');types[kind]+=1;offsets.append(offset)
    alignment=values.get('general.alignment',32)
    if type(alignment) is not int or not 1<=alignment<=4096 or alignment&(alignment-1):raise ValueError('Invalid tensor alignment')
    data_start=((f.tell()+alignment-1)//alignment)*alignment
    if any(offset%alignment or data_start+offset>=file_size for offset in offsets):raise ValueError('Invalid tensor offset')
    return {'gguf_version':version,'tensor_count':tensors,'metadata_count':count,'metadata':values,
            'tensor_types':{TENSOR_TYPES.get(k,'UNKNOWN_'+str(k)):n for k,n in sorted(types.items())},
            'tensor_data_start':data_start,'inspection':'Metadata/descriptors only; no tensor decoding or native tokenization'}

def identity(s):return (s.st_dev,s.st_ino,s.st_size,s.st_mtime_ns,s.st_ctime_ns)

def probe(path):
    path=Path(path).resolve(strict=True)
    repo=Path(__file__).resolve().parents[1]
    if path.is_relative_to(repo):raise ValueError('Model must remain outside the repository')
    before=path.stat()
    if before.st_size!=EXPECTED_BYTES:raise ValueError('Candidate exact size mismatch')
    with path.open('rb') as f:
        if identity(before)!=identity(os.fstat(f.fileno())):raise ValueError('Model changed before opening')
        d=hashlib.sha256()
        while True:
            chunk=f.read(1024*1024)
            if not chunk:break
            d.update(chunk)
        actual=d.hexdigest()
        if actual!=EXPECTED_SHA256:raise ValueError('Candidate SHA-256 mismatch')
        f.seek(0);result=inspect_metadata(f,before.st_size)
        if identity(before)!=identity(os.fstat(f.fileno())) or identity(before)!=identity(path.stat()):raise ValueError('Model changed during inspection')
    values=result['metadata']
    if values.get('general.architecture')!='qwen3':raise ValueError('Candidate architecture is not qwen3')
    if values.get('general.file_type')!=15 or not result['tensor_types'].get('Q4_K'):raise ValueError('Candidate is not declared Q4_K_M')
    return {'status':'LOCAL_BYTES_VERIFIED_NOT_NATIVE_QUALIFIED','repository':'Qwen/Qwen3-0.6B-GGUF',
            'revision':REVISION,'filename':'Qwen3-0.6B-Q4_K_M.gguf','actual_sha256':actual,'actual_bytes':before.st_size,
            'native_load_executed':False,'tokenizer_executed':False,'license_verification':'Separate pinned LICENSE review required',
            'gguf':result}

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--model',type=Path,required=True,help='Already authorized local public GGUF; never downloads')
    p.add_argument('--report',type=Path,required=True)
    args=p.parse_args()
    # Reject output aliases before writing: a report must never overwrite a model.
    if args.report.resolve()==args.model.resolve():p.error('Report must not overwrite model')
    try:
        if args.report.exists() and args.model.exists() and os.path.samefile(args.report,args.model):p.error('Report aliases model')
        result=probe(args.model);code=0
    except (OSError,ValueError,UnicodeError,struct.error) as error:
        result={'status':'REFUSED_NOT_QUALIFIED','error':str(error),'native_load_executed':False};code=2
    args.report.parent.mkdir(parents=True,exist_ok=True)
    args.report.write_text(json.dumps(result,indent=2,ensure_ascii=False,allow_nan=False)+'\n')
    print(result['status'])
    return code

if __name__=='__main__':raise SystemExit(main())
