from pathlib import Path
import tempfile
from detectors.error_code import Code
from detectors.face_compare import SPEECH_MIN_FACE_MATCH, compare_b64face_template_with_video
from pydub import AudioSegment
import uuid
import whisper
model = whisper.load_model("base")

def recognize_word_audio(audio_path:str, expected:str):
    try:
        result = model.transcribe(audio_path, language="en", condition_on_previous_text = False, initial_prompt="house dog cat tree sun moon book rain smile hat ball friend water bird", temperature= 0)
        if "text" in result:
            recognized_word =  result["text"].strip().lower()
            expected_word = expected.lower()
            is_match = expected_word in recognized_word
            if is_match:
                return True,"Speech match", Code.SUCCESS.value
            else:
                return False, "Speech mismatch", Code.SPEECH_FAILED.value
        else:
            return False, "Speech mismatch",  Code.SPEECH_FAILED.value
    except Exception as e:
         print("exception no sound found",e)
         return False, "Speech mismatch",  Code.SPEECH_FAILED.value

def recognize_word(video_path:str, expected:str, face_template:str):
    if face_template is not None:
            face_success, face_message = compare_b64face_template_with_video(
                face_template, video_path, SPEECH_MIN_FACE_MATCH)
            if not face_success:
                return False, face_message, Code.FACE_MATCH_FAILED.value
    generated_uuid = str(uuid.uuid4())
    td = tempfile.gettempdir()
    temp_filename = str(Path(td, generated_uuid))
    wav_file = AudioSegment.from_file(video_path)
    wav_file.export(temp_filename, format="wav")
    return recognize_word_audio(temp_filename,expected)