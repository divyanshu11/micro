package com.proptiger.app.repo.order;

import java.util.List;

import com.proptiger.core.model.transaction.ProductPaymentStatus;
import com.proptiger.core.pojo.FIQLSelector;
import com.proptiger.core.pojo.response.PaginatedResponse;

public interface ProductPaymentStatusDaoCustom {

	public PaginatedResponse<List<ProductPaymentStatus>> getProductPaymentStatusBySelector(FIQLSelector selector);
}
