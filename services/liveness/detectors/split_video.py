import os
import tempfile
import ffmpeg

from service.file_storage import store_to_temp

def split_video(input_video, start_time, end_time):
    try:
        output_file = init(input_video, start_time, end_time)
        return True, output_file
    
    except Exception as e:
        print(f"Error occured: {e}")
        return False , str(e)


def init(input_file, start_time, end_time):
    temp_filename = tempfile.gettempdir()
    output_file = os.path.join(temp_filename, "video_interval.mp4")

    # Execute ffmpeg command to split the video
    ffmpeg.input(input_file, ss=start_time).output(output_file, t=end_time, loglevel="quiet").run(overwrite_output=True)

    return output_file