package com.btctech.mailapp.service;

import com.btctech.mailapp.dto.CasboxMessageDto;
import com.btctech.mailapp.dto.CasboxSendRequest;
import com.btctech.mailapp.entity.CasboxMessage;
import com.btctech.mailapp.repository.CasboxMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CasboxService {

    private final CasboxMessageRepository casboxMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public CasboxMessageDto sendMessage(String senderEmail, CasboxSendRequest request) {
        CasboxMessage message = new CasboxMessage();
        message.setSenderEmail(senderEmail);
        message.setReceiverEmail(request.getReceiverEmail());
        message.setSubject(request.getSubject());
        message.setBody(request.getBody());
        message.setAttachmentsJson(request.getAttachmentsJson());
        message.setStatus("SENT");
        message.setTimestamp(LocalDateTime.now());

        CasboxMessage saved = casboxMessageRepository.save(message);
        CasboxMessageDto dto = convertToDto(saved);

        // Send to receiver via WebSocket
        messagingTemplate.convertAndSendToUser(
                request.getReceiverEmail(),
                "/queue/casbox/messages",
                dto
        );

        // Also send to sender via WebSocket for instant UI update
        messagingTemplate.convertAndSendToUser(
                senderEmail,
                "/queue/casbox/messages",
                dto
        );

        return dto;
    }

    @Transactional(readOnly = true)
    public List<CasboxMessageDto> getThread(String email1, String email2) {
        return casboxMessageRepository.findConversation(email1, email2)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CasboxMessageDto> getAllMessages(String userEmail) {
        return casboxMessageRepository.findAllMessagesForUser(userEmail)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateStatus(List<Long> messageIds, String status, String receiverEmail) {
        List<CasboxMessage> messages = casboxMessageRepository.findAllById(messageIds);
        for (CasboxMessage msg : messages) {
            // Only update if the current user is the receiver of these messages
            if (msg.getReceiverEmail().equals(receiverEmail)) {
                msg.setStatus(status);
                
                // Notify sender about the status update
                messagingTemplate.convertAndSendToUser(
                        msg.getSenderEmail(),
                        "/queue/casbox/status",
                        convertToDto(msg)
                );
            }
        }
        casboxMessageRepository.saveAll(messages);
    }
    
    @Transactional
    public void markUnseenAsDelivered(String receiverEmail) {
        List<CasboxMessage> unseen = casboxMessageRepository.findUnseenMessagesForUser(receiverEmail);
        for (CasboxMessage msg : unseen) {
            if ("SENT".equals(msg.getStatus())) {
                msg.setStatus("DELIVERED");
                messagingTemplate.convertAndSendToUser(
                        msg.getSenderEmail(),
                        "/queue/casbox/status",
                        convertToDto(msg)
                );
            }
        }
        casboxMessageRepository.saveAll(unseen);
    }

    private CasboxMessageDto convertToDto(CasboxMessage entity) {
        CasboxMessageDto dto = new CasboxMessageDto();
        dto.setId(entity.getId());
        dto.setSenderEmail(entity.getSenderEmail());
        dto.setReceiverEmail(entity.getReceiverEmail());
        dto.setSubject(entity.getSubject());
        dto.setBody(entity.getBody());
        dto.setAttachmentsJson(entity.getAttachmentsJson());
        dto.setStatus(entity.getStatus());
        dto.setTimestamp(entity.getTimestamp());
        return dto;
    }
}
