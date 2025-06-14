/**
 * wall-interactions.js
 * Интерактивные действия со стеной (лайки, комментарии, удаление)
 */

class WallInteractions {

    constructor(config) {
        this.wallOwnerId = config.wallOwnerId;
        this.currentUserId = config.currentUserId;
        this.wallPosts = config.wallPosts; // Ссылка на экземпляр WallPosts
    }

    /**
     * Переключение лайка поста
     */
    async toggleLike(postId) {
        const likeBtn = document.querySelector(`[data-post-id="${postId}"] .like-btn`);
        const likeCount = document.querySelector(`[data-post-id="${postId}"] .like-count`);

        if (!likeBtn || !likeCount) return;

        const isLiked = likeBtn.classList.contains('liked');

        try {
            // TODO: Заменить на реальный API когда будет готова сущность Like
            const response = await fetch(`/wall/api/posts/${postId}/like`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ liked: !isLiked })
            });

            if (response.ok) {
                const data = await response.json();

                // Обновляем UI через WallPosts
                this.wallPosts.togglePostLikeState(postId);
                this.wallPosts.updatePostLikeCount(postId, data.likesCount);

            } else {
                throw new Error('Ошибка при лайке');
            }
        } catch (error) {
            WallUtils.logError('toggleLike', error);

            // Временная заглушка - локальное изменение
            this.handleLikeLocally(postId, isLiked, likeCount);

            WallUtils.showNotification(
                isLiked ? 'Лайк убран' : 'Лайк поставлен',
                'info'
            );
        }
    }

    /**
     * Локальная обработка лайка (заглушка)
     */
    handleLikeLocally(postId, wasLiked, likeCountElement) {
        const currentCount = parseInt(likeCountElement.textContent);
        const newCount = wasLiked ? currentCount - 1 : currentCount + 1;

        this.wallPosts.togglePostLikeState(postId);
        this.wallPosts.updatePostLikeCount(postId, Math.max(0, newCount));
    }

    /**
     * Показать комментарии
     */
    showComments(postId) {
        // TODO: Реализовать когда будет готова сущность Comment
        WallUtils.showNotification('Комментарии будут доступны позже', 'info');

        // Пример будущего функционала:
        // this.openCommentsModal(postId);
        // или
        // window.location.href = `/wall/posts/${postId}/comments`;
    }

    /**
     * Удаление поста
     */
    async deletePost(postId) {
        const confirmMessage = this.getDeleteConfirmMessage();

        if (!WallUtils.confirm(confirmMessage)) {
            return;
        }

        try {
            const response = await fetch(`/wall/api/posts/${postId}`, {
                method: 'DELETE'
            });

            if (!response.ok) {
                throw new Error('Ошибка удаления поста');
            }

            // Удаляем пост через WallPosts
            this.wallPosts.removePost(postId);

            WallUtils.showNotification('Пост удален', 'success');

        } catch (error) {
            WallUtils.logError('deletePost', error);
            WallUtils.showNotification('Ошибка при удалении поста', 'error');
        }
    }

    /**
     * Получить сообщение подтверждения удаления
     */
    getDeleteConfirmMessage() {
        return this.currentUserId == this.wallOwnerId
            ? 'Удалить эту запись со своей стены?'
            : 'Удалить свой пост с этой стены?';
    }

    /**
     * Редактирование поста (будущий функционал)
     */
    editPost(postId) {
        // TODO: Реализовать редактирование постов
        WallUtils.showNotification('Функция редактирования будет добавлена позже', 'info');
    }

    /**
     * Жалоба на пост
     */
    reportPost(postId) {
        if (WallUtils.confirm('Пожаловаться на этот пост?')) {
            // TODO: Реализовать систему жалоб
            WallUtils.showNotification('Жалоба отправлена', 'success');
        }
    }

    /**
     * Поделиться постом
     */
    sharePost(postId) {
        const postUrl = `${window.location.origin}/wall/posts/${postId}`;

        if (navigator.share) {
            // Используем Web Share API если доступен
            navigator.share({
                title: 'Пост на стене',
                url: postUrl
            }).catch(error => {
                WallUtils.logError('sharePost', error);
                this.copyToClipboard(postUrl);
            });
        } else {
            // Копируем в буфер обмена
            this.copyToClipboard(postUrl);
        }
    }

    /**
     * Копирование в буфер обмена
     */
    async copyToClipboard(text) {
        try {
            await navigator.clipboard.writeText(text);
            WallUtils.showNotification('Ссылка скопирована в буфер обмена', 'success');
        } catch (error) {
            WallUtils.logError('copyToClipboard', error);
            WallUtils.showNotification('Не удалось скопировать ссылку', 'error');
        }
    }

    /**
     * Открыть модальное окно с комментариями (будущий функционал)
     */
    openCommentsModal(postId) {
        // TODO: Создать модальное окно для комментариев
        console.log('Opening comments modal for post:', postId);
    }

    /**
     * Закрыть модальное окно комментариев
     */
    closeCommentsModal() {
        // TODO: Закрыть модальное окно
        console.log('Closing comments modal');
    }

    /**
     * Добавить комментарий
     */
    async addComment(postId, commentText) {
        try {
            const response = await fetch(`/wall/api/posts/${postId}/comments`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ text: commentText })
            });

            if (!response.ok) {
                throw new Error('Ошибка добавления комментария');
            }

            const comment = await response.json();
            WallUtils.showNotification('Комментарий добавлен', 'success');

            // TODO: Обновить счетчик комментариев в посте
            return comment;

        } catch (error) {
            WallUtils.logError('addComment', error);
            WallUtils.showNotification('Ошибка при добавлении комментария', 'error');
            throw error;
        }
    }

    /**
     * Удалить комментарий
     */
    async deleteComment(commentId) {
        if (!WallUtils.confirm('Удалить комментарий?')) {
            return;
        }

        try {
            const response = await fetch(`/wall/api/comments/${commentId}`, {
                method: 'DELETE'
            });

            if (!response.ok) {
                throw new Error('Ошибка удаления комментария');
            }

            WallUtils.showNotification('Комментарий удален', 'success');

            // TODO: Обновить UI после удаления комментария

        } catch (error) {
            WallUtils.logError('deleteComment', error);
            WallUtils.showNotification('Ошибка при удалении комментария', 'error');
        }
    }
}