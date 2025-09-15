import psycopg

from users.model.permission import Permission
from users.postgres import Postgres


def command_handler(
        postgres: Postgres,
        permission: Permission,
        username: str
) -> None:
    """
    Grants permission to user.
    :param postgres: PostgreSQL info.
    :param permission: permission to grant.
    :param username: username of user who will be granted permission.
    """
    connection_url = postgres.connection_url
    print(f"Connection URL is {connection_url}")

    with psycopg.connect(connection_url) as connection:
        user_id = connection.execute(
            "SELECT id FROM users WHERE username = %s",
            (username,)
        ).fetchone()[0]
        assert user_id, "Couldn't find user in DB."

        reference_id = connection.execute(
            "SELECT reference_id FROM permissions "
            "WHERE namespace = %s "
            "AND name = %s",
            (permission.namespace, permission.name)
        ).fetchone()[0]
        assert reference_id, "Couldn't find permission in DB."

        connection.execute(
            "INSERT INTO user_permissions (user_id, permission) "
            "VALUES (%s, %s)",
            (user_id, reference_id)
        )
