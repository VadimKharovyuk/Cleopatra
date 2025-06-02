// ========================================
// AJAX AVATAR UPLOADER MODULE
// Готовый модуль для загрузки аватара через REST API
// ========================================

class AjaxAvatarUploader {
    constructor(options = {}) {
        this.config = {
            // API endpoints
            apiBaseUrl: '/api/profile',

            // DOM элементы
            formId: 'avatarUploadForm',
            fileInputId: 'avatarFile',
            uploadBtnId: 'avatarUploadBtn',
            previewId: 'avatarPreview',
            placeholderId: 'avatarPlaceholder',

            // Настройки
            maxFileSize: 10, // MB
            allowedTypes: ['image/jpeg', 'image/jpg', 'image/png'],

            // Переопределяем настройками
            ...options
        };

        this.isUploading = false;
        console.log('🚀 AjaxAvatarUploader инициализирован', this.config);
    }

    // ========================================
    // ОСНОВНОЙ МЕТОД ЗАГРУЗКИ
    // ========================================

    async uploadAvatar() {
        if (this.isUploading) {
            console.warn('⚠️ Загрузка уже в процессе');
            return;
        }

        const fileInput = document.getElementById(this.config.fileInputId);
        const uploadBtn = document.getElementById(this.config.uploadBtnId);

        if (!fileInput || !fileInput.files || !fileInput.files[0]) {
            this.showError('Пожалуйста, выберите файл');
            return;
        }

        const file = fileInput.files[0];
        console.log('📁 Выбран файл аватара:', {
            name: file.name,
            size: `${(file.size / 1024 / 1024).toFixed(2)} MB`,
            type: file.type
        });

        // Валидация файла
        if (!this.validateFile(file)) {
            fileInput.value = '';
            return;
        }

        // Показываем превью
        this.showPreview(file);

        // Начинаем загрузку
        this.setLoadingState(uploadBtn, true);
        this.isUploading = true;

        try {
            const userId = this.getCurrentUserId();
            if (!userId) {
                throw new Error('Не удалось определить ID пользователя');
            }

            const result = await this.uploadToServer(userId, file);

            if (result.success) {
                this.handleUploadSuccess(result);
            } else {
                this.handleUploadError(result.message || 'Ошибка загрузки');
            }

        } catch (error) {
            console.error('❌ Ошибка при загрузке аватара:', error);
            this.handleUploadError(error.message || 'Ошибка соединения с сервером');
        } finally {
            this.setLoadingState(uploadBtn, false);
            this.isUploading = false;
        }
    }

    // ========================================
    // ВАЛИДАЦИЯ ФАЙЛА
    // ========================================

    validateFile(file) {
        // Проверка размера
        if (file.size > this.config.maxFileSize * 1024 * 1024) {
            const fileSizeMB = (file.size / 1024 / 1024).toFixed(2);
            this.showError(`Файл слишком большой: ${fileSizeMB} MB. Максимум: ${this.config.maxFileSize} MB`);
            return false;
        }

        // Проверка пустого файла
        if (file.size === 0) {
            this.showError('Файл пустой или поврежден');
            return false;
        }

        // Проверка типа
        if (!this.config.allowedTypes.includes(file.type.toLowerCase())) {
            this.showError(`Неподдерживаемый формат: ${file.type}. Поддерживаются: JPG, PNG`);
            return false;
        }

        console.log('✅ Файл прошел валидацию');
        return true;
    }

    // ========================================
    // ЗАГРУЗКА НА СЕРВЕР
    // ========================================

