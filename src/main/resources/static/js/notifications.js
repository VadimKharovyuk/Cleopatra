// // notifications.js - подключаемый файл для любой страницы
//
// class NotificationManager {
//     constructor(userId) {
//         this.userId = userId;
//         this.socket = null;
//         this.unreadCount = 0;
//         this.init();
//     }
//
//     init() {
//         this.createNotificationContainer();
//         this.connectWebSocket();
//         this.loadUnreadCount();
//     }
//
//     createNotificationContainer() {
//         if (!document.getElementById('notification-container')) {
//             const container = document.createElement('div');
//             container.id = 'notification-container';
//             container.className = 'notification-container';
//             document.body.appendChild(container);
//         }
//     }
//
//     connectWebSocket() {
//         const url = `ws://localhost:2027/ws/notifications/websocket?userId=${this.userId}`;
//         this.socket = new WebSocket(url);
//
//         this.socket.onmessage = (event) => {
//             const data = JSON.parse(event.data);
//             if (data.type === 'new_notification') {
//                 this.showNotification(data.notification);
//             }
//         };
//     }
//
//     showNotification(notification) {
//         // Создание и показ уведомления
//         const popup = document.createElement('div');
//         popup.className = 'notification-popup';
//         popup.innerHTML = `
//             <div class="notification-icon">${this.getIcon(notification.type)}</div>
//             <div class="notification-content">
//                 <div class="notification-title">${notification.title}</div>
//                 <div class="notification-message">${notification.message}</div>
//             </div>
//         `;
//
//         document.getElementById('notification-container').appendChild(popup);
//
//         // Автоскрытие через 5 секунд
//         setTimeout(() => {
//             popup.remove();
//         }, 5000);
//
//         // Обновляем счетчик
//         this.updateBadge();
//     }
//
//     getIcon(type) {
//         const icons = {
//             'PROFILE_VISIT': '👤',
//             'POST_LIKE': '❤️',
//             'FOLLOW': '👥'
//         };
//         return icons[type] || '🔔';
//     }
//
//     updateBadge() {
//         // Обновление badge в навбаре
//         const badge = document.querySelector('.notification-badge');
//         if (badge) {
//             this.unreadCount++;
//             badge.textContent = this.unreadCount;
//             badge.style.display = 'inline-block';
//         }
//     }
//
//     loadUnreadCount() {
//         fetch('/api/notifications/unread-count')
//             .then(response => response.json())
//             .then(data => {
//                 this.unreadCount = data.unreadCount;
//                 this.updateBadge();
//             });
//     }
// }
//
// // Автоинициализация если есть userId
// document.addEventListener('DOMContentLoaded', function() {
//     const userIdElement = document.querySelector('[data-user-id]');
//     if (userIdElement) {
//         const userId = userIdElement.getAttribute('data-user-id');
//         window.notificationManager = new NotificationManager(userId);
//     }
// });

// notifications.js - подключаемый файл для любой страницы

class NotificationManager {
    constructor(userId) {
        this.userId = userId;
        this.socket = null;
        this.unreadCount = 0;
        this.init();
    }

    init() {
        this.createNotificationContainer();
        this.connectWebSocket();
        this.loadUnreadCount();
    }

    createNotificationContainer() {
        if (!document.getElementById('notification-container')) {
            const container = document.createElement('div');
            container.id = 'notification-container';
            container.className = 'notification-container';
            document.body.appendChild(container);
        }
    }

    connectWebSocket() {
        const url = `ws://localhost:2027/ws/notifications/websocket?userId=${this.userId}`;
        console.log('🔌 Connecting to notifications:', url);

        this.socket = new WebSocket(url);

        this.socket.onopen = () => {
            console.log('🔔 Connected to notifications WebSocket');
        };

        this.socket.onmessage = (event) => {
            console.log('📨 Received notification:', event.data);
            const data = JSON.parse(event.data);
            if (data.type === 'new_notification') {
                this.showNotification(data.notification);
            }
        };

        this.socket.onclose = () => {
            console.log('🔌 Notifications WebSocket closed');
            // Переподключение через 3 секунды
            setTimeout(() => this.connectWebSocket(), 3000);
        };

        this.socket.onerror = (error) => {
            console.error('❌ Notifications WebSocket error:', error);
        };
    }

    showNotification(notification) {
        // Создание и показ уведомления
        const popup = document.createElement('div');
        popup.className = 'notification-popup';
        popup.onclick = () => this.handleNotificationClick(notification);

        popup.innerHTML = `
            <div class="notification-icon">${this.getIcon(notification.type)}</div>
            <div class="notification-content">
                <div class="notification-title">${notification.title}</div>
                <div class="notification-message">${notification.message}</div>
            </div>
        `;

        document.getElementById('notification-container').appendChild(popup);

        // Автоскрытие через 5 секунд
        setTimeout(() => {
            popup.remove();
        }, 5000);

        // Обновляем счетчик
        this.updateBadge();

        // Звук уведомления
        this.playNotificationSound();
    }

    handleNotificationClick(notification) {
        // Переход по ссылке из уведомления
        if (notification.data) {
            try {
                const data = JSON.parse(notification.data);
                if (data.profileUrl) {
                    window.location.href = data.profileUrl;
                } else if (data.postUrl) {
                    window.location.href = data.postUrl;
                }
            } catch (e) {
                console.warn('Unable to parse notification data:', e);
            }
        }
    }

    getIcon(type) {
        const icons = {
            'PROFILE_VISIT': '👤',
            'POST_LIKE': '❤️',
            'POST_COMMENT': '💬',
            'FOLLOW': '👥',
            'SYSTEM_ANNOUNCEMENT': '📢'
        };
        return icons[type] || '🔔';
    }

    updateBadge() {
        // Обновление badge в навбаре
        const badge = document.getElementById('notifications-badge'); // 🔧 Используем ваш ID
        if (badge) {
            if (this.unreadCount > 0) {
                badge.textContent = this.unreadCount > 99 ? '99+' : this.unreadCount;
                badge.style.display = 'flex'; // 🔧 flex вместо inline-block
            } else {
                badge.style.display = 'none';
            }
        }
    }

    loadUnreadCount() {
        fetch('/api/notifications/unread-count')
            .then(response => response.json())
            .then(data => {
                this.unreadCount = data.unreadCount;
                this.updateBadge();
            })
            .catch(error => {
                console.error('❌ Error loading unread count:', error);
            });
    }

    playNotificationSound() {
        // Простой звук уведомления
        try {
            const audioContext = new (window.AudioContext || window.webkitAudioContext)();
            const oscillator = audioContext.createOscillator();
            const gainNode = audioContext.createGain();

            oscillator.connect(gainNode);
            gainNode.connect(audioContext.destination);

            oscillator.frequency.setValueAtTime(800, audioContext.currentTime);
            gainNode.gain.setValueAtTime(0.1, audioContext.currentTime);
            gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.3);

            oscillator.start(audioContext.currentTime);
            oscillator.stop(audioContext.currentTime + 0.3);
        } catch (e) {
            // Ignore audio errors
        }
    }
}

// Автоинициализация если есть userId
document.addEventListener('DOMContentLoaded', function() {
    const userIdElement = document.querySelector('[data-user-id]');
    if (userIdElement) {
        const userId = userIdElement.getAttribute('data-user-id');
        console.log('🚀 Initializing notifications for user:', userId);
        window.notificationManager = new NotificationManager(userId);
    }
});