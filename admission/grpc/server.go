package grpcserver

import (
	"context"

	"control-plane-go/audit"
	"control-plane-go/policy"
	"control-plane-go/ratelimit"

	pb "control-plane-go/proto"
)

type Server struct {
	pb.UnimplementedAdmissionServiceServer
	engine  *policy.Engine
	limiter *ratelimit.Limiter
}

func NewServer() *Server {
	engine := policy.NewEngine(
		policy.EnforceUserQuota(),
		policy.MaxDuration(3600),
	)
	return &Server{
		engine:  engine,
		limiter: ratelimit.New(10),
	}
}


func (s *Server) ValidateExecution(
	ctx context.Context,
	req *pb.ExecutionRequest,
) (*pb.ExecutionResponse, error) {

	observability.IncTotal()

	if deduper.Seen(req.RequestId) {
		return &pb.ExecutionResponse{Allowed: true}, nil
	}

	if !shed.Allow() {
		observability.IncDenied()
		return &pb.ExecutionResponse{
			Allowed: false,
			Reason:  "system overloaded",
		}, nil
	}

	shed.Enter()
	defer shed.Exit()

	err := s.engine.Evaluate(policy.Context{
		OrgID:    req.OrgId,
		Users:   req.Users,
		Duration: req.Duration,
	})

	if err != nil {
		observability.IncDenied()
		return &pb.ExecutionResponse{
			Allowed: false,
			Reason:  err.Error(),
		}, nil
	}

	deduper.Mark(req.RequestId)
	return &pb.ExecutionResponse{Allowed: true}, nil
}
