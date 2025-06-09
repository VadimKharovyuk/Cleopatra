
    function togglePrivacy(checkbox) {
    const toggleSwitch = checkbox.closest('.toggle-switch');
    const card = checkbox.closest('.privacy-toggle-card');

    // Добавляем состояние загрузки
    toggleSwitch.classList.add('toggle-loading');

    // Отправляем AJAX запрос
    fetch('/settings/privacy/toggle', {
    method: 'POST',
    headers: {
    'Content-Type': 'application/json',
    'X-Requested-With': 'XMLHttpRequest'
},
    body: JSON.stringify({
    isPrivateProfile: checkbox.checked
})
})
    .then(response => response.json())
    .then(data => {
    // Убираем состояние загрузки
    toggleSwitch.classList.remove('toggle-loading');

    if (data.success) {
    // Показываем уведомление об успехе
    showNotification(
    checkbox.checked ?
    'Профиль стал приватным' :
    'Профиль стал публичным',
    'success'
    );

    // Обновляем описание карточки
    const description = card.querySelector('.card-description');
    description.textContent = checkbox.checked ?
    'Только подписчики смогут видеть ваши посты и активность' :
    'Все пользователи могут видеть ваш профиль и контент';

} else {
    // В случае ошибки возвращаем чекбокс в предыдущее состояние
    checkbox.checked = !checkbox.checked;
    showNotification('Ошибка при обновлении настроек', 'error');
}
})
    .catch(error => {
    console.error('Error:', error);
    // Убираем состояние загрузки и возвращаем чекбокс
    toggleSwitch.classList.remove('toggle-loading');
    checkbox.checked = !checkbox.checked;
    showNotification('Произошла ошибка. Попробуйте еще раз.', 'error');
});
}

    function showNotification(message, type = 'success') {
    // Удаляем существующие уведомления
    const existingNotifications = document.querySelectorAll('.privacy-notification');
    existingNotifications.forEach(notification => notification.remove());

    // Создаем новое уведомление
    const notification = document.createElement('div');
    notification.className = `privacy-notification ${type}`;
    notification.innerHTML = `
            <i class="fas fa-${type === 'success' ? 'check-circle' : 'exclamation-triangle'}"></i>
            <span>${message}</span>
        `;

    document.body.appendChild(notification);

    // Автоматически удаляем через 3 секунды
    setTimeout(() => {
    notification.style.animation = 'slideOutRight 0.3s ease-out';
    setTimeout(() => {
    if (notification.parentNode) {
    notification.parentNode.removeChild(notification);
}
}, 300);
}, 3000);
}

    // Инициализация при загрузке страницы
    document.addEventListener('DOMContentLoaded', function() {
    const privacyToggle = document.getElementById('privacyToggle');
    if (privacyToggle) {
    // Устанавливаем правильное описание при загрузке
    const card = privacyToggle.closest('.privacy-toggle-card');
    const description = card.querySelector('.card-description');

    if (privacyToggle.checked) {
    description.textContent = 'Только подписчики смогут видеть ваши посты и активность';
} else {
    description.textContent = 'Все пользователи могут видеть ваш профиль и контент';
}
}
});
