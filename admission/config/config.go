package config

import (
	"os"
	"strconv"
)

type Config struct {
	GrpcPort     string
	HttpPort     string
	RedisAddr    string
	MaxInflight  int64
	RateLimit    int64
	Environment  string
	Version      string

	TLS struct {
		Enabled      bool
		ServerCert   string
		ServerKey    string
		CACert       string
		ClientAuth   string // "RequireAndVerifyClientCert" or "NoClientCert"
	}
}

func Load() *Config {
	cfg := &Config{
		GrpcPort:    get("GRPC_PORT", "9090"),
		HttpPort:    get("HTTP_PORT", "8081"),
		RedisAddr:   must("REDIS_ADDR"),
		MaxInflight: getInt("MAX_INFLIGHT", 500),
		RateLimit:   getInt("RATE_LIMIT", 50),
		Environment: get("ENV", "dev"),
		Version:     get("VERSION", "unknown"),
	}

	// TLS settings
	tlsEnabled := get("TLS_ENABLED", "true")
	if tlsEnabled == "true" {
		cfg.TLS.Enabled = true
		cfg.TLS.ServerCert = must("TLS_SERVER_CERT") // path inside container e.g. /etc/tls/server.crt
		cfg.TLS.ServerKey = must("TLS_SERVER_KEY")   // e.g. /etc/tls/server.key
		cfg.TLS.CACert = must("TLS_CA_CERT")         // e.g. /etc/tls/ca.crt
		cfg.TLS.ClientAuth = get("TLS_CLIENT_AUTH", "RequireAndVerifyClientCert")
	}
	return cfg
}

func get(key, def string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return def
}

func must(key string) string {
	v := os.Getenv(key)
	if v == "" {
		panic("missing env var: " + key)
	}
	return v
}

func getInt(key string, def int64) int64 {
	if v := os.Getenv(key); v != "" {
		i, _ := strconv.ParseInt(v, 10, 64)
		return i
	}
	return def
}
