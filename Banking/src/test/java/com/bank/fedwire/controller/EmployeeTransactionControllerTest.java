package com.bank.fedwire.controller;

import com.bank.fedwire.dto.EmployeeTransactionQueueResponse;
import com.bank.fedwire.dto.TransactionDetailResponse;
import com.bank.fedwire.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeTransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private EmployeeTransactionController controller;

    @Test
    void pacs002XmlReturnsStoredXmlWhenPresent() {
        when(transactionService.findEmployeeTransactionPacs002Xml(77L))
                .thenReturn(Optional.of("<Document><transaction_status>ACCP</transaction_status></Document>"));

        ResponseEntity<String> response = controller.getPacs002Xml(77L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_XML, response.getHeaders().getContentType());
        assertTrue(response.getBody().contains("<transaction_status>ACCP</transaction_status>"));
    }

    @Test
    void pacs002XmlReturnsMessageWhenMissing() {
        when(transactionService.findEmployeeTransactionPacs002Xml(77L)).thenReturn(Optional.empty());

        ResponseEntity<String> response = controller.getPacs002Xml(77L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(MediaType.TEXT_PLAIN, response.getHeaders().getContentType());
        assertEquals("PACS002 response has not been received yet.", response.getBody());
    }

    @Test
    void queueEndpointDelegatesToTransactionService() {
        when(transactionService.getEmployeeTransactionQueue()).thenReturn(List.of(
                EmployeeTransactionQueueResponse.builder()
                        .transactionId(1L)
                        .status("PROCESSING")
                        .build()));

        ResponseEntity<List<EmployeeTransactionQueueResponse>> response = controller.getQueue();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("PROCESSING", response.getBody().get(0).getStatus());
    }
}
