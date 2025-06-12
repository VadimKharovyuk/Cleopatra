// Обновленный файл advertisement-widget.js

class AdvertisementWidget {
    constructor() {
        this.loadAdvertisement();
    }

    async loadAdvertisement() {
        console.log('Загружаем рекламу...');

        try {
            const response = await fetch('/advertisements/api/random');

            console.log('Ответ сервера:', response.status, response.statusText);

            if (response.ok) {
                // Проверяем, есть ли содержимое в ответе
                const contentType = response.headers.get('content-type');

                if (contentType && contentType.includes('application/json')) {
                    const ad = await response.json();
                    console.log('Получена реклама:', ad);

                    if (ad && ad.id) {
                        this.displayAdvertisement(ad);
                        this.registerView(ad.id);
                    } else {
                        console.log('Реклама пустая или нет ID');
                        this.hideWidget();
                    }
                } else {
                    console.log('Ответ не JSON или пустой');
                    this.hideWidget();
                }
            } else {
                console.error('Ошибка сервера:', response.status, response.statusText);

                // Пытаемся получить детали ошибки
                try {
                    const errorText = await response.text();
                    console.error('Детали ошибки:', errorText);
                } catch (e) {
                    console.error('Не удалось прочитать ошибку');
                }

                this.hideWidget();
            }
        } catch (error) {
            console.error('Ошибка загрузки рекламы:', error);
            this.hideWidget();
        }
    }

    displayAdvertisement(ad) {
        console.log('Отображаем рекламу:', ad.title);

        const adContent = document.getElementById('ad-content');
        if (!adContent) {
            console.error('Контейнер ad-content не найден');
            return;
        }

        adContent.innerHTML = `
            <div class="ad-container" data-ad-id="${ad.id}">
                <div class="ad-label">Реклама</div>
                <div class="ad-body">
                    ${ad.imageUrl ? `<img src="${ad.imageUrl}" alt="${ad.title}" class="ad-image">` : ''}
                    <div class="ad-text">
                        <h4 class="ad-title">${this.escapeHtml(ad.title)}</h4>
                        <p class="ad-description">${this.escapeHtml(ad.shortDescription)}</p>
                    </div>
                </div>
                <div class="ad-footer">
                    <a href="${ad.url}" target="_blank" class="ad-link" onclick="window.advertisementWidget.registerClick(${ad.id}); return true;">
                        Перейти →
                    </a>
                    <span class="ad-category">${ad.category}</span>
                </div>
            </div>
        `;
    }

    async registerView(adId) {
        console.log('Регистрируем просмотр:', adId);

        try {
            const response = await fetch(`/advertisements/api/${adId}/view`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                }
            });

            if (response.ok) {
                console.log('Просмотр зарегистрирован');
            } else {
                console.warn('Ошибка регистрации просмотра:', response.status);
            }
        } catch (error) {
            console.error('Ошибка регистрации просмотра:', error);
        }
    }

    async registerClick(adId) {
        console.log('Регистрируем клик:', adId);

        try {
            const response = await fetch(`/advertisements/api/${adId}/click`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                }
            });

            if (response.ok) {
                console.log('Клик зарегистрирован');
            } else {
                console.warn('Ошибка регистрации клика:', response.status);
            }
        } catch (error) {
            console.error('Ошибка регистрации клика:', error);
        }
    }

    hideWidget() {
        console.log('Скрываем виджет');

        const widget = document.getElementById('advertisement-widget');
        if (widget) {
            widget.style.display = 'none';
        }
    }

    escapeHtml(text) {
        if (!text) return '';

        const map = {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#039;'
        };
        return text.replace(/[&<>"']/g, m => map[m]);
    }
}

// Инициализация
document.addEventListener('DOMContentLoaded', () => {
    console.log('DOM загружен, инициализируем виджет');

    if (document.getElementById('advertisement-widget')) {
        window.advertisementWidget = new AdvertisementWidget();
    } else {
        console.warn('Контейнер advertisement-widget не найден');
    }
});

// Если DOM уже загружен
if (document.readyState !== 'loading') {
    console.log('DOM уже загружен');
    if (document.getElementById('advertisement-widget')) {
        window.advertisementWidget = new AdvertisementWidget();
    }
}