package com.grow.member.adapter.webapi;

import com.grow.member.adapter.webapi.dto.MemberSummaryResponse;
import com.grow.member.application.member.MemberQueryService;
import com.grow.member.application.member.provided.MemberFinder;
import com.grow.member.domain.member.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member")
public class MemberApi {

    private final MemberFinder memberFinder;

    @GetMapping("{id}")
    public ResponseEntity<MemberSummaryResponse> getMemberSummary(@PathVariable Long id) {
        Member member = memberFinder.findMember(id);
        return ResponseEntity.ok(MemberSummaryResponse.of(member));
    }


}
