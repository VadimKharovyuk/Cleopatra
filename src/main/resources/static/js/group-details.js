
let groupId = null;
let currentUserId = null;
let currentPage = 0;
let isLoading = false;

// ==================== INITIALIZATION ====================
document.addEventListener('DOMContentLoaded', function () {
    // Инициализация глобальных переменных
    initializeGlobalVariables();

    console.log(`🏛️ Инициализация страницы группы. Group ID: ${groupId}, User ID: ${currentUserId}`);

    if (!groupId) {
        console.error('❌ Критическая ошибка: groupId не определен');
        showAlert('Ошибка: не удалось определить ID группы. Перезагрузите страницу.', 'danger');
        return;
    }

    initializeGroupDetails();
    console.log('🏛️ Group details page initialized');
});

function initializeGlobalVariables() {
    // Получение переменных из Thymeleaf (эти значения будут установлены при рендеринге)
    // В HTML template нужно будет добавить:
    // <script th:inline="javascript">
    //     window.groupId = /*[[${group.id}]]*/ null;
    //     window.currentUserId = /*[[${currentUserId}]]*/ null;
    //     window.currentPage = /*[[${posts != null ? posts.currentPage : 0}]]*/ 0;
    // </script>

    groupId = window.groupId;
    currentUserId = window.currentUserId;
    currentPage = window.currentPage || 0;

    // Резервные способы получения groupId
    if (!groupId) {
        const contentSections = document.querySelector('.content-sections');
        groupId = contentSections ? contentSections.getAttribute('data-group-id') : null;
    }

    if (!groupId) {
        // Получаем из URL: /groups/3 -> ID = 3
        const pathParts = window.location.pathname.split('/');
        const groupsIndex = pathParts.indexOf('groups');
        if (groupsIndex !== -1 && pathParts[groupsIndex + 1]) {
            groupId = parseInt(pathParts[groupsIndex + 1]);
        }
    }

    if (!groupId) {
        console.error('❌ Не удалось получить ID группы');
        showAlert('Ошибка: не удалось определить ID группы', 'danger');
    }
}

function initializeGroupDetails() {
    initializePostCreation();
    initializePostDeletion();
    initializeLoadMore();
    initializeImageUpload();
    initializeJoinLeaveButtons();
    initializeAnimations();
    initializePostMenus();
    initializeKeyboardShortcuts();
}

// ==================== POST CREATION ====================
function initializePostCreation() {
    const form = document.getElementById('createPostForm');
    const textarea = document.getElementById('postText');
    const submitBtn = document.querySelector('.post-submit-btn');

    if (!form || !textarea || !submitBtn) return;

    // Auto-resize textarea
    textarea.addEventListener('input', function () {
        this.style.height = 'auto';
        this.style.height = Math.max(this.scrollHeight, 120) + 'px';
        updateSubmitButton();
    });

    // Form submission
    form.addEventListener('submit', function (e) {
        e.preventDefault();
        if (!isLoading) {
            createPost();
        }
    });

    // Initially disable submit button
    updateSubmitButton();
}

function updateSubmitButton() {
    const textarea = document.getElementById('postText');
    const imageInput = document.getElementById('imageInput');
    const submitBtn = document.querySelector('.post-submit-btn');

    if (!textarea || !imageInput || !submitBtn) return;

    const hasText = textarea.value.trim().length > 0;
    const hasImage = imageInput.files.length > 0;
    const hasContent = hasText || hasImage;

    submitBtn.disabled = !hasContent || isLoading;
    submitBtn.style.opacity = hasContent && !isLoading ? '1' : '0.5';
}

