package com.peopleapp.service;

import com.peopleapp.dto.requestresponsedto.SystemDataResponseDTO;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SystemDataServiceTest extends BaseTest {

    @Inject
    private SystemDataService systemDataService;

    @Before
    public void setUp() {
    }

    /**
     * Method - getSystemData
     * TestCase - Success
     * Fetches System Defined Data
     */
    @Test
    public void testGetSystemData() {
        SystemDataResponseDTO responseDTO = systemDataService.getSystemData();
        Assert.assertNotNull("Success - Labels are present", responseDTO.getLabels());
        Assert.assertNotNull("Success - Network Categories are present", responseDTO.getNetworkCategories());
        Assert.assertNotNull("Success - Tags are present", responseDTO.getTagList());
    }

    @After
    public void tearDown() {
    }
}
