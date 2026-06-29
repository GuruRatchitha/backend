package com.bank.fedwire.service;

import com.bank.fedwire.entity.PACS008;
import com.bank.fedwire.entity.Account;
import com.bank.fedwire.entity.Beneficiary;
import com.bank.fedwire.entity.Transaction;
import com.bank.fedwire.repository.AccountRepository;
import com.bank.fedwire.repository.BeneficiaryRepository;
import com.bank.fedwire.repository.PACS008Repository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class Pacs008XmlGeneratorService {

    private static final String FEDWIRE_NS = "urn:fedwirefunds:incoming:v001";
    private static final String HEAD_NS_ROOT = "urn:iso:std:iso:20022:tech:xsd:head.001.001.02";
    private static final String HEAD_NS = "urn:iso:std:iso:20022:tech:xsd:head.001.001.03";
    private static final String PACS_NS = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08";
    private static final String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";
    private static final String MESSAGE_TYPE = "pacs.008.001.08";
    private static final String BUSINESS_SERVICE = "TEST";
    private static final String MARKET_PRACTICE_REGISTRY =
            "www2.swift.com/mystandards/#/group/Federal_Reserve_Financial_Services/Fedwire_Funds_Service";
    private static final String MARKET_PRACTICE_ID = "frb.fedwire.01";
    private static final String SETTLEMENT_METHOD = "CLRG";
    private static final String CLEARING_SYSTEM = "FDW";
    private static final String CLEARING_SYSTEM_MEMBER_CODE = "USABA";
    private static final String DEFAULT_TO_MEMBER_ID = "021151080";
    private static final String CREDITOR_ACCOUNT_ID = "33333333330";
    private static final String LOCAL_INSTRUMENT = "CTRC";
    private static final String CHARGE_BEARER = "DEBT";
    private static final DateTimeFormatter ISO_LOCAL_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final String APP_HEADER_SCHEMA = "BusinessApplicationHeader_head_001_001_03.xsd";
    private static final String PACS_SCHEMA =
            "Fedwire_Funds_Service_Release_2025_CustomerCreditTransfer_pacs_008_001_08_20240708_1351_iso15enriched.xsd";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_WAITING = "WAITING_FOR_PACS002";

    private final AccountRepository accountRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final PACS008Repository pacs008Repository;

    @Transactional
    public String generateXml(Long transactionId) {
        if (transactionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "transactionId is required");
        }

        PACS008 pacs008 = pacs008Repository.findTopByTransactionIdOrderByCreatedDateDesc(transactionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "PACS008 record not found for transactionId " + transactionId
                                + ". Generate XML only for transactions created through POST /api/payments."));

        Transaction transaction = pacs008.getTransaction();
        if (transaction == null || (!STATUS_APPROVED.equalsIgnoreCase(transaction.getTransactionStatus())
                && !STATUS_WAITING.equalsIgnoreCase(transaction.getTransactionStatus()))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "PACS008 XML can only be generated for APPROVED or WAITING_FOR_PACS002 transactions");
        }

        if (pacs008.getXmlPayload() != null && !pacs008.getXmlPayload().isBlank()) {
            return pacs008.getXmlPayload();
        }

        String xml = buildXml(pacs008);
        pacs008.setXmlPayload(xml);
        pacs008Repository.save(pacs008);
        return xml;
    }

    private String buildXml(PACS008 pacs008) {
        try {
            StringWriter output = new StringWriter();
            XMLStreamWriter writer = XMLOutputFactory.newFactory().createXMLStreamWriter(output);

            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeStartElement("urn", "FedwireFundsIncoming", FEDWIRE_NS);
            writer.writeNamespace("urn", FEDWIRE_NS);
            writer.writeNamespace("h", HEAD_NS_ROOT);
            writer.writeNamespace("p", PACS_NS);

            start(writer, "urn", "FedwireFundsIncomingMessage", FEDWIRE_NS);
            start(writer, "urn", "FedwireFundsCustomerCreditTransfer", FEDWIRE_NS);
            String senderRoutingNumber = resolveSenderRoutingNumber(pacs008);
            String receiverRoutingNumber = resolveReceiverRoutingNumber(pacs008);

            writeAppHeader(writer, pacs008, senderRoutingNumber);
            writeDocument(writer, pacs008, senderRoutingNumber, receiverRoutingNumber);
            writer.writeEndElement();
            writer.writeEndElement();
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.close();

            return output.toString();
        } catch (XMLStreamException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to generate PACS008 XML", ex);
        }
    }

    private void writeAppHeader(XMLStreamWriter writer, PACS008 pacs008, String senderRoutingNumber)
            throws XMLStreamException {
        start(writer, "h", "AppHdr", HEAD_NS);
        writer.writeNamespace("h", HEAD_NS);
        writer.writeNamespace("xsi", XSI_NS);
        writer.writeAttribute("xsi", XSI_NS, "schemaLocation", HEAD_NS + " " + APP_HEADER_SCHEMA);

        writeHeaderMember(writer, "Fr", senderRoutingNumber);
        writeHeaderMember(writer, "To", DEFAULT_TO_MEMBER_ID);
        element(writer, "h", HEAD_NS, "BizMsgIdr", require(pacs008.getMessageId(), "messageId"));
        element(writer, "h", HEAD_NS, "MsgDefIdr", MESSAGE_TYPE);
        element(writer, "h", HEAD_NS, "BizSvc", BUSINESS_SERVICE);

        start(writer, "h", "MktPrctc", HEAD_NS);
        element(writer, "h", HEAD_NS, "Regy", MARKET_PRACTICE_REGISTRY);
        element(writer, "h", HEAD_NS, "Id", MARKET_PRACTICE_ID);
        writer.writeEndElement();

        element(writer, "h", HEAD_NS, "CreDt", formatDateTime(pacs008.getAcceptanceDatetime(), "acceptanceDatetime"));
        writer.writeEndElement();
    }

    private void writeDocument(XMLStreamWriter writer, PACS008 pacs008, String senderRoutingNumber, String receiverRoutingNumber)
            throws XMLStreamException {
        start(writer, "p", "Document", PACS_NS);
        writer.writeNamespace("xsi", XSI_NS);
        writer.writeAttribute("xsi", XSI_NS, "schemaLocation", PACS_NS + " " + PACS_SCHEMA);

        start(writer, "p", "FIToFICstmrCdtTrf", PACS_NS);
        writeGroupHeader(writer, pacs008);
        writeCreditTransferTransaction(writer, pacs008, senderRoutingNumber, receiverRoutingNumber);
        writer.writeEndElement();
        writer.writeEndElement();
    }

    private void writeGroupHeader(XMLStreamWriter writer, PACS008 pacs008) throws XMLStreamException {
        start(writer, "p", "GrpHdr", PACS_NS);
        element(writer, "p", PACS_NS, "MsgId", require(pacs008.getMessageId(), "messageId"));
        element(writer, "p", PACS_NS, "CreDtTm", formatDateTime(pacs008.getAcceptanceDatetime(), "acceptanceDatetime"));
        element(writer, "p", PACS_NS, "NbOfTxs", "1");

        start(writer, "p", "SttlmInf", PACS_NS);
        element(writer, "p", PACS_NS, "SttlmMtd", SETTLEMENT_METHOD);
        start(writer, "p", "ClrSys", PACS_NS);
        element(writer, "p", PACS_NS, "Cd", CLEARING_SYSTEM);
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndElement();
    }

    private void writeCreditTransferTransaction(XMLStreamWriter writer, PACS008 pacs008, String senderRoutingNumber,
                                               String receiverRoutingNumber) throws XMLStreamException {
        start(writer, "p", "CdtTrfTxInf", PACS_NS);
        writePaymentId(writer, pacs008);
        writePaymentTypeInfo(writer);
        amountElement(writer, "IntrBkSttlmAmt", pacs008.getCurrency(), pacs008.getAmount());
        element(writer, "p", PACS_NS, "IntrBkSttlmDt", formatDate(pacs008.getSettlementDate(), "settlementDate"));
        element(writer, "p", PACS_NS, "AccptncDtTm", formatDateTime(pacs008.getAcceptanceDatetime(), "acceptanceDatetime"));
        amountElement(writer, "InstdAmt", pacs008.getCurrency(), pacs008.getAmount());
        element(writer, "p", PACS_NS, "ChrgBr", CHARGE_BEARER);
        writeAgent(writer, "InstgAgt", senderRoutingNumber, null, null, null);
        writeAgent(writer, "InstdAgt", receiverRoutingNumber, null, null, null);
        writeParty(writer, "Dbtr", pacs008.getDebtorName(), pacs008.getDebtorTown(), pacs008.getDebtorCountry());
        writeAccount(writer, "DbtrAcct", pacs008.getDebtorAccount());
        writeAgent(writer, "DbtrAgt", senderRoutingNumber,
                pacs008.getDebtorName(), pacs008.getDebtorTown(), pacs008.getDebtorCountry());
        writeAgent(writer, "CdtrAgt", receiverRoutingNumber,
                pacs008.getCreditorName(), pacs008.getCreditorTown(), pacs008.getCreditorCountry());
        writeParty(writer, "Cdtr", pacs008.getCreditorName(), pacs008.getCreditorTown(), pacs008.getCreditorCountry());
        writeAccount(writer, "CdtrAcct", CREDITOR_ACCOUNT_ID);
        writer.writeEndElement();
    }

    private void writePaymentId(XMLStreamWriter writer, PACS008 pacs008) throws XMLStreamException {
        start(writer, "p", "PmtId", PACS_NS);
        element(writer, "p", PACS_NS, "InstrId", require(pacs008.getInstructionId(), "instructionId"));
        element(writer, "p", PACS_NS, "EndToEndId", require(pacs008.getEndToEndId(), "endToEndId"));
        element(writer, "p", PACS_NS, "TxId", require(pacs008.getTxId(), "txId"));
        element(writer, "p", PACS_NS, "UETR", require(pacs008.getUetr(), "uetr"));
        writer.writeEndElement();
    }

    private void writePaymentTypeInfo(XMLStreamWriter writer) throws XMLStreamException {
        start(writer, "p", "PmtTpInf", PACS_NS);
        start(writer, "p", "LclInstrm", PACS_NS);
        element(writer, "p", PACS_NS, "Prtry", LOCAL_INSTRUMENT);
        writer.writeEndElement();
        writer.writeEndElement();
    }

    private void writeHeaderMember(XMLStreamWriter writer, String name, String memberId) throws XMLStreamException {
        start(writer, "h", name, HEAD_NS);
        start(writer, "h", "FIId", HEAD_NS);
        start(writer, "h", "FinInstnId", HEAD_NS);
        start(writer, "h", "ClrSysMmbId", HEAD_NS);
        element(writer, "h", HEAD_NS, "MmbId", memberId);
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndElement();
    }

    private void writeAgent(XMLStreamWriter writer, String name, String memberId, String partyName, String town, String country)
            throws XMLStreamException {
        start(writer, "p", name, PACS_NS);
        start(writer, "p", "FinInstnId", PACS_NS);
        start(writer, "p", "ClrSysMmbId", PACS_NS);
        start(writer, "p", "ClrSysId", PACS_NS);
        element(writer, "p", PACS_NS, "Cd", CLEARING_SYSTEM_MEMBER_CODE);
        writer.writeEndElement();
        element(writer, "p", PACS_NS, "MmbId", memberId);
        writer.writeEndElement();

        if (partyName != null && town != null && country != null) {
            element(writer, "p", PACS_NS, "Nm", require(partyName, name + " name"));
            writePostalAddress(writer, town, country);
        }

        writer.writeEndElement();
        writer.writeEndElement();
    }

    private void writeParty(XMLStreamWriter writer, String name, String partyName, String town, String country)
            throws XMLStreamException {
        start(writer, "p", name, PACS_NS);
        element(writer, "p", PACS_NS, "Nm", require(partyName, name + " name"));
        writePostalAddress(writer, town, country);
        writer.writeEndElement();
    }

    private void writePostalAddress(XMLStreamWriter writer, String town, String country) throws XMLStreamException {
        start(writer, "p", "PstlAdr", PACS_NS);
        element(writer, "p", PACS_NS, "TwnNm", require(town, "town"));
        element(writer, "p", PACS_NS, "Ctry", require(country, "country"));
        writer.writeEndElement();
    }

    private void writeAccount(XMLStreamWriter writer, String name, String accountNumber) throws XMLStreamException {
        start(writer, "p", name, PACS_NS);
        start(writer, "p", "Id", PACS_NS);
        start(writer, "p", "Othr", PACS_NS);
        element(writer, "p", PACS_NS, "Id", require(accountNumber, name + " account"));
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndElement();
    }

    private void amountElement(XMLStreamWriter writer, String name, String currency, BigDecimal amount)
            throws XMLStreamException {
        start(writer, "p", name, PACS_NS);
        writer.writeAttribute("Ccy", require(currency, "currency"));
        writer.writeCharacters(require(amount, "amount").toPlainString());
        writer.writeEndElement();
    }

    private void start(XMLStreamWriter writer, String prefix, String name, String namespace) throws XMLStreamException {
        writer.writeStartElement(prefix, name, namespace);
    }

    private void element(XMLStreamWriter writer, String prefix, String namespace, String name, String value)
            throws XMLStreamException {
        start(writer, prefix, name, namespace);
        writer.writeCharacters(require(value, name));
        writer.writeEndElement();
    }

    private String formatDate(LocalDate value, String fieldName) {
        return require(value, fieldName).toString();
    }

    private String formatDateTime(LocalDateTime value, String fieldName) {
        return require(value, fieldName).format(ISO_LOCAL_DATE_TIME);
    }

    private String resolveSenderRoutingNumber(PACS008 pacs008) {
        Transaction transaction = require(pacs008.getTransaction(), "transaction");
        Account senderAccount = accountRepository.findByAccountNumber(transaction.getAccountNumber())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Sender account not found for account number " + transaction.getAccountNumber()));

        return require(senderAccount.getRoutingNumber(), "sender routing_number");
    }

    private String resolveReceiverRoutingNumber(PACS008 pacs008) {
        Transaction transaction = require(pacs008.getTransaction(), "transaction");
        Beneficiary beneficiary = beneficiaryRepository.findByAccountNumberAndRoutingNumber(
                        transaction.getBeneficiaryAccountNumber(), transaction.getBeneficiaryRoutingNumber())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Beneficiary not found for account number " + transaction.getBeneficiaryAccountNumber()));

        return require(beneficiary.getRoutingNumber(), "receiver routing_number");
    }

    private String require(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PACS008 " + fieldName + " is required");
        }
        return value.trim();
    }

    private <T> T require(T value, String fieldName) {
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PACS008 " + fieldName + " is required");
        }
        return value;
    }
}
