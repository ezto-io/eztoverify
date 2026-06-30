import threading
from flask import Flask, request, jsonify
from detectors.all_detector import live_all_template
from detectors.error_code import Code
from detectors.hook_url_response import sendResult
from service.file_storage import download_file_from_minio
from detectors.blink_detector import get_blink_count_video
from detectors.face_compare import  SPEECH_MIN_FACE_MATCH, compare_b64face_template_with_video, compare_b64face_with_image, compare_two_base64_images, generate_face_template

from detectors.movement_detector import check_for_movement_from_video
from detectors.number_detector import get_finger_count
from detectors.speech_detector import recognize_word
from waitress import serve
app = Flask(__name__)


@app.route("/")
def hello_world():
    return "Status OK :) 3.0.30"


@app.route("/live/all", methods=['POST'])
def live_all():
    required_fields = ['file_id', 'bucket_name', 'expected', 'metadata', 'hook_url', 'action_token']
    missing_fields = [field for field in required_fields if field not in request.json]
    if missing_fields:
        return jsonify(success=False, message=f"{missing_fields[0]} field missing", code=Code.FIELD_MISSING.value)
    file_id = request.json.get('file_id', None)
    bucket_name = request.json.get('bucket_name', None)
    face_template = request.json.get('face_template', None)
    expected = request.json.get('expected', None)
    metadata = request.json.get('metadata', None)
    hook_url = request.json.get('hook_url', None)
    action_token = request.json.get('action_token', None)
    if hook_url is None or hook_url.strip() == "":
        return jsonify(success=False, message=f"Hook url is not valid", code=Code.HOOK_URL_EMPTY.value)
    def run_live_all_template():
        with app.app_context():
            result = live_all_template(file_id, bucket_name, face_template, expected, metadata)
            sendResult(hook_url, result, action_token)
    
    thread = threading.Thread(target=run_live_all_template)
    thread.start()

    return jsonify(success=True, message="In process", code=Code.SUCCESS.value)


@app.route("/live/face/template", methods=['POST'])
def live_face_to_template():
    if 'file_id' not in request.json:
        return jsonify(success=False, message="file_id field missing")
    if 'bucket_name' not in request.json:
        return jsonify(success=False, message="bucket_name field found")
    file_id = request.json['file_id']
    bucket_name = request.json['bucket_name']
    anti_spoofing = request.json.get('anti_spoofing', True)
    temp_path = download_file_from_minio(file_id, bucket_name)

    if temp_path is None:
        return jsonify(success=False, message="Invalid File")

    success, message, result = generate_face_template(temp_path, anti_spoofing)
    return jsonify(success=success, message=message, template=result)

@app.route("/live/face/compare", methods=['POST'])
def live_face_compare():
    if 'file_id' not in request.json:
        return jsonify(success=False, message="file_id field missing")
    if 'bucket_name' not in request.json:
        return jsonify(success=False, message="bucket_name field found")
    if 'face_template' not in request.json:
        return jsonify(success=False, message="face_template field found")
    
    file_id = request.json['file_id']
    bucket_name = request.json['bucket_name']
    face_template = request.json['face_template']
    anti_spoofing = request.json.get('anti_spoofing', True)
    
    temp_path = download_file_from_minio(file_id, bucket_name)

    if temp_path is None:
        return jsonify(success=False, message="Invalid File")

    success, message, result, verified, spoof = compare_b64face_with_image(face_template, temp_path, anti_spoofing)
    return jsonify(success=success, message=message, confidence=result, verified=verified, spoof=spoof)


@app.route("/live/face/compare/images", methods=['POST'])
def live_two_base64_face_compare():
    if 'face_image' not in request.json:
        return jsonify(success=False, message="face_image field missing")
    if 'face_image_to_compare' not in request.json:
        return jsonify(success=False, message="face_image_to_compare field found")
    
    face_image = request.json['face_image']
    face_image_to_compare = request.json['face_image_to_compare']
    anti_spoofing = request.json.get('anti_spoofing', True)

    success, message, result, verified, spoof = compare_two_base64_images(face_image, face_image_to_compare, anti_spoofing)
    return jsonify(success=success, message=message, confidence=result, verified=verified, spoof = spoof)


@app.route("/live/blink", methods=['POST'])
def live_blink_check():
    if 'file_id' not in request.json:
        return jsonify(success=False, message="file_id field found")
    if 'blink_expected' not in request.json:
        return jsonify(success=False, message="blink_expected field found")
    if 'bucket_name' not in request.json:
        return jsonify(success=False, message="bucket_name field found")
    video_id = request.json["file_id"]
    bucket_name = request.json["bucket_name"]
    video_url = download_file_from_minio(video_id, bucket_name)
    if video_url is None:
        return jsonify(success=False, message="Invalid File")
    blink_expected = request.json['blink_expected']

    face_template = None if 'face_template' not in request.json else request.json[
        "face_template"]

    success, message = get_blink_count_video(
        str(video_url), int(blink_expected), face_template)
    return jsonify(success=success, message=message)


@app.route("/live/movement", methods=['POST'])
def live_movement_check():
    if 'file_id' not in request.json:
        return jsonify(success=False, message="file_id field found")
    if 'bucket_name' not in request.json:
        return jsonify(success=False, message="bucket_name field found")
    video_id = request.json["file_id"]
    bucket_name = request.json["bucket_name"]
    video_url = download_file_from_minio(video_id, bucket_name)
    if video_url is None:
        return jsonify(success=False, message="Invalid File")
    face_template = None if 'face_template' not in request.json else request.json[
        "face_template"]
    success, message = check_for_movement_from_video(
        str(video_url), face_template)
    return jsonify(success=success, message=message)


@app.route("/live/fingercount", methods=['POST'])
def live_fingercount():
    if 'file_id' not in request.json:
        return jsonify(success=False, message="file_id field found")
    if 'finger_expected' not in request.json:
        return jsonify(success=False, message="finger_expected field found")
    if 'bucket_name' not in request.json:
        return jsonify(success=False, message="bucket_name field found")
    finger_expected = request.json['finger_expected']
    bucket_name = request.json['bucket_name']
    video_id = request.json["file_id"]
    video_url = download_file_from_minio(video_id, bucket_name)
    if video_url is None:
        return jsonify(success=False, message="Invalid File")
    face_template = None if 'face_template' not in request.json else request.json[
        "face_template"]
    successs, message = get_finger_count(
        str(video_url), int(finger_expected), face_template)
    return jsonify(success=successs, message=message)


@app.route("/live/speech", methods=['POST'])
def live_speech_check():
    if 'file_id' not in request.json:
        return jsonify(success=False, message="file_id field found")
    if 'word_expected' not in request.json:
        return jsonify(success=False, message="word_expected field found")
    if 'bucket_name' not in request.json:
        return jsonify(success=False, message="bucket_name field found")
    word_expected = request.json['word_expected']
    video_id = request.json["file_id"]
    bucket_name = request.json["bucket_name"]
    video_url = download_file_from_minio(video_id, bucket_name)
    if video_url is None:
        return jsonify(success=False, message="Invalid File")
    face_template = None if 'face_template' not in request.json else request.json[
            "face_template"]
    face_success, face_message = compare_b64face_template_with_video(
                face_template, video_url, SPEECH_MIN_FACE_MATCH)
    if not face_success:
        return jsonify(success=False, message=face_message)
    success, message = recognize_word(video_url, str(word_expected))
    return jsonify(success=success, message=message)


if __name__ == '__main__':
    serve(app,  port=8000,  host='0.0.0.0')
