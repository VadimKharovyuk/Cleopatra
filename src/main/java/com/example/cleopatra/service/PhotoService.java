package com.example.cleopatra.service;

import com.example.cleopatra.dto.user.PhotoCreateDto;
import com.example.cleopatra.dto.user.PhotoResponseDto;
import com.example.cleopatra.model.User;

import java.util.List;



public interface PhotoService {

    boolean canUploadPhoto(Long userId);

    int getPhotoLimitForUser(User user);

    PhotoResponseDto createPhoto(Long userId, PhotoCreateDto photoCreateDto);

    void deletePhoto(Long userId, Long id);
    PhotoResponseDto getPhotoById(Long userId, Long photoId);

    List<PhotoResponseDto> getUserPhotos(Long userId);


    List<PhotoResponseDto> getPublicPhotos(Long userId);
    PhotoResponseDto getPublicPhotoById(Long photoId);

    int getRemainingPhotoLimit(Long userId);
    int getPhotoLimitForUserId(Long userId);

    List<PhotoResponseDto> getUserPhotos(Long userId, Long viewerId);



}
