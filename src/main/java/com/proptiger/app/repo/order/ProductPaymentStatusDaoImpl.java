package com.proptiger.app.repo.order;

import java.util.List;

import javax.persistence.EntityManagerFactory;

import org.springframework.beans.factory.annotation.Autowired;

import com.proptiger.core.model.transaction.ProductPaymentStatus;
import com.proptiger.core.pojo.FIQLSelector;
import com.proptiger.core.pojo.response.PaginatedResponse;
import com.proptiger.core.repo.FIQLDao;

public class ProductPaymentStatusDaoImpl {

	@Autowired
    private EntityManagerFactory emf;

    @Autowired
    private FIQLDao              fIQLDao;

    /**
     * 
     * @param selector
     * @return
     */
    public PaginatedResponse<List<ProductPaymentStatus>> getProductPaymentStatusBySelector(FIQLSelector selector) {
        return fIQLDao.getEntitiesFromDb(selector, emf, ProductPaymentStatus.class);
    }
}
