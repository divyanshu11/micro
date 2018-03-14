package com.proptiger.app.config.security;

import java.net.URI;
import java.util.Date;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.MapSession;
import org.springframework.session.SessionRepository;

import com.google.gson.reflect.TypeToken;
import com.proptiger.core.dto.internal.ActiveUser;
import com.proptiger.core.dto.internal.ActiveUserCopy;
import com.proptiger.core.util.Constants;
import com.proptiger.core.util.CorePropertyKeys;
import com.proptiger.core.util.MadroxRequestCommandUtil;
import com.proptiger.core.util.PropertyReader;

/**
 * A session repository that fetch session information from user service module
 * 
 * 
 */
public class UserServiceSessionRepository implements SessionRepository<MapSession>{
	
	private static final String      ANONYMOUS_USER             = "anonymousUser";
    private static Logger            logger                     =
            LoggerFactory.getLogger(UserServiceSessionRepository.class);
    @Autowired
    private MadroxRequestCommandUtil madroxRequestCommandUtil;

    @Value("${internal.api.userservice}")
    private String                   userServiceModuleInternalApiHost;

    private String                   key                        = UUID.randomUUID().toString();

    private static final String      URL_DATA_V1_ENTITY_SESSION = "data/v1/entity/session";

    @Override
    public MapSession createSession() {
        return new MapSession();
    }

    @Override
    public void save(MapSession session) {
        // do nothing
    }

    @Override
    public MapSession getSession(String jsessionId) {
        if (jsessionId != null && !jsessionId.isEmpty()) {
            HttpHeaders header = new HttpHeaders();
            header.add("Cookie", Constants.Security.COOKIE_NAME_JSESSIONID + "=" + jsessionId);
            String stringUrl =
                    new StringBuilder(userServiceModuleInternalApiHost).append(URL_DATA_V1_ENTITY_SESSION).toString();
            try {
                ActiveUserCopy activeUserCopy = madroxRequestCommandUtil
                        .createMadroxGetCommand(URI.create(stringUrl), header, new TypeToken<ActiveUserCopy>() {})
                        .execute();
                if (activeUserCopy != null) {
                    ActiveUser activeUser = activeUserCopy.toActiveUser();
                    return createSessionForActiveUser(activeUser, jsessionId);
                }
                else {
                    /*
                     * return anonymous session
                     */
                    return createAnonymousSession(jsessionId);
                }
            }
            catch (Exception e) {
                logger.error(
                        "Error {} while getting session info from user service for {}",
                        e.getMessage(),
                        jsessionId,
                        e);
                /*
                 * return anonymous session
                 */
                return createAnonymousSession(jsessionId);
            }
        }
        return null;
    }

    private MapSession createAnonymousSession(String jsessionId) {
        AnonymousAuthenticationToken auth = new AnonymousAuthenticationToken(
                key,
                ANONYMOUS_USER,
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
        MapSession loaded = new MapSession();
        loaded.setId(jsessionId);
        loaded.setCreationTime(new Date().getTime());
        loaded.setLastAccessedTime(new Date().getTime());
        loaded.setMaxInactiveInterval(
                PropertyReader
                        .getRequiredPropertyAsType(CorePropertyKeys.SESSION_MAX_INTERACTIVE_INTERVAL, Integer.class));
        loaded.setAttribute(Constants.LOGIN_INFO_OBJECT_NAME, ANONYMOUS_USER);
        SecurityContext securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(auth);
        loaded.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        return loaded;
    }

    private MapSession createSessionForActiveUser(ActiveUser activeUser, String sessionId) {
        MapSession loaded = new MapSession();
        loaded.setId(sessionId);
        loaded.setCreationTime(new Date().getTime());
        loaded.setLastAccessedTime(new Date().getTime());
        loaded.setMaxInactiveInterval(
                PropertyReader
                        .getRequiredPropertyAsType(CorePropertyKeys.SESSION_MAX_INTERACTIVE_INTERVAL, Integer.class));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(activeUser, null, activeUser.getAuthorities());
        loaded.setAttribute(Constants.LOGIN_INFO_OBJECT_NAME, activeUser);
        SecurityContext securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(auth);
        loaded.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        return loaded;
    }

    @Override
    public void delete(String id) {
        // do nothing
    }


}
