package com.peopleapp.configuration;

import com.peopleapp.security.TokenAuthFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.cors().and().authorizeRequests()
                .anyRequest().authenticated().and()
                .addFilterBefore(new TokenAuthFilter(), BasicAuthenticationFilter.class).csrf().disable();

    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        /*
         * Permit only below listed URls to skip authentication, other API calls should
         * be authenticated
         */
        web.ignoring()
                .antMatchers(HttpMethod.GET, "/health")
                .antMatchers(HttpMethod.GET, "/version-info")
                .antMatchers(HttpMethod.POST, "/v1.0/user/api/connect")
                .antMatchers(HttpMethod.GET, "/v1.0/webview/api/email/verify/**")
                .antMatchers("/v2/api-docs", "/configuration/ui", "/swagger-resources/**", "/configuration/security",
                        "/swagger-ui.html", "/webjars/**");

        super.configure(web);
    }

}
