// static/js/comments-utils.js

/**
 * Утилиты для работы с комментариями
 */
class CommentsUtils {

    /**
     * Валидация содержимого комментария
     */
    static validateCommentContent(content) {
        const errors = [];

        if (!content || content.trim().length === 0) {
            errors.push('Комментарий не может быть пустым');
        }

        if (content.length > 500) {
            errors.push('Комментарий не может содержать более 500 символов');
        }

        if (content.trim().length < 2) {
            errors.push('Комментарий должен содержать минимум 2 символа');
        }

        // Проверка на спам (простая)
        if (this.isSpamContent(content)) {
            errors.push('Содержимое похоже на спам');
        }

        return {
            isValid: errors.length === 0,
            errors: errors
        };
    }

    /**
     * Простая проверка на спам
     */
    static isSpamContent(content) {
        const spamPatterns = [
            /(.)\1{10,}/i, // Повторение одного символа более 10 раз
            /(https?:\/\/[^\s]+.*){3,}/i, // Более 3 ссылок
            /^\s*[A-Z\s!]{20,}\s*$/i, // Много заглавных букв подряд
        ];

        return spamPatterns.some(pattern => pattern.test(content));
    }

    /**
     * Анимация появления элемента
     */
    static animateIn(element, animationType = 'slideIn') {
        return new Promise((resolve) => {
            element.style.opacity = '0';
            element.style.transform = this.getInitialTransform(animationType);
            element.style.transition = 'all 0.3s ease';

            // Небольшая задержка для применения стилей
            setTimeout(() => {
                element.style.opacity = '1';
                element.style.transform = 'translateY(0) scale(1)';

                setTimeout(() => {
                    element.style.transition = '';
                    resolve();
                }, 300);
            }, 10);
        });
    }

    /**
     * Анимация исчезновения элемента
     */
    static animateOut(element, animationType = 'slideOut') {
        return new Promise((resolve) => {
            element.style.transition = 'all 0.3s ease';
            element.style.opacity = '0';
            element.style.transform = this.getFinalTransform(animationType);

            setTimeout(() => {
                resolve();
            }, 300);
        });
    }

    /**
     * Получить начальную трансформацию для анимации
     */
    static getInitialTransform(animationType) {
        switch (animationType) {
            case 'slideIn':
                return 'translateY(20px) scale(0.95)';
            case 'fadeIn':
                return 'scale(0.9)';
            case 'slideInLeft':
                return 'translateX(-100%)';
            case 'slideInRight':
                return 'translateX(100%)';
            default:
                return 'translateY(20px)';
        }
    }

    /**
     * Получить финальную трансформацию для анимации
     */
    static getFinalTransform(animationType) {
        switch (animationType) {
            case 'slideOut':
                return 'translateY(-20px) scale(0.95)';
            case 'fadeOut':
                return 'scale(0.9)';
            case 'slideOutLeft':
                return 'translateX(-100%)';
            case 'slideOutRight':
                return 'translateX(100%)';
            default:
                return 'translateY(-20px)';
        }
    }

    /**
     * Обновление счетчика символов с цветовой индикацией
     */
    static updateCharCounterWithColors(counter, currentLength, maxLength) {
        counter.textContent = currentLength;

        // Удаляем предыдущие классы
        counter.classList.remove('text-success', 'text-warning', 'text-danger');

        const percentage = (currentLength / maxLength) * 100;

        if (percentage < 70) {
            counter.classList.add('text-success');
        } else if (percentage < 90) {
            counter.classList.add('text-warning');
        } else {
            counter.classList.add('text-danger');
        }

        // Анимация при достижении лимита
        if (currentLength >= maxLength) {
            counter.style.animation = 'shake 0.5s ease-in-out';
            setTimeout(() => {
                counter.style.animation = '';
            }, 500);
        }
    }

