package co.com.ticket.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class UseCasesConfigTest {

    @Test
    void configIsInstantiable() {
        assertNotNull(new UseCasesConfig());
    }
}
