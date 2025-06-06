// **
// * Универсальный скрипт для управления онлайн статусом
// * Версия БЕЗ CSRF токенов
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
        console.log('🔍 Checking authentication...');

        // Ищем признаки аутентификации без CSRF
        const userElement = document.querySelector('[data-user-id]');
        const userMenuElement = document.querySelector('.user-menu, .navbar-user, [data-user], .username');
        const logoutButton = document.querySelector('a[href*="logout"], button[onclick*="logout"]');

        // Проверяем cookies на наличие сессии
        const hasSessionCookie = document.cookie.includes('JSESSIONID') ||
            document.cookie.includes('SESSION') ||
            document.cookie.includes('session');

        // Проверяем URL - если есть /login или /auth, то скорее всего не авторизован
        const isOnLoginPage = window.location.pathname.includes('/login') ||
            window.location.pathname.includes('/auth');

        console.log('User element:', userElement);
        console.log('User menu element:', userMenuElement);
        console.log('Logout button:', logoutButton);
        console.log('Has session cookie:', hasSessionCookie);
        console.log('Is on login page:', isOnLoginPage);

        isUserAuthenticated = !isOnLoginPage && !!(userElement || userMenuElement || logoutButton || hasSessionCookie);
        console.log('✅ User authenticated:', isUserAuthenticated);

        return isUserAuthenticated;
    }

    // ===================== HTTP ЗАПРОСЫ =====================

    function makeRequest(url, method = 'POST') {
        console.log(`🌐 Making ${method} request to: ${url}`);

        const options = {
            method: method,
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            credentials: 'same-origin' // Важно! Передает cookies с сессией
        };

        console.log('📤 Request options:', JSON.stringify(options, null, 2));

        return fetch(url, options)
            .then(response => {
                console.log(`📥 Response status: ${response.status} ${response.statusText}`);

                if (!response.ok) {
                    return response.text().then(text => {
                        console.error(`❌ HTTP error! status: ${response.status}, body: ${text}`);
                        throw new Error(`HTTP error! status: ${response.status}, body: ${text}`);
                    });
                }
                return response.text();
            })
            .then(data => {
                console.log('✅ Request successful, response:', data);
                return data;
            })
            .catch(error => {
                console.error('❌ Request failed:', error);
                // Не выбрасываем ошибку дальше, чтобы не ломать весь скрипт
                return null;
            });
    }

    // ===================== ОСНОВНЫЕ ФУНКЦИИ =====================

    function setOnline() {
        console.log('🟢 Setting user ONLINE...');
        if (!isUserAuthenticated) {
            console.warn('⚠️ User not authenticated, skipping setOnline');
            return;
        }

        makeRequest(CONFIG.ENDPOINTS.ONLINE)
            .then((result) => {
                if (result !== null) {
                    console.log('✅ User set to ONLINE');
                    startPingInterval();
                } else {
                    console.error('❌ Failed to set user ONLINE');
                }
            });
    }

    function setOffline() {
        console.log('🔴 Setting user OFFLINE...');
        if (!isUserAuthenticated) {
            console.warn('⚠️ User not authenticated, skipping setOffline');
            return;
        }

        // Для надежности при закрытии страницы используем обычный fetch
        makeRequest(CONFIG.ENDPOINTS.OFFLINE)
            .then((result) => {
                if (result !== null) {
                    console.log('📴 User set to OFFLINE');
                } else {
                    console.error('❌ Failed to set user OFFLINE');
                }
            });

        stopPingInterval();
    }

    function ping() {
        console.log('🏓 Sending ping...');
        if (!isUserAuthenticated) {
            console.warn('⚠️ User not authenticated, skipping ping');
            return;
        }

        makeRequest(CONFIG.ENDPOINTS.PING)
            .then((result) => {
                if (result !== null) {
                    console.log('🏓 Ping sent successfully');
                } else {
                    console.error('❌ Ping failed');
                }
            });
    }

    function startPingInterval() {
        if (pingInterval) {
            console.log('⏰ Ping interval already running');
            return;
        }

        pingInterval = setInterval(() => {
            ping();
        }, CONFIG.PING_INTERVAL);

        console.log(`⏰ Ping interval started (every ${CONFIG.PING_INTERVAL / 1000} seconds)`);
    }

    function stopPingInterval() {
        if (pingInterval) {
            clearInterval(pingInterval);
            pingInterval = null;
            console.log('⏹️ Ping interval stopped');
        }
    }

    // ===================== ОБРАБОТЧИКИ СОБЫТИЙ =====================

    function onPageLoad() {
        console.log('📄 Page loaded');
        if (checkAuthentication()) {
            setOnline();
        } else {
            console.log('❌ User not authenticated, not setting online');
        }
    }

    function onPageUnload() {
        console.log('📄 Page unloading');
        setOffline();
    }

    function onVisibilityChange() {
        console.log('👁️ Visibility changed, hidden:', document.hidden);
        if (!isUserAuthenticated) return;

        if (document.hidden) {
            console.log('👁️ Page hidden');
        } else {
            console.log('👁️ Page visible');
            ping();
            startPingInterval();
        }
    }

    function onWindowFocus() {
        console.log('🎯 Window focused');
        if (isUserAuthenticated) {
            ping();
        }
    }

    function onWindowBlur() {
        console.log('🌫️ Window blurred');
    }

    // ===================== ИНИЦИАЛИЗАЦИЯ =====================

    function init() {
        console.log('🚀 Initializing Online Status Manager (NO CSRF)...');

        if (document.readyState === 'loading') {
            console.log('⏳ DOM still loading, waiting for DOMContentLoaded');
            document.addEventListener('DOMContentLoaded', onPageLoad);
        } else {
            console.log('✅ DOM ready, calling onPageLoad immediately');
            onPageLoad();
        }

        // Обработчики событий
        window.addEventListener('beforeunload', onPageUnload);
        window.addEventListener('pagehide', onPageUnload);
        document.addEventListener('visibilitychange', onVisibilityChange);
        window.addEventListener('focus', onWindowFocus);
        window.addEventListener('blur', onWindowBlur);
        window.addEventListener('popstate', onPageLoad);

        console.log('🚀 Online status manager initialized');
    }

    // Запускаем инициализацию
    init();

    // ===================== ГЛОБАЛЬНЫЙ API =====================

    window.OnlineStatus = {
        setOnline: setOnline,
        setOffline: setOffline,
        ping: ping,
        isActive: () => !!pingInterval,
        // Отладочные функции
        debug: {
            checkAuth: checkAuthentication,
            testPing: () => {
                console.log('🧪 Testing ping manually...');
                ping();
            },
            getConfig: () => CONFIG,
            getAuthStatus: () => isUserAuthenticated,
            forceSetOnline: () => {
                isUserAuthenticated = true;
                setOnline();
            }
        }
    };

})();