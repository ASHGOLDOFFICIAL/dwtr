import os
import pathlib

import argparse
import enum

from dotenv import load_dotenv

from users.handlers import add_user_command, grant_permission_command
from users.model.permission import Permission
from users.postgres import Postgres


class Commands(enum.StrEnum):
    """Commands for user management."""
    ADD = "add"
    GRANT = "grant"


def _setup_argparse() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="User management."
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

    add_user_parser = subparsers.add_parser(
        Commands.ADD,
        description="Add new user."
    )
    add_user_parser.add_argument(
        "--admin",
        action="store_true",
        help="Whether to make this user admin",
    )
    add_user_parser.add_argument(
        "username",
        type=str,
        help="New user's username."
    )
    
    grant_permission_parser = subparsers.add_parser(
        Commands.GRANT,
        description="Grant permission to a user."
    )
    grant_permission_parser.add_argument(
        "username",
        type=str,
        help="Username of the user who will be granted permission."
    )
    grant_permission_parser.add_argument(
        "permission",
        type=str,
        help="Permission to grant.",
        metavar="[namespace].[name]"
    )
    return parser.parse_args()


def _main() -> None:
    args: argparse.Namespace = _setup_argparse()

    env: pathlib.Path | None = args.env
    if env:
        print(f"Loading variables from {env}...")
        load_dotenv(env)
    postgres = _get_postgres_info(args)

    if args.command == Commands.ADD:
        add_user_command.command_handler(postgres, args.username)
        if args.admin:
            permission = _get_admin_permission()
            print(f"Admin permission is {permission}.")
            grant_permission_command.command_handler(
                postgres,
                permission,
                args.username)
    elif args.command == Commands.GRANT:
        permission_str: str = args.permission
        permission_args = permission_str.split(".", maxsplit=1)
        permission = Permission(permission_args[0], permission_args[1])
        print(f"Permission is {permission}.")
        grant_permission_command.command_handler(
            postgres,
            permission,
            args.username)

    return None


def _get_postgres_info(args: argparse.Namespace) -> Postgres:
    """
    Makes PostgreSQL info from given args and environment variables.
    :rtype: Postgres PostgreSQL info.
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
    return Postgres(user, password, host, port, db)


def _get_admin_permission():
    """
    Returns users permission found in environment variables.
    """
    permission_namespace = os.getenv("ADMIN_PERMISSION_NAMESPACE")
    assert permission_namespace, _missing_variable_message(
        "ADMIN_PERMISSION_NAMESPACE")
    permission_name = os.getenv("ADMIN_PERMISSION_NAME")
    assert permission_name, _missing_variable_message(
        "ADMIN_PERMISSION_NAME")
    return Permission(permission_namespace, permission_name)


def _missing_variable_message(variable: str) -> str:
    """
    Makes "VARIABLE is empty, check your variables." message.
    :param variable: variable name.
    :return: message string.
    """
    return f"{variable} is empty, check your variables."


if __name__ == "__main__":
    _main()
