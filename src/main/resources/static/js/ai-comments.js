/**
 * AI Comments Integration
 * Интеграция AI для генерации и улучшения комментариев
 */

// Глобальные переменные для AI
let aiServiceAvailable = false;
let lastGeneratedComment = '';
let commentToImprove = '';

// Конфигурация AI
const AI_CONFIG = {
    maxPromptLength: 200,
    retryAttempts: 3,
    timeoutMs: 30000,
    debounceDelay: 300
};

// Кэш для AI данных
const aiCache = {
    templates: null,
    examples: null,
    lastCheck: null
};

/**
 * Инициализация AI функций
 */
document.addEventListener('DOMContentLoaded', function() {
    initializeAIComments();
});

/**
 * Основная инициализация
 */
async function initializeAIComments() {
    console.log('🤖 Инициализация AI комментариев...');

    try {
        await checkAIServiceStatus();
        setupAIEventListeners();
        addImproveButtonsToComments();
        setupPromptValidation();
        preloadAIData();

        console.log('✅ AI комментарии успешно инициализированы');
    } catch (error) {
        console.error('❌ Ошибка инициализации AI комментариев:', error);
    }
}

/**
 * Проверка статуса AI сервиса
 */
async function checkAIServiceStatus() {
    try {
        const response = await fetch(`/api/posts/${postId}/comments/ai/status`);
        const data = await response.json();

        aiServiceAvailable = data.aiServiceAvailable;
        aiCache.lastCheck = Date.now();

        const statusBadge = document.getElementById('ai-status-badge');
        const aiSection = document.querySelector('.ai-assistant-section');

        if (!statusBadge || !aiSection) {
            console.warn('AI элементы не найдены в DOM');
            return;
        }

        if (aiServiceAvailable) {
            statusBadge.textContent = 'Готов';
            statusBadge.className = 'badge bg-success text-white ms-2';
            aiSection.style.display = 'block';
            aiSection.style.opacity = '1';
            aiSection.style.pointerEvents = 'auto';
        } else {
            statusBadge.textContent = 'Недоступен';
            statusBadge.className = 'badge bg-danger text-white ms-2';
            aiSection.style.opacity = '0.6';
            aiSection.style.pointerEvents = 'none';
        }
    } catch (error) {
        console.error('Ошибка проверки AI сервиса:', error);
        const statusBadge = document.getElementById('ai-status-badge');
        if (statusBadge) {
            statusBadge.textContent = 'Ошибка';
            statusBadge.className = 'badge bg-warning text-dark ms-2';
        }
    }
}

/**
 * Настройка обработчиков событий
 */
function setupAIEventListeners() {
    // Автосохранение типа комментария
    const commentType = document.getElementById('comment-type');
    if (commentType) {
        commentType.addEventListener('change', saveAIPreferences);
    }

    // Клавиатурные сокращения
    document.addEventListener('keydown', handleKeyboardShortcuts);
}

/**
 * Обработка клавиатурных сокращений
 */
function handleKeyboardShortcuts(event) {
    // Ctrl/Cmd + Shift + A - фокус на AI промпт
    if ((event.ctrlKey || event.metaKey) && event.shiftKey && event.key === 'A') {
        event.preventDefault();
        const promptInput = document.getElementById('ai-prompt');
        if (promptInput) {
            promptInput.focus();
            showSuccess('🤖 AI помощник активирован');
        }
    }

    // Ctrl/Cmd + Enter - генерация превью (если фокус на промпте)
    if ((event.ctrlKey || event.metaKey) && event.key === 'Enter' &&
        document.activeElement?.id === 'ai-prompt') {
        event.preventDefault();
        generateCommentPreview();
    }

    // Escape - скрыть превью
    if (event.key === 'Escape') {
        const previewSection = document.getElementById('ai-preview-section');
        if (previewSection && previewSection.style.display === 'block') {
            hidePreview();
        }
    }
}

/**
 * Настройка валидации промпта
 */
