package com.cdweb.be.service;

import com.cdweb.be.dto.UserDto;
import com.cdweb.be.entity.Role;
import com.cdweb.be.entity.User;
import com.cdweb.be.exception.BadRequestException;
import com.cdweb.be.exception.ResourceNotFoundException;
import com.cdweb.be.repository.RoleRepository;
import com.cdweb.be.repository.UserRepository;
import java.util.Collections;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

  @Autowired private UserRepository userRepository;

  @Autowired private RoleRepository roleRepository;

  @Autowired private ModelMapper modelMapper;

  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private AuditLogService auditLogService;

  public Page<UserDto.Response> getAllUsers(Pageable pageable) {
    return userRepository
        .findAllActive(pageable)
        .map(user -> modelMapper.map(user, UserDto.Response.class));
  }

  public Page<UserDto.Response> getAllUsersForAdmin(String keyword, Pageable pageable) {
    Page<User> usersPage;
    if (keyword != null && !keyword.trim().isEmpty()) {
      usersPage = userRepository.searchUsers(keyword.trim(), pageable);
    } else {
      usersPage = userRepository.findAll(pageable);
    }
    return usersPage.map(user -> modelMapper.map(user, UserDto.Response.class));
  }

  public UserDto.Response getUserById(Integer id) {
    User user =
        userRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    return modelMapper.map(user, UserDto.Response.class);
  }

  public UserDto.Response getUserByUsername(String username) {
    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    return modelMapper.map(user, UserDto.Response.class);
  }

  public UserDto.Response createUser(UserDto.CreateRequest request) {
    if (userRepository.existsByUsername(request.getUsername())) {
      throw new BadRequestException("Username is already taken!");
    }
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new BadRequestException("Email Address already in use!");
    }

    User user = new User();
    user.setUsername(request.getUsername());
    user.setEmail(request.getEmail());
    user.setPassword(passwordEncoder.encode(request.getPassword()));
    user.setName(request.getName());
    user.setPhone(request.getPhone());
    user.setBirth(request.getBirth());
    user.setGender(request.getGender());
    user.setStatus(1); // Active by default

    Integer roleId =
        request.getRoleId() != null ? request.getRoleId() : 4; // Default to CUSTOMER/USER
    Role userRole =
        roleRepository
            .findById(roleId)
            .orElseThrow(() -> new BadRequestException("Role not found"));
    user.setRoles(Collections.singleton(userRole));

    User savedUser = userRepository.save(user);
    auditLogService.log(
        "CREATE_USER",
        "User",
        savedUser.getId().toString(),
        "Created user: " + savedUser.getUsername());
    return modelMapper.map(savedUser, UserDto.Response.class);
  }

  public UserDto.Response updateUser(Integer id, UserDto.UpdateRequest updateRequest) {
    User user =
        userRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

    if (updateRequest.getName() != null) {
      user.setName(updateRequest.getName());
    }
    if (updateRequest.getPhone() != null) {
      user.setPhone(updateRequest.getPhone());
    }
    if (updateRequest.getBirth() != null) {
      user.setBirth(updateRequest.getBirth());
    }
    if (updateRequest.getGender() != null) {
      user.setGender(updateRequest.getGender());
    }

    User updatedUser = userRepository.save(user);
    auditLogService.log("UPDATE_USER", "User", updatedUser.getId().toString(), "Updated user info");
    return modelMapper.map(updatedUser, UserDto.Response.class);
  }

  public void deleteUser(Integer id) {
    User user =
        userRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    // Soft delete: set status to 0
    user.setStatus(0);
    userRepository.save(user);
    auditLogService.log("DELETE_USER", "User", id.toString(), "Soft deleted user");
  }

  public void activateUser(Integer id) {
    User user =
        userRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    user.setStatus(1);
    userRepository.save(user);
  }

  public void deactivateUser(Integer id) {
    User user =
        userRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    user.setStatus(0);
    userRepository.save(user);
  }

  public UserDto.Response toggleStatus(Integer id) {
    User user =
        userRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    user.setStatus(user.getStatus() == 1 ? 0 : 1);
    User updatedUser = userRepository.save(user);
    auditLogService.log(
        "TOGGLE_STATUS", "User", id.toString(), "New status: " + updatedUser.getStatus());
    return modelMapper.map(updatedUser, UserDto.Response.class);
  }

  public Page<UserDto.Response> searchUsers(String keyword, Pageable pageable) {
    return userRepository
        .searchUsers(keyword, pageable)
        .map(user -> modelMapper.map(user, UserDto.Response.class));
  }

  public void changePassword(String username, UserDto.ChangePasswordRequest request) {
    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

    if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
      throw new BadRequestException("Current password is incorrect");
    }

    if (!request.getNewPassword().equals(request.getConfirmPassword())) {
      throw new BadRequestException("New password and confirm password do not match");
    }

    user.setPassword(passwordEncoder.encode(request.getNewPassword()));
    userRepository.save(user);
  }
}
