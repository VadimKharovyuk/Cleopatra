// wall-main.js - Основной файл инициализации стены

/**
 * Основной класс для управления стеной пользователя
 */
class WallManager {
    constructor() {
        // Переменные из Thymeleaf (должны быть определены в HTML)
        this.wallOwnerId = window.wallOwnerId || 0;
        this.currentUserId = window.currentUserId || null;
        this.canWriteOnWall = window.canWriteOnWall || false;

        // Инициализация компонентов
        this.posts = null;
        this.interactions = null;
        this.form = null;

        this.init();
    }

    /**
     * Инициализирует все компоненты стены
     */
    init() {
        // Проверяем, что все необходимые данные загружены
        if (!this.wallOwnerId) {
            console.error('wallOwnerId не определен');
            return;
        }

        // Инициализируем компоненты
        this.initComponents();

        // Запускаем загрузку данных
        this.loadInitialData();

        console.log('Wall Manager инициализирован', {
            wallOwnerId: this.wallOwnerId,
            currentUserId: this.currentUserId,
            canWriteOnWall: this.canWriteOnWall
        });
    }

    /**
     * Инициализирует все компоненты
     */
    initComponents() {
        console.log('Инициализация компонентов...');

        // Инициализируем взаимодействия
        this.interactions = new WallInteractions();
        console.log('WallInteractions инициализирован');

        // Инициализируем управление постами
        this.posts = new WallPosts(this.wallOwnerId, this.currentUserId);
        console.log('WallPosts инициализирован');

        // Инициализируем форму создания постов (если пользователь может писать на стене)
        if (this.canWriteOnWall) {
            this.form = new WallForm(this.wallOwnerId, (newPost) => {
                this.posts.addNewPost(newPost);
            });
            console.log('WallForm инициализирован');
        }

        // Делаем компоненты доступными глобально для обратной совместимости
        window.wallManager = this;
        window.wallPosts = this.posts;
        window.wallInteractions = this.interactions;
        window.wallForm = this.form;

        console.log('Все компоненты инициализированы и доступны глобально');
    }

    /**
     * Загружает начальные данные
     */
    async loadInitialData() {
        try {
            // Настраиваем бесконечный скролл
            this.posts.setupInfiniteScroll();

            // Загружаем первую порцию постов
            await this.posts.loadWallPosts();

        } catch (error) {
            console.error('Ошибка загрузки начальных данных:', error);
            showNotification('Ошибка загрузки данных стены', 'error');
        }
    }

    /**
     * Обновляет всю стену
     */
    async refresh() {
        try {
            // Сбрасываем состояние постов
            this.posts.currentPage = 0;
            this.posts.hasMorePosts = true;
            this.posts.totalPostsCount = 0;

            // Очищаем контейнер постов
            this.posts.postsContainer.innerHTML = '';

            // Скрываем состояния
            this.posts.emptyState.style.display = 'none';
            this.posts.endMessage.style.display = 'none';

            // Загружаем посты заново
            await this.posts.loadWallPosts();

            showNotification('Стена обновлена', 'success');

        } catch (error) {
            console.error('Ошибка обновления стены:', error);
            showNotification('Ошибка обновления стены', 'error');
        }
    }

    /**
     * Возвращает информацию о текущем состоянии стены
     */
    getState() {
        return {
            wallOwnerId: this.wallOwnerId,
            currentUserId: this.currentUserId,
            canWriteOnWall: this.canWriteOnWall,
            totalPosts: this.posts ? this.posts.totalPostsCount : 0,
            currentPage: this.posts ? this.posts.currentPage : 0,
            hasMorePosts: this.posts ? this.posts.hasMorePosts : true,
            isLoading: this.posts ? this.posts.isLoading : false
        };
    }

