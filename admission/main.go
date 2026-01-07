package main

import (
	"log"
	"net"
	"os"

	grpcserver "admission/grpc"
	"admission/httpserver"
	"admission/lifecycle"
	"admission/observability"
	"admission/state"

	pb "admission/proto"
	"google.golang.org/grpc"
)

func main() {
	// =========================
	// Redis (distributed state)
	// =========================
	redisAddr := os.Getenv("REDIS_ADDR")
	if redisAddr == "" {
		log.Fatal("REDIS_ADDR environment variable is not set")
	}

	store := state.NewRedis(redisAddr)

	// =========================
	// HTTP endpoints
	// =========================
	// /healthz, /readyz
	httpserver.Start()

	// /metrics (Prometheus)
	observability.Handler()

	// =========================
	// gRPC server
	// =========================
	listener, err := net.Listen("tcp", ":9090")
	if err != nil {
		log.Fatalf("failed to listen on port 9090: %v", err)
	}

	grpcServer := grpc.NewServer()

	pb.RegisterAdmissionServiceServer(
		grpcServer,
		grpcserver.NewServer(store),
	)

	log.Println("Admission service running on :9090")

	// =========================
	// Serve gRPC asynchronously
	// =========================
	go func() {
		if err := grpcServer.Serve(listener); err != nil {
			log.Fatalf("failed to serve gRPC: %v", err)
		}
	}()

	// =========================
	// Graceful shutdown
	// =========================
	lifecycle.Wait()
	log.Println("Shutting down admission service")

	grpcServer.GracefulStop()
}
