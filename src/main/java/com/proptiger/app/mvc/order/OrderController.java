package com.proptiger.app.mvc.order;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.proptiger.app.dto.order.LeadPaymentDto;
import com.proptiger.app.dto.order.LeadPrePaymentDTO;
import com.proptiger.app.service.order.LeadPaymentStatusService;
import com.proptiger.app.service.order.SellerCallService;
import com.proptiger.core.annotations.InternalIp;
import com.proptiger.core.annotations.LoggedIn;
import com.proptiger.core.dto.external.LeadPaymentStatusPatchDTO;
import com.proptiger.core.dto.internal.ActiveUser;
import com.proptiger.core.helper.MIDLServiceHelper;
import com.proptiger.core.meta.DisableCaching;
import com.proptiger.core.model.transaction.LeadPaymentStatus;
import com.proptiger.core.mvc.BaseController;
import com.proptiger.core.pojo.FIQLSelector;
import com.proptiger.core.pojo.response.APIResponse;
import com.proptiger.core.pojo.response.PaginatedResponse;
import com.proptiger.core.util.Constants;
//import com.proptiger.data.mvc.order.LeadPaymentDto;
//import com.proptiger.data.mvc.order.LeadPrePaymentDTO;
//import com.proptiger.data.service.order.LeadPaymentStatusService;
//import com.proptiger.data.service.order.SellerCallService;
//import com.proptiger.marketforce.data.service.SellerActionService;

/**
 * 
 * @author swapnil
 *
 */
@Controller
@DisableCaching
public class OrderController extends BaseController{
	
	@Autowired
    private LeadPaymentStatusService leadPaymentStatusService;

    @Autowired
    private SellerCallService        sellerCallService;
//---@Divyanshu
//    @Autowired
//    SellerActionService              sellerActionService;
    
    @Autowired
    MIDLServiceHelper				midlServiceHelper;

    private static final String      PATCH_LEAD_PAYMENT_STATUS_RESPONSE_STRING = "Lead amount updated in %d entries";

    /**
     *
     * @param selector
     * @param activeUser
     * @return
     */
    @LoggedIn()
    @ResponseBody
    @RequestMapping(value = "data/v1/order/leads", method = RequestMethod.GET)
    public APIResponse getAllocationHistoryBySelector(
            @ModelAttribute FIQLSelector selector,
            @ModelAttribute(Constants.LOGIN_INFO_OBJECT_NAME) ActiveUser activeUser) {
        return new APIResponse(leadPaymentStatusService.getLeadPaymentStatusBySelector(selector, activeUser));
    }

    /**
     *
     * @param leadPaymentStatuses
     * @param activeUser
     * @return
     */
    @LoggedIn()
    @ResponseBody
    @RequestMapping(value = "data/v1/order/leads", method = RequestMethod.PUT)
    public APIResponse updateLeadStatus(
            @RequestBody List<LeadPaymentStatus> leadPaymentStatuses,
            @ModelAttribute(Constants.LOGIN_INFO_OBJECT_NAME) ActiveUser activeUser) {
        return new APIResponse(leadPaymentStatusService.updateLeadStatus(leadPaymentStatuses, activeUser));
    }

    /**
     *
     * @param activeUser
     * @return
     */
    @LoggedIn()
    @ResponseBody
    @RequestMapping(value = "data/v1/order/lead-distribution", method = RequestMethod.GET)
    public APIResponse getLeadCountDistribution(
            @ModelAttribute(Constants.LOGIN_INFO_OBJECT_NAME) ActiveUser activeUser) {
        return new APIResponse(
                leadPaymentStatusService.getLeadCountDistribution(Integer.parseInt(activeUser.getUserId())));
    }

    /**
     * @param leadPaymentStatuses
     * @param sellerUserId
     * @return
     */
    @LoggedIn()
    @ResponseBody
    @RequestMapping(value = "data/v1/order/pre-payment/{sellerUserId}/lead", method = RequestMethod.PATCH)
    public APIResponse patchLeadWithPrepayment(
            @RequestBody LeadPaymentStatus leadPaymentStatuses,
            @PathVariable int sellerUserId,
            @ModelAttribute(Constants.LOGIN_INFO_OBJECT_NAME) ActiveUser activeUser) {
        return new APIResponse(
                leadPaymentStatusService.patchLeadWithPrepayment(leadPaymentStatuses, sellerUserId, activeUser));
    }

    @LoggedIn()
    @ResponseBody
    @RequestMapping(value = "data/v1/order/pre-payment/attach-leads", method = RequestMethod.PUT)
    public APIResponse batchUpdatePrePaymentWithLeads(
            @RequestBody List<LeadPaymentStatus> leadPaymentStatuses,
            @ModelAttribute(Constants.LOGIN_INFO_OBJECT_NAME) ActiveUser activeUser) {
        return new APIResponse(
                leadPaymentStatusService.batchUpdatePrePaymentWithLeads(leadPaymentStatuses, activeUser));
    }

