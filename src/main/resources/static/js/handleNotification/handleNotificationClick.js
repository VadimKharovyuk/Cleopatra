// Добавьте этот JavaScript в конец страницы или в отдельный файл

function handleNotificationClick(element, event) {
    // Предотвращаем всплытие события от дочерних элементов
    if (event.target.closest('.notification-actions') ||
        event.target.closest('.avatar-link')) {
        return;
    }

    try {
        // Получаем данные уведомления
        const notificationData = element.getAttribute('data-notification-data');

        if (!notificationData) {
            console.log('Нет данных для навигации');
            return;
        }

        // Парсим JSON данные
        const data = JSON.parse(notificationData);
        console.log('Notification data:', data);

        // Определяем URL для перехода
        let targetUrl = null;

        if (data.postUrl) {
            // Используем готовый URL из данных (для всех типов постов)
            targetUrl = data.postUrl;
        } else if (data.postId) {
            // Для лайков и комментариев к постам (если нет готового URL)
            targetUrl = `/posts/${data.postId}`;
        } else if (data.profileUrl) {
            // Для подписок/отписок
            targetUrl = data.profileUrl;
        } else if (data.followerId) {
            // Альтернативный способ для профилей
            targetUrl = `/profile/${data.followerId}`;
        }

        if (targetUrl) {
            // Помечаем уведомление как прочитанное (если не прочитано)
            const isUnread = element.classList.contains('unread');
            if (isUnread) {
                markAsReadAndNavigate(element, targetUrl);
            } else {
                // Просто переходим
                window.location.href = targetUrl;
            }
        } else {
            console.log('Не удалось определить URL для перехода');
        }

    } catch (error) {
        console.error('Ошибка при обработке клика по уведомлению:', error);
    }
}

function markAsReadAndNavigate(element, targetUrl) {
    // Находим форму для пометки как прочитанное
    const readForm = element.querySelector('form[action*="/read"]');

    if (readForm) {
        // Отправляем AJAX запрос для пометки как прочитанное
        const formData = new FormData(readForm);

        fetch(readForm.action, {
            method: 'POST',
            body: formData,
            headers: {
                'X-Requested-With': 'XMLHttpRequest'
            }
        })
            .then(response => {
                if (response.ok) {
                    // Убираем класс unread
                    element.classList.remove('unread');

                    // Переходим по ссылке
                    window.location.href = targetUrl;
                } else {
                    console.error('Ошибка при пометке как прочитанное');
                    // Все равно переходим
                    window.location.href = targetUrl;
                }
            })
            .catch(error => {
                console.error('Ошибка AJAX запроса:', error);
                // В случае ошибки все равно переходим
                window.location.href = targetUrl;
            });
    } else {
        // Если нет формы, просто переходим
        window.location.href = targetUrl;
    }
}

// Добавляем визуальную обратную связь при наведении
document.addEventListener('DOMContentLoaded', function() {
    const notificationItems = document.querySelectorAll('.notification-item');

    notificationItems.forEach(item => {
        // Добавляем курсор pointer для кликабельных элементов
        const hasData = item.getAttribute('data-notification-data');
        if (hasData) {
            item.style.cursor = 'pointer';

            // Добавляем подсказку
            item.title = 'Нажмите, чтобы перейти к связанному контенту';
        }
    });
});