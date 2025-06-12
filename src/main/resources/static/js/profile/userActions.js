// // userActions.js - Модуль для действий с пользователями
//
// class UserActions {
//     constructor() {
//         this.userId = /*[[${user.id}]]*/ 0;
//         this.userName = /*[[${user.firstName ?: 'Пользователь'}]]*/ 'Пользователь';
//         this.currentUserId = /*[[${currentUserId}]]*/ null;
//         this.isOwnProfile = /*[[${currentUserId == user.id}]]*/ false;
//     }
//
//     // Отправка сообщения
//     sendMessage() {
//         if (confirm(`Отправить сообщение пользователю ${this.userName}?`)) {
//             window.location.href = `/messages/new?to=${this.userId}`;
//         }
//     }
//
//     // Подписка/отписка
//     async toggleFollow(event) {
//         const button = event.target.closest('a');
//         const isFollowing = button.querySelector('span').textContent.trim() === 'Отписаться';
//         const action = isFollowing ? 'unfollow' : 'follow';
//         const confirmText = isFollowing
//             ? `Отписаться от ${this.userName}?`
//             : `Подписаться на ${this.userName}?`;
//
//         if (!confirm(confirmText)) return;
//
//         try {
//             const data = isFollowing
//                 ? await window.apiClient.unfollowUser(this.userId)
//                 : await window.apiClient.followUser(this.userId);
//
//             if (data.success) {
//                 this.updateFollowButton(button, !isFollowing);
//                 window.notificationManager.show(data.message || 'Действие выполнено успешно', 'success');
//             } else {
//                 window.notificationManager.show(data.message || 'Произошла ошибка', 'error');
//             }
//         } catch (error) {
//             console.error('Ошибка при выполнении действия:', error);
//             window.notificationManager.show('Произошла ошибка при выполнении действия', 'error');
//         }
//     }
//
//     // Обновление кнопки подписки
//     updateFollowButton(button, isFollowing) {
//         const icon = button.querySelector('i');
//         const span = button.querySelector('span');
//
//         if (isFollowing) {
//             // Стали подписанными
//             icon.className = 'fas fa-user-check';
//             span.textContent = 'Отписаться';
//             button.className = button.className.replace('btn-primary-luxury', 'btn-danger-luxury');
//         } else {
//             // Стали НЕ подписанными
//             icon.className = 'fas fa-user-plus';
//             span.textContent = 'Подписаться';
//             button.className = button.className.replace('btn-danger-luxury', 'btn-primary-luxury');
//         }
//     }
//
//     // Блокировка пользователя
//     async blockUser() {
//         const confirmText = `Заблокировать ${this.userName}? Вы больше не будете видеть их посты и они не смогут с вами взаимодействовать.`;
//
//         if (!confirm(confirmText)) return;
//
//         try {
//             const data = await window.apiClient.blockUser(this.userId);
//
//             if (data.success) {
//                 window.notificationManager.show('Пользователь заблокирован', 'success');
//                 setTimeout(() => {
//                     window.location.href = '/home';
//                 }, 2000);
//             } else {
//                 window.notificationManager.show(data.message || 'Не удалось заблокировать пользователя', 'error');
//             }
//         } catch (error) {
//             console.error('Ошибка при блокировке:', error);
//             window.notificationManager.show('Произошла ошибка при блокировке', 'error');
//         }
//     }
// }
//
// // Создаем глобальный экземпляр
// window.userActions = new UserActions();
//
// // Экспортируем функции для обратной совместимости
// window.sendMessage = () => window.userActions.sendMessage();
// window.toggleFollow = (event) => window.userActions.toggleFollow(event);
// window.blockUser = () => window.userActions.blockUser();


// ===== 3. ИСПРАВЛЕНИЕ userActions.js =====
class UserActions {
    static async followUser(userId) {
        try {
            const result = await ApiClient.post(`/api/users/${userId}/follow`);
            NotificationManager.show('Подписка оформлена!', 'success');
            return result;
        } catch (error) {
            NotificationManager.show('Ошибка при подписке', 'error');
            throw error;
        }
    }

    static async unfollowUser(userId) {
        try {
            const result = await ApiClient.delete(`/api/users/${userId}/follow`);
            NotificationManager.show('Подписка отменена', 'success');
            return result;
        } catch (error) {
            NotificationManager.show('Ошибка при отписке', 'error');
            throw error;
        }
    }

    static async blockUser(userId) {
        try {
            const result = await ApiClient.post(`/api/users/${userId}/block`);
            NotificationManager.show('Пользователь заблокирован', 'success');
            return result;
        } catch (error) {
            NotificationManager.show('Ошибка при блокировке', 'error');
            throw error;
        }
    }

    static async unblockUser(userId) {
        try {
            const result = await ApiClient.delete(`/api/users/${userId}/block`);
            NotificationManager.show('Пользователь разблокирован', 'success');
            return result;
        } catch (error) {
            NotificationManager.show('Ошибка при разблокировке', 'error');
            throw error;
        }
    }

    static init() {
        console.log('✅ UserActions инициализирован');
    }
}

// ✅ ОБЯЗАТЕЛЬНЫЙ ЭКСПОРТ
window.UserActions = UserActions;