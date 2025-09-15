from dataclasses import dataclass


@dataclass
class Permission:
    """Permission model."""
    namespace: str
    name: str
