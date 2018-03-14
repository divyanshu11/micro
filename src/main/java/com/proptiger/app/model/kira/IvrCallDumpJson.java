package com.proptiger.app.model.kira;

import com.proptiger.core.enums.Domain;
import com.proptiger.core.model.BaseModel;
/**
*
* @author swapnil
*
*/
public class IvrCallDumpJson extends BaseModel{
	/**
    *
    */
   private static final long serialVersionUID = -6583114801450593752L;

   private String recordingUrl;

   private Integer sellerId;

   private Integer cityId;

   private String clientPhone;

   private Domain domainId;

   private Integer callLogId;

   private Integer clientCountryId;

   private Integer enquiryTypeId;

   private String callStatus;

   private String virtualNumber;

   public String getRecordingUrl() {
       return recordingUrl;
   }

   public void setRecordingUrl(String recordingUrl) {
       this.recordingUrl = recordingUrl;
   }

   public Integer getSellerId() {
       return sellerId;
   }

   public void setSellerId(Integer sellerId) {
       this.sellerId = sellerId;
   }

   public Integer getCityId() {
       return cityId;
   }

   public void setCityId(Integer cityId) {
       this.cityId = cityId;
   }

   public String getClientPhone() {
       return clientPhone;
   }

   public void setClientPhone(String clientPhone) {
       this.clientPhone = clientPhone;
   }

   public Domain getDomainId() {
       return domainId;
   }

   public void setDomainId(Domain domainId) {
       this.domainId = domainId;
   }

   public Integer getCallLogId() {
       return callLogId;
   }

   public void setCallLogId(Integer callLogId) {
       this.callLogId = callLogId;
   }

   public Integer getClientCountryId() {
       return clientCountryId;
   }

   public void setClientCountryId(Integer clientCountryId) {
       this.clientCountryId = clientCountryId;
   }

   public Integer getEnquiryTypeId() {
       return enquiryTypeId;
   }

   public void setEnquiryTypeId(Integer enquiryTypeId) {
       this.enquiryTypeId = enquiryTypeId;
   }

   public String getCallStatus() {
       return callStatus;
   }

   public void setCallStatus(String callStatus) {
       this.callStatus = callStatus;
   }

   public String getVirtualNumber() {
       return virtualNumber;
   }

   public void setVirtualNumber(String virtualNumber) {
       this.virtualNumber = virtualNumber;
   }

}
