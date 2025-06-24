package com.example.cleopatra.service.impl;

import com.example.cleopatra.ExistsException.*;
import com.example.cleopatra.dto.GroupDto.CreateGroupRequestDTO;
import com.example.cleopatra.dto.GroupDto.GroupPageResponse;
import com.example.cleopatra.dto.GroupDto.GroupResponseDTO;
import com.example.cleopatra.enums.GroupRole;
import com.example.cleopatra.enums.GroupStatus;
import com.example.cleopatra.enums.GroupType;
import com.example.cleopatra.enums.MembershipStatus;
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
    private final ImageValidator imageValidator ;
    private final UserService userService;
    private final GroupMembershipRepository groupMembershipRepository;


    @Override
    @Transactional
    public GroupResponseDTO createGroup(CreateGroupRequestDTO createGroupRequestDTO, Long ownerId) {
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
    public GroupResponseDTO getGroupById(Long groupId, Long currentUserId) {
        Group group = groupRepository.findById(groupId).orElse(null);
        if (group == null) {
            throw new GroupNotFoundException("Группа с ID " + groupId + " не найдена");
        }

        return groupMapper.toResponseDTO(group, currentUserId);
    }

    @Override
    public GroupPageResponse getAllPublicGroups(Pageable pageable) {
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
            return GroupPageResponse.builder()
                    .groups(groupDTOs)
                    .hasNext(groupSlice.hasNext())
                    .currentPage(pageable.getPageNumber())
                    .size(pageable.getPageSize())
                    .isEmpty(groupSlice.isEmpty())
                    .numberOfElements(groupSlice.getNumberOfElements())
                    .build();

        } catch (Exception e) {
            log.error("Ошибка при получении публичных групп: {}", e.getMessage());
            throw new GroupRetrievalException("Не удалось получить список публичных групп: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public GroupPageResponse getAllAvailableGroups(Pageable pageable, Long currentUserId) {
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
            return GroupPageResponse.builder()
                    .groups(groupDTOs)
                    .hasNext(groupSlice.hasNext())
                    .currentPage(pageable.getPageNumber())
                    .size(pageable.getPageSize())
                    .isEmpty(groupSlice.isEmpty())
                    .numberOfElements(groupSlice.getNumberOfElements())
                    .build();

        } catch (UsernameNotFoundException e) {
            log.error("Пользователь не найден при получении доступных групп: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при получении доступных групп для пользователя ID {}: {}",
                    currentUserId, e.getMessage());
            throw new GroupRetrievalException("Не удалось получить список доступных групп: " + e.getMessage());
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
