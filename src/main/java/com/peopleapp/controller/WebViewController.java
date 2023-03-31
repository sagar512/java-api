package com.peopleapp.controller;

import com.peopleapp.configuration.LocaleMessageReader;
import com.peopleapp.enums.MessageCodes;
import com.peopleapp.service.PeopleUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.inject.Inject;

@Controller
@RequestMapping(value = "/v1.0/webview/api")
@Api(value = "VerifyEmail", tags = "Email Verification")
public class WebViewController {

    @Inject
    private PeopleUserService peopleUserService;

    @Inject
    private LocaleMessageReader messages;

    private final Logger logger = LoggerFactory.getLogger(getClass());


    private static final String EXISTING_MAIL_ID = "verification-already-verified";
    private static final String EMAIL_VERIFIED_SUCCESSFULLY = "verification-success";
    private static final String EMAIL_VERIFICATION_LINK_EXPIRED = "verification-expired";


    @ApiOperation(
            value = "Email verification",
            notes = "<div>This API will verify the email given by the user.</div>\n" + "\n" +
                    "| Error Case                                                    | Error Code |\n" +
                    "| ------------------------------------------------------------- | ---------- |\n" +
                    "| If verification token has expired or token is invalid.        | 902        |\n" +
                    "| If email being verified is already linked to another account. | 805        |"
    )
    @GetMapping(
            value = "/email/verify"
    )
    public String verifyEmail(@RequestParam("token") String token) {

        try {

            logger.info( " Inside WebViewController -> Email verification ");
            peopleUserService.verifyEmail(token);

        } catch (Exception e) {

            if (e.getMessage().equals(MessageCodes.INVALID_VERIFICATION_TOKEN.getValue())) {
                return EMAIL_VERIFICATION_LINK_EXPIRED;
            }
            return EXISTING_MAIL_ID;
        }

        return EMAIL_VERIFIED_SUCCESSFULLY;
    }
}
