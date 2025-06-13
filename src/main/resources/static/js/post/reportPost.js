// JavaScript только для кнопки жалобы
function reportPost(postId) {
    showReportModal(postId);
}

function showReportModal(postId) {
    const modalHTML = `
        <div class="modal fade" id="reportModal" tabindex="-1" aria-labelledby="reportModalLabel" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content">
                    <div class="modal-header bg-danger text-white">
                        <h5 class="modal-title" id="reportModalLabel">
                            <i class="fas fa-flag me-2"></i>
                            Пожаловаться на пост
                        </h5>
                        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <form id="reportForm">
                            <div class="mb-3">
                                <label for="reportReason" class="form-label">
                                    Причина жалобы <span class="text-danger">*</span>
                                </label>
                                <select class="form-select" id="reportReason" name="reason" required>
                                    <option value="">Выберите причину</option>
                                    <option value="SPAM">Спам</option>
                                    <option value="INAPPROPRIATE">Неподходящий контент</option>
                                    <option value="VIOLENCE">Насилие</option>
                                    <option value="ADULT_CONTENT">Контент 18+</option>
                                    <option value="FRAUD">Мошенничество</option>
                                    <option value="OTHER">Другое</option>
                                </select>
                                <div class="invalid-feedback">
                                    Выберите причину жалобы
                                </div>
                            </div>
                            
                            <div class="mb-3">
                                <label for="reportDescription" class="form-label">
                                    Дополнительная информация
                                </label>
                                <textarea class="form-control" 
                                        id="reportDescription" 
                                        name="description" 
                                        rows="3" 
                                        maxlength="1000"
                                        placeholder="Опишите проблему..."></textarea>
                                <div class="form-text">
                                    <span id="charCount">0</span>/1000 символов
                                </div>
                            </div>
                        </form>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">
                            Отмена
                        </button>
                        <button type="button" class="btn btn-danger" onclick="submitReport(${postId})">
                            Отправить жалобу
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `;

    // Удаляем старый модал
    const existingModal = document.getElementById('reportModal');
    if (existingModal) {
        existingModal.remove();
    }

    // Добавляем модал
    document.body.insertAdjacentHTML('beforeend', modalHTML);

    // Настраиваем счетчик символов
    const textarea = document.getElementById('reportDescription');
    const charCount = document.getElementById('charCount');
    textarea.addEventListener('input', function() {
        charCount.textContent = this.value.length;
    });

    // Показываем модал
    const modal = new bootstrap.Modal(document.getElementById('reportModal'));
    modal.show();

    // Удаляем после закрытия
    document.getElementById('reportModal').addEventListener('hidden.bs.modal', function() {
        this.remove();
    });
}

async function submitReport(postId) {
    const reason = document.getElementById('reportReason').value;
    const description = document.getElementById('reportDescription').value;

    // Валидация
    if (!reason) {
        document.getElementById('reportReason').classList.add('is-invalid');
        return;
    }

    const reportData = {
        postId: postId,
        reason: reason,
        description: description
    };

    try {
        const response = await fetch('/api/reports', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(reportData)
        });

        const result = await response.json();

        if (result.success) {
            // Закрываем модал
            const modal = bootstrap.Modal.getInstance(document.getElementById('reportModal'));
            modal.hide();

            // Обновляем кнопку
            const reportBtn = document.getElementById(`reportBtn_${postId}`);
            if (reportBtn) {
                reportBtn.innerHTML = '<i class="fas fa-check"></i> <span class="d-none d-sm-inline ms-1">Жалоба отправлена</span>';
                reportBtn.disabled = true;
                reportBtn.classList.remove('btn-outline-secondary');
                reportBtn.classList.add('btn-outline-success');
            }

            alert('Жалоба успешно отправлена!');
        } else {
            alert(result.message || 'Ошибка при отправке жалобы');
        }
    } catch (error) {
        console.error('Ошибка:', error);
        alert('Произошла ошибка. Попробуйте позже.');
    }
}