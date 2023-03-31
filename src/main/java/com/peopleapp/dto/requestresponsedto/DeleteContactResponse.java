package com.peopleapp.dto.requestresponsedto;

import lombok.Data;

import java.util.List;

@Data
public class DeleteContactResponse {

    private List<String> deletedConnectionIdList;
}
