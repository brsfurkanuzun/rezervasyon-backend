package com.randevupazaryeri.business.service;

import com.randevupazaryeri.business.dto.*;
import com.randevupazaryeri.business.entity.Business;
import com.randevupazaryeri.business.entity.BusinessStatus;
import com.randevupazaryeri.business.entity.Category;
import com.randevupazaryeri.business.mapper.BusinessMapper;
import com.randevupazaryeri.business.repository.BusinessRepository;
import com.randevupazaryeri.business.repository.CategoryRepository;
import com.randevupazaryeri.common.exception.BusinessRuleException;
import com.randevupazaryeri.common.exception.ForbiddenException;
import com.randevupazaryeri.common.exception.ResourceNotFoundException;
import com.randevupazaryeri.common.security.SecurityUtils;
import com.randevupazaryeri.employee.dto.EmployeeResponse;
import com.randevupazaryeri.employee.mapper.EmployeeMapper;
import com.randevupazaryeri.employee.repository.EmployeeRepository;
import com.randevupazaryeri.review.dto.ReviewResponse;
import com.randevupazaryeri.review.mapper.ReviewMapper;
import com.randevupazaryeri.review.repository.ReviewRepository;
import com.randevupazaryeri.serviceoffer.dto.ServiceResponse;
import com.randevupazaryeri.serviceoffer.mapper.ServiceMapper;
import com.randevupazaryeri.serviceoffer.repository.ServiceOfferRepository;
import com.randevupazaryeri.user.entity.Role;
import com.randevupazaryeri.user.service.UserService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final CategoryRepository categoryRepository;
    private final SlugService slugService;
    private final UserService userService;
    private final BusinessOwnershipService ownershipService;
    private final ServiceOfferRepository serviceOfferRepository;
    private final EmployeeRepository employeeRepository;
    private final ReviewRepository reviewRepository;

    @Transactional
    public BusinessSummaryResponse create(CreateBusinessRequest request) {
        var principal = SecurityUtils.currentPrincipal();
        if (principal.getRole() != Role.PROVIDER && principal.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only providers can create businesses");
        }
        Business business = Business.builder()
                .owner(userService.getById(principal.getId()))
                .name(request.getName())
                .slug(slugService.uniqueSlug(request.getName()))
                .description(request.getDescription())
                .phone(request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .city(request.getCity())
                .district(request.getDistrict())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .logoUrl(request.getLogoUrl())
                .coverImageUrl(request.getCoverImageUrl())
                .timezone(request.getTimezone() != null ? request.getTimezone() : "Europe/Istanbul")
                .autoConfirm(request.getAutoConfirm() == null || request.getAutoConfirm())
                .status(BusinessStatus.PENDING_APPROVAL)
                .build();
        applyCategories(business, request.getCategoryIds());
        businessRepository.save(business);
        return toSummary(business);
    }

    @Transactional
    public BusinessSummaryResponse update(UUID id, UpdateBusinessRequest request) {
        Business business = ownershipService.requireOwnedBusiness(id);
        if (request.getName() != null) business.setName(request.getName());
        if (request.getDescription() != null) business.setDescription(request.getDescription());
        if (request.getPhone() != null) business.setPhone(request.getPhone());
        if (request.getEmail() != null) business.setEmail(request.getEmail());
        if (request.getAddress() != null) business.setAddress(request.getAddress());
        if (request.getCity() != null) business.setCity(request.getCity());
        if (request.getDistrict() != null) business.setDistrict(request.getDistrict());
        if (request.getLatitude() != null) business.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) business.setLongitude(request.getLongitude());
        if (request.getLogoUrl() != null) business.setLogoUrl(request.getLogoUrl());
        if (request.getCoverImageUrl() != null) business.setCoverImageUrl(request.getCoverImageUrl());
        if (request.getTimezone() != null) business.setTimezone(request.getTimezone());
        if (request.getAutoConfirm() != null) business.setAutoConfirm(request.getAutoConfirm());
        if (request.getCategoryIds() != null) applyCategories(business, request.getCategoryIds());
        return toSummary(business);
    }

    @Transactional(readOnly = true)
    public BusinessDetailResponse getBySlug(String slug) {
        Business business = businessRepository.findBySlugWithCategories(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));
        if (business.getStatus() != BusinessStatus.ACTIVE
                && !isOwnerOrAdmin(business)) {
            throw new ResourceNotFoundException("Business not found");
        }
        Double avg = reviewRepository.averageRatingByBusinessId(business.getId());
        long count = reviewRepository.countByBusinessId(business.getId());
        List<ServiceResponse> services = serviceOfferRepository.findByBusinessIdAndIsActiveTrue(business.getId())
                .stream().map(ServiceMapper::toResponse).toList();
        List<EmployeeResponse> employees = employeeRepository.findByBusinessIdAndIsActiveTrue(business.getId())
                .stream().map(EmployeeMapper::toResponse).toList();
        List<ReviewResponse> reviews = reviewRepository.findTop5ByBusinessIdOrderByCreatedAtDesc(business.getId())
                .stream().map(ReviewMapper::toResponse).toList();
        return BusinessDetailResponse.builder()
                .id(business.getId()).name(business.getName()).slug(business.getSlug())
                .description(business.getDescription()).phone(business.getPhone()).email(business.getEmail())
                .address(business.getAddress()).city(business.getCity()).district(business.getDistrict())
                .latitude(business.getLatitude()).longitude(business.getLongitude())
                .logoUrl(business.getLogoUrl()).coverImageUrl(business.getCoverImageUrl())
                .timezone(business.getTimezone()).autoConfirm(business.isAutoConfirm())
                .status(business.getStatus())
                .averageRating(avg).reviewCount(count)
                .categories(business.getCategories().stream().map(BusinessMapper::toCategory).toList())
                .services(services).employees(employees).recentReviews(reviews)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<BusinessSummaryResponse> search(String query, String category, String city, String district,
                                                BigDecimal minPrice, BigDecimal maxPrice, Double rating,
                                                Pageable pageable) {
        Specification<Business> spec = (root, cq, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.equal(root.get("status"), BusinessStatus.ACTIVE));
            if (query != null && !query.isBlank()) {
                String like = "%" + query.toLowerCase() + "%";
                preds.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("description")), like)
                ));
            }
            if (city != null && !city.isBlank()) {
                preds.add(cb.equal(cb.lower(root.get("city")), city.toLowerCase()));
            }
            if (district != null && !district.isBlank()) {
                preds.add(cb.equal(cb.lower(root.get("district")), district.toLowerCase()));
            }
            if (category != null && !category.isBlank()) {
                Join<Object, Object> cats = root.join("categories");
                preds.add(cb.equal(cats.get("code"), category.toUpperCase()));
            }
            cq.distinct(true);
            return cb.and(preds.toArray(new Predicate[0]));
        };
        return businessRepository.findAll(spec, pageable).map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public List<BusinessSummaryResponse> myBusinesses() {
        UUID ownerId = SecurityUtils.currentUserId();
        return businessRepository.findByOwnerId(ownerId).stream().map(this::toSummary).toList();
    }

    @Transactional
    public BusinessSummaryResponse updateStatus(UUID id, BusinessStatus status) {
        Business business = ownershipService.getBusiness(id);
        if (SecurityUtils.currentPrincipal().getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only admin can change business status");
        }
        business.setStatus(status);
        return toSummary(business);
    }

    private void applyCategories(Business business, List<UUID> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return;
        }
        Set<Category> cats = new HashSet<>(categoryRepository.findAllById(categoryIds));
        if (cats.size() != categoryIds.size()) {
            throw new BusinessRuleException("One or more categories not found");
        }
        business.setCategories(cats);
    }

    private BusinessSummaryResponse toSummary(Business b) {
        Double avg = reviewRepository.averageRatingByBusinessId(b.getId());
        long count = reviewRepository.countByBusinessId(b.getId());
        BigDecimal starting = serviceOfferRepository.findMinActivePriceByBusinessId(b.getId()).orElse(null);
        return BusinessMapper.toSummary(b, avg, count, starting);
    }

    private boolean isOwnerOrAdmin(Business business) {
        try {
            var p = SecurityUtils.currentPrincipal();
            return p.getRole() == Role.ADMIN || business.getOwner().getId().equals(p.getId());
        } catch (Exception e) {
            return false;
        }
    }
}
