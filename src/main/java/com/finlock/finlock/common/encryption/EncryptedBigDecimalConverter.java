package com.finlock.finlock.common.encryption;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Converter
@Component
public class EncryptedBigDecimalConverter implements AttributeConverter<BigDecimal, String> {

    private static AesEncryptionUtil aesEncryptionUtil;

    @Autowired
    public void setAesEncryptionUtil(AesEncryptionUtil util) {
        EncryptedBigDecimalConverter.aesEncryptionUtil = util;
    }

    @Override
    public String convertToDatabaseColumn(BigDecimal attribute){
        if (attribute == null) return null;
        return aesEncryptionUtil.encrypt(attribute.toString());

    }

    @Override
    public BigDecimal convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return new BigDecimal(aesEncryptionUtil.decrypt(dbData));
    }


}
