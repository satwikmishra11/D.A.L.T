package grpcserver

import (
	"context"
	"time"

	"admission/observability"
	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
	"go.uber.org/zap"
)

// Interceptor chain
func UnaryInterceptor() grpc.UnaryServerInterceptor {
	return func(
		ctx context.Context,
		req interface{},
		info *grpc.UnaryServerInfo,
		handler grpc.UnaryHandler,
	) (resp interface{}, err error) {
		start := time.Now()
		
		// Recovery logic
		defer func() {
			if r := recover(); r != nil {
				observability.Error("grpc_panic_recovered", zap.Any("panic", r))
				err = status.Errorf(codes.Internal, "internal server error")
			}
		}()

		// Process request
		resp, err = handler(ctx, req)

		// Metrics
		duration := time.Since(start).Seconds()
		statusCode := "OK"
		if err != nil {
			if s, ok := status.FromError(err); ok {
				statusCode = s.Code().String()
			} else {
				statusCode = "UNKNOWN"
			}
		}

		observability.RequestsTotal.WithLabelValues(info.FullMethod, statusCode).Inc()
		observability.RequestDuration.WithLabelValues(info.FullMethod).Observe(duration)

		// Logging
		logFields := []zap.Field{
			zap.String("method", info.FullMethod),
			zap.String("status", statusCode),
			zap.Float64("duration", duration),
		}
		
		if err != nil {
			logFields = append(logFields, zap.Error(err))
			observability.Error("grpc_request_failed", logFields...)
		} else {
			observability.Info("grpc_request_success", logFields...)
		}

		return resp, err
	}
}