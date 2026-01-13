package observability

import (
	"net/http"

	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promhttp"
)

var (
	RequestsTotal = prometheus.NewCounterVec(
		prometheus.CounterOpts{
			Name: "admission_requests_total",
			Help: "Total number of admission requests",
		},
		[]string{"method", "status"},
	)

	RequestDuration = prometheus.NewHistogramVec(
		prometheus.HistogramOpts{
			Name:    "admission_request_duration_seconds",
			Help:    "Duration of admission requests",
			Buckets: prometheus.DefBuckets,
		},
		[]string{"method"},
	)
	
	PolicyDeniedTotal = prometheus.NewCounterVec(
		prometheus.CounterOpts{
			Name: "admission_policy_denied_total",
			Help: "Total number of requests denied by policy/limits",
		},
		[]string{"reason"},
	)
)

func init() {
	prometheus.MustRegister(RequestsTotal)
	prometheus.MustRegister(RequestDuration)
	prometheus.MustRegister(PolicyDeniedTotal)
}

// Handler returns the HTTP handler for Prometheus metrics
func Handler() http.Handler {
	return promhttp.Handler()
}
