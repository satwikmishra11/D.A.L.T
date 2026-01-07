package httpserver

import (
	"net/http"
)

func StartHealthServer() {
	http.HandleFunc("/healthz", func(w http.ResponseWriter, _ *http.Request) {
		w.Write([]byte("ok"))
	})

	http.HandleFunc("/readyz", func(w http.ResponseWriter, _ *http.Request) {
		w.Write([]byte("ready"))
	})

	go http.ListenAndServe(":8081", nil)
}
