package com.grow.favorite.application;

import com.grow.favorite.application.provided.FavoriteService;
import com.grow.favorite.application.required.FavoriteRepository;
import com.grow.favorite.domain.favorite.Favorite;
import com.grow.favorite.domain.favorite.FavoriteCount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteRepository favoriteRepository;

    @Override
    public List<Favorite> read(Long memberId) {
        return favoriteRepository.findByMemberId(memberId);
    }

    @Override
    @Transactional
    public Favorite favorite(Long memberId, Long studyId) {
        if (favoriteRepository.existsByMemberIdAndStudyId(memberId, studyId)) {
            throw new IllegalStateException("이미 즐겨찾기에 추가된 스터디입니다.");
        }
        Favorite favorite = Favorite.create(memberId, studyId);
        Favorite save = favoriteRepository.save(favorite);

        int result = favoriteRepository.increase(studyId);
        if(result == 0){
            favoriteRepository.countSave(FavoriteCount.init(studyId,1L));
        }
        return save;
    }

    @Override
    @Transactional
    public void unfavorite(Long memberId, Long studyId) {
//        Favorite favorite = favoriteRepository.findByMemberIdAndStudyId(memberId, studyId)
//                .orElseThrow(() -> new IllegalStateException("즐겨찾기에 존재하지 않는 스터디입니다."));
//        favoriteRepository.delete(favorite);
//        favoriteRepository.decrease(studyId);

        favoriteRepository.findByMemberIdAndStudyId(memberId, studyId)
                .ifPresent(favorite -> {
                    favoriteRepository.delete(favorite);
                    favoriteRepository.decrease(studyId);
                });

    }
}
