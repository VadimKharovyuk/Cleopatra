// // ===== УЛУЧШЕННАЯ ЛОГИКА ОБНОВЛЕНИЯ СТАТУСА =====
//
// let statusCheckInterval = null;
// let isCurrentUser = true; // Флаг: смотрим ли мы на свой профиль
// let currentUserId = null; // ID текущего пользователя
// let viewedUserId = null;  // ID пользователя, на которого смотрим
//
// /**
//  * Инициализация - определяем контекст
//  */
// function initializeStatusContext() {
//     // Получаем ID из атрибутов body (ваша структура)
//     const bodyElement = document.body;
//
//     if (bodyElement) {
//         currentUserId = parseInt(bodyElement.dataset.currentUserId);
//         viewedUserId = parseInt(bodyElement.dataset.profileUserId);
//     }
//
//     // Определяем, смотрим ли мы на свой профиль
//     isCurrentUser = viewedUserId === currentUserId;
//
//     console.log('🔍 Контекст статуса:', {
//         currentUserId,
//         viewedUserId,
//         isCurrentUser: isCurrentUser ? 'да (свой профиль)' : 'нет (чужой профиль)'
//     });
// }
//
// /**
//  * Проверить и обновить статус (УЛУЧШЕННАЯ ВЕРСИЯ)
//  */
// async function checkAndUpdateStatus() {
//     try {
//         let response;
//
//         if (isCurrentUser) {
//             // Если это наш профиль - получаем свой статус
//             response = await fetch('/api/users/me/status');
//         } else {
//             // Если это чужой профиль - получаем статус конкретного пользователя
//             response = await fetch(`/api/users/${viewedUserId}/status`);
//         }
//
//         if (response.ok) {
//             const data = await response.json();
//             console.log('📊 Статус получен:', data);
//
//             // Обновляем отображение
//             updateStatusDisplay(data.isOnline, data.lastSeen);
//         } else {
//             console.warn('⚠️ Не удалось получить статус:', response.status);
//         }
//     } catch (error) {
//         console.error('❌ Ошибка при проверке статуса:', error);
//     }
// }
//
// /**
//  * Обновить отображение статуса с учетом времени
//  */
// function updateStatusDisplay(isOnline, lastSeen) {
//     // Скрываем серверные статусы
//     hideElement('server-status-online');
//     hideElement('server-status-recently');
//     hideElement('server-status-offline');
//
//     // Скрываем динамические статусы
//     hideElement('dynamic-status-online');
//     hideElement('dynamic-status-offline');
//     hideElement('dynamic-status-recently');
//
//     if (isOnline) {
//         showElement('dynamic-status-online');
//         console.log('✅ Отображен статус: ОНЛАЙН');
//     } else {
//         // Проверяем, когда был последний раз онлайн
//         if (lastSeen) {
//             const lastSeenDate = new Date(lastSeen);
//             const now = new Date();
//             const minutesAgo = (now - lastSeenDate) / (1000 * 60);
//
//             if (minutesAgo < 15) {
//                 // Недавно был онлайн
//                 showElement('dynamic-status-recently');
//                 updateRecentlySeenTime(lastSeenDate);
//                 console.log('🕐 Отображен статус: НЕДАВНО ОНЛАЙН');
//             } else {
//                 // Давно не был онлайн
//                 showElement('dynamic-status-offline');
//                 updateOfflineTime(lastSeenDate);
//                 console.log('📴 Отображен статус: ОФЛАЙН');
//             }
//         } else {
//             showElement('dynamic-status-offline');
//             console.log('📴 Отображен статус: ОФЛАЙН (нет данных)');
//         }
//     }
// }
//
// /**
//  * Установить пользователя онлайн и обновить статус (ТОЛЬКО ДЛЯ СЕБЯ)
//  */
// async function setUserOnlineAndUpdate() {
//     // ВАЖНО: Устанавливаем онлайн ТОЛЬКО для себя!
//     if (!isCurrentUser) {
//         console.log('⚠️ Пропуск установки онлайн - это не наш профиль');
//         return;
//     }
//
//     try {
//         // Устанавливаем онлайн
//         const response = await fetch('/api/users/me/online', {
//             method: 'POST',
//             headers: {'Content-Type': 'application/json'}
//         });
//
//         if (response.ok) {
//             console.log('✅ Пользователь установлен онлайн');
//
//             // СРАЗУ обновляем отображение без задержки
//             updateStatusDisplay(true, new Date().toISOString());
//
//             // Дополнительная проверка через небольшую задержку
//             setTimeout(() => {
//                 checkAndUpdateStatus();
//             }, 200);
//         }
//     } catch (error) {
//         console.error('❌ Ошибка установки онлайн:', error);
//     }
// }
//
// /**
//  * Запустить мониторинг статуса (УМНЫЙ)
//  */
// function startStatusMonitoring() {
//     // Инициализируем контекст
//     initializeStatusContext();
//
//     if (isCurrentUser) {
//         // Если это наш профиль - устанавливаем себя онлайн
//         setUserOnlineAndUpdate();
//
//         // Частая проверка для собственного профиля (каждые 15 секунд)
//         statusCheckInterval = setInterval(checkAndUpdateStatus, 15000);
//         console.log('⏰ Мониторинг собственного статуса запущен');
//     } else {
//         // Если это чужой профиль - просто проверяем статус
//         checkAndUpdateStatus();
//
//         // Редкая проверка для чужих профилей (каждые 60 секунд)
//         statusCheckInterval = setInterval(checkAndUpdateStatus, 60000);
//         console.log('⏰ Мониторинг чужого статуса запущен');
//     }
// }
//
// /**
//  * Показать элемент
//  */
// function showElement(id) {
//     const element = document.getElementById(id);
//     if (element) {
//         element.style.display = 'flex';
//     }
// }
//
// /**
//  * Скрыть элемент
//  */
// function hideElement(id) {
//     const element = document.getElementById(id);
//     if (element) {
//         element.style.display = 'none';
//     }
// }
//
// /**
//  * Обновить время для "недавно онлайн"
//  */
// function updateRecentlySeenTime(lastSeenDate) {
//     const timeElement = document.querySelector('#dynamic-status-recently .status-sub');
//     if (timeElement) {
//         const time = lastSeenDate.toLocaleTimeString('ru-RU', {
//             hour: '2-digit',
//             minute: '2-digit'
//         });
//         timeElement.textContent = `был в ${time}`;
//     }
// }
//
// /**
//  * Обновить время для "давно офлайн"
//  */
// function updateOfflineTime(lastSeenDate) {
//     const timeElement = document.querySelector('#dynamic-status-offline .status-sub');
//     if (timeElement) {
//         const now = new Date();
//         const diffInDays = Math.floor((now - lastSeenDate) / (1000 * 60 * 60 * 24));
//
//         if (diffInDays === 0) {
//             const time = lastSeenDate.toLocaleTimeString('ru-RU', {
//                 hour: '2-digit',
//                 minute: '2-digit'
//             });
//             timeElement.textContent = `был сегодня в ${time}`;
//         } else if (diffInDays === 1) {
//             timeElement.textContent = 'был вчера';
//         } else if (diffInDays < 7) {
//             timeElement.textContent = `был ${diffInDays} дн. назад`;
//         } else {
//             timeElement.textContent = 'давно не был в сети';
//         }
//     }
// }
//
// /**
//  * Остановить мониторинг
//  */
// function stopStatusMonitoring() {
//     if (statusCheckInterval) {
//         clearInterval(statusCheckInterval);
//         statusCheckInterval = null;
//         console.log('⏹️ Мониторинг статуса остановлен');
//     }
// }
//
// // ===== СОБЫТИЯ =====
//
// // Запуск при загрузке страницы
// document.addEventListener('DOMContentLoaded', function() {
//     console.log('🚀 Страница загружена, запускаем умный мониторинг статуса...');
//
//     // Небольшая задержка для завершения загрузки
//     setTimeout(startStatusMonitoring, 500);
// });
//
// // Установка офлайн при закрытии (ТОЛЬКО для себя)
// window.addEventListener('beforeunload', function() {
//     if (isCurrentUser) {
//         navigator.sendBeacon('/api/users/me/offline');
//     }
// });
//
// // Обновление при возвращении на вкладку (ТОЛЬКО для себя)
// document.addEventListener('visibilitychange', function() {
//     if (!document.hidden && isCurrentUser) {
//         console.log('👁️ Вкладка активна, обновляем свой статус...');
//         setUserOnlineAndUpdate();
//     } else if (!document.hidden && !isCurrentUser) {
//         console.log('👁️ Вкладка активна, проверяем чужой статус...');
//         checkAndUpdateStatus();
//     }
// });
//
//
// // Дополнительный ping каждые 60 секунд (ТОЛЬКО для себя)
// setInterval(function() {
//     if (isCurrentUser) {
//         fetch('/api/users/me/ping', {method: 'POST'})
//             .catch(error => console.warn('⚠️ Ping failed:', error));
//     }
// }, 60000);
//
// console.log('📱 Умный скрипт управления статусом подключен');