    async uploadToServer(userId, file) {
        const formData = new FormData();
        formData.append('avatar', file);

        const url = `${this.config.apiBaseUrl}/${userId}/avatar`;
        console.log('📤 Отправка на:', url);

        const response = await fetch(url, {
            method: 'POST',
            body: formData,
            headers: {
                // Добавляем CSRF токен если используется
                'X-Requested-With': 'XMLHttpRequest'
            }
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        const result = await response.json();
        console.log('📥 Ответ сервера:', result);
        return result;
    }

    // ========================================
    // ОБРАБОТКА РЕЗУЛЬТАТОВ
    // ========================================

    handleUploadSuccess(result) {
        console.log('✅ Аватар успешно загружен');

        // Обновляем UI
        if (result.user && result.user.imageUrl) {
            this.updateAvatarOnPage(result.user.imageUrl);
        }

        // Показываем уведомление
        this.showSuccess(result.message || 'Аватар успешно обновлен!');

        // Вызываем callback если есть
        if (this.config.onSuccess) {
            this.config.onSuccess(result);
        }
    }

    handleUploadError(message) {
        console.error('❌ Ошибка загрузки аватара:', message);
        this.showError(message);

        // Вызываем callback если есть
        if (this.config.onError) {
            this.config.onError(message);
        }
    }

    // ========================================
    // ОБНОВЛЕНИЕ UI
    // ========================================

    updateAvatarOnPage(newImageUrl) {
        // Обновляем превью в форме
        const preview = document.getElementById(this.config.previewId);
        const placeholder = document.getElementById(this.config.placeholderId);

        if (preview) {
            preview.src = newImageUrl + '?t=' + Date.now(); // Cache busting
            preview.style.display = 'block';
        }

        if (placeholder) {
            placeholder.style.display = 'none';
        }

        // Обновляем аватар в других местах страницы
        const avatarSelectors = [
            '.sidebar .user-avatar img',
            '.header .avatar img',
            '.profile-avatar img',
            '.nav-user-avatar img'
        ];

        avatarSelectors.forEach(selector => {
            const elements = document.querySelectorAll(selector);
            elements.forEach(el => {
                el.src = newImageUrl + '?t=' + Date.now();
            });
        });

        console.log('🔄 Аватар обновлен на странице:', newImageUrl);
    }

    showPreview(file) {
        const preview = document.getElementById(this.config.previewId);
        const placeholder = document.getElementById(this.config.placeholderId);

        if (!preview) return;

        const reader = new FileReader();
        reader.onload = (e) => {
            preview.src = e.target.result;
            preview.style.display = 'block';
            if (placeholder) {
                placeholder.style.display = 'none';
            }
        };
        reader.readAsDataURL(file);
    }

    setLoadingState(button, isLoading) {
        if (!button) return;

        if (isLoading) {
            button.disabled = true;
            button.classList.add('loading');
            button.dataset.originalText = button.innerHTML;
            button.innerHTML = '<i class="fas fa-spinner fa-spin"></i> <span>Загружаем...</span>';
        } else {
            button.disabled = false;
            button.classList.remove('loading');
            if (button.dataset.originalText) {
                button.innerHTML = button.dataset.originalText;
                delete button.dataset.originalText;
            }
        }
    }

    // ========================================
    // УВЕДОМЛЕНИЯ
    // ========================================

    showSuccess(message) {
        this.showNotification(message, 'success');
    }

    showError(message) {
        this.showNotification(message, 'error');
    }

    showNotification(message, type = 'info') {
        // Удаляем предыдущие уведомления
        document.querySelectorAll('.ajax-notification').forEach(n => n.remove());

        const notification = document.createElement('div');
        notification.className = `ajax-notification alert-luxury alert-${type === 'success' ? 'success' : 'danger'}-luxury`;
        notification.innerHTML = `
            <i class="fas fa-${type === 'success' ? 'check-circle' : 'exclamation-circle'}"></i>
            <span>${message}</span>
            <button type="button" class="alert-close" onclick="this.parentElement.remove()">
                <i class="fas fa-times"></i>
            </button>
        `;

        // Добавляем стили
        notification.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            z-index: 10000;
            min-width: 300px;
            animation: slideInRight 0.3s ease;
        `;

        document.body.appendChild(notification);

        // Автоудаление
        setTimeout(() => {
            if (notification.parentNode) {
                notification.style.animation = 'slideOutRight 0.3s ease';
                setTimeout(() => notification.remove(), 300);
            }
        }, 5000);
    }

    // ========================================
    // УТИЛИТЫ
    // ========================================

    getCurrentUserId() {
        // Способ 1: из URL
        const path = window.location.pathname;
        const match = path.match(/\/profile\/(\d+)/);
        if (match) return match[1];

        // Способ 2: из глобальной переменной
        if (window.currentUserId) return window.currentUserId;

        // Способ 3: из meta тега
        const meta = document.querySelector('meta[name="user-id"]');
        if (meta) return meta.content;

        // Способ 4: из data атрибута формы
        const form = document.getElementById(this.config.formId);
        if (form && form.dataset.userId) return form.dataset.userId;

        console.error('❌ Не удалось определить ID пользователя');
        return null;
    }

    // ========================================
    // ИНИЦИАЛИЗАЦИЯ
    // ========================================

    init() {
        const fileInput = document.getElementById(this.config.fileInputId);
        if (fileInput) {
            // Привязываем обработчик изменения файла
            fileInput.addEventListener('change', () => {
                if (fileInput.files && fileInput.files[0]) {
                    // Автоматическая загрузка при выборе файла
                    this.uploadAvatar();
                }
            });

            console.log('✅ Обработчик файла аватара установлен');
        }

        // Добавляем CSS анимации
        this.addStyles();
    }

    addStyles() {
        if (document.getElementById('ajax-uploader-styles')) return;

        const style = document.createElement('style');
        style.id = 'ajax-uploader-styles';
        style.textContent = `
            @keyframes slideInRight {
                from { transform: translateX(100%); opacity: 0; }
                to { transform: translateX(0); opacity: 1; }
            }
            
            @keyframes slideOutRight {
                from { transform: translateX(0); opacity: 1; }
                to { transform: translateX(100%); opacity: 0; }
            }
            
            .alert-close {
                background: none;
                border: none;
                color: inherit;
                cursor: pointer;
                float: right;
                font-size: 1.2rem;
                opacity: 0.7;
                padding: 0;
                margin-left: 1rem;
                transition: opacity 0.2s ease;
            }
            
            .alert-close:hover {
                opacity: 1;
            }
        `;
        document.head.appendChild(style);
    }
}

// ========================================
// ГЛОБАЛЬНЫЕ ФУНКЦИИ ДЛЯ ОБРАТНОЙ СОВМЕСТИМОСТИ
// ========================================

// Создаем глобальный экземпляр
window.ajaxAvatarUploader = new AjaxAvatarUploader();

// Функция для замены старой uploadAvatar()
function uploadAvatarAjax() {
    return window.ajaxAvatarUploader.uploadAvatar();
}

// Инициализация при загрузке DOM
document.addEventListener('DOMContentLoaded', () => {
    window.ajaxAvatarUploader.init();
});

// ========================================
// ПРИМЕР ИСПОЛЬЗОВАНИЯ
// ========================================

/*
// 1. Базовое использование (автоматическая инициализация)
// Просто подключите этот файл - всё заработает автоматически

// 2. Кастомная конфигурация
const customUploader = new AjaxAvatarUploader({
    maxFileSize: 15, // 15 MB
    onSuccess: (result) => {
        console.log('Кастомный callback успеха:', result);
    },
    onError: (error) => {
        console.log('Кастомный callback ошибки:', error);
    }
});

// 3. Ручная загрузка
document.getElementById('myUploadBtn').addEventListener('click', () => {
    customUploader.uploadAvatar();
});

// 4. Интеграция с существующим кодом
// Замените в HTML: onchange="uploadAvatar()" на onchange="uploadAvatarAjax()"
*/