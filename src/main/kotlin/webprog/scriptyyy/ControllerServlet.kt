package webprog.scriptyyy

import jakarta.servlet.annotation.WebServlet
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.util.Locale

@WebServlet(name = "ControllerServlet", urlPatterns = ["", "/controller"])
class ControllerServlet : HttpServlet() {

    // отдаем страницу или json со списком точек
    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        val accept = req.getHeader("Accept") ?: ""

        if (accept.contains("application/json")) {
            @Suppress("UNCHECKED_CAST")
            val list = servletContext.getAttribute("results") as? List<PointResult> ?: emptyList()

            val json = list.joinToString(prefix = "[", postfix = "]", separator = ",") { pt ->
                String.format(
                    Locale.US, // чтобы точки были с точкой, а не с запятой
                    """{"x":%.2f,"y":%.2f,"r":${pt.r},"hit":%b,"currentTime":"%s","executionTimeMicro":%d}""",
                    pt.x, pt.y, pt.hit, pt.currentTime, pt.executionTimeMicro
                )
            }

            resp.contentType = "application/json;charset=UTF-8"
            resp.setHeader("Cache-Control", "no-store")
            resp.writer.print(json)
            return
        }

        req.getRequestDispatcher("/index.jsp").forward(req, resp)
    }

    // отправка новой точки на проверку
    override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        req.getRequestDispatcher("/check").forward(req, resp)
    }

    // очистка списка точек
    override fun doDelete(req: HttpServletRequest, resp: HttpServletResponse) {
        servletContext.removeAttribute("results")
        resp.status = HttpServletResponse.SC_OK
        resp.contentType = "text/plain;charset=UTF-8"
        resp.writer.print("")
    }

    // проверка доступности сервера без тела ответа
    override fun doHead(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.status = HttpServletResponse.SC_OK
        resp.contentType = "text/plain;charset=UTF-8"
    }
}