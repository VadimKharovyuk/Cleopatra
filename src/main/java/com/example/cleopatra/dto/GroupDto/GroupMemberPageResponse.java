package com.example.cleopatra.dto.GroupDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupMemberPageResponse {

    private List<GroupMemberDTO> members;


    private Boolean hasNext;
    private Integer currentPage;
    private Integer size;
    private Boolean isEmpty;
    private Integer numberOfElements;
}