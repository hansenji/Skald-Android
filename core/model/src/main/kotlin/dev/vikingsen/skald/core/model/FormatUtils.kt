package dev.vikingsen.skald.core.model

fun formatDuration(seconds: Double): String {
    val h = (seconds / 3600).toInt()
    val m = ((seconds % 3600) / 60).toInt()
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

fun formatPosition(seconds: Double): String {
    val h = (seconds / 3600).toInt()
    val m = ((seconds % 3600) / 60).toInt()
    val s = (seconds % 60).toInt()
    val hStr = if (h > 0) "$h:" else ""
    val mStr = if (h > 0) m.toString().padStart(2, '0') else m.toString()
    val sStr = s.toString().padStart(2, '0')
    return "$hStr$mStr:$sStr"
}
