#if os(macOS)
import FlutterMacOS
#elseif os(iOS)
import Flutter
import UIKit
#endif
import AuthenticationServices


public class FidoPlugin: NSObject, FlutterPlugin, ASAuthorizationControllerDelegate, ASAuthorizationControllerPresentationContextProviding,FidoApi {
    var finalRegResult: ((Result<FidoRegResponse, Error>) -> Void)?;
    var finalVerifyResult: ((Result<FidoVerifyResponse, Error>) -> Void)?;
    let failedRegResponse = FidoRegResponse(
        success: false,
        message: "Use IOS Version more than 15!",
        errorCode: "platformNotSupported"
    )
    let failedVerifyResponse = FidoVerifyResponse(
        success: false,
        message: "Use IOS Version more than 15!",
        errorCode: "platformNotSupported"
    )
    
    
    
    func registrationFido(request: FidoRegRequest, completion: @escaping (Result<FidoRegResponse, Error>) -> Void) {
        finalRegResult = completion;
        finalVerifyResult = nil
        registerFido(
            rpEntityId: request.rpEntityId,
            challenge: request.challenge.data,
            userDisplayName: request.userDisplayName,
            userName: request.userName,
            userId: request.userId.data,
            publicCredentials: request.pubKeyIds
        );
    }
    
    func verifyFido(request: FidoVerifyRequest, completion: @escaping (Result<FidoVerifyResponse, Error>) -> Void) {
        finalVerifyResult = completion;
        finalRegResult = nil
        signFido(
            rpEntityId: request.rpID,
            challenge: request.challenge.data,
            allowCreds: request.allowCredentials
            )
    }
    

