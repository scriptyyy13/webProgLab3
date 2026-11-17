package webprog.scriptyyy

import jakarta.servlet.annotation.WebServlet
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@WebServlet(name = "HeaderStatsServlet", urlPatterns = ["/headers"])
class HeaderStatsServlet : HttpServlet() {

    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        val context = servletContext
        @Suppress("UNCHECKED_CAST")
        val headerStats = context.getAttribute("headerStats") as? ConcurrentHashMap<String, AtomicLong>

        resp.contentType = "text/html;charset=UTF-8"
        val out = resp.writer

        out.println("""
            <!DOCTYPE html>
            <html lang="ru">
            <head>
                <meta charset="UTF-8">
                <title>Статистика HTTP заголовков</title>
                <link rel="stylesheet" href="css/headers.css">
            </head>
            <body>
                <h2>Статистика входящих заголовков</h2>
        """.trimIndent())

        if (headerStats.isNullOrEmpty()) {
            out.println("<p>Заголовки еще не были зафиксированы</p>")
        } else {
            out.println("""
                <table>
                    <thead>
                        <tr>
                            <th>Имя заголовка</th>
                            <th>Количество запросов</th>
                        </tr>
                    </thead>
                    <tbody>
            """.trimIndent())

            // сортируем по убыванию количества
            headerStats.entries.sortedByDescending { it.value.get() }.forEach { (name, count) ->
                out.println("<tr><td><code>$name</code></td><td>${count.get()}</td></tr>")
            }

            out.println("</tbody></table>")
        }

        out.println("""
                <br>
                <a href="${req.contextPath}/controller">назад к приложению</a>
            </body>
            </html>
        """.trimIndent())
    }
}