package com.randevupazaryeri.business.service;

import com.randevupazaryeri.business.entity.Business;
import com.randevupazaryeri.business.repository.BusinessRepository;
import com.randevupazaryeri.common.exception.ForbiddenException;
import com.randevupazaryeri.common.exception.ResourceNotFoundException;
import com.randevupazaryeri.common.security.SecurityUtils;
import com.randevupazaryeri.employee.entity.Employee;
import com.randevupazaryeri.employee.repository.EmployeeRepository;
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
    private final EmployeeRepository employeeRepository;

    public Business getBusiness(UUID id) {
        return businessRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found: " + id));
    }

    /**
     * Who is acting on a business: the owner (or an admin), or a staff member linked to an employee record.
     * {@code staff} is null for owners.
     */
    public record BusinessAccess(Business business, Employee staff) {
        public boolean isOwner() {
            return staff == null;
        }

        /** Owners see everything; staff only what belongs to their own employee record. */
        public boolean canAccessEmployee(UUID employeeId) {
            return staff == null || staff.getId().equals(employeeId);
        }
    }

    public BusinessAccess requireMember(UUID businessId) {
        Business business = getBusiness(businessId);
        var principal = SecurityUtils.currentPrincipal();
        if (principal.getRole() == Role.ADMIN || business.getOwner().getId().equals(principal.getId())) {
            return new BusinessAccess(business, null);
        }
        Employee staff = employeeRepository.findByBusinessIdAndUserIdAndIsActiveTrue(businessId, principal.getId())
                .orElseThrow(() -> new ForbiddenException("You are not a member of this business"));
        return new BusinessAccess(business, staff);
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
