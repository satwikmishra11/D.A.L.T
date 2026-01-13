package state

import (
	"context"

	"github.com/redis/go-redis/v9"
)

type RedisStore struct {
	rdb *redis.Client
}

func NewRedis(addr string) *RedisStore {
	return &RedisStore{
		rdb: redis.NewClient(&redis.Options{
			Addr: addr,
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
