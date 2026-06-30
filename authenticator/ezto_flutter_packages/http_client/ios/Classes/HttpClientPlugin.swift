import Flutter
import UIKit

public class HttpClientPlugin: NSObject, FlutterPlugin, HttpNativeClientHostApi {

    public static func register(with registrar: FlutterPluginRegistrar) {
      let messenger : FlutterBinaryMessenger = registrar.messenger()
      let api : HttpNativeClientHostApi & NSObjectProtocol = HttpClientPlugin.init()
      HttpNativeClientHostApiSetup.setUp(binaryMessenger: messenger, api:api)
    }
    
    func getUrl(url: String, completion: @escaping (Result<HttpClientResponse, any Error>) -> Void) {
        guard let uri = URL(string: url) else {
            completion(.failure(PigeonError(code: "http_invalid_url", message: "Invalid URL", details: "") as! any Error))
            return
        }
        
        let session = URLSession.shared
        
        let task = session.dataTask(with: uri) { data, response, error in
            DispatchQueue.main.async {
                if let error = error {
                    completion(.failure(PigeonError(code: "http_generic_error", message: error.localizedDescription, details: "") as! any Error))
                    return
                }
                
                guard let httpResponse = response as? HTTPURLResponse else {
                    completion(.failure(PigeonError(code: "http_invalid_response", message: "Invalid response received from server", details: "") as! any Error))
                    return
                }
                
                guard let data = data else {
                    completion(.failure(PigeonError(code: "http_invalid_data", message: "Invalid data received from server", details: "") as! any Error))
                    return
                }
                
                if let responseString = String(data:data, encoding: .utf8) {
                    completion(.success(HttpClientResponse(statusCode: Int64(httpResponse.statusCode), body: responseString)))
                } else {
                    completion(.failure(PigeonError(code: "http_invalid_data", message: "Invalid data received from server", details: "") as! any Error))
                }
            }
        }
        task.resume()
    }
}
