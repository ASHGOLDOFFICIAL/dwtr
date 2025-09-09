import pathlib

import argparse
import enum
from admin import add_command

from dotenv import load_dotenv


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
    add_admin_parser.add_argument(
        "google_id",
        type=str,
        help="new admin's ID in Google"
    )
    return parser.parse_args()


def _main() -> None:
    args: argparse.Namespace = _setup_argparse()

    env: pathlib.Path | None = args.env
    if env:
        print(f"Loading variables from {env}...")
        load_dotenv(env)

    if args.command == Commands.ADD:
        return add_command.command_handler(args)
    return None


if __name__ == "__main__":
    _main()
