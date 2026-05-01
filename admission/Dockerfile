FROM golang:1.21-alpine AS builder
WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN go build -o admission-controller main.go

FROM alpine:latest
WORKDIR /app
COPY --from=builder /app/admission-controller .
EXPOSE 9090 8081 9091
CMD ["./admission-controller"]