    /**
     * Уничтожает все компоненты (cleanup)
     */
    destroy() {
        // Удаляем обработчики событий
        if (this.posts) {
            window.removeEventListener('scroll', this.posts.scrollHandler);
        }

        // Очищаем глобальные ссылки
        window.wallManager = null;
        window.wallPosts = null;
        window.wallInteractions = null;
        window.wallForm = null;

        console.log('Wall Manager уничтожен');
    }
}

// Глобальные переменные для обратной совместимости
let wallManager = null;
let wallPosts = null;
let wallInteractions = null;
let wallForm = null;

// Определяем глобальные функции сразу (заглушки до инициализации)
window.deletePost = function(postId) {
    console.log('deletePost вызвана для поста:', postId);
    if (window.wallPosts) {
        window.wallPosts.deletePost(postId);
    } else {
        console.error('wallPosts еще не инициализирован, ожидаем...');
        // Попробуем найти через wallManager
        if (window.wallManager && window.wallManager.posts) {
            window.wallManager.posts.deletePost(postId);
        }
    }
};

window.toggleLike = function(postId) {
    console.log('toggleLike вызвана для поста:', postId);
    if (window.wallInteractions) {
        window.wallInteractions.toggleLike(postId);
    } else {
        console.error('wallInteractions еще не инициализирован');
        if (window.wallManager && window.wallManager.interactions) {
            window.wallManager.interactions.toggleLike(postId);
        }
    }
};

window.showComments = function(postId) {
    console.log('showComments вызвана для поста:', postId);
    if (window.wallInteractions) {
        window.wallInteractions.showComments(postId);
    } else {
        console.error('wallInteractions еще не инициализирован');
        if (window.wallManager && window.wallManager.interactions) {
            window.wallManager.interactions.showComments(postId);
        }
    }
};

window.previewFile = function(input) {
    console.log('previewFile вызвана');
    if (window.wallForm) {
        window.wallForm.previewFile(input);
    } else {
        console.error('wallForm еще не инициализирован');
        if (window.wallManager && window.wallManager.form) {
            window.wallManager.form.previewFile(input);
        }
    }
};

window.removeFile = function() {
    console.log('removeFile вызвана');
    if (window.wallForm) {
        window.wallForm.removeFile();
    } else {
        console.error('wallForm еще не инициализирован');
        if (window.wallManager && window.wallManager.form) {
            window.wallManager.form.removeFile();
        }
    }
};

// Инициализация при загрузке DOM
document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM загружен, начинаем инициализацию...');
    // Проверяем, что мы на странице стены
    if (typeof window.wallOwnerId !== 'undefined') {
        console.log('wallOwnerId найден:', window.wallOwnerId);
        wallManager = new WallManager();
    } else {
        console.error('wallOwnerId не определен, инициализация пропущена');
    }
});

// Глобальные функции для обратной совместимости с существующим HTML (дублируем для надежности)
window.deletePost = function(postId) {
    console.log('deletePost вызвана для поста:', postId);
    if (wallPosts) {
        wallPosts.deletePost(postId);
    } else {
        console.error('wallPosts не инициализирован');
    }
};

window.toggleLike = function(postId) {
    console.log('toggleLike вызвана для поста:', postId);
    if (wallInteractions) {
        wallInteractions.toggleLike(postId);
    } else {
        console.error('wallInteractions не инициализирован');
    }
};

window.showComments = function(postId) {
    console.log('showComments вызвана для поста:', postId);
    if (wallInteractions) {
        wallInteractions.showComments(postId);
    } else {
        console.error('wallInteractions не инициализирован');
    }
};

window.previewFile = function(input) {
    console.log('previewFile вызвана');
    if (wallForm) {
        wallForm.previewFile(input);
    } else {
        console.error('wallForm не инициализирован');
    }
};

window.removeFile = function() {
    console.log('removeFile вызвана');
    if (wallForm) {
        wallForm.removeFile();
    } else {
        console.error('wallForm не инициализирован');
    }
};

// Экспорт для возможного использования как модуль
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        WallManager,
        WallPosts,
        WallInteractions,
        WallForm
    };
}