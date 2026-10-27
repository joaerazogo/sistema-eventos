package co.com.ticketing.processor.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ObjectMapperConfigTest {

    private final ObjectMapperConfig config = new ObjectMapperConfig();

    @Test
    void jacksonObjectMapperDisablesWriteDatesAsTimestamps() {
        ObjectMapper mapper = config.jacksonObjectMapper();
        assertNotNull(mapper);
        assertFalse(mapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));
    }

    @Test
    void objectMapperBeanIsNotNull() {
        assertNotNull(config.objectMapper());
    }
}