function setupPromptValidation() {
    const promptInput = document.getElementById('ai-prompt');
    if (!promptInput) return;

    // Добавляем счетчик символов
    const charCounter = document.createElement('div');
    charCounter.className = 'form-text text-end';
    charCounter.id = 'prompt-char-counter';
    promptInput.parentNode.appendChild(charCounter);

    // Debounced валидация
    let validationTimeout;
    promptInput.addEventListener('input', function() {
        clearTimeout(validationTimeout);
        validationTimeout = setTimeout(() => {
            validatePrompt(this.value);
        }, AI_CONFIG.debounceDelay);
    });
}

/**
 * Валидация промпта
 */
function validatePrompt(value) {
    const length = value.length;
    const maxLength = AI_CONFIG.maxPromptLength;
    const charCounter = document.getElementById('prompt-char-counter');
    const promptInput = document.getElementById('ai-prompt');

    if (charCounter) {
        charCounter.textContent = `${length}/${maxLength}`;
        charCounter.className = `form-text text-end ${length > maxLength ? 'text-danger' : 'text-muted'}`;
    }

    if (promptInput) {
        if (length > maxLength) {
            promptInput.classList.add('is-invalid');
        } else {
            promptInput.classList.remove('is-invalid');
        }
    }
}

/**
 * Предзагрузка AI данных
 */
async function preloadAIData() {
    try {
        const [templatesResponse, examplesResponse] = await Promise.all([
            fetch(`/api/posts/${postId}/comments/ai/templates`),
            fetch(`/api/posts/${postId}/comments/ai/examples`)
        ]);

        if (templatesResponse.ok) {
            aiCache.templates = await templatesResponse.json();
        }

        if (examplesResponse.ok) {
            aiCache.examples = await examplesResponse.json();
        }

        console.log('📁 AI данные предзагружены');
    } catch (error) {
        console.warn('Не удалось предзагрузить AI данные:', error);
    }
}

/**
 * Генерация превью комментария
 */
async function generateCommentPreview() {
    const prompt = document.getElementById('ai-prompt')?.value?.trim();
    const commentType = document.getElementById('comment-type')?.value;

    if (!prompt) {
        showError('Введите описание желаемого комментария');
        return;
    }

    if (!aiServiceAvailable) {
        showError('AI сервис недоступен');
        return;
    }

    showAILoading(true);
    hidePreview();

    try {
        const response = await fetchWithTimeout(`/api/posts/${postId}/comments/ai/preview`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                prompt: prompt,
                commentType: commentType
            })
        });

        const data = await response.json();

        if (data.success) {
            lastGeneratedComment = data.data.generatedComment;
            displayPreview(lastGeneratedComment);
            showSuccess(`Комментарий сгенерирован за ${data.data.generationTimeMs} мс`);
        } else {
            showError('Ошибка генерации: ' + data.error);
        }
    } catch (error) {
        console.error('Ошибка:', error);
        showError('Ошибка при генерации комментария: ' + error.message);
    } finally {
        showAILoading(false);
    }
}

/**
 * Отображение превью комментария
 */
function displayPreview(text) {
    const previewText = document.getElementById('ai-preview-text');
    const previewSection = document.getElementById('ai-preview-section');

    if (previewText && previewSection) {
        previewText.textContent = text;
        previewSection.style.display = 'block';

        // Плавная анимация появления
        previewSection.style.opacity = '0';
        previewSection.style.transform = 'translateY(-10px)';

        requestAnimationFrame(() => {
            previewSection.style.transition = 'all 0.3s ease';
            previewSection.style.opacity = '1';
            previewSection.style.transform = 'translateY(0)';
        });
    }
}

/**
 * Генерация и публикация комментария
 */
async function generateAndPostComment() {
    const prompt = document.getElementById('ai-prompt')?.value?.trim();
    const commentType = document.getElementById('comment-type')?.value;

    if (!prompt) {
        showError('Введите описание желаемого комментария');
        return;
    }

    if (!aiServiceAvailable) {
        showError('AI сервис недоступен');
        return;
    }

    if (!confirm('Создать и опубликовать комментарий с помощью AI?')) {
        return;
    }

    showAILoading(true);
    hidePreview();

    try {
        const response = await fetchWithTimeout(`/api/posts/${postId}/comments/ai/create`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                prompt: prompt,
                commentType: commentType
            })
        });

        const data = await response.json();

        if (data.success) {
            showSuccess('AI комментарий успешно создан и опубликован!');
            clearAIForm();
            refreshComments();
        } else {
            showError('Ошибка создания комментария: ' + data.error);
        }
    } catch (error) {
        console.error('Ошибка:', error);
        showError('Ошибка при создании комментария: ' + error.message);
    } finally {
        showAILoading(false);
    }
}

