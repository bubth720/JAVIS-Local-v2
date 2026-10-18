#!/usr/bin/env python3
"""Specification oracle for SYNTHETIC traces, not a native backend or release authority.
Future instrumented tests must independently capture actual native events/IDs.
No synthetic trace, checksum or passing oracle can qualify JNI or authorize admission.
"""
import copy
import json
import unittest


def validate(events):
    model = None
    prepared = None
    admitted = False
    running = False
    cancelled = False
    quarantined = False
    returned = False
    retained = []
    decoded = []
    for event in events:
        kind = event['kind']
        if kind == 'load':
            assert model is None and not quarantined, 'SECOND_MODEL_OR_QUARANTINE'
            assert event['generation'] > 0 and event['private_copy'] and event['hash_verified']
            model = event
            cancelled = False
            returned = False
            continue
        assert model is not None, 'NO_OWNER'
        assert event['worker'] == model['worker'], 'WRONG_WORKER'
        assert event['generation'] == model['generation'], 'STALE_GENERATION'
        assert event['lease'] == model['lease'], 'WRONG_LEASE'
        if kind == 'prepare':
            assert not running and prepared is None and not quarantined, 'PREPARED_BUSY'
            assert event['kv_before'] == event['kv_after'], 'PREPARE_CHANGED_KV'
            assert event['system_distinct'] and event['renders'] == 1
            assert event['output'] in range(1, 257) and event['margin'] == 128
            assert 0 < event['capacity'] <= 2048
            assert event['ids'] and len(event['ids']) + event['output'] + event['margin'] <= event['capacity'], 'CONTEXT_EXCEEDED'
            prepared = event
            retained = event['ids'][:]
            decoded = []
            admitted = False
        elif kind == 'admit':
            assert prepared and not cancelled and not quarantined, 'REVOKED_ADMISSION'
            admitted = True
        elif kind == 'consume':
            assert prepared and admitted and not running and not quarantined, 'NO_PREPARED_ADMISSION'
            assert event['ticket'] == prepared['ticket'] and event['binding'] == prepared['binding'], 'WRONG_TICKET_OR_BINDING'
            assert event['age_ms'] < 5000 and event['age_ms'] >= 0, 'EXPIRED'
            assert event['renders'] == 0 and event['tokenizations'] == 0, 'RENDERED_AGAIN'
            prepared = None
            running = True
        elif kind == 'prefill':
            assert running, 'NOT_RUNNING'
            decoded += event['ids']
            assert decoded == retained[:len(decoded)], 'TOKEN_IDS_DIFFER'
        elif kind == 'native_return':
            assert running and decoded == retained, 'INCOMPLETE_PREFILL'
            running = False
            returned = True
        elif kind == 'cancel':
            cancelled = True  # intent only; neither running nor residency is cleared
        elif kind == 'output':
            assert returned and not running and not cancelled and not quarantined, 'LATE_OUTPUT'
        elif kind == 'discard':
            assert not running, 'DISCARD_WHILE_RUNNING'
            prepared = None  # no change in model ownership
        elif kind == 'unknown':
            quarantined = True
        elif kind == 'unload_confirmed':
            assert not running and prepared is None and not quarantined, 'UNLOAD_UNATTESTED'
            model = None
            admitted = False
        else:
            raise AssertionError('UNKNOWN_EVENT')
    return {'resident': model is not None, 'running': running,
            'quarantined': quarantined, 'prepared': prepared is not None}


def sample():
    common = dict(worker='synthetic-worker', generation=1, lease='synthetic-lease')
    def event(kind, **fields):
        return dict(common, kind=kind, **fields)
    return [event('load', private_copy=True, hash_verified=True),
            event('prepare', kv_before=0, kv_after=0, system_distinct=True, renders=1,
                  ids=[11, 22, 33], output=32, margin=128, capacity=512,
                  ticket='synthetic-ticket', binding='synthetic-binding'),
            event('admit'),
            event('consume', ticket='synthetic-ticket', binding='synthetic-binding',
                  age_ms=1, renders=0, tokenizations=0),
            event('prefill', ids=[11, 22]), event('prefill', ids=[33]),
            event('native_return'), event('output'), event('discard'), event('unload_confirmed')]


class OracleTests(unittest.TestCase):
    def reject(self, mutate):
        events = copy.deepcopy(sample())
        mutate(events)
        with self.assertRaises(AssertionError):
            validate(events)

    def test_valid_batched_sequence(self):
        self.assertFalse(validate(sample())['resident'])

    def test_double_model(self):
        self.reject(lambda e: e.insert(1, dict(e[0])))

    def test_wrong_worker(self):
        self.reject(lambda e: e[3].update(worker='other'))

    def test_stale_generation(self):
        self.reject(lambda e: e[3].update(generation=2))

    def test_wrong_lease(self):
        self.reject(lambda e: e[3].update(lease='other'))

    def test_wrong_ticket(self):
        self.reject(lambda e: e[3].update(ticket='other'))

    def test_wrong_binding(self):
        self.reject(lambda e: e[3].update(binding='other'))

    def test_expiration(self):
        self.reject(lambda e: e[3].update(age_ms=5000))

    def test_double_consume(self):
        self.reject(lambda e: e.insert(4, dict(e[3])))

    def test_system_not_distinct(self):
        self.reject(lambda e: e[1].update(system_distinct=False))

    def test_prepare_mutated_kv(self):
        self.reject(lambda e: e[1].update(kv_after=1))

    def test_second_render(self):
        self.reject(lambda e: e[3].update(renders=1))

    def test_second_tokenization(self):
        self.reject(lambda e: e[3].update(tokenizations=1))

    def test_changed_token_id(self):
        self.reject(lambda e: e[5].update(ids=[44]))

    def test_missing_prefill(self):
        self.reject(lambda e: e.pop(5))

    def test_context_exceeded(self):
        self.reject(lambda e: e[1].update(output=256, capacity=256))

    def test_cancel_before_admission(self):
        self.reject(lambda e: e.insert(2, dict(e[2], kind='cancel')))

    def test_cancelled_output(self):
        self.reject(lambda e: e.insert(6, dict(e[2], kind='cancel')))

    def test_cancel_does_not_end_native_call(self):
        e = sample()[:4]
        e.append(dict(e[2], kind='cancel'))
        state = validate(e)
        self.assertTrue(state['running'] and state['resident'])

    def test_discard_does_not_unload(self):
        e = sample()[:2]
        e.append(dict(e[0], kind='discard'))
        state = validate(e)
        self.assertTrue(state['resident'])
        self.assertFalse(state['prepared'])

    def test_no_unload_during_native(self):
        self.reject(lambda e: e.insert(4, dict(e[2], kind='unload_confirmed')))

    def test_unknown_blocks_release(self):
        self.reject(lambda e: e.insert(9, dict(e[2], kind='unknown')))

    def test_unknown_blocks_new_load(self):
        e = sample()[:1] + [dict(sample()[0], kind='unknown'), sample()[0]]
        with self.assertRaises(AssertionError):
            validate(e)

    def test_copy_not_private(self):
        self.reject(lambda e: e[0].update(private_copy=False))

    def test_hash_not_verified(self):
        self.reject(lambda e: e[0].update(hash_verified=False))


if __name__ == '__main__':
    suite = unittest.defaultTestLoader.loadTestsFromTestCase(OracleTests)
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    print(json.dumps({'scope': 'synthetic-specification-oracle-only',
                      'tests': result.testsRun, 'failures': len(result.failures),
                      'errors': len(result.errors), 'jni_executed': False}))
    raise SystemExit(0 if result.wasSuccessful() else 1)
