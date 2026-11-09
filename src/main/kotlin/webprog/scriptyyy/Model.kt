package webprog.scriptyyy

import java.io.Serializable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// моделька для точки
data class PointResult(
    val x: Double,
    val y: Double,
    val r: Double,
    val hit: Boolean,
    val currentTime: String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")),
    val executionTimeMicro: Long
) : Serializable

// чекер попадания по графику
object AreaChecker {
    fun check(x: Double, y: Double, r: Double): Boolean {
        // вторая четверть: треугольник
        if (x <= 0 && y >= 0) {
            return y <= (x + r / 2.0)
        }
        // третья четверть: прямоугольник
        if (x <= 0 && y <= 0) {
            return x >= -r && y >= -r / 2.0
        }
        // четвертая четверть: четверть круга
        if (x >= 0 && y <= 0) {
            return (x * x + y * y) <= (r * r)
        }
        return false
    }
}