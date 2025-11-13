package com.example.chatserver.chat.service;

import com.example.chatserver.chat.domain.ChatMessage;
import com.example.chatserver.chat.domain.ChatParticipant;
import com.example.chatserver.chat.domain.ChatRoom;
import com.example.chatserver.chat.domain.ReadStatus;
import com.example.chatserver.chat.dto.ChatMessageDto;
import com.example.chatserver.chat.dto.ChatRoomListResDTO;
import com.example.chatserver.chat.dto.MyChatListResDto;
import com.example.chatserver.chat.repository.ChatMessageRepository;
import com.example.chatserver.chat.repository.ChatParticipantRepository;
import com.example.chatserver.chat.repository.ChatRoomRepository;
import com.example.chatserver.chat.repository.ReadStatusRepository;
import com.example.chatserver.member.domain.Member;
import com.example.chatserver.member.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ReadStatusRepository readStatusRepository;
    private final MemberRepository memberRepository;

    public ChatService(ChatRoomRepository chatRoomRepository, ChatParticipantRepository chatParticipantRepository, ChatMessageRepository chatMessageRepository, ReadStatusRepository readStatusRepository, MemberRepository memberRepository) {
        this.chatRoomRepository = chatRoomRepository;
        this.chatParticipantRepository = chatParticipantRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.readStatusRepository = readStatusRepository;
        this.memberRepository = memberRepository;
    }

    public void saveMessage(Long roomId, ChatMessageDto chatMessageReqDto) {
        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(() -> new EntityNotFoundException("saveMessage - room을 찾을 수 없습니다."));

        // 보낸이 조회
        Member sender = memberRepository.findByEmail(chatMessageReqDto.getSenderEmail()).orElseThrow(() -> new EntityNotFoundException("saveMessage - 사용자를 찾을 수 없습니다."));
        
        // 메세지 저장
        ChatMessage chatMessage = ChatMessage.builder()
                .chatRoom(chatRoom)
                .member(sender)
                .content(chatMessageReqDto.getMessage())
                .build();

        chatMessageRepository.save(chatMessage);

        // 읽음 여부 저장(사용자별로) - 메세지를 보낸이는 보내자마자 읽음으로써 보낸이를 제외하고 채팅방의 읽은 이의 수를 count한다.
        List<ChatParticipant> chatParticipants = chatParticipantRepository.findByChatRoom(chatRoom);

        for(ChatParticipant c : chatParticipants) {
            ReadStatus readStatus = ReadStatus.builder()
                    .chatRoom(chatRoom)
                    .member(c.getMember())
                    .chatMessage(chatMessage)
                    .isRead(c.getMember().equals(sender))
                    .build();
            readStatusRepository.save(readStatus);
        }

    }

    public void createGroupRoom(String chatRoomName) {
        // 채팅방 생성
        ChatRoom chatRoom = ChatRoom.builder()
                .name(chatRoomName)
                .isGroupChat("Y")
                .build();
        chatRoomRepository.save(chatRoom);

        // 채팅 참여자로 개설자를 추가
        // 방 생성자의 정보
        Member roomCreater = memberRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(() -> new EntityNotFoundException("createGroupRoom - 사용자를 찾을 수 없습니다."));

        // 개설자 추가
        ChatParticipant chatParticipant = ChatParticipant.builder()
                .chatRoom(chatRoom)
                .member(roomCreater)
                .build();

        chatParticipantRepository.save(chatParticipant);
    }

    public List<ChatRoomListResDTO> getGroupchatRoom() {
        List<ChatRoom> chatRooms = chatRoomRepository.findByIsGroupChat("Y");

        List<ChatRoomListResDTO> chatRoomListResDTOList = new ArrayList<>();

        for(ChatRoom c : chatRooms) {
            ChatRoomListResDTO chatRoomListResDTO = ChatRoomListResDTO.builder()
                    .roomName(c.getName())
                    .roomId(c.getId())
                    .build();
            chatRoomListResDTOList.add(chatRoomListResDTO);
        }

        return chatRoomListResDTOList;
    }

    public void addParticipantToGroupChat(Long roomId) {
        // 채킹방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(()-> new EntityNotFoundException("addParticipantToGroupChat - room을 찾을 수 없습니다."));

        // member 조회
        Member currentMember = memberRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(() -> new EntityNotFoundException("addParticipantToGroupChat - 사용자를 찾을 수 없습니다."));

        // 이미 참여자인지 검증
        Optional<ChatParticipant> participant = chatParticipantRepository.findByChatRoomAndMember(chatRoom, currentMember);
        if(!participant.isPresent()){
            addParticipant(chatRoom, currentMember);
        }

    }

    // ChatParticipant 생성 후 저장 (채팅 참여시 참여자가 아니라면 사용)
    public void addParticipant(ChatRoom chatRoom, Member member){
        ChatParticipant chatParticipant = ChatParticipant.builder()
                .chatRoom(chatRoom)
                .member(member)
                .build();

        chatParticipantRepository.save(chatParticipant);
    }

    public List<ChatMessageDto> getChatHistory(Long roomId) {
        // 해당 채팅방의 참여자인지 확인
        // - 채킹방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(()-> new EntityNotFoundException("getChatHistory - room을 찾을 수 없습니다."));

        // - member 조회
        Member currentMember = memberRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(() -> new EntityNotFoundException("getChatHistory - 사용자를 찾을 수 없습니다."));
    
        // - 채팅방 참여자 List
        List<ChatParticipant> chatParticipants = chatParticipantRepository.findByChatRoom(chatRoom);

        // - 현재 사용자가 채팅방 참여자인지 확인
        boolean check = false;

        for(ChatParticipant c : chatParticipants) {
            if(c.getMember().equals(currentMember)){
                check = true;
            }
        }

        if(!check)throw new IllegalArgumentException("getChatHistory - 해당 채팅방에 참여한 사용자가 아닙니다.");

        // 특정 room에 대한 message 조회
        List<ChatMessage> chatMessages = chatMessageRepository.findByChatRoomOrderByCreatedTimeAsc(chatRoom);

        List<ChatMessageDto> chatMessageDtos = new ArrayList<>();

        for(ChatMessage c : chatMessages) {
            ChatMessageDto chatMessageDto = ChatMessageDto.builder()
                    .message(c.getContent())
                    .senderEmail(c.getMember().getEmail())
                    .build();

            chatMessageDtos.add(chatMessageDto);
        }

        return chatMessageDtos;
    }

    public boolean isRoomPaticipant(String email, Long roomId) {
        // - 채킹방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(()-> new EntityNotFoundException("isRoomPaticipant - room을 찾을 수 없습니다."));

        // - member 조회
        Member currentMember = memberRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("isRoomPaticipant - 사용자를 찾을 수 없습니다."));

        List<ChatParticipant> chatParticipants = chatParticipantRepository.findByChatRoom(chatRoom);
        for(ChatParticipant c : chatParticipants) {
            if(c.getMember().equals(currentMember)){
                return true;
            }
        }

        return false;
    }

    public void messageRead(Long roomId) {
        // - 채킹방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(()-> new EntityNotFoundException("messageRead - room을 찾을 수 없습니다."));

        // - member 조회
        Member currentMember = memberRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(() -> new EntityNotFoundException("messageRead - 사용자를 찾을 수 없습니다."));

        List<ReadStatus> readStatuses = readStatusRepository.findByChatRoomAndMember(chatRoom, currentMember);
        for(ReadStatus r : readStatuses) {
            r.updateIsRead(true);
        }
    }

    public List<MyChatListResDto> getMyChatRooms(){
        // - member 조회
        Member currentMember = memberRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(() -> new EntityNotFoundException("getMyChatRooms - 사용자를 찾을 수 없습니다."));

        List<ChatParticipant> chatParticipants = chatParticipantRepository.findAllByMember(currentMember);

        List<MyChatListResDto> chatListResDtos = new ArrayList<>();

        for(ChatParticipant c : chatParticipants) {
            Long count = readStatusRepository.countByChatRoomAndMemberAndIsReadFalse(c.getChatRoom(), currentMember);

            MyChatListResDto dto = MyChatListResDto.builder()
                    .roomId(c.getChatRoom().getId())
                    .roomName(c.getChatRoom().getName())
                    .isGroupChat(c.getChatRoom().getIsGroupChat())
                    .unReadCount(count)
                    .build();

            chatListResDtos.add(dto);
        }

        return chatListResDtos;
    }

    public void leaveGroupChatRoom(Long roomId) {
        // - 채킹방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(()-> new EntityNotFoundException("leaveGroupChatRoom - room을 찾을 수 없습니다."));

        // - member 조회
        Member currentMember = memberRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(() -> new EntityNotFoundException("leaveGroupChatRoom - 사용자를 찾을 수 없습니다."));

        if(chatRoom.getIsGroupChat().equals("N")) {
            throw new IllegalArgumentException("단체 채팅방이 아닙니다.");
        }

        ChatParticipant c = chatParticipantRepository.findByChatRoomAndMember(chatRoom, currentMember).orElseThrow(() -> new EntityNotFoundException("leaveGroupChatRoom - 참여자 정보를 찾을 수 없습니다."));

        chatParticipantRepository.delete(c);

        // 참여자가 0인 채팅방은 채팅방도 삭제
        List<ChatParticipant> chatParticipants = chatParticipantRepository.findByChatRoom(chatRoom);
        if(chatParticipants.isEmpty()) {
            chatRoomRepository.delete(chatRoom);
        }
    }

}
