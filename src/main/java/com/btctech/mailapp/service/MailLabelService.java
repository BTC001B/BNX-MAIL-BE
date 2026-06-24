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
    private final com.btctech.mailapp.repository.StarredEmailRepository starredEmailRepository;

    public List<MailLabel> getLabels(String userEmail) {
        return labelRepository.findByUserEmail(userEmail);
    }

    @Transactional
    public MailLabel createLabel(String userEmail, String name, String colorHex, Long parentId) {
        if (labelRepository.existsByUserEmailAndName(userEmail, name)) {
            throw new MailException("Label with name '" + name + "' already exists");
        }

        if (parentId != null) {
            MailLabel parent = labelRepository.findById(parentId)
                    .orElseThrow(() -> new MailException("Parent label not found"));
            if (!parent.getUserEmail().equals(userEmail)) {
                throw new MailException("Unauthorized parent label");
            }
        }

        MailLabel label = MailLabel.builder()
                .userEmail(userEmail)
                .name(name)
                .colorHex(colorHex)
                .parentId(parentId)
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

        // Recursively delete children
        deleteChildren(userEmail, labelId);

        // Clean up mappings first
        mappingRepository.deleteByLabelId(labelId);
        labelRepository.delete(label);
    }

    private void deleteChildren(String userEmail, Long parentId) {
        List<MailLabel> children = labelRepository.findByParentId(parentId);
        for (MailLabel child : children) {
            // Recursively delete its children
            deleteChildren(userEmail, child.getId());
            // Delete mappings and the child label
            mappingRepository.deleteByLabelId(child.getId());
            labelRepository.delete(child);
        }
    }

    private String normalizeFolder(String userEmail, String emailUid, String folderName) {
        if (folderName == null) return "INBOX";
        String upper = folderName.toUpperCase();
        if (upper.contains("STARRED")) {
            var list = starredEmailRepository.findByUserEmailAndUid(userEmail, emailUid);
            if (!list.isEmpty()) return list.get(0).getFolderName();
            return "INBOX";
        }
        if (upper.contains("ALLMAIL") || upper.contains("ALL-MAIL") || upper.contains("LABEL")) {
            var list = mappingRepository.findByUserEmailAndEmailUid(userEmail, emailUid);
            if (!list.isEmpty()) return list.get(0).getFolderName();
            return "INBOX";
        }
        if (upper.contains("INBOX")) return "INBOX";
        if (upper.contains("SENT")) return "Sent";
        if (upper.contains("TRASH") || upper.contains("DELETED")) return "Trash";
        if (upper.contains("SPAM") || upper.contains("JUNK")) return "Spam";
        if (upper.contains("SNOOZED")) return "Snoozed";
        if (upper.contains("ARCHIVE")) return "Archive";
        return folderName;
    }

    @Transactional
    public void applyLabelToEmail(String userEmail, String emailUid, String folderName, Long labelId) {
        MailLabel label = labelRepository.findById(labelId)
                .orElseThrow(() -> new MailException("Label not found"));

        if (!label.getUserEmail().equals(userEmail)) {
            throw new MailException("Unauthorized label application");
        }

        String normFolder = normalizeFolder(userEmail, emailUid, folderName);

        // Check if already applied
        if (mappingRepository.findByUserEmailAndEmailUidAndFolderNameAndLabelId(userEmail, emailUid, normFolder, labelId).isPresent()) {
            return;
        }

        MailLabelMapping mapping = MailLabelMapping.builder()
                .userEmail(userEmail)
                .emailUid(emailUid)
                .folderName(normFolder)
                .label(label)
                .build();

        mappingRepository.save(mapping);
    }

    @Transactional
    public void removeLabelFromEmail(String userEmail, String emailUid, String folderName, Long labelId) {
        List<MailLabelMapping> mappings = mappingRepository.findByUserEmailAndEmailUidAndLabelId(userEmail, emailUid, labelId);
        mappingRepository.deleteAll(mappings);
    }

    public List<MailLabel> getLabelsForEmail(String userEmail, String emailUid, String folderName) {
        String normFolder = normalizeFolder(userEmail, emailUid, folderName);
        return mappingRepository.findByUserEmailAndEmailUidAndFolderName(userEmail, emailUid, normFolder)
                .stream()
                .map(MailLabelMapping::getLabel)
                .collect(Collectors.toList());
    }
}
