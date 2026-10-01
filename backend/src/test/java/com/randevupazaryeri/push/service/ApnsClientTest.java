package com.randevupazaryeri.push.service;

import com.randevupazaryeri.config.ApnsProperties;
import com.randevupazaryeri.push.entity.PushApp;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class ApnsClientTest {

    @Test
    void signsProviderTokenWithP8KeyFromEnvironmentStyleString() throws Exception {
        KeyPair keyPair = newKeyPair();
        ApnsProperties properties = new ApnsProperties();
        properties.setTeamId("TEAM123456");
        properties.setKeyId("KEY1234567");
        properties.setPrivateKey(envStylePem(keyPair));
        ApnsClient client = new ApnsClient(properties);

        String token = client.providerToken(properties.keyFor(PushApp.PARTNER));
        Jws<Claims> jws = Jwts.parser().verifyWith(keyPair.getPublic()).build().parseSignedClaims(token);

        assertThat(client.isConfigured(PushApp.CUSTOMER)).isTrue();
        assertThat(jws.getHeader().getKeyId()).isEqualTo("KEY1234567");
        assertThat(jws.getHeader().getAlgorithm()).isEqualTo("ES256");
        assertThat(jws.getPayload().getIssuer()).isEqualTo("TEAM123456");
        assertThat(client.providerToken(properties.keyFor(PushApp.PARTNER))).isEqualTo(token);
    }

    @Test
    void usesTopicSpecificKeyPerApp() throws Exception {
        KeyPair partnerKey = newKeyPair();
        ApnsProperties properties = new ApnsProperties();
        properties.setTeamId("TEAM123456");
        properties.getPartner().setKeyId("PARTNERKEY");
        properties.getPartner().setPrivateKey(envStylePem(partnerKey));
        ApnsClient client = new ApnsClient(properties);

        assertThat(client.isConfigured(PushApp.PARTNER)).isTrue();
        assertThat(client.isConfigured(PushApp.CUSTOMER)).isFalse();
        String token = client.providerToken(properties.keyFor(PushApp.PARTNER));
        assertThat(Jwts.parser().verifyWith(partnerKey.getPublic()).build().parseSignedClaims(token)
                .getHeader().getKeyId()).isEqualTo("PARTNERKEY");
    }

    private static KeyPair newKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        return generator.generateKeyPair();
    }

    private static String envStylePem(KeyPair keyPair) {
        String body = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(keyPair.getPrivate().getEncoded());
        return ("-----BEGIN PRIVATE KEY-----\n" + body + "\n-----END PRIVATE KEY-----").replace("\n", "\\n");
    }
}
