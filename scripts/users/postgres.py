from dataclasses import dataclass


@dataclass
class Postgres:
    """
    PostgreSQL info.
    :param user: user's name.
    :param password: user's password.
    :param host: hostname.
    :param port: port.
    :param db: database name. 
    """
    user: str
    password: str
    host: str
    port: int
    db: str

    @property
    def connection_url(self):
        """PostgreSQL connection URL."""
        return (f"postgresql://{self.user}:{self.password}"
                f"@{self.host}:{self.port}/{self.db}")
