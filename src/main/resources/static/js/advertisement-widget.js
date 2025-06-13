// // Обновленный файл advertisement-widget.js
//
// class AdvertisementWidget {
//     constructor() {
//         this.loadAdvertisement();
//     }
//
//     async loadAdvertisement() {
//         console.log('Загружаем рекламу...');
//
//         try {
//             const response = await fetch('/advertisements/api/random');
//
//             console.log('Ответ сервера:', response.status, response.statusText);
//
//             if (response.ok) {
//                 // Проверяем, есть ли содержимое в ответе
//                 const contentType = response.headers.get('content-type');
//
//                 if (contentType && contentType.includes('application/json')) {
//                     const ad = await response.json();
//                     console.log('Получена реклама:', ad);
//
//                     if (ad && ad.id) {
//                         this.displayAdvertisement(ad);
//                         this.registerView(ad.id);
//                     } else {
//                         console.log('Реклама пустая или нет ID');
//                         this.hideWidget();
//                     }
//                 } else {
//                     console.log('Ответ не JSON или пустой');
//                     this.hideWidget();
//                 }
//             } else {
//                 console.error('Ошибка сервера:', response.status, response.statusText);
//
//                 // Пытаемся получить детали ошибки
//                 try {
//                     const errorText = await response.text();
//                     console.error('Детали ошибки:', errorText);
//                 } catch (e) {
//                     console.error('Не удалось прочитать ошибку');
//                 }
//
//                 this.hideWidget();
//             }
//         } catch (error) {
//             console.error('Ошибка загрузки рекламы:', error);
//             this.hideWidget();
//         }
//     }
//
//     displayAdvertisement(ad) {
//         console.log('Отображаем рекламу:', ad.title);
//
//         const adContent = document.getElementById('ad-content');
//         if (!adContent) {
//             console.error('Контейнер ad-content не найден');
//             return;
//         }
//
//         adContent.innerHTML = `
//             <div class="ad-container" data-ad-id="${ad.id}">
//                 <div class="ad-label">Реклама</div>
//                 <div class="ad-body">
//                     ${ad.imageUrl ? `<img src="${ad.imageUrl}" alt="${ad.title}" class="ad-image">` : ''}
//                     <div class="ad-text">
//                         <h4 class="ad-title">${this.escapeHtml(ad.title)}</h4>
//                         <p class="ad-description">${this.escapeHtml(ad.shortDescription)}</p>
//                     </div>
//                 </div>
//                 <div class="ad-footer">
//                     <a href="${ad.url}" target="_blank" class="ad-link" onclick="window.advertisementWidget.registerClick(${ad.id}); return true;">
//                         Перейти →
//                     </a>
//                     <span class="ad-category">${ad.category}</span>
//                 </div>
//             </div>
//         `;
//     }
//
//     async registerView(adId) {
//         console.log('Регистрируем просмотр:', adId);
//
//         try {
//             const response = await fetch(`/advertisements/api/${adId}/view`, {
//                 method: 'POST',
//                 headers: {
//                     'Content-Type': 'application/json',
//                 }
//             });
//
//             if (response.ok) {
//                 console.log('Просмотр зарегистрирован');
//             } else {
//                 console.warn('Ошибка регистрации просмотра:', response.status);
//             }
//         } catch (error) {
//             console.error('Ошибка регистрации просмотра:', error);
//         }
//     }
//
//     async registerClick(adId) {
//         console.log('Регистрируем клик:', adId);
//
//         try {
//             const response = await fetch(`/advertisements/api/${adId}/click`, {
//                 method: 'POST',
//                 headers: {
//                     'Content-Type': 'application/json',
//                 }
//             });
//
//             if (response.ok) {
//                 console.log('Клик зарегистрирован');
//             } else {
//                 console.warn('Ошибка регистрации клика:', response.status);
//             }
//         } catch (error) {
//             console.error('Ошибка регистрации клика:', error);
//         }
//     }
//
//     hideWidget() {
//         console.log('Скрываем виджет');
//
//         const widget = document.getElementById('advertisement-widget');
//         if (widget) {
//             widget.style.display = 'none';
//         }
//     }
//
//     escapeHtml(text) {
//         if (!text) return '';
//
//         const map = {
//             '&': '&amp;',
//             '<': '&lt;',
//             '>': '&gt;',
//             '"': '&quot;',
//             "'": '&#039;'
//         };
//         return text.replace(/[&<>"']/g, m => map[m]);
//     }
// }
//
// // Инициализация
// document.addEventListener('DOMContentLoaded', () => {
//     console.log('DOM загружен, инициализируем виджет');
//
//     if (document.getElementById('advertisement-widget')) {
//         window.advertisementWidget = new AdvertisementWidget();
//     } else {
//         console.warn('Контейнер advertisement-widget не найден');
//     }
// });
//
// // Если DOM уже загружен
// if (document.readyState !== 'loading') {
//     console.log('DOM уже загружен');
//     if (document.getElementById('advertisement-widget')) {
//         window.advertisementWidget = new AdvertisementWidget();
//     }
// }