/**
 * Очистка AI формы
 */
function clearAIForm() {
    const promptInput = document.getElementById('ai-prompt');
    const commentTypeSelect = document.getElementById('comment-type');

    if (promptInput) promptInput.value = '';
    if (commentTypeSelect) commentTypeSelect.value = 'GENERAL';
}

/**
 * Обновление комментариев после создания
 */
function refreshComments() {
    // Вызываем существующие функции обновления
    if (typeof updateCommentsCount === 'function') {
        updateCommentsCount();
    }
    if (typeof loadLatestComments === 'function') {
        loadLatestComments();
    }
    if (typeof loadComments === 'function') {
        loadComments(0);
    }
}

/**
 * Использование сгенерированного комментария
 */
function useGeneratedComment() {
    if (lastGeneratedComment) {
        const commentTextarea = document.getElementById('comment-content');
        if (commentTextarea) {
            commentTextarea.value = lastGeneratedComment;
            hidePreview();
            showSuccess('Комментарий добавлен в форму. Нажмите "Отправить" для публикации.');

            // Прокручиваем к форме комментария
            commentTextarea.scrollIntoView({ behavior: 'smooth' });
            commentTextarea.focus();
        }
    }
}

/**
 * Скрытие превью
 */
function hidePreview() {
    const previewSection = document.getElementById('ai-preview-section');
    if (previewSection) {
        previewSection.style.display = 'none';
    }
}

/**
 * Показ/скрытие загрузки AI
 */
function showAILoading(show) {
    const loadingElement = document.getElementById('ai-loading');
    if (loadingElement) {
        loadingElement.style.display = show ? 'block' : 'none';
    }
}

/**
 * Показ шаблонов
 */
async function showAITemplates() {
    const modal = new bootstrap.Modal(document.getElementById('templatesModal'));
    modal.show();

    try {
        let data = aiCache.templates;

        if (!data) {
            const response = await fetch(`/api/posts/${postId}/comments/ai/templates`);
            data = await response.json();
            aiCache.templates = data;
        }

        if (data.success) {
            renderTemplates(data);
        }
    } catch (error) {
        document.getElementById('templates-content').innerHTML =
            '<div class="alert alert-danger">Ошибка загрузки шаблонов</div>';
    }
}

/**
 * Отображение шаблонов
 */
function renderTemplates(data) {
    let templatesHtml = '<div class="row">';

    Object.entries(data.templates).forEach(([key, template]) => {
        templatesHtml += `
            <div class="col-md-6 mb-3">
                <div class="card template-card h-100">
                    <div class="card-body">
                        <h6 class="card-title text-capitalize">${key.replace('_', ' ')}</h6>
                        <p class="card-text small text-muted">${template.example}</p>
                        <button class="btn btn-sm btn-primary" onclick="useTemplate('${escapeHtml(template.prompt)}')">
                            <i class="fas fa-copy"></i> Использовать
                        </button>
                    </div>
                </div>
            </div>
        `;
    });

    templatesHtml += '</div>';
    templatesHtml += `<div class="alert alert-info mt-3"><i class="fas fa-info-circle"></i> ${data.usage}</div>`;

    document.getElementById('templates-content').innerHTML = templatesHtml;
}

/**
 * Показ примеров
 */
async function showAIExamples() {
    const modal = new bootstrap.Modal(document.getElementById('examplesModal'));
    modal.show();

    try {
        let data = aiCache.examples;

        if (!data) {
            const response = await fetch(`/api/posts/${postId}/comments/ai/examples`);
            data = await response.json();
            aiCache.examples = data;
        }

        if (data.success) {
            renderExamples(data);
        }
    } catch (error) {
        document.getElementById('examples-content').innerHTML =
            '<div class="alert alert-danger">Ошибка загрузки примеров</div>';
    }
}

