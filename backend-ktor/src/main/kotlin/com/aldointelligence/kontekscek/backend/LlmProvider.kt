package com.aldointelligence.kontekscek.backend

interface LlmProvider {
    suspend fun analyze(request: AnalyzeRequest): AnalyzeResponse
}
