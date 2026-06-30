package com.bank.fedwire.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

@Component
@Slf4j
public class InboundSqsMessageSupport {

    public String extractXmlPayload(String body) {
        if (body == null || body.isBlank()) {
            return body;
        }

        String trimmed = body.trim();
        if (!trimmed.startsWith("{")) {
            return body;
        }

        String message = extractJsonStringField(trimmed, "Message");
        if (message != null && !message.isBlank()) {
            return message;
        }

        return body;
    }

    public boolean isSnsNotificationWrapper(String body) {
        return body != null && !body.isBlank() && body.trim().startsWith("{");
    }

    public InboundMessageMetadata extractMetadata(String xmlPayload) {
        if (xmlPayload == null || xmlPayload.isBlank()) {
            return new InboundMessageMetadata(null, null);
        }

        try {
            Document document = parse(xmlPayload);
            return new InboundMessageMetadata(
                    findFirstText(document, "MsgDefIdr"),
                    findFirstText(document, "BizMsgIdr"));
        } catch (Exception ex) {
            log.warn("Unable to extract inbound metadata from XML payload length={}",
                    xmlPayload.length(), ex);
            return new InboundMessageMetadata(null, null);
        }
    }

    public InboundMessageMetadata detectMessageTypeByScan(String body) {
        if (body == null || body.isBlank()) {
            return new InboundMessageMetadata(null, null);
        }

        String normalized = body.toLowerCase();
        String messageType = null;
        if (normalized.contains("admi.002.001.01")) {
            messageType = "admi.002.001.01";
        } else if (normalized.contains("pacs.002.")) {
            messageType = "pacs.002.001.10";
        }

        return new InboundMessageMetadata(messageType, findFirstTextFromBody(body, "BizMsgIdr"));
    }

    public boolean isPacs002(String messageType) {
        return messageType != null && messageType.toLowerCase().startsWith("pacs.002.");
    }

    public boolean isAdmi002(String messageType) {
        return messageType != null && messageType.toLowerCase().startsWith("admi.002.");
    }

    private String extractJsonStringField(String json, String fieldName) {
        String needle = "\"" + fieldName + "\"";
        int keyIndex = json.indexOf(needle);
        if (keyIndex < 0) {
            return null;
        }

        int colonIndex = json.indexOf(':', keyIndex + needle.length());
        if (colonIndex < 0) {
            return null;
        }

        int startQuote = json.indexOf('"', colonIndex + 1);
        if (startQuote < 0) {
            return null;
        }

        StringBuilder value = new StringBuilder();
        boolean escaping = false;
        for (int i = startQuote + 1; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (escaping) {
                switch (ch) {
                    case '"', '\\', '/' -> value.append(ch);
                    case 'b' -> value.append('\b');
                    case 'f' -> value.append('\f');
                    case 'n' -> value.append('\n');
                    case 'r' -> value.append('\r');
                    case 't' -> value.append('\t');
                    case 'u' -> {
                        if (i + 4 < json.length()) {
                            String hex = json.substring(i + 1, i + 5);
                            try {
                                value.append((char) Integer.parseInt(hex, 16));
                                i += 4;
                            } catch (NumberFormatException ex) {
                                log.warn("Failed to decode JSON unicode escape for fieldName={} at index={} json={}",
                                        fieldName, i, json, ex);
                                return null;
                            }
                        } else {
                            return null;
                        }
                    }
                    default -> value.append(ch);
                }
                escaping = false;
                continue;
            }

            if (ch == '\\') {
                escaping = true;
                continue;
            }

            if (ch == '"') {
                return value.toString();
            }

            value.append(ch);
        }

        return null;
    }

    private String findFirstTextFromBody(String xmlPayload, String localName) {
        if (xmlPayload == null || xmlPayload.isBlank()) {
            return null;
        }

        try {
            return findFirstText(parse(xmlPayload), localName);
        } catch (Exception ex) {
            return null;
        }
    }

    private Document parse(String xmlPayload) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setExpandEntityReferences(false);
        factory.setXIncludeAware(false);
        factory.setNamespaceAware(true);

        Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xmlPayload)));
        document.getDocumentElement().normalize();
        return document;
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

    public record InboundMessageMetadata(String messageType, String businessMessageId) {
    }
}
