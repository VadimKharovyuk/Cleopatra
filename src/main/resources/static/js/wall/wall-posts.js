// wall-posts.js - Управление постами стены

/**
 * Класс для управления постами стены
 */
class WallPosts {
    constructor(wallOwnerId, currentUserId) {
        this.wallOwnerId = wallOwnerId;
        this.currentUserId = currentUserId;
        this.currentPage = 0;
        this.isLoading = false;
        this.hasMorePosts = true;
        this.pageSize = 10;
        this.totalPostsCount = 0;

        this.postsContainer = document.getElementById('postsContainer');
        this.emptyState = document.getElementById('emptyState');
        this.endMessage = document.getElementById('endMessage');
    }

    /**
     * Загружает посты стены с сервера
     */
    async loadWallPosts() {
        if (this.isLoading || !this.hasMorePosts) return;

        this.isLoading = true;
        showLoading();

        try {
            const response = await fetch(`/wall/api/${this.wallOwnerId}/posts?page=${this.currentPage}&size=${this.pageSize}`);

            if (!response.ok) {
                throw new Error('Ошибка загрузки постов');
            }

            const data = await response.json();

            // Добавляем посты в контейнер
            data.wallPosts.forEach(post => {
                this.postsContainer.appendChild(this.createPostElement(post));
            });

            // Обновляем общий счетчик постов
            if (this.currentPage === 0) {
                this.totalPostsCount = data.wallPosts.length;
            } else {
                this.totalPostsCount += data.wallPosts.length;
            }
            updatePostsCount(this.totalPostsCount);

            // Обновляем состояние пагинации
            this.hasMorePosts = data.hasNext;
            this.currentPage++;

            // Показываем empty state если нет постов
            if (data.isEmpty && this.currentPage === 1) {
                this.emptyState.style.display = 'block';
            } else if (!this.hasMorePosts && this.currentPage > 1) {
                this.endMessage.style.display = 'block';
            }

        } catch (error) {
            console.error('Ошибка загрузки постов:', error);
            showNotification('Ошибка загрузки постов', 'error');
        } finally {
            this.isLoading = false;
            hideLoading();
        }
    }

    /**
     * Создает HTML элемент поста
     * @param {Object} post - Данные поста
     * @returns {HTMLElement} Элемент поста
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
          <h4 class="post-author-name">${escapeHtml(authorText)}</h4>
          <p class="post-meta">${formatDate(post.createdAt)}</p>
        </div>
        <div class="post-actions-header">
          ${(post.canEdit || post.canDelete) ? `
            ${post.canDelete ? `
              <button onclick="deletePost(${post.id})" class="delete-btn" title="Удалить пост">
                <i class="fas fa-trash"></i>
                <span>Удалить</span>
              </button>
            ` : ''}
          ` : ''}
        </div>
      </div>

      <div class="post-content">
        ${post.text ? `<div class="post-text">${escapeHtml(post.text)}</div>` : ''}
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
     * Добавляет новый пост в начало списка
     * @param {Object} post - Данные нового поста
     */
    addNewPost(post) {
        // Скрываем empty state если он показан
        this.emptyState.style.display = 'none';

        // Добавляем новый пост в начало списка
        this.postsContainer.insertBefore(this.createPostElement(post), this.postsContainer.firstChild);

        // Увеличиваем счетчик постов
        this.totalPostsCount++;
        updatePostsCount(this.totalPostsCount);
    }

    /**
     * Удаляет пост с подтверждением
     * @param {number} postId - ID поста для удаления
     */
    async deletePost(postId) {
        const confirmMessage = this.currentUserId == this.wallOwnerId
            ? 'Удалить эту запись со своей стены?'
            : 'Удалить свой пост с этой стены?';

        if (!confirm(confirmMessage)) return;

        try {
            const response = await fetch(`/wall/api/posts/${postId}`, {
                method: 'DELETE'
            });

            if (!response.ok) {
                throw new Error('Ошибка удаления поста');
            }

            // Удаляем пост из DOM
            const postElement = document.querySelector(`[data-post-id="${postId}"]`);
            if (postElement) {
                postElement.remove();

                // Уменьшаем счетчик постов
                this.totalPostsCount--;
                updatePostsCount(this.totalPostsCount);
            }

            showNotification('Пост удален', 'success');

            // Проверяем, нужно ли показать empty state
            if (this.postsContainer.children.length === 0) {
                this.emptyState.style.display = 'block';
            }

        } catch (error) {
            console.error('Ошибка удаления поста:', error);
            showNotification('Ошибка при удалении поста', 'error');
        }
    }

    /**
     * Настраивает бесконечный скролл
     */
    setupInfiniteScroll() {
        window.addEventListener('scroll', () => {
            if (window.innerHeight + window.scrollY >= document.body.offsetHeight - 1000) {
                this.loadWallPosts();
            }
        });
    }
}