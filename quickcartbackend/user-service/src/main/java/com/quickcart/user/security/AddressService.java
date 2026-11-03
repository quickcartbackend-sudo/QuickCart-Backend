package com.quickcart.user.service;

import com.quickcart.user.dto.request.AddressRequest;
import com.quickcart.user.dto.response.AddressResponse;
import com.quickcart.user.entity.Address;
import com.quickcart.user.exception.ApiException;
import com.quickcart.user.mapper.AddressMapper;
import com.quickcart.user.repository.AddressRepository;
import com.quickcart.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddressService {

    private static final int MAX_ADDRESSES = 5;

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final AddressMapper mapper;

    public AddressService(AddressRepository addressRepository, UserRepository userRepository,
                          AddressMapper mapper) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> list(long userId) {                               // U5, default first
        return addressRepository.findByUserIdOrderByDefaultAddressDescIdAsc(userId).stream()
                .map(mapper::toResponse).toList();
    }

    @Transactional
    public AddressResponse create(long userId, AddressRequest req) {               // U6
        lockUser(userId);
        long count = addressRepository.countByUserId(userId);
        if (count >= MAX_ADDRESSES) {
            throw new ApiException(HttpStatus.CONFLICT, "ADDRESS_LIMIT_REACHED",
                    "You can save at most " + MAX_ADDRESSES + " addresses.");
        }
        boolean makeDefault = count == 0 || Boolean.TRUE.equals(req.isDefault());   // first one is always default
        if (makeDefault && count > 0) {
            addressRepository.clearDefaults(userId, Instant.now());
        }
        Address saved = addressRepository.save(new Address(userId, req.label().trim(),
                req.recipientName().trim(), req.phone(), req.line1().trim(), blankToNull(req.line2()),
                req.city().trim(), req.state().trim(), req.pincode(), makeDefault));
        return mapper.toResponse(saved);
    }

    @Transactional
    public AddressResponse update(long userId, long id, AddressRequest req) {      // U7
        lockUser(userId);
        Address address = find(userId, id);
        address.update(req.label().trim(), req.recipientName().trim(), req.phone(), req.line1().trim(),
                blankToNull(req.line2()), req.city().trim(), req.state().trim(), req.pincode());
        // isDefault=true moves the default here; isDefault=false on the current default is ignored (see notes)
        if (Boolean.TRUE.equals(req.isDefault()) && !address.isDefaultAddress()) {
            addressRepository.clearDefaults(userId, Instant.now());
            address.setDefaultAddress(true);
        }
        return mapper.toResponse(address);
    }

    @Transactional
    public void delete(long userId, long id) {                                     // U8
        lockUser(userId);
        Address address = find(userId, id);
        boolean wasDefault = address.isDefaultAddress();
        addressRepository.delete(address);
        addressRepository.flush();
        if (wasDefault) {                                                          // keep a default while any address exists
            addressRepository.findFirstByUserIdOrderByIdAsc(userId)
                    .ifPresent(next -> next.setDefaultAddress(true));
        }
    }

    // another user's address is indistinguishable from a missing one (Section 8.3)
    private Address find(long userId, long id) {
        return addressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Address not found."));
    }

    private void lockUser(long userId) {
        userRepository.findByIdForUpdate(userId)
                .filter(u -> u.isActive())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                        "Authentication is required."));
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}