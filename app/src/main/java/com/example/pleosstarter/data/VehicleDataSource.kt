package com.example.pleosstarter.data

data class VehicleSnapshot(val gear: String, val speedKph: Int)

/** UI와 차량 연동을 분리합니다. 실제 SDK 연동 시 이 경계를 구현하세요. */
interface VehicleDataSource {
    fun snapshot(): VehicleSnapshot
}

class DemoVehicleDataSource : VehicleDataSource {
    private var driving = false

    override fun snapshot() = if (driving) VehicleSnapshot("D", 40) else VehicleSnapshot("P", 0)

    fun toggleScenario() { driving = !driving }
}
