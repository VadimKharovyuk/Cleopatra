////виджет для рекламы
    class AdvertisementWidget {
    constructor() {
    this.loadAdvertisement();
}

    async loadAdvertisement() {
    try {
    const response = await fetch('/advertisements/api/random');
    if (response.ok) {
    const ad = await response.json();
    if (ad && ad.id) {
    this.displayAdvertisement(ad);
    this.registerView(ad.id);
} else {
    this.hideWidget();
}
} else {
    this.hideWidget();
}
} catch (error) {
    console.error('Ошибка загрузки рекламы:', error);
    this.hideWidget();
}
}

    displayAdvertisement(ad) {
    const adContent = document.getElementById('ad-content');

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
                            <a href="${ad.url}" target="_blank" class="ad-link" onclick="advertisementWidget.registerClick(${ad.id})">
                                Перейти →
                            </a>
                            <span class="ad-category">${ad.category}</span>
                        </div>
                    </div>
                `;
}

    async registerView(adId) {
    try {
    await fetch(`/advertisements/api/${adId}/view`, {
    method: 'POST',
    headers: {
    'Content-Type': 'application/json',
}
});
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
} catch (error) {
    console.error('Ошибка регистрации клика:', error);
}
}

    hideWidget() {
    const widget = document.getElementById('advertisement-widget');
    widget.style.display = 'none';
}

    escapeHtml(text) {
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

    // Инициализация виджета при загрузке страницы
    document.addEventListener('DOMContentLoaded', () => {
    window.advertisementWidget = new AdvertisementWidget();
});
