
import cv2
from detectors.error_code import Code
from detectors.face_compare import FINGER_MIN_FACE_MATCH, compare_face_encoding_with_image, decode_b64face_to_encoding
import mediapipe as mp
from collections import Counter

mp_Hands = mp.solutions.hands
hands = mp_Hands.Hands()
index_coord = (8, 7, 6, 5)
middle_coord = (12, 11, 10, 9)
ring_coord = (16, 15, 14, 13)
pinky_coord = (20, 19, 18, 17)
thumb_Coord = (4, 3, 2)
finger_Coord = [index_coord, middle_coord, ring_coord, pinky_coord]


def check_match(detected_fingers, expected: int, is_face_match_needed: bool, current_face_match: int):
    is_facematch_success = (not is_face_match_needed) or (
        current_face_match >= FINGER_MIN_FACE_MATCH)
    if not is_facematch_success:
        return False, f"Face mismatch", Code.FACE_MATCH_FAILED.value
    if len(detected_fingers) == 0:
        return False, f"Fingercount mismatch. Expected {expected}, found 0", Code.FINGER_COUNT_FAILED.value
    counts = Counter(detected_fingers)

    common = counts.most_common(2)
    max_repeated_value = common[0][0]
    finger_match = max_repeated_value == expected

    if not finger_match and len(common) == 2:
        max_repeated_2nd_value = common[1][0]
        finger_match = max_repeated_2nd_value == expected

    if finger_match:
        return finger_match, "Fingercount match", Code.SUCCESS.value
    else:
        return finger_match, f"Fingercount mismatch. Expected {expected}, found {max_repeated_value}", Code.FINGER_COUNT_FAILED.value


def get_finger_count(videopath: str, expected: int, face_template: str) -> int:
    current_frame = 0
    detected_fingers = []
    current_face_match = 0
    spoof_match = 0
    is_face_match_needed = face_template is not None
    print("getting finger count")
    if is_face_match_needed:
        decoded_face_encoding = decode_b64face_to_encoding(face_template)

    # image = cv2.flip(image, 0)
    cap = cv2.VideoCapture(videopath)
    while True:
        current_frame += 1
        if spoof_match >= 4:
            return False, message, Code.SPOOF_DETECT.value
        if current_frame == 150:
            # No matching frame found in first 150 frames
            print("No match found in first 150 frames")
            cap.release()
            return check_match(detected_fingers, expected, is_face_match_needed, current_face_match)
        success, image = cap.read()
        if success:
            RGB_image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
            if is_face_match_needed and current_face_match < FINGER_MIN_FACE_MATCH:
                s,message, similarity, verified, spoof = compare_face_encoding_with_image(decoded_face_encoding,image)
                if spoof:
                    spoof_match += 1
                if verified:
                    current_face_match = current_face_match + 1
            results = hands.process(RGB_image)
            multiLandMarks = results.multi_hand_landmarks
            upCount = 0
            if multiLandMarks:
                # handlist will hold the coordinates
                handList = []
                for handLms in multiLandMarks:
                    for idx, lm in enumerate(handLms.landmark):
                        h, w, c = image.shape
                        cx, cy = int(lm.x * w), int(lm.y * h)
                        handList.append((cx, cy))
                for coordinate in finger_Coord:
                    # Check y coordinate value is greater
                    if (handList[coordinate[0]][1] < handList[coordinate[1]][1] and handList[coordinate[1]][1] < handList[coordinate[2]][1] and handList[coordinate[2]][1] < handList[coordinate[3]][1]):
                        upCount += 1

                # Check if thumb is in right or left direction
                index_tip_x_coord = handList[5][0]
                pinky_mcp_x_coord = handList[17][0]
                is_thumb_in_left = index_tip_x_coord < pinky_mcp_x_coord

                if (is_thumb_in_left):
                    if (handList[thumb_Coord[0]][0]+20 < handList[thumb_Coord[1]][0]) and (handList[thumb_Coord[1]][0]+20 < handList[thumb_Coord[2]][0]):
                        upCount += 1
                else:
                    if (handList[thumb_Coord[0]][0] > handList[thumb_Coord[1]][0]+20) and (handList[thumb_Coord[1]][0] > handList[thumb_Coord[2]][0]+20):
                        upCount += 1

                if (upCount != 0):
                    detected_fingers.append(upCount)
        else:
            return check_match(detected_fingers, expected, is_face_match_needed, current_face_match)
