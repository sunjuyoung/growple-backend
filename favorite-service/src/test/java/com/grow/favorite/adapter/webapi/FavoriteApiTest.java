package com.grow.favorite.adapter.webapi;

import com.grow.favorite.application.provided.FavoriteService;
import com.grow.favorite.domain.favorite.Favorite;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FavoriteApi.class)
class FavoriteApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FavoriteService favoriteService;

    private static final String USER_ID_HEADER = "X-User-Id";

    @Test
    @DisplayName("회원의 즐겨찾기 목록을 조회한다")
    void read() throws Exception {
        // given
        Long memberId = 1L;
        Favorite favorite1 = Favorite.create(memberId, 100L);
        Favorite favorite2 = Favorite.create(memberId, 200L);
        given(favoriteService.read(memberId)).willReturn(List.of(favorite1, favorite2));

        // when & then
        mockMvc.perform(get("/api/v1/favorites")
                        .header(USER_ID_HEADER, memberId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].studyId").value(100))
                .andExpect(jsonPath("$[1].studyId").value(200));
    }

    @Test
    @DisplayName("즐겨찾기 목록이 비어있으면 빈 배열을 반환한다")
    void read_empty() throws Exception {
        // given
        Long memberId = 1L;
        given(favoriteService.read(memberId)).willReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/v1/favorites")
                        .header(USER_ID_HEADER, memberId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("스터디를 즐겨찾기에 추가한다")
    void favorite() throws Exception {
        // given
        Long memberId = 1L;
        Long studyId = 100L;
        Favorite favorite = Favorite.create(memberId, studyId);
        given(favoriteService.favorite(memberId, studyId)).willReturn(favorite);

        // when & then
        mockMvc.perform(post("/api/v1/favorites/studies/{studyId}", studyId)
                        .header(USER_ID_HEADER, memberId))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberId").value(memberId))
                .andExpect(jsonPath("$.studyId").value(studyId));
    }

    @Test
    @DisplayName("이미 즐겨찾기에 추가된 스터디를 추가하면 409 에러를 반환한다")
    void favorite_duplicate() throws Exception {
        // given
        Long memberId = 1L;
        Long studyId = 100L;
        given(favoriteService.favorite(memberId, studyId))
                .willThrow(new IllegalStateException("이미 즐겨찾기에 추가된 스터디입니다."));

        // when & then
        mockMvc.perform(post("/api/v1/favorites/studies/{studyId}", studyId)
                        .header(USER_ID_HEADER, memberId))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("이미 즐겨찾기에 추가된 스터디입니다."));
    }

    @Test
    @DisplayName("스터디를 즐겨찾기에서 삭제한다")
    void unfavorite() throws Exception {
        // given
        Long memberId = 1L;
        Long studyId = 100L;
        willDoNothing().given(favoriteService).unfavorite(memberId, studyId);

        // when & then
        mockMvc.perform(delete("/api/v1/favorites/studies/{studyId}", studyId)
                        .header(USER_ID_HEADER, memberId))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("즐겨찾기에 없는 스터디를 삭제하면 409 에러를 반환한다")
    void unfavorite_notFound() throws Exception {
        // given
        Long memberId = 1L;
        Long studyId = 100L;
        willThrow(new IllegalStateException("즐겨찾기에 존재하지 않는 스터디입니다."))
                .given(favoriteService).unfavorite(memberId, studyId);

        // when & then
        mockMvc.perform(delete("/api/v1/favorites/studies/{studyId}", studyId)
                        .header(USER_ID_HEADER, memberId))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("즐겨찾기에 존재하지 않는 스터디입니다."));
    }
}
