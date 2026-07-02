package com.btctech.mailapp.service;

import com.btctech.mailapp.dto.SignatureDTO;
import com.btctech.mailapp.entity.Signature;
import com.btctech.mailapp.entity.User;
import com.btctech.mailapp.repository.SignatureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SignatureService {

    private final SignatureRepository signatureRepository;

    public List<SignatureDTO> getUserSignatures(User user) {
        return signatureRepository.findByUserId(user.getId())
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public SignatureDTO createSignature(User user, SignatureDTO dto) {
        Signature signature = Signature.builder()
                .user(user)
                .name(dto.getName())
                .content(dto.getContent())
                .isDefault(dto.getIsDefault() != null ? dto.getIsDefault() : false)
                .build();

        if (signature.getIsDefault()) {
            clearDefaultSignatures(user);
        } else if (signatureRepository.findByUserId(user.getId()).isEmpty()) {
            // If it's the first signature, make it default automatically
            signature.setIsDefault(true);
        }

        Signature saved = signatureRepository.save(signature);
        return mapToDTO(saved);
    }

    @Transactional
    public SignatureDTO updateSignature(User user, Long id, SignatureDTO dto) {
        Signature signature = signatureRepository.findById(id)
                .filter(s -> s.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Signature not found"));

        if (dto.getName() != null) signature.setName(dto.getName());
        if (dto.getContent() != null) signature.setContent(dto.getContent());

        if (dto.getIsDefault() != null && dto.getIsDefault()) {
            clearDefaultSignatures(user);
            signature.setIsDefault(true);
        }

        Signature saved = signatureRepository.save(signature);
        return mapToDTO(saved);
    }

    @Transactional
    public void deleteSignature(User user, Long id) {
        Signature signature = signatureRepository.findById(id)
                .filter(s -> s.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Signature not found"));

        boolean wasDefault = signature.getIsDefault();
        signatureRepository.delete(signature);

        if (wasDefault) {
            // Assign another default if possible
            List<Signature> remaining = signatureRepository.findByUserId(user.getId());
            if (!remaining.isEmpty()) {
                Signature newDefault = remaining.get(0);
                newDefault.setIsDefault(true);
                signatureRepository.save(newDefault);
            }
        }
    }

    @Transactional
    public SignatureDTO setDefaultSignature(User user, Long id) {
        Signature signature = signatureRepository.findById(id)
                .filter(s -> s.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Signature not found"));

        clearDefaultSignatures(user);
        signature.setIsDefault(true);

        Signature saved = signatureRepository.save(signature);
        return mapToDTO(saved);
    }

    private void clearDefaultSignatures(User user) {
        List<Signature> defaults = signatureRepository.findByUserIdAndIsDefaultTrue(user.getId());
        for (Signature sig : defaults) {
            sig.setIsDefault(false);
            signatureRepository.save(sig);
        }
    }

    private SignatureDTO mapToDTO(Signature signature) {
        return SignatureDTO.builder()
                .id(signature.getId())
                .name(signature.getName())
                .content(signature.getContent())
                .isDefault(signature.getIsDefault())
                .createdAt(signature.getCreatedAt())
                .updatedAt(signature.getUpdatedAt())
                .build();
    }
}
