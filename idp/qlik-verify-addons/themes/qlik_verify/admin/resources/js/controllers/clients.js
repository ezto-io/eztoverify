Array.prototype.remove = function (from, to) {
  var rest = this.slice((to || from) + 1 || this.length);
  this.length = from < 0 ? this.length + from : from;
  return this.push.apply(this, rest);
};

module.controller(
  "ClientTabCtrl",
  function (
    InputDialog,
    Dialog,
    $scope,
    ClientSecret,
    Current,
    Notifications,
    $route,
    $location,
    contentConfig,
    $translate,
  ) {
    $scope.mode = Current.realm.mode;
    $scope.highSecure = contentConfig.high_secure;
    $scope.browserBased = contentConfig.browser_based;
    $scope.mappers = contentConfig.mappers;
    $scope.flowOverride = contentConfig.flow_override;
    $scope.connections = contentConfig.connections;
    $scope.showTemplate =
      $scope.client.authenticationFlowBindingOverrides.ciba_auth !== "twilio-ivr-ciba-authorizer"
        && $scope.client.authenticationFlowBindingOverrides.ciba_auth !== "api-authorizer";
    $scope.removeClient = function () {
      InputDialog.confirmTenantDelete(
        $scope.client.name,
        "client",
        function () {
          $scope.client.$remove(
            {
              realm: Current.realm.realm,
              client: $scope.client.id,
            },
            function () {
              if ($scope.client.attributes["isSSO"]) {
                $location.url("/realms/" + Current.realm.realm + "/sso");
              } else {
                $location.url(
                  "/realms/" +
                  Current.realm.realm +
                  "/clients/type/" +
                  $scope.client.appType.replaceAll("_", "-"),
                );
              }
              Notifications.success($translate.instant("application-deleted"));
            },
          );
        },
      );
    };
    $scope.changePassword = function () {
      Dialog.confirmRegenerateScrt(
        $translate.instant("regenerate-client-title"),
        $translate.instant("regenerate-client-title"),
        function () {
          var secret = ClientSecret.update(
            { realm: $scope.realm.realm, client: $scope.client.id },
            function () {
              $route.reload();
              Notifications.success(
                $translate.instant("secret-has-been-changed"),
              );
              $scope.secret = secret.value;
            },
            function () {
              Notifications.error(
                $translate.instant("secret-not-changed-error"),
              );
            },
          );
        },
      );
    };
  },
);

function copyToClipboard(element) {
  /* Creates the empty input element and set the value of coming input field */
  var $temp = $("<input>");
  $("body").append($temp);
  /* Select the Value in Input field */
  $temp.val($(element).val()).select();
  /* Copy the text inside the text field */
  document.execCommand("copy");
  $temp.remove();
}
module.controller(
  "ClientRoleListCtrl",
  function (
    $scope,
    $route,
    realm,
    client,
    ClientRoleList,
    RoleById,
    Notifications,
    Dialog,
    $translate,
  ) {
    $scope.realm = realm;
    $scope.roles = [];
    $scope.client = client;

    $scope.query = {
      realm: realm.realm,
      client: $scope.client.id,
      search: null,
      max: 20,
      first: 0,
    };

    $scope.$watch(
      "query.search",
      function (newVal, oldVal) {
        if ($scope.query.search && $scope.query.search.length >= 3) {
          $scope.firstPage();
        }
      },
      true,
    );

    $scope.firstPage = function () {
      $scope.query.first = 0;
      $scope.searchQuery();
    };

    $scope.previousPage = function () {
      $scope.query.first -= parseInt($scope.query.max);
      if ($scope.query.first < 0) {
        $scope.query.first = 0;
      }
      $scope.searchQuery();
    };

    $scope.nextPage = function () {
      $scope.query.first += parseInt($scope.query.max);
      $scope.searchQuery();
    };

    $scope.searchQuery = function () {
      $scope.searchLoaded = false;

      $scope.roles = ClientRoleList.query($scope.query, function () {
        $scope.searchLoaded = true;
        $scope.lastSearch = $scope.query.search;
      });
    };

    $scope.searchQuery();

    $scope.removeRole = function (role) {
      Dialog.confirmDelete(role.name, "role", function () {
        RoleById.remove(
          {
            realm: realm.realm,
            role: role.id,
          },
          function () {
            $route.reload();
            Notifications.success($translate.instant("role-has-been-deleted"));
          },
        );
      });
    };
  },
);

module.controller(
  "ClientCredentialsCtrl",
  function (
    $scope,
    $location,
    realm,
    client,
    clientAuthenticatorProviders,
    clientConfigProperties,
    Client,
    ClientRegistrationAccessToken,
    Notifications,
    $translate,
  ) {
    $scope.realm = realm;
    $scope.client = angular.copy(client);
    $scope.clientAuthenticatorProviders = clientAuthenticatorProviders;

    var updateCurrentPartial = function (val) {
      $scope.clientAuthenticatorConfigPartial;
      switch (val) {
        case "client-secret":
          $scope.clientAuthenticatorConfigPartial =
            "client-credentials-secret.html";
          break;
        case "client-jwt":
          $scope.clientAuthenticatorConfigPartial =
            "client-credentials-jwt.html";
          break;
        case "client-secret-jwt":
          $scope.clientAuthenticatorConfigPartial =
            "client-credentials-secret-jwt.html";
          break;
        case "client-x509":
          $scope.clientAuthenticatorConfigPartial =
            "client-credentials-x509.html";
          break;
        default:
          $scope.currentAuthenticatorConfigProperties =
            clientConfigProperties[val];
          $scope.clientAuthenticatorConfigPartial =
            "client-credentials-generic.html";
          break;
      }
    };

    updateCurrentPartial(client.clientAuthenticatorType);

    $scope.$watch(
      "client.clientAuthenticatorType",
      function () {
        if (
          !angular.equals(
            $scope.client.clientAuthenticatorType,
            client.clientAuthenticatorType,
          )
        ) {
          Client.update(
            {
              realm: realm.realm,
              client: client.id,
            },
            $scope.client,
            function () {
              $scope.changed = false;
              client = angular.copy($scope.client);
              updateCurrentPartial(client.clientAuthenticatorType);
            },
          );
        }
      },
      true,
    );

    $scope.regenerateRegistrationAccessToken = function () {
      var secret = ClientRegistrationAccessToken.update(
        { realm: $scope.realm.realm, client: $scope.client.id },
        function (data) {
          Notifications.success($translate.instant("reg-access-token-updated"));
          $scope.client["registrationAccessToken"] =
            data.registrationAccessToken;
        },
        function () {
          Notifications.error("Failed to update the registration access token");
        },
      );
    };
  },
);

module.controller(
  "ClientViewCtrl",
  function (
    $scope,
    ClientTryUrl,
    CurlUtils,
    ShowCurlSnippetDialog,
    Notifications,
    $window,
    $translate,
    $route,
    $http,
    Client,
    DuplicateClient,
    ConvertSSOLogoToUrl,
    InputDialog,
    $location
  ) {
    $scope.realm = $scope.$parent.realm;
    $scope.client = $scope.$parent.client;
    $scope.appType = $scope.client.appType;
    $scope.redirectUris = $scope.client.redirectUris;
    $scope.secret = "@client_secret";
    $scope.tooltipTxt = $translate.instant("copy");
    $scope.isUploading = false;
    $scope.isWorkflow = function (client) {
      if (client.attributes["clientType"]) {
        return client.attributes["clientType"] == "workflows";
      } else {
        return false;
      }
    };
    $scope.uploadImg = () => {
      $("#imageUpload").click();
    };


  $(document).ready(function(){
      $("#imageUpload").change(function () {
          uploadLogo(this);
      });
  });

  function uploadLogo(uploader) {
    $scope.isUploading = true;
    let formData = new FormData();
    if (uploader.files[0].size > 4000000 || uploader.files[0].size <  100000) {
      $scope.isUploading = false;
      Notifications.error($translate.instant('image-size-error'));
      $scope.$apply();
      return;
    }
    formData.append($scope.client.id, uploader.files[0]);
    ConvertSSOLogoToUrl.create(
      {
        realm: $scope.realm.realm,
        client: $scope.client.id,
        type: $scope.client.appType,
      },
      formData,
      function (res) {
        $scope.clientCopy = angular.copy($scope.client);
        $scope.clientCopy.attributes = {
          ...$scope.clientCopy.attributes,
          "sso.logourl": dashboardUrl + "/" + res.url,
        };
        Client.update(
          {
            realm: $scope.realm.realm,
            client: $scope.client.id,
          },
          $scope.clientCopy,
          function () {
            $route.reload();
            $scope.isUploading = false;
            Notifications.success(
              $translate.instant("logo-updated-successfully")
            );
          },
          function () {
            $scope.isUploading = false;
            Notifications.error($translate.instant("something-went-wrong-error"));
          }
        );
      },
      function (err) {
        Notifications.error($translate.instant("something-went-wrong-error"));
        $scope.isUploading = false;
        $route.reload();
      }
    );
  }

  $scope.initCopyIcon = () => {
    $(".copy-paste-icon").click(function () {
      $scope.tooltipTxt = $translate.instant("copied");
      $(".copy-paste-icon").mouseleave(function () {
        $scope.tooltipTxt = $translate.instant("copy");
      });
    });
  }
    if ($scope.client.enabled) {
      $("#tryIt").removeAttr("disabled", "disabled");
    } else {
      $("#tryIt").attr("disabled", "disabled");
    }
    $scope.copyToClipboard = function (element) {
      /* Creates the empty input element and set the value of coming input field */
      var $temp = $("<input>");
      $("body").append($temp);
      /* Select the Value in Input field */
      $temp.val($(element).text().trim()).select();
      /* Copy the text inside the text field */
      document.execCommand("copy");
      $temp.remove();
    };

    function getFormattedRedirectURI(url) {
      if (url.endsWith("*")) {
        url = url.replace("*", "");
      }
      if (url.length == 0) {
        url = "{replace_with_redirect_url}";
        return url;
      }
      if (url.startsWith("http://") || url.startsWith("https://")) {
        return url;
      }

      return authUrl + url;
    }

    $scope.openTab = function (event, elementType) {
      if (
        $scope.appType === "browser_based" ||
        $scope.appType === "high_secure"
      ) {
        let stopFunction = createLoader(event, elementType);
        ClientTryUrl.get(
          { realm: $scope.realm.realm, client: $scope.client.id },
          function (data) {
            stopFunction && stopFunction();
            redirectInsideAsync(data.oidcurl);
          },
          function () {
            Notifications.error(
              $translate.instant("application-tryurl-fetch-failed"),
            );
            stopFunction && stopFunction();
          },
        );
      }
    };

    $scope.duplicateClient = function (client) {
        InputDialog.clientNameForDuplication(client.name, "client", function (newAppName) {
            DuplicateClient.create(
                {
                    realm: $scope.realm.realm,
                    client: client.id
                },
                {
                    clientId:client.clientId,
                    newAppName:newAppName
                },
        function (data) {
            Notifications.success($translate.instant('application-duplicated-successfully'));
            $location.path(`/realms/${$scope.realm.realm}/clients/${data.body}`);
        },
            );
        });
    };

    $scope.tryItOut = function (event, elementType) {
      let stopFunction = createLoader(event, elementType);

      ClientTryUrl.get(
        { realm: $scope.realm.realm, client: $scope.client.id },
        function (data) {
          if (
            $scope.appType === "browser_based" ||
            $scope.appType === "high_secure" ||
            $scope.appType === "iws"
          ) {
            if (!$scope.redirectUris || $scope.redirectUris.length == 0) {
              Notifications.warn(
                $translate.instant("atleast-one-redirect-uri-required"),
              );
            } else {
              let parsedURL = new URL(data.oidcurl);
              let parseParamsObj = parseQueryParamsToObject(parsedURL.search);
              let urlBody = {
                authServerUrl: parsedURL.origin + parsedURL.pathname + "?",
                ...parseParamsObj,
              };

              let tryItOutUrl = data.oidcurl;
              ShowCurlSnippetDialog.open(tryItOutUrl, $scope.appType, urlBody);
            }
          } else {
            const curlUrl = data.oidcurl;
            var curlBody = "";
            if ($scope.appType === "rest_api") {
              curlBody = {
                client_id: $scope.client.clientId,
                grant_type: "password",
                username: "@your_username",
                password: "@your_password",
              };
            } else if ($scope.appType === "machine_to_machine") {
              curlBody = {
                grant_type: "client_credentials",
                client_id: $scope.client.clientId,
                client_secret: $scope.secret,
              };
            }

            const params = {
              url: curlUrl,
              method: "POST",
              headers: {
                "Content-Type": "application/x-www-form-urlencoded",
              },
              body: curlBody,
            };
            const curlSnippet = CurlUtils.generateCurl(params);
            ShowCurlSnippetDialog.open(curlSnippet, $scope.appType);
          }
          stopFunction && stopFunction();
        },
        function () {
          Notifications.error(
            $translate.instant("application-tryurl-fetch-failed"),
          );
          stopFunction && stopFunction();
        },
      );
    };
  },
);
module.controller("ClientIdCtrl", function ($scope, $translate) {
  $scope.tooltipTxt = $translate.instant("copy");
  $(".copy-icon").click(function () {
    $scope.tooltipTxt = $translate.instant("copied");
    $(".copy-icon").mouseleave(function () {
      $scope.tooltipTxt = $translate.instant("copy");
    });
  });
  $scope.copyToClipboard = function (element) {
    /* Creates the empty input element and set the value of coming input field */
    var $temp = $("<input>");
    $("body").append($temp);
    /* Select the Value in Input field */
    $temp.val($(element).val()).select();
    /* Copy the text inside the text field */
    document.execCommand("copy");
    $temp.remove();
  };
});

module.controller(
  "ClientSecretCtrl",
  function (
    $scope,
    Dialog,
    $location,
    ClientSecret,
    Notifications,
    $translate,
    $route,
  ) {
    $scope.tooltipTxt = $translate.instant("copy");
    $(".copy-icon").click(function () {
      $scope.tooltipTxt = $translate.instant("copied");
      $(".copy-icon").mouseleave(function () {
        $scope.tooltipTxt = $translate.instant("copy");
      });
    });

    var secret = ClientSecret.get(
      { realm: $scope.realm.realm, client: $scope.client.id },
      function () {
        $scope.secret = secret.value;
      },
    );

    $scope.copyToClipboard = function (element) {
      /* Creates the empty input element and set the value of coming input field */
      var $temp = $("<input>");
      $("body").append($temp);
      /* Select the Value in Input field */
      $temp.val($(element).val()).select();
      /* Copy the text inside the text field */
      document.execCommand("copy");
      $temp.remove();
    };
    $scope.hideShowPassword = function () {
      if ($("#show_hide_password input").attr("type") === "text") {
        $("#show_hide_password input").attr("type", "password");
        $("#show_hide_password .hide_show span").removeClass("hidden_outlined");
        $("#show_hide_password .hide_show span").addClass("visible_outlined");
      } else if ($("#show_hide_password input").attr("type") === "password") {
        $("#show_hide_password input").attr("type", "text");
        $("#show_hide_password .hide_show span").addClass("hidden_outlined");
        $("#show_hide_password .hide_show span").removeClass(
          "visible_outlined",
        );
      }
    };

    $scope.$watch(
      function () {
        if ($("#show_hide_password input").attr("type") === "text") {
          $scope.tooltipTxtReveal = $translate.instant("hide-secret");
        }
        if ($("#show_hide_password input").attr("type") === "password") {
          $scope.tooltipTxtReveal = $translate.instant("reveal-secret");
        }
        return $location.path();
      },
      function () {
        $scope.path = $location.path().substring(1).split("/");
      },
    );

    $scope.cancel = function () {
      $location.url(
        "/realms/" +
        $scope.realm.realm +
        "/clients/" +
        $scope.client.id +
        "/credentials",
      );
    };

    $scope.changePassword = function () {
      Dialog.confirmRegenerateScrt(
        $translate.instant($scope.realm.mode == 'bo' ? "regenerate-app-title" : "regenerate-client-title"),
        $translate.instant($scope.realm.mode == 'bo' ? "regenerate-app-title" : "regenerate-client-title"),
        function () {
          var secret = ClientSecret.update(
            { realm: $scope.realm.realm, client: $scope.client.id },
            function () {
              if ($("#show_hide_password input").attr("type") === "text") {
                $scope.hideShowPassword();
              }
              $route.reload();
              Notifications.success(
                $translate.instant("regenerate-successful"),
              );
              $scope.secret = secret.value;
            },
            function () {
              Notifications.error($translate.instant("regenerate-failed"));
            },
          );
        },
      );
    };
  },
);

module.controller(
  "ClientX509Ctrl",
  function ($scope, $location, Client, Notifications, $translate) {
    $scope.clientCopy = angular.copy($scope.client);
    $scope.changed = false;

    $scope.$watch(
      "client",
      function () {
        if (!angular.equals($scope.client, $scope.clientCopy)) {
          $scope.changed = true;
        }
      },
      true,
    );

    $scope.save = function () {
      if (!$scope.client.attributes["x509.subjectdn"]) {
        Notifications.error("The SubjectDN must not be empty.");
      } else {
        Client.update(
          {
            realm: $scope.realm.realm,
            client: $scope.client.id,
          },
          $scope.client,
          function () {
            $scope.changed = false;
            $scope.clientCopy = angular.copy($scope.client);
            Notifications.success(
              $translate.instant("client-auth-config-saved"),
            );
          },
          function () {
            Notifications.error(
              "The SubjectDN was not changed due to a problem.",
            );
            $scope.subjectdn = "error";
          },
        );
      }
    };

    $scope.$watch(
      function () {
        return $location.path();
      },
      function () {
        $scope.path = $location.path().substring(1).split("/");
      },
    );

    $scope.reset = function () {
      $scope.client.attributes["x509.subjectdn"] =
        $scope.clientCopy.attributes["x509.subjectdn"];
      $location.url(
        "/realms/" +
        $scope.realm.realm +
        "/clients/" +
        $scope.client.id +
        "/credentials",
      );
    };
  },
);

module.controller(
  "ClientSignedJWTCtrl",
  function (
    $scope,
    $location,
    Client,
    ClientCertificate,
    Notifications,
    $route,
    $translate,
  ) {
    var signingKeyInfo = ClientCertificate.get(
      {
        realm: $scope.realm.realm,
        client: $scope.client.id,
        attribute: "jwt.credential",
      },
      function () {
        $scope.signingKeyInfo = signingKeyInfo;
      },
    );

    $scope.clientCopy = angular.copy($scope.client);
    $scope.changed = false;

    $scope.$watch(
      "client",
      function () {
        if (!angular.equals($scope.client, $scope.clientCopy)) {
          $scope.changed = true;
        }
      },
      true,
    );

    if ($scope.client.attributes["use.jwks.url"]) {
      if ($scope.client.attributes["use.jwks.url"] == "true") {
        $scope.useJwksUrl = true;
      } else {
        $scope.useJwksUrl = false;
      }
    }

    $scope.switchChange = function () {
      $scope.changed = true;
    };

    $scope.save = function () {
      if ($scope.useJwksUrl == true) {
        $scope.client.attributes["use.jwks.url"] = "true";
      } else {
        $scope.client.attributes["use.jwks.url"] = "false";
      }

      Client.update(
        {
          realm: $scope.realm.realm,
          client: $scope.client.id,
        },
        $scope.client,
        function () {
          $scope.changed = false;
          $scope.clientCopy = angular.copy($scope.client);
          Notifications.success($translate.instant("client-auth-config-saved"));
        },
      );
    };

    $scope.importCertificate = function () {
      $location.url(
        "/realms/" +
        $scope.realm.realm +
        "/clients/" +
        $scope.client.id +
        "/credentials/client-jwt/Signing/import/jwt.credential",
      );
    };

    $scope.generateSigningKey = function () {
      $location.url(
        "/realms/" +
        $scope.realm.realm +
        "/clients/" +
        $scope.client.id +
        "/credentials/client-jwt/Signing/export/jwt.credential",
      );
    };

    $scope.reset = function () {
      $route.reload();
    };
  },
);

module.controller(
  "ClientGenericCredentialsCtrl",
  function ($scope, $location, Client, Notifications, $translate) {
    $scope.clientCopy = angular.copy($scope.client);
    $scope.changed = false;

    $scope.$watch(
      "client",
      function () {
        if (!angular.equals($scope.client, $scope.clientCopy)) {
          $scope.changed = true;
        }
      },
      true,
    );

    $scope.save = function () {
      Client.update(
        {
          realm: $scope.realm.realm,
          client: $scope.client.id,
        },
        $scope.client,
        function () {
          $scope.changed = false;
          $scope.clientCopy = angular.copy($scope.client);
          Notifications.success($translate.instant("client-auth-config-saved"));
        },
      );
    };

    $scope.reset = function () {
      $scope.client = angular.copy($scope.clientCopy);
      $scope.changed = false;
    };
  },
);

module.controller(
  "ClientIdentityProviderCtrl",
  function (
    $scope,
    $location,
    $route,
    realm,
    client,
    Client,
    $location,
    Notifications,
    $translate,
  ) {
    $scope.realm = realm;
    $scope.client = angular.copy(client);
    var length = 0;

    if ($scope.client.identityProviders) {
      length = $scope.client.identityProviders.length;

      for (i = 0; i < $scope.client.identityProviders.length; i++) {
        var clientProvider = $scope.client.identityProviders[i];
        if (clientProvider.retrieveToken) {
          clientProvider.retrieveToken =
            clientProvider.retrieveToken.toString();
        }
      }
    } else {
      $scope.client.identityProviders = [];
    }

    $scope.identityProviders = [];
    var providersMissingInClient = [];

    for (j = 0; j < realm.identityProviders.length; j++) {
      var identityProvider = realm.identityProviders[j];
      var clientProvider = null;

      for (i = 0; i < $scope.client.identityProviders.length; i++) {
        clientProvider = $scope.client.identityProviders[i];

        if (clientProvider) {
          if (clientProvider.id == identityProvider.id) {
            $scope.identityProviders[i] = {};
            $scope.identityProviders[i].identityProvider = identityProvider;
            $scope.identityProviders[i].retrieveToken =
              clientProvider.retrieveToken;
            break;
          }

          clientProvider = null;
        }
      }

      if (clientProvider == null) {
        providersMissingInClient.push(identityProvider);
      }
    }

    for (j = 0; j < providersMissingInClient.length; j++) {
      var identityProvider = providersMissingInClient[j];

      var currentProvider = {};
      currentProvider.identityProvider = identityProvider;
      currentProvider.retrieveToken = "false";
      $scope.identityProviders.push(currentProvider);

      var currentClientProvider = {};
      currentClientProvider.id = identityProvider.id;
      currentClientProvider.retrieveToken = "false";
      $scope.client.identityProviders.push(currentClientProvider);
    }

    var oldCopy = angular.copy($scope.client);

    $scope.save = function () {
      Client.update(
        {
          realm: realm.realm,
          client: client.id,
        },
        $scope.client,
        function () {
          $scope.changed = false;
          $route.reload();
          Notifications.success(
            $translate.instant("changes-saved-to-application"),
          );
        },
      );
    };

    $scope.reset = function () {
      $scope.client = angular.copy(oldCopy);
      $scope.changed = false;
    };

    $scope.$watch(
      "client",
      function () {
        if (!angular.equals($scope.client, oldCopy)) {
          $scope.changed = true;
        }
      },
      true,
    );
  },
);

module.controller(
  "ClientSamlKeyCtrl",
  function (
    $scope,
    $location,
    $http,
    $upload,
    realm,
    client,
    ClientCertificate,
    ClientCertificateGenerate,
    ClientCertificateDownload,
    Notifications,
    $translate,
  ) {
    $scope.realm = realm;
    $scope.client = client;

    var signingKeyInfo = ClientCertificate.get(
      { realm: realm.realm, client: client.id, attribute: "saml.signing" },
      function () {
        $scope.signingKeyInfo = signingKeyInfo;
      },
    );

    $scope.generateSigningKey = function () {
      var keyInfo = ClientCertificateGenerate.generate(
        { realm: realm.realm, client: client.id, attribute: "saml.signing" },
        function () {
          Notifications.success($translate.instant("signing-key-regenerated"));
          $scope.signingKeyInfo = keyInfo;
        },
        function () {
          Notifications.error("Signing key was not regenerated.");
        },
      );
    };

    $scope.importSigningKey = function () {
      $location.url(
        "/realms/" +
        realm.realm +
        "/clients/" +
        client.id +
        "/saml/Signing/import/saml.signing",
      );
    };

    $scope.exportSigningKey = function () {
      $location.url(
        "/realms/" +
        realm.realm +
        "/clients/" +
        client.id +
        "/saml/Signing/export/saml.signing",
      );
    };

    var encryptionKeyInfo = ClientCertificate.get(
      { realm: realm.realm, client: client.id, attribute: "saml.encryption" },
      function () {
        $scope.encryptionKeyInfo = encryptionKeyInfo;
      },
    );

    $scope.generateEncryptionKey = function () {
      var keyInfo = ClientCertificateGenerate.generate(
        { realm: realm.realm, client: client.id, attribute: "saml.encryption" },
        function () {
          Notifications.success(
            $translate.instant("encryption-key-regenerated"),
          );
          $scope.encryptionKeyInfo = keyInfo;
        },
        function () {
          Notifications.error("Encryption key was not regenerated.");
        },
      );
    };

    $scope.importEncryptionKey = function () {
      $location.url(
        "/realms/" +
        realm.realm +
        "/clients/" +
        client.id +
        "/saml/Encryption/import/saml.encryption",
      );
    };

    $scope.exportEncryptionKey = function () {
      $location.url(
        "/realms/" +
        realm.realm +
        "/clients/" +
        client.id +
        "/saml/Encryption/export/saml.encryption",
      );
    };

    $scope.$watch(
      function () {
        return $location.path();
      },
      function () {
        $scope.path = $location.path().substring(1).split("/");
      },
    );
  },
);

module.controller(
  "ClientCertificateImportCtrl",
  function (
    $scope,
    $location,
    $http,
    $upload,
    realm,
    client,
    callingContext,
    $routeParams,
    ClientCertificate,
    ClientCertificateGenerate,
    ClientCertificateDownload,
    Notifications,
    $translate,
  ) {
    var keyType = $routeParams.keyType;
    var attribute = $routeParams.attribute;
    $scope.realm = realm;
    $scope.client = client;
    $scope.keyType = keyType;

    if (callingContext == "saml") {
      var uploadUrl =
        authUrl +
        "/admin/realms/" +
        realm.realm +
        "/clients/" +
        client.id +
        "/certificates/" +
        attribute +
        "/upload";
      var redirectLocation =
        "/realms/" + realm.realm + "/clients/" + client.id + "/saml/keys";
    } else if (callingContext == "jwt-credentials") {
      var uploadUrl =
        authUrl +
        "/admin/realms/" +
        realm.realm +
        "/clients/" +
        client.id +
        "/certificates/" +
        attribute +
        "/upload-certificate";
      var redirectLocation =
        "/realms/" + realm.realm + "/clients/" + client.id + "/credentials";
    }

    $scope.files = [];

    $scope.onFileSelect = function ($files) {
      $scope.files = $files;
    };

    $scope.cancel = function () {
      $location.url(redirectLocation);
    };

    $scope.keyFormats = ["JKS", "PKCS12", "Certificate PEM"];

    if (callingContext == "jwt-credentials") {
      $scope.keyFormats.push("Public Key PEM");
      $scope.keyFormats.push("JSON Web Key Set");
    }

    $scope.hideKeystoreSettings = function () {
      return (
        $scope.uploadKeyFormat == "Certificate PEM" ||
        $scope.uploadKeyFormat == "Public Key PEM" ||
        $scope.uploadKeyFormat == "JSON Web Key Set"
      );
    };

    $scope.uploadKeyFormat = $scope.keyFormats[0];

    $scope.uploadFile = function () {
      //$files: an array of files selected, each file has name, size, and type.
      for (var i = 0; i < $scope.files.length; i++) {
        var $file = $scope.files[i];
        $scope.upload = $upload
          .upload({
            url: uploadUrl,
            // method: POST or PUT,
            // headers: {'headerKey': 'headerValue'}, withCredential: true,
            data: {
              keystoreFormat: $scope.uploadKeyFormat,
              keyAlias: $scope.uploadKeyAlias,
              keyPassword: $scope.uploadKeyPassword,
              storePassword: $scope.uploadStorePassword,
            },
            file: $file,
            /* set file formData name for 'Content-Desposition' header. Default: 'file' */
            //fileFormDataName: myFile,
            /* customize how data is added to formData. See #40#issuecomment-28612000 for example */
            //formDataAppender: function(formData, key, val){}
          })
          .then(function (data, status, headers) {
            Notifications.success($translate.instant("keystore-uploaded"));
            $location.url(redirectLocation);
          });
        //.then(success, error, progress);
      }
    };

    $scope.$watch(
      function () {
        return $location.path();
      },
      function () {
        $scope.path = $location.path().substring(1).split("/");
      },
    );
  },
);

module.controller(
  "ClientCertificateExportCtrl",
  function (
    $scope,
    $location,
    $http,
    $upload,
    realm,
    client,
    callingContext,
    $routeParams,
    ClientCertificate,
    ClientCertificateGenerate,
    ClientCertificateDownload,
    Notifications,
    $translate,
  ) {
    var keyType = $routeParams.keyType;
    var attribute = $routeParams.attribute;
    $scope.realm = realm;
    $scope.client = client;
    $scope.keyType = keyType;

    if (callingContext == "saml") {
      var downloadUrl =
        authUrl +
        "/admin/realms/" +
        realm.realm +
        "/clients/" +
        client.id +
        "/certificates/" +
        attribute +
        "/download";
      var realmCertificate = true;
    } else if (callingContext == "jwt-credentials") {
      var downloadUrl =
        authUrl +
        "/admin/realms/" +
        realm.realm +
        "/clients/" +
        client.id +
        "/certificates/" +
        attribute +
        "/generate-and-download";
      var realmCertificate = false;
    }

    var jks = {
      keyAlias: client.clientId,
      realmAlias: realm.realm,
      realmCertificate: realmCertificate,
    };

    $scope.keyFormats = ["JKS", "PKCS12"];

    var keyInfo = ClientCertificate.get(
      { realm: realm.realm, client: client.id, attribute: attribute },
      function () {
        $scope.keyInfo = keyInfo;
      },
    );
    $scope.jks = jks;
    $scope.jks.format = $scope.keyFormats[0];

    $scope.download = function () {
      $http({
        url: downloadUrl,
        method: "POST",
        responseType: "arraybuffer",
        data: $scope.jks,
        headers: {
          "Content-Type": "application/json",
          Accept: "application/octet-stream",
        },
      })
        .then(function (response) {
          var blob = new Blob([response.data], {
            type: "application/octet-stream",
          });
          var ext = ".jks";
          if ($scope.jks.format == "PKCS12") ext = ".p12";

          if (callingContext == "jwt-credentials") {
            $location.url(
              "/realms/" +
              realm.realm +
              "/clients/" +
              client.id +
              "/credentials",
            );
            Notifications.success(
              $translate.instant("keypair-and-cert-generated"),
            );
          }

          saveAs(blob, "keystore" + ext);
        })
        .catch(function (response) {
          var errorMsg = "Error downloading";
          try {
            var error = JSON.parse(
              String.fromCharCode.apply(null, new Uint8Array(response.data)),
            );
            errorMsg = error["error_description"]
              ? error["error_description"]
              : errorMsg;
          } catch (err) { }
          Notifications.error(errorMsg);
        });
    };

    $scope.$watch(
      function () {
        return $location.path();
      },
      function () {
        $scope.path = $location.path().substring(1).split("/");
      },
    );

    $scope.cancel = function () {
      $location.url(
        "/realms/" + realm.realm + "/clients/" + client.id + "/credentials",
      );
    };
  },
);

module.controller(
  "ClientSessionsCtrl",
  function ($scope, realm, sessionCount, client, ClientUserSessions) {
    $scope.realm = realm;
    $scope.count = sessionCount.count;
    $scope.sessions = [];
    $scope.client = client;

    $scope.page = 0;

    $scope.query = {
      realm: realm.realm,
      client: $scope.client.id,
      max: 5,
      first: 0,
    };

    $scope.firstPage = function () {
      $scope.query.first = 0;
      if ($scope.query.first < 0) {
        $scope.query.first = 0;
      }
      $scope.loadUsers();
    };

    $scope.previousPage = function () {
      $scope.query.first -= parseInt($scope.query.max);
      if ($scope.query.first < 0) {
        $scope.query.first = 0;
      }
      $scope.loadUsers();
    };

    $scope.nextPage = function () {
      $scope.query.first += parseInt($scope.query.max);
      $scope.loadUsers();
    };

    $scope.toDate = function (val) {
      return new Date(val);
    };

    $scope.loadUsers = function () {
      ClientUserSessions.query($scope.query, function (updated) {
        $scope.sessions = updated;
      });
    };
  },
);

module.controller(
  "ClientOfflineSessionsCtrl",
  function ($scope, realm, offlineSessionCount, client, ClientOfflineSessions) {
    $scope.realm = realm;
    $scope.count = offlineSessionCount.count;
    $scope.sessions = [];
    $scope.client = client;

    $scope.page = 0;

    $scope.query = {
      realm: realm.realm,
      client: $scope.client.id,
      max: 5,
      first: 0,
    };

    $scope.firstPage = function () {
      $scope.query.first = 0;
      if ($scope.query.first < 0) {
        $scope.query.first = 0;
      }
      $scope.loadUsers();
    };

    $scope.previousPage = function () {
      $scope.query.first -= parseInt($scope.query.max);
      if ($scope.query.first < 0) {
        $scope.query.first = 0;
      }
      $scope.loadUsers();
    };

    $scope.nextPage = function () {
      $scope.query.first += parseInt($scope.query.max);
      $scope.loadUsers();
    };

    $scope.toDate = function (val) {
      return new Date(val);
    };

    $scope.loadUsers = function () {
      ClientOfflineSessions.query($scope.query, function (updated) {
        $scope.sessions = updated;
      });
    };
  },
);

module.controller(
  "ClientRoleDetailCtrl",
  function (
    $scope,
    $route,
    realm,
    client,
    role,
    roles,
    Client,
    Role,
    ClientRole,
    RoleById,
    RoleRealmComposites,
    RoleClientComposites,
    $http,
    $location,
    Dialog,
    Notifications,
    ComponentUtils,
    $translate,
  ) {
    $scope.realm = realm;
    $scope.client = client;
    $scope.role = angular.copy(role);
    $scope.create = !role.name;

    $scope.changed = $scope.create;

    $scope.save = function () {
      convertAttributeValuesToLists();
      if ($scope.create) {
        ClientRole.save(
          {
            realm: realm.realm,
            client: client.id,
          },
          $scope.role,
          function (data, headers) {
            $scope.changed = false;
            convertAttributeValuesToString($scope.role);
            role = angular.copy($scope.role);

            ClientRole.get(
              { realm: realm.realm, client: client.id, role: role.name },
              function (role) {
                var id = role.id;
                $location.url(
                  "/realms/" +
                  realm.realm +
                  "/clients/" +
                  client.id +
                  "/roles/" +
                  id,
                );
                Notifications.success($translate.instant("role-created"));
              },
            );
          },
        );
      } else {
        $scope.update();
      }
    };

    $scope.remove = function () {
      Dialog.confirmDelete($scope.role.name, "role", function () {
        $scope.role.$remove(
          {
            realm: realm.realm,
            client: client.id,
            role: $scope.role.id,
          },
          function () {
            $location.url(
              "/realms/" + realm.realm + "/clients/" + client.id + "/roles",
            );
            Notifications.success($translate.instant("role-has-been-deleted"));
          },
        );
      });
    };

    $scope.cancel = function () {
      $location.url(
        "/realms/" + realm.realm + "/clients/" + client.id + "/roles",
      );
    };

    $scope.addAttribute = function () {
      $scope.role.attributes[$scope.newAttribute.key] =
        $scope.newAttribute.value;
      delete $scope.newAttribute;
    };

    $scope.removeAttribute = function (key) {
      delete $scope.role.attributes[key];
    };

    function convertAttributeValuesToLists() {
      var attrs = $scope.role.attributes;
      for (var attribute in attrs) {
        if (typeof attrs[attribute] === "string") {
          var attrVals = attrs[attribute].split("##");
          attrs[attribute] = attrVals;
        }
      }
    }

    function convertAttributeValuesToString(role) {
      var attrs = role.attributes;
      for (var attribute in attrs) {
        if (typeof attrs[attribute] === "object") {
          var attrVals = attrs[attribute].join("##");
          attrs[attribute] = attrVals;
        }
      }
    }

    roleControl(
      $scope,
      $route,
      realm,
      role,
      roles,
      Client,
      ClientRole,
      RoleById,
      RoleRealmComposites,
      RoleClientComposites,
      $http,
      $location,
      Notifications,
      Dialog,
      ComponentUtils,
    );
  },
);

module.controller(
  "ClientRoleMembersCtrl",
  function (
    $scope,
    realm,
    client,
    role,
    ClientRoleMembership,
    Dialog,
    Notifications,
    $location,
  ) {
    $scope.realm = realm;
    $scope.page = 0;
    $scope.role = role;
    $scope.client = client;

    $scope.query = {
      realm: realm.realm,
      role: role.name,
      client: client.id,
      max: 5,
      first: 0,
    };

    $scope.firstPage = function () {
      $scope.query.first = 0;
      $scope.searchQuery();
    };

    $scope.previousPage = function () {
      $scope.query.first -= parseInt($scope.query.max);
      if ($scope.query.first < 0) {
        $scope.query.first = 0;
      }
      $scope.searchQuery();
    };

    $scope.nextPage = function () {
      $scope.query.first += parseInt($scope.query.max);
      $scope.searchQuery();
    };

    $scope.searchQuery = function () {
      $scope.searchLoaded = false;

      $scope.users = ClientRoleMembership.query($scope.query, function () {
        $scope.searchLoaded = true;
        $scope.lastSearch = $scope.query.search;
      });
    };

    $scope.searchQuery();
  },
);

module.controller(
  "ClientImportCtrl",
  function (
    $scope,
    $location,
    $upload,
    realm,
    serverInfo,
    Notifications,
    $translate,
  ) {
    $scope.realm = realm;

    $scope.files = [];

    $scope.onFileSelect = function ($files) {
      $scope.files = $files;
    };

    $scope.clearFileSelect = function () {
      $scope.files = null;
    };

    $scope.uploadFile = function () {
      //$files: an array of files selected, each file has name, size, and type.
      for (var i = 0; i < $scope.files.length; i++) {
        var $file = $scope.files[i];
        $scope.upload = $upload
          .upload({
            url:
              authUrl +
              "/admin/realms/" +
              realm.realm +
              "/client-importers/" +
              $scope.configFormat.id +
              "/upload",
            // method: POST or PUT,
            // headers: {'headerKey': 'headerValue'}, withCredential: true,
            data: { myObj: "" },
            file: $file,
            /* set file formData name for 'Content-Desposition' header. Default: 'file' */
            //fileFormDataName: myFile,
            /* customize how data is added to formData. See #40#issuecomment-28612000 for example */
            //formDataAppender: function(formData, key, val){}
          })
          .success(function (data, status, headers) {
            Notifications.success($translate.instant("uploaded-successfully"));
            $location.url("/realms/" + realm.realm + "/clients");
          })
          .error(function () {
            Notifications.error(
              "The file can not be uploaded. Please verify the file.",
            );
          });
        //.then(success, error, progress);
      }
    };

    $scope.$watch(
      function () {
        return $location.path();
      },
      function () {
        $scope.path = $location.path().substring(1).split("/");
      },
    );
  },
);

