package org.example.veportal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "task_documents")
public class TaskDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private SessionTask task;
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;
    @Column(name = "storage_key", nullable = false, unique = true, length = 255)
    private String storageKey;
    @Column(name = "content_type", length = 150)
    private String contentType;
    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_id", nullable = false)
    private UserAccount uploadedBy;
    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    public Long getId() { return id; }
    public SessionTask getTask() { return task; }
    public void setTask(SessionTask task) { this.task = task; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }
    public UserAccount getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(UserAccount uploadedBy) { this.uploadedBy = uploadedBy; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
