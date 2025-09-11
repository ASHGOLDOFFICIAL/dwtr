from dataclasses import dataclass

import uuid


@dataclass
class User:
    """User model."""
    id: uuid.UUID
    username: str
    hashed_password: str
