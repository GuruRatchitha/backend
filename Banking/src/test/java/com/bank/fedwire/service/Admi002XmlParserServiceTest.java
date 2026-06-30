package com.bank.fedwire.service;

import com.bank.fedwire.dto.Admi002MessageDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Admi002XmlParserServiceTest {

    private final Admi002XmlParserService parser = new Admi002XmlParserService();

    @Test
    void parseExtractsAdmi002Fields() {
        String xml = """
                <Envelope>
                  <AppHdr>
                    <MsgDefIdr>admi.002.001.01</MsgDefIdr>
                    <BizMsgIdr>ADM-BIZ-1</BizMsgIdr>
                    <CreDt>2026-06-30T11:00:00Z</CreDt>
                  </AppHdr>
                  <Document>
                    <MsgId>ADM-MSG-1</MsgId>
                    <OrgnlMsgId>PACS008-MSG-1</OrgnlMsgId>
                    <RltdRef><Ref>TRF-123</Ref></RltdRef>
                    <RjctgPtyRsn>
                      <Cd>FF01</Cd>
                      <RsnDesc>Invalid account number</RsnDesc>
                    </RjctgPtyRsn>
                    <Svrty>FATAL</Svrty>
                    <RjctnDtTm>2026-06-30T11:01:00Z</RjctnDtTm>
                  </Document>
                </Envelope>
                """;

        Admi002MessageDto parsed = parser.parse(xml);

        assertEquals("admi.002.001.01", parsed.getMessageType());
        assertEquals("ADM-MSG-1", parsed.getMessageId());
        assertEquals("PACS008-MSG-1", parsed.getOriginalMessageId());
        assertEquals("ADM-BIZ-1", parsed.getBusinessMessageId());
        assertEquals("TRF-123", parsed.getRelatedMessageId());
        assertEquals("FF01", parsed.getErrorCode());
        assertEquals("Invalid account number", parsed.getErrorDescription());
        assertEquals("FATAL", parsed.getSeverity());
        assertEquals(2026, parsed.getCreationDateTime().getYear());
    }
}
