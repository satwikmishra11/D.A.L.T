package observability

import (
	"context"

	"github.com/google/uuid"
)

type traceKeyType string

const traceKey traceKeyType = "trace_id"

func WithTrace(ctx context.Context) context.Context {
	return context.WithValue(ctx, traceKey, uuid.NewString())
}

func TraceID(ctx context.Context) string {
	if v := ctx.Value(traceKey); v != nil {
		return v.(string)
	}
	return ""
}
