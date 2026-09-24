"""Synthetic Android material pack with a lazy builder export."""


def __getattr__(name: str):
    if name == "ensure_app_materials":
        from .build import ensure_app_materials
        return ensure_app_materials
    raise AttributeError(name)

__all__ = ["ensure_app_materials"]