function createPost() {
    const form = document.getElementById('createPostForm');
    const textarea = document.getElementById('postText');
    const imageInput = document.getElementById('imageInput');
    const submitBtn = document.querySelector('.post-submit-btn');

    if (!form || !textarea || !submitBtn) return;

    if (!groupId) {
        showAlert('Ошибка: не удалось определить ID группы', 'danger');
        return;
    }

    const text = textarea.value.trim();
    const hasImage = imageInput.files.length > 0;

    if (!text && !hasImage) {
        showAlert('Пожалуйста, добавьте текст или изображение для вашего поста.', 'danger');
        return;
    }

    isLoading = true;
    const formData = new FormData(form);
    const buttonText = submitBtn.querySelector('span');
    const buttonIcon = submitBtn.querySelector('i');

    // Show loading state
    submitBtn.disabled = true;
    buttonIcon.className = 'fas fa-spinner fa-spin';
    buttonText.textContent = 'Публикация...';

    console.log(`📝 Создание поста в группе ${groupId} пользователем ${currentUserId}`);

    // Send AJAX request
    fetch(`/groups/${groupId}/posts`, {
        method: 'POST',
        body: formData,
        headers: {
            'X-Requested-With': 'XMLHttpRequest'
        }
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                showAlert(data.message, 'success');

                // Clear form
                form.reset();
                removeImagePreview();

                // Reset textarea height
                textarea.style.height = '120px';

                // Add new post to list
                addNewPostToList(data.post);

                // Hide empty state if visible
                const emptyState = document.getElementById('emptyState');
                if (emptyState) {
                    emptyState.style.display = 'none';
                }
            } else {
                showAlert(data.error || 'Произошла ошибка при создании поста.', 'danger');
            }
        })
        .catch(error => {
            console.error('Error creating post:', error);
            showAlert('Произошла ошибка при создании поста.', 'danger');
        })
        .finally(() => {
            isLoading = false;
            // Reset button
            buttonIcon.className = 'fas fa-paper-plane';
            buttonText.textContent = 'Опубликовать';
            updateSubmitButton();
        });
}

function addNewPostToList(post) {
    const postsContainer = document.getElementById('postsContainer');

    if (!postsContainer) return;

    const postHtml = createPostHTML(post, true);

    // Insert at the beginning
    postsContainer.insertAdjacentHTML('afterbegin', postHtml);

    // Animate in
    const newPost = postsContainer.querySelector('.post-card');
    if (newPost) {
        newPost.style.opacity = '0';
        newPost.style.transform = 'translateY(-20px)';

        setTimeout(() => {
            newPost.style.transition = 'all 0.3s ease';
            newPost.style.opacity = '1';
            newPost.style.transform = 'translateY(0)';
        }, 10);

        // Reinitialize event listeners for new post
        initializePostDeletion();
        initializePostMenus();
    }
}

// ==================== IMAGE UPLOAD ====================
function initializeImageUpload() {
    const imageInput = document.getElementById('imageInput');

    if (!imageInput) return;

    imageInput.addEventListener('change', function (e) {
        const file = e.target.files[0];
        if (file) {
            if (!file.type.startsWith('image/')) {
                showAlert('Пожалуйста, выберите изображение.', 'danger');
                this.value = '';
                return;
            }

            if (file.size > 5 * 1024 * 1024) { // 5MB limit
                showAlert('Размер изображения не должен превышать 5MB.', 'danger');
                this.value = '';
                return;
            }

            showImagePreview(file);
            updateSubmitButton();
        }
    });
}

function showImagePreview(file) {
    const reader = new FileReader();
    const previewSection = document.querySelector('.image-upload-section');
    const previewImage = document.getElementById('imagePreview');

    if (!reader || !previewSection || !previewImage) return;

    reader.onload = function (e) {
        previewImage.src = e.target.result;
        previewSection.style.display = 'block';
    };

    reader.onerror = function () {
        showAlert('Ошибка при чтении файла.', 'danger');
    };

    reader.readAsDataURL(file);
}

function removeImagePreview() {
    const previewSection = document.querySelector('.image-upload-section');
    const imageInput = document.getElementById('imageInput');

    if (previewSection) previewSection.style.display = 'none';
    if (imageInput) imageInput.value = '';
    updateSubmitButton();
}

// ==================== POST DELETION ====================
function initializePostDeletion() {
    const deleteButtons = document.querySelectorAll('.delete-post-btn');

    deleteButtons.forEach(btn => {
        btn.removeEventListener('click', handlePostDeletion); // Remove previous listeners
        btn.addEventListener('click', handlePostDeletion);
    });
}

function handlePostDeletion(e) {
    e.preventDefault();
    e.stopPropagation();

    const postId = this.getAttribute('data-post-id');

    if (!confirm('Вы уверены, что хотите удалить этот пост?')) {
        return;
    }

    deletePost(postId);
}

