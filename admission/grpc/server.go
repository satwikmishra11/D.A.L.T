package grpcserver

import (
	"context"

	"admission/audit"
	"admission/dedupe"
	"admission/observability"
	"admission/policy"
	"admission/ratelimit"
	"admission/shed"
	"admission/state"

	pb "admission/proto"
)

type Server struct {
	pb.UnimplementedAdmissionServiceServer
	engine *policy.Engine
	dedupe *dedupe.Idempotency
	shed   *shed.Shedder
	limit  *ratelimit.Limiter
}

func NewServer(store state.Store) *Server {
	return &Server{
		engine: policy.NewEngine(
			policy.EnforceQuota(),
			policy.MaxDuration(3600),
		),
		dedupe: dedupe.New(store),
		shed:   shed.New(store),
		limit:  ratelimit.New(store, 50),
	}
}

func (s *Server) ValidateExecution(
	ctx context.Context,
	req *pb.ExecutionRequest,
) (*pb.ExecutionResponse, error) {

	ctx, cancel := context.WithTimeout(ctx, 2*time.Second)
	defer cancel()

	defer func() {
		if r := recover(); r != nil {
			observability.Error("panic_recovered", map[string]interface{}{
				"panic": r,
			})
		}
	}()

	observability.Total.Add(1)

	if s.dedupe.Seen(ctx, req.RequestId) {
		return &pb.ExecutionResponse{Allowed: true}, nil
	}

	if !s.shed.Enter(ctx) {
		observability.Denied.Add(1)
		return &pb.ExecutionResponse{
			Allowed: false,
			Reason:  "system overloaded",
		}, nil
	}
	defer s.shed.Exit(ctx)

	if !s.limit.Allow(ctx, req.OrgId) {
		observability.Denied.Add(1)
		return &pb.ExecutionResponse{
			Allowed: false,
			Reason:  "rate limit exceeded",
		}, nil
	}
	defer s.limit.Release(ctx, req.OrgId)

	err := s.engine.Evaluate(policy.Context{
		OrgID:    req.OrgId,
		Users:   req.Users,
		Duration: req.Duration,
	})

	if err != nil {
		observability.Denied.Add(1)
		audit.Record(req.OrgId, "DENIED", err.Error())
		return &pb.ExecutionResponse{
			Allowed: false,
			Reason:  err.Error(),
		}, nil
	}

	s.dedupe.Mark(ctx, req.RequestId)
	audit.Record(req.OrgId, "ALLOWED", "")
	return &pb.ExecutionResponse{Allowed: true}, nil
}
