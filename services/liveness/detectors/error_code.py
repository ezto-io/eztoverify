from enum import Enum

class Code(Enum):
    SUCCESS = 'success'
    FIELD_MISSING = 'field_missing'
    FILE_DOWNLOAD_FAILED = 'file_download_failed'
    FACE_MATCH_FAILED = 'face_match_failed'
    BLINK_FAILED = 'blink_failed'
    MOVEMENT_FAILED = 'movement_failed'
    SPEECH_FAILED = 'speech_failed'
    FINGER_COUNT_FAILED = 'finger_count_failed'
    VIDEO_INTERVAL_FAILED = 'video_interval_failed'
    NOT_PROCESSED = 'not_processed'
    IN_PROGRESS = 'in_progress'
    HOOK_URL_EMPTY = 'hook_url_empty'
    SPOOF_DETECT = 'spoof_detect'