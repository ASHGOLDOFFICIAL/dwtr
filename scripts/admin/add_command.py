import pathlib

import os
import uuid

import argparse
import psycopg


def command_handler(args: argparse.Namespace):
    """
    Handles command for adding admins.
    :param args: CLI arguments.
    """
    host: str = args.host
    assert host, "Host is either empty of None"

    port: int = args.port
    assert port, "Port is None"

    user: str = os.getenv("POSTGRES_USER")
    assert user, _missing_variable_message("POSTGRES_USER")

    password: str = os.getenv("POSTGRES_PASSWORD")
    assert password, _missing_variable_message("POSTGRES_PASSWORD")

    db: str = os.getenv("POSTGRES_DB")
    assert db, _missing_variable_message("POSTGRES_DB")

    permission_namespace = os.getenv("ADMIN_PERMISSION_NAMESPACE")
    assert permission_namespace, _missing_variable_message(
        "ADMIN_PERMISSION_NAMESPACE")

    permission_name = os.getenv("ADMIN_PERMISSION_NAME")
    assert permission_name, _missing_variable_message(
        "ADMIN_PERMISSION_NAME")
    print(f"Admin permission is {permission_namespace}.{permission_name}.")

    user_uuid = uuid.uuid4()
    connection_url = f"postgresql://{user}:{password}@{host}:{port}/{db}"
    print(f"Connection URL is {connection_url}")

    with psycopg.connect(connection_url) as connection:
        connection.execute(
            "INSERT INTO users (id, username, google_id) "
            "VALUES (%s, %s, %s)",
            (user_uuid, args.username, args.google_id)
        )

        reference_id = connection.execute(
            "SELECT reference_id FROM permissions "
            "WHERE namespace = %s "
            "AND name = %s",
            (permission_namespace, permission_name)
        ).fetchone()[0]
        assert reference_id, "Couldn't find admin permission in DB."

        connection.execute(
            "INSERT INTO user_permissions (user_id, permission) "
            "VALUES (%s, %s)",
            (user_uuid, reference_id)
        )


def _missing_variable_message(variable: str) -> str:
    """
    Makes "VARIABLE is empty, check your variables." message.
    :param variable: variable name.
    :return: message string.
    """
    return f"{variable} is empty, check your variables."
