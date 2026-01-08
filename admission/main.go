package main

import (
	"crypto/tls"
	"crypto/x509"
	"io/ioutil"
	"log"
	"net"
	"os"

	grpcserver "admission/grpc"
	"admission/config"
	"admission/httpserver"
	"admission/lifecycle"
	"admission/observability"
	"admission/state"

	pb "admission/proto"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials"
)

func loadTLSConfig(cfg *config.Config) (credentials.TransportCredentials, error) {
	// Read server cert & key
	cert, err := tls.LoadX509KeyPair(cfg.TLS.ServerCert, cfg.TLS.ServerKey)
	if err != nil {
		return nil, err
	}

	// Read CA cert pool
	caCertPEM, err := ioutil.ReadFile(cfg.TLS.CACert)
	if err != nil {
		return nil, err
	}
	certPool := x509.NewCertPool()
	if !certPool.AppendCertsFromPEM(caCertPEM) {
		return nil, err
	}

	tlsCfg := &tls.Config{
		Certificates: []tls.Certificate{cert},
		ClientCAs:    certPool,
		MinVersion:   tls.VersionTLS12,
	}

	// client auth mode
	// default: require & verify client cert
	switch cfg.TLS.ClientAuth {
	case "NoClientCert":
		tlsCfg.ClientAuth = tls.NoClientCert
	case "RequireAndVerifyClientCert":
		tlsCfg.ClientAuth = tls.RequireAndVerifyClientCert
	default:
		tlsCfg.ClientAuth = tls.RequireAndVerifyClientCert
	}

	return credentials.NewTLS(tlsCfg), nil
}

func main() {
	cfg := config.Load()

	// Create Redis-backed store
	store := state.NewRedis(cfg.RedisAddr)
	// Wait for Redis readiness (existing)
	lifecycle.WaitForRedis(store)

	// HTTP endpoints
	httpserver.Start()
	observability.Handler()

	// gRPC server setup
	lis, err := net.Listen("tcp", ":"+cfg.GrpcPort)
	if err != nil {
		log.Fatalf("failed to listen: %v", err)
	}

	var opts []grpc.ServerOption
	// Enable TLS if configured
	if cfg.TLS.Enabled {
		creds, err := loadTLSConfig(cfg)
		if err != nil {
			log.Fatalf("failed to load TLS config: %v", err)
		}
		opts = append(opts, grpc.Creds(creds))
	}

	grpcSrv := grpc.NewServer(opts...)

	// register server
	pb.RegisterAdmissionServiceServer(
		grpcSrv,
		grpcserver.NewServer(store),
	)

	log.Printf("Admission service running on :%s (tls=%v)", cfg.GrpcPort, cfg.TLS.Enabled)

	// Serve asynchronously
	go func() {
		if err := grpcSrv.Serve(lis); err != nil {
			log.Fatalf("gRPC serve failed: %v", err)
		}
	}()

	// Graceful shutdown
	lifecycle.Wait()
	log.Println("Shutting down admission service")
	grpcSrv.GracefulStop()
}
