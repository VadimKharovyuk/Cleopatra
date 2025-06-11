// static/js/modules/notifications.js

class NotificationManager {
    static show(message, type = 'success') {
        console.log('🔔 Уведомление:', type, message);

        // Удаляем существующие уведомления
        const existingNotifications = document.querySelectorAll('.notification');
        existingNotifications.forEach(n => n.remove());

        // Создаем новое уведомление
        const notification = document.createElement('div');
        notification.className = `notification ${type}`;
        notification.innerHTML = `
            <i class="fas ${type === 'success' ? 'fa-check-circle' : 'fa-exclamation-circle'}" 
               style="margin-right: 0.5rem;"></i>
            ${message}
        `;

        document.body.appendChild(notification);

        // Показываем уведомление
        setTimeout(() => {
            notification.classList.add('show');
        }, 100);

        // Скрываем через 3 секунды
        setTimeout(() => {
            notification.classList.remove('show');
            setTimeout(() => {
                if (notification.parentNode) {
                    notification.parentNode.removeChild(notification);
                }
            }, 300);
        }, 3000);
    }
}

// Экспортируем для использования в других модулях
window.NotificationManager = NotificationManager;