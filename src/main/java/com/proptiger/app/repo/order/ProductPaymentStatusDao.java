package com.proptiger.app.repo.order;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

//import com.proptiger.app.service.order.ProductPaymentStatusCustomDao;
import com.proptiger.core.model.transaction.ProductPaymentStatus;
//import com.proptiger.app.model.transaction.ProductPaymentStatus;
//import com.proptiger.data.repo.order.ProductPaymentStatus;
//import com.proptiger.data.repo.order.ProductPaymentStatusCustomDao;

public interface ProductPaymentStatusDao 
extends JpaRepository<ProductPaymentStatus, Integer>, ProductPaymentStatusDaoCustom
{
	 public ProductPaymentStatus findById(Integer id);

	    public List<ProductPaymentStatus> findByIdIn(Collection<Integer> ids);

	    public ProductPaymentStatus findByProductTypeIdAndProductId(Integer productTypeId, Integer productId);

	    @Modifying
	    @Transactional
	    @Query(value = "delete from ProductPaymentStatus where productTypeId=?1 and productId=?2")
	    public void deleteByProductTypeIdAndProductId(int productTypeId, Integer productId);

	    @Modifying
	    @Transactional
	    @Query(value = "update ProductPaymentStatus PPS set PPS.productId = NULL WHERE PPS.id IN (?1)")
	    public void setProductIdNull(List<Integer> productPaymentIdList);

	    @Modifying
	    @Transactional
	    @Query("UPDATE ProductPaymentStatus PPS set PPS.amount=?5 where PPS.crmUserId=?1 and PPS.saleTypeId=?2 and PPS.statusId=1 and PPS.productTypeId=?3 and PPS.createdAt <=?4 ")
	    public Integer updateAmountForSellerSaleType(
	            Integer sellerId,
	            Integer saleTypeId,
	            Integer productTypeId,
	            Date createdDateLessThan,
	            Integer amount);
}
