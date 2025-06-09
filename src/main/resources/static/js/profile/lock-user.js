function showNotification(message, type = 'success') {
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        padding: 15px 20px;
        border-radius: 5px;
        color: white;
        z-index: 9999;
        max-width: 300px;
        word-wrap: break-word;
        ${type === 'success' ? 'background-color: #28a745;' : 'background-color: #dc3545;'}
    `;
    notification.textContent = message;

    document.body.appendChild(notification);

    setTimeout(() => {
        if (notification.parentNode) {
            notification.parentNode.removeChild(notification);
        }
    }, 3000);
}

function blockUserById(userId, userName = '') {
    const confirmMessage = userName
        ? `Вы уверены, что хотите заблокировать пользователя ${userName}?`
        : 'Вы уверены, что хотите заблокировать этого пользователя?';

    if (!confirm(confirmMessage)) {
        return;
    }

    console.log('Блокировка пользователя...', userId);

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

                setTimeout(() => {
                    window.location.reload();
                }, 1500);

            } else {
                showNotification(data.error || 'Ошибка при блокировке пользователя', 'error');
            }
        })
        .catch(error => {
            console.error('Ошибка при блокировке пользователя:', error);
            showNotification('Произошла ошибка при блокировке пользователя', 'error');
        });
}

function unblockUserById(userId, userName = '') {
    const confirmMessage = userName
        ? `Вы уверены, что хотите разблокировать пользователя ${userName}?`
        : 'Вы уверены, что хотите разблокировать этого пользователя?';

    if (!confirm(confirmMessage)) {
        return;
    }

    console.log('Разблокировка пользователя...', userId);

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

                setTimeout(() => {
                    window.location.reload();
                }, 1500);

            } else {
                showNotification(data.error || 'Ошибка при разблокировке пользователя', 'error');
            }
        })
        .catch(error => {
            console.error('Ошибка при разблокировке пользователя:', error);
            showNotification('Произошла ошибка при разблокировке пользователя', 'error');
        });
}

document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM загружен, инициализируем обработчики блокировки');

    document.addEventListener('click', function(event) {
        const target = event.target.closest('[data-action]');

        if (!target) return;

        event.preventDefault();

        const action = target.getAttribute('data-action');
        const userId = target.getAttribute('data-user-id');
        const userName = target.getAttribute('data-user-name') || '';

        console.log(`Действие: ${action}, userId: ${userId}, userName: ${userName}`);

        if (action === 'block') {
            blockUserById(userId, userName);
        } else if (action === 'unblock') {
            unblockUserById(userId, userName);
        }
    });
});