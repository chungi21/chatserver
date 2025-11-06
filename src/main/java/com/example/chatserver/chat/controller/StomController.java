package com.example.chatserver.chat.controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class StomController {

    @MessageMapping("/{roomId}") // 클라이언트에서 특정 publish/roomId 형태로 메세지 발생시 MessageMapping 수신
    @SendTo("/topic/{roomId}") // 해당 roomId에 메세지를 발행하여 구독 중인 클라이언트에게 메세지 전송
    public String sendMessage(@DestinationVariable Long roomId, String message) { // @DestinationVariable은 @MessageMapping어노테이션으로 정의된 Websocket Controller 내에서만 사용
        System.out.println(message);
        return message;
    }

}
