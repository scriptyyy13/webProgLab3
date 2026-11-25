package webprog.scriptyyy

import jakarta.servlet.annotation.WebServlet
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

@WebServlet(name = "AreaCheckServlet", urlPatterns = ["/check"])
class AreaCheckServlet : HttpServlet() {

    // берем список точек из контекста сервлета
    @Suppress("UNCHECKED_CAST")
    private fun getResultsList(): CopyOnWriteArrayList<PointResult> {
        val context = servletContext
        synchronized(context) {
            var list = context.getAttribute("results") as? CopyOnWriteArrayList<PointResult>
            if (list == null) {
                list = CopyOnWriteArrayList()
                context.setAttribute("results", list)
            }
            return list
        }
    }

    // достаем мапу со временем запросов по ip
    @Suppress("UNCHECKED_CAST")
    private fun getIpTimestamps(): ConcurrentHashMap<String, MutableList<Long>> {
        val context = servletContext
        synchronized(context) {
            var map = context.getAttribute("ipTimestamps") as? ConcurrentHashMap<String, MutableList<Long>>
            if (map == null) {
                map = ConcurrentHashMap()
                context.setAttribute("ipTimestamps", map)
            }
            return map
        }
    }

    // получаем реальный ip из заголовков cloudflare или прокси
    private fun getClientIp(req: HttpServletRequest): String {
        val cfIp = req.getHeader("cf-connecting-ip")?.trim()
        if (!cfIp.isNullOrEmpty()) return cfIp

        val xff = req.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
        if (!xff.isNullOrEmpty()) return xff

        return req.remoteAddr ?: "unknown"
    }

    override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        val startTime = System.nanoTime()

        // парсим параметры и меняем запятую на точку если надо
        val x = req.getParameter("x")?.replace(',', '.')?.toDoubleOrNull()
        val yValues = req.getParameterValues("y")?.mapNotNull { it.replace(',', '.').toDoubleOrNull() }
        val r = req.getParameter("r")?.replace(',', '.')?.toDoubleOrNull()

        // проверка диапазона
        if (x == null || yValues.isNullOrEmpty() || r == null || x <= -5.0 || x >= 3.0 || r !in 1.0..5.0) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "некорректные параметры")
            return
        }

        // проверяем лимит 25 точек в минуту с одного ip
        val clientIp = getClientIp(req)
        val timestamps = getIpTimestamps().computeIfAbsent(clientIp) { mutableListOf() }
        val now = System.currentTimeMillis()
        val oneMinuteAgo = now - 60_000L

        synchronized(timestamps) {
            // чистим старые запросы, которые были больше минуты назад
            timestamps.removeAll { it < oneMinuteAgo }

            if (timestamps.size + yValues.size > 25) {
                resp.sendError(429, "лимит 25 точек в минуту")
                return
            }

            // записываем новые точки в окно
            repeat(yValues.size) {
                timestamps.add(now)
            }
        }

        val resultsList = getResultsList()
        val htmlRows = StringBuilder()

        // проверяем попадание для каждого выбранного y
        for (y in yValues) {
            val hit = AreaChecker.check(x, y, r)
            val execTime = (System.nanoTime() - startTime) / 1000
            val res = PointResult(x, y, r, hit, executionTimeMicro = execTime)

            // добавляем в начало общего списка
            resultsList.add(0, res)

            val hitClass = if (res.hit) "hit-true" else "hit-false"
            val hitText = if (res.hit) "Есть пробитие" else "Промазал"

            // формируем строку для вставки в таблицу
            htmlRows.append("""
                <tr>
                    <td>${String.format(Locale.US, "%.2f", res.x)}</td>
                    <td>${String.format(Locale.US, "%.2f", res.y)}</td>
                    <td>${res.r.toInt()}</td>
                    <td class="$hitClass">$hitText</td>
                    <td>${res.currentTime}</td>
                    <td>${res.executionTimeMicro}</td>
                </tr>
            """.trimIndent())
        }

        // отдаем готовые строки обратно
        resp.contentType = "text/html;charset=UTF-8"
        resp.writer.print(htmlRows.toString())
    }
}