// ===== УЛУЧШЕННАЯ ЛОГИКА ОБНОВЛЕНИЯ СТАТУСА =====

let statusCheckInterval = null;
let isCurrentUser = true; // Флаг: смотрим ли мы на свой профиль
let currentUserId = null; // ID текущего пользователя
let viewedUserId = null;  // ID пользователя, на которого смотрим

/**
 * Инициализация - определяем контекст
 */
function initializeStatusContext() {
    // Приоритет: window.appData, затем data-атрибуты
    if (window.appData) {
        console.log('🔍 Используем window.appData:', window.appData);
        currentUserId = parseUserIdSafely(window.appData.currentUserId);
        viewedUserId = parseUserIdSafely(window.appData.profileUserId);
    } else {
        // Fallback к data-атрибутам
        const bodyElement = document.body;
        if (bodyElement) {
            const currentUserIdRaw = bodyElement.dataset.currentUserId;
            const viewedUserIdRaw = bodyElement.dataset.profileUserId;

            console.log('🔍 Сырые данные из data-атрибутов:', {
                currentUserIdRaw,
                viewedUserIdRaw
            });

            // Безопасное преобразование в числа
            currentUserId = parseUserIdSafely(currentUserIdRaw);
            viewedUserId = parseUserIdSafely(viewedUserIdRaw);
        }
    }

    // Если viewedUserId не определен, используем currentUserId
    if (!viewedUserId && currentUserId) {
        viewedUserId = currentUserId;
    }

    // Проверяем валидность ID
    if (!isValidUserId(currentUserId)) {
        console.error('❌ Некорректный currentUserId:', currentUserId);
        return false;
    }

    if (!isValidUserId(viewedUserId)) {
        console.error('❌ Некорректный viewedUserId:', viewedUserId);
        return false;
    }

    // Определяем, смотрим ли мы на свой профиль
    isCurrentUser = viewedUserId === currentUserId;

    console.log('🔍 Контекст статуса:', {
        currentUserId,
        viewedUserId,
        isCurrentUser: isCurrentUser ? 'да (свой профиль)' : 'нет (чужой профиль)'
    });

    return true; // Успешная инициализация
}