module.controller(
  "ClientSSOListCtrl",
  function (
    $scope,
    realm,
    ClientSSO,
    ClientsSSOCount,
    Client,
    ClientTryUrl,
    ClientListSearchState,
    $route,
    Notifications,
    Session,
    InputDialog,
    serverInfo,
    $window,
    $http,
    $translate,
  ) {
    $scope.filterQuery = {};
    $scope.searchBy = "name";
    $scope.init = function () {
      $scope.realm = realm;
      $scope.searchLoaded = true;
      $scope.serverInfo = serverInfo;
      ClientListSearchState.query.realm = realm.realm;
      $scope.query = ClientListSearchState.query;
      $scope.loaderTypeName = "cardListLoader";
      $scope.entries = 0;
      $scope.pageCount = 5;
      $scope.startCount = 1;
      $scope.maxValueOfPage = 0;
      $scope.query.max = Session.getItem("clientSSOPageFilterCount")
        ? Session.getItem("clientSSOPageFilterCount")
        : 5;
      $scope.query.first = 0;
      $scope.query.orderBy = 'last-updated'
      $scope.orderBy = $translate.instant($scope.query.orderBy);
      $scope.OldElement = angular.copy($scope.query.max);
      $scope.searchQueryLoaded = false;
      if ($scope.query && $scope.query.first === 0 && $scope.query.clientName) {
        $scope.query.clientName = "";
      }
      if (ClientListSearchState.isFirstSearch) {
        $scope.query.clientId = null;
      }
      initialQueryUpdate();
    };

    $scope.setOrderBy = (value) => {
      $scope.query.orderBy = value;
      $scope.orderBy = $translate.instant($scope.query.orderBy)
      $scope.showFilter = false;
      $scope.queryUpdate();
    }

    $scope.tooltipTxt = $translate.instant("copy");

    $scope.isInBuilt = function (client) {
      if (client.attributes["isInBuilt"]) {
        return client.attributes["isInBuilt"] == "true";
      } else {
        return false;
      }
    };

    $scope.copyToClipboard = function (element, elementVal) {
      var $temp = $("<input>");
      $("body").append($temp);
      $temp.val(elementVal).select();
      document.execCommand("copy");
      $scope.tooltipTxt = $translate.instant("copied");
      $temp.remove();
      //on MouseLeave the $scope.tooltipTxt will be changed to back.
      $(element).mouseleave(function () {
        $scope.tooltipTxt = $translate.instant("copy");
      });
    };

    $scope.searchQuery = function () {
      if ($scope.lastSearch !== $scope.query.clientName) {
        $scope.searchLoaded = false;
        if ($scope.query.clientName) {
          $scope.searchQueryLoaded = true;
        } else {
          $scope.searchQueryLoaded = false;
        }
        searchQueryWithLoaded();
        oldQueryUpdate();
        $scope.query.first = 0;
        $scope.startCount = 1;
        ClientSSO.query($scope.query, function (data) {
          $scope.clients = data;
          $scope.searchLoaded = true;
          $scope.lastSearch = $scope.query.clientName;
          ClientListSearchState.isFirstSearch = false;
        });
      }
    };

    $scope.selectQuery = function () {
      Session.setItem("clientSSOPageFilterCount", $scope.query.max);
      $scope.searchLoaded = false;
      searchQueryWithLoaded();
      oldQueryUpdate();
      ClientSSO.query($scope.query, function (data) {
        $scope.clients = data;
        $scope.searchLoaded = true;
        $scope.lastSearch = $scope.query.search;
        ClientListSearchState.isFirstSearch = false;
      });
    };

    $scope.$watch(
      "query.max",
      function () {
        $scope.query.first = 0;
      },
      true,
    );

    $scope.reset = function () {
      $scope.query.clientName = "";
      $route.reload();
    };

    $scope.clearSearch = function () {
      $scope.query.clientName = "";
      $scope.searchQuery();
    };

    $scope.firstPage = function () {
      searchQueryWithLoaded();
      $scope.pageCount = parseInt($scope.query.max);
      $scope.query.first = 0;
      $scope.startCount = 1;
      $scope.queryUpdate();
    };
    function oldQueryUpdate() {
      $scope.lengthQuery = {};
      if ($scope.query.max !== $scope.OldElement) {
        $scope.query.first = 0;
      }
      $scope.searchLoaded = false;
      let querySelectorValue = {};
      for (var i in $scope.query) {
        if (!$scope.query[i] === "") {
          delete $scope.query[i];
        } else if (i !== "max" && i !== "first") {
          querySelectorValue = {
            [i]: $scope.query[i],
          };
          Object.assign($scope.lengthQuery, querySelectorValue);
        }
      }
      const queryParams =
        $scope.query.clientName && $scope.query.search
          ? `clientName=${$scope.query.clientName}&search=true`
          : "";
      ClientsSSOCount.query(
        { queryParams, realm: realm.realm },
        function (res) {
          $scope.searchLoaded = true;
          const count = res.count;
          $scope.entries = count > 0 ? parseInt(count) : 0;
          if ($scope.entries !== 0 && $scope.entries < $scope.query.max) {
            $scope.pageCount = $scope.entries;
            $scope.startCount = 1;
          } else {
            $scope.pageCount = $scope.query.max;
            $scope.startCount = 1;
          }
        },
      );
    }

    function initialQueryUpdate() {
      $scope.searchLoaded = false;
      oldQueryUpdate();
      searchQueryWithLoaded();
      ClientSSO.query($scope.query, function (data) {
        $scope.clients = data;
        $scope.searchLoaded = true;
        $scope.lastSearch = $scope.query.search;
        ClientListSearchState.isFirstSearch = false;
      });
    }

    $scope.openTab = function () {
      ClientTryUrl.get(
        { realm: $scope.realm.realm, client: "sso" },
        function (data) {
          redirectInsideAsync(data.oidcurl);
        },
        function () {
          Notifications.error(
            $translate.instant("application-tryurl-fetch-failed"),
          );
        },
      );
    };

    $scope.queryUpdate = function () {
      $scope.searchLoaded = false;
      for (var i in $scope.query) {
        if ($scope.query[i] === "") {
          delete $scope.query[i];
        }
      }
      $scope.clients = ClientSSO.query($scope.query, function () {
        $scope.searchLoaded = true;
      });
    };

    function searchQueryWithLoaded() {
      $scope.searchLoaded = false;
      if ($scope.query.clientName && $scope.searchQueryLoaded) {
        $scope.searchQueryLoaded = true;
        $scope.query.search = true;
      } else {
        $scope.query.clientName = "";
        $scope.searchQueryLoaded = false;
      }
    }
    $scope.previousPage = function () {
      searchQueryWithLoaded();
      $scope.startCount = $scope.startCount - $scope.query.max;
      let pageCount = $scope.pageCount;
      let queryMax = parseInt($scope.query.max);
      if (pageCount === 0) {
        $scope.pageCount = queryMax - 5;
      } else if (pageCount === $scope.entries) {
        let backFlow = pageCount + queryMax - $scope.maxValueOfPage;
        $scope.pageCount = pageCount - backFlow;
      } else {
        $scope.pageCount -= queryMax;
      }
      $scope.query.first -= parseInt($scope.query.max);
      if ($scope.query.first < 0) {
        $scope.query.first = 0;
      }
      $scope.queryUpdate();
    };

    $scope.nextPage = function () {
      searchQueryWithLoaded();
      $scope.startCount = $scope.pageCount + 1;
      let fullLenthOfPages = parseInt($scope.query.max) + $scope.pageCount;
      $scope.maxValueOfPage = parseInt($scope.query.max) + $scope.pageCount;
      $scope.pageCount =
        $scope.entries < fullLenthOfPages ? $scope.entries : fullLenthOfPages;
      $scope.query.first += parseInt($scope.query.max);
      $scope.queryUpdate();
    };

    $scope.removeClient = function (client) {
      InputDialog.confirmTenantDelete(client.name, "client", function () {
        Client.remove(
          {
            realm: realm.realm,
            client: client.id,
          },
          function () {
            $route.reload();
            Notifications.success(
              client.name + " " + $translate.instant("sso-application-deleted"),
            );
          },
        );
      });
    };

    $scope.exportClient = function (client) {
      var clientCopy = angular.copy(client);
      delete clientCopy.id;

      if (clientCopy.protocolMappers) {
        for (var i = 0; i < clientCopy.protocolMappers.length; i++) {
          delete clientCopy.protocolMappers[i].id;
        }
      }

      saveAs(
        new Blob([angular.toJson(clientCopy, 4)], { type: "application/json" }),
        clientCopy.clientId + ".json",
      );
    };

    $scope.isClientListEmpty = function () {
      return $scope.clients.length == 0;
    };
  },
);

module.controller(
  "ClientListCtrl",
  function (
    $scope,
    realm,
    Client,
    ClientsCount,
    ClientListSearchState,
    $route,
    Dialog,
    Notifications,
    Session,
    InputDialog,
    serverInfo,
    $http,
    $translate,
    $routeParams,
    $location,
    DuplicateClient,
    ChangeApplicationMode
  ) {
    $scope.showSSOTab = realm.mode !== "bo";
    $scope.showBOAppModeTab = false;
    wiamTabs = [
      "in-built",
      "sso",
      "browser-based",
      "saml",
      "high-secure",
      "rest-api",
      "machine-to-machine",
      "iws",
      "custom"
    ];
    customtabs = [
      "high-secure",
      "saml",
      "rest-api",
      "machine-to-machine",
    ]
    ciamTabs = [
      "in-built",
      "browser-based",
      "saml",
      "high-secure",
      "rest-api",
      "machine-to-machine",
      "custom"
    ];
    backOfficeTabs = ["ciba"];

    if (realm.mode === "bo" && !backOfficeTabs.includes($routeParams.type)) {
      $location.url("/notfound");
    } else if (realm.mode === "ciam" && !ciamTabs.includes($routeParams.type)) {
      $location.url("/notfound");
    } else if (realm.mode === "wiam" && !wiamTabs.includes($routeParams.type)) {
      $location.url("/notfound");
    }

    $scope.filterQuery = {};
    $scope.searchBy = "name";
    $scope.init = function () {
      $scope.realm = realm;
      $scope.searchLoaded = true;
      $scope.serverInfo = serverInfo;
      ClientListSearchState.query.realm = realm.realm;
      $scope.query = ClientListSearchState.query;
      $scope.loaderTypeName = "cardListLoader";
      $scope.entries = 0;
      $scope.pageCount = 5;
      $scope.startCount = 1;
      $scope.maxValueOfPage = 0;
      $scope.query.max = Session.getItem("pageFilterCount")
        ? Session.getItem("pageFilterCount")
        : 5;
      $scope.appType = $routeParams.type;
      $scope.query.type = $routeParams.type;
      $scope.query.sandboxMode = $routeParams.mode === "sandbox";
      $scope.query.first = 0;
      $scope.query.orderBy = 'last-updated'
      $scope.orderBy = $translate.instant($scope.query.orderBy);
      $scope.OldElement = angular.copy($scope.query.max);
      $scope.searchQueryLoaded = false;
      $scope.showFilter = false;
      if ($scope.query && $scope.query.first === 0 && $scope.query.clientName) {
        $scope.query.clientName = "";
      }
      if (ClientListSearchState.isFirstSearch) {
        $scope.query.clientId = null;
      }
      $scope.showDropdownIndex = -1;
      initialQueryUpdate();
    };
    $scope.tooltipTxt = $translate.instant("copy");

    $scope.setOrderBy = (value) => {
      $scope.query.orderBy = value;
      $scope.orderBy = $translate.instant($scope.query.orderBy)
      $scope.showFilter = false;
      $scope.queryUpdate();
    }

    $scope.isInBuilt = function (client) {
      if (client.attributes["isInBuilt"]) {
        return client.attributes["isInBuilt"] == "true";
      } else {
        return false;
      }
    };

    $scope.isWorkflow = function (client) {
      if (client.attributes["clientType"]) {
        return client.attributes["clientType"] == "workflows";
      } else {
        return false;
      }
    };

    $scope.toggleShow = function (index) {
      $scope.showDropdownIndex = index;
    };

    $scope.copyToClipboard = function (element, elementVal) {
      var $temp = $("<input>");
      $("body").append($temp);
      $temp.val(elementVal).select();
      document.execCommand("copy");
      $scope.tooltipTxt = $translate.instant("copied");
      $temp.remove();
      //on MouseLeave the $scope.tooltipTxt will be changed to back.
      $(element).mouseleave(function () {
        $scope.tooltipTxt = $translate.instant("copy");
      });
    };

    $scope.searchQuery = function () {
      if ($scope.lastSearch !== $scope.query.clientName) {
        $scope.searchLoaded = false;
        if ($scope.query.clientName) {
          $scope.searchQueryLoaded = true;
        } else {
          $scope.searchQueryLoaded = false;
        }
        searchQueryWithLoaded();
        oldQueryUpdate();
        $scope.query.first = 0;
        $scope.startCount = 1;
        Client.query($scope.query, function (data) {
          $scope.clients = data;
          $scope.searchLoaded = true;
          $scope.lastSearch = $scope.query.clientName;
          ClientListSearchState.isFirstSearch = false;
        });
      }
    };

    $scope.selectQuery = function () {
      Session.setItem("pageFilterCount", $scope.query.max);
      $scope.searchLoaded = false;
      searchQueryWithLoaded();
      oldQueryUpdate();
      Client.query($scope.query, function (data) {
        $scope.clients = data;
        $scope.searchLoaded = true;
        $scope.lastSearch = $scope.query.search;
        ClientListSearchState.isFirstSearch = false;
      });
    };

    $scope.$watch(
      "query.max",
      function () {
        $scope.query.first = 0;
      },
      true,
    );

    $scope.$watch(
      "searchQueryLoaded",
      function () {
      },
      true,
    );

    $scope.reset = function () {
      $scope.query.clientName = "";
      $route.reload();
    };
    $scope.firstPage = function () {
      searchQueryWithLoaded();
      $scope.pageCount = parseInt($scope.query.max);
      $scope.query.first = 0;
      $scope.startCount = 1;
      $scope.queryUpdate();
    };
    function oldQueryUpdate() {
      $scope.lengthQuery = {};
      if ($scope.query.max !== $scope.OldElement) {
        $scope.query.first = 0;
      }
      $scope.searchLoaded = false;
      let querySelectorValue = {};
      for (var i in $scope.query) {
        if (!$scope.query[i] === "") {
          delete $scope.query[i];
        } else if (i !== "max" && i !== "first") {
          querySelectorValue = {
            [i]: $scope.query[i],
          };
          Object.assign($scope.lengthQuery, querySelectorValue);
        }
      }
      const queryParams =
        $scope.query.clientName && $scope.query.search
          ? `&clientName=${$scope.query.clientName}&search=true`
          : "";

      ClientsCount.query(
        {
          queryParams: `type=${$scope.query.type}${queryParams}&sandboxMode=${$scope.query.sandboxMode}`,
          realm: realm.realm,
        },
        function (res) {
          const count = res.count;
          if(!realm.sandboxMode && count > 0 ){
              $scope.showBOAppModeTab = true;
          }
          if($scope.query.sandboxMode){
            $scope.searchLoaded = true;
            $scope.entries = count > 0 ? parseInt(count) : 0;
            if ($scope.entries !== 0 && $scope.entries < $scope.query.max) {
              $scope.pageCount = $scope.entries;
              $scope.startCount = 1;
            } else {
              $scope.pageCount = $scope.query.max;
              $scope.startCount = 1;
            }
          }
        },
      );


      ClientsCount.query(
        {
          queryParams: `type=${$scope.query.type}${queryParams}&sandboxMode=false`,
          realm: realm.realm,
        },
        function (res) {
          const count = res.count;
          if(realm.sandboxMode && count > 0 ){
              $scope.showBOAppModeTab = true;
          } 
          if(!$scope.query.sandboxMode){
            $scope.searchLoaded = true;
            $scope.entries = count > 0 ? parseInt(count) : 0;
            if ($scope.entries !== 0 && $scope.entries < $scope.query.max) {
              $scope.pageCount = $scope.entries;
              $scope.startCount = 1;
            } else {
              $scope.pageCount = $scope.query.max;
              $scope.startCount = 1;
            }
          }
        },
      );
    }

    $scope.changeApplicationMode = (mode, client) => {
      ChangeApplicationMode.update(
        {
            realm: $scope.realm.realm,
            client: client.id
        },
        {
            clientId:client.clientId,
            sandboxMode: mode === 'sandbox' ? false : true
        },
        function (data) {
            Notifications.success($translate.instant('application-mode-changed-successfully'));
            $location.url(`/realms/${$scope.realm.realm}/clients/type/ciba/${mode == 'sandbox' ? 'live' : 'sandbox'}`);
        },
        function (error) {
          Notifications.error($translate.instant('application-mode-change-error'));
        }
      );
    }

    function initialQueryUpdate() {
      $scope.searchLoaded = false;
      oldQueryUpdate();
      Client.query($scope.query, function (data) {
        $scope.clients = data;
        $scope.searchLoaded = true;
        $scope.lastSearch = $scope.query.search;
        ClientListSearchState.isFirstSearch = false;
      });
    }

    $scope.queryUpdate = function () {
      $scope.searchLoaded = false;
      $scope.showDropdownIndex = -1;
      for (var i in $scope.query) {
        if ($scope.query[i] === "") {
          delete $scope.query[i];
        }
      }
      Client.query($scope.query, function (data) {
        $scope.clients = data;
        $scope.searchLoaded = true;
      });
    };

    function searchQueryWithLoaded() {
      $scope.searchLoaded = false;
      if ($scope.query.clientName && $scope.searchQueryLoaded) {
        $scope.searchQueryLoaded = true;
        $scope.query.search = true;
      } else {
        $scope.query.clientName = "";
        $scope.searchQueryLoaded = false;
      }
    }
    $scope.previousPage = function () {
      searchQueryWithLoaded();
      $scope.startCount = $scope.startCount - $scope.query.max;
      let pageCount = $scope.pageCount;
      let queryMax = parseInt($scope.query.max);
      if (pageCount === 0) {
        $scope.pageCount = queryMax - 5;
      } else if (pageCount === $scope.entries) {
        let backFlow = pageCount + queryMax - $scope.maxValueOfPage;
        $scope.pageCount = pageCount - backFlow;
      } else {
        $scope.pageCount -= queryMax;
      }
      $scope.query.first -= parseInt($scope.query.max);
      if ($scope.query.first < 0) {
        $scope.query.first = 0;
      }
      $scope.queryUpdate();
    };

    $scope.nextPage = function () {
      searchQueryWithLoaded();
      $scope.startCount = $scope.pageCount + 1;
      let fullLenthOfPages = parseInt($scope.query.max) + $scope.pageCount;
      $scope.maxValueOfPage = parseInt($scope.query.max) + $scope.pageCount;
      $scope.pageCount =
        $scope.entries < fullLenthOfPages ? $scope.entries : fullLenthOfPages;
      $scope.query.first += parseInt($scope.query.max);
      $scope.queryUpdate();
    };

    $scope.removeClient = function (client) {
      InputDialog.confirmTenantDelete(client.name, "client", function () {
        Client.remove(
          {
            realm: realm.realm,
            client: client.id,
          },
          function () {
            $route.reload();
            Notifications.success($translate.instant("application-deleted"));
          },
        );
      });
    };

    $scope.duplicateClient = function (client) {
    InputDialog.clientNameForDuplication(client.name, "client", function (newAppName) {
            DuplicateClient.create(
              {
                realm: realm.realm,
                client: client.id
              },
            {
            clientId:client.clientId,
            newAppName:newAppName
            },
              function (data) {
                  Notifications.success($translate.instant('application-duplicated-successfully'));
                  $location.path(`/realms/${realm.realm}/clients/${data.body}`);
              },
            );
          });
    };

    $scope.exportClient = function (client) {
      var clientCopy = angular.copy(client);
      delete clientCopy.id;

      if (clientCopy.protocolMappers) {
        for (var i = 0; i < clientCopy.protocolMappers.length; i++) {
          delete clientCopy.protocolMappers[i].id;
        }
      }

      saveAs(
        new Blob([angular.toJson(clientCopy, 4)], { type: "application/json" }),
        clientCopy.clientId + ".json",
      );
    };

    $scope.isClientListEmpty = function () {
      return $scope.clients?.length == 0;
    };

    $scope.clearSearch = function () {
      $scope.query.clientName = "";
      $scope.searchQuery();
    };
  },
);

module.controller(
  "ClientInstallationCtrl",
  function (
    $scope,
    realm,
    client,
    serverInfo,
    ClientInstallation,
    $http,
    $routeParams,
  ) {
    $scope.realm = realm;
    $scope.client = client;
    $scope.installation = null;
    $scope.download = null;
    $scope.configFormat = null;
    $scope.filename = null;

    var protocol = client.protocol;
    if (!protocol) protocol = "openid-connect";
    $scope.configFormats = serverInfo.clientInstallations[protocol];

    $scope.changeFormat = function () {
      var url = ClientInstallation.url({
        realm: $routeParams.realm,
        client: $routeParams.client,
        provider: $scope.configFormat.id,
      });
      if ($scope.configFormat.mediaType == "application/zip") {
        $http({
          url: url,
          method: "GET",
          responseType: "arraybuffer",
          cache: false,
        }).then(function (response) {
          var installation = response.data;
          $scope.installation = installation;
        });
      } else {
        $http.get(url).then(function (response) {
          var installation = response.data;
          if ($scope.configFormat.mediaType == "application/json") {
            installation = angular.fromJson(response.data);
            installation = angular.toJson(installation, true);
          }
          $scope.installation = installation;
        });
      }
    };
    $scope.download = function () {
      saveAs(
        new Blob([$scope.installation], {
          type: $scope.configFormat.mediaType,
        }),
        $scope.configFormat.filename,
      );
    };
  },
);

module.controller(
  "ClientSSOTutorialCtrl",
  function ($scope, realm, client, serverInfo) {
    $scope.realm = realm;
    $scope.client = client;
    $scope.serverInfo = serverInfo;
    $scope.installDoc = $scope.client.attributes.installation_url
      ? $scope.client.attributes.installation_url
      : "";
  },
);

module.controller(
  "ClientSSOMetadataCtrl",
  function ($scope, realm, client, clientMetadata, serverInfo, $translate) {
    $scope.realm = realm;
    $scope.client = client;
    $scope.clientMetadata = clientMetadata;
    $scope.scriptDownloadUrl = clientMetadata.scriptDownloadUrl
      ? authUrl + clientMetadata.scriptDownloadUrl
      : undefined;
    $scope.x509DownloadUrl = authUrl + clientMetadata.x509DownloadUrl;
    $scope.metaDataDownloadUrl = authUrl + clientMetadata.metaDataDownloadUrl;
    $scope.serverInfo = serverInfo;
    $scope.signatureAlgorithms = ["SHA1", "SHA256", "SHA384", "SHA512"];
    $scope.signatureAlgorithm = "SHA1";
    $scope.isInBuilt = client.attributes.isInBuilt === 'true';
    $scope.changeAlgorithm = function () {
      switch ($scope.signatureAlgorithm) {
        case "SHA1":
          $scope.thumbPrint = clientMetadata.thumbPrint_sha_1;
          break;
        case "SHA256":
          $scope.thumbPrint = clientMetadata.thumbPrint_sha_256;
          break;
        case "SHA384":
          $scope.thumbPrint = clientMetadata.thumbPrint_sha_384;
          break;
        case "SHA512":
          $scope.thumbPrint = clientMetadata.thumbPrint_sha_512;
          break;
      }
    };
    $scope.changeAlgorithm();
    console.log(clientMetadata);
    $scope.tooltipTxt = $translate.instant("copy");
    $(".copy-icon").click(function () {
      $scope.tooltipTxt = $translate.instant("copied");
      $(".copy-icon").mouseleave(function () {
        $scope.tooltipTxt = $translate.instant("copy");
      });
    });
    $scope.code = "powershell -ExecutionPolicy ByPass -File script.ps1";
    $scope.copyValue = function (value) {
      /* Creates the empty input element and set the value of coming input field */
      var $temp = $("<input>");
      $("body").append($temp);
      /* Select the Value in Input field */
      $temp.val(value.trim()).select();
      /* Copy the text inside the text field */
      document.execCommand("copy");
      $temp.remove();
    };

    $scope.copyToClipboard = function (element) {
      let $temp = $("<input>");
      $("body").append($temp);
      $temp.val($(element).val().trim()).select();
      document.execCommand("copy");
      $temp.remove();
    };
  },
);

module.controller(
  "ClientFlowOverrideCtrl",
  function (
    $scope,
    $translate,
    actionPolicies,
    cibaPolicies,
    realm,
    client,
    flows,
    actions,
    onboardingFlows,
    loginFlows,
    recoveryFlows,
    directGrantFlows,
    $route,
    serverInfo,
    Client,
    Components,
    ClientStorageOperations,
    $location,
    Notifications,
    ConfirmExitDialog,
    $translate,
  ) {
    if (realm.mode == "bo") {
      $location.url("/notfound");
    }
    $scope.realm = realm;
    $scope.client = client;
    $scope.serverInfo = serverInfo;
    $scope.flows = [];
    $scope.clientFlows = [];
    $scope.actions = actions;
    $scope.onboardingFlows = onboardingFlows;
    $scope.loginFlows = loginFlows;
    $scope.recoveryFlows = recoveryFlows;
    $scope.directGrantFlows = directGrantFlows;
    $scope.stage = 1;
    $scope.isInBuilt = false;
    $scope.appConnections = client.appConnections;
    $scope.configuredProviders = angular.copy(realm.identityProviders);
    $scope.createSocialConnDesc = $translate.instant(
      "no-social-connection-sec-desc",
      { link: "#/realms/" + realm.realm + "/connections/social/create" },
    );
    $scope.createMFADesc = $translate.instant("no-mfa-sec-desc ", {
      link: "#/realms/" + realm.realm + "/connections/mfa/create ",
    });
    $scope.mfaConnections = client.mfaConnections;
    $scope.mfaProviders = angular.copy(realm.mfaProviders);
    $scope.actionPolicies = actionPolicies;
    $scope.allPolicies =
      $scope.client.appType == "ciba"
        ? actionPolicies.concat(cibaPolicies)
        : actionPolicies;
    $scope.action = {};
    if ($scope.client.mfaPolicyId != undefined) {
      for (const policy of $scope.allPolicies) {
        if (policy.id === $scope.client.mfaPolicyId) {
          $scope.action.selectedPolicy = policy.id;
          $scope.action.selectedPolicyType = policy.policyType;
          break;
        }
      }
    } else {
      $scope.action.selectedPolicy = "";
      $scope.action.selectedPolicyType = "action";
    }

    $scope.actionEdit = angular.copy($scope.action);

    if ($scope.client.attributes["isInBuilt"]) {
      if ($scope.client.attributes["isInBuilt"] == "true") {
        $scope.isInBuilt = true;
      } else {
        $scope.isInBuilt = false;
      }
    }
    var emptyFlow = {
      id: "",
      alias: "",
    };
    for (var i = 0; i < flows.length; i++) {
      if (flows[i].providerId == "client-flow") {
        $scope.clientFlows.push(flows[i]);
      } else {
        $scope.flows.push(flows[i]);
      }
    }
    $scope.flows.push(emptyFlow);
    $scope.clientFlows.push(emptyFlow);
    // $scope.onboardingFlows.push(emptyFlow)
    // $scope.loginFlows.push(emptyFlow)
    // $scope.recoveryFlows.push(emptyFlow)

    if (client.origin) {
      if ($scope.access.viewRealm) {
        Components.get(
          { realm: realm.realm, componentId: client.origin },
          function (link) {
            $scope.originName = link.name;
            //$scope.originLink = "#/realms/" + realm.realm + "/user-storage/providers/" + link.providerId + "/" + link.id;
          },
        );
      } else {
        // KEYCLOAK-4328
        ClientStorageOperations.simpleName.get(
          { realm: realm.realm, componentId: client.origin },
          function (link) {
            $scope.originName = link.name;
            //$scope.originLink = $location.absUrl();
          },
        );
      }
    } else {
    }

    if (!$scope.create) {
      $scope.client = client;

      $scope.clientEdit = angular.copy(client);
    }

    $scope.switchChange = function () {
      $scope.changed = true;
    };

    function isActionChanged() {
      if (!angular.equals($scope.action, $scope.actionEdit)) {
        return true;
      }
      return false;
    }

    function updatePolicyType() {
      if ($scope.actionEdit.selectedPolicy != undefined) {
        for (const policy of $scope.allPolicies) {
          if (policy.id === $scope.actionEdit.selectedPolicy) {
            $scope.actionEdit.selectedPolicy = policy.id;
            $scope.actionEdit.selectedPolicyType = policy.policyType;
            break;
          }
        }
      }
    }

    function isChanged() {
      if (!angular.equals($scope.client, $scope.clientEdit)) {
        return true;
      }
      return false;
    }

    $scope.$watch(
      "clientEdit",
      function () {
        $scope.changed = isChanged();
      },
      true,
    );

    $scope.$watch(
      "actionEdit",
      function () {
        $scope.changed = isActionChanged();
      },
      true,
    );

    $scope.openRedirect = false;
    $scope.reset = function () {
      $scope.openRedirect = true;
      $route.reload();
    };

    $scope.save = function () {
      $scope.stage = 2;
      $("#cancelBtn").attr("disabled", "disabled");
      updatePolicyType();
      $scope.clientEdit.mfaPolicyId = $scope.actionEdit.selectedPolicy;
      $scope.clientEdit.mfaPolicyType = $scope.actionEdit.selectedPolicyType;
      Client.update(
        {
          realm: realm.realm,
          client: client.id,
        },
        $scope.clientEdit,
        function () {
          $scope.stage = 1;
          $("#cancelBtn").removeAttr("disabled", "disabled");
          Notifications.success(
            $translate.instant("changes-saved-to-application"),
          );
          $scope.reset();
        },
        function () {
          $scope.reset();
        },
      );
    };

    $scope.cancel = function () {
      $location.url("/realms/" + realm.realm + "/clients");
    };

    $scope.$on("$routeChangeStart", function (event, newUrl) {
      if (!$scope.openRedirect && isChanged()) {
        showConfirmExitDialog(
          event,
          ConfirmExitDialog,
          $scope,
          $location,
          newUrl,
        );
      }
    });

    $scope.toTemplate = (type, id, alias) => {
      var link = "";
      switch (type) {
          case "settings":
            link = `/realms/${$scope.realm.realm}/authentication-settings`;
            break;
          case "mfa":
            link = `/realms/${$scope.realm.realm}/connections/mfa/settings/${id}`;
            break;
          case "social":
            link = `/realms/${$scope.realm.realm}/connections/social/settings/${id}/${alias}`;
            break;
          case "action":
            link = `/realms/${$scope.realm.realm}/authentication/actions/${$scope.clientEdit.authenticationFlowBindingOverrides[type]}`;
            break;
          case "policy":
            link = `/realms/${$scope.realm.realm}/policies/action/${id}`;
            break;
          case 'direct':
            link = `/realms/${$scope.realm.realm}/authentication/flows/${$scope.clientEdit.authenticationFlowBindingOverrides['direct_grant']}/${type}/flow`
            break;
          default:
            link = `/realms/${$scope.realm.realm}/authentication/flows/${$scope.clientEdit.authenticationFlowBindingOverrides[type]}/${type}/flow`;
            break;
      }
      $location.path(link);
    }
  },
);

module.controller(
  "TransactionClientGatepassCtrl",
  function (
    $scope,
    $translate,
    cibaPolicies,
    realm,
    client,
    $route,
    serverInfo,
    Client,
    $location,
    Notifications,
    ConfirmExitDialog,
    $translate,
  ) {
    $scope.realm = realm;
    $scope.client = client;
    $scope.serverInfo = serverInfo;
    $scope.stage = 1;
    $scope.isInBuilt = false;
    $scope.appConnections = client.appConnections;
    $scope.configuredProviders = angular.copy(realm.identityProviders);
    $scope.cibaPolicies = cibaPolicies;

    if ($scope.client.attributes["isInBuilt"]) {
      if ($scope.client.attributes["isInBuilt"] == "true") {
        $scope.isInBuilt = true;
      } else {
        $scope.isInBuilt = false;
      }
    }
    if (client.origin) {
      if ($scope.access.viewRealm) {
        Components.get(
          { realm: realm.realm, componentId: client.origin },
          function (link) {
            $scope.originName = link.name;
          },
        );
      } else {
        ClientStorageOperations.simpleName.get(
          { realm: realm.realm, componentId: client.origin },
          function (link) {
            $scope.originName = link.name;
          },
        );
      }
    }

    if (!$scope.create) {
      $scope.client = client;
      $scope.clientEdit = angular.copy(client);
    }

    $scope.switchChange = function () {
      $scope.changed = true;
    };

    function isChanged() {
      if (!angular.equals($scope.client, $scope.clientEdit)) {
        return true;
      }
      return false;
    }

    $scope.$watch(
      "clientEdit",
      function () {
        $scope.changed = isChanged();
      },
      true,
    );

    $scope.openRedirect = false;
    $scope.reset = function () {
      $scope.openRedirect = true;
      $route.reload();
    };

    $scope.save = function () {
      $scope.stage = 2;
      $("#cancelBtn").attr("disabled", "disabled");
      Client.update(
        {
          realm: realm.realm,
          client: client.id,
        },
        $scope.clientEdit,
        function () {
          $scope.stage = 1;
          $("#cancelBtn").removeAttr("disabled", "disabled");
          Notifications.success(
            $translate.instant("changes-saved-to-application"),
          );
          $scope.reset();
        },
        function () {
          $scope.reset();
        },
      );
    };

    $scope.cancel = function () {
      $location.url("/realms/" + realm.realm + "/clients");
    };

    $scope.$on("$routeChangeStart", function (event, newUrl) {
      if (!$scope.openRedirect && isChanged()) {
        showConfirmExitDialog(
          event,
          ConfirmExitDialog,
          $scope,
          $location,
          newUrl,
        );
      }
    });
  },
);

