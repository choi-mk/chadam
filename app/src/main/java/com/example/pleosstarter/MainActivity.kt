package com.example.pleosstarter

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.content.res.ColorStateList
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
        render(animateInbox = previous != Stage.INBOX && session.stage == Stage.INBOX)
        scheduleListen()
    }

    private fun render(animateInbox: Boolean = false) {
        val background = Color.rgb(9, 16, 25)
        val surface = Color.rgb(24, 39, 54)
        val accent = Color.rgb(115, 234, 201)
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
        fun row(parent: LinearLayout = root) = LinearLayout(this).apply { gravity = Gravity.CENTER }.also { parent.addView(it) }
        fun rounded(color: Int, border: Int = color) = GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(14).toFloat()
            setStroke(dp(1), border)
        }
        fun button(parent: LinearLayout, title: String, enabled: Boolean = true, highlighted: Boolean = false, carMarker: Boolean = false, icon: String? = null, action: () -> Unit) {
            val horizontal = parent.orientation == LinearLayout.HORIZONTAL
            val params = LinearLayout.LayoutParams(
                if (horizontal) 0 else LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, if (horizontal) 1f else 0f
            ).apply { setMargins(0, dp(3), dp(6), dp(3)) }
            parent.addView(Button(this).apply {
                text = title
                textSize = if (carMarker) 27f else 25f
                minHeight = dp(70)
                isAllCaps = false
                isEnabled = enabled
                alpha = if (enabled || highlighted) 1f else 0.65f
                val shape = rounded(
                    if (highlighted) accent else surface,
                    if (highlighted) accent else Color.rgb(60, 81, 98)
                )
                this.background = RippleDrawable(ColorStateList.valueOf(0x26FFFFFF), shape, null)
                if (carMarker) {
                    val marker = CarMarker(if (highlighted) Color.WHITE else Color.rgb(171, 202, 224))
                    marker.setBounds(0, 0, dp(78), dp(104))
                    setCompoundDrawables(null, marker, null, null)
                    compoundDrawablePadding = dp(4)
                    setPadding(dp(4), dp(8), dp(4), dp(8))
                }
                icon?.let {
                    val glyph = ActionIcon(it, if (highlighted) background else accent)
                    glyph.setBounds(0, 0, dp(24), dp(24))
                    setCompoundDrawablesRelative(glyph, null, null, null)
                    compoundDrawablePadding = dp(12)
                    setPadding(dp(18), dp(12), dp(18), dp(12))
                    gravity = Gravity.CENTER_VERTICAL or Gravity.START
                }
                setTextColor(if (highlighted) background else Color.WHITE)
                setOnClickListener { act(action) }
            }, params)
        }
        val stage = session.stage
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        fun attachContent() {
            root.addView(content, LinearLayout.LayoutParams(-1, -2, 1f).apply {
                setMargins(0, dp(12), 0, dp(16))
            })
        }
        val recipient = stage == Stage.INBOX || stage == Stage.READ
        label(getString(R.string.app_name), 45f)
        label(if (recipient) "시뮬레이션 · 수신 차량 ${session.selected?.id}" else "시뮬레이션 · 송신 차량 A", 22f, accent)
        if (recipient) {
            attachContent()
            content.background = rounded(surface, if (stage == Stage.INBOX) accent else Color.rgb(60, 81, 98))
            if (stage == Stage.INBOX) {
                content.addView(TextView(this).apply {
                    text = "새 메시지"
                    textSize = 24f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(accent)
                    gravity = Gravity.CENTER
                    val bell = ActionIcon("bell", accent)
                    bell.setBounds(0, 0, dp(24), dp(24))
                    setCompoundDrawablesRelative(bell, null, null, null)
                    compoundDrawablePadding = dp(10)
                    setPadding(0, 0, 0, dp(20))
                })
            }
            content.setPadding(dp(28), dp(24), dp(28), dp(28))
            session.senderDirection?.let { direction ->
                content.addView(TextView(this).apply {
                    text = "보낸 차량 · ${direction.label} 차량"
                    val marker = CarMarker(accent)
                    marker.setBounds(0, 0, dp(72), dp(96))
                    setCompoundDrawables(null, marker, null, null)
                    compoundDrawablePadding = dp(12)
                    gravity = Gravity.CENTER
                    textSize = 29f
                    setTextColor(accent)
                })
            }
            content.addView(TextView(this).apply {
                text = session.notice
                textSize = 43f
                gravity = Gravity.CENTER
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
                setPadding(0, dp(16), 0, dp(8))
            })
            if (stage == Stage.INBOX) {
                val controls = row()
                button(controls, "수락", highlighted = true, icon = "check") { session.accept() }
                button(controls, "거절", icon = "close") { session.decline() }
            } else {
                button(root, "송신 화면으로 돌아가기", highlighted = true) { session.reset() }
            }
        } else {
            label(session.notice, 30f)
            attachContent()
            val map = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                this.background = RoadSurface()
                setPadding(dp(16), dp(12), dp(16), dp(12))
            }
            val wide = resources.configuration.screenWidthDp >= 760
            val selectingPreset = stage == Stage.PRESET
            val body = if (wide && selectingPreset) row(content) else content
            body.addView(map, if (body === content) LinearLayout.LayoutParams(-1, -2, 1f)
                else LinearLayout.LayoutParams(0, -2, 1.15f))
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
            fun mapLane() = LinearLayout(this).apply {
                gravity = Gravity.CENTER
                map.addView(this, LinearLayout.LayoutParams(-1, -2, 1f))
            }
            fun centerCar(direction: Direction) {
                val lane = mapLane()
                spacer(lane)
                car(lane, direction)
                spacer(lane)
            }
            centerCar(Direction.FRONT)
            val sides = mapLane()
            car(sides, Direction.LEFT)
            sides.addView(TextView(this).apply {
                text = "내 차 A"
                val marker = CarMarker(accent)
                marker.setBounds(0, 0, dp(66), dp(88))
                setCompoundDrawables(null, marker, null, null)
                compoundDrawablePadding = dp(4)
                gravity = Gravity.CENTER
                setTextColor(accent)
                textSize = 26f
            }, LinearLayout.LayoutParams(0, dp(116), 1f))
            car(sides, Direction.RIGHT)
            centerCar(Direction.REAR)

            when (stage) {
                Stage.IDLE -> button(root, "메시지 보내기", highlighted = true, icon = "send") { session.start() }
                Stage.PRESET -> {
                    val presetsPanel = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(if (wide) dp(16) else 0, dp(8), 0, 0)
                    }
                    body.addView(presetsPanel, if (wide) LinearLayout.LayoutParams(0, -2, 1f)
                        else LinearLayout.LayoutParams(-1, -2))
                    Preset.entries.chunked(if (wide) 1 else 2).forEach { presets ->
                        val messages = row(presetsPanel)
                        presets.forEach { preset -> button(messages, preset.label) { session.pick(preset) } }
                    }
                }
                Stage.CONFIRM -> button(root, "보내기", highlighted = true, icon = "send") { session.send() }
                else -> Unit
            }
            if (selectable) button(root, "취소", icon = "close") { session.reset() }
        }

        button(root, if (voiceEnabled) "음성 시연 끄기" else "음성 시연 켜기", icon = "mic") { toggleVoice() }
        if (voiceNotice.isNotEmpty()) label(voiceNotice, 19f, accent)
        setContentView(ScrollView(this).apply {
            fitsSystemWindows = true
            isFillViewport = true
            setBackgroundColor(background)
            addView(root)
        })
        if (animateInbox && android.animation.ValueAnimator.areAnimatorsEnabled()) {
            content.alpha = 0f
            content.translationY = dp(24).toFloat()
            content.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(240)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
