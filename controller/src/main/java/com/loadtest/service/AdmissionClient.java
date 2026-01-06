package com.loadtest.service;

import admission.AdmissionServiceGrpc;
import admission.Admission.ExecutionRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.stereotype.Service;

@Service
public class AdmissionClient {

    private final AdmissionServiceGrpc.AdmissionServiceBlockingStub stub;

    public AdmissionClient() {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("control-plane-go", 9090)
                .usePlaintext()
                .build();

        stub = AdmissionServiceGrpc.newBlockingStub(channel);
    }

    public void validate(String scenarioId, int users, int duration, String status) {
        var response = stub.validateExecution(
            ExecutionRequest.newBuilder()
                .setScenarioId(scenarioId)
                .setUsers(users)
                .setDuration(duration)
                .setApprovalStatus(status)
                .build()
        );

        if (!response.getAllowed()) {
            throw new RuntimeException(response.getReason());
        }
    }
}
