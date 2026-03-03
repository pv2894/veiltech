import enum
from datetime import datetime
from sqlalchemy import String, DateTime, ForeignKey, Enum, Boolean, Integer
from sqlalchemy.orm import Mapped, mapped_column, relationship
from .db import Base


class RequestStatus(str, enum.Enum):
    PENDING = "PENDING"
    ACCEPTED = "ACCEPTED"
    EXPIRED = "EXPIRED"


class User(Base):
    __tablename__ = "users"

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    name: Mapped[str] = mapped_column(String(100), nullable=False)
    phone: Mapped[str] = mapped_column(String(20), unique=True, index=True, nullable=False)
    password_hash: Mapped[str] = mapped_column(String(255), nullable=False)


class ConnectionRequest(Base):
    __tablename__ = "requests"

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    sender_id: Mapped[int] = mapped_column(ForeignKey("users.id"), nullable=False)
    receiver_id: Mapped[int] = mapped_column(ForeignKey("users.id"), nullable=False)
    status: Mapped[RequestStatus] = mapped_column(Enum(RequestStatus), nullable=False)
    expiry_time: Mapped[datetime] = mapped_column(DateTime, nullable=False)

    sender = relationship("User", foreign_keys=[sender_id])
    receiver = relationship("User", foreign_keys=[receiver_id])


class Session(Base):
    __tablename__ = "sessions"

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    request_id: Mapped[int] = mapped_column(ForeignKey("requests.id"), nullable=False)
    masked_file_path: Mapped[str] = mapped_column(String(500), nullable=False)
    encrypted_file_path: Mapped[str] = mapped_column(String(500), nullable=False)
    pin_hash: Mapped[str] = mapped_column(String(255), nullable=False)
    expiry_time: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    failed_attempts: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    locked: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)

    request = relationship("ConnectionRequest")
