"""Synthetic GGUF metadata, never a language model or native inference proof."""
import hashlib
import importlib.util
import io
import json
from pathlib import Path
import struct
import subprocess
import sys
import tempfile
import unittest
spec=importlib.util.spec_from_file_location('probe',Path(__file__).resolve().parents[1]/'mvp4_model_probe.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
def s(value):
    b=value.encode('utf-8');return struct.pack('<Q',len(b))+b
def kv(key,kind,data):return s(key)+struct.pack('<I',kind)+data
def tensor(name='weight',kind=12,offset=0,ndim=1):
    return s(name)+struct.pack('<I',ndim)+struct.pack('<Q',256)*ndim+struct.pack('<IQ',kind,offset)
def gguf(rows=None,tensors=None,version=3):
    if rows is None:rows=[kv('general.architecture',8,s('qwen3')),kv('general.file_type',4,struct.pack('<I',15))]
    if tensors is None:tensors=[tensor()]
    data=b'GGUF'+struct.pack('<IQQ',version,len(tensors),len(rows))+b''.join(rows)+b''.join(tensors)
    return data+b'\0'*((-len(data))%32)+b'\0'*512
class ProbeTest(unittest.TestCase):
    def parse(self,data):return m.inspect_metadata(io.BytesIO(data),len(data))
    def rejects(self,data):
        with self.assertRaises((ValueError,UnicodeError,struct.error)):self.parse(data)
    def test_v2_v3_qwen_metadata_and_mixed_quantization(self):
        for v in (2,3):
            r=self.parse(gguf(tensors=[tensor(),tensor('other',14,160)],version=v))
            self.assertEqual(r['metadata']['general.architecture'],'qwen3')
            self.assertEqual(r['tensor_types'],{'Q4_K':1,'Q6_K':1})
    def test_unicode_system_template_is_preserved_not_rendered(self):
        text="SYSTEM distinct : français éèê 😀 漢字 <|im_start|>"
        r=self.parse(gguf(rows=[kv('tokenizer.chat_template',8,s(text))]))
        self.assertEqual(r['metadata']['tokenizer.chat_template'],text)
    def test_token_array_records_order_and_raw_digest_without_tokenization(self):
        a=s('é')+s('😀');r=self.parse(gguf(rows=[kv('tokenizer.ggml.tokens',9,struct.pack('<IQ',8,2)+a)]))
        v=r['metadata']['tokenizer.ggml.tokens'];self.assertEqual(v['length'],2)
        self.assertEqual(v['serialized_elements_sha256'],hashlib.sha256(a).hexdigest())
    def test_invalid_signatures_versions_and_truncations(self):
        for data in (b'',b'BAD!'+gguf()[4:],gguf(version=1),gguf(version=4),gguf()[:25]):self.rejects(data)
    def test_header_count_bounds(self):
        for tensors,keys in ((0,1),(100001,1),(1,0),(1,100001)):
            self.rejects(b'GGUF'+struct.pack('<IQQ',3,tensors,keys))
    def test_duplicate_keys_and_tensor_names(self):
        self.rejects(gguf(rows=[kv('x',4,struct.pack('<I',1))]*2))
        self.rejects(gguf(tensors=[tensor(),tensor()]))
    def test_invalid_utf8_and_oversized_string(self):
        self.rejects(gguf(rows=[struct.pack('<Q',1)+b'\xff'+struct.pack('<II',4,1)]))
        self.rejects(gguf(rows=[struct.pack('<Q',1024*1024+1)]))
    def test_array_bombs_nested_arrays_and_unknown_type(self):
        for value in (struct.pack('<IQ',8,1_000_001),struct.pack('<IQ',9,1),struct.pack('<IQ',99,1)):
            self.rejects(gguf(rows=[kv('array',9,value)]))
        self.rejects(gguf(rows=[kv('unknown',99,b'')]))
    def test_bool_float_and_alignment_refusals(self):
        for value in (kv('bool',7,b'\2'),kv('float',6,struct.pack('<f',float('nan'))),
                      kv('general.alignment',4,struct.pack('<I',3))):self.rejects(gguf(rows=[value]))
    def test_tensor_dimensions_and_offsets_are_bounded(self):
        for t in (tensor(ndim=0),tensor(ndim=5),tensor(offset=2),tensor(offset=2**63)):
            self.rejects(gguf(tensors=[t]))
    def test_reader_budget_and_truncated_array(self):
        with self.assertRaises(ValueError):m.Reader(io.BytesIO(b'x'*100),limit=2).read(3)
        with self.assertRaises(ValueError):m.Reader(io.BytesIO(b'')).read(1)
    def test_cli_refuses_wrong_model_size_and_never_claims_native_load(self):
        with tempfile.TemporaryDirectory() as td:
            p=Path(td)/'synthetic.gguf';p.write_bytes(gguf());report=Path(td)/'report.json'
            result=subprocess.run([sys.executable,m.__file__,'--model',str(p),'--report',str(report)],capture_output=True)
            self.assertEqual(result.returncode,2);r=json.loads(report.read_text())
            self.assertEqual(r['status'],'REFUSED_NOT_QUALIFIED');self.assertFalse(r['native_load_executed'])
    def test_report_cannot_overwrite_model_or_its_hardlink(self):
        with tempfile.TemporaryDirectory() as td:
            p=Path(td)/'synthetic.gguf';data=gguf();p.write_bytes(data)
            link=Path(td)/'alias';link.hardlink_to(p)
            for output in (p,link):
                r=subprocess.run([sys.executable,m.__file__,'--model',str(p),'--report',str(output)],capture_output=True)
                self.assertNotEqual(r.returncode,0);self.assertEqual(p.read_bytes(),data)
if __name__=='__main__':unittest.main()
