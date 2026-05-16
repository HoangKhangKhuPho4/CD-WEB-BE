package com.cdweb.be.service;

import com.cdweb.be.dto.AddressDto;
import com.cdweb.be.entity.User;
import com.cdweb.be.entity.UserAddress;
import com.cdweb.be.exception.BadRequestException;
import com.cdweb.be.exception.ResourceNotFoundException;
import com.cdweb.be.repository.AddressRepository;
import com.cdweb.be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AddressService {

  private static final int MAX_ADDRESSES_PER_USER = 10;

  private final AddressRepository addressRepository;

  private final UserRepository userRepository;

  // ─────────────────────────────────────────────────────────────────────────
  // GET all addresses of the current user
  // ─────────────────────────────────────────────────────────────────────────
  @Transactional(readOnly = true)
  public List<AddressDto.Response> getAddressesByUsername(String username) {
    User user = findUser(username);
    return addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(user.getId()).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
  }

  // ─────────────────────────────────────────────────────────────────────────
  // CREATE a new address
  // ─────────────────────────────────────────────────────────────────────────
  public AddressDto.Response createAddress(String username, AddressDto.Request request) {
    User user = findUser(username);

    long count = addressRepository.countByUserId(user.getId());
    if (count >= MAX_ADDRESSES_PER_USER) {
      throw new BadRequestException(
              "You can only save up to " + MAX_ADDRESSES_PER_USER + " addresses");
    }

    UserAddress address = new UserAddress();
    address.setUser(user);
    mapRequestToEntity(request, address);

    // First address is always default regardless of request flag
    if (count == 0) {
      address.setIsDefault(true);
    } else if (Boolean.TRUE.equals(request.getIsDefault())) {
      // Unset other defaults before saving
      address.setIsDefault(true);
    }

    UserAddress saved = addressRepository.save(address);

    // Unset other defaults AFTER getting the saved id
    if (Boolean.TRUE.equals(saved.getIsDefault())) {
      addressRepository.unsetDefaultForUser(user.getId(), saved.getId());
    }

    return toResponse(saved);
  }

  // ─────────────────────────────────────────────────────────────────────────
  // UPDATE an existing address
  // ─────────────────────────────────────────────────────────────────────────
  // ĐÃ SỬA: Integer addressId -> Long addressId
  public AddressDto.Response updateAddress(
          String username, Long addressId, AddressDto.Request request) {
    User user = findUser(username);
    UserAddress address =
            addressRepository
                    .findByIdAndUserId(addressId, user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));

    mapRequestToEntity(request, address);

    UserAddress saved = addressRepository.save(address);

    if (Boolean.TRUE.equals(saved.getIsDefault())) {
      addressRepository.unsetDefaultForUser(user.getId(), saved.getId());
    }

    return toResponse(saved);
  }

  // ─────────────────────────────────────────────────────────────────────────
  // DELETE an address
  // ─────────────────────────────────────────────────────────────────────────
  // ĐÃ SỬA: Integer addressId -> Long addressId
  public void deleteAddress(String username, Long addressId) {
    User user = findUser(username);
    UserAddress address =
            addressRepository
                    .findByIdAndUserId(addressId, user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));

    boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
    addressRepository.delete(address);

    // If deleted address was default, promote the latest remaining address to default
    if (wasDefault) {
      List<UserAddress> remaining =
              addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(user.getId());
      if (!remaining.isEmpty()) {
        UserAddress newDefault = remaining.get(0);
        newDefault.setIsDefault(true);
        addressRepository.save(newDefault);
      }
    }
  }

  // ─────────────────────────────────────────────────────────────────────────
  // SET DEFAULT address
  // ─────────────────────────────────────────────────────────────────────────
  // ĐÃ SỬA: Integer addressId -> Long addressId
  public AddressDto.Response setDefaultAddress(String username, Long addressId) {
    User user = findUser(username);
    UserAddress address =
            addressRepository
                    .findByIdAndUserId(addressId, user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));

    address.setIsDefault(true);
    UserAddress saved = addressRepository.save(address);

    addressRepository.unsetDefaultForUser(user.getId(), saved.getId());

    return toResponse(saved);
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Helpers
  // ─────────────────────────────────────────────────────────────────────────
  private User findUser(String username) {
    return userRepository
            .findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
  }

  private void mapRequestToEntity(AddressDto.Request request, UserAddress address) {
    address.setReceiverName(request.getReceiverName());
    address.setPhone(request.getPhone());
    address.setProvince(request.getProvince());
    address.setDistrict(request.getDistrict());
    address.setWard(request.getWard());
    address.setAddressDetail(request.getAddressDetail());
    address.setIsDefault(Boolean.TRUE.equals(request.getIsDefault()));
    address.setLabel(request.getLabel());
  }

  private AddressDto.Response toResponse(UserAddress a) {
    AddressDto.Response r = new AddressDto.Response();
    r.setId(a.getId()); // Đảm bảo DTO của bạn cũng dùng Long cho id nhé
    r.setReceiverName(a.getReceiverName());
    r.setPhone(a.getPhone());
    r.setProvince(a.getProvince());
    r.setDistrict(a.getDistrict());
    r.setWard(a.getWard());
    r.setAddressDetail(a.getAddressDetail());
    r.setIsDefault(a.getIsDefault());
    r.setLabel(a.getLabel());
    r.setCreatedAt(a.getCreatedAt());
    r.setUpdatedAt(a.getUpdatedAt());
    return r;
  }
}