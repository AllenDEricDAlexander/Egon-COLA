package top.egon.cola.component.common.crypto;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.crypto.codec.Base64s;
import top.egon.cola.component.common.crypto.codec.Hexes;
import top.egon.cola.component.common.crypto.digest.Digests;
import top.egon.cola.component.common.crypto.hmac.Hmacs;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CryptoTest {

    @Test
    void sha256HexUsesStableLowercaseOutput() {
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", Digests.sha256Hex("hello"));
    }

    @Test
    void hmacSha256HexUsesStableLowercaseOutput() {
        assertEquals("9307b3b915efb5171ff14d8cb55fbcc798c6c0ef1456d66ded1a6aa723a58b7b", Hmacs.sha256Hex("hello", "key"));
    }

    @Test
    void base64RoundTripWorks() {
        String encoded = Base64s.encodeToString("hello");

        assertEquals("hello", Base64s.decodeToString(encoded));
    }

    @Test
    void hexRoundTripWorks() {
        String encoded = Hexes.encodeToString("hello");

        assertEquals("68656x6c6f".replace("x", "c"), encoded);
        assertEquals("hello", Hexes.decodeToString(encoded));
    }
}
