package com.example.core.media

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.core.storage.MediaFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerPlaybackState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val volume: Float = 0.7f,
    val isMuted: Boolean = false,
    val activeTrack: MediaFile? = null,
    val trackIndex: Int = 0
) {
    val progress: Float
        get() = if (durationMs > 0L) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}

@OptIn(UnstableApi::class)
class Media3PlayerManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressTickerJob: Job? = null

    var musicPlayer: ExoPlayer? = null
        private set

    var videoPlayer: ExoPlayer? = null
        private set

    private val _musicState = MutableStateFlow(PlayerPlaybackState())
    val musicState: StateFlow<PlayerPlaybackState> = _musicState.asStateFlow()

    private val _videoState = MutableStateFlow(PlayerPlaybackState())
    val videoState: StateFlow<PlayerPlaybackState> = _videoState.asStateFlow()

    init {
        initMusicPlayer()
        initVideoPlayer()
        startProgressTicker()
    }

    private fun initMusicPlayer() {
        musicPlayer = ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            volume = 0.7f
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _musicState.value = _musicState.value.copy(
                        isPlaying = isPlaying,
                        durationMs = duration.coerceAtLeast(0L),
                        currentPositionMs = currentPosition.coerceAtLeast(0L)
                    )
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        _musicState.value = _musicState.value.copy(
                            durationMs = duration.coerceAtLeast(0L),
                            currentPositionMs = currentPosition.coerceAtLeast(0L)
                        )
                    }
                }
            })
        }
    }

    private fun initVideoPlayer() {
        videoPlayer = ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            volume = 0.7f
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _videoState.value = _videoState.value.copy(
                        isPlaying = isPlaying,
                        durationMs = duration.coerceAtLeast(0L),
                        currentPositionMs = currentPosition.coerceAtLeast(0L)
                    )
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        _videoState.value = _videoState.value.copy(
                            durationMs = duration.coerceAtLeast(0L),
                            currentPositionMs = currentPosition.coerceAtLeast(0L)
                        )
                    }
                }
            })
        }
    }

    private fun startProgressTicker() {
        progressTickerJob?.cancel()
        progressTickerJob = scope.launch {
            while (isActive) {
                delay(250L)
                musicPlayer?.let { player ->
                    if (player.isPlaying) {
                        _musicState.value = _musicState.value.copy(
                            currentPositionMs = player.currentPosition.coerceAtLeast(0L),
                            durationMs = player.duration.coerceAtLeast(0L)
                        )
                    }
                }
                videoPlayer?.let { player ->
                    if (player.isPlaying) {
                        _videoState.value = _videoState.value.copy(
                            currentPositionMs = player.currentPosition.coerceAtLeast(0L),
                            durationMs = player.duration.coerceAtLeast(0L)
                        )
                    }
                }
            }
        }
    }

    fun playMusicTrack(track: MediaFile, index: Int, autoPlay: Boolean = true) {
        val player = musicPlayer ?: return
        try {
            val mediaItem = if (track.uri != null) {
                MediaItem.fromUri(track.uri)
            } else if (track.path.isNotEmpty()) {
                MediaItem.fromUri(Uri.parse(track.path))
            } else {
                null
            }

            if (mediaItem != null) {
                player.setMediaItem(mediaItem)
                player.prepare()
                if (autoPlay) player.play()
            }

            _musicState.value = _musicState.value.copy(
                activeTrack = track,
                trackIndex = index,
                isPlaying = autoPlay,
                durationMs = track.durationMs
            )
        } catch (_: Exception) {
        }
    }

    fun toggleMusicPlayPause() {
        val player = musicPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
                player.prepare()
            }
            player.play()
        }
        _musicState.value = _musicState.value.copy(isPlaying = player.isPlaying)
    }

    fun seekMusic(fraction: Float) {
        val player = musicPlayer ?: return
        val targetMs = (player.duration * fraction).toLong().coerceIn(0L, player.duration.coerceAtLeast(0L))
        player.seekTo(targetMs)
        _musicState.value = _musicState.value.copy(currentPositionMs = targetMs)
    }

    fun seekMusicRelative(offsetMs: Long) {
        val player = musicPlayer ?: return
        val targetMs = (player.currentPosition + offsetMs).coerceIn(0L, player.duration.coerceAtLeast(0L))
        player.seekTo(targetMs)
        _musicState.value = _musicState.value.copy(currentPositionMs = targetMs)
    }

    fun setMusicVolume(volume: Float) {
        val safeVol = volume.coerceIn(0f, 1f)
        musicPlayer?.volume = if (_musicState.value.isMuted) 0f else safeVol
        _musicState.value = _musicState.value.copy(volume = safeVol)
    }

    fun toggleMusicMute() {
        val newMuted = !_musicState.value.isMuted
        musicPlayer?.volume = if (newMuted) 0f else _musicState.value.volume
        _musicState.value = _musicState.value.copy(isMuted = newMuted)
    }

    fun playVideoTrack(video: MediaFile, autoPlay: Boolean = true) {
        val player = videoPlayer ?: return
        try {
            val mediaItem = if (video.uri != null) {
                MediaItem.fromUri(video.uri)
            } else if (video.path.isNotEmpty()) {
                MediaItem.fromUri(Uri.parse(video.path))
            } else {
                null
            }

            if (mediaItem != null) {
                player.setMediaItem(mediaItem)
                player.prepare()
                if (autoPlay) player.play()
            }

            _videoState.value = _videoState.value.copy(
                activeTrack = video,
                isPlaying = autoPlay,
                durationMs = video.durationMs
            )
        } catch (_: Exception) {
        }
    }

    fun toggleVideoPlayPause() {
        val player = videoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
                player.prepare()
            }
            player.play()
        }
        _videoState.value = _videoState.value.copy(isPlaying = player.isPlaying)
    }

    fun seekVideo(fraction: Float) {
        val player = videoPlayer ?: return
        val targetMs = (player.duration * fraction).toLong().coerceIn(0L, player.duration.coerceAtLeast(0L))
        player.seekTo(targetMs)
        _videoState.value = _videoState.value.copy(currentPositionMs = targetMs)
    }

    fun seekVideoRelative(offsetMs: Long) {
        val player = videoPlayer ?: return
        val targetMs = (player.currentPosition + offsetMs).coerceIn(0L, player.duration.coerceAtLeast(0L))
        player.seekTo(targetMs)
        _videoState.value = _videoState.value.copy(currentPositionMs = targetMs)
    }

    fun setVideoVolume(volume: Float) {
        val safeVol = volume.coerceIn(0f, 1f)
        videoPlayer?.volume = if (_videoState.value.isMuted) 0f else safeVol
        _videoState.value = _videoState.value.copy(volume = safeVol)
    }

    fun toggleVideoMute() {
        val newMuted = !_videoState.value.isMuted
        videoPlayer?.volume = if (newMuted) 0f else _videoState.value.volume
        _videoState.value = _videoState.value.copy(isMuted = newMuted)
    }

    fun release() {
        progressTickerJob?.cancel()
        musicPlayer?.release()
        videoPlayer?.release()
        musicPlayer = null
        videoPlayer = null
    }
}
