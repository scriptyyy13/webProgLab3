package webprog.scriptyyy

import jakarta.servlet.annotation.WebServlet
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

@WebServlet(name = "ControllerServlet", urlPatterns = ["", "/controller"])
class ControllerServlet : HttpServlet() {

    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        val action = req.getParameter("action")

        // проверка доступности сервера
        if ("ping" == action) {
            resp.contentType = "application/json;charset=UTF-8"
            resp.writer.print("""{"status":"ok","serverUnixTime":${System.currentTimeMillis()}}""")
            return
        }

        // очистка сохраненных точек
        if ("clear" == action) {
            servletContext.removeAttribute("results")
            resp.contentType = "text/plain;charset=UTF-8"
            resp.writer.print("")
            return
        }

        val x = req.getParameter("x")
        val y = req.getParameterValues("y")
        val r = req.getParameter("r")

        // если есть координаты, передаем запрос на проверку
        if (x != null || y != null || r != null) {
            req.getRequestDispatcher("/check").forward(req, resp)
        } else {
            // иначе просто открываем страницу с формой
            req.getRequestDispatcher("/index.jsp").forward(req, resp)
        }
    }
}