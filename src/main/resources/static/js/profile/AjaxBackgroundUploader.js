// ========================================
// AJAX BACKGROUND UPLOADER MODULE
// Готовый модуль для загрузки фонового изображения через REST API
// ========================================

class AjaxBackgroundUploader {
    constructor(options = {}) {
        this.config = {
            // API endpoints
            apiBaseUrl: '/api/profile',

            // DOM элементы
            formId: 'backgroundUploadForm',
            fileInputId: 'backgroundFile',
            uploadBtnId: 'backgroundUploadBtn',
            previewId: 'backgroundImagePreview',
            placeholderId: 'backgroundPlaceholder',

            // Настройки
            maxFileSize: 10, // MB
            allowedTypes: ['image/jpeg', 'image/jpg', 'image/png'],
            minFileSize: 50, // KB - минимум для фона

            // Переопределяем настройками
            ...options
        };

        this.isUploading = false;
        console.log('🌄 AjaxBackgroundUploader инициализирован', this.config);
    }

    // ========================================
    // ОСНОВНОЙ МЕТОД ЗАГРУЗКИ
    // ========================================

    async uploadBackground() {
        if (this.isUploading) {
            console.warn('⚠️ Загрузка фона уже в процессе');
            return;
        }

        const fileInput = document.getElementById(this.config.fileInputId);
        const uploadBtn = document.getElementById(this.config.uploadBtnId);

        if (!fileInput || !fileInput.files || !fileInput.files[0]) {
            this.showError('Пожалуйста, выберите файл для фона');
            return;
        }

        const file = fileInput.files[0];
        console.log('🖼️ Выбран файл фона:', {
            name: file.name,
            size: `${(file.size / 1024 / 1024).toFixed(2)} MB`,
            type: file.type,
            dimensions: 'будут определены при загрузке'
        });

        // Валидация файла
        if (!this.validateFile(file)) {
            fileInput.value = '';
            return;
        }

        // Показываем превью
        await this.showPreview(file);

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
                this.handleUploadError(result.message || 'Ошибка загрузки фона');
            }

        } catch (error) {
            console.error('❌ Ошибка при загрузке фона:', error);
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
        // Проверка размера (максимум)
        if (file.size > this.config.maxFileSize * 1024 * 1024) {
            const fileSizeMB = (file.size / 1024 / 1024).toFixed(2);
            this.showError(`Файл слишком большой: ${fileSizeMB} MB. Максимум: ${this.config.maxFileSize} MB`);
            return false;
        }

        // Проверка размера (минимум) - фон должен быть достаточно большим
        if (file.size < this.config.minFileSize * 1024) {
            const fileSizeKB = (file.size / 1024).toFixed(0);
            this.showError(`Файл слишком маленький: ${fileSizeKB} KB. Минимум: ${this.config.minFileSize} KB`);
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

        console.log('✅ Файл фона прошел валидацию');
        return true;
    }

    // ========================================
    // ВАЛИДАЦИЯ РАЗМЕРОВ ИЗОБРАЖЕНИЯ
    // ========================================

    async validateImageDimensions(file) {
        return new Promise((resolve) => {
            const img = new Image();
            const url = URL.createObjectURL(file);

            img.onload = () => {
                URL.revokeObjectURL(url);

                const width = img.naturalWidth;
                const height = img.naturalHeight;
                const ratio = width / height;

                console.log('📏 Размеры изображения:', { width, height, ratio: ratio.toFixed(2) });

                // Рекомендуемые размеры для фона
                const minWidth = 800;
                const minHeight = 400;
                const idealRatio = 3; // 3:1 как у Twitter/X
                const maxRatio = 4; // Максимальное соотношение
                const minRatio = 2; // Минимальное соотношение

                // Проверки
                if (width < minWidth || height < minHeight) {
                    this.showWarning(`Рекомендуемый минимальный размер: ${minWidth}x${minHeight}px. У вас: ${width}x${height}px`);
                }

                if (ratio < minRatio || ratio > maxRatio) {
                    this.showWarning(`Рекомендуемое соотношение сторон: 2:1 - 4:1. У вас: ${ratio.toFixed(2)}:1`);
                }

                resolve({
                    width,
                    height,
                    ratio,
                    isOptimal: width >= minWidth && height >= minHeight && ratio >= minRatio && ratio <= maxRatio
                });
            };

            img.onerror = () => {
                URL.revokeObjectURL(url);
                resolve(null);
            };

            img.src = url;
        });
    }

    // ========================================
    // ЗАГРУЗКА НА СЕРВЕР
    // ========================================

    async uploadToServer(userId, file) {
        const formData = new FormData();
        formData.append('background', file);

        const url = `${this.config.apiBaseUrl}/${userId}/background`;
        console.log('📤 Отправка фона на:', url);

        const response = await fetch(url, {
            method: 'POST',
            body: formData,
            headers: {
                'X-Requested-With': 'XMLHttpRequest'
            }
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        const result = await response.json();
        console.log('📥 Ответ сервера (фон):', result);
        return result;
    }

    // ========================================
    // ОБРАБОТКА РЕЗУЛЬТАТОВ
    // ========================================

    handleUploadSuccess(result) {
        console.log('✅ Фон успешно загружен');

        // Обновляем UI
        if (result.user && result.user.imgBackground) {
            this.updateBackgroundOnPage(result.user.imgBackground);
        }

        // Показываем уведомление
        this.showSuccess(result.message || 'Фоновое изображение успешно обновлено!');

        // Вызываем callback если есть
        if (this.config.onSuccess) {
            this.config.onSuccess(result);
        }
    }

    handleUploadError(message) {
        console.error('❌ Ошибка загрузки фона:', message);
        this.showError(message);

        // Вызываем callback если есть
        if (this.config.onError) {
            this.config.onError(message);
        }
    }

    // ========================================
    // ОБНОВЛЕНИЕ UI
    // ========================================

    updateBackgroundOnPage(newImageUrl) {
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

        // Обновляем фон в других местах
        const backgroundSelectors = [
            '.cover-section .cover-image',
            '.profile-cover img',
            '.header-background img',
            '.background-image'
        ];

        backgroundSelectors.forEach(selector => {
            const elements = document.querySelectorAll(selector);
            elements.forEach(el => {
                if (el.tagName === 'IMG') {
                    el.src = newImageUrl + '?t=' + Date.now();
                } else {
                    // Для div с background-image
                    el.style.backgroundImage = `url("${newImageUrl}?t=${Date.now()}")`;
                }
            });
        });

        console.log('🔄 Фон обновлен на странице:', newImageUrl);
    }

    async showPreview(file) {
        const preview = document.getElementById(this.config.previewId);
        const placeholder = document.getElementById(this.config.placeholderId);

        if (!preview) return;

        // Валидируем размеры изображения
        const dimensions = await this.validateImageDimensions(file);

        const reader = new FileReader();
        reader.onload = (e) => {
            preview.src = e.target.result;
            preview.style.display = 'block';
            if (placeholder) {
                placeholder.style.display = 'none';
            }

            // Добавляем информацию о размерах
            if (dimensions) {
                this.showImageInfo(dimensions);
            }
        };
        reader.readAsDataURL(file);
    }

    showImageInfo(dimensions) {
        const { width, height, ratio, isOptimal } = dimensions;

        // Создаем временное info сообщение
        const info = document.createElement('div');
        info.className = 'image-info';
        info.style.cssText = `
            position: absolute;
            top: 10px;
            left: 10px;
            background: rgba(0, 0, 0, 0.7);
            color: white;
            padding: 0.5rem;
            border-radius: 4px;
            font-size: 0.8rem;
            z-index: 100;
        `;

        info.innerHTML = `
            📏 ${width}x${height}px (${ratio.toFixed(2)}:1)
            ${isOptimal ? '✅' : '⚠️'} ${isOptimal ? 'Оптимальный размер' : 'Размер не оптимален'}
        `;

        const preview = document.getElementById(this.config.previewId);
        if (preview && preview.parentElement) {
            preview.parentElement.style.position = 'relative';
            preview.parentElement.appendChild(info);

            // Удаляем через 3 секунды
            setTimeout(() => {
                if (info.parentNode) {
                    info.remove();
                }
            }, 3000);
        }
    }

    setLoadingState(button, isLoading) {
        if (!button) return;

        if (isLoading) {
            button.disabled = true;
            button.classList.add('loading');
            button.dataset.originalText = button.innerHTML;
            button.innerHTML = '<i class="fas fa-spinner fa-spin"></i> <span>Загружаем фон...</span>';
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

    showWarning(message) {
        this.showNotification(message, 'warning');
    }

    showNotification(message, type = 'info') {
        // Удаляем предыдущие уведомления от этого модуля
        document.querySelectorAll('.ajax-background-notification').forEach(n => n.remove());

        const notification = document.createElement('div');
        notification.className = `ajax-background-notification alert-luxury alert-${this.getAlertClass(type)}-luxury`;

        const icon = this.getIconForType(type);
        notification.innerHTML = `
            <i class="fas fa-${icon}"></i>
            <span>${message}</span>
            <button type="button" class="alert-close" onclick="this.parentElement.remove()">
                <i class="fas fa-times"></i>
            </button>
        `;

        // Добавляем стили
        notification.style.cssText = `
            position: fixed;
            top: 80px;
            right: 20px;
            z-index: 10001;
            min-width: 350px;
            animation: slideInRight 0.3s ease;
        `;

        document.body.appendChild(notification);

        // Автоудаление
        const timeout = type === 'warning' ? 7000 : 5000;
        setTimeout(() => {
            if (notification.parentNode) {
                notification.style.animation = 'slideOutRight 0.3s ease';
                setTimeout(() => notification.remove(), 300);
            }
        }, timeout);
    }

    getAlertClass(type) {
        const classes = {
            success: 'success',
            error: 'danger',
            warning: 'warning',
            info: 'info'
        };
        return classes[type] || 'info';
    }

    getIconForType(type) {
        const icons = {
            success: 'check-circle',
            error: 'exclamation-circle',
            warning: 'exclamation-triangle',
            info: 'info-circle'
        };
        return icons[type] || 'info-circle';
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
    // ДОПОЛНИТЕЛЬНЫЕ УТИЛИТЫ ДЛЯ ФОНА
    // ========================================

    // Получение оптимальных размеров для фона
    getOptimalDimensions() {
        return {
            recommended: { width: 1500, height: 500 },
            minimum: { width: 800, height: 400 },
            ratios: { min: 2, max: 4, ideal: 3 }
        };
    }

    // Проверка поддержки современных форматов
    supportsWebP() {
        const canvas = document.createElement('canvas');
        canvas.width = 1;
        canvas.height = 1;
        return canvas.toDataURL('image/webp').indexOf('image/webp') === 5;
    }

    // Сжатие изображения (если размер слишком большой)
    async compressImage(file, maxWidth = 1920, quality = 0.8) {
        return new Promise((resolve) => {
            const canvas = document.createElement('canvas');
            const ctx = canvas.getContext('2d');
            const img = new Image();

            img.onload = () => {
                const { width, height } = img;

                // Вычисляем новые размеры
                let newWidth = width;
                let newHeight = height;

                if (width > maxWidth) {
                    newWidth = maxWidth;
                    newHeight = (height * maxWidth) / width;
                }

                canvas.width = newWidth;
                canvas.height = newHeight;

                // Рисуем сжатое изображение
                ctx.drawImage(img, 0, 0, newWidth, newHeight);

                // Конвертируем в blob
                canvas.toBlob(resolve, 'image/jpeg', quality);
            };

            img.src = URL.createObjectURL(file);
        });
    }

    // Получение превью в Base64 для кеширования
    async getPreviewBase64(file, maxWidth = 400) {
        return new Promise((resolve) => {
            const canvas = document.createElement('canvas');
            const ctx = canvas.getContext('2d');
            const img = new Image();

            img.onload = () => {
                const { width, height } = img;
                const ratio = width / height;

                let newWidth = Math.min(width, maxWidth);
                let newHeight = newWidth / ratio;

                canvas.width = newWidth;
                canvas.height = newHeight;

                ctx.drawImage(img, 0, 0, newWidth, newHeight);
                resolve(canvas.toDataURL('image/jpeg', 0.7));
            };

            img.src = URL.createObjectURL(file);
        });
    }

    // ========================================
    // РАСШИРЕННАЯ ВАЛИДАЦИЯ
    // ========================================

    async validateAdvanced(file) {
        const dimensions = await this.validateImageDimensions(file);
        const analysis = {
            fileValid: this.validateFile(file),
            dimensions: dimensions,
            recommendations: []
        };

        if (dimensions) {
            const optimal = this.getOptimalDimensions();

            // Анализ размеров
            if (dimensions.width < optimal.minimum.width || dimensions.height < optimal.minimum.height) {
                analysis.recommendations.push({
                    type: 'warning',
                    message: `Изображение меньше рекомендуемого размера. Минимум: ${optimal.minimum.width}x${optimal.minimum.height}px`
                });
            }

            // Анализ соотношения сторон
            if (dimensions.ratio < optimal.ratios.min) {
                analysis.recommendations.push({
                    type: 'info',
                    message: 'Изображение слишком квадратное. Рекомендуется более широкий формат.'
                });
            } else if (dimensions.ratio > optimal.ratios.max) {
                analysis.recommendations.push({
                    type: 'info',
                    message: 'Изображение слишком вытянутое. Рекомендуется менее широкий формат.'
                });
            }

            // Анализ качества
            const megapixels = (dimensions.width * dimensions.height) / 1000000;
            if (megapixels < 0.5) {
                analysis.recommendations.push({
                    type: 'warning',
                    message: 'Низкое разрешение изображения может привести к размытости.'
                });
            }
        }

        return analysis;
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
                    this.uploadBackground();
                }
            });

            console.log('✅ Обработчик файла фона установлен');
        }

        // Добавляем CSS анимации (если еще не добавлены)
        this.addStyles();

        // Показываем совет по оптимальным размерам
        this.showOptimalDimensionsHint();

        // Инициализируем drag & drop если нужно
        if (this.config.enableDragDrop !== false) {
            this.initDragDrop();
        }
    }

    initDragDrop() {
        const dropZone = document.getElementById(this.config.previewId)?.parentElement;
        if (!dropZone) return;

        dropZone.addEventListener('dragover', (e) => {
            e.preventDefault();
            dropZone.classList.add('drag-over');
        });

        dropZone.addEventListener('dragleave', (e) => {
            e.preventDefault();
            dropZone.classList.remove('drag-over');
        });

        dropZone.addEventListener('drop', (e) => {
            e.preventDefault();
            dropZone.classList.remove('drag-over');

            const files = e.dataTransfer.files;
            if (files.length > 0) {
                const fileInput = document.getElementById(this.config.fileInputId);
                if (fileInput) {
                    fileInput.files = files;
                    this.uploadBackground();
                }
            }
        });

        console.log('✅ Drag & Drop инициализирован для фона');
    }

    showOptimalDimensionsHint() {
        const uploadInfo = document.querySelector('.upload-info .upload-hint');
        if (uploadInfo) {
            const optimal = this.getOptimalDimensions();
            const originalText = uploadInfo.innerHTML;
            uploadInfo.innerHTML = `${originalText}<br>
                <small style="color: var(--text-muted); font-size: 0.8em;">
                    💡 Рекомендуемый размер: ${optimal.recommended.width}x${optimal.recommended.height}px
                </small>`;
        }
    }

    addStyles() {
        if (document.getElementById('ajax-background-uploader-styles')) return;

        const style = document.createElement('style');
        style.id = 'ajax-background-uploader-styles';
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
            
            .image-info {
                font-family: 'Inter', sans-serif;
                transition: opacity 0.3s ease;
                pointer-events: none;
            }
            
            .image-info:hover {
                opacity: 0.9;
            }
            
            /* Стили для предупреждений */
            .alert-warning-luxury {
                background: linear-gradient(135deg, rgba(217, 119, 6, 0.1), rgba(245, 158, 11, 0.1));
                color: var(--accent-warning);
                border-left: 4px solid var(--accent-warning);
            }
            
            .alert-info-luxury {
                background: linear-gradient(135deg, rgba(59, 130, 246, 0.1), rgba(96, 165, 250, 0.1));
                color: var(--accent-info);
                border-left: 4px solid var(--accent-info);
            }
            
            /* Drag & Drop стили */
            .drag-over {
                border: 2px dashed var(--accent-primary) !important;
                background: rgba(37, 99, 235, 0.1) !important;
                transform: scale(1.02);
                transition: all 0.3s ease;
            }
            
            /* Анимация загрузки */
            .loading-background {
                position: relative;
                overflow: hidden;
            }
            
            .loading-background::after {
                content: '';
                position: absolute;
                top: 0;
                left: -100%;
                width: 100%;
                height: 100%;
                background: linear-gradient(90deg, transparent, rgba(255,255,255,0.4), transparent);
                animation: shimmer 2s infinite;
            }
            
            @keyframes shimmer {
                0% { left: -100%; }
                100% { left: 100%; }
            }
        `;
        document.head.appendChild(style);
    }

    // ========================================
    // ДОПОЛНИТЕЛЬНЫЕ МЕТОДЫ
    // ========================================

    // Метод для ручного обновления конфигурации
    updateConfig(newConfig) {
        this.config = { ...this.config, ...newConfig };
        console.log('🔧 Конфигурация обновлена:', this.config);
    }

    // Метод для получения информации о текущем состоянии
    getStatus() {
        return {
            isUploading: this.isUploading,
            config: this.config,
            hasPreview: !!document.getElementById(this.config.previewId)?.src,
            userId: this.getCurrentUserId()
        };
    }

    // Метод для очистки (cleanup)
    destroy() {
        const fileInput = document.getElementById(this.config.fileInputId);
        if (fileInput) {
            fileInput.removeEventListener('change', this.uploadBackground);
        }

        // Удаляем уведомления
        document.querySelectorAll('.ajax-background-notification').forEach(n => n.remove());

        console.log('🗑️ AjaxBackgroundUploader уничтожен');
    }
}

// ========================================
// ГЛОБАЛЬНЫЕ ФУНКЦИИ ДЛЯ ОБРАТНОЙ СОВМЕСТИМОСТИ
// ========================================

// Создаем глобальный экземпляр
window.ajaxBackgroundUploader = new AjaxBackgroundUploader();

// Функция для замены старой uploadBackground()
function uploadBackgroundAjax() {
    return window.ajaxBackgroundUploader.uploadBackground();
}

// Инициализация при загрузке DOM
document.addEventListener('DOMContentLoaded', () => {
    window.ajaxBackgroundUploader.init();
});

// ========================================
// ЭКСПОРТ ДЛЯ МОДУЛЬНЫХ СИСТЕМ
// ========================================

// Поддержка CommonJS
if (typeof module !== 'undefined' && module.exports) {
    module.exports = AjaxBackgroundUploader;
}

// Поддержка AMD
if (typeof define === 'function' && define.amd) {
    define([], function() {
        return AjaxBackgroundUploader;
    });
}

// ========================================
// ПРИМЕР РАСШИРЕННОГО ИСПОЛЬЗОВАНИЯ
// ========================================

/*
// 1. Базовое использование (автоматическая инициализация)
// Просто подключите этот файл - всё заработает автоматически

// 2. Кастомная конфигурация с расширенными возможностями
const advancedUploader = new AjaxBackgroundUploader({
    maxFileSize: 20, // 20 MB
    minFileSize: 100, // 100 KB
    enableDragDrop: true, // Drag & Drop поддержка
    validateDimensions: true,
    onSuccess: (result) => {
        console.log('✅ Фон загружен успешно:', result);

        // Дополнительные действия
        updatePageTheme(result.user.imgBackground);
        saveToLocalStorage('lastBackground', result.user.imgBackground);
        trackAnalytics('background_uploaded');
    },
    onError: (error) => {
        console.log('❌ Ошибка загрузки фона:', error);

        // Дополнительная обработка
        reportError('background_upload_failed', error);
        showFallbackOptions();
    }
});

// 3. Программное управление
document.getElementById('compressAndUpload').addEventListener('click', async () => {
    const fileInput = document.getElementById('backgroundFile');
    const file = fileInput.files[0];

    if (file) {
        // Сжимаем перед загрузкой
        const compressed = await advancedUploader.compressImage(file, 1920, 0.8);

        // Загружаем сжатое изображение
        advancedUploader.uploadBackground();
    }
});

// 4. Расширенная валидация
const validator = new AjaxBackgroundUploader({
    validateAdvanced: true
});

document.getElementById('backgroundFile').addEventListener('change', async (e) => {
    const file = e.target.files[0];
    if (file) {
        const analysis = await validator.validateAdvanced(file);

        if (analysis.recommendations.length > 0) {
            analysis.recommendations.forEach(rec => {
                validator.showNotification(rec.message, rec.type);
            });
        }
    }
});

// 5. Интеграция с прогресс баром
const progressUploader = new AjaxBackgroundUploader({
    onProgress: (percent) => {
        document.getElementById('uploadProgress').style.width = percent + '%';
    },
    onSuccess: () => {
        document.getElementById('uploadProgress').style.width = '100%';
        setTimeout(() => {
            document.getElementById('uploadProgress').style.width = '0%';
        }, 1000);
    }
});

// 6. Множественные экземпляры для разных форм
const mainUploader = new AjaxBackgroundUploader({
    formId: 'mainBackgroundForm',
    fileInputId: 'mainBackgroundFile'
});

const modalUploader = new AjaxBackgroundUploader({
    formId: 'modalBackgroundForm',
    fileInputId: 'modalBackgroundFile'
});
*/