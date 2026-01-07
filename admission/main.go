package main

import (
	"log"
	"net"
	"control-plane-go/httpserver"

	pb "control-plane-go/proto"
	grpcserver "control-plane-go/grpc"

	"google.golang.org/grpc"
)

func main() {
	// Start health & readiness endpoints
	httpserver.StartHealthServer()

	// Start gRPC server
	lis, err := net.Listen("tcp", ":9090")
	if err != nil {
		log.Fatalf("failed to listen: %v", err)
	}

	grpcSrv := grpc.NewServer()

	pb.RegisterAdmissionServiceServer(
		grpcSrv,
		grpcserver.NewServer(), // alias now resolves correctly
	)

	log.Println("Go Control Plane running on :9090")
	if err := grpcSrv.Serve(lis); err != nil {
		log.Fatalf("failed to serve gRPC: %v", err)
	}
}

func main() {
	leadership.StartElection()

	ctx := context.Background()

	go httpserver.StartHealthServer()

	go func() {
		lifecycle.Wait(ctx)
		log.Println("Graceful shutdown complete")
		os.Exit(0)
	}()

	startGrpc()
}