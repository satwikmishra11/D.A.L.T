from fastapi import FastAPI
from prometheus_fastapi_instrumentator import Instrumentator
from app.api.endpoints import router as api_router
from app.core.config import settings

def create_app() -> FastAPI:
    app = FastAPI(
        title=settings.PROJECT_NAME,
        version=settings.VERSION,
        openapi_url=f"{settings.API_V1_STR}/openapi.json"
    )

    # Register Routes
    app.include_router(api_router, prefix=settings.API_V1_STR)

    # Observability
    Instrumentator().instrument(app).expose(app)

    return app

app = create_app()
