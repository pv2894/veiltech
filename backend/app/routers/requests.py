from datetime import datetime, timedelta
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from ..deps import get_db_session, get_current_user
from ..models import User, ConnectionRequest, RequestStatus
from ..schemas import CreateRequest, AcceptRequest, RequestView


router = APIRouter(tags=["requests"], dependencies=[Depends(get_current_user)])


def to_view(r: ConnectionRequest) -> RequestView:
    return RequestView(
        id=r.id,
        senderId=r.sender_id,
        receiverId=r.receiver_id,
        status=r.status,
        expiryTime=r.expiry_time,
    )


def expire_old(db: Session):
    now = datetime.utcnow()
    pending = db.query(ConnectionRequest).filter(
        ConnectionRequest.status == RequestStatus.PENDING,
        ConnectionRequest.expiry_time < now,
    ).all()
    for req in pending:
        req.status = RequestStatus.EXPIRED
    if pending:
        db.commit()


@router.post("/request", response_model=RequestView)
def create_request(payload: CreateRequest, db: Session = Depends(get_db_session)):
    sender = db.get(User, payload.senderId)
    receiver = db.get(User, payload.receiverId)
    if not sender or not receiver:
        raise HTTPException(status_code=404, detail="Sender or receiver not found")

    req = ConnectionRequest(
        sender_id=payload.senderId,
        receiver_id=payload.receiverId,
        status=RequestStatus.PENDING,
        expiry_time=datetime.utcnow() + timedelta(minutes=5),
    )
    db.add(req)
    db.commit()
    db.refresh(req)
    return to_view(req)


@router.post("/accept", response_model=RequestView)
def accept_request(payload: AcceptRequest, db: Session = Depends(get_db_session)):
    req = db.get(ConnectionRequest, payload.requestId)
    if not req:
        raise HTTPException(status_code=404, detail="Request not found")

    if req.expiry_time < datetime.utcnow():
        req.status = RequestStatus.EXPIRED
        db.commit()
        raise HTTPException(status_code=400, detail="Request expired")

    req.status = RequestStatus.ACCEPTED
    db.commit()
    db.refresh(req)
    return to_view(req)


@router.get("/requests/{user_id}", response_model=list[RequestView])
def list_requests(user_id: int, db: Session = Depends(get_db_session)):
    expire_old(db)
    rows = db.query(ConnectionRequest).filter(ConnectionRequest.receiver_id == user_id).all()
    return [to_view(r) for r in rows]