    /**
     * Дебаунс функция для оптимизации частых вызовов
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
     * Проверка видимости элемента в viewport
     */
    static isElementInViewport(element) {
        const rect = element.getBoundingClientRect();
        return (
            rect.top >= 0 &&
            rect.left >= 0 &&
            rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) &&
            rect.right <= (window.innerWidth || document.documentElement.clientWidth)
        );
    }

    /**
     * Плавная прокрутка к элементу
     */
    static scrollToElement(element, offset = 0) {
        const elementPosition = element.getBoundingClientRect().top + window.pageYOffset;
        const offsetPosition = elementPosition - offset;

        window.scrollTo({
            top: offsetPosition,
            behavior: 'smooth'
        });
    }

    /**
     * Создание уникального ID для элементов
     */
    static generateUniqueId(prefix = 'comment') {
        return `${prefix}-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
    }

    /**
     * Форматирование текста для отображения (обработка ссылок, @упоминаний)
     */
    static formatCommentText(text) {
        // Замена URL на ссылки
        text = text.replace(
            /(https?:\/\/[^\s]+)/g,
            '<a href="$1" target="_blank" rel="noopener noreferrer">$1</a>'
        );

        // Замена @упоминаний (простая версия)
        text = text.replace(
            /@(\w+)/g,
            '<span class="mention">@$1</span>'
        );

        // Замена переносов строк на <br>
        text = text.replace(/\n/g, '<br>');

        return text;
    }

    /**
     * Очистка HTML для безопасности
     */
    static sanitizeHtml(text) {
        const map = {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#039;',
            '/': '&#x2F;'
        };

        return text.replace(/[&<>"'/]/g, (s) => map[s]);
    }

    /**
     * Сохранение черновика комментария в localStorage
     */
    static saveDraft(postId, content) {
        if (typeof Storage !== 'undefined') {
            localStorage.setItem(`comment-draft-${postId}`, content);
        }
    }

    /**
     * Загрузка черновика комментария из localStorage
     */
    static loadDraft(postId) {
        if (typeof Storage !== 'undefined') {
            return localStorage.getItem(`comment-draft-${postId}`) || '';
        }
        return '';
    }

    /**
     * Удаление черновика комментария
     */
    static clearDraft(postId) {
        if (typeof Storage !== 'undefined') {
            localStorage.removeItem(`comment-draft-${postId}`);
        }
    }

    /**
     * Проверка поддержки уведомлений браузером
     */
    static checkNotificationSupport() {
        return 'Notification' in window;
    }

    /**
     * Запрос разрешения на уведомления
     */
    static async requestNotificationPermission() {
        if (this.checkNotificationSupport()) {
            return await Notification.requestPermission();
        }
        return 'denied';
    }

    /**
     * Показ уведомления
     */
    static showNotification(title, options = {}) {
        if (this.checkNotificationSupport() && Notification.permission === 'granted') {
            return new Notification(title, {
                icon: '/images/notification-icon.png',
                badge: '/images/notification-badge.png',
                ...options
            });
        }
        return null;
    }

    /**
     * Копирование ссылки на комментарий в буфер обмена
     */
    static async copyCommentLink(postId, commentId) {
        const link = `${window.location.origin}/posts/${postId}/comments#comment-${commentId}`;

        try {
            await navigator.clipboard.writeText(link);
            return true;
        } catch (err) {
            // Fallback для старых браузеров
            const textArea = document.createElement('textarea');
            textArea.value = link;
            document.body.appendChild(textArea);
            textArea.select();
            try {
                document.execCommand('copy');
                document.body.removeChild(textArea);
                return true;
            } catch (fallbackErr) {
                document.body.removeChild(textArea);
                return false;
            }
        }
    }

    /**
     * Проверка доступности сети
     */
    static isOnline() {
        return navigator.onLine;
    }

    /**
     * Отслеживание изменений состояния сети
     */
    static onNetworkChange(callback) {
        window.addEventListener('online', () => callback(true));
        window.addEventListener('offline', () => callback(false));
    }
}

// CSS анимации (добавить в CSS файл)
const additionalCSS = `
@keyframes shake {
    0%, 100% { transform: translateX(0); }
    25% { transform: translateX(-5px); }
    75% { transform: translateX(5px); }
}

.mention {
    color: #007bff;
    font-weight: 500;
    text-decoration: none;
}

.mention:hover {
    text-decoration: underline;
}

.char-counter {
    transition: color 0.3s ease;
    font-weight: 500;
}

.offline-indicator {
    position: fixed;
    top: 10px;
    right: 10px;
    background: #dc3545;
    color: white;
    padding: 8px 12px;
    border-radius: 4px;
    font-size: 0.875rem;
    z-index: 9999;
    display: none;
}

.offline-indicator.show {
    display: block;
    animation: slideInDown 0.3s ease;
}

@keyframes slideInDown {
    from {
        transform: translateY(-100%);
        opacity: 0;
    }
    to {
        transform: translateY(0);
        opacity: 1;
    }
}
`;

// Добавление CSS стилей
if (typeof document !== 'undefined') {
    const style = document.createElement('style');
    style.textContent = additionalCSS;
    document.head.appendChild(style);
}

// Экспорт для использования как модуль
if (typeof module !== 'undefined' && module.exports) {
    module.exports = CommentsUtils;
}