    /**
     *
     * @param activeUser
     * @return
     */
    @LoggedIn()
    @ResponseBody
    @RequestMapping(value = "data/v1/order/lead-prepayment-distribution", method = RequestMethod.GET)
    public APIResponse getPrePaymentsLead(
            @ModelAttribute(Constants.LOGIN_INFO_OBJECT_NAME) ActiveUser activeUser,
            @RequestParam Integer sellerId,
            @RequestParam boolean dealDisclosed) {
        return new APIResponse(
                leadPaymentStatusService.getLeadCountDistribution(Integer.parseInt(activeUser.getUserId())));
    }

    /**
     *
     * @param leadPaymentDto
     * @param userInfo
     * @return
     */
    @LoggedIn()
    @ResponseBody
    @RequestMapping(value = "data/v1/order/leads", method = RequestMethod.POST)
    public APIResponse createCouponTransaction(
            @RequestBody LeadPaymentDto leadPaymentDto,
            @ModelAttribute(Constants.LOGIN_INFO_OBJECT_NAME) ActiveUser userInfo) {
        return new APIResponse(leadPaymentStatusService.addLeadPaymentStatus(leadPaymentDto, userInfo));
    }

    /**
     *
     * @param leadPaymentDto
     * @param userInfo
     * @return
     */
    @LoggedIn()
    @ResponseBody
    @RequestMapping(value = "data/v1/order/prepayment-leads", method = RequestMethod.POST)
    public APIResponse createPrePaymentTransaction(
            @RequestBody LeadPrePaymentDTO leadPrePaymentDTO,
            @ModelAttribute(Constants.LOGIN_INFO_OBJECT_NAME) ActiveUser userInfo) {
        return new APIResponse(leadPaymentStatusService.addPrePaymentLeadPaymentStatus(leadPrePaymentDTO, userInfo));
    }

    /**
     *
     * @param selector
     * @return
     */
    @InternalIp
    @ResponseBody
    @RequestMapping(value = "data/v1/order/data", method = RequestMethod.GET)
    public APIResponse getDataBySelector(@ModelAttribute FIQLSelector selector) {
        PaginatedResponse<List<LeadPaymentStatus>> leadPaymentStatus =
                leadPaymentStatusService.getDataBySelector(selector);
        return new APIResponse(super.filterFieldsFromSelector(leadPaymentStatus, selector));
    }

    /**
     * 
     * @param selector
     * @param activeUser
     * @return
     */
    @LoggedIn(roles = { "Calling" })
    @ResponseBody
    @RequestMapping(value = "data/v1/order/leads-data", method = RequestMethod.GET)
    public APIResponse getDataBySelectorForCalling(
            @ModelAttribute FIQLSelector selector,
            @ModelAttribute(Constants.LOGIN_INFO_OBJECT_NAME) ActiveUser activeUser) {
        PaginatedResponse<List<LeadPaymentStatus>> leadPaymentStatus =
                leadPaymentStatusService.getLeadPaymentDataUnderCro(selector, activeUser);
        return new APIResponse(super.filterFieldsFromSelector(leadPaymentStatus, selector));
    }

    /**
     * 
     * here sellerId is crmUserId
     * 
     * @param sellerId
     * @return
     */
    @InternalIp
    @ResponseBody
    @RequestMapping(value = "data/v1/order/payment-pending/send-email", method = RequestMethod.POST)
    public APIResponse sendEmailForRemainingLeadPayment(@RequestParam Integer sellerId) {
        leadPaymentStatusService.sendEmailForRemainingLeadPayment(sellerId);
        return new APIResponse("email request sent.");
    }

    /**
     *
     * @param leadId
     * @param userInfo
     * @return
     */
    @LoggedIn()
    @ResponseBody
    @RequestMapping(value = "data/v1/order/leads", method = RequestMethod.DELETE)
    public APIResponse deleteLeadPaymentStatus(
            @RequestParam Integer leadId,
            @ModelAttribute(Constants.LOGIN_INFO_OBJECT_NAME) ActiveUser userInfo) {
        return new APIResponse(leadPaymentStatusService.deleteLeadPaymentStatus(leadId, userInfo));
    }

    /**
     * @param leadPaymentIdList
     * @param userInfo
     * @return
     */
    @LoggedIn()
    @ResponseBody
    @RequestMapping(value = "data/v1/order/lead-payment-status", method = RequestMethod.PUT)
    public APIResponse updateLeadPaymentStatusByLeadPaymentStatusId(
            @RequestBody List<LeadPaymentStatus> leadPaymentStatusList,
            @ModelAttribute(Constants.LOGIN_INFO_OBJECT_NAME) ActiveUser userInfo) {
        return new APIResponse(
                leadPaymentStatusService.updateLeadPaymentStatusByLeadPaymentId(leadPaymentStatusList, userInfo));
    }