module.controller(
  "TransactionClientFlowOverrideCtrl",
  function (
    $scope,
    $translate,
    $location,
    $modal,
    actions,
    realm,
    client,
    $route,
    serverInfo,
    Client,
    $location,
    Notifications,
    ConfirmExitDialog,
    $translate,
    WorkflowPreviewDialog,
    WorkflowPreviewGif
  ) {
    $scope.realm = realm;
    $scope.client = client;
    $scope.serverInfo = serverInfo;
    $scope.stage = 1;
    $scope.isInBuilt = false;
    $scope.appConnections = client.appConnections;
    $scope.configuredProviders = angular.copy(realm.identityProviders);
    $scope.allPolicies = actions;
    $scope.action = {};
    $scope.action.selectedPolicy = "";
    $scope.changed = false;
    $scope.action.selectedPolicyType = "action";

    $scope.actionEdit = angular.copy($scope.action);

    if ($scope.client.attributes["isInBuilt"]) {
      if ($scope.client.attributes["isInBuilt"] == "true") {
        $scope.isInBuilt = true;
      } else {
        $scope.isInBuilt = false;
      }
    }
    if (client.origin) {
      if ($scope.access.viewRealm) {
        Components.get(
          { realm: realm.realm, componentId: client.origin },
          function (link) {
            $scope.originName = link.name;
          },
        );
      } else {
        ClientStorageOperations.simpleName.get(
          { realm: realm.realm, componentId: client.origin },
          function (link) {
            $scope.originName = link.name;
          },
        );
      }
    }

    if (!$scope.create) {
      client.attributes['enableCustomVerificationMsg'] = client.attributes?.enableCustomVerificationMsg == "true" ? true : (client.attributes?.enableCustomVerificationMsg == "false" ? false : true);
      client.attributes['enableCustomOnboardingMsg'] = client.attributes?.enableCustomOnboardingMsg == "true" ? true : (client.attributes?.enableCustomOnboardingMsg == "false" ? false : true);
      client.attributes.customSuccessOnboardingMsgTitle = client.attributes.customSuccessOnboardingMsgTitle ? client.attributes.customSuccessOnboardingMsgTitle : $translate.instant("onboard-completed");
      client.attributes.customSuccessOnboardingMsgDesc = client.attributes.customSuccessOnboardingMsgDesc ? client.attributes.customSuccessOnboardingMsgDesc : $translate.instant("you-can-now-close-window");
      client.attributes.customFailureOnboardingMsgTitle = client.attributes.customFailureOnboardingMsgTitle ? client.attributes.customFailureOnboardingMsgTitle : $translate.instant("onboard-failed")
      client.attributes.customFailureOnboardingMsgDesc = client.attributes.customFailureOnboardingMsgDesc ? client.attributes.customFailureOnboardingMsgDesc : $translate.instant("no-action-required");
      client.attributes.customSuccessVerificationMsgTitle =  client.attributes.customSuccessVerificationMsgTitle ?  client.attributes.customSuccessVerificationMsgTitle : $translate.instant("verification-completed");
      client.attributes.customSuccessVerificationMsgDesc = client.attributes.customSuccessVerificationMsgDesc ? client.attributes.customSuccessVerificationMsgDesc : $translate.instant("you-can-now-close-window");
      client.attributes.customFailureVerificationMsgTitle = client.attributes.customFailureVerificationMsgTitle ? client.attributes.customFailureVerificationMsgTitle : $translate.instant("verification-failed");
      client.attributes.customFailureVerificationMsgDesc = client.attributes.customFailureVerificationMsgDesc ? client.attributes.customFailureVerificationMsgDesc : $translate.instant("no-action-required");

      $scope.client = client;
      $scope.clientEdit = angular.copy(client);
    }

    $scope.switchChange = function () {
      $scope.changed = true;
    };

    function isActionChanged() {
      if (!angular.equals($scope.action, $scope.actionEdit)) {
        return true;
      }
      return false;
    }

    function updatePolicyType() {
      if ($scope.actionEdit.selectedPolicy != undefined) {
        for (const policy of $scope.allPolicies) {
          if (policy.id === $scope.actionEdit.selectedPolicy) {
            $scope.actionEdit.selectedPolicy = policy.id;
            $scope.actionEdit.selectedPolicyType = policy.policyType;
            break;
          }
        }
      }
    }

    function isChanged() {
      if (!angular.equals($scope.client, $scope.clientEdit)) {
        return true;
      }
      return false;
    }

    $scope.$watch(
      "clientEdit",
      function () {
        var changed = true;
        if(!$scope.clientEdit.authenticationFlowBindingOverrides['verification-action']){
          changed = false;
        }

        if ($scope.clientEdit.attributes['enableCustomOnboardingMsg']
        && (!$scope.clientEdit.attributes.customSuccessOnboardingMsgTitle
        || !$scope.clientEdit.attributes.customSuccessOnboardingMsgDesc
        || !$scope.clientEdit.attributes.customFailureOnboardingMsgTitle
        || !$scope.clientEdit.attributes.customFailureOnboardingMsgDesc)) {
          changed = false;
        }

        if ($scope.clientEdit.attributes['enableCustomVerificationMsg']
        && (!$scope.clientEdit.attributes.customSuccessVerificationMsgTitle
        || !$scope.clientEdit.attributes.customSuccessVerificationMsgDesc
        || !$scope.clientEdit.attributes.customFailureVerificationMsgTitle
        || !$scope.clientEdit.attributes.customFailureVerificationMsgDesc)) {
          changed = false;
        }

        if (!changed) {
          $scope.isChanged = changed;
        } else {
          $scope.changed = isChanged();
        }
      },
      true,
    );

    $scope.$watch(
      "actionEdit",
      function () {
        $scope.changed = isActionChanged();
      },
      true,
    );

    $scope.openRedirect = false;
    $scope.reset = function () {
      $scope.openRedirect = true;
      $route.reload();
    };

    $scope.isTemplatePreviewVisible = false;

    $scope.showTemplatePreview = function (selectedTemplateId, actionFlow) {
      $scope.isTemplatePreviewVisible = true;
      WorkflowPreviewGif.query({ realm: realm.realm, clientId: $scope.client.id, action: selectedTemplateId, actionFlow: actionFlow}, function (response){
        var selectedTemplate =  $scope.allPolicies.filter((t) => t.actionId === selectedTemplateId)
        WorkflowPreviewDialog.open(response, selectedTemplate[0], false)
        $scope.isTemplatePreviewVisible = false;
      }, function () {
        $scope.isTemplatePreviewVisible = false;
      })
    }

    $scope.navigateToLink = function (templateId) {
      var link = `/realms/${$scope.realm.realm}/authentication/templates/${templateId}`
      $location.path(link);
    }

    $scope.save = function () {
      $scope.stage = 2;
      $("#cancelBtn").attr("disabled", "disabled");
      updatePolicyType();
      $scope.clientEdit.mfaPolicyId = $scope.actionEdit.selectedPolicy;
      $scope.clientEdit.mfaPolicyType = $scope.actionEdit.selectedPolicyType;
      Client.update(
        {
          realm: realm.realm,
          client: client.id,
        },
        $scope.clientEdit,
        function () {
          $scope.stage = 1;
          $("#cancelBtn").removeAttr("disabled", "disabled");
          Notifications.success(
            $translate.instant("changes-saved-to-application"),
          );
          $scope.reset();
        },
        function () {
          $scope.reset();
        },
      );
    };

    $scope.cancel = function () {
      $location.url("/realms/" + realm.realm + "/clients");
    };

    $scope.$on("$routeChangeStart", function (event, newUrl) {
      if (!$scope.openRedirect && isChanged()) {
        showConfirmExitDialog(
          event,
          ConfirmExitDialog,
          $scope,
          $location,
          newUrl,
        );
      }
    });

    $scope.openPreview = (type, state) => {

      var isOnboarding = false, isVerification = false, title = "", Desc = "";

      if (type === "onboarding") {
        isOnboarding = true;
      } else {
        isVerification = true;
      }

      if (isOnboarding && state === "success") {
        title = $scope.clientEdit.attributes.customSuccessOnboardingMsgTitle;
        desc = $scope.clientEdit.attributes.customSuccessOnboardingMsgDesc;
      } else if (isOnboarding && state === "failure") {
        title = $scope.clientEdit.attributes.customFailureOnboardingMsgTitle;
        desc = $scope.clientEdit.attributes.customFailureOnboardingMsgDesc;
      } else if (isVerification && state === "success") {
        title = $scope.clientEdit.attributes.customSuccessVerificationMsgTitle;
        desc = $scope.clientEdit.attributes.customSuccessVerificationMsgDesc;
      } else {
        title = $scope.clientEdit.attributes.customFailureVerificationMsgTitle;
        desc = $scope.clientEdit.attributes.customFailureVerificationMsgDesc;
      }

      var controller = function ($scope, $modalInstance, title, desc, state, realm) {
        $scope.title = title;
        $scope.desc = desc;
        $scope.state = state;
        $scope.resourceUrl = resourceUrl;
        $scope.realm = realm;
        $scope.cancel = function () {
            $modalInstance.dismiss('cancel');
        };
        $scope.$on('$routeChangeStart', function () {
            $modalInstance.close();
        });
      };

      $modal.open({
        templateUrl: resourceUrl + '/partials/modal/preview-screen-modal.html',
        controller: controller,
        backdrop: 'static',
        resolve: { title : function(){return title}, desc : function(){return desc}, state: function() {return state}, realm: function() {return realm} }
      });
  }
  },
);

module.controller(
  "ClientMFACtrl",
  function (
    $scope,
    realm,
    client,
    $route,
    serverInfo,
    Client,
    $location,
    Notifications,
    $translate,
  ) {
    $scope.realm = realm;
    $scope.stage = 1;
    $scope.client = client;
    console.log(client);
    $scope.serverInfo = serverInfo;
    $scope.mfaConnections = client.mfaConnections;
    $scope.configuredProviders = angular.copy(realm.mfaProviders);

    if (!$scope.create) {
      $scope.client = client;
      $scope.clientEdit = angular.copy(client);
    }

    $scope.switchChange = function () {
      $scope.changed = true;
    };

    function isChanged() {
      if (!angular.equals($scope.client, $scope.clientEdit)) {
        return true;
      }
      return false;
    }

    $scope.$watch(
      "clientEdit",
      function () {
        $scope.changed = isChanged();
      },
      true,
    );

    $scope.save = function () {
      $scope.stage = 2;
      $("#cancelBtn").attr("disabled", "disabled");
      Client.update(
        {
          realm: realm.realm,
          client: client.id,
        },
        $scope.clientEdit,
        function () {
          $route.reload();
          $scope.stage = 1;
          $("#cancelBtn").removeAttr("disabled", "disabled");
          Notifications.success(
            $translate.instant("changes-saved-to-application"),
          );
        },
        function () {
          $scope.stage = 1;
          $("#cancelBtn").removeAttr("disabled", "disabled");
        },
      );
    };

    $scope.reset = function () {
      $route.reload();
    };

    $scope.cancel = function () {
      $location.url("/realms/" + realm.realm + "/clients");
    };
  },
);

module.controller(
  "ClientSSODetailCtrl",
  function (
    $scope,
    realm,
    client,
    flows,
    $route,
    serverInfo,
    Client,
    ClientDescriptionConverter,
    Components,
    ClientStorageOperations,
    $location,
    $modal,
    Dialog,
    Notifications,
    TimeUnit2,
    brandings,
    ConfirmExitDialog,
    $translate,
  ) {
    $scope.serverInfo = serverInfo;
    $scope.stage = 1;
    $scope.brandings = brandings;
    $scope.helptxt = $translate.instant("client.name.helptxt");
    $scope.appTypes = [
      "high_secure",
      "browser_based",
      "rest_api",
      "machine_to_machine",
    ];
    $scope.displayAppTypes = [
      "High Secured Application",
      "Browser Based Application",
      "REST API Application",
      "Machine to Machine Application",
    ];

    $scope.protocols = serverInfo.listProviderIds("login-protocol");

    $scope.signatureAlgorithms = [
      "RSA_SHA1",
      "RSA_SHA256",
      "RSA_SHA512",
      "DSA_SHA1",
    ];
    $scope.nameIdFormats = ["username", "email", "transient", "persistent"];
    $scope.xmlKeyNameTranformers = ["NONE", "KEY_ID", "CERT_SUBJECT"];

    $scope.canonicalization = [
      { name: "EXCLUSIVE", value: "http://www.w3.org/2001/10/xml-exc-c14n#" },
      {
        name: "EXCLUSIVE_WITH_COMMENTS",
        value: "http://www.w3.org/2001/10/xml-exc-c14n#WithComments",
      },
      {
        name: "INCLUSIVE",
        value: "http://www.w3.org/TR/2001/REC-xml-c14n-20010315",
      },
      {
        name: "INCLUSIVE_WITH_COMMENTS",
        value: "http://www.w3.org/TR/2001/REC-xml-c14n-20010315#WithComments",
      },
    ];

    $scope.requestObjectRequiredOptions = [
      { name: $translate.instant("not-required-txt"), value: "not required" },
      {
        name: $translate.instant("request-or-req-uri"),
        value: "request or request_uri",
      },
      { name: $translate.instant("req-only-txt"), value: "request only" },
      {
        name: $translate.instant("req-uri-only-txt"),
        value: "request_uri only",
      },
    ];
    $scope.changePkceCodeChallengeMethodOptions = ["S256", "plain"];

    $scope.realm = realm;
    $scope.samlAuthnStatement = false;
    $scope.samlOneTimeUseCondition = false;
    $scope.samlMultiValuedRoles = false;
    $scope.samlServerSignature = false;
    $scope.samlServerSignatureEnableKeyInfoExtension = false;
    $scope.samlAssertionSignature = false;
    $scope.samlClientSignature = false;
    $scope.samlEncrypt = false;
    $scope.samlForcePostBinding = false;
    $scope.samlForceNameIdFormat = false;
    $scope.samlXmlKeyNameTranformer = $scope.xmlKeyNameTranformers[1];
    $scope.disableAuthorizationTab = !client.authorizationServicesEnabled;
    $scope.disableServiceAccountRolesTab = !client.serviceAccountsEnabled;
    $scope.disableCredentialsTab = client.publicClient;
    $scope.appType = client.appType;
    $scope.isInBuilt = false;
    $scope.displayAppType =
      $scope.displayAppTypes[$scope.appTypes.indexOf(client.appType)];

    // KEYCLOAK-6771 Certificate Bound Token
    // https://tools.ietf.org/html/draft-ietf-oauth-mtls-08#section-3
    $scope.tlsClientCertificateBoundAccessTokens = false;

    $scope.accessTokenLifespan = TimeUnit2.asUnit(
      client.attributes["access.token.lifespan"],
    );
    $scope.samlAssertionLifespan = TimeUnit2.asUnit(
      client.attributes["saml.assertion.lifespan"],
    );
    $scope.availableAppsList = client.attributes.app_list;

    $scope.disableEvent = function (e) {
      e.preventDefault();
      return false;
    };

    if (client.origin) {
      if ($scope.access.viewRealm) {
        Components.get(
          { realm: realm.realm, componentId: client.origin },
          function (link) {
            $scope.originName = link.name;
            //$scope.originLink = "#/realms/" + realm.realm + "/user-storage/providers/" + link.providerId + "/" + link.id;
          },
        );
      } else {
        // KEYCLOAK-4328
        ClientStorageOperations.simpleName.get(
          { realm: realm.realm, componentId: client.origin },
          function (link) {
            $scope.originName = link.name;
            //$scope.originLink = $location.absUrl();
          },
        );
      }
    } else {
      console.log("origin is null");
    }

    function capitalizeFirstLetter(string) {
      return string.charAt(0).toUpperCase() + string.slice(1);
    }

    function formatLabel(label) {
      var splitted = label.split("_");
      splitted.splice(0, 1);
      splitted.forEach((element, index) => {
        splitted[index] = capitalizeFirstLetter(element);
      });
      return splitted.join(" ");
    }

    function formatName(label) {
      var splitted = label.split("_");
      splitted.splice(0, 1);
      return splitted.join("");
    }

    function getGeneralAttributes() {
      var generalAttrs = [];
      var keys = Object.keys($scope.client.attributes);
      keys.forEach((element) => {
        if (element.startsWith("edit_")) {
          if (element === "edit_supported_apps") {
            generalAttrs.push({
              propKey: element,
              name: formatName(element),
              value: $scope.client.attributes.edit_supported_apps.split(","),
              label: formatLabel(element),
            });
            return;
          }
          generalAttrs.push({
            propKey: element,
            name: formatName(element),
            value: client.attributes[element],
            label: formatLabel(element),
          });
        }
      });
      return generalAttrs;
    }

    function updateProperties() {
      if (!$scope.client.attributes) {
        $scope.client.attributes = {};
      }
      if ($scope.client.protocol) {
        $scope.protocol =
          $scope.protocols[$scope.protocols.indexOf($scope.client.protocol)];
      } else {
        $scope.protocol = $scope.protocols[0];
      }
      if ($scope.client.attributes["saml.signature.algorithm"] == "RSA_SHA1") {
        $scope.signatureAlgorithm = $scope.signatureAlgorithms[0];
      } else if (
        $scope.client.attributes["saml.signature.algorithm"] == "RSA_SHA256"
      ) {
        $scope.signatureAlgorithm = $scope.signatureAlgorithms[1];
      } else if (
        $scope.client.attributes["saml.signature.algorithm"] == "RSA_SHA512"
      ) {
        $scope.signatureAlgorithm = $scope.signatureAlgorithms[2];
      } else if (
        $scope.client.attributes["saml.signature.algorithm"] == "DSA_SHA1"
      ) {
        $scope.signatureAlgorithm = $scope.signatureAlgorithms[3];
      }
      if ($scope.client.attributes["saml_name_id_format"] == "username") {
        $scope.nameIdFormat = $scope.nameIdFormats[0];
      } else if ($scope.client.attributes["saml_name_id_format"] == "email") {
        $scope.nameIdFormat = $scope.nameIdFormats[1];
      } else if (
        $scope.client.attributes["saml_name_id_format"] == "transient"
      ) {
        $scope.nameIdFormat = $scope.nameIdFormats[2];
      } else if (
        $scope.client.attributes["saml_name_id_format"] == "persistent"
      ) {
        $scope.nameIdFormat = $scope.nameIdFormats[3];
      }
      if ($scope.client.attributes["saml.server.signature"]) {
        if ($scope.client.attributes["saml.server.signature"] == "true") {
          $scope.samlServerSignature = true;
        } else {
          $scope.samlServerSignature = false;
        }
      }
      if ($scope.client.attributes["saml.server.signature.keyinfo.ext"]) {
        if (
          $scope.client.attributes["saml.server.signature.keyinfo.ext"] ==
          "true"
        ) {
          $scope.samlServerSignatureEnableKeyInfoExtension = true;
        } else {
          $scope.samlServerSignatureEnableKeyInfoExtension = false;
        }
      }
      if (
        $scope.client.attributes[
        "saml.server.signature.keyinfo.xmlSigKeyInfoKeyNameTransformer"
        ] === "NONE"
      ) {
        $scope.samlXmlKeyNameTranformer = $scope.xmlKeyNameTranformers[0];
      } else if (
        $scope.client.attributes[
        "saml.server.signature.keyinfo.xmlSigKeyInfoKeyNameTransformer"
        ] === "KEY_ID"
      ) {
        $scope.samlXmlKeyNameTranformer = $scope.xmlKeyNameTranformers[1];
      } else if (
        $scope.client.attributes[
        "saml.server.signature.keyinfo.xmlSigKeyInfoKeyNameTransformer"
        ] === "CERT_SUBJECT"
      ) {
        $scope.samlXmlKeyNameTranformer = $scope.xmlKeyNameTranformers[2];
      }
      if ($scope.client.attributes["saml.assertion.signature"]) {
        if ($scope.client.attributes["saml.assertion.signature"] == "true") {
          $scope.samlAssertionSignature = true;
        } else {
          $scope.samlAssertionSignature = false;
        }
      }
      if ($scope.client.attributes["saml.client.signature"]) {
        if ($scope.client.attributes["saml.client.signature"] == "true") {
          $scope.samlClientSignature = true;
        } else {
          $scope.samlClientSignature = false;
        }
      }
      if ($scope.client.attributes["saml.encrypt"]) {
        if ($scope.client.attributes["saml.encrypt"] == "true") {
          $scope.samlEncrypt = true;
        } else {
          $scope.samlEncrypt = false;
        }
      }
      if ($scope.client.attributes["saml.authnstatement"]) {
        if ($scope.client.attributes["saml.authnstatement"] == "true") {
          $scope.samlAuthnStatement = true;
        } else {
          $scope.samlAuthnStatement = false;
        }
      }
      if ($scope.client.attributes["saml.onetimeuse.condition"]) {
        if ($scope.client.attributes["saml.onetimeuse.condition"] == "true") {
          $scope.samlOneTimeUseCondition = true;
        } else {
          $scope.samlOneTimeUseCondition = false;
        }
      }
      if ($scope.client.attributes["saml_force_name_id_format"]) {
        if ($scope.client.attributes["saml_force_name_id_format"] == "true") {
          $scope.samlForceNameIdFormat = true;
        } else {
          $scope.samlForceNameIdFormat = false;
        }
      }
      if ($scope.client.attributes["saml.multivalued.roles"]) {
        if ($scope.client.attributes["saml.multivalued.roles"] == "true") {
          $scope.samlMultiValuedRoles = true;
        } else {
          $scope.samlMultiValuedRoles = false;
        }
      }
      if ($scope.client.attributes["saml.force.post.binding"]) {
        if ($scope.client.attributes["saml.force.post.binding"] == "true") {
          $scope.samlForcePostBinding = true;
        } else {
          $scope.samlForcePostBinding = false;
        }
      }

      if ($scope.client.attributes["isInBuilt"]) {
        if ($scope.client.attributes["isInBuilt"] == "true") {
          $scope.isInBuilt = true;
        } else {
          $scope.isInBuilt = false;
        }
      }

      $scope.accessTokenSignedResponseAlg =
        $scope.client.attributes["access.token.signed.response.alg"];
      $scope.idTokenSignedResponseAlg =
        $scope.client.attributes["id.token.signed.response.alg"];
      $scope.idTokenEncryptedResponseAlg =
        $scope.client.attributes["id.token.encrypted.response.alg"];
      $scope.idTokenEncryptedResponseEnc =
        $scope.client.attributes["id.token.encrypted.response.enc"];

      var attrVal1 =
        $scope.client.attributes["user.info.response.signature.alg"];
      $scope.userInfoSignedResponseAlg =
        attrVal1 == null ? "unsigned" : attrVal1;

      var attrVal2 = $scope.client.attributes["request.object.signature.alg"];
      $scope.requestObjectSignatureAlg = attrVal2 == null ? "any" : attrVal2;

      var attrVal3 = $scope.client.attributes["request.object.required"];
      $scope.requestObjectRequired =
        attrVal3 == null ? "not required" : attrVal3;

      var attrVal4 = $scope.client.attributes["pkce.code.challenge.method"];
      $scope.pkceCodeChallengeMethod = attrVal4 == null ? "" : attrVal4;

      if (
        $scope.client.attributes["exclude.session.state.from.auth.response"]
      ) {
        if (
          $scope.client.attributes[
          "exclude.session.state.from.auth.response"
          ] == "true"
        ) {
          $scope.excludeSessionStateFromAuthResponse = true;
        } else {
          $scope.excludeSessionStateFromAuthResponse = false;
        }
      }

      // KEYCLOAK-6771 Certificate Bound Token
      // https://tools.ietf.org/html/draft-ietf-oauth-mtls-08#section-3
      if (
        $scope.client.attributes["tls.client.certificate.bound.access.tokens"]
      ) {
        if (
          $scope.client.attributes[
          "tls.client.certificate.bound.access.tokens"
          ] == "true"
        ) {
          $scope.tlsClientCertificateBoundAccessTokens = true;
        } else {
          $scope.tlsClientCertificateBoundAccessTokens = false;
        }
      }

      if ($scope.client.attributes["display.on.consent.screen"]) {
        if ($scope.client.attributes["display.on.consent.screen"] == "true") {
          $scope.displayOnConsentScreen = true;
        } else {
          $scope.displayOnConsentScreen = false;
        }
      }
    }

    $scope.generalChange = function (proKey) {
      var gnr = $scope.general.filter((s) => s.propKey == proKey)[0];
      if (
        gnr.value &&
        (proKey !== "edit_supported_apps"
          ? gnr.value !== $scope.clientEdit.attributes[proKey]
          : gnr.value.toString() !== $scope.clientEdit.attributes[proKey])
      ) {
        $scope.changed = true;
      } else {
        $scope.changed = false;
      }
    };

    if (!$scope.create) {
      $scope.client = client;
      updateProperties();

      $scope.general = getGeneralAttributes();
      $scope.generalEdit = angular.copy($scope.general);

      $scope.$watch("general", function (newVal, oldVal) {
        if (newVal.value !== oldVal.value) {
          $scope.changed = true;
        }
      });

      $scope.clientEdit = angular.copy(client);
      $scope.oldDisplayOnConsentScreen = angular.copy(
        $scope.displayOnConsentScreen,
      );
    }

    $scope.samlIdpInitiatedUrl = function (ssoName) {
      return (
        encodeURI(
          $location.absUrl().replace(/\/admin.*/, "/realms/") +
          realm.realm +
          "/protocol/saml/clients/",
        ) + encodeURIComponent(ssoName)
      );
    };

    $scope.importFile = function (fileContent) {
      console.debug(fileContent);
      ClientDescriptionConverter.save(
        {
          realm: realm.realm,
        },
        fileContent,
        function (data) {
          $scope.client = data;
          updateProperties();
          $scope.importing = true;

          $scope.clientEdit = angular.copy(client);
        },
      );
    };

    $scope.viewImportDetails = function () {
      $modal.open({
        templateUrl: resourceUrl + "/partials/modal/view-object.html",
        controller: "ObjectModalCtrl",
        backdrop: "static",
        resolve: {
          object: function () {
            return $scope.client;
          },
        },
      });
    };

    $scope.switchChange = function () {
      $scope.changed = true;
    };

    $scope.changeProtocol = function () {
      if ($scope.protocol == "openid-connect") {
        $scope.clientEdit.protocol = "openid-connect";
      } else if ($scope.protocol == "saml") {
        $scope.clientEdit.protocol = "saml";
      }
    };

    $scope.changeAlgorithm = function () {
      $scope.clientEdit.attributes["saml.signature.algorithm"] =
        $scope.signatureAlgorithm;
    };

    $scope.changeNameIdFormat = function () {
      $scope.clientEdit.attributes["saml_name_id_format"] = $scope.nameIdFormat;
    };

    $scope.changeSamlSigKeyNameTranformer = function () {
      $scope.clientEdit.attributes[
        "saml.server.signature.keyinfo.xmlSigKeyInfoKeyNameTransformer"
      ] = $scope.samlXmlKeyNameTranformer;
    };

    $scope.changeAccessTokenSignedResponseAlg = function () {
      $scope.clientEdit.attributes["access.token.signed.response.alg"] =
        $scope.accessTokenSignedResponseAlg;
    };

    $scope.changeIdTokenSignedResponseAlg = function () {
      $scope.clientEdit.attributes["id.token.signed.response.alg"] =
        $scope.idTokenSignedResponseAlg;
    };

    $scope.changeIdTokenEncryptedResponseAlg = function () {
      $scope.clientEdit.attributes["id.token.encrypted.response.alg"] =
        $scope.idTokenEncryptedResponseAlg;
    };

    $scope.changeIdTokenEncryptedResponseEnc = function () {
      $scope.clientEdit.attributes["id.token.encrypted.response.enc"] =
        $scope.idTokenEncryptedResponseEnc;
    };

    $scope.changeUserInfoSignedResponseAlg = function () {
      if ($scope.userInfoSignedResponseAlg === "unsigned") {
        $scope.clientEdit.attributes["user.info.response.signature.alg"] = null;
      } else {
        $scope.clientEdit.attributes["user.info.response.signature.alg"] =
          $scope.userInfoSignedResponseAlg;
      }
    };

    $scope.changeRequestObjectSignatureAlg = function () {
      if ($scope.requestObjectSignatureAlg === "any") {
        $scope.clientEdit.attributes["request.object.signature.alg"] = null;
      } else {
        $scope.clientEdit.attributes["request.object.signature.alg"] =
          $scope.requestObjectSignatureAlg;
      }
    };

    $scope.changeRequestObjectRequired = function () {
      if ($scope.requestObjectRequired === "not required") {
        $scope.clientEdit.attributes["request.object.required"] = null;
      } else {
        $scope.clientEdit.attributes["request.object.required"] =
          $scope.requestObjectRequired;
      }
    };

    $scope.changePkceCodeChallengeMethod = function () {
      if ($scope.pkceCodeChallengeMethod === "none") {
        $scope.clientEdit.attributes["pkce.code.challenge.method"] = "";
      } else {
        $scope.clientEdit.attributes["pkce.code.challenge.method"] =
          $scope.pkceCodeChallengeMethod;
      }
    };

    $scope.$watch(
      function () {
        return $location.path();
      },
      function () {
        $scope.path = $location.path().substring(1).split("/");
      },
    );

    function isChanged() {
      let isGeneralExist = true;
      $scope.general.forEach((gen) => {
        if (!gen.value) {
          isGeneralExist = false;
        }
      });
      if (!angular.equals($scope.client, $scope.clientEdit) && isGeneralExist) {
        return true;
      }
      if (
        !angular.equals(
          $scope.oldDisplayOnConsentScreen,
          $scope.displayOnConsentScreen,
        )
      ) {
        return true;
      }
      if ($scope.newRedirectUri && $scope.newRedirectUri.length > 0) {
        return true;
      }
      if ($scope.newWebOrigin && $scope.newWebOrigin.length > 0) {
        return true;
      }
      return false;
    }

    $scope.updateTimeouts = function () {
      if ($scope.accessTokenLifespan.time) {
        if ($scope.accessTokenLifespan.time === -1) {
          $scope.clientEdit.attributes["access.token.lifespan"] = -1;
        } else {
          $scope.clientEdit.attributes["access.token.lifespan"] =
            $scope.accessTokenLifespan.toSeconds();
        }
      } else {
        $scope.clientEdit.attributes["access.token.lifespan"] = null;
      }
    };

    $scope.updateAssertionLifespan = function () {
      if ($scope.samlAssertionLifespan.time) {
        $scope.clientEdit.attributes["saml.assertion.lifespan"] =
          $scope.samlAssertionLifespan.toSeconds();
      } else {
        $scope.clientEdit.attributes["saml.assertion.lifespan"] = null;
      }
    };

    function configureAuthorizationServices() {
      if ($scope.clientEdit.authorizationServicesEnabled) {
        if ($scope.accessType == "public") {
          $scope.accessType = "confidential";
        }
        $scope.clientEdit.publicClient = false;
        $scope.clientEdit.serviceAccountsEnabled = true;
      } else if ($scope.clientEdit.bearerOnly) {
        $scope.clientEdit.serviceAccountsEnabled = false;
      }
      if (
        $scope.client.authorizationServicesEnabled &&
        !$scope.clientEdit.authorizationServicesEnabled
      ) {
        Dialog.confirm(
          "Disable Authorization Settings",
          "Are you sure you want to disable authorization ? Once you save your changes, all authorization settings associated with this client will be removed. This operation can not be reverted.",
          function () { },
          function () {
            $scope.clientEdit.authorizationServicesEnabled = true;
          },
        );
      }
    }

    $scope.$watch(
      "clientEdit",
      function () {
        $scope.changed = isChanged();
        configureAuthorizationServices();
      },
      true,
    );

    $scope.$watch(
      "newRedirectUri",
      function () {
        $scope.changed = isChanged();
      },
      true,
    );

    $scope.$watch(
      "newWebOrigin",
      function () {
        $scope.changed = isChanged();
      },
      true,
    );

    $scope.$watch(
      "displayOnConsentScreen",
      function () {
        $scope.changed = isChanged();
      },
      true,
    );

    $scope.deleteWebOrigin = function (index) {
      $scope.clientEdit.webOrigins.splice(index, 1);
    };
    $scope.addWebOrigin = function () {
      $scope.clientEdit.webOrigins.push($scope.newWebOrigin);
      $scope.newWebOrigin = "";
    };
    $scope.deleteRedirectUri = function (index) {
      $scope.clientEdit.redirectUris.splice(index, 1);
    };

    $scope.addRedirectUri = function () {
      $scope.clientEdit.redirectUris.push($scope.newRedirectUri);
      $scope.newRedirectUri = "";
    };

    $scope.openRedirect = false;

    $scope.reset = function () {
      $scope.openRedirect = true;
      $route.reload();
    };

    $scope.save = function () {
      $scope.stage = 2;

      if ($scope.general && $scope.general.length > 0) {
        $scope.general.forEach((element) => {
          $scope.clientEdit.attributes[element.propKey] =
            element.propKey !== "edit_supported_apps"
              ? element.value
              : element.value.toString();
        });
      }

      if ($scope.newRedirectUri && $scope.newRedirectUri.length > 0) {
        $scope.addRedirectUri();
      }

      if ($scope.newWebOrigin && $scope.newWebOrigin.length > 0) {
        $scope.addWebOrigin();
      }

      if ($scope.samlServerSignature == true) {
        $scope.clientEdit.attributes["saml.server.signature"] = "true";
      } else {
        $scope.clientEdit.attributes["saml.server.signature"] = "false";
      }
      if ($scope.samlServerSignatureEnableKeyInfoExtension == true) {
        $scope.clientEdit.attributes["saml.server.signature.keyinfo.ext"] =
          "true";
      } else {
        $scope.clientEdit.attributes["saml.server.signature.keyinfo.ext"] =
          "false";
      }
      if ($scope.samlAssertionSignature == true) {
        $scope.clientEdit.attributes["saml.assertion.signature"] = "true";
      } else {
        $scope.clientEdit.attributes["saml.assertion.signature"] = "false";
      }
      if ($scope.samlClientSignature == true) {
        $scope.clientEdit.attributes["saml.client.signature"] = "true";
      } else {
        $scope.clientEdit.attributes["saml.client.signature"] = "false";
      }
      if ($scope.samlEncrypt == true) {
        $scope.clientEdit.attributes["saml.encrypt"] = "true";
      } else {
        $scope.clientEdit.attributes["saml.encrypt"] = "false";
      }
      if ($scope.samlAuthnStatement == true) {
        $scope.clientEdit.attributes["saml.authnstatement"] = "true";
      } else {
        $scope.clientEdit.attributes["saml.authnstatement"] = "false";
      }
      if ($scope.samlOneTimeUseCondition == true) {
        $scope.clientEdit.attributes["saml.onetimeuse.condition"] = "true";
      } else {
        $scope.clientEdit.attributes["saml.onetimeuse.condition"] = "false";
      }
      if ($scope.samlForceNameIdFormat == true) {
        $scope.clientEdit.attributes["saml_force_name_id_format"] = "true";
      } else {
        $scope.clientEdit.attributes["saml_force_name_id_format"] = "false";
      }
      if ($scope.samlMultiValuedRoles == true) {
        $scope.clientEdit.attributes["saml.multivalued.roles"] = "true";
      } else {
        $scope.clientEdit.attributes["saml.multivalued.roles"] = "false";
      }
      if ($scope.samlForcePostBinding == true) {
        $scope.clientEdit.attributes["saml.force.post.binding"] = "true";
      } else {
        $scope.clientEdit.attributes["saml.force.post.binding"] = "false";
      }

      if ($scope.excludeSessionStateFromAuthResponse == true) {
        $scope.clientEdit.attributes[
          "exclude.session.state.from.auth.response"
        ] = "true";
      } else {
        $scope.clientEdit.attributes[
          "exclude.session.state.from.auth.response"
        ] = "false";
      }

      // KEYCLOAK-6771 Certificate Bound Token
      // https://tools.ietf.org/html/draft-ietf-oauth-mtls-08#section-3
      if ($scope.tlsClientCertificateBoundAccessTokens == true) {
        $scope.clientEdit.attributes[
          "tls.client.certificate.bound.access.tokens"
        ] = "true";
      } else {
        $scope.clientEdit.attributes[
          "tls.client.certificate.bound.access.tokens"
        ] = "false";
      }

      if ($scope.displayOnConsentScreen == true) {
        $scope.clientEdit.attributes["display.on.consent.screen"] = "true";
      } else {
        $scope.clientEdit.attributes["display.on.consent.screen"] = "false";
      }

      $scope.clientEdit.protocol = $scope.protocol;
      $scope.clientEdit.attributes["saml.signature.algorithm"] =
        $scope.signatureAlgorithm;
      $scope.clientEdit.attributes["saml_name_id_format"] = $scope.nameIdFormat;
      if (
        $scope.clientEdit.protocol != "saml" &&
        !$scope.clientEdit.bearerOnly &&
        ($scope.clientEdit.standardFlowEnabled ||
          $scope.clientEdit.implicitFlowEnabled) &&
        (!$scope.clientEdit.redirectUris ||
          $scope.clientEdit.redirectUris.length == 0 ||
          !$scope.checkRedirectUriNull())
      ) {
        $scope.stage = 1;
        Notifications.error("You must specify at least one redirect uri");
      } else {
        Client.update(
          {
            realm: realm.realm,
            client: client.id,
          },
          $scope.clientEdit,
          function () {
            $scope.reset();
            $scope.stage = 1;
            Notifications.success(
              $translate.instant("changes-saved-to-application"),
            );
          },
          function () {
            $scope.stage = 1;
          },
        );
      }
    };
    $scope.checkRedirectUriNull = function () {
      for (const uri of $scope.clientEdit.redirectUris) {
        if (!uri) {
          return false;
        }
      }
      return true;
    };
    $scope.tooltipTxt = $translate.instant("copy");
    $(".copy-paste-icon").click(function () {
      $scope.tooltipTxt = $translate.instant("copied");
      $(".copy-paste-icon").mouseleave(function () {
        $scope.tooltipTxt = $translate.instant("copy");
      });
    });
    $scope.copyToClipboard = function (element) {
      /* Creates the empty input element and set the value of coming input field */
      var $temp = $("<input>");
      $("body").append($temp);
      /* Select the Value in Input field */
      $temp.val($(element).val()).select();
      /* Copy the text inside the text field */
      document.execCommand("copy");
      $temp.remove();
    };

    $scope.cancel = function () {
      $location.url("/realms/" + realm.realm + "/clients");
    };

    $scope.$on("$routeChangeStart", function (event, newUrl) {
      if (!$scope.openRedirect && isChanged()) {
        showConfirmExitDialog(
          event,
          ConfirmExitDialog,
          $scope,
          $location,
          newUrl,
        );
      }
    });
  },
);

module.controller(
  "ClientDeveloperCtrl",
  function (
    $scope,
    $translate,
    $http,
    realm,
    client,
    serverInfo,
    keys,
    Client,
    $route,
    Notifications,
  ) {
    $scope.serverInfo = serverInfo;
    $scope.realm = realm;
    $scope.client = angular.copy(client);
    $scope.clientCopy = angular.copy(client);
    $scope.keys = keys.keys.filter(
      (x) => x.algorithm == "AES" && x.status === "ACTIVE",
    );
    $scope.secret = "";
    $scope.changed = false;
    $scope.tooltipTxt = $translate.instant("copy");

    if (
      !$scope.keys.find(
        (key) => key.providerId === $scope.client.attributes.ciba_encryption_id,
      )
    ) {
      $scope.client.attributes.ciba_encryption_id = null;
    }

    if ($scope.client.attributes["enable_ciba_request_encryption"]) {
      if (
        $scope.client.attributes["enable_ciba_request_encryption"] == "true"
      ) {
        $scope.encryptionToggle = true;
      } else {
        $scope.encryptionToggle = false;
      }
      if (!$scope.encryptionToggle) {
        $scope.clientCopy.attributes.ciba_encryption_id = null;
      }
    } else {
      $scope.encryptionToggle = false;
      $scope.client.attributes["enable_ciba_request_encryption"] = "false";
      $scope.clientCopy.attributes["enable_ciba_request_encryption"] = "false";
      $scope.clientCopy.attributes.ciba_encryption_id = null;
    }

    $scope.$watch(
      "client.attributes.ciba_encryption_id",
      function () {
        if ($scope.client.attributes.ciba_encryption_id) {
          $http
            .get(
              authUrl +
              "/admin/realms/" +
              realm.realm +
              "/components/" +
              $scope.client.attributes.ciba_encryption_id,
            )
            .then((res) => {
              if (res.data.config.secret) {
                $scope.secret = res.data.config.secret[0];
              } else {
                $scope.secret = "no-access";
              }
            })
            .catch((err) => {
              Notifications.error($translate.instant(err));
            });
        }
      },
      true,
    );

    $scope.$watch(
      "encryptionToggle",
      function () {
        $scope.client.attributes["enable_ciba_request_encryption"] =
          $scope.encryptionToggle.toString();
        if (!$scope.encryptionToggle) {
          $scope.client.attributes.ciba_encryption_id = null;
          $scope.secret = "";
        }
      },
      true,
    );

    $scope.$watch(
      "client",
      function () {
        if (
          !angular.equals($scope.clientCopy, $scope.client) &&
          (!$scope.encryptionToggle ||
            ($scope.encryptionToggle &&
              $scope.client.attributes.ciba_encryption_id))
        ) {
          $scope.changed = true;
        } else {
          $scope.changed = false;
        }
      },
      true,
    );

    $scope.save = function () {
      Client.update(
        { realm: realm.realm, client: client.id },
        $scope.client,
        function () {
          Notifications.success(
            $translate.instant("changes-saved-to-application"),
          );
          $route.reload();
        },
      );
    };

    $scope.reset = function () {
      $scope.client = angular.copy(client);
      $route.reload();
    };

    $scope.copyToClipboard = function (element) {
      let $temp = $("<input>");
      $("body").append($temp);
      $temp.val($(element).val().trim()).select();
      document.execCommand("copy");
      $scope.tooltipTxt = $translate.instant("copied");
      $temp.remove();
      $(element).mouseleave(function () {
        $scope.tooltipTxt = $translate.instant("copy");
      });
    };
  },
);

