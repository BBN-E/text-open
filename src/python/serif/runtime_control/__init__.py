import enum


class ProductionLevel(enum.IntEnum):
    DEV = 0
    INTERNAL_USAGE = 1
    EXTERNAL_USAGE = 2
