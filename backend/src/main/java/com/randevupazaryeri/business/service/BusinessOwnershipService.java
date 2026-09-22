package com.randevupazaryeri.business.service;

import com.randevupazaryeri.business.entity.Business;
import com.randevupazaryeri.business.repository.BusinessRepository;
import com.randevupazaryeri.common.exception.ForbiddenException;
import com.randevupazaryeri.common.exception.ResourceNotFoundException;
import com.randevupazaryeri.common.security.SecurityUtils;
import com.randevupazaryeri.user.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusinessOwnershipService {
    private final BusinessRepository businessRepository;

    public Business getBusiness(UUID id) {
        return businessRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found: " + id));
    }

    public Business requireOwnedBusiness(UUID businessId) {
        Business business = getBusiness(businessId);
        var principal = SecurityUtils.currentPrincipal();
        if (principal.getRole() == Role.ADMIN) {
            return business;
        }
        if (principal.getRole() != Role.PROVIDER || !business.getOwner().getId().equals(principal.getId())) {
            throw new ForbiddenException("You do not own this business");
        }
        return business;
    }
}
