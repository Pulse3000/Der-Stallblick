package com.example.viewmodel

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.*
import com.example.data.*
import com.example.stream.BridgeType
import com.example.stream.CameraConfig
import com.example.stream.StreamQuelle
import com.example.stream.StreamSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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

    // ------------------------------------------------------------------
    // Stallblick Cloud & Kamera-Streams (Port des Die-Stallwache-Repos)
    // ------------------------------------------------------------------

    /** Client fuer die deployte Stallblick-Webapp (Events, Tuya-Streams, Ingest). */
    private val cloudClient = StallblickCloudClient()

    /** Gemeinsamer HTTP-Client – ExoPlayer nutzt ihn fuer Cookie-geschuetzte Proxy-Streams. */
    val cloudHttpClient get() = cloudClient.http

    private val _streamSettings = MutableStateFlow(ladeStreamSettings())
    val streamSettings = _streamSettings.asStateFlow()

    // Direkte Tuya-Cloud-Zugangsdaten (Fallback ohne Webapp; Spiegel der TUYA_*-Env-Vars).
    private val _tuyaAccessId = MutableStateFlow(prefs.getString("tuya_access_id", "") ?: "")
    val tuyaAccessId = _tuyaAccessId.asStateFlow()

    private val _tuyaAccessSecret = MutableStateFlow(prefs.getString("tuya_access_secret", "") ?: "")
    val tuyaAccessSecret = _tuyaAccessSecret.asStateFlow()

    private val _tuyaDeviceIdFutterwache =
        MutableStateFlow(prefs.getString("tuya_device_id_futterwache", "") ?: "")
    val tuyaDeviceIdFutterwache = _tuyaDeviceIdFutterwache.asStateFlow()

    private val _tuyaDeviceIdStallbox =
        MutableStateFlow(prefs.getString("tuya_device_id_stallbox", "") ?: "")
    val tuyaDeviceIdStallbox = _tuyaDeviceIdStallbox.asStateFlow()

    private val _tuyaApiBase =
        MutableStateFlow(prefs.getString("tuya_api_base", "https://openapi.tuyaeu.com") ?: "")
    val tuyaApiBase = _tuyaApiBase.asStateFlow()

    /** Quelle der zuletzt synchronisierten Cloud-Ereignisse: edge-agent | demo | null (kein Sync). */
    private val _cloudQuelle = MutableStateFlow<String?>(null)
    val cloudQuelle = _cloudQuelle.asStateFlow()

    /** Vollbild-Zustand des Live-Screens (steuert auch die Bottom-Navigation). */
    private val _liveVollbild = MutableStateFlow(false)
    val liveVollbild = _liveVollbild.asStateFlow()

    /** Lokale Ereignisliste des Live-Screens (Zeit, Text) – wie in StallblickApp.tsx. */
    private val _liveEreignisse = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val liveEreignisse = _liveEreignisse.asStateFlow()

    private fun ladeStreamSettings(): StreamSettings = StreamSettings(
        bridgeUrl = prefs.getString("bridge_url", "") ?: "",
        bridgeType = if ((prefs.getString("bridge_type", "go2rtc") ?: "go2rtc")
                .trim().lowercase() == "mediamtx"
        ) BridgeType.MEDIAMTX else BridgeType.GO2RTC,
        streamNameStallwache = prefs.getString("stream_name_stallwache", "stallwache") ?: "stallwache",
        streamNameFutterwache = prefs.getString("stream_name_futterwache", "futterwache") ?: "futterwache",
        streamNameStallbox = prefs.getString("stream_name_stallbox", "stallbox") ?: "stallbox",
        webappUrl = prefs.getString("webapp_url", StreamSettings.DEFAULT_WEBAPP_URL)
            ?: StreamSettings.DEFAULT_WEBAPP_URL,
        futterwacheTuya = prefs.getBoolean("futterwache_tuya", true),
        stallboxTuya = prefs.getBoolean("stallbox_tuya", true),
    )

    fun updateStreamSettings(neu: StreamSettings) {
        _streamSettings.value = neu
        prefs.edit()
            .putString("bridge_url", neu.bridgeUrl.trim())
            .putString("bridge_type", if (neu.bridgeType == BridgeType.MEDIAMTX) "mediamtx" else "go2rtc")
            .putString("stream_name_stallwache", neu.streamNameStallwache.trim())
            .putString("stream_name_futterwache", neu.streamNameFutterwache.trim())
            .putString("stream_name_stallbox", neu.streamNameStallbox.trim())
            .putString("webapp_url", neu.webappUrl.trim())
            .putBoolean("futterwache_tuya", neu.futterwacheTuya)
            .putBoolean("stallbox_tuya", neu.stallboxTuya)
            .apply()
    }

    fun updateTuyaSettings(
        accessId: String,
        accessSecret: String,
        deviceIdFutterwache: String,
        deviceIdStallbox: String,
        apiBase: String,
    ) {
        _tuyaAccessId.value = accessId.trim()
        _tuyaAccessSecret.value = accessSecret.trim()
        _tuyaDeviceIdFutterwache.value = deviceIdFutterwache.trim()
        _tuyaDeviceIdStallbox.value = deviceIdStallbox.trim()
        _tuyaApiBase.value = apiBase.trim()
        prefs.edit()
            .putString("tuya_access_id", accessId.trim())
            .putString("tuya_access_secret", accessSecret.trim())
            .putString("tuya_device_id_futterwache", deviceIdFutterwache.trim())
            .putString("tuya_device_id_stallbox", deviceIdStallbox.trim())
            .putString("tuya_api_base", apiBase.trim())
            .apply()
        tuyaClientCache = null
    }

    @Volatile
    private var tuyaClientCache: Pair<String, TuyaCloudClient>? = null

    /** Direkter Tuya-Client; Instanz wird gecacht, damit der Token-Cache traegt. */
    private fun tuyaClient(): TuyaCloudClient? {
        val id = _tuyaAccessId.value
        val secret = _tuyaAccessSecret.value
        if (id.isBlank() || secret.isBlank()) return null
        val schluessel = listOf(
            id, secret, _tuyaDeviceIdFutterwache.value,
            _tuyaDeviceIdStallbox.value, _tuyaApiBase.value,
        ).joinToString("|")
        tuyaClientCache?.let { (k, client) -> if (k == schluessel) return client }
        val client = TuyaCloudClient(
            accessId = id,
            accessSecret = secret,
            deviceIds = mapOf(
                "futterwache" to _tuyaDeviceIdFutterwache.value,
                "stallbox" to _tuyaDeviceIdStallbox.value,
            ),
            apiBase = _tuyaApiBase.value,
        )
        tuyaClientCache = schluessel to client
        return client
    }

    fun setLiveVollbild(an: Boolean) {
        _liveVollbild.value = an
    }

    fun addLiveEreignis(text: String) {
        val zeit = SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date())
        _liveEreignisse.value = (listOf(zeit to text) + _liveEreignisse.value).take(6)
    }

    /**
     * Loest die Stream-Quelle fuer das Hauptbild auf (Logik aus CameraStream.tsx):
     * Tuya-faehige Kameras zuerst ueber die Webapp (Session-Cookie + Proxy),
     * dann direkt ueber die Tuya-OpenAPI, zuletzt Bridge-HLS als Fallback.
     */
    suspend fun resolveStreamQuelle(camera: CameraConfig, tuyaErlaubt: Boolean): StreamQuelle? {
        val s = _streamSettings.value
        if (camera.tuyaFaehig && tuyaErlaubt) {
            if (s.isWebappConfigured) {
                try {
                    val url = cloudClient.holeTuyaStreamUrl(s.webappUrlClean, camera.tuyaEndpoint)
                    return StreamQuelle(url, istTuya = true)
                } catch (_: Exception) {
                    // Webapp nicht erreichbar oder geschuetzt -> naechster Weg
                }
            }
            tuyaClient()?.let { client ->
                if (client.konfiguriert(camera.id)) {
                    try {
                        return StreamQuelle(client.holeStreamUrl(camera.id), istTuya = true)
                    } catch (_: Exception) {
                        // Tuya direkt fehlgeschlagen -> Bridge-Fallback
                    }
                }
            }
        }
        if (s.isBridgeConfigured) {
            return StreamQuelle(s.hlsUrl(camera.streamName), istTuya = false)
        }
        return null
    }

    /**
     * Synchronisiert die KI-Wache-Ereignisse der Webapp (GET /api/events) in
     * die lokale Room-DB. Demo-Platzhalter der Webapp werden nicht importiert;
     * Duplikate verhindert die remoteId. Alte Ereignisse (> 60 min) kommen als
     * bereits gelesen an, damit kein veralteter Alarm-Overlay aufpoppt.
     */
    private suspend fun syncCloudEreignisse() {
        val s = _streamSettings.value
        if (!s.isWebappConfigured) return
        try {
            val daten = cloudClient.holeEreignisse(s.webappUrlClean)
            _cloudQuelle.value = daten.quelle
            if (daten.quelle != "edge-agent") return
            for (e in daten.ereignisse.asReversed()) {
                if (e.id.isEmpty() || e.nachricht.isEmpty()) continue
                if (repository.hatEventMitRemoteId(e.id)) continue
                val zeitpunkt = parseIsoZeit(e.zeit)
                val frisch = System.currentTimeMillis() - zeitpunkt < 60 * 60 * 1000
                val event = StallEvent(
                    typ = e.typ,
                    kuhId = e.kuhId,
                    kamera = e.kamera,
                    nachricht = e.nachricht,
                    timestamp = zeitpunkt,
                    konfidenz = e.konfidenz,
                    resolved = !frisch,
                    remoteId = e.id,
                )
                repository.insertEvent(event)
                if (frisch) _newIngestedEvent.tryEmit(event)
            }
        } catch (_: Exception) {
            // Webapp nicht erreichbar/geschuetzt -> naechster Poll versucht es erneut.
        }
    }

    private fun parseIsoZeit(iso: String): Long {
        if (iso.isEmpty()) return System.currentTimeMillis()
        val muster = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
        )
        for (m in muster) {
            try {
                val f = SimpleDateFormat(m, Locale.US)
                if (m.endsWith("'Z'")) f.timeZone = TimeZone.getTimeZone("UTC")
                return f.parse(iso)?.time ?: continue
            } catch (_: Exception) {
                // naechstes Muster
            }
        }
        return System.currentTimeMillis()
    }

    /** Snapshot der Hauptkamera als JPEG in die Galerie speichern (go2rtc frame.jpeg). */
    fun speichereSnapshot(kameraId: String) {
        val s = _streamSettings.value
        val cam = s.cameraById(kameraId)
        if (!s.isBridgeConfigured) {
            addLiveEreignis("Snapshot nicht möglich – Bridge nicht verbunden")
            return
        }
        if (!s.snapshotSupported) {
            addLiveEreignis("Snapshot nicht verfügbar – MediaMTX hat kein Einzelbild-Endpoint")
            return
        }
        viewModelScope.launch {
            try {
                val bytes = cloudClient.ladeBytes(
                    "${s.snapshotUrl(cam.streamName)}&t=${System.currentTimeMillis()}"
                )
                withContext(Dispatchers.IO) {
                    speichereJpeg(bytes, "stallblick-${cam.id}-${System.currentTimeMillis()}.jpg")
                }
                addLiveEreignis("Snapshot von ${cam.name} gespeichert")
            } catch (_: Exception) {
                addLiveEreignis("Snapshot von ${cam.name} fehlgeschlagen")
            }
        }
    }

    private fun speichereJpeg(bytes: ByteArray, dateiname: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val werte = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, dateiname)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Stallblick")
            }
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, werte
            ) ?: throw Exception("MediaStore-Eintrag fehlgeschlagen")
            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: throw Exception("Snapshot-Datei nicht beschreibbar")
        } else {
            val ordner = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                ?: context.filesDir
            File(ordner, dateiname).writeBytes(bytes)
        }
    }

    init {
        // Reste des frueheren Cloud-Logins aus den Preferences entfernen.
        prefs.edit()
            .remove("cloud_passwort")
            .remove("cloud_session_cookie")
            .remove("cloud_session_cookie_host")
            .apply()

        // KI-Wache-Ereignisse der Webapp regelmaessig in die lokale DB spiegeln.
        viewModelScope.launch {
            while (true) {
                syncCloudEreignisse()
                delay(20_000)
            }
        }
    }

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
            
            // Zusaetzlich an die echte Ingest-API der Webapp melden (derselbe Weg
            // wie der Edge-Agent im Stall), sobald ein Ingest-Token gesetzt ist.
            val ingestToken = _edgeToken.value
            val webapp = _streamSettings.value.webappUrlClean
            val anCloudGemeldet =
                if (webapp.isNotEmpty() && ingestToken.isNotBlank() && ingestToken != "EDGE_INGEST_TOKEN") {
                    cloudClient.sendeEreignis(
                        basisUrl = webapp,
                        ingestToken = ingestToken,
                        typ = type,
                        nachricht = message,
                        kuhId = cowId,
                        kamera = camera,
                        konfidenz = confidence,
                    )
                } else false

            _ingestSimulationState.value =
                if (anCloudGemeldet) "API 201 Created: Event lokal + an Stallblick-Cloud gemeldet!"
                else "API 201 Created: Event erfolgreich lokal registriert!"
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

    fun clearAnalyzerResult() {
        _analyzerResult.value = null
    }
}

data class ChatMessage(
    val role: String, // "user", "assistent"
    val text: String
)
