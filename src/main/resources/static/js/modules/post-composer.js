// static/js/modules/post-composer.js

class PostComposer {
    static init() {
        console.log('🔧 Инициализация composer...');

        const postContent = document.getElementById('postContent');
        const postButton = document.getElementById('postButton');
        const charCount = document.getElementById('charCount');
        const imageInput = document.getElementById('imageInput');
        const createPostForm = document.getElementById('createPostForm');

        if (!postContent || !postButton || !createPostForm) {
            console.error('❌ Не найдены обязательные элементы формы!');
            return;
        }

        // Счетчик символов и валидация
        postContent.addEventListener('input', function() {
            PostComposer.updateCharCount(this, charCount, postButton);
        });

        // Обработка загрузки изображения
        if (imageInput) {
            imageInput.addEventListener('change', PostComposer.handleImageSelect);
        }

        // Отправка формы
        createPostForm.addEventListener('submit', PostComposer.handleFormSubmit);

        console.log('✅ Инициализация завершена');
    }

    static updateCharCount(textElement, charCount, postButton) {
        const length = textElement.value.length;
        const maxLength = 1000;

        if (charCount) {
            charCount.textContent = length;

            const counter = charCount.parentElement;
            counter.className = 'char-counter';
            if (length > maxLength * 0.9) {
                counter.classList.add('warning');
            }
            if (length > maxLength * 0.95) {
                counter.classList.remove('warning');
                counter.classList.add('danger');
            }
        }

        const isValid = length > 0 && length <= maxLength;
        postButton.disabled = !isValid;
    }

    static selectImage() {
        const imageInput = document.getElementById('imageInput');
        if (imageInput) {
            imageInput.click();
        }
    }

    static handleImageSelect(event) {
        const file = event.target.files[0];
        if (!file) return;

        console.log('📁 Обрабатываем файл:', file.name, file.type, file.size);

        const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/heif', 'image/heic'];
        if (!allowedTypes.includes(file.type)) {
            NotificationManager.show('Неподдерживаемый формат файла. Используйте JPG, PNG, HEIF или HEIC.', 'error');
            event.target.value = '';
            return;
        }

        const maxSize = 10 * 1024 * 1024;
        if (file.size > maxSize) {
            NotificationManager.show('Файл слишком большой. Максимальный размер: 10MB.', 'error');
            event.target.value = '';
            return;
        }

        const reader = new FileReader();
        reader.onload = function(e) {
            const previewImg = document.getElementById('previewImg');
            const imagePreview = document.getElementById('imagePreview');

            if (previewImg && imagePreview) {
                previewImg.src = e.target.result;
                imagePreview.style.display = 'block';
                console.log('✅ Превью показано');
            }
        };
        reader.readAsDataURL(file);
    }

    static removeImage() {
        console.log('🗑️ Удаляем изображение...');
        const imageInput = document.getElementById('imageInput');
        const imagePreview = document.getElementById('imagePreview');

        if (imageInput) imageInput.value = '';
        if (imagePreview) imagePreview.style.display = 'none';
    }

    static async handleFormSubmit(event) {
        event.preventDefault();
        console.log('🚀 Начинаем отправку формы...');

        const postContent = document.getElementById('postContent');
        const imageInput = document.getElementById('imageInput');

        const content = postContent.value.trim();
        if (!content) {
            NotificationManager.show('Введите текст поста', 'error');
            return;
        }

        PostComposer.setLoadingState(true);

        try {
            const formData = new FormData();
            formData.append('content', content);

            if (imageInput && imageInput.files[0]) {
                formData.append('image', imageInput.files[0]);
                console.log('📎 Добавлено изображение к запросу');
            }

            // Собираем данные геолокации
            const locationId = document.getElementById('locationId').value.trim();
            const latitude = document.getElementById('latitude').value.trim();
            const longitude = document.getElementById('longitude').value.trim();

            console.log('📍 Проверяем геолокацию перед отправкой:');
            console.log('LocationId:', locationId);
            console.log('Latitude:', latitude);
            console.log('Longitude:', longitude);

            // Добавляем геолокацию в FormData
            if (locationId && locationId !== '') {
                formData.append('locationId', locationId);
                console.log('📍 Добавлен locationId:', locationId);
            } else if (latitude && longitude && latitude !== '' && longitude !== '') {
                formData.append('latitude', latitude);
                formData.append('longitude', longitude);
                console.log('📍 Добавлены координаты:', latitude, longitude);
            }

            console.log('📤 Отправляем запрос на /posts/api/create');

            const response = await fetch('/posts/api/create', {
                method: 'POST',
                body: formData
            });

            console.log('📥 Получен ответ:', response.status, response.statusText);

            if (response.ok) {
                const result = await response.json();
                console.log('✅ Пост создан:', result);

                if (result.location) {
                    console.log('✅ Геолокация сохранена:', result.location);
                    NotificationManager.show('Пост с геолокацией успешно создан!', 'success');
                } else {
                    console.log('ℹ️ Пост создан без геолокации');
                    NotificationManager.show('Пост успешно создан!', 'success');
                }

                PostComposer.resetForm();

                setTimeout(() => {
                    location.reload();
                }, 1000);

            } else {
                const errorText = await response.text();
                console.error('❌ Ошибка ответа:', errorText);
                NotificationManager.show('Ошибка при создании поста: ' + response.status, 'error');
            }
        } catch (error) {
            console.error('❌ Ошибка при создании поста:', error);
            NotificationManager.show('Произошла ошибка при создании поста', 'error');
        } finally {
            PostComposer.setLoadingState(false);
        }
    }

    static setLoadingState(loading) {
        const postButton = document.getElementById('postButton');
        const btnText = postButton?.querySelector('.btn-text');
        const btnSpinner = postButton?.querySelector('.btn-spinner');

        if (!postButton) return;

        postButton.disabled = loading;

        if (loading) {
            if (btnText) btnText.style.opacity = '0';
            if (btnSpinner) btnSpinner.style.display = 'inline-block';
        } else {
            if (btnText) btnText.style.opacity = '1';
            if (btnSpinner) btnSpinner.style.display = 'none';
        }
    }

    static resetForm() {
        console.log('🔄 Сбрасываем форму...');
        const postContent = document.getElementById('postContent');
        const imageInput = document.getElementById('imageInput');
        const imagePreview = document.getElementById('imagePreview');
        const charCount = document.getElementById('charCount');
        const postButton = document.getElementById('postButton');

        if (postContent) postContent.value = '';
        if (imageInput) imageInput.value = '';
        if (imagePreview) imagePreview.style.display = 'none';
        if (charCount) {
            charCount.textContent = '0';
            charCount.parentElement.className = 'char-counter';
        }
        if (postButton) postButton.disabled = true;

        // Очищаем геолокацию
        GeolocationManager.clearLocation();

        // Скрываем секцию геолокации
        const controls = document.getElementById('locationControls');
        const toggleText = document.getElementById('locationToggleText');
        if (controls) controls.classList.remove('active');
        if (toggleText) toggleText.textContent = 'Добавить местоположение';
    }
}

// Глобальные функции для совместимости с HTML onclick
window.selectImage = () => PostComposer.selectImage();
window.removeImage = () => PostComposer.removeImage();

window.PostComposer = PostComposer;