module.controller(
  "ClientDetailCtrl",
  function (
    $scope,
    realm,
    client,
    flows,
    $route,
    serverInfo,
    Client,
    ClientDescriptionConverter,
    Components,
    ClientStorageOperations,
    $location,
    $modal,
    Dialog,
    Notifications,
    TimeUnit2,
    brandings,
    ConfirmExitDialog,
    $translate,
    $routeParams
  ) {
    $scope.serverInfo = serverInfo;
    $scope.stage = 1;
    $scope.brandings = brandings;
    $scope.helptxt = $translate.instant("client.name.helptxt");
    $scope.debuggerUrl = serverInfo.debuggerUrl;
    $scope.appTypes = [
      "high_secure",
      "browser_based",
      "rest_api",
      "machine_to_machine",
    ];
    $scope.displayAppTypes = [
      "High Secured Application",
      "Browser Based Application",
      "REST API Application",
      "Machine to Machine Application",
      "Backoffice",
    ];
    $scope.defaultCibaRejectionReasons = [
      "Incorrect Information",
      "Suspicious Activity",
      "Transaction not triggered by me",
      "Inappropriate Transaction"
    ]

    $scope.selectedCibaRejectionReasons = client.attributes["consent_rejected_reason"] ? client.attributes["consent_rejected_reason"].split("##") : angular.copy($scope.defaultCibaRejectionReasons);
    $scope.newCibaConsentRejectReason = '';
    $scope.isConsentHelperVisible = false;
    $scope.isWorkflow = function (client) {
      if (client.attributes["clientType"]) {
        return client.attributes["clientType"] == "workflows";
      } else {
        return false;
      }
    };

    $scope.addReasonForConsent = function () {
      if($scope.newCibaConsentRejectReason.length){
        $scope.selectedCibaRejectionReasons.push($scope.newCibaConsentRejectReason);
        $scope.newCibaConsentRejectReason = "";
      }
    }

    $scope.removeReasonForConsent = function (index) {
      $scope.selectedCibaRejectionReasons.splice(index, 1);
    }


    $scope.handleKeyPress = (event, name) => {
        if(!totalCharCount(String.fromCharCode(event.which), name, event.target.value.length)) {
            event.preventDefault();
        }
    }

    const totalCharCount = (value, name, valueLen)=> {
            let isValid = true;
            let pattern = "";
            let length = 35;
            if ((pattern && !pattern.test(value)) || length < valueLen) {
                isValid = false;
            }
            return isValid;
        }

    $scope.$watch(
      "selectedCibaRejectionReasons",
      function (newValue, oldValue) {
        $scope.changed =  angular.equals(newValue, oldValue) ? (isChanged() && !angular.equals(newValue, oldValue)) : ($scope.ciba.consent ? ($scope.clientEdit.name.length && newValue.length) : $scope.clientEdit.name.length) ;
        const isSuspiciousActivity = $scope.selectedCibaRejectionReasons.includes("Suspicious Activity");
        const isTransactionNotTriggered = $scope.selectedCibaRejectionReasons.includes("Transaction not triggered by me");
        if(isSuspiciousActivity || isTransactionNotTriggered){
          $scope.isConsentHelperVisible = true;
        }else{
          $scope.isConsentHelperVisible = false;
        }
      },
      true,
    );

    $scope.protocols = serverInfo.listProviderIds("login-protocol");

    $scope.signatureAlgorithms = [
      "RSA_SHA1",
      "RSA_SHA256",
      "RSA_SHA512",
      "DSA_SHA1",
    ];
    $scope.nameIdFormats = ["username", "email", "transient", "persistent"];
    $scope.xmlKeyNameTranformers = ["NONE", "KEY_ID", "CERT_SUBJECT"];

    $scope.canonicalization = [
      { name: "EXCLUSIVE", value: "http://www.w3.org/2001/10/xml-exc-c14n#" },
      {
        name: "EXCLUSIVE_WITH_COMMENTS",
        value: "http://www.w3.org/2001/10/xml-exc-c14n#WithComments",
      },
      {
        name: "INCLUSIVE",
        value: "http://www.w3.org/TR/2001/REC-xml-c14n-20010315",
      },
      {
        name: "INCLUSIVE_WITH_COMMENTS",
        value: "http://www.w3.org/TR/2001/REC-xml-c14n-20010315#WithComments",
      },
    ];

    $scope.requestObjectRequiredOptions = [
      { name: $translate.instant("not-required-txt"), value: "not required" },
      {
        name: $translate.instant("request-or-req-uri"),
        value: "request or request_uri",
      },
      { name: $translate.instant("req-only-txt"), value: "request only" },
      {
        name: $translate.instant("req-uri-only-txt"),
        value: "request_uri only",
      },
    ];
    $scope.changePkceCodeChallengeMethodOptions = ["S256", "plain"];

    $scope.realm = realm;
    $scope.samlAuthnStatement = false;
    $scope.samlOneTimeUseCondition = false;
    $scope.samlMultiValuedRoles = false;
    $scope.samlServerSignature = false;
    $scope.samlServerSignatureEnableKeyInfoExtension = false;
    $scope.samlAssertionSignature = false;
    $scope.samlClientSignature = false;
    $scope.samlEncrypt = false;
    $scope.samlForcePostBinding = false;
    $scope.samlForceNameIdFormat = false;
    $scope.samlXmlKeyNameTranformer = $scope.xmlKeyNameTranformers[1];
    $scope.disableAuthorizationTab = !client.authorizationServicesEnabled;
    $scope.disableServiceAccountRolesTab = !client.serviceAccountsEnabled;
    $scope.disableCredentialsTab = client.publicClient;
    $scope.appType = client.appType;
    $scope.isInBuilt = false;
    $scope.sso = {
      show: false,
    };
    $scope.ciba = {
      consent: false,
    };

    $scope.displayAppType =
      $scope.displayAppTypes[$scope.appTypes.indexOf(client.appType)];

    // KEYCLOAK-6771 Certificate Bound Token
    // https://tools.ietf.org/html/draft-ietf-oauth-mtls-08#section-3
    $scope.tlsClientCertificateBoundAccessTokens = false;

    $scope.accessTokenLifespan = TimeUnit2.asUnit(
      client.attributes["access.token.lifespan"],
    );
    $scope.samlAssertionLifespan = TimeUnit2.asUnit(
      client.attributes["saml.assertion.lifespan"],
    );

    if (client.origin) {
      if ($scope.access.viewRealm) {
        Components.get(
          { realm: realm.realm, componentId: client.origin },
          function (link) {
            $scope.originName = link.name;
            //$scope.originLink = "#/realms/" + realm.realm + "/user-storage/providers/" + link.providerId + "/" + link.id;
          },
        );
      } else {
        // KEYCLOAK-4328
        ClientStorageOperations.simpleName.get(
          { realm: realm.realm, componentId: client.origin },
          function (link) {
            $scope.originName = link.name;
            //$scope.originLink = $location.absUrl();
          },
        );
      }
    } else {
      console.log("origin is null");
    }

    function updateProperties() {
      if (!$scope.client.attributes) {
        $scope.client.attributes = {};
      }
      if ($scope.client.protocol) {
        $scope.protocol =
          $scope.protocols[$scope.protocols.indexOf($scope.client.protocol)];
      } else {
        $scope.protocol = $scope.protocols[0];
      }
      if ($scope.client.attributes["saml.signature.algorithm"] == "RSA_SHA1") {
        $scope.signatureAlgorithm = $scope.signatureAlgorithms[0];
      } else if (
        $scope.client.attributes["saml.signature.algorithm"] == "RSA_SHA256"
      ) {
        $scope.signatureAlgorithm = $scope.signatureAlgorithms[1];
      } else if (
        $scope.client.attributes["saml.signature.algorithm"] == "RSA_SHA512"
      ) {
        $scope.signatureAlgorithm = $scope.signatureAlgorithms[2];
      } else if (
        $scope.client.attributes["saml.signature.algorithm"] == "DSA_SHA1"
      ) {
        $scope.signatureAlgorithm = $scope.signatureAlgorithms[3];
      }
      if ($scope.client.attributes["saml_name_id_format"] == "username") {
        $scope.nameIdFormat = $scope.nameIdFormats[0];
      } else if ($scope.client.attributes["saml_name_id_format"] == "email") {
        $scope.nameIdFormat = $scope.nameIdFormats[1];
      } else if (
        $scope.client.attributes["saml_name_id_format"] == "transient"
      ) {
        $scope.nameIdFormat = $scope.nameIdFormats[2];
      } else if (
        $scope.client.attributes["saml_name_id_format"] == "persistent"
      ) {
        $scope.nameIdFormat = $scope.nameIdFormats[3];
      }
      if ($scope.client.attributes["saml.server.signature"]) {
        if ($scope.client.attributes["saml.server.signature"] == "true") {
          $scope.samlServerSignature = true;
        } else {
          $scope.samlServerSignature = false;
        }
      }
      if ($scope.client.attributes["saml.server.signature.keyinfo.ext"]) {
        if (
          $scope.client.attributes["saml.server.signature.keyinfo.ext"] ==
          "true"
        ) {
          $scope.samlServerSignatureEnableKeyInfoExtension = true;
        } else {
          $scope.samlServerSignatureEnableKeyInfoExtension = false;
        }
      }
      if (
        $scope.client.attributes[
        "saml.server.signature.keyinfo.xmlSigKeyInfoKeyNameTransformer"
        ] === "NONE"
      ) {
        $scope.samlXmlKeyNameTranformer = $scope.xmlKeyNameTranformers[0];
      } else if (
        $scope.client.attributes[
        "saml.server.signature.keyinfo.xmlSigKeyInfoKeyNameTransformer"
        ] === "KEY_ID"
      ) {
        $scope.samlXmlKeyNameTranformer = $scope.xmlKeyNameTranformers[1];
      } else if (
        $scope.client.attributes[
        "saml.server.signature.keyinfo.xmlSigKeyInfoKeyNameTransformer"
        ] === "CERT_SUBJECT"
      ) {
        $scope.samlXmlKeyNameTranformer = $scope.xmlKeyNameTranformers[2];
      }
      if ($scope.client.attributes["saml.assertion.signature"]) {
        if ($scope.client.attributes["saml.assertion.signature"] == "true") {
          $scope.samlAssertionSignature = true;
        } else {
          $scope.samlAssertionSignature = false;
        }
      }
      if ($scope.client.attributes["saml.client.signature"]) {
        if ($scope.client.attributes["saml.client.signature"] == "true") {
          $scope.samlClientSignature = true;
        } else {
          $scope.samlClientSignature = false;
        }
      }
      if ($scope.client.attributes["saml.encrypt"]) {
        if ($scope.client.attributes["saml.encrypt"] == "true") {
          $scope.samlEncrypt = true;
        } else {
          $scope.samlEncrypt = false;
        }
      }
      if ($scope.client.attributes["saml.authnstatement"]) {
        if ($scope.client.attributes["saml.authnstatement"] == "true") {
          $scope.samlAuthnStatement = true;
        } else {
          $scope.samlAuthnStatement = false;
        }
      }
      if ($scope.client.attributes["saml.onetimeuse.condition"]) {
        if ($scope.client.attributes["saml.onetimeuse.condition"] == "true") {
          $scope.samlOneTimeUseCondition = true;
        } else {
          $scope.samlOneTimeUseCondition = false;
        }
      }
      if ($scope.client.attributes["saml_force_name_id_format"]) {
        if ($scope.client.attributes["saml_force_name_id_format"] == "true") {
          $scope.samlForceNameIdFormat = true;
        } else {
          $scope.samlForceNameIdFormat = false;
        }
      }
      if ($scope.client.attributes["saml.multivalued.roles"]) {
        if ($scope.client.attributes["saml.multivalued.roles"] == "true") {
          $scope.samlMultiValuedRoles = true;
        } else {
          $scope.samlMultiValuedRoles = false;
        }
      }
      if ($scope.client.attributes["saml.force.post.binding"]) {
        if ($scope.client.attributes["saml.force.post.binding"] == "true") {
          $scope.samlForcePostBinding = true;
        } else {
          $scope.samlForcePostBinding = false;
        }
      }

      if ($scope.client.attributes["isInBuilt"]) {
        if ($scope.client.attributes["isInBuilt"] == "true") {
          $scope.isInBuilt = true;
        } else {
          $scope.isInBuilt = false;
        }
      }

      if ($scope.client.attributes["sso.show"]) {
        if ($scope.client.attributes["sso.show"] == "true") {
          $scope.sso.show = true;
        } else {
          $scope.sso.show = false;
        }
      }

      if ($scope.client.attributes["ciba.consent"]) {
        if ($scope.client.attributes["ciba.consent"] == "true") {
          $scope.ciba.consent = true;
        } else {
          $scope.ciba.consent = false;
        }
      }

      $scope.accessTokenSignedResponseAlg =
        $scope.client.attributes["access.token.signed.response.alg"];
      $scope.idTokenSignedResponseAlg =
        $scope.client.attributes["id.token.signed.response.alg"];
      $scope.idTokenEncryptedResponseAlg =
        $scope.client.attributes["id.token.encrypted.response.alg"];
      $scope.idTokenEncryptedResponseEnc =
        $scope.client.attributes["id.token.encrypted.response.enc"];

      var attrVal1 =
        $scope.client.attributes["user.info.response.signature.alg"];
      $scope.userInfoSignedResponseAlg =
        attrVal1 == null ? "unsigned" : attrVal1;

      var attrVal2 = $scope.client.attributes["request.object.signature.alg"];
      $scope.requestObjectSignatureAlg = attrVal2 == null ? "any" : attrVal2;

      var attrVal3 = $scope.client.attributes["request.object.required"];
      $scope.requestObjectRequired =
        attrVal3 == null ? "not required" : attrVal3;

      var attrVal4 = $scope.client.attributes["pkce.code.challenge.method"];
      $scope.pkceCodeChallengeMethod = attrVal4 == null ? "" : attrVal4;

      if (
        $scope.client.attributes["exclude.session.state.from.auth.response"]
      ) {
        if (
          $scope.client.attributes[
          "exclude.session.state.from.auth.response"
          ] == "true"
        ) {
          $scope.excludeSessionStateFromAuthResponse = true;
        } else {
          $scope.excludeSessionStateFromAuthResponse = false;
        }
      }

      // KEYCLOAK-6771 Certificate Bound Token
      // https://tools.ietf.org/html/draft-ietf-oauth-mtls-08#section-3
      if (
        $scope.client.attributes["tls.client.certificate.bound.access.tokens"]
      ) {
        if (
          $scope.client.attributes[
          "tls.client.certificate.bound.access.tokens"
          ] == "true"
        ) {
          $scope.tlsClientCertificateBoundAccessTokens = true;
        } else {
          $scope.tlsClientCertificateBoundAccessTokens = false;
        }
      }

      if ($scope.client.attributes["display.on.consent.screen"]) {
        if ($scope.client.attributes["display.on.consent.screen"] == "true") {
          $scope.displayOnConsentScreen = true;
        } else {
          $scope.displayOnConsentScreen = false;
        }
      }
    }

    if (!$scope.create) {
      // Checking branding exist or not. If not default value will be assigned.
      client.attributes['login_theme'] = $scope.brandings.some(branding => branding.name === client.attributes['login_theme']) ? client.attributes['login_theme'] : 'default';
      $scope.client = client;
      updateProperties();

      $scope.clientEdit = angular.copy(client);
      $scope.oldDisplayOnConsentScreen = angular.copy(
        $scope.displayOnConsentScreen,
      );
      $scope.oldShowInSSO = $scope.sso.show;
      $scope.oldCibaConsent = $scope.ciba.consent;
    }

    $scope.samlIdpInitiatedUrl = function (ssoName) {
      return (
        encodeURI(
          $location.absUrl().replace(/\/admin.*/, "/realms/") +
          realm.realm +
          "/protocol/saml/clients/",
        ) + encodeURIComponent(ssoName)
      );
    };

    $scope.importFile = function (fileContent) {
      console.debug(fileContent);
      ClientDescriptionConverter.save(
        {
          realm: realm.realm,
        },
        fileContent,
        function (data) {
          $scope.client = data;
          updateProperties();
          $scope.importing = true;

          $scope.clientEdit = angular.copy(client);
        },
      );
    };

    $scope.viewImportDetails = function () {
      $modal.open({
        templateUrl: resourceUrl + "/partials/modal/view-object.html",
        controller: "ObjectModalCtrl",
        backdrop: "static",
        resolve: {
          object: function () {
            return $scope.client;
          },
        },
      });
    };

    $scope.switchChange = function () {
      $scope.changed = true;
    };

    $scope.changeAppType = function (type) {
      $scope.appType = $scope.appTypes[$scope.displayAppTypes.indexOf(type)];
      if ($scope.appType === $scope.appTypes[0]) {
        $scope.clientEdit.appType = $scope.appTypes[0];
      } else if ($scope.appType === $scope.appTypes[1]) {
        $scope.clientEdit.appType = $scope.appTypes[1];
      } else if ($scope.appType === $scope.appTypes[2]) {
        $scope.clientEdit.appType = $scope.appTypes[2];
      } else if ($scope.appType === $scope.appTypes[3]) {
        $scope.clientEdit.appType = $scope.appTypes[3];
      }
    };

    $scope.changeProtocol = function () {
      if ($scope.protocol == "openid-connect") {
        $scope.clientEdit.protocol = "openid-connect";
      } else if ($scope.protocol == "saml") {
        $scope.clientEdit.protocol = "saml";
      }
    };

    $scope.changeAlgorithm = function () {
      $scope.clientEdit.attributes["saml.signature.algorithm"] =
        $scope.signatureAlgorithm;
    };

    $scope.changeNameIdFormat = function () {
      $scope.clientEdit.attributes["saml_name_id_format"] = $scope.nameIdFormat;
    };

    $scope.changeSamlSigKeyNameTranformer = function () {
      $scope.clientEdit.attributes[
        "saml.server.signature.keyinfo.xmlSigKeyInfoKeyNameTransformer"
      ] = $scope.samlXmlKeyNameTranformer;
    };

    $scope.changeAccessTokenSignedResponseAlg = function () {
      $scope.clientEdit.attributes["access.token.signed.response.alg"] =
        $scope.accessTokenSignedResponseAlg;
    };

    $scope.changeIdTokenSignedResponseAlg = function () {
      $scope.clientEdit.attributes["id.token.signed.response.alg"] =
        $scope.idTokenSignedResponseAlg;
    };

    $scope.changeIdTokenEncryptedResponseAlg = function () {
      $scope.clientEdit.attributes["id.token.encrypted.response.alg"] =
        $scope.idTokenEncryptedResponseAlg;
    };

    $scope.changeIdTokenEncryptedResponseEnc = function () {
      $scope.clientEdit.attributes["id.token.encrypted.response.enc"] =
        $scope.idTokenEncryptedResponseEnc;
    };

    $scope.changeUserInfoSignedResponseAlg = function () {
      if ($scope.userInfoSignedResponseAlg === "unsigned") {
        $scope.clientEdit.attributes["user.info.response.signature.alg"] = null;
      } else {
        $scope.clientEdit.attributes["user.info.response.signature.alg"] =
          $scope.userInfoSignedResponseAlg;
      }
    };

    $scope.changeRequestObjectSignatureAlg = function () {
      if ($scope.requestObjectSignatureAlg === "any") {
        $scope.clientEdit.attributes["request.object.signature.alg"] = null;
      } else {
        $scope.clientEdit.attributes["request.object.signature.alg"] =
          $scope.requestObjectSignatureAlg;
      }
    };

    $scope.changeRequestObjectRequired = function () {
      if ($scope.requestObjectRequired === "not required") {
        $scope.clientEdit.attributes["request.object.required"] = null;
      } else {
        $scope.clientEdit.attributes["request.object.required"] =
          $scope.requestObjectRequired;
      }
    };

    $scope.changePkceCodeChallengeMethod = function () {
      if ($scope.pkceCodeChallengeMethod === "none") {
        $scope.clientEdit.attributes["pkce.code.challenge.method"] = "";
      } else {
        $scope.clientEdit.attributes["pkce.code.challenge.method"] =
          $scope.pkceCodeChallengeMethod;
      }
    };

    $scope.$watch(
      function () {
        return $location.path();
      },
      function () {
        $scope.path = $location.path().substring(1).split("/");
      },
    );

    function isChanged() {
      if (
        (JSON.stringify($scope.clientEdit) !== JSON.stringify(client) &&
          $scope.protocol != "saml" &&
          $scope.clientEdit.name) ||
        (JSON.stringify($scope.clientEdit) !== JSON.stringify(client) && $scope.protocol == "saml" && $scope.clientEdit?.attributes.aud && $scope.clientEdit.name)
      ) {
        if($scope.client.appType === 'ciba'){
          return $scope.ciba.consent ? $scope.selectedCibaRejectionReasons.length : true;
        }else{
          return true;
        }
      }
      if (
        !angular.equals(
          $scope.oldDisplayOnConsentScreen,
          $scope.displayOnConsentScreen,
        )
      ) {
        return true;
      }
      if (!angular.equals($scope.oldShowInSSO, $scope.sso.show)) {
        return true;
      }
      if (!angular.equals($scope.oldCibaConsent, $scope.ciba.consent)) {
        if(($scope.ciba.consent && $scope.selectedCibaRejectionReasons.length == 0) || !$scope.clientEdit.name){
          return false;
        }
        return true;
      }
      if ($scope.newRedirectUri && $scope.newRedirectUri.length > 0) {
        return true;
      }
      if ($scope.newWebOrigin && $scope.newWebOrigin.length > 0) {
        return true;
      }
      return false;
    }

    $scope.updateTimeouts = function () {
      if ($scope.accessTokenLifespan.time) {
        if ($scope.accessTokenLifespan.time === -1) {
          $scope.clientEdit.attributes["access.token.lifespan"] = -1;
        } else {
          $scope.clientEdit.attributes["access.token.lifespan"] =
            $scope.accessTokenLifespan.toSeconds();
        }
      } else {
        $scope.clientEdit.attributes["access.token.lifespan"] = null;
      }
    };

    $scope.updateAssertionLifespan = function () {
      if ($scope.samlAssertionLifespan.time) {
        $scope.clientEdit.attributes["saml.assertion.lifespan"] =
          $scope.samlAssertionLifespan.toSeconds();
      } else {
        $scope.clientEdit.attributes["saml.assertion.lifespan"] = null;
      }
    };

    function configureAuthorizationServices() {
      if ($scope.clientEdit.authorizationServicesEnabled) {
        if ($scope.accessType == "public") {
          $scope.accessType = "confidential";
        }
        $scope.clientEdit.publicClient = false;
        $scope.clientEdit.serviceAccountsEnabled = true;
      } else if ($scope.clientEdit.bearerOnly) {
        $scope.clientEdit.serviceAccountsEnabled = false;
      }
      if (
        $scope.client.authorizationServicesEnabled &&
        !$scope.clientEdit.authorizationServicesEnabled
      ) {
        Dialog.confirm(
          "Disable Authorization Settings",
          "Are you sure you want to disable authorization ? Once you save your changes, all authorization settings associated with this client will be removed. This operation can not be reverted.",
          function () { },
          function () {
            $scope.clientEdit.authorizationServicesEnabled = true;
          },
        );
      }
    }

    $scope.$watch(
      "clientEdit",
      function () {
        $scope.changed = isChanged();
        if (
          ($scope.clientEdit.appType === "high_secure" ||
            $scope.clientEdit.appType === "browser_based") &&
          $scope.clientEdit.redirectUris &&
          $scope.clientEdit.redirectUris.length < 1
        ) {
          $scope.changed = false;
        }
        configureAuthorizationServices();
      },
      true,
    );

    $scope.$watch(
      "newWebOrigin",
      function () {
        $scope.changed = isChanged();
      },
      true,
    );

    $scope.$watch(
      "ciba",
      function () {
        $scope.changed = isChanged();
      },
      true,
    );

    $scope.$watch(
      "displayOnConsentScreen",
      function () {
        $scope.changed = isChanged();
      },
      true,
    );

    $scope.$watch(
      "sso",
      function () {
        $scope.changed = isChanged();
      },
      true,
    );

    $scope.deleteWebOrigin = function (index) {
      $scope.clientEdit.webOrigins.splice(index, 1);
    };
    $scope.addWebOrigin = function () {
      $scope.clientEdit.webOrigins.push($scope.newWebOrigin);
      $scope.newWebOrigin = "";
    };
    $scope.deleteRedirectUri = function (index) {
      $scope.clientEdit.redirectUris.splice(index, 1);
    };

    $scope.addRedirectUri = function () {
      $scope.clientEdit.redirectUris.push($scope.newRedirectUri);
      $scope.newRedirectUri = "";
    };

    $scope.addDefaultDebuggerUrl = function () {
      if ($scope.clientEdit.redirectUris) {
        let isDebuggUrlPresent = $scope.clientEdit.redirectUris.find(
          (debuggURL) => debuggURL === $scope.debuggerUrl,
        );
        if (!isDebuggUrlPresent) {
          $scope.clientEdit.redirectUris.push($scope.debuggerUrl);
        }
      }
    };

    $scope.openRedirect = false;

    $scope.reset = function () {
      $scope.openRedirect = true;
      $route.reload();
    };

    $scope.save = function () {
      $scope.clientEdit.name = $scope.clientEdit.name.trim();
      const name = $scope.clientEdit.name;
      if (name === "-" || !$scope.isWorkflow(client) && !/^[a-zA-Z\- ]+$/.test(name)) {
          $scope.stage = 1;
          Notifications.error($translate.instant("invalid-client-name"));
          return;
      } else if ($scope.isWorkflow(client) && !/^[a-zA-Z0-9\- ]+$/.test(name)) {
          $scope.stage = 1;
          Notifications.error($translate.instant("invalid-client-name-alpha"));
          return;
      }

      $scope.stage = 2;

      if ($scope.newRedirectUri && $scope.newRedirectUri.length > 0) {
        $scope.addRedirectUri();
      }

      if ($scope.newWebOrigin && $scope.newWebOrigin.length > 0) {
        $scope.addWebOrigin();
      }

      if ($scope.samlServerSignature == true) {
        $scope.clientEdit.attributes["saml.server.signature"] = "true";
      } else {
        $scope.clientEdit.attributes["saml.server.signature"] = "false";
      }
      if ($scope.samlServerSignatureEnableKeyInfoExtension == true) {
        $scope.clientEdit.attributes["saml.server.signature.keyinfo.ext"] =
          "true";
      } else {
        $scope.clientEdit.attributes["saml.server.signature.keyinfo.ext"] =
          "false";
      }
      if ($scope.samlAssertionSignature == true) {
        $scope.clientEdit.attributes["saml.assertion.signature"] = "true";
      } else {
        $scope.clientEdit.attributes["saml.assertion.signature"] = "false";
      }
      if ($scope.samlClientSignature == true) {
        $scope.clientEdit.attributes["saml.client.signature"] = "true";
      } else {
        $scope.clientEdit.attributes["saml.client.signature"] = "false";
      }
      if ($scope.samlEncrypt == true) {
        $scope.clientEdit.attributes["saml.encrypt"] = "true";
      } else {
        $scope.clientEdit.attributes["saml.encrypt"] = "false";
      }
      if ($scope.samlAuthnStatement == true) {
        $scope.clientEdit.attributes["saml.authnstatement"] = "true";
      } else {
        $scope.clientEdit.attributes["saml.authnstatement"] = "false";
      }
      if ($scope.samlOneTimeUseCondition == true) {
        $scope.clientEdit.attributes["saml.onetimeuse.condition"] = "true";
      } else {
        $scope.clientEdit.attributes["saml.onetimeuse.condition"] = "false";
      }
      if ($scope.samlForceNameIdFormat == true) {
        $scope.clientEdit.attributes["saml_force_name_id_format"] = "true";
      } else {
        $scope.clientEdit.attributes["saml_force_name_id_format"] = "false";
      }
      if ($scope.samlMultiValuedRoles == true) {
        $scope.clientEdit.attributes["saml.multivalued.roles"] = "true";
      } else {
        $scope.clientEdit.attributes["saml.multivalued.roles"] = "false";
      }
      if ($scope.samlForcePostBinding == true) {
        $scope.clientEdit.attributes["saml.force.post.binding"] = "true";
      } else {
        $scope.clientEdit.attributes["saml.force.post.binding"] = "false";
      }

      if ($scope.excludeSessionStateFromAuthResponse == true) {
        $scope.clientEdit.attributes[
          "exclude.session.state.from.auth.response"
        ] = "true";
      } else {
        $scope.clientEdit.attributes[
          "exclude.session.state.from.auth.response"
        ] = "false";
      }

      // KEYCLOAK-6771 Certificate Bound Token
      // https://tools.ietf.org/html/draft-ietf-oauth-mtls-08#section-3
      if ($scope.tlsClientCertificateBoundAccessTokens == true) {
        $scope.clientEdit.attributes[
          "tls.client.certificate.bound.access.tokens"
        ] = "true";
      } else {
        $scope.clientEdit.attributes[
          "tls.client.certificate.bound.access.tokens"
        ] = "false";
      }

      if ($scope.displayOnConsentScreen == true) {
        $scope.clientEdit.attributes["display.on.consent.screen"] = "true";
      } else {
        $scope.clientEdit.attributes["display.on.consent.screen"] = "false";
      }

      if ($scope.sso.show == true) {
        $scope.clientEdit.attributes["sso.show"] = "true";
      } else {
        $scope.clientEdit.attributes["sso.show"] = "false";
      }

      if ($scope.ciba.consent == true) {
        $scope.clientEdit.attributes["consent_rejected_reason"] = $scope.selectedCibaRejectionReasons.join("##");
        $scope.clientEdit.attributes["ciba.consent"] = "true";
      } else {
        $scope.clientEdit.attributes["ciba.consent"] = "false";
        $scope.clientEdit.attributes["consent_rejected_reason"] = "";
      }

      $scope.clientEdit.protocol = $scope.protocol;
      $scope.clientEdit.attributes["saml.signature.algorithm"] =
        $scope.signatureAlgorithm;
      $scope.clientEdit.attributes["saml_name_id_format"] = $scope.nameIdFormat;
      if (
        $scope.clientEdit.appType === "high_secure" ||
        $scope.clientEdit.appType === "saml"
      ) {
        $scope.clientEdit.standardFlowEnabled = true;
        $scope.clientEdit.implicitFlowEnabled = false;
      } else if ($scope.clientEdit.appType === "browser_based") {
        $scope.clientEdit.standardFlowEnabled = false;
        $scope.clientEdit.implicitFlowEnabled = true;
      } else {
        $scope.clientEdit.standardFlowEnabled = false;
        $scope.clientEdit.implicitFlowEnabled = false;
      }
      if (
        $scope.clientEdit.protocol != "saml" &&
        !$scope.clientEdit.bearerOnly &&
        ($scope.clientEdit.standardFlowEnabled ||
          $scope.clientEdit.implicitFlowEnabled) &&
        ($scope.clientEdit.appType === "high_secure" ||
          $scope.clientEdit.appType === "browser_based") &&
        (!$scope.clientEdit.redirectUris ||
          $scope.clientEdit.redirectUris.length == 0 ||
          !$scope.checkRedirectUriNull())
      ) {
        $scope.stage = 1;
        Notifications.error("You must specify at least one redirect uri");
      } else if (
        $scope.clientEdit.protocol === "saml" &&
        !$scope.isInBuilt &&
        !$scope.clientEdit.attributes.aud
      ) {
        $scope.stage = 1;
        Notifications.error(
          "You must provide a valid Service Provider Entity ID",
        );
      } else {
        if (
          $scope.clientEdit.appType === "rest_api" ||
          $scope.clientEdit.appType === "machine_to_machine"
        ) {
          $scope.clientEdit.redirectUris = [];
          $scope.clientEdit.webOrigins = [];
        }
        Client.update(
          {
            realm: realm.realm,
            client: client.id,
          },
          $scope.clientEdit,
          function () {
            $scope.reset();
            $scope.stage = 1;
            Notifications.success(
              $translate.instant("changes-saved-to-application"),
            );
          },
          function () {
            $scope.stage = 1;
          },
        );
      }
    };
    $scope.checkRedirectUriNull = function () {
      for (const uri of $scope.clientEdit.redirectUris) {
        if (!uri) {
          return false;
        }
      }
      return true;
    };
    $scope.tooltipTxt = $translate.instant("copy");
    $(".copy-paste-icon").click(function () {
      $scope.tooltipTxt = $translate.instant("copied");
      $(".copy-paste-icon").mouseleave(function () {
        $scope.tooltipTxt = $translate.instant("copy");
      });
    });
    $scope.copyToClipboard = function (element) {
      /* Creates the empty input element and set the value of coming input field */
      var $temp = $("<input>");
      $("body").append($temp);
      /* Select the Value in Input field */
      $temp.val($(element).val()).select();
      /* Copy the text inside the text field */
      document.execCommand("copy");
      $temp.remove();
    };

    $scope.cancel = function () {
      $location.url("/realms/" + realm.realm + "/clients");
    };

    $scope.$on("$routeChangeStart", function (event, newUrl) {
      if (!$scope.openRedirect && isChanged()) {
        showConfirmExitDialog(
          event,
          ConfirmExitDialog,
          $scope,
          $location,
          newUrl,
        );
      }
    });
  },
);
module.controller(
  "CreateClientCtrl",
  function (
    $scope,
    realm,
    $translate,
    client,
    $route,
    serverInfo,
    Client,
    ClientDescriptionConverter,
    $location,
    $modal,
    Dialog,
    Notifications,
    $routeParams,
  ) {
    //Prevent enter action
    document.addEventListener("keydown", (e) => {
      if (e.key === "Enter" || e.which == "13") {
        e.preventDefault();
      }
    });

    $scope.serverProtocols = serverInfo.listProviderIds("login-protocol");
    $scope.protocols = $scope.serverProtocols;
    $scope.create = true;
    $scope.stage = 1;
    $scope.cibaProtocol = ["openid-connect"];
    $scope.entityId = "";

    $scope.realm = realm;
    $scope.client = {
      enabled: true,
      attributes: {}
    };
    $scope.client.redirectUris = [];
    $scope.protocol = $scope.protocols[0];
    $scope.client.protocol = $scope.protocols[0];
    $scope.init = function () {
      $scope.client.appType = $routeParams.type;
      $scope.onClientClick($scope.client.appType);
      $scope.nextStep($scope.client.appType);
    };

    $scope.clientTypes = [];
    if (serverInfo.cibaFeatureEnabled) {
      $scope.clientTypes.push("ciba");
    }
    if (serverInfo.iwsenabled) {
      $scope.clientTypes.push("iws");
    }
    if (serverInfo.oidcenabled) {
      $scope.clientTypes.push("high-secure");
      $scope.clientTypes.push("browser-based");
      $scope.clientTypes.push("rest-api");
      $scope.clientTypes.push("machine-to-machine");
    }
    $scope.client.appType = $scope.clientTypes[0];
    $scope.debuggerUrl = serverInfo.debuggerUrl;
    $scope.onClientClick = function (clientType) {
      $scope.client.appType = clientType;
      $scope.protocols = $scope.serverProtocols;
      $scope.protocol = $scope.serverProtocols[0];
      if ($scope.client.appType === "iws") {
        $scope.client.attributes = {};
        $scope.client.attributes["iws.credtype"] = "iws_user_set_pass";
        $scope.client.redirectUris = [""];
      } else {
        if ($scope.client?.attributes) {
          delete $scope.client.attributes;
        }
        $scope.client.redirectUris = [];
      }
    };

    $scope.onProtocolClick = function (protocolType) {
      $scope.protocol = protocolType;
      $scope.client.protocol = protocolType;
      $scope.changed = isChanged();
    };

    $scope.addDefaultDebuggerUrl = function () {
      $scope.client.redirectUris[0] = $scope.debuggerUrl;
    };

    $scope.importFile = function (fileContent) {
      console.debug(fileContent);
      ClientDescriptionConverter.save(
        {
          realm: realm.realm,
        },
        fileContent,
        function (data) {
          $scope.client = data;
          if (data.protocol) {
            $scope.protocol = data.protocol;
          }
          $scope.importing = true;
        },
      );
    };

    $scope.viewImportDetails = function () {
      $modal.open({
        templateUrl: resourceUrl + "/partials/modal/view-object.html",
        controller: "ObjectModalCtrl",
        backdrop: "static",
        resolve: {
          object: function () {
            return $scope.client;
          },
        },
      });
    };

    $scope.switchChange = function () {
      $scope.changed = true;
    };

    $scope.changeProtocol = function () {
      if ($scope.protocol == "openid-connect") {
        $scope.client.protocol = "openid-connect";
      } else if ($scope.protocol == "saml") {
        $scope.client.protocol = "saml";
      }
    };

    $scope.$watch(
      function () {
        return $location.path();
      },
      function () {
        $scope.path = $location.path().substring(1).split("/");
      },
    );

    function isChanged() {
      let changed = false;
      const isNameExist = $scope.client.name ? true : false;
      switch ($scope.client.appType) {
        case "high-secure":
          changed = isNameExist && $scope.client.redirectUris[0];
          break;
        case "browser-based":
          changed =
            $scope.protocol === "openid-connect" &&
            isNameExist &&
            $scope.client.redirectUris[0];
          break;
        case "saml":
          changed = isNameExist && $scope.entityId;
          break;
        case "iws":
          changed =
            isNameExist &&
            $scope.client.redirectUris[0] &&
            ($scope.client.attributes["iws.credtype"] === "iws_user_set_pass" ||
              ($scope.client.attributes["iws.credtype"] ===
                "iws_admin_set_pass" &&
                $scope.client.attributes["iws.admin.user"] &&
                $scope.client.attributes["iws.admin.pass"])) &&
            $scope.client.attributes["sso.logourl"] &&
            $scope.client.attributes["iws.script"];
          break;
        case "rest-api":
        case "machine-to-machine":
          changed = isNameExist;
          $scope.client.redirectUris = [];
          break;
        default:
          changed = false;
          break;
      }

      return changed;
    }

    $scope.$watch(
      "entityId",
      function () {
        $scope.changed = isChanged();
      },
      true,
    );

    $scope.$watch(
      "client",
      function () {
        $scope.changed = isChanged();
      },
      true,
    );

    function isValidRedirectUri(uri) {
      return uri.startsWith("http") || uri === "*";
    }

    $scope.save = function () {
      $scope.stage = 2;
      $scope.client.name = $scope.client.name.trim();
      const name = $scope.client.name;
      if (name === "-" || !/^[a-zA-Z\- ]+$/.test(name)) {
        $scope.stage = 1;
        Notifications.error($translate.instant("invalid-client-name"));
        return;
      }

      // SAML 2.0 Application
      if ($scope.client.appType === "saml") {
        $scope.client["attributes"] = {};
        if ($scope.client.adminUrl) {
          $scope.client.attributes["saml_single_logout_service_url_post"] =
            $scope.client.adminUrl;
          $scope.client.attributes["saml_assertion_consumer_url_post"] =
            $scope.client.adminUrl;
        }
        $scope.client.attributes["saml.client.signature"] = false;
        $scope.client.attributes["sso_type"] = "custom-saml";
        $scope.client.protocolMappers = [];
        $scope.protocol = "saml";
        $scope.client.publicClient = true;
        $scope.client.clientId = $scope.client.name;
        $scope.client.id = $scope.client.name;
        $scope.client.attributes["aud"] = $scope.entityId;
        $scope.client.standardFlowEnabled = true;
        $scope.client.redirectUris.push($scope.entityId);
      }

      $("#cancelBtn").attr("disabled", "disabled");
      $scope.client.protocol = $scope.protocol;

      if (
        ($scope.client.appType === "high-secure" ||
          $scope.client.appType === "browser-based") &&
        $scope.client.redirectUris[0] &&
        !isValidRedirectUri($scope.client.redirectUris[0])
      ) {
        Notifications.error($translate.instant("enter-valid-redirect-uri-error"));
        $scope.stage = 1;
      } else {
        // //replace '-' in appType with '_' - backend apptype standard
        // $scope.client.appType = $scope.client.appType.replaceAll('-', '_')
        Client.save(
          {
            realm: realm.realm,
            client: "",
          },
          {
            ...$scope.client,
            appType: $scope.client.appType.replaceAll("-", "_"),
          },
          function (data, headers) {
            $scope.stage = 1;
            $("#cancelBtn").removeAttr("disabled", "disabled");
            $scope.changed = false;
            var l = headers().location;
            var id = l.substring(l.lastIndexOf("/") + 1);
            $location.url("/realms/" + realm.realm + "/clients/" + id);
            Notifications.success($translate.instant("application-created"));
          },
          function (err) {
            $scope.stage = 1;
            $("#cancelBtn").removeAttr("disabled", "disabled");
            Notifications.error($translate.instant(err.data.errorMessage));
          },
        );
      }
    };

    $scope.reset = function () {
      $route.reload();
    };

    $scope.cancel = function () {
      $location.url(
        `/realms/${realm.realm}/clients/type/${$scope.client.appType}`,
      );
    };
  },
);