<!-- JavaScript для загрузки рекламы в ленте -->

    class FeedAdvertisementManager {
    constructor() {
    this.loadedAds = new Set();
    this.init();
}

    init() {
    // Находим все контейнеры рекламы в ленте
    const adContainers = document.querySelectorAll('.ad-widget.ad-post-item');

    // Загружаем рекламу в каждый контейнер с небольшой задержкой
    adContainers.forEach((container, index) => {
    setTimeout(() => {
    this.loadAdvertisementForContainer(container);
}, index * 200); // Задержка между загрузками
});
}

    async loadAdvertisementForContainer(container) {
    try {
    const response = await fetch('/advertisements/api/random');

    if (response.ok) {
    const ad = await response.json();
    if (ad && ad.id) {
    this.displayAdvertisement(container, ad);
    this.registerView(ad.id);
} else {
    this.hideContainer(container);
}
} else {
    this.hideContainer(container);
}
} catch (error) {
    console.error('Ошибка загрузки рекламы:', error);
    this.hideContainer(container);
}
}

    displayAdvertisement(container, ad) {
    const adContent = container.querySelector('.ad-content');

    adContent.innerHTML = `
            <div class="ad-container" data-ad-id="${ad.id}">
                <div class="ad-label">
                    <i class="fas fa-bullhorn"></i>
                    Спонсируемое
                </div>
                <div class="ad-body">
                    ${ad.imageUrl ? `<img src="${ad.imageUrl}" alt="${ad.title}" class="ad-image">` : ''}
                    <div class="ad-text">
                        <h4 class="ad-title">${this.escapeHtml(ad.title)}</h4>
                        <p class="ad-description">${this.escapeHtml(ad.shortDescription)}</p>
                    </div>
                </div>
                <div class="ad-footer">
                    <a href="${ad.url}" 
                       target="_blank" 
                       class="ad-link" 
                       onclick="feedAdManager.registerClick(${ad.id}); return true;">
                        Подробнее →
                    </a>
                    <span class="ad-category">${this.escapeHtml(ad.category)}</span>
                </div>
            </div>
        `;
}

    async registerView(adId) {
    if (this.loadedAds.has(adId)) return; // Избегаем дублирования

    this.loadedAds.add(adId);

    try {
    await fetch(`/advertisements/api/${adId}/view`, {
    method: 'POST',
    headers: {
    'Content-Type': 'application/json',
}
});
    console.log(`Просмотр рекламы ${adId} зарегистрирован`);
} catch (error) {
    console.error('Ошибка регистрации просмотра:', error);
}
}

    async registerClick(adId) {
    try {
    await fetch(`/advertisements/api/${adId}/click`, {
    method: 'POST',
    headers: {
    'Content-Type': 'application/json',
}
});
    console.log(`Клик по рекламе ${adId} зарегистрирован`);
} catch (error) {
    console.error('Ошибка регистрации клика:', error);
}
}

    hideContainer(container) {
    container.style.display = 'none';
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

    // Инициализация после загрузки DOM
    document.addEventListener('DOMContentLoaded', () => {
    window.feedAdManager = new FeedAdvertisementManager();
});
