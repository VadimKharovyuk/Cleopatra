// static/js/main.js

document.addEventListener('DOMContentLoaded', function() {
    console.log('🚀 Инициализация приложения...');

    // Инициализируем все модули
    if (window.PostComposer) {
        PostComposer.init();
    }

    if (window.GeolocationManager) {
        GeolocationManager.init();
    }

    console.log('✅ Все модули инициализированы');
});

// Инициализация если DOM уже загружен
if (document.readyState !== 'loading') {
    PostComposer.init();
    GeolocationManager.init();
}

console.log('📝 Главный скрипт загружен');