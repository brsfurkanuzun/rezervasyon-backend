package com.randevupazaryeri.employee.service;

import com.randevupazaryeri.business.entity.Business;
import com.randevupazaryeri.business.service.BusinessOwnershipService;
import com.randevupazaryeri.common.exception.BusinessRuleException;
import com.randevupazaryeri.common.exception.ResourceNotFoundException;
import com.randevupazaryeri.common.security.SecurityUtils;
import com.randevupazaryeri.employee.dto.CreateInvitationRequest;
import com.randevupazaryeri.employee.dto.EmployeeResponse;
import com.randevupazaryeri.employee.dto.InvitationResponse;
import com.randevupazaryeri.employee.entity.Employee;
import com.randevupazaryeri.employee.entity.EmployeeInvitation;
import com.randevupazaryeri.employee.entity.InvitationStatus;
import com.randevupazaryeri.employee.entity.WorkingHour;
import com.randevupazaryeri.employee.mapper.EmployeeMapper;
import com.randevupazaryeri.employee.repository.EmployeeInvitationRepository;
import com.randevupazaryeri.employee.repository.EmployeeRepository;
import com.randevupazaryeri.employee.repository.WorkingHourRepository;
import com.randevupazaryeri.serviceoffer.repository.ServiceOfferRepository;
import com.randevupazaryeri.notification.service.NotificationService;
import com.randevupazaryeri.push.entity.PushApp;
import com.randevupazaryeri.user.entity.Role;
import com.randevupazaryeri.user.entity.User;
import com.randevupazaryeri.user.repository.UserRepository;
import com.randevupazaryeri.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Owners invite people to their team. An open invitation (no employee) creates the expert profile from the
 * invitee's account; an employee-bound one links the account to that existing profile.
 */
@Service
@RequiredArgsConstructor
public class EmployeeInvitationService {
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;
    private static final Duration TTL = Duration.ofDays(7);
    private static final String LINK_PREFIX = "rezplzpartner://join/";
    private static final LocalTime DEFAULT_OPEN = LocalTime.of(9, 0);
    private static final LocalTime DEFAULT_CLOSE = LocalTime.of(18, 0);

    private final EmployeeInvitationRepository invitationRepository;
    private final EmployeeRepository employeeRepository;
    private final WorkingHourRepository workingHourRepository;
    private final ServiceOfferRepository serviceOfferRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final BusinessOwnershipService ownershipService;
    private final NotificationService notificationService;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public InvitationResponse create(UUID businessId, UUID employeeId, CreateInvitationRequest request) {
        Business business = ownershipService.requireOwnedBusiness(businessId);
        Employee employee = getEmployee(businessId, employeeId);
        if (employee.getUser() != null) {
            throw new BusinessRuleException("This employee is already linked to an account");
        }
        revokePending(employeeId);
        return issue(business, employee, request);
    }

    /** A fresh single-use code for a new expert; their profile is created from their account when they join. */
    @Transactional
    public InvitationResponse createOpen(UUID businessId, CreateInvitationRequest request) {
        return issue(ownershipService.requireOwnedBusiness(businessId), null, request);
    }

    @Transactional(readOnly = true)
    public List<InvitationResponse> pendingOpen(UUID businessId) {
        ownershipService.requireOwnedBusiness(businessId);
        return invitationRepository
                .findByBusinessIdAndEmployeeIsNullAndStatusOrderByCreatedAtDesc(businessId, InvitationStatus.PENDING)
                .stream()
                .filter(EmployeeInvitation::isUsable)
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void revokeById(UUID businessId, UUID invitationId) {
        ownershipService.requireOwnedBusiness(businessId);
        EmployeeInvitation invitation = invitationRepository.findByIdAndBusinessId(invitationId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));
        if (invitation.getStatus() == InvitationStatus.PENDING) {
            invitation.setStatus(InvitationStatus.REVOKED);
        }
    }

