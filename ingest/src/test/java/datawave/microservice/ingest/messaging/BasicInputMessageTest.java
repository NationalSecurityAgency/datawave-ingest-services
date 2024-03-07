package datawave.microservice.ingest.messaging;

import datawave.microservice.ingest.configuration.IngestProperties;
import org.apache.hadoop.conf.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class BasicInputMessageTest {

    private IngestProperties properties;
    private Configuration conf;
    private BasicInputMessage basicInputMessage;

    @BeforeEach
    public void setUp() {
        properties = mock(IngestProperties.class);
        conf = new Configuration();
        basicInputMessage = new BasicInputMessage(properties, conf);
    }

    @Test
    void testParseValidMessage() throws IOException {
        String validMessage = "/home,org.apache.hadoop.mapreduce.lib.input.TextInputFormat,TestDataName";
        basicInputMessage.setMessage(validMessage);

        assertNotNull(basicInputMessage.getSplit());
        assertNotNull(basicInputMessage.getRecordReader());
        assertEquals("TestDataName", basicInputMessage.getDataName());
    }

    @Test
    void testParseInvalidMessage() {
        String invalidMessage = "InvalidFormatMessage";
        basicInputMessage.setMessage(invalidMessage);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            basicInputMessage.getSplit();
        });

        String expectedMessage = "Unexpected message format";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }
}
