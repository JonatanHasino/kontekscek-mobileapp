package com.aldointelligence.kontekscek.backend

import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

data class ChecklistTemplateDto(
    val id: Long = 0,
    val mode: String, // KULIAH, KERJA, KLIEN
    val institution: String, // mis. "UIB - Prodi TI"
    val items: List<String>
)

data class AnonymousMetricDto(
    val mode: String,
    val durationSeconds: Long,
    val issuesCaught: Int,
    val completedAllChecklist: Boolean
)

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(ContentNegotiation) { gson { setPrettyPrinting() } }

        val templateSeq = AtomicLong(1)
        val templates = ConcurrentHashMap<Long, ChecklistTemplateDto>()
        val metricsStore = mutableListOf<AnonymousMetricDto>()

        // Seed 1 templat awal UIB
        val initialId = templateSeq.getAndIncrement()
        templates[initialId] = ChecklistTemplateDto(
            id = initialId,
            mode = "KULIAH",
            institution = "Standar Akademik UIB (Kemendikbudristek)",
            items = listOf(
                "Kesesuaian dengan rubrik tugas dosen",
                "Verifikasi sitasi dan tahun publikasi jurnal asli",
                "Parafrase & dokumentasi bagian bantuan AI",
                "Pemahaman lisan atas argumen utama",
                "Format penulisan sesuai pedoman prodi"
            )
        )

        routing {
            get("/health") {
                call.respond(mapOf("status" to "OK", "service" to "KonteksCek Backend API"))
            }

            // ===== CRUD TEMPLAT CHECKLIST (PRO / TIM) =====
            route("/api/v1/templates") {
                get {
                    call.respond(templates.values.toList())
                }
                post {
                    val req = call.receive<ChecklistTemplateDto>()
                    val id = templateSeq.getAndIncrement()
                    val created = req.copy(id = id)
                    templates[id] = created
                    call.respond(HttpStatusCode.Created, created)
                }
                put("/{id}") {
                    val id = call.parameters["id"]?.toLongOrNull()
                        ?: return@put call.respond(HttpStatusCode.BadRequest)
                    if (!templates.containsKey(id)) return@put call.respond(HttpStatusCode.NotFound)
                    val updated = call.receive<ChecklistTemplateDto>().copy(id = id)
                    templates[id] = updated
                    call.respond(updated)
                }
                delete("/{id}") {
                    val id = call.parameters["id"]?.toLongOrNull()
                        ?: return@delete call.respond(HttpStatusCode.BadRequest)
                    templates.remove(id) ?: return@delete call.respond(HttpStatusCode.NotFound)
                    call.respond(HttpStatusCode.NoContent)
                }
            }

            // ===== TELEMETRI ANONIM (TANPA TEKS SENSITIF - PATUH UU PDP) =====
            route("/api/v1/metrics") {
                post {
                    val metric = call.receive<AnonymousMetricDto>()
                    metricsStore.add(metric)
                    call.respond(HttpStatusCode.Created, mapOf("recorded" to true))
                }
                get("/summary") {
                    val total = metricsStore.size
                    val under5Min = metricsStore.count { it.durationSeconds <= 300 }
                    val avgSec = if (total == 0) 0.0 else metricsStore.map { it.durationSeconds }.average()
                    call.respond(
                        mapOf(
                            "totalSessions" to total,
                            "underFiveMinutesCount" to under5Min,
                            "avgDurationSeconds" to avgSec,
                            "totalIssuesCaught" to metricsStore.sumOf { it.issuesCaught }
                        )
                    )
                }
            }
        }
    }.start(wait = true)
}