package state

import "context"

type Store interface {
	Get(ctx context.Context, key string) (string, bool)
	Set(ctx context.Context, key, value string)
	// Acquire and Release for distributed semaphore/limits
	Acquire(ctx context.Context, key string, limit int64) bool
	Release(ctx context.Context, key string)
}