package main

import (
	"log"
	"net"

	pb "control-plane-go/proto"
	grpcserver "control-plane-go/grpc"

	"google.golang.org/grpc"
)

func main() {
	lis, _ := net.Listen("tcp", ":9090")
	server := grpc.NewServer()

	pb.RegisterAdmissionServiceServer(server, &grpcserver.Server{})

	log.Println("Go Control Plane running on :9090")
	server.Serve(lis)
}
