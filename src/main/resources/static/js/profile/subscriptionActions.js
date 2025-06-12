// ===================================================
// subscriptionActions.js - Система подписок
// ===================================================

// Функция для показа красивых уведомлений
function showNotification(message, type = 'success') {
    const alertClass = type === 'success' ? 'alert-success-luxury' : 'alert-danger-luxury';
    const iconClass = type === 'success' ? 'fa-check-circle' : 'fa-exclamation-circle';

    const alert = document.createElement('div');
    alert.className = `alert-luxury ${alertClass}`;
    alert.style.opacity = '0';
    alert.style.transform = 'translateY(-20px)';
    alert.style.transition = 'all 0.3s ease';
    alert.innerHTML = `
        <i class="fas ${iconClass} me-2"></i>
        <span>${message}</span>
    `;

    // Универсальный поиск контейнера для уведомлений
    const containers = [
        '.main-content',
        '.container',
        '.content',
        'main',
        'body'
    ];

    let targetContainer = null;
    let insertBefore = null;

    for (const selector of containers) {
        const container = document.querySelector(selector);
        if (container) {
            targetContainer = container;

            // Ищем элемент для вставки перед ним
            const insertTargets = [
                '.profile-container',
                '.recommendations-container',
                '.content-container',
                '.page-content',
                container.firstElementChild
            ];

            for (const target of insertTargets) {
                const element = typeof target === 'string' ? document.querySelector(target) : target;
                if (element && container.contains(element)) {
                    insertBefore = element;
                    break;
                }
            }
            break;
        }
    }

    if (targetContainer) {
        if (insertBefore) {
            targetContainer.insertBefore(alert, insertBefore);
        } else {
            targetContainer.appendChild(alert);
        }

        // Анимация появления
        setTimeout(() => {
            alert.style.opacity = '1';
            alert.style.transform = 'translateY(0)';
        }, 10);

        // Автоматическое скрытие через 3 секунды
        setTimeout(() => {
            alert.style.opacity = '0';
            alert.style.transform = 'translateY(-20px)';
            setTimeout(() => {
                if (alert.parentNode) {
                    alert.parentNode.removeChild(alert);
                }
            }, 300);
        }, 3000);
    }
}

// Универсальная функция для переключения подписки
async function toggleFollow(buttonElement) {
    console.log('🔄 Переключение подписки');

    const currentUserId = document.body.dataset.currentUserId;

    // Пробуем получить ID пользователя из разных источников
    let profileUserId = null;

    if (buttonElement && buttonElement.dataset.userId) {
        profileUserId = buttonElement.dataset.userId;
    } else if (document.body.dataset.profileUserId) {
        profileUserId = document.body.dataset.profileUserId;
    } else if (buttonElement && buttonElement.getAttribute('data-user-id')) {
        profileUserId = buttonElement.getAttribute('data-user-id');
    }

    console.log('Current User ID:', currentUserId);
    console.log('Profile User ID:', profileUserId);

    // Проверяем что получили оба ID
    if (!currentUserId) {
        showNotification('Для подписки необходимо войти в аккаунт', 'error');
        setTimeout(() => {
            window.location.href = '/login';
        }, 1500);
        return;
    }

    if (!profileUserId) {
        console.error('Profile User ID не найден');
        showNotification('Ошибка: не удалось определить пользователя', 'error');
        return;
    }

    // Проверяем что не подписываемся на себя
    if (currentUserId === profileUserId) {
        showNotification('Нельзя подписаться на самого себя', 'error');
        return;
    }

    // Находим элементы кнопки (поддерживаем как ID, так и классы)
    const btn = buttonElement ||
        document.getElementById('follow-btn') ||
        document.querySelector('.follow-btn');

    if (!btn) {
        console.error('Кнопка подписки не найдена');
        return;
    }

    const icon = btn.querySelector('[id*="follow-icon"], .follow-icon, i') ||
        document.getElementById('follow-icon');
    const text = btn.querySelector('[id*="follow-text"], .follow-text, span') ||
        document.getElementById('follow-text');

    // Сохраняем исходные состояния
    const originalIconClass = icon ? icon.className : '';
    const originalBtnClass = btn.className;

    // Показываем загрузку
    btn.disabled = true;
    btn.style.opacity = '0.7';
    if (icon) {
        icon.className = 'fas fa-spinner fa-spin';
    }

    try {
        // Получаем CSRF токен
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

        // Формируем заголовки
        const requestHeaders = {
            'Content-Type': 'application/json'
        };

        // Добавляем CSRF токен если есть
        if (csrfToken && csrfHeader) {
            requestHeaders[csrfHeader] = csrfToken;
        }

        const response = await fetch('/api/subscriptions/toggle', {
            method: 'POST',
            headers: requestHeaders,
            body: JSON.stringify({
                subscribedToId: parseInt(profileUserId)
            })
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`HTTP ${response.status}: ${errorText}`);
        }

        const result = await response.json();
        console.log('Результат:', result);

        if (result.success) {
            // Обновляем кнопку в зависимости от состояния
            updateFollowButton(btn, icon, text, result.isSubscribed);

            // Обновляем счетчик подписчиков
            updateFollowersCount(result.followersCount);

            // Красивое уведомление
            showNotification(result.message, 'success');

            // Анимация успеха для кнопки
            btn.style.transform = 'scale(0.95)';
            setTimeout(() => {
                btn.style.transform = 'scale(1)';
            }, 150);

        } else {
            showNotification('Ошибка: ' + (result.message || 'Неизвестная ошибка'), 'error');
            // Возвращаем исходное состояние при ошибке
            if (icon) icon.className = originalIconClass;
            btn.className = originalBtnClass;
        }

    } catch (error) {
        console.error('Ошибка при подписке:', error);
        showNotification('Произошла ошибка при подписке. Попробуйте снова.', 'error');

        // Возвращаем исходное состояние при ошибке
        if (icon) icon.className = originalIconClass;
        btn.className = originalBtnClass;
    } finally {
        // Убираем состояние загрузки
        btn.disabled = false;
        btn.style.opacity = '1';
    }
}

