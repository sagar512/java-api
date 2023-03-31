package com.peopleapp.service;

import com.peopleapp.dto.requestresponsedto.DeleteAccountRequest;

public interface UserAccountMgmtService {

    void deleteUserAccount(DeleteAccountRequest deleteAccountRequest);

    void suspendAccount();

    void logout();
}
