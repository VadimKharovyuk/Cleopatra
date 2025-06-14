/**
 * wall-form.js
 * Форма создания постов на стене
 */

class WallForm {

    constructor(config) {
        this.wallOwnerId = config.wallOwnerId;
        this.currentUserId = config.currentUserId;
        this.canWriteOnWall = config.canWriteOnWall;
        this.wallPosts = config.wallPosts; // Ссылка на экземпляр WallPosts

        // DOM элементы
        this.form = document.getElementById('createPostForm');
        this.textArea = document.getElementById('postText');
        this.fileInput = document.getElementById('postImage');
        this.filePreview = document.getElementById('filePreview');
        this.previewImage = document.getElementById('previewImage');
        this.submitBtn = document.getElementById('submitBtn');
    }

    /**
     * Инициализация формы
     */
    init() {
        if (!this.canWriteOnWall || !this.form) {
            return;
        }

        this.setupCreatePostForm();
        this.setupTextareaResize();
        this.setupFileInput();
    }

    /**
     * Настройка основной формы создания поста
     */
    setupCreatePostForm() {
        this.form.addEventListener('submit', async (e) => {
            await this.handleFormSubmit(e);
        });
    }

    /**
     * Обработка отправки формы
     */
    async handleFormSubmit(e) {
        e.preventDefault();

        const formData = new FormData(this.form);
        const originalText = this.submitBtn.textContent;

        // Валидация
        if (!this.validateForm(formData)) {
            return;
        }

        // Показываем состояние загрузки
        this.setSubmitButtonLoading(true, originalText);

        try {
            const response = await fetch(`/wall/api/${this.wallOwnerId}/posts`, {
                method: 'POST',
                body: formData
            });

            if (!response.ok) {
                throw new Error('Ошибка создания поста');
            }

            const newPost = await response.json();

            // Добавляем новый пост через WallPosts
            this.wallPosts.addNewPost(newPost);

            // Очищаем и сбрасываем форму
            this.resetForm();

            WallUtils.showNotification('Пост успешно опубликован!', 'success');

        } catch (error) {
            WallUtils.logError('handleFormSubmit', error);
            WallUtils.showNotification('Ошибка при создании поста', 'error');
        } finally {
            this.setSubmitButtonLoading(false, originalText);
        }
    }

    /**
     * Валидация формы
     */
    validateForm(formData) {
        const text = formData.get('text');
        const file = formData.get('image');

        // Проверяем, что есть хотя бы текст или изображение
        if (!text.trim() && (!file || file.size === 0)) {
            WallUtils.showNotification('Добавьте текст или изображение для поста', 'error');
            return false;
        }

        // Валидация файла, если он есть
        if (file && file.size > 0) {
            const validation = WallUtils.validateImageFile(file);
            if (!validation.valid) {
                WallUtils.showNotification(validation.message, 'error');
                return false;
            }
        }

        return true;
    }

    /**
     * Установка состояния загрузки для кнопки отправки
     */
    setSubmitButtonLoading(isLoading, originalText) {
        if (isLoading) {
            this.submitBtn.textContent = 'Публикация...';
            this.submitBtn.disabled = true;
        } else {
            this.submitBtn.textContent = originalText;
            this.submitBtn.disabled = false;
        }
    }

    /**
     * Сброс формы
     */
    resetForm() {
        this.form.reset();
        this.textArea.style.height = 'auto';
        this.hideFilePreview();
    }

    /**
     * Настройка автоматического изменения размера textarea
     */
    setupTextareaResize() {
        if (!this.textArea) return;

        const resizeTextarea = () => {
            this.textArea.style.height = 'auto';
            this.textArea.style.height = Math.min(this.textArea.scrollHeight, 120) + 'px';
        };

        this.textArea.addEventListener('input', resizeTextarea);

        // Также реагируем на изменения при вставке текста
        this.textArea.addEventListener('paste', () => {
            setTimeout(resizeTextarea, 0);
        });
    }

    /**
     * Настройка работы с файлами
     */
    setupFileInput() {
        if (!this.fileInput) return;

        this.fileInput.addEventListener('change', (e) => {
            this.handleFileSelect(e.target.files[0]);
        });
    }

