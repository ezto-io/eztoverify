import math
import cv2
from detectors.error_code import Code
import mediapipe as mp
from typing import NamedTuple

from detectors.face_compare import MOVEMENT_MIN_FACE_MATCH, compare_face_encoding_with_image, decode_b64face_to_encoding

mpFaceMesh = mp.solutions.face_mesh

face = mpFaceMesh.FaceMesh(
    refine_landmarks=True,
    min_detection_confidence=0.5,
    min_tracking_confidence=0.5,
    max_num_faces=1,
)

LEFT_EAR = 454
RIGHT_EAR = 234
MIN_MOVEMENT = 30


def euclidean_distance(point, point1):
    x, y = point
    x1, y1 = point1
    return math.sqrt((x1 - x) ** 2 + (y1 - y) ** 2)


def get_result(min_x, max_x, distance_factor, current_face_match, is_face_match_needed):
    if min_x == 0:
        return False, "No Movement detected", Code.MOVEMENT_FAILED.value

    is_facematch_success = (not is_face_match_needed) or (
        current_face_match >= MOVEMENT_MIN_FACE_MATCH)
    is_movement_success = max_x - min_x > (MIN_MOVEMENT * distance_factor)

    if not is_movement_success:
        return False, "No Movement detected",  Code.MOVEMENT_FAILED.value
    elif not is_facematch_success:
        return False, "Face mismatch",  Code.FACE_MATCH_FAILED.value
    else:
        return True, "Movement detected", Code.SUCCESS.value


def check_for_movement_from_video(videopath: str, face_template: str):
    current_frame = 0
    min_x = 0
    max_x = 0
    current_face_match = 0
    spoof_match = 0
    distance_factor = 0
    is_face_match_needed = face_template is not None
    if is_face_match_needed:
        decoded_face_encoding = decode_b64face_to_encoding(face_template)

    cap = cv2.VideoCapture(videopath)

    while current_frame < 150:
        if spoof_match >= 4:
            return False, message, Code.SPOOF_DETECT.value
        current_frame += 1
        success, image = cap.read()

        if success:
            img_height, img_width = image.shape[:2]
            img_rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)

            if is_face_match_needed and current_face_match <= MOVEMENT_MIN_FACE_MATCH:
                s, message, similarity, verified, spoof = compare_face_encoding_with_image(
                    decoded_face_encoding, image)
                if spoof:
                    spoof_match += 1
                if verified:
                    current_face_match += 1

            results = face.process(img_rgb)

            if results.multi_face_landmarks:
                mesh_coords = [(int(point.x * img_width), int(point.y * img_height)) for point in
                               results.multi_face_landmarks[0].landmark]
                left_pupil_position = mesh_coords[LEFT_EAR]
                right_pupil_position = mesh_coords[RIGHT_EAR]

                distance = euclidean_distance(
                    left_pupil_position, right_pupil_position)

                if min_x == 0:
                    distance_factor = distance / 170
                    min_x = distance
                    max_x = distance
                else:
                    min_x = min(min_x, distance)
                    max_x = max(max_x, distance)

                is_success, message, code = get_result(
                    min_x, max_x, distance_factor, current_face_match, is_face_match_needed)
                if is_success:
                    cap.release()
                    return True, message, code
        else:
            cap.release()
            return get_result(min_x, max_x, distance_factor, current_face_match, is_face_match_needed)

    cap.release()
    return get_result(min_x, max_x, distance_factor, current_face_match, is_face_match_needed)
