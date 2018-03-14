package com.proptiger.app.model.relevance;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.proptiger.core.exception.ProAPIException;
import com.proptiger.core.model.cms.SellerRelevanceFactors;

@Component
public class LevelFactory extends BaseLevelFactory{

	  @Autowired
	    private ApplicationContext                   applicationContext;

	    @Override
	    public BaseLevel getLevel(String level, SellerRelevanceFactors sellerRelevanceFactors) {
	        BaseLevel baseLevel = null;

	        switch (level) {

	            case "LEVEL_1":
	                baseLevel = applicationContext.getBean(Level1.class);
	                break;
	            case "LEVEL_2":
	                baseLevel = applicationContext.getBean(Level2.class);
	                break;
	            case "LEVEL_3":
	                baseLevel = applicationContext.getBean(Level3.class);
	                break;
	            default:
	                throw new ProAPIException("Invalid level {}", level);
	        }
	        return baseLevel;
	    }

}
