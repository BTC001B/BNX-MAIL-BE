package com.btctech.mailapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentInfo {
    private String fileName;
    private String filePath;
    private String thumbnailPath;
    private long size;

    public AttachmentInfo(String fileName, String filePath, long size) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.size = size;
    }
}
