package com.example.pleosstarter

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Locale

class MainActivity : Activity(), RecognitionListener {
    private val session = DemoSession(SystemClock::elapsedRealtime)
    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var speaker: TextToSpeech? = null
    private var speakerReady = false
    private var voiceEnabled = false
    private var speaking = false
    private var listening = false
    private var voiceNotice = ""
    private val listen = Runnable { listenOnce() }
    private val expiry = Runnable {
        session.expire()
        render()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        speaker = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val engine = speaker ?: return@TextToSpeech
                speakerReady = engine.setLanguage(Locale.KOREAN) >= TextToSpeech.LANG_AVAILABLE
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) = Unit
                    override fun onDone(id: String?) = finishSpeaking()
                    @Deprecated("Required by Android")
                    override fun onError(id: String?) = finishSpeaking()
                })
            }
        }
        render()
    }

    private fun finishSpeaking() {
        handler.post {
            speaking = false
            scheduleListen()
        }
    }

    private fun speak(text: String) {
        val engine = speaker ?: return
        if (!speakerReady) return
        handler.removeCallbacks(listen)
        if (voiceEnabled) {
            recognizer?.destroy()
            recognizer = null
            listening = false
        }
        speaking = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "message") == TextToSpeech.SUCCESS
    }

    private fun toggleVoice() {
        if (voiceEnabled) {
            stopVoice()
            render()
        } else if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            voiceNotice = "이 기기에 음성 인식 서비스가 없습니다. 화면 버튼으로 시연할 수 있습니다."
            render()
        } else if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        } else {
            voiceEnabled = true
            voiceNotice = "음성 시연 켜짐 · 대기 화면에서 ‘메시지 전송’"
            scheduleListen()
            render()
        }
    }

    override fun onRequestPermissionsResult(code: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, permissions, results)
        if (code != 1) return
        if (results.firstOrNull() == PackageManager.PERMISSION_GRANTED) toggleVoice()
        else {
            voiceNotice = "마이크 권한이 필요합니다. 화면 버튼으로 시연할 수 있습니다."
            render()
        }
    }

    private fun scheduleListen() {
        handler.removeCallbacks(listen)
        if (voiceEnabled && !speaking && !listening && session.stage != Stage.READ) handler.postDelayed(listen, 350)
    }

    private fun listenOnce() {
        if (!voiceEnabled || speaking || listening) return
        val engine = recognizer ?: SpeechRecognizer.createSpeechRecognizer(this).also {
            it.setRecognitionListener(this)
            recognizer = it
        }
        listening = true
        engine.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
        })
    }

    private fun stopVoice() {
        voiceEnabled = false
        listening = false
        handler.removeCallbacks(listen)
        recognizer?.destroy()
        recognizer = null
        voiceNotice = ""
    }

    override fun onStop() {
        stopVoice()
        speaker?.stop()
        speaking = false
        render()
        super.onStop()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        speaker?.shutdown()
        super.onDestroy()
    }

    override fun onResults(results: Bundle?) {
        if (!voiceEnabled || speaking || !listening) return
        listening = false
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
        val previous = session.stage
        session.expire()
        val understood = matches.any(session::command)
        voiceNotice = if (understood) "음성 시연 켜짐" else "명령을 인식하지 못했습니다. 다시 말해 주세요."
        update(previous)
    }

    override fun onError(error: Int) {
        if (!voiceEnabled || speaking || !listening) return
        listening = false
        if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
            scheduleListen()
        } else {
            stopVoice()
            voiceNotice = "음성 인식에 연결할 수 없습니다. 화면 버튼을 사용하거나 다시 시도하세요."
            render()
        }
    }

    override fun onReadyForSpeech(params: Bundle?) = Unit
    override fun onBeginningOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() = Unit
    override fun onPartialResults(partialResults: Bundle?) = Unit
    override fun onEvent(eventType: Int, params: Bundle?) = Unit

    private fun act(action: () -> Unit) {
        val previous = session.stage
        action()
        update(previous)
    }

    private fun update(previous: Stage) {
        handler.removeCallbacks(expiry)
        if (session.stage == Stage.INBOX) {
            handler.postDelayed(expiry, (session.deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0))
        }
        if (session.stage != previous && session.stage in listOf(Stage.CONFIRM, Stage.INBOX, Stage.READ)) {
            speak(session.notice)
        }
        render()
        scheduleListen()
    }

    private fun render() {
        val background = Color.rgb(12, 20, 29)
        val surface = Color.rgb(30, 47, 62)
        val accent = Color.rgb(111, 224, 193)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(26), dp(18), dp(26), dp(20))
            setBackgroundColor(background)
        }
        fun label(text: String, size: Float, color: Int = Color.WHITE) {
            root.addView(TextView(this).apply {
                this.text = text
                textSize = size
                setTextColor(color)
                setPadding(0, dp(4), 0, dp(8))
                if (size >= 26) setTypeface(null, Typeface.BOLD)
            })
        }
        fun row() = LinearLayout(this).apply { gravity = Gravity.CENTER }.also { root.addView(it) }
        fun rounded(color: Int, border: Int = color) = GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(14).toFloat()
            setStroke(dp(1), border)
        }
        fun button(parent: LinearLayout, title: String, enabled: Boolean = true, highlighted: Boolean = false, carMarker: Boolean = false, action: () -> Unit) {
            val horizontal = parent !== root
            val params = LinearLayout.LayoutParams(
                if (horizontal) 0 else LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, if (horizontal) 1f else 0f
            ).apply { setMargins(0, dp(3), dp(6), dp(3)) }
            parent.addView(Button(this).apply {
                text = title
                textSize = 17f
                minHeight = dp(56)
                isAllCaps = false
                isEnabled = enabled
                alpha = if (enabled || highlighted) 1f else 0.65f
                this.background = rounded(
                    if (highlighted) accent else surface,
                    if (highlighted) accent else Color.rgb(60, 81, 98)
                )
                if (carMarker) {
                    val marker = CarMarker(if (highlighted) Color.WHITE else Color.rgb(171, 202, 224))
                    marker.setBounds(0, 0, dp(62), dp(82))
                    setCompoundDrawables(null, marker, null, null)
                    compoundDrawablePadding = dp(4)
                    setPadding(dp(4), dp(8), dp(4), dp(8))
                }
                setTextColor(if (highlighted) background else Color.WHITE)
                setOnClickListener { act(action) }
            }, params)
        }
        val stage = session.stage
        val recipient = stage == Stage.INBOX || stage == Stage.READ
        label(getString(R.string.app_name), 30f)
        label(if (recipient) "시뮬레이션 · 수신 차량 ${session.selected?.id}" else "시뮬레이션 · 송신 차량 A", 16f, accent)
        if (recipient) {
            session.senderDirection?.let { label("보낸 차량 · ${it.label} 차량", 19f, accent) }
        }
        label(session.notice, if (stage == Stage.READ) 28f else 20f)

        if (recipient) {
            if (stage == Stage.INBOX) {
                val controls = row()
                button(controls, "수락", highlighted = true) { session.accept() }
                button(controls, "거절") { session.decline() }
            } else {
                button(root, "송신 화면으로 돌아가기") { session.reset() }
            }
        } else {
            val selectable = stage != Stage.IDLE
            fun car(parent: LinearLayout, direction: Direction) {
                val nearby = session.cars.firstOrNull { it.direction == direction }
                val title = if (nearby == null) "${direction.label} · 없음" else
                    "${direction.label} · ${nearby.id} · ${nearby.meters}m"
                button(parent, title, selectable && nearby != null,
                    nearby != null && nearby.id == session.selected?.id, carMarker = nearby != null) { session.choose(direction) }
            }
            fun spacer(parent: LinearLayout) {
                parent.addView(android.view.View(this), LinearLayout.LayoutParams(0, 1, 1f))
            }
            fun centerCar(direction: Direction) {
                val lane = row()
                spacer(lane)
                car(lane, direction)
                spacer(lane)
            }
            centerCar(Direction.FRONT)
            val sides = row()
            car(sides, Direction.LEFT)
            sides.addView(TextView(this).apply {
                text = "내 차 A"
                val marker = CarMarker(accent)
                marker.setBounds(0, 0, dp(66), dp(88))
                setCompoundDrawables(null, marker, null, null)
                compoundDrawablePadding = dp(4)
                gravity = Gravity.CENTER
                setTextColor(accent)
                textSize = 18f
            }, LinearLayout.LayoutParams(0, dp(116), 1f))
            car(sides, Direction.RIGHT)
            centerCar(Direction.REAR)

            when (stage) {
                Stage.IDLE -> button(root, "메시지 보내기", highlighted = true) { session.start() }
                Stage.PRESET -> Preset.entries.chunked(2).forEach { presets ->
                    val messages = row()
                    presets.forEach { preset -> button(messages, preset.label) { session.pick(preset) } }
                }
                Stage.CONFIRM -> button(root, "보내기", highlighted = true) { session.send() }
                else -> Unit
            }
            val controls = row()
            button(controls, "테스트 · 앞차 교체") { session.replaceFront() }
            if (selectable) button(controls, "취소") { session.reset() }
        }

        button(root, if (voiceEnabled) "음성 시연 끄기" else "음성 시연 켜기") { toggleVoice() }
        if (voiceNotice.isNotEmpty()) label(voiceNotice, 15f, accent)
        setContentView(ScrollView(this).apply {
            fitsSystemWindows = true
            isFillViewport = true
            setBackgroundColor(background)
            addView(root)
        })
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}




