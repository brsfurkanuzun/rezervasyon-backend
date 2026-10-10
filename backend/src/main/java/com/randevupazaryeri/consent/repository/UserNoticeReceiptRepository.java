package com.randevupazaryeri.consent.repository;

import com.randevupazaryeri.consent.entity.UserNoticeReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserNoticeReceiptRepository extends JpaRepository<UserNoticeReceipt, UUID> {
}
