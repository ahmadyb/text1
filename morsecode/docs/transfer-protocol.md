# Morsecode Transfer Protocol (MORSE 1.0)

## Overview
The Morsecode transfer protocol operates over local TCP sockets (default port `:33456`) with UDP discovery (port `:33457`). It is designed for maximum throughput, zero internet dependency, and robust resumption over Wi-Fi and direct connections.

## Handshake
1. Client opens TCP socket to Peer IP `:33456`.
2. Client sends `HELLO` frame:
   - Magic: `0x01`
   - Client Device Name (UTF)
   - Client Unique ID (UTF)
3. Server evaluates consent (if new peer) and responds with `WELCOME` (`0x01`) or `REJECT` (`0xFF`).

## Frame Structure
All binary frames are big-endian:
- **`0x01` (HELLO):** Version negotiation and device identification.
- **`0x02` (FILE_START):**
  - Transfer ID: String (UUID)
  - Relative File Name: String
  - Total Size: Int64 (bytes)
  - Checksum Type: String (CRC32/SHA256)
- **`0x03` (CHUNK):**
  - Chunk Offset: Int64
  - Payload Length: Int32 (default 64 KB – 256 KB)
  - Chunk CRC32: Int64
  - Payload Data: Raw bytes
- **`0x04` (CHUNK_ACK):**
  - Confirmed Contiguous Byte Offset: Int64
- **`0x05` (FILE_COMPLETE):**
  - Full SHA-256 Digest
- **`0x06` (PAUSE):** Preserves confirmed offset
- **`0x07` (RESUME):** Resumes at last acknowledged offset
- **`0x08` (CANCEL):** Explicit cancellation

## Invariants & Resumption
- Temporary files are written to `.part` files in app cache or target folder.
- On disconnect or pause, the receiver confirms the exact contiguous byte count verified on disk.
- When resuming, transfer restarts strictly from the confirmed byte offset.
- File is committed/renamed only after final CRC/SHA-256 verification matches.
