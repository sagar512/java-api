package com.peopleapp.service;

import com.peopleapp.dto.SQSPayload;

import java.util.List;

public interface QueueService {

    void sendPayloadToSQS(List<SQSPayload> payloadList);

    void sendPayloadToSQS(SQSPayload payload);
}
