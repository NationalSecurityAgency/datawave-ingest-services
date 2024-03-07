package datawave.microservice.ingest.configurtion;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.hibernate.validator.internal.util.Contracts.assertTrue;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import datawave.microservice.config.accumulo.AccumuloProperties;
import datawave.microservice.ingest.configuration.IngestProperties;

@ExtendWith(MockitoExtension.class)
class IngestPropertiesTest {
    
    @Mock
    private AccumuloProperties mockAccumuloProperties;
    
    @Test
    void testFsConfigResourcesSetterGetter() {
        IngestProperties properties = new IngestProperties();
        List<String> fsConfigResources = Arrays.asList("/path/to/config1", "/pth/two/config7","/root/stuff/config");
        properties.setFsConfigResources(fsConfigResources);
        assertEquals(fsConfigResources, properties.getFsConfigResources(), "FsConfigResources should match the input list.");
    }
    
    @Test
    void testResourceDirPatternsSetterGetter() {
        IngestProperties properties = new IngestProperties();
        List<String> resourceDirPatterns = Arrays.asList("dir1/*", "dir2/*");
        properties.setResourceDirPatterns(resourceDirPatterns);
        assertEquals(resourceDirPatterns, properties.getResourceDirPatterns(), "ResourceDirPatterns should match the input list.");
    }
    
    @Test
    void testLiveIngestSetterGetter() {
        IngestProperties properties = new IngestProperties();
        properties.setLiveIngest(true);
        assertTrue(properties.isLiveIngest(), "LiveIngest should be true.");
        properties.setLiveIngest(false);
        assertFalse(properties.isLiveIngest(), "LiveIngest should be false.");
    }
    
    @Test
    void testAccumuloPropertiesSetterGetter() {
        IngestProperties properties = new IngestProperties();
        properties.setAccumuloProperties(mockAccumuloProperties);
        assertNotNull(properties.getAccumuloProperties(), "AccumuloProperties should not be null.");
        assertEquals(mockAccumuloProperties, properties.getAccumuloProperties(), "AccumuloProperties should match the mocked object.");
    }
    
    @Test
    void fsConfigResourcesWithNull() {
        IngestProperties properties = new IngestProperties();
        properties.setFsConfigResources(null);
        assertNull(properties.getFsConfigResources(), "FsConfigResources should accept null.");
    }
    
    @Test
    void fsConfigResourcesWithEmptyList() {
        IngestProperties properties = new IngestProperties();
        properties.setFsConfigResources(Collections.emptyList());
        assertTrue(properties.getFsConfigResources().isEmpty(), "FsConfigResources should be empty.");
    }
    
    @Test
    void resourceDirPatternsWithEmptyList() {
        IngestProperties properties = new IngestProperties();
        properties.setResourceDirPatterns(Collections.emptyList());
        assertTrue(properties.getResourceDirPatterns().isEmpty(), "ResourceDirPatterns should be empty.");
    }
    
    @Test
    void fsConfigResourcesWithLargeList() {
        IngestProperties properties = new IngestProperties();
        List<String> largeList = Collections.nCopies(10000, "/path/to/config");
        properties.setFsConfigResources(largeList);
        assertEquals(largeList.size(), properties.getFsConfigResources().size(), "FsConfigResources should support large lists.");
    }
    
    @Test
    void resourceDirPatternsWithSpecialCharacters() {
        IngestProperties properties = new IngestProperties();
        List<String> specialCharsList = List.of("*?[]");
        properties.setResourceDirPatterns(specialCharsList);
        assertEquals(specialCharsList, properties.getResourceDirPatterns(), "ResourceDirPatterns should accept special characters.");
    }
    
    @Test
    void accumuloPropertiesWithNull() {
        IngestProperties properties = new IngestProperties();
        properties.setAccumuloProperties(null);
        //Should probably have an error if the accumuloProperties are set to null.
        assertNotNull(properties);
    }
    
    @Test
    void liveIngestDefaultValue() {
        IngestProperties properties = new IngestProperties();
        assertFalse(properties.isLiveIngest(), "Default value of liveIngest should be false.");
    }
    
    @Test
    void accumuloPropertiesNotNullAfterSet() {
        IngestProperties properties = new IngestProperties();
        AccumuloProperties accumuloProperties = new AccumuloProperties();
        properties.setAccumuloProperties(accumuloProperties);
        assertNotNull(properties.getAccumuloProperties(), "AccumuloProperties should not be null after being set.");
    }
}
