"""Android APK reproduction sandbox shared by Android experiment conditions."""


def __getattr__(name: str):
    if name == "AppReproductionWorkspace":
        from .workspace import AppReproductionWorkspace
        return AppReproductionWorkspace
    if name == "AndroidReproductionReview":
        from .review import AndroidReproductionReview
        return AndroidReproductionReview
    raise AttributeError(name)

__all__ = ["AppReproductionWorkspace", "AndroidReproductionReview"]
