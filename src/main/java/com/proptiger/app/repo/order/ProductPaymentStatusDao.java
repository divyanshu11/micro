package com.proptiger.app.repo.order;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManagerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.proptiger.core.dto.order.LeadPaymentStatusDto;
import com.proptiger.core.model.transaction.LeadPaymentStatus;
//import com.proptiger.app.service.order.ProductPaymentStatusCustomDao;
import com.proptiger.core.model.transaction.ProductPaymentStatus;
import com.proptiger.core.pojo.FIQLSelector;
import com.proptiger.core.pojo.response.PaginatedResponse;
import com.proptiger.core.repo.FIQLDao;
//import com.proptiger.app.model.transaction.ProductPaymentStatus;
//import com.proptiger.data.repo.order.ProductPaymentStatus;
//import com.proptiger.data.repo.order.ProductPaymentStatusCustomDao;
import com.proptiger.core.util.Constants;

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
	    //@Divyanshu

		/**
	     * 
	     * @param userId
	     * @return
	     */
	    @Query(
	            value = "select statusId, saleTypeId, count(*) from ProductPaymentStatus where crmUserId = ?1 and productId IS NOT NULL group by statusId, saleTypeId")
	    public List<Object[]> getLeadDistribution(int userId);
	    /**
	     * 
	     * @param userIds
	     * @return
	     */
	    @Query(
	            value = "select crmUserId, count(*) from ProductPaymentStatus where statusId = 3 and saleTypeId in (1,2) and crmUserId in (?1) and productId IS NOT NULL group by crmUserId")
	    public List<Object[]> getLeadCounts(List<Integer> userIds);

	    @Query(
	            value = "select crmUserId, count(*) from ProductPaymentStatus where statusId = 3 and saleTypeId in (1,2) and crmUserId in (?1) group by crmUserId")
	    public List<Object[]> getPrepaidAndPostPaidLeadCounts(Set<Integer> sellerIds);
	    
	    /**
	     * @param leadTypeId
	     * @return
	     */
	    @Cacheable(value = Constants.CacheName.SELLER_PAYMENT_COUNT)
	    @Query(
	            value = "select new com.proptiger.core.dto.order.LeadPaymentStatusDto(lps.crmUserId,max(p.updatedAt)) from ProductPaymentStatus pps join pps.payment p where pps.statusId=3 and pps.saleTypeId = ?1 group by pps.crmUserId")
	    public List<LeadPaymentStatusDto> findPaidSellersWithLastPaymentDateForLeadType(int leadTypeId);
	    /**
	     * 
	     * @param leadId(ProductId)
	     * @return
	     */
	    public List<ProductPaymentStatus> findByProductId(Integer productId);
}
