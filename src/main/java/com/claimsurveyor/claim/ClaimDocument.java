package com.claimsurveyor.claim;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** A supporting document (photo, FIR copy, bill) uploaded for a claim. */
@Entity
@Table(name = "claim_documents")
public class ClaimDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claim_id")
    private Claim claim;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    @Lob
    @Column(nullable = false)
    private byte[] data;

    @Column(nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    protected ClaimDocument() {
        // for JPA
    }

    public ClaimDocument(Claim claim, String fileName, String contentType, byte[] data) {
        this.claim = claim;
        this.fileName = fileName;
        this.contentType = contentType;
        this.data = data;
        this.sizeBytes = data.length;
    }

    public Long getId() { return id; }
    public Claim getClaim() { return claim; }
    public String getFileName() { return fileName; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public byte[] getData() { return data; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
}
