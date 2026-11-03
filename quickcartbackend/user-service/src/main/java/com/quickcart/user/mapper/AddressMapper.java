package com.quickcart.user.mapper;

import com.quickcart.user.dto.response.AddressResponse;
import com.quickcart.user.entity.Address;
import org.springframework.stereotype.Component;

@Component
public class AddressMapper {

    public AddressResponse toResponse(Address a) {
        return new AddressResponse(a.getId(), a.getLabel(), a.getRecipientName(), a.getPhone(), a.getLine1(),
                a.getLine2(), a.getCity(), a.getState(), a.getPincode(), a.isDefaultAddress());
    }
}