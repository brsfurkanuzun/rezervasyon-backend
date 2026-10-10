package com.randevupazaryeri.consent.service;

import com.randevupazaryeri.consent.dto.ConsentsRequest;
import com.randevupazaryeri.consent.entity.ConsentType;
import com.randevupazaryeri.consent.entity.NoticeType;
import com.randevupazaryeri.consent.entity.UserConsent;
import com.randevupazaryeri.consent.entity.UserNoticeReceipt;
import com.randevupazaryeri.consent.repository.UserConsentRepository;
import com.randevupazaryeri.consent.repository.UserNoticeReceiptRepository;
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
    private final UserNoticeReceiptRepository noticeReceiptRepository;

    /** Stores the sign-up decisions of a newly created account. */
    @Transactional
    public void recordSignUp(User user, ConsentsRequest request) {
        if (request == null) {
            return;
        }
        List<UserConsent> consents = new ArrayList<>();
        addIfAccepted(consents, user, ConsentType.TERMS, request.getTermsAccepted(), request.getChannel(), request.getTermsVersion());
        addIfAccepted(consents, user, ConsentType.PARTNER_TERMS, request.getPartnerTermsAccepted(), request.getChannel(), request.getPartnerTermsVersion());
        // Backward compatibility for older native clients. New clients record notices separately below.
        addIfAccepted(consents, user, ConsentType.KVKK, request.getKvkkAcknowledged(), request.getChannel());
        addIfAccepted(consents, user, ConsentType.PRIVACY, request.getPrivacyAcknowledged(), request.getChannel());
        if (request.getMarketingConsent() != null) {
            consents.add(consent(user, ConsentType.MARKETING, request.getMarketingConsent(), request.getChannel(), request.getMarketingConsentVersion()));
        }
        consentRepository.saveAll(consents);
        addNoticeReceipt(user, NoticeType.KVKK_NOTICE, request.getKvkkNoticeVersion(), request.getChannel());
        addNoticeReceipt(user, NoticeType.PRIVACY_POLICY, request.getPrivacyPolicyVersion(), request.getChannel());
    }

    private void addIfAccepted(List<UserConsent> consents, User user, ConsentType type, Boolean value, String channel) {
        addIfAccepted(consents, user, type, value, channel, null);
    }

    private void addIfAccepted(List<UserConsent> consents, User user, ConsentType type, Boolean value, String channel, String version) {
        if (Boolean.TRUE.equals(value)) {
            consents.add(consent(user, type, true, channel, version));
        }
    }

    private UserConsent consent(User user, ConsentType type, boolean granted, String channel, String version) {
        return UserConsent.builder()
                .user(user)
                .consentType(type)
                .granted(granted)
                .channel(channel)
                .documentVersion(version)
                .build();
    }

    private void addNoticeReceipt(User user, NoticeType type, String version, String channel) {
        if (version == null || version.isBlank() || channel == null || channel.isBlank()) {
            return;
        }
        noticeReceiptRepository.save(UserNoticeReceipt.builder()
                .user(user)
                .noticeType(type)
                .documentVersion(version.trim())
                .channel(channel)
                .build());
    }
}