// Функция для обновления состояния кнопки подписки
function updateFollowButton(btn, icon, text, isSubscribed) {
    if (isSubscribed) {
        // Подписан - добавляем класс following
        btn.className = 'btn-luxury btn-secondary-luxury following';

        if (icon) icon.className = 'fas fa-user-check';
        if (text) text.textContent = 'Подписан';
    } else {
        // Не подписан - убираем класс following
        btn.className = 'btn-luxury btn-primary-luxury';

        if (icon) icon.className = 'fas fa-user-plus';
        if (text) text.textContent = 'Подписаться';
    }
}

// Функция для обновления счетчика подписчиков
function updateFollowersCount(newCount) {
    if (newCount === undefined) return;

    // Поиск элемента со счетчиком подписчиков
    const selectors = [
        '.stats-section .stat-item:nth-child(2) .stat-number',
        '.followers-count',
        '[data-followers-count]',
        '.stat-number',
        '.followers .number',
        '.subscriber-count'
    ];

    for (const selector of selectors) {
        const element = document.querySelector(selector);
        if (element) {
            // Анимированное обновление числа
            const currentCount = parseInt(element.textContent) || 0;
            animateNumber(element, currentCount, newCount);
            break;
        }
    }
}

// Функция для анимированного изменения числа
function animateNumber(element, from, to) {
    const duration = 500;
    const steps = 20;
    const stepSize = (to - from) / steps;
    const stepDuration = duration / steps;

    let current = from;
    let step = 0;

    const timer = setInterval(() => {
        step++;
        current += stepSize;

        if (step >= steps) {
            element.textContent = to;
            clearInterval(timer);
        } else {
            element.textContent = Math.round(current);
        }
    }, stepDuration);
}

// Обратная совместимость - алиас для старой функции
function simpleToggleFollow() {
    const btn = document.getElementById('follow-btn') ||
        document.querySelector('.follow-btn');
    if (btn) {
        toggleFollow(btn);
    } else {
        console.error('Кнопка подписки не найдена');
    }
}

// Инициализация при загрузке страницы
document.addEventListener('DOMContentLoaded', function() {
    console.log('🚀 Система подписок инициализирована');

    // Находим все кнопки подписки и привязываем события
    const followButtons = document.querySelectorAll(
        '[data-action="follow"], .follow-btn, #follow-btn, [onclick*="toggleFollow"], [onclick*="simpleToggleFollow"]'
    );

    followButtons.forEach(button => {
        // НЕ удаляем onclick, если он есть - оставляем для обратной совместимости

        // Добавляем новый event listener только если нет onclick
        if (!button.hasAttribute('onclick')) {
            button.addEventListener('click', function(e) {
                e.preventDefault();
                toggleFollow(this);
            });
            console.log('🔗 Подключена кнопка подписки через addEventListener:', button);
        } else {
            console.log('🔗 Кнопка подписки уже имеет onclick обработчик:', button);
        }
    });
});

// Экспортируем функции для глобального использования
window.toggleFollow = toggleFollow;
window.simpleToggleFollow = simpleToggleFollow;
window.showNotification = showNotification;