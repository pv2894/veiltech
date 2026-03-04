from datetime import datetime
from pathlib import Path
from fastapi import APIRouter, Depends, File, Form, HTTPException, UploadFile
from fastapi.responses import FileResponse
from sqlalchemy.orm import Session
from ..config import settings
from ..deps import get_db_session, get_current_user
from ..models import ConnectionRequest, Session as TransferSession, RequestStatus
from ..schemas import UploadResponse, VerifyPinRequest, VerifyPinResponse, FileInfo


router = APIRouter(tags=["files"], dependencies=[Depends(get_current_user)])
ALLOWED_EXT = {"jpg", "jpeg", "png", "pdf", "txt"}


def get_ext(filename: str) -> str:
    if "." not in filename:
        return ""
    return filename.rsplit(".", 1)[1].lower()


@router.post("/upload", response_model=UploadResponse)
def upload_file(
    requestId: int = Form(...),
    pinHash: str = Form(...),
    expiryTime: datetime = Form(...),
    masked: UploadFile = File(...),
    encryptedActual: UploadFile = File(...),
    db: Session = Depends(get_db_session),
):
    req = db.get(ConnectionRequest, requestId)
    if not req:
        raise HTTPException(status_code=404, detail="Request not found")
    if req.status != RequestStatus.ACCEPTED:
        raise HTTPException(status_code=400, detail="Request is not accepted")

    masked_name = masked.filename or "masked"
    if get_ext(masked_name) not in ALLOWED_EXT:
        raise HTTPException(status_code=400, detail="Unsupported file type")

    masked_bytes = masked.file.read()
    encrypted_bytes = encryptedActual.file.read()
    if len(encrypted_bytes) > len(masked_bytes):
        raise HTTPException(status_code=400, detail="Actual file must be <= masked file size")

    sess = TransferSession(
        request_id=requestId,
        masked_file_path="",
        encrypted_file_path="",
        pin_hash=pinHash,
        expiry_time=expiryTime,
        failed_attempts=0,
        locked=False,
    )
    db.add(sess)
    db.commit()
    db.refresh(sess)

    folder = Path(settings.uploads_root) / str(sess.id)
    folder.mkdir(parents=True, exist_ok=True)
    masked_path = folder / f"masked_{masked_name}"
    encrypted_path = folder / "actual.enc"

    masked_path.write_bytes(masked_bytes)
    encrypted_path.write_bytes(encrypted_bytes)

    sess.masked_file_path = str(masked_path)
    sess.encrypted_file_path = str(encrypted_path)
    db.commit()

    return UploadResponse(sessionId=sess.id, expiryTime=sess.expiry_time)


@router.get("/file/{session_id}")
def read_file(session_id: int, type: str, db: Session = Depends(get_db_session)):
    sess = db.get(TransferSession, session_id)
    if not sess:
        raise HTTPException(status_code=404, detail="Session not found")

    path = Path(sess.masked_file_path) if type.lower() == "masked" else Path(sess.encrypted_file_path)
    if not path.exists():
        raise HTTPException(status_code=404, detail="File missing")
    return FileResponse(path)


@router.get("/fileInfo/{session_id}", response_model=FileInfo)
def file_info(session_id: int, db: Session = Depends(get_db_session)):
    sess = db.get(TransferSession, session_id)
    if not sess:
        raise HTTPException(status_code=404, detail="Session not found")
    if sess.expiry_time < datetime.utcnow():
        raise HTTPException(status_code=400, detail="Session expired")

    return FileInfo(
        sessionId=sess.id,
        maskedDownloadUrl=f"/file/{session_id}?type=masked",
        encryptedDownloadUrl=f"/file/{session_id}?type=encrypted",
        expiryTime=sess.expiry_time,
    )


@router.post("/verifyPin", response_model=VerifyPinResponse)
def verify_pin(payload: VerifyPinRequest, db: Session = Depends(get_db_session)):
    sess = db.get(TransferSession, payload.sessionId)
    if not sess:
        raise HTTPException(status_code=404, detail="Session not found")

    if sess.locked:
        return VerifyPinResponse(success=False, message="Session locked", attemptsLeft=0)

    if sess.expiry_time < datetime.utcnow():
        return VerifyPinResponse(success=False, message="Session expired", attemptsLeft=0)

    if sess.pin_hash == payload.pinHash:
        sess.failed_attempts = 0
        db.commit()
        return VerifyPinResponse(success=True, message="PIN verified", attemptsLeft=3)

    sess.failed_attempts += 1
    if sess.failed_attempts >= 3:
        sess.locked = True
    db.commit()
    left = max(0, 3 - sess.failed_attempts)
    return VerifyPinResponse(success=False, message="Invalid PIN", attemptsLeft=left)
