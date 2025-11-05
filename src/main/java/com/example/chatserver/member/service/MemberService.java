package com.example.chatserver.member.service;

import com.example.chatserver.member.domain.Member;
import com.example.chatserver.member.dto.MemberListResDto;
import com.example.chatserver.member.dto.MemberLoginReqDto;
import com.example.chatserver.member.dto.MemberSaveReqDto;
import com.example.chatserver.member.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 회원 가입
    public Member create(MemberSaveReqDto memberSaveReqDto) {
        // 이미 email 존재 여부 확인
        if(memberRepository.findByEmail(memberSaveReqDto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 이메일");
        }

        Member newMember = Member.builder()
                .name(memberSaveReqDto.getName())
                .email(memberSaveReqDto.getEmail())
                .password(passwordEncoder.encode(memberSaveReqDto.getPassword()))
                .build();

        Member member = memberRepository.save(newMember);

        return member;
    }

    // 로그인
    public Member login(MemberLoginReqDto memberLoginReqDto) {
        Member member = memberRepository.findByEmail(memberLoginReqDto.getEmail()).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 email"));

        if(!passwordEncoder.matches(memberLoginReqDto.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("비밀번호 불일치");
        }

        return member;
    }

    public List<MemberListResDto> findAll() {
        List<Member> memberList = memberRepository.findAll();
        List<MemberListResDto> memberListResDtoList = new ArrayList<>();

        for (Member m : memberList) {
            MemberListResDto memberListResDto = new MemberListResDto();
            memberListResDto.setId(m.getId());
            memberListResDto.setEmail(m.getEmail());
            memberListResDto.setName(m.getName());
            memberListResDtoList.add(memberListResDto);
        }

        return memberListResDtoList;
    }

}















