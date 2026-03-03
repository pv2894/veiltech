from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from ..deps import get_db_session, get_current_user
from ..models import User
from ..schemas import UserOut


router = APIRouter(tags=["users"])


@router.get("/users", response_model=list[UserOut], dependencies=[Depends(get_current_user)])
def list_users(db: Session = Depends(get_db_session)):
    users = db.query(User).all()
    return [UserOut(id=u.id, name=u.name, phone=u.phone) for u in users]
