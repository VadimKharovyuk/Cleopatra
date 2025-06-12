// ===== ИСПРАВЛЕННЫЙ mention-indicator.js =====

class MentionSystem {
    constructor(textareaId, suggestionsId) {
        this.textareaId = textareaId;
        this.suggestionsId = suggestionsId;
        this.textarea = null;
        this.suggestions = null;
        this.indicator = null;

        this.isActive = false;
        this.mentionStart = -1;
        this.mentionQuery = '';
        this.selectedIndex = 0;
        this.users = [];
        this.debounceTimer = null;

        // ✅ ОТЛОЖЕННАЯ ИНИЦИАЛИЗАЦИЯ
        this.initializeWhenReady();
    }

    // ✅ БЕЗОПАСНАЯ ИНИЦИАЛИЗАЦИЯ
    initializeWhenReady() {
        const maxAttempts = 50; // 5 секунд максимум
        let attempts = 0;

        const tryInitialize = () => {
            attempts++;

            // Ищем элементы
            this.textarea = document.getElementById(this.textareaId);
            this.suggestions = document.getElementById(this.suggestionsId);
            this.indicator = document.getElementById('mentionIndicator');

            if (this.textarea && this.suggestions) {
                console.log('✅ Элементы найдены, инициализируем систему упоминаний');
                this.init();
                return;
            }

            if (attempts < maxAttempts) {
                console.log(`⏳ Попытка ${attempts}: элементы не найдены, повторяем через 100мс`);
                setTimeout(tryInitialize, 100);
            } else {
                console.error('❌ Не удалось найти элементы для системы упоминаний:', {
                    textarea: this.textareaId,
                    suggestions: this.suggestionsId,
                    textareaFound: !!this.textarea,
                    suggestionsFound: !!this.suggestions
                });
            }
        };

        // Пробуем сразу, если DOM уже загружен
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', tryInitialize);
        } else {
            tryInitialize();
        }
    }

    init() {
        if (!this.textarea || !this.suggestions) {
            console.error('❌ Невозможно инициализировать: элементы не найдены');
            return;
        }

        console.log('🚀 Инициализация системы упоминаний...');

        // Добавляем обработчики событий
        this.textarea.addEventListener('input', (e) => this.handleInput(e));
        this.textarea.addEventListener('keydown', (e) => this.handleKeydown(e));
        this.textarea.addEventListener('blur', () => {
            setTimeout(() => this.hideSuggestions(), 150);
        });

        this.textarea.addEventListener('focus', () => {
            this.showIndicator();
            setTimeout(() => this.hideIndicator(), 3000);
        });

        document.addEventListener('click', (e) => {
            if (!this.suggestions.contains(e.target) && e.target !== this.textarea) {
                this.hideSuggestions();
            }
        });

        console.log('✅ Система упоминаний успешно инициализирована');
    }

    handleInput(e) {
        if (!this.textarea) return;

        const text = this.textarea.value;
        const cursorPos = this.textarea.selectionStart;

        const beforeCursor = text.substring(0, cursorPos);
        const lastAtIndex = beforeCursor.lastIndexOf('@');

        if (lastAtIndex === -1) {
            this.hideSuggestions();
            return;
        }

        const charBeforeAt = lastAtIndex > 0 ? beforeCursor[lastAtIndex - 1] : ' ';
        if (charBeforeAt !== ' ' && charBeforeAt !== '\n' && lastAtIndex !== 0) {
            this.hideSuggestions();
            return;
        }

        const afterAt = text.substring(lastAtIndex + 1, cursorPos);
        const spaceCount = (afterAt.match(/ /g) || []).length;
        if (spaceCount > 1 || afterAt.includes('\n')) {
            this.hideSuggestions();
            return;
        }

        this.mentionStart = lastAtIndex;
        this.mentionQuery = afterAt;
        this.isActive = true;

        clearTimeout(this.debounceTimer);
        this.debounceTimer = setTimeout(() => {
            this.searchUsers(this.mentionQuery);
        }, 300);
    }

    handleKeydown(e) {
        if (!this.isActive || this.users.length === 0) return;

        switch (e.key) {
            case 'ArrowDown':
                e.preventDefault();
                this.selectedIndex = (this.selectedIndex + 1) % this.users.length;
                this.updateSelection();
                break;

            case 'ArrowUp':
                e.preventDefault();
                this.selectedIndex = this.selectedIndex > 0 ? this.selectedIndex - 1 : this.users.length - 1;
                this.updateSelection();
                break;

            case 'Enter':
            case 'Tab':
                if (this.users.length > 0) {
                    e.preventDefault();
                    this.selectUser(this.users[this.selectedIndex]);
                }
                break;

            case 'Escape':
                e.preventDefault();
                this.hideSuggestions();
                break;
        }
    }

    async searchUsers(query) {
        if (query.length < 1) {
            this.showSuggestions([]);
            return;
        }

        try {
            this.showLoading();

            const response = await fetch(`/api/mentions/search?query=${encodeURIComponent(query)}`);

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            const users = await response.json();
            this.users = users;
            this.selectedIndex = 0;
            this.showSuggestions(users);

        } catch (error) {
            console.error('Ошибка поиска пользователей:', error);
            this.showError('Ошибка поиска пользователей');
        }
    }

    showSuggestions(users) {
        if (!this.isActive || !this.suggestions) return;

        if (users.length === 0) {
            this.suggestions.innerHTML = '<div class="mention-no-results">Пользователи не найдены</div>';
        } else {
            this.suggestions.innerHTML = users.map((user, index) => {
                const fullName = `${user.firstName} ${user.lastName}`;
                const initials = user.initials || `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toUpperCase();

                return `
          <div class="mention-suggestion ${index === this.selectedIndex ? 'selected' : ''}" 
               data-index="${index}" onclick="window.mentionSystemInstance.selectUserByIndex(${index})">
            <div class="mention-user-avatar">${initials}</div>
            <div class="mention-user-info">
              <div class="mention-user-name">${this.highlightQuery(fullName, this.mentionQuery)}</div>
              <div class="mention-user-email">${user.email || ''}</div>
            </div>
          </div>
        `;
            }).join('');
        }

        this.suggestions.style.display = 'block';
    }

    showLoading() {
        if (!this.suggestions) return;
        this.suggestions.innerHTML = '<div class="mention-loading"><i class="fas fa-spinner fa-spin me-2"></i>Поиск пользователей...</div>';
        this.suggestions.style.display = 'block';
    }

    showError(message) {
        if (!this.suggestions) return;
        this.suggestions.innerHTML = `<div class="mention-no-results"><i class="fas fa-exclamation-triangle me-2"></i>${message}</div>`;
        this.suggestions.style.display = 'block';
    }

    hideSuggestions() {
        if (this.suggestions) {
            this.suggestions.style.display = 'none';
        }
        this.isActive = false;
        this.users = [];
        this.mentionStart = -1;
        this.mentionQuery = '';
    }

    updateSelection() {
        if (!this.suggestions) return;
        const items = this.suggestions.querySelectorAll('.mention-suggestion');
        items.forEach((item, index) => {
            item.classList.toggle('selected', index === this.selectedIndex);
        });
    }

    selectUserByIndex(index) {
        this.selectedIndex = index;
        this.selectUser(this.users[index]);
    }

    selectUser(user) {
        if (!this.textarea) return;

        const fullName = `${user.firstName} ${user.lastName}`;
        const textBefore = this.textarea.value.substring(0, this.mentionStart);
        const textAfter = this.textarea.value.substring(this.textarea.selectionStart);

        let mention;
        if (fullName.includes(' ')) {
            mention = `@"${fullName}"`;
        } else {
            mention = `@${fullName}`;
        }

        const newText = textBefore + mention + ' ' + textAfter;
        const newCursorPos = textBefore.length + mention.length + 1;

        this.textarea.value = newText;
        this.textarea.setSelectionRange(newCursorPos, newCursorPos);

        // Триггерим событие input
        this.textarea.dispatchEvent(new Event('input', { bubbles: true }));

        this.hideSuggestions();
        this.textarea.focus();

        console.log(`✅ Выбран пользователь: ${fullName}`);
    }

    highlightQuery(text, query) {
        if (!query) return text;

        const regex = new RegExp(`(${query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi');
        return text.replace(regex, '<strong>$1</strong>');
    }

    showIndicator() {
        if (this.indicator) {
            this.indicator.classList.add('show');
        }
    }

    hideIndicator() {
        if (this.indicator) {
            this.indicator.classList.remove('show');
        }
    }

    // ✅ Публичный метод для вставки упоминания
    insertMentionInText(textarea, userName) {
        const cursorPos = textarea.selectionStart;
        const textBefore = textarea.value.substring(0, cursorPos);
        const textAfter = textarea.value.substring(cursorPos);

        let mention = userName.includes(' ') ? `@"${userName}"` : `@${userName}`;

        const newText = textBefore + mention + ' ' + textAfter;
        const newCursorPos = textBefore.length + mention.length + 1;

        textarea.value = newText;
        textarea.setSelectionRange(newCursorPos, newCursorPos);
        textarea.dispatchEvent(new Event('input', { bubbles: true }));
    }
}

// ===== БЕЗОПАСНАЯ ИНИЦИАЛИЗАЦИЯ =====

let mentionSystemInstance = null;

// ✅ Функция для инициализации системы упоминаний
function initializeMentionSystem() {
    // Пробуем найти textarea с разными возможными ID
    const possibleIds = ['postContent', 'content', 'createPostTextarea'];
    let textareaId = null;

    for (const id of possibleIds) {
        if (document.getElementById(id)) {
            textareaId = id;
            break;
        }
    }

    if (!textareaId) {
        console.warn('⚠️ Textarea для упоминаний не найден');
        return null;
    }

    // Создаем suggestions div если его нет
    let suggestionsId = 'mentionSuggestions';
    if (!document.getElementById(suggestionsId)) {
        const suggestionsDiv = document.createElement('div');
        suggestionsDiv.id = suggestionsId;
        suggestionsDiv.className = 'mention-suggestions';
        suggestionsDiv.style.display = 'none';

        const textarea = document.getElementById(textareaId);
        const container = textarea.closest('.mention-container') || textarea.parentElement;
        container.appendChild(suggestionsDiv);
    }

    // Создаем систему упоминаний
    mentionSystemInstance = new MentionSystem(textareaId, suggestionsId);

    // Делаем доступной глобально
    window.mentionSystemInstance = mentionSystemInstance;

    return mentionSystemInstance;
}

// ✅ Автоматическая инициализация
function autoInitialize() {
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => {
            setTimeout(initializeMentionSystem, 100);
        });
    } else {
        setTimeout(initializeMentionSystem, 100);
    }
}

// Запускаем автоинициализацию
autoInitialize();

// ===== ЭКСПОРТ ДЛЯ ИСПОЛЬЗОВАНИЯ В ДРУГИХ СКРИПТАХ =====

window.MentionSystem = MentionSystem;
window.initializeMentionSystem = initializeMentionSystem;

// ===== ДОПОЛНИТЕЛЬНЫЕ УТИЛИТЫ =====


// Функция для ручной инициализации с кастомными параметрами
window.createMentionSystem = function(textareaId, suggestionsId) {
    return new MentionSystem(textareaId, suggestionsId);
};

// Функция для получения текущего экземпляра
window.getMentionSystem = function() {
    return mentionSystemInstance;
};

console.log('📝 mention-indicator.js загружен');