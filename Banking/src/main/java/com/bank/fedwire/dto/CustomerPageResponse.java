package com.bank.fedwire.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerPageResponse {

    private List<CustomerResponse> content;

    private long totalElements;

    private int totalPages;

    private int currentPage;

    private int pageSize;
}
