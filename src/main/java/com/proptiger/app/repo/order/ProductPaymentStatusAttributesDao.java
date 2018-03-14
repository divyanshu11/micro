package com.proptiger.app.repo.order;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.proptiger.core.model.transaction.ProductPaymentStatusAttributes;

public interface ProductPaymentStatusAttributesDao extends JpaRepository<ProductPaymentStatusAttributes, Integer> {

	/**
	 * @Divyanshu
     * Moved from petra
     * @param singletonList
     * @return
     */
    @Modifying
    @Transactional
    @Query("delete from ProductPaymentStatusAttributes ppsa where ppsa.productPaymentStatusId in (?1)")
    public void deleteByProductPaymentStatusId(List<Integer> singletonList);

}
