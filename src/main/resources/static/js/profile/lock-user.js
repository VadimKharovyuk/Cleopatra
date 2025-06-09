// function showNotification(message, type = 'success') {
//     const notification = document.createElement('div');
//     notification.className = `notification notification-${type}`;
//     notification.style.cssText = `
//         position: fixed;
//         top: 20px;
//         right: 20px;
//         padding: 15px 20px;
//         border-radius: 5px;
//         color: white;
//         z-index: 9999;
//         max-width: 300px;
//         word-wrap: break-word;
//         ${type === 'success' ? 'background-color: #28a745;' : 'background-color: #dc3545;'}
//     `;
//     notification.textContent = message;
//
//     document.body.appendChild(notification);
//
//     setTimeout(() => {
//         if (notification.parentNode) {
//             notification.parentNode.removeChild(notification);
//         }
//     }, 3000);
// }
//
// function blockUserById(userId, userName = '') {
//     const confirmMessage = userName
//         ? `Вы уверены, что хотите заблокировать пользователя ${userName}?`
//         : 'Вы уверены, что хотите заблокировать этого пользователя?';
//
//     if (!confirm(confirmMessage)) {
//         return;
//     }
//
//     console.log('Блокировка пользователя...', userId);
//
//     fetch(`/api/v1/blocks/block/${userId}`, {
//         method: 'POST',
//         headers: {
//             'Content-Type': 'application/json'
//         }
//     })
//         .then(response => response.json())
//         .then(data => {
//             if (data.success) {
//                 showNotification(data.message, 'success');
//
//                 setTimeout(() => {
//                     window.location.reload();
//                 }, 1500);
//
//             } else {
//                 showNotification(data.error || 'Ошибка при блокировке пользователя', 'error');
//             }
//         })
//         .catch(error => {
//             console.error('Ошибка при блокировке пользователя:', error);
//             showNotification('Произошла ошибка при блокировке пользователя', 'error');
//         });
// }
//
// function unblockUserById(userId, userName = '') {
//     const confirmMessage = userName
//         ? `Вы уверены, что хотите разблокировать пользователя ${userName}?`
//         : 'Вы уверены, что хотите разблокировать этого пользователя?';
//
//     if (!confirm(confirmMessage)) {
//         return;
//     }
//
//     console.log('Разблокировка пользователя...', userId);
//
//     fetch(`/api/v1/blocks/unblock/${userId}`, {
//         method: 'DELETE',
//         headers: {
//             'Content-Type': 'application/json'
//         }
//     })
//         .then(response => response.json())
//         .then(data => {
//             if (data.success) {
//                 showNotification(data.message, 'success');
//
//                 setTimeout(() => {
//                     window.location.reload();
//                 }, 1500);
//
//             } else {
//                 showNotification(data.error || 'Ошибка при разблокировке пользователя', 'error');
//             }
//         })
//         .catch(error => {
//             console.error('Ошибка при разблокировке пользователя:', error);
//             showNotification('Произошла ошибка при разблокировке пользователя', 'error');
//         });
// }
//
// document.addEventListener('DOMContentLoaded', function() {
//     console.log('DOM загружен, инициализируем обработчики блокировки');
//
//     document.addEventListener('click', function(event) {
//         const target = event.target.closest('[data-action]');
//
//         if (!target) return;
//
//         event.preventDefault();
//
//         const action = target.getAttribute('data-action');
//         const userId = target.getAttribute('data-user-id');
//         const userName = target.getAttribute('data-user-name') || '';
//
//         console.log(`Действие: ${action}, userId: ${userId}, userName: ${userName}`);
//
//         if (action === 'block') {
//             blockUserById(userId, userName);
//         } else if (action === 'unblock') {
//             unblockUserById(userId, userName);
//         }
//     });
// });

