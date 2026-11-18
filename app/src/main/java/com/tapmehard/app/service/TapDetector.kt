package com.tapmehard.app.service

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Core tap-detection engine.
 *
 * Uses a high-pass filter on the accelerometer Z-axis to isolate sharp impacts,
 * classifies taps into Morse code format ('.' for normal tap, '-' for hard tap),
 * rejects accidental movement via gyroscope magnitude, enforces a 150ms cooldown
 * so a single physical hit is never counted twice, and waits a configurable
 * "sequence delay" (default 500ms) after the last tap before emitting the sequence.
 */
class TapDetector(
    private val sensorManager: SensorManager,
    private val scope: CoroutineScope
) : SensorEventListener {

    // Tuneable parameters (updated from DataStore)
    var sensitivity: Float = 15f   // Z-axis threshold
    var sequenceDelay: Long = 500L // ms to wait after last tap

    // Outgoing tap events with Morse sequence
    private val _tapEvents = MutableSharedFlow<TapEvent>(extraBufferCapacity = 8)
    val tapEvents: SharedFlow<TapEvent> = _tapEvents

    // High-pass filter gravity estimate
    private val gravity = FloatArray(3)

    // Tap-sequence state
    private var tapCount = 0
    private var lastTapTime = 0L
    private val cooldownMs = 150L
    private var evaluationJob: Job? = null
    private val currentSequence = StringBuilder()

    // Gyroscope-based movement rejection
    private var lastGyroMagnitude = 0f
    private val gyroRejectThreshold = 5f

    // Recording mode (custom rhythm capture)
    var isRecording = false
        private set
    private val recordedIntervals = mutableListOf<Long>()
    private var recordingFirstTapTime = 0L
    private var recordingLastTapTime = 0L
    private var recordingJob: Job? = null
    private val _recordingResult = MutableSharedFlow<RecordingResult>(extraBufferCapacity = 1)
    val recordingResult: SharedFlow<RecordingResult> = _recordingResult

    // ---- Lifecycle ----

    fun start() {
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        evaluationJob?.cancel()
        recordingJob?.cancel()
        tapCount = 0
        currentSequence.clear()
        isRecording = false
    }

    // ---- Recording (custom rhythm & sequence) ----

    fun startRecording() {
        isRecording = true
        recordedIntervals.clear()
        recordingFirstTapTime = 0L
        recordingLastTapTime = 0L
        tapCount = 0
        currentSequence.clear()
        recordingJob?.cancel()
    }

    private fun finishRecording(count: Int, sequence: String) {
        isRecording = false
        scope.launch {
            _recordingResult.emit(
                RecordingResult(
                    tapCount = count,
                    morseSequence = sequence,
                    intervals = recordedIntervals.toList()
                )
            )
        }
    }

    // ---- SensorEventListener ----

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> {
                val (x, y, z) = event.values
                lastGyroMagnitude = sqrt(x * x + y * y + z * z)
            }
            Sensor.TYPE_ACCELEROMETER -> processAccelerometer(event)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    // ---- Accelerometer processing ----

    private fun processAccelerometer(event: SensorEvent) {
        // High-pass filter: remove gravity component
        val alpha = 0.8f
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]

        val linearZ = abs(event.values[2] - gravity[2])
        val now = System.currentTimeMillis()

        // Reject gyroscope-detected rotation (device being moved, not tapped)
        if (lastGyroMagnitude > gyroRejectThreshold) return

        // Check if Z-axis spike exceeds sensitivity threshold
        if (linearZ < sensitivity) return

        // 150ms cooldown — ignore vibration echoes from the same physical hit
        if (now - lastTapTime < cooldownMs) return

        // Determine Morse symbol: hard tap = '-' (dash), normal tap = '.' (dot)
        val isHardTap = linearZ >= (sensitivity * 1.5f).coerceAtLeast(22f)
        val symbol = if (isHardTap) '-' else '.'

        // --- Valid tap detected ---
        if (isRecording) {
            if (recordingFirstTapTime == 0L) {
                recordingFirstTapTime = now
            } else {
                recordedIntervals.add(now - recordingLastTapTime)
            }
            recordingLastTapTime = now
            tapCount++
            currentSequence.append(symbol)

            recordingJob?.cancel()
            recordingJob = scope.launch {
                delay(sequenceDelay)
                val count = tapCount
                val seq = currentSequence.toString()
                tapCount = 0
                currentSequence.clear()
                finishRecording(count, seq)
            }
        } else {
            lastTapTime = now
            tapCount++
            currentSequence.append(symbol)

            // Reset the sequence evaluation timer
            evaluationJob?.cancel()
            evaluationJob = scope.launch {
                delay(sequenceDelay)
                val count = tapCount
                val seq = currentSequence.toString()
                tapCount = 0
                currentSequence.clear()
                _tapEvents.emit(TapEvent(tapCount = count, morseSequence = seq))
            }
        }
    }
}

data class TapEvent(
    val tapCount: Int,
    val morseSequence: String
)

data class RecordingResult(
    val tapCount: Int,
    val morseSequence: String,
    val intervals: List<Long>
)
