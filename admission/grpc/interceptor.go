package grpcserver

import (
	"context"
	"time"

	"admission/observability"
	"google.golang.org/grpc"
)

func UnaryInterceptor() grpc.UnaryServerInterceptor {
	return func(
		ctx context.Context,
		req interface{},
		info *grpc.UnaryServerInfo,
		handler grpc.UnaryHandler,
	) (interface{}, error) {

		ctx = observability.WithTrace(ctx)
		start := time.Now()

		resp, err := handler(ctx, req)

		observability.Info("grpc_request", map[string]interface{}{
			"method":   info.FullMethod,
			"trace_id": observability.TraceID(ctx),
			"duration": time.Since(start).Milliseconds(),
			"error":    err != nil,
		})

		return resp, err
	}
}
