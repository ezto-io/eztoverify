import tempfile
import cv2
import pickle
import base64
import cv2
import os
import io
from deepface import DeepFace
from PIL import Image, ImageFile
import numpy as np

BLINK_MIN_FACE_MATCH = 10
MOVEMENT_MIN_FACE_MATCH = 6
FINGER_MIN_FACE_MATCH = 6
SPEECH_MIN_FACE_MATCH = 10
face_tolerance = float(os.environ.get("FACE_TOLERANCE", 0.68))
print(f"face_tolerance {face_tolerance}")


def generate_face_template(imagepath, anti_spoofing=True):
    try:
        embedding_objs = DeepFace.represent(
            img_path=imagepath,
            enforce_detection=False,
            normalization="VGGFace",
            detector_backend="ssd",
            anti_spoofing=anti_spoofing
        )

        if len(embedding_objs) == 0:
            return False, "No Face Detected", None
        elif len(embedding_objs) != 1:
            return False, "Multiple Faces detected", None
        else:
            embedding_vector = embedding_objs[0]['embedding']
            embedding_pickle = pickle.dumps(embedding_vector)
            embedding_base64 = base64.b64encode(embedding_pickle).decode('utf-8')
            return True, "Face Template generated successfully", embedding_base64
    except Exception as e:
        print("Exception error", e)
        if("Spoof" in str(e)):
            return False, str(e), None
        return False, "Unable to generate Face Template", None,


def decode_b64face_to_encoding(b64_face_template: str):
    try:
        embedding_pickle = base64.b64decode(b64_face_template)
        saved_face_encoding = pickle.loads(embedding_pickle)
        return saved_face_encoding
    except:
        return None


def compare_face_encoding_with_image(saved_face_encoding, image, anti_spoofing=True):
    try:
        if saved_face_encoding is None:
            return False, "Invalid Face Encoding", None, None, False
        current_face_encodings = DeepFace.represent(
            img_path=image,
            enforce_detection=False,
            normalization="VGGFace",
            detector_backend="ssd",
            anti_spoofing=anti_spoofing
        )

        if len(current_face_encodings) == 0:
            return False, "No Face Detected", None, None, False
        elif len(current_face_encodings) != 1:
            return False, "Multiple Faces detected", None, None, False
        else:
            result = DeepFace.verify(
                img1_path=saved_face_encoding,
                img2_path=current_face_encodings[0]['embedding'],
                silent=True,
                enforce_detection=False,
                threshold=face_tolerance,
                normalization="VGGFace",
                detector_backend="ssd",
                anti_spoofing=anti_spoofing
            )
            distance = (1-min(result["distance"], 1))*100
            return True, "Success", round(distance, 2), result['verified'], False
    except Exception as e:
        print("Exception error", e)
        if("Spoof" in str(e)):
            return False, str(e), None, None, True
        return False, "Failed", None, None, True


def compare_b64face_with_image(b64_face_template: str, image, anti_spoofing=True):
    saved_face_encoding = decode_b64face_to_encoding(b64_face_template)
    return compare_face_encoding_with_image(saved_face_encoding, image, anti_spoofing)


def compare_b64face_template_with_video(b64_face_template: str, video_url: str, match: int):
    current_face_match = 0
    spoof_match = 0
    cap = cv2.VideoCapture(video_url)
    while True:
        success, image = cap.read()
        if success:
            if spoof_match >= 6:
                return False, "Spoofed face detected"
            if current_face_match >= match:
                return True, "Face match"
            image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
            s, message, sim, verified, spoof = compare_b64face_with_image(
                b64_face_template, image)
            if spoof:
                spoof_match = spoof_match + 1
            if verified:
                current_face_match = current_face_match + 1
        else:
            if current_face_match >= match:
                return True, "Face match"
            return False, "Face mismatch"
        
ImageFile.LOAD_TRUNCATED_IMAGES = True


def base64_to_image_file(base64_str):
    try:
        image_data = base64.b64decode(base64_str)
        with tempfile.NamedTemporaryFile(delete=False, mode='wb') as temp_file:
            temp_file.write(image_data)
            temp_file_path = temp_file.name

        return temp_file_path
    except base64.binascii.Error as e:
        print(f"Base64 decoding error: {e}")
        raise
    except IOError as e:
        print(f"IOError during file operations: {e}")
        raise
    except Exception as e:
        print(f"Error in base64_to_image_file: {e}")
        raise
  

def compare_two_base64_images(image_one_base64, image_two_base64, anti_spoofing=True):
    try:
        image_one_path = base64_to_image_file(image_one_base64)
        image_two_path = base64_to_image_file(image_two_base64)

        result = DeepFace.verify(
            img1_path=image_one_path,
            img2_path=image_two_path,
            silent=True,
            enforce_detection=False,
            threshold=face_tolerance,
            normalization="VGGFace",
            detector_backend="ssd",
            anti_spoofing=anti_spoofing
        )
        
        distance = (1 - min(result["distance"], 1)) * 100
        os.remove(image_one_path)
        os.remove(image_two_path)
        return True, "Success", round(distance, 2), result['verified'], False
    except ValueError as e:
        print(f"Exception error value: {e}")
        return False, "Failed", None, None, True
    except Exception as e:
        print(f"Exception error: {e}")
        if "Spoof" in str(e):
            return False, str(e), None, None, True
        return False, "Failed", None, None, True