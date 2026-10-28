package com.claimsurveyor.claim;

import com.claimsurveyor.surveyor.Surveyor;
import com.claimsurveyor.surveyor.SurveyorRepository;
import java.io.IOException;
import java.time.Year;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ClaimService {

    static final long MAX_DOCUMENT_BYTES = 2L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png");
    private static final int MIN_REJECTION_REMARKS = 5;

    private final ClaimRepository repository;
    private final SurveyorRepository surveyorRepository;
    private final ClaimDocumentRepository documentRepository;

    public ClaimService(ClaimRepository repository,
                        SurveyorRepository surveyorRepository,
                        ClaimDocumentRepository documentRepository) {
        this.repository = repository;
        this.surveyorRepository = surveyorRepository;
        this.documentRepository = documentRepository;
    }

    /** Saves a new claim in SUBMITTED status and gives it a readable claim number. */
    @Transactional
    public Claim submit(ClaimForm form) {
        Claim claim = Claim.create();
        claim.setPolicyNumber(form.getPolicyNumber().trim());
        claim.setClaimantName(form.getClaimantName().trim());
        claim.setClaimantEmail(form.getClaimantEmail().trim());
        claim.setClaimantPhone(form.getClaimantPhone().trim());
        claim.setIncidentDate(form.getIncidentDate());
        claim.setIncidentLocation(form.getIncidentLocation().trim());
        claim.setClaimType(form.getClaimType());
        claim.setEstimatedLoss(form.getEstimatedLoss());
        claim.setDescription(form.getDescription().trim());
        claim.setStatus(ClaimStatus.SUBMITTED);

        Claim saved = repository.save(claim);
        saved.setClaimNumber(formatClaimNumber(saved.getId()));
        return repository.save(saved);
    }

    @Transactional(readOnly = true)
    public List<Claim> findAll() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    /** Claims that still need a reviewer's attention, oldest first. */
    @Transactional(readOnly = true)
    public List<Claim> findReviewQueue() {
        return repository.findByStatusInOrderByCreatedAtAsc(
                List.of(ClaimStatus.SUBMITTED, ClaimStatus.UNDER_REVIEW));
    }

    @Transactional(readOnly = true)
    public Claim get(Long id) {
        return repository.findById(id).orElseThrow(() -> new ClaimNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Surveyor> activeSurveyors() {
        return surveyorRepository.findByActiveTrueOrderByNameAsc();
    }

    /**
     * Assigns a surveyor and moves the claim to UNDER_REVIEW.
     * If surveyorId is null the active surveyor with the fewest open claims is chosen.
     */
    @Transactional
    public Claim assignSurveyor(Long claimId, Long surveyorId) {
        Claim claim = get(claimId);
        if (claim.getStatus() != ClaimStatus.SUBMITTED) {
            throw new ClaimWorkflowException("A surveyor can only be assigned to a submitted claim.");
        }
        Surveyor surveyor = (surveyorId == null) ? pickLeastLoadedSurveyor() : findActiveSurveyor(surveyorId);
        claim.assignTo(surveyor);
        return repository.save(claim);
    }

    /** Reviewer approves or rejects a claim that is under review. */
    @Transactional
    public Claim decide(Long claimId, ReviewDecision decision, String remarks) {
        Claim claim = get(claimId);
        if (claim.getStatus() != ClaimStatus.UNDER_REVIEW) {
            throw new ClaimWorkflowException("Only a claim under review can be approved or rejected.");
        }
        String cleanRemarks = remarks == null ? "" : remarks.trim();
        if (decision == ReviewDecision.APPROVE) {
            if (documentRepository.countByClaimId(claimId) == 0) {
                throw new ClaimWorkflowException("Cannot approve: no supporting document has been uploaded.");
            }
            claim.decide(ClaimStatus.APPROVED, cleanRemarks.isEmpty() ? null : cleanRemarks);
        } else {
            if (cleanRemarks.length() < MIN_REJECTION_REMARKS) {
                throw new ClaimWorkflowException("Please give a reason for rejection (at least 5 characters).");
            }
            claim.decide(ClaimStatus.REJECTED, cleanRemarks);
        }
        return repository.save(claim);
    }

    /** Validates and stores a supporting document for an open claim. */
    @Transactional
    public ClaimDocument addDocument(Long claimId, MultipartFile file) {
        Claim claim = get(claimId);
        if (claim.getStatus() == ClaimStatus.APPROVED || claim.getStatus() == ClaimStatus.REJECTED) {
            throw new ClaimWorkflowException("Documents cannot be added to a closed claim.");
        }
        if (file == null || file.isEmpty()) {
            throw new ClaimWorkflowException("Please choose a file to upload.");
        }
        if (file.getSize() > MAX_DOCUMENT_BYTES) {
            throw new ClaimWorkflowException("File is too large. The limit is 2 MB.");
        }
        String fileName = cleanFileName(file.getOriginalFilename());
        String extension = extensionOf(fileName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ClaimWorkflowException("Only PDF, JPG and PNG files are allowed.");
        }
        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new ClaimWorkflowException("The file could not be read. Please try again.");
        }
        String detectedType = detectContentType(data);
        if (detectedType == null || !matchesExtension(detectedType, extension)) {
            throw new ClaimWorkflowException("The file content does not match its type (" + extension + ").");
        }
        return documentRepository.save(new ClaimDocument(claim, fileName, detectedType, data));
    }

    @Transactional(readOnly = true)
    public List<ClaimDocument> documentsFor(Long claimId) {
        return documentRepository.findByClaimIdOrderByUploadedAtDesc(claimId);
    }

    @Transactional(readOnly = true)
    public ClaimDocument getDocument(Long claimId, Long documentId) {
        return documentRepository.findByIdAndClaimId(documentId, claimId)
                .orElseThrow(() -> new ClaimNotFoundException(documentId));
    }

    private Surveyor pickLeastLoadedSurveyor() {
        return surveyorRepository.findByActiveTrueOrderByNameAsc().stream()
                .min(Comparator.comparingLong(
                        s -> repository.countBySurveyorIdAndStatus(s.getId(), ClaimStatus.UNDER_REVIEW)))
                .orElseThrow(() -> new ClaimWorkflowException("No active surveyor is available."));
    }

    private Surveyor findActiveSurveyor(Long surveyorId) {
        return surveyorRepository.findById(surveyorId)
                .filter(Surveyor::isActive)
                .orElseThrow(() -> new ClaimWorkflowException("The selected surveyor is not available."));
    }

    static String cleanFileName(String original) {
        if (original == null || original.isBlank()) {
            return "document";
        }
        String name = original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).trim();
        return name.isEmpty() ? "document" : name;
    }

    private static String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** Identifies the real file type from its first bytes instead of trusting the browser. */
    static String detectContentType(byte[] d) {
        if (d.length >= 4 && d[0] == '%' && d[1] == 'P' && d[2] == 'D' && d[3] == 'F') {
            return "application/pdf";
        }
        if (d.length >= 8 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G') {
            return "image/png";
        }
        if (d.length >= 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        return null;
    }

    private static boolean matchesExtension(String contentType, String extension) {
        return switch (contentType) {
            case "application/pdf" -> extension.equals("pdf");
            case "image/png" -> extension.equals("png");
            case "image/jpeg" -> extension.equals("jpg") || extension.equals("jpeg");
            default -> false;
        };
    }

    static String formatClaimNumber(Long id) {
        return String.format("CLM-%d-%05d", Year.now().getValue(), id);
    }
}
