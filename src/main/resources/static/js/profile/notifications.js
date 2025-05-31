// notifications.js - Модуль для работы с уведомлениями

class NotificationManager {
    constructor() {
        this.initStyles();
    }

    // Универсальная функция для показа уведомлений
    show(message, type = 'info') {
        const notification = document.createElement('div');
        notification.className = `alert-luxury alert-${type === 'success' ? 'success' : 'danger'}-luxury`;
        notification.style.position = 'fixed';
        notification.style.top = '20px';
        notification.style.right = '20px';
        notification.style.zIndex = '9999';
        notification.style.minWidth = '300px';
        notification.style.animation = 'slideInRight 0.3s ease';

        notification.innerHTML = `
            <i class="fas fa-${this.getIcon(type)}"></i>
            <span>${message}</span>
            <button onclick="this.parentElement.remove()" style="background: none; border: none; color: inherit; margin-left: auto; cursor: pointer;">
                <i class="fas fa-times"></i>
            </button>
        `;

        document.body.appendChild(notification);

        // Автоматически убираем через 5 секунд
        setTimeout(() => {
            if (notification.parentElement) {
                notification.style.animation = 'slideOutRight 0.3s ease';
                setTimeout(() => notification.remove(), 300);
            }
        }, 5000);
    }

    getIcon(type) {
        switch (type) {
            case 'success': return 'check-circle';
            case 'info': return 'info-circle';
            case 'error': return 'exclamation-circle';
            default: return 'info-circle';
        }
    }

    // Инициализация CSS анимаций
    initStyles() {
        if (document.getElementById('notification-styles')) return;

        const style = document.createElement('style');
        style.id = 'notification-styles';
        style.textContent = `
            @keyframes slideInRight {
                from { transform: translateX(100%); opacity: 0; }
                to { transform: translateX(0); opacity: 1; }
            }

            @keyframes slideOutRight {
                from { transform: translateX(0); opacity: 1; }
                to { transform: translateX(100%); opacity: 0; }
            }
        `;
        document.head.appendChild(style);
    }
}

// Создаем глобальный экземпляр
window.notificationManager = new NotificationManager();

// Для обратной совместимости
window.showNotification = (message, type) => window.notificationManager.show(message, type);