/**
 * Отображение примеров
 */
function renderExamples(data) {
    let examplesHtml = '<div class="row">';

    Object.entries(data.examples).forEach(([key, example]) => {
        examplesHtml += `
            <div class="col-md-6 mb-3">
                <div class="card example-card h-100">
                    <div class="card-body">
                        <h6 class="card-title text-capitalize">${key}</h6>
                        <p class="card-text">"${escapeHtml(example.prompt)}"</p>
                        <small class="text-muted">${example.description}</small>
                        <div class="mt-2">
                            <button class="btn btn-sm btn-primary" onclick="useExample('${escapeHtml(example.prompt)}', '${key.toUpperCase()}')">
                                <i class="fas fa-copy"></i> Использовать
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `;
    });

    examplesHtml += '</div>';

    // Добавляем советы
    if (data.tips) {
        examplesHtml += '<div class="alert alert-info mt-3"><h6>💡 Советы:</h6><ul class="mb-0">';
        Object.entries(data.tips).forEach(([key, tip]) => {
            examplesHtml += `<li><strong>${key}:</strong> ${tip}</li>`;
        });
        examplesHtml += '</ul></div>';
    }

    document.getElementById('examples-content').innerHTML = examplesHtml;
}

/**
 * Использование шаблона
 */
function useTemplate(prompt) {
    const promptInput = document.getElementById('ai-prompt');
    if (promptInput) {
        promptInput.value = prompt;
        bootstrap.Modal.getInstance(document.getElementById('templatesModal')).hide();
        showSuccess('Шаблон добавлен в поле промпта');
        promptInput.focus();
    }
}

/**
 * Использование примера
 */
function useExample(prompt, type) {
    const promptInput = document.getElementById('ai-prompt');
    const typeSelect = document.getElementById('comment-type');

    if (promptInput) promptInput.value = prompt;
    if (typeSelect) typeSelect.value = type;

    bootstrap.Modal.getInstance(document.getElementById('examplesModal')).hide();
    showSuccess('Пример добавлен в форму');

    if (promptInput) promptInput.focus();
}

/**
 * Добавление кнопок улучшения к существующим комментариям
 */
function addImproveButtonsToComments() {
    // Переопределяем функцию renderCommentActions если она существует
    if (typeof window.renderCommentActions === 'function') {
        const originalRenderCommentActions = window.renderCommentActions;

        window.renderCommentActions = function(comment) {
            if (!currentUser) return '';

            const isOwner = currentUser.id === comment.author.id;
            let actions = '';

            if (isOwner) {
                actions += `
                    <button class="btn btn-sm btn-outline-primary me-2" onclick="editComment(${comment.id})">
                        <i class="fas fa-edit"></i> Редактировать
                    </button>
                    <button class="btn btn-sm btn-outline-danger me-2" onclick="deleteComment(${comment.id})">
                        <i class="fas fa-trash"></i> Удалить
                    </button>
                `;
            }

            // Добавляем кнопку улучшения для всех комментариев
            if (aiServiceAvailable) {
                actions += `
                    <button class="btn btn-sm btn-outline-info" onclick="showImproveModal('${escapeHtml(comment.content)}')">
                        <i class="fas fa-magic"></i> Улучшить с AI
                    </button>
                `;
            }

            return actions ? `<div class="comment-actions mt-2">${actions}</div>` : '';
        };
    }
}

/**
 * Показ модального окна улучшения
 */
function showImproveModal(commentText) {
    commentToImprove = commentText;

    const improveTextarea = document.getElementById('improve-text');
    const improveResult = document.getElementById('improve-result');
    const useImprovedBtn = document.getElementById('use-improved-btn');

    if (improveTextarea) improveTextarea.value = commentText;
    if (improveResult) improveResult.style.display = 'none';
    if (useImprovedBtn) useImprovedBtn.style.display = 'none';

    const modal = new bootstrap.Modal(document.getElementById('improveModal'));
    modal.show();
}

/**
 * Улучшение комментария
 */
