document.addEventListener('DOMContentLoaded', function() {
    // Обработка кнопок подписки в рекомендациях
    document.querySelectorAll('.btn-follow').forEach(button => {
        button.addEventListener('click', async function() {
            const userId = this.dataset.userId;
            const isFollowing = this.textContent.trim() === 'Читаю';

            try {
                // Здесь будет AJAX запрос для подписки/отписки
                // const response = await fetch(`/api/users/${userId}/${isFollowing ? 'unfollow' : 'follow'}`, {
                //     method: 'POST',
                //     headers: { 'Content-Type': 'application/json' }
                // });

                // Пока что просто меняем текст
                this.textContent = isFollowing ? 'Читать' : 'Читаю';
                this.dataset.following = !isFollowing;

                console.log(`${isFollowing ? 'Отписались от' : 'Подписались на'} пользователя ${userId}`);
            } catch (error) {
                console.error('Ошибка при подписке:', error);
            }
        });
    });
});