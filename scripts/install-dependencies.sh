#!/bin/bash
set -e

echo "Installing LoadTest Platform Dependencies..."

# Check for required tools
command -v docker >/dev/null 2>&1 || { echo "Docker is required but not installed. Aborting." >&2; exit 1; }
command -v docker-compose >/dev/null 2>&1 || { echo "Docker Compose is required but not installed. Aborting." >&2; exit 1; }

# Backend dependencies
echo "Setting up backend..."
cd controller
if command -v mvn >/dev/null 2>&1; then
    mvn dependency:go-offline
    echo "✓ Backend dependencies cached"
else
    echo "⚠ Maven not found - skipping backend dependency cache"
fi
cd ..

# Worker dependencies
echo "Setting up worker..."
cd worker
if command -v cargo >/dev/null 2>&1; then
    cargo fetch
    echo "✓ Worker dependencies cached"
else
    echo "⚠ Cargo not found - skipping worker dependency cache"
fi
cd ..

# Frontend dependencies
echo "Setting up frontend..."
cd frontend
if command -v npm >/dev/null 2>&1; then
    npm install
    echo "✓ Frontend dependencies installed"
else
    echo "⚠ npm not found - skipping frontend dependencies"
fi
cd ..

echo ""
echo "✓ Installation complete!"
echo ""
echo "Next steps:"
echo "  1. Copy .env.example to .env and configure"
echo "  2. Run 'make build' to build all images"
echo "  3. Run 'make run' to start the platform"