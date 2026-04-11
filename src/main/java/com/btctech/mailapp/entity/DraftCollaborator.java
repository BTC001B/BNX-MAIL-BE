package com.btctech.mailapp.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Entity
@Table(name = "mail_draft_collaborators")
@NoArgsConstructor
@AllArgsConstructor
public class DraftCollaborator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "draft_id", nullable = false)
    private MailDraft draft;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CollaboratorPermission permission;

    public DraftCollaborator(MailDraft draft, User user, CollaboratorPermission permission) {
        this.draft = draft;
        this.user = user;
        this.permission = permission;
    }
}
