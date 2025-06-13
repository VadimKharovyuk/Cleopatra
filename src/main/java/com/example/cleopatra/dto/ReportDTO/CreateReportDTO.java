package com.example.cleopatra.dto.ReportDTO;

import com.example.cleopatra.enums.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateReportDTO {

    @NotNull(message = "ID поста обязателен")
    private Long postId;


    private ReportReason reason;

    @Size(max = 1000, message = "Описание не должно превышать 1000 символов")
    private String description;
}