function deletePost(postId) {
    if (isLoading) return;

    if (!groupId) {
        showAlert('Ошибка: не удалось определить ID группы', 'danger');
        return;
    }

    isLoading = true;

    console.log(`🗑️ Удаление поста ${postId} из группы ${groupId}`);

    fetch(`/groups/${groupId}/posts/${postId}/delete`, {
        method: 'POST',
        headers: {
            'X-Requested-With': 'XMLHttpRequest'
        }
    })
        .then(response => {
            if (response.ok) {
                // Remove post from DOM with animation
                const postElement = document.querySelector(`[data-post-id="${postId}"]`);
                if (postElement) {
                    postElement.style.transition = 'all 0.3s ease';
                    postElement.style.opacity = '0';
                    postElement.style.transform = 'translateX(-100%)';

                    setTimeout(() => {
                        postElement.remove();
                        showAlert('Пост успешно удален!', 'success');

                        // Show empty state if no posts left
                        const remainingPosts = document.querySelectorAll('.post-card');
                        if (remainingPosts.length === 0) {
                            const emptyState = document.getElementById('emptyState');
                            if (emptyState) {
                                emptyState.style.display = 'block';
                            }
                        }
                    }, 300);
                }
            } else {
                showAlert('Ошибка при удалении поста', 'danger');
            }
        })
        .catch(error => {
            console.error('Error deleting post:', error);
            showAlert('Произошла ошибка при удалении поста', 'danger');
        })
        .finally(() => {
            isLoading = false;
        });
}

// ==================== POST MENU MANAGEMENT ====================
function initializePostMenus() {
    // Close menus when clicking outside
    document.addEventListener('click', function (e) {
        if (!e.target.closest('.post-actions-menu')) {
            document.querySelectorAll('.post-menu.show').forEach(menu => {
                menu.classList.remove('show');
            });
        }
    });
}

function togglePostMenu(button) {
    const menu = button.nextElementSibling;
    const isVisible = menu.classList.contains('show');

    // Close all other menus
    document.querySelectorAll('.post-menu.show').forEach(m => {
        if (m !== menu) m.classList.remove('show');
    });

    // Toggle current menu
    menu.classList.toggle('show', !isVisible);
}

// ==================== LOAD MORE POSTS ====================
function initializeLoadMore() {
    const loadMoreBtn = document.getElementById('loadMoreBtn');

    if (!loadMoreBtn) return;

    loadMoreBtn.addEventListener('click', function () {
        if (isLoading) return;

        const nextPage = parseInt(this.getAttribute('data-current-page')) + 1;
        loadMorePosts(nextPage);
    });
}

function loadMorePosts(page) {
    if (isLoading) return;

    if (!groupId) {
        showAlert('Ошибка: не удалось определить ID группы', 'danger');
        return;
    }

    isLoading = true;
    const loadMoreBtn = document.getElementById('loadMoreBtn');
    const originalText = loadMoreBtn.innerHTML;

    // Show loading
    loadMoreBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Загрузка...';
    loadMoreBtn.disabled = true;

    console.log(`📄 Загрузка дополнительных постов для группы ${groupId}, страница ${page}`);

    fetch(`/groups/${groupId}/posts/load-more?page=${page}&size=10`, {
        headers: {
            'X-Requested-With': 'XMLHttpRequest'
        }
    })
        .then(response => response.json())
        .then(data => {
            if (data.posts && data.posts.length > 0) {
                // Add posts to container
                const postsContainer = document.getElementById('postsContainer');

                data.posts.forEach(post => {
                    const postHtml = createPostHTML(post, false);
                    postsContainer.insertAdjacentHTML('beforeend', postHtml);
                });

                // Update page number
                loadMoreBtn.setAttribute('data-current-page', page);

                // Hide button if no more posts
                if (!data.hasNext) {
                    loadMoreBtn.style.display = 'none';
                }

                // Reinitialize event listeners
                initializePostDeletion();
                initializePostMenus();
            } else {
                loadMoreBtn.style.display = 'none';
            }
        })
        .catch(error => {
            console.error('Error loading more posts:', error);
            showAlert('Ошибка при загрузке постов', 'danger');
        })
        .finally(() => {
            isLoading = false;
            loadMoreBtn.innerHTML = originalText;
            loadMoreBtn.disabled = false;
        });
}

