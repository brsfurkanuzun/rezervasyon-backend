package com.randevupazaryeri.business.service;

import com.randevupazaryeri.business.dto.CategoryResponse;
import com.randevupazaryeri.business.entity.Category;
import com.randevupazaryeri.business.mapper.BusinessMapper;
import com.randevupazaryeri.business.repository.CategoryRepository;
import com.randevupazaryeri.common.exception.BusinessRuleException;
import com.randevupazaryeri.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> listActive() {
        return categoryRepository.findByIsActiveTrueOrderByNameAsc().stream()
                .map(BusinessMapper::toCategory).toList();
    }

    @Transactional
    public CategoryResponse create(String code, String name, String description) {
        if (categoryRepository.existsByCode(code)) {
            throw new BusinessRuleException("Category code already exists");
        }
        Category c = Category.builder().code(code.toUpperCase()).name(name).description(description).isActive(true).build();
        categoryRepository.save(c);
        return BusinessMapper.toCategory(c);
    }

    @Transactional
    public CategoryResponse update(UUID id, String name, String description, Boolean active) {
        Category c = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        if (name != null) c.setName(name);
        if (description != null) c.setDescription(description);
        if (active != null) c.setActive(active);
        return BusinessMapper.toCategory(c);
    }
}
