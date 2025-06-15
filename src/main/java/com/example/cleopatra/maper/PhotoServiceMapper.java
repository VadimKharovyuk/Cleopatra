package com.example.cleopatra.maper;

import com.example.cleopatra.dto.user.PhotoResponseDto;
import com.example.cleopatra.model.Photo;
import com.example.cleopatra.model.User;
import org.springframework.stereotype.Component;

@Component
public class PhotoServiceMapper {

    public PhotoResponseDto toResponseDto(Photo photo) {
        if (photo == null) {
            return null;
        }

        return PhotoResponseDto.builder()
                .id(photo.getId())
                .picUrl(photo.getPicUrl())
                .picId(photo.getPicId())
                .uploadDate(photo.getUploadDate())
                .description(photo.getDescription())
                .authorId(photo.getAuthor().getId())
                .authorName(getAuthorName(photo.getAuthor()))
                .build();
    }

    private String getAuthorName(User author) {
        if (author == null) {
            return "Unknown";
        }

        String firstName = author.getFirstName() != null ? author.getFirstName() : "";
        String lastName = author.getLastName() != null ? author.getLastName() : "";

        return (firstName + " " + lastName).trim();
    }
}