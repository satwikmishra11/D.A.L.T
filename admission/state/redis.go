package state

import (
	"context"

	"admission/config"
	"github.com/redis/go-redis/v9"
	"time"
)

type RedisStore struct {
	rdb *redis.Client
}

func NewRedis(cfg *config.Config) *RedisStore {
	return &RedisStore{
		rdb: redis.NewClient(&redis.Options{
			Addr:         cfg.RedisAddr,
			PoolSize:     cfg.RedisPoolSize,
			DialTimeout:  time.Duration(cfg.RedisDialTimeout) * time.Millisecond,
			ReadTimeout:  time.Duration(cfg.RedisReadTimeout) * time.Millisecond,
			WriteTimeout: time.Duration(cfg.RedisReadTimeout) * time.Millisecond,
		}),
	}
}

func (r *RedisStore) Get(ctx context.Context, key string) (string, bool) {
	val, err := r.rdb.Get(ctx, key).Result()
	if err != nil {
		return "", false
	}
	return val, true
}

func (r *RedisStore) Set(ctx context.Context, key, value string) {
	r.rdb.Set(ctx, key, value, 0)
}

func (r *RedisStore) Client() *redis.Client {
	return r.rdb
}

/*
Distributed semaphore using INCR/DECR
*/
func (r *RedisStore) Acquire(
	ctx context.Context,
	key string,
	limit int64,
) bool {
	count, err := r.rdb.Incr(ctx, key).Result()
	if err != nil || count > limit {
		r.rdb.Decr(ctx, key)
		return false
	}
	return true
}

func (r *RedisStore) Release(ctx context.Context, key string) {
	r.rdb.Decr(ctx, key)
}
