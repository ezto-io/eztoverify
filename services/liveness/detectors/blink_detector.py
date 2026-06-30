import cv2
import math
from typing import NamedTuple
from detectors.error_code import Code
from detectors.face_compare import compare_face_encoding_with_image, BLINK_MIN_FACE_MATCH, decode_b64face_to_encoding
import mediapipe as mp

mpFaceMesh = mp.solutions.face_mesh
face = mpFaceMesh.FaceMesh(
    refine_landmarks=True,
    min_detection_confidence=0.5,
    min_tracking_confidence=0.5,
    max_num_faces=1,
)
LEFT_EYE = [362, 382, 381, 380, 374, 373, 390,
            249, 263, 466, 388, 387, 386, 385, 384, 398]
RIGHT_EYE = [33, 7, 163, 144, 145, 153, 154,
             155, 133, 173, 157, 158, 159, 160, 161, 246]


def is_eye_closed(mesh_coord, is_eye_closed_in_previous_frame) -> bool:
    # Right eyes
    # horizontal line
    rh_right = mesh_coord[RIGHT_EYE[0]]
    rh_left = mesh_coord[RIGHT_EYE[8]]
    # vertical line
    rv_top = mesh_coord[RIGHT_EYE[12]]
    rv_bottom = mesh_coord[RIGHT_EYE[4]]

    # LEFT_EYE
    # horizontal line
    lh_right = mesh_coord[LEFT_EYE[0]]
    lh_left = mesh_coord[LEFT_EYE[8]]

    # vertical line
    lv_top = mesh_coord[LEFT_EYE[12]]
    lv_bottom = mesh_coord[LEFT_EYE[4]]

    right_horizontal_distance = euclaidean_Distance(rh_right, rh_left)
    right_vertical_distance = euclaidean_Distance(rv_top, rv_bottom)

    left_vertical_distance = euclaidean_Distance(lv_top, lv_bottom)
    left_horizontal_distance = euclaidean_Distance(lh_right, lh_left)
    if right_horizontal_distance == 0 or right_vertical_distance == 0 or left_vertical_distance == 0 or right_horizontal_distance == 0:
        return is_eye_closed_in_previous_frame
    right_eye_ratio: float = right_horizontal_distance / right_vertical_distance
    left_eye_ratio: float = left_horizontal_distance / left_vertical_distance

    total_ratio: float = (right_eye_ratio + left_eye_ratio) / 2
    return total_ratio > 5.5


def get_blink_count_video(video_path: str, expected: int, face_template: str):
    is_eye_closed_in_previous_frame = False
    blink_count = 0
    current_face_match = 0
    spoof_match = 0
    is_face_match_needed = face_template is not None
    if is_face_match_needed:
        decoded_face_encoding = decode_b64face_to_encoding(face_template)

    cap = cv2.VideoCapture(video_path)
    while True:
        if spoof_match >= 4:
            return False, message, Code.SPOOF_DETECT.value
        success, image = cap.read()
        if success:
            img_height, img_width = image.shape[:2]
            img_rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
            if is_face_match_needed and current_face_match <= BLINK_MIN_FACE_MATCH:
                s, message, sim, verified, spoof = compare_face_encoding_with_image(
                    decoded_face_encoding, image)
                if spoof:
                    spoof_match = spoof_match + 1
                if verified:
                    current_face_match = current_face_match + 1
            results: NamedTuple = face.process(img_rgb)
            if results.multi_face_landmarks:
                mesh_coords = [(int(point.x * img_width), int(point.y * img_height)) for point in
                               results.multi_face_landmarks[0].landmark]
                eye_status = is_eye_closed(
                    mesh_coords, is_eye_closed_in_previous_frame)
                if eye_status != is_eye_closed_in_previous_frame:
                    blink_count += 1
                is_eye_closed_in_previous_frame = eye_status
        else:
            print("expected blink: ", expected)
            print("found blink: ", int(blink_count / 2))
            blinked = int(blink_count / 2)
            blink_success = blinked >= expected
            face_match_success = not is_face_match_needed or (
                is_face_match_needed and current_face_match >= BLINK_MIN_FACE_MATCH)
            success = blink_success and face_match_success
            message = "Blink matched"
            code = Code.SUCCESS.value
            if not face_match_success:
                message = "Face match failed"
                code = Code.FACE_MATCH_FAILED.value
            if not blink_success:
                message = "Blink count mismatch"
                code = Code.BLINK_FAILED.value
            return success, message, code


def euclaidean_Distance(point, point1):
    x, y = point
    x1, y1 = point1
    distance = math.sqrt((x1 - x) ** 2 + (y1 - y) ** 2)
    return distance