// Функция для переключения состояния кнопки
function toggleBlockButton(userId, isBlocked) {
    const container = document.querySelector('.block-button-container');
    if (!container) return;

    const currentBtn = container.querySelector('.smart-block-btn');
    if (!currentBtn) return;

    // Получаем данные пользователя
    const userName = currentBtn.getAttribute('data-user-name');

    // Создаем новую кнопку
    const newBtn = document.createElement('a');
    newBtn.href = '#';
    newBtn.className = 'btn-luxury smart-block-btn';
    newBtn.setAttribute('data-user-id', userId);
    newBtn.setAttribute('data-user-name', userName);

    if (isBlocked) {
        // Пользователь заблокирован - показываем кнопку разблокировки
        newBtn.classList.add('btn-success-luxury');
        newBtn.setAttribute('data-action', 'unblock');
        newBtn.setAttribute('title', 'Разблокировать пользователя');
        newBtn.innerHTML = '<i class="fas fa-unlock"></i><span>Разблокировать</span>';
    } else {
        // Пользователь разблокирован - показываем кнопку блокировки
        newBtn.classList.add('btn-danger-luxury');
        newBtn.setAttribute('data-action', 'block');
        newBtn.setAttribute('title', 'Заблокировать пользователя');
        newBtn.innerHTML = '<i class="fas fa-ban"></i><span>Заблокировать</span>';
    }

    // Плавная замена кнопки
    currentBtn.style.opacity = '0';
    currentBtn.style.transform = 'scale(0.9)';

    setTimeout(() => {
        container.replaceChild(newBtn, currentBtn);
        newBtn.style.opacity = '0';
        newBtn.style.transform = 'scale(0.9)';

        // Анимация появления новой кнопки
        setTimeout(() => {
            newBtn.style.transition = 'all 0.3s ease';
            newBtn.style.opacity = '1';
            newBtn.style.transform = 'scale(1)';
        }, 10);
    }, 150);
}

// Функция для показа состояния загрузки
function setButtonLoading(button, loading) {
    if (loading) {
        button.classList.add('loading');
        const icon = button.querySelector('i');
        icon.className = 'fas fa-spinner';
    } else {
        button.classList.remove('loading');
    }
}

// Обработчик клика для умных кнопок
document.addEventListener('click', function(event) {
    const button = event.target.closest('.smart-block-btn');
    if (!button) return;

    event.preventDefault();
    event.stopPropagation(); // Останавливаем всплытие события
    event.stopImmediatePropagation(); // Останавливаем другие обработчики

    const action = button.getAttribute('data-action');
    const userId = button.getAttribute('data-user-id');
    const userName = button.getAttribute('data-user-name');

    if (!action || !userId) return;

    // Показываем состояние загрузки
    setButtonLoading(button, true);

    if (action === 'block') {
        // Прямой вызов без confirm
        blockUserById(userId, userName);
    } else if (action === 'unblock') {
        // Прямой вызов без confirm
        unblockUserById(userId, userName);
    }
}, true); // Используем capturing phase

// Переопределяем функции блокировки для работы с умными кнопками
function blockUserById(userId, userName) {
    fetch(`/api/v1/blocks/block/${userId}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                showNotification(data.message, 'success');
                // Переключаем кнопку на состояние "заблокирован"
                toggleBlockButton(userId, true);
            } else {
                showNotification(data.error || 'Ошибка при блокировке пользователя', 'error');
                // Убираем состояние загрузки
                const button = document.querySelector('.smart-block-btn.loading');
                if (button) setButtonLoading(button, false);
            }
        })
        .catch(error => {
            console.error('Ошибка при блокировке пользователя:', error);
            showNotification('Произошла ошибка при блокировке пользователя', 'error');
            // Убираем состояние загрузки
            const button = document.querySelector('.smart-block-btn.loading');
            if (button) setButtonLoading(button, false);
        });
}

function unblockUserById(userId, userName) {
    fetch(`/api/v1/blocks/unblock/${userId}`, {
        method: 'DELETE',
        headers: {
            'Content-Type': 'application/json'
        }
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                showNotification(data.message, 'success');
                // Переключаем кнопку на состояние "разблокирован"
                toggleBlockButton(userId, false);
            } else {
                showNotification(data.error || 'Ошибка при разблокировке пользователя', 'error');
                // Убираем состояние загрузки
                const button = document.querySelector('.smart-block-btn.loading');
                if (button) setButtonLoading(button, false);
            }
        })
        .catch(error => {
            console.error('Ошибка при разблокировке пользователя:', error);
            showNotification('Произошла ошибка при разблокировке пользователя', 'error');
            // Убираем состояние загрузки
            const button = document.querySelector('.smart-block-btn.loading');
            if (button) setButtonLoading(button, false);
        });
}

// Функция уведомлений (если ее нет)
function showNotification(message, type = 'success') {
    // Ваша реализация уведомлений
    console.log(`${type.toUpperCase()}: ${message}`);
}