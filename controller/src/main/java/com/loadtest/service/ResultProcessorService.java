webClient.post()
    .uri("http://analytics-python:8000/analyze/latency")
    .bodyValue(Map.of("latencies", latencies))
    .retrieve()
    .bodyToMono(Map.class)
    .subscribe(result -> alertService.processAnomalies(result));
