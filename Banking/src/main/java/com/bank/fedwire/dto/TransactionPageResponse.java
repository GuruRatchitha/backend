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
public class TransactionPageResponse {

    private List<TransactionResponse> transactions;

    private int page;

    private int size;

    private long totalElements;

    private int totalPages;
}
