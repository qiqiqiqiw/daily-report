const App = {
    repositories: [],
    selectedRepoId: null,
    currentReport: null,
    selectedDate: null,

    async init() {
        console.log('[DEBUG] App.init start');
        await this.loadRepositories();
        await this.loadSettings();
        Calendar.init((dateStr) => this.onDateClick(dateStr));
        this.bindEvents();
        await this.loadCalendarData();
        console.log('[DEBUG] App.init done, selectedRepoId:', this.selectedRepoId);
    },

    bindEvents() {
        // Header
        document.getElementById('repoSelector').addEventListener('change', (e) => {
            this.selectedRepoId = e.target.value ? Number(e.target.value) : null;
            this.loadCalendarData();
        });
        document.getElementById('btnAddRepo').addEventListener('click', () => this.showModal('repoModal'));
        document.getElementById('btnSettings').addEventListener('click', () => this.openSettings());

        // Report Modal
        document.getElementById('btnCloseReport').addEventListener('click', () => this.closeModal('reportModal'));
        document.getElementById('btnCloseReportFooter').addEventListener('click', () => this.closeModal('reportModal'));
        document.getElementById('reportModal').querySelector('.modal-overlay').addEventListener('click', () => this.closeModal('reportModal'));
        document.getElementById('btnGenerateReport').addEventListener('click', () => this.generateReport());
        document.getElementById('btnSaveReport').addEventListener('click', () => this.saveReport());
        document.getElementById('btnCopyReport').addEventListener('click', () => this.copyReport());
        document.getElementById('btnDeleteReport').addEventListener('click', () => this.deleteReport());

        // Repo Modal
        document.getElementById('btnCloseRepo').addEventListener('click', () => this.closeModal('repoModal'));
        document.getElementById('repoModal').querySelector('.modal-overlay').addEventListener('click', () => this.closeModal('repoModal'));
        document.getElementById('btnConfirmAddRepo').addEventListener('click', () => this.addRepository());
        document.getElementById('btnCancelAddRepo').addEventListener('click', () => this.closeModal('repoModal'));
        document.getElementById('btnPickFolder').addEventListener('click', () => this.pickFolder());

        // Settings Modal
        document.getElementById('btnCloseSettings').addEventListener('click', () => this.closeModal('settingsModal'));
        document.getElementById('settingsModal').querySelector('.modal-overlay').addEventListener('click', () => this.closeModal('settingsModal'));
        document.getElementById('btnSaveSettings').addEventListener('click', () => this.saveSettings());
        document.getElementById('btnCancelSettings').addEventListener('click', () => this.closeModal('settingsModal'));
        document.getElementById('btnToggleApiKey').addEventListener('click', () => this.toggleApiKeyVisibility());

        // 监听日历翻页
        document.addEventListener('click', (e) => {
            if (e.target.closest('.fc-prev-button') || e.target.closest('.fc-next-button') || e.target.closest('.fc-today-button')) {
                setTimeout(() => this.loadCalendarData(), 100);
            }
        });
    },

    // === Repositories ===

    async loadRepositories() {
        try {
            this.repositories = await API.getRepositories();
            this.renderRepoSelectors();
        } catch (e) {
            this.showToast('加载仓库列表失败: ' + e.message);
        }
    },

    renderRepoSelectors() {
        const selector = document.getElementById('repoSelector');
        const settingsSelect = document.getElementById('inputDefaultRepo');
        const opts = this.repositories.map(r =>
            `<option value="${r.id}">${r.name}</option>`
        ).join('');

        selector.innerHTML = '<option value="">独立模式</option>' + opts;
        settingsSelect.innerHTML = '<option value="">不设置</option>' + opts;

        if (this.selectedRepoId) {
            selector.value = this.selectedRepoId;
        }
    },

    async addRepository() {
        const path = document.getElementById('inputRepoPath').value.trim();
        if (!path) {
            this.showToast('请选择或输入仓库路径');
            return;
        }
        // Auto-derive name from path: take the last path segment
        const name = path.replace(/[\\/]+$/, '').split(/[\\/]/).pop() || path;
        try {
            await API.addRepository(name, path);
            this.closeModal('repoModal');
            document.getElementById('inputRepoPath').value = '';
            await this.loadRepositories();
            this.showToast('仓库添加成功');
        } catch (e) {
            this.showToast('添加失败: ' + e.message);
        }
    },

    async pickFolder() {
        if (window.showDirectoryPicker) {
            try {
                const dirHandle = await window.showDirectoryPicker();
                // showDirectoryPicker doesn't expose the full path in standard browsers
                // but we can try reading it from the handle's name
                // For local dev tools running on localhost, we fall back to text input
                document.getElementById('inputRepoPath').value = dirHandle.name;
                this.showToast('已选择文件夹: ' + dirHandle.name + '，请补充完整路径');
            } catch (e) {
                if (e.name !== 'AbortError') {
                    this.showToast('无法获取文件夹路径，请手动输入');
                }
            }
        } else {
            this.showToast('当前浏览器不支持文件夹选择，请手动输入路径');
        }
    },

    // === Calendar ===

    async loadCalendarData() {
        const month = Calendar.getDateStr();
        try {
            const reports = await API.getMonthlyReports(month, this.selectedRepoId || 0);
            Calendar.loadEvents(reports);
        } catch (e) {
            this.showToast('加载日历数据失败');
        }
    },

    // === Report ===

    async onDateClick(dateStr) {
        console.log('[DEBUG] dateClick:', dateStr);
        this.selectedDate = dateStr;
        this.currentReport = null;

        document.getElementById('editCompleted').value = '';
        document.getElementById('editInProgress').value = '';
        document.getElementById('editNotes').value = '';

        const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
        const parts = dateStr.split('-');
        const d = new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]));
        document.getElementById('modalDateTitle').textContent =
            `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 ${weekdays[d.getDay()]} 的工作日报`;

        this.setModalState('loading');
        this.showModal('reportModal');
        console.log('[DEBUG] modal shown');

        try {
            const report = await API.getReportByDate(dateStr, this.selectedRepoId || 0);
            console.log('[DEBUG] report loaded:', report);
            if (report) {
                this.currentReport = report;
                document.getElementById('editCompleted').value = report.completedTasks || '';
                document.getElementById('editInProgress').value = report.inProgressTasks || '';
                document.getElementById('editNotes').value = report.notes || '';
            } else {
                this.currentReport = null;
            }
            this.setModalState('edit');
        } catch (e) {
            console.log('[DEBUG] no report, entering edit mode:', e.message);
            this.currentReport = null;
            this.setModalState('edit');
        }
    },

    setModalState(mode) {
        console.log('[DEBUG] setModalState:', mode);
        document.getElementById('reportEditor').style.display = mode === 'edit' ? '' : 'none';
        document.getElementById('reportEmpty').style.display = 'none';
        document.getElementById('loadingSpinner').style.display = mode === 'loading' ? '' : 'none';
        document.getElementById('btnGenerateReport').style.display = (mode === 'edit' && this.selectedRepoId) ? '' : 'none';
        document.getElementById('btnSaveReport').style.display = mode === 'edit' ? '' : 'none';
        document.getElementById('btnCopyReport').style.display = mode === 'edit' ? '' : 'none';
        document.getElementById('btnDeleteReport').style.display = (mode === 'edit' && this.currentReport) ? '' : 'none';
    },

    async generateReport() {
        if (!this.selectedRepoId) {
            this.showToast('请先选择一个仓库');
            return;
        }
        this.setModalState('loading');
        try {
            const report = await API.generateReport(this.selectedDate, this.selectedRepoId);
            this.currentReport = report;
            document.getElementById('editCompleted').value = report.completedTasks || '';
            document.getElementById('editInProgress').value = report.inProgressTasks || '';
            document.getElementById('editNotes').value = report.notes || '';
            this.setModalState('edit');
            this.loadCalendarData();
            this.showToast('日报生成成功');
        } catch (e) {
            this.showToast('生成失败: ' + e.message);
            this.setModalState('edit');
        }
    },

    async saveReport() {
        const data = {
            completedTasks: document.getElementById('editCompleted').value,
            inProgressTasks: document.getElementById('editInProgress').value,
            notes: document.getElementById('editNotes').value,
        };
        try {
            let report;
            if (this.currentReport) {
                report = await API.updateReport(this.currentReport.id, data);
            } else {
                report = await API.createReport(this.selectedDate, this.selectedRepoId || 0, data);
            }
            this.currentReport = report;
            this.setModalState('edit');
            this.loadCalendarData();
            this.showToast('保存成功');
        } catch (e) {
            this.showToast('保存失败: ' + e.message);
        }
    },

    async deleteReport() {
        if (!this.currentReport) return;
        if (!confirm('确定删除该日报？')) return;
        try {
            await API.deleteReport(this.currentReport.id);
            this.currentReport = null;
            this.setModalState('edit');
            this.loadCalendarData();
            this.showToast('已删除');
        } catch (e) {
            this.showToast('删除失败: ' + e.message);
        }
    },

    copyReport() {
        const completed = document.getElementById('editCompleted').value;
        const inProgress = document.getElementById('editInProgress').value;
        const notes = document.getElementById('editNotes').value;
        const d = new Date(this.selectedDate);
        const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
        const title = `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 ${weekdays[d.getDay()]} 工作日报`;
        const text = `${title}\n\n今日完成：\n${completed || '无'}\n\n进行中：\n${inProgress || '无'}\n\n备注：\n${notes || '无'}`;

        navigator.clipboard.writeText(text).then(() => {
            this.showToast('已复制到剪贴板');
        }).catch(() => {
            this.showToast('复制失败，请手动复制');
        });
    },

    // === Settings ===

    async loadSettings() {
        try {
            const settings = await API.getSettings();
            if (settings.defaultRepositoryId) {
                this.selectedRepoId = Number(settings.defaultRepositoryId);
                document.getElementById('repoSelector').value = this.selectedRepoId;
            }
        } catch (e) {
            // 首次运行无设置，忽略
        }
    },

    async openSettings() {
        try {
            const settings = await API.getSettings();
            document.getElementById('inputDefaultRepo').value = settings.defaultRepositoryId || '';
            document.getElementById('inputAutoGenerate').checked = settings.autoGenerateEnabled === 'true';

            if (settings.autoGenerateCron) {
                const parts = settings.autoGenerateCron.split(' ');
                const h = parts[1].padStart(2, '0');
                const m = parts[0].padStart(2, '0');
                document.getElementById('inputCronTime').value = `${h}:${m}`;
            }

            document.getElementById('inputAiApiUrl').value = settings.aiApiUrl || '';
            document.getElementById('inputAiApiKey').value = settings.aiApiKey || '';
            document.getElementById('inputAiModelName').value = settings.aiModelName || '';
        } catch (e) {
            // ignore
        }
        this.showModal('settingsModal');
    },

    async saveSettings() {
        const time = document.getElementById('inputCronTime').value;
        const [h, m] = time.split(':');
        const cron = `${Number(m)} ${Number(h)} * * ?`;

        const settings = {
            autoGenerateEnabled: document.getElementById('inputAutoGenerate').checked ? 'true' : 'false',
            autoGenerateCron: cron,
            defaultRepositoryId: document.getElementById('inputDefaultRepo').value || null,
            aiApiUrl: document.getElementById('inputAiApiUrl').value.trim(),
            aiApiKey: document.getElementById('inputAiApiKey').value.trim(),
            aiModelName: document.getElementById('inputAiModelName').value.trim(),
        };
        try {
            await API.updateSettings(settings);
            this.closeModal('settingsModal');
            if (settings.defaultRepositoryId) {
                this.selectedRepoId = Number(settings.defaultRepositoryId);
                document.getElementById('repoSelector').value = this.selectedRepoId;
                this.loadCalendarData();
            }
            this.showToast('设置已保存');
        } catch (e) {
            this.showToast('保存失败: ' + e.message);
        }
    },

    // === UI Helpers ===

    toggleApiKeyVisibility() {
        const input = document.getElementById('inputAiApiKey');
        const btn = document.getElementById('btnToggleApiKey');
        const eyeOpen = '<svg viewBox="0 0 24 24"><path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/></svg>';
        const eyeClosed = '<svg viewBox="0 0 24 24"><path d="M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z"/></svg>';
        if (input.type === 'password') {
            input.type = 'text';
            btn.innerHTML = eyeClosed;
            btn.setAttribute('aria-label', '隐藏密钥');
        } else {
            input.type = 'password';
            btn.innerHTML = eyeOpen;
            btn.setAttribute('aria-label', '显示密钥');
        }
    },

    showModal(id) {
        document.getElementById(id).style.display = 'flex';
    },

    closeModal(id) {
        document.getElementById(id).style.display = 'none';
    },

    showToast(msg, duration = 2500) {
        const toast = document.getElementById('toast');
        toast.textContent = msg;
        toast.style.display = 'block';
        clearTimeout(this._toastTimer);
        this._toastTimer = setTimeout(() => {
            toast.style.display = 'none';
        }, duration);
    },
};

document.addEventListener('DOMContentLoaded', () => App.init());
