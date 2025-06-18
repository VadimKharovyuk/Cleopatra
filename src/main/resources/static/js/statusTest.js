// <script th:inline="javascript">
//     window.appData = {
//     currentUserId: [[${currentUserId}]] || null,
//     profileUserId: [[${user?.id}]] || null,
//     isSubscribed: [[${isSubscribed ?: false}]],
//     hasStatusTracking: [[${hasStatusTracking ?: false}]]
// };
// </script>
//
// <!-- Условное подключение скрипта статуса -->
// <script th:if="${hasStatusTracking ?: false}" th:src="@{/js/online-status.js}"></script>