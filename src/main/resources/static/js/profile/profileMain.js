// profileMain.js - Главный файл для инициализации профиля

document.addEventListener('DOMContentLoaded', function() {
    // Инициализация всех модулей
    initializeProfileModules();

    // Отладочная информация
    console.log('👤 Profile Actions initialized');
    console.log('Current User ID:', /*[[${currentUserId}]]*/ null);
    console.log('Profile User ID:', /*[[${user.id}]]*/ null);
    console.log('Is Own Profile:', /*[[${currentUserId == user.id}]]*/ false);
});

function initializeProfileModules() {
    // Проверяем, что все модули загружены
    if (typeof window.notificationManager === 'undefined') {
        console.error('NotificationManager не загружен');
        return;
    }

    if (typeof window.apiClient === 'undefined') {
        console.error('ApiClient не загружен');
        return;
    }

    if (typeof window.userActions === 'undefined') {
        console.error('UserActions не загружен');
        return;
    }

    if (typeof window.profileUtils === 'undefined') {
        console.error('ProfileUtils не загружен');
        return;
    }

    console.log('✅ Все модули профиля успешно инициализированы');
}