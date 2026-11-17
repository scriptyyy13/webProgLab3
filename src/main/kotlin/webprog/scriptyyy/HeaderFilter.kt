package webprog.scriptyyy

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.annotation.WebFilter
import jakarta.servlet.http.HttpServletRequest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@WebFilter(filterName = "HeaderFilter", urlPatterns = ["/*"])
class HeaderFilter : Filter {

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val req = request as? HttpServletRequest
        if (req != null) {
            val context = req.servletContext

            // достаем или инициализируем мапу со счетчиками в контексте
            @Suppress("UNCHECKED_CAST")
            var headerStats = context.getAttribute("headerStats") as? ConcurrentHashMap<String, AtomicLong>
            if (headerStats == null) {
                synchronized(context) {
                    headerStats = context.getAttribute("headerStats") as? ConcurrentHashMap<String, AtomicLong>
                    if (headerStats == null) {
                        headerStats = ConcurrentHashMap()
                        context.setAttribute("headerStats", headerStats)
                    }
                }
            }

            // собираем заголовки текущего запроса
            val headerNames = req.headerNames
            if (headerNames != null) {
                while (headerNames.hasMoreElements()) {
                    val headerName = headerNames.nextElement()
                    // заголовки регистронезависимые, приводим к нижнему регистру
                    headerStats!!.computeIfAbsent(headerName.lowercase()) { AtomicLong(0) }.incrementAndGet()
                }
            }
        }

        // пускаем запрос дальше к сервлетам
        chain.doFilter(request, response)
    }
}