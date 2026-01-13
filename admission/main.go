package main

import (
	"crypto/tls"
	"crypto/x509"
	"io/ioutil"
	"net"
	"net/http"
	"os"
	"os/signal"
	"syscall"

	_ "net/http/pprof" // Profiling

	grpcserver "admission/grpc"
	"admission/config"
	"admission/httpserver"
	"admission/lifecycle"
	"admission/observability"
	"admission/state"

	pb "admission/proto"
	"go.uber.org/zap"
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
	defer observability.Sync()

	cfg := config.Load()
	observability.Info("Starting admission service", 
		zap.String("version", cfg.Version),
		zap.String("env", cfg.Environment),
	)

	store := state.NewRedis(cfg.RedisAddr)
	lifecycle.WaitForRedis(store)

	// Observability Server (Metrics + Pprof)
	go func() {
		// Default mux has pprof registered
		mux := http.DefaultServeMux 
		mux.Handle("/metrics", observability.Handler())
		
		observability.Info("Observability server listening", zap.String("port", "9091"))
		if err := http.ListenAndServe(":9091", mux); err != nil {
			observability.Error("Observability server failed", zap.Error(err))
		}
	}()

	httpserver.Start()

	lis, err := net.Listen("tcp", ":"+cfg.GrpcPort)
	if err != nil {
		observability.Error("Failed to listen for gRPC", zap.Error(err))
		os.Exit(1)
	}

	var opts []grpc.ServerOption
	opts = append(opts, grpc.UnaryInterceptor(grpcserver.UnaryInterceptor()))

	if cfg.TLS.Enabled {
		creds, err := loadTLSConfig(cfg)
		if err != nil {
			observability.Error("Failed to load TLS config", zap.Error(err))
			os.Exit(1)
		}
		opts = append(opts, grpc.Creds(creds))
	}

	grpcSrv := grpc.NewServer(opts...)
	
	// Create implementation
	srv := grpcserver.NewServer(store)

	// Register services
	pb.RegisterAdmissionServiceServer(grpcSrv, srv)
	srv.RegisterHealthServer(grpcSrv)

	observability.Info("Admission gRPC service running", zap.String("port", cfg.GrpcPort), zap.Bool("tls", cfg.TLS.Enabled))

	go func() {
		if err := grpcSrv.Serve(lis); err != nil {
			observability.Error("gRPC serve failed", zap.Error(err))
			os.Exit(1)
		}
	}()

	stop := make(chan os.Signal, 1)
	signal.Notify(stop, syscall.SIGINT, syscall.SIGTERM)
	
	<-stop
	observability.Info("Shutting down admission service...")
	
	grpcSrv.GracefulStop()
	lifecycle.Wait()
	
	observability.Info("Shutdown complete")
}
