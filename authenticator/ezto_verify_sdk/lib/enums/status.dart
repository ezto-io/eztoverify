enum Status {
  success,

  osSmsPermission, //Missing SMS Permission
  osPhonePermission, //Missing Phone permission
  osCameraPermission, //Missing camera permission
  osMicPermission, //Missing Mic permission
  osOtherPermission, //Missing Other permission

  platformNotSupported, //Current OS/platform is not supported

  jsMissingFields, //Mandatory fields not password through javascript

  //Ashield
  as101, //Invalid Device
  as102, //SMS Balance error
  as103, //Duplicate request
  as104, //Bad request
  as105, //Server exception
  as108, //Network error
  as109, //Unauthorized MID
  as110, //Missing parameter
  as112, //Invalid request source
  as113, //Invalid source
  as205, //Session timeout
  as211, //Auth success, MSISDN mismatch
  as209, //Auth fail
  as301, //No data connectivity
  as302, //Device has no sim card
  as303, //Device in airplane mode
  as307, //Exception in ashield api
  as308, //Permission error
  as309, //Server connection error
  as311, //Registration moved to background
  as312, //Duplicate merchant ID
  as313, //Invalid signature

  //Fido
  fidoRegFailed, //Fido registered failed
  fidoVerifyFailed // Fido verify failed
}
