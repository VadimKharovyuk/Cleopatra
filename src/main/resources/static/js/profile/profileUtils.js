// profileUtils.js - Модуль утилит для профиля

class ProfileUtils {
    constructor() {
        this.userId = /*[[${user.id}]]*/ 0;
        this.userName = /*[[${user.firstName ?: 'пользователя'}]]*/ 'пользователя';
    }

    // Поделиться профилем
    async shareProfile() {
        const profileUrl = window.location.href;

        // Проверяем поддержку Web Share API
        if (navigator.share) {
            try {
                await navigator.share({
                    title: `Профиль ${this.userName} - Cleopatra`,
                    text: `Посмотрите профиль ${this.userName} в Cleopatra`,
                    url: profileUrl
                });
            } catch (err) {
                console.log('Ошибка при sharing:', err);
            }
        } else {
            // Fallback - копируем в буфер обмена
            try {
                await navigator.clipboard.writeText(profileUrl);
                window.notificationManager.show('Ссылка на профиль скопирована в буфер обмена', 'success');
            } catch (error) {
                // Если и это не работает, показываем ссылку
                prompt('Скопируйте ссылку на профиль:', profileUrl);
            }
        }
    }

    // Показать статистику профиля
    async showProfileStats() {
        window.notificationManager.show('Загружаем статистику профиля...', 'info');

        try {
            const data = await window.apiClient.getUserStats(this.userId);

            if (data.success) {
                this.showStatsModal(data.stats);
            } else {
                window.notificationManager.show('Не удалось загрузить статистику', 'error');
            }
        } catch (error) {
            console.error('Ошибка при загрузке статистики:', error);
            // Fallback - перенаправляем на страницу статистики
            window.location.href = `/profile/${this.userId}/stats`;
        }
    }

    // Показать модальное окно со статистикой
    showStatsModal(stats) {
        const modal = document.createElement('div');
        modal.className = 'stats-modal';
        modal.style.cssText = `
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: rgba(0, 0, 0, 0.5);
            z-index: 10000;
            display: flex;
            align-items: center;
            justify-content: center;
        `;

        modal.innerHTML = `
            <div style="
                background: var(--bg-secondary);
                border-radius: var(--radius-xl);
                padding: 2rem;
                max-width: 500px;
                width: 90%;
                box-shadow: var(--shadow-xl);
            ">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem;">
                    <h2 style="margin: 0; font-family: 'Playfair Display', serif;">Статистика профиля</h2>
                    <button onclick="this.closest('.stats-modal').remove()" style="
                        background: none;
                        border: none;
                        font-size: 1.5rem;
                        cursor: pointer;
                        color: var(--text-muted);
                    ">×</button>
                </div>

                <div style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 1rem;">
                    <div style="text-align: center; padding: 1rem; background: var(--bg-accent); border-radius: var(--radius-md);">
                        <div style="font-size: 2rem; font-weight: 700; color: var(--accent-primary);">${stats.profileViews || 0}</div>
                        <div style="color: var(--text-secondary);">Просмотры профиля</div>
                    </div>

                    <div style="text-align: center; padding: 1rem; background: var(--bg-accent); border-radius: var(--radius-md);">
                        <div style="font-size: 2rem; font-weight: 700; color: var(--accent-primary);">${stats.postViews || 0}</div>
                        <div style="color: var(--text-secondary);">Просмотры постов</div>
                    </div>

                    <div style="text-align: center; padding: 1rem; background: var(--bg-accent); border-radius: var(--radius-md);">
                        <div style="font-size: 2rem; font-weight: 700; color: var(--accent-success);">${stats.totalLikes || 0}</div>
                        <div style="color: var(--text-secondary);">Всего лайков</div>
                    </div>

                    <div style="text-align: center; padding: 1rem; background: var(--bg-accent); border-radius: var(--radius-md);">
                        <div style="font-size: 2rem; font-weight: 700; color: var(--accent-warning);">${stats.avgEngagement || 0}%</div>
                        <div style="color: var(--text-secondary);">Вовлеченность</div>
                    </div>
                </div>

                <div style="margin-top: 1.5rem; text-align: center;">
                    <a href="/profile/${this.userId}/stats" style="
                        color: var(--accent-primary);
                        text-decoration: none;
                        font-weight: 500;
                    ">Подробная статистика →</a>
                </div>
            </div>
        `;

        document.body.appendChild(modal);

        // Закрываем по клику на overlay
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                modal.remove();
            }
        });
    }
}

// Создаем глобальный экземпляр
window.profileUtils = new ProfileUtils();

// Экспортируем функции для обратной совместимости
window.shareProfile = () => window.profileUtils.shareProfile();
window.showProfileStats = () => window.profileUtils.showProfileStats();
window.showStatsModal = (stats) => window.profileUtils.showStatsModal(stats);