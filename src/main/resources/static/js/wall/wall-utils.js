/**
 * wall-utils.js
 * Утилиты для работы со стеной
 */

class WallUtils {

    /**
     * Форматирование даты в читаемый формат
     */
    static formatDate(dateString) {
        const date = new Date(dateString);
        const now = new Date();
        const diffMs = now - date;
        const diffMins = Math.floor(diffMs / 60000);
        const diffHours = Math.floor(diffMs / 3600000);
        const diffDays = Math.floor(diffMs / 86400000);

        if (diffMins < 1) return 'только что';
        if (diffMins < 60) return `${diffMins} мин назад`;
        if (diffHours < 24) return `${diffHours} ч назад`;
        if (diffDays < 7) return `${diffDays} дн назад`;

        return date.toLocaleDateString('ru-RU', {
            day: 'numeric',
            month: 'short'
        });
    }

    /**
     * Экранирование HTML для безопасности
     */
    static escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    /**
     * Показать уведомление
     */
    static showNotification(message, type = 'info') {
        const notification = document.createElement('div');
        notification.className = `notification-toast ${type}`;
        notification.textContent = message;

        document.body.appendChild(notification);

        setTimeout(() => {
            notification.style.transform = 'translateX(100%)';
            notification.style.opacity = '0';
            setTimeout(() => notification.remove(), 300);
        }, 3000);
    }

    /**
     * Показать индикатор загрузки
     */
    static showLoading() {
        const loadingIndicator = document.getElementById('loadingIndicator');
        if (loadingIndicator) {
            loadingIndicator.style.display = 'block';
        }
    }

    /**
     * Скрыть индикатор загрузки
     */
    static hideLoading() {
        const loadingIndicator = document.getElementById('loadingIndicator');
        if (loadingIndicator) {
            loadingIndicator.style.display = 'none';
        }
    }

    /**
     * Показать empty state
     */
    static showEmptyState() {
        const emptyState = document.getElementById('emptyState');
        if (emptyState) {
            emptyState.style.display = 'block';
        }
    }

    /**
     * Скрыть empty state
     */
    static hideEmptyState() {
        const emptyState = document.getElementById('emptyState');
        if (emptyState) {
            emptyState.style.display = 'none';
        }
    }

    /**
     * Показать сообщение об окончании постов
     */
    static showEndMessage() {
        const endMessage = document.getElementById('endMessage');
        if (endMessage) {
            endMessage.style.display = 'block';
        }
    }

    /**
     * Обновить счетчик постов
     */
    static updatePostsCount(count) {
        const wallPostsCountElement = document.getElementById('wallPostsCount');
        if (wallPostsCountElement) {
            wallPostsCountElement.textContent = count;
        }
    }

    /**
     * Получить количество постов в контейнере
     */
    static getPostsCount() {
        const postsContainer = document.getElementById('postsContainer');
        return postsContainer ? postsContainer.children.length : 0;
    }

    /**
     * Проверить, пустой ли контейнер постов
     */
    static isPostsContainerEmpty() {
        return this.getPostsCount() === 0;
    }

    /**
     * Создать подтверждающий диалог
     */
    static confirm(message) {
        return window.confirm(message);
    }

    /**
     * Логирование ошибок
     */
    static logError(context, error) {
        console.error(`[Wall ${context}]:`, error);
    }

    /**
     * Валидация файла изображения
     */
    static validateImageFile(file) {
        const maxSize = 10 * 1024 * 1024; // 10MB
        const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif'];

        if (!file) {
            return { valid: false, message: 'Файл не выбран' };
        }

        if (file.size > maxSize) {
            return { valid: false, message: 'Размер файла не должен превышать 10MB' };
        }

        if (!allowedTypes.includes(file.type)) {
            return { valid: false, message: 'Поддерживаются только JPG, PNG и GIF' };
        }

        return { valid: true };
    }

    /**
     * Дебаунс функция
     */
    static debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }

    /**
     * Троттлинг функция
     */
    static throttle(func, limit) {
        let inThrottle;
        return function() {
            const args = arguments;
            const context = this;
            if (!inThrottle) {
                func.apply(context, args);
                inThrottle = true;
                setTimeout(() => inThrottle = false, limit);
            }
        };
    }
}