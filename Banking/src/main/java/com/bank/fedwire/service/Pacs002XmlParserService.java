package com.bank.fedwire.service;

import com.bank.fedwire.dto.Pacs002MessageDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

@Service
@Slf4j
public class Pacs002XmlParserService {

    public Pacs002MessageDto parse(String xmlPayload) {
        if (xmlPayload == null || xmlPayload.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PACS002 XML payload is required");
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);
            factory.setXIncludeAware(false);
            factory.setNamespaceAware(true);

            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xmlPayload)));
            document.getDocumentElement().normalize();

            Pacs002MessageDto dto = Pacs002MessageDto.builder()
                    .originalMessageId(findFirstText(document, "original_message_id", "OrgnlMsgId"))
                    .messageId(findFirstText(document, "message_id", "MsgId"))
                    .transferId(findFirstText(document, "transfer_id", "TrfId", "OrgnlTxId", "TxId",
                            "OrgnlInstrId", "InstrId", "OrgnlEndToEndId", "EndToEndId"))
                    .transactionStatus(findFirstText(document, "transaction_status", "TxSts"))
                    .reasonCode(findFirstText(document, "reason_code", "Rsn", "Prtry"))
                    .xmlPayload(xmlPayload)
                    .build();

            log.info("PACS002 XML parsed original_message_id={}, message_id={}, transfer_id={}, transaction_status={}, reason_code={}",
                    dto.getOriginalMessageId(), dto.getMessageId(), dto.getTransferId(),
                    dto.getTransactionStatus(), dto.getReasonCode());
            return dto;
        } catch (Exception ex) {
            log.error("Malformed PACS002 XML payload length={}, payload={}",
                    xmlPayload != null ? xmlPayload.length() : 0, abbreviate(xmlPayload), ex);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed PACS002 XML", ex);
        }
    }

    private String findFirstText(Document document, String... localNames) {
        for (String localName : localNames) {
            NodeList nodes = document.getElementsByTagNameNS("*", localName);
            if (nodes == null || nodes.getLength() == 0) {
                nodes = document.getElementsByTagName(localName);
            }

            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node instanceof Element element) {
                    String text = element.getTextContent();
                    if (text != null && !text.isBlank()) {
                        return text.trim();
                    }
                }
            }
        }
        return null;
    }

    private String abbreviate(String value) {
        if (value == null) {
            return null;
        }

        int maxLength = 1000;
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
}
