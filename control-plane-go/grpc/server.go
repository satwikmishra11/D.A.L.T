package grpcserver

import (
	"context"
	"control-plane-go/policy"
	pb "control-plane-go/proto"
)

type Server struct {
	pb.UnimplementedAdmissionServiceServer
}

func (s *Server) ValidateExecution(
	ctx context.Context,
	req *pb.ExecutionRequest,
) (*pb.ExecutionResponse, error) {

	err := policy.Validate(
		req.Users,
		req.Duration,
		req.ApprovalStatus,
	)

	if err != nil {
		return &pb.ExecutionResponse{
			Allowed: false,
			Reason:  err.Error(),
		}, nil
	}

	return &pb.ExecutionResponse{Allowed: true}, nil
}
