package com.proptiger.app.mvc;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.proptiger.app.services.srf.SellerRelevancePackageService;
import com.proptiger.core.annotations.InternalIp;
import com.proptiger.core.model.cms.SellerRelevancePackage;
import com.proptiger.core.pojo.response.APIResponse;
//import com.proptiger.data.mvc.SellerRelevancePackageService;

@Controller
public class SellerRelevancePackageController {

	 @Autowired
	    private SellerRelevancePackageService sellerRelevancePackageService;

	    @InternalIp
	    @ResponseBody
	    @RequestMapping(
	            value = "data/v1/entity/seller-relevance/{sellerRelevanceId:[\\d]+}/packages",
	            method = RequestMethod.POST)
	    public APIResponse addOrPatchSellerRelevancePackage(
	            @PathVariable int sellerRelevanceId,
	            @RequestBody List<SellerRelevancePackage> sellerRelevancePackages) {
	        return new APIResponse(
	                sellerRelevancePackageService.saveSellerRelevancePackage(sellerRelevancePackages, sellerRelevanceId));
	    }

	    @InternalIp
	    @ResponseBody
	    @RequestMapping(
	            value = "data/v1/entity/seller-relevance/{sellerRelevanceId:[\\d]+}/package/{sellerRelevancePackageId:[\\d]+}",
	            method = RequestMethod.PUT)
	    public APIResponse updateSellerRelevancePackage(
	            @PathVariable int sellerRelevanceId,
	            @PathVariable int sellerRelevancePackageId,
	            @RequestBody SellerRelevancePackage sellerRelevancePackage) {
	        return new APIResponse(
	                sellerRelevancePackageService.updateSellerRelevancePackage(
	                        sellerRelevancePackage,
	                        sellerRelevanceId,
	                        sellerRelevancePackageId));
	    }

	    @InternalIp
	    @ResponseBody
	    @RequestMapping(value = "data/v1/entity/seller-relevance-package", method = RequestMethod.DELETE)
	    public APIResponse deleteSellerPackage(@RequestParam List<Integer> sellerPackageIds) {
	        sellerRelevancePackageService.deleteSellerRelevancePackage(sellerPackageIds);
	        return new APIResponse();
	    }

	    @InternalIp
	    @ResponseBody
	    @RequestMapping(value = "data/v1/entity/seller-relevance-factors-attribute-mapping", method = RequestMethod.DELETE)
	    public APIResponse deleteSellerRelevanceFactorAttributeMapping(@RequestParam List<Integer> attributeMappingIds) {
	        sellerRelevancePackageService.deleteSellerRelevanceMapping(attributeMappingIds);
	        return new APIResponse();
	    }

	    @InternalIp
	    @ResponseBody
	    @RequestMapping(value = "data/v1/entity/seller-relevance-package/{saleTypeId:[\\d]+}", method = RequestMethod.GET)
	    public APIResponse getPackageComponentsByEntity(
	            @RequestParam(required = true) List<String> entityTypes,
	            @PathVariable int saleTypeId) {
	        return new APIResponse(
	                sellerRelevancePackageService.getPackageComponentsByEnitySaleTypeId(entityTypes, saleTypeId));
	    }
}
