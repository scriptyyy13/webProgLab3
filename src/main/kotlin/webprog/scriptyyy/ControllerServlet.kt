package webprog.scriptyyy

import jakarta.servlet.annotation.WebServlet
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

@WebServlet(name = "ControllerServlet", urlPatterns = ["", "/controller"])
class ControllerServlet : HttpServlet() {

    // отдаем саму страницу
    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
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