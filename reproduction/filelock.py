"""Compatibility import for the shared platform lock.

New code should import :mod:`benchmark.filelock`.  Keeping this module avoids
breaking third-party runners that used the earlier reproduction-domain path.
"""
from benchmark.filelock import lock_for

__all__ = ["lock_for"]
