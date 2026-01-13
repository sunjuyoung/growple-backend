package com.grow.member.application.member;

import com.grow.member.adapter.config.CacheConfig;
import com.grow.member.application.member.required.*;
import com.grow.member.application.member.provided.MemberFinder;
import com.grow.member.domain.Email;
import com.grow.member.domain.member.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberQueryService implements MemberFinder {

    private final MemberRepository memberRepository;
    private final CacheManager cacheManager;

    @Override
    @Cacheable(value = CacheConfig.MEMBER_CACHE, key = "#memberId")
    public Member findMember(Long memberId) {
        return memberRepository.findById(memberId).orElseThrow(()
                -> new IllegalArgumentException("존재하지 않는 회원입니다. ID: " + memberId));
    }

    @Override
    public Member findMember(String email) {
        return memberRepository.findByEmail(new Email(email)).orElseThrow(()
                -> new IllegalArgumentException("존재하지 않는 이메일입니다. " + email));
    }

    @Override
    public List<Member> findMembers(List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return List.of();
        }

        Cache cache = cacheManager.getCache(CacheConfig.MEMBER_CACHE);
        List<Member> result = new ArrayList<>();
        List<Long> cacheMissIds = new ArrayList<>();

        // 1. 캐시에서 먼저 조회
        for (Long memberId : memberIds) {
            Member cached = cache != null ? cache.get(memberId, Member.class) : null;
            if (cached != null) {
                result.add(cached);
            } else {
                cacheMissIds.add(memberId);
            }
        }

        // 2. 캐시에 없는 것만 DB 조회
        if (!cacheMissIds.isEmpty()) {
            List<Member> fromDb = memberRepository.findByIds(cacheMissIds);
            for (Member member : fromDb) {
                // 3. DB 조회 결과를 캐시에 저장
                if (cache != null) {
                    cache.put(member.getId(), member);
                }
                result.add(member);
            }
        }

        return result;
    }

}
