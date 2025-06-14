/**
 * wall-main.js
 * Главный класс приложения стены
 */

class WallApp {

    constructor(config) {
        this.config = {
            wallOwnerId: config.wallOwnerId,
            currentUserId: config.currentUserId,
            canWriteOnWall: config.canWriteOnWall,
            pageSize: config.pageSize || 10
        };

        // Экземпляры модулей
        this.wallPosts = null;
        this.wallInteractions = null;
        this.wallForm = null;

        // Состояние приложения
        this.isInitialized = false;
    }

    /**
     * Инициализация приложения
     */
    async init() {
        try {
            // Проверяем готовность DOM
            if (document.readyState === 'loading') {
                await this.waitForDOMReady();
            }

            // Инициализируем модули
            this.initializeModules();

            // Настраиваем глобальные обработчики
            this.setupGlobalHandlers();

            // Помечаем как инициализированное
            this.isInitialized = true;

            console.log('🏛️ Wall App initialized successfully');

        } catch (error) {
            WallUtils.logError('init', error);
            WallUtils.showNotification('Ошибка инициализации приложения', 'error');
        }
    }

    /**
     * Ожидание готовности DOM
     */
    waitForDOMReady() {
        return new Promise((resolve) => {
            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', resolve);
            } else {
                resolve();
            }
        });
    }

    /**
     * Инициализация всех модулей
     */
    initializeModules() {
        // Инициализируем WallPosts
        this.wallPosts = new WallPosts(this.config);

        // Инициализируем WallInteractions с ссылкой на WallPosts
        this.wallInteractions = new WallInteractions({
            ...this.config,
            wallPosts: this.wallPosts
        });

        // Инициализируем WallForm с ссылкой на WallPosts
        this.wallForm = new WallForm({
            ...this.config,
            wallPosts: this.wallPosts
        });

        // Запускаем инициализацию модулей
        this.wallPosts.init();
        this.wallForm.init();

        // Делаем экземпляры доступными глобально для onclick обработчиков
        window.wallInteractions = this.wallInteractions;
        window.wallForm = this.wallForm;
        window.wallPosts = this.wallPosts;
    }

    /**
     * Настройка глобальных обработчиков
     */
    setupGlobalHandlers() {
        // Обработчик ошибок
        window.addEventListener('error', (event) => {
            WallUtils.logError('Global', event.error);
        });

        // Обработчик необработанных промисов
        window.addEventListener('unhandledrejection', (event) => {
            WallUtils.logError('Unhandled Promise', event.reason);
            event.preventDefault();
        });

        // Обработчик изменения размера окна
        window.addEventListener('resize', WallUtils.debounce(() => {
            this.handleWindowResize();
        }, 250));

        // Обработчик фокуса/потери фокуса окна
        document.addEventListener('visibilitychange', () => {
            this.handleVisibilityChange();
        });
    }

    /**
     * Обработка изменения размера окна
     */
    handleWindowResize() {
        // Здесь можно добавить логику для адаптации под новый размер
        console.log('Window resized');
    }

    /**
     * Обработка изменения видимости страницы
     */
    handleVisibilityChange() {
        if (document.hidden) {
            // Страница скрыта
            console.log('Page hidden');
        } else {
            // Страница видима
            console.log('Page visible');
            // Можно обновить данные, если нужно
        }
    }

    /**
     * Получить экземпляр WallPosts
     */
    getPosts() {
        return this.wallPosts;
    }

    /**
     * Получить экземпляр WallInteractions
     */
    getInteractions() {
        return this.wallInteractions;
    }

    /**
     * Получить экземпляр WallForm
     */
    getForm() {
        return this.wallForm;
    }

    /**
     * Получить конфигурацию
     */
    getConfig() {
        return { ...this.config };
    }

    /**
     * Проверить, инициализировано ли приложение
     */
    isReady() {
        return this.isInitialized;
    }

    /**
     * Перезагрузить посты
     */
    async reloadPosts() {
        if (this.wallPosts) {
            // Сбрасываем состояние пагинации
            this.wallPosts.currentPage = 0;
            this.wallPosts.hasMorePosts = true;
            this.wallPosts.totalPostsCount = 0;

            // Очищаем контейнер
            const postsContainer = document.getElementById('postsContainer');
            if (postsContainer) {
                postsContainer.innerHTML = '';
            }

            // Скрываем все состояния
            WallUtils.hideEmptyState();
            const endMessage = document.getElementById('endMessage');
            if (endMessage) {
                endMessage.style.display = 'none';
            }

            // Загружаем посты заново
            await this.wallPosts.loadWallPosts();
        }
    }

    /**
     * Обновить настройки приложения
     */
    updateConfig(newConfig) {
        this.config = { ...this.config, ...newConfig };

        // Обновляем конфигурацию в модулях
        if (this.wallPosts) {
            Object.assign(this.wallPosts, newConfig);
        }
        if (this.wallInteractions) {
            Object.assign(this.wallInteractions, newConfig);
        }
        if (this.wallForm) {
            Object.assign(this.wallForm, newConfig);
        }
    }

    /**
     * Уничтожить приложение (cleanup)
     */
    destroy() {
        // Удаляем глобальные ссылки
        delete window.wallInteractions;
        delete window.wallForm;
        delete window.wallPosts;

        // Вызываем cleanup методы модулей если они есть
        if (this.wallForm && typeof this.wallForm.destroy === 'function') {
            this.wallForm.destroy();
        }

        // Сбрасываем состояние
        this.isInitialized = false;
        this.wallPosts = null;
        this.wallInteractions = null;
        this.wallForm = null;

        console.log('🏛️ Wall App destroyed');
    }

    /**
     * Показать информацию о приложении
     */
    getAppInfo() {
        return {
            version: '1.0.0',
            initialized: this.isInitialized,
            config: this.config,
            modules: {
                posts: !!this.wallPosts,
                interactions: !!this.wallInteractions,
                form: !!this.wallForm
            }
        };
    }

    /**
     * Методы для отладки
     */
    debug() {
        return {
            app: this,
            config: this.config,
            posts: this.wallPosts,
            interactions: this.wallInteractions,
            form: this.wallForm,
            utils: WallUtils
        };
    }
}

// Экспортируем для глобального использования
window.WallApp = WallApp;