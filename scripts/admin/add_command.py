import uuid

import psycopg
import getpass

from admin.postgres import Postgres
from admin.user import User
from admin.permission import Permission
from argon2 import PasswordHasher, Type


def command_handler(
        postgres: Postgres,
        permission: Permission,
        username: str):
    """
    Handles command for adding admins.
    :param postgres PostgreSQL info.
    :param permission admin permission.
    :param username new admin user's username.
    """
    connection_url = postgres.connection_url
    print(f"Connection URL is {connection_url}")

    user_uuid = uuid.uuid4()
    password = getpass.getpass("Password: ")
    hashed = PasswordHasher(type=Type.I).hash(password)
    user = User(user_uuid, username, hashed)
    _commit_admin_user(connection_url, permission, user)


def _commit_admin_user(
        connection_url: str,
        permission: Permission,
        user: User):
    """
    Persists user to DB and grants them given admin permission.
    :param connection_url: PostgreSQL connection URL.
    :param permission: admin permission.
    :param user: user that will become admin.
    :return: nothing.
    """
    with psycopg.connect(connection_url) as connection:
        connection.execute(
            "INSERT INTO users (id, username, password) "
            "VALUES (%s, %s, %s)",
            (user.id, user.username, user.hashed_password)
        )

        reference_id = connection.execute(
            "SELECT reference_id FROM permissions "
            "WHERE namespace = %s "
            "AND name = %s",
            (permission.namespace, permission.name)
        ).fetchone()[0]
        assert reference_id, "Couldn't find admin permission in DB."

        connection.execute(
            "INSERT INTO user_permissions (user_id, permission) "
            "VALUES (%s, %s)",
            (user.id, reference_id)
        )
