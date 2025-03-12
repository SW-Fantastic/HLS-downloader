package org.swdc.hls.core;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class HLSKeySet {

    private SecretKeySpec key;

    private IvParameterSpec iv;

    public HLSKeySet(SecretKeySpec key, IvParameterSpec iv) {
        this.key = key;
        this.iv = iv;
    }

    public Cipher createCipher() {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key, iv);
            return cipher;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
