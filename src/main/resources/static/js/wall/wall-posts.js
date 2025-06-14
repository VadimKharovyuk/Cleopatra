/**
 * wall-posts.js
 * Управление постами на стене
 */

class WallPosts {

    constructor(config) {
        this.wallOwnerId = config.wallOwnerId;
        this.currentUserId = config.currentUserId;
        this.pageSize = config.pageSize || 10;

        // Состояние пагинации
        this.currentPage = 0;
        this.isLoading = false;
        this.hasMorePosts = true;
        this.totalPostsCount = 0;

        // DOM элементы
        this.postsContainer = document.getElementById('postsContainer');
    }

    /**
     * Инициализация
     */
    init() {
        this.loadWallPosts();
        this.setupInfiniteScroll();
    }

    /**
     * Загрузка постов стены
     */
    async loadWallPosts() {
        if (this.isLoading || !this.hasMorePosts) return;

        this.isLoading = true;
        WallUtils.showLoading();

        try {
            const response = await fetch(
                `/wall/api/${this.wallOwnerId}/posts?page=${this.currentPage}&size=${this.pageSize}`
            );

            if (!response.ok) {
                throw new Error('Ошибка загрузки постов');
            }

            const data = await response.json();

            // Добавляем посты в контейнер
            data.wallPosts.forEach(post => {
                this.postsContainer.appendChild(this.createPostElement(post));
            });

            // Обновляем счетчик постов
            this.updateTotalPostsCount(data.wallPosts.length);

            // Обновляем состояние пагинации
            this.hasMorePosts = data.hasNext;
            this.currentPage++;

            // Обработка пустого состояния
            this.handleEmptyState(data);

        } catch (error) {
            WallUtils.logError('loadWallPosts', error);
            WallUtils.showNotification('Ошибка загрузки постов', 'error');
        } finally {
            this.isLoading = false;
            WallUtils.hideLoading();
        }
    }

    /**
     * Создание HTML элемента поста
     */
    createPostElement(post) {
        const postDiv = document.createElement('div');
        postDiv.className = 'post-item';
        postDiv.setAttribute('data-post-id', post.id);

        const authorInfo = post.author;
        const wallOwnerInfo = post.wallOwner;

        const isOwnPost = authorInfo.id !== wallOwnerInfo.id;
        const authorText = isOwnPost
            ? `${authorInfo.firstName} ${authorInfo.lastName} → ${wallOwnerInfo.firstName} ${wallOwnerInfo.lastName}`
            : `${authorInfo.firstName} ${authorInfo.lastName}`;

        postDiv.innerHTML = `
            <div class="post-header">
                <a href="/profile/${authorInfo.id}" class="avatar-link">
                    <img src="${authorInfo.imageUrl || '/default-avatar.png'}"
                         alt="${authorInfo.firstName}"
                         class="post-avatar">
                </a>
                <div class="post-author-info">
                    <h4 class="post-author-name">${WallUtils.escapeHtml(authorText)}</h4>
                    <p class="post-meta">${WallUtils.formatDate(post.createdAt)}</p>
                </div>
                <div class="post-actions-header">
                    ${this.renderPostActions(post)}
                </div>
            </div>

            <div class="post-content">
                ${post.text ? `<div class="post-text">${WallUtils.escapeHtml(post.text)}</div>` : ''}
                ${post.picUrl ? `
                    <div class="post-image">
                        <img src="${post.picUrl}" alt="Изображение поста">
                    </div>
                ` : ''}
            </div>

            <div class="post-stats">
                <div class="stat-item like-btn" onclick="wallInteractions.toggleLike(${post.id})" data-post-id="${post.id}">
                    <span class="stat-icon like-icon">❤️</span>
                    <span class="like-count">${post.likesCount}</span>
                </div>
                <div class="stat-item comment-btn" onclick="wallInteractions.showComments(${post.id})">
                    <span class="stat-icon">💬</span>
                    <span>${post.commentsCount}</span>
                </div>
            </div>
        `;

        return postDiv;
    }

    /**
     * Рендер действий для поста
     */
    renderPostActions(post) {
        if (!(post.canEdit || post.canDelete)) {
            return '';
        }

        return `
            ${post.canDelete ? `
                <button onclick="wallInteractions.deletePost(${post.id})" class="delete-btn" title="Удалить пост">
                    <i class="fas fa-trash"></i>
                    <span>Удалить</span>
                </button>
            ` : ''}
        `;
    }

    /**
     * Добавление нового поста в начало списка
     */
    addNewPost(post) {
        const postElement = this.createPostElement(post);
        this.postsContainer.insertBefore(postElement, this.postsContainer.firstChild);

        // Скрываем empty state если он показан
        WallUtils.hideEmptyState();

        // Увеличиваем счетчик
        this.totalPostsCount++;
        this.updatePostsCount();
    }

    /**
     * Удаление поста из DOM
     */
    removePost(postId) {
        const postElement = document.querySelector(`[data-post-id="${postId}"]`);
        if (postElement) {
            postElement.remove();

            // Уменьшаем счетчик
            this.totalPostsCount--;
            this.updatePostsCount();

            // Показываем empty state если нет постов
            if (WallUtils.isPostsContainerEmpty()) {
                WallUtils.showEmptyState();
            }
        }
    }

    /**
     * Настройка бесконечного скролла
     */
    setupInfiniteScroll() {
        const throttledScroll = WallUtils.throttle(() => {
            if (window.innerHeight + window.scrollY >= document.body.offsetHeight - 1000) {
                this.loadWallPosts();
            }
        }, 200);

        window.addEventListener('scroll', throttledScroll);
    }

    /**
     * Обновление общего счетчика постов
     */
    updateTotalPostsCount(newPostsCount) {
        if (this.currentPage === 1) {
            // При первой загрузке устанавливаем счетчик
            this.totalPostsCount = newPostsCount;
        } else {
            // При последующих загрузках добавляем к счетчику
            this.totalPostsCount += newPostsCount;
        }
        this.updatePostsCount();
    }

    /**
     * Обновление отображения счетчика
     */
    updatePostsCount() {
        WallUtils.updatePostsCount(this.totalPostsCount);
    }

    /**
     * Обработка пустого состояния
     */
    handleEmptyState(data) {
        if (data.isEmpty && this.currentPage === 1) {
            WallUtils.showEmptyState();
        } else if (!this.hasMorePosts && this.currentPage > 1) {
            WallUtils.showEndMessage();
        }
    }

    /**
     * Получить пост по ID
     */
    getPostElement(postId) {
        return document.querySelector(`[data-post-id="${postId}"]`);
    }

    /**
     * Обновить счетчик лайков для поста
     */
    updatePostLikeCount(postId, newCount) {
        const postElement = this.getPostElement(postId);
        if (postElement) {
            const likeCount = postElement.querySelector('.like-count');
            if (likeCount) {
                likeCount.textContent = newCount;
            }
        }
    }

    /**
     * Переключить состояние лайка поста
     */
    togglePostLikeState(postId) {
        const postElement = this.getPostElement(postId);
        if (postElement) {
            const likeBtn = postElement.querySelector('.like-btn');
            if (likeBtn) {
                likeBtn.classList.toggle('liked');

                // Анимация
                likeBtn.style.transform = 'scale(1.2)';
                setTimeout(() => {
                    likeBtn.style.transform = 'scale(1)';
                }, 200);
            }
        }
    }
}