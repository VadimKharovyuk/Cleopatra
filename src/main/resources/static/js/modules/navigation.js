// static/js/modules/navigation.js

class Navigation {
    static navigateToPost(event, element) {
        console.log('navigateToPost вызвана');
        console.log('Event target:', event.target);

        // Проверяем, что клик не был по интерактивным элементам
        if (event.target.closest('.action-btn') ||
            event.target.closest('.post-author') ||
            event.target.closest('.post-avatar') ||
            event.target.closest('a')) {
            console.log('Клик по интерактивному элементу, переход отменен');
            return;
        }

        const postId = element.getAttribute('data-post-id');
        console.log('Post ID:', postId);

        if (postId) {
            console.log('Переходим к посту:', postId);
            window.location.href = `/posts/${postId}`;
        } else {
            console.error('Post ID не найден!');
        }
    }
}

// Глобальные функции для совместимости с HTML onclick
window.navigateToPost = (event, element) => Navigation.navigateToPost(event, element);

window.Navigation = Navigation;