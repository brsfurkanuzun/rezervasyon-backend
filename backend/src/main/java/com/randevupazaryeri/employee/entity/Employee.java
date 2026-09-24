package com.randevupazaryeri.employee.entity;

import com.randevupazaryeri.business.entity.Business;
import com.randevupazaryeri.serviceoffer.entity.ServiceOffer;
import com.randevupazaryeri.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "employees")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Employee {
    @Id @GeneratedValue @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "first_name", nullable = false)
    private String firstName;
    @Column(name = "last_name", nullable = false)
    private String lastName;
    private String title;
    @Column(columnDefinition = "TEXT")
    private String bio;
    @Column(name = "photo_url")
    private String photoUrl;

    @ElementCollection
    @CollectionTable(name = "employee_portfolio_urls", joinColumns = @JoinColumn(name = "employee_id"))
    @Column(name = "url", nullable = false)
    @OrderColumn(name = "sort_order")
    @Builder.Default
    private List<String> portfolioUrls = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "employee_languages", joinColumns = @JoinColumn(name = "employee_id"))
    @Column(name = "language", nullable = false)
    @OrderColumn(name = "sort_order")
    @Builder.Default
    private List<String> languages = new ArrayList<>();

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @ManyToMany
    @JoinTable(name = "employee_services",
            joinColumns = @JoinColumn(name = "employee_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id"))
    @Builder.Default
    private Set<ServiceOffer> services = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now; updatedAt = now;
    }
    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}
