package com.bank.fedwire.service;

import com.bank.fedwire.dto.Admi002MessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class Admi002ParserService {

    private final Admi002XmlParserService admi002XmlParserService;

    public Admi002MessageDto parse(String xmlPayload) {
        return admi002XmlParserService.parse(xmlPayload);
    }
}
