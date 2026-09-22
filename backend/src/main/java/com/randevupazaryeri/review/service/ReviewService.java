package com.randevupazaryeri.review.service;

import com.randevupazaryeri.appointment.entity.Appointment;
import com.randevupazaryeri.appointment.entity.AppointmentStatus;
import com.randevupazaryeri.appointment.repository.AppointmentRepository;
import com.randevupazaryeri.common.exception.BusinessRuleException;
import com.randevupazaryeri.common.exception.ForbiddenException;
import com.randevupazaryeri.common.exception.ResourceNotFoundException;
import com.randevupazaryeri.common.security.SecurityUtils;
import com.randevupazaryeri.review.dto.CreateReviewRequest;
import com.randevupazaryeri.review.dto.ReviewResponse;
import com.randevupazaryeri.review.entity.Review;
import com.randevupazaryeri.review.mapper.ReviewMapper;
import com.randevupazaryeri.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final AppointmentRepository appointmentRepository;

    @Transactional
    public ReviewResponse create(CreateReviewRequest request) {
        UUID customerId = SecurityUtils.currentUserId();
        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
        if (!appointment.getCustomer().getId().equals(customerId)) {
            throw new ForbiddenException("You can only review your own appointments");
        }
        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new BusinessRuleException("Only completed appointments can be reviewed");
        }
        if (reviewRepository.existsByAppointmentId(appointment.getId())) {
            throw new BusinessRuleException("Appointment already reviewed");
        }
        Review review = Review.builder()
                .customer(appointment.getCustomer())
                .business(appointment.getBusiness())
                .appointment(appointment)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();
        reviewRepository.save(review);
        return ReviewMapper.toResponse(review);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> listByBusiness(UUID businessId, Pageable pageable) {
        return reviewRepository.findByBusinessIdOrderByCreatedAtDesc(businessId, pageable).map(ReviewMapper::toResponse);
    }
}
