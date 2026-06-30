import time
import requests

def sendResult(hook_url : str, result : any, action_token : any):
    print(result.json)
    attempt = 0
    while attempt < 3:
        headers = {
            'Authorization': 'Bearer ' + action_token,
            'Content-Type': 'application/json'
        }
        response = requests.post(hook_url, json=result.json, headers=headers)
        if response.status_code == 200:
            print("Data updated successfully")
            break
        else:
            print("Failed to update data. Status code:", response.status_code)
            attempt += 1
            time.sleep(1)
