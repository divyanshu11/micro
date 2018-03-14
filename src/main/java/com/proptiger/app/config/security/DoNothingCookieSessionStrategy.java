package com.proptiger.app.config.security;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.session.Session;
import org.springframework.session.web.http.HttpSessionStrategy;

import com.proptiger.core.util.Constants;

public class DoNothingCookieSessionStrategy implements HttpSessionStrategy{
	@Override
    public String getRequestedSessionId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(Constants.JSESSIONID)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    @Override
    public void onNewSession(Session session, HttpServletRequest request, HttpServletResponse response) {
        
    }

    @Override
    public void onInvalidateSession(HttpServletRequest request, HttpServletResponse response) {
        
    }

}
