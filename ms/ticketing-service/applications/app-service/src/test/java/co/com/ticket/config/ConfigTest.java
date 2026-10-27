package co.com.ticket.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.reactivecommons.utils.ObjectMapperImp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigTest {

    private final ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();

    @Test
    void shouldCreateJacksonObjectMapper() {
        assertNotNull(objectMapperConfig.jacksonObjectMapper());
    }

    @Test
    void shouldCreateReactiveCommonsObjectMapper() {
        org.reactivecommons.utils.ObjectMapper mapper = objectMapperConfig.objectMapper();
        assertNotNull(mapper);
        assertTrue(mapper instanceof ObjectMapperImp);
    }

    @Test
    void jacksonObjectMapperShouldNotWriteDatesAsTimestamps() throws Exception {
        ObjectMapper mapper = objectMapperConfig.jacksonObjectMapper();
        java.time.Instant now = java.time.Instant.now();
        String json = mapper.writeValueAsString(now);
        assertFalse(json.matches("^[0-9]+.*"), "Should not serialize Instant as numeric timestamp");
    }
}
