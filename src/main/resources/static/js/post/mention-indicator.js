// // ===== ИСПРАВЛЕННЫЙ mention-indicator.js =====
//
// class MentionSystem {
//     constructor(textareaId, suggestionsId) {
//         this.textareaId = textareaId;
//         this.suggestionsId = suggestionsId;
//         this.textarea = null;
//         this.suggestions = null;
//         this.indicator = null;
//
//         this.isActive = false;
//         this.mentionStart = -1;
//         this.mentionQuery = '';
//         this.selectedIndex = 0;
//         this.users = [];
//         this.debounceTimer = null;
//
//         // ✅ ОТЛОЖЕННАЯ ИНИЦИАЛИЗАЦИЯ
//         this.initializeWhenReady();
//     }
//
//     // ✅ БЕЗОПАСНАЯ ИНИЦИАЛИЗАЦИЯ
//     initializeWhenReady() {
//         const maxAttempts = 50; // 5 секунд максимум
//         let attempts = 0;
//
//         const tryInitialize = () => {
//             attempts++;
//
//             // Ищем элементы
//             this.textarea = document.getElementById(this.textareaId);
//             this.suggestions = document.getElementById(this.suggestionsId);
//             this.indicator = document.getElementById('mentionIndicator');
//
//             if (this.textarea && this.suggestions) {
//                 console.log('✅ Элементы найдены, инициализируем систему упоминаний');
//                 this.init();
//                 return;
//             }
//
//             if (attempts < maxAttempts) {
//                 console.log(`⏳ Попытка ${attempts}: элементы не найдены, повторяем через 100мс`);
//                 setTimeout(tryInitialize, 100);
//             } else {
//                 console.error('❌ Не удалось найти элементы для системы упоминаний:', {
//                     textarea: this.textareaId,
//                     suggestions: this.suggestionsId,
//                     textareaFound: !!this.textarea,
//                     suggestionsFound: !!this.suggestions
//                 });
//             }
//         };
//
//         // Пробуем сразу, если DOM уже загружен
//         if (document.readyState === 'loading') {
//             document.addEventListener('DOMContentLoaded', tryInitialize);
//         } else {
//             tryInitialize();
//         }
//     }
//
//     init() {
//         if (!this.textarea || !this.suggestions) {
//             console.error('❌ Невозможно инициализировать: элементы не найдены');
//             return;
//         }
//
//         console.log('🚀 Инициализация системы упоминаний...');
//
//         // Добавляем обработчики событий
//         this.textarea.addEventListener('input', (e) => this.handleInput(e));
//         this.textarea.addEventListener('keydown', (e) => this.handleKeydown(e));
//         this.textarea.addEventListener('blur', () => {
//             setTimeout(() => this.hideSuggestions(), 150);
//         });
//
//         this.textarea.addEventListener('focus', () => {
//             this.showIndicator();
//             setTimeout(() => this.hideIndicator(), 3000);
//         });
//
//         document.addEventListener('click', (e) => {
//             if (!this.suggestions.contains(e.target) && e.target !== this.textarea) {
//                 this.hideSuggestions();
//             }
//         });
//
//         console.log('✅ Система упоминаний успешно инициализирована');
//     }
//
//     handleInput(e) {
//         if (!this.textarea) return;
//
//         const text = this.textarea.value;
//         const cursorPos = this.textarea.selectionStart;
//
//         const beforeCursor = text.substring(0, cursorPos);
//         const lastAtIndex = beforeCursor.lastIndexOf('@');
//
//         if (lastAtIndex === -1) {
//             this.hideSuggestions();
//             return;
//         }
//
//         const charBeforeAt = lastAtIndex > 0 ? beforeCursor[lastAtIndex - 1] : ' ';
//         if (charBeforeAt !== ' ' && charBeforeAt !== '\n' && lastAtIndex !== 0) {
//             this.hideSuggestions();
//             return;
//         }
//
//         const afterAt = text.substring(lastAtIndex + 1, cursorPos);
//         const spaceCount = (afterAt.match(/ /g) || []).length;
//         if (spaceCount > 1 || afterAt.includes('\n')) {
//             this.hideSuggestions();
//             return;
//         }
//
//         this.mentionStart = lastAtIndex;
//         this.mentionQuery = afterAt;
//         this.isActive = true;
//
//         clearTimeout(this.debounceTimer);
//         this.debounceTimer = setTimeout(() => {
//             this.searchUsers(this.mentionQuery);
//         }, 300);
//     }
//
//     handleKeydown(e) {
//         if (!this.isActive || this.users.length === 0) return;
//
//         switch (e.key) {
//             case 'ArrowDown':
//                 e.preventDefault();
//                 this.selectedIndex = (this.selectedIndex + 1) % this.users.length;
//                 this.updateSelection();
//                 break;
//
//             case 'ArrowUp':
//                 e.preventDefault();
//                 this.selectedIndex = this.selectedIndex > 0 ? this.selectedIndex - 1 : this.users.length - 1;
//                 this.updateSelection();
//                 break;
//
//             case 'Enter':
//             case 'Tab':
//                 if (this.users.length > 0) {
//                     e.preventDefault();
//                     this.selectUser(this.users[this.selectedIndex]);
//                 }
//                 break;
//
//             case 'Escape':
//                 e.preventDefault();
//                 this.hideSuggestions();
//                 break;
//         }
//     }
//
//     async searchUsers(query) {
//         if (query.length < 1) {
//             this.showSuggestions([]);
//             return;
//         }
//
//         try {
//             this.showLoading();
//
//             const response = await fetch(`/api/mentions/search?query=${encodeURIComponent(query)}`);
//
//             if (!response.ok) {
//                 throw new Error(`HTTP ${response.status}: ${response.statusText}`);
//             }
//
//             const users = await response.json();
//             this.users = users;
//             this.selectedIndex = 0;
//             this.showSuggestions(users);
//
//         } catch (error) {
//             console.error('Ошибка поиска пользователей:', error);
//             this.showError('Ошибка поиска пользователей');
//         }
//     }
//
//     showSuggestions(users) {
//         if (!this.isActive || !this.suggestions) return;
//
//         if (users.length === 0) {
//             this.suggestions.innerHTML = '<div class="mention-no-results">Пользователи не найдены</div>';
//         } else {
//             this.suggestions.innerHTML = users.map((user, index) => {
//                 const fullName = `${user.firstName} ${user.lastName}`;
//                 const initials = user.initials || `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toUpperCase();
//
//                 return `
//           <div class="mention-suggestion ${index === this.selectedIndex ? 'selected' : ''}"
//                data-index="${index}" onclick="window.mentionSystemInstance.selectUserByIndex(${index})">
//             <div class="mention-user-avatar">${initials}</div>
//             <div class="mention-user-info">
//               <div class="mention-user-name">${this.highlightQuery(fullName, this.mentionQuery)}</div>
//               <div class="mention-user-email">${user.email || ''}</div>
//             </div>
//           </div>
//         `;
//             }).join('');
//         }
//
//         this.suggestions.style.display = 'block';
//     }
//
//     showLoading() {
//         if (!this.suggestions) return;
//         this.suggestions.innerHTML = '<div class="mention-loading"><i class="fas fa-spinner fa-spin me-2"></i>Поиск пользователей...</div>';
//         this.suggestions.style.display = 'block';
//     }
//
//     showError(message) {
//         if (!this.suggestions) return;
//         this.suggestions.innerHTML = `<div class="mention-no-results"><i class="fas fa-exclamation-triangle me-2"></i>${message}</div>`;
//         this.suggestions.style.display = 'block';
//     }
//
//     hideSuggestions() {
//         if (this.suggestions) {
//             this.suggestions.style.display = 'none';
//         }
//         this.isActive = false;
//         this.users = [];
//         this.mentionStart = -1;
//         this.mentionQuery = '';
//     }
//
//     updateSelection() {
//         if (!this.suggestions) return;
//         const items = this.suggestions.querySelectorAll('.mention-suggestion');
//         items.forEach((item, index) => {
//             item.classList.toggle('selected', index === this.selectedIndex);
//         });
//     }
//
//     selectUserByIndex(index) {
//         this.selectedIndex = index;
//         this.selectUser(this.users[index]);
//     }
//
//     selectUser(user) {
//         if (!this.textarea) return;
//
//         const fullName = `${user.firstName} ${user.lastName}`;
//         const textBefore = this.textarea.value.substring(0, this.mentionStart);
//         const textAfter = this.textarea.value.substring(this.textarea.selectionStart);
//
//         let mention;
//         if (fullName.includes(' ')) {
//             mention = `@"${fullName}"`;
//         } else {
//             mention = `@${fullName}`;
//         }
//
//         const newText = textBefore + mention + ' ' + textAfter;
//         const newCursorPos = textBefore.length + mention.length + 1;
//
//         this.textarea.value = newText;
//         this.textarea.setSelectionRange(newCursorPos, newCursorPos);
//
//         // Триггерим событие input
//         this.textarea.dispatchEvent(new Event('input', { bubbles: true }));
//
//         this.hideSuggestions();
//         this.textarea.focus();
//
//         console.log(`✅ Выбран пользователь: ${fullName}`);
//     }
//
//     highlightQuery(text, query) {
//         if (!query) return text;
//
//         const regex = new RegExp(`(${query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi');
//         return text.replace(regex, '<strong>$1</strong>');
//     }
//
//     showIndicator() {
//         if (this.indicator) {
//             this.indicator.classList.add('show');
//         }
//     }
//
//     hideIndicator() {
//         if (this.indicator) {
//             this.indicator.classList.remove('show');
//         }
//     }
//
//     // ✅ Публичный метод для вставки упоминания
//     insertMentionInText(textarea, userName) {
//         const cursorPos = textarea.selectionStart;
//         const textBefore = textarea.value.substring(0, cursorPos);
//         const textAfter = textarea.value.substring(cursorPos);
//
//         let mention = userName.includes(' ') ? `@"${userName}"` : `@${userName}`;
//
//         const newText = textBefore + mention + ' ' + textAfter;
//         const newCursorPos = textBefore.length + mention.length + 1;
//
//         textarea.value = newText;
//         textarea.setSelectionRange(newCursorPos, newCursorPos);
//         textarea.dispatchEvent(new Event('input', { bubbles: true }));
//     }
// }
//
// // ===== БЕЗОПАСНАЯ ИНИЦИАЛИЗАЦИЯ =====
//
// let mentionSystemInstance = null;
//
// // ✅ Функция для инициализации системы упоминаний
// function initializeMentionSystem() {
//     // Пробуем найти textarea с разными возможными ID
//     const possibleIds = ['postContent', 'content', 'createPostTextarea'];
//     let textareaId = null;
//
//     for (const id of possibleIds) {
//         if (document.getElementById(id)) {
//             textareaId = id;
//             break;
//         }
//     }
//
//     if (!textareaId) {
//         console.warn('⚠️ Textarea для упоминаний не найден');
//         return null;
//     }
//
//     // Создаем suggestions div если его нет
//     let suggestionsId = 'mentionSuggestions';
//     if (!document.getElementById(suggestionsId)) {
//         const suggestionsDiv = document.createElement('div');
//         suggestionsDiv.id = suggestionsId;
//         suggestionsDiv.className = 'mention-suggestions';
//         suggestionsDiv.style.display = 'none';
//
//         const textarea = document.getElementById(textareaId);
//         const container = textarea.closest('.mention-container') || textarea.parentElement;
//         container.appendChild(suggestionsDiv);
//     }
//
//     // Создаем систему упоминаний
//     mentionSystemInstance = new MentionSystem(textareaId, suggestionsId);
//
//     // Делаем доступной глобально
//     window.mentionSystemInstance = mentionSystemInstance;
//
//     return mentionSystemInstance;
// }
//
// // ✅ Автоматическая инициализация
// function autoInitialize() {
//     if (document.readyState === 'loading') {
//         document.addEventListener('DOMContentLoaded', () => {
//             setTimeout(initializeMentionSystem, 100);
//         });
//     } else {
//         setTimeout(initializeMentionSystem, 100);
//     }
// }
//
// // Запускаем автоинициализацию
// autoInitialize();
//
// // ===== ЭКСПОРТ ДЛЯ ИСПОЛЬЗОВАНИЯ В ДРУГИХ СКРИПТАХ =====
//
// window.MentionSystem = MentionSystem;
// window.initializeMentionSystem = initializeMentionSystem;
//
// // ===== ДОПОЛНИТЕЛЬНЫЕ УТИЛИТЫ =====
//
//
// // Функция для ручной инициализации с кастомными параметрами
// window.createMentionSystem = function(textareaId, suggestionsId) {
//     return new MentionSystem(textareaId, suggestionsId);
// };
//
// // Функция для получения текущего экземпляра
// window.getMentionSystem = function() {
//     return mentionSystemInstance;
// };
//
// console.log('📝 mention-indicator.js загружен');

