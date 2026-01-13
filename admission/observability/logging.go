package observability

import (
	"context"
	"os"

	"go.uber.org/zap"
	"go.uber.org/zap/zapcore"
)

var Logger *zap.Logger

func init() {
	// Default config for development; can be enhanced based on environment
	config := zap.NewProductionEncoderConfig()
	config.EncodeTime = zapcore.ISO8601TimeEncoder
	
	// Create core
	encoder := zapcore.NewJSONEncoder(config)
	writer := zapcore.Lock(os.Stdout)
	
	// Default level info
	atom := zap.NewAtomicLevelAt(zap.InfoLevel)
	
	core := zapcore.NewCore(encoder, writer, atom)
	Logger = zap.New(core, zap.AddCaller())
}

// Info logs an info message
func Info(msg string, fields ...zap.Field) {
	Logger.Info(msg, fields...)
}

// Error logs an error message
func Error(msg string, fields ...zap.Field) {
	Logger.Error(msg, fields...)
}

// Debug logs a debug message
func Debug(msg string, fields ...zap.Field) {
	Logger.Debug(msg, fields...)
}

// Warn logs a warning message
func Warn(msg string, fields ...zap.Field) {
	Logger.Warn(msg, fields...)
}

// WithTrace adds trace_id to context (placeholder for actual tracing integration)
func WithTrace(ctx context.Context) context.Context {
	// In a real app, this would extract/inject OpenTelemetry trace IDs
	return ctx
}

// TraceID extracts trace_id from context
func TraceID(ctx context.Context) string {
	return "trace-id-placeholder" 
}

func Sync() {
	_ = Logger.Sync()
}