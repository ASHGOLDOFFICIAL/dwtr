import os
import pathlib

import argparse
import enum
from admin import add_command

from dotenv import load_dotenv

from admin.permission import Permission
from admin.postgres import Postgres


class Commands(enum.StrEnum):
    """Commands for admin management."""
    ADD = "add"


def _setup_argparse() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Admin management."
    )
    parser.add_argument(
        "host",
        type=str,
        help="PostgreSQL host."
    )
    parser.add_argument(
        "port",
        type=int,
        help="PostgreSQL port."
    )
    parser.add_argument(
        "--env",
        type=pathlib.Path,
        help="Path to .env file to load variables."
    )

    subparsers = parser.add_subparsers(
        dest="command",
        required=True,
        help="Command to perform."
    )

    add_admin_parser = subparsers.add_parser(
        Commands.ADD,
        description="Add new admin user."
    )
    add_admin_parser.add_argument(
        "username",
        type=str,
        help="new admin's username."
    )
    return parser.parse_args()


def _main() -> None:
    args: argparse.Namespace = _setup_argparse()

    env: pathlib.Path | None = args.env
    if env:
        print(f"Loading variables from {env}...")
        load_dotenv(env)

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

    postgres: Postgres = Postgres(user, password, host, port, db)

    if args.command == Commands.ADD:
        permission_namespace = os.getenv("ADMIN_PERMISSION_NAMESPACE")
        assert permission_namespace, _missing_variable_message(
            "ADMIN_PERMISSION_NAMESPACE")

        permission_name = os.getenv("ADMIN_PERMISSION_NAME")
        assert permission_name, _missing_variable_message(
            "ADMIN_PERMISSION_NAME")

        permission: Permission = Permission(
            permission_namespace,
            permission_name)

        print(f"Admin permission is {permission}.")
        return add_command.command_handler(postgres, permission, args.username)
    return None


def _missing_variable_message(variable: str) -> str:
    """
    Makes "VARIABLE is empty, check your variables." message.
    :param variable: variable name.
    :return: message string.
    """
    return f"{variable} is empty, check your variables."


if __name__ == "__main__":
    _main()
