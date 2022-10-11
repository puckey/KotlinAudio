package com.doublesymmetry.kotlinaudio

import androidx.test.platform.app.InstrumentationRegistry
import com.doublesymmetry.kotlinaudio.models.AudioPlayerState
import com.doublesymmetry.kotlinaudio.models.CacheConfig
import com.doublesymmetry.kotlinaudio.players.QueuedAudioPlayer
import com.doublesymmetry.kotlinaudio.utils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class AudioPlayerTest {
    private lateinit var testPlayer: QueuedAudioPlayer
    private lateinit var states: MutableList<String>

    @BeforeEach
    fun setUp(testInfo: TestInfo) {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        testPlayer = QueuedAudioPlayer(
            appContext,
            cacheConfig = CacheConfig(maxCacheSize = (1024 * 50).toLong(), identifier = testInfo.displayName)
        )
        states = mutableListOf()
        testPlayer.event.stateChange.map {
            if (it != AudioPlayerState.BUFFERING) {
                states.add(it.toString())
            }
        }.stateIn(
            CoroutineScope(Dispatchers.Default),
            SharingStarted.Eagerly,
            emptyList<AudioPlayerState>()
        )
        testPlayer.event.stateChange.waitUntil { it == AudioPlayerState.IDLE }
    }

    @Nested
    inner class State {
        @Test
        fun givenNewPlayer_thenShouldBeIdle() = runBlocking(Dispatchers.Main) {
            assertEquals(AudioPlayerState.IDLE, testPlayer.playerState)
        }

        @Test
        fun givenLoadSource_thenShouldBeReady() = runBlocking(Dispatchers.Main) {
            testPlayer.load(TestSound.default)
            eventually {
                assertEquals(mutableListOf<String>("IDLE", "LOADING", "READY"), states);
            }
        }

        @Test
        fun givenLoadSourceAndPlayWhenReady_thenShouldBePlaying() = runBlocking(Dispatchers.Main) {
            testPlayer.load(TestSound.fiveSeconds, true)
            eventually(Duration.ofSeconds(1), Dispatchers.Main, fun () {
                assertEquals(mutableListOf<String>("IDLE", "LOADING", "READY", "PLAYING"), states);
                assertEquals(AudioPlayerState.PLAYING, testPlayer.playerState)
            })
        }

        @Test
        fun givenLoadSourceAndPlayWhenReadyAfterLoadSource_thenShouldBePlaying() = runBlocking(Dispatchers.Main) {
            testPlayer.load(TestSound.fiveSeconds, true)
            launchWithTimeoutSync(this) {
                testPlayer.event.stateChange
                    .waitUntil { it == AudioPlayerState.PLAYING }
                    .collect {
                        testPlayer.load(TestSound.fiveSeconds2, true)
                    }
            }
            eventually(Duration.ofSeconds(1), Dispatchers.Main, fun () {
                assertEquals(mutableListOf<String>("IDLE", "LOADING", "READY", "PLAYING", "LOADING", "READY", "PLAYING"), states);
            })
        }

        @Test
        fun givenPlaySource_thenShouldBePlaying() = runBlocking(Dispatchers.Main) {
            testPlayer.play()
            testPlayer.load(TestSound.default)
            eventually {
                assertEquals(mutableListOf<String>("IDLE", "LOADING", "READY", "PLAYING"), states.subList(0, 4));
            }
        }

        @Test
        fun givenPausingSource_thenShouldBePaused() = runBlocking(Dispatchers.Main) {
            testPlayer.load(TestSound.fiveSeconds, playWhenReady = true)

            launchWithTimeoutSync(this) {
                testPlayer.event.stateChange
                    .waitUntil { it == AudioPlayerState.PLAYING }
                    .collect { testPlayer.pause() }
            }

            eventually(Duration.ofSeconds(1), Dispatchers.Main, fun () {
                assertEquals(mutableListOf<String>("IDLE", "LOADING", "READY", "PLAYING", "PAUSED"), states);
                assertEquals(AudioPlayerState.PAUSED, testPlayer.playerState)
            })
        }

        @Test
        fun givenStoppingSource_thenShouldBeIdle() = runBlocking(Dispatchers.Main) {
            testPlayer.load(TestSound.long, playWhenReady = true)

            var hasBeenPlaying = false

            launchWithTimeoutSync(this) {
                testPlayer.event.stateChange
                    .waitUntil { it == AudioPlayerState.PLAYING }
                    .collect {
                        hasBeenPlaying = true
                        testPlayer.stop()
                    }
            }
            eventually {
                assertEquals(mutableListOf<String>("IDLE", "LOADING", "READY", "PLAYING", "IDLE"), states);
                assertEquals(true, hasBeenPlaying)
                assertEquals(AudioPlayerState.IDLE, testPlayer.playerState)
            }
        }
    }

    @Nested
    inner class Position {
        @Test
        fun thenShouldBe0() = runBlocking(Dispatchers.Main) {
            assertEquals(0, testPlayer.position)
        }

        @Test
        fun givenPlayingSource_thenShouldBeGreaterThan0() = runBlocking(Dispatchers.Main) {
            // TODO: Fix bug with load when you use it to add first item
//            testPlayer.load(TestSound.default, playWhenReady = false)
            testPlayer.add(TestSound.long, playWhenReady = true)

            eventually {
                assertTrue(testPlayer.position > 0)
            }
        }
    }

    @Nested
    inner class Rate {
        @Test
        fun givenNewPlayer_thenShouldBe1() = runBlocking(Dispatchers.Main) {
            assertEquals(1.0f, testPlayer.playbackSpeed)
        }

        @Test
        fun givenPlayingSource_thenShouldBe1() = runBlocking(Dispatchers.Main) {
            testPlayer.load(TestSound.fiveSeconds, playWhenReady = true)

            var hasMetSpeedExpectation = false
            launchWithTimeoutSync(this) {
                testPlayer.event.stateChange
                    .waitUntil { it == AudioPlayerState.PLAYING }
                    .collect { hasMetSpeedExpectation = testPlayer.playbackSpeed == 1.0f }
            }

            eventually {
                assertTrue(hasMetSpeedExpectation)
            }
        }
    }

    @Nested
    inner class CurrentItem {
        @Test
        fun givenNewPlayer_thenShouldBeNull() = runBlocking(Dispatchers.Main) {
            assertNull(testPlayer.currentItem)
        }

        @Test
        fun givenLoadingSource_thenShouldNotBeNull() = runBlocking(Dispatchers.Main) {
            // TODO: Fix bug with load when you use it to add first item
//            testPlayer.load(TestSound.default, playWhenReady = false)
            testPlayer.add(TestSound.long, playWhenReady = true)

            eventually {
                assertNotNull(testPlayer.currentItem)
            }
        }
    }
}
