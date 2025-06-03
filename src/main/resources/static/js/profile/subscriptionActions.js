
    async function simpleToggleFollow() {
    console.log('🔄 Простая подписка');

    const currentUserId = document.body.dataset.currentUserId;
    const profileUserId = document.body.dataset.profileUserId;

    if (!currentUserId) {
    window.location.href = '/login';
    return;
}

    const btn = document.getElementById('follow-btn');
    const icon = document.getElementById('follow-icon');
    const text = document.getElementById('follow-text');

    // Показываем загрузку
    btn.disabled = true;
    btn.style.opacity = '0.7';
    icon.className = 'fas fa-spinner fa-spin';

    try {
    const response = await fetch('/api/subscriptions/toggle', {
    method: 'POST',
    headers: {
    'Content-Type': 'application/json'
},
    body: JSON.stringify({
    subscribedToId: parseInt(profileUserId)
})
});

    const result = await response.json();
    console.log('Результат:', result);

    if (result.success) {
    // Обновляем кнопку
    if (result.isSubscribed) {
    btn.className = 'btn-luxury btn-secondary-luxury';
    icon.className = 'fas fa-user-check';
    text.textContent = 'Подписан';
} else {
    btn.className = 'btn-luxury btn-primary-luxury';
    icon.className = 'fas fa-user-plus';
    text.textContent = 'Подписаться';
}

    // Обновляем счетчик
    const followersCount = document.querySelector('.stats-section .stat-item:nth-child(2) .stat-number');
    if (followersCount && result.followersCount !== undefined) {
    followersCount.textContent = result.followersCount;
}

    alert(result.message);
} else {
    alert('Ошибка: ' + result.message);
}
} catch (error) {
    console.error('Ошибка:', error);
    alert('Произошла ошибка при подписке');
} finally {
    // Убираем загрузку
    btn.disabled = false;
    btn.style.opacity = '1';
}
}
