// apiClient.js - Модуль для работы с API

class ApiClient {
    constructor() {
        this.baseHeaders = {
            'Content-Type': 'application/json',
            'X-Requested-With': 'XMLHttpRequest'
        };
    }

    // Базовый метод для выполнения запросов
    async request(url, options = {}) {
        const config = {
            headers: { ...this.baseHeaders, ...options.headers },
            ...options
        };

        try {
            const response = await fetch(url, config);
            return await response.json();
        } catch (error) {
            console.error('API Error:', error);
            throw error;
        }
    }

    // Подписка на пользователя
    async followUser(userId) {
        return this.request(`/api/users/${userId}/follow`, { method: 'POST' });
    }

    // Отписка от пользователя
    async unfollowUser(userId) {
        return this.request(`/api/users/${userId}/unfollow`, { method: 'POST' });
    }

    // Блокировка пользователя
    async blockUser(userId) {
        return this.request(`/api/users/${userId}/block`, { method: 'POST' });
    }

    // Получение статистики пользователя
    async getUserStats(userId) {
        return this.request(`/api/users/${userId}/stats`, { method: 'GET' });
    }
}

// Создаем глобальный экземпляр
window.apiClient = new ApiClient();