    /**
     * @param leadPaymentIdList
     * @param userInfo
     * @return
     */
    @LoggedIn()
    @ResponseBody
    @RequestMapping(value = "data/v1/order/lead-payment-status", method = RequestMethod.DELETE)
    public APIResponse deleteLeadPaymentStatusByLeadPaymentStatusId(
            @RequestParam List<Integer> leadPaymentIdList,
            @ModelAttribute(Constants.LOGIN_INFO_OBJECT_NAME) ActiveUser userInfo) {
        return new APIResponse(
                leadPaymentStatusService.deleteLeadPaymentStatusByLeadPaymentId(leadPaymentIdList, userInfo));
    }

    /**
     * @param leadIdList
     * @param userInfo
     * @return
     */
    @LoggedIn()
    @ResponseBody
    @RequestMapping(value = "data/v1/order/revert-lead-id", method = RequestMethod.PATCH)
    public APIResponse revertLeadIdFromLeadPaymentStatus(
            @RequestBody List<Integer> leadPaymentIdList,
            @ModelAttribute(Constants.LOGIN_INFO_OBJECT_NAME) ActiveUser userInfo) {
        return new APIResponse(leadPaymentStatusService.revertPrePaymentLeadIds(leadPaymentIdList, userInfo));
    }

    /**
     *
     * @return
     */
    @RequestMapping(value = "data/v1/process-call", method = RequestMethod.GET)
    @ResponseBody
    public APIResponse processCall() {
        int processed = sellerCallService.processMessagesInQueue();
        return new APIResponse("processed " + processed + " messages");
    }

    /**
     *
     * @param leadId
     * @param sellerId
     * @param sellerLeadActionId
     * @return
     */
    //OK TESTED
    @InternalIp
    @RequestMapping(
            value = "data/v1/entity/seller-lead-action-offline/{leadId:[\\d]+}/{sellerId:[\\d]+}/{sellerLeadActionId:[\\d]+}",
            method = RequestMethod.POST)
    @ResponseBody
    public APIResponse trackSellerResponseOffline(
            @PathVariable Integer leadId,
            @PathVariable Integer sellerId,
            @PathVariable Integer sellerLeadActionId) {
       // sellerActionService.trackSellerAction(sellerId, leadId, sellerLeadActionId, null);
    		midlServiceHelper.trackSellerAction(sellerId, leadId, sellerLeadActionId, null);
    	return new APIResponse("done");
    }

    @PreAuthorize("hasRole('InternalIP')")
    @RequestMapping(value = "v1/entity/lead-payment-status", method = RequestMethod.PATCH)
    @ResponseBody
    public APIResponse updateAmount(@RequestBody LeadPaymentStatusPatchDTO leadPaymentStatusPatchDTO) {
        String patchResponse = String.format(
                PATCH_LEAD_PAYMENT_STATUS_RESPONSE_STRING,
                leadPaymentStatusService.updateLeadAmount(leadPaymentStatusPatchDTO));
        return new APIResponse(patchResponse);
    }
    //Ok tested
    @ResponseBody
    @RequestMapping(value="data/v1/order/lead-payment-status-by-id",method=RequestMethod.GET)
    public APIResponse findByLeadId(@RequestParam Integer leadId)
    {
    	return new APIResponse(leadPaymentStatusService.findByLeadId(leadId));
    }
    //PENDING
    //SOME POST ISSUE IN MicroServiceHelper
    @ResponseBody
    @RequestMapping(value="data/v1/order/save/lead-payment-status",method=RequestMethod.POST)
    public APIResponse saveLeadPaymentStatusAsProductPaymentStatus(@RequestBody List<LeadPaymentStatus> leadPaymentStatus)
    {
    		return new APIResponse(leadPaymentStatusService.saveLeadPaymentStatusAsProductPaymentStatus(leadPaymentStatus));
    }
    //OK TESTED
    @ResponseBody
    @RequestMapping(value="data/v1/order/count-deals-disclosed",method=RequestMethod.GET)
    public APIResponse getCountOfLeadsDisclosed(@RequestParam int userId,
    		@RequestParam Date date,
    		@RequestParam int leadTypeId)
    {
    	return new APIResponse(leadPaymentStatusService.getCountOfLeadsDisclosed(userId, date, leadTypeId));
    }
    @ResponseBody
    @RequestMapping(value="data/v1/order/lead-payment-status-by-selector",method=RequestMethod.GET)
    public APIResponse getLeadPaymentStatusBySelector(@RequestParam FIQLSelector selector)
    {
    	return new APIResponse(leadPaymentStatusService.getLeadPaymentStatusBySelector(selector));
    }
    //OK TESTED
    @ResponseBody
    @RequestMapping(value="data/v1/order/seller-with-date-and-type",method=RequestMethod.GET)
    public APIResponse findPaidSellersWithLastPaymentDateForLeadType(@RequestParam Integer saleTypeId)
    {
 	   return new APIResponse(leadPaymentStatusService.findPaidSellersWithLastPaymentDateForLeadType(saleTypeId));
    }
}
