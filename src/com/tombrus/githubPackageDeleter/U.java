package com.tombrus.githubPackageDeleter;

import java.security.Key;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class U {
    private static final String CRYPT_ALGORITHM = "DESede";

    public static String humanReadable(long bytes) {
        int unit = 1024;
        if (bytes < unit) {
            return Long.toString(bytes);
        } else {
            int          exp       = (int) (Math.log(bytes) / Math.log(unit));
            final String formatted = String.format("%.1f", bytes / Math.pow(unit, exp));
            char         period    = formatted.contains(".") ? '.' : formatted.contains(",") ? ',' : '?';
            return formatted.replace(period, "kMGTPE".charAt(exp - 1));
        }
    }

    public static String addLineNumbers(String s) {
        AtomicInteger n = new AtomicInteger();
        return Arrays.stream(s.split("\n")).map(l -> String.format("%5d %s\n", n.incrementAndGet(), l)).collect(Collectors.joining());
    }

    public static void sort(DefaultMutableTreeNode node) {
        Collections.list(node.children())
                .stream()
                .map(c->(MutableTreeNode)c)
                .sorted(Comparator.comparing(Object::toString).reversed())
                .forEach(c->node.insert(c,0));
    }

    public static String encrypt(String toEncrypt) {
        try {
            Cipher c   = Cipher.getInstance(CRYPT_ALGORITHM);
            Key    key = KeyGenerator.getInstance(CRYPT_ALGORITHM).generateKey();
            c.init(Cipher.ENCRYPT_MODE, key);

            byte[] keyBytes = key.getEncoded();
            byte[] msgBytes = c.doFinal(toEncrypt.getBytes());
            byte[] both     = new byte[keyBytes.length + msgBytes.length + 1];
            if (255 < keyBytes.length) {
                throw new IllegalArgumentException("Key can not be longer then 255 bytes");
            }
            both[0] = (byte) keyBytes.length;
            System.arraycopy(keyBytes, 0, both, 1, keyBytes.length);
            System.arraycopy(msgBytes, 0, both, 1 + keyBytes.length, msgBytes.length);

            return Base64.getEncoder().encodeToString(both);
        } catch (Exception e) {
            throw new RuntimeException("problems with token encryption", e);
        }
    }

    public static String decrypt(String toDecrypt) {
        try {
            byte[] both      = Base64.getDecoder().decode(toDecrypt);
            int    keyLength = both[0];
            byte[] keyBytes  = new byte[keyLength];
            byte[] msgBytes  = new byte[both.length - keyLength - 1];
            System.arraycopy(both, 1, keyBytes, 0, keyBytes.length);
            System.arraycopy(both, 1 + keyLength, msgBytes, 0, msgBytes.length);

            Cipher        c   = Cipher.getInstance(CRYPT_ALGORITHM);
            SecretKeySpec key = new SecretKeySpec(keyBytes, CRYPT_ALGORITHM);
            c.init(Cipher.DECRYPT_MODE, key);
            return new String(c.doFinal(msgBytes));
        } catch (Exception e) {
            throw new RuntimeException("problems with token decryption", e);
        }
    }
}
