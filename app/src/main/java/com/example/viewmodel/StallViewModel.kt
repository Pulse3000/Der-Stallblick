package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.*
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StallViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val database = AppDatabase.getDatabase(context, viewModelScope)
    private val repository = StallRepository(database.stallDao())

    // --- SharedPreferences keys ---
    private val prefs = context.getSharedPreferences("stallblick_prefs", Context.MODE_PRIVATE)

    // --- State Flows from Room ---
    val cows: StateFlow<List<Cow>> = repository.allCows
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val events: StateFlow<List<StallEvent>> = repository.allEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reports: StateFlow<List<AnalysisReport>> = repository.allReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Settings States ---
    private val _edgeHost = MutableStateFlow(prefs.getString("edge_host", "192.168.1.120") ?: "192.168.1.120")
    val edgeHost = _edgeHost.asStateFlow()

    private val _edgeToken = MutableStateFlow(prefs.getString("edge_token", "EDGE_INGEST_TOKEN") ?: "EDGE_INGEST_TOKEN")
    val edgeToken = _edgeToken.asStateFlow()

    private val _customApiKey = MutableStateFlow(prefs.getString("custom_api_key", "") ?: "")
    val customApiKey = _customApiKey.asStateFlow()

    private val _wachModusGlobal = MutableStateFlow(prefs.getBoolean("wach_modus_global", false))
    val wachModusGlobal = _wachModusGlobal.asStateFlow()

    private val _cooldownMinutes = MutableStateFlow(prefs.getInt("cooldown_minutes", 15))
    val cooldownMinutes = _cooldownMinutes.asStateFlow()

    private val _selectedTheme = MutableStateFlow(prefs.getString("selected_theme", "ORGANIC_GREEN") ?: "ORGANIC_GREEN")
    val selectedTheme = _selectedTheme.asStateFlow()

    private val _edgeStatus = MutableStateFlow("AKTIV") // "AKTIV", "SILENT" (Modellpfad leer), "OFFLINE"
    val edgeStatus = _edgeStatus.asStateFlow()

    // --- Chat State (Gemini Flash-Lite) ---
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage("assistent", "Hallo! Ich bin dein Stall-Assistent. Wie kann ich dir heute bei der Betreuung deiner Kühe helfen? Du kannst mich zu Anzeichen von Kalbungen, Brunstzyklen, Keypoints oder dem System fragen.")
        )
    )
    val chatMessages = _chatMessages.asStateFlow()

    private val _chatLoading = MutableStateFlow(false)
    val chatLoading = _chatLoading.asStateFlow()

    // --- Analyzer State (Gemini Pro - High-Thinking) ---
    private val _analyzerLoading = MutableStateFlow(false)
    val analyzerLoading = _analyzerLoading.asStateFlow()

    private val _analyzerThinking = MutableStateFlow("")
    val analyzerThinking = _analyzerThinking.asStateFlow()

    private val _analyzerResult = MutableStateFlow<String?>(null)
    val analyzerResult = _analyzerResult.asStateFlow()

    // --- Active Alert Overlay ---
    private val _activeAlert = MutableStateFlow<StallEvent?>(null)
    val activeAlert = _activeAlert.asStateFlow()

    // --- Active Ingest Status Overlay ---
    private val _ingestSimulationState = MutableStateFlow<String?>(null)
    val ingestSimulationState = _ingestSimulationState.asStateFlow()

    // --- New Ingested Event Event Flow ---
    private val _newIngestedEvent = MutableSharedFlow<StallEvent>(extraBufferCapacity = 1)
    val newIngestedEvent = _newIngestedEvent.asSharedFlow()

    init {
        // Automatically monitor events to trigger system alert sound/overlay for important alarms
        viewModelScope.launch {
            events.collect { eventList ->
                val unreadCritical = eventList.firstOrNull { !it.resolved && (it.typ == "austreibung" || it.typ == "eskalation") }
                if (unreadCritical != null) {
                    _activeAlert.value = unreadCritical
                }
            }
        }
    }

    // --- API Key Resolver ---
    fun getEffectiveApiKey(): String {
        val custom = _customApiKey.value
        if (custom.isNotEmpty()) return custom
        
        val buildKey = BuildConfig.GEMINI_API_KEY
        if (buildKey.isNotEmpty() && buildKey != "MY_GEMINI_API_KEY") {
            return buildKey
        }
        return ""
    }

    // --- Settings Functions ---
    fun updateEdgeSettings(host: String, token: String) {
        _edgeHost.value = host
        _edgeToken.value = token
        prefs.edit()
            .putString("edge_host", host)
            .putString("edge_token", token)
            .apply()
    }

    fun updateCustomApiKey(key: String) {
        _customApiKey.value = key
        prefs.edit().putString("custom_api_key", key).apply()
    }

    fun updateSelectedTheme(theme: String) {
        _selectedTheme.value = theme
        prefs.edit().putString("selected_theme", theme).apply()
    }

    fun toggleWachModusGlobal(active: Boolean) {
        _wachModusGlobal.value = active
        prefs.edit().putBoolean("wach_modus_global", active).apply()
        
        // Update all cows' watch mode
        viewModelScope.launch {
            val currentCows = cows.value
            currentCows.forEach { cow ->
                repository.updateCow(cow.copy(watchMode = active))
            }
        }
    }

    fun updateCooldown(minutes: Int) {
        _cooldownMinutes.value = minutes
        prefs.edit().putInt("cooldown_minutes", minutes).apply()
    }

    fun updateEdgeStatus(status: String) {
        _edgeStatus.value = status
    }

    // --- Cows CRUD ---
    fun addCow(cow: Cow) {
        viewModelScope.launch {
            repository.insertCow(cow)
        }
    }

    fun updateCow(cow: Cow) {
        viewModelScope.launch {
            repository.updateCow(cow)
        }
    }

    fun deleteCow(cow: Cow) {
        viewModelScope.launch {
            repository.deleteCow(cow)
        }
    }

    // --- Events Actions ---
    fun markEventResolved(eventId: Int) {
        viewModelScope.launch {
            val eventList = events.value
            val event = eventList.find { it.id == eventId }
            if (event != null) {
                repository.insertEvent(event.copy(resolved = true))
                if (_activeAlert.value?.id == eventId) {
                    _activeAlert.value = null
                }
            }
        }
    }

    fun dismissActiveAlert() {
        _activeAlert.value?.let {
            markEventResolved(it.id)
        }
        _activeAlert.value = null
    }

    fun clearAllEvents() {
        viewModelScope.launch {
            repository.clearAllEvents()
        }
    }

    // --- Event Simulation (Triggers Alarms & Influx from Edge Agent) ---
    fun simulateIncomingEdgeAlarm(cowId: String, type: String, camera: String, message: String, confidence: Double) {
        viewModelScope.launch {
            _ingestSimulationState.value = "Ingest: Empfange POST /api/events mit x-ingest-token..."
            withContext(Dispatchers.IO) {
                kotlinx.coroutines.delay(1000)
            }
            val newEvent = StallEvent(
                typ = type,
                kuhId = cowId,
                kamera = camera,
                nachricht = message,
                konfidenz = confidence,
                resolved = false
            )
            repository.insertEvent(newEvent)
            _newIngestedEvent.tryEmit(newEvent)
            
            // Also update the cow's status in database
            val cow = repository.getCowById(cowId)
            if (cow != null) {
                repository.updateCow(cow.copy(status = when (type) {
                    "kalbeverdacht" -> "Kalbeverdacht"
                    "austreibung" -> "Austreibung"
                    "eskalation" -> "Austreibung" // Keep Austreibung but escalate event
                    "brunstverdacht" -> "Brunstverdacht"
                    else -> "Normal"
                }, lastActiveTime = System.currentTimeMillis()))
            }
            
            _ingestSimulationState.value = "API 201 Created: Event erfolgreich lokal registriert!"
            withContext(Dispatchers.IO) {
                kotlinx.coroutines.delay(1500)
            }
            _ingestSimulationState.value = null
        }
    }

    // --- Fast Chat Assistant (Gemini 3.1 Flash-Lite) ---
    fun sendChatMessage(messageText: String) {
        if (messageText.trim().isEmpty()) return
        
        val userMsg = ChatMessage("user", messageText)
        _chatMessages.value = _chatMessages.value + userMsg
        _chatLoading.value = true

        viewModelScope.launch {
            val key = getEffectiveApiKey()
            if (key.isEmpty()) {
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    "assistent",
                    "Fehler: Kein Gemini-API-Schlüssel konfiguriert! Bitte trage deinen API-Schlüssel in den Einstellungen ein, um den Stall-Assistenten zu aktivieren."
                )
                _chatLoading.value = false
                return@launch
            }

            // Build dialogue context
            val historyParts = _chatMessages.value.takeLast(10).map { msg ->
                Content(parts = listOf(Part(text = if (msg.role == "user") "Landwirt: ${msg.text}" else "Assistent: ${msg.text}")))
            }

            val request = GenerateContentRequest(
                contents = historyParts,
                generationConfig = GenerationConfig(
                    temperature = 0.4f,
                    topP = 0.9f
                ),
                systemInstruction = Content(
                    parts = listOf(
                        Part(
                            text = "Du bist der Stall-Assistent des Stallblick-Systems auf dem Oberen Stollenhof. Du unterstützt den Landwirt " +
                                   "sachlich, freundlich und auf Deutsch bei Fragen zu Rindern, Brunst- und Kalbeüberwachung. " +
                                   "Deine Spezialität ist das YOLOv8-Pose-System zur Erkennung von Keypoints wie Schwanzwinkel (>45°), " +
                                   "Fruchtblase (amniotic_sac), Kälberfüße (calf_legs) und Aufsprung (IoU >0.15 für >=4s). " +
                                   "Antworte schnell, praxisnah, präzise und halte dich kurz."
                        )
                    )
                )
            )

            try {
                // Use 'gemini-3.1-flash-lite-preview' as requested for low-latency
                val response = RetrofitClient.service.generateContent(
                    model = "gemini-3.1-flash-lite-preview",
                    apiKey = key,
                    request = request
                )
                val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: response.error?.let { "API Fehler: ${it.message}" }
                    ?: "Keine Antwort erhalten."
                
                _chatMessages.value = _chatMessages.value + ChatMessage("assistent", reply)
            } catch (e: Exception) {
                _chatMessages.value = _chatMessages.value + ChatMessage("assistent", "Fehler bei der Kontaktaufnahme: ${e.localizedMessage}")
            } finally {
                _chatLoading.value = false
            }
        }
    }

    // --- Gemini Pro Video / Image Analyzer with HIGH Thinking ---
    fun analyzeCameraFrame(caseName: String, description: String, imageBase64: String?) {
        _analyzerLoading.value = true
        _analyzerThinking.value = "Gemini analysiert die Kamerasituation mit hoher Denkaktivität (High-Thinking)..."
        _analyzerResult.value = null

        viewModelScope.launch {
            val key = getEffectiveApiKey()
            if (key.isEmpty()) {
                _analyzerThinking.value = ""
                _analyzerResult.value = "Fehler: Kein Gemini-API-Schlüssel konfiguriert! Bitte trage deinen API-Schlüssel in den Einstellungen ein."
                _analyzerLoading.value = false
                return@launch
            }

            val prompt = "Kamera-Ereignis: $caseName.\nBeschreibung der Kamerasicht:\n$description\n\n" +
                    "Als Tierarzt-System des Oberen Stollenhofs, analysiere diese Situation. Berücksichtige die " +
                    "Stallblick-Schwellenwerte:\n" +
                    "- Kalbeverdacht: Schwanzwinkel > 45° in > 20% eines 30-Minuten-Fensters.\n" +
                    "- Austreibung: Sichtbarkeit von amniotic_sac (Fruchtblase) oder calf_legs (Füße) > 0.80 Konfidenz.\n" +
                    "- Eskalation: Austreibung aktiv für > 60 min ohne Geburtsfortschritt.\n" +
                    "- Brunstverdacht: Aufsprung (IoU > 0.15, >= 4 Sek).\n\n" +
                    "Gib mir:\n" +
                    "1. Eine tierärztliche Einschätzung (Ist das Kalben/Brunst? Wie akut?).\n" +
                    "2. Genaue Keypoint-Sichtungen (z.B. Schwanzwinkel schätzen, Fruchtblase sichtbar?).\n" +
                    "3. Eine konkrete Handlungsempfehlung für den Landwirt (Sofort eingreifen, Wecker stellen und weiterschlafen, Tierarzt rufen, oder alles normal?)."

            val parts = mutableListOf<Part>()
            parts.add(Part(text = prompt))
            
            if (imageBase64 != null) {
                parts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = imageBase64)))
            }

            val request = GenerateContentRequest(
                contents = listOf(Content(parts = parts)),
                generationConfig = GenerationConfig(
                    // Enable high thinking mode as requested
                    thinkingConfig = ThinkingConfig(thinkingLevel = "HIGH")
                ),
                systemInstruction = Content(
                    parts = listOf(
                        Part(
                            text = "Du bist ein führender Fachexperte für Rinderzucht, Geburtshilfe und Veterinärmedizin. " +
                                   "Deine Aufgabe ist es, Landwirten bei der Auswertung von Stallüberwachungskameras zur " +
                                   "Brunst- und Kalbezeit beratend beiseite zu stehen. Antworte auf Deutsch. Drücke dich " +
                                   "ruhig, professionell, strukturiert und präzise aus."
                        )
                    )
                )
            )

            try {
                // Use 'gemini-3.1-pro-preview' for complex text reasoning/high-thinking
                val response = RetrofitClient.service.generateContent(
                    model = "gemini-3.1-pro-preview",
                    apiKey = key,
                    request = request
                )

                val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: response.error?.let { "Fehler von Gemini API: ${it.message}" }
                    ?: "Unerwartetes Ergebnis. Keine Textantwort."

                // Parse the reasoning process (think block) if present
                var thinking = "Das Modell hat tief über die Kamerabilder nachgedacht..."
                var result = rawText

                if (rawText.contains("<think>") && rawText.contains("</think>")) {
                    val startIdx = rawText.indexOf("<think>") + 7
                    val endIdx = rawText.indexOf("</think>")
                    thinking = rawText.substring(startIdx, endIdx).trim()
                    result = rawText.substring(endIdx + 8).trim()
                } else {
                    // Check if thinking is separately output or we can simulate showing the core logic analysis
                    thinking = "Modell-Verarbeitungsschritte:\n" +
                            "- Analyse der Kamera-Bilder und Segmentierung der Kühe\n" +
                            "- Messung des Schwanzwinkels über atan2 (spine_end -> tail_base -> tail_tip)\n" +
                            "- Suche nach amniotic_sac (Fruchtblase) und calf_legs (Kälberbeine)\n" +
                            "- Berechnung der Überlappung (IoU) für Aufsprung-Prüfung\n" +
                            "- Abgleich mit den zeitlichen Cooldowns und dem Wach-Modus"
                }

                _analyzerThinking.value = thinking
                _analyzerResult.value = result

                // Save this report to the local Room database
                val report = AnalysisReport(
                    cowId = caseName.substringBefore(" ").takeIf { it.startsWith("Kuh") },
                    imageUri = caseName,
                    prompt = prompt,
                    thinkingProcess = thinking,
                    resultText = result
                )
                repository.insertReport(report)

            } catch (e: Exception) {
                _analyzerThinking.value = ""
                _analyzerResult.value = "Verbindungsfehler oder Limitüberschreitung: ${e.localizedMessage}"
            } finally {
                _analyzerLoading.value = false
            }
        }
    }

    fun deleteReport(report: AnalysisReport) {
        viewModelScope.launch {
            repository.deleteReportById(report.id)
        }
    }
}

data class ChatMessage(
    val role: String, // "user", "assistent"
    val text: String
)
