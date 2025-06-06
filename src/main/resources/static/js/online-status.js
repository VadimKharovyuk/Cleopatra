// **
// * Универсальный скрипт для управления онлайн статусом
// * Подключается один раз и работает на всех страницах
// */

(function() {
    'use strict';

    // Конфигурация
    const CONFIG = {
        PING_INTERVAL: 5 * 60 * 1000,  // 5 минут
        ENDPOINTS: {
            ONLINE: '/api/users/me/online',
            OFFLINE: '/api/users/me/offline',
            PING: '/api/users/me/ping'
        }
    };

    let pingInterval = null;
    let isUserAuthenticated = false;

    // ===================== ПРОВЕРКА АУТЕНТИФИКАЦИИ =====================

    function checkAuthentication() {
        // Можно проверить по наличию CSRF токена, или другим способом
        const csrfToken = document.querySelector('meta[name="_csrf"]');
        const userElement = document.querySelector('[data-user-id]');

        isUserAuthenticated = !!(csrfToken || userElement);
        return isUserAuthenticated;
    }

    // ===================== HTTP ЗАПРОСЫ =====================

    function makeRequest(url, method = 'POST') {
        const options = {
            method: method,
            headers: {
                'Content-Type': 'application/json'
            }
        };

        // Добавляем CSRF токен если есть
        const csrfToken = document.querySelector('meta[name="_csrf"]');
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]');

        if (csrfToken && csrfHeader) {
            options.headers[csrfHeader.getAttribute('content')] = csrfToken.getAttribute('content');
        }

        return fetch(url, options)
            .then(response => {
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                return response.text();
            })
            .catch(error => {
                console.debug('Online status request failed:', error);
            });
    }

    // ===================== ОСНОВНЫЕ ФУНКЦИИ =====================

    function setOnline() {
        if (!isUserAuthenticated) return;

        makeRequest(CONFIG.ENDPOINTS.ONLINE)
            .then(() => {
                console.debug('✅ User set to ONLINE');
                startPingInterval();
            });
    }

    function setOffline() {
        if (!isUserAuthenticated) return;

        // Используем sendBeacon для надежности при закрытии страницы
        if (navigator.sendBeacon) {
            navigator.sendBeacon(CONFIG.ENDPOINTS.OFFLINE);
        } else {
            makeRequest(CONFIG.ENDPOINTS.OFFLINE);
        }

        console.debug('📴 User set to OFFLINE');
        stopPingInterval();
    }

    function ping() {
        if (!isUserAuthenticated) return;

        makeRequest(CONFIG.ENDPOINTS.PING)
            .then(() => {
                console.debug('🏓 Ping sent');
            });
    }

    function startPingInterval() {
        if (pingInterval) return; // Уже запущен

        pingInterval = setInterval(() => {
            ping();
        }, CONFIG.PING_INTERVAL);

        console.debug('⏰ Ping interval started');
    }

    function stopPingInterval() {
        if (pingInterval) {
            clearInterval(pingInterval);
            pingInterval = null;
            console.debug('⏹️ Ping interval stopped');
        }
    }

    // ===================== ОБРАБОТЧИКИ СОБЫТИЙ =====================

    // При загрузке страницы
    function onPageLoad() {
        if (checkAuthentication()) {
            setOnline();
        }
    }

    // При закрытии страницы
    function onPageUnload() {
        setOffline();
    }

    // При изменении видимости страницы
    function onVisibilityChange() {
        if (!isUserAuthenticated) return;

        if (document.hidden) {
            // Страница скрыта - можно остановить ping (опционально)
            // stopPingInterval();
        } else {
            // Страница стала видимой - отправляем ping
            ping();
            startPingInterval();
        }
    }

    // При фокусе/потере фокуса окна
    function onWindowFocus() {
        if (isUserAuthenticated) {
            ping();
        }
    }

    function onWindowBlur() {
        // Можно ничего не делать или остановить ping
    }

    // ===================== ИНИЦИАЛИЗАЦИЯ =====================

    function init() {
        // Проверяем что DOM загружен
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', onPageLoad);
        } else {
            onPageLoad();
        }

        // Обработчики событий
        window.addEventListener('beforeunload', onPageUnload);
        window.addEventListener('pagehide', onPageUnload);  // Для мобильных
        document.addEventListener('visibilitychange', onVisibilityChange);
        window.addEventListener('focus', onWindowFocus);
        window.addEventListener('blur', onWindowBlur);

        // Обработка навигации по SPA (если используете)
        window.addEventListener('popstate', onPageLoad);

        console.debug('🚀 Online status manager initialized');
    }

    // Запускаем инициализацию
    init();

    // ===================== ГЛОБАЛЬНЫЙ API (опционально) =====================

    // Экспортируем функции для ручного управления
    window.OnlineStatus = {
        setOnline: setOnline,
        setOffline: setOffline,
        ping: ping,
        isActive: () => !!pingInterval
    };

})();