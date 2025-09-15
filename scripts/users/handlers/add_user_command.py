import uuid

import psycopg
import getpass

from users.postgres import Postgres
from users.model.user import User
from argon2 import PasswordHasher, Type


def command_handler(
        postgres: Postgres,
        username: str):
    """
    Adds new users.
    :param postgres PostgreSQL info.
    :param username new user's username.
    """
    connection_url = postgres.connection_url
    print(f"Connection URL is {connection_url}")

    user_uuid = uuid.uuid4()
    password = getpass.getpass("Password: ")
    hashed = PasswordHasher(type=Type.I).hash(password)
    user = User(user_uuid, username, hashed)

    with psycopg.connect(connection_url) as connection:
        connection.execute(
            "INSERT INTO users (id, username, password) "
            "VALUES (%s, %s, %s)",
            (user.id, user.username, user.hashed_password)
        )
