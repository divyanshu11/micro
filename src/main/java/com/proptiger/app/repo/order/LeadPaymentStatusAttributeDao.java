package com.proptiger.app.repo.order;

import org.springframework.data.jpa.repository.JpaRepository;

import com.proptiger.core.model.transaction.LeadPaymentStatusMetaAttributes;

public interface LeadPaymentStatusAttributeDao extends JpaRepository<LeadPaymentStatusMetaAttributes, Integer> {

}
