// wall-interactions.js - Взаимодействия с постами (лайки, комментарии)

/**
 * Класс для управления взаимодействиями с постами
 */
class WallInteractions {
    constructor() {
        // Можно добавить инициализацию если нужно
    }

    /**
     * Переключает лайк поста
     * @param {number} postId - ID поста
     */
    async toggleLike(postId) {
        const likeBtn = document.querySelector(`[data-post-id="${postId}"] .like-btn`);
        const likeCount = document.querySelector(`[data-post-id="${postId}"] .like-count`);

        if (!likeBtn || !likeCount) {
            console.error('Элементы лайка не найдены');
            return;
        }

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

                // Обновляем UI
                likeBtn.classList.toggle('liked');
                likeCount.textContent = data.likesCount;

                // Анимация
                this.animateLikeButton(likeBtn);
            } else {
                throw new Error('Ошибка при лайке');
            }
        } catch (error) {
            console.error('Ошибка лайка:', error);

            // Временная заглушка - просто меняем счетчик
            const currentCount = parseInt(likeCount.textContent);
            const newCount = isLiked ? currentCount - 1 : currentCount + 1;

            likeBtn.classList.toggle('liked');
            likeCount.textContent = newCount;

            // Анимация
            this.animateLikeButton(likeBtn);

            showNotification(isLiked ? 'Лайк убран' : 'Лайк поставлен', 'info');
        }
    }

    /**
     * Анимирует кнопку лайка
     * @param {HTMLElement} likeBtn - Кнопка лайка
     */
    animateLikeButton(likeBtn) {
        likeBtn.style.transform = 'scale(1.2)';
        setTimeout(() => {
            likeBtn.style.transform = 'scale(1)';
        }, 200);
    }

    /**
     * Показывает комментарии к посту
     * @param {number} postId - ID поста
     */
    showComments(postId) {
        // TODO: Реализовать когда будет готова сущность Comment
        showNotification('Комментарии будут доступны позже', 'info');

        // Пример будущего функционала:
        // window.location.href = `/wall/posts/${postId}/comments`;
        // или открыть модальное окно с комментариями
    }

    /**
     * Открывает модальное окно с комментариями (заготовка)
     * @param {number} postId - ID поста
     */
    async openCommentsModal(postId) {
        // Заготовка для будущего функционала
        try {
            // const response = await fetch(`/wall/api/posts/${postId}/comments`);
            // const comments = await response.json();

            // Создать и показать модальное окно с комментариями
            console.log(`Открытие комментариев для поста ${postId}`);

        } catch (error) {
            console.error('Ошибка загрузки комментариев:', error);
            showNotification('Ошибка загрузки комментариев', 'error');
        }
    }

    /**
     * Добавляет новый комментарий (заготовка)
     * @param {number} postId - ID поста
     * @param {string} text - Текст комментария
     */
    async addComment(postId, text) {
        // Заготовка для будущего функционала
        try {
            // const response = await fetch(`/wall/api/posts/${postId}/comments`, {
            //   method: 'POST',
            //   headers: {
            //     'Content-Type': 'application/json'
            //   },
            //   body: JSON.stringify({ text })
            // });

            // if (response.ok) {
            //   const newComment = await response.json();
            //   // Обновить UI с новым комментарием
            //   showNotification('Комментарий добавлен', 'success');
            // }

        } catch (error) {
            console.error('Ошибка добавления комментария:', error);
            showNotification('Ошибка добавления комментария', 'error');
        }
    }

    /**
     * Удаляет комментарий (заготовка)
     * @param {number} commentId - ID комментария
     */
    async deleteComment(commentId) {
        // Заготовка для будущего функционала
        if (!confirm('Удалить комментарий?')) return;

        try {
            // const response = await fetch(`/wall/api/comments/${commentId}`, {
            //   method: 'DELETE'
            // });

            // if (response.ok) {
            //   // Удалить комментарий из DOM
            //   showNotification('Комментарий удален', 'success');
            // }

        } catch (error) {
            console.error('Ошибка удаления комментария:', error);
            showNotification('Ошибка удаления комментария', 'error');
        }
    }
}