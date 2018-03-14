package com.proptiger.app.mvc.cms;

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

import com.proptiger.app.services.srf.SellerRelevanceFactorsService;
import com.proptiger.core.annotations.InternalIp;
import com.proptiger.core.annotations.LoggedIn;
import com.proptiger.core.dto.internal.ActiveUser;
import com.proptiger.core.meta.DisableCaching;
import com.proptiger.core.model.cms.SellerRelevanceFactors;
import com.proptiger.core.model.enums.transaction.MasterLeadPaymentTypeEnum;
import com.proptiger.core.mvc.BaseController;
import com.proptiger.core.pojo.FIQLSelector;
import com.proptiger.core.pojo.response.APIResponse;
import com.proptiger.core.util.Constants;
//import com.proptiger.data.mvc.cms.SellerRelevanceFactorsService;
/*
 * This class has been copied from petra 
 * /dal/src/main/java/com/proptiger/data/mvc/cms/SellerRelevanceFactorsController.java
 */

@Controller
@DisableCaching
public class SellerRelevanceFactorsController extends BaseController{

	@Autowired
    private SellerRelevanceFactorsService sellerRelevanceFactorsService;

    /**
     * Get seller relevance factors from db using fiql
     * 
     * @param selector
     * @return
     */
    @InternalIp
    @ResponseBody
    @RequestMapping(value = "data/v1/entity/seller-relevance-factors")
    public APIResponse getSellerRelevanceFactorsOld(@ModelAttribute FIQLSelector selector) {
        return new APIResponse(sellerRelevanceFactorsService.getSellerRelevanceFactorsOld(selector));
    }

    /**
     * Get seller relevance factors from db using fiql
     * 
     * @param selector
     * @return
     */
    @InternalIp
    @ResponseBody
    @RequestMapping(value = "data/v2/entity/seller-relevance-factors")
    public APIResponse getSellerRelevanceFactors(@ModelAttribute FIQLSelector selector) {
        return new APIResponse(sellerRelevanceFactorsService.getSellerRelevanceFactors(selector));
    }

    /**
     * 
     * @param sellerUserIds
     * @param masterLeadPaymentTypeEnum
     * @return
     */
    @InternalIp
    @ResponseBody
    @RequestMapping(value = "data/v3/entity/seller-relevance-factors")
    @DisableCaching
    public APIResponse getSellerRelevanceFactors(
            @RequestParam(required = true) Set<Integer> sellerUserIds,
            @RequestParam(required = false) MasterLeadPaymentTypeEnum masterLeadPaymentTypeEnum) {
        List<SellerRelevanceFactors> lst =
                sellerRelevanceFactorsService.getSellerRelevanceFactors(sellerUserIds, masterLeadPaymentTypeEnum);
        return new APIResponse(lst);
    }

    /**
     * 
     * @param sellerUserIds
     * @param masterLeadPaymentTypeEnum
     * @return
     */
    @InternalIp
    @ResponseBody
    @RequestMapping(value = "data/v4/entity/seller-relevance-factors")
    @DisableCaching
    public APIResponse getSellerRelevanceFactorsWithPackageAndComponents(
            @RequestParam(required = true) Set<Integer> sellerUserIds,
            @RequestParam(required = false) MasterLeadPaymentTypeEnum masterLeadPaymentTypeEnum) {
        return new APIResponse(
                sellerRelevanceFactorsService
                        .getSellerRelevanceFactorsWithPackageAndComponents(sellerUserIds, masterLeadPaymentTypeEnum));
    }

    /**
     * 
     * @param sellerRelevanceFactors
     * @return
     */
    @LoggedIn(roles = {"MarketForceCRO"})
    @ResponseBody
    @RequestMapping(value = "data/v1/entity/seller/seller-relevance-factors", method = RequestMethod.POST)
    public APIResponse saveSellerRelevanceFactors(
            @RequestBody List<SellerRelevanceFactors> sellerRelevanceFactors,
            @ModelAttribute(Constants.LOGIN_INFO_OBJECT_NAME) ActiveUser userInfo) {
        sellerRelevanceFactors.forEach(srf -> srf.setUpdatedBy(userInfo.getUserIdentifier()));
        return new APIResponse(sellerRelevanceFactorsService.saveSellerRelevanceFactors(sellerRelevanceFactors));
    }

    /**
     * 
     * @param sellerRelevanceFactors
     * @return
     */
    @LoggedIn(roles = {"MarketForceCRO"})
    @ResponseBody
    @RequestMapping(value = "data/v1/entity/seller/seller-relevance-factors", method = RequestMethod.PUT)
    public APIResponse updateSellerRelevanceFactors(
            @RequestBody List<SellerRelevanceFactors> sellerRelevanceFactors,
            @ModelAttribute(Constants.LOGIN_INFO_OBJECT_NAME) ActiveUser userInfo) {
        sellerRelevanceFactors.forEach(srf -> srf.setUpdatedBy(userInfo.getUserIdentifier()));
        return new APIResponse(sellerRelevanceFactorsService.updateSellerRelevanceFactors(sellerRelevanceFactors));
    }

    /**
     * 
     * @param sellerRelevanceFactors
     * @return
     */

    @LoggedIn(roles = {"MarketForceCRO"})
    @ResponseBody
    @RequestMapping(value = "data/v1/entity/seller/seller-relevance-factors", method = RequestMethod.PATCH)
    public APIResponse patchSellerRelevanceFactors(
            @RequestBody List<SellerRelevanceFactors> sellerRelevanceFactors,
            @ModelAttribute(Constants.LOGIN_INFO_OBJECT_NAME) ActiveUser userInfo) {
        sellerRelevanceFactors.forEach(srf -> srf.setUpdatedBy(userInfo.getUserIdentifier()));
        return new APIResponse(sellerRelevanceFactorsService.patchSellerRelevanceFactors(sellerRelevanceFactors));
    }

}