/**
 * Безопасное преобразование строки в userId
 */
function parseUserIdSafely(value) {
    // Проверяем различные "пустые" значения
    if (!value ||
        value === 'undefined' ||
        value === 'null' ||
        value === '' ||
        value === '0') {
        return null;
    }

    const parsed = parseInt(value, 10);

    if (isNaN(parsed) || parsed <= 0) {
        console.warn('⚠️ Некорректное значение userId:', value, 'parsed:', parsed);
        return null;
    }

    return parsed;
}

/**
 * Проверка валидности userId
 */
function isValidUserId(userId) {
    return userId !== null && userId !== undefined && !isNaN(userId) && userId > 0;
}

/**
 * Проверить и обновить статус (УЛУЧШЕННАЯ ВЕРСИЯ)
 */
async function checkAndUpdateStatus() {
    // ДОБАВЛЕНА ПРОВЕРКА: Не выполняем запросы с некорректными ID
    if (!isValidUserId(currentUserId) || !isValidUserId(viewedUserId)) {
        console.error('❌ Пропуск запроса статуса - некорректные ID:', {
            currentUserId,
            viewedUserId
        });
        return;
    }

    try {
        let response;

        if (isCurrentUser) {
            // Если это наш профиль - получаем свой статус
            response = await fetch('/api/users/me/status');
        } else {
            // Если это чужой профиль - получаем статус конкретного пользователя
            response = await fetch(`/api/users/${viewedUserId}/status`);
        }

        if (response.ok) {
            const data = await response.json();
            console.log('📊 Статус получен:', data);

            // Обновляем отображение
            updateStatusDisplay(data.isOnline, data.lastSeen);
        } else {
            console.warn('⚠️ Не удалось получить статус:', response.status);
        }
    } catch (error) {
        console.error('❌ Ошибка при проверке статуса:', error);
    }
}

