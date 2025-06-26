//package com.example.cleopatra.service.impl;
//
//import com.example.cleopatra.ExistsException.*;
//import com.example.cleopatra.dto.GroupDto.CreateGroupRequestDTO;
//import com.example.cleopatra.dto.GroupDto.GroupPageResponse;
//import com.example.cleopatra.dto.GroupDto.GroupResponseDTO;
//import com.example.cleopatra.enums.*;
//import com.example.cleopatra.maper.GroupMapper;
//import com.example.cleopatra.model.Group;
//import com.example.cleopatra.model.GroupMembership;
//import com.example.cleopatra.model.User;
//import com.example.cleopatra.repository.GroupMembershipRepository;
//import com.example.cleopatra.repository.GroupRepository;
//import com.example.cleopatra.repository.UserRepository;
//import com.example.cleopatra.service.*;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Slice;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.stream.Collectors;
//
//
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class GroupServiceImpl implements GroupService {
//    private final GroupRepository groupRepository;
//    private final GroupMapper groupMapper;
//    private final StorageService storageService;
//    private final ImageValidator imageValidator ;
//    private final UserService userService;
//    private final GroupMembershipRepository groupMembershipRepository;
//
//
//    @Override
//    @Transactional
//    public GroupResponseDTO createGroup(CreateGroupRequestDTO createGroupRequestDTO, Long ownerId) {
//        log.info("Начинаем создание группы '{}' для пользователя с ID: {}",
//                createGroupRequestDTO.getName(), ownerId);
//
//        try {
//            // 1. Проверяем существование пользователя-владельца
//            User owner = userService.findById(ownerId);
//            if (owner == null) {
//                throw new UsernameNotFoundException("Пользователь с ID " + ownerId + " не найден");
//            }
//
//            // 2. Проверяем, не заблокирован ли пользователь
//            if (owner.isBlocked()) {
//                throw new UserBlockedException("Заблокированный пользователь не может создавать группы");
//            }
//
//            // 3. Создаем Group entity из DTO
//            Group group = groupMapper.toEntity(createGroupRequestDTO);
//            group.setOwnerId(ownerId);
//
//            // 4. Обрабатываем загрузку основного изображения группы
//            if (createGroupRequestDTO.getImage() != null && !createGroupRequestDTO.getImage().isEmpty()) {
//                log.debug("Обработка основного изображения группы");
//                StorageService.StorageResult imageResult = processImage(
//                        createGroupRequestDTO.getImage(),
//                        "основного изображения группы"
//                );
//                group.setImageUrl(imageResult.getUrl());
//                group.setImgId(imageResult.getImageId());
//            }
//
//            // 5. Обрабатываем загрузку фонового изображения
//            if (createGroupRequestDTO.getBackgroundImage() != null && !createGroupRequestDTO.getBackgroundImage().isEmpty()) {
//                log.debug("Обработка фонового изображения группы");
//                StorageService.StorageResult backgroundResult = processImage(
//                        createGroupRequestDTO.getBackgroundImage(),
//                        "фонового изображения группы"
//                );
//                group.setBackgroundImageUrl(backgroundResult.getUrl());
//                group.setBackgroundImgId(backgroundResult.getImageId());
//            }
//
//            // 6. Сохраняем группу в базу данных
//            Group savedGroup = groupRepository.save(group);
//            log.info("Группа '{}' успешно создана с ID: {}", savedGroup.getName(), savedGroup.getId());
//
//            // 7. Создаем GroupMembership для владельца группы
//            createOwnerMembership(savedGroup.getId(), ownerId);
//
//            // 8. Обновляем счетчик участников
//            savedGroup.setMemberCount(1L);
//            savedGroup = groupRepository.save(savedGroup);
//
//            // 9. Конвертируем в DTO и возвращаем
//            return groupMapper.toResponseDTO(savedGroup, ownerId);
//        } catch (IOException e) {
//            log.error("Ошибка при загрузке изображений для группы '{}': {}",
//                    createGroupRequestDTO.getName(), e.getMessage());
//            throw new ImageUploadException("Не удалось загрузить изображения группы: " + e.getMessage(), e);
//        } catch (Exception e) {
//            log.error("Ошибка при создании группы '{}': {}",
//                    createGroupRequestDTO.getName(), e.getMessage());
//            throw new GroupCreationException("Не удалось создать группу: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public GroupResponseDTO getGroupById(Long groupId, Long currentUserId) {
//        Group group = groupRepository.findById(groupId).orElse(null);
//        if (group == null) {
//            throw new GroupNotFoundException("Группа с ID " + groupId + " не найдена");
//        }
//
//        return groupMapper.toResponseDTO(group, currentUserId);
//    }
//
//    @Override
//    public GroupPageResponse getAllPublicGroups(Pageable pageable) {
//        log.info("Получение публичных групп, страница: {}, размер: {}",
//                pageable.getPageNumber(), pageable.getPageSize());
//
//        try {
//            // 1. Получаем публичные и закрытые группы (исключаем приватные)
//            Slice<Group> groupSlice = groupRepository.findByGroupTypeInAndGroupStatus(
//                    List.of(GroupType.OPEN, GroupType.CLOSED),
//                    GroupStatus.ACTIVE,
//                    pageable
//            );
//
//            log.debug("Найдено {} публичных групп на странице {}",
//                    groupSlice.getNumberOfElements(), pageable.getPageNumber());
//
//            // 2. Конвертируем в DTO (без currentUserId, так как это публичный метод)
//            List<GroupResponseDTO> groupDTOs = groupSlice.getContent()
//                    .stream()
//                    .map(group -> groupMapper.toSimpleResponseDTO(group, null))
//                    .collect(Collectors.toList());
//
//            // 3. Создаем ответ с метаданными пагинации
//            return GroupPageResponse.builder()
//                    .groups(groupDTOs)
//                    .hasNext(groupSlice.hasNext())
//                    .currentPage(pageable.getPageNumber())
//                    .size(pageable.getPageSize())
//                    .isEmpty(groupSlice.isEmpty())
//                    .numberOfElements(groupSlice.getNumberOfElements())
//                    .build();
//
//        } catch (Exception e) {
//            log.error("Ошибка при получении публичных групп: {}", e.getMessage());
//            throw new GroupRetrievalException("Не удалось получить список публичных групп: " + e.getMessage());
//        }
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public GroupPageResponse getAllAvailableGroups(Pageable pageable, Long currentUserId) {
//        log.info("Получение доступных групп для пользователя ID: {}, страница: {}, размер: {}",
//                currentUserId, pageable.getPageNumber(), pageable.getPageSize());
//
//        if (currentUserId == null) {
//            log.warn("Попытка получить доступные группы без указания пользователя");
//            throw new IllegalArgumentException("ID пользователя обязателен для получения доступных групп");
//        }
//
//        try {
//            // 1. Проверяем существование пользователя
//            User user = userService.findById(currentUserId);
//            if (user == null) {
//                throw new UsernameNotFoundException("Пользователь с ID " + currentUserId + " не найден");
//            }
//
//            // 2. Получаем группы доступные пользователю
//            Slice<Group> groupSlice = groupRepository.findAvailableGroupsForUser(
//                    currentUserId,
//                    GroupStatus.ACTIVE,
//                    pageable
//            );
//
//            log.debug("Найдено {} доступных групп для пользователя ID: {} на странице {}",
//                    groupSlice.getNumberOfElements(), currentUserId, pageable.getPageNumber());
//
//            // 3. Конвертируем в DTO с информацией о статусе пользователя
//            List<GroupResponseDTO> groupDTOs = groupSlice.getContent()
//                    .stream()
//                    .map(group -> groupMapper.toResponseDTO(group, currentUserId))
//                    .collect(Collectors.toList());
//
//            // 4. Создаем ответ с метаданными пагинации
//            return GroupPageResponse.builder()
//                    .groups(groupDTOs)
//                    .hasNext(groupSlice.hasNext())
//                    .currentPage(pageable.getPageNumber())
//                    .size(pageable.getPageSize())
//                    .isEmpty(groupSlice.isEmpty())
//                    .numberOfElements(groupSlice.getNumberOfElements())
//                    .build();
//
//        } catch (UsernameNotFoundException e) {
//            log.error("Пользователь не найден при получении доступных групп: {}", e.getMessage());
//            throw e;
//        } catch (Exception e) {
//            log.error("Ошибка при получении доступных групп для пользователя ID {}: {}",
//                    currentUserId, e.getMessage());
//            throw new GroupRetrievalException("Не удалось получить список доступных групп: " + e.getMessage());
//        }
//    }
//
//
//
//    @Override
//    @Transactional
//    public void deleteGroup(Long groupId, Long currentUserId) {
//        log.info("Удаление группы ID: {} пользователем ID: {}", groupId, currentUserId);
//
//        try {
//            // 1. Проверяем существование группы
//            Group group = groupRepository.findById(groupId).orElse(null);
//            if (group == null) {
//                throw new GroupNotFoundException("Группа с ID " + groupId + " не найдена");
//            }
//
//            // 2. Проверяем существование пользователя
//            User user = userService.findById(currentUserId);
//            if (user == null) {
//                throw new UsernameNotFoundException("Пользователь с ID " + currentUserId + " не найден");
//            }
//
//            // 3. Проверяем права на удаление (только владелец или админ системы)
//            if (!group.getOwnerId().equals(currentUserId) && !isSystemAdmin(user)) {
//                throw new GroupAccessDeniedException("Недостаточно прав для удаления группы");
//            }
//
//            // 4. Проверяем, не удалена ли уже группа
//            if (GroupStatus.DELETED.equals(group.getGroupStatus())) {
//                throw new GroupAlreadyDeletedException("Группа уже удалена");
//            }
//
//            // 5. Soft delete - меняем статус вместо физического удаления
//            group.setGroupStatus(GroupStatus.DELETED);
//            groupRepository.save(group);
//
//            // 6. Опционально: удаляем изображения из хранилища
//            deleteGroupImages(group);
//
//            log.info("Группа '{}' (ID: {}) успешно удалена пользователем ID: {}",
//                    group.getName(), groupId, currentUserId);
//
//        } catch (GroupNotFoundException | UsernameNotFoundException | GroupAccessDeniedException | GroupAlreadyDeletedException e) {
//            log.error("Ошибка при удалении группы ID {}: {}", groupId, e.getMessage());
//            throw e;
//        } catch (Exception e) {
//            log.error("Неожиданная ошибка при удалении группы ID {}: {}", groupId, e.getMessage());
//            throw new GroupDeletionException("Не удалось удалить группу: " + e.getMessage(), e);
//        }
//    }
//
//    /**
//     * Проверяет, является ли пользователь админом системы
//     */
//    private boolean isSystemAdmin(User user) {
//        return Role.ADMIN.equals(user.getRole()) || GroupRole.ADMIN.equals(user.getRole());
//    }
//
//    /**
//     * Удаляет изображения группы из хранилища
//     */
//    private void deleteGroupImages(Group group) {
//        try {
//            if (group.getImgId() != null) {
//                storageService.deleteImage(group.getImgId());
//                log.debug("Удалено основное изображение группы: {}", group.getImgId());
//            }
//
//            if (group.getBackgroundImgId() != null) {
//                storageService.deleteImage(group.getBackgroundImgId());
//                log.debug("Удалено фоновое изображение группы: {}", group.getBackgroundImgId());
//            }
//        } catch (Exception e) {
//            log.warn("Не удалось удалить изображения группы ID {}: {}", group.getId(), e.getMessage());
//
//        }
//    }
//
//    /**
//     * Обрабатывает загрузку изображения: валидация + загрузка в хранилище
//     */
//    private StorageService.StorageResult processImage(MultipartFile imageFile, String imageType) throws IOException {
//        log.debug("Начинаем обработку {}", imageType);
//
//        // Валидируем изображение
//        ImageConverterService.ProcessedImage processedImage = imageValidator.validateAndProcess(imageFile);
//        log.debug("Валидация {} прошла успешно", imageType);
//
//        // Загружаем в хранилище
//        StorageService.StorageResult result = storageService.uploadProcessedImage(processedImage);
//        log.debug("Загрузка {} завершена. URL: {}, ID: {}",
//                imageType, result.getUrl(), result.getImageId());
//
//        return result;
//    }
//
//    /**
//     * Создает запись о членстве владельца в группе
//     */
//    private void createOwnerMembership(Long groupId, Long ownerId) {
//        log.debug("Создаем членство владельца для группы ID: {} и пользователя ID: {}", groupId, ownerId);
//
//        GroupMembership ownerMembership = GroupMembership.builder()
//                .groupId(groupId)
//                .userId(ownerId)
//                .role(GroupRole.OWNER)
//                .status(MembershipStatus.APPROVED)
//                .joinedAt(LocalDateTime.now())
//                .requestedAt(LocalDateTime.now())
//                .build();
//
//        groupMembershipRepository.save(ownerMembership);
//        log.debug("Членство владельца успешно создано");
//    }
//}




package com.example.cleopatra.service.impl;

import com.example.cleopatra.ExistsException.*;
import com.example.cleopatra.dto.GroupDto.CreateGroupRequestDTO;
import com.example.cleopatra.dto.GroupDto.GroupPageResponse;
import com.example.cleopatra.dto.GroupDto.GroupResponseDTO;
import com.example.cleopatra.enums.*;
import com.example.cleopatra.maper.GroupMapper;
import com.example.cleopatra.model.Group;
import com.example.cleopatra.model.GroupMembership;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.GroupMembershipRepository;
import com.example.cleopatra.repository.GroupRepository;
import com.example.cleopatra.repository.UserRepository;
import com.example.cleopatra.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupServiceImpl implements GroupService {
    private final GroupRepository groupRepository;
    private final GroupMapper groupMapper;
    private final StorageService storageService;
    private final ImageValidator imageValidator;
    private final UserService userService;
    private final GroupMembershipRepository groupMembershipRepository;

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "groups", allEntries = true),
            @CacheEvict(value = "group-members", allEntries = true),
            @CacheEvict(value = "group-stats", allEntries = true)
    })
    public GroupResponseDTO createGroup(CreateGroupRequestDTO createGroupRequestDTO, Long ownerId) {
        log.info("🗑️ CACHE EVICT: Clearing groups, group-members, and group-stats caches");
        log.info("Начинаем создание группы '{}' для пользователя с ID: {}",
                createGroupRequestDTO.getName(), ownerId);

        try {
            // 1. Проверяем существование пользователя-владельца
            User owner = userService.findById(ownerId);
            if (owner == null) {
                throw new UsernameNotFoundException("Пользователь с ID " + ownerId + " не найден");
            }

            // 2. Проверяем, не заблокирован ли пользователь
            if (owner.isBlocked()) {
                throw new UserBlockedException("Заблокированный пользователь не может создавать группы");
            }

            // 3. Создаем Group entity из DTO
            Group group = groupMapper.toEntity(createGroupRequestDTO);
            group.setOwnerId(ownerId);

            // 4. Обрабатываем загрузку основного изображения группы
            if (createGroupRequestDTO.getImage() != null && !createGroupRequestDTO.getImage().isEmpty()) {
                log.debug("Обработка основного изображения группы");
                StorageService.StorageResult imageResult = processImage(
                        createGroupRequestDTO.getImage(),
                        "основного изображения группы"
                );
                group.setImageUrl(imageResult.getUrl());
                group.setImgId(imageResult.getImageId());
            }

            // 5. Обрабатываем загрузку фонового изображения
            if (createGroupRequestDTO.getBackgroundImage() != null && !createGroupRequestDTO.getBackgroundImage().isEmpty()) {
                log.debug("Обработка фонового изображения группы");
                StorageService.StorageResult backgroundResult = processImage(
                        createGroupRequestDTO.getBackgroundImage(),
                        "фонового изображения группы"
                );
                group.setBackgroundImageUrl(backgroundResult.getUrl());
                group.setBackgroundImgId(backgroundResult.getImageId());
            }

            // 6. Сохраняем группу в базу данных
            Group savedGroup = groupRepository.save(group);
            log.info("Группа '{}' успешно создана с ID: {}", savedGroup.getName(), savedGroup.getId());

            // 7. Создаем GroupMembership для владельца группы
            createOwnerMembership(savedGroup.getId(), ownerId);

            // 8. Обновляем счетчик участников
            savedGroup.setMemberCount(1L);
            savedGroup = groupRepository.save(savedGroup);

            // 9. Конвертируем в DTO и возвращаем
            return groupMapper.toResponseDTO(savedGroup, ownerId);
        } catch (IOException e) {
            log.error("Ошибка при загрузке изображений для группы '{}': {}",
                    createGroupRequestDTO.getName(), e.getMessage());
            throw new ImageUploadException("Не удалось загрузить изображения группы: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Ошибка при создании группы '{}': {}",
                    createGroupRequestDTO.getName(), e.getMessage());
            throw new GroupCreationException("Не удалось создать группу: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "groups", key = "#groupId + '_' + (#currentUserId != null ? #currentUserId : 'anonymous')")
    public GroupResponseDTO getGroupById(Long groupId, Long currentUserId) {
        String cacheKey = groupId + "_" + (currentUserId != null ? currentUserId : "anonymous");
        log.info("🔍 CACHE MISS: Loading group {} for user {} from database", groupId, currentUserId);

        Group group = groupRepository.findById(groupId).orElse(null);
        if (group == null) {
            throw new GroupNotFoundException("Группа с ID " + groupId + " не найдена");
        }

        log.info("✅ Successfully loaded group {} from database", groupId);
        return groupMapper.toResponseDTO(group, currentUserId);
    }

    @Override
    @Cacheable(value = "group-stats", key = "'public_groups_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public GroupPageResponse getAllPublicGroups(Pageable pageable) {
        String cacheKey = "public_groups_" + pageable.getPageNumber() + "_" + pageable.getPageSize();
        log.info("🔍 CACHE MISS: Loading public groups for key: {}", cacheKey);

        log.info("Получение публичных групп, страница: {}, размер: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        try {
            // 1. Получаем публичные и закрытые группы (исключаем приватные)
            Slice<Group> groupSlice = groupRepository.findByGroupTypeInAndGroupStatus(
                    List.of(GroupType.OPEN, GroupType.CLOSED),
                    GroupStatus.ACTIVE,
                    pageable
            );

            log.debug("Найдено {} публичных групп на странице {}",
                    groupSlice.getNumberOfElements(), pageable.getPageNumber());

            // 2. Конвертируем в DTO (без currentUserId, так как это публичный метод)
            List<GroupResponseDTO> groupDTOs = groupSlice.getContent()
                    .stream()
                    .map(group -> groupMapper.toSimpleResponseDTO(group, null))
                    .collect(Collectors.toList());

            // 3. Создаем ответ с метаданными пагинации
            GroupPageResponse response = GroupPageResponse.builder()
                    .groups(groupDTOs)
                    .hasNext(groupSlice.hasNext())
                    .currentPage(pageable.getPageNumber())
                    .size(pageable.getPageSize())
                    .isEmpty(groupSlice.isEmpty())
                    .numberOfElements(groupSlice.getNumberOfElements())
                    .build();

            return response;

        } catch (Exception e) {
            log.error("Ошибка при получении публичных групп: {}", e.getMessage());
            throw new GroupRetrievalException("Не удалось получить список публичных групп: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "group-members", key = "'available_groups_' + #currentUserId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public GroupPageResponse getAllAvailableGroups(Pageable pageable, Long currentUserId) {
        String cacheKey = "available_groups_" + currentUserId + "_" + pageable.getPageNumber() + "_" + pageable.getPageSize();
        log.info("🔍 CACHE MISS: Loading available groups for key: {}", cacheKey);

        log.info("Получение доступных групп для пользователя ID: {}, страница: {}, размер: {}",
                currentUserId, pageable.getPageNumber(), pageable.getPageSize());

        if (currentUserId == null) {
            log.warn("Попытка получить доступные группы без указания пользователя");
            throw new IllegalArgumentException("ID пользователя обязателен для получения доступных групп");
        }

        try {
            // 1. Проверяем существование пользователя
            User user = userService.findById(currentUserId);
            if (user == null) {
                throw new UsernameNotFoundException("Пользователь с ID " + currentUserId + " не найден");
            }

            // 2. Получаем группы доступные пользователю
            Slice<Group> groupSlice = groupRepository.findAvailableGroupsForUser(
                    currentUserId,
                    GroupStatus.ACTIVE,
                    pageable
            );

            log.debug("Найдено {} доступных групп для пользователя ID: {} на странице {}",
                    groupSlice.getNumberOfElements(), currentUserId, pageable.getPageNumber());

            // 3. Конвертируем в DTO с информацией о статусе пользователя
            List<GroupResponseDTO> groupDTOs = groupSlice.getContent()
                    .stream()
                    .map(group -> groupMapper.toResponseDTO(group, currentUserId))
                    .collect(Collectors.toList());

            // 4. Создаем ответ с метаданными пагинации
            GroupPageResponse response = GroupPageResponse.builder()
                    .groups(groupDTOs)
                    .hasNext(groupSlice.hasNext())
                    .currentPage(pageable.getPageNumber())
                    .size(pageable.getPageSize())
                    .isEmpty(groupSlice.isEmpty())
                    .numberOfElements(groupSlice.getNumberOfElements())
                    .build();

            log.info("✅ Successfully loaded {} available groups for user {}", groupDTOs.size(), currentUserId);
            return response;

        } catch (UsernameNotFoundException e) {
            log.error("Пользователь не найден при получении доступных групп: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при получении доступных групп для пользователя ID {}: {}",
                    currentUserId, e.getMessage());
            throw new GroupRetrievalException("Не удалось получить список доступных групп: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "groups", allEntries = true),
            @CacheEvict(value = "group-members", allEntries = true),
            @CacheEvict(value = "group-stats", allEntries = true),
            @CacheEvict(value = "group-posts", allEntries = true)
    })
    public void deleteGroup(Long groupId, Long currentUserId) {
        log.info("🗑️ CACHE EVICT: Clearing all group-related caches for group deletion");
        log.info("Удаление группы ID: {} пользователем ID: {}", groupId, currentUserId);

        try {
            // 1. Проверяем существование группы
            Group group = groupRepository.findById(groupId).orElse(null);
            if (group == null) {
                throw new GroupNotFoundException("Группа с ID " + groupId + " не найдена");
            }

            // 2. Проверяем существование пользователя
            User user = userService.findById(currentUserId);
            if (user == null) {
                throw new UsernameNotFoundException("Пользователь с ID " + currentUserId + " не найден");
            }

            // 3. Проверяем права на удаление (только владелец или админ системы)
            if (!group.getOwnerId().equals(currentUserId) && !isSystemAdmin(user)) {
                throw new GroupAccessDeniedException("Недостаточно прав для удаления группы");
            }

            // 4. Проверяем, не удалена ли уже группа
            if (GroupStatus.DELETED.equals(group.getGroupStatus())) {
                throw new GroupAlreadyDeletedException("Группа уже удалена");
            }

            // 5. Soft delete - меняем статус вместо физического удаления
            group.setGroupStatus(GroupStatus.DELETED);
            groupRepository.save(group);

            // 6. Опционально: удаляем изображения из хранилища
            deleteGroupImages(group);

            log.info("Группа '{}' (ID: {}) успешно удалена пользователем ID: {}",
                    group.getName(), groupId, currentUserId);

        } catch (GroupNotFoundException | UsernameNotFoundException | GroupAccessDeniedException | GroupAlreadyDeletedException e) {
            log.error("Ошибка при удалении группы ID {}: {}", groupId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Неожиданная ошибка при удалении группы ID {}: {}", groupId, e.getMessage());
            throw new GroupDeletionException("Не удалось удалить группу: " + e.getMessage(), e);
        }
    }

    /**
     * Проверяет, является ли пользователь админом системы
     */
    private boolean isSystemAdmin(User user) {
        return Role.ADMIN.equals(user.getRole()) || GroupRole.ADMIN.equals(user.getRole());
    }

    /**
     * Удаляет изображения группы из хранилища
     */
    private void deleteGroupImages(Group group) {
        try {
            if (group.getImgId() != null) {
                storageService.deleteImage(group.getImgId());
                log.debug("Удалено основное изображение группы: {}", group.getImgId());
            }

            if (group.getBackgroundImgId() != null) {
                storageService.deleteImage(group.getBackgroundImgId());
                log.debug("Удалено фоновое изображение группы: {}", group.getBackgroundImgId());
            }
        } catch (Exception e) {
            log.warn("Не удалось удалить изображения группы ID {}: {}", group.getId(), e.getMessage());
        }
    }

    /**
     * Обрабатывает загрузку изображения: валидация + загрузка в хранилище
     */
    private StorageService.StorageResult processImage(MultipartFile imageFile, String imageType) throws IOException {
        log.debug("Начинаем обработку {}", imageType);

        // Валидируем изображение
        ImageConverterService.ProcessedImage processedImage = imageValidator.validateAndProcess(imageFile);
        log.debug("Валидация {} прошла успешно", imageType);

        // Загружаем в хранилище
        StorageService.StorageResult result = storageService.uploadProcessedImage(processedImage);
        log.debug("Загрузка {} завершена. URL: {}, ID: {}",
                imageType, result.getUrl(), result.getImageId());

        return result;
    }

    /**
     * Создает запись о членстве владельца в группе
     */
    private void createOwnerMembership(Long groupId, Long ownerId) {
        log.debug("Создаем членство владельца для группы ID: {} и пользователя ID: {}", groupId, ownerId);

        GroupMembership ownerMembership = GroupMembership.builder()
                .groupId(groupId)
                .userId(ownerId)
                .role(GroupRole.OWNER)
                .status(MembershipStatus.APPROVED)
                .joinedAt(LocalDateTime.now())
                .requestedAt(LocalDateTime.now())
                .build();

        groupMembershipRepository.save(ownerMembership);
        log.debug("Членство владельца успешно создано");
    }
}