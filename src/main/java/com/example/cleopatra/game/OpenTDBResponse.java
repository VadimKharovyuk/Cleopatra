package com.example.cleopatra.game;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenTDBResponse {
    @JsonProperty("response_code")
    private int responseCode;

    private List<OpenTDBQuestion> results;
}
