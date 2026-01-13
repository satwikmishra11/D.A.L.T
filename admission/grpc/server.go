package grpcserver

import (
	"context"
	"time"

	"admission/audit"
	"admission/dedupe"
	"admission/observability"
	"admission/policy"
	"admission/ratelimit"
	"admission/shed"
	"admission/state"

	pb "admission/proto"
	"go.uber.org/zap"
	"google.golang.org/grpc"
	"google.golang.org/grpc/health"
	healthpb "google.golang.org/grpc/health/grpc_health_v1"
)

type Server struct {
	pb.UnimplementedAdmissionServiceServer
	engine *policy.Engine
	dedupe *dedupe.Idempotency
	shed   *shed.Shedder
	limit  *ratelimit.Limiter
	health *health.Server
}

func NewServer(store state.Store) *Server {
	// Try to get Redis client for rate limiter
	var limiter *ratelimit.Limiter
	if redisStore, ok := store.(*state.RedisStore); ok {
		limiter = ratelimit.New(redisStore.Client())
	} else {
		// In production, we'd probably want to fail or use a backup.
		// For now, if no Redis, we might panic or skip rate limiting (unsafe).
		// We choose panic to ensure config is correct.
		panic("RedisStore required for Rate Limiter")
	}

	srv := &Server{
		engine: policy.NewEngine(
			policy.EnforceQuota(),
			policy.MaxDuration(3600),
		),
		dedupe: dedupe.New(store),
		shed:   shed.New(store),
		limit:  limiter,
		health: health.NewServer(),
	}
	
	// Set serving status
	srv.health.SetServingStatus("", healthpb.HealthCheckResponse_SERVING)
	srv.health.SetServingStatus("admission.AdmissionService", healthpb.HealthCheckResponse_SERVING)
	
	return srv
}

// RegisterHealthServer registers the health server to the grpc server
func (s *Server) RegisterHealthServer(grpcServer *grpc.Server) {
	healthpb.RegisterHealthServer(grpcServer, s.health)
}

func (s *Server) ValidateExecution(
	ctx context.Context,
	req *pb.ExecutionRequest,
) (*pb.ExecutionResponse, error) {

	ctx, cancel := context.WithTimeout(ctx, 2*time.Second)
	defer cancel()

	if s.dedupe.Seen(ctx, req.RequestId) {
		return &pb.ExecutionResponse{Allowed: true}, nil
	}

	if !s.shed.Enter(ctx) {
		observability.PolicyDeniedTotal.WithLabelValues("overload").Inc()
		return &pb.ExecutionResponse{
			Allowed: false,
			Reason:  "system overloaded",
		}, nil
	}
	defer s.shed.Exit(ctx)

	// Rate Limit: 100 reqs / minute per Org
	allowed, err := s.limit.Allow(ctx, req.OrgId, 100, time.Minute)
	if err != nil {
		observability.Error("rate_limit_error", zap.Error(err))
		return &pb.ExecutionResponse{
			Allowed: false,
			Reason:  "rate limit error",
		}, nil
	}
	
	if !allowed {
		observability.PolicyDeniedTotal.WithLabelValues("ratelimit").Inc()
		return &pb.ExecutionResponse{
			Allowed: false,
			Reason:  "rate limit exceeded",
		}, nil
	}

	err = s.engine.Evaluate(policy.Context{
		OrgID:    req.OrgId,
		Users:   req.Users,
		Duration: req.Duration,
	})

	if err != nil {
		observability.PolicyDeniedTotal.WithLabelValues("policy").Inc()
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
