package com.auth.module.security;

import com.auth.module.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class UserPrincipal implements OAuth2User, UserDetails {

    private Long id;
    private String email;
    private String password;
    private boolean enabled;
    private boolean accountNonLocked;
    private Collection<? extends GrantedAuthority> authorities;
    private Map<String, Object> attributes;

    public UserPrincipal(Long id, String email, String password, boolean enabled,
                        boolean accountNonLocked, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.enabled = enabled;
        this.accountNonLocked = accountNonLocked;
        this.authorities = authorities;
    }

    public static UserPrincipal create(User user) {
        Collection<GrantedAuthority> authorities = user.getRoles().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getEnabled() != null ? user.getEnabled() : true,  // Default to true if null
                user.getAccountLocked() != null ? !user.getAccountLocked() : true,  // accountNonLocked is inverse of accountLocked
                authorities
        );
    }

    public static UserPrincipal create(User user, Map<String, Object> attributes) {
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        userPrincipal.setAttributes(attributes);
        return userPrincipal;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getName() {
        return String.valueOf(id);
    }
}
