"""Trusted Android black-box runtime and target registry.

Nothing in this package is mounted into an Agent or reproduction container.
The only Agent-visible values derived from it are pixels, coordinate-action
receipts and the explicitly safe target brief.
"""

from .targets import AndroidTargetSpec, get_android_target, list_android_targets

__all__ = ["AndroidTargetSpec", "get_android_target", "list_android_targets"]
