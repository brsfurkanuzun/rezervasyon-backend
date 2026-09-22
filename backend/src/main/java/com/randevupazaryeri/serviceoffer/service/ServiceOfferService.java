package com.randevupazaryeri.serviceoffer.service;

import com.randevupazaryeri.business.service.BusinessOwnershipService;
import com.randevupazaryeri.common.exception.ResourceNotFoundException;
import com.randevupazaryeri.serviceoffer.dto.CreateServiceRequest;
import com.randevupazaryeri.serviceoffer.dto.ServiceResponse;
import com.randevupazaryeri.serviceoffer.entity.ServiceOffer;
import com.randevupazaryeri.serviceoffer.mapper.ServiceMapper;
import com.randevupazaryeri.serviceoffer.repository.ServiceOfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceOfferService {
    private final ServiceOfferRepository repository;
    private final BusinessOwnershipService ownershipService;

    @Transactional
    public ServiceResponse create(UUID businessId, CreateServiceRequest request) {
        var business = ownershipService.requireOwnedBusiness(businessId);
        ServiceOffer offer = ServiceOffer.builder()
                .business(business)
                .name(request.getName())
                .description(request.getDescription())
                .durationMinutes(request.getDurationMinutes())
                .price(request.getPrice())
                .currency(request.getCurrency() != null ? request.getCurrency() : "TRY")
                .isActive(true)
                .build();
        repository.save(offer);
        return ServiceMapper.toResponse(offer);
    }

    @Transactional
    public ServiceResponse update(UUID businessId, UUID serviceId, CreateServiceRequest request) {
        ownershipService.requireOwnedBusiness(businessId);
        ServiceOffer offer = repository.findByIdAndBusinessId(serviceId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        offer.setName(request.getName());
        offer.setDescription(request.getDescription());
        offer.setDurationMinutes(request.getDurationMinutes());
        offer.setPrice(request.getPrice());
        if (request.getCurrency() != null) offer.setCurrency(request.getCurrency());
        return ServiceMapper.toResponse(offer);
    }

    @Transactional
    public void deactivate(UUID businessId, UUID serviceId) {
        ownershipService.requireOwnedBusiness(businessId);
        ServiceOffer offer = repository.findByIdAndBusinessId(serviceId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        offer.setActive(false);
    }

    @Transactional(readOnly = true)
    public List<ServiceResponse> list(UUID businessId) {
        return repository.findByBusinessId(businessId).stream().map(ServiceMapper::toResponse).toList();
    }
}
