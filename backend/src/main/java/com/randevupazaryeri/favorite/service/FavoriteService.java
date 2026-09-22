package com.randevupazaryeri.favorite.service;

import com.randevupazaryeri.business.entity.Business;
import com.randevupazaryeri.business.service.BusinessOwnershipService;
import com.randevupazaryeri.common.exception.BusinessRuleException;
import com.randevupazaryeri.common.exception.ResourceNotFoundException;
import com.randevupazaryeri.common.security.SecurityUtils;
import com.randevupazaryeri.favorite.dto.FavoriteResponse;
import com.randevupazaryeri.favorite.entity.Favorite;
import com.randevupazaryeri.favorite.repository.FavoriteRepository;
import com.randevupazaryeri.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FavoriteService {
    private final FavoriteRepository favoriteRepository;
    private final BusinessOwnershipService ownershipService;
    private final UserService userService;

    @Transactional
    public FavoriteResponse add(UUID businessId) {
        UUID customerId = SecurityUtils.currentUserId();
        if (favoriteRepository.existsByCustomerIdAndBusinessId(customerId, businessId)) {
            throw new BusinessRuleException("Already favorited");
        }
        Business business = ownershipService.getBusiness(businessId);
        Favorite favorite = Favorite.builder()
                .customer(userService.getById(customerId))
                .business(business)
                .build();
        favoriteRepository.save(favorite);
        return toResponse(favorite);
    }

    @Transactional
    public void remove(UUID businessId) {
        UUID customerId = SecurityUtils.currentUserId();
        Favorite favorite = favoriteRepository.findByCustomerIdAndBusinessId(customerId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Favorite not found"));
        favoriteRepository.delete(favorite);
    }

    @Transactional(readOnly = true)
    public List<FavoriteResponse> listMine() {
        return favoriteRepository.findByCustomerIdOrderByCreatedAtDesc(SecurityUtils.currentUserId())
                .stream().map(this::toResponse).toList();
    }

    private FavoriteResponse toResponse(Favorite f) {
        return FavoriteResponse.builder()
                .id(f.getId())
                .businessId(f.getBusiness().getId())
                .businessName(f.getBusiness().getName())
                .businessSlug(f.getBusiness().getSlug())
                .createdAt(f.getCreatedAt())
                .build();
    }
}
