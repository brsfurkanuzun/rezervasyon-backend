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
import com.randevupazaryeri.employee.entity.Employee;
import com.randevupazaryeri.employee.entity.WorkingHour;
import com.randevupazaryeri.employee.repository.EmployeeRepository;
import com.randevupazaryeri.employee.repository.WorkingHourRepository;
import com.randevupazaryeri.review.dto.ReviewResponse;
import com.randevupazaryeri.review.mapper.ReviewMapper;
import com.randevupazaryeri.review.repository.ReviewRepository;
import com.randevupazaryeri.serviceoffer.dto.ServiceResponse;
import com.randevupazaryeri.serviceoffer.mapper.ServiceMapper;
import com.randevupazaryeri.serviceoffer.repository.ServiceOfferRepository;
import com.randevupazaryeri.user.entity.Role;
import com.randevupazaryeri.user.service.UserService;
import com.randevupazaryeri.review.entity.Review;
import com.randevupazaryeri.serviceoffer.entity.ServiceOffer;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    private final WorkingHourRepository workingHourRepository;

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
                .autoConfirm(Boolean.TRUE.equals(request.getAutoConfirm()))
                .status(BusinessStatus.ACTIVE)
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
                .stream()
                .map(e -> {
                    EmployeeResponse er = EmployeeMapper.toResponse(e);
                    Double empAvg = reviewRepository.averageRatingByEmployeeId(e.getId());
                    long empCount = reviewRepository.countByEmployeeId(e.getId());
                    er.setAverageRating(empAvg);
                    er.setReviewCount(empCount);
                    return er;
                })
                .toList();
        List<ReviewResponse> reviews = reviewRepository.findTop20ByBusinessIdOrderByCreatedAtDesc(business.getId())
                .stream().map(ReviewMapper::toResponse).toList();
        List<OpeningHourResponse> openingHours = openingHours(business.getId());
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
                .openingHours(openingHours)
                .build();
    }

    private List<OpeningHourResponse> openingHours(UUID businessId) {
        Map<Integer, OpeningHourResponse> byDay = new TreeMap<>();
        for (WorkingHour wh : workingHourRepository
                .findByEmployeeBusinessIdAndEmployeeIsActiveTrueAndIsAvailableTrue(businessId)) {
            byDay.merge(wh.getDayOfWeek(),
                    new OpeningHourResponse(wh.getDayOfWeek(), wh.getStartTime(), wh.getEndTime()),
                    (a, b) -> new OpeningHourResponse(a.getDayOfWeek(),
                            a.getOpenTime().isBefore(b.getOpenTime()) ? a.getOpenTime() : b.getOpenTime(),
                            a.getCloseTime().isAfter(b.getCloseTime()) ? a.getCloseTime() : b.getCloseTime()));
        }
        return List.copyOf(byDay.values());
    }

    @Transactional(readOnly = true)
    public Page<BusinessSummaryResponse> search(BusinessSearchFilter f, Pageable pageable) {
        boolean byDistance = "distance".equals(f.sortBy()) && f.hasOrigin();
        boolean byRating = "rating".equals(f.sortBy());
        Specification<Business> spec = (root, cq, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.equal(root.get("status"), BusinessStatus.ACTIVE));
            if (f.query() != null && !f.query().isBlank()) {
                String like = "%" + f.query().toLowerCase(Locale.ROOT) + "%";
                preds.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("description")), like)
                ));
            }
            if (f.city() != null && !f.city().isBlank()) {
                preds.add(cb.equal(cb.lower(root.get("city")), f.city().toLowerCase(Locale.ROOT)));
            }
            if (f.district() != null && !f.district().isBlank()) {
                preds.add(cb.equal(cb.lower(root.get("district")), f.district().toLowerCase(Locale.ROOT)));
            }
            if (f.category() != null && !f.category().isBlank()) {
                Subquery<UUID> inCategory = cq.subquery(UUID.class);
                Root<Business> self = inCategory.correlate(root);
                Join<Business, Category> cats = self.join("categories");
                inCategory.select(cats.get("id")).where(cb.equal(cats.get("code"), f.category().toUpperCase(Locale.ROOT)));
                preds.add(cb.exists(inCategory));
            }
            if (f.hasBounds()) {
                preds.add(cb.between(root.get("latitude"), Math.min(f.minLat(), f.maxLat()), Math.max(f.minLat(), f.maxLat())));
                Path<Double> lng = root.get("longitude");
                preds.add(f.minLng() <= f.maxLng()
                        ? cb.between(lng, f.minLng(), f.maxLng())
                        : cb.or(cb.ge(lng, f.minLng()), cb.le(lng, f.maxLng())));
            }
            if (f.minPrice() != null) {
                preds.add(cb.ge(startingPrice(root, cq, cb), f.minPrice()));
            }
            if (f.maxPrice() != null) {
                Subquery<BigDecimal> starting = startingPrice(root, cq, cb);
                preds.add(cb.or(cb.isNull(starting), cb.le(starting, f.maxPrice())));
            }
            if (f.rating() != null) {
                preds.add(cb.ge(averageRating(root, cq, cb), f.rating()));
            }

            boolean countQuery = Long.class.equals(cq.getResultType()) || long.class.equals(cq.getResultType());
            if (!countQuery && byDistance) {
                double lngScale = Math.cos(Math.toRadians(f.lat()));
                Expression<Double> dLat = cb.diff(root.get("latitude"), f.lat());
                Expression<Double> dLng = cb.prod(cb.diff(root.<Double>get("longitude"), f.lng()), lngScale);
                cq.orderBy(cb.asc(cb.sum(cb.prod(dLat, dLat), cb.prod(dLng, dLng))), cb.asc(root.get("name")));
            } else if (!countQuery && byRating) {
                Subquery<Long> reviews = cq.subquery(Long.class);
                Root<Review> r = reviews.from(Review.class);
                reviews.select(cb.count(r)).where(cb.equal(r.get("business"), root));
                cq.orderBy(cb.desc(cb.coalesce(averageRating(root, cq, cb), 0.0)), cb.desc(reviews), cb.asc(root.get("name")));
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };
        Pageable paging = byDistance || byRating ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()) : pageable;
        return businessRepository.findAll(spec, paging).map(b -> {
            BusinessSummaryResponse summary = toSummary(b);
            if (f.hasOrigin() && b.getLatitude() != null && b.getLongitude() != null) {
                double km = distanceKm(f.lat(), f.lng(), b.getLatitude(), b.getLongitude());
                summary.setDistanceKm(Math.round(km * 10) / 10.0);
            }
            return summary;
        });
    }

    private static Subquery<BigDecimal> startingPrice(Root<Business> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
        Subquery<BigDecimal> sq = cq.subquery(BigDecimal.class);
        Root<ServiceOffer> s = sq.from(ServiceOffer.class);
        sq.select(cb.min(s.get("price"))).where(cb.equal(s.get("business"), root), cb.isTrue(s.get("isActive")));
        return sq;
    }

    private static Subquery<Double> averageRating(Root<Business> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
        Subquery<Double> sq = cq.subquery(Double.class);
        Root<Review> r = sq.from(Review.class);
        sq.select(cb.avg(r.get("rating"))).where(cb.equal(r.get("business"), root));
        return sq;
    }

    @Transactional(readOnly = true)
    public List<BusinessSummaryResponse> nearby(String slug, int limit) {
        Business origin = businessRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));
        Comparator<Business> sameArea = Comparator
                .comparing((Business b) -> !Objects.equals(b.getDistrict(), origin.getDistrict()))
                .thenComparing(b -> !Objects.equals(b.getCity(), origin.getCity()));
        boolean originHasCoords = origin.getLatitude() != null && origin.getLongitude() != null;
        return businessRepository.findByStatusAndIdNot(BusinessStatus.ACTIVE, origin.getId()).stream()
                .map(b -> Map.entry(b, originHasCoords && b.getLatitude() != null && b.getLongitude() != null
                        ? distanceKm(origin.getLatitude(), origin.getLongitude(), b.getLatitude(), b.getLongitude())
                        : Double.MAX_VALUE))
                .sorted(Map.Entry.<Business, Double>comparingByValue()
                        .thenComparing(Map.Entry::getKey, sameArea))
                .limit(Math.max(1, Math.min(limit, 20)))
                .map(entry -> {
                    BusinessSummaryResponse summary = toSummary(entry.getKey());
                    if (entry.getValue() != Double.MAX_VALUE) {
                        summary.setDistanceKm(Math.round(entry.getValue() * 10) / 10.0);
                    }
                    return summary;
                })
                .toList();
    }

    private static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    @Transactional(readOnly = true)
    public List<BusinessSummaryResponse> myBusinesses() {
        UUID ownerId = SecurityUtils.currentUserId();
        return businessRepository.findByOwnerId(ownerId).stream().map(this::toSummary).toList();
    }

    /** Owned businesses first, then businesses where the caller is linked staff. */
    @Transactional(readOnly = true)
    public List<WorkplaceResponse> myWorkplaces() {
        UUID userId = SecurityUtils.currentUserId();
        Map<UUID, UUID> staffSeats = new LinkedHashMap<>();
        for (Employee e : employeeRepository.findByUserIdAndIsActiveTrue(userId)) {
            staffSeats.put(e.getBusiness().getId(), e.getId());
        }
        List<WorkplaceResponse> result = new ArrayList<>();
        for (Business b : businessRepository.findByOwnerId(userId)) {
            result.add(WorkplaceResponse.builder()
                    .business(toSummary(b)).role(WorkplaceResponse.Role.OWNER)
                    .employeeId(staffSeats.remove(b.getId())).build());
        }
        for (Employee e : employeeRepository.findByUserIdAndIsActiveTrue(userId)) {
            if (staffSeats.containsKey(e.getBusiness().getId())) {
                result.add(WorkplaceResponse.builder()
                        .business(toSummary(e.getBusiness())).role(WorkplaceResponse.Role.STAFF)
                        .employeeId(e.getId()).build());
            }
        }
        return result;
    }

    /** Hidden from customers and closed to new bookings; existing appointments are kept. */
    @Transactional
    public BusinessSummaryResponse freeze(UUID id) {
        Business business = ownershipService.requireOwnedBusiness(id);
        if (business.getStatus() != BusinessStatus.ACTIVE) {
            throw new BusinessRuleException("Only active businesses can be frozen");
        }
        business.setStatus(BusinessStatus.INACTIVE);
        return toSummary(business);
    }

    /** Only undoes an owner's freeze; suspended or pending businesses stay with the admin. */
    @Transactional
    public BusinessSummaryResponse unfreeze(UUID id) {
        Business business = ownershipService.requireOwnedBusiness(id);
        if (business.getStatus() != BusinessStatus.INACTIVE) {
            throw new BusinessRuleException("Only frozen businesses can be reactivated");
        }
        business.setStatus(BusinessStatus.ACTIVE);
        return toSummary(business);
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
