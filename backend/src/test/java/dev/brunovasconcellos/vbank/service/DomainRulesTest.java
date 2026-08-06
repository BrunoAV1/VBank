package dev.brunovasconcellos.vbank.service;

import dev.brunovasconcellos.vbank.domain.Enums;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DomainRulesTest {
    @Test
    void normalizesInternalKeysWithoutDocuments() {
        assertThat(DomainNormalizer.email(" Bruno@Example.COM ")).isEqualTo("bruno@example.com");
        assertThat(DomainNormalizer.username(" @Bruno.V ")).isEqualTo("bruno.v");
        assertThat(DomainNormalizer.pixKey(Enums.PixKeyType.PHONE, "+55 (11) 99999-9999"))
                .isEqualTo("+5511999999999");
    }

    @Test
    void detectsValidCpfAndCnpjButNotOrdinaryPhone() {
        assertThat(DocumentDetector.looksLikeCpfOrCnpj("529.982.247-25")).isTrue();
        assertThat(DocumentDetector.looksLikeCpfOrCnpj("04.252.011/0001-10")).isTrue();
        assertThat(DocumentDetector.looksLikeCpfOrCnpj("+55 11 98888-7777")).isFalse();
    }

    @Test
    void masksRecipientName() {
        assertThat(DtoMapper.maskName("Marina de Souza")).isEqualTo("M****a S***a");
        assertThat(DtoMapper.maskName("Li")).isEqualTo("L*");
    }
}