function createPostHTML(post, isNew = false) {
    const timeText = isNew ? 'только что' : formatDate(post.createdAt);
    // Пользователь может удалить пост если он автор или имеет права администратора/владельца
    const canDelete = post.authorId === currentUserId;

    return `
        <div class="post-card" data-post-id="${post.id}">
            <div class="post-header">
                <div class="post-author">
                    <img src="${post.authorImageUrl || '/images/default-avatar.png'}"
                         alt="${post.authorName || 'Автор'}" class="author-avatar"
                         onerror="this.src='/images/default-avatar.png'">
                    <div class="author-info">
                        <h6 class="author-name">${post.authorName || 'Неизвестный автор'}</h6>
                        <small class="post-time">${timeText}</small>
                    </div>
                </div>

                ${canDelete ? `
                    <div class="post-actions-menu">
                        <button class="btn-menu" onclick="togglePostMenu(this)" aria-label="Меню поста">
                            <i class="fas fa-ellipsis-h"></i>
                        </button>
                        <div class="post-menu">
                            <button class="menu-item delete-post-btn" data-post-id="${post.id}">
                                <i class="fas fa-trash"></i> Удалить
                            </button>
                        </div>
                    </div>
                ` : ''}
            </div>

            ${post.text ? `<div class="post-text">${escapeHtml(post.text)}</div>` : ''}

            ${post.hasImage && post.imageUrl ? `
                <div class="post-image">
                    <img src="${post.imageUrl}" alt="Post image" loading="lazy"
                         onerror="this.style.display='none'">
                </div>
            ` : ''}

            <div class="post-stats">
                <span class="stat-item" onclick="likePost(${post.id})" role="button" tabindex="0">
                    <i class="fas fa-heart"></i>
                    <span>${post.likeCount || 0}</span>
                </span>
                <span class="stat-item" onclick="showComments(${post.id})" role="button" tabindex="0">
                    <i class="fas fa-comment"></i>
                    <span>${post.commentCount || 0}</span>
                </span>
                <span class="stat-item" onclick="sharePost(${post.id})" role="button" tabindex="0">
                    <i class="fas fa-share"></i>
                    <span>Поделиться</span>
                </span>
            </div>
        </div>
    `;
}

// ==================== JOIN/LEAVE BUTTONS ====================
function initializeJoinLeaveButtons() {
    const joinBtn = document.querySelector('.join-btn');
    const leaveBtn = document.querySelector('.leave-btn');

    if (joinBtn) {
        joinBtn.addEventListener('click', handleJoinGroup);
    }

    if (leaveBtn) {
        leaveBtn.addEventListener('click', handleLeaveGroup);
    }
}

function handleJoinGroup(e) {
    e.preventDefault();

    if (isLoading) return;

    const button = this;
    const buttonText = button.querySelector('span');
    const buttonIcon = button.querySelector('i');

    isLoading = true;
    // Show loading
    button.disabled = true;
    buttonIcon.className = 'fas fa-spinner fa-spin';
    buttonText.textContent = 'Отправка...';

    // Simulate API call (replace with actual endpoint)
    setTimeout(() => {
        buttonIcon.className = 'fas fa-check';
        buttonText.textContent = 'Заявка отправлена';
        showAlert('Заявка на вступление отправлена!', 'success');

        setTimeout(() => {
            button.style.animation = 'fadeOut 0.3s ease-out';
            setTimeout(() => {
                button.style.display = 'none';
            }, 300);
        }, 2000);

        isLoading = false;
    }, 1500);
}

function handleLeaveGroup(e) {
    e.preventDefault();

    if (isLoading) return;

    if (!confirm('Вы уверены, что хотите покинуть эту группу?')) {
        return;
    }

    const button = this;
    const buttonText = button.querySelector('span');
    const buttonIcon = button.querySelector('i');

    isLoading = true;
    // Show loading
    button.disabled = true;
    buttonIcon.className = 'fas fa-spinner fa-spin';
    buttonText.textContent = 'Выход...';

    // Simulate API call (replace with actual endpoint)
    setTimeout(() => {
        showAlert('Вы покинули группу', 'success');
        setTimeout(() => {
            window.location.href = '/groups';
        }, 1000);
    }, 1500);
}

// ==================== ANIMATIONS ====================
function initializeAnimations() {
    // Stagger animation for sections
    const sections = document.querySelectorAll('.section-card');
    sections.forEach((section, index) => {
        section.style.animationDelay = `${(index + 1) * 0.1}s`;
    });

    // Intersection observer for posts
    const observerOptions = {
        threshold: 0.1,
        rootMargin: '50px'
    };

    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.style.animation = 'fadeIn 0.6s ease-out forwards';
            }
        });
    }, observerOptions);

    document.querySelectorAll('.post-card').forEach(post => {
        observer.observe(post);
    });
}

// ==================== KEYBOARD SHORTCUTS ====================
function initializeKeyboardShortcuts() {
    document.addEventListener('keydown', function (e) {
        // Ctrl + Enter to submit post
        if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
            const textarea = document.getElementById('postText');
            if (textarea && document.activeElement === textarea) {
                e.preventDefault();
                const form = document.getElementById('createPostForm');
                if (form && !isLoading) {
                    createPost();
                }
            }
        }

        // Escape to close menus
        if (e.key === 'Escape') {
            document.querySelectorAll('.post-menu.show').forEach(menu => {
                menu.classList.remove('show');
            });
        }
    });
}