    /** The owner joins their own team as an expert with a new profile. */
    @Transactional
    public EmployeeResponse addOwnerAsEmployee(UUID businessId) {
        Business business = ownershipService.requireOwnedBusiness(businessId);
        User owner = business.getOwner();
        if (employeeRepository.existsByBusinessIdAndUserId(businessId, owner.getId())) {
            throw new BusinessRuleException("You are already linked to another employee profile");
        }
        return EmployeeMapper.toResponse(createLinkedEmployee(business, owner));
    }

    private InvitationResponse issue(Business business, Employee employee, CreateInvitationRequest request) {
        String email = request != null && request.getEmail() != null && !request.getEmail().isBlank()
                ? request.getEmail().trim().toLowerCase(Locale.ROOT) : null;
        EmployeeInvitation invitation = invitationRepository.save(EmployeeInvitation.builder()
                .business(business)
                .employee(employee)
                .code(newCode())
                .email(email)
                .status(InvitationStatus.PENDING)
                .expiresAt(Instant.now().plus(TTL))
                .build());

        if (email != null) {
            userRepository.findByEmailIgnoreCase(email)
                    .filter(user -> user.getRole() == Role.PROVIDER)
                    .ifPresent(user -> notificationService.notifyUser(user.getId(), "TEAM_INVITATION",
                            "Ekibe katılma daveti",
                            business.getName() + " seni uzman olarak ekibine davet etti.",
                            PushApp.PARTNER, Map.of("type", "TEAM_INVITATION", "code", invitation.getCode())));
        }
        return toResponse(invitation);
    }

