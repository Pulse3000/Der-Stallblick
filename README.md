# 🐄 Der Stallblick — Android-App

Native Android-Version von **Stallblick** (Repo
[Die-Stallwache](https://github.com/Pulse3000/Die_Stallwache)): Live-Blick auf
die Stallkameras plus KI-Wache für Brunst- und Kalbeerkennung. Betrieb:
Oberer Stollenhof.

## Was die App kann

| Tab | Inhalt |
| --- | --- |
| **Live** | Echte Kamera-Streams (Port des Web-Hauptscreens): Stallwache über die go2rtc/MediaMTX-Bridge (HLS via ExoPlayer), Futterwache & Stallbox über die Tuya-Cloud. Rollenwechsel ohne Stream-Neuaufbau, Vollbild, Snapshot in die Galerie, Statusblock, Ereignisliste. |
| **KI-Wache** | Alarm-Dashboard; synchronisiert die echten Ereignisse der Webapp (`GET /api/events`) in die lokale Room-DB (Dedupe über `remoteId`). |
| **Herde / KI-Diagnose / Assistent** | Kuh-Verwaltung, Gemini-Diagnose, Stall-Chat. |
| **Konfig.** | Webapp-URL der Stallblick-Cloud, Bridge-URL & -Typ, Stream-Namen, direkte Tuya-OpenAPI-Zugangsdaten, Gemini-Key, Themes. |

## So funktionieren die Kamera-Streams in der APK

Anders als im Browser gibt es nativ kein CORS und kein hls.js — **ExoPlayer
(Media3) spielt HLS direkt ab**:

```
Stallwache:    Tapo ──RTSP──▶ Bridge (go2rtc/MediaMTX) ──HLS──▶ Cloudflare Tunnel ──▶ ExoPlayer
Futterwache/   Tuya-Cloud ──▶ 1) Webapp /api/<kamera>/stream (CORS-Proxy der Webapp)
Stallbox:                     2) direkt Tuya-OpenAPI (HMAC-SHA256, kurzlebige HLS-URL)
                              3) Fallback: Bridge-HLS
```

* **go2rtc**: HLS `BRIDGE/api/stream.m3u8?src=<name>`, Vorschau per
  Snapshot-Polling `BRIDGE/api/frame.jpeg?src=<name>`.
* **MediaMTX**: HLS `BRIDGE/<name>/index.m3u8`, Vorschau per HEAD-Status-Ping
  (kein JPEG-Endpoint).
* **Tuya-URLs laufen ab** → bei Player-Fehlern wird automatisch eine frische
  URL angefordert (exponentielles Backoff, Bridge-Fallback wie in der Webapp).
* Die App greift ohne Login auf die Webapp zu (kein `STALLBLICK_PASSWORT` —
  die Webapp läuft offen); ist sie doch geschützt, greifen automatisch
  Tuya-direkt bzw. die Bridge.
* `android:usesCleartextTraffic="true"` erlaubt Bridges im Stall-LAN
  (`http://192.168.x.x:1984`) ohne Tunnel.

## Einrichtung in der App (Tab „Konfig.")

1. **Stallblick Cloud**: Webapp-URL eintragen (Default
   `https://stallwache.vercel.app`). Damit laufen die
   Futterwache/Stallbox-Streams und der KI-Wache-Ereignis-Sync — ohne Login.
2. **Kamera-Bridge**: Bridge-URL (Cloudflare-Tunnel-Hostname oder LAN-IP),
   Bridge-Typ (go2rtc/MediaMTX), optional abweichende Stream-Namen.
3. **Tuya direkt** (optional, ohne Webapp): Access ID/Secret des
   Tuya-Cloud-Projekts + Geräte-IDs von Futterwache/Stallbox.

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)

1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device
