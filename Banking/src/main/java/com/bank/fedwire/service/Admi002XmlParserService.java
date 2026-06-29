package com.bank.fedwire.service;

import com.bank.fedwire.dto.Admi002MessageDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

@Service
@Slf4j
public class Admi002XmlParserService {

    public Admi002MessageDto parse(String xmlPayload) {
        if (xmlPayload == null || xmlPayload.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ADMI002 XML payload is required");
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

            Admi002MessageDto dto = Admi002MessageDto.builder()
                    .messageType(findFirstText(document, "MsgDefIdr"))
                    .businessMessageId(findFirstText(document, "BizMsgIdr"))
                    .originalReference(findNestedText(document, "RltdRef", "Ref"))
                    .rejectReasonCode(findNestedText(document, "RjctgPtyRsn", "Cd", "Prtry"))
                    .rejectReasonDescription(findNestedText(document, "RjctgPtyRsn", "RsnDesc", "Desc", "AddtlInf"))
                    .rejectionDateTime(parseDateTime(findFirstText(document, "RjctnDtTm")))
                    .xmlPayload(xmlPayload)
                    .build();

            log.info("ADMI002 XML parsed messageType={}, businessMessageId={}, originalReference={}, rejectReasonCode={}, rejectionDateTime={}",
                    dto.getMessageType(), dto.getBusinessMessageId(), dto.getOriginalReference(),
                    dto.getRejectReasonCode(), dto.getRejectionDateTime());
            return dto;
        } catch (Exception ex) {
            log.error("Malformed ADMI002 XML payload length={}, payload={}",
                    xmlPayload.length(), abbreviate(xmlPayload), ex);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed ADMI002 XML", ex);
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

    private String findNestedText(Document document, String parentLocalName, String... childLocalNames) {
        NodeList parents = document.getElementsByTagNameNS("*", parentLocalName);
        if (parents == null || parents.getLength() == 0) {
            parents = document.getElementsByTagName(parentLocalName);
        }

        for (int i = 0; i < parents.getLength(); i++) {
            Node parent = parents.item(i);
            if (!(parent instanceof Element parentElement)) {
                continue;
            }

            for (String childLocalName : childLocalNames) {
                NodeList children = parentElement.getElementsByTagNameNS("*", childLocalName);
                if (children == null || children.getLength() == 0) {
                    children = parentElement.getElementsByTagName(childLocalName);
                }

                for (int j = 0; j < children.getLength(); j++) {
                    Node child = children.item(j);
                    if (child instanceof Element childElement) {
                        String text = childElement.getTextContent();
                        if (text != null && !text.isBlank()) {
                            return text.trim();
                        }
                    }
                }
            }
        }

        return null;
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return OffsetDateTime.parse(value.trim()).toLocalDateTime();
        } catch (DateTimeParseException ex) {
            try {
                return LocalDateTime.parse(value.trim());
            } catch (DateTimeParseException ignored) {
                log.warn("Unable to parse ADMI002 rejectionDateTime value={}", value);
                return null;
            }
        }
    }

    private String abbreviate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 1000 ? value : value.substring(0, 1000) + "...";
    }
}
