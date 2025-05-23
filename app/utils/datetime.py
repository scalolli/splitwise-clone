import datetime

def utcnow():
    """Return the current UTC time as a timezone-aware datetime object."""
    return datetime.datetime.now(datetime.timezone.utc)