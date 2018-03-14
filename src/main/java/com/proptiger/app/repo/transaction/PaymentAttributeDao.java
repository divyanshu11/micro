package com.proptiger.app.repo.transaction;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.proptiger.app.dto.order.LeadPaymentDocument;
//import com.proptiger.app.dto.order.LeadPaymentDocument;
import com.proptiger.core.model.transaction.PaymentAttribute;
//import com.proptiger.data.repo.transaction.LeadPaymentDocument;
/**
 * payment Attribute like demand draft number,cheque number 
 */
@Repository
public interface PaymentAttributeDao extends JpaRepository<PaymentAttribute, Integer>{
	/**
     * 
     * @param leadIds
     * @param paymentypeTId
     * @param transactionTypeId
     * @return 
     */
    @Query("Select NEW com.proptiger.app.dto.order.LeadPaymentDocument(PA.paymentNumber,TPM.productId,PT.label) from PaymentAttribute PA join PA.payment P join P.masterPaymentType PT join P.transaction T join T.transactionProductMapping TPM where TPM.productId in ?1 and T.typeId = ?2")
    public List<LeadPaymentDocument> getPaymentNumberFromLeadIds(
            List<Integer> leadIds,
            Integer transactionTypeId);

    @Query("Select NEW com.proptiger.app.dto.order.LeadPaymentDocument(PA.paymentNumber, LPS.id, PT.label) from LeadPaymentStatus LPS JOIN LPS.payment P JOIN P.transaction T JOIN P.paymentAttribute PA join P.masterPaymentType PT WHERE LPS.id in ?1 and T.typeId = ?2")
    public List<LeadPaymentDocument> getPaymentNumberFromLeadPaymentStatusIds(
            List<Integer> leadPaymentStatusIds,
            Integer transactionTypeId);
}
