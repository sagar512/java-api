package com.peopleapp.service;

import com.peopleapp.dto.GetTagsResponseDTO;
import com.peopleapp.model.PeopleUser;
import com.peopleapp.repository.PeopleUserRepository;
import com.peopleapp.repository.TagRepository;
import com.peopleapp.security.TokenAuthService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;

import static org.mockito.BDDMockito.given;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TagServiceTest extends BaseTest {
    @Inject
    private TagService tagService;

    @Inject
    private PeopleUserRepository peopleUserRepository;

    @Inject
    private TagRepository tagRepository;

    @MockBean
    private TokenAuthService tokenAuthService;

    private PeopleUser user1;

    @Before
    public void setUp() {

        user1 = peopleUserRepository.save(MethodStubs.getUserObjectWithTags("9000000001", "testuser1"));

    }

    /**
     * Method: getSuggestedTagList
     * Test Case: Success
     */
    @Test
    public void testMergeSharedInfoToStaticInfo(){
        given(this.tokenAuthService.getSessionUser()).willReturn(user1);

        GetTagsResponseDTO responseDTO = tagService.getSuggestedTagList();

        Assert.assertNotNull("Success - TagList is present", responseDTO.getTagList());
    }

    @After
    public void tearDown() {
        peopleUserRepository.deleteAll();
        tagRepository.deleteAll();
    }
}
