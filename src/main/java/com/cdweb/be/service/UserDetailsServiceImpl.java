package com.cdweb.be.service;

import com.cdweb.be.entity.User;
import com.cdweb.be.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

  @Autowired private UserRepository userRepository;

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
    User user =
        userRepository
            .findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
            .orElseThrow(
                () ->
                    new UsernameNotFoundException(
                        "User not found with username or email: " + usernameOrEmail));

    List<GrantedAuthority> authorities = new ArrayList<>();
    if (user.getRoles() != null) {
      user.getRoles()
          .forEach(
              role -> {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
                if (role.getPermissions() != null) {
                  role.getPermissions()
                      .forEach(
                          permission -> {
                            authorities.add(new SimpleGrantedAuthority(permission.getCode()));
                          });
                }
              });
    }

    return org.springframework.security.core.userdetails.User.builder()
        .username(user.getUsername())
        .password(user.getPassword())
        .authorities(authorities)
        .accountExpired(false)
        .accountLocked(!user.getIsActive())
        .credentialsExpired(false)
        .disabled(!user.getIsActive())
        .build();
  }
}
