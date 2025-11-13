package com.example.chatserver.chat.controller;

import com.example.chatserver.chat.dto.ChatMessageDto;
import com.example.chatserver.chat.service.ChatService;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class StomController {

    private final SimpMessageSendingOperations messageTemplate;
    private final ChatService chatService;

    public StomController(SimpMessageSendingOperations messageTemplate, ChatService chatService) {
        this.messageTemplate = messageTemplate;
        this.chatService = chatService;
    }

    // 방법1. MessageMapping(수신)과 sendTo(topic에 메시지 전달) 한꺼번에 처리
//    @MessageMapping("/{roomId}") // 클라이언트에서 특정 publish/roomId 형태로 메세지 발생시 MessageMapping 수신
//    @SendTo("/topic/{roomId}") // 해당 roomId에 메세지를 발행하여 구독 중인 클라이언트에게 메세지 전송
//    public String sendMessage(@DestinationVariable Long roomId, String message) { // @DestinationVariable은 @MessageMapping어노테이션으로 정의된 Websocket Controller 내에서만 사용
//        System.out.println(message);
//        return message;
//    }

    // 방법2. MessageMapping 어노테이션만 활용
    // why? redis pub/sub 연동시 유연성이 떨어지기 때문에
    @MessageMapping("/{roomId}") // 클라이언트에서 특정 publish/roomId 형태로 메세지 발생시 MessageMapping 수신
    @SendTo("/topic/{roomId}") // 해당 roomId에 메세지를 발행하여 구독 중인 클라이언트에게 메세지 전송
    public void sendMessage(@DestinationVariable Long roomId, ChatMessageDto chatMessageReqDto) { // @DestinationVariable은 @MessageMapping어노테이션으로 정의된 Websocket Controller 내에서만 사용
//        System.out.println(chatMessageReqDto.getMessage());
//        System.out.println(chatMessageReqDto.getSenderEmail());
        chatService.saveMessage(roomId, chatMessageReqDto);
        messageTemplate.convertAndSend("/topic/"+roomId, chatMessageReqDto);
    }

}