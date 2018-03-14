package com.proptiger.app.dto.order;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.proptiger.core.model.BaseModel;

/**
 * 
 * @author swapnil
 *
 */
@JsonInclude(Include.NON_NULL)
public class LeadPaymentDto extends BaseModel {
	/**
     * 
     */
    private static final long serialVersionUID = 9069834109602318955L;
    private List<Integer> productIds;
    private int           crmUserId;

    public List<Integer> getProductIds() {
        return productIds;
    }

    public void setProductIds(List<Integer> productIds) {
        this.productIds = productIds;
    }

    public int getCrmUserId() {
        return crmUserId;
    }

    public void setCrmUserId(int crmUserId) {
        this.crmUserId = crmUserId;
    }

}
