# Morsecode WebShare HTTP API

## Base URL
`http://<phone-ip>:33455`

## Endpoints

### 1. `GET /api/v1/storage`
Returns storage volume metrics:
```json
{
  "totalBytes": 128000000000,
  "usedBytes": 104200000000,
  "freeBytes": 23800000000,
  "photosBytes": 43600000000,
  "videosBytes": 33100000000,
  "musicBytes": 26800000000,
  "otherBytes": 10700000000
}
```

### 2. `GET /api/v1/photos`
Returns list of indexed photos with thumbnails and albums.

### 3. `GET /api/v1/videos`
Returns video collection with durations and metadata.

### 4. `GET /api/v1/music`
Returns song library with artist, title, and length.

### 5. `GET /api/v1/apps`
Returns installed user apps with package names and APK download sizes.

### 6. `GET /api/v1/files?path=<relative_path>`
Returns directory listing for internal storage.

### 7. `GET /api/v1/download?path=<filename>`
Downloads individual file. Supports standard HTTP Range requests:
- **Header:** `Range: bytes=1000-5000`
- **Response:** `206 Partial Content`, `Content-Range: bytes 1000-5000/total`, `Accept-Ranges: bytes`

### 8. `GET /api/v1/zip?path=<folder>`
Streams an on-the-fly compressed ZIP archive directly to the client socket without creating intermediate duplicate files on disk.

### 9. `POST /api/v1/upload`
Streams incoming upload directly to phone storage (`Download/Morsecode/`). Header `x-filename` specifies relative destination name.
