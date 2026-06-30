from data.file_type import FileType
import uuid
import os
import tempfile
from pathlib import Path
from minio import Minio
from minio.error import MinioException

def store_to_temp(file_type: FileType, file: any):
    try:
        td = tempfile.gettempdir()
        temp_filename = Path(td) / file_type.value
        file.save(temp_filename)  
        return str(temp_filename)
    except Exception as e:
        print("exception on store_to_temp")
        print(e)
        return None
    
def download_file_from_minio(file_path: str, bucket_name: str):
    try:
        host =  os.environ['MINIO_HOST']
        access_key =  os.environ['MINIO_ACCESS_KEY']
        secret =  os.environ['MINIO_SECRET']
        
        client = Minio(host,
            access_key=access_key,
            secret_key=secret,
            secure=False
        )
        generated_uuid = str(uuid.uuid4())
        td = tempfile.gettempdir()
        temp_filename = Path(td, generated_uuid)
        print("downloading file")
        object_data = client.get_object(bucket_name, file_path)
        with open(temp_filename, "wb") as file_data:
            for data in object_data:
                file_data.write(data)
        print("file downloaded")

        object_data.close()
        object_data.release_conn()
        return str(temp_filename)
    except Exception as e:
        print("exception occured")
        print(e)
        return None
    