    @available(iOS 13.0, *)
    public func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        #if os(iOS)
        return UIApplication.shared.windows.first!
        #elseif os(macOS)
        return NSApplication.shared.windows.first!
        #endif
    }
    
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        print("IOS IS STARTED")
        let api: FidoApi & NSObjectProtocol = FidoPlugin.init()
        #if os(iOS)
        FidoApiSetup.setUp(binaryMessenger: registrar.messenger() ,api: api)
        #elseif os(macOS)
        FidoApiSetup.setUp(binaryMessenger: registrar.messenger ,api: api)
        #endif
    }
    
    func registerFido(rpEntityId :String,challenge : Data,userDisplayName:String,userName:String,userId:Data, publicCredentials: [Int64?]) {
        if #available(iOS 15.0, *) {
            print("Ios more than 15")
            print(challenge)
            print(userDisplayName)
            let platformProvider = ASAuthorizationPlatformPublicKeyCredentialProvider(relyingPartyIdentifier : rpEntityId)
            
            let key =  ASAuthorizationSecurityKeyPublicKeyCredentialProvider(relyingPartyIdentifier: rpEntityId)
        
            let platformKeyRequest = platformProvider.createCredentialRegistrationRequest(
                challenge: challenge,
                name: userName,
                userID: userId
            )
            let keyRequest = key.createCredentialRegistrationRequest(
                challenge: challenge,
                displayName: userName,
                name: userName,
                userID: userId
            )
           
            print(platformKeyRequest)
            publicCredentials.forEach { value in
                keyRequest.credentialParameters.append(ASAuthorizationPublicKeyCredentialParameters(algorithm: ASCOSEAlgorithmIdentifier(rawValue: Int(value!))) )
            }
           
            let authController = ASAuthorizationController(authorizationRequests: [platformKeyRequest, keyRequest])
            authController.delegate = self
            authController.presentationContextProvider = self
            authController.performRequests()
        } else {
            finalRegResult!(
                Result.success(failedRegResponse)
            )
        }
    }
    
    
    func signFido(rpEntityId :String, challenge : Data, allowCreds: [AllowedCredentials?]) {
        if #available(iOS 15.0, *) {
            print("Ios more than 15")
            
            let platformProvider = ASAuthorizationPlatformPublicKeyCredentialProvider(relyingPartyIdentifier : rpEntityId)
            let keyProvider = ASAuthorizationSecurityKeyPublicKeyCredentialProvider(relyingPartyIdentifier : rpEntityId)
            let platformKeyRequest = platformProvider.createCredentialAssertionRequest(challenge: challenge)
            let keyProviderRequest = keyProvider.createCredentialAssertionRequest(challenge: challenge)
            
            allowCreds.forEach { cred in
                platformKeyRequest.allowedCredentials.append(ASAuthorizationPlatformPublicKeyCredentialDescriptor(credentialID: Data(cred!.rawId.data)))
                
                keyProviderRequest.allowedCredentials.append(ASAuthorizationSecurityKeyPublicKeyCredentialDescriptor(credentialID: Data(cred!.rawId.data), transports: [ASAuthorizationSecurityKeyPublicKeyCredentialDescriptor.Transport.bluetooth, ASAuthorizationSecurityKeyPublicKeyCredentialDescriptor.Transport.nfc, ASAuthorizationSecurityKeyPublicKeyCredentialDescriptor.Transport.usb]))
            }
            
            let authController = ASAuthorizationController(authorizationRequests: [platformKeyRequest,keyProviderRequest])
            authController.delegate = self
            authController.presentationContextProvider = self
            authController.performRequests()
        } else {
            finalVerifyResult!(
                Result.success(failedVerifyResponse)
            )
        }
    }
    
    
    @available(iOS 13.0, *)
    public func authorizationController(controller: ASAuthorizationController, didCompleteWithAuthorization authorization: ASAuthorization) {
        if #available(iOS 15.0, *) {
            print("authoriation in")
            if let platformCredential = authorization.credential as? ASAuthorizationSecurityKeyPublicKeyCredentialRegistration {
                registrationSuccess(credentialID: platformCredential.credentialID, rawClientDataJSON: platformCredential.rawClientDataJSON, rawAttestationObject: platformCredential.rawAttestationObject, authenticatorAttachment: "cross-platform")

            } else if let platformCredential = authorization.credential as? ASAuthorizationPlatformPublicKeyCredentialRegistration {
                registrationSuccess(credentialID: platformCredential.credentialID, rawClientDataJSON: platformCredential.rawClientDataJSON, rawAttestationObject: platformCredential.rawAttestationObject,authenticatorAttachment: "platform")
            } else if let platformCredential = authorization.credential as? ASAuthorizationPlatformPublicKeyCredentialAssertion {
                
                verifySuccess(credentialID: platformCredential.credentialID, rawClientDataJSON: platformCredential.rawClientDataJSON, userID: platformCredential.userID, rawAuthenticatorData: platformCredential.rawAuthenticatorData, signature: platformCredential.signature)
            } else if let platformCredential = authorization.credential as? ASAuthorizationSecurityKeyPublicKeyCredentialAssertion {
                verifySuccess(credentialID: platformCredential.credentialID, rawClientDataJSON: platformCredential.rawClientDataJSON, userID: platformCredential.userID, rawAuthenticatorData: platformCredential.rawAuthenticatorData, signature: platformCredential.signature)
            }
        }else{
            finalRegResult!(
                Result.success(failedRegResponse)
            )
        }
    }
    
    public func verifySuccess(credentialID: Data, rawClientDataJSON:Data, userID:Data? ,rawAuthenticatorData:Data,signature:Data ) {
        let rawId = Data(credentialID).base64EncodedData(options: [])
        let rawClientDataJSON = Data(rawClientDataJSON).base64EncodedData(options: [])
        let userID = Data(userID ?? Data()).base64EncodedData(options: [])
        let rawAuthenticatorData = Data(rawAuthenticatorData).base64EncodedData(options: [])
        let signature = Data(signature).base64EncodedData(options: [])
      
        
        let successResponse = FidoVerifyResponse(
            success: true,
            message: "Verify Success",
            data: """
            {
            "id": "\(convert(data: rawId).toggleBase64URLSafe(on: true))",
            "type": "public-key",
            "rawId": "\(convert(data: rawId).toggleBase64URLSafe(on: true))",
            "response": {
              "clientDataJSON": "\(convert(data: rawClientDataJSON).toggleBase64URLSafe(on: true))",
              "authenticatorData": "\(convert(data: rawAuthenticatorData).toggleBase64URLSafe(on: true))",
              "signature": "\(convert(data: signature).toggleBase64URLSafe(on: true))",
              "userHandle": "\(convert(data: userID).toggleBase64URLSafe(on: true))"
            }
          }
"""
        )
        print(successResponse)
        finalVerifyResult!(
            Result.success(successResponse)
        )
    }
    
    @available(iOS 15.0, *)
    public func registrationSuccess(credentialID:Data, rawClientDataJSON: Data,rawAttestationObject:Data?, authenticatorAttachment:String) {
       
        let publicKey = Data(credentialID).base64EncodedData(options: [])
        let attestation = Data(rawAttestationObject!).base64EncodedData(options: [])
        let clientDataJSON = Data(rawClientDataJSON).base64EncodedData(options: [])
        let successResponse = FidoRegResponse(
            success: true,
            message: "Registration Success",
            data: """
            {
               "response": {
                 "clientDataJSON": "\(convert(data: clientDataJSON).toggleBase64URLSafe(on: true))",
                 "attestationObject": "\(convert(data: attestation).toggleBase64URLSafe(on: true))",
                 "transports": ["internal", "hybrid"]
                },
                "authenticatorAttachment": "\(authenticatorAttachment)",
                "clientExtensionResults": {
                    "credProps": {
                        "rk": true
                    }
                },
                "id": "\(convert(data: publicKey).toggleBase64URLSafe(on: true))",
                "rawId": "\(convert(data: publicKey).toggleBase64URLSafe(on: true))",
                "type": "public-key"
                    }
"""
        );
       
        print(successResponse)
        finalRegResult!(
            Result.success(successResponse)
        )
    }
    
    
    @available(iOS 13.0, *)
    public func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        print(error)
        let failedRegResponse = FidoRegResponse(
            success: false,
            message: "\(error.localizedDescription)",
            errorCode: "fidoRegFailed"
        )
        let failedVerifyResponse = FidoVerifyResponse(
            success: false,
            message: "\(error.localizedDescription)",
            errorCode: "fidoRegFailedVerify"
        )
        finalRegResult?(Result.success(failedRegResponse))
        finalVerifyResult?(Result.success(failedVerifyResponse))
    }
    
    
    
    private func convert(data: Data) -> String{
        if let a = String(data: data, encoding: .utf8) {
            return a;
        } else {
            return "";
        }
    }
    
}

extension String {
    func toggleBase64URLSafe(on: Bool) -> String {
        if on {
            let base64url = self.replacingOccurrences(of: "/", with: "_")
                .replacingOccurrences(of: "+", with: "-")
                .replacingOccurrences(of: "=", with: "")
            return base64url
        } else {
            var base64 = self.replacingOccurrences(of: "_", with: "/")
                .replacingOccurrences(of: "-", with: "+")
            if base64.count % 4 != 0 {
                base64.append(String(repeating: "=", count: 4 - base64.count % 4))
            }
            return base64
        }
    }
}