/**
 * Обновить отображение статуса с учетом времени
 */
function updateStatusDisplay(isOnline, lastSeen) {
    // Скрываем серверные статусы
    hideElement('server-status-online');
    hideElement('server-status-recently');
    hideElement('server-status-offline');

    // Скрываем динамические статусы
    hideElement('dynamic-status-online');
    hideElement('dynamic-status-offline');
    hideElement('dynamic-status-recently');

    if (isOnline) {
        showElement('dynamic-status-online');
        console.log('✅ Отображен статус: ОНЛАЙН');
    } else {
        // Проверяем, когда был последний раз онлайн
        if (lastSeen) {
            const lastSeenDate = new Date(lastSeen);
            const now = new Date();
            const minutesAgo = (now - lastSeenDate) / (1000 * 60);

            if (minutesAgo < 15) {
                // Недавно был онлайн
                showElement('dynamic-status-recently');
                updateRecentlySeenTime(lastSeenDate);
                console.log('🕐 Отображен статус: НЕДАВНО ОНЛАЙН');
            } else {
                // Давно не был онлайн
                showElement('dynamic-status-offline');
                updateOfflineTime(lastSeenDate);
                console.log('📴 Отображен статус: ОФЛАЙН');
            }
        } else {
            showElement('dynamic-status-offline');
            console.log('📴 Отображен статус: ОФЛАЙН (нет данных)');
        }
    }
}

/**
 * Установить пользователя онлайн и обновить статус (ТОЛЬКО ДЛЯ СЕБЯ)
 */
async function setUserOnlineAndUpdate() {
    // ВАЖНО: Устанавливаем онлайн ТОЛЬКО для себя!
    if (!isCurrentUser) {
        console.log('⚠️ Пропуск установки онлайн - это не наш профиль');
        return;
    }

    // ДОБАВЛЕНА ПРОВЕРКА: Валидность currentUserId
    if (!isValidUserId(currentUserId)) {
        console.error('❌ Пропуск установки онлайн - некорректный currentUserId:', currentUserId);
        return;
    }

    try {
        // Устанавливаем онлайн
        const response = await fetch('/api/users/me/online', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'}
        });

        if (response.ok) {
            console.log('✅ Пользователь установлен онлайн');

            // СРАЗУ обновляем отображение без задержки
            updateStatusDisplay(true, new Date().toISOString());

            // Дополнительная проверка через небольшую задержку
            setTimeout(() => {
                checkAndUpdateStatus();
            }, 200);
        } else {
            console.error('❌ Ошибка установки онлайн:', response.status);
        }
    } catch (error) {
        console.error('❌ Ошибка установки онлайн:', error);
    }
}

/**
 * Запустить мониторинг статуса (УМНЫЙ)
 */
