package com.randevupazaryeri.review.service;

import com.randevupazaryeri.common.exception.BusinessRuleException;
import com.randevupazaryeri.common.exception.ResourceNotFoundException;
import com.randevupazaryeri.common.security.SecurityUtils;
import com.randevupazaryeri.mail.EmailService;
import com.randevupazaryeri.review.entity.Review;
import com.randevupazaryeri.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Lets customers report a review or block its author (App Store guideline 1.2). Both hide the content from
 * that customer straight away; each new report is emailed to the moderation inbox when one is configured.
 */
@Service
@RequiredArgsConstructor
public class ReviewModerationService {

    /** Keeps the NOT IN lists non-empty; no row has this id. */
    private static final UUID NO_ID = new UUID(0L, 0L);

    private final ReviewRepository reviewRepository;
    private final JdbcTemplate jdbc;
    private final EmailService emailService;

    @Value("${app.moderation.report-email:}")
    private String reportEmail;

    @Transactional
    public void report(UUID reviewId, String reason) {
        UUID userId = SecurityUtils.currentUserId();
        Review review = findReview(reviewId);
        if (review.getCustomer().getId().equals(userId)) {
            throw new BusinessRuleException("You cannot report your own review");
        }
        String cleanReason = reason == null || reason.isBlank() ? null : reason.trim();
        int inserted = jdbc.update("""
                INSERT INTO review_reports (review_id, reporter_id, reason) VALUES (?, ?, ?)
                ON CONFLICT (review_id, reporter_id) DO NOTHING
                """, reviewId, userId, cleanReason);
        if (inserted > 0 && reportEmail != null && !reportEmail.isBlank()) {
            emailService.send(reportEmail.trim(), "Şikayet edilen yorum", """
                    Bir müşteri bu yorumu şikayet etti. Lütfen 24 saat içinde inceleyin.

                    Yorum: %s
                    İşletme: %s
                    Puan: %d
                    Metin: %s
                    Neden: %s
                    """.formatted(review.getId(), review.getBusiness().getName(), review.getRating(),
                    review.getComment() == null ? "-" : review.getComment(),
                    cleanReason == null ? "-" : cleanReason));
        }
    }

    @Transactional
    public void blockAuthor(UUID reviewId) {
        UUID userId = SecurityUtils.currentUserId();
        UUID authorId = findReview(reviewId).getCustomer().getId();
        if (authorId.equals(userId)) {
            throw new BusinessRuleException("You cannot block yourself");
        }
        jdbc.update("""
                INSERT INTO user_blocks (blocker_id, blocked_id) VALUES (?, ?)
                ON CONFLICT (blocker_id, blocked_id) DO NOTHING
                """, userId, authorId);
    }

    /** A business's reviews minus those the signed-in viewer reported or whose author they blocked. */
    @Transactional(readOnly = true)
    public Page<Review> visibleReviews(UUID businessId, Pageable pageable) {
        UUID viewerId = SecurityUtils.currentUserIdIfPresent().orElse(null);
        if (viewerId == null) {
            return reviewRepository.findByBusinessIdOrderByCreatedAtDesc(businessId, pageable);
        }
        List<UUID> reported = withPlaceholder(jdbc.queryForList(
                "SELECT review_id FROM review_reports WHERE reporter_id = ?", UUID.class, viewerId));
        List<UUID> blocked = withPlaceholder(jdbc.queryForList(
                "SELECT blocked_id FROM user_blocks WHERE blocker_id = ?", UUID.class, viewerId));
        return reviewRepository.findVisibleByBusinessId(businessId, reported, blocked, pageable);
    }

    private Review findReview(UUID reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
    }

    private static List<UUID> withPlaceholder(List<UUID> ids) {
        List<UUID> result = new ArrayList<>(ids);
        result.add(NO_ID);
        return result;
    }
}
