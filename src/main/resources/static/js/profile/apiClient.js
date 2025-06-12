// // apiClient.js - Модуль для работы с API
//
// class ApiClient {
//     constructor() {
//         this.baseHeaders = {
//             'Content-Type': 'application/json',
//             'X-Requested-With': 'XMLHttpRequest'
//         };
//     }
//
//     // Базовый метод для выполнения запросов
//     async request(url, options = {}) {
//         const config = {
//             headers: { ...this.baseHeaders, ...options.headers },
//             ...options
//         };
//
//         try {
//             const response = await fetch(url, config);
//             return await response.json();
//         } catch (error) {
//             console.error('API Error:', error);
//             throw error;
//         }
//     }
//
//     // Подписка на пользователя
//     async followUser(userId) {
//         return this.request(`/api/users/${userId}/follow`, { method: 'POST' });
//     }
//
//     // Отписка от пользователя
//     async unfollowUser(userId) {
//         return this.request(`/api/users/${userId}/unfollow`, { method: 'POST' });
//     }
//
//     // Блокировка пользователя
//     async blockUser(userId) {
//         return this.request(`/api/users/${userId}/block`, { method: 'POST' });
//     }
//
//     // Получение статистики пользователя
//     async getUserStats(userId) {
//         return this.request(`/api/users/${userId}/stats`, { method: 'GET' });
//     }
// }
//
// // Создаем глобальный экземпляр
// window.apiClient = new ApiClient();


// ===== 2. ИСПРАВЛЕНИЕ apiClient.js =====
class ApiClient {
    static async request(url, options = {}) {
        try {
            const response = await fetch(url, {
                headers: {
                    'Content-Type': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest',
                    ...options.headers
                },
                ...options
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            return await response.json();
        } catch (error) {
            console.error('API Error:', error);
            throw error;
        }
    }

    static async get(url) {
        return this.request(url, { method: 'GET' });
    }

    static async post(url, data) {
        return this.request(url, {
            method: 'POST',
            body: JSON.stringify(data)
        });
    }

    static async put(url, data) {
        return this.request(url, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
    }

    static async delete(url) {
        return this.request(url, { method: 'DELETE' });
    }

    static init() {
        console.log('✅ ApiClient инициализирован');
    }
}

// ✅ ОБЯЗАТЕЛЬНЫЙ ЭКСПОРТ
window.ApiClient = ApiClient;
