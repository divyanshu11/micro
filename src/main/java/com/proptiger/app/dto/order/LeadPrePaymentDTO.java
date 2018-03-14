package com.proptiger.app.dto.order;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.proptiger.core.dto.order.LeadsCityDTO;
import com.proptiger.core.model.BaseModel;
//import com.proptiger.data.dto.order.LeadsCityDTO;
@JsonInclude(Include.NON_NULL)
public class LeadPrePaymentDTO extends BaseModel{
	 /**
     * 
     */
    private static final long serialVersionUID = 9069834119602318955L;

    private List<LeadsCityDTO> leadCityDtoList;
    private String saleType;

    private Integer            crmUserId;
    private Integer            transactionId;
    private Integer            leadPaymentStatusId;


    public List<LeadsCityDTO> getLeadCityDtoList() {
        return leadCityDtoList;
    }

    public void setLeadCityDtoList(List<LeadsCityDTO> leadCityDtoList) {
        this.leadCityDtoList = leadCityDtoList;
    }

    public String getSaleType() {
        return saleType;
    }

    public void setSaleType(String saleType) {
        this.saleType = saleType;
    }

    public Integer getCrmUserId() {
        return crmUserId;
    }

    public void setCrmUserId(Integer crmUserId) {
        this.crmUserId = crmUserId;
    }

    public Integer getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Integer transactionId) {
        this.transactionId = transactionId;
    }

    public Integer getLeadPaymentStatusId() {
        return leadPaymentStatusId;
    }

    public void setLeadPaymentStatusId(Integer leadPaymentStatusId) {
        this.leadPaymentStatusId = leadPaymentStatusId;
    }

}
