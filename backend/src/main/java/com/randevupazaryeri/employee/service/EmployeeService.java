package com.randevupazaryeri.employee.service;

import com.randevupazaryeri.business.service.BusinessOwnershipService;
import com.randevupazaryeri.common.exception.BusinessRuleException;
import com.randevupazaryeri.common.exception.ResourceNotFoundException;
import com.randevupazaryeri.employee.dto.*;
import com.randevupazaryeri.employee.entity.Employee;
import com.randevupazaryeri.employee.entity.TimeOff;
import com.randevupazaryeri.employee.entity.WorkingHour;
import com.randevupazaryeri.employee.mapper.EmployeeMapper;
import com.randevupazaryeri.employee.repository.EmployeeRepository;
import com.randevupazaryeri.employee.repository.TimeOffRepository;
import com.randevupazaryeri.employee.repository.WorkingHourRepository;
import com.randevupazaryeri.serviceoffer.entity.ServiceOffer;
import com.randevupazaryeri.serviceoffer.repository.ServiceOfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final WorkingHourRepository workingHourRepository;
    private final TimeOffRepository timeOffRepository;
    private final ServiceOfferRepository serviceOfferRepository;
    private final BusinessOwnershipService ownershipService;

    @Transactional
    public EmployeeResponse create(UUID businessId, CreateEmployeeRequest request) {
        var business = ownershipService.requireOwnedBusiness(businessId);
        Employee employee = Employee.builder()
                .business(business)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .title(request.getTitle())
                .bio(request.getBio())
                .photoUrl(request.getPhotoUrl())
                .isActive(true)
                .build();
        linkServices(employee, businessId, request.getServiceIds());
        employeeRepository.save(employee);
        return EmployeeMapper.toResponse(employee);
    }

    @Transactional
    public EmployeeResponse update(UUID businessId, UUID employeeId, CreateEmployeeRequest request) {
        ownershipService.requireOwnedBusiness(businessId);
        Employee employee = getInBusiness(employeeId, businessId);
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setTitle(request.getTitle());
        employee.setBio(request.getBio());
        employee.setPhotoUrl(request.getPhotoUrl());
        if (request.getServiceIds() != null) {
            linkServices(employee, businessId, request.getServiceIds());
        }
        return EmployeeMapper.toResponse(employee);
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> list(UUID businessId) {
        return employeeRepository.findByBusinessId(businessId).stream().map(EmployeeMapper::toResponse).toList();
    }

    @Transactional
    public List<WorkingHourResponse> replaceWorkingHours(UUID businessId, UUID employeeId, List<WorkingHourRequest> requests) {
        ownershipService.requireOwnedBusiness(businessId);
        Employee employee = getInBusiness(employeeId, businessId);
        workingHourRepository.deleteByEmployeeId(employeeId);
        workingHourRepository.flush();
        List<WorkingHour> saved = new ArrayList<>();
        for (WorkingHourRequest req : requests) {
            if (!req.getStartTime().isBefore(req.getEndTime())) {
                throw new BusinessRuleException("startTime must be before endTime");
            }
            WorkingHour wh = WorkingHour.builder()
                    .employee(employee)
                    .dayOfWeek(req.getDayOfWeek())
                    .startTime(req.getStartTime())
                    .endTime(req.getEndTime())
                    .isAvailable(req.getIsAvailable() == null || req.getIsAvailable())
                    .build();
            saved.add(workingHourRepository.save(wh));
        }
        return saved.stream().map(w -> WorkingHourResponse.builder()
                .id(w.getId()).dayOfWeek(w.getDayOfWeek())
                .startTime(w.getStartTime()).endTime(w.getEndTime())
                .isAvailable(w.isAvailable()).build()).toList();
    }

    @Transactional(readOnly = true)
    public List<WorkingHourResponse> listWorkingHours(UUID businessId, UUID employeeId) {
        getInBusiness(employeeId, businessId);
        return workingHourRepository.findByEmployeeId(employeeId).stream()
                .map(w -> WorkingHourResponse.builder()
                        .id(w.getId()).dayOfWeek(w.getDayOfWeek())
                        .startTime(w.getStartTime()).endTime(w.getEndTime())
                        .isAvailable(w.isAvailable()).build()).toList();
    }

    @Transactional
    public TimeOffResponse addTimeOff(UUID businessId, UUID employeeId, TimeOffRequest request) {
        ownershipService.requireOwnedBusiness(businessId);
        Employee employee = getInBusiness(employeeId, businessId);
        if (!request.getStartAt().isBefore(request.getEndAt())) {
            throw new BusinessRuleException("startAt must be before endAt");
        }
        TimeOff timeOff = TimeOff.builder()
                .employee(employee).title(request.getTitle())
                .startAt(request.getStartAt()).endAt(request.getEndAt()).build();
        timeOffRepository.save(timeOff);
        return TimeOffResponse.builder().id(timeOff.getId()).title(timeOff.getTitle())
                .startAt(timeOff.getStartAt()).endAt(timeOff.getEndAt()).build();
    }

    @Transactional
    public void deleteTimeOff(UUID businessId, UUID employeeId, UUID timeOffId) {
        ownershipService.requireOwnedBusiness(businessId);
        getInBusiness(employeeId, businessId);
        TimeOff timeOff = timeOffRepository.findById(timeOffId)
                .orElseThrow(() -> new ResourceNotFoundException("Time off not found"));
        if (!timeOff.getEmployee().getId().equals(employeeId)) {
            throw new ResourceNotFoundException("Time off not found");
        }
        timeOffRepository.delete(timeOff);
    }

    private void linkServices(Employee employee, UUID businessId, List<UUID> serviceIds) {
        if (serviceIds == null) {
            employee.setServices(new HashSet<>());
            return;
        }
        Set<ServiceOffer> services = new HashSet<>();
        for (UUID sid : serviceIds) {
            ServiceOffer s = serviceOfferRepository.findByIdAndBusinessId(sid, businessId)
                    .orElseThrow(() -> new BusinessRuleException("Service not in business: " + sid));
            services.add(s);
        }
        employee.setServices(services);
    }

    private Employee getInBusiness(UUID employeeId, UUID businessId) {
        return employeeRepository.findByIdAndBusinessId(employeeId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
    }
}
