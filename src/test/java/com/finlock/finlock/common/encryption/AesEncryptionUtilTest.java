package com.finlock.finlock.common.encryption;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SpringBootTest
@ActiveProfiles("test")
public class AesEncryptionUtilTest {

    @Autowired
    private AesEncryptionUtil aesEncryptionUtil;

    @Test
    void encryptThenDecrypt_shouldRetunOriginalValue(){
        String original = "1000.0000";
        String encrypted = aesEncryptionUtil.encrypt(original);
        String decrypted = aesEncryptionUtil.decrypt(encrypted);
        assertNotEquals(original, encrypted);
        assertEquals(original, decrypted);
    }

    @Test
    void encryptingSameValueTwice_shouldProduceDifferentCipherText() {
        String original = "500.0000";
        String encrypted1 = aesEncryptionUtil.encrypt(original);
        String encrypted2 = aesEncryptionUtil.encrypt(original);
        assertNotEquals(encrypted1, encrypted2);
    }
}
