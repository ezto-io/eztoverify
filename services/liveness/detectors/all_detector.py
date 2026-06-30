from flask import jsonify
from detectors.error_code import Code
from detectors.split_video import split_video
from detectors.blink_detector import get_blink_count_video

from detectors.movement_detector import check_for_movement_from_video
from detectors.number_detector import get_finger_count
from detectors.speech_detector import recognize_word
from service.file_storage import download_file_from_minio



def live_all_template(file_id, bucket_name, face_template, expected, metadata):
    final_result = {}
    isFailed = False
    error_mapping = {
        'blink': Code.BLINK_FAILED.value,
        'movement': Code.MOVEMENT_FAILED.value,
        'speech': Code.SPEECH_FAILED.value,
        'fingercount': Code.FINGER_COUNT_FAILED.value
    }

    if expected is not None and metadata is not None:
        video_path = download_file_from_minio(file_id, bucket_name)
        if video_path == None:
            return jsonify(success=False, message="File download failed", code=Code.FILE_DOWNLOAD_FAILED.value)
        for key in expected:
            if expected[key]['enabled']:
                video_interval = metadata[key]["video"]

                if key == "blink" and not isFailed:
                    blink_expected = expected[key]["expected"]
                    success, file_path = live_video_interval(video_path, video_interval)
                    if success:
                        final_result[key] = live_blink(file_path, blink_expected, face_template)
                        if not final_result[key]['success']:
                            isFailed = True
                    else:
                        return jsonify(success=False, message=file_path, results=final_result, code=Code.VIDEO_INTERVAL_FAILED.value)
                
                elif key == "movement" and not isFailed:
                    print("movement", key)
                    success, file_path = live_video_interval(video_path, video_interval)
                    if success:
                        final_result[key] = live_movement(file_path, face_template)
                        if not final_result[key]['success']:
                            isFailed = True
                    else:
                        return jsonify(success=False, message=file_path, results=final_result, code=Code.VIDEO_INTERVAL_FAILED.value)
                
                elif key == "speech" and not isFailed:
                    speech_expected = expected[key]["expected"]
                    success, file_path = live_video_interval(video_path, video_interval)
                    if success:
                        final_result[key] = live_speech(file_path, speech_expected, face_template)
                        if not final_result[key]['success']:
                            isFailed = True
                    else:
                        return jsonify(success=False, message=file_path, results=final_result, code=Code.VIDEO_INTERVAL_FAILED.value)
                
                elif key == "fingercount" and not isFailed:
                    finger_count_expected = expected[key]["expected"]
                    success, file_path = live_video_interval(video_path, video_interval)
                    if success:
                        final_result[key] = live_finger_count(file_path, finger_count_expected, face_template)
                        if not final_result[key]['success']:
                            isFailed = True
                    else:
                        return jsonify(success=False, message=file_path, results=final_result, code=Code.VIDEO_INTERVAL_FAILED.value)
                
                else:
                    final_result[key] = {"success": False, "message": "Not processed", "code": Code.NOT_PROCESSED.value}

                    
        for key in final_result:
            if not final_result[key]['success']:
                error_code = error_mapping.get(key, "")
                return jsonify(success=False, message=final_result[key]["message"], results=final_result, code=error_code)

        return jsonify(success=True, message="Success", results=final_result, code=Code.SUCCESS.value)            

    else:
        return jsonify(success=False, message="Expected or metadata field is missing", code=Code.FIELD_MISSING.value)
                



def live_video_interval(file_id, video_interval):
    video_start = "{:.5f}".format(video_interval['start'])
    video_end = "{:.5f}".format(video_interval['end'])
    success, file_path = split_video(
        file_id, video_start, video_end)
    return success, file_path


def live_blink(file_id, blink_expected, face_template):
    success, message, code = get_blink_count_video(
        str(file_id), int(blink_expected), face_template)
    return {"success": success, "message": message, "code": code}


def live_movement(file_id, face_template):
    success, message, code = check_for_movement_from_video(file_id, face_template)
    return {"success": success, "message": message, "code": code}


def live_speech(file_id, word_expected, face_template):
    success, message, code = recognize_word(file_id, str(word_expected), face_template)
    return {"success": success, "message": message, "code": code}


def live_finger_count(file_id, finger_count_expected, face_template):
    success, message, code = get_finger_count(
        str(file_id), int(finger_count_expected), face_template)
    return {"success": success, "message": message, "code": code}

