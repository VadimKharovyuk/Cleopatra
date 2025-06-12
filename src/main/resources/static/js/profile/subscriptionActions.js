// // Функция для показа красивых уведомлений
// function showNotification(message, type = 'success') {
//     const alertClass = type === 'success' ? 'alert-success-luxury' : 'alert-danger-luxury';
//     const iconClass = type === 'success' ? 'fa-check-circle' : 'fa-exclamation-circle';
//
//     const alert = document.createElement('div');
//     alert.className = `alert-luxury ${alertClass}`;
//     alert.style.opacity = '0';
//     alert.style.transform = 'translateY(-20px)';
//     alert.style.transition = 'all 0.3s ease';
//     alert.innerHTML = `
//         <i class="fas ${iconClass} me-2"></i>
//         <span>${message}</span>
//     `;
//
//
//
//     const mainContent = document.querySelector('.main-content');
//     const profileContainer = document.querySelector('.profile-container');
//
//     if (mainContent && profileContainer) {
//         mainContent.insertBefore(alert, profileContainer);
//
//         // Анимация появления
//         setTimeout(() => {
//             alert.style.opacity = '1';
//             alert.style.transform = 'translateY(0)';
//         }, 10);
//
//         // Автоматическое скрытие через 3 секунды
//         setTimeout(() => {
//             alert.style.opacity = '0';
//             alert.style.transform = 'translateY(-20px)';
//             setTimeout(() => {
//                 if (alert.parentNode) {
//                     alert.parentNode.removeChild(alert);
//                 }
//             }, 300);
//         }, 3000);
//     }
// }
//
// // Обновленная функция подписки с красивыми уведомлениями
// async function simpleToggleFollow() {
//     console.log('🔄 Простая подписка');
//
//     const currentUserId = document.body.dataset.currentUserId;
//     const profileUserId = document.body.dataset.profileUserId;
//
//     if (!currentUserId) {
//         window.location.href = '/login';
//         return;
//     }
//
//     const btn = document.getElementById('follow-btn');
//     const icon = document.getElementById('follow-icon');
//     const text = document.getElementById('follow-text');
//
//     // Показываем загрузку
//     btn.disabled = true;
//     btn.style.opacity = '0.7';
//     icon.className = 'fas fa-spinner fa-spin';
//
//     try {
//         const response = await fetch('/api/subscriptions/toggle', {
//             method: 'POST',
//             headers: {
//                 'Content-Type': 'application/json'
//             },
//             body: JSON.stringify({
//                 subscribedToId: parseInt(profileUserId)
//             })
//         });
//
//         const result = await response.json();
//         console.log('Результат:', result);
//
//         if (result.success) {
//             // Обновляем кнопку
//             if (result.isSubscribed) {
//                 btn.className = 'btn-luxury btn-secondary-luxury';
//                 icon.className = 'fas fa-user-check';
//                 text.textContent = 'Подписан';
//             } else {
//                 btn.className = 'btn-luxury btn-primary-luxury';
//                 icon.className = 'fas fa-user-plus';
//                 text.textContent = 'Подписаться';
//             }
//
//             // Обновляем счетчик
//             const followersCount = document.querySelector('.stats-section .stat-item:nth-child(2) .stat-number');
//             if (followersCount && result.followersCount !== undefined) {
//                 followersCount.textContent = result.followersCount;
//             }
//
//             // Красивое уведомление вместо alert
//             showNotification(result.message, 'success');
//
//             // Добавляем короткую анимацию успеха для кнопки
//             btn.style.transform = 'scale(0.95)';
//             setTimeout(() => {
//                 btn.style.transform = 'scale(1)';
//             }, 150);
//
//         } else {
//             showNotification('Ошибка: ' + result.message, 'error');
//         }
//     } catch (error) {
//         console.error('Ошибка:', error);
//         showNotification('Произошла ошибка при подписке', 'error');
//     } finally {
//         // Убираем загрузку
//         btn.disabled = false;
//         btn.style.opacity = '1';
//     }
// }

class SubscriptionActions {
    static async toggleSubscription(userId, button) {
        const isFollowing = button.classList.contains('following');

        try {
            button.disabled = true;

            let result;
            if (isFollowing) {
                result = await UserActions.unfollowUser(userId);
            } else {
                result = await UserActions.followUser(userId);
            }

            // Обновляем UI
            ProfileUtils.updateFollowButton(button, !isFollowing);

            // Обновляем счетчик подписчиков
            const followersElement = document.querySelector('.followers-count');
            if (followersElement && result.followersCount !== undefined) {
                ProfileUtils.updateFollowersCount(followersElement, result.followersCount);
            }

            return result;
        } catch (error) {
            console.error('Ошибка при изменении подписки:', error);
        } finally {
            button.disabled = false;
        }
    }

    static init() {
        console.log('✅ SubscriptionActions инициализирован');

        // Инициализируем обработчики кнопок подписки
        const followButtons = document.querySelectorAll('[data-action="follow"]');
        followButtons.forEach(button => {
            button.addEventListener('click', (e) => {
                e.preventDefault();
                const userId = button.getAttribute('data-user-id');
                if (userId) {
                    this.toggleSubscription(userId, button);
                }
            });
        });
    }
}
