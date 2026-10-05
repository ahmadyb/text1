# Morsecode — Local P2P & WebShare File Transfer for Android

Morsecode is a high-speed, local peer-to-peer file transfer and media sharing application built for Android with Jetpack Compose and Kotlin. It operates completely offline over Wi-Fi networks and local hotspots with zero cloud dependency, zero speed throttling, and zero tracking.

---

## Key Highlights & Capabilities

- **Real LAN Discovery & Duplex Transport:**
  - Automated UDP beacon broadcast and listener on port `33457` with `WifiManager.MulticastLock`.
  - High-performance TCP transfer engine on port `33456` transferring raw chunks with per-chunk and whole-file CRC32 verification.
  - Symmetrical duplex transfers: send and receive files concurrently on the same session.
  - 1→N Independent Multi-Phone Broadcast: fans out transfers to multiple devices in parallel without stalling on slow clients.
  - Resumption from confirmed byte offsets (`0x05 FRAME_FILE_RESUME`) across network disconnections.
- **Embedded WebShare HTTP Server & Browser Client:**
  - Embedded HTTP server on port `33455`.
  - Zero-install web client served directly from Android assets (`/assets/webshare/index.html`).
  - Mandatory phone authorization modal: browser requests require explicit approval on the phone before files can be accessed.
  - Instant session revocation from phone or browser.
  - HTTP 206 Partial Content byte-range streaming: enables smooth instant seeking in 4K/1080p videos in Chrome, Safari, and Firefox.
  - On-the-fly ZIP streaming (`/api/v1/zip`): archives folders or selected files directly to the client's HTTP response stream.
  - Path traversal and canonical security validation preventing unauthorized directory access.
- **Storage & Media Engine:**
  - MediaStore scanning for Photos, Videos, Audio, and installed APK extraction (`queries` configured in Manifest).
  - Built-in background audio player powered by **AndroidX Media3 (ExoPlayer)** with sticky playback bar, seeking, volume, and mute.
- **Resilient Durable State Machine:**
  - Persistent queue in **Room Database** tracking in-flight files.
  - Temporary partial files (`.part`) with atomic rename upon verification.
  - Process crash recovery: automatically restores interrupted transfers to `PAUSED` state on app launch with validated confirmed offsets.
- **Diagnostics & Operations:**
  - **Connection Doctor:** Real-time hardware and network health checks (Wi-Fi state, Multicast lock, TCP ServerSocket binding, WebShare port binding, Local IP detection, and Storage permissions).
  - **Crash Logging & Diagnostics:** In-memory and Room-persisted crash and event logs with copy-to-clipboard and share sheet export.

---

## Network Architecture & Port Matrix

| Port | Protocol | Purpose |
|------|----------|---------|
| **33455** | TCP (HTTP/1.1) | Embedded WebShare server & browser client |
| **33456** | TCP (Custom Binary Frame) | Peer-to-Peer duplex file transfer engine |
| **33457** | UDP (Broadcast) | LAN device discovery & beacon advertisement |

### Binary Transfer Framing Protocol
```
[1 byte: Frame Type]
  0x01 -> FRAME_HELLO       (Peer Handshake)
  0x02 -> FRAME_FILE_START   (Metadata: name, size, transferId)
  0x03 -> FRAME_CHUNK        (Offset, Length, CRC32, Payload bytes)
  0x04 -> FRAME_ACK          (Confirmed offset acknowledged)
  0x05 -> FRAME_FILE_RESUME  (Resume from confirmed offset)
  0x06 -> FRAME_FILE_PAUSE   (Halt streaming)
  0x07 -> FRAME_FILE_CANCEL  (Cancel and delete partial file)
```

---

## How to Run the App

### 1. Build and Install via Android Studio / Gradle
```bash
# Clean build and compile
gradle :app:compileDebugKotlin

# Assemble Debug APK
gradle :app:assembleDebug

# The generated APK is located at:
# app/build/outputs/apk/debug/app-debug.apk
```

### 2. Device-to-Device P2P Transfer (Two Android Phones)
1. Connect both devices to the same local Wi-Fi router or turn on Personal Hotspot on one device and connect the second device.
2. Open Morsecode on both phones.
3. Phone A will automatically discover Phone B via UDP broadcast.
4. Select files from the **Send** tab and tap on Phone B to begin sending.
5. Phone B receives a consent modal. Tap **Accept** to stream the transfer.
6. The ongoing transfer will show progress in the app and via the ongoing foreground notification.

### 3. WebShare Browser Transfer (Phone to Laptop/PC/Tablet)
1. Ensure the computer/laptop is connected to the same Wi-Fi network as the phone.
2. In Morsecode on the phone, navigate to the **WebShare** tab and tap **Start WebShare**.
3. Note the displayed IP address (e.g. `http://192.168.1.24:33455`).
4. On your computer's browser, open `http://<phone-ip>:33455`.
5. The browser displays "Phone Approval Required".
6. On the phone, a consent modal appears showing the browser's IP and User-Agent. Tap **Accept**.
7. The browser immediately unlocks:
   - Browse folders with breadcrumb navigation.
   - Click any photo to view in the full-resolution lightbox.
   - Stream videos with instant scrubbing via HTTP 206 byte-ranges.
   - Play music in the web audio player.
   - Drag and drop files from desktop into the upload zone to send them directly to the phone's `Download/Morsecode` folder.
   - Select multiple files or folders and click **Download as ZIP**.

---

## Automated Test Suites

The codebase includes 27 comprehensive automated tests running via Robolectric across modern Android (API 34) and legacy Android (API 23):

```bash
gradle :app:testDebugUnitTest
```

- **`Milestone6WebShareIntegrationTest` (API 23):**
  - Session authorization handshake and 401 enforcement.
  - HTTP 206 Partial Content byte-range slicing and video seeking.
  - Dynamic folder ZIP streaming (`ZipInputStream` validation).
  - Directory traversal defense (`isPathSafe` rejecting `../../`).
  - Session revocation.
- **`Milestone5LanDuplexIntegrationTest`:**
  - Simultaneous bidirectional duplex transfers between two real socket instances.
  - Reconnection and resumption from confirmed offset.
  - Independent multi-phone broadcast (1→N).
- **`Milestone4DurableEngineTest`:**
  - 64 KB chunk CRC32 verification and controlled failure rejection.
  - Full local streamed transfers with byte-for-byte fidelity.
  - Pause and resume state machine.
  - Cancellation cleanup of `.part` temporary files.
  - Conflict policies (RENAME vs OVERWRITE).
  - Process crash recovery.
- **`Milestone3MediaAndStorageTest`:**
  - MediaStore queries, SAF document providers, installed app resolution.
  - Media3 player seeking, volume, and mute.
- **`Milestone2ScreenshotAndUITest`:**
  - Visual regression testing of 14 screen states at 360 × 740 dp.
- **`Milestone1FoundationTest`:**
  - Room DAO operations, DataStore preferences, domain models.

---

## Remaining Limitations & Considerations

1. **AP Isolation (Client Isolation) on Enterprise/Campus Wi-Fi:**
   - Some university, airport, or hotel Wi-Fi networks block direct peer-to-peer communication between connected clients on the router level. In these environments, turning on the Android Personal Hotspot on one device and connecting the other device bypasses AP isolation completely.
2. **Nearby Connections Transport:**
   - The transport layer uses the `TransportChannel` abstraction. LAN TCP sockets and WebShare are fully operational; Google Play Services Nearby Connections can plug directly into `TransportChannel` when Google Play Services credentials and permissions are granted on physical hardware.
