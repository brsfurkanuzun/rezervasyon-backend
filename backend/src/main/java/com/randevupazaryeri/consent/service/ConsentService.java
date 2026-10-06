package com.randevupazaryeri.consent.service;

import com.randevupazaryeri.consent.dto.ConsentsRequest;
import com.randevupazaryeri.consent.entity.ConsentType;
import com.randevupazaryeri.consent.entity.UserConsent;
import com.randevupazaryeri.consent.repository.UserConsentRepository;
import com.randevupazaryeri.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsentService {

    private final UserConsentRepository consentRepository;

    /** Stores the sign-up decisions of a newly created account. */
    @Transactional
    public void recordSignUp(User user, ConsentsRequest request) {
        if (request == null) {
            return;
        }
        List<UserConsent> consents = new ArrayList<>();
        addIfAccepted(consents, user, ConsentType.TERMS, request.getTermsAccepted(), request.getChannel());
        addIfAccepted(consents, user, ConsentType.PARTNER_TERMS, request.getPartnerTermsAccepted(), request.getChannel());
        addIfAccepted(consents, user, ConsentType.KVKK, request.getKvkkAcknowledged(), request.getChannel());
        addIfAccepted(consents, user, ConsentType.PRIVACY, request.getPrivacyAcknowledged(), request.getChannel());
        if (request.getMarketingConsent() != null) {
            consents.add(consent(user, ConsentType.MARKETING, request.getMarketingConsent(), request.getChannel()));
        }
        consentRepository.saveAll(consents);
    }

    private void addIfAccepted(List<UserConsent> consents, User user, ConsentType type, Boolean value, String channel) {
        if (Boolean.TRUE.equals(value)) {
            consents.add(consent(user, type, true, channel));
        }
    }

    private UserConsent consent(User user, ConsentType type, boolean granted, String channel) {
        return UserConsent.builder()
                .user(user)
                .consentType(type)
                .granted(granted)
                .channel(channel)
                .build();
    }
}
