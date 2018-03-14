package com.proptiger.app.mvc.order;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.proptiger.app.service.order.ProductPaymentStatusService;
import com.proptiger.core.dto.internal.ActiveUser;
import com.proptiger.core.dto.order.ProductPrePaymentDto;
import com.proptiger.core.meta.DisableCaching;
import com.proptiger.core.pojo.response.APIResponse;
import com.proptiger.core.util.Constants;
//import com.proptiger.data.dto.order.ProductPrePaymentDto;
//import com.proptiger.data.service.order.ProductPaymentStatusService;

@Controller
@DisableCaching
@RequestMapping(value = "data/v2/order/")
public class OrderControllerV2 {

	 @Autowired
	    private ProductPaymentStatusService productPaymentStatusService;

	    @ResponseBody
	    @RequestMapping(value = "product-prepayment", method = RequestMethod.POST)
	    public APIResponse createPrePaymentTransaction(
	            @RequestBody List<ProductPrePaymentDto> productPrePaymentDTOList,
	            @ModelAttribute(Constants.LOGIN_INFO_OBJECT_NAME) ActiveUser userInfo) {
	        return new APIResponse(
	                productPaymentStatusService.addPrePaymentList(productPrePaymentDTOList, userInfo));
	    }
}
