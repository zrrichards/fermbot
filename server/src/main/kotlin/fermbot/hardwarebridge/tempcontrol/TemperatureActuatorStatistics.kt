package fermbot.hardwarebridge.tempcontrol

//class TemperatureActuatorStatistics {
//    var coolingTime = Duration.ZERO
//        private set
//
//    var heatingTime = Duration.ZERO
//        private set
//
//    var offTime = Duration.ZERO
//        private set
//
//    val totalTime: Duration
//        get() = coolingTime + heatingTime + offTime
//
//    fun percentInMode(heatingMode: HeatingMode) = when (heatingMode) {
//        HeatingMode.HEATING -> heatingTime / totalTime
//        HeatingMode.COOLING -> coolingTime / totalTime
//        HeatingMode.OFF -> offTime / totalTime
//    }
//
//    fun addTime(duration: Duration, heatingMode: HeatingMode) {
//        when (heatingMode) {
//            HeatingMode.OFF -> offTime += duration
//            HeatingMode.HEATING -> heatingTime += duration
//            HeatingMode.COOLING -> coolingTime += duration
//        }
//    }
//
//    fun reset() {
//        coolingTime = Duration.ZERO
//        heatingTime = Duration.ZERO
//        offTime = Duration.ZERO
//    }
//}


