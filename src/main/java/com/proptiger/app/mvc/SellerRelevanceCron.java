package com.proptiger.app.mvc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.proptiger.app.service.cron.SellerRelevanceCronService;
import com.proptiger.core.annotations.InternalIp;
import com.proptiger.core.meta.DisableCaching;
import com.proptiger.core.pojo.response.APIResponse;
//import com.proptiger.data.mvc.SellerRelevanceCronService;

@Controller
public class SellerRelevanceCron {

	@Autowired
    private SellerRelevanceCronService sellerRelevanceCronService;

    @InternalIp
    @DisableCaching
    @ResponseBody
    @RequestMapping(value = "cron/v2/seller-relevance-factors/process", method = RequestMethod.GET)
    public APIResponse penalizeSellersV2(
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false, defaultValue = "true") boolean saveSellerInfo,
            @RequestParam(required = false) Integer sellerUserId) {

        return new APIResponse(sellerRelevanceCronService.processSellers(saveSellerInfo, sellerUserId));
    }
}
