from datetime import datetime
from pydantic import BaseModel, Field
from .models import RequestStatus


class RegisterRequest(BaseModel):
    name: str
    phone: str
    passwordHash: str


class LoginRequest(BaseModel):
    phone: str
    passwordHash: str


class AuthResponse(BaseModel):
    token: str
    userId: int
    name: str


class UserOut(BaseModel):
    id: int
    name: str
    phone: str


class CreateRequest(BaseModel):
    senderId: int
    receiverId: int


class AcceptRequest(BaseModel):
    requestId: int


class RequestView(BaseModel):
    id: int
    senderId: int
    receiverId: int
    status: RequestStatus
    expiryTime: datetime


class UploadResponse(BaseModel):
    sessionId: int
    expiryTime: datetime


class VerifyPinRequest(BaseModel):
    sessionId: int
    pinHash: str


class VerifyPinResponse(BaseModel):
    success: bool
    message: str
    attemptsLeft: int = Field(ge=0, le=3)


class FileInfo(BaseModel):
    sessionId: int
    maskedDownloadUrl: str
    encryptedDownloadUrl: str
    expiryTime: datetime
