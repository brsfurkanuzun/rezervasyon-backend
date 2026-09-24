package com.randevupazaryeri.address.service;

import com.randevupazaryeri.address.dto.SaveUserAddressRequest;
import com.randevupazaryeri.address.dto.UserAddressResponse;
import com.randevupazaryeri.address.entity.UserAddress;
import com.randevupazaryeri.address.mapper.UserAddressMapper;
import com.randevupazaryeri.address.repository.UserAddressRepository;
import com.randevupazaryeri.common.exception.ResourceNotFoundException;
import com.randevupazaryeri.common.security.SecurityUtils;
import com.randevupazaryeri.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAddressService {
    private final UserAddressRepository repository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<UserAddressResponse> listMine() {
        return repository.findByUserIdOrderByIsDefaultDescUpdatedAtDesc(SecurityUtils.currentUserId())
                .stream().map(UserAddressMapper::toResponse).toList();
    }

    @Transactional
    public UserAddressResponse create(SaveUserAddressRequest request) {
        UserAddress address = new UserAddress();
        address.setUser(userService.getById(SecurityUtils.currentUserId()));
        apply(address, request);
        if (address.isDefault()) clearDefaults(address.getUser().getId(), null);
        return UserAddressMapper.toResponse(repository.save(address));
    }

    @Transactional
    public UserAddressResponse update(UUID id, SaveUserAddressRequest request) {
        UserAddress address = repository.findByIdAndUserId(id, SecurityUtils.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        apply(address, request);
        if (address.isDefault()) clearDefaults(address.getUser().getId(), id);
        return UserAddressMapper.toResponse(address);
    }

    @Transactional
    public void delete(UUID id) {
        UserAddress address = repository.findByIdAndUserId(id, SecurityUtils.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        repository.delete(address);
    }

    private void apply(UserAddress address, SaveUserAddressRequest request) {
        address.setLabel(request.getLabel().trim());
        address.setRecipientName(request.getRecipientName().trim());
        address.setPhone(blankToNull(request.getPhone()));
        address.setAddressLine(request.getAddressLine().trim());
        address.setCity(request.getCity().trim());
        address.setDistrict(blankToNull(request.getDistrict()));
        address.setPostalCode(blankToNull(request.getPostalCode()));
        address.setDefault(request.isDefault());
    }

    private void clearDefaults(UUID userId, UUID exceptId) {
        repository.findByUserIdOrderByIsDefaultDescUpdatedAtDesc(userId).stream()
                .filter(address -> !address.getId().equals(exceptId))
                .filter(UserAddress::isDefault)
                .forEach(address -> address.setDefault(false));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}