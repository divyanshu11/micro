package com.proptiger.app.config.security;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import com.proptiger.core.constants.ResponseCodes;
import com.proptiger.core.constants.ResponseErrorMessages;
import com.proptiger.core.handler.ResponseErrorWriter;
import com.proptiger.core.util.IPUtils;

/**
* The Entry Point will not redirect to any sort of Login - it will return the
* 401, in case of protected url called without login.
* 
*/
public class AuthEntryPoint extends LoginUrlAuthenticationEntryPoint{

	 public AuthEntryPoint(String loginUrl){
	        super(loginUrl);
	    }
	    
	    @Override
	    public void commence(
	            final HttpServletRequest request,
	            final HttpServletResponse response,
	            final AuthenticationException authException) throws IOException {
	        String userIpAddress = IPUtils.getClientIP(request);
	        ResponseErrorWriter.writeErrorToResponse(
	                request,
	                response,
	                ResponseCodes.UNAUTHORIZED,
	                ResponseErrorMessages.User.AUTHENTICATION_ERROR,
	                userIpAddress, authException);
	    }
}
