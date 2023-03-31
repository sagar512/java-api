package com.peopleapp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.peopleapp.enums.ReportDataType;
import lombok.Data;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "reportedUser")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReportedData {

    @Id
    @Field(value = "_id")
    private ObjectId id;

    private ObjectId reportedByUserId;

    private ObjectId reportedUserId;

    private ObjectId reportedNetworkId;

    private ReportDataType reportDataType;

    private String reportMessage;

    private DateTime createdOn;

    public String getReportedUserId() {
        return reportedUserId.toString();
    }

    public void setReportedUserId(String reportedUserId) {
        this.reportedUserId = new ObjectId(reportedUserId);
    }

    public String getReportedByUserId() {
        return reportedByUserId.toString();
    }

    public void setReportedByUserId(String reportedByUserId) {
        this.reportedByUserId = new ObjectId(reportedByUserId);
    }


    public String getReportedNetworkId() {
        return reportedNetworkId.toString();
    }

    public void setReportedNetworkId(String reportedNetworkId) {
        this.reportedNetworkId = new ObjectId(reportedNetworkId);
    }


}
