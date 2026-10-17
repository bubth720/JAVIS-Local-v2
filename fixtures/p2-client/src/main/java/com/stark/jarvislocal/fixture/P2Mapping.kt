package com.stark.jarvislocal.fixture

import com.stark.jarvislocal.InferenceRequest

/** Exact 003E synthetic-only mapping. Never flatten or silently omit CORE context. */
internal object P2Mapping {
    val receiptNames = Wire.keyNames + setOf("request_digest","phase","code","native_ever_started","effective_end",
        "model_unloaded","release_confirmed","model_sha256","template_sha256","rendered_sha256","input_tokens",
        "context_tokens","loaded_context_tokens","threads","output_tokens","text")
    fun key(key: PilotKey): Map<String,String> = mapOf("version" to Wire.VERSION,"contract" to Wire.contract,
        "epoch" to key.epoch,"session_id" to key.sessionId,"operation_id" to key.operationId,
        "correlation_id" to key.correlationId,"cancellation_id" to key.cancellationId,
        "authorization_ref" to key.authorizationRef,"lease_id" to key.leaseId,"generation" to key.generation.toString())
    fun request(request: InferenceRequest, key: PilotKey, model: String, sha: String, profile: String, remaining: Long): Map<String,String> {
        PilotRequestGate.validate(request)
        requirePilot(request.sessionId == key.sessionId && request.requestId == key.operationId, "STALE_OPERATION")
        requirePilot(listOf(key.epoch,key.sessionId,key.operationId,key.correlationId,key.cancellationId,key.authorizationRef,key.leaseId).all(::canonicalUuid) &&
            key.generation in 1..9007199254740991L, "INVALID_KEY")
        requirePilot(request.question in setOf("Dis bonjour en français.","Combien font deux plus deux ?") &&
            request.systemPrompt in setOf("","Réponds brièvement en français. /no_think","Réponds en une phrase en français. /no_think"), "SYNTHETIC_ONLY")
        requirePilot(model in setOf("SMALL","MAIN") && sha.matches(Regex("[0-9a-f]{64}")) &&
            profile in setOf("TABLET_ECO","ECO","BALANCED","AUTO") && remaining in 1..90000 &&
            (model != "MAIN" || request.limits.contextSize >= 768), "UNSUPPORTED_P2_LIMITS")
        return key(key) + mapOf("system" to request.systemPrompt,"question" to request.question,"model" to model,
            "model_sha256" to sha,"profile" to profile,"context_tokens" to request.limits.contextSize.toString(),
            "output_tokens" to request.limits.maxTokens.toString(),"remaining_ms" to remaining.toString(),"synthetic" to "true")
    }
    fun query(request: Map<String,String>, clientUid: Int, serverUid: Int): Map<String,String> =
        request.filterKeys { it in Wire.keyNames || it in setOf("model","model_sha256","profile","context_tokens","output_tokens","remaining_ms") } +
            mapOf("request_digest" to Wire.digest(request),"client_uid" to clientUid.toString(),"server_uid" to serverUid.toString())

    /** Correlation is checked even for refusal/cleanup receipts; no optimistic interpretation. */
    fun correlate(request: Map<String,String>, receipt: Map<String,String>) {
        requirePilot(receipt.keys == receiptNames, "INVALID_RECEIPT")
        requirePilot(request.filterKeys { it in Wire.keyNames }.all { receipt[it.key] == it.value } &&
            receipt["request_digest"] == Wire.digest(request), "STALE_RECEIPT")
        for (field in listOf("native_ever_started","effective_end","model_unloaded","release_confirmed"))
            requirePilot(receipt[field] in setOf("true","false"), "INVALID_PROOF")
    }
    fun released(receipt: Map<String,String>) = receipt["phase"] == "UNLOADED" &&
        listOf("effective_end","model_unloaded","release_confirmed").all { receipt[it] == "true" }
    fun finalText(request: Map<String,String>, receipt: Map<String,String>): String {
        correlate(request,receipt)
        requirePilot(released(receipt) && receipt["native_ever_started"] == "true" && receipt["code"] == "NONE", "NATIVE_SUCCESS_NOT_ATTESTED")
        requirePilot(receipt["model_sha256"] == request["model_sha256"], "NATIVE_LIMITS_MISMATCH")
        for (field in listOf("template_sha256","rendered_sha256"))
            requirePilot(receipt.getValue(field).matches(Regex("[0-9a-f]{64}")), "NATIVE_DIGEST_MISSING")
        fun positive(field: String,max: Long): Long {
            val text=receipt.getValue(field)
            val n=text.toLongOrNull()
            requirePilot(text.matches(Regex("[1-9][0-9]{0,15}")) && n != null && n in 1..max,"NATIVE_BUDGET_MISMATCH")
            return n ?: throw PilotBlocked("NATIVE_BUDGET_MISMATCH")
        }
        val context=positive("context_tokens",request.getValue("context_tokens").toLong())
        val output=positive("output_tokens",request.getValue("output_tokens").toLong())
        val input=positive("input_tokens",2048)
        val loaded=positive("loaded_context_tokens",2048)
        positive("threads",256)
        requirePilot(context >= 512 && context % 256 == 0L && loaded >= context && input + output <= context,"NATIVE_BUDGET_MISMATCH")
        requirePilot(receipt.getValue("text").isNotBlank(), "EMPTY_NATIVE_TEXT")
        return receipt.getValue("text")
    }
}
