package com.peopleapp.security;

import com.peopleapp.model.PeopleUser;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Map;

public class SpringSecurityAuthToken extends AbstractAuthenticationToken {

    private static final long serialVersionUID = 3682973059583497018L;
    private transient Map<String, String> token;
    private transient PeopleUser peopleUser;

    public SpringSecurityAuthToken(Collection<? extends GrantedAuthority> authorities, Map<String, String> token) {
        super(authorities);
        this.token = token;
        setAuthenticated(false);
    }

    public SpringSecurityAuthToken(SpringSecurityAuthToken partialToken, PeopleUser peopleUser) {
        super(partialToken.getAuthorities());
        this.token = partialToken.token;
        this.peopleUser = peopleUser;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public Object getPrincipal() {
        return peopleUser;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

}
