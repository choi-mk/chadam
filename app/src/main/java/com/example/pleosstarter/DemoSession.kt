package com.example.pleosstarter

enum class Direction(val label: String) {
    FRONT("앞"), REAR("뒤"), LEFT("왼쪽"), RIGHT("오른쪽");

    fun opposite(): Direction = when (this) {
        FRONT -> REAR
        REAR -> FRONT
        LEFT -> RIGHT
        RIGHT -> LEFT
    }
}

data class NearbyCar(val id: String, val direction: Direction, val meters: Int)

enum class Preset(val label: String, vararg val phrases: String) {
    SIGNAL("신호가 바뀌었습니다.", "신호바뀜", "신호가바뀌었습니다"),
    CRASH("전방 사고로 통행이 어렵습니다.", "앞에사고", "사고", "전방사고로통행이어렵습니다"),
    DEBRIS("전방 도로에 낙하물이 있습니다.", "낙하물", "전방도로에낙하물이있습니다"),
    TRUNK("트렁크가 열려 있는 것 같습니다.", "트렁크열림", "트렁크가열려있는것같습니다"),
    PARK("후진 주차를 준비 중입니다.", "주차중", "후진주차", "후진주차를준비중입니다"),
    THANKS("양보해 주셔서 감사합니다.", "고마워", "감사합니다", "양보해주셔서감사합니다"),
    SORRY("불편을 드려 죄송합니다.", "미안해", "죄송합니다", "불편을드려죄송합니다")
}

enum class Stage { IDLE, TARGET, PRESET, CONFIRM, INBOX, READ }

class DemoSession(private val now: () -> Long) {
    var stage = Stage.IDLE
        private set
    var selected: NearbyCar? = null
        private set
    var preset: Preset? = null
        private set
    var notice = "음성 시연을 시작하거나 메시지 보내기를 선택하세요."
        private set
    // The demo assumes both cars face the same way and snapshots the position at send time.
    var senderDirection: Direction? = null
        private set
    var deadline = 0L
        private set
    var cars = listOf(
        NearbyCar("B", Direction.FRONT, 12), NearbyCar("C", Direction.REAR, 18),
        NearbyCar("D", Direction.LEFT, 6), NearbyCar("E", Direction.RIGHT, 8)
    )
        private set

    fun start() {
        reset("보낼 차량을 선택하세요.")
        stage = Stage.TARGET
    }

    fun choose(direction: Direction) {
        if (stage != Stage.TARGET && stage != Stage.PRESET && stage != Stage.CONFIRM) return
        selected = cars.firstOrNull { it.direction == direction }
        preset = null
        stage = if (selected == null) Stage.TARGET else Stage.PRESET
        notice = if (selected == null) "해당 방향에 차량이 없습니다." else "보낼 문구를 선택하세요."
    }

    fun pick(value: Preset) {
        val car = selected ?: return
        if (stage != Stage.PRESET && stage != Stage.CONFIRM) return
        preset = value
        stage = Stage.CONFIRM
        notice = "${car.direction.label} 차량 ${car.id}에 ‘${value.label}’를 보낼까요?"
    }

    fun send() {
        if (stage != Stage.CONFIRM) return
        if (cars.none { it.id == selected?.id }) {
            start()
            notice = "선택했던 차량이 범위를 벗어났습니다. 다시 선택하세요."
            return
        }
        senderDirection = checkNotNull(selected).direction.opposite()
        stage = Stage.INBOX
        deadline = now() + 20_000
        notice = "${checkNotNull(senderDirection).label} 차량에서 메시지가 왔습니다."
    }

    fun expire() {
        if (stage == Stage.INBOX && now() >= deadline) reset("수락 시간이 지나 메시지가 만료되었습니다.")
    }

    fun accept() {
        expire()
        if (stage != Stage.INBOX) return
        stage = Stage.READ
        notice = checkNotNull(preset).label
    }

    fun decline() {
        expire()
        if (stage == Stage.INBOX) reset("상대 차량이 메시지를 거절했습니다.")
    }

    fun reset(message: String = "보낼 메시지를 시작하세요.") {
        stage = Stage.IDLE
        selected = null
        preset = null
        senderDirection = null
        deadline = 0
        notice = message
    }

    fun command(text: String): Boolean {
        val value = text.filter { it.isLetterOrDigit() }
        when {
            value == "취소" && stage in listOf(Stage.TARGET, Stage.PRESET, Stage.CONFIRM) -> reset()
            stage == Stage.IDLE && value in listOf("메시지전송", "메세지전송") -> start()
            stage == Stage.CONFIRM && value in listOf("보내", "전송", "보내기") -> send()
            stage == Stage.INBOX && value in listOf("확인", "수락") -> accept()
            stage == Stage.INBOX && value == "거절" -> decline()
            stage in listOf(Stage.TARGET, Stage.PRESET, Stage.CONFIRM) -> {
                val direction = Direction.entries.firstOrNull {
                    value in listOf(it.label, it.label + "차량", it.label + "차")
                }
                val message = Preset.entries.firstOrNull { value in it.phrases }
                when {
                    direction != null -> choose(direction)
                    message != null && stage != Stage.TARGET -> pick(message)
                    else -> return false
                }
            }
            else -> return false
        }
        return true
    }
}

