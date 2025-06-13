
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
