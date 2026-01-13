package observability

import (
	"context"

	"go.uber.org/zap"
)

// Field creates a zap.Field for structured logging
func Field(key string, val interface{}) zap.Field {
	return zap.Any(key, val)
}
