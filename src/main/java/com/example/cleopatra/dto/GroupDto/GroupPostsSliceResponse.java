package com.example.cleopatra.dto.GroupDto;



import lombok.*;
import org.springframework.data.domain.Slice;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupPostsSliceResponse {

    private List<GroupPostDetails> posts;
    private boolean hasNext;
    private boolean hasPrevious;
    private int currentPage;
    private int size;
    private boolean isFirst;
    private boolean isLast;



    public static GroupPostsSliceResponse from(Slice<GroupPostDetails> slice) {
        if (slice == null) {
            return new GroupPostsSliceResponse();
        }

        return GroupPostsSliceResponse.builder()
                .posts(slice.getContent())
                .hasNext(slice.hasNext())
                .hasPrevious(slice.hasPrevious())
                .currentPage(slice.getNumber())
                .size(slice.getSize())
                .isFirst(slice.isFirst())
                .isLast(slice.isLast())
                .build();
    }
}