    @Transactional(readOnly = true)
    public List<InvitationResponse> pendingForEmployee(UUID businessId, UUID employeeId) {
        ownershipService.requireOwnedBusiness(businessId);
        getEmployee(businessId, employeeId);
        return invitationRepository.findByEmployeeIdAndStatus(employeeId, InvitationStatus.PENDING).stream()
                .filter(EmployeeInvitation::isUsable)
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void revoke(UUID businessId, UUID employeeId) {
        ownershipService.requireOwnedBusiness(businessId);
        getEmployee(businessId, employeeId);
        revokePending(employeeId);
    }

    /** Removes the staff member's access; the employee record and its history stay. */
    @Transactional
    public EmployeeResponse unlinkAccount(UUID businessId, UUID employeeId) {
        ownershipService.requireOwnedBusiness(businessId);
        Employee employee = getEmployee(businessId, employeeId);
        employee.setUser(null);
        return EmployeeMapper.toResponse(employee);
    }

    /** The owner works as an expert too: links the owner's own account to one of the employee profiles. */
    @Transactional
    public EmployeeResponse linkOwner(UUID businessId, UUID employeeId) {
        Business business = ownershipService.requireOwnedBusiness(businessId);
        Employee employee = getEmployee(businessId, employeeId);
        User owner = business.getOwner();
        if (employee.getUser() != null) {
            throw new BusinessRuleException("This employee is already linked to an account");
        }
        if (employeeRepository.existsByBusinessIdAndUserId(businessId, owner.getId())) {
            throw new BusinessRuleException("You are already linked to another employee profile");
        }
        revokePending(employeeId);
        employee.setUser(owner);
        return EmployeeMapper.toResponse(employee);
    }

    @Transactional(readOnly = true)
    public InvitationResponse preview(String code) {
        return toResponse(getUsable(code));
    }

    @Transactional(readOnly = true)
    public List<InvitationResponse> mine() {
        User me = userService.getById(SecurityUtils.currentUserId());
        return invitationRepository.findPendingForEmail(me.getEmail(), Instant.now()).stream()
                .filter(i -> i.getEmployee() == null || i.getEmployee().getUser() == null)
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public InvitationResponse accept(String code) {
        EmployeeInvitation invitation = getUsable(code);
        User me = userService.getById(SecurityUtils.currentUserId());
        Business business = invitation.getBusiness();
        Employee employee = invitation.getEmployee();

        if (me.getRole() != Role.PROVIDER) {
            throw new BusinessRuleException("Only business accounts can join a team");
        }
        if (business.getOwner().getId().equals(me.getId())) {
            throw new BusinessRuleException("You already own this business");
        }
        if (employeeRepository.existsByBusinessIdAndUserId(business.getId(), me.getId())) {
            throw new BusinessRuleException("You are already on this team");
        }
        if (employee != null && employee.getUser() != null) {
            throw new BusinessRuleException("This employee is already linked to an account");
        }

        String joinedAs;
        if (employee == null) {
            employee = createLinkedEmployee(business, me);
            invitation.setEmployee(employee);
            joinedAs = " ekibine uzman olarak katıldı.";
        } else {
            employee.setUser(me);
            joinedAs = ", " + employee.getFirstName() + " " + employee.getLastName() + " olarak ekibine katıldı.";
        }
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedBy(me);

        notificationService.notifyUser(business.getOwner().getId(), "TEAM_INVITATION_ACCEPTED",
                "Uzman ekibe katıldı",
                me.getFirstName() + " " + me.getLastName() + joinedAs,
                PushApp.PARTNER, Map.of("type", "TEAM_INVITATION_ACCEPTED", "employeeId", employee.getId().toString()));
        return toResponse(invitation);
    }

    /**
     * New expert profile for an account. It starts bookable: every active service of the business and
     * Monday–Saturday 09:00–18:00; the owner adjusts both afterwards.
     */
    private Employee createLinkedEmployee(Business business, User user) {
        Employee employee = employeeRepository.save(Employee.builder()
                .business(business)
                .user(user)
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .photoUrl(user.getPhotoUrl())
                .isActive(true)
                .services(new HashSet<>(serviceOfferRepository.findByBusinessIdAndIsActiveTrue(business.getId())))
                .build());
        for (int day = 1; day <= 6; day++) {
            workingHourRepository.save(WorkingHour.builder()
                    .employee(employee)
                    .dayOfWeek(day)
                    .startTime(DEFAULT_OPEN)
                    .endTime(DEFAULT_CLOSE)
                    .isAvailable(true)
                    .build());
        }
        return employee;
    }

    private EmployeeInvitation getUsable(String code) {
        return invitationRepository.findByCode(normalize(code))
                .filter(EmployeeInvitation::isUsable)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found or expired"));
    }

    private Employee getEmployee(UUID businessId, UUID employeeId) {
        return employeeRepository.findByIdAndBusinessId(employeeId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
    }

    private void revokePending(UUID employeeId) {
        invitationRepository.findByEmployeeIdAndStatus(employeeId, InvitationStatus.PENDING)
                .forEach(i -> i.setStatus(InvitationStatus.REVOKED));
    }

    private String newCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }
            code = sb.toString();
        } while (invitationRepository.existsByCode(code));
        return code;
    }

    static String normalize(String code) {
        return code == null ? "" : code.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    private static String display(String code) {
        return code.substring(0, 4) + "-" + code.substring(4);
    }

    private InvitationResponse toResponse(EmployeeInvitation i) {
        Employee e = i.getEmployee();
        Business b = i.getBusiness();
        return InvitationResponse.builder()
                .id(i.getId())
                .code(display(i.getCode()))
                .link(LINK_PREFIX + i.getCode())
                .email(i.getEmail())
                .status(i.getStatus())
                .expiresAt(i.getExpiresAt())
                .businessId(b.getId())
                .businessName(b.getName())
                .businessLogoUrl(b.getLogoUrl())
                .employeeId(e != null ? e.getId() : null)
                .employeeName(e != null ? e.getFirstName() + " " + e.getLastName() : null)
                .employeeTitle(e != null ? e.getTitle() : null)
                .build();
    }
}
