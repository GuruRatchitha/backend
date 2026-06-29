package com.bank.fedwire.service;

import com.bank.fedwire.dto.Pacs002MessageDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class Pacs002XmlParserServiceTest {

    private final Pacs002XmlParserService parser = new Pacs002XmlParserService();

    @Test
    void parseFedwireFundsOutgoingPaymentStatus() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <urn:FedwireFundsOutgoing xmlns:urn="urn:fedwirefunds:outgoing:v001"
                    xmlns:h="urn:iso:std:iso:20022:tech:xsd:head.001.001.02"
                    xmlns:p="urn:iso:std:iso:20022:tech:xsd:pacs.002.001.10">
                    <urn:FedwireFundsOutgoingMessage>
                        <urn:FedwireFundsPaymentStatus>
                            <h:AppHdr xmlns:h="urn:iso:std:iso:20022:tech:xsd:head.001.001.03">
                                <h:BizMsgIdr>20260629A1B2C3D400004106290310FT03</h:BizMsgIdr>
                                <h:MsgDefIdr>pacs.002.001.10</h:MsgDefIdr>
                            </h:AppHdr>
                            <p:Document xmlns:p="urn:iso:std:iso:20022:tech:xsd:pacs.002.001.10">
                                <p:FIToFIPmtStsRpt>
                                    <p:GrpHdr>
                                        <p:MsgId>20260629A1B2C3D400004106290310FT03</p:MsgId>
                                    </p:GrpHdr>
                                    <p:TxInfAndSts>
                                        <p:OrgnlGrpInf>
                                            <p:OrgnlMsgId>20260629AERTYUHG000001</p:OrgnlMsgId>
                                            <p:OrgnlMsgNmId>pacs.008.001.08</p:OrgnlMsgNmId>
                                        </p:OrgnlGrpInf>
                                        <p:OrgnlInstrId>5dc4d752-af8a-4455-ab70-75adb18ebcf</p:OrgnlInstrId>
                                        <p:OrgnlEndToEndId>INV0019253</p:OrgnlEndToEndId>
                                        <p:OrgnlTxId>456cc68c-995f-4f8c-a5b6-5c4aa6358c4</p:OrgnlTxId>
                                        <p:TxSts>ACSC</p:TxSts>
                                        <p:InstgAgt>
                                            <p:FinInstnId>
                                                <p:ClrSysMmbId>
                                                    <p:ClrSysId>
                                                        <p:Cd>USABA</p:Cd>
                                                    </p:ClrSysId>
                                                </p:ClrSysMmbId>
                                            </p:FinInstnId>
                                        </p:InstgAgt>
                                    </p:TxInfAndSts>
                                </p:FIToFIPmtStsRpt>
                            </p:Document>
                        </urn:FedwireFundsPaymentStatus>
                    </urn:FedwireFundsOutgoingMessage>
                </urn:FedwireFundsOutgoing>
                """;

        Pacs002MessageDto parsed = parser.parse(xml);

        assertEquals("20260629AERTYUHG000001", parsed.getOriginalMessageId());
        assertEquals("20260629A1B2C3D400004106290310FT03", parsed.getMessageId());
        assertEquals("456cc68c-995f-4f8c-a5b6-5c4aa6358c4", parsed.getTransferId());
        assertEquals("ACSC", parsed.getTransactionStatus());
        assertNull(parsed.getReasonCode());
        assertEquals(xml, parsed.getXmlPayload());
    }
}
