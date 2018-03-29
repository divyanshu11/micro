package com.proptiger.app.mvc;


import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.security.authentication.server.PseudoAuthenticationHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.proptiger.core.helper.MicroServiceHelper;
import com.proptiger.core.model.transaction.ProductPaymentStatus;

@Service
public class Testing {
	@Autowired
	MicroServiceHelper microServiceHelper;
	

	//List<ProductPaymentStatus> productPaymentStatus = microServiceHelper.updateProductPaymentStatus(productPaymentStatuses, 3547236)
}