// create ciba client controller.
module.controller(
  "CreateCibaClientCtrl",
  function (
    $scope,
    $translate,
    $http,
    angularSlideOutPanel,
    realm,
    client,
    Client,
    actions,
    cibaPolicies,
    serverInfo,
    Notifications,
    brandings,
    actionPolicies,
    CibaProviderDetails,
    CibaProviderConfigDetails,
    CibaNotProviderDetails,
    CibaNotProviderConfigDetails,
    CibaResultHookProviderDetails,
    CibaResultHookProviderConfigDetails,
    ComponentUtils,
    CibaTutorial,
    TestEventsConfig,
    CibaTemplateCreateDialog,
    CibaRuleCreateDialog,
    WorkflowPreviewDialog,
    WorkflowPreviewGif
  ) {
    $scope.clientEdit = angular.copy(client);
    $scope.clientEdit['attributes'] = {};
    $scope.clientEdit['authenticationFlowBindingOverrides'] = {};
    $scope.actionPolicies = actionPolicies;
    $scope.cibaPolicies = cibaPolicies;
    $scope.actionOnboardId = "";
    $scope.actionVerificationId = "";
    $scope.realm = realm;
    $scope.brandings = brandings;
    $scope.cibaAuthProviders = serverInfo.cibaAuthProviders;
    $scope.cibaNotificationProviders = serverInfo.cibaNotificationProviders;
    $scope.cibaResultProviders = serverInfo.cibaDeadLetterHookNotificationProviders;
    $scope.actions = actions;
    $scope.clientId = "";
    $scope.changed = false;
    $scope.loading = false;
    $scope.ClickableConfig = true;
    $scope.tooltipTxt = $translate.instant("copy");
    $scope.appType = "ciba";
    $scope.testResponse = {};
    $scope.resultResponse = {};
    $scope.loadingTest = false;
    $scope.loadingResult = false;
    $scope.tutorial = {};
    $scope.selectedFlow = 'onboarding';
    $scope.providerId = "";
    $scope.deliveryMode = "";
    $scope.showNotificationWay = true

    const onboardTemplate = { index: 4, name: $translate.instant("ciba-onboard-flow-title"), description: $translate.instant("ciba-onboard-flow-description"), completed: false, step: 4 };
    const verificationTemplate = { index: 5, name: $translate.instant("ciba-verify-flow-title"), description: $translate.instant("ciba-verify-flow-description"), completed: false, step: 5 };
    $scope.pages = [
      { index: 1, name: $translate.instant("ciba-details-title"), description: $translate.instant("ciba-details-description"), completed: false, step: 1 },
      { index: 2, name: $translate.instant("ciba-gatepass-title"), description: $translate.instant("ciba-gatepass-description"), completed: false, step: 2 },
      { index: 3, name: $translate.instant("ciba-configuration-title"), description: $translate.instant("ciba-configuration-description"), completed: false, step: 3 },
      { ...onboardTemplate },
      { ...verificationTemplate },
      { index: 6, name: $translate.instant("ciba-tutorial-title"), description: $translate.instant("ciba-tutorial-description"), completed: false, step: 6 },
    ];
    $scope.currentPage = $scope.pages[0];

    $scope.setProgress = function (percent) {
      const circumference = 2 * Math.PI * 40;
      const dashOffset = circumference - (percent / 100) * circumference;
      const progressBar = document.querySelector('.progress-bar');
      progressBar.style.strokeDasharray = `${circumference}, ${circumference}`;
      progressBar.style.strokeDashoffset = dashOffset;

      const progressLabel = document.querySelector('.progress-label');
      progressLabel.textContent = `${$scope.currentPage.step} ${$translate.instant('of')} ${$scope.pages.length}`;
    }

    $scope.defaultCibaRejectionReasons = [
          "Incorrect Information",
          "Suspicious Activity",
          "Transaction not triggered by me",
          "Inappropriate Transaction"
    ]

    $scope.selectedCibaRejectionReasons = angular.copy($scope.defaultCibaRejectionReasons);
    $scope.newCibaConsentRejectReason = '';
    $scope.isConsentHelperVisible = false;

    $scope.addReasonForConsent = function () {
      if($scope.newCibaConsentRejectReason.length){
        $scope.selectedCibaRejectionReasons.push($scope.newCibaConsentRejectReason);
        $scope.newCibaConsentRejectReason = "";
      }
    }

    $scope.removeReasonForConsent = function (index) {
      $scope.selectedCibaRejectionReasons.splice(index, 1);
    }

    $scope.handleKeyPress = (event, name) => {
        if(!totalCharCount(String.fromCharCode(event.which), name, event.target.value.length)) {
            event.preventDefault();
        }
    }

    const totalCharCount = (value, name, valueLen)=> {
            let isValid = true;
            let pattern = "";
            let length = 35;
            if ((pattern && !pattern.test(value)) || length < valueLen) {
                isValid = false;
            }
            return isValid;
        }

    $scope.$watch(
      "selectedCibaRejectionReasons",
      function () {
        if ($scope.currentPage.index === 1) {
          validateDetails();
        }
        const isSuspiciousActivity = $scope.selectedCibaRejectionReasons.includes("Suspicious Activity");
        const isTransactionNotTriggered = $scope.selectedCibaRejectionReasons.includes("Transaction not triggered by me");
        if(isSuspiciousActivity || isTransactionNotTriggered){
          $scope.isConsentHelperVisible = true;
        }else{
          $scope.isConsentHelperVisible = false;
        }
      },
      true,
    );

    const addTemplateStep = function (state) {
      const pagesCopy = angular.copy($scope.pages);
      if (state && pagesCopy[3].index === 4) {
        pagesCopy[5].step = 4;
        pagesCopy.splice(3, 2);
      } else if (!state && pagesCopy[3].index !== 4) {
        pagesCopy.splice(3, 0, onboardTemplate, verificationTemplate);
        pagesCopy[5].step = 6;
      }
      $scope.pages = pagesCopy;
      $scope.setProgress(($scope.currentPage.step*100)/$scope.pages.length);
    };

    document.addEventListener("keydown", (e) => {
      if (e.key === "Enter" || e.which == "13") {
        e.preventDefault();
      }
    });

    $scope.$watch(function () {
      var divElement = document.querySelector("#qv-input__mobile-apps");
      if (divElement) {
        var paragraphElement = divElement.querySelector("a");
        paragraphElement.addEventListener("click", () =>
          window.location.reload(),
        );
      }
      if ($scope.currentPage.index === 3) {
        validatePolicy();
      }
    });

    $scope.$watch("currentPage", function () {
      $scope.setProgress(($scope.currentPage.step*100)/$scope.pages.length);
      switch ($scope.currentPage.index) {
        case 1:
          validateDetails();
          break;
        case 2:
          $scope.changed = true
          break;
        case 3:
          validatePolicy();
          break;
        case 4:
          validateOnboardTemplates();
          break;
        case 5:
          validateVerificationTemplates();
          break;
        case 6:
          $scope.changed = true;
          fetchTutorials();
          hljs.highlightAll();
          break;
      }
    });

    $scope.$watch(
      "client",
      function () {
        if ($scope.currentPage.index === 1) {
          validateDetails();
        }
      },
      true,
    );

    $scope.$watch(
      "clientEdit",
      function () {
        if ($scope.currentPage.index === 4 ) {
          validateOnboardTemplates();
        } else if ( $scope.currentPage.index === 5) {
          validateVerificationTemplates();
        }
      },
      true,
    );

    $scope.setCurrentPage = function (page, isBack) {
      if (isBack) {
        $scope.currentPage = page;
        return;
      }
      $scope.save(page);
    };

    $scope.changeCurrentPage = function(page){
      $scope.currentPage = page;
    }

    $scope.save = function (page) {
      switch ($scope.currentPage.index) {
        case 1:
          if ($scope.pages[0].completed) updateClient(page, $scope.client);
          else createClient(page);
          break;
        case 2:
          updateClient(page, $scope.clientEdit);
          break;
        case 4:
          if ($scope.showCreateOnboardingTemplate) createTemplate('onboard');
          else if ($scope.actionOnboardId) {
            updateClient(page, {
              authenticationFlowBindingOverrides: {
                "onboarding-action": $scope.actionOnboardId,
              },
            });
            $scope.action.alias = '';
          } else updateClient(page, $scope.clientEdit);
          break;
        case 5:
          if ($scope.showCreateVerificaionTemplate) createTemplate('verification');
          else if ($scope.actionVerificationId) {
            updateClient(page, {
              authenticationFlowBindingOverrides: {
                "verification-action": $scope.actionVerificationId,
              },
            });
            $scope.action.alias = '';
          } else updateClient(page, $scope.clientEdit);
          break;
        default:
          $scope.currentPage = page;
          break;
      }
    };

    $scope.displayNextButton = function () {
      if ($scope.currentPage.index == 1 || $scope.currentPage.index == 3 || $scope.currentPage.index == 5) {
        return true;
      } else if ($scope.currentPage.index == 2) {
        return ($scope.clientEdit && $scope.clientEdit.attributes['gatepass_rule'] ) ? $scope.clientEdit.attributes['gatepass_rule'].length > 0 : false;
      } else if ($scope.currentPage.index == 4) {
        return ($scope.clientEdit && $scope.clientEdit.authenticationFlowBindingOverrides && $scope.clientEdit.authenticationFlowBindingOverrides['onboarding-action']) ? $scope.clientEdit.authenticationFlowBindingOverrides['onboarding-action'].length > 0 : false;
      } else if ($scope.currentPage.index == 6) {
        return false;
      } else {
        return false;
      }
    }

    $scope.isTemplatePreviewVisible = false;

    $scope.showTemplatePreview = function (selectedTemplateId, actionFlow) {
      $scope.isTemplatePreviewVisible = true;
      WorkflowPreviewGif.query({ realm: realm.realm, clientId: $scope.clientId, action: selectedTemplateId, actionFlow: actionFlow}, function (response){
        var selectedTemplate =  $scope.actions.filter((t) => t.actionId === selectedTemplateId)
        WorkflowPreviewDialog.open(response, selectedTemplate[0], false)
        $scope.isTemplatePreviewVisible = false;
      }, function () {
        $scope.isTemplatePreviewVisible = false;
      })
    }

    $scope.navigateToLink = function (templateId) {
      var link = `/realms/${$scope.realm.realm}/authentication/templates/${templateId}`
      $location.path(link);
    }

    $scope.displaySkipButton = function () {
      if ($scope.currentPage.index == 2) {
        return !(($scope.clientEdit && $scope.clientEdit.attributes['gatepass_rule'] ) ? $scope.clientEdit.attributes['gatepass_rule'].length > 0 : false);
      } else if ( $scope.currentPage.index == 4) {
        return !(($scope.clientEdit && $scope.clientEdit.authenticationFlowBindingOverrides && $scope.clientEdit.authenticationFlowBindingOverrides['onboarding-action']) ? $scope.clientEdit.authenticationFlowBindingOverrides['onboarding-action'].length > 0 : false);
      } else {
        return false;
      }
    }

    $scope.createTemplate = function () {
      CibaTemplateCreateDialog.open( $scope.realm, $scope.actionPolicies, $scope.cibaPolicies, function(templates, createdTemplate){
        if($scope.currentPage.index === 4){
          $scope.clientEdit.authenticationFlowBindingOverrides['onboarding-action'] = createdTemplate.id;
        }else if($scope.currentPage.index === 5){
          $scope.clientEdit.authenticationFlowBindingOverrides['verification-action'] = createdTemplate.id;
        }
        $scope.actions = templates;
      })
    };

    $scope.createRule = function () {
      CibaRuleCreateDialog.open( $scope.realm, serverInfo ,function(rules, createdRule){
        $scope.cibaPolicies = rules;
        $scope.clientEdit.attributes['gatepass_rule'] = createdRule.id;
      })
    };

    const validateDetails = function () {
      $scope.changed =
        $scope.client &&
        $scope.client.name &&
        ( $scope.client.attributes['ciba.consent'] ? $scope.selectedCibaRejectionReasons.length : true  ) &&
        $scope.client.attributes["login_theme"];
    };

    const validatePolicy = function () {
      $scope.notifyWayId = client.notifyWayId;
      $scope.resultHookId = client.resultHookId;
      if (client.authWayId) {
        $scope.authWayId = client.authWayId;
      }
      if ($scope.authWayId && $scope.notifyWayId || ($scope.authWayId && $scope.authWayId === 'api-authorizer')) {
        $scope.pages[2].completed = true;
        $scope.changed = true;
      } else $scope.changed = false;
    };

    const validateOnboardTemplates = function () {
      $scope.changed = ($scope.clientEdit && $scope.clientEdit.authenticationFlowBindingOverrides && $scope.clientEdit.authenticationFlowBindingOverrides["onboarding-action"]);
    };

    const validateVerificationTemplates = function () {
      $scope.changed =  ($scope.clientEdit && $scope.clientEdit.authenticationFlowBindingOverrides && $scope.clientEdit.authenticationFlowBindingOverrides["verification-action"]);
    };

    const fetchTutorials = function () {
      CibaTutorial.get(
        { realm: realm.realm, client: client.id },
        function (data) {
          $scope.tutorial = data;
        },
        function (error) {
          Notifications.error("Failed to load CIBA tutorial.");
        },
      );
    };

    const updateClient = function (page, updatedClient) {
      $scope.loading = true;
      Client.get(
        { realm: realm.realm, client: client.id },
        function (response) {
          $scope.providerId = response.authenticationFlowBindingOverrides['ciba_auth'];
          $scope.deliveryMode = response.attributes['ciba.backchannel.token.delivery.mode'];
          $scope.clientId = response.id;
          Client.update(
            {
              realm: realm.realm,
              client: client.id,
            },
            { ...JSON.parse(angular.toJson(response)), ...updatedClient },
            function () {
              $scope.loading = false;
              $scope.pages[page.index - 2].completed = true;
              $scope.currentPage = page;
            },
            function () {
              $scope.loading = false;
            },
          );
        },
      );
    };

    const createClient = function (page) {
      $scope.loading = true;
      const configs = {
        enabled: true,
        redirectUris: [],
        protocol: "openid-connect",
        appType: $scope.appType,
        sandboxMode: realm?.sandboxMode || false,
      };
      $scope.client.attributes['consent_rejected_reason'] = $scope.selectedCibaRejectionReasons.join("##");
      Client.save(
        {
          realm: realm.realm,
          client: "",
        },
        { ...$scope.client, ...configs },
        function (data, headers) {
          $scope.changed = false;
          var l = headers().location;
          client.id = l.substring(l.lastIndexOf("/") + 1);
          $scope.clientId = client.id;
          $scope.loading = false;
          $scope.pages[0].completed = true;
          $scope.currentPage = page;
          $http
            .get(
              authUrl +
              "/admin/realms/" +
              realm.realm +
              "/clients/" +
              $scope.clientId,
            )
            .then(function (res) {
              $scope.authWayId =
                res.data.authenticationFlowBindingOverrides.ciba_auth;
              CibaProviderDetails.get(
                {
                  realm: realm.realm,
                  client: client.id,
                  providerId: $scope.authWayId,
                },
                function (data) {
                  let configProps = data.properties;
                  var defaultConfig = {};
                  if (configProps && Array.isArray(configProps)) {
                    for (var i = 0; i < configProps.length; i++) {
                      var property = configProps[i];
                      if (property && property.name) {
                        defaultConfig[property.name] = property.defaultValue;
                      }
                    }
                  }
                  var config = { config: defaultConfig };
                  CibaProviderConfigDetails.update(
                    {
                      realm: realm.realm,
                      providerId: $scope.authWayId,
                      client: client.id,
                    },
                    config,
                    function () {
                      console.log("authentication is set as Web by default");
                    },
                    function (err) {
                      Notifications.error(err.data.errorMessage);
                    },
                  );
                },
                function (data) {
                  console.log(
                    "Error occurred while getting properties",
                    data.error,
                  );
                },
              );
            });
        },
        function (err) {
          $scope.loading = false;
          Notifications.error($translate.instant(err.data.errorMessage));
        },
      );
    };

    const makeConfigVisible = function () {
      $scope.ClickableConfig = true;
    };

    $scope.copyToClipboard = function (command) {
      /* Creates the empty input element and set the value of coming input field */
      var $temp = $("<input>");
      $("body").append($temp);
      /* Select the Value in Input field */
      const text = command;
      $temp.val(text.trim()).select();
      /* Copy the text inside the text field */
      document.execCommand("copy");
      $temp.remove();
      $scope.tooltipTxt = $translate.instant("copied");
      //  on MouseLeave the $scope.tooltipTxt will be changed to back.
      $(".copy-paste-icon").mouseleave(function () {
        $scope.tooltipTxt = $translate.instant("copy");
      });
    };

    $scope.loadAuthorizerConfig = function (provider) {
      if (provider && provider.configured === false) {
        Notifications.warn($translate.instant("ciba-authorizer-not-configured-tooltip"));
        return;
      }
      if ($scope.ClickableConfig) {
        $scope.ClickableConfig = false;
        CibaProviderDetails.get(
          { realm: realm.realm, client: client.id, providerId: provider.id },
          function (data) {
            let configProps = data.properties;
            for (var i = 0; i < configProps.length; i++) {
              var property = configProps[i];
              if (property && property.name == "sms-template") {
                property.defaultValue =
                  $translate.instant("sms-default-txt-welcome") +
                  " " +
                  "{{transaction_from}}" +
                  " " +
                  $translate.instant("sms-default-txt-approve") +
                  " " +
                  "{{link}}" +
                  ". " +
                  $translate.instant("sms-default-txt-expiration") +
                  " " +
                  "{{expirationTime}}" +
                  " " +
                  "{{expirationUnit}}";
              }
            }
            CibaProviderConfigDetails.get(
              {
                realm: realm.realm,
                client: client.id,
                providerId: provider.id,
              },
              function (data) {
                let settingConfigs = data.config;
                if (configProps && configProps.length > 0) {
                    $scope.showNotificationWay = true;
                    $scope.openAuthorizerConfigPanel(
                      provider,
                      configProps,
                      settingConfigs,
                    );
                } else {
                  console.log("No Config while creating client, hence saving");

                  CibaProviderConfigDetails.update(
                      {
                        realm: realm.realm,
                        providerId: provider.id,
                        client: client.id,
                      },
                      {},
                      function () {
                        $scope.authWayId = provider.id;
                        addTemplateStep(provider.id === "twilio-ivr-ciba-authorizer" || provider.id === 'api-authorizer');
                        $scope.showNotificationWay = false;
                        $scope.ClickableConfig = true;
                        $scope.changed = true;
                      },
                      function (err) {
                        Notifications.error(err.data.errorMessage);
                      },
                  );
                }
              },
              function (data) {
                console.log("Error occurred while getting config", data.error);
                $scope.ClickableConfig = true;
              },
            );
          },
          function (data) {
            console.log("Error occurred while getting properties", data.error);
            $scope.ClickableConfig = true;
          },
        );
      }
    };

    $scope.loadNotificationConfig = function (provider) {
      if ($scope.ClickableConfig) {
        $scope.ClickableConfig = false;
        CibaNotProviderDetails.get(
          { realm: realm.realm, client: client.id, providerId: provider.id },
          function (data) {
            let configProps = data.properties;
            CibaNotProviderConfigDetails.get(
              {
                realm: realm.realm,
                client: client.id,
                providerId: provider.id,
              },
              function (data) {
                let settingConfigs = data.config;
                $scope.openNotifierConfigPanel(
                  provider,
                  configProps,
                  settingConfigs,
                );
              },
              function (data) {
                console.log("Error occurred while getting config", data.error);
                $scope.ClickableConfig = true;
              },
            );
          },
          function (data) {
            console.log("Error occurred while getting properties", data.error);
            $scope.ClickableConfig = true;
          },
        );
      }
    };

    $scope.loadResultHookConfig = function (provider) {
      if ($scope.ClickableConfig) {
        $scope.ClickableConfig = false;
        CibaResultHookProviderDetails.get(
          { realm: realm.realm, client: client.id, providerId: provider.id },
          function (data) {
            let configProps = data.properties;
            CibaResultHookProviderConfigDetails.get(
              {
                realm: realm.realm,
                client: client.id,
                providerId: provider.id,
              },
              function (data) {
                let settingConfigs = data.config;
                $scope.openNotifierConfigPanel(
                  provider,
                  configProps,
                  settingConfigs,
                );
              },
              function (data) {
                console.log("Error occurred while getting config", data.error);
                $scope.ClickableConfig = true;
              },
            );
          },
          function (data) {
            console.log("Error occurred while getting properties", data.error);
            $scope.ClickableConfig = true;
          },
        );
      }
    };

    $scope.openNotifierConfigPanel = function (
      provider,
      configProps,
      settingConfigs,
    ) {
      var controller = function ($scope, $location) {
        $scope.realm = realm;
        $scope.stage = 1;
        $scope.teststage = 1;
        $scope.alreadySet = false;
        $scope.provider = provider;
        $scope.configType = angular.copy(configProps);
        $scope.resourceUrl = resourceUrl;
        $scope.changed = false;
        $scope.oldvalue = {};
        $scope.oldvalue = angular.copy(settingConfigs);
        let isInitial = true;

        var defaultConfig = {};
        if (configProps && Array.isArray(configProps)) {
          for (var i = 0; i < configProps.length; i++) {
            var property = configProps[i];
            if (property && property.name) {
              defaultConfig[property.name] = property.defaultValue;
            }
          }
        }
        if (!angular.equals(settingConfigs, {})) {
          $scope.config = { config: settingConfigs };
          $scope.config.alias = settingConfigs.alias;
        } else {
          $scope.alreadySet = false;
          $scope.config = { config: defaultConfig };
        }
        $scope.$watch(
          "config",
          function () {
            if (
              (!angular.equals($scope.oldvalue, $scope.config.config)) &&
              checkConfigWithMultipleDependsOn(
                $scope.config.config,
                $scope.configType,
              )
            ) {
              $scope.changed = true;
              isInitial = false;
            } else {
              $scope.changed = false;
            }
          },
          true,
        );

        $scope.isUpdated = function () {
          return !angular.equals(settingConfigs, {}) && $scope.alreadySet;
        };

        $scope.save = function () {
          makeConfigVisible();
          $scope.stage = 2;
          $("#cancelBtn").attr("disabled", "disabled");
          var configCopy = angular.copy($scope.config);
          ComponentUtils.convertAllListValuesToMultivaluedString(
            $scope.configType,
            configCopy.config,
          );

          if (provider.id === "dead-letter-hook-notification-provider") {
            CibaResultHookProviderConfigDetails.update(
              {
                realm: realm.realm,
                providerId: provider.id,
                client: client.id,
              },
              configCopy,
              function () {
                Notifications.success($translate.instant("changesSavedSuccess"));
                $scope.$panelInstance.dismiss("this is from the controller!!");
                if(configCopy.config && configCopy.config.enable && configCopy.config.enable === 'true'){
                  client.resultHookId = provider.id;
                }else {
                  client.resultHookId = "";
                }
              },
              function (err) {
                $scope.stage = 1;
                $("#cancelBtn").removeAttr("disabled", "disabled");
                Notifications.error($translate.instant(err.data.error_message));            },
            );
          } else {
            CibaNotProviderConfigDetails.update(
              {
                realm: realm.realm,
                providerId: provider.id,
                client: client.id,
              },
              configCopy,
              function () {
                Notifications.success($translate.instant("changesSavedSuccess"));
                $scope.$panelInstance.dismiss("this is from the controller!!");
                client.notifyWayId = provider.id;
              },
              function (err) {
                $scope.stage = 1;
                $("#cancelBtn").removeAttr("disabled", "disabled");
                Notifications.error($translate.instant(err.data.error_message));            },
            );
          }
        };

        $scope.dismissPanel = function () {
          makeConfigVisible();
          $scope.$panelInstance.dismiss("this is from the controller!!");
        };

        $scope.$on("$routeChangeStart", function () {
          $scope.$panelInstance.dismiss();
        });

        $scope.reset = function () {
          $scope.config.config = angular.copy($scope.oldvalue);
          makeConfigVisible();
          $scope.$panelInstance.dismiss("this is from the controller!!");
        };

        $scope.istestConfigured = false;
        $scope.test = function () {
          $scope.teststage = 2;
          var configCopy = angular.copy($scope.config);
          ComponentUtils.convertAllMultivaluedStringValuesToList(
            $scope.configType.properties,
            configCopy.config,
          );
          ComponentUtils.convertAllListValuesToMultivaluedString(
            $scope.configType.properties,
            configCopy.config,
          );
          TestEventsConfig.save(
            { realm: realm.realm, provider: "kafka-streamer" },
            configCopy.config,
            function (confRes) {
              $scope.teststage = 1;
              if (confRes.success) {
                Notifications.success(
                  $translate.instant("config-test-succeeded"),
                );
                $scope.istestConfigured = true;
              } else {
                Notifications.error($translate.instant(confRes.message));
              }
            },
          );
        };
      };

      $http
        .get(resourceUrl + "/partials/panels/ciba-config-panel.html")
        .then(function (result) {
          var panelInstance2 = angularSlideOutPanel.open({
            template: result.data,
            openOn: $scope.getPanelDirection(),
            backdrop: "static",
            controller: ["$scope", "$location", controller],
          });
          panelInstance2.result
            .then(function (result) {
              //Deselects the Selected Node in the Fl
              console.log("panel was closed with result : ", result);
              makeConfigVisible();
            })
            .catch(function (error) {
              makeConfigVisible();
              console.log("panel was rejected with error : ", error);
            });
        });
    };

    $scope.openAuthorizerConfigPanel = function (
      provider,
      configProps,
      settingConfigs,
    ) {
      var controller = function ($scope, $location) {
        $scope.realm = realm;
        $scope.stage = 1;
        $scope.alreadySet = false;
        $scope.provider = provider;
        $scope.configType = angular.copy(configProps);
        $scope.resourceUrl = resourceUrl;
        $scope.changed = false;
        $scope.oldvalue = {};
        ComponentUtils.convertAllMultivaluedStringValuesToList(configProps, settingConfigs)
        $scope.oldvalue = angular.copy(settingConfigs);
        ComponentUtils.addLastEmptyValueToMultivaluedLists(configProps, $scope.oldvalue);
        let isInitial = true;
        $scope.cibaNotify = settingConfigs["ciba-notify"];

        var defaultConfig = {};
        if (configProps && Array.isArray(configProps)) {
          for (var i = 0; i < configProps.length; i++) {
            var property = configProps[i];
            if (property && property.name) {
              defaultConfig[property.name] = property.defaultValue;
            }
          }
        }

        if (!angular.equals(settingConfigs, {})) {
          $scope.config = { config: settingConfigs };
          $scope.config.alias = settingConfigs.alias;
        } else {
          $scope.alreadySet = false;
          $scope.config = { config: defaultConfig };
        }

        const smsDefaultText =
          $translate.instant("sms-default-txt-welcome") +
          " " +
          "{{transaction_from}}" +
          " " +
          $translate.instant("sms-default-txt-approve") +
          " " +
          "{{link}}" +
          ". " +
          $translate.instant("sms-default-txt-expiration") +
          " " +
          "{{expirationTime}}" +
          " " +
          "{{expirationUnit}}";

        $scope.$watch(
          "config.config['ciba-notify']",
          function () {
            if ($scope.config.config["ciba-notify"] !== $scope.cibaNotify) {
              Object.keys(defaultConfig).map((key) => {
                if (key === "sms-template") {
                  $scope.config.config[key] =
                    settingConfigs["sms-template"] === smsDefaultText
                      ? settingConfigs["sms-template"]
                      : smsDefaultText;
                } else if (key !== "ciba-notify") {
                  $scope.config.config[key] = defaultConfig[key];
                }
              });
              $scope.cibaNotify = $scope.config.config["ciba-notify"];
            }
          },
          true,
        );

        $scope.$watch(
          "config",
          function () {
            if (
              !angular.equals($scope.oldvalue, $scope.config.config) &&
              checkConfigWithMultipleDependsOn(
                $scope.config.config,
                $scope.configType,
              )
            ) {
              isInitial = false;
              $scope.changed = true;
            } else {
              $scope.changed = false;
            }
          },
          true,
        );

        $scope.isUpdated = function () {
          return !angular.equals(settingConfigs, {}) && $scope.alreadySet;
        };

        $scope.save = function () {
          $scope.stage = 2;
          $("#cancelBtn").attr("disabled", "disabled");
          var configCopy = angular.copy($scope.config);
          ComponentUtils.convertAllListValuesToMultivaluedString(
            $scope.configType,
            configCopy.config,
          );
          if (
            configCopy.config.hasOwnProperty("verify-expiry-time") &&
            configCopy.config["verify-expiry-time"]
          ) {
            const isValidExpiryTime = ComponentUtils.checkExpiryTimeValid(
              $scope.configType,
              configCopy.config,
            );
            if (!isValidExpiryTime) {
              $scope.stage = 1;
              $("#cancelBtn").removeAttr("disabled", "disabled");
              Notifications.error($translate.instant("invalid-expiry"));
              return;
            }
          }
          // if ($scope.alreadySet) {
          CibaProviderConfigDetails.update(
            {
              realm: realm.realm,
              providerId: provider.id,
              client: client.id,
            },
            configCopy,
            function () {
              Notifications.success($translate.instant("changesSavedSuccess"));
              $scope.$panelInstance.dismiss("this is from the controller!!");
              addTemplateStep(provider.id === "twilio-ivr-ciba-authorizer" || provider.id === 'api-authorizer');
              client.authWayId = provider.id;
            },
            function (err) {
              $scope.stage = 1;
              $("#cancelBtn").removeAttr("disabled", "disabled");
              Notifications.error(err.data.errorMessage);
            },
          );
        };

        $scope.dismissPanel = function () {
          makeConfigVisible();
          $scope.$panelInstance.dismiss("this is from the controller!!");
        };

        $scope.$on("$routeChangeStart", function () {
          $scope.$panelInstance.dismiss();
        });

        $scope.reset = function () {
          $scope.config.config = angular.copy($scope.oldvalue);
          $scope.$panelInstance.dismiss("this is from the controller!!");
        };
      };

      $http
        .get(resourceUrl + "/partials/panels/ciba-config-panel.html")
        .then(function (result) {
          var panelInstance2 = angularSlideOutPanel.open({
            template: result.data,
            openOn: $scope.getPanelDirection(),
            backdrop: "static",
            controller: ["$scope", "$location", controller],
          });
          panelInstance2.result
            .then(function (result) {
              //Deselects the Selected Node in the Fl
              console.log("panel was closed with result : ", result);
              makeConfigVisible();
            })
            .catch(function (error) {
              makeConfigVisible();
              console.log("panel was rejected with error : ", error);
            });
        });
    };
  },
);
// CIBA controller
module.controller(
  "ClientCibaCtrl",
  function (
    $scope,
    $route,
    realm,
    client,
    Client,
    serverInfo,
    angularSlideOutPanel,
    $http,
    CibaProviderDetails,
    CibaProviderConfigDetails,
    CibaNotProviderDetails,
    CibaNotProviderConfigDetails,
    CibaResultHookProviderDetails,
    CibaResultHookProviderConfigDetails,
    ComponentUtils,
    Notifications,
    $translate,
    $http,
    TestEventsConfig,
  ) {
    function getmanagementType(typeId) {
      var filteredValue = $scope.cibaAuthProviders.filter((provider) => {
        if (provider.id === typeId) {
          return provider;
        }
      });
      if (filteredValue !== undefined || filteredValue !== null) {
        return filteredValue[0];
      } else {
        return " ";
      }
    }


    function getNotificationType(typeId) {
      var filteredValue = $scope.cibaNotificationProviders.filter(
        (provider) => {
          if (provider.id === typeId) {
            return provider;
          }
        },
      );
      if (filteredValue !== undefined || filteredValue !== null) {
        return filteredValue[0];
      } else {
        return " ";
      }
    }
    $scope.realm = realm;
    $scope.changed = false;
    $scope.client = angular.copy(client);
    $scope.stage = 1;
    $scope.ClickableConfig = true;
    // Provider details
    $scope.cibaAuthProviders = serverInfo.cibaAuthProviders;
    $scope.cibaResultProviders = serverInfo.cibaDeadLetterHookNotificationProviders;
    $scope.cibaNotificationProviders = serverInfo.cibaNotificationProviders;
    // management display name
    $scope.cibaAuthenticationManagementType =
      $scope.client.authenticationFlowBindingOverrides.ciba_auth;
    $scope.cibaNotificationManagementType =
      $scope.client.attributes["ciba.backchannel.token.delivery.mode"];
    $scope.clientEdit = angular.copy(client);
    $scope.clientEdit.attributes.cibaAuthenticationManagementType =
      getmanagementType($scope.cibaAuthenticationManagementType);
    $scope.clientEdit.attributes["ciba.backchannel.token.delivery.mode"] =
      getNotificationType($scope.cibaNotificationManagementType);
    $scope.showNotificationWay = $scope.cibaAuthenticationManagementType !== 'api-authorizer';
    // $scope.supportedCibaAuthenticationMethods =
    //   client.attributes.supportedCibaAuthenticationMethods
    //     .replace("[", "")
    //     .replace("]", "")
    //     .split(",");
    // $scope.backchannelTokenDeliveryMode =
    //   client.attributes["ciba.backchannel.token.delivery.mode"];
    // $scope.supportedAuthenticationUserHint = [
    //   client.attributes.cibaAuthRequestedUserHint,
    // ];
    // $scope.authenticationUserHint = client.attributes.cibaAuthRequestedUserHint;
    // $scope.clientNotificationEndpoint =
    //   client.attributes["ciba.backchannel.client.notification.endpoint"];
    // // expiry and interval returns as string from backend even thought it is saved as integer
    // $scope.clientEdit.attributes.cibaExpiresIn = Number(
    //   $scope.clientEdit.attributes.cibaExpiresIn
    // );
    // $scope.clientEdit.attributes.cibaInterval = Number(
    //   $scope.clientEdit.attributes.cibaInterval
    // );

    function isValidRedirectUri(uri) {
      return uri.startsWith("http") || uri === "*";
    }

    document.addEventListener("keydown", function (event) {
      if (event.key === "Escape" || event.keyCode === 27) {
        if (!$scope.ClickableConfig) {
          $scope.ClickableConfig = true;
          $route.reload();
        }
      }
    });

    $scope.loadAuthorizerConfig = function (provider) {
      if (provider && provider.configured === false) {
        Notifications.warn($translate.instant("ciba-authorizer-not-configured-tooltip"));
        return;
      }
      if ($scope.ClickableConfig) {
        $scope.ClickableConfig = false;
        CibaProviderDetails.get(
          { realm: realm.realm, client: client.id, providerId: provider.id },
          function (data) {
            let configProps = data.properties;
            CibaProviderConfigDetails.get(
              {
                realm: realm.realm,
                client: client.id,
                providerId: provider.id,
              },
              function (data) {
                  if (configProps && configProps.length > 0) {
                    let settingConfigs = data.config;
                    $scope.showNotificationWay = true;
                    $scope.openAuthorizerConfigPanel(
                      provider,
                      configProps,
                      settingConfigs,
                    );
                  } else {
                    CibaProviderConfigDetails.update(
                        {
                          realm: realm.realm,
                          providerId: provider.id,
                          client: client.id,
                        },
                        data.config,
                        function () {
                          $scope.authWayId = provider.id
                          $scope.showNotificationWay = false;
                          Notifications.success($translate.instant("changesSavedSuccess"));
                          $route.reload();
                          $scope.$panelInstance.dismiss("this is from the controller!!");
                        },
                        function (err) {
                          $scope.stage = 1;
//                          $("#cancelBtn").removeAttr("disabled", "disabled");
                          Notifications.error(err.data.errorMessage);
                        },
                      );
                  }

              },
              function (data) {
                console.log("Error occurred while getting config", data.error);
                $scope.ClickableConfig = true;
              },
            );
          },
          function (data) {
            console.log("Error occurred while getting properties", data.error);
            $scope.ClickableConfig = true;
          },
        );
      }
    };

    $scope.loadNotificationConfig = function (provider) {
      if ($scope.ClickableConfig) {
        $scope.ClickableConfig = false;
        CibaNotProviderDetails.get(
          { realm: realm.realm, client: client.id, providerId: provider.id },
          function (data) {
            let configProps = data.properties;
            CibaNotProviderConfigDetails.get(
              {
                realm: realm.realm,
                client: client.id,
                providerId: provider.id,
              },
              function (data) {
                let settingConfigs = data.config;
                $scope.openNotifierConfigPanel(
                  provider,
                  configProps,
                  settingConfigs,
                );
              },
              function (data) {
                console.log("Error occurred while getting config", data.error);
                $scope.ClickableConfig = true;
              },
            );
          },
          function (data) {
            console.log("Error occurred while getting properties", data.error);
            $scope.ClickableConfig = true;
          },
        );
      }
    };

    $scope.loadResultHookConfig = function (provider) {
          if ($scope.ClickableConfig) {
            $scope.ClickableConfig = false;
            CibaResultHookProviderDetails.get(
              { realm: realm.realm, client: client.id, providerId: provider.id },
              function (data) {
                let configProps = data.properties;
                CibaResultHookProviderConfigDetails.get(
                  {
                    realm: realm.realm,
                    client: client.id,
                    providerId: provider.id,
                  },
                  function (data) {
                    let settingConfigs = data.config;
                    $scope.openNotifierConfigPanel(
                      provider,
                      configProps,
                      settingConfigs,
                    );
                  },
                  function (data) {
                    console.log("Error occurred while getting config", data.error);
                    $scope.ClickableConfig = true;
                  },
                );
              },
              function (data) {
                console.log("Error occurred while getting properties", data.error);
                $scope.ClickableConfig = true;
              },
            );
          }
        };

    // $scope.openNotificationTypePanel = function (notificationType) {
    //   const _generatePropertiesBasedOnNotificationType = (notificationType) => {
    //     var properties = [];

    //     // Add property based on notificaiton type
    //     switch (notificationType.trim()) {
    //       case "poll":
    //         properties.push({
    //           label: $translate.instant("secure-plus-expires-in"),
    //           helpText: $translate.instant("secure-plus-expires-in-hlp-txt"),
    //           name: "cibaExpiresIn",
    //           type: "Number",
    //           required: true,
    //           secret: false,
    //           maxVal: 0,
    //           minVal: 5,
    //         });
    //         properties.push({
    //           label: $translate.instant("ciba-interval"),
    //           helpText: $translate.instant("ciba-interval.tooltip"),
    //           name: "cibaInterval",
    //           type: "Number",
    //           required: true,
    //           secret: false,
    //           maxVal: 0,
    //           minVal: 120,
    //         });
    //         break;
    //       case "push":
    //         properties.push({
    //           label: $translate.instant("notification-endpoint"),
    //           helpText: $translate.instant(
    //             "ciba-push-client-notification-endpoint.tooltip"
    //           ),
    //           name: "ciba.backchannel.client.notification.endpoint",
    //           type: "String",
    //           required: true,
    //           secret: false,
    //           maxVal: 0,
    //           minVal: 0,
    //         });
    //         break;
    //       case "ping":
    //         properties.push({
    //           label: $translate.instant("secure-plus-expires-in"),
    //           helpText: $translate.instant("secure-plus-expires-in-hlp-txt"),
    //           name: "cibaExpiresIn",
    //           type: "Number",
    //           required: true,
    //           secret: false,
    //           maxVal: 0,
    //           minVal: 120,
    //         });
    //         properties.push({
    //           label: $translate.instant("notification-endpoint"),
    //           helpText: $translate.instant(
    //             "ciba-client-notification-endpoint.tooltip"
    //           ),
    //           name: "ciba.backchannel.client.notification.endpoint",
    //           type: "String",
    //           required: true,
    //           secret: false,
    //           maxVal: 0,
    //           minVal: 0,
    //         });

    //         break;
    //     }
    //     return properties;
    //   };

    //   const _generateConfig = (client, notificationType) => {
    //     var config = {};
    //     switch (notificationType.trim()) {
    //       case "poll":

    //         config["cibaExpiresIn"] =
    //         Number(client.attributes.cibaExpiresIn) || 120;
    //         config["cibaInterval"] =
    //           Number(client.attributes.cibaInterval) || 5;
    //         break;
    //       case "push":
    //         config["ciba.backchannel.client.notification.endpoint"] =
    //           client.attributes[
    //             "ciba.backchannel.client.notification.endpoint"
    //           ] || "";
    //           break;
    //       case "ping":
    //         config["cibaExpiresIn"] =
    //         Number(client.attributes.cibaExpiresIn) || 120;
    //         config["ciba.backchannel.client.notification.endpoint"] =
    //           client.attributes[
    //             "ciba.backchannel.client.notification.endpoint"
    //           ] || "";
    //         break;
    //     }
    //     return config;
    //   };

    //   const properties =
    //     _generatePropertiesBasedOnNotificationType(notificationType);
    //   const config = _generateConfig(client, notificationType);

    //   var controller = function ($scope, $location) {
    //     $scope.realm = realm;
    //     $scope.client = angular.copy(client);
    //     $scope.stage = 1;
    //     $scope.properties = properties;
    //     $scope.config = config;
    //     $scope.configType = angular.copy($scope.config);
    //     $scope.notification = notificationType.trim();
    //     $scope.changed = false;
    //     $scope.alreadySet = false;
    //     if (
    //       $scope.provider ===
    //       client.authenticationFlowBindingOverrides.ciba_auth
    //     ) {
    //       $scope.alreadySet = true;
    //     }
    //     $scope.oldvalue = angular.copy($scope.config);

    //     // validations
    //     if (!angular.equals(config, {})) {
    //       $scope.config = $scope.config;
    //       $scope.config.alias = $scope.config.alias;
    //     }

    //     $scope.$watch(
    //       "config",
    //       function () {
    //         if (
    //           (!angular.equals($scope.oldvalue, $scope.config) &&
    //             checkConfigNull($scope.config, [$scope.configType])) ||
    //           $scope.client.attributes[
    //             "ciba.backchannel.token.delivery.mode"
    //           ].trim() !== $scope.notification.trim()
    //         ) {
    //           $scope.changed = true;
    //         } else {
    //           $scope.changed = false;
    //         }
    //       },
    //       true
    //     );

    //     // panel settings
    //     $scope.isUpdated = function () {
    //       return !angular.equals($scope.config, {}) && $scope.alreadySet;
    //     };

    //     $scope.dismissPanel = function () {
    //       $route.reload();
    //       $scope.$panelInstance.dismiss("this is from the controller!!");
    //     };

    //     $scope.$on("$routeChangeStart", function () {
    //       $scope.$panelInstance.dismiss();
    //     });

    //     $scope.save = function () {
    //       $scope.stage = 2;
    //       $("#cancelBtn").attr("disabled", "disabled");
    //       var configCopy = angular.copy($scope.config);
    //       ComponentUtils.convertAllListValuesToMultivaluedString(
    //         $scope.configType,
    //         configCopy.config
    //       );

    //       // check expiry time
    //       if (!configCopy["cibaExpiresIn"] && $scope.notification != "push") {
    //         $scope.stage = 1;
    //         $("#cancelBtn").removeAttr("disabled", "disabled");
    //         Notifications.error("Invalid Expiry Time. Please check settings");
    //         return;
    //       }

    //       if ($scope.notification == "poll" && !configCopy["cibaInterval"]) {
    //         $scope.stage = 1;
    //         $("#cancelBtn").removeAttr("disabled", "disabled");
    //         Notifications.error("Invalid Interval. Please check settings");
    //         return;
    //       }
    //       // check for valid url
    //       if (
    //         ($scope.notification == "ping" || $scope.notification == "push") &&
    //         (!configCopy["ciba.backchannel.client.notification.endpoint"] ||
    //           !configCopy[
    //             "ciba.backchannel.client.notification.endpoint"
    //           ].startsWith("https") ||
    //           configCopy["ciba.backchannel.client.notification.endpoint"] ===
    //             "*")
    //       ) {
    //         $scope.stage = 1;
    //         $("#cancelBtn").removeAttr("disabled", "disabled");
    //         Notifications.error("Invalid endpoint. Please check settings");
    //         return;
    //       }
    //       $scope.client.attributes["ciba.backchannel.token.delivery.mode"] =
    //         $scope.notification.trim();
    //       $scope.client.attributes.cibaExpiresIn = $scope.config.cibaExpiresIn;

    //       if ($scope.notification.trim() === "poll") {
    //         $scope.client.attributes.cibaInterval = $scope.config.cibaInterval;
    //       } else if ($scope.notification.trim() === "ping" || $scope.notification.trim() === "push") {
    //         $scope.client.attributes[
    //           "ciba.backchannel.client.notification.endpoint"
    //         ] = $scope.config["ciba.backchannel.client.notification.endpoint"];
    //       }

    //       Client.update(
    //         {
    //           realm: realm.realm,
    //           client: client.id,
    //         },
    //         $scope.client,
    //         function () {
    //           $route.reload();
    //           Notifications.success(
    //             $translate.instant("changes-saved-to-application")
    //           );
    //         },
    //         function () {
    //           $scope.stage = 1;
    //           Notifications.error(
    //             "Failed to update notification type settings."
    //           );
    //         }
    //       );
    //     };
    //   };
    //   $http
    //     .get(resourceUrl + "/partials/panels/ciba-notfication-type-panel.html")
    //     .then(function (result) {
    //       var panelInstance2 = angularSlideOutPanel.open({
    //         template: result.data,
    //         openOn: "right",
    //         backdrop: "static",
    //         controller: ["$scope", "$location", controller],
    //       });
    //       panelInstance2.result
    //         .then(function (result) {
    //           //Deselects the Selected Node in the Flow
    //           editor.deselectNode();
    //           ev.stopPropagation();
    //           ev.preventDefault();
    //           console.log("panel was closed with result : ", result);
    //         })
    //         .catch(function (error) {
    //           if (error && error === "backdrop click") {
    //             $route.reload();
    //           }
    //           editor.deselectNode();
    //           ev.stopPropagation();
    //           ev.preventDefault();
    //           console.log("panel was rejected with error : ", error);
    //         });
    //     });
    // };

    $scope.openNotifierConfigPanel = function (
      provider,
      configProps,
      settingConfigs,
    ) {
      var controller = function ($scope, $location) {
        $scope.realm = realm;
        $scope.stage = 1;
        $scope.teststage = 1;
        $scope.alreadySet = false;
        $scope.provider = provider;
        if (
          $scope.provider ===
          client.attributes["ciba.backchannel.token.delivery.mode"]
        ) {
          $scope.alreadySet = true;
        }
        $scope.configType = angular.copy(configProps);
        $scope.resourceUrl = resourceUrl;
        $scope.changed = false;
        $scope.oldvalue = {};

        $scope.oldvalue = angular.copy(settingConfigs);

        var defaultConfig = {};
        if (configProps && Array.isArray(configProps)) {
          for (var i = 0; i < configProps.length; i++) {
            var property = configProps[i];
            if (property && property.name) {
              defaultConfig[property.name] = property.defaultValue;
            }
          }
        }

        if (!angular.equals(settingConfigs, {})) {
          $scope.config = { config: settingConfigs };
          $scope.config.alias = settingConfigs.alias;
        } else {
          $scope.alreadySet = false;
          $scope.config = { config: defaultConfig };
        }
        $scope.$watch(
          "config",
          function () {
            if (
              !angular.equals($scope.oldvalue, $scope.config.config) &&
              checkConfigWithMultipleDependsOn(
                $scope.config.config,
                $scope.configType,
              )
            ) {
              $scope.changed = true;
            } else {
              $scope.changed = false;
            }
          },
          true,
        );

        $scope.$watch('config.config["hook-way"]', function(){
          if(!$scope.config.config['secret-keys']){
            $scope.configType = $scope.configType.filter(
              (e) => e.label != $scope.onDemandSecretProp.label,
            );
          }
        }, true);

        $scope.checkSecretKey = function () {
          $scope.configType.map((property) => {
            if (property.onDemand && property.type === "MapList") {
              $http
                .post(authUrl + property.onDemand, $scope.config.config)
                .then(function (response) {
                  var newConfigProp = response.data.body;
                  $scope.onDemandSecretProp = response.data.body[0];
                  $scope.configType = $scope.configType.filter(
                    (e) => e.label != $scope.onDemandSecretProp.label,
                  );
                  $scope.configType.push(
                    $scope.onDemandSecretProp,
                  );
                });
            }
          });
        };

        $scope.$watch(
          "config",
          function () {
            $scope.checkSecretKey();
            if (
              !angular.equals($scope.oldvalue, $scope.config.config) &&
              checkConfigWithMultipleDependsOn(
                $scope.config.config,
                $scope.configType,
              )
            ) {
              $scope.changed = true;
            } else {
              $scope.changed = false;
            }
          },
          true,
        );

        $scope.isUpdated = function () {
          return !angular.equals(settingConfigs, {}) && $scope.alreadySet;
        };

        $scope.save = function () {
          $scope.stage = 2;
          $("#cancelBtn").attr("disabled", "disabled");
          var configCopy = angular.copy($scope.config);
          ComponentUtils.convertAllListValuesToMultivaluedString(
            $scope.configType,
            configCopy.config,
          );
          // if ($scope.alreadySet) {
          if (provider.id === "dead-letter-hook-notification-provider") {
            CibaResultHookProviderConfigDetails.update(
              {
                realm: realm.realm,
                providerId: provider.id,
                client: client.id,
              },
              configCopy,
              function () {
                Notifications.success($translate.instant("changesSavedSuccess"));
                $route.reload();
                $scope.$panelInstance.dismiss("this is from the controller!!");
              },
              function (err) {
                $scope.stage = 1;
                $("#cancelBtn").removeAttr("disabled", "disabled");
                Notifications.error($translate.instant(err.data.error_message));            },
            );
          } else  {
            CibaNotProviderConfigDetails.update(
              {
                realm: realm.realm,
                providerId: provider.id,
                client: client.id,
              },
              configCopy,
              function () {
                Notifications.success($translate.instant("changesSavedSuccess"));
                $route.reload();
                $scope.$panelInstance.dismiss("this is from the controller!!");
              },
              function (err) {
                $scope.stage = 1;
                $("#cancelBtn").removeAttr("disabled", "disabled");
                Notifications.error($translate.instant(err.data.error_message));            },
            );
          }
        };

        $scope.dismissPanel = function () {
          $route.reload();
          $scope.$panelInstance.dismiss("this is from the controller!!");
        };

        $scope.$on("$routeChangeStart", function () {
          $scope.$panelInstance.dismiss();
        });

        $scope.reset = function () {
          $scope.config.config = angular.copy($scope.oldvalue);
          $scope.$panelInstance.dismiss("this is from the controller!!");
          $route.reload();
        };

        $scope.istestConfigured = false;
        $scope.test = function () {
          $scope.teststage = 2;
          var configCopy = angular.copy($scope.config);
          ComponentUtils.convertAllMultivaluedStringValuesToList(
            $scope.configType.properties,
            configCopy.config,
          );
          ComponentUtils.convertAllListValuesToMultivaluedString(
            $scope.configType.properties,
            configCopy.config,
          );
          TestEventsConfig.save(
            { realm: realm.realm, provider: "kafka-streamer" },
            configCopy.config,
            function (confRes) {
              $scope.teststage = 1;
              if (confRes.success) {
                Notifications.success(
                  $translate.instant("config-test-succeeded"),
                );
                $scope.istestConfigured = true;
              } else {
                Notifications.error($translate.instant(confRes.message));
              }
            },
          );
        };
      };

      $http
        .get(resourceUrl + "/partials/panels/ciba-config-panel.html")
        .then(function (result) {
          var panelInstance2 = angularSlideOutPanel.open({
            template: result.data,
            openOn: $scope.getPanelDirection(),
            backdrop: "static",
            controller: ["$scope", "$location", controller],
          });
          panelInstance2.result
            .then(function (result) {
              //Deselects the Selected Node in the Flow
              editor.deselectNode();
              ev.stopPropagation();
              ev.preventDefault();
              console.log("panel was closed with result : ", result);
            })
            .catch(function (error) {
              if (error && error === "backdrop click") {
                $route.reload();
              }
              editor.deselectNode();
              ev.stopPropagation();
              ev.preventDefault();
              console.log("panel was rejected with error : ", error);
            });
        });
    };

    $scope.openAuthorizerConfigPanel = function (
      provider,
      configProps,
      settingConfigs,
    ) {
      var controller = function ($scope, $location, ComponentUtils) {
        $scope.realm = realm;
        $scope.stage = 1;
        $scope.alreadySet = false;
        $scope.provider = provider;
        $scope.config = {};
        if (
          $scope.provider ===
          client.authenticationFlowBindingOverrides.ciba_auth
        ) {
          $scope.alreadySet = true;
        }
        $scope.configType = angular.copy(configProps);
        $scope.resourceUrl = resourceUrl;
        $scope.changed = false;
        $scope.oldvalue = {};

        ComponentUtils.convertAllMultivaluedStringValuesToList(configProps, settingConfigs)
        $scope.oldvalue = angular.copy(settingConfigs);
        ComponentUtils.addLastEmptyValueToMultivaluedLists(configProps, $scope.oldvalue);
        $scope.cibaNotify = settingConfigs["ciba-notify"];

        var defaultConfig = {};
        if (configProps && Array.isArray(configProps)) {
          for (var i = 0; i < configProps.length; i++) {
            var property = configProps[i];
            if (property && property.name) {
              defaultConfig[property.name] = property.defaultValue;
            }
          }
        }

        $scope.config['config'] = {};
        Object.keys(defaultConfig).map(key => {
          if(settingConfigs[key]) {
            $scope.config.config[key] = settingConfigs[key];
            $scope.config.alias = settingConfigs.alias;
          } else {
            $scope.config.config[key] = defaultConfig[key];
            $scope.alreadySet = false;
          }
        });

        // if (!angular.equals(settingConfigs, {})) {
        //   $scope.config = { config: settingConfigs };
        //   $scope.config.alias = settingConfigs.alias;
        // } else {
        //   $scope.alreadySet = false;
        //   $scope.config = { config: defaultConfig };
        // }

        const smsDefaultText =
          $translate.instant("sms-default-txt-welcome") +
          " " +
          "{{transaction_from}}" +
          " " +
          $translate.instant("sms-default-txt-approve") +
          " " +
          "{{link}}" +
          ". " +
          $translate.instant("sms-default-txt-expiration") +
          " " +
          "{{expirationTime}}" +
          " " +
          "{{expirationUnit}}";

        $scope.$watch(
          "config.config['ciba-notify']",
          function () {
            if ($scope.config.config["ciba-notify"] !== $scope.cibaNotify) {
              Object.keys(defaultConfig).map((key) => {
                if (key === "sms-template") {
                  $scope.config.config[key] =
                    settingConfigs["sms-template"] === smsDefaultText
                      ? settingConfigs["sms-template"]
                      : smsDefaultText;
                } else if (key !== "ciba-notify") {
                  $scope.config.config[key] = defaultConfig[key];
                }
              });
              $scope.cibaNotify = $scope.config.config["ciba-notify"];
            }
          },
          true,
        );

        $scope.checkSecretKey = function () {
          $scope.configType.map((property) => {
            if (property.onDemand && property.type === "MapList") {
              $http
                .post(authUrl + property.onDemand, $scope.config.config)
                .then(function (response) {
                  var newConfigProp = response.data.body;
                  $scope.configType = $scope.configType.filter(
                    (e) => e.label != newConfigProp[0].label,
                  );
                  $scope.configType.push.apply(
                    $scope.configType,
                    newConfigProp,
                  );
                });
            }
          });
        };

        $scope.$watch(
          "config",
          function () {
            $scope.checkSecretKey();
            if (
              !angular.equals($scope.oldvalue, $scope.config.config) &&
              checkConfigWithMultipleDependsOn(
                $scope.config.config,
                $scope.configType,
              )
            ) {
              $scope.changed = true;
            } else {
              $scope.changed = false;
            }
          },
          true,
        );

        $scope.isUpdated = function () {
          return !angular.equals(settingConfigs, {}) && $scope.alreadySet;
        };

        $scope.save = function () {
          $scope.stage = 2;
          $("#cancelBtn").attr("disabled", "disabled");
          var configCopy = angular.copy($scope.config);
          ComponentUtils.convertAllListValuesToMultivaluedString(
            $scope.configType,
            configCopy.config,
          );
          if (
            configCopy.config.hasOwnProperty("verify-expiry-time") &&
            configCopy.config["verify-expiry-time"]
          ) {
            const isValidExpiryTime = ComponentUtils.checkExpiryTimeValid(
              $scope.configType,
              configCopy.config,
            );
            if (!isValidExpiryTime) {
              $scope.stage = 1;
              $("#cancelBtn").removeAttr("disabled", "disabled");
              Notifications.error($translate.instant("invalid-expiry"));
              return;
            }
          }
          // if ($scope.alreadySet) {
          CibaProviderConfigDetails.update(
            {
              realm: realm.realm,
              providerId: provider.id,
              client: client.id,
            },
            configCopy,
            function () {
              Notifications.success($translate.instant("changesSavedSuccess"));
              $route.reload();
              $scope.$panelInstance.dismiss("this is from the controller!!");
            },
            function (err) {
              $scope.stage = 1;
              $("#cancelBtn").removeAttr("disabled", "disabled");
              Notifications.error(err.data.errorMessage);
            },
          );
        };

        $scope.dismissPanel = function () {
          $route.reload();
          $scope.$panelInstance.dismiss("this is from the controller!!");
        };

        $scope.$on("$routeChangeStart", function () {
          $scope.$panelInstance.dismiss();
        });

        $scope.reset = function () {
          $scope.config.config = angular.copy($scope.oldvalue);
          $scope.$panelInstance.dismiss("this is from the controller!!");
          $route.reload();
        };
      };

      $http
        .get(resourceUrl + "/partials/panels/ciba-config-panel.html")
        .then(function (result) {
          var panelInstance2 = angularSlideOutPanel.open({
            template: result.data,
            openOn: $scope.getPanelDirection(),
            backdrop: "static",
            controller: ["$scope", "$location", "ComponentUtils",  controller],
          });
          panelInstance2.result
            .then(function (result) {
              //Deselects the Selected Node in the Flow
              editor.deselectNode();
              ev.stopPropagation();
              ev.preventDefault();
              console.log("panel was closed with result : ", result);
            })
            .catch(function (error) {
              if (error && error === "backdrop click") {
                $route.reload();
              }
              editor.deselectNode();
              ev.stopPropagation();
              ev.preventDefault();
              console.log("panel was rejected with error : ", error);
            });
        });
    };
  },
);

