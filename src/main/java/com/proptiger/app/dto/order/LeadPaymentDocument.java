package com.proptiger.app.dto.order;

import java.io.Serializable;

/**
 * leadId with payment detail 
 */
public class LeadPaymentDocument implements Serializable{

	 /**
     * 
     */
    private static final long serialVersionUID = 7248795147970438020L;
    private String            paymentType;
    private String            paymentNumber;
    private Integer           leadId;

    /**
     * @param paymentNumber ie cheque,demand draft no. etc
     * @param leadId
     * @param paymentType is online,cheque e
     */
    public LeadPaymentDocument(String paymentNumber, Integer leadId, String paymentType) {
        this.paymentNumber = paymentNumber;
        this.leadId = leadId;
        this.paymentType = paymentType;
    }
    
    /**
     * empty public constructor for hql query because of the different package
     */
    public LeadPaymentDocument() {
        /**
         * empty public constructor
         */
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getPaymentNumber() {
        return paymentNumber;
    }

    public void setPaymentNumber(String paymentNumber) {
        this.paymentNumber = paymentNumber;
    }

    public Integer getLeadId() {
        return leadId;
    }

    public void setLeadId(Integer leadId) {
        this.leadId = leadId;
    }

}