function startStatusMonitoring() {
    // Инициализируем контекст
    const initSuccess = initializeStatusContext();

    if (!initSuccess) {
        console.error('❌ Не удалось инициализировать контекст статуса. Мониторинг не запущен.');
        return;
    }

    if (isCurrentUser) {
        // Если это наш профиль - устанавливаем себя онлайн
        setUserOnlineAndUpdate();

        // Частая проверка для собственного профиля (каждые 15 секунд)
        statusCheckInterval = setInterval(checkAndUpdateStatus, 15000);
        console.log('⏰ Мониторинг собственного статуса запущен');
    } else {
        // Если это чужой профиль - просто проверяем статус
        checkAndUpdateStatus();

        // Редкая проверка для чужих профилей (каждые 60 секунд)
        statusCheckInterval = setInterval(checkAndUpdateStatus, 60000);
        console.log('⏰ Мониторинг чужого статуса запущен');
    }
}

/**
 * Показать элемент
 */
function showElement(id) {
    const element = document.getElementById(id);
    if (element) {
        element.style.display = 'flex';
    }
}

/**
 * Скрыть элемент
 */
function hideElement(id) {
    const element = document.getElementById(id);
    if (element) {
        element.style.display = 'none';
    }
}

/**
 * Обновить время для "недавно онлайн"
 */
function updateRecentlySeenTime(lastSeenDate) {
    const timeElement = document.querySelector('#dynamic-status-recently .status-sub');
    if (timeElement) {
        const time = lastSeenDate.toLocaleTimeString('ru-RU', {
            hour: '2-digit',
            minute: '2-digit'
        });
        timeElement.textContent = `был в ${time}`;
    }
}

/**
 * Обновить время для "давно офлайн"
 */
function updateOfflineTime(lastSeenDate) {
    const timeElement = document.querySelector('#dynamic-status-offline .status-sub');
    if (timeElement) {
        const now = new Date();
        const diffInDays = Math.floor((now - lastSeenDate) / (1000 * 60 * 60 * 24));

        if (diffInDays === 0) {
            const time = lastSeenDate.toLocaleTimeString('ru-RU', {
                hour: '2-digit',
                minute: '2-digit'
            });
            timeElement.textContent = `был сегодня в ${time}`;
        } else if (diffInDays === 1) {
            timeElement.textContent = 'был вчера';
        } else if (diffInDays < 7) {
            timeElement.textContent = `был ${diffInDays} дн. назад`;
        } else {
            timeElement.textContent = 'давно не был в сети';
        }
    }
}

/**
 * Остановить мониторинг
 */
function stopStatusMonitoring() {
    if (statusCheckInterval) {
        clearInterval(statusCheckInterval);
        statusCheckInterval = null;
        console.log('⏹️ Мониторинг статуса остановлен');
    }
}

// ===== СОБЫТИЯ =====

// Запуск при загрузке страницы
document.addEventListener('DOMContentLoaded', function() {
    console.log('🚀 Страница загружена, запускаем умный мониторинг статуса...');

    // Небольшая задержка для завершения загрузки
    setTimeout(startStatusMonitoring, 500);
});

// Установка офлайн при закрытии (ТОЛЬКО для себя)
window.addEventListener('beforeunload', function() {
    if (isCurrentUser && isValidUserId(currentUserId)) {
        navigator.sendBeacon('/api/users/me/offline');
    }
});

// Обновление при возвращении на вкладку (ТОЛЬКО для себя)
document.addEventListener('visibilitychange', function() {
    if (!document.hidden && isCurrentUser && isValidUserId(currentUserId)) {
        console.log('👁️ Вкладка активна, обновляем свой статус...');
        setUserOnlineAndUpdate();
    } else if (!document.hidden && !isCurrentUser && isValidUserId(viewedUserId)) {
        console.log('👁️ Вкладка активна, проверяем чужой статус...');
        checkAndUpdateStatus();
    }
});

// Дополнительный ping каждые 60 секунд (ТОЛЬКО для себя)
setInterval(function() {
    if (isCurrentUser && isValidUserId(currentUserId)) {
        fetch('/api/users/me/ping', {method: 'POST'})
            .catch(error => console.warn('⚠️ Ping failed:', error));
    }
}, 60000);

console.log('📱 Умный скрипт управления статусом подключен');