module.controller(
  "ClientCibaTutorialCtrl",
  function ($scope, realm, client) {
    $scope.realm = realm;
    $scope.client = client;
    $scope.providerId = client.authenticationFlowBindingOverrides['ciba_auth'];
    $scope.deliveryMode = client.attributes['ciba.backchannel.token.delivery.mode'];
    $scope.clientId = client.id;
});

module.controller(
  "KcClientCibaTutorialCtrl",
  function ($scope, $http, CibaTutorial, ApiConfig, TestCheck, TestTransaction, TestApi, FetchClientSecret, ResultTransaction, $translate) {
    const realm = $scope.$parent.realm;
    const client = $scope.$parent.client;
    $scope.realm = realm;
    $scope.client = client;
    $scope.docsUrl = docsUrl;
    $scope.providerId = $scope.$parent.authWayId || $scope.$parent.providerId
    $scope.deliveryMode = $scope.$parent.deliveryMode;
    $scope.clientId = $scope.$parent.clientId;
    $scope.resourceUrl = resourceUrl;
    $scope.tutorial = {};
    $scope.selectedFlow = 'onboarding';
    $scope.selectedTab = 'test';
    $scope.selectedVersion = 'v1';
    $scope.tooltipTxt = $translate.instant("copy");
    $scope.testResponse = {};
    $scope.resultResponse = {};
    $scope.loadingTest = false;
    $scope.loadingResult = false;
    $scope.selectedLanguage = 'curl';
    $scope.supportedLanguges = [];
    $scope.pingAuthRequestId = '';
    $scope.isValidResultCurl = false;
    $scope.isValidTransactoiinCurl = false;
    $scope.isTestableCheck = false;
    $scope.canTest = true;
    $scope.isLoading = true;
    $scope.isApiApp = $scope.providerId === 'api-authorizer'
    $scope.apiArray = [];
    $scope.selectedApi = "";
    $scope.nonTestApi = "";
    $scope.selectedApiLanguage = 'curl';
    $scope.apiRequest = "";
    $scope.apiRequestLoading = false;
    $scope.isApiTestBlockVisible = false;
    $scope.apiRequestMetadata = {};
    $scope.apiRequestBodyForCurl = {};
    $scope.query = {};

    if ($scope.providerId === 'api-authorizer') {
        fetchClientSecret();
        fetchApis();
    }

    function fetchClientSecret() {
        let clientId = $scope.client.id || $scope.clientId;
        if ($scope.client.isSandbox) {
            clientId = $scope.client.sandboxClientId;
        }
        FetchClientSecret.query({ realm: $scope.realm.realm, client: clientId }, function(data) {
            console.log(data);
            $scope.apiAuth = btoa(`${clientId}:${data.value}`);
        }, function (err) {
            console.err(err.message);
        })
    }

    function fetchApis() {
        $scope.query.sort = "ASC";
        $scope.query.enabled = "false";
        $scope.query.first = "0";
        $scope.query.last = "100";
        $scope.query.max = "100";
        $scope.verifiers = ApiConfig.query({realm: $scope.realm.id, ...$scope.query}, function(data) {
            $scope.isLoading = false;
            $scope.apiArray = data;
        }, function(err) {
            $scope.isLoading = false;
        });
    }

    $scope.mockCurlTemplate = `curl --location --request POST '{{domain}}/auth/realms/ezto-verify/protocol/openid-connect/transaction/' \
      --header 'Content-Type: application/x-www-form-urlencoded' \
      --header 'api-version: 1' \
      --header 'Authorization: Basic {{token}}' \
      --data-urlencode 'request={
          "requested_verification": "{{enter_verifier}}",
          "metadata": {}
        }'`

    $scope.mockCurlTemplateCopy = angular.copy($scope.mockCurlTemplate)

    curl_converter.then((curlConverterJs) => {
      $scope.curlConverterJs = curlConverterJs;
      $scope.supportedLanguges = $scope.curlConverterJs.supportedLanguges;
    });

    function getMetadataFromVerifier(jsonData, verifierName) {
          const metadata = {};

          function extractVariables(obj) {
            if (Array.isArray(obj)) {
              obj.forEach(item => extractVariables(item));
            } else if (typeof obj === "object" && obj !== null) {
              for (const key in obj) {
                extractVariables(obj[key]);
              }
            } else if (typeof obj === "string" && obj !== '${GenerateUUID}' && obj.startsWith("${") && obj.endsWith("}")) {
              const varName = obj.slice(2, -1);
              metadata[varName] = "";
            }
          }

          Object.values(jsonData).forEach(entry => {
            if (entry.id === verifierName) {
              const requestBody = JSON.parse(entry.requestBody || "{}");
              extractVariables(requestBody);
            }
          });

          return metadata;
        }



    function changeApiRequestCurl () {
      $scope.apiRequestMetadata = getMetadataFromVerifier($scope.apiArray, $scope.selectedApi);
      $scope.verifierName = $scope.apiArray.find(api => api.id === $scope.selectedApi).verifierName;
      $scope.mockCurlTemplateCopy = generateCurl();
      if($scope.selectedApiLanguage == 'curl'){
        $scope.apiRequest = $scope.mockCurlTemplateCopy;
      }else{
        $scope.apiRequest = $scope.curlConverterJs.convertCurlToSnippets($scope.mockCurlTemplateCopy.replaceAll("\n",""), $scope.selectedApiLanguage);
      }
    }

    function generateCurl() {
      const url = authUrl + `/realms/${$scope.realm.realm}/protocol/openid-connect/transaction/`;
      const auth = $scope.apiAuth;
      $scope.nonTestVerifier = $scope.verifierName.includes("ocr")
      const contentType = $scope.nonTestVerifier ? 'multipart/form-data' : 'application/json'
      let bodySection = "";
      if ($scope.nonTestVerifier) {
      const requestPayload = {
        ...$scope.apiRequestBodyForCurl,
        requestedVerification: $scope.verifierName
      };
      bodySection = Object.keys(requestPayload)
        .map(param => {
        if (param === 'file') {
          return `--form '${param}=@"/absolute/path/to/file.jpg"'`; // ⬅️ Prompt user to replace
        } else {
          return `--form '${param}="${requestPayload[param]}"'`;
        }
        }).join(" \\\n");
      } else {
        const jsonBody = JSON.stringify({
          ...$scope.apiRequestBodyForCurl,
          requestedVerification: $scope.verifierName
        }).replace(/'/g, "\\'");
        bodySection = `--data-raw '${jsonBody}'`
      }

      return `curl --location --request POST '${url}' \\
            --header 'Authorization: Basic ${auth}' \\
            --header 'Content-Type: ${contentType}' \\
            --header 'api-version: 1' \\
            --header 'Accept: application/json' \\
            ${bodySection}`;
    }

    $scope.sendApiRequest = function () {
      $scope.apiRequestLoading = true;
      //send api request here and update response in $scope.apiRequest
      const curl = generateCurl();
      TestApi.create({ realm: $scope.realm.realm, client: $scope.client.id }, { curl }, function(data) {
        $scope.apiRequest = JSON.stringify(data, null, 2);
        $scope.apiRequestLoading = false;
      });
    }

    $scope.checkAllMetaDataFilled = function () {
      return Object.values($scope.apiRequestBodyForCurl).every(value => value != null && value != "");
    }

    $scope.showApiTestBlock = function (status) {
      $scope.isApiTestBlockVisible = status;
      if (!status) {
        $scope.selectedApi = ""
      }
    }

    $scope.$watch('selectedApi', function () {
      changeApiRequestCurl();
      $scope.apiRequestBodyForCurl = getMetadataFromVerifier($scope.apiArray, $scope.selectedApi);
    }, true)

    $scope.$watch('apiRequestBodyForCurl', function () {
          changeApiRequestCurl();
        }, true)

    $scope.$watch('selectedApiLanguage', function () {
      changeApiRequestCurl();
      $scope.apiRequestBodyForCurl = getMetadataFromVerifier($scope.apiArray, $scope.selectedApi);
    }, true)

    $scope.$watch('selectedLanguage', function () {
      if (!$scope.tutorial.hasOwnProperty($scope.selectedFlow)) {
        return;
      }
      if($scope.selectedLanguage == 'curl'){
        $scope.transactionBody = $scope.tutorial[$scope.selectedFlow][0].command.data.hasOwnProperty($scope.selectedVersion) ? $scope.tutorial[$scope.selectedFlow][0].command.data[$scope.selectedVersion] : '';
        $scope.resultBody = $scope.tutorial[$scope.selectedFlow][0].command.data.hasOwnProperty($scope.selectedVersion) ? $scope.tutorial[$scope.selectedFlow][2].command.data[$scope.selectedVersion] : '';
      }else{
        $scope.transactionBody = $scope.curlConverterJs.convertCurlToSnippets($scope.tutorial[$scope.selectedFlow][0].command.data[$scope.selectedVersion].replaceAll("\n",""), $scope.selectedLanguage);
        $scope.resultBody = $scope.curlConverterJs.convertCurlToSnippets($scope.tutorial[$scope.selectedFlow][2].command.data[$scope.selectedVersion].replaceAll("\n",""), $scope.selectedLanguage);
      }
    }, true)

    $scope.$watch('selectedVersion', function () {
      if (!$scope.tutorial.hasOwnProperty($scope.selectedFlow)) {
        return;
      }
      if($scope.selectedLanguage == 'curl'){
        $scope.transactionBody = $scope.tutorial[$scope.selectedFlow][0].command.data.hasOwnProperty($scope.selectedVersion) ? $scope.tutorial[$scope.selectedFlow][0].command.data[$scope.selectedVersion] : '';
        $scope.resultBody = $scope.tutorial[$scope.selectedFlow][0].command.data.hasOwnProperty($scope.selectedVersion) ? $scope.tutorial[$scope.selectedFlow][2].command.data[$scope.selectedVersion] : '';
      }else{
        $scope.transactionBody = $scope.curlConverterJs.convertCurlToSnippets($scope.tutorial[$scope.selectedFlow][0].command.data[$scope.selectedVersion].replaceAll("\n",""), $scope.selectedLanguage);
        $scope.resultBody = $scope.curlConverterJs.convertCurlToSnippets($scope.tutorial[$scope.selectedFlow][2].command.data[$scope.selectedVersion].replaceAll("\n",""), $scope.selectedLanguage);
      }
    }, true)

    $scope.$watch('selectedFlow', function () {
      if (!$scope.tutorial.hasOwnProperty($scope.selectedFlow)) {
        return;
      }
      if($scope.selectedLanguage == 'curl'){
        $scope.transactionBody = $scope.tutorial[$scope.selectedFlow][0].command.data.hasOwnProperty($scope.selectedVersion) ? $scope.tutorial[$scope.selectedFlow][0].command.data[$scope.selectedVersion] : '';
        $scope.resultBody = $scope.tutorial[$scope.selectedFlow][0].command.data.hasOwnProperty($scope.selectedVersion) ? $scope.tutorial[$scope.selectedFlow][2].command.data[$scope.selectedVersion] : '';
      }else{
        $scope.transactionBody = $scope.curlConverterJs.convertCurlToSnippets($scope.tutorial[$scope.selectedFlow][0].command.data[$scope.selectedVersion].replaceAll("\n",""), $scope.selectedLanguage);
        $scope.resultBody = $scope.curlConverterJs.convertCurlToSnippets($scope.tutorial[$scope.selectedFlow][2].command.data[$scope.selectedVersion].replaceAll("\n",""), $scope.selectedLanguage);
      }
      $scope.cancelTesting();
    }, true)

    var pollInterval;
    $scope.testTransaction = function () {
      $scope.cancelTesting();
      $scope.loadingTest = true;
      TestTransaction.get({realm: realm.realm, client: $scope.clientId, actionFlow: $scope.selectedFlow}, function (data) {
        $scope.testResponse = data;
        if($scope.testResponse.result_delivery_mode == 'hook@poll'){
          pollInterval = setInterval(() => $scope.resultTransaction(), $scope.testResponse.interval*1000)
        }
        $scope.loadingTest = false;
      }, function (error) {
        Notifications.error("Failed to load Test Response.");
        $scope.loadingResult = false;
      })
    }

    $scope.resultTransaction = function () {
      $scope.loadingResult = true;
      ResultTransaction.send({realm: realm.realm, client: $scope.clientId, actionFlow: $scope.selectedFlow}, { authRequestId: $scope.testResponse.result_delivery_mode == 'hook@ping'  ? $scope.pingAuthRequestId : $scope.testResponse.auth_req_id }, function (data) {
        $scope.loadingResult = false;
        if(data.error && $scope.testResponse.result_delivery_mode == 'hook@poll'){
          if(data.error != "verification_pending" && data.error != "slow_down"){
            clearInterval(pollInterval);
            $scope.resultResponse = data;
            $scope.stringifiedResult = JSON.stringify($scope.resultResponse, null, '\t');
          }
        }else{
          clearInterval(pollInterval);
          $scope.resultResponse = data;
          $scope.stringifiedResult = JSON.stringify($scope.resultResponse, null, '\t');
        }
      }, function (error) {
        Notifications.error("Failed to load Test Response.");
        $scope.loadingResult = false;
      })
    }

    $scope.resetCopy = function () {
      $scope.tooltipTxt = $translate.instant('copy');
    };

    $scope.cancelTesting = function () {
      $scope.testResponse = {};
      $scope.resultResponse = {};
      clearInterval(pollInterval);
      $scope.pingAuthRequestId = '';
      $scope.loadingTest = false;
      $scope.loadingResult = false;
    }

    $scope.setSelectedTab = function (tab) {
      $scope.selectedTab = tab;
      if($scope.selectedTab == 'integration'){
        $scope.cancelTesting();
      }
    }

    $scope.copyToClipboard = function (command) {
      var $temp = $("<textarea>");
      $("body").append($temp);
      var text;
      if (typeof command == 'object') {
        text = JSON.stringify(command);
      } else {
        text = command;
      }
      $temp.val(text.trim()).select();
      document.execCommand("copy");
      $temp.remove();
      $scope.tooltipTxt = $translate.instant("copied");
      $(".copy-code-icon").mouseleave(function () {
        $scope.tooltipTxt = $translate.instant("copy");
      });
    };

    TestCheck.get({ realm: realm.realm, client: $scope.clientId}, function (data) {
      $scope.canTest = data.test_possible;
      $scope.isTestPossible = data.flow_possible;
      $scope.isTestableCheck = true;
    });

    CibaTutorial.get(
      { realm: realm.realm, client: $scope.clientId },
      function (data) {
        $scope.tutorial = data;
        $scope.isVersionSelectorVisible = $scope.transactionBody = $scope.tutorial[$scope.selectedFlow][0].command.data.hasOwnProperty('v0');
        $scope.transactionBody = $scope.tutorial[$scope.selectedFlow][0].command.data.hasOwnProperty($scope.selectedVersion) ? $scope.tutorial[$scope.selectedFlow][0].command.data[$scope.selectedVersion] : '';
        $scope.isValidTransactionCurl = $scope.curlConverterJs.isValidCurl($scope.transactionBody);
        $scope.resultBody = $scope.tutorial[$scope.selectedFlow][0].command.data.hasOwnProperty($scope.selectedVersion) ? $scope.tutorial[$scope.selectedFlow][2].command.data[$scope.selectedVersion] : '';
        $scope.isValidResultCurl = $scope.curlConverterJs.isValidCurl($scope.resultBody);
      },
      function (error) {
        Notifications.error("Failed to load CIBA tutorial.");
      },
    );

    $scope.$on("$routeChangeStart", function () {
      clearInterval(pollInterval);
    });

  },
);

module.controller(
  "ClientScopeMappingCtrl",
  function (
    $scope,
    $http,
    realm,
    $route,
    client,
    clients,
    Notifications,
    Client,
    ClientScope,
    ClientRealmScopeMapping,
    ClientClientScopeMapping,
    ClientRole,
    ClientAvailableRealmScopeMapping,
    ClientAvailableClientScopeMapping,
    ClientCompositeRealmScopeMapping,
    ClientCompositeClientScopeMapping,
    $translate,
  ) {
    $scope.realm = realm;
    $scope.client = angular.copy(client);
    $scope.selectedRealmRoles = [];
    $scope.selectedRealmMappings = [];
    $scope.realmMappings = [];
    $scope.clients = clients;
    $scope.clientRoles = [];
    $scope.clientComposite = [];
    $scope.selectedClientRoles = [];
    $scope.selectedClientMappings = [];
    $scope.clientMappings = [];
    $scope.dummymodel = [];

    $scope.hideRoleSelector = function () {
      return $scope.client.fullScopeAllowed;
    };

    $scope.changeFlag = function () {
      Client.update(
        {
          realm: realm.realm,
          client: client.id,
        },
        $scope.client,
        function () {
          $scope.changed = false;
          client = angular.copy($scope.client);
          updateRealmRoles();
          Notifications.success($translate.instant("scope-mappings-updated"));
        },
      );
    };

    $scope.selectedClient = null;

    $scope.selectClient = function (client) {
      if (!client || !client.id) {
        $scope.selectedClient = null;
        return;
      }

      $scope.selectedClient = client;
      updateClientRoles();
    };

    function updateRealmRoles() {
      $scope.realmRoles = ClientAvailableRealmScopeMapping.query({
        realm: realm.realm,
        client: client.id,
      });
      $scope.realmMappings = ClientRealmScopeMapping.query({
        realm: realm.realm,
        client: client.id,
      });
      $scope.realmComposite = ClientCompositeRealmScopeMapping.query({
        realm: realm.realm,
        client: client.id,
      });
    }

    function updateClientRoles() {
      if ($scope.selectedClient) {
        $scope.clientRoles = ClientAvailableClientScopeMapping.query({
          realm: realm.realm,
          client: client.id,
          targetClient: $scope.selectedClient.id,
        });
        $scope.clientMappings = ClientClientScopeMapping.query({
          realm: realm.realm,
          client: client.id,
          targetClient: $scope.selectedClient.id,
        });
        $scope.clientComposite = ClientCompositeClientScopeMapping.query({
          realm: realm.realm,
          client: client.id,
          targetClient: $scope.selectedClient.id,
        });
      } else {
        $scope.clientRoles = null;
        $scope.clientMappings = null;
        $scope.clientComposite = null;
      }
    }

    $scope.addRealmRole = function () {
      $scope.selectedRealmRolesToAdd = JSON.parse(
        "[" + $scope.selectedRealmRoles + "]",
      );
      $scope.selectedRealmRoles = [];
      $http
        .post(
          authUrl +
          "/admin/realms/" +
          realm.realm +
          "/clients/" +
          client.id +
          "/scope-mappings/realm",
          $scope.selectedRealmRolesToAdd,
        )
        .then(function () {
          updateRealmRoles();
          $scope.selectedRealmRolesToAdd = [];
          Notifications.success($translate.instant("scope-mappings-updated"));
        });
    };

    $scope.deleteRealmRole = function () {
      $scope.selectedRealmMappingsToRemove = JSON.parse(
        "[" + $scope.selectedRealmMappings + "]",
      );
      $scope.selectedRealmMappings = [];
      $http
        .delete(
          authUrl +
          "/admin/realms/" +
          realm.realm +
          "/clients/" +
          client.id +
          "/scope-mappings/realm",
          {
            data: $scope.selectedRealmMappingsToRemove,
            headers: { "content-type": "application/json" },
          },
        )
        .then(function () {
          updateRealmRoles();
          $scope.selectedRealmMappingsToRemove = [];
          Notifications.success($translate.instant("scope-mappings-updated"));
        });
    };

    $scope.addClientRole = function () {
      $scope.selectedClientRolesToAdd = JSON.parse(
        "[" + $scope.selectedClientRoles + "]",
      );
      $scope.selectedClientRoles = [];
      $http
        .post(
          authUrl +
          "/admin/realms/" +
          realm.realm +
          "/clients/" +
          client.id +
          "/scope-mappings/clients/" +
          $scope.selectedClient.id,
          $scope.selectedClientRolesToAdd,
        )
        .then(function () {
          updateClientRoles();
          $scope.selectedClientRolesToAdd = [];
          Notifications.success($translate.instant("scope-mappings-updated"));
        });
    };

    $scope.deleteClientRole = function () {
      $scope.selectedClientMappingsToRemove = JSON.parse(
        "[" + $scope.selectedClientMappings + "]",
      );
      $scope.selectedClientMappings = [];
      $http
        .delete(
          authUrl +
          "/admin/realms/" +
          realm.realm +
          "/clients/" +
          client.id +
          "/scope-mappings/clients/" +
          $scope.selectedClient.id,
          {
            data: $scope.selectedClientMappingsToRemove,
            headers: { "content-type": "application/json" },
          },
        )
        .then(function () {
          updateClientRoles();
          $scope.selectedClientMappingsToRemove = [];
          Notifications.success($translate.instant("scope-mappings-updated"));
        });
    };

    clientSelectControl($scope, $route.current.params.realm, Client);
    updateRealmRoles();
  },
);

module.controller(
  "ClientRevocationCtrl",
  function (
    $scope,
    realm,
    client,
    Client,
    ClientPushRevocation,
    $location,
    Dialog,
    Notifications,
    $translate,
  ) {
    $scope.realm = realm;
    $scope.client = client;

    var setNotBefore = function () {
      if ($scope.client.notBefore == 0) {
        $scope.notBefore = "None";
      } else {
        $scope.notBefore = new Date($scope.client.notBefore * 1000);
      }
    };

    setNotBefore();

    var refresh = function () {
      Client.get(
        { realm: realm.realm, client: $scope.client.id },
        function (updated) {
          $scope.client = updated;
          setNotBefore();
        },
      );
    };

    $scope.clear = function () {
      $scope.client.notBefore = 0;
      Client.update(
        { realm: realm.realm, client: client.id },
        $scope.client,
        function () {
          $scope.notBefore = "None";
          Notifications.success($translate.instant("not-before-cleared"));
          refresh();
        },
      );
    };
    $scope.setNotBeforeNow = function () {
      $scope.client.notBefore = new Date().getTime() / 1000;
      Client.update(
        { realm: realm.realm, client: $scope.client.id },
        $scope.client,
        function () {
          Notifications.success(
            $translate.instant("not-before-set-for-client"),
          );
          refresh();
        },
      );
    };
    $scope.pushRevocation = function () {
      ClientPushRevocation.save(
        { realm: realm.realm, client: $scope.client.id },
        function (globalReqResult) {
          var successCount = globalReqResult.successRequests
            ? globalReqResult.successRequests.length
            : 0;
          var failedCount = globalReqResult.failedRequests
            ? globalReqResult.failedRequests.length
            : 0;

          if (successCount == 0 && failedCount == 0) {
            Notifications.warn($translate.instant("push-not-sent"));
          } else if (failedCount > 0) {
            var msgStart =
              successCount > 0
                ? "Successfully push notBefore to: " +
                globalReqResult.successRequests +
                " . "
                : "";
            Notifications.error(
              msgStart +
              "Failed to push notBefore to: " +
              globalReqResult.failedRequests +
              ". Verify availability of failed hosts and try again",
            );
          } else {
            Notifications.success(
              $translate.instant("successfull-push-not-before") +
              " " +
              globalReqResult.successRequests,
            );
          }
        },
      );
    };
  },
);

module.controller(
  "ClientClusteringCtrl",
  function (
    $scope,
    client,
    Client,
    ClientTestNodesAvailable,
    ClientClusterNode,
    realm,
    $location,
    $route,
    Dialog,
    Notifications,
    TimeUnit,
    $translate,
  ) {
    $scope.client = client;
    $scope.realm = realm;

    var oldCopy = angular.copy($scope.client);
    $scope.changed = false;

    $scope.$watch(
      "client",
      function () {
        if (!angular.equals($scope.client, oldCopy)) {
          $scope.changed = true;
        }
      },
      true,
    );

    $scope.client.nodeReRegistrationTimeoutUnit = TimeUnit.autoUnit(
      client.nodeReRegistrationTimeout,
    );
    $scope.client.nodeReRegistrationTimeout = TimeUnit.toUnit(
      client.nodeReRegistrationTimeout,
      $scope.client.nodeReRegistrationTimeoutUnit,
    );

    $scope.save = function () {
      var clientCopy = angular.copy($scope.client);
      delete clientCopy["nodeReRegistrationTimeoutUnit"];
      clientCopy.nodeReRegistrationTimeout = TimeUnit.toSeconds(
        $scope.client.nodeReRegistrationTimeout,
        $scope.client.nodeReRegistrationTimeoutUnit,
      );
      Client.update(
        { realm: realm.realm, client: client.id },
        clientCopy,
        function () {
          $route.reload();
          Notifications.success(
            $translate.instant("changes-saved-to-application"),
          );
        },
      );
    };

    $scope.reset = function () {
      $route.reload();
    };

    $scope.testNodesAvailable = function () {
      ClientTestNodesAvailable.get(
        { realm: realm.realm, client: client.id },
        function (globalReqResult) {
          $route.reload();

          var successCount = globalReqResult.successRequests
            ? globalReqResult.successRequests.length
            : 0;
          var failedCount = globalReqResult.failedRequests
            ? globalReqResult.failedRequests.length
            : 0;

          if (successCount == 0 && failedCount == 0) {
            Notifications.warn($translate.instant("request-not-sent"));
          } else if (failedCount > 0) {
            var msgStart =
              successCount > 0
                ? "Successfully verify availability for " +
                globalReqResult.successRequests +
                " . "
                : "";
            Notifications.error(
              msgStart +
              "Failed to verify availability for: " +
              globalReqResult.failedRequests +
              ". Fix or unregister failed cluster nodes and try again",
            );
          } else {
            Notifications.success(
              $translate.instant("successfully-sent-request") +
              globalReqResult.successRequests,
            );
          }
        },
      );
    };

    if (client.registeredNodes) {
      var nodeRegistrations = [];
      for (node in client.registeredNodes) {
        reg = {
          host: node,
          lastRegistration: new Date(client.registeredNodes[node] * 1000),
        };
        nodeRegistrations.push(reg);
      }

      $scope.nodeRegistrations = nodeRegistrations;
    }

    $scope.removeNode = function (node) {
      Dialog.confirmDelete(node.host, "node", function () {
        ClientClusterNode.remove(
          { realm: realm.realm, client: client.id, node: node.host },
          function () {
            Notifications.success(
              "Node " + node.host + " unregistered successfully.",
            );
            $route.reload();
          },
        );
      });
    };
  },
);

module.controller(
  "ClientClusteringNodeCtrl",
  function (
    $scope,
    client,
    Client,
    ClientClusterNode,
    realm,
    $location,
    $routeParams,
    Notifications,
    Dialog,
  ) {
    $scope.client = client;
    $scope.realm = realm;
    $scope.create = !$routeParams.node;

    $scope.save = function () {
      ClientClusterNode.save(
        { realm: realm.realm, client: client.id, node: $scope.node.host },
        function () {
          Notifications.success(
            "Node " + $scope.node.host + " registered successfully.",
          );
          $location.url(
            "/realms/" + realm.realm + "/clients/" + client.id + "/clustering",
          );
        },
      );
    };

    $scope.unregisterNode = function () {
      Dialog.confirmDelete($scope.node.host, "node", function () {
        ClientClusterNode.remove(
          { realm: realm.realm, client: client.id, node: $scope.node.host },
          function () {
            Notifications.success(
              "Node " + $scope.node.host + " unregistered successfully.",
            );
            $location.url(
              "/realms/" +
              realm.realm +
              "/clients/" +
              client.id +
              "/clustering",
            );
          },
        );
      });
    };

    if ($scope.create) {
      $scope.node = {};
      $scope.registered = false;
    } else {
      var lastRegTime = client.registeredNodes[$routeParams.node];

      if (lastRegTime) {
        $scope.registered = true;
        $scope.node = {
          host: $routeParams.node,
          lastRegistration: new Date(lastRegTime * 1000),
        };
      } else {
        $scope.registered = false;
        $scope.node = {
          host: $routeParams.node,
        };
      }
    }
  },
);

module.controller(
  "AddBuiltinProtocolMapperCtrl",
  function (
    $scope,
    realm,
    client,
    serverInfo,
    ClientProtocolMappersByProtocol,
    $http,
    $location,
    Dialog,
    Notifications,
    ConfirmExitDialog,
    $translate,
  ) {
    $scope.realm = realm;
    $scope.client = client;
    if (client.protocol == null) {
      client.protocol = "openid-connect";
    }

    var protocolMappers = serverInfo.protocolMapperTypes[client.protocol];
    var mapperTypes = {};
    for (var i = 0; i < protocolMappers.length; i++) {
      mapperTypes[protocolMappers[i].id] = protocolMappers[i];
    }
    $scope.mapperTypes = mapperTypes;
    $scope.fragment = $location.path();
    $scope.path = $location.path().substring(1).split("/");

    var updateMappers = function () {
      var clientMappers = ClientProtocolMappersByProtocol.query(
        { realm: realm.realm, client: client.id, protocol: client.protocol },
        function () {
          var builtinMappers =
            serverInfo.builtinProtocolMappers[client.protocol];
          for (var i = 0; i < clientMappers.length; i++) {
            for (var j = 0; j < builtinMappers.length; j++) {
              if (
                builtinMappers[j].name == clientMappers[i].name &&
                builtinMappers[j].protocolMapper ==
                clientMappers[i].protocolMapper
              ) {
                builtinMappers.splice(j, 1);
                break;
              }
            }
          }
          $scope.mappers = builtinMappers;
          for (var i = 0; i < $scope.mappers.length; i++) {
            $scope.mappers[i].isChecked = false;
          }
        },
      );
    };
    updateMappers();
    $scope.enableBtn = false;
    $scope.$watch(function () {
      for (var i = 0; i < $scope.mappers.length; i++) {
        if ($scope.mappers[i].isChecked == true) {
          $scope.enableBtn = true;
          break;
        } else {
          $scope.enableBtn = false;
        }
      }
    }, true);

    $scope.openRedirect = false;
    $scope.add = function () {
      var toAdd = [];
      for (var i = 0; i < $scope.mappers.length; i++) {
        if ($scope.mappers[i].isChecked) {
          delete $scope.mappers[i].isChecked;
          toAdd.push($scope.mappers[i]);
        }
      }
      $http
        .post(
          authUrl +
          "/admin/realms/" +
          realm.realm +
          "/clients/" +
          client.id +
          "/protocol-mappers/add-models",
          toAdd,
        )
        .then(function () {
          Notifications.success($translate.instant("mappers-added"));
          $scope.openRedirect = true;
          $location.url(
            "/realms/" +
            realm.realm +
            "/" +
            $scope.path[2] +
            "/" +
            client.id +
            "/mappers",
          );
        })
        .catch(function () {
          Notifications.error("Error adding mappers");
          $scope.openRedirect = true;
          $location.url(
            "/realms/" +
            realm.realm +
            "/" +
            $scope.path[2] +
            "/" +
            client.id +
            "/mappers",
          );
        });
    };
    $scope.changeSearch = function () {
      $scope.search = $scope.searchValue;
    };
    $scope.clearSearch = function () {
      $scope.search = "";
      $scope.searchValue = "";
      updateMappers();
    };

    $scope.$on("$routeChangeStart", function (event, newUrl) {
      if (!$scope.openRedirect && $scope.enableBtn) {
        showConfirmExitDialog(
          event,
          ConfirmExitDialog,
          $scope,
          $location,
          newUrl,
        );
      }
    });
  },
);

module.controller(
  "ClientProtocolMapperListCtrl",
  function (
    $scope,
    realm,
    client,
    serverInfo,
    Client,
    ClientProtocolMappersByProtocol,
    ClientProtocolMapper,
    $route,
    Dialog,
    Notifications,
    $location,
    $translate,
  ) {
    $scope.realm = realm;
    $scope.client = client;
    $scope.loaderTypeName = "tableListLoader";
    $scope.isInBuilt = client.attributes.isInBuilt === 'true';
    if ($scope.client.attributes["isInBuilt"]) {
      if ($scope.client.attributes["isInBuilt"] == "true" && $scope.client.appType !=='saml') {
        $location.path("/notfound");
      }
    }
    if (client.protocol == null) {
      client.protocol = "openid-connect";
    }
    $scope.editTxt = $translate.instant("edit");
    $scope.deleteTxt = "Remove";

    $scope.changeSearch = function () {
      $scope.search = $scope.searchValue;
    };
    $scope.changeFlag = function () {
      Client.update(
        {
          realm: realm.realm,
          client: client.id,
        },
        $scope.client,
        function () {
          $scope.changed = false;
          client = angular.copy($scope.client);
          Notifications.success($translate.instant("client-updated"));
        },
      );
    };

    var protocolMappers = serverInfo.protocolMapperTypes[client.protocol];
    var mapperTypes = {};
    for (var i = 0; i < protocolMappers.length; i++) {
      mapperTypes[protocolMappers[i].id] = protocolMappers[i];
    }
    $scope.mapperTypes = mapperTypes;

    $scope.removeMapper = function (mapper) {
      console.debug(mapper);
      Dialog.confirmDelete(mapper.name, "mapper", function () {
        ClientProtocolMapper.remove(
          { realm: realm.realm, client: client.id, id: mapper.id },
          function () {
            Notifications.success($translate.instant("mapper-deleted"));
            $route.reload();
          },
        );
      });
    };

    $scope.sortMappersByPriority = function (mapper) {
      return $scope.mapperTypes[mapper.protocolMapper].priority;
    };

    var updateMappers = function () {
      $scope.updateFinished = false;
      $scope.mappers = ClientProtocolMappersByProtocol.query(
        { realm: realm.realm, client: client.id, protocol: client.protocol },
        function () {
          $scope.updateFinished = true;
        },
      );
    };
    $scope.clearSearch = function () {
      $scope.search = "";
      $scope.searchValue = "";
      updateMappers();
    };

    updateMappers();
  },
);

module.controller(
  "ClientProtocolMapperCtrl",
  function (
    $scope,
    realm,
    serverInfo,
    client,
    clients,
    mapper,
    ClientProtocolMapper,
    Notifications,
    Dialog,
    $location,
    $translate,
  ) {
    $scope.realm = realm;
    $scope.clients = clients;
    $scope.updateStage = 1;
    $scope.fragment = $location.path();
    $scope.path = $location.path().substring(1).split("/");

    /*
    $scope.client = client;
    $scope.create = false;
    $scope.protocol = client.protocol;
    $scope.mapper = angular.copy(mapper);
    $scope.changed = false;
    */

    if (client.protocol == null) {
      client.protocol = "openid-connect";
    }

    $scope.model = {
      realm: realm,
      client: client,
      create: false,
      protocol: client.protocol,
      mapper: angular.copy(mapper),
      changed: false,
    };

    var protocolMappers = serverInfo.protocolMapperTypes[client.protocol];
    for (var i = 0; i < protocolMappers.length; i++) {
      if (protocolMappers[i].id === mapper.protocolMapper) {
        $scope.model.mapperType = protocolMappers[i];
      }
    }

    $scope.model.mapperType.properties.forEach((data) => {
      if (
        !mapper.config.hasOwnProperty(data.name) ||
        mapper?.config?.[data.name] === null
      ) {
        mapper.config[data.name] = "";
      }
    });

    $scope.$watch(
      "model.mapper",
      function () {
        if (!angular.equals($scope.model.mapper, mapper)) {
          $scope.model.changed = true;
        } else {
          $scope.model.changed = false;
        }
      },
      true,
    );

    $scope.save = function () {
      $scope.updateStage = 2;
      $("#updateCancelBtn").attr("disabled", "disabled");
      ClientProtocolMapper.update(
        {
          realm: realm.realm,
          client: client.id,
          id: $scope.model.mapper.id,
        },
        $scope.model.mapper,
        function () {
          $scope.updateStage = 1;
          $("#updateCancelBtn").removeAttr("disabled", "disabled");
          $scope.model.changed = false;
          mapper = angular.copy($scope.mapper);
          $location.url(
            "/realms/" +
            realm.realm +
            "/" +
            $scope.path[2] +
            "/" +
            client.id +
            "/mappers/" +
            $scope.model.mapper.id,
          );
          Notifications.success($translate.instant("changesSavedSuccess"));
        },
        function () {
          $scope.updateStage = 1;
          $("#updateCancelBtn").removeAttr("disabled", "disabled");
        },
      );
    };

    $scope.reset = function () {
      $scope.model.mapper = angular.copy(mapper);
      $scope.model.changed = false;
    };

    $scope.cancel = function () {
      //$location.url("/realms");
      window.history.back();
    };

    $scope.remove = function () {
      Dialog.confirmDelete($scope.model.mapper.name, "mapper", function () {
        ClientProtocolMapper.remove(
          { realm: realm.realm, client: client.id, id: $scope.model.mapper.id },
          function () {
            Notifications.success($translate.instant("mapper-deleted"));
            $location.url(
              "/realms/" + realm.realm + "/clients/" + client.id + "/mappers",
            );
          },
        );
      });
    };
  },
);

module.controller(
  "ClientProtocolMapperCreateCtrl",
  function (
    $scope,
    realm,
    serverInfo,
    client,
    clients,
    ClientProtocolMapper,
    Notifications,
    Dialog,
    $location,
    ConfirmExitDialog,
    $translate,
  ) {
    $scope.realm = realm;
    $scope.clients = clients;
    $scope.saveStage = 1;

    if (client.protocol == null) {
      client.protocol = "openid-connect";
    }
    var protocol = client.protocol;
    $scope.fragment = $location.path();
    $scope.path = $location.path().substring(1).split("/");
    /*
    $scope.client = client;
    $scope.create = true;
    $scope.protocol = protocol;
    $scope.mapper = { protocol :  client.protocol, config: {}};
    $scope.mapperTypes = serverInfo.protocolMapperTypes[protocol];
    */
    $scope.model = {
      realm: realm,
      client: client,
      create: true,
      protocol: client.protocol,
      mapper: { protocol: client.protocol, config: {} },
      changed: false,
      mapperTypes: serverInfo.protocolMapperTypes[protocol],
    };

    // apply default configurations on change for selected protocolmapper type.
    $scope.$watch(
      "model.mapperType",
      function () {
        var currentMapperType = $scope.model.mapperType;
        var defaultConfig = {};

        if (currentMapperType && Array.isArray(currentMapperType.properties)) {
          for (var i = 0; i < currentMapperType.properties.length; i++) {
            var property = currentMapperType.properties[i];
            if (property && property.name && property.defaultValue) {
              defaultConfig[property.name] = property.defaultValue;
            }
          }
        }

        $scope.model.mapper.config = defaultConfig;
      },
      true,
    );

    $scope.model.mapperType = $scope.model.mapperTypes[0];

    $scope.enableBtn = true;
    $scope.$watch(
      "model",
      function () {
        if (!$("#nameMapper").val()) {
          $scope.enableBtn = false;
        }
        if ($("#nameMapper").val()) {
          $scope.enableBtn = true;
        }
      },
      true,
    );

    $scope.openRedirect = false;
    $scope.save = function () {
      $scope.saveStage = 2;
      $("#saveCancelBtn").attr("disabled", "disabled");
      $scope.model.mapper.protocolMapper = $scope.model.mapperType.id;
      ClientProtocolMapper.save(
        {
          realm: realm.realm,
          client: client.id,
        },
        $scope.model.mapper,
        function (data, headers) {
          $scope.saveStage = 1;
          $("#saveCancelBtn").removeAttr("disabled", "disabled");
          var l = headers().location;
          var id = l.substring(l.lastIndexOf("/") + 1);
          $scope.openRedirect = true;
          $location.url(
            "/realms/" +
            realm.realm +
            "/" +
            $scope.path[1] +
            "/" +
            client.id +
            "/mappers/" +
            id,
          );
          Notifications.success($translate.instant("mapper-created"));
        },
        function () {
          $scope.saveStage = 1;
          $("#saveCancelBtn").removeAttr("disabled", "disabled");
        },
      );
    };

    $scope.cancel = function () {
      window.history.back();
    };

    $scope.$on("$routeChangeStart", function (event, newUrl) {
      if (!$scope.openRedirect && $scope.enableBtn) {
        showConfirmExitDialog(
          event,
          ConfirmExitDialog,
          $scope,
          $location,
          newUrl,
        );
      }
    });
  },
);

module.controller(
  "ClientClientScopesSetupCtrl",
  function (
    $scope,
    realm,
    Realm,
    client,
    clientScopes,
    serverInfo,
    clientDefaultClientScopes,
    ClientDefaultClientScopes,
    clientOptionalClientScopes,
    ClientOptionalClientScopes,
    $route,
    Notifications,
    $location,
    $translate,
  ) {
    $scope.realm = realm;
    $scope.client = client;
    $scope.isInBuilt = client.attributes.isInBuilt === 'true';
    if ($scope.client.attributes["isInBuilt"]) {
      if ($scope.client.attributes["isInBuilt"] == "true" && $scope.client.appType !=='saml') {
        $location.path("/notfound");
      }
    }

    $scope.clientDefaultClientScopes = clientDefaultClientScopes;
    $scope.clientOptionalClientScopes = clientOptionalClientScopes;

    $scope.availableClientScopes = [];
    $scope.selectedDefaultClientScopes = [];
    $scope.selectedDefDefaultClientScopes = [];

    $scope.selectedOptionalClientScopes = [];
    $scope.selectedDefOptionalClientScopes = [];

    var availableDefaultClientScope = $("#available-default-client-scopes")[0];
    var assignedDefaultClientScope = $("#assigned-default-client-scopes")[0];
    availableDefaultClientScope.addEventListener(
      "focus",
      removeAssignedFocus,
      true,
    );
    assignedDefaultClientScope.addEventListener(
      "focus",
      removeAvailableFocus,
      true,
    );
    function removeAssignedFocus() {
      $("#add-default-client-scopes").removeAttr("disabled", "disabled");
      $("#remove-default-client-scopes").attr("disabled", "disabled");
    }
    function removeAvailableFocus() {
      $("#remove-default-client-scopes").removeAttr("disabled", "disabled");
      $("#add-default-client-scopes").attr("disabled", "disabled");
    }

    // Populate available client scopes. Available client scopes are neither already assigned to 'default' or 'optional'
    for (var i = 0; i < clientScopes.length; i++) {
      var clientScope = clientScopes[i];
      var scopeName = clientScopes[i].name;

      var available = true;
      if (clientScope.protocol != client.protocol) {
        available = false;
      }

      for (var j = 0; j < $scope.clientDefaultClientScopes.length; j++) {
        if (scopeName === $scope.clientDefaultClientScopes[j].name) {
          available = false;
        }
      }
      for (var j = 0; j < $scope.clientOptionalClientScopes.length; j++) {
        if (scopeName === $scope.clientOptionalClientScopes[j].name) {
          available = false;
        }
      }

      if (available) {
        $scope.availableClientScopes.push(clientScope);
      }
    }

    $scope.addDefaultClientScope = function () {
      $scope.selectedDefaultClientScopesToAdd = JSON.parse(
        "[" + $scope.selectedDefaultClientScopes + "]",
      );
      toAdd = $scope.selectedDefaultClientScopesToAdd.length;

      for (var i = 0; i < $scope.selectedDefaultClientScopesToAdd.length; i++) {
        var currentScope = $scope.selectedDefaultClientScopesToAdd[i];

        ClientDefaultClientScopes.update(
          {
            realm: realm.realm,
            client: client.id,
            clientScopeId: currentScope.id,
          },
          function () {
            toAdd = toAdd - 1;
            if (toAdd === 0) {
              $route.reload();
              Notifications.success(
                $translate.instant("default-scopes-updated"),
              );
            }
          },
        );
      }
      $scope.selectedDefaultClientScopesToAdd = [];
    };
    var availableOptionalClientScope = $(
      "#available-optional-client-scopes",
    )[0];
    var assignedOptionalClientScope = $("#assigned-optional-client-scopes")[0];
    availableOptionalClientScope.addEventListener(
      "focus",
      removeoptionalAssignedFocus,
      true,
    );
    assignedOptionalClientScope.addEventListener(
      "focus",
      removeoptionalAvailableFocus,
      true,
    );
    function removeoptionalAssignedFocus() {
      $("#add-optional-client-scopes").removeAttr("disabled", "disabled");
      $("#remove-optional-client-scopes").attr("disabled", "disabled");
    }
    function removeoptionalAvailableFocus() {
      $("#remove-optional-client-scopes").removeAttr("disabled", "disabled");
      $("#add-optional-client-scopes").attr("disabled", "disabled");
    }

    $scope.deleteDefaultClientScope = function () {
      $scope.selectedDefDefaultClientScopesToRemove = JSON.parse(
        "[" + $scope.selectedDefDefaultClientScopes + "]",
      );
      toRemove = $scope.selectedDefDefaultClientScopesToRemove.length;

      for (
        var i = 0;
        i < $scope.selectedDefDefaultClientScopesToRemove.length;
        i++
      ) {
        var currentScope = $scope.selectedDefDefaultClientScopesToRemove[i];

        ClientDefaultClientScopes.remove(
          {
            realm: realm.realm,
            client: client.id,
            clientScopeId: currentScope.id,
          },
          function () {
            toRemove = toRemove - 1;
            if (toRemove === 0) {
              $route.reload();
              Notifications.success(
                $translate.instant("default-scopes-updated"),
              );
            }
          },
        );
      }
      $scope.selectedDefDefaultClientScopesToRemove = [];
    };

    $scope.addOptionalClientScope = function () {
      $scope.selectedOptionalClientScopesToAdd = JSON.parse(
        "[" + $scope.selectedOptionalClientScopes + "]",
      );
      toAdd = $scope.selectedOptionalClientScopesToAdd.length;

      for (
        var i = 0;
        i < $scope.selectedOptionalClientScopesToAdd.length;
        i++
      ) {
        var currentScope = $scope.selectedOptionalClientScopesToAdd[i];

        ClientOptionalClientScopes.update(
          {
            realm: realm.realm,
            client: client.id,
            clientScopeId: currentScope.id,
          },
          function () {
            toAdd = toAdd - 1;
            if (toAdd === 0) {
              $route.reload();
              Notifications.success(
                $translate.instant("optional-scopes-updated"),
              );
            }
          },
        );
      }
    };

    $scope.deleteOptionalClientScope = function () {
      $scope.selectedDefOptionalClientScopesToRemove = JSON.parse(
        "[" + $scope.selectedDefOptionalClientScopes + "]",
      );
      toRemove = $scope.selectedDefOptionalClientScopesToRemove.length;

      for (
        var i = 0;
        i < $scope.selectedDefOptionalClientScopesToRemove.length;
        i++
      ) {
        var currentScope = $scope.selectedDefOptionalClientScopesToRemove[i];

        ClientOptionalClientScopes.remove(
          {
            realm: realm.realm,
            client: client.id,
            clientScopeId: currentScope.id,
          },
          function () {
            toRemove = toRemove - 1;
            if (toRemove === 0) {
              $route.reload();
              Notifications.success(
                $translate.instant("optional-scopes-updated"),
              );
            }
          },
        );
      }
      $scope.selectedDefOptionalClientScopesToRemove = [];
    };
  },
);

module.controller(
  "ClientClientScopesEvaluateCtrl",
  function (
    $scope,
    Realm,
    User,
    ClientEvaluateProtocolMappers,
    ClientEvaluateGrantedRoles,
    ClientEvaluateNotGrantedRoles,
    ClientEvaluateGenerateExampleToken,
    realm,
    client,
    clients,
    clientScopes,
    serverInfo,
    ComponentUtils,
    clientOptionalClientScopes,
    clientDefaultClientScopes,
    $route,
    $routeParams,
    $http,
    Notifications,
    $location,
    Client,
  ) {
    var protocolMappers = serverInfo.protocolMapperTypes[client.protocol];
    var mapperTypes = {};
    for (var i = 0; i < protocolMappers.length; i++) {
      mapperTypes[protocolMappers[i].id] = protocolMappers[i];
    }
    $scope.mapperTypes = mapperTypes;

    $scope.realm = realm;
    $scope.client = client;
    $scope.clients = clients;
    $scope.userId = null;

    $scope.availableClientScopes = [];
    $scope.assignedClientScopes = [];
    $scope.selectedClientScopes = [];
    $scope.selectedDefClientScopes = [];
    $scope.effectiveClientScopes = [];

    // Populate available client scopes. Available client scopes are neither already assigned to 'default' or 'optional'
    for (var i = 0; i < clientOptionalClientScopes.length; i++) {
      $scope.availableClientScopes.push(clientOptionalClientScopes[i]);
    }

    function clearEvalResponse() {
      $scope.protocolMappers = null;
      $scope.grantedRealmRoles = null;
      $scope.notGrantedRealmRoles = null;
      $scope.grantedClientRoles = null;
      $scope.notGrantedClientRoles = null;
      $scope.targetClient = null;
      $scope.oidcAccessToken = null;

      $scope.selectedTab = 0;
    }

    function updateState() {
      // Compute scope parameter
      $scope.scopeParam = "openid";
      for (var i = 0; i < $scope.assignedClientScopes.length; i++) {
        var currentScopeParam = $scope.assignedClientScopes[i].name;
        $scope.scopeParam = $scope.scopeParam + " " + currentScopeParam;
      }

      // Compute effective scopes
      $scope.effectiveClientScopes = [];

      for (var i = 0; i < clientDefaultClientScopes.length; i++) {
        var currentScope = clientDefaultClientScopes[i];
        $scope.effectiveClientScopes.push(currentScope);
      }
      for (var i = 0; i < $scope.assignedClientScopes.length; i++) {
        var currentScope = $scope.assignedClientScopes[i];
        $scope.effectiveClientScopes.push(currentScope);
      }

      // Clear the evaluation response
      clearEvalResponse();
    }

    updateState();

    $scope.addAppliedClientScope = function () {
      $scope.selectedClientScopesToAdd = JSON.parse(
        "[" + $scope.selectedClientScopes + "]",
      );
      for (var i = 0; i < $scope.selectedClientScopesToAdd.length; i++) {
        var currentScope = $scope.selectedClientScopesToAdd[i];

        $scope.assignedClientScopes.push(currentScope);

        var index = ComponentUtils.findIndexById(
          $scope.availableClientScopes,
          currentScope.id,
        );
        if (index > -1) {
          $scope.availableClientScopes.splice(index, 1);
        }
      }

      $scope.selectedClientScopes = [];
      $scope.selectedClientScopesToAdd = [];
      updateState();
    };

    $scope.deleteAppliedClientScope = function () {
      $scope.selectedDefClientScopesToRemove = JSON.parse(
        "[" + $scope.selectedDefClientScopes + "]",
      );
      for (var i = 0; i < $scope.selectedDefClientScopesToRemove.length; i++) {
        var currentScope = $scope.selectedDefClientScopesToRemove[i];

        $scope.availableClientScopes.push(currentScope);

        var index = ComponentUtils.findIndexById(
          $scope.assignedClientScopes,
          currentScope.id,
        );
        if (index > -1) {
          $scope.assignedClientScopes.splice(index, 1);
        }
      }

      $scope.selectedDefClientScopes = [];
      $scope.selectedDefClientScopesToRemove = [];

      updateState();
    };

    $scope.usersUiSelect = {
      minimumInputLength: 1,
      delay: 500,
      allowClear: true,
      query: function (query) {
        var data = { results: [] };
        if ("" == query.term.trim()) {
          query.callback(data);
          return;
        }
        User.query(
          {
            realm: $route.current.params.realm,
            search: query.term.trim(),
            max: 20,
          },
          function (response) {
            data.results = response;
            query.callback(data);
          },
        );
      },
      formatResult: function (object, container, query) {
        object.text = object.username;
        return object.username;
      },
    };

    $scope.selectedUser = null;

    $scope.selectUser = function (user) {
      clearEvalResponse();

      if (!user || !user.id) {
        $scope.selectedUser = null;
        $scope.userId = "";
        return;
      }

      $scope.userId = user.id;
    };

    clientSelectControl($scope, $route.current.params.realm, Client);

    $scope.selectedClient = null;

    $scope.selectClient = function (client) {
      if (!client || !client.id) {
        $scope.selectedClient = null;
        return;
      }

      $scope.selectedClient = client;
      updateScopeClientRoles();
    };

    $scope.sendEvaluationRequest = function () {
      // Send request for retrieve protocolMappers
      $scope.protocolMappers = ClientEvaluateProtocolMappers.query({
        realm: realm.realm,
        client: client.id,
        scopeParam: $scope.scopeParam,
      });

      // Send request for retrieve realmRoles
      updateScopeRealmRoles();

      // Send request for retrieve accessToken (in case user was selected)
      if (
        client.protocol === "openid-connect" &&
        $scope.userId != null &&
        $scope.userId !== ""
      ) {
        var url = ClientEvaluateGenerateExampleToken.url({
          realm: realm.realm,
          client: client.id,
          userId: $scope.userId,
          scopeParam: $scope.scopeParam,
        });

        $http.get(url).then(function (response) {
          if (response.data) {
            var oidcAccessToken = angular.fromJson(response.data);
            oidcAccessToken = angular.toJson(oidcAccessToken, true);
            $scope.oidcAccessToken = oidcAccessToken;
          } else {
            $scope.oidcAccessToken = null;
          }
        });
      }

      $scope.showTab(1);
    };

    $scope.isResponseAvailable = function () {
      return $scope.protocolMappers != null;
    };

    $scope.isTokenAvailable = function () {
      return $scope.oidcAccessToken != null;
    };

    $scope.showTab = function (tab) {
      $scope.selectedTab = tab;

      // Check if there is more clever way to do it... :/
      if (tab === 1) {
        $scope.tabCss = { tab1: "active", tab2: "", tab3: "" };
      } else if (tab === 2) {
        $scope.tabCss = { tab1: "", tab2: "active", tab3: "" };
      } else if (tab === 3) {
        $scope.tabCss = { tab1: "", tab2: "", tab3: "active" };
      }
    };

    $scope.protocolMappersShown = function () {
      return $scope.selectedTab === 1;
    };

    $scope.rolesShown = function () {
      return $scope.selectedTab === 2;
    };

    $scope.tokenShown = function () {
      return $scope.selectedTab === 3;
    };

    $scope.sortMappersByPriority = function (mapper) {
      return $scope.mapperTypes[mapper.protocolMapper].priority;
    };

    // Roles

    function updateScopeRealmRoles() {
      $scope.grantedRealmRoles = ClientEvaluateGrantedRoles.query({
        realm: realm.realm,
        client: client.id,
        roleContainer: realm.realm,
        scopeParam: $scope.scopeParam,
      });
      $scope.notGrantedRealmRoles = ClientEvaluateNotGrantedRoles.query({
        realm: realm.realm,
        client: client.id,
        roleContainer: realm.realm,
        scopeParam: $scope.scopeParam,
      });
    }

    function updateScopeClientRoles() {
      if ($scope.selectedClient) {
        $scope.grantedClientRoles = ClientEvaluateGrantedRoles.query({
          realm: realm.realm,
          client: client.id,
          roleContainer: $scope.selectedClient.id,
          scopeParam: $scope.scopeParam,
        });
        $scope.notGrantedClientRoles = ClientEvaluateNotGrantedRoles.query({
          realm: realm.realm,
          client: client.id,
          roleContainer: $scope.selectedClient.id,
          scopeParam: $scope.scopeParam,
        });
      } else {
        $scope.grantedClientRoles = null;
        $scope.notGrantedClientRoles = null;
      }
    }
  },
);

module.controller(
  "ClientScopeTabCtrl",
  function (Dialog, $scope, Current, Notifications, $location, $translate) {
    $scope.removeClientScope = function () {
      Dialog.confirmDelete(
        $scope.clientScope.name,
        "client scope",
        function () {
          $scope.clientScope.$remove(
            {
              realm: Current.realm.realm,
              clientScope: $scope.clientScope.id,
            },
            function () {
              $location.url(
                "/realms/" + Current.realm.realm + "/client-scopes",
              );
              Notifications.success($translate.instant("client-scope-deleted"));
            },
          );
        },
      );
    };
  },
);

module.controller(
  "ClientScopeListCtrl",
  function (
    $scope,
    realm,
    clientScopes,
    ClientScope,
    serverInfo,
    $route,
    Dialog,
    Notifications,
    $location,
    Session,
    $translate,
  ) {
    $scope.removeClientScope = function (clientScope) {
      Dialog.confirmDelete(
        clientScope.name,
        $translate.instant("client-scope"),
        function () {
          ClientScope.remove(
            {
              realm: realm.realm,
              clientScope: clientScope.id,
            },
            function () {
              $route.reload();
              Notifications.success($translate.instant("client-scope-deleted"));
            },
          );
        },
      );
    };

    $scope.clearSearch = function () {
      $scope.search = "";
      $scope.updateQuery("");
    };

    $scope.init = function () {
      $scope.realm = realm;
      $scope.clientScopes = clientScopes;
      $scope.first = 0;
      $scope.limit = Session.getItem("clientScopeFilterCount")
        ? Session.getItem("clientScopeFilterCount")
        : 5;
      $scope.last = $scope.first + $scope.limit;
      $scope.perPage = 5;
      $scope.query = {
        realm: realm.realm,
      };
      $scope.updateSliceArray();
    };
    $scope.selectQuery = function (limit) {
      $scope.limit = limit;
      Session.setItem("clientScopeFilterCount", $scope.limit);
      $scope.first = 0;
      $scope.last = $scope.limit;
      $scope.updateSliceArray();
    };
    $scope.firstPage = function () {
      $scope.first = 0;
      $scope.last = $scope.first + $scope.limit;
      $scope.updateSliceArray();
    };
    $scope.next = function () {
      $scope.first += $scope.limit;
      $scope.last += $scope.limit;
      $scope.updateSliceArray();
    };
    $scope.previous = function () {
      $scope.first -= $scope.limit;
      $scope.last -= $scope.limit;
      $scope.updateSliceArray();
    };
    $scope.updateSliceArray = function () {
      $scope.slicedClientScopes = $scope.clientScopes.slice(
        $scope.first,
        $scope.last,
      );
    };
    $scope.updateQuery = function (searchvalue) {
      $scope.clientRolesearch = searchvalue;
      if (!searchvalue) {
        $scope.updateSliceArray();
      } else {
        $scope.slicedClientScopes = $scope.clientScopes.filter((scope) => {
          return scope.name.includes(searchvalue);
        });
      }
    };
  },
);

module.controller(
  "ClientScopesRealmDefaultCtrl",
  function (
    $scope,
    realm,
    Realm,
    clientScopes,
    realmDefaultClientScopes,
    RealmDefaultClientScopes,
    realmOptionalClientScopes,
    RealmOptionalClientScopes,
    serverInfo,
    $route,
    Dialog,
    Notifications,
    $location,
    $translate,
  ) {
    $scope.realm = realm;
    $scope.realmDefaultClientScopes = realmDefaultClientScopes;
    $scope.realmOptionalClientScopes = realmOptionalClientScopes;

    $scope.availableClientScopes = [];
    $scope.selectedDefaultClientScopes = [];
    $scope.selectedDefDefaultClientScopes = [];

    $scope.selectedOptionalClientScopes = [];
    $scope.selectedDefOptionalClientScopes = [];

    // Populate available client scopes. Available client scopes are neither already assigned to 'default' or 'optional'
    for (var i = 0; i < clientScopes.length; i++) {
      var scopeName = clientScopes[i].name;

      var available = true;
      for (var j = 0; j < $scope.realmDefaultClientScopes.length; j++) {
        if (scopeName === $scope.realmDefaultClientScopes[j].name) {
          available = false;
        }
      }
      for (var j = 0; j < $scope.realmOptionalClientScopes.length; j++) {
        if (scopeName === $scope.realmOptionalClientScopes[j].name) {
          available = false;
        }
      }

      if (available) {
        $scope.availableClientScopes.push(clientScopes[i]);
      }
    }

    $scope.addDefaultClientScope = function () {
      $scope.selectedDefaultClientScopesToAdd = JSON.parse(
        "[" + $scope.selectedDefaultClientScopes + "]",
      );
      toAdd = $scope.selectedDefaultClientScopesToAdd.length;

      for (var i = 0; i < $scope.selectedDefaultClientScopesToAdd.length; i++) {
        var currentScope = $scope.selectedDefaultClientScopesToAdd[i];

        RealmDefaultClientScopes.update(
          {
            realm: realm.realm,
            clientScopeId: currentScope.id,
          },
          function () {
            toAdd = toAdd - 1;
            if (toAdd === 0) {
              $route.reload();
              Notifications.success(
                $translate.instant("realm-default-scopes-updated"),
              );
            }
          },
        );
      }
      $scope.selectedDefaultClientScopesToAdd = [];
    };

    $scope.deleteDefaultClientScope = function () {
      $scope.selectedDefDefaultClientScopesToRemove = JSON.parse(
        "[" + $scope.selectedDefDefaultClientScopes + "]",
      );
      toRemove = $scope.selectedDefDefaultClientScopesToRemove.length;

      for (
        var i = 0;
        i < $scope.selectedDefDefaultClientScopesToRemove.length;
        i++
      ) {
        var currentScope = $scope.selectedDefDefaultClientScopesToRemove[i];

        RealmDefaultClientScopes.remove(
          {
            realm: realm.realm,
            clientScopeId: currentScope.id,
          },
          function () {
            toRemove = toRemove - 1;
            if (toRemove === 0) {
              $route.reload();
              Notifications.success(
                $translate.instant("realm-default-scopes-updated"),
              );
            }
          },
        );
      }
      $scope.selectedDefDefaultClientScopesToRemove = [];
    };

    $scope.addOptionalClientScope = function () {
      $scope.selectedOptionalClientScopesToAdd = JSON.parse(
        "[" + $scope.selectedOptionalClientScopes + "]",
      );
      toAdd = $scope.selectedOptionalClientScopesToAdd.length;

      for (
        var i = 0;
        i < $scope.selectedOptionalClientScopesToAdd.length;
        i++
      ) {
        var currentScope = $scope.selectedOptionalClientScopesToAdd[i];

        RealmOptionalClientScopes.update(
          {
            realm: realm.realm,
            clientScopeId: currentScope.id,
          },
          function () {
            toAdd = toAdd - 1;
            if (toAdd === 0) {
              $route.reload();
              Notifications.success(
                $translate.instant("realm-optional-scopes-updated"),
              );
            }
          },
        );
      }
      $scope.selectedOptionalClientScopesToAdd = [];
    };

    $scope.deleteOptionalClientScope = function () {
      $scope.selectedDefOptionalClientScopesToRemove = JSON.parse(
        "[" + $scope.selectedDefOptionalClientScopes + "]",
      );
      toRemove = $scope.selectedDefOptionalClientScopesToRemove.length;

      for (
        var i = 0;
        i < $scope.selectedDefOptionalClientScopesToRemove.length;
        i++
      ) {
        var currentScope = $scope.selectedDefOptionalClientScopesToRemove[i];

        RealmOptionalClientScopes.remove(
          {
            realm: realm.realm,
            clientScopeId: currentScope.id,
          },
          function () {
            toRemove = toRemove - 1;
            if (toRemove === 0) {
              $route.reload();
              Notifications.success(
                $translate.instant("realm-optional-scopes-updated"),
              );
            }
          },
        );
      }
      $scope.selectedDefOptionalClientScopesToRemove = [];
    };
  },
);

module.controller(
  "ClientScopeDetailCtrl",
  function (
    $scope,
    realm,
    clientScope,
    $route,
    serverInfo,
    ClientScope,
    $location,
    $modal,
    Dialog,
    Notifications,
    ConfirmExitDialog,
    $translate,
  ) {
    $scope.protocols = serverInfo.listProviderIds("login-protocol");

    $scope.realm = realm;
    $scope.create = !clientScope.name;
    $scope.openRedirect = false;

    function updateProperties() {
      if (!$scope.clientScope.attributes) {
        $scope.clientScope.attributes = {};
      }

      if ($scope.clientScope.protocol) {
        $scope.protocol =
          $scope.protocols[
          $scope.protocols.indexOf($scope.clientScope.protocol)
          ];
      } else {
        $scope.protocol = $scope.protocols[0];
      }

      if ($scope.clientScope.attributes["display.on.consent.screen"]) {
        if (
          $scope.clientScope.attributes["display.on.consent.screen"] == "true"
        ) {
          $scope.displayOnConsentScreen = true;
        } else {
          $scope.displayOnConsentScreen = false;
        }
      } else {
        $scope.displayOnConsentScreen = true;
      }

      if ($scope.clientScope.attributes["include.in.token.scope"]) {
        if ($scope.clientScope.attributes["include.in.token.scope"] == "true") {
          $scope.includeInTokenScope = true;
        } else {
          $scope.includeInTokenScope = false;
        }
      } else {
        $scope.includeInTokenScope = true;
      }
    }

    if (!$scope.create) {
      $scope.clientScope = angular.copy(clientScope);
    } else {
      $scope.clientScope = {};
    }

    updateProperties();

    $scope.switchChange = function () {
      $scope.changed = true;
    };

    $scope.changeProtocol = function () {
      if ($scope.protocol == "openid-connect") {
        $scope.clientScope.protocol = "openid-connect";
      } else if ($scope.protocol == "saml") {
        $scope.clientScope.protocol = "saml";
      }
    };

    $scope.$watch(
      function () {
        return $location.path();
      },
      function () {
        $scope.path = $location.path().substring(1).split("/");
      },
    );

    function isChanged() {
      if (
        !angular.equals($scope.clientScope, clientScope) &&
        $scope.clientScope.name
      ) {
        return true;
      } else {
        return false;
      }
    }

    $scope.$watch(
      "clientScope",
      function () {
        $scope.changed = isChanged();
      },
      true,
    );

    $scope.save = function () {
      $scope.clientScope.protocol = $scope.protocol;

      if ($scope.displayOnConsentScreen == true) {
        $scope.clientScope.attributes["display.on.consent.screen"] = "true";
      } else {
        $scope.clientScope.attributes["display.on.consent.screen"] = "false";
      }

      if ($scope.includeInTokenScope == true) {
        $scope.clientScope.attributes["include.in.token.scope"] = "true";
      } else {
        $scope.clientScope.attributes["include.in.token.scope"] = "false";
      }

      if ($scope.create) {
        ClientScope.save(
          {
            realm: realm.realm,
            clientScope: "",
          },
          $scope.clientScope,
          function (data, headers) {
            $scope.changed = false;
            var l = headers().location;
            var id = l.substring(l.lastIndexOf("/") + 1);
            $scope.openRedirect = true;
            $location.url("/realms/" + realm.realm + "/client-scopes/" + id);
            Notifications.success($translate.instant("client-scope-created"));
          },
        );
      } else {
        ClientScope.update(
          {
            realm: realm.realm,
            clientScope: clientScope.id,
          },
          $scope.clientScope,
          function () {
            $scope.changed = false;
            clientScope = angular.copy($scope.clientScope);
            $location.url(
              "/realms/" + realm.realm + "/client-scopes/" + clientScope.id,
            );
            Notifications.success(
              $translate.instant("changes-saved-to-client-scope"),
            );
          },
        );
      }
    };

    $scope.reset = function () {
      $scope.openRedirect = true;
      $route.reload();
    };

    $scope.cancel = function () {
      $location.url("/realms/" + realm.realm + "/client-scopes");
    };

    $scope.$on("$routeChangeStart", function (event, newUrl) {
      if (!$scope.openRedirect && isChanged()) {
        showConfirmExitDialog(
          event,
          ConfirmExitDialog,
          $scope,
          $location,
          newUrl,
        );
      }
    });
  },
);

module.controller(
  "ClientScopeProtocolMapperListCtrl",
  function (
    $scope,
    realm,
    clientScope,
    serverInfo,
    ClientScopeProtocolMappersByProtocol,
    ClientScopeProtocolMapper,
    $route,
    Dialog,
    Notifications,
    $translate,
  ) {
    $scope.realm = realm;
    $scope.clientScope = clientScope;
    $scope.mappersUpdated = false;
    $scope.loaderTypeName = "tableListLoader";
    if (clientScope.protocol == null) {
      clientScope.protocol = "openid-connect";
    }

    var protocolMappers = serverInfo.protocolMapperTypes[clientScope.protocol];
    var mapperTypes = {};
    for (var i = 0; i < protocolMappers.length; i++) {
      mapperTypes[protocolMappers[i].id] = protocolMappers[i];
    }
    $scope.mapperTypes = mapperTypes;

    $scope.removeMapper = function (mapper) {
      console.debug(mapper);
      Dialog.confirmDelete(
        mapper.name,
        $translate.instant("mapper"),
        function () {
          ClientScopeProtocolMapper.remove(
            { realm: realm.realm, clientScope: clientScope.id, id: mapper.id },
            function () {
              Notifications.success($translate.instant("mapper-deleted"));
              $route.reload();
            },
          );
        },
      );
    };

    $scope.sortMappersByPriority = function (mapper) {
      return $scope.mapperTypes[mapper.protocolMapper].priority;
    };

    var updateMappers = function () {
      $scope.mappersUpdated = false;
      $scope.mappers = ClientScopeProtocolMappersByProtocol.query(
        {
          realm: realm.realm,
          clientScope: clientScope.id,
          protocol: clientScope.protocol,
        },
        function () {
          $scope.mappersUpdated = true;
        },
      );
    };

    updateMappers();
    $scope.changeSearch = function () {
      $scope.search = $scope.searchValue;
    };
    $scope.clearSearch = function () {
      $scope.search = "";
      $scope.searchValue = "";
      updateMappers();
    };
  },
);

module.controller(
  "ClientScopeProtocolMapperCtrl",
  function (
    $scope,
    realm,
    serverInfo,
    clientScope,
    mapper,
    clients,
    ClientScopeProtocolMapper,
    Notifications,
    Dialog,
    $location,
    $route,
    ConfirmExitDialog,
    $translate,
  ) {
    $scope.realm = realm;
    $scope.clients = clients;
    $scope.clientScope = clientScope;
    $scope.updateStage = 1;
    if (clientScope.protocol == null) {
      clientScope.protocol = "openid-connect";
    }
    $scope.configType = "mapper";

    $scope.model = {
      realm: realm,
      clientScope: clientScope,
      create: false,
      protocol: clientScope.protocol,
      mapper: angular.copy(mapper),
      changed: false,
    };

    var protocolMappers = serverInfo.protocolMapperTypes[clientScope.protocol];
    for (var i = 0; i < protocolMappers.length; i++) {
      if (protocolMappers[i].id == mapper.protocolMapper) {
        $scope.model.mapperType = protocolMappers[i];
      }
    }
    $scope.$watch(
      function () {
        return $location.path();
      },
      function () {
        $scope.path = $location.path().substring(1).split("/");
      },
    );

    $scope.model.mapperType.properties.forEach((data) => {
      if (
        !mapper.config.hasOwnProperty(data.name) ||
        mapper?.config?.[data.name] === null
      ) {
        mapper.config[data.name] = "";
      }
    });

    $scope.$watch(
      "model.mapper",
      function () {
        if (!angular.equals($scope.model.mapper, mapper)) {
          $scope.model.changed = true;
        } else {
          $scope.model.changed = false;
        }
      },
      true,
    );

    $scope.reset = function () {
      $scope.model.mapper = angular.copy(mapper);
      $scope.model.changed = false;
    };

    $scope.save = function () {
      ClientScopeProtocolMapper.update(
        {
          realm: realm.realm,
          clientScope: clientScope.id,
          id: mapper.id,
        },
        $scope.model.mapper,
        function () {
          mapper = angular.copy($scope.model.mapper);
          $scope.model.changed = false;
          Notifications.success($translate.instant("changesSavedSuccess"));
        },
      );
    };

    $scope.cancel = function () {
      window.history.back();
    };

    $scope.openRedirect = false;
    $scope.remove = function () {
      Dialog.confirmDelete($scope.model.mapper.name, "mapper", function () {
        ClientScopeProtocolMapper.remove(
          {
            realm: realm.realm,
            clientScope: clientScope.id,
            id: $scope.model.mapper.id,
          },
          function () {
            Notifications.success($translate, instant("mapper-deleted"));
            $scope.openRedirect = true;
            $location.url(
              "/realms/" +
              realm.realm +
              "/client-scopes/" +
              clientScope.id +
              "/mappers",
            );
          },
        );
      });
    };

    $scope.$on("$routeChangeStart", function (event, newUrl) {
      if (!$scope.openRedirect && $scope.model.changed) {
        showConfirmExitDialog(
          event,
          ConfirmExitDialog,
          $scope,
          $location,
          newUrl,
        );
      }
    });
  },
);

module.controller(
  "ClientScopeProtocolMapperCreateCtrl",
  function (
    $scope,
    realm,
    serverInfo,
    clientScope,
    clients,
    ClientScopeProtocolMapper,
    Notifications,
    Dialog,
    $location,
    ConfirmExitDialog,
    $translate,
  ) {
    $scope.realm = realm;
    $scope.clients = clients;
    $scope.clientScope = clientScope;
    $scope.saveStage = 1;
    $scope.updateStage = 1;
    if (clientScope.protocol == null) {
      clientScope.protocol = "openid-connect";
    }
    var protocol = clientScope.protocol;
    $scope.model = {
      realm: realm,
      clientScope: clientScope,
      create: true,
      protocol: clientScope.protocol,
      mapper: { protocol: clientScope.protocol, config: {} },
      changed: false,
      mapperTypes: serverInfo.protocolMapperTypes[protocol],
    };

    // apply default configurations on change for selected protocolmapper type.
    $scope.$watch(
      "model.mapperType",
      function () {
        var currentMapperType = $scope.model.mapperType;
        var defaultConfig = {};

        if (currentMapperType && Array.isArray(currentMapperType.properties)) {
          for (var i = 0; i < currentMapperType.properties.length; i++) {
            var property = currentMapperType.properties[i];
            if (property && property.name && property.defaultValue) {
              defaultConfig[property.name] = property.defaultValue;
            }
          }
        }

        $scope.model.mapper.config = defaultConfig;
      },
      true,
    );

    $scope.model.mapperType = $scope.model.mapperTypes[0];

    $scope.$watch(
      function () {
        return $location.path();
      },
      function () {
        $scope.path = $location.path().substring(1).split("/");
      },
    );
    $scope.oldModal = angular.copy($scope.model);
    $scope.$watch(
      "model",
      function () {
        if (
          !angular.equals($scope.oldModal, $scope.model) &&
          $scope.model.mapper.name
        ) {
          $scope.enableBtn = true;
        } else {
          $scope.enableBtn = false;
        }
      },
      true,
    );

    $scope.openRedirect = false;
    $scope.save = function () {
      $scope.model.mapper.protocolMapper = $scope.model.mapperType.id;
      ClientScopeProtocolMapper.save(
        {
          realm: realm.realm,
          clientScope: clientScope.id,
        },
        $scope.model.mapper,
        function (data, headers) {
          var l = headers().location;
          var id = l.substring(l.lastIndexOf("/") + 1);
          $scope.openRedirect = true;
          $location.url(
            "/realms/" +
            realm.realm +
            "/client-scopes/" +
            clientScope.id +
            "/mappers/" +
            id,
          );
          Notifications.success($translate.instant("mapper-created"));
        },
      );
    };

    $scope.cancel = function () {
      //$location.url("/realms");
      window.history.back();
    };

    $scope.$on("$routeChangeStart", function (event, newUrl) {
      if (!$scope.openRedirect && $scope.enableBtn) {
        showConfirmExitDialog(
          event,
          ConfirmExitDialog,
          $scope,
          $location,
          newUrl,
        );
      }
    });
  },
);

module.controller(
  "ClientScopeAddBuiltinProtocolMapperCtrl",
  function (
    $scope,
    realm,
    clientScope,
    serverInfo,
    ClientScopeProtocolMappersByProtocol,
    $http,
    $location,
    Dialog,
    Notifications,
    ConfirmExitDialog,
    $translate,
  ) {
    $scope.realm = realm;
    $scope.clientScope = clientScope;
    $scope.enableBtn = false;
    $scope.mappersUpdated = false;
    $scope.loaderTypeName = "tableListLoader";
    if (clientScope.protocol == null) {
      clientScope.protocol = "openid-connect";
    }

    var protocolMappers = serverInfo.protocolMapperTypes[clientScope.protocol];
    var mapperTypes = {};
    for (var i = 0; i < protocolMappers.length; i++) {
      mapperTypes[protocolMappers[i].id] = protocolMappers[i];
    }
    $scope.mapperTypes = mapperTypes;

    $scope.changeSearch = function () {
      $scope.search = $scope.searchValue;
    };
    $scope.$watch(function () {
      if ($scope.mappers) {
        for (var i = 0; i < $scope.mappers.length; i++) {
          if ($scope.mappers[i].isChecked == true) {
            $scope.enableBtn = true;
            break;
          } else {
            $scope.enableBtn = false;
          }
        }
      }
    }, true);
    $scope.clearSearch = function () {
      $scope.search = "";
      $scope.searchValue = "";
    };
    var updateMappers = function () {
      $scope.mappersUpdated = false;
      var clientMappers = ClientScopeProtocolMappersByProtocol.query(
        {
          realm: realm.realm,
          clientScope: clientScope.id,
          protocol: clientScope.protocol,
        },
        function () {
          var builtinMappers =
            serverInfo.builtinProtocolMappers[clientScope.protocol];
          for (var i = 0; i < clientMappers.length; i++) {
            for (var j = 0; j < builtinMappers.length; j++) {
              if (
                builtinMappers[j].name == clientMappers[i].name &&
                builtinMappers[j].protocolMapper ==
                clientMappers[i].protocolMapper
              ) {
                builtinMappers.splice(j, 1);
                break;
              }
            }
          }
          $scope.mappers = builtinMappers;
          for (var i = 0; i < $scope.mappers.length; i++) {
            $scope.mappers[i].isChecked = false;
          }
          $scope.mappersUpdated = true;
        },
      );
    };

    updateMappers();

    $scope.openRedirect = false;
    $scope.add = function () {
      var toAdd = [];
      for (var i = 0; i < $scope.mappers.length; i++) {
        if ($scope.mappers[i].isChecked) {
          delete $scope.mappers[i].isChecked;
          toAdd.push($scope.mappers[i]);
        }
      }
      $http
        .post(
          authUrl +
          "/admin/realms/" +
          realm.realm +
          "/client-scopes/" +
          clientScope.id +
          "/protocol-mappers/add-models",
          toAdd,
        )
        .then(function () {
          Notifications.success($translate.instant("mappers-added"));
          $scope.openRedirect = true;
          $location.url(
            "/realms/" +
            realm.realm +
            "/client-scopes/" +
            clientScope.id +
            "/mappers",
          );
        })
        .catch(function () {
          $scope.openRedirect = true;
          Notifications.error("Error adding mappers");
          $location.url(
            "/realms/" +
            realm.realm +
            "/client-scopes/" +
            clientScope.id +
            "/mappers",
          );
        });
    };

    $scope.$on("$routeChangeStart", function (event, newUrl) {
      if (!$scope.openRedirect && $scope.enableBtn) {
        showConfirmExitDialog(
          event,
          ConfirmExitDialog,
          $scope,
          $location,
          newUrl,
        );
      }
    });
  },
);

module.controller(
  "ClientScopeScopeMappingCtrl",
  function (
    $scope,
    $http,
    $route,
    realm,
    clientScope,
    Notifications,
    Session,
    Client,
    AddRolesToScope,
    Dialog,
    ClientScopeRealmScopeMapping,
    ClientScopeRealmScopeMappingCount,
    $translate,
  ) {
    $scope.loaderTypeName = "roleListLoader";
    $scope.realm = realm;
    $scope.clientScope = angular.copy(clientScope);
    $scope.selectedRealmRoles = [];
    $scope.selectedRealmMappings = [];
    $scope.realmMappings = [];
    $scope.clientRoles = [];
    $scope.clientComposite = [];
    $scope.selectedClientRoles = [];
    $scope.selectedClientMappings = [];
    $scope.selectedRealmMappingsToRemove = [];
    $scope.clientMappings = [];
    $scope.dummymodel = [];
    $scope.selectedClient = null;

    $scope.query = {
      search: null,
      realm: realm.realm,
      max: 5,
      first: 0,
      clientScope: clientScope.id,
    };
    $scope.entries = 0;
    $scope.query.max = Session.getItem("pageFilterCount")
      ? Session.getItem("pageFilterCount")
      : 5;
    $scope.query.first = 0;
    $scope.pageCount = 5;
    $scope.startCount = 1;
    $scope.maxValueOfPage = 0;
    $scope.searchQueryLoaded = false;

    function updateScopeRealmRoles() {
      $scope.scopeRealmRoles = ClientScopeRealmScopeMapping.query(
        $scope.query,
        function () {
          oldQueryUpdate();
          $scope.searchLoaded = true;
          $scope.lastSearch = $scope.query.search;
        },
      );
    }

    $scope.deleteRealmRole = function (role) {
      Dialog.confirm(
        $translate.instant("delete-role-title"),
        $translate.instant("sure-to-delete-role-from-scope"),
        function () {
          $scope.selectedRealmMappingsToRemove = [{ ...role }];
          $http
            .delete(
              authUrl +
              "/admin/realms/" +
              realm.realm +
              "/client-scopes/" +
              clientScope.id +
              "/scope-mappings/realm",
              {
                data: $scope.selectedRealmMappingsToRemove,
                headers: { "content-type": "application/json" },
              },
            )
            .then(function () {
              Notifications.success(
                $translate.instant("user.roles.remove.success"),
              );
              $route.reload();
            });
        },
      );
    };

    $scope.addRolesToScope = function () {
      AddRolesToScope.open(realm.realm, clientScope.id);
    };

    $scope.firstPage = function () {
      searchQueryWithLoaded();
      $scope.pageCount = parseInt($scope.query.max);
      $scope.query.first = 0;
      $scope.startCount = 1;
      $scope.QueryUpdate();
    };

    $scope.previousPage = function () {
      searchQueryWithLoaded();
      $scope.startCount = $scope.startCount - $scope.query.max;
      let pageCount = $scope.pageCount;
      let queryMax = parseInt($scope.query.max);
      if (pageCount === 0) {
        $scope.pageCount = queryMax - 5;
      } else if (pageCount === $scope.entries) {
        let backFlow = pageCount + queryMax - $scope.maxValueOfPage;
        $scope.pageCount = pageCount - backFlow;
      } else {
        $scope.pageCount -= queryMax;
      }

      $scope.query.first -= parseInt($scope.query.max);
      if ($scope.query.first < 0) {
        $scope.query.first = 0;
      }
      $scope.QueryUpdate();
    };

    $scope.nextPage = function () {
      searchQueryWithLoaded();
      $scope.startCount = $scope.pageCount + 1;
      let fullLenthOfPages = parseInt($scope.query.max) + $scope.pageCount;
      $scope.maxValueOfPage = parseInt($scope.query.max) + $scope.pageCount;
      $scope.pageCount =
        $scope.entries < fullLenthOfPages ? $scope.entries : fullLenthOfPages;
      $scope.query.first += parseInt($scope.query.max);
      $scope.QueryUpdate();
    };

    $scope.reset = function () {
      $scope.query.search = "";
      $route.reload();
    };

    $scope.QueryUpdate = function () {
      $scope.searchLoaded = false;
      for (var i in $scope.query) {
        if ($scope.query[i] === "") {
          delete $scope.query[i];
        }
      }
      $scope.scopeRealmRoles = ClientScopeRealmScopeMapping.query(
        $scope.query,
        function () {
          $scope.searchLoaded = true;
        },
      );
    };

    $scope.isRoleListEmpty = function () {
      return $scope.realmRoles.length == 0;
    };

    function searchQueryWithLoaded() {
      $scope.searchLoaded = false;
      if ($scope.query.search && $scope.searchQueryLoaded) {
        $scope.searchQueryLoaded = true;
      } else {
        $scope.query.search = "";
        $scope.searchQueryLoaded = false;
      }
    }

    function oldQueryUpdate() {
      ClientScopeRealmScopeMappingCount.query(
        {
          realm: realm.realm,
          clientScope: clientScope.id,
          search: $scope.query.search,
        },
        function (response) {
          $scope.entries = response.count;
          if ($scope.entries !== 0 && $scope.entries < $scope.query.max) {
            $scope.pageCount = $scope.entries;
            $scope.startCount = 1;
          } else {
            $scope.pageCount = $scope.query.max;
            $scope.startCount = 1;
          }
        },
      );
    }

    clientSelectControl($scope, $route.current.params.realm, Client);
    updateScopeRealmRoles();
  },
);

module.controller(
  "ClientStoresCtrl",
  function (
    $scope,
    $location,
    $route,
    realm,
    serverInfo,
    Components,
    Notifications,
    Dialog,
    $translate,
  ) {
    $scope.realm = realm;
    $scope.providers =
      serverInfo.componentTypes[
      "org.keycloak.storage.client.ClientStorageProvider"
      ];
    $scope.clientStorageProviders =
      serverInfo.componentTypes[
      "org.keycloak.storage.client.ClientStorageProvider"
      ];
    $scope.instancesLoaded = false;

    if (!$scope.providers) $scope.providers = [];

    $scope.addProvider = function (provider) {
      $location.url(
        "/create/client-storage/" + realm.realm + "/providers/" + provider.id,
      );
    };

    $scope.getInstanceLink = function (instance) {
      return (
        "/realms/" +
        realm.realm +
        "/client-storage/providers/" +
        instance.providerId +
        "/" +
        instance.id
      );
    };

    $scope.getInstanceName = function (instance) {
      return instance.name;
    };
    $scope.getInstanceProvider = function (instance) {
      return instance.providerId;
    };

    $scope.isProviderEnabled = function (instance) {
      return (
        !instance.config["enabled"] || instance.config["enabled"][0] == "true"
      );
    };

    $scope.getInstancePriority = function (instance) {
      if (!instance.config["priority"]) {
        return "0";
      }
      return instance.config["priority"][0];
    };

    Components.query(
      {
        realm: realm.realm,
        parent: realm.id,
        type: "org.keycloak.storage.client.ClientStorageProvider",
      },
      function (data) {
        $scope.instances = data;
        $scope.instancesLoaded = true;
      },
    );

    $scope.removeInstance = function (instance) {
      Dialog.confirmDelete(
        instance.name,
        "client storage provider",
        function () {
          Components.remove(
            {
              realm: realm.realm,
              componentId: instance.id,
            },
            function () {
              $route.reload();
              Notifications.success(
                $translate.instant("user.storage.remove.success"),
              );
            },
          );
        },
      );
    };
  },
);

module.controller(
  "GenericClientStorageCtrl",
  function (
    $scope,
    $location,
    Notifications,
    $route,
    Dialog,
    realm,
    serverInfo,
    instance,
    providerId,
    Components,
    $translate,
  ) {
    $scope.create = !instance.providerId;

    var providers =
      serverInfo.componentTypes[
      "org.keycloak.storage.client.ClientStorageProvider"
      ];

    var providerFactory = null;
    for (var i = 0; i < providers.length; i++) {
      var p = providers[i];

      if (p.id == providerId) {
        $scope.providerFactory = p;
        providerFactory = p;
        break;
      }
    }
    $scope.changed = false;

    function initClientStorageSettings() {
      if ($scope.create) {
        $scope.changed = true;
        instance.name = providerFactory.id;
        instance.providerId = providerFactory.id;
        instance.providerType =
          "org.keycloak.storage.client.ClientStorageProvider";
        instance.parentId = realm.id;
        instance.config = {};
        instance.config["priority"] = ["0"];
        instance.config["enabled"] = ["true"];

        $scope.fullSyncEnabled = false;
        $scope.changedSyncEnabled = false;
        instance.config["cachePolicy"] = ["DEFAULT"];
        instance.config["evictionDay"] = [""];
        instance.config["evictionHour"] = [""];
        instance.config["evictionMinute"] = [""];
        instance.config["maxLifespan"] = [""];
        if (providerFactory.properties) {
          for (var i = 0; i < providerFactory.properties.length; i++) {
            var configProperty = providerFactory.properties[i];
            if (configProperty.defaultValue) {
              instance.config[configProperty.name] = [
                configProperty.defaultValue,
              ];
            } else {
              instance.config[configProperty.name] = [""];
            }
          }
        }
      } else {
        $scope.changed = false;
        if (!instance.config["enabled"]) {
          instance.config["enabled"] = ["true"];
        }
        if (!instance.config["cachePolicy"]) {
          instance.config["cachePolicy"] = ["DEFAULT"];
        }
        if (!instance.config["evictionDay"]) {
          instance.config["evictionDay"] = [""];
        }
        if (!instance.config["evictionHour"]) {
          instance.config["evictionHour"] = [""];
        }
        if (!instance.config["evictionMinute"]) {
          instance.config["evictionMinute"] = [""];
        }
        if (!instance.config["maxLifespan"]) {
          instance.config["maxLifespan"] = [""];
        }
        if (!instance.config["priority"]) {
          instance.config["priority"] = ["0"];
        }

        if (providerFactory.properties) {
          for (var i = 0; i < providerFactory.properties.length; i++) {
            var configProperty = providerFactory.properties[i];
            if (!instance.config[configProperty.name]) {
              instance.config[configProperty.name] = [""];
            }
          }
        }
      }
    }

    initClientStorageSettings();
    $scope.instance = angular.copy(instance);
    $scope.realm = realm;

    $scope.$watch(
      "instance",
      function () {
        if (!angular.equals($scope.instance, instance)) {
          $scope.changed = true;
        }
      },
      true,
    );

    $scope.save = function () {
      $scope.changed = false;
      if ($scope.create) {
        Components.save(
          { realm: realm.realm },
          $scope.instance,
          function (data, headers) {
            var l = headers().location;
            var id = l.substring(l.lastIndexOf("/") + 1);

            $location.url(
              "/realms/" +
              realm.realm +
              "/client-storage/providers/" +
              $scope.instance.providerId +
              "/" +
              id,
            );
            Notifications.success(
              $translate.instant("user.storage.create.success"),
            );
          },
        );
      } else {
        Components.update(
          { realm: realm.realm, componentId: instance.id },
          $scope.instance,
          function () {
            $route.reload();
            Notifications.success(
              $translate.instant("user.storage.edit.success"),
            );
          },
        );
      }
    };

    $scope.reset = function () {
      $route.reload();
    };

    $scope.cancel = function () {
      if ($scope.create) {
        $location.url("/realms/" + realm.realm + "/client-stores");
      } else {
        $route.reload();
      }
    };
  },
);

function formOIDCUrl(
  authServerUrl,
  clientId,
  redirectURI,
  scope,
  response,
  nonce,
  code_challenge,
  code_challenge_method,
) {
  let url =
    authServerUrl +
    "client_id=" +
    clientId +
    "&redirect_uri=" +
    redirectURI +
    "&scope=" +
    scope +
    response +
    "&nonce=" +
    nonce;

  if (code_challenge && code_challenge_method) {
    url += "&code_challenge=" + code_challenge;
    url += "&code_challenge_method=" + code_challenge_method;
  }
  return url;
}

function createUUID() {
  var hexDigits = "0123456789abcdef";
  var s = generateRandomString(36, hexDigits).split("");
  s[14] = "4";
  s[19] = hexDigits.substr((s[19] & 0x3) | 0x8, 1);
  s[8] = s[13] = s[18] = s[23] = "-";
  var uuid = s.join("");
  return uuid;
}
function generateRandomString(len, alphabet) {
  var randomData = generateRandomData(len);
  var chars = new Array(len);
  for (var i = 0; i < len; i++) {
    chars[i] = alphabet.charCodeAt(randomData[i] % alphabet.length);
  }
  return String.fromCharCode.apply(null, chars);
}
function generateRandomData(len) {
  // use web crypto APIs if possible
  var array = null;
  var crypto = window.crypto || window.msCrypto;
  if (crypto && crypto.getRandomValues && window.Uint8Array) {
    array = new Uint8Array(len);
    crypto.getRandomValues(array);
    return array;
  }

  // fallback to Math random
  array = new Array(len);
  for (var j = 0; j < array.length; j++) {
    array[j] = Math.floor(256 * Math.random());
  }
  return array;
}

function generateCodeVerifier(len) {
  return generateRandomString(
    len,
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789",
  );
}

function generatePkceChallenge(pkceMethod, codeVerifier) {
  const sha256_imported = window["sha256"];
  const base64js_imported = window["base64js"];
  switch (pkceMethod) {
    case "plain":
      return codeVerifier;
    case "S256":
      // hash codeVerifier, then encode as url-safe base64 without padding
      var hashBytes = new Uint8Array(sha256_imported.arrayBuffer(codeVerifier));
      var encodedHash = base64js_imported
        .fromByteArray(hashBytes)
        .replace(/\+/g, "-")
        .replace(/\//g, "_")
        .replace(/\=/g, "");
      return encodedHash;
    default:
      throw "Invalid value for pkceMethod";
  }
}


module.controller('APICtrl', function ($scope, Session, Client, ClientsCount, FetchClientSecret, $translate, realm, $routeParams, Notifications, InputDialog, $route, $modal) {
    $scope.realm = angular.copy(realm);
    $scope.isLoading = true;
    $scope.apiArray = [];
    $scope.query = {};
    $scope.apps = [];
    $scope.showFilter = false;
    $scope.loaderTypeName = "cardListLoader";
    $scope.query.max = Session.getItem("pageFilterCount")
    ? Session.getItem("pageFilterCount")
    : 5;
    $scope.query.type = $routeParams.type;
    $scope.query.sandboxMode = $routeParams.mode === "sandbox";
    $scope.query.first = 0;
    $scope.query.max = 5;
    $scope.query.isApiClient = true;
    $scope.query.orderBy = 'last-updated'
    $scope.orderBy = $translate.instant($scope.query.orderBy);
    $scope.appCount = 0;
    $scope.clientSecrets = {};

    $scope.tooltipTxt = $translate.instant("copy");

    function fetchApps() {
      $scope.query.sort = "ASC";
      $scope.query.realm = $scope.realm.realm;
      $scope.isLoading = true;
      const queryParams =
        $scope.query.clientName && $scope.query.search
          ? `&clientName=${$scope.query.clientName}&search=true`
          : "";
      ClientsCount.query(
      {
        queryParams: `${queryParams}&sandboxMode=false&isApiClient=true`,
        realm: realm.realm,
      }, function(response) {
        $scope.appCount = response.count;
        Client.query($scope.query, function (data) {
        $scope.apps = data;
        $scope.isLoading = false;
      });})
    }

    fetchApps();

    $scope.copyToClipboard = async (app, type) => {
      let value = "";
      let toastMsg = "";
      switch(type) {
        case "id":
          value = app.clientId;
          toastMsg = "app-id-copied"
          break;
        case "secret":
          value = app.attributes['client-secret'];
          toastMsg = "app-secret-copied"
          break;
        case "basic":
          value = btoa(`${app.clientId}:${app.attributes['client-secret']}`);
          toastMsg = "app-basic-copied"
          break;
      }
      try {
        await navigator.clipboard.writeText(value);
        Notifications.success(
          $translate.instant(toastMsg),
        );
        $scope.$apply()
      } catch (err) {
        console.error('Failed to copy: ', err);
      }
    }

    $scope.nextPage = () => {
      $scope.query.first += $scope.query.max;
      fetchApps();
    }

    $scope.previousPage = () => {
      $scope.query.first -= $scope.query.max;
      fetchApps();
    }

    $scope.firstPage = () => {
      $scope.query.first = 0;
      fetchApps();
    }

    $scope.setOrderBy = (value) => {
      $scope.query.orderBy = value;
      $scope.orderBy = $translate.instant($scope.query.orderBy)
      $scope.showFilter = false;
      $scope.first = 0;
      fetchApps();
    }

    $scope.selectQuery = () => {
      $scope.query.first = 0;
      fetchApps();
    }

    $scope.searchQuery = () => {
      $scope.query.first = 0;
      $scope.query.search = true;
      fetchApps();
    }

    $scope.clearSearch = () => {
      $scope.query.first = 0;
      $scope.query.search = true;
      $scope.query.clientName = "";
      fetchApps();
    }

    $scope.removeApp = function (app) {
      InputDialog.confirmTenantDelete(app.name, "client", function () {
        Client.remove(
          {
            realm: realm.realm,
            client: app.id,
          },
          function () {
            $route.reload();
            Notifications.success(
              app.name + " " + $translate.instant("api-application-deleted"),
            );
          },
        );
      });
    }

     $scope.createApp = function () {
        var controller = function ($scope, $modalInstance, Client, Notifications) {
          $scope.client = {
            "attributes": {
              "login_theme": "default",
              "consent_rejected_reason": "Incorrect Information##Suspicious Activity##Transaction not triggered by me##Inappropriate Transaction"
            },
            "authenticationFlowBindingOverrides": {
              "ciba_auth": "api-authorizer"
            },
            "enabled": true,
            "redirectUris": [],
            "protocol": "openid-connect",
            "appType": "ciba",
            "sandboxMode": false,
          };

          $scope.ok = function () {
            Client.save(
              {
                realm: realm.realm,
                client: "",
              },
              { ...$scope.client},
              function (data, headers) {
                Notifications.success($translate.instant("api-app-created"))
                $route.reload();
              },
              function (err) {
                Notifications.error($translate.instant(err.data.errorMessage));
              },
            );
          };

          $scope.cancel = function () {
            $modalInstance.dismiss('cancel');
          };
          $scope.$on('$routeChangeStart', function () {
            $modalInstance.close();
          });
          $(document).ready(function () {
              setTimeout(function () {
                  $("#name").focus();
              }, 100);
          });
        };
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/create-api-app.html',
            controller: controller,
            backdrop: 'static',
            resolve: {
            }
        });
    };

});
