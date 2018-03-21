package com.proptiger.app.mvc.order;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.proptiger.app.service.order.ProductPaymentStatusService;
import com.proptiger.core.annotations.LoggedIn;
import com.proptiger.core.dto.internal.ActiveUser;
import com.proptiger.core.dto.order.ProductPrePaymentDto;
import com.proptiger.core.meta.DisableCaching;
import com.proptiger.core.model.transaction.ProductPaymentStatus;
import com.proptiger.core.pojo.response.APIResponse;
import com.proptiger.core.util.Constants;
//import com.proptiger.data.dto.order.ProductPrePaymentDto;
//import com.proptiger.data.service.order.ProductPaymentStatusService;

import io.swagger.annotations.Api;

@Controller
@DisableCaching
@RequestMapping(value = "data/v2/order/")
public class OrderControllerV2 {

	@Autowired
	private ProductPaymentStatusService productPaymentStatusService;

	@ResponseBody
	@RequestMapping(value = "product-prepayment", method = RequestMethod.POST)
	public APIResponse createPrePaymentTransaction(@RequestBody List<ProductPrePaymentDto> productPrePaymentDTOList,
			@ModelAttribute(Constants.LOGIN_INFO_OBJECT_NAME) ActiveUser userInfo) {
		return new APIResponse(productPaymentStatusService.addPrePaymentList(productPrePaymentDTOList, userInfo));
	}

	/*
	 * @Divyanshu
	 */
	@ResponseBody
	@RequestMapping(value = "product-payment-history", method = RequestMethod.GET)
	public APIResponse getProductPaymentHistoryBySeller(@RequestParam Integer sellerId) {
		return new APIResponse(productPaymentStatusService.getProductPaymentHistoryBySeller(sellerId));
	}
	
	@ResponseBody
	@RequestMapping(value="product-payment-leads-counts",method=RequestMethod.GET)
	public APIResponse getLeadCounts(@RequestParam List<Integer> crmUserIds)
	{
		return new APIResponse(productPaymentStatusService.getLeadCounts(crmUserIds));
	}
	
	@ResponseBody
	@RequestMapping(value="product-payment-prepostpaid-leads-count",method=RequestMethod.GET)
	public APIResponse getPrepaidAndPostPaidLeadCounts(@RequestParam Set<Integer> sellerIds)
	{
		return new APIResponse(productPaymentStatusService.getPrepaidAndPostPaidLeadCounts(sellerIds));
	}
    /**
    *
    * @param sellerId (activeUser)
    * @return
    */
//   @LoggedIn()
   @ResponseBody
   @RequestMapping(value = "lead-distribution", method = RequestMethod.GET)
   public APIResponse getLeadCountDistribution( @RequestParam Integer sellerId) {
         //  @ModelAttribute(Constants.LOGIN_INFO_OBJECT_NAME) ActiveUser activeUser) {
       return new APIResponse(
    		   productPaymentStatusService.getLeadCountDistribution(sellerId));
      //         productPaymentStatusService.getLeadCountDistribution(Integer.parseInt(activeUser.getUserId())));
   }
   @ResponseBody
   @RequestMapping(value="seller-with-date-and-type",method=RequestMethod.GET)
   public APIResponse findPaidSellersWithLastPaymentDateForLeadType(@RequestParam Integer saleTypeId)
   {
	   return new APIResponse(productPaymentStatusService.findPaidSellersWithLastPaymentDateForLeadType(saleTypeId));
   }
   
   @ResponseBody
   @RequestMapping(value="product-payment-status-by-productid",method=RequestMethod.GET)
   public APIResponse getProductPaymentStatusByProductId(@RequestParam Integer productId)
   {
	   return new APIResponse(productPaymentStatusService.getProductPaymentStatusByProductId(productId));
   }
   
   @ResponseBody
   @RequestMapping(value="prod-payment-status-by-ids-in",method=RequestMethod.GET)
   public APIResponse findByIdIn(@RequestParam Collection<Integer> ids)
   {
	   return new APIResponse(productPaymentStatusService.findByIdIn(ids));
   }
   
   @ResponseBody
   @RequestMapping(value="prod-payment-status-update",method=RequestMethod.GET)
   public APIResponse updateProductPaymentStatus(
		  @RequestParam List<ProductPaymentStatus> productPaymentStatuses,
           @RequestParam int crmUserId)
   {
	   return new APIResponse(productPaymentStatusService.updateProductPaymentStatus(productPaymentStatuses, crmUserId));
   }
   
   @ResponseBody
   @RequestMapping(value="prod-payment-update-lead-status",method=RequestMethod.GET)
   public APIResponse updateLeadStatus(
		  @RequestParam List<ProductPaymentStatus> productPaymentStatuses,
           @RequestParam int crmUserId)
   {
	   return new APIResponse(productPaymentStatusService.updateLeadStatus(productPaymentStatuses, crmUserId));
   }
   @ResponseBody
   @RequestMapping(value="prod-payment-by-prod-id-and-type",method=RequestMethod.GET)
   public APIResponse findByProductTypeIdAndProductId(@RequestParam Integer productTypeId,
		   @RequestParam Integer productId)
   {
	   return new APIResponse(productPaymentStatusService.findByProductTypeIdAndProductId(productTypeId, productId));
   }
   @ResponseBody
   @RequestMapping(value="prod-payment-by-id",method=RequestMethod.GET)
   public APIResponse findById(@RequestParam Integer id)
   {
	   return new APIResponse(productPaymentStatusService.findById(id));
   }
   
   @ResponseBody
   @RequestMapping(value="prod-payment-save",method=RequestMethod.GET)
   public APIResponse saveProductPaymentStatus(@RequestParam List<ProductPaymentStatus> productPaymentStatusList)
   {
	   return new APIResponse(productPaymentStatusService.saveProductPaymentStatus(productPaymentStatusList));
   }
}
