// wall-form.js - Управление формой создания постов

/**
 * Класс для управления формой создания постов
 */
class WallForm {
    constructor(wallOwnerId, onPostCreated) {
        this.wallOwnerId = wallOwnerId;
        this.onPostCreated = onPostCreated; // Колбэк для обработки созданного поста

        this.form = document.getElementById('createPostForm');
        this.textArea = document.getElementById('postText');
        this.fileInput = document.getElementById('postImage');
        this.filePreview = document.getElementById('filePreview');
        this.previewImage = document.getElementById('previewImage');
        this.submitBtn = document.getElementById('submitBtn');

        this.init();
    }

    /**
     * Инициализирует форму
     */
    init() {
        if (!this.form) return;

        this.setupForm();
        this.setupTextareaResize();
        this.setupFileInput();
    }

    /**
     * Настраивает обработчик отправки формы
     */
    setupForm() {
        this.form.addEventListener('submit', async (e) => {
            e.preventDefault();
            await this.submitPost();
        });
    }

    /**
     * Настраивает автоматическое изменение размера textarea
     */
    setupTextareaResize() {
        if (!this.textArea) return;

        this.textArea.addEventListener('input', () => {
            this.textArea.style.height = 'auto';
            this.textArea.style.height = Math.min(this.textArea.scrollHeight, 120) + 'px';
        });
    }

    /**
     * Настраивает обработчик файлового инпута
     */
    setupFileInput() {
        if (!this.fileInput) return;

        this.fileInput.addEventListener('change', (e) => {
            this.previewFile(e.target);
        });
    }

    /**
     * Отправляет пост на сервер
     */
    async submitPost() {
        const formData = new FormData(this.form);
        const originalText = this.submitBtn.textContent;

        // Показываем состояние загрузки
        this.setLoadingState(true);

        try {
            const response = await fetch(`/wall/api/${this.wallOwnerId}/posts`, {
                method: 'POST',
                body: formData
            });

            if (!response.ok) {
                throw new Error('Ошибка создания поста');
            }

            const newPost = await response.json();

            // Вызываем колбэк для обработки нового поста
            if (this.onPostCreated) {
                this.onPostCreated(newPost);
            }

            // Очищаем форму
            this.resetForm();

            showNotification('Пост успешно опубликован!', 'success');

        } catch (error) {
            console.error('Ошибка создания поста:', error);
            showNotification('Ошибка при создании поста', 'error');
        } finally {
            this.setLoadingState(false);
        }
    }

    /**
     * Устанавливает состояние загрузки для кнопки
     * @param {boolean} loading - Состояние загрузки
     */
    setLoadingState(loading) {
        if (!this.submitBtn) return;

        if (loading) {
            this.submitBtn.textContent = 'Публикация...';
            this.submitBtn.disabled = true;
        } else {
            this.submitBtn.textContent = 'Опубликовать';
            this.submitBtn.disabled = false;
        }
    }

    /**
     * Показывает предпросмотр выбранного файла
     * @param {HTMLInputElement} input - Файловый инпут
     */
    previewFile(input) {
        const file = input.files[0];

        if (!file) {
            this.hideFilePreview();
            return;
        }

        if (!file.type.startsWith('image/')) {
            showNotification('Можно загружать только изображения', 'error');
            input.value = '';
            return;
        }

        // Проверяем размер файла (например, максимум 10MB)
        const maxSize = 10 * 1024 * 1024; // 10MB
        if (file.size > maxSize) {
            showNotification('Размер файла не должен превышать 10MB', 'error');
            input.value = '';
            return;
        }

        const reader = new FileReader();
        reader.onload = (e) => {
            this.showFilePreview(e.target.result);
        };
        reader.onerror = () => {
            showNotification('Ошибка чтения файла', 'error');
        };
        reader.readAsDataURL(file);
    }

    /**
     * Показывает предпросмотр файла
     * @param {string} src - URL изображения
     */
    showFilePreview(src) {
        if (!this.filePreview || !this.previewImage) return;

        this.previewImage.src = src;
        this.filePreview.style.display = 'block';
    }

    /**
     * Скрывает предпросмотр файла
     */
    hideFilePreview() {
        if (!this.filePreview) return;
        this.filePreview.style.display = 'none';
    }

    /**
     * Удаляет выбранный файл
     */
    removeFile() {
        if (this.fileInput) {
            this.fileInput.value = '';
        }
        this.hideFilePreview();
    }

    /**
     * Сбрасывает форму к первоначальному состоянию
     */
    resetForm() {
        if (this.form) {
            this.form.reset();
        }

        if (this.textArea) {
            this.textArea.style.height = 'auto';
        }

        this.hideFilePreview();
    }

    /**
     * Проверяет, заполнена ли форма
     * @returns {boolean} True если форма заполнена
     */
    isFormFilled() {
        const hasText = this.textArea && this.textArea.value.trim().length > 0;
        const hasFile = this.fileInput && this.fileInput.files.length > 0;

        return hasText || hasFile;
    }

    /**
     * Устанавливает фокус на textarea
     */
    focus() {
        if (this.textArea) {
            this.textArea.focus();
        }
    }
}

// Глобальные функции для обратной совместимости
function previewFile(input) {
    if (window.wallForm) {
        window.wallForm.previewFile(input);
    }
}

function removeFile() {
    if (window.wallForm) {
        window.wallForm.removeFile();
    }
}