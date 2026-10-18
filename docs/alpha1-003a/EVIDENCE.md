# Contrôles obtenus — ALPHA1-003A

## Mutation du vérificateur, pas du moteur

```text
test_abandonment_hook_removed_detected (__main__.StaticReviewTests.test_abandonment_hook_removed_detected) ... ok
test_all_26_checks (__main__.StaticReviewTests.test_all_26_checks) ... ok
test_all_hunks_roundtrip_in_memory (__main__.StaticReviewTests.test_all_hunks_roundtrip_in_memory) ... ok
test_caller_gate_clobber_detected (__main__.StaticReviewTests.test_caller_gate_clobber_detected) ... ok
test_cancel_release_in_handler_detected (__main__.StaticReviewTests.test_cancel_release_in_handler_detected) ... ok
test_destination_core_hidden_in_header_refused (__main__.StaticReviewTests.test_destination_core_hidden_in_header_refused) ... ok
test_directory_sync_removed_detected (__main__.StaticReviewTests.test_directory_sync_removed_detected) ... ok
test_eof_probe_removed_detected (__main__.StaticReviewTests.test_eof_probe_removed_detected) ... ok
test_exclusive_flag_removed_detected (__main__.StaticReviewTests.test_exclusive_flag_removed_detected) ... ok
test_file_identity_check_removed_detected (__main__.StaticReviewTests.test_file_identity_check_removed_detected) ... ok
test_future_cancel_detected (__main__.StaticReviewTests.test_future_cancel_detected) ... ok
test_missing_plus_header_refused (__main__.StaticReviewTests.test_missing_plus_header_refused) ... ok
test_model_hash_removed_detected (__main__.StaticReviewTests.test_model_hash_removed_detected) ... ok
test_private_root_removed_detected (__main__.StaticReviewTests.test_private_root_removed_detected) ... ok
test_raw_handle_retention_moved_detected (__main__.StaticReviewTests.test_raw_handle_retention_moved_detected) ... ok
test_recursive_cleanup_detected (__main__.StaticReviewTests.test_recursive_cleanup_detected) ... ok
test_reentrancy_guard_removed_detected (__main__.StaticReviewTests.test_reentrancy_guard_removed_detected) ... ok
test_retry_unknown_native_detected (__main__.StaticReviewTests.test_retry_unknown_native_detected) ... ok
test_seven_targets_no_core (__main__.StaticReviewTests.test_seven_targets_no_core) ... ok
test_source_parent_destination_detected (__main__.StaticReviewTests.test_source_parent_destination_detected) ... ok
test_three_native_units_identical_to_prior_proposal (__main__.StaticReviewTests.test_three_native_units_identical_to_prior_proposal) ... ok
test_unbounded_read_detected (__main__.StaticReviewTests.test_unbounded_read_detected) ... ok

----------------------------------------------------------------------
Ran 22 tests in 0.033s

OK
{"scope": "static-checker-mutations-only", "tests": 22, "failures": 0, "errors": 0, "jni_executed": false}
```

## Contrôle de types Kotlin uniquement

```json
{
  "kotlin_typecheck": "PASS",
  "jvm_signatures": {
    "Llama": {
      "old_public_declarations": 9,
      "all_present": true
    },
    "LlamaJNI": {
      "old_public_declarations": 6,
      "all_present": true
    },
    "LlamaModel": {
      "old_public_declarations": 6,
      "all_present": true
    }
  },
  "kotlin_source_files": 7,
  "jni_loaded": false,
  "native_compiled": false,
  "aar_built": false,
  "patch_applied": false
}
```

Contrôle à blanc du diff : `git apply --check`, exit0, aucune application.
