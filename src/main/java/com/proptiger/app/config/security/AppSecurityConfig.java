package com.proptiger.app.config.security;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.session.ExpiringSession;
import org.springframework.session.SessionRepository;
import org.springframework.session.web.http.SessionRepositoryFilter;

import com.proptiger.api.filter.CustomAnonymousAuthenticationFilter;
import com.proptiger.core.config.CustomAccessDeniedHandler;
import com.proptiger.core.enums.security.UserRole;
import com.proptiger.core.util.Constants;

@Configuration
@EnableWebSecurity
@ComponentScan(basePackages = { "com.proptiger" })
@Order
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class AppSecurityConfig<S extends ExpiringSession> extends WebSecurityConfigurerAdapter {
	 @Autowired
	    private SessionRepository<S> sessionRepository;

	    @Override
	    protected void configure(HttpSecurity http) throws Exception {
	        http.addFilterBefore(createSessionRepositoryFilter(), ChannelProcessingFilter.class);
	        http.csrf().disable();

	        http.anonymous().authenticationFilter(getAnonymousAuthenticationFilter());

	        http.authorizeRequests().regexMatchers(Constants.Security.USER_API_REGEX)
	                .access("hasRole('" + UserRole.USER.name() + "')").regexMatchers(Constants.Security.USER_API_REGEX)
	                .access("hasRole('" + UserRole.Admin.name() + "')")
	                .regexMatchers(Constants.Security.MARKETFORCE_API_REGEX)
	                .access("hasRole('" + UserRole.USER.name() + "') or hasRole('" + UserRole.Admin.name() + "')")
	                .regexMatchers(Constants.Security.MARKETFORCE_CRON_API_REGEX)
	                .access("hasRole('" + UserRole.InternalIP + "')")
	                .regexMatchers(Constants.Security.ANONYMOUS_USER_API_REGEX)
	                .access("hasRole('" + UserRole.AnonymousEnabled.name() + "')").anyRequest().permitAll();

	        http.exceptionHandling().authenticationEntryPoint(createAuthEntryPoint());

	        http.exceptionHandling().accessDeniedHandler(createAccessDeniedHandler());
	    }

	    @Bean
	    public AuthenticationManager createAuthenticationManager() {
	        return new AuthenticationManager() {

				public Authentication authenticate(Authentication authentication) throws AuthenticationException {
					// TODO Auto-generated method stub
					return null;
				}
	            
	        };
	    }

	    @Bean
	    public AnonymousAuthenticationFilter getAnonymousAuthenticationFilter() {
	        return new CustomAnonymousAuthenticationFilter(UUID.randomUUID().toString());
	    }

	    /**
	     * Create session repository filter, that will create session object instead
	     * of container
	     * 
	     * @return
	     */
	    @Bean(name = "springSessionRepositoryFilter")
	    public SessionRepositoryFilter<? extends ExpiringSession> createSessionRepositoryFilter() {
	        final SelectiveSessionRepositoryFilter<S> sessionRepositoryFilter = new SelectiveSessionRepositoryFilter<S>(
	                sessionRepository);
	        DoNothingCookieSessionStrategy httpSessionStrategy = new DoNothingCookieSessionStrategy();
	        sessionRepositoryFilter.setHttpSessionStrategy(httpSessionStrategy);
	        return sessionRepositoryFilter;
	    }

	    // @Bean
	    public UserServiceSessionRepository createSessionRepository() {
	        return new UserServiceSessionRepository();
	    }

	    @Bean
	    public AccessDeniedHandler createAccessDeniedHandler() {
	        AccessDeniedHandler accessDeniedHandler = new CustomAccessDeniedHandler();
	        return accessDeniedHandler;
	    }

	    @Bean
	    public LogoutHandler createLogoutHandler() {
	        return new CookieClearingLogoutHandler(
	                Constants.Security.COOKIE_NAME_JSESSIONID,
	                Constants.Security.REMEMBER_ME_COOKIE);
	    }

	    @Bean
	    public LoginUrlAuthenticationEntryPoint createAuthEntryPoint() {
	        AuthEntryPoint authEntryPoint = new AuthEntryPoint(Constants.Security.LOGIN_URL);
	        return authEntryPoint;
	    }

	    @Bean
	    public SessionRegistry createSessionRegistry() {
	        return new SessionRegistryImpl();
	    }

	    @Bean
	    public Md5PasswordEncoder createPasswordEncoder() {
	        return new Md5PasswordEncoder();
	    }

}