// ==================== UTILITY FUNCTIONS ====================
function showAlert(message, type = 'info') {
    const alertContainer = document.getElementById('alertContainer');

    if (!alertContainer) return;

    const alertId = `alert-${Date.now()}`;
    const iconClass = type === 'success' ? 'check-circle' :
        type === 'danger' ? 'exclamation-triangle' :
            type === 'warning' ? 'exclamation-triangle' : 'info-circle';

    const alertHtml = `
        <div id="${alertId}" class="alert alert-${type}">
            <div class="alert-content">
                <i class="fas fa-${iconClass}"></i>
                <span>${escapeHtml(message)}</span>
            </div>
            <button class="alert-close" onclick="removeAlert('${alertId}')" aria-label="Закрыть">
                <i class="fas fa-times"></i>
            </button>
        </div>
    `;

    alertContainer.insertAdjacentHTML('beforeend', alertHtml);

    // Auto remove after 5 seconds
    setTimeout(() => removeAlert(alertId), 5000);
}

function removeAlert(alertId) {
    const alert = document.getElementById(alertId);
    if (alert) {
        alert.style.animation = 'slideOut 0.3s ease-out';
        setTimeout(() => alert.remove(), 300);
    }
}

function formatDate(dateString) {
    if (!dateString) return 'неизвестно';

    try {
        const date = new Date(dateString);
        const now = new Date();
        const diff = now - date;

        const minutes = Math.floor(diff / 60000);
        const hours = Math.floor(diff / 3600000);
        const days = Math.floor(diff / 86400000);

        if (minutes < 1) return 'только что';
        if (minutes < 60) return `${minutes} мин. назад`;
        if (hours < 24) return `${hours} ч. назад`;
        if (days < 7) return `${days} дн. назад`;

        return date.toLocaleDateString('ru-RU', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    } catch (e) {
        return 'неизвестно';
    }
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function focusPostCreation() {
    const textarea = document.getElementById('postText');
    if (textarea) {
        textarea.focus();
        textarea.scrollIntoView({behavior: 'smooth', block: 'center'});
    }
}

// ==================== PLACEHOLDER FUNCTIONS ====================
// Эти функции нужно будет реализовать позже
function showComingSoon(feature) {
    showAlert(`Функция "${feature}" скоро будет доступна!`, 'info');
}

function likePost(postId) {
    console.log('Like post:', postId);
    showAlert('Функция лайков скоро будет доступна!', 'info');
}

function showComments(postId) {
    console.log('Show comments for post:', postId);
    showAlert('Функция комментариев скоро будет доступна!', 'info');
}

function sharePost(postId) {
    console.log('Share post:', postId);
    showAlert('Функция "Поделиться" скоро будет доступна!', 'info');
}

// ==================== ERROR HANDLING ====================
window.addEventListener('error', function (e) {
    console.error('JavaScript error:', e.error);
    if (isLoading) {
        isLoading = false;
        showAlert('Произошла ошибка. Попробуйте обновить страницу.', 'danger');
    }
});

// ==================== PERFORMANCE OPTIMIZATION ====================
// Debounce function for performance
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// Throttle function for scroll events
function throttle(func, limit) {
    let inThrottle;
    return function () {
        const args = arguments;
        const context = this;
        if (!inThrottle) {
            func.apply(context, args);
            inThrottle = true;
            setTimeout(() => inThrottle = false, limit);
        }
    }
}

// ==================== ACCESSIBILITY ====================
// Add keyboard navigation for interactive elements
document.addEventListener('keydown', function (e) {
    if (e.key === 'Enter' || e.key === ' ') {
        const target = e.target;
        if (target.classList.contains('stat-item') || target.classList.contains('attachment-btn')) {
            e.preventDefault();
            target.click();
        }
    }
});

// ==================== GLOBAL FUNCTIONS ====================
// Make functions globally available
window.focusPostCreation = focusPostCreation;
window.removeAlert = removeAlert;
window.togglePostMenu = togglePostMenu;
window.removeImagePreview = removeImagePreview;
window.showComingSoon = showComingSoon;
window.likePost = likePost;
window.showComments = showComments;
window.sharePost = sharePost;

console.log('🏛️ Cleopatra Group Details with AJAX Posts - Loaded Successfully');
console.log('📊 Features: Post Creation, Deletion, Image Upload, Load More, Responsive Design');