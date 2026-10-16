package com.stark.jarvislocal

/** Each synthetic test process owns its admission, without resetting the production singleton. */
internal fun isolatedInferenceFactory(
    historical: () -> LocalLanguageModel,
    delegatedSimulator: (() -> LocalLanguageModel)? = null
) = InferenceEngineFactory(historical, delegatedSimulator, ProcessInferenceAdmission())
