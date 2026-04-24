package com.btctech.mailapp.service;

import com.btctech.mailapp.entity.MailLabel;
import com.btctech.mailapp.entity.MailLabelMapping;
import com.btctech.mailapp.exception.MailException;
import com.btctech.mailapp.repository.MailLabelMappingRepository;
import com.btctech.mailapp.repository.MailLabelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailLabelService {

    private final MailLabelRepository labelRepository;
    private final MailLabelMappingRepository mappingRepository;

    public List<MailLabel> getLabels(String userEmail) {
        return labelRepository.findByUserEmail(userEmail);
    }

    @Transactional
    public MailLabel createLabel(String userEmail, String name, String colorHex) {
        if (labelRepository.existsByUserEmailAndName(userEmail, name)) {
            throw new MailException("Label with name '" + name + "' already exists");
        }

        MailLabel label = MailLabel.builder()
                .userEmail(userEmail)
                .name(name)
                .colorHex(colorHex)
                .build();

        return labelRepository.save(label);
    }

    @Transactional
    public void deleteLabel(String userEmail, Long labelId) {
        MailLabel label = labelRepository.findById(labelId)
                .orElseThrow(() -> new MailException("Label not found"));

        if (!label.getUserEmail().equals(userEmail)) {
            throw new MailException("You do not have permission to delete this label");
        }

        // Clean up mappings first
        mappingRepository.deleteByLabelId(labelId);
        labelRepository.delete(label);
    }

    @Transactional
    public void applyLabelToEmail(String userEmail, String emailUid, String folderName, Long labelId) {
        MailLabel label = labelRepository.findById(labelId)
                .orElseThrow(() -> new MailException("Label not found"));

        if (!label.getUserEmail().equals(userEmail)) {
            throw new MailException("Unauthorized label application");
        }

        // Check if already applied
        if (mappingRepository.findByUserEmailAndEmailUidAndFolderNameAndLabelId(userEmail, emailUid, folderName, labelId).isPresent()) {
            return;
        }

        MailLabelMapping mapping = MailLabelMapping.builder()
                .userEmail(userEmail)
                .emailUid(emailUid)
                .folderName(folderName)
                .label(label)
                .build();

        mappingRepository.save(mapping);
    }

    @Transactional
    public void removeLabelFromEmail(String userEmail, String emailUid, String folderName, Long labelId) {
        mappingRepository.findByUserEmailAndEmailUidAndFolderNameAndLabelId(userEmail, emailUid, folderName, labelId)
                .ifPresent(mappingRepository::delete);
    }

    public List<MailLabel> getLabelsForEmail(String userEmail, String emailUid, String folderName) {
        return mappingRepository.findByUserEmailAndEmailUidAndFolderName(userEmail, emailUid, folderName)
                .stream()
                .map(MailLabelMapping::getLabel)
                .collect(Collectors.toList());
    }
}
