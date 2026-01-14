package com.loadtest.service;

import admission.AdmissionServiceGrpc;
import admission.Admission.ExecutionRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.annotation.PreDestroy;

@Service
public class AdmissionClient {

    private final AdmissionServiceGrpc.AdmissionServiceBlockingStub stub;
    private final ManagedChannel channel;

    public AdmissionClient(
            @Value("${loadtest.admission.host}") String host,
            @Value("${loadtest.admission.port}") int port
    ) {
        this.channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();

        this.stub = AdmissionServiceGrpc.newBlockingStub(channel);
    }

    @CircuitBreaker(name = "admission")
    @Retry(name = "admission")
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

    @PreDestroy
    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }
}