    /**
     * Обработка выбора файла
     */
    handleFileSelect(file) {
        if (!file) {
            this.hideFilePreview();
            return;
        }

        // Валидация файла
        const validation = WallUtils.validateImageFile(file);
        if (!validation.valid) {
            WallUtils.showNotification(validation.message, 'error');
            this.clearFileInput();
            return;
        }

        /**
         * Обработка выбора файла
         */
        handleFileSelect(file) {
            if (!file) {
                this.hideFilePreview();
                return;
            }

            // Валидация файла
            const validation = WallUtils.validateImageFile(file);
            if (!validation.valid) {
                WallUtils.showNotification(validation.message, 'error');
                this.clearFileInput();
                return;
            }

            // Показываем предпросмотр
            this.showFilePreview(file);
        }(file);
    }

    /**
     * Показать предпросмотр изображения
     */
    showFilePreview(file) {
        if (!this.filePreview || !this.previewImage) return;

        const reader = new FileReader();
        reader.onload = (e) => {
            this.previewImage.src = e.target.result;
            this.filePreview.style.display = 'block';
        };
        reader.readAsDataURL(file);
    }

    /**
     * Скрыть предпросмотр изображения
     */
    hideFilePreview() {
        if (this.filePreview) {
            this.filePreview.style.display = 'none';
        }
        if (this.previewImage) {
            this.previewImage.src = '';
        }
    }

    /**
     * Удалить выбранный файл
     */
    removeFile() {
        this.clearFileInput();
        this.hideFilePreview();
    }

    /**
     * Очистить input файла
     */
    clearFileInput() {
        if (this.fileInput) {
            this.fileInput.value = '';
        }
    }

    /**
     * Получить текст из textarea
     */
    getTextContent() {
        return this.textArea ? this.textArea.value.trim() : '';
    }

    /**
     * Установить текст в textarea
     */
    setTextContent(text) {
        if (this.textArea) {
            this.textArea.value = text;
            // Триггерим событие для автоматического изменения размера
            this.textArea.dispatchEvent(new Event('input'));
        }
    }

    /**
     * Проверить, есть ли контент в форме
     */
    hasContent() {
        const hasText = this.getTextContent().length > 0;
        const hasFile = this.fileInput && this.fileInput.files.length > 0;
        return hasText || hasFile;
    }

    /**
     * Фокус на поле ввода текста
     */
    focusTextArea() {
        if (this.textArea) {
            this.textArea.focus();
        }
    }

    /**
     * Установить placeholder для textarea
     */
    setPlaceholder(placeholder) {
        if (this.textArea) {
            this.textArea.placeholder = placeholder;
        }
    }

    /**
     * Заблокировать/разблокировать форму
     */
    setFormDisabled(disabled) {
        if (this.textArea) {
            this.textArea.disabled = disabled;
        }
        if (this.fileInput) {
            this.fileInput.disabled = disabled;
        }
        if (this.submitBtn) {
            this.submitBtn.disabled = disabled;
        }
    }

    /**
     * Добавить обработчик на изменение текста
     */
    onTextChange(callback) {
        if (this.textArea && typeof callback === 'function') {
            this.textArea.addEventListener('input', callback);
        }
    }

    /**
     * Добавить обработчик на изменение файла
     */
    onFileChange(callback) {
        if (this.fileInput && typeof callback === 'function') {
            this.fileInput.addEventListener('change', callback);
        }
    }

    /**
     * Получить данные формы
     */
    getFormData() {
        const formData = new FormData(this.form);
        return {
            text: formData.get('text'),
            image: formData.get('image')
        };
    }

    /**
     * Проверить, активна ли форма
     */
    isActive() {
        return this.canWriteOnWall && this.form;
    }

    /**
     * Уничтожить обработчики событий (cleanup)
     */
    destroy() {
        // Удаляем обработчики событий если нужно
        // В данном случае они привязаны к элементам, которые будут удалены
        // автоматически при удалении DOM элементов
    }
}

// Глобальная функция для удаления файла (вызывается из HTML)
function removeFile() {
    if (window.wallForm) {
        window.wallForm.removeFile();
    }
}

// Глобальная функция для предпросмотра файла (вызывается из HTML)
function previewFile(input) {
    if (window.wallForm && input.files.length > 0) {
        window.wallForm.handleFileSelect(input.files[0]);
    }
}