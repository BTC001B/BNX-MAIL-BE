package com.btctech.mailapp.entity;

public enum CollaboratorPermission {
    VIEW,   // Read-only access
    EDIT,   // Can update subject, body, and attachments
    SEND    // Can dispatch the final email
}
