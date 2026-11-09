<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="webprog.scriptyyy.PointResult" %>
<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Лабораторная работа 3</title>
    <link rel="stylesheet" href="css/style.css">

    <!-- скрипты -->
    <script src="js/htmx.min.js"></script>
</head>
<body x-data="labApp()">

    <!-- шапка -->
    <header id="main-header">
        <h1>Лаба намбер три по вебу</h1>
        <div class="student-info">
            <span>ФИО: <strong>Василенко Михаил Вадимович</strong></span> |
            <span>Группа: <strong>P3216</strong></span> |
            <span>Вариант: <strong>47483</strong></span>
        </div>
    </header>

    <main>
        <div class="content-grid">
            <div class="left-column">
                <!-- график -->
                <div class="card canvas-card">
                    <h2>Область координат</h2>
                    <div id="canvas-container">
                        <canvas id="graph" width="320" height="320" @click="handleCanvasClick($event)"></canvas>
                    </div>
                </div>

                <!-- инфо о сервере -->
                <div class="card server-status-card">
                    <h2>Информация о сервере</h2>
                    <div class="status-item">
                        <span>Статус:</span>
                        <strong :class="isServerOnline ? 'state-online' : 'state-offline'"
                                x-text="serverStatusText">Проверка...</strong>
                    </div>
                    <div class="status-item">
                        <span>Время до сервера:</span> <span x-text="timeTo">--</span> мс
                    </div>
                    <div class="status-item">
                        <span>Время от сервера:</span> <span x-text="timeFrom">--</span> мс
                    </div>
                    <div class="status-item">
                        <span>Общий пинг:</span> <span x-text="totalPing">--</span> мс
                    </div>
                </div>
            </div>

            <!-- форма -->
            <div class="card form-card">
                <h2>Параметры точки</h2>
                <form id="point-form"
                      hx-get="controller"
                      hx-target="#results-body"
                      hx-swap="afterbegin"
                      @submit="validateForm($event)"
                      @htmx:config-request="validateForm($event)">

                    <!-- поле ввода x -->
                    <div class="form-group">
                        <label class="group-title" for="x-input">Изменение X (-5 ... 3):</label>
                        <input type="text" id="x-input" name="x" x-model="x" maxlength="17" placeholder="Введите число строго от -5 до 3" autocomplete="off">
                        <div class="error-message" :style="xError ? 'display: block;' : 'display: none;'">Введите число строго в интервале (-5 ... 3)</div>
                    </div>

                    <!-- чекбоксы y -->
                    <div class="form-group">
                        <label class="group-title">Изменение Y (выберите одно или более значений):</label>
                        <div class="checkbox-group" id="y-checkboxes">
                            <template x-for="val in ['-4','-3','-2','-1','0','1','2','3','4']" :key="val">
                                <label class="checkbox-label">
                                    <input type="checkbox" name="y" :value="val" :checked="selectedY.includes(val)" @change="toggleY(val, $event.target.checked)">
                                    <span x-text="val"></span>
                                </label>
                            </template>
                        </div>
                        <div class="error-message" :style="yError ? 'display: block;' : 'display: none;'">Выберите хотя бы одно значение Y</div>
                    </div>

                    <!-- кнопки r -->
                    <div class="form-group">
                        <label class="group-title">Изменение R (кнопка):</label>
                        <div class="checkbox-group" id="r-buttons">
                            <template x-for="val in ['1','2','3','4','5']" :key="val">
                                <button type="button"
                                        :class="r === val ? 'btn-submit' : 'btn-clear'"
                                        style="padding: 12px 12px; margin-right: 5px;"
                                        @click="selectR(val)"
                                        x-text="val"></button>
                            </template>
                        </div>
                        <input type="hidden" name="r" :value="r">
                        <div class="error-message" :style="rError ? 'display: block;' : 'display: none;'">Выберите значение R</div>
                    </div>

                    <!-- баннер ошибки сервера -->
                    <div id="server-error-banner" class="server-error" style="display: none;"></div>

                    <div class="buttons-group">
                        <button type="submit" class="btn-submit">Проверить</button>
                        <!-- кнопка очистки истории -->
                        <button type="button"
                                class="btn-clear"
                                id="clear-btn"
                                hx-get="controller?action=clear"
                                hx-target="#results-body"
                                hx-swap="innerHTML"
                                @htmx:config-request.stop
                                @click="clearPoints()">Очистить историю</button>
                    </div>
                </form>
            </div>
        </div>

        <!-- таблица результатов -->
        <div class="card table-card">
            <h2>История проверок</h2>
            <table id="results-table">
                <thead>
                <tr>
                    <th>X</th>
                    <th>Y</th>
                    <th>R</th>
                    <th>Результат</th>
                    <th>Дата и время проверки</th>
                    <th>Время работы (мкс)</th>
                </tr>
                </thead>
                <tbody id="results-body">
                <%
                    List<PointResult> results = (List<PointResult>) application.getAttribute("results");
                    if (results != null) {
                        for (PointResult pt : results) {
                %>
                <tr>
                    <td><%= String.format(java.util.Locale.US, "%.2f", pt.getX()) %></td>
                    <td><%= String.format(java.util.Locale.US, "%.2f", pt.getY()) %></td>
                    <td><%= pt.getR() %></td>
                    <td class="<%= pt.getHit() ? "hit-true" : "hit-false" %>"><%= pt.getHit() ? "Есть пробитие" : "Промазал" %></td>
                    <td><%= pt.getCurrentTime() %></td>
                    <td><%= pt.getExecutionTimeMicro() %></td>
                </tr>
                <%
                        }
                    }
                %>
                </tbody>
            </table>
        </div>
    </main>

    <script id="initial-points-data" type="application/json">
        <%-- тут мы кладем точки в такой массив, чтобы после загрузки страницы их отсюда достать в методе init и клиент их мог нарисовать без лишнего запроса --%>
                [
        <%
            if (results != null) {
                for (int i = 0; i < results.size(); i++) {
                    PointResult pt = results.get(i);
        %>
        {"x": <%= pt.getX() %>, "y": <%= pt.getY() %>, "r": <%= pt.getR() %>, "hit": <%= pt.getHit() %>}<%= (i < results.size() - 1) ? "," : "" %>
        <%
            }
        }
    %>
        ]
    </script>

    <footer id="footer-info">
        Это подвал | Но тут не будет шутки про детей
    </footer>

    <script src="js/script.js"></script>
    <script src="js/alpine.min.js"></script>
</body>
</html>