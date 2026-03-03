from pydantic import BaseModel
import os


class Settings(BaseModel):
    db_url: str = os.getenv("VEILTECH_DB_URL", "mysql+pymysql://veiltech:veiltech123@localhost:3306/veiltech")
    jwt_secret: str = os.getenv("VEILTECH_JWT_SECRET", "change-this-to-a-long-secret-key-for-demo")
    jwt_exp_minutes: int = int(os.getenv("VEILTECH_JWT_EXP_MINUTES", "1440"))
    uploads_root: str = os.getenv("VEILTECH_UPLOADS_ROOT", "/var/veiltech/uploads")


settings = Settings()
