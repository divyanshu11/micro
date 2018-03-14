package com.proptiger.app.dto.order;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.proptiger.core.model.BaseModel;

/**
 * 
 * @author swapnil
 *
 */
@JsonInclude(Include.NON_NULL)
public class LeadPaymentStatusDto extends BaseModel{

	/**
    *
    */
   private static final long serialVersionUID = 5828691781353106705L;
   private int               id;
   private String            statusName;
   private String            leadSaleType;
   private long              leadCount;
   private int               sellerId;
   private Date              paymentDate;

   /**
    * Default constructor
    */
   public LeadPaymentStatusDto() {
       super();
   }

   /**
    *
    * @param id
    * @param statusName
    * @param leadSaleType
    * @param leadCount
    */
   public LeadPaymentStatusDto(int id, String statusName, String leadSaleType, int leadCount) {
       super();
       this.id = id;
       this.setStatusName(statusName);
       this.setLeadSaleType(leadSaleType);
       this.setLeadCount(leadCount);
   }

   /**
    * 
    * @param sellerId
    * @param paymentDate
    */
   public LeadPaymentStatusDto(int sellerId, Date paymentDate){
       super();
       this.sellerId = sellerId;
       this.paymentDate = paymentDate;
   }
   
   public int getId() {
       return id;
   }

   public void setId(int id) {
       this.id = id;
   }

   public String getStatusName() {
       return statusName;
   }

   public void setStatusName(String statusName) {
       this.statusName = statusName;
   }

   public String getLeadSaleType() {
       return leadSaleType;
   }

   public void setLeadSaleType(String leadSaleType) {
       this.leadSaleType = leadSaleType;
   }

   public long getLeadCount() {
       return leadCount;
   }

   public void setLeadCount(long leadCount) {
       this.leadCount = leadCount;
   }

   public int getSellerId() {
       return sellerId;
   }

   public void setSellerId(int sellerId) {
       this.sellerId = sellerId;
   }

   public Date getPaymentDate() {
       return paymentDate;
   }

   public void setPaymentDate(Date paymentDate) {
       this.paymentDate = paymentDate;
   }
}
