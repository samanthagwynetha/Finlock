package com.finlock.finlock.common.encryption;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConverterSpringContextBridge {

    private final AesEncryptionUtil aesEncryptionUtil;
    private final EncryptedBigDecimalConverter converter;

    @PostConstruct
    public void init() {
        converter.setAesEncryptionUtil(aesEncryptionUtil);
    }

}
