const Calendar = {
    instance: null,
    currentEvents: [],

    init(onDateClick) {
        const calendarEl = document.getElementById('calendar');
        this.instance = new FullCalendar.Calendar(calendarEl, {
            locale: 'zh-cn',
            initialView: 'dayGridMonth',
            headerToolbar: {
                left: 'prev,next today',
                center: 'title',
                right: 'dayGridMonth',
            },
            height: 'auto',
            dateClick: (info) => {
                onDateClick(info.dateStr);
            },
            eventDidMount: (info) => {
                // 在有日报的日期格子上添加标记点
                const dayCell = info.el.closest('.fc-daygrid-day');
                if (dayCell && !dayCell.querySelector('.report-dot')) {
                    const dot = document.createElement('span');
                    dot.className = 'report-dot';
                    dayCell.style.position = 'relative';
                    dayCell.appendChild(dot);
                }
            },
        });
        this.instance.render();
    },

    loadEvents(reports) {
        // 清除旧事件
        this.currentEvents.forEach(e => e.remove());
        this.currentEvents = [];

        reports.forEach(report => {
            const event = this.instance.addEvent({
                id: report.id,
                start: report.reportDate,
                allDay: true,
                display: 'background',
                backgroundColor: 'transparent',
            });
            this.currentEvents.push(event);
        });
    },

    getDateStr() {
        // 返回当前视图的年月 "2026-05" 格式
        const date = this.instance.getDate();
        const y = date.getFullYear();
        const m = String(date.getMonth() + 1).padStart(2, '0');
        return `${y}-${m}`;
    },
};
