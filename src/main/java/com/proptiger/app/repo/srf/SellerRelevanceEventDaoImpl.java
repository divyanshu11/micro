package com.proptiger.app.repo.srf;

import java.util.List;

import javax.persistence.EntityManagerFactory;

import org.springframework.beans.factory.annotation.Autowired;

import com.proptiger.app.model.srf.SellerRelevanceEvent;
import com.proptiger.core.pojo.FIQLSelector;
import com.proptiger.core.pojo.response.PaginatedResponse;
import com.proptiger.core.repo.FIQLDao;


public class SellerRelevanceEventDaoImpl {
	@Autowired
    public EntityManagerFactory emf;

    @Autowired
    private FIQLDao             fIQLDao;

    public PaginatedResponse<List<SellerRelevanceEvent>> getSellerRelevanceEventsBySelector(FIQLSelector selector) {
        return fIQLDao.getEntitiesFromDb(selector, emf, SellerRelevanceEvent.class);
    }

}
