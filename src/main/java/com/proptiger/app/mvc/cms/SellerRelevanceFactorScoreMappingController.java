package com.proptiger.app.mvc.cms;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.proptiger.app.services.srf.SellerRelevanceFactorScoreMappingService;
import com.proptiger.core.pojo.response.APIResponse;
/*
 * Divyanshu
 */
@Controller
public class SellerRelevanceFactorScoreMappingController {

	@Autowired 
	SellerRelevanceFactorScoreMappingService sellerRelevanceFactorScoreMappingService;
	
	@ResponseBody
	@RequestMapping(value="data/v1/seller-relevance-score-dto",method=RequestMethod.GET)
	public APIResponse getSellerIdToSellerRelevanceScoreDTOMapV2(
			@RequestParam Collection<Integer> sellerIdsWOSellerRelevanceScore,
			@RequestParam Integer domainId)
	{
		return new APIResponse(sellerRelevanceFactorScoreMappingService.getSellerIdToSellerRelevanceScoreDTOMapV2(sellerIdsWOSellerRelevanceScore, domainId));
	}
}
