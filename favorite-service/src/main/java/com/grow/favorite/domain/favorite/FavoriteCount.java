package com.grow.favorite.domain.favorite;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Table
@Getter
@Entity
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FavoriteCount {

    @Id
    private Long studyId;

    private Long count;

    public static FavoriteCount init(Long studyId, Long count){
        FavoriteCount favoriteCount = new FavoriteCount();
        favoriteCount.count = count;
        favoriteCount.studyId = studyId;
        return favoriteCount;

    }

    public void increase(){
        this.count++;
    }

    public void decrease(){
        this.count--;
    }
}
