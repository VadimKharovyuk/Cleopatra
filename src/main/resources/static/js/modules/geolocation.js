// static/js/modules/geolocation.js

class GeolocationManager {
    static toggleLocationSection() {
        const controls = document.getElementById('locationControls');
        const toggleText = document.getElementById('locationToggleText');

        if (controls.classList.contains('active')) {
            controls.classList.remove('active');
            toggleText.textContent = 'Добавить местоположение';
        } else {
            controls.classList.add('active');
            toggleText.textContent = 'Скрыть местоположение';

            // Автоматически запрашиваем местоположение при открытии секции
            setTimeout(() => {
                GeolocationManager.getCurrentLocation();
            }, 300);
        }
    }

    static getCurrentLocation() {
        const btn = event.target;
        btn.classList.add('loading');
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Получаем...';

        if (!navigator.geolocation) {
            NotificationManager.show('Геолокация не поддерживается этим браузером', 'error');
            GeolocationManager.resetLocationButton(btn);
            return;
        }

        navigator.geolocation.getCurrentPosition(
            function(position) {
                const lat = position.coords.latitude;
                const lng = position.coords.longitude;

                document.getElementById('latitudeInput').value = lat;
                document.getElementById('longitudeInput').value = lng;

                // Обновляем скрытые поля
                document.getElementById('latitude').value = lat;
                document.getElementById('longitude').value = lng;

                GeolocationManager.showCurrentLocation(lat, lng, 'Текущее местоположение');
                GeolocationManager.resetLocationButton(btn);

                console.log('📍 Получены координаты:', lat, lng);
            },
            function(error) {
                console.error('Ошибка геолокации:', error);
                NotificationManager.show('Не удалось получить местоположение', 'error');
                GeolocationManager.resetLocationButton(btn);
            }
        );
    }

    static resetLocationButton(btn) {
        btn.classList.remove('loading');
        btn.innerHTML = '<i class="fas fa-crosshairs"></i> Моё местоположение';
    }

    static showCurrentLocation(lat, lng, placeName) {
        const display = document.getElementById('currentLocationDisplay');
        const text = document.getElementById('currentLocationText');

        text.textContent = `${placeName} (${lat.toFixed(4)}, ${lng.toFixed(4)})`;
        display.style.display = 'block';
    }

    static clearLocation() {
        // Очищаем все поля
        document.getElementById('latitudeInput').value = '';
        document.getElementById('longitudeInput').value = '';

        // Очищаем скрытые поля
        document.getElementById('locationId').value = '';
        document.getElementById('latitude').value = '';
        document.getElementById('longitude').value = '';

        // Скрываем отображение текущей локации
        document.getElementById('currentLocationDisplay').style.display = 'none';

        console.log('🗑️ Локация очищена');
    }

    static init() {
        // Обработчики изменения координат
        const latInput = document.getElementById('latitudeInput');
        const lngInput = document.getElementById('longitudeInput');

        if (latInput) {
            latInput.addEventListener('input', function() {
                const value = this.value.trim();
                document.getElementById('latitude').value = value;
                console.log('📍 Обновлена широта:', value);
            });
        }

        if (lngInput) {
            lngInput.addEventListener('input', function() {
                const value = this.value.trim();
                document.getElementById('longitude').value = value;
                console.log('📍 Обновлена долгота:', value);
            });
        }
    }
}

// Глобальные функции для совместимости с HTML onclick
window.toggleLocationSection = () => GeolocationManager.toggleLocationSection();
window.getCurrentLocation = () => GeolocationManager.getCurrentLocation();
window.clearLocation = () => GeolocationManager.clearLocation();

window.GeolocationManager = GeolocationManager;