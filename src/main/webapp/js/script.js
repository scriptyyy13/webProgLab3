let lastErrorMsg = "";
let lastFormSoundTime = 0;

// проигрывание звука
function playSound(name) {
    const audio = new Audio(`sounds/${name}.mp3`);
    audio.volume = 0.6;
    audio.play().catch(() => {});
}

// звук ошибки ввода с кулдауном
function playFormErrorSound() {
    const now = Date.now();
    if (now - lastFormSoundTime < 300) return;
    lastFormSoundTime = now;

    const randomNum = Math.floor(Math.random() * 2) + 2;
    playSound(randomNum);
}

// показ и скрытие плашки ошибки
function showServerError(msg) {
    const errBanner = document.getElementById("server-error-banner");
    if (!errBanner) return;

    if (msg) {
        if (errBanner.style.display === "none" || lastErrorMsg !== msg) {
            playSound('1');
            lastErrorMsg = msg;
        }
        errBanner.textContent = msg;
        errBanner.style.display = "block";
    } else {
        lastErrorMsg = "";
        errBanner.style.display = "none";
        errBanner.textContent = "";
    }
}

function labApp() {
    return {
        x: '',
        selectedY: [],
        r: '3',
        xError: false,
        yError: false,
        rError: false,
        points: [],

        isServerOnline: false,
        serverStatusText: 'Проверка...',
        timeTo: '--',
        timeFrom: '--',
        totalPing: '--',

        canvas: null,
        ctx: null,
        scale: 120,
        bgImage: new Image(),

        init() {
            this.canvas = document.getElementById("graph");
            this.ctx = this.canvas.getContext("2d");

            try {
                const initialEl = document.getElementById("initial-points-data");
                if (initialEl && initialEl.textContent.trim()) {
                    this.points = JSON.parse(initialEl.textContent);
                }
            } catch (e) {
                this.points = [];
            }

            this.bgImage.src = "img/cat.png";
            this.bgImage.onload = () => this.drawArea();
            this.bgImage.onerror = () => this.drawArea();

            this.drawArea();

            // обработка ответа от сервера
            document.body.addEventListener('htmx:afterOnLoad', (evt) => {
                if (evt.detail.target && evt.detail.target.id === 'results-body') {
                    showServerError(null);

                    // проверяем попадание и включаем звук
                    const resp = evt.detail.xhr ? evt.detail.xhr.responseText : "";
                    if (resp.includes("hit-true") || resp.includes("пробитие")) {
                        playSound('ok');
                    } else if (resp.includes("hit-false") || resp.includes("Промазал")) {
                        playSound('fail');
                    }

                    setTimeout(() => {
                        this.syncPointsFromTable();
                        this.drawArea();
                    }, 10);
                }
            });

            // ошибка отправки запроса
            document.body.addEventListener('htmx:sendError', () => {
                this.isServerOnline = false;
                this.serverStatusText = 'Недоступен';
                showServerError("Сервер недоступен. Ошибка отправки запроса.");
            });

            // сервер вернул ошибку
            document.body.addEventListener('htmx:responseError', (evt) => {
                showServerError("Ошибка сервера: код " + evt.detail.xhr.status);
            });

            this.pingServer();
            setInterval(() => this.pingServer(), 2000);
        },

        // вытаскиваем точки из таблицы
        syncPointsFromTable() {
            const rows = document.querySelectorAll("#results-body tr");
            const list = [];
            rows.forEach(tr => {
                const tds = tr.querySelectorAll("td");
                if (tds.length >= 4) {
                    const x = parseFloat(tds[0].textContent.replace(',', '.'));
                    const y = parseFloat(tds[1].textContent.replace(',', '.'));
                    const ptR = parseFloat(tds[2].textContent.replace(',', '.'));
                    const hit = tds[3].classList.contains("hit-true") || tds[3].textContent.includes("пробитие");
                    if (!isNaN(x) && !isNaN(y)) {
                        list.push({ x, y, r: ptR, hit });
                    }
                }
            });
            this.points = list;
        },

        selectR(val) {
            this.r = val;
            this.rError = false;
            this.drawArea();
        },

        toggleY(val, checked) {
            if (checked) {
                this.selectedY.push(val);
            } else {
                this.selectedY = this.selectedY.filter(item => item !== val);
            }
            this.yError = false;
        },

        // проверка полей перед отправкой
        validateForm(e) {
            // клик по очистке не валидируем
            if (e && e.detail && e.detail.elt && e.detail.elt.classList.contains("btn-clear")) {
                return true;
            }
            if (e && e.target && e.target.classList && e.target.classList.contains("btn-clear")) {
                return true;
            }

            showServerError(null);

            if (!this.isServerOnline) {
                showServerError("Сервер недоступен. Отправка формы невозможна.");
                if (e) e.preventDefault();
                return false;
            }

            this.xError = false;
            this.yError = false;
            this.rError = false;

            const numX = parseFloat(this.x.replace(',', '.'));
            let hasError = false;

            if (this.x.trim() === "" || isNaN(numX) || numX <= -5 || numX >= 3) {
                this.xError = true;
                hasError = true;
            }

            if (this.selectedY.length === 0) {
                this.yError = true;
                hasError = true;
            }

            if (!this.r) {
                this.rError = true;
                hasError = true;
            }

            // если есть ошибки играем звук и отменяем отправку
            if (hasError) {
                playFormErrorSound();
                if (e) e.preventDefault();
                return false;
            }
            return true;
        },

        clearPoints() {
            this.points = [];
            this.xError = false;
            this.yError = false;
            this.rError = false;
            showServerError(null);
            this.drawArea();
        },

        handleCanvasClick(e) {
            showServerError(null);

            if (!this.isServerOnline) {
                showServerError("Сервер недоступен. Проверка точки невозможна.");
                return;
            }

            if (!this.r) {
                playFormErrorSound();
                alert("Невозможно определить координаты: сначала выберите радиус R!");
                return;
            }

            const rect = this.canvas.getBoundingClientRect();
            const clickX = e.clientX - rect.left;
            const clickY = e.clientY - rect.top;

            const centerX = this.canvas.width / 2;
            const centerY = this.canvas.height / 2;

            const rNum = parseFloat(this.r);
            const mathX = (((clickX - centerX) / this.scale) * rNum).toFixed(2);
            const mathY = (((centerY - clickY) / this.scale) * rNum).toFixed(2);

            htmx.ajax('GET', `controller?x=${mathX}&y=${mathY}&r=${this.r}`, {
                target: '#results-body',
                swap: 'afterbegin'
            });
        },

        // отрисовка графика
        drawArea() {
            if (!this.ctx || !this.canvas) return;

            const ctx = this.ctx;
            const width = this.canvas.width;
            const height = this.canvas.height;
            const centerX = width / 2;
            const centerY = height / 2;
            const r = parseFloat(this.r) || 3;

            ctx.clearRect(0, 0, width, height);

            if (this.bgImage.complete && this.bgImage.naturalWidth !== 0) {
                ctx.save();
                ctx.globalAlpha = 0.2;
                ctx.drawImage(this.bgImage, 0, 0, width, height);
                ctx.restore();
            }

            ctx.fillStyle = "rgba(113, 128, 150, 0.4)";
            ctx.strokeStyle = "#4a5568";
            ctx.lineWidth = 1.5;

            // треугольник во 2 четверти
            ctx.beginPath();
            ctx.moveTo(centerX, centerY);
            ctx.lineTo(centerX - this.scale / 2, centerY);
            ctx.lineTo(centerX, centerY - this.scale / 2);
            ctx.closePath();
            ctx.fill();
            ctx.stroke();

            // прямоугольник в 3 четверти
            ctx.beginPath();
            ctx.rect(centerX - this.scale, centerY, this.scale, this.scale / 2);
            ctx.fill();
            ctx.stroke();

            // сектор круга в 4 четверти
            ctx.beginPath();
            ctx.moveTo(centerX, centerY);
            ctx.arc(centerX, centerY, this.scale, 0, Math.PI / 2, false);
            ctx.closePath();
            ctx.fill();
            ctx.stroke();

            ctx.strokeStyle = "#2d3748";
            ctx.lineWidth = 1.5;
            ctx.fillStyle = "#2d3748";
            ctx.font = "11px Arial";

            // ось x со стрелкой
            ctx.beginPath();
            ctx.moveTo(10, centerY);
            ctx.lineTo(width - 10, centerY);
            ctx.stroke();
            ctx.beginPath();
            ctx.moveTo(width - 10, centerY);
            ctx.lineTo(width - 16, centerY - 4);
            ctx.lineTo(width - 16, centerY + 4);
            ctx.fill();

            // ось y со стрелкой
            ctx.beginPath();
            ctx.moveTo(centerX, height - 10);
            ctx.lineTo(centerX, 10);
            ctx.stroke();
            ctx.beginPath();
            ctx.moveTo(centerX, 10);
            ctx.lineTo(centerX - 4, 16);
            ctx.lineTo(centerX + 4, 16);
            ctx.fill();

            ctx.fillText("x", width - 12, centerY - 8);
            ctx.fillText("y", centerX + 8, 14);

            const rVal = this.r ? this.r : "R";
            const rHalfVal = this.r ? (r / 2) : "R/2";

            const labels = [
                { x: centerX + this.scale, y: centerY + 14, text: rVal },
                { x: centerX + this.scale / 2, y: centerY + 14, text: rHalfVal },
                { x: centerX - this.scale / 2, y: centerY + 14, text: "-" + rHalfVal },
                { x: centerX - this.scale, y: centerY + 14, text: "-" + rVal },

                { x: centerX + 8, y: centerY - this.scale + 4, text: rVal },
                { x: centerX + 8, y: centerY - this.scale / 2 + 4, text: rHalfVal },
                { x: centerX + 8, y: centerY + this.scale / 2 + 4, text: "-" + rHalfVal },
                { x: centerX + 8, y: centerY + this.scale + 4, text: "-" + rVal }
            ];

            ctx.strokeStyle = "#4a5568";
            ctx.lineWidth = 1;

            // засечки x
            [centerX - this.scale, centerX - this.scale / 2, centerX + this.scale / 2, centerX + this.scale].forEach(xPx => {
                ctx.beginPath();
                ctx.moveTo(xPx, centerY - 3);
                ctx.lineTo(xPx, centerY + 3);
                ctx.stroke();
            });

            // засечки y
            [centerY - this.scale, centerY - this.scale / 2, centerY + this.scale / 2, centerY + this.scale].forEach(yPx => {
                ctx.beginPath();
                ctx.moveTo(centerX - 3, yPx);
                ctx.lineTo(centerX + 3, yPx);
                ctx.stroke();
            });

            labels.forEach(l => ctx.fillText(l.text, l.x, l.y));

            // рисуем точки
            this.points.forEach(pt => {
                const px = centerX + (pt.x / r) * this.scale;
                const py = centerY - (pt.y / r) * this.scale;

                ctx.beginPath();
                ctx.arc(px, py, 4, 0, 2 * Math.PI);
                if (Number(r) === Number(pt.r)) {
                    ctx.fillStyle = pt.hit ? "#2f855a" : "#e53e3e";
                } else {
                    ctx.fillStyle = pt.hit ? "#1c4c34" : "#6a1120";
                }
                ctx.fill();
                ctx.strokeStyle = "#ffffff";
                ctx.lineWidth = 1;
                ctx.stroke();
            });
        },

        // пинг сервера
        pingServer() {
            const sendTime = Date.now();
            fetch('controller?action=ping')
                .then(res => {
                    if (!res.ok) throw new Error("Offline");
                    return res.json();
                })
                .then(data => {
                    const receiveTime = Date.now();
                    const rtt = receiveTime - sendTime;

                    this.isServerOnline = true;
                    this.serverStatusText = 'Доступен';
                    showServerError(null);

                    this.timeTo = Math.max(0, (rtt / 2)).toFixed(1);
                    this.timeFrom = Math.max(0, (rtt / 2)).toFixed(1);
                    this.totalPing = rtt.toFixed(1);
                })
                .catch(() => {
                    this.isServerOnline = false;
                    this.serverStatusText = 'Недоступен';
                    showServerError("Сервер недоступен (соединение разорвано)");
                    this.timeTo = '--';
                    this.timeFrom = '--';
                    this.totalPing = '--';
                });
        }
    };
}