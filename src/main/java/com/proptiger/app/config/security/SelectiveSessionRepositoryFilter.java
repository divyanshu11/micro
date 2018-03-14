package com.proptiger.app.config.security;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.session.ExpiringSession;
import org.springframework.session.SessionRepository;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.common.reflect.ClassPath;
import com.proptiger.core.annotations.SkipSessionLoading;
import com.proptiger.core.exception.ProAPIException;

public class SelectiveSessionRepositoryFilter<S extends ExpiringSession> extends SessionRepositoryFilter<S> {
	
	private static final String              PACKAGES_TO_SCAN               = "com.proptiger.";
    private static Map<String, List<String>> urlsToSkip                     = new HashMap<String,List<String>>();
    private static final String              PATHVARIABLE_REGEX             = "\\{[a-zA-Z-]+\\}|\\{[a-zA-Z-]+:\\[\\\\[a-z]{1}\\]\\+\\}";
    private static final String              PATHVARIABLE_REQUEST_URL_REGEX = "[0-9a-zA-Z]*";
    private static final String              SLASH                          = "/";
    private static final String              SLASH_REGEX_AT_BEGINNING       = "^/+";
    private static final String              SLASH_REGEX_AT_END             = "/+$";
    private static final String              MULTIPLE_SLASH_REGEX           = "//+";
    private static final String              EMPTY_STRING                   = "";

    private static Logger                    logger                         = LoggerFactory
                                                                                    .getLogger("SelectiveSessionRepositoryFilter");

    static {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        try {
            for (final ClassPath.ClassInfo info : ClassPath.from(classLoader).getTopLevelClasses()) {
                if (info.getName().startsWith(PACKAGES_TO_SCAN)) {
                    final Class<?> clazz = info.load();
                    if (clazz.isAnnotationPresent(Controller.class)) {
                        boolean isClassSkipSessionAnnotated = clazz.isAnnotationPresent(SkipSessionLoading.class);
                        String controllerRequestPath = "";
                        if (clazz.isAnnotationPresent(RequestMapping.class)) {
                            String[] controllerRequestPaths = clazz.getAnnotationsByType(RequestMapping.class)[0]
                                    .value();
                            if (controllerRequestPaths.length > 0) {
                                if (controllerRequestPaths.length > 1) {
                                    throw new ProAPIException(
                                            "More than one request paths defined on controller request mapping");
                                }
                                else {
                                    controllerRequestPath = controllerRequestPaths[0];
                                }
                            }
                        }

                        for (Method method : clazz.getMethods()) {
                            if ((isClassSkipSessionAnnotated || method.isAnnotationPresent(SkipSessionLoading.class)) && method
                                    .isAnnotationPresent(RequestMapping.class)) {
                                String[] urls = method.getAnnotationsByType(RequestMapping.class)[0].value();
                                List<RequestMethod> requestMethods = new ArrayList<RequestMethod>();
                                requestMethods
                                        .addAll(Arrays.asList(method.getAnnotationsByType(RequestMapping.class)[0]
                                                .method()));

                                if (requestMethods.isEmpty()) {
                                    requestMethods.add(RequestMethod.GET);
                                }

                                List<String> stringRequestMethods = requestMethods.stream()
                                        .map(RequestMethod::toString).collect(Collectors.toList());

                                for (String url : urls) {
                                    url = getFinalRequestRegexPath(controllerRequestPath, url);
                                    urlsToSkip.put(url, stringRequestMethods);
                                }
                            }
                        }

                    }
                }
            }
        }
        catch (Exception e) {
            logger.error("Exception in creating urs list for skipping session: {}", e);
            throw new ProAPIException(e);
        }
    }

    /**
     * returns string regex to be matched from request path depending on
     * {@link RequestMapping} annotations present on controller and method
     * 
     * assumes a non empty methodRequestPath
     * 
     * @param controllerRequestPath
     * @param methodRequestPath
     * @return
     */
    private static String getFinalRequestRegexPath(String controllerRequestPath, String methodRequestPath) {
        controllerRequestPath = controllerRequestPath.replaceAll(SLASH_REGEX_AT_BEGINNING, EMPTY_STRING).replaceAll(
                SLASH_REGEX_AT_END,
                EMPTY_STRING);
        methodRequestPath = methodRequestPath.replaceAll(SLASH_REGEX_AT_BEGINNING, EMPTY_STRING).replaceAll(
                SLASH_REGEX_AT_END,
                EMPTY_STRING);
        StringBuilder builder = new StringBuilder();
        builder.append(SLASH);

        if (!controllerRequestPath.isEmpty()) {
            builder.append(controllerRequestPath);
            builder.append(SLASH);
        }
        builder.append(methodRequestPath);
        String url = builder.toString().replaceAll(PATHVARIABLE_REGEX, PATHVARIABLE_REQUEST_URL_REGEX);
        logger.debug("Final regex url for skipping session = {}", url);
        return url;
    }

    public SelectiveSessionRepositoryFilter(SessionRepository<S> sessionRepository) {
        super(sessionRepository);
    }

    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        path = path.replaceAll(MULTIPLE_SLASH_REGEX, SLASH).replaceAll(SLASH_REGEX_AT_END, EMPTY_STRING);
        String requestMethod = request.getMethod();
        logger.debug("Checking if session loading is needed for path {} and method {}", path, requestMethod);

        for (String url : urlsToSkip.keySet()) {
            if (path.matches(url) && urlsToSkip.get(url).contains(requestMethod)) {
                logger.debug("Skipping session loading for path {} and method {}", path, requestMethod);
                filterChain.doFilter(request, response);
                return;
            }
        }
        logger.debug("Loading session for path {} and method {}", path, requestMethod);
        super.doFilterInternal(request, response, filterChain);
    }

}
