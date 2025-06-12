package com.example.cleopatra.service;
import com.example.cleopatra.model.Post;
import com.example.cleopatra.model.PostMention;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.PostMentionRepository;
import com.example.cleopatra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MentionService {

    private final UserRepository userRepository;
    private final PostMentionRepository postMentionRepository;
    // private final NotificationService notificationService; // раскомментируйте когда будет готов

    // Паттерн для поиска упоминаний: @Имя Фамилия или @"Имя Фамилия"
    private static final Pattern MENTION_PATTERN = Pattern.compile(
            "@(?:\"([^\"]+)\"|(\\S+(?:\\s+\\S+)?))(?=\\s|$|[.,!?;:])"
    );

    /**
     * Извлекает все упоминания из текста поста
     */
    public Set<String> extractMentions(String content) {
        if (content == null || content.trim().isEmpty()) {
            return Set.of();
        }

        Set<String> mentions = new java.util.HashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(content);

        while (matcher.find()) {
            String mention = null;
            if (matcher.group(1) != null) {
                // Упоминание в кавычках: @"Имя Фамилия"
                mention = matcher.group(1);
            } else if (matcher.group(2) != null) {
                // Упоминание без кавычек: @Имя или @Имя Фамилия
                mention = matcher.group(2);
            }

            if (mention != null && !mention.trim().isEmpty()) {
                mentions.add(mention.trim().toLowerCase());
            }
        }

        log.debug("Найдены упоминания: {}", mentions);
        return mentions;
    }

    /**
     * Находит пользователей по упоминаниям
     */
    public List<User> findUsersByMentions(Set<String> mentions) {
        if (mentions.isEmpty()) {
            return List.of();
        }
        return userRepository.findUsersByMentions(mentions);
    }

    /**
     * Создает упоминания для поста
     * ✅ ИСПРАВЛЕННАЯ ВЕРСИЯ
     */
    @Transactional
    public void createPostMentions(Post post) {
        // Проверяем, что пост сохранен и имеет ID
        if (post.getId() == null) {
            log.error("Попытка создать упоминания для несохраненного поста");
            throw new IllegalArgumentException("Пост должен быть сохранен перед созданием упоминаний");
        }

        // Получаем автора из поста
        User author = post.getAuthor();
        if (author == null) {
            log.warn("Автор поста не найден для поста с ID: {}", post.getId());
            return;
        }

        Set<String> mentions = extractMentions(post.getContent());

        if (mentions.isEmpty()) {
            log.debug("Упоминания не найдены в посте ID: {}", post.getId());
            return;
        }

        log.info("Обработка {} упоминаний для поста ID: {}", mentions.size(), post.getId());

        List<User> mentionedUsers = findUsersByMentions(mentions);

        // Исключаем автора поста из упоминаний
        mentionedUsers = mentionedUsers.stream()
                .filter(user -> !user.getId().equals(author.getId()))
                .collect(Collectors.toList());

        if (mentionedUsers.isEmpty()) {
            log.info("Пользователи для упоминания не найдены или все упоминания указывают на автора поста");
            return;
        }

        // Проверяем, не существуют ли уже упоминания для этого поста
        List<PostMention> existingMentions = postMentionRepository.findByPostId(post.getId());
        Set<Long> existingMentionedUserIds = existingMentions.stream()
                .map(mention -> mention.getMentionedUser().getId())
                .collect(Collectors.toSet());

        // Фильтруем пользователей, которые еще не упомянуты
        List<User> newMentionedUsers = mentionedUsers.stream()
                .filter(user -> !existingMentionedUserIds.contains(user.getId()))
                .toList();

        if (newMentionedUsers.isEmpty()) {
            log.info("Все найденные пользователи уже упомянуты в посте ID: {}", post.getId());
            return;
        }

        // Создаем упоминания батчем для лучшей производительности
        List<PostMention> mentionsToSave = newMentionedUsers.stream()
                .map(mentionedUser -> PostMention.builder()
                        .post(post)
                        .mentionedUser(mentionedUser)
                        .mentionedBy(author)
                        .build())
                .collect(Collectors.toList());

        // Сохраняем все упоминания одним запросом
        List<PostMention> savedMentions = postMentionRepository.saveAll(mentionsToSave);

        // Отправляем уведомления
        for (PostMention mention : savedMentions) {
            try {
                // notificationService.sendMentionNotification(mention.getMentionedUser(), post, author);
                log.info("Создано упоминание: {} упомянут пользователем {} в посте {}",
                        mention.getMentionedUser().getFirstName() + " " + mention.getMentionedUser().getLastName(),
                        author.getFirstName() + " " + author.getLastName(),
                        post.getId());
            } catch (Exception e) {
                log.error("Ошибка отправки уведомления пользователю {}: {}",
                        mention.getMentionedUser().getId(), e.getMessage());
            }
        }

        log.info("Успешно создано {} новых упоминаний для поста ID: {}", savedMentions.size(), post.getId());
    }

    /**
     * ✅ ДОБАВИТЬ ЭТОТ МЕТОД в ваш MentionService
     * Поиск пользователей для автодополнения упоминаний
     */
    public List<User> searchUsersForMentions(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        String trimmedQuery = query.trim();
        log.debug("Поиск пользователей для упоминаний по запросу: '{}'", trimmedQuery);

        List<User> users = userRepository.searchUsersForMentions(trimmedQuery);

        log.debug("Найдено {} пользователей для запроса: '{}'", users.size(), trimmedQuery);
        return users;
    }








    /**
     * ✅ АЛЬТЕРНАТИВНЫЙ МЕТОД: С кэшированием для производительности
     */
    public String convertMentionsToLinksWithCache(String content, Long postId) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }

        // Получаем все упоминания для поста из базы данных
        List<PostMention> postMentions = postMentionRepository.findByPostId(postId);
        Map<String, User> mentionCache = postMentions.stream()
                .collect(Collectors.toMap(
                        mention -> (mention.getMentionedUser().getFirstName() + " " +
                                mention.getMentionedUser().getLastName()).toLowerCase(),
                        PostMention::getMentionedUser,
                        (existing, replacement) -> existing // В случае дубликатов берем первый
                ));

        Pattern mentionPattern = Pattern.compile("@(?:\"([^\"]+)\"|([\\S]+(?:\\s+[\\S]+)?))");
        Matcher matcher = mentionPattern.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String mentionText = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);

            if (mentionText != null && !mentionText.trim().isEmpty()) {
                User mentionedUser = mentionCache.get(mentionText.trim().toLowerCase());

                if (mentionedUser != null) {
                    String link = String.format(
                            "<a href=\"/profile/%d\" class=\"mention-link\" title=\"%s %s\">@%s</a>",
                            mentionedUser.getId(),
                            mentionedUser.getFirstName(),
                            mentionedUser.getLastName(),
                            mentionText
                    );
                    matcher.appendReplacement(result, link);
                } else {
                    String styledMention = String.format(
                            "<span class=\"mention-inactive\">@%s</span>",
                            mentionText
                    );
                    matcher.appendReplacement(result, styledMention);
                }
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }


    /**
     * Получает все упоминания пользователя
     */
    public List<PostMention> getUserMentions(User user) {
        return postMentionRepository.findByMentionedUserOrderByCreatedAtDesc(user);
    }

    /**
     * Получает упоминания для конкретного поста
     */
    public List<PostMention> getPostMentions(Long postId) {
        return postMentionRepository.findByPostId(postId);
    }



    /**
     * Проверяет, упомянут ли пользователь в посте
     */
    public boolean isUserMentioned(Long postId, Long userId) {
        return postMentionRepository.existsByPostIdAndMentionedUserId(postId, userId);
    }
}