// ===== ФИНАЛЬНАЯ ВЕРСИЯ mention-indicator.js =====

class MentionSystem {
    constructor(textareaElement, suggestionsElement) {
        this.textarea = textareaElement;
        this.suggestions = suggestionsElement;
        this.indicator = document.getElementById('mentionIndicator');

        this.isActive = false;
        this.mentionStart = -1;
        this.mentionQuery = '';
        this.selectedIndex = 0;
        this.users = [];
        this.debounceTimer = null;

        if (this.textarea && this.suggestions) {
            this.init();
        } else {
            console.error('❌ MentionSystem: необходимы textarea и suggestions элементы');
        }
    }

    init() {
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

        // Обработчик кликов вне области
        document.addEventListener('click', (e) => {
            if (!this.suggestions.contains(e.target) && e.target !== this.textarea) {
                this.hideSuggestions();
            }
        });

        // Обновляем счетчик упоминаний при изменении текста
        this.textarea.addEventListener('input', () => {
            this.updateMentionCounter();
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

        // Позиционируем suggestions относительно курсора
        this.positionSuggestions();
    }

    positionSuggestions() {
        if (!this.textarea || !this.suggestions) return;

        const textareaRect = this.textarea.getBoundingClientRect();
        const containerRect = this.textarea.closest('.mention-container').getBoundingClientRect();

        // Позиционируем под textarea
        this.suggestions.style.top = (textareaRect.bottom - containerRect.top + 5) + 'px';
        this.suggestions.style.left = '0px';
        this.suggestions.style.width = textareaRect.width + 'px';
    }

    showLoading() {
        if (!this.suggestions) return;
        this.suggestions.innerHTML = '<div class="mention-loading"><i class="fas fa-spinner fa-spin me-2"></i>Поиск пользователей...</div>';
        this.suggestions.style.display = 'block';
        this.positionSuggestions();
    }

    showError(message) {
        if (!this.suggestions) return;
        this.suggestions.innerHTML = `<div class="mention-no-results"><i class="fas fa-exclamation-triangle me-2"></i>${message}</div>`;
        this.suggestions.style.display = 'block';
        this.positionSuggestions();
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

        // Триггерим событие input для обновления счетчиков
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

    // Обновление счетчика упоминаний
    updateMentionCounter() {
        const text = this.textarea.value;
        const mentions = text.match(/@"[^"]+"|@\w+/g) || [];
        const mentionCount = mentions.length;

        const counterElement = document.getElementById('mentionCount');
        const counterNumber = document.getElementById('mentionCountNumber');

        if (counterElement && counterNumber) {
            if (mentionCount > 0) {
                counterElement.style.display = 'inline';
                counterNumber.textContent = mentionCount;
            } else {
                counterElement.style.display = 'none';
            }
        }
    }

    // Публичный метод для вставки упоминания
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

// ===== СИСТЕМА ИНИЦИАЛИЗАЦИИ =====

let mentionSystemInstance = null;

// Функция для точного поиска элементов по вашей структуре
function findElements() {
    console.log('🔍 Поиск элементов в известной структуре...');

    // 1. Ищем точно ваш textarea
    const textarea = document.getElementById('postContent');
    if (!textarea) {
        console.log('❌ Элемент #postContent не найден');
        return { textarea: null, suggestions: null };
    }

    console.log('✅ Найден textarea #postContent');

    // 2. Ищем точно ваш контейнер suggestions
    const suggestions = document.getElementById('mentionSuggestions');
    if (!suggestions) {
        console.log('❌ Элемент #mentionSuggestions не найден');
        return { textarea, suggestions: null };
    }

    console.log('✅ Найден suggestions #mentionSuggestions');

    return { textarea, suggestions };
}

// Основная функция инициализации
function initializeMentionSystem() {
    console.log('🚀 Инициализация системы упоминаний...');

    const { textarea, suggestions } = findElements();

    if (!textarea || !suggestions) {
        console.warn('⚠️ Не найдены необходимые элементы для системы упоминаний');
        return null;
    }

    // Проверяем, что элементы видимы и доступны
    const textareaStyle = window.getComputedStyle(textarea);
    if (textareaStyle.display === 'none' || textareaStyle.visibility === 'hidden') {
        console.warn('⚠️ Textarea скрыт, ожидаем...');
        return null;
    }

    // Уничтожаем предыдущий экземпляр если есть
    if (mentionSystemInstance) {
        console.log('🔄 Удаляем предыдущий экземпляр системы упоминаний');
        mentionSystemInstance = null;
    }

    // Создаем новый экземпляр
    mentionSystemInstance = new MentionSystem(textarea, suggestions);
    window.mentionSystemInstance = mentionSystemInstance;

    console.log('🎉 Система упоминаний успешно инициализирована!');
    return mentionSystemInstance;
}

// Умная система ожидания элементов
function waitForElements() {
    let attempts = 0;
    const maxAttempts = 50; // 5 секунд

    const tryInit = () => {
        attempts++;
        console.log(`🔄 Попытка инициализации ${attempts}/${maxAttempts}`);

        const result = initializeMentionSystem();

        if (result) {
            console.log(`✅ Система упоминаний инициализирована с попытки ${attempts}`);
            return;
        }

        if (attempts < maxAttempts) {
            setTimeout(tryInit, 100);
        } else {
            console.warn('⚠️ Превышено время ожидания инициализации');
            console.log('💡 Попробуйте вызвать initializeMentionSystem() вручную');
        }
    };

    tryInit();
}

// Наблюдатель за изменениями DOM для SPA
function setupDOMObserver() {
    const observer = new MutationObserver((mutations) => {
        const hasTargetElements = mutations.some(mutation =>
            Array.from(mutation.addedNodes).some(node =>
                    node.nodeType === 1 && (
                        node.id === 'postContent' ||
                        node.id === 'mentionSuggestions' ||
                        (node.querySelector && node.querySelector('#postContent, #mentionSuggestions'))
                    )
            )
        );

        if (hasTargetElements && !mentionSystemInstance) {
            console.log('🔄 Обнаружены целевые элементы, переинициализируем...');
            setTimeout(waitForElements, 100);
        }
    });

    observer.observe(document.body, {
        childList: true,
        subtree: true
    });

    console.log('👁️ DOM Observer активен');
}

// Автоматическая инициализация
function autoInitialize() {
    console.log('📝 Загружена финальная версия mention-indicator.js');

    // Настраиваем наблюдатель
    setupDOMObserver();

    // Пробуем инициализировать
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => {
            setTimeout(waitForElements, 100);
        });
    } else {
        setTimeout(waitForElements, 100);
    }
}

// Запускаем автоинициализацию
autoInitialize();

// ===== ПУБЛИЧНЫЙ API =====

window.MentionSystem = MentionSystem;
window.initializeMentionSystem = initializeMentionSystem;

window.getMentionSystem = function() {
    return mentionSystemInstance;
};

window.isMentionSystemReady = function() {
    return mentionSystemInstance !== null;
};

window.reinitializeMentionSystem = function() {
    console.log('🔄 Ручная переинициализация системы упоминаний...');
    return initializeMentionSystem();
};

// ===== ФУНКЦИИ ДЛЯ ИНТЕГРАЦИИ С ВАШИМИ КНОПКАМИ =====

// Функция для кнопки упоминаний
window.showMentionHint = function() {
    const textarea = document.getElementById('postContent');
    if (!textarea) return;

    // Вставляем @ символ в текущую позицию курсора
    const cursorPos = textarea.selectionStart;
    const textBefore = textarea.value.substring(0, cursorPos);
    const textAfter = textarea.value.substring(cursorPos);

    textarea.value = textBefore + '@' + textAfter;
    textarea.setSelectionRange(cursorPos + 1, cursorPos + 1);

    // Фокусируемся на textarea и триггерим событие
    textarea.focus();
    textarea.dispatchEvent(new Event('input', { bubbles: true }));

    console.log('💡 Подсказка упоминания активирована');
};

// Функция для программной вставки упоминания
window.insertMention = function(userName) {
    if (mentionSystemInstance && mentionSystemInstance.textarea) {
        mentionSystemInstance.insertMentionInText(mentionSystemInstance.textarea, userName);
        return true;
    }
    return false;
};

// Добавляем улучшенные стили
if (!document.getElementById('mention-system-styles')) {
    const style = document.createElement('style');
    style.id = 'mention-system-styles';
    style.textContent = `
        /* Контейнер упоминаний */
        .mention-container {
            position: relative;
        }
        
        /* Индикатор упоминаний */
        .mention-indicator {
            position: absolute;
            top: 10px;
            right: 10px;
            background: rgba(0, 123, 255, 0.1);
            color: #007bff;
            padding: 4px 8px;
            border-radius: 12px;
            font-size: 12px;
            opacity: 0;
            transition: opacity 0.3s;
            pointer-events: none;
            z-index: 10;
        }
        
        .mention-indicator.show {
            opacity: 1;
        }
        
        /* Suggestions dropdown */
        .mention-suggestions {
            position: absolute;
            background: white;
            border: 1px solid #ddd;
            border-radius: 8px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.1);
            max-height: 200px;
            overflow-y: auto;
            z-index: 1000;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            display: none;
        }
        
        .mention-suggestion {
            display: flex;
            align-items: center;
            padding: 10px 12px;
            cursor: pointer;
            border-bottom: 1px solid #f0f0f0;
            transition: background-color 0.2s;
        }
        
        .mention-suggestion:last-child {
            border-bottom: none;
        }
        
        .mention-suggestion:hover,
        .mention-suggestion.selected {
            background-color: #f8f9fa;
        }
        
        .mention-user-avatar {
            width: 36px;
            height: 36px;
            border-radius: 50%;
            background: linear-gradient(135deg, #007bff, #0056b3);
            color: white;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 14px;
            font-weight: bold;
            margin-right: 12px;
            flex-shrink: 0;
        }
        
        .mention-user-info {
            flex: 1;
            min-width: 0;
        }
        
        .mention-user-name {
            font-weight: 500;
            color: #333;
            margin-bottom: 2px;
        }
        
        .mention-user-email {
            font-size: 12px;
            color: #666;
        }
        
        .mention-no-results,
        .mention-loading {
            padding: 16px;
            text-align: center;
            color: #666;
            font-size: 14px;
        }
        
        .mention-loading {
            color: #007bff;
        }
        
        /* Кнопка упоминаний */
        .mention-tool-btn {
            color: #007bff !important;
        }
        
        .mention-tool-btn:hover {
            background-color: rgba(0, 123, 255, 0.1) !important;
        }
        
        /* Счетчик упоминаний */
        .mention-count {
            margin-left: 10px;
            color: #007bff;
            font-size: 12px;
        }
        
        .mention-count i {
            margin-right: 4px;
        }
    `;
    document.head.appendChild(style);
}

console.log('🎯 Финальная версия mention-indicator.js загружена и готова к работе!');