 // Функция для показа уведомлений
    function showNotification(message, type = 'success') {
    // Создаем элемент уведомления если его нет
    let notification = document.getElementById('notification');
    if (!notification) {
    notification = document.createElement('div');
    notification.id = 'notification';
    notification.className = 'notification';
    notification.style.display = 'none';

    notification.innerHTML = `
                <i class="notification-icon"></i>
                <span class="notification-text"></span>
            `;

    document.body.appendChild(notification);
}

    const icon = notification.querySelector('.notification-icon');
    const text = notification.querySelector('.notification-text');

    // Устанавливаем стили в зависимости от типа
    if (type === 'success') {
    icon.className = 'notification-icon fas fa-check-circle';
    notification.className = 'notification success';
} else if (type === 'error') {
    icon.className = 'notification-icon fas fa-exclamation-circle';
    notification.className = 'notification error';
}

    text.textContent = message;
    notification.style.display = 'flex';

    // Показываем уведомление
    setTimeout(() => {
    notification.classList.add('show');
}, 100);

    // Скрываем через 4 секунды
    setTimeout(() => {
    hideNotification();
}, 4000);
}

    function hideNotification() {
    const notification = document.getElementById('notification');
    notification.classList.remove('show');

    setTimeout(() => {
    notification.style.display = 'none';
}, 300);
}

    // JavaScript функции для работы с постами
    function togglePostMenu(btn) {
    const dropdown = btn.nextElementSibling;
    const isOpen = dropdown.classList.contains('show');

    // Закрываем все открытые меню
    document.querySelectorAll('.post-menu-dropdown').forEach(menu => {
    menu.classList.remove('show');
});

    // Открываем/закрываем текущее меню
    if (!isOpen) {
    dropdown.classList.add('show');
}
}

    function openComments(btn) {
    const postId = btn.dataset.postId;
    window.location.href = `/posts/${postId}#comments`;
}

    // ✅ ОБНОВЛЕННАЯ функция для лайков с AJAX
    async function toggleLike(btn) {
    const postId = btn.dataset.postId;
    const icon = btn.querySelector('i');
    const count = btn.querySelector('span');

    // Блокируем кнопку во время запроса
    const originalDisabled = btn.disabled;
    btn.disabled = true;
    btn.classList.add('loading');

    try {
    const response = await fetch(`/api/posts/${postId}/like`, {
    method: 'POST',
    headers: {
    'Content-Type': 'application/json',
    // Добавьте CSRF токен если используете Spring Security
    // 'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').getAttribute('content')
}
});

    if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
}

    const result = await response.json();

    // Обновляем UI на основе ответа сервера
    updateLikeButton(btn, result.isLiked, result.likesCount);

    // Обновляем информацию о лайках
    await updateLikesInfo(postId);

    // Показываем уведомление
    const message = result.isLiked ? 'Пост добавлен в понравившиеся ❤️' : 'Лайк убран';
    showNotification(message, 'success');

} catch (error) {
    console.error('Ошибка при лайке поста:', error);
    showNotification('Произошла ошибка. Попробуйте снова.', 'error');
} finally {
    // Разблокируем кнопку
    btn.disabled = originalDisabled;
    btn.classList.remove('loading');
}
}

    // Функция для обновления кнопки лайка
    function updateLikeButton(btn, isLiked, likesCount) {
    const icon = btn.querySelector('i');
    const count = btn.querySelector('span');

    if (isLiked) {
    btn.classList.add('liked');
    icon.className = 'fas fa-heart';
} else {
    btn.classList.remove('liked');
    icon.className = 'far fa-heart';
}

    count.textContent = likesCount;

    // Добавляем небольшую анимацию
    btn.style.transform = 'scale(0.95)';
    setTimeout(() => {
    btn.style.transform = 'scale(1)';
}, 150);
}

    // Функция для обновления информации о лайках
    async function updateLikesInfo(postId) {
    try {
    const response = await fetch(`/api/posts/${postId}/likes`);
    if (!response.ok) return;

    const likesInfo = await response.json();

    // Находим карточку поста
    const postCard = document.querySelector(`[data-post-id="${postId}"]`)?.closest('.post-card');
    if (!postCard) return;

    let likesInfoBlock = postCard.querySelector('.post-likes-info');

    if (likesInfo.likesCount > 0 && likesInfo.recentLikes.length > 0) {
    if (!likesInfoBlock) {
    // Создаем блок с информацией о лайках
    likesInfoBlock = createLikesInfoBlock(likesInfo);
    const postActions = postCard.querySelector('.post-actions');
    if (postActions) {
    postActions.parentNode.insertBefore(likesInfoBlock, postActions);
}
} else {
    // Обновляем существующий блок
    updateLikesInfoBlock(likesInfoBlock, likesInfo);
}
} else if (likesInfoBlock) {
    // Удаляем блок если лайков нет
    likesInfoBlock.remove();
}

} catch (error) {
    console.error('Ошибка при загрузке информации о лайках:', error);
}
}

    // Создание блока с информацией о лайках
    function createLikesInfoBlock(likesInfo) {
    const block = document.createElement('div');
    block.className = 'post-likes-info';
    updateLikesInfoBlock(block, likesInfo);
    return block;
}

    // Обновление блока с информацией о лайках
    function updateLikesInfoBlock(block, likesInfo) {
    const maxAvatars = 3; // Для карточек постов показываем меньше аватаров
    const avatarsHtml = likesInfo.recentLikes.slice(0, maxAvatars).map(user => {
    const fullName = user.fullName || `${user.firstName} ${user.lastName || ''}`.trim();
    if (user.imageUrl) {
    return `<div class="like-avatar">
                    <img src="${user.imageUrl}" alt="${fullName}" title="${fullName}" class="like-avatar-img">
                </div>`;
} else {
    const initial = user.firstName ? user.firstName.charAt(0).toUpperCase() : '?';
    return `<div class="like-avatar">
                    <div class="like-avatar-default" title="${fullName}">
                        <span>${initial}</span>
                    </div>
                </div>`;
}
}).join('');

    const moreCount = likesInfo.likesCount > maxAvatars ? likesInfo.likesCount - maxAvatars : 0;
    const moreHtml = moreCount > 0 ? `<div class="like-more"><span>+${moreCount}</span></div>` : '';

    let likesText = '';
    if (likesInfo.likesCount === 1) {
    const userName = likesInfo.recentLikes[0].fullName || likesInfo.recentLikes[0].firstName;
    likesText = `Нравится <strong>${userName}</strong>`;
} else if (likesInfo.likesCount === 2) {
    const user1 = likesInfo.recentLikes[0].fullName || likesInfo.recentLikes[0].firstName;
    const user2 = likesInfo.recentLikes[1].fullName || likesInfo.recentLikes[1].firstName;
    likesText = `Нравится <strong>${user1}</strong> и <strong>${user2}</strong>`;
} else {
    const user1 = likesInfo.recentLikes[0].fullName || likesInfo.recentLikes[0].firstName;
    likesText = `Нравится <strong>${user1}</strong> и еще <strong>${likesInfo.likesCount - 1}</strong> пользователям`;
}

    block.innerHTML = `
            <div class="likes-avatars">
                ${avatarsHtml}
                ${moreHtml}
            </div>
            <div class="likes-text">
                <span>${likesText}</span>
            </div>
        `;
}

    function sharePost(btn) {
    const postId = btn.dataset.postId;
    const url = window.location.origin + `/posts/${postId}`;

    if (navigator.share) {
    navigator.share({
    title: 'Пост',
    url: url
}).catch(err => {
    console.log('Ошибка при sharing:', err);
});
} else {
    navigator.clipboard.writeText(url).then(() => {
    showNotification('Ссылка на пост скопирована! 📋', 'success');
}).catch(err => {
    console.error('Ошибка при копировании:', err);
    showNotification('Не удалось скопировать ссылку', 'error');
});
}
}

    // Навигация к посту при клике на контент
    function navigateToPost(event, element) {
    // Не переходим если кликнули на ссылку "Читать далее"
    if (event.target.closest('.read-more-link')) {
    return;
}

    // Не переходим если кликнули на изображение (оно уже кликабельно)
    if (event.target.closest('.post-image')) {
    return;
}

    const postId = element.dataset.postId;
    if (postId) {
    window.location.href = `/posts/${postId}`;
}
}

    // Копирование ссылки на пост
    function copyPostLink(element) {
    const postId = element.dataset.postId;
    const postUrl = `${window.location.origin}/posts/${postId}`;

    navigator.clipboard.writeText(postUrl).then(() => {
    showNotification('Ссылка на пост скопирована! 📋', 'success');
}).catch(err => {
    console.error('Ошибка при копировании: ', err);
    showNotification('Не удалось скопировать ссылку', 'error');
});
}

    function loadPage(page) {
    const currentUrl = new URL(window.location);
    currentUrl.searchParams.set('page', page);
    window.location.href = currentUrl.toString();
}

    function loadMorePosts() {
    showNotification('Функция загрузки постов в разработке...', 'success');
}

    // Функции для удаления поста
    let postToDelete = null;

    function deletePost(event, element) {
    event.preventDefault();
    event.stopPropagation();

    const postId = element.getAttribute('data-post-id');
    if (!postId) {
    console.error('ID поста не найден');
    return;
}

    postToDelete = postId;

    const modal = document.getElementById('deleteConfirmModal');
    if (modal) {
    modal.style.display = 'flex';
    document.body.style.overflow = 'hidden';
}
}

    function closeDeleteModal() {
    const modal = document.getElementById('deleteConfirmModal');
    if (modal) {
    modal.style.display = 'none';
    document.body.style.overflow = '';
}
    postToDelete = null;
}

    async function confirmDelete() {
    if (!postToDelete) {
    console.error('ID поста для удаления не найден');
    return;
}

    const confirmBtn = document.getElementById('confirmDeleteBtn');
    const btnText = confirmBtn.querySelector('.btn-text');
    const btnSpinner = confirmBtn.querySelector('.btn-spinner');

    confirmBtn.disabled = true;
    if (btnText) btnText.style.opacity = '0';
    if (btnSpinner) btnSpinner.style.display = 'inline-block';

    try {
    const response = await fetch(`/api/posts/${postToDelete}`, {
    method: 'DELETE',
    headers: {
    'Content-Type': 'application/json'
}
});

    const result = await response.json();

    if (response.ok && result.success) {
    showNotification('Пост успешно удален', 'success');

    const postElement = document.querySelector(`[data-post-id="${postToDelete}"]`);
    if (postElement) {
    const postCard = postElement.closest('article.post-card');
    if (postCard) {
    postCard.style.transition = 'opacity 0.3s ease, transform 0.3s ease';
    postCard.style.opacity = '0';
    postCard.style.transform = 'translateY(-20px)';

    setTimeout(() => {
    postCard.remove();

    const remainingPosts = document.querySelectorAll('article.post-card');
    if (remainingPosts.length === 0) {
    window.location.reload();
}
}, 300);
}
}

    closeDeleteModal();

} else {
    const errorMessage = result.error || 'Произошла ошибка при удалении поста';
    showNotification(errorMessage, 'error');
}

} catch (error) {
    console.error('Ошибка при удалении поста:', error);
    showNotification('Произошла ошибка при удалении поста', 'error');
} finally {
    confirmBtn.disabled = false;
    if (btnText) btnText.style.opacity = '1';
    if (btnSpinner) btnSpinner.style.display = 'none';
}
}

    // Обработчики событий
    document.addEventListener('DOMContentLoaded', function() {
    // Закрытие меню при клике вне его
    document.addEventListener('click', function (e) {
        if (!e.target.closest('.post-menu')) {
            document.querySelectorAll('.post-menu-dropdown').forEach(menu => {
                menu.classList.remove('show');
            });
        }
    });

    // Закрытие модального окна по клику вне его
    document.addEventListener('click', function(event) {
    const modal = document.getElementById('deleteConfirmModal');
    if (event.target === modal) {
    closeDeleteModal();
}
});

    // Закрытие модального окна по нажатию Escape
    document.addEventListener('keydown', function(event) {
    if (event.key === 'Escape') {
    closeDeleteModal();
}
});

    // Закрытие уведомления по клику
    document.addEventListener('click', function(e) {
    if (e.target.closest('#notification')) {
    hideNotification();
}
});
});
