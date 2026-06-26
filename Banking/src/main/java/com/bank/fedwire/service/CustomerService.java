package com.bank.fedwire.service;

import com.bank.fedwire.dto.CustomerPageResponse;
import com.bank.fedwire.dto.CustomerRequest;
import com.bank.fedwire.dto.CustomerResponse;
import com.bank.fedwire.dto.CustomerUpdateRequest;
import org.springframework.data.domain.Pageable;

public interface CustomerService {

    CustomerResponse createCustomer(CustomerRequest request);

    CustomerPageResponse getAllCustomers(Pageable pageable);

    CustomerResponse getCustomerById(Long id);

    CustomerResponse updateCustomer(Long id, CustomerUpdateRequest request);

    void deleteCustomer(Long id);
}
