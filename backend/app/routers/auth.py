from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from ..deps import get_db_session
from ..models import User
from ..schemas import RegisterRequest, LoginRequest, AuthResponse
from ..security import hash_for_storage, verify_hash, create_access_token


router = APIRouter(tags=["auth"])


@router.post("/register", response_model=AuthResponse)
def register(payload: RegisterRequest, db: Session = Depends(get_db_session)):
    exists = db.query(User).filter(User.phone == payload.phone).first()
    if exists:
        raise HTTPException(status_code=400, detail="Phone already exists")

    user = User(name=payload.name, phone=payload.phone, password_hash=hash_for_storage(payload.passwordHash))
    db.add(user)
    db.commit()
    db.refresh(user)

    token = create_access_token(subject=user.phone, user_id=user.id)
    return AuthResponse(token=token, userId=user.id, name=user.name)


@router.post("/login", response_model=AuthResponse)
def login(payload: LoginRequest, db: Session = Depends(get_db_session)):
    user = db.query(User).filter(User.phone == payload.phone).first()
    if not user or not verify_hash(payload.passwordHash, user.password_hash):
        raise HTTPException(status_code=400, detail="Invalid credentials")

    token = create_access_token(subject=user.phone, user_id=user.id)
    return AuthResponse(token=token, userId=user.id, name=user.name)
