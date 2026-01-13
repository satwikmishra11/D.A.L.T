package ratelimit

import (
	"context"
	"fmt"
	"time"

	"github.com/redis/go-redis/v9"
)

// Lua script for a fixed-window counter with automatic expiration
var allowScript = redis.NewScript(`
local key = KEYS[1]
local limit = tonumber(ARGV[1])
local window = tonumber(ARGV[2])

local current = redis.call("INCR", key)
if current == 1 then
    redis.call("EXPIRE", key, window)
end

if current > limit then
    return 0 // denied
else
    return 1 // allowed
end
`)

type Limiter struct {
	client *redis.Client
}

func New(client *redis.Client) *Limiter {
	return &Limiter{
		client: client,
	}
}

// Allow checks if the request is allowed within the given window.
// key: unique identifier (e.g., user_id, ip)
// limit: max requests
// window: time duration
func (l *Limiter) Allow(ctx context.Context, key string, limit int64, window time.Duration) (bool, error) {
	// Key includes window to allow changing window size without collision if needed,
	// though usually just the identifier is enough if window is static.
	// We'll construct a key based on the identifier and the current time window bucket
	// if we wanted sliding window, but for Fixed Window (Simple), we just use the key.
	// The Lua script handles the expiration.
	
	redisKey := fmt.Sprintf("ratelimit:%s", key)
	windowSecs := int64(window.Seconds())
	if windowSecs < 1 {
		windowSecs = 1
	}

	result, err := allowScript.Run(ctx, l.client, []string{redisKey}, limit, windowSecs).Result()
	if err != nil {
		return false, err
	}

	return result.(int64) == 1, nil
}