async function improveComment() {
    const originalComment = document.getElementById('improve-text')?.value?.trim();
    const improvementType = document.getElementById('improve-type')?.value;

    if (!originalComment) {
        showError('Введите текст для улучшения');
        return;
    }

    try {
        const response = await fetchWithTimeout(`/api/posts/${postId}/comments/ai/improve`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                originalComment: originalComment,
                improvementType: improvementType
            })
        });

        const data = await response.json();

        if (data.success) {
            const improvedText = document.getElementById('improved-text');
            const improveResult = document.getElementById('improve-result');
            const useImprovedBtn = document.getElementById('use-improved-btn');

            if (improvedText) improvedText.textContent = data.data.generatedComment;
            if (improveResult) improveResult.style.display = 'block';
            if (useImprovedBtn) useImprovedBtn.style.display = 'inline-block';

            showSuccess(`Комментарий улучшен за ${data.data.generationTimeMs} мс`);
        } else {
            showError('Ошибка улучшения: ' + data.error);
        }
    } catch (error) {
        console.error('Ошибка:', error);
        showError('Ошибка при улучшении комментария: ' + error.message);
    }
}

/**
 * Использование улучшенного комментария
 */
function useImprovedComment() {
    const improvedText = document.getElementById('improved-text')?.textContent;
    const commentTextarea = document.getElementById('comment-content');

    if (improvedText && commentTextarea) {
        commentTextarea.value = improvedText;

        bootstrap.Modal.getInstance(document.getElementById('improveModal')).hide();
        showSuccess('Улучшенный комментарий добавлен в форму');

        // Прокручиваем к форме
        commentTextarea.scrollIntoView({ behavior: 'smooth' });
        commentTextarea.focus();
    }
}

/**
 * Сохранение предпочтений AI
 */
function saveAIPreferences() {
    try {
        const preferences = {
            commentType: document.getElementById('comment-type')?.value,
            timestamp: Date.now()
        };

        localStorage.setItem('aiCommentPreferences', JSON.stringify(preferences));
    } catch (error) {
        console.warn('Не удалось сохранить предпочтения AI:', error);
    }
}

/**
 * Загрузка предпочтений AI
 */
function loadAIPreferences() {
    try {
        const preferences = JSON.parse(localStorage.getItem('aiCommentPreferences') || '{}');

        if (preferences.commentType) {
            const typeSelect = document.getElementById('comment-type');
            if (typeSelect) {
                typeSelect.value = preferences.commentType;
            }
        }
    } catch (error) {
        console.warn('Не удалось загрузить предпочтения AI:', error);
    }
}

/**
 * Fetch с таймаутом
 */
async function fetchWithTimeout(url, options = {}) {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), AI_CONFIG.timeoutMs);

    try {
        const response = await fetch(url, {
            ...options,
            signal: controller.signal
        });

        clearTimeout(timeoutId);

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        return response;
    } catch (error) {
        clearTimeout(timeoutId);

        if (error.name === 'AbortError') {
            throw new Error('Запрос превысил время ожидания');
        }

        throw error;
    }
}

/**
 * Экранирование HTML
 */
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

/**
 * Показ сообщения об ошибке
 */
function showError(message) {
    if (typeof window.showError === 'function') {
        window.showError(message);
    } else {
        console.error('AI Error:', message);
        alert('Ошибка: ' + message);
    }
}

/**
 * Показ сообщения об успехе
 */
function showSuccess(message) {
    if (typeof window.showSuccess === 'function') {
        window.showSuccess(message);
    } else {
        console.log('AI Success:', message);
    }
}

// Загружаем предпочтения при инициализации
document.addEventListener('DOMContentLoaded', loadAIPreferences);

// Экспорт функций для глобального использования
window.AIComments = {
    generatePreview: generateCommentPreview,
    generateAndPost: generateAndPostComment,
    showTemplates: showAITemplates,
    showExamples: showAIExamples,
    improve: improveComment,
    checkStatus: checkAIServiceStatus,
    config: AI_CONFIG,
    cache: aiCache
};

console.log('🚀 AI Comments JavaScript модуль загружен');