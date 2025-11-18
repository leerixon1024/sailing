package com.spinnaker.sailing.genclasses



import kotlin.math.abs
import kotlin.math.floor

data class Latlongfunctions(val degrees: Int, val decimalMinutes: Double, val hemisphere: String)

fun decimalToDDM(decimalDegrees: Double, isLatitude: Boolean): Latlongfunctions {
    val hemisphere = when {
        isLatitude && decimalDegrees >= 0 -> "N"
        isLatitude && decimalDegrees < 0 -> "S"
        !isLatitude && decimalDegrees >= 0 -> "E"
        else -> "W"
    }
    val absDegrees = abs(decimalDegrees)
    val degrees = floor(absDegrees).toInt()
    val decimalMinutes = (absDegrees - degrees) * 60.0
    return Latlongfunctions(degrees, decimalMinutes, hemisphere)
}

fun ddmToDecimal(degrees: Int, decimalMinutes: Double, hemisphere: String): Double {
    var decimalDegrees = degrees + (decimalMinutes / 60.0)
    if (hemisphere == "S" || hemisphere == "W") {
        decimalDegrees *= -1.0
    }
    return decimalDegrees
}
