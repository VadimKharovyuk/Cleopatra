// static/js/modules/post-interactions.js

class PostInteractions {
    static async toggleLike(event, button) {
        event.stopPropagation();
        console.log('toggleLike вызвана');

        const postId = button.getAttribute('data-post-id');
        console.log('Toggling like for post:', postId);

        if (!postId) {
            console.error('Post ID не найден для лайка!');
            return;
        }

        try {
            // Добавляем визуальную обратную связь
            button.disabled = true;
            button.style.opacity = '0.6';

            const response = await fetch(`/api/posts/${postId}/like`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest'
                }
            });

            if (response.ok) {
                const result = await response.json();
                console.log('Like result:', result);

                // Обновляем UI
                const icon = button.querySelector('i');
                const count = button.querySelector('span');

                if (result.liked) {
                    button.classList.add('liked');
                    icon.classList.remove('far');
                    icon.classList.add('fas');
                    button.style.color = '#e0245e';
                } else {
                    button.classList.remove('liked');
                    icon.classList.remove('fas');
                    icon.classList.add('far');
                    button.style.color = '';
                }

                if (count) {
                    count.textContent = result.likesCount || 0;
                }

                console.log('Лайк обновлен успешно');
            } else {
                console.error('Ошибка при лайке:', response.status, response.statusText);
                NotificationManager.show('Ошибка при обновлении лайка', 'error');
            }
        } catch (error) {
            console.error('Ошибка при лайке:', error);
            NotificationManager.show('Ошибка при обновлении лайка', 'error');
        } finally {
            // Восстанавливаем состояние кнопки
            button.disabled = false;
            button.style.opacity = '1';
        }
    }

    static openComments(event, button) {
        event.stopPropagation();
        console.log('openComments вызвана');

        const postId = button.getAttribute('data-post-id');
        console.log('Opening comments for post:', postId);

        if (postId) {
            window.location.href = `/posts/${postId}#comments`;
        } else {
            console.error('Post ID не найден для комментариев!');
        }
    }

    static sharePost(event, button) {
        event.stopPropagation();
        console.log('sharePost вызвана');

        const postId = button.getAttribute('data-post-id');
        console.log('Sharing post:', postId);

        if (!postId) {
            console.error('Post ID не найден для шаринга!');
            return;
        }

        const postUrl = `${window.location.origin}/posts/${postId}`;

        // Проверяем, поддерживает ли браузер Web Share API
        if (navigator.share) {
            navigator.share({
                title: 'Пост из Cleopatra',
                text: 'Посмотрите этот интересный пост!',
                url: postUrl
            }).then(() => {
                console.log('Пост успешно поделен');
                NotificationManager.show('Пост поделен!', 'success');
            }).catch((error) => {
                console.log('Ошибка при шаринге:', error);
                PostInteractions.copyToClipboard(postUrl);
            });
        } else {
            PostInteractions.copyToClipboard(postUrl);
        }
    }

    static copyToClipboard(text) {
        if (navigator.clipboard) {
            navigator.clipboard.writeText(text).then(() => {
                NotificationManager.show('Ссылка скопирована в буфер обмена', 'success');
            }).catch(() => {
                PostInteractions.fallbackCopyToClipboard(text);
            });
        } else {
            PostInteractions.fallbackCopyToClipboard(text);
        }
    }

    static fallbackCopyToClipboard(text) {
        const textArea = document.createElement('textarea');
        textArea.value = text;
        textArea.style.position = 'fixed';
        textArea.style.left = '-999999px';
        textArea.style.top = '-999999px';
        document.body.appendChild(textArea);
        textArea.focus();
        textArea.select();

        try {
            document.execCommand('copy');
            NotificationManager.show('Ссылка скопирована в буфер обмена', 'success');
        } catch (err) {
            console.error('Не удалось скопировать текст: ', err);
            NotificationManager.show('Не удалось скопировать ссылку', 'error');
        }

        document.body.removeChild(textArea);
    }
}

// Глобальные функции для совместимости с HTML onclick
window.toggleLike = (event, button) => PostInteractions.toggleLike(event, button);
window.openComments = (event, button) => PostInteractions.openComments(event, button);
window.sharePost = (event, button) => PostInteractions.sharePost(event, button);

window.PostInteractions = PostInteractions;