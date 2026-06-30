'use strict';

var module = angular.module('keycloak.services', ['ngResource', 'ngRoute']);
var realmObject = {};

module.service('Dialog', function ($modal, $translate) {
    var dialog = {};

    var openDialog = function (title, message, btns, template, cancel) {
        var controller = function ($scope, $modalInstance, title, message, btns) {
            $scope.title = title;
            $scope.message = message;
            $scope.btns = btns;

            $scope.ok = function () {
                $modalInstance.close();
            };
            $scope.cancel = function () {
                $modalInstance.dismiss();
                cancel && cancel();
            };

            $scope.$on('$routeChangeStart', function () {
                $modalInstance.dismiss();
            });
        };

        return $modal.open({
            templateUrl: resourceUrl + template,
            controller: controller,
            backdrop: 'static',
            resolve: {
                title: function () {
                    return title;
                },
                message: function () {
                    return message;
                },
                btns: function () {
                    return btns;
                }
            }
        }).result;
    };

    var escapeHtml = function (str) {
        var div = document.createElement('div');
        div.appendChild(document.createTextNode(str));
        return div.innerHTML;
    };

    //Confirming on  Regenerating Client Secret
    dialog.confirmRegenerateScrt = function (name, type, success) {
        var title = type;
        var msg = $translate.instant('are-you-sure-msg') + ' ' + name + $translate.instant('question-mark');
        var btns = {
            ok: {
                label: $translate.instant('yes'),
                cssClass: 'btn btn-danger'
            },
            cancel: {
                label: $translate.instant('no'),
                cssClass: 'btn btn-secondary'
            }
        };
        openDialog(title, msg, btns, '/templates/kc-modal.html').then(success);
    };

    //Asks for the Mobile Number
    dialog.asksForPhoneNumbertoTest = function (name, type, success) {
        var title = type;
        var msg = name;
        var btns = {
            ok: {
                label: $translate.instant('submit'),
                cssClass: 'btn btn-danger'
            },
            cancel: {
                label: $translate.instant('cancel'),
                cssClass: 'btn btn-secondary'
            }
        };
        openInputDialog(title, msg, btns, '/templates/kc-input-modal.html').then(success);
    };

    dialog.confirmDelete = function (name, type, success, cancel) {
        //        var title = 'Delete '  (type.charAt(0).toUpperCase() + type.slice(1));
        //        var title = $translate.instant('delete-policies', {type:type.charAt(0).toUpperCase() + type.slice(1)});
        var title = $translate.instant('delete-policies') + $translate.instant(type);
        if (type === 'execution') {
            var msg = $translate.instant('dialogs.delete.message') + ' ' + $translate.instant(type) + ' ' + $translate.instant(name) + $translate.instant('question-mark');
        }
        else {
            var msg = $translate.instant('dialogs.delete.message') + ' ' + $translate.instant(type) + ' ' + $translate.instant(name) + $translate.instant('question-mark');
        }
        var btns = {
            ok: {
                label: $translate.instant('delete'),
                cssClass: 'btn btn-danger'
            },
            cancel: {
                label: $translate.instant('cancel'),
                cssClass: 'btn btn-secondary'
            }
        };
        openDialog(title, msg, btns, '/templates/kc-modal.html', cancel).then(success);
    };

    dialog.confirmInvite = function (name, type, success) {
        var title = $translate.instant('send-group-invite-btn-txt');
        var msg = $translate.instant('are-you-sure-msg-for-enrollment-email') + type + ' ' + name + $translate.instant('question-mark');
        var btns = {
            ok: {
                label: $translate.instant('yes'),
                cssClass: 'btn btn-primary'
            },
            cancel: {
                label: $translate.instant('no'),
                cssClass: 'btn btn-secondary'
            }
        };

        openDialog(title, msg, btns, '/templates/kc-modal.html').then(success);
    };

    dialog.confirmPriorityChange = function (success) {
        var title = $translate.instant('confirm-priority-change');
        var msg = $translate.instant('confirm-priority-change-help-txt');
        var btns = {
            ok: {
                label: $translate.instant('save'),
                cssClass: 'btn btn-primary'
            },
            cancel: {
                label: $translate.instant('cancel'),
                cssClass: 'btn btn-secondary'
            }
        };

        openDialog(title, msg, btns, '/templates/kc-modal.html').then(success);
    };

    dialog.confirmInvite = function (name, type, success) {
        var title = $translate.instant('invite-users');
        var msg = $translate.instant('are-you-sure-to-invite-users') + ' ' + type + ' ' + name + $translate.instant('question-mark');
        var btns = {
            ok: {
                label: $translate.instant('yes'),
                cssClass: 'btn btn-primary'
            },
            cancel: {
                label: $translate.instant('no'),
                cssClass: 'btn btn-secondary'
            }
        };

        openDialog(title, msg, btns, '/templates/kc-modal.html').then(success);
    };

    dialog.confirmUnlink = function (title, msg, success) {
        var btns = {
            ok: {
                label: $translate.instant('unlink-importedusers-btn'),
                cssClass: 'btn btn-danger'
            },
            cancel: {
                label: $translate.instant('cancel'),
                cssClass: 'btn btn-secondary'
            }
        };

        openDialog(title, msg, btns, '/templates/kc-modal.html').then(success);
    };

    dialog.confirmRemove = function (name, type, success) {
        var title = $translate.instant('remove') + ' ' + escapeHtml(type.charAt(0).toUpperCase() + type.slice(1));
        var msg = $translate.instant('are-you-sure-to-remove-link') + ' ' + name + $translate.instant('question-mark');
        var btns = {
            ok: {
                label: $translate.instant('remove'),
                cssClass: 'btn btn-danger'
            },
            cancel: {
                label: $translate.instant('cancel'),
                cssClass: 'btn btn-secondary'
            }
        };

        openDialog(title, msg, btns, '/templates/kc-modal.html').then(success);
    };

    dialog.confirmClearCache = function (type, success) {
        var title = $translate.instant('clear') + ' ' + $translate.instant(type) + ' ' + $translate.instant('realm-tab-cache');
        var msg = $translate.instant('are-you-sure-to-clear') + ' ' + $translate.instant(type) + ' ' + $translate.instant('cache') + $translate.instant('question-mark');
        var btns = {
            ok: {
                label: $translate.instant('clear'),
                cssClass: 'btn btn-danger'
            },
            cancel: {
                label: $translate.instant('cancel'),
                cssClass: 'btn btn-secondary'
            }
        };

        openDialog(title, msg, btns, '/templates/kc-modal.html').then(success);
    };

    dialog.confirmClearQueue = function (type, success) {
        var title = $translate.instant('clear') + ' ' + $translate.instant(type) + ' ' + $translate.instant('realm-tab-queue');
        var msg = $translate.instant('are-you-sure-to-clear') + ' ' + $translate.instant(type) + ' ' + $translate.instant('queue') + $translate.instant('question-mark');
        var btns = {
            ok: {
                label: $translate.instant('clear'),
                cssClass: 'btn btn-danger'
            },
            cancel: {
                label: $translate.instant('cancel'),
                cssClass: 'btn btn-secondary'
            }
        };

        openDialog(title, msg, btns, '/templates/kc-modal.html').then(success);
    };

    dialog.confirmGenerateKeys = function (name, type, success) {
        var title = $translate.instant('generate-new-keys-for-realm');
        var msg = $translate.instant('are-you-sure-to-generate-new-keys') + ' ' + name + $translate.instant('question-mark');
        var btns = {
            ok: {
                label: $translate.instant('generate-keys'),
                cssClass: 'btn btn-danger'
            },
            cancel: {
                label: $translate.instant('cancel'),
                cssClass: 'btn btn-secondary'
            }
        };

        openDialog(title, msg, btns, '/templates/kc-modal.html').then(success);
    };

    dialog.confirm = function (title, message, success, cancel) {
        var btns = {
            ok: {
                label: title,
                cssClass: 'btn btn-danger'
            },
            cancel: {
                label: $translate.instant('cancel'),
                cssClass: 'btn btn-secondary'
            }
        };

        openDialog(title, message, btns, '/templates/kc-modal.html').then(success, cancel);
    };

    dialog.message = function (title, message, success, cancel) {
        var btns = {
            ok: {
                label: $translate.instant('ok'),
                cssClass: 'btn btn-secondary'
            }
        };

        openDialog(title, message, btns, '/templates/kc-modal-message.html').then(success, cancel);
    };

    dialog.open = function (title, message, btns, success, cancel) {
        openDialog(title, message, btns, '/templates/kc-modal.html').then(success, cancel);
    };



    dialog.confirmUnInstall = function (name, type, success) {
        var title = $translate.instant('uninstall') + ' ' + name;
        var msg = $translate.instant('uninstall-partner-txt') + ' ' + name + '?';
        var btns = {
            ok: {
                label: $translate.instant('uninstall'),
                cssClass: 'btn btn-danger'
            },
            cancel: {
                label: $translate.instant('cancel'),
                cssClass: 'btn btn-secondary'
            }
        };

        openDialog(title, msg, btns, '/templates/kc-modal.html').then(success);
    };

    dialog.selectUsersToGroup;

    return dialog;
});

module.service('InputDialog', function ($modal, $translate) {
    var dialog = {};

    //Asks for the Mobile Number
    dialog.asksForPhoneNumbertoTest = function (config, configType, id, name, type, success, realm) {
        var title = type;
        var message = name;
        var btns = {
            ok: {
                label: $translate.instant('submit'),
                cssClass: 'btn btn-primary'
            },
            cancel: {
                label: $translate.instant('cancel'),
                cssClass: 'btn btn-secondary'
            }
        };
        var inputController = function ($scope, $modalInstance, title, message, btns, $document, ComponentUtils, TestSmsConfig) {
            $scope.title = title;
            $scope.message = message;
            $scope.btns = btns;
            $scope.sms = {};
            $scope.realm = angular.copy(realm);
            $scope.stage = 1;
            $scope.enableSubmit = function () {
                if (intl.isValidNumber()) {
                    $('#submitBtn').removeClass('disabled');
                    $('#submitBtn').removeAttr('disabled');
                } else {
                    $('#submitBtn').addClass('disabled');
                    $('#submitBtn').attr('disabled', 'disabled');
                }
            };
            function validatePhoneNumber() {
                if (intl.isValidNumber()) {
                    return true;
                } else {
                    return false;
                }
            };
            $(document).ready(function () {
                setTimeout(function () {
                    $("#mobNum").focus();
                }, 100);
            });

            function resetFields() {
                $('#testphoneCancelBtn').removeAttr('disabled', 'disabled');
                $('#mobNum').removeAttr('disabled', 'disabled');
                $('#submitBtn').removeAttr('disabled', 'disabled');
            }

            $scope.ok = function () {
                $scope.stage = 2;
                $('#testphoneCancelBtn').attr('disabled', 'disabled');
                $('#mobNum').attr('disabled', 'disabled');
                $('#submitBtn').attr('disabled', 'disabled');
                document.getElementById("body-container").classList.add("hideNotification");
                var error_msg_elem = angular.element($document[0].querySelector('#error_msg'));
                if (validatePhoneNumber()) {
                    var configCopy = angular.copy(config);
                    ComponentUtils.convertAllMultivaluedStringValuesToList(configType.properties, configCopy.config);
                    ComponentUtils.convertAllListValuesToMultivaluedString(configType.properties, configCopy.config);
                    configCopy.config.phoneNumber = intl.getNumber();
                    TestSmsConfig.save({
                        realm: $scope.realm.realm,
                        provider: id
                    }, configCopy.config, function (confRes) {
                        $scope.status = confRes.success;
                        $scope.errMsg = confRes.message;
                        resetFields();
                        if (confRes.success) {
                            $scope.stage = 3;
                        }
                        else {
                            $scope.stage = 0;
                        }
                    });
                    error_msg_elem.text("");
                } else {
                    resetFields();
                    $scope.stage = 1;
                    error_msg_elem.text($translate.instant("invalid-phone-number"));
                }
            };
            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
                setTimeout(() => document.getElementById("body-container").classList.remove("hideNotification"), 5000);
            };
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
                setTimeout(() => document.getElementById("body-container").classList.remove("hideNotification"), 5000);
            });
        };
        $modal.open({
            templateUrl: resourceUrl + '/templates/kc-input-modal.html',
            controller: inputController,
            backdrop: 'static',
            resolve: {
                title: function () {
                    return title;
                },
                message: function () {
                    return message;
                },
                btns: function () {
                    return btns;
                }
            }
        });
    };

    dialog.confirmWorkflowChange = function (selectedNode, draggingNode, success, cancel) {
        var title = $translate.instant('confirm-workflow-update');
        var message = $translate.instant('confirm-change-workflow-helperTxt');
        var btns = {
            ok: {
                label: $translate.instant('confirm'),
                cssClass: 'btn btn-primary'
            },
            cancel: {
                label: $translate.instant('cancel'),
                cssClass: 'btn btn-secondary'
            }
        };
        var inputController = function ($scope, $modalInstance, title, message) {
            $scope.title = title;
            $scope.message = message;
            $scope.btns = btns;
            $scope.selectedNode = selectedNode;
            $scope.draggingNode = draggingNode;

            $scope.ok = function () {
                success()
                $modalInstance.close();
            };
            $scope.cancel = function () {
                $modalInstance.dismiss();
                cancel && cancel();
            };
        
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
            });
        };
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/workflow-confirm-change.html',
            controller: inputController,
            backdrop: 'static',
            size: 'md',
            resolve: {
                title: function () {
                    return title;
                },
                message: function () {
                    return message;
                },
                btns: function () {
                    return btns;
                }
            }
        });
    };

    //Asks for EmailID for testing connection
    dialog.asksForEmailtoTest = function (config, configType, id, name, type, success, realm) {
        var title = name;
        var message = type;
        var btns = {
            ok: {
                label: $translate.instant('submit'),
                cssClass: 'btn btn-primary'
            },
            cancel: {
                label: $translate.instant('cancel'),
                cssClass: 'btn btn-secondary'
            }
        };
        var inputController = function ($scope, $modalInstance, title, message, btns, ValidationUtils, $document, RealmSMTPConnectionTester, Notifications, ComponentUtils, TestEmailConfig) {
            $scope.title = title;
            $scope.message = message;
            $scope.btns = btns;
            $scope.email = {};
            $scope.realm = angular.copy(realm);
            $scope.stage = 1;
            $scope.enableSubmit = function () {
                const emailToTest = $("#email").val();
                if (emailToTest && ValidationUtils.validateEmail(emailToTest)) {
                    $('#submitBtn').removeClass('disabled');
                    $('#submitBtn').removeAttr('disabled');
                } else {
                    $('#submitBtn').addClass('disabled');
                    $('#submitBtn').attr('disabled', 'disabled');
                }
            };
            var initSMTPTest = function (email) {
                //adding emailId params to the config object
                $scope.realm.smtpServer.emailId = email;
                return {
                    realm: $scope.realm.realm,
                    config: JSON.stringify($scope.realm.smtpServer)
                };
            };
            $(document).ready(function () {
                setTimeout(function () {
                    $("#email").focus();
                }, 100);
            });
            $scope.ok = function () {
                $scope.stage = 2;
                $('#testEmailCancelBtn').attr('disabled', 'disabled');
                $('#email').attr('disabled', 'disabled');
                $('#submitBtn').attr('disabled', 'disabled');
                document.getElementById("body-container").classList.add("hideNotification");
                var error_msg_elem = angular.element($document[0].querySelector('#error_msg'));
                if ($scope.email.id && ValidationUtils.validateEmail($scope.email.id)) {
                    var configCopy = angular.copy(config);
                    ComponentUtils.convertAllMultivaluedStringValuesToList(configType.properties, configCopy.config);
                    ComponentUtils.convertAllListValuesToMultivaluedString(configType.properties, configCopy.config);
                    configCopy.config.emailId = $scope.email.id;
                    TestEmailConfig.save({
                        realm: $scope.realm.realm,
                        provider: id
                    }, configCopy.config, function (confRes) {
                        $scope.status = confRes.success;
                        $scope.errMsg = confRes.message;
                        $('#testEmailCancelBtn').removeAttr('disabled', 'disabled');
                        $('#email').removeAttr('disabled', 'disabled');
                        $('#submitBtn').removeAttr('disabled', 'disabled');
                        if (confRes.success) {
                            $scope.stage = 3;
                            success(true)
                        }
                        else {
                            success(false)
                            $scope.stage = 0;
                        }
                    });
                    error_msg_elem.text("");
                } else {
                    success(false)
                    error_msg_elem.text($translate.instant("invalid-email-address"));
                }
            };
            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
                setTimeout(() => document.getElementById("body-container").classList.remove("hideNotification"), 5000);
            };
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
                setTimeout(() => document.getElementById("body-container").classList.remove("hideNotification"), 5000);
            });
        };
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/email-test-conn.html',
            controller: inputController,
            backdrop: 'static',
            resolve: {
                title: function () {
                    return title;
                },
                message: function () {
                    return message;
                },
                btns: function () {
                    return btns;
                }
            }
        });
    };
    //asks for emailId for testing template
    dialog.asksForEmailtoTestTemplate = function (name, type, realmId, templateName, success, realm) {
        var title = name;
        var message = type;
        var btns = {
            ok: {
                label: $translate.instant('submit'),
                cssClass: 'btn btn-primary'
            },
            cancel: {
                label: $translate.instant('cancel'),
                cssClass: 'btn btn-secondary'
            }
        };
        var inputController = function ($scope, $modalInstance, title, message, btns, ValidationUtils, $document, TestEmailTemplate) {
            $scope.title = title;
            $scope.message = message;
            $scope.btns = btns;
            $scope.email = {};
            $scope.realm = angular.copy(realm);
            $scope.stage = 1;
            $scope.enableSubmit = function () {
                const emailToTest = $("#email").val();
                if (emailToTest && ValidationUtils.validateEmail(emailToTest)) {
                    $('#submitBtn').removeClass('disabled');
                    $('#submitBtn').removeAttr('disabled');
                } else {
                    $('#submitBtn').addClass('disabled');
                    $('#submitBtn').attr('disabled', 'disabled');
                }
            };
            var initSMTPTest = function (email) {
                //adding emailId params to the config object
                $scope.realm.smtpServer.emailId = email;
                return {
                    realm: $scope.realm.realm,
                    config: JSON.stringify($scope.realm.smtpServer)
                };
            };
            $(document).ready(function () {
                setTimeout(function () {
                    $("#email").focus();
                }, 100);
            });
            $scope.ok = function () {
                $scope.stage = 2;
                $('#testEmailCancelBtn').attr('disabled', 'disabled');
                $('#email').attr('disabled', 'disabled');
                $('#submitBtn').attr('disabled', 'disabled');
                document.getElementById("body-container").classList.add("hideNotification");
                var error_msg_elem = angular.element($document[0].querySelector('#error_msg'));
                if ($scope.email.id && ValidationUtils.validateEmail($scope.email.id)) {
                    //success($scope.email.id);
                    var testConfig = {
                        emailId: $scope.email.id,
                        templateName: templateName,
                    };
                    TestEmailTemplate.save({
                        realm: realmId
                    }, testConfig, function (confRes) {
                        $('#testEmailCancelBtn').removeAttr('disabled', 'disabled');
                        $('#email').removeAttr('disabled', 'disabled');
                        $("#submitBtn").removeAttr("disabled", "disabled");
                        $scope.errMsg = '';
                        $scope.statusMsg = "email-sent-successfully";
                        $scope.stage = 3;
                    }, function (err) {
                        $('#testEmailCancelBtn').removeAttr('disabled', 'disabled');
                        $('#email').removeAttr('disabled', 'disabled');
                        $scope.statusMsg = '';
                        $scope.errMsg = err.data.errorMessage;
                        $scope.stage = 0;
                    });
                    error_msg_elem.text("");
                } else {
                    error_msg_elem.text($translate.instant("invalid-email-address"));
                }
            };
            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
                setTimeout(() => document.getElementById("body-container").classList.remove("hideNotification"), 5000);
            };
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
                setTimeout(() => document.getElementById("body-container").classList.remove("hideNotification"), 5000);
            });
        };
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/email-test-conn.html',
            controller: inputController,
            backdrop: 'static',
            resolve: {
                title: function () {
                    return title;
                },
                message: function () {
                    return message;
                },
                btns: function () {
                    return btns;
                }
            }
        });
    };

    //asks for tenantName for deleting tenant
    dialog.confirmTenantDelete = function (name, type, success) {
        var title = $translate.instant('delete') + " " + name;
        var message = name;
        var typeTodelete = type;
        var btns = {
            ok: {
                label: $translate.instant('modal-yes-label'),
                cssClass: 'btn btn-danger'
            },
            cancel: {
                label: $translate.instant('cancel'),
                cssClass: 'btn btn-secondary'
            }
        };
        var inputController = function ($scope, $modalInstance, title, typeTodelete, message, btns, $sce, $translate, $document) {
            $scope.title = title;
            $scope.type = type;
            $scope.typeTodelete = typeTodelete;
            $scope.message = $sce.trustAsHtml($translate.instant(`delete-${typeTodelete}-warning-message`));
            $scope.label = $sce.trustAsHtml($translate.instant('delete-tenant-input-label', { tenantName: message }));
            $scope.tenant = { name: '' };
            $scope.btns = btns;

            $(document).ready(function () {
                setTimeout(function () {
                    $("#tenantName").focus();
                }, 100);
            });
            $scope.ok = function (event, elementType) {
                const tenantNametoCheck = $('#deleteValue').val();
                if (message === tenantNametoCheck) {
                    success(event, elementType);
                    $modalInstance.close();
                } else {
                    $('#error_msg').text($translate.instant(`${typeTodelete}-name-mismatch`));
                }
            };
            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
            });
        };
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/delete-tenant-modal.html',
            controller: inputController,
            backdrop: 'static',
            resolve: {
                title: function () {
                    return title;
                },
                typeTodelete: function () {
                    return typeTodelete;
                },
                message: function () {
                    return message;
                },
                btns: function () {
                    return btns;
                }
            }
        });
    };

    // asks tenant name for client duplication
    dialog.clientNameForDuplication = function (appToDuplicate, type, success) {
            var title = $translate.instant('duplicate') + ' ' + appToDuplicate;
            var appToDuplicate = appToDuplicate;
            var typeToDuplicate = type;
            var btns = {
                ok: {
                    label: $translate.instant('modal-yes-label'),
                    cssClass: 'btn btn-primary'
                },
                cancel: {
                    label: $translate.instant('cancel'),
                    cssClass: 'btn btn-secondary'
                }
            };
            var inputController = function ($scope, $modalInstance, title, typeToDuplicate, btns, $sce, $translate, $document, appToDuplicate) {
                $scope.title = title;
                $scope.type = type;
                $scope.typeToDuplicate = typeToDuplicate;
                $scope.appToDuplicate = appToDuplicate;
                $scope.label = $sce.trustAsHtml($translate.instant('duplicate-client-help-txt'));
                $scope.warnMsg = $sce.trustAsHtml($translate.instant('application-duplication-msg'));
                $scope.btns = btns;
                $scope.duplicateAppName = $translate.instant('copy-of') + " " + appToDuplicate;

                $scope.ok = function (event, elementType) {
                    const newAppName = $('#newAppName').val();
                    if(newAppName.trim().length > 0) {
                        success(newAppName);
                        $modalInstance.close();
                    } else {
                        $('#error_msg').text($translate.instant('duplicate-invalid-client-name'));
                    }
                };
                $scope.cancel = function () {
                    $modalInstance.dismiss('cancel');
                };
                $scope.$on('$routeChangeStart', function () {
                    $modalInstance.close();
                });
            };
            $modal.open({
                templateUrl: resourceUrl + '/partials/modal/duplicate-client-modal.html',
                controller: inputController,
                backdrop: 'static',
                resolve: {
                    title: function () {
                        return title;
                    },
                    typeToDuplicate: function () {
                        return typeToDuplicate;
                    },
                    appToDuplicate: function () {
                        return appToDuplicate;
                    },
                    btns: function () {
                        return btns;
                    }
                }
            });
        };

    dialog.confirmTenantRemove = function (name, type, success) {
        var title = $translate.instant('remove') + ' ' + name;
        var message = name;
        var typeTodelete = type;
        var btns = {
            ok: {
                label: $translate.instant('modal-yes-label'),
                cssClass: 'btn btn-danger'
            },
            cancel: {
                label: $translate.instant('cancel'),
                cssClass: 'btn btn-secondary'
            }
        };
        var inputController = function ($scope, $route, $modalInstance, title, typeTodelete, message, btns, $sce, $translate, $document, Current, Realm, Notifications, ValidateForceDelete, $location) {
            $scope.title = title;
            $scope.type = type;
            $scope.typeTodelete = typeTodelete;
            $scope.label = $sce.trustAsHtml($translate.instant('delete-tenant-input-label', { tenantName: message }));
            $scope.tenant = { name: '' };
            $scope.btns = btns;
            $scope.isForceDelete = false;
            $scope.message = !$scope.isForceDelete ? $sce.trustAsHtml($translate.instant(`remove-${typeTodelete}-warning-message`, { variant })) : $sce.trustAsHtml($translate.instant(`force-remove-${typeTodelete}-warning-message`,  { variant }));
            $scope.OTPSession = false;
            $scope.getOTPLabel = $translate.instant('enter-otp-label');
            $scope.query = {
                id: Current.realm.realm
            };
            $scope.access = $route.current.locals.$scope.$parent.access;
            $(document).ready(function () {
                setTimeout(function () {
                    $("#deleteValue").focus();
                }, 100);
            });
            $scope.ok = function (event, elementType) {
                if ($scope.OTPSession) {
                    $scope.validateOTPAndRemove(event, elementType);
                } else {
                    const tenantNametoCheck = $('#deleteValue').val();
                    if (message === tenantNametoCheck) {
                        $scope.removeRealm(event, elementType);
                    } else {
                        $('#error_msg').text($translate.instant(`${typeTodelete}-name-mismatch`));
                    }
                }
            };
            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };
            $scope.removeRealm = function (event, elementType) {
                let stopFunction = createLoader(event, elementType);
                Realm.remove($scope.query, function () {
                    stopFunction && stopFunction();
                    if (!$scope.isForceDelete) {
                        $scope.updateRealms();
                        location.reload(true);
                    } else {
                        $scope.OTPSession = true;
                        $scope.btns.ok.label = $translate.instant('remove-workspace');
                    }
                }, function(response){
                    stopFunction && stopFunction();
                    Notifications.error(
                        $translate.instant(response.data.errorMessage)
                      );
                });
            };
            $scope.updateRealms = function () {
                //Current.realms = Realm.query();
                Notifications.success($translate.instant('realm-removed-msg'));
            };
            $scope.validateOTPAndRemove = function (event, elementType) {
                const otp = {
                    deleteReqCode: $('#otpValue').val()
                };
                let stopFunction = createLoader(event, elementType);
                ValidateForceDelete.validate({ id: Current.realm.realm }, otp, function () {
                    stopFunction && stopFunction()
                    $scope.updateRealms();
                    $location.url("/realms");
                }, function (err) {
                    stopFunction && stopFunction()
                    $scope.otpErrorMsg = err.data.errorMessage;
                });
            };
            $scope.changeForceDeleteState = function (val) {
                $scope.isForceDelete = val;
                if (!$scope.isForceDelete) {
                    $scope.message = $sce.trustAsHtml($translate.instant(`remove-${typeTodelete}-warning-message`, { variant }));
                    $scope.label = $sce.trustAsHtml($translate.instant('delete-tenant-input-label', { tenantName: message }));
                    $scope.query.forceDelete ? delete $scope.query.forceDelete : '';
                    $scope.btns.ok.label = $translate.instant('modal-yes-label');
                } else {
                    $scope.message = $sce.trustAsHtml($translate.instant(`force-remove-${typeTodelete}-warning-message`,  { variant }));
                    $scope.label = $sce.trustAsHtml($translate.instant('force-delete-tenant-input-label', { tenantName: message }));
                    $scope.query.forceDelete = true;
                    $scope.btns.ok.label = $translate.instant('modal-get-otp-label');
                }
            };
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
            });
        };
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/delete-tenant-modal.html',
            controller: inputController,
            backdrop: 'static',
            resolve: {
                title: function () {
                    return title;
                },
                typeTodelete: function () {
                    return typeTodelete;
                },
                message: function () {
                    return message;
                },
                btns: function () {
                    return btns;
                }
            }
        });
    }

    dialog.changeAccountCreator = function (name,user) {
        var title = $translate.instant('to-change-account-creator');
        var message = name;
        var user = user;
        var btns = {
            ok: {
                label: $translate.instant('modal-yes-label'),
                cssClass: 'btn btn-danger'
            },
            cancel: {
                label: $translate.instant('cancel'),
                cssClass: 'btn btn-secondary'
            }
        };
        var inputController = function ($scope, $modalInstance, title, message, btns, $sce, $translate, $document, Current, Realm, Notifications, AccountCreator,$route, $location) {
            $scope.title = title;
            $scope.label = $sce.trustAsHtml($translate.instant('change-account-user-label', { messageValue: message }));
            $scope.btns = btns;
            $scope.getOTPLabel = $translate.instant('enter-otp-label-send-to-mail');
            $scope.ok = function (event, elementType) {
                $scope.validateOTPAndChangeAccountCreator(event, elementType);
            };
            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };
            $scope.validateOTPAndChangeAccountCreator = function (event, elementType) {
                const otp = {
                    changeAccountCreatorReqCode: $('#otpValue').val()
                };
                let stopFunction = createLoader(event, elementType);
                AccountCreator.addAccountCreator({ realm: Current.realm.realm, userId: user.id }, otp, function () {
                    stopFunction && stopFunction();
                    Notifications.success($translate.instant('change-account-creator-success'));
                    location.reload();
                }, function (err) {
                    stopFunction && stopFunction()
                    $scope.otpErrorMsg = err.data.errorMessage;
                });
            };

        };
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/change-account-creator.html',
            controller: inputController,
            backdrop: 'static',
            resolve: {
                title: function () {
                    return title;
                },
                message: function () {
                    return message;
                },
                btns: function () {
                    return btns;
                }
            }
        });
    }

    dialog.asksForClientToResetPassword = function (name, desc, success) {
        var title = name;
        var message = desc;
        var btns = {
            ok: {
                label: $translate.instant('reset'),
                cssClass: 'btn btn-danger'
            },
            cancel: {
                label: $translate.instant('cancel'),
                cssClass: 'btn btn-secondary'
            }
        };
        var inputController = function ($scope, $modalInstance, title, message, btns, Client, $route, $sce, $translate, $document) {
            $scope.title = title;
            $scope.message = message;
            $scope.btns = btns;
            $scope.clients = [];
            $scope.selectedClient;
            $scope.initClientStreams = function () {
                Client.query({ realm: $route.current.params.realm }, function (data) {
                    $scope.clients = data;
                    $scope.clients = $scope.clients.filter(client => {
                        if (client.attributes["clientType"] && client.attributes["clientType"] === "workflows") {
                            return false;
                        }
                        return true;
                    })
                    if ($scope.clients.length !== 0) {
                        $scope.selectedClient = $scope.clients[0];
                        setTimeout(function () {
                            let clientOptions = document.getElementById("clientList").options;
                            clientOptions[1].setAttribute("selected", "selected");
                            clientOptions[0].remove();
                        }, 100);
                    }
                });
            };
            $scope.updateClient = function (client) {
                $scope.selectedClient = $scope.clients.find(data => data.clientId === client);
            };
            $scope.ok = function () {
                success($scope.selectedClient.clientId);
            };
            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
            });
        };
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/rp-client-select.html',
            controller: inputController,
            backdrop: 'static',
            resolve: {
                title: function () {
                    return title;
                },
                message: function () {
                    return message;
                },
                btns: function () {
                    return btns;
                }
            }
        });
    };

    dialog.brandingUpdate = function (updateType, name, desc, scope, success) {
        var inputController = function ($scope, $modalInstance) {
            $scope.title = name;
            $scope.message = desc;
            $scope.mainScope = scope;
            $scope.ok = function () {
                if (updateType === 'publish') {
                    success(scope, $scope.cancel);
                } else if (updateType === 'cancel' || updateType === 'reset') {
                    success(scope);
                }
            };
            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
            });
        };
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/branding-update-modal.html',
            controller: inputController,
            backdrop: 'static'
        });
    };

    return dialog;
});
module.service('CopyDialog', function ($modal) {
    var dialog = {};
    dialog.open = function (title, suggested, success) {
        var controller = function ($scope, $modalInstance, title) {
            $scope.title = title;
            $scope.name = { value: 'Copy of ' + suggested };
            $scope.ok = function () {
                $modalInstance.close();
                success($scope.name.value);
            };
            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
            });
        };
        $modal.open({
            templateUrl: resourceUrl + '/templates/kc-copy.html',
            controller: controller,
            backdrop: 'static',
            resolve: {
                title: function () {
                    return title;
                }
            }
        });
    };
    return dialog;
});

module.service('ShowCurlSnippetDialog', function ($modal, $translate) {
    var dialog = {};
    dialog.open = function (curlSnippet, appType, urlBody) {
        var controller = function ($scope, $modalInstance, $sce, curlSnippet) {
            if (typeof urlBody !== "undefined") {
                $scope.authServerUrl = urlBody.authServerUrl;
                delete urlBody["authServerUrl"];
                $scope.queryParams = urlBody;
            }
            $scope.curlSnippet = curlSnippet;
            $scope.appType = appType;
            $scope.showSteps = $scope.appType === 'browser_based' || $scope.appType === 'high_secure' || $scope.appType === 'iws';
            $scope.helpText = "";
            if (curlSnippet.includes('grant_type=password')) {
                $scope.helpText = $sce.trustAsHtml($translate.instant('rest-api-app-try-warn-msg'));
            } else if (curlSnippet.includes('grant_type=client_credentials')) {
                $scope.helpText = $translate.instant("machine-to-machine-try-warn-msg");
            } else if ($scope.appType === "high_secure" || $scope.appType === "browser_based" || $scope.appType === "iws") {
                $scope.helpText = $translate.instant('helpText');
            }
            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
            });
            $scope.tooltipTxt = $translate.instant("copy");
            $(document).ready(function () {
                $(".copy-paste-icon").click(function () {
                    $scope.tooltipTxt = $translate.instant("copied");
                    $(".copy-paste-icon").mouseleave(function () {
                        $scope.tooltipTxt = $translate.instant("copy");
                    });
                });
            });
            $scope.copyToClipboard = function (element) {
                var $temp = $("<textarea>");
                $("body").append($temp);
                $temp.val(element).select();
                document.execCommand("copy");
                $temp.remove();
            };
        };
        if (appType === 'browser_based' || appType === 'high_secure' || appType === 'iws') {
            $modal.open({
                templateUrl: resourceUrl + '/partials/modal/show-oidc-url.html',
                controller: controller,
                windowClass: 'curl-modal',
                backdrop: 'static',
                resolve: {
                    curlSnippet: function () {
                        return curlSnippet;
                    }
                }
            });
        } else {
            $modal.open({
                templateUrl: resourceUrl + '/partials/modal/show-snippet.html',
                controller: controller,
                windowClass: 'curl-modal',
                backdrop: 'static',
                resolve: {
                    curlSnippet: function () {
                        return curlSnippet;
                    }
                }
            });
        }
    };

    dialog.showConstraints = function (constraints) {
        var controller = function ($scope, $modalInstance, constraints) {
            $scope.constraints = constraints;

            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
            });

            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };
        };

        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/show-constraints.html',
            controller: controller,
            windowClass: 'constraint-modal',
            backdrop: 'static',
            resolve: {
                constraints: function () {
                    return constraints;
                }
            }
        });
    };
    return dialog;
});


module.service('MarketplaceInstallTemplate', function ($modal) {
    var dialog = {};
    dialog.open = function (title, properties, realm, success) {
        var controller = function ($scope, $modalInstance, title) {
            $scope.realm = realm;
            $scope.title = title;
            $scope.config = { props: properties };
            $scope.configType = {};
            const supportedAppsKey = "supported_apps";
            $scope.$watch('configType', function () {
                if ($scope.configType[supportedAppsKey] && $scope.configType[supportedAppsKey][0] === '') {
                    $scope.configType[supportedAppsKey] = [];
                    return;
                }
                if (configCheckNullAuthentication($scope.configType, $scope.config.props)) {
                    $scope.changed = $scope.configType[supportedAppsKey] && $scope.configType[supportedAppsKey].length === 0 ? false : true;
                } else {
                    $scope.changed = false;
                }
            }, true);
            $scope.ok = function () {
                if ($scope.configType[supportedAppsKey] && $scope.configType[supportedAppsKey].length === 0) {
                    return;
                }
                $modalInstance.close();
                success($scope.configType);
            };
            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
            });
        };
        $modal.open({
            templateUrl: resourceUrl + '/templates/kc-copy.html',
            controller: controller,
            backdrop: 'static',
            resolve: {
                title: function () {
                    return title;
                }
            }
        });
    };
    return dialog;
});
module.service('CreateCustomTemplate', function ($modal) {
    var dialog = {};
    dialog.open = function (title, flowChosen, suggested, success) {
        var controller = function ($scope, $modalInstance, title) {
            $scope.title = title;
            $scope.flowChosen = flowChosen;
            $scope.defaultFlowType = {};
            if ($scope.flowChosen === 'login') {
                $scope.defaultFlowType.value = 'browser_flow';
            }
            $scope.name = { value: suggested };
            $scope.$watch('name', function () {
                $scope.changed = $scope.name.value && $scope.name.value.length > 0;
            }, true);
            $scope.ok = function () {
                $modalInstance.close();
                success($scope.name.value, $scope.defaultFlowType.value);
            };
            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
            });
        };
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/create-template-modal.html',
            controller: controller,
            backdrop: 'static',
            resolve: {
                title: function () {
                    return title;
                }
            }
        });
    };
    return dialog;
});

module.service('PasswordRegDialog', function ($modal) {
    var dialog = {};
    dialog.open = function (title, suggested, success) {
        var controller = function ($scope, $modalInstance, title) {
            $scope.title = title;
            $scope.newPasswd = { value: "" };
            $scope.confirmNewPasswd = { value: "" };
            $scope.error = "";

            $scope.$watch('newPasswd', function () {
                $scope.pwdChanged = $scope.newPasswd.value && $scope.newPasswd.value.length > 0;
            }, true);
            $scope.$watch('confirmNewPasswd', function () {
                $scope.cnfPwdChanged = $scope.confirmNewPasswd.value && $scope.confirmNewPasswd.value.length > 0;
            }, true);
            $scope.ok = function () {
                if (passwordAndConfirmPasswordMatches()) {
                    $modalInstance.close();
                    success($scope.newPasswd.value, $scope.confirmNewPasswd.value);
                } else {
                    $scope.error = "New Password and Confirm Password doesn't matches or not allowed to be empty.";
                }
            };
            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
            });

            $scope.hideShowNewPassword = function () {
                if ($('#show_hide_new_password input').attr("type") === "text") {
                    $('#show_hide_new_password input').attr('type', 'password');
                    $('#show_hide_new_password .hide_show span').addClass("hidden_outlined");
                    $('#show_hide_new_password .hide_show span').removeClass("visible_outlined");
                } else if ($('#show_hide_new_password input').attr("type") === "password") {
                    $('#show_hide_new_password input').attr('type', 'text');
                    $('#show_hide_new_password .hide_show span').removeClass("hidden_outlined");
                    $('#show_hide_new_password .hide_show span').addClass("visible_outlined");
                }
            };

            $scope.hideShowConfirmPassword = function () {
                if ($('#show_hide_confirm_password input').attr("type") === "text") {
                    $('#show_hide_confirm_password input').attr('type', 'password');
                    $('#show_hide_confirm_password .hide_show span').addClass("hidden_outlined");
                    $('#show_hide_confirm_password .hide_show span').removeClass("visible_outlined");
                } else if ($('#show_hide_confirm_password input').attr("type") === "password") {
                    $('#show_hide_confirm_password input').attr('type', 'text');
                    $('#show_hide_confirm_password .hide_show span').removeClass("hidden_outlined");
                    $('#show_hide_confirm_password .hide_show span').addClass("visible_outlined");
                }
            };


            //to check password & confirm Password matches
            function passwordAndConfirmPasswordMatches() {
                return $scope.newPasswd.value === $scope.confirmNewPasswd.value;
            }
        };

        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/password-reg.html',
            controller: controller,
            backdrop: 'static',
            resolve: {
                title: function () {
                    return title;
                }
            }
        });
    };
    return dialog;
});

module.service('RealmPolicyCreateDialog', function ($modal) {
    var dialog = {};
    dialog.open = function (title, suggested, success) {
        var controller = function ($scope, $modalInstance, title) {
            $scope.title = title;
            $scope.policyName = { value: suggested };
            $scope.policyType = { value: "password" };
            $scope.$watch('policyName', function () {
                $scope.changed = $scope.policyName.value && $scope.policyName.value.length > 0;
            }, true);
            $scope.ok = function () {
                $modalInstance.close();
                if($scope.$parent.isCibaEnabled)
                {
                    $scope.policyType = { value: "ciba" };
                }
                success($scope.policyName.value, $scope.policyType.value);
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
            templateUrl: resourceUrl + '/templates/kc-policy-create.html',
            controller: controller,
            backdrop: 'static',
            resolve: {
                title: function () {
                    return title;
                }
            }
        });
    };
    return dialog;
});


module.service('CreateInputModal', function ($modal) {
    var dialog = {};
    dialog.open = function (title, maxInputLength, success, modalOpened) {
        var controller = function ($scope, $modalInstance, title) {
            $scope.title = title;
            $scope.maxInputLength = maxInputLength;
            $scope.input = { value : '' };
            $scope.$watch('input', function () {
                $scope.changed = $scope.input.value && $scope.input.value.length > 0;
            }, true);
            $scope.ok = function () {
                success($scope.input.value);
                $modalInstance.close();
            };
            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
            });
            modalOpened();
            $(document).ready(function () {
                setTimeout(function () {
                    $("#name").focus();
                }, 100);
            });
        };
        $modal.open({
            templateUrl: resourceUrl + '/templates/kc-marketplace-category-create.html',
            controller: controller,
            backdrop: 'static',
            resolve: {
                title: function () {
                    return title;
                }
            }
        });
    };
    return dialog;
});

module.service('CibaTemplateCreateDialog', function ($modal) {
    var dialog = {};
    dialog.open = function (realm, actionPolicies, cibaPolicies, success) {
        var controller = function ($scope, $modalInstance, $translate, Actions, ActionsList, SubWorkflowsList, UnregisteredRequiredActions, Notifications) {
            $scope.realm = realm;
            $scope.isTemplateCreated = false;
            $scope.actionPolicies = actionPolicies;
            $scope.cibaPolicies = cibaPolicies;
            $scope.resourceUrl = resourceUrl;
            $scope.action = {
                alias: "",
                actionType : "verification"
            };
            $scope.actionId = "";
            $scope.actionrep = {};
            $scope.unregisteredRequiredActions = [];
            $scope.showTopBar = false;

            $scope.fetchActions = function () {
                UnregisteredRequiredActions.query(
                    { realm: $scope.realm.realm, action: $scope.actionId },
                    function (data) {
                      $scope.unregisteredRequiredActions = data;
                      $scope.isTemplateCreated = true;
                    },
                    function (data) {
                      Notifications.error($translate.instant("ciba-failed-to-fetch-required-action"));
                    },
                );
            }
            
            $scope.create = function () {
                const templateName = $('#create-template-input').val();
                if(templateName){
                    $scope.action.alias = templateName;
                    Actions.save({ realm: $scope.realm.realm }, $scope.action,
                        function (response) {
                            $scope.actionId = response.id;
                            $scope.actionrep = response;
                            $scope.fetchActions();
                            ActionsList.query({realm: $scope.realm.realm},
                                function (res) {
                                    success(res, $scope.actionrep)
                                }
                            )
                        },
                        function (error) {
                            Notifications.error($translate.instant("ciba-template-already-exist"));
                        }
                    )
                }else{
                    Notifications.error($translate.instant("ciba-template-name-empty"));
                }

            }
            
            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };
    
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
            });
        };

        $modal.open({
            templateUrl: resourceUrl + '/templates/kc-create-ciba-template-modal.html',
            controller: controller,
            backdrop: 'static',
            size: 'lg',
            windowClass: 'window-index-xl',
            windowTopClass: 'window-index-xl',
            backdropClass: 'window-index-xl'
        });
    };

    return dialog;
});

module.service('CibaRuleCreateDialog', function ($modal) {
    var dialog = {};
    dialog.open = function (realm, serverInfo, success) {
        var controller = function ($scope, $modalInstance, $translate, Policies, CibaPolicies, Notifications) {
            $scope.realm = realm;
            $scope.isRuleCreated = false;
            $scope.policies = {};
            $scope.serverInfo = serverInfo;
            $scope.policyRep = {
                policyName: '',
                policyType: 'ciba',
                policyValue: "",
                realmPolicy: false,
            };
            
            $scope.create = function () {
                const ruleName = $('#create-rule-input').val();
                if(ruleName){
                    $scope.policyRep.policyName = ruleName;
                    Policies.update({ realm: $scope.realm.realm }, $scope.policyRep,
                        function (response) {
                            $scope.policies = response;
                            $scope.isRuleCreated = true;
                            CibaPolicies.query({realm: $scope.realm.realm}, 
                                function (res) {
                                    success(res, $scope.policies);
                                }
                            )
                        },
                        function (error) {
                            Notifications.error($translate.instant("ciba-rule-already-exist"));
                        }
                    )
                }else{
                    Notifications.error($translate.instant("ciba-rule-name-empty"));
                }
            }
            
            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };
    
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
            });
        };

        $modal.open({
            templateUrl: resourceUrl + '/templates/kc-create-ciba-rule-modal.html',
            controller: controller,
            backdrop: 'static',
            size: 'lg',
            bindToController: true,
        });
    };

    return dialog;
});

module.service('WorkflowPreviewDialog', function ($modal) {
    var dialog = {};
    dialog.open = function (previewTemplateFlow, selectedTemplateName, isSubWorkflow) {
        var controller = function ($scope, $q, $translate, $anchorScroll, $modalInstance) {
            $scope.templateFlows = previewTemplateFlow;
            $scope.isSubWorkflow = isSubWorkflow;
            $scope.currentIndex = 0;
            $scope.isRTL = locale == 'ar';
            $scope.templatesLength = $scope.templateFlows.length;
            $scope.childTemplatesLength = 0;
            $scope.selectedTemplateName = selectedTemplateName.alias;
            $scope.selectedTransactionFlow = $translate.instant($scope.selectedTemplateId);
           
            if ($scope.templateFlows.length > 0) {
                $scope.childIndex = $scope.templateFlows[0].subFlow ? 0 : -1;
                $scope.selectedTemplateId = $scope.templateFlows[$scope.currentIndex].id;
                $scope.currentTemplate =  $scope.templateFlows[$scope.currentIndex]
            }

            $scope.templateFlows.forEach(function(template) {
                var img = new Image();
                var deferred = $q.defer();
                img.onload = function() {
                    deferred.resolve();
                };
                if(template.subFlow){
                    template.subFlow.forEach((subTemplate) => {
                        img.src = subTemplate.gifUrl
                    });
                }else{
                    img.src = template.gifUrl;
                }
            });
            
            function isElementInViewport(el) {
                var rect = el[0].getBoundingClientRect();
                var parentRect = el[0].parentNode.getBoundingClientRect();
                return (
                    rect.top >= parentRect.top &&
                    rect.left >= parentRect.left &&
                    rect.bottom <= parentRect.bottom &&
                    rect.right <= parentRect.right
                );
            }
            
            $scope.scrollToView = function (id) {
                var targetElement = angular.element(document.getElementById(id));
                if (!isElementInViewport(targetElement)) {
                    $anchorScroll(id);
                }
            }

            $scope.setCurrentTemplate = function (template, templateIndex, childIndex, parentTemplate) {
                $scope.currentTemplate = parentTemplate;
                $scope.selectedTemplateId = template.id;
                $scope.selectedTransactionFlow =  $translate.instant($scope.selectedTemplateId);
                $scope.currentIndex = templateIndex;
                $scope.childIndex = childIndex;
                $scope.scrollToView(`${!$scope.currentTemplate.subFlow ? "sidebar-" : "sidebar-sub-" }`+$scope.selectedTemplateId);
            }


            $scope.changePrev = function () {
                if($scope.templateFlows[$scope.currentIndex].subFlow){
                    $scope.childTemplatesLength = $scope.templateFlows[$scope.currentIndex].subFlow.length;
                    if($scope.childIndex == 0){
                        $scope.currentIndex = $scope.currentIndex - 1;
                        if($scope.templateFlows[$scope.currentIndex].subFlow){
                            $scope.childTemplatesLength = $scope.templateFlows[$scope.currentIndex].subFlow.length;
                            $scope.childIndex = $scope.childTemplatesLength -1;
                            $scope.selectedTemplateId = $scope.templateFlows[$scope.currentIndex].subFlow[$scope.childIndex].id;
                        }else{
                            $scope.childIndex = -1;
                            $scope.selectedTemplateId = $scope.templateFlows[$scope.currentIndex].id;
                        }
                    }else if($scope.childIndex < $scope.childTemplatesLength){
                        $scope.childIndex = $scope.childIndex -1;
                        $scope.selectedTemplateId = $scope.templateFlows[$scope.currentIndex].subFlow[$scope.childIndex].id;
                    }
                }else{
                    $scope.childIndex = -1;
                    $scope.currentIndex = $scope.currentIndex - 1;
                    if($scope.templateFlows[$scope.currentIndex].subFlow){
                        $scope.childTemplatesLength = $scope.templateFlows[$scope.currentIndex].subFlow.length;
                        $scope.childIndex = $scope.childTemplatesLength -1;
                        $scope.selectedTemplateId = $scope.templateFlows[$scope.currentIndex].subFlow[$scope.childIndex].id;
                    }else{
                        $scope.selectedTemplateId = $scope.templateFlows[$scope.currentIndex].id;
                    }
                }
                $scope.selectedTransactionFlow = $translate.instant($scope.selectedTemplateId);
                $scope.currentTemplate =  $scope.templateFlows[$scope.currentIndex]
                $scope.scrollToView(`${$scope.childIndex == -1 ? "sidebar-" : "sidebar-sub-" }`+$scope.selectedTemplateId);
            }

            $scope.changeNext = function () {
                if($scope.templateFlows[$scope.currentIndex].subFlow){
                    $scope.childTemplatesLength = $scope.templateFlows[$scope.currentIndex].subFlow.length;
                    if($scope.childIndex == $scope.childTemplatesLength-1){
                        $scope.currentIndex = $scope.currentIndex + 1;
                        if($scope.templateFlows[$scope.currentIndex].subFlow){
                            $scope.childIndex = 0;
                            $scope.selectedTemplateId = $scope.templateFlows[$scope.currentIndex].subFlow[$scope.childIndex].id;
                        }else{
                            $scope.childIndex = -1;
                            $scope.selectedTemplateId = $scope.templateFlows[$scope.currentIndex].id;
                        }
                    }else if($scope.childIndex < $scope.childTemplatesLength-1){
                        $scope.childIndex = $scope.childIndex + 1;
                        $scope.selectedTemplateId = $scope.templateFlows[$scope.currentIndex].subFlow[$scope.childIndex].id;
                    }
                }else{
                    $scope.childIndex = -1;
                    $scope.currentIndex = $scope.currentIndex + 1;
                    if($scope.templateFlows[$scope.currentIndex].subFlow){
                        $scope.childIndex = 0;
                        $scope.selectedTemplateId = $scope.templateFlows[$scope.currentIndex].subFlow[$scope.childIndex].id;
                    }else{
                        $scope.selectedTemplateId = $scope.templateFlows[$scope.currentIndex].id;
                    }
                }
                $scope.selectedTransactionFlow = $translate.instant($scope.selectedTemplateId);
                $scope.currentTemplate =  $scope.templateFlows[$scope.currentIndex]
                $scope.scrollToView(`${$scope.childIndex == -1 ? "sidebar-" : "sidebar-sub-" }`+$scope.selectedTemplateId);
            }

            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };
    
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
            });
        };

        $modal.open({
            templateUrl: resourceUrl + '/templates/kc-template-preview-modal.html',
            controller: controller,
            backdrop: 'static',
            size: 'lg',
        });
    };

    return dialog;
});

module.service('ActionVariablesDialog', function ($modal) {
    var dialog = {};
    dialog.open = function (title,realm, actionrep, success, modalOpened) {
        var controller = function (
          $scope,
          $translate,
          $modalInstance,
          ActionVariables,
          Notifications
        ) {
          let variables =
            actionrep.actionVariables != undefined
              ? actionrep.actionVariables
              : "";
          $scope.saveBtnDisabled = true;
          $scope.actionVariablesCopy = angular.copy(variables);
          $scope.title = title;
          $scope.actionVariables = variables;

          $scope.cancel = function () {
            $modalInstance.dismiss("cancel");
          };

          $scope.$on("$routeChangeStart", function () {
            $modalInstance.close();
          });

          $scope.save = function () {
            var element = $("#action-variables");
            var value = element.val();

            try {
              if(value.trim().length > 0){
                  JSON.parse(value);
              }
            } catch (err) {
              Notifications.error(
                $translate.instant("invalid-action-variables")
              );
              return;
            }

            ActionVariables.save(
              {
                realm: realm,
                actionId: actionrep.actionId,
                actionVariables: value,
              },
              function (data) {
                Notifications.success(
                  $translate.instant("action-variables-updated")
                );
                $modalInstance.close();
                success(data);
              },
              function () {
                $scope.saveBtnDisabled = true;
                Notifications.error(
                  $translate.instant("invalid-action-variables")
                );
              }
            );
          };

          $scope.enableButton = function () {
            if(!angular.equals($scope.actionVariablesCopy, $("#action-variables").val())){
                $scope.saveBtnDisabled = false;
            }else{
                $scope.saveBtnDisabled = true;
            }
          };

          modalOpened();
        };

        $modal.open({
            templateUrl: resourceUrl + '/templates/kc-action-variables-preview.html',
            controller: controller,
            backdrop: 'static',
            size: 'lg',
            bindToController: true,
        });
    };

    return dialog;
});

module.service('CodePreviewDialog', function ($modal) {
    var dialog = {};
    dialog.open = function (title, code) {
        var controller = function ($scope, $translate, $modalInstance) {
            $scope.title = title;
            $scope.tooltipTxt = $translate.instant("copy");
            $scope.code = JSON.stringify(code, null, '\t');


            $scope.copyToClipboard = (code) => {
                var $temp = $("<input>");
                $("body").append($temp);
                $temp.val(code).select();
                document.execCommand("copy");
                $scope.tooltipTxt = $translate.instant("copied");
                setInterval(()=> $scope.tooltipTxt = $translate.instant("copy"), 3000)
                $temp.remove();
            };
            
            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };
    
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
            });
        };

        $modal.open({
            templateUrl: resourceUrl + '/templates/kc-code-preview.html',
            controller: controller,
            backdrop: 'static',
            size: 'lg',
            bindToController: true,
        });
    };

    return dialog;
});

module.service('DeviceApplicationDialog', function ($modal) {
    var dialog = {};
    dialog.open = function (realm, flow, app, type, itemsPerPage, success) {
        var controller = function ($scope, $modalInstance, DeviceApplication, DeviceApplicationRegister, GetDeviceApplications, $translate, Notifications) {
            $scope.realm = realm;
            $scope.flow = flow;
            $scope.app = app;
            $scope.appCopy = angular.copy(app);
            $scope.enableBtn = false;
            $scope.platformOption = type == "mobile" ? ['ios', 'android'] : ['windows', 'macos'];
            $scope.deviceApplication = [];
            $scope.appUpdateUrl = $scope.flow == 'edit' ? authUrl + '/realms/api/app-updates/' + $scope.realm.id + '/application/' + $scope.app.appId : '' ;

            $scope.$watch('app', function () {
                if(!angular.equals($scope.appCopy, $scope.app) && $scope.app.appName && $scope.app.appVersion && $scope.app.appUrl && $scope.app.appPlatform){
                    $scope.enableBtn = true;
                }else{
                    $scope.enableBtn = false;
                }
            }, true)

            $scope.copyToClipboard = function (element) {
                $scope.tooltipTxt = $translate.instant("copied");
                var $temp = $("<input>");
                $("body").append($temp);
                const text = element;
                $temp.val(text.trim()).select();
                document.execCommand("copy");
                $temp.remove();
                setTimeout(() =>$scope.tooltipTxt = $translate.instant("copy") , 1000)
              };

            $scope.fetchAllDeviceApp = function () {
                GetDeviceApplications.get({ realmId: $scope.realm.id, appType: type, first: 0, max: itemsPerPage , appName: '', platform: '' }, function (data) {
                    $scope.deviceApplication = data;
                    success($scope.deviceApplication)
                }, function () {
                    Notifications.error($translate.instant("device-app-fetch-error"))
                })
            }
            
            $scope.save = function () {
                if($scope.flow == 'create'){
                    $scope.app['appType'] = type;
                    DeviceApplicationRegister.save({realmId: $scope.realm.id}, $scope.app, function() {
                        Notifications.success($translate.instant("device-app-created"));
                        $scope.fetchAllDeviceApp();
                        $scope.cancel();
                    }, function (err) {
                        Notifications.error(err.data.body);
                        $scope.cancel();
                    })
                }else if($scope.flow == 'edit'){
                    DeviceApplication.update({realmId: $scope.realm.id, appId: $scope.app.appId}, $scope.app, function() {
                        Notifications.success($translate.instant("device-app-updated"));
                        $scope.fetchAllDeviceApp();
                        $scope.cancel();
                    }, function (err) {
                        Notifications.error($translate.instant(err.data.body));
                    })
                }
            }

            $scope.delete = function () {
                DeviceApplication.remove({realmId: $scope.realm.id, appId: $scope.app.appId}, function() {
                    Notifications.success($translate.instant("device-app-deleted"));
                    $scope.fetchAllDeviceApp();
                    $scope.cancel();
                }, function () {
                    Notifications.error($translate.instant("device-app-not-deleted"));
                })
            }
            
            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };
    
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
            });
        };

        $modal.open({
            templateUrl: resourceUrl + '/templates/kc-device-application-modal.html',
            controller: controller,
            backdrop: 'static',
            size: 'md',
            bindToController: true,
        });
    };

    return dialog;
});

module.service('RealmDirectorySyncCreateDialog', function ($modal) {
    var dialog = {};
    dialog.open = function (title, providerDetails, success, currentDetails) {
        var controller = function ($scope, $modalInstance, title, currentDetails, providerDetails) {
            $scope.title = title;
            $scope.providerId = providerDetails?.providerId;
            if (currentDetails) {
                $scope.update = true;
                $scope.connectionDetails = angular.copy(currentDetails);
            } else {
                $scope.connectionDetails = {
                    name: '',
                    enrollmentOption: 'sendEnrollmentLink',
                    config: {
                        mapperType: 'email'
                    }
                };
            }
            $scope.$watch('connectionDetails', function () {
                if ($scope.update) {
                    if(providerDetails?.providerId === "duo_ws") {
                        if (!angular.equals($scope.connectionDetails, currentDetails) && $scope.connectionDetails.name && $scope.connectionDetails.name.length > 0 && $scope.connectionDetails.config.apiHostName && $scope.connectionDetails.config.apiHostName.length > 0 && $scope.connectionDetails.config.integrationKey && $scope.connectionDetails.config.integrationKey.length > 0 && $scope.connectionDetails.config.secretKey && $scope.connectionDetails.config.secretKey.length > 0) {
                            $scope.changed = true;
                        } else {
                            $scope.changed = false;
                        }
                    } else if(providerDetails?.providerId === "google_ws") {
                        if (!angular.equals($scope.connectionDetails, currentDetails) && $scope.connectionDetails.name && $scope.connectionDetails.name.length > 0 && $scope.connectionDetails.config.GWS_CLIENT_ID && $scope.connectionDetails.config.GWS_CLIENT_ID.length > 0 && $scope.connectionDetails.config.GWS_CLIENT_SECRET && $scope.connectionDetails.config.GWS_CLIENT_SECRET.length > 0) {
                            $scope.changed = true;
                        } else {
                            $scope.changed = false;
                        }
                    } else if(providerDetails?.providerId === "okta_ws") {
                        if (!angular.equals($scope.connectionDetails, currentDetails) && $scope.connectionDetails.name && $scope.connectionDetails.name.length > 0 && $scope.connectionDetails.config.domainName && $scope.connectionDetails.config.domainName.length > 0 && $scope.connectionDetails.config.apiToken && $scope.connectionDetails.config.apiToken.length > 0) {
                            $scope.changed = true;
                        } else {
                            $scope.changed = false;
                        }
                    } else {
                        if (!angular.equals($scope.connectionDetails, currentDetails) && $scope.connectionDetails.name && $scope.connectionDetails.name.length > 0) {
                            $scope.changed = true;
                        } else {
                            $scope.changed = false;
                        }
                    }
                }
                if (!$scope.update) {
                    if(providerDetails?.providerId === "duo_ws") {
                        if ($scope.connectionDetails.name && $scope.connectionDetails.name.length > 0 && $scope.connectionDetails.config.apiHostName && $scope.connectionDetails.config.apiHostName.length > 0 && $scope.connectionDetails.config.integrationKey && $scope.connectionDetails.config.integrationKey.length > 0 && $scope.connectionDetails.config.secretKey && $scope.connectionDetails.config.secretKey.length > 0) {
                            $scope.changed = true;
                        } else {
                            $scope.changed = false;
                        }
                    } else if(providerDetails?.providerId === "google_ws") {
                        if ($scope.connectionDetails.name && $scope.connectionDetails.name.length > 0 && $scope.connectionDetails.config.GWS_CLIENT_ID && $scope.connectionDetails.config.GWS_CLIENT_ID.length > 0 && $scope.connectionDetails.config.GWS_CLIENT_SECRET && $scope.connectionDetails.config.GWS_CLIENT_SECRET.length > 0) {
                            $scope.changed = true;
                        } else {
                            $scope.changed = false;
                        }
                    } else if(providerDetails?.providerId === "okta_ws") {
                        if ($scope.connectionDetails.name && $scope.connectionDetails.name.length > 0 && $scope.connectionDetails.config.domainName && $scope.connectionDetails.config.domainName.length > 0 && $scope.connectionDetails.config.apiToken && $scope.connectionDetails.config.apiToken.length > 0) {
                            $scope.changed = true;
                        } else {
                            $scope.changed = false;
                        }
                    } else {
                        if ($scope.connectionDetails.name && $scope.connectionDetails.name.length > 0) {
                            $scope.changed = true;
                        } else {
                            $scope.changed = false;
                        }
                    }
                }

            }, true);
            $scope.ok = function () {
                $modalInstance.close();
                success({ connectionName: $scope.connectionDetails.name, connectionType: $scope.connectionDetails.enrollmentOption, syncConfig: $scope.connectionDetails.config });
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
            templateUrl: resourceUrl + '/templates/kc-directory-sync-create.html',
            controller: controller,
            backdrop: 'static',
            resolve: {
                title: function () {
                    return title;
                },
                providerDetails : function() {
                    return providerDetails;
                },
                currentDetails: function () {
                    return currentDetails;
                }
            }
        });
    };
    return dialog;
});

module.service('RealmBrandingCreateDialog', function ($modal) {
    var dialog = {};
    dialog.open = function (title, suggested, success) {
        var controller = function ($scope, $modalInstance, title) {
            $scope.title = title;
            $scope.brandName = { value: suggested };
            $scope.$watch('brandName', function () {
                $scope.changed = $scope.brandName.value && $scope.brandName.value.length > 0;
            }, true);
            $scope.ok = function () {
                $modalInstance.close();
                success($scope.brandName.value);
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
            templateUrl: resourceUrl + '/templates/kc-branding-create.html',
            controller: controller,
            backdrop: 'static',
            resolve: {
                title: function () {
                    return title;
                }
            }
        });
    };
    return dialog;
});

module.service('AdditonalClaimsDialog', function ($modal) {
    var dialog = {};
    dialog.open = function (title, claims, success) {
        var controller = function ($scope, $modalInstance, title, claims) {
            $scope.title = title;
            $scope.claims = JSON.stringify(claims, null, 4)
            $scope.cancel = function () {
                success(true);
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
        };
        $modal.open({
            templateUrl: resourceUrl + '/templates/kc-additional-claims.html',
            controller: controller,
            backdrop: 'static',
            resolve: {
                title: function () {
                    return title.replace("bo-","");
                },
                claims: function () {
                    return claims;
                }
            },
            size: 'lg'
        });
    };
    return dialog;
});

module.service('RealmTenantCreateDialog', function ($modal) {
    var dialog = {};
    dialog.open = function (title,showMode,modes, suggested, success) {
        var controller = function ($scope, $modalInstance, title,showMode,modes, Notifications, $translate) {
            $scope.isAddressInvalid = true;
            $scope.countryList = countryList;
            $scope.countryDetails = {};
            $scope.stateList = [];
            $scope.title = title;
            $scope.showMode =showMode;
            $scope.modes = modes;
            $scope.logoUpdated = false;
            $scope.validLogo = true;
            $scope.realm = {
                enabled: true,
                realm: "",
                id: "",
                mode:"wiam",
                subscriptionType: "wiam_users",
                realmLogo: null,
                "accountCreator": {
                    "email": ""
                },
                displayName: ""
            };

            if(modes.length === 1 && modes[0] === 'bo' ){
                $scope.realm.mode = 'bo';
                $scope.showMode =false;
            }

            $scope.stage = 1;
            $scope.error = "";
            $scope.emailError = "";
            $scope.tenantCreating = $translate.instant("loader-creating");

            $scope.$watch('realm.displayName', function () {
                $scope.error = "";
            }, true);

            $scope.$watch('realm.accountCreator.email', function () {
                $scope.emailError = "";
            }, true);

            $scope.$watch('realm.billingAddress.country', function () {
                $scope.countryDetails = getCountryDetails($scope.realm.billingAddress?.country);
                const subLabels = $scope.countryDetails.sub_labels;
                const subKeys = $scope.countryDetails.sub_keys;
                if (subLabels && subLabels.length > 0) $scope.stateList = subLabels;
                else if (subKeys && subLabels.length > 0) $scope.stateList = subKeys;
                else {
                    $scope.stateList = [];
                    $scope.realm.billingAddress?.state && delete $scope.realm.billingAddress.state;
                }
                $scope.validateAddress();
            }, true);

            $scope.$watch('realm.billingAddress.postal_code', function () {
                const zipReg = $scope.countryDetails.zip ? new RegExp($scope.countryDetails.zip) : "";
                const postalCode = $scope.realm.billingAddress?.postal_code;
                if (zipReg && postalCode && !zipReg.test(postalCode)) {
                    $scope.pinError = "invalid-postal-code";
                } else {
                    $scope.pinError = "";
                }
            }, true);

            $scope.validateAddress = function () {
                $scope.isAddressInvalid = true;
                const values = [];
                Object.keys($scope.realm.billingAddress || {}).forEach((key)=>{
                    if(key !== 'line2'){
                        values.push($scope.realm.billingAddress[key])
                    }
                })
                const stateLen = $scope.stateList?.length;
                if ((stateLen && values.length === 5) || (!stateLen && values.length === 4)) {
                    let isValid = true
                    values.forEach((value) => {
                        if (!value) isValid = false;
                    })
                    if (isValid) $scope.isAddressInvalid = false
                }
            };

            $scope.tenantLogoHelptxt = $translate.instant('tenant-logo-helptxt');
            function processLogo(uploader) {
                if (uploader.files && uploader.files[0]) {
                    $('#realmTenantLogo').removeAttr('src');
                    $('#realmTenantLogo').attr('src',
                        window.URL.createObjectURL(uploader.files[0]));
                }
                getBase64(uploader.files[0]).then(
                    data => {
                        $scope.$apply(
                            function () {
                                $scope.realm.realmLogo = data;
                                $scope.logoUpdated = true;
                            }
                        );
                    }
                );
            }
            function checkDimension(uploader) {
                var img = new Image();
                img.src = window.URL.createObjectURL(uploader.files[0]);
                var boolVal = true;
                if (!(Math.round(uploader.files[0].size / 1024) <= 4000)) {
                    boolVal = false;
                    return boolVal;
                }
                img.onload = function () {
                    if (!(img.naturalWidth <= 1024 && img.naturalHeight <= 1024)) {
                        boolVal = false;
                        return boolVal;
                    }
                };
                return boolVal;
            }
            $scope.changeLogo = function () {
                const updateTenantLogo = document.querySelector("#updateTenantLogo");
                updateTenantLogo.value = "";
                updateTenantLogo.addEventListener("change", function () {
                    processLogo(this);
                    $scope.validLogo = checkDimension(this);
                });
                updateTenantLogo.click();
            };

            $scope.reset = function () {
                $scope.realm.realmLogo = "";
                $scope.logoUpdated = false;
            };

            $scope.ok = function () {
                $scope.stage = 2;
                var realmName = $scope.realm.displayName;
                $scope.error = "";
                if (!$scope.validLogo) {
                    Notifications.error($translate.instant('logo-error-msg'));
                    $scope.reset();
                    $scope.stage = 1;
                    return;
                }
                let validationResult = validateTenantName(realmName);
                if (!validationResult.valid) {
                    $scope.error = validationResult.err;
                    $scope.stage = 1;
                } else if (!validateEmail($scope.realm.accountCreator.email)) {
                    $scope.emailError = "Enter a valid email";
                    $scope.stage = 1;
                } else {
                    $scope.realm.displayName = $scope.realm.displayName.trim();
                    $scope.realm.realm = $scope.realm.displayName.trim().toLowerCase().replace(/\s+/g, '-').trim();
                    success($scope.realm, $scope);
                }
            };
            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
            });
        };
        $modal.open({
            templateUrl: resourceUrl + '/templates/kc-tenant-create.html',
            controller: controller,
            backdrop: 'static',
            size: 'lg',
            resolve: {
                title: function () {
                    return title;
                },
                showMode: function(){
                    return showMode;
                },
                modes: function(){
                    return modes;
                }
            }
        });
    };
    return dialog;
});

module.service('ConfirmExitDialog', function ($modal) {
    var dialog = {};
    dialog.open = function (success) {
        var controller = function ($scope, $modalInstance) {
            $scope.ok = function () {
                success();
            };
            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
            });
        };
        $modal.open({
            templateUrl: resourceUrl + '/templates/kc-confirm-exit.html',
            controller: controller,
            backdrop: 'static'
        });
    };
    return dialog;
});

module.service("OnboardingDoneDialog", function ($modal) {
    var dialog = {};
    dialog.open = function (title) {
        var controller = function ($scope, $modalInstance, IntroManagement, Current, IntroJs) {
            $scope.title = title;
            $scope.introTours = getToursList(IntroJs.toursList, Current.realm.realm, title);
            $scope.ok = function () {
                $modalInstance.close();
            };
            $scope.modalInstance = $modalInstance;
            $scope.exitIntro = IntroJs.exit.bind(IntroJs);
            $scope.cancel = function () {
                $modalInstance.dismiss("cancel");
            };
            $scope.$on("$routeChangeStart", function () {
                $modalInstance ? $modalInstance.close() : "";
            });
            $scope.redirectToTour = IntroManagement.setInfoRedirect.bind(IntroManagement);
        };
        $modal.open({
            templateUrl: resourceUrl + "/templates/kc-onboarding-complete.html",
            controller: controller,
            windowClass: "constraint-modal",
            backdrop: "static"
        });
    };
    return dialog;
});

module.service("OnboardingToursDialog", function ($modal) {
    var dialog = {};
    dialog.open = function () {
        var controller = function (
            $scope,
            $modalInstance,
            $translate,
            IntroManagement,
            Current,
            IntroJs
        ) {
            $scope.introTours = getToursList(IntroJs.toursList, Current.realm.realm);
            $scope.modalInstance = $modalInstance;
            $scope.exitIntro = IntroJs.exit.bind(IntroJs);
            $scope.ok = function () {
                $modalInstance.close();
            };
            $scope.cancel = function () {
                $modalInstance.dismiss("cancel");
            };
            $scope.$on("$routeChangeStart", function () {
                $modalInstance ? $modalInstance.close() : "";
            });
            $scope.redirectToTour = IntroManagement.setInfoRedirect.bind(IntroManagement);
        };
        $modal.open({
            templateUrl: resourceUrl + "/templates/kc-onboarding-tours.html",
            controller: controller,
            windowClass: "constraint-modal",
            backdrop: "static"
        });
    };
    return dialog;
});

module.service("OnboardingSkipDialog", function ($modal, IntroManagement) {
    var dialog = {};
    dialog.open = function () {
        var controller = function ($scope, $modalInstance) {
            $scope.cancel = function () {
                IntroManagement.setIntroSkipInfo(true);
                $modalInstance.dismiss("cancel");
            };

            $scope.ok = function () {
                IntroManagement.setIntroSkipInfo(true);
                $scope.cancel();
            };

            $scope.$on("$routeChangeStart", function () {
                $modalInstance.dismiss("cancel");
            });
        };
        if (!IntroManagement.getIntroInfo().skipOn) {
            $modal.open({
                templateUrl: resourceUrl + "/templates/kc-onboarding-skip.html",
                controller: controller,
                backdrop: "static"
            });
        }
    };
    return dialog;
});

module.service("AddGroupsToUser", function ($modal) {
    var dialog = {};
    dialog.open = function (realmName, userId, title) {
        var controller = function ($scope, $modalInstance, $route, Groups, Group, Session, $http, Notifications, $translate, AssignUserToGroups, UserGroupMembership) {
            const realm = { realm: $route.current.params.realm };
            $scope.isRTL = () => locale == 'ar';
            $scope.checkedGroups = {};
            $scope.realm = realm;
            $scope.groupList = [];
            $scope.loaderTypeName = 'roleListLoader';
            $scope.query = {
                realm: realm.realm,
                search: null,
                max: 5,
                first: 0
            };

            $scope.query.max = Session.getItem("pageFilterCount") ? Session.getItem("pageFilterCount") : 5;
            $scope.query.first = 0;
            $scope.entries = 0;
            $scope.pageCount = 5;
            $scope.startCount = 1;
            $scope.maxValueOfPage = 0;
            $scope.searchQueryLoaded = false;
            if ($scope.query && $scope.query.first === 0 && $scope.query.search) {
                $scope.query.search = "";
            }
            initialQueryUpdate();
            $scope.copyElement = angular.copy($scope.query.max);
            $scope.editTxt = "Edit";
            $scope.deleteTxt = "Remove";
            $scope.groupMemberships = [];

            $scope.$watch(function () {
                for (var i = 0; i < $scope.groupList?.length; i++) {
                    if ($scope.groupList[i].isChecked == true) {
                        $scope.checkedGroups[$scope.groupList[i].id] = $scope.groupList[i];
                    } else {
                        $scope.checkedGroups[$scope.groupList[i].id] && delete $scope.checkedGroups[$scope.groupList[i].id];
                    }
                }
            }, true);

            $scope.$watch(function () {
                $scope.enableBtn = false;
                Object.keys($scope.checkedGroups).forEach((key) => {
                    if ($scope.checkedGroups[key].isChecked == true) {
                        $scope.enableBtn = true;
                    }
                });
            }, true);

            function oldQueryUpdate() {
                $scope.lengthQuery = {};
                if ($scope.query.max !== $scope.copyElement) {
                    $scope.query.first = 0;
                }
                let querySelectorValue = {};
                for (var i in $scope.query) {
                    if ($scope.query[i] === "") {
                        delete $scope.query[i];
                    }
                    else if (i !== "max" && i !== "first") {
                        querySelectorValue = {
                            [i]: $scope.query[i]
                        };
                        Object.assign($scope.lengthQuery, querySelectorValue);
                    }
                }

                Group.query($scope.lengthQuery, function (res) {
                    $scope.entries = res.length;
                    if ($scope.entries !== 0 && $scope.entries < $scope.query.max) {
                        $scope.pageCount = $scope.entries;
                        $scope.startCount = 1;
                    }

                    else {
                        $scope.pageCount = $scope.query.max;
                        $scope.startCount = 1;
                    }
                });
            }
            $scope.$watch('query.max', function () {
                $scope.query.first = 0;
            }, true);

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
                }
                else if (pageCount === $scope.entries) {
                    let backFlow = pageCount + queryMax - $scope.maxValueOfPage;
                    $scope.pageCount = pageCount - backFlow;
                }
                else {
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
                let fullLengthOfPages = parseInt($scope.query.max) + $scope.pageCount;
                $scope.maxValueOfPage = parseInt($scope.query.max) + $scope.pageCount;
                $scope.pageCount = ($scope.entries < fullLengthOfPages) ? $scope.entries : fullLengthOfPages;
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
                Groups.query($scope.query, function (data) {
                    updateCheckedGroups(data);
                });
            };

            $scope.searchQuery = function () {
                if ($scope.lastSearch !== $scope.query.search) {
                    $scope.searchLoaded = false;
                    if ($scope.query.search) {
                        $scope.searchQueryLoaded = true;
                    }
                    else {
                        $scope.searchQueryLoaded = false;
                    }
                    oldQueryUpdate();
                    searchQueryWithLoaded();
                    $scope.query.first = 0;
                    $scope.startCount = 1;
                    Groups.query($scope.query, function (data) {
                        updateCheckedGroups(data);
                        $scope.lastSearch = $scope.query.search;
                    });
                }
            };
            function initialQueryUpdate()  {
                $scope.searchLoaded = false;
                oldQueryUpdate();
                searchQueryWithLoaded();
                Groups.query($scope.query, function (data) {
                    updateCheckedGroups(data);
                    $scope.lastSearch = $scope.query.search;
                });
            };

            $scope.selectQuery = function () {
                Session.setItem("pageFilterCount", $scope.query.max);
                $scope.searchLoaded = false;
                searchQueryWithLoaded();
                oldQueryUpdate();
                $scope.groupList = Groups.query($scope.query, function () {
                    $scope.lastSearch = $scope.query.search;
                    $scope.searchLoaded = true;
                });
            };

            function searchQueryWithLoaded() {
                $scope.searchLoaded = false;
                if ($scope.query.search && $scope.searchQueryLoaded) {
                    $scope.searchQueryLoaded = true;
                }
                else {
                    $scope.query.search = "";
                    $scope.searchQueryLoaded = false;
                }
            }

            $scope.addGroupsToUser = function () {
                let selectedGroups = [];
                Object.keys($scope.checkedGroups).forEach((key) => {
                    delete $scope.checkedGroups[key].isChecked;
                    selectedGroups.push($scope.checkedGroups[key].id)
                });
                AssignUserToGroups.create({ "realm": realm.realm, "userId": userId }, { "grpIds": selectedGroups }, function () {
                    selectedGroups = [];
                    $modalInstance.dismiss('cancel');
                    $route.reload();
                    Notifications.success($translate.instant("user.groups.join.success"));
                });
            };

            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };

            function updateCheckedGroups(fetchedGroups) {
                UserGroupMembership.query({realm: realm.realm, userId}, function(response) {
                    const assignedGroupIds = response.map(group => group.id);
                    fetchedGroups
                        .map((group) => {
                            if ($scope.checkedGroups[group.id]) {
                                group.isChecked = true;
                            }
                            if (assignedGroupIds.includes(group.id)) {
                                group.isAssigned = true;
                            }
                            return group;
                        });
                    $scope.groupList = fetchedGroups;
                    $scope.searchLoaded = true;
                })
            };

            $scope.unCheckGroups = function(groupId) {
                for (var i = 0; i < $scope.groupList?.length; i++) {
                    if ($scope.groupList[i].id == groupId) {
                        $scope.groupList[i].isChecked = false;
                    }
                }
                delete $scope.checkedGroups[groupId];
            }

        };

        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/add-groups-modal.html',
            controller: controller,
            backdrop: 'static',
            resolve: {
                title: function () {
                    return title;
                }
            },
            size: 'lg'
        });
    };
    return dialog;
});

module.service("AddRolesToScope", function ($modal) {
    var dialog = {};
    dialog.open = function (realmName, clientScopeId) {
        var controller = function ($scope, $modalInstance, $http, $route, Notifications, $translate, ClientScopeAvailableRealmScopeMapping, ClientScopeAvailableRealmScopeMappingCount, Session) {
            $scope.isRTL = () => locale == 'ar';
            $scope.checkedRoles = {};
            $scope.query = {};
            $scope.resourceUrl = resourceUrl;
            $scope.realm = realmName;
            $scope.clientScopeId = clientScopeId;
            $scope.query.clientScope = clientScopeId;
            $scope.query.realm = realmName;
            $scope.loaderTypeName = 'roleListLoader';
            $scope.query.briefRepresentation = 'true';
            $scope.query.max = 5;
            $scope.query.first = 0;
            $scope.pageCount = 5;
            $scope.startCount = 1;
            $scope.maxValueOfPage = 0;
            $scope.searchQueryLoaded = false;
            $scope.navigationLoaded = false;
            $scope.isConfigLoaded = true;
            $scope.currentRole = {};

            $scope.query = {
                search: null,
                realm: realmName,
                max: 5,
                first: 0,
                clientScope: clientScopeId
            };

            if ($scope.query && $scope.query.first === 0 && $scope.query.search) {
                $scope.query.search = "";
            }
            $scope.copyElement = angular.copy($scope.query.max);
            initialQueryUpdate();
            function initialQueryUpdate() {
               ClientScopeAvailableRealmScopeMapping.query($scope.query, function (data) {
                    updateCheckedRoles(data);
                    oldQueryUpdate();
                    $scope.searchLoaded = true;
                    $scope.lastSearch = $scope.query.search;
                });
            }

            $scope.$watch(function () {
                for (var i = 0; i < $scope.realmRoles?.length; i++) {
                    if ($scope.realmRoles[i].isChecked == true) {
                        $scope.checkedRoles[$scope.realmRoles[i].id] = $scope.realmRoles[i];
                    } else {
                        $scope.checkedRoles[$scope.realmRoles[i].id] && delete $scope.checkedRoles[$scope.realmRoles[i].id];
                    }
                }
            }, true);

            $scope.$watch(function () {
                $scope.enableBtn = false;
                Object.keys($scope.checkedRoles).forEach((key) => {
                    if ($scope.checkedRoles[key].isChecked == true) {
                        $scope.enableBtn = true;
                    }
                });
            }, true);

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
                $scope.pageCount = ($scope.entries < fullLenthOfPages) ? $scope.entries : fullLenthOfPages;
                $scope.query.first += parseInt($scope.query.max);
                $scope.QueryUpdate();
            };

            $scope.searchQuery = function () {
                if ($scope.lastSearch !== $scope.query.search) {
                    $scope.searchLoaded = false;
                    if ($scope.query.search) {
                        $scope.searchQueryLoaded = true;
                    } else {
                        $scope.searchQueryLoaded = false;
                    }
                    $scope.query.first = 0;
                    $scope.startCount = 1;
                    initialQueryUpdate();
                }
            };

            $scope.QueryUpdate = function () {
                $scope.searchLoaded = false;
                for (var i in $scope.query) {
                    if ($scope.query[i] === "") {
                        delete $scope.query[i];
                    }
                }
                ClientScopeAvailableRealmScopeMapping.query($scope.query, function (data) {
                    updateCheckedRoles(data)
                    $scope.searchLoaded = true;
                });
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
                ClientScopeAvailableRealmScopeMappingCount.query({ realm: realmName, clientScope: clientScopeId, search: $scope.query.search }, function (response) {
                    $scope.entries = response.count;
                    if ($scope.entries !== 0 && $scope.entries < $scope.query.max) {
                        $scope.pageCount = $scope.entries;
                        $scope.startCount = 1;
                    } else {
                        $scope.pageCount = $scope.query.max;
                        $scope.startCount = 1;
                    }
                });
            }

            $scope.addUserAttr = () => {
                $scope.addRealmRole();
            }

            $scope.addRealmRole = function () {
                const selectedRoles = [];
                Object.keys($scope.checkedRoles).forEach((key) => {
                    delete $scope.checkedRoles[key].isChecked;
                    selectedRoles.push($scope.checkedRoles[key])
                });

                if (!selectedRoles) return;
                $scope.selectedRealmRoles = [];
                $http.post(authUrl + '/admin/realms/' + realmName + '/client-scopes/' + clientScopeId + '/scope-mappings/realm', selectedRoles)
                    .then(function (res) {
                        $modalInstance.dismiss('cancel');
                        $route.reload();
                        Notifications.succeconfirmDeletess($translate.instant('scope-mappings-updated'));
                    }, function (err) {
                        Notifications.error(err.data.error);
                    });
            };

            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };

            $scope.unCheckRoles = function(roleId) {
                for (var i = 0; i < $scope.realmRoles?.length; i++) {
                    if ($scope.realmRoles[i].id == roleId) {
                        $scope.realmRoles[i].isChecked = false;
                    }
                }
                delete $scope.checkedRoles[roleId];
            }

            function updateCheckedRoles(fetchedRoles) {
                fetchedRoles
                    .map((role) => {
                        if ($scope.checkedRoles[role.id]) {
                            role.isChecked = true;
                        }
                        return role;
                    });
                $scope.realmRoles = fetchedRoles;
            }

        };

        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/add-roles-modal.html',
            controller: controller,
            backdrop: 'static',
            size: 'lg'
        });
    };
    return dialog;
});


module.service("AddUsersToGroup", function ($modal) {
    var dialog = {};
    dialog.open = function (title, suggested, success, assignUsers, assignedUsers, role) {
        var controller = function ($scope, $modalInstance, $http, AllUsers, AllUsersCount, RoleById, UserSearchState, $route, Session) {
            const realm = { realm: $route.current.params.realm };
            $scope.isRTL = () => locale == 'ar';
            $scope.selectedUsers = {};
            $scope.resourceUrl = resourceUrl;
            $scope.init = function () {
                $scope.realm = realm;
                $scope.loaderTypeName = 'addUsersListLoader';
                UserSearchState.query.realm = $route.current.params.realm;
                $scope.query = UserSearchState.query;
                $scope.query.briefRepresentation = 'true';
                $scope.query.max = 5;
                $scope.query.first = 0;
                $scope.pageCount = 5;
                $scope.startCount = 1;
                $scope.maxValueOfPage = 0;
                $scope.searchQueryLoaded = false;
                $scope.navigationLoaded = false;
                if ($scope.query && $scope.query.first === 0 && $scope.query.search) {
                    $scope.query.search = "";
                }
                $scope.copyElement = angular.copy($scope.query.max);
                initialQueryUpdate();
            };

            $scope.currentUser = {};
            $scope.userOrganizations = null;
            $scope.userProjects = null;
            $scope.workflows = []
            $scope.roleMetadata = {};
            $scope.availableOrg =  []
            $scope.availableProj = []
            $scope.organizations = [];
            $scope.projects = [];
            $scope.configuredUsers = {};
            $scope.isConfigLoaded = false;
            $scope.enableDone = false

            const getMetaData = () => {
                $http.get(authUrl + `/realms/${realm.realm}/workflows/connectedAppMetadata?type=user`).then(function (result) {
                    $scope.workflows = result.data;
                    $scope.availableOrg = $scope.workflows[0].metaData.organization
                    $scope.availableProj = $scope.workflows[0].metaData.projects
                    $scope.organizations = $scope.availableOrg.map(org => org.name);
                    $scope.projects = $scope.availableProj.map(prj => prj.name);
                    $scope.isConfigLoaded = true;
                }).catch(function (error) {
                    $scope.isConfigLoaded = true;
                });
            }
            getMetaData();

            $scope.addRoleAttribute = () => {
                Object.keys($scope.configuredUsers).map( key => {
                    const organizations = $scope.configuredUsers[key].organizations;
                    const projects = $scope.configuredUsers[key].projects
                    $scope.roleMetadata[`${key}_organizations`] = [JSON.stringify($scope.availableOrg.filter(org => organizations.includes(org.name)))];
                    $scope.roleMetadata[`${key}_projects`] = [JSON.stringify($scope.availableProj.filter(pro => projects.includes(pro.name)))];
                })  
            }

            $scope.saveRoleAttr = () => {
                if(!$scope.isWorkflowRole()) {
                    $scope.ok();
                    return;
                }
                const metaData = {}
                Object.keys($scope.selectedUsers).map(id => {
                    Object.keys($scope.roleMetadata).map(key => {
                        if(key.split('_')[0] === id) {
                            metaData[key] = $scope.roleMetadata[key];
                        }
                    })
                })
                role.attributes = {...role.attributes, ...metaData}
                RoleById.update({
                    realm: realm.realm,
                    role:role.id
                }, role, function (data, headers) {
                   $scope.ok();
                }, function () {
                    Notifications.error($translate.instant(error.data.errorMessage));                 
                });
            }

            $scope.isUserConfigured = (user) => {
                return Object.keys($scope.configuredUsers).includes(user.id);
            }

            $scope.isWorkflowRole = () => {
                if (!role) return false
                return role.attributes['connectedApp'] ? true : false;
            } 
 
            $scope.setCurrentUser = (user) => {
                $scope.currentUser = user;
                if (Object.keys($scope.configuredUsers).includes(user.id)) {
                    const organizations = $scope.configuredUsers[user.id].organizations;
                    const projects = $scope.configuredUsers[user.id].projects;
                    $scope.currentUser['organizations'] = organizations.length > 0 ? organizations : null;
                    $scope.currentUser['projects'] = projects.length > 0 ? projects : null;
                    return;
                }
                $scope.currentUser['organizations'] =  null;
                $scope.currentUser['projects'] = null;
            }

            $scope.configureUser = ()=> {
                $scope.configuredUsers[$scope.currentUser.id] = {
                    organizations:$scope.currentUser.organizations, 
                    projects:$scope.currentUser.projects
                }
                $scope.addRoleAttribute();
                $scope.users = $scope.users.map(user => {
                    if (user.id === $scope.currentUser.id) {
                        return {...user, isChecked : true}
                    } else {
                        return user;
                    } 
                })
                $scope.currentUser = {};
            }

            if ($scope.query && $scope.query.first === 0 && $scope.query.search) {
                $scope.query.search = "";
            }

            $scope.cancelWorkflow = () => {
                $scope.currentUser = {};
            } 

            $scope.$watch('currentUser', function() {
                if (($scope.currentUser.hasOwnProperty('projects') && $scope.currentUser.projects.length > 0) 
                    || ($scope.currentUser.hasOwnProperty('organizations') && $scope.currentUser.organizations.length)) {
                    $scope.enableDone = true;
                } else {
                    $scope.enableDone = false;
                }
            },true)

            $scope.getImage = function (imageData) {
                return getUpdatedImgData(imageData);
            };

            $scope.$watch(function () {
                $scope.enableBtn = false;
                Object.keys($scope.selectedUsers).forEach((key)=> {
                    if($scope.selectedUsers[key].isChecked == true) {
                        $scope.enableBtn = true;
                    }
                })
            }, true);

            $scope.$watch(function () {
                for (var i = 0; i < $scope.users?.length; i++) {
                    if ($scope.users[i].isChecked == true) {
                        $scope.selectedUsers[$scope.users[i].id] = $scope.users[i];
                    } else {
                        $scope.selectedUsers[$scope.users[i].id] && delete $scope.selectedUsers[$scope.users[i].id];
                    }
                }
            }, true);

            $scope.title = title;
            $scope.brandName = { value: suggested };
            $scope.$watch('brandName', function () {
                $scope.changed = $scope.brandName.value && $scope.brandName.value.length > 0;
            }, true);
            $scope.ok = function () {
                $modalInstance.close();
                success($scope.brandName.value);
            };
            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
            });
            function oldQueryUpdate() {
                $scope.lengthQuery = {};
                if ($scope.copyElement && ($scope.query.max !== $scope.copyElement)) {
                    $scope.query.first = 0;
                }
                let querySelectorValue = {};
                for (var i in $scope.query) {
                    if ($scope.query[i] === "") {
                        delete $scope.query[i];
                    }
                    else if (i !== "max" && i !== "first") {
                        querySelectorValue = {
                            [i]: $scope.query[i]
                        };
                        Object.assign($scope.lengthQuery, querySelectorValue);
                    }
                }
                AllUsersCount.get($scope.query, (data) => {
                    $scope.entries = data.count;
                    if ($scope.entries !== 0 && $scope.entries < $scope.query.max) {
                        $scope.pageCount = $scope.entries;
                        $scope.startCount = 1;
                    } else {
                        $scope.pageCount = $scope.query.max;
                        $scope.startCount = 1;
                    }
                });
            }
            $scope.$watch('query.max', function () {
                $scope.query.first = 0;
            }, true);

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
                }
                else if (pageCount === $scope.entries) {
                    let backFlow = pageCount + queryMax - $scope.maxValueOfPage;
                    $scope.pageCount = pageCount - backFlow;
                }
                else {
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
                let fullLengthOfPages = parseInt($scope.query.max) + $scope.pageCount;
                $scope.maxValueOfPage = parseInt($scope.query.max) + $scope.pageCount;
                $scope.pageCount = ($scope.entries < fullLengthOfPages) ? $scope.entries : fullLengthOfPages;
                $scope.query.first += parseInt($scope.query.max);
                $scope.QueryUpdate();
            };

            function searchQueryWithLoaded() {
                $scope.searchLoaded = false;
                if ($scope.query.search && $scope.searchQueryLoaded) {
                    $scope.searchQueryLoaded = true;
                } else {
                    $scope.searchQueryLoaded = false;
                }
            }
            $scope.reset = function () {
                $scope.query.search = "";
                $route.reload();
            };
            $scope.QueryUpdate = function () {
                for (var i in $scope.query) {
                    if ($scope.query[i] === "") {
                        delete $scope.query[i];
                    }
                }
                AllUsers.query($scope.query, function (data) {
                    updateCheckedUsers(data);
                    $scope.searchLoaded = true;
                });
            };

            $scope.searchQuery = function () {
                if ($scope.lastSearch !== $scope.query.search) {
                    $scope.searchLoaded = false;
                    if ($scope.query.search) {
                        $scope.searchQueryLoaded = true;
                    }
                    else {
                        $scope.searchQueryLoaded = false;
                    }
                    oldQueryUpdate();
                    searchQueryWithLoaded();
                    $scope.query.first = 0;
                    $scope.startCount = 1;
                    AllUsers.query($scope.query, function (data) {
                        updateCheckedUsers(data);
                        $scope.lastSearch = $scope.query.search;
                        UserSearchState.isFirstSearch = false;
                        $scope.searchLoaded = true;
                    });
                }
            };

            $scope.isUserListEmpty = function () {
                return $scope.users?.length == 0;
            };

            $scope.getProfilePicture = function (username, email) {
                const profPicBase64 = generateAvatar(username ? username : email);
                $scope.users.profPic = profPicBase64.split("base64,")[1];
                return $scope.users.profPic;

            };

            $scope.ok = function () {
                let selectedUserIds = {
                    userIds: Object.keys($scope.selectedUsers).map((key) => $scope.selectedUsers[key].id)
                };
                assignUsers(selectedUserIds);
            };

            function initialQueryUpdate() {
                $scope.searchLoaded = false;
                oldQueryUpdate();
                searchQueryWithLoaded();
                let userQuery;
                AllUsers.query($scope.query, function (data) {
                    updateCheckedUsers(data);
                    $scope.searchLoaded = true;
                    $scope.lastSearch = $scope.query.search;
                    ClientListSearchState.isFirstSearch = false;
                    userQuery;
                });
            };

            function updateCheckedUsers(fetchedUsers) {
                const assignedUserIds = assignedUsers.map(user => user.id);
                fetchedUsers
                    .map((user) => {
                        if ($scope.selectedUsers[user.id]) {
                            user.isChecked = true;
                        }
                        if (assignedUserIds.includes(user.id)) {
                            user.isAssigned = true;
                        }
                        return user;
                    });
                $scope.users = fetchedUsers;
            };

            $scope.unSelectUser = function(userId) {
                for (var i = 0; i < $scope.users?.length; i++) {
                    if ($scope.users[i].id == userId) {
                        $scope.users[i].isChecked = false;
                    }
                }
                delete $scope.selectedUsers[userId];
            }
        };
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/select-users-modal.html',
            controller: controller,
            backdrop: 'static',
            resolve: {
                title: function () {
                    return title;
                }
            },
            size: 'lg'
        });
    };
    return dialog;
});


module.service("BlackListingModal", function ($modal) {
    var dialog = {};
    dialog.open = function (value, index, removeValue) {
        var controller = function ($scope, $modalInstance) {
            $scope.value = value;
            $scope.index = index;
            $scope.removeValue = removeValue;
            $scope.searchResults = [];
            $scope.search = '';
            $scope.close = function () {
                $modalInstance.close()
            };
            $scope.handleSearch = (search) => {
                $scope.searchResults = value.defaultValue.filter(val => val.includes(search))
            }
            $scope.emptySearch = () => {
                $('#searchInputField').val('');
            }
        };
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/blacklisting-modal.html',
            controller: controller,
            backdrop: 'static',
            size: 'md'
        });
    };
    return dialog;
});

module.factory('Notifications', function ($rootScope, $timeout, $translate) {
    // time (in ms) the notifications are shown
    var delay = 5000;

    var notifications = {};
    notifications.current = { display: false };
    notifications.current.remove = function () {
        if (notifications.scheduled) {
            $timeout.cancel(notifications.scheduled);
            delete notifications.scheduled;
        }
        delete notifications.current.type;
        delete notifications.current.header;
        delete notifications.current.message;
        notifications.current.display = false;
    };

    $rootScope.notification = notifications.current;

    notifications.message = function (type, header, message) {
        notifications.current.remove();

        notifications.current.type = type;
        notifications.current.header = header;
        notifications.current.message = message;
        notifications.current.display = true;

        notifications.scheduled = $timeout(function () {
            notifications.current.remove();
        }, delay);

    };

    notifications.info = function (message) {
        notifications.message("info", $translate.instant("notifications.info.header"), message);
    };

    notifications.success = function (message) {
        notifications.message("success", $translate.instant("notifications.success.header"), message);
    };

    notifications.error = function (message) {
        message = message.split('\n').join('<br>');
        notifications.message("danger", $translate.instant("notifications.error.header"), message);
    };

    notifications.warn = function (message) {
        notifications.message("warning", $translate.instant("notifications.warn.header"), message);
    };

    return notifications;
});

module.factory('SetupGuides', function ($rootScope, $timeout) {
    let setupguides = {};
    setupguides.current = { display: false, guideUrl: '' };

    $rootScope.setupguide = setupguides.current;

    setupguides.open = function (guideUrl) {
        if (guideUrl) {
            $('body').addClass('overflow-hidden');
            $('#freshworks-container').hide();
            setupguides.current.guideUrl = guideUrl;
        }
        setupguides.current.display = true;
    };

    setupguides.getUrl = function () {
        return setupguides.current.guideUrl;
    };

    setupguides.close = function () {
        $('body').removeClass('overflow-hidden');
        $('#freshworks-container').show();
        setupguides.current.display = false;
    };

    return setupguides;
});

module.factory('ValidationUtils', function () {
    var validationUtils = {};
    const Configuration = {
        EmailRegex: /^(([^<>!%&*#$()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,63}))$/,
        Spacing: /\s/,
        UrlRegex: /https?:\/\/(www\.)?[-a-zA-Z0-9@:%._\+~#=]{1,256}\.[a-zA-Z0-9()]{1,6}\b([-a-zA-Z0-9()@:%_\+.~#?&//=]*)/,
        mobileNumberRegex: /^[ 0-9+]*$/
    };

    validationUtils.validateEmail = function (email) {
        if (email && email.match(Configuration.EmailRegex)) {
            return true;
        } else {
            return false;
        }
    };

    validationUtils.validateURL = function (url) {
        if (url && url.match(Configuration.UrlRegex)) {
            return true;
        } else {
            return false;
        }
    };

    validationUtils.validateMobileNumber = function (mobile_number) {
        if (mobile_number && mobile_number.match(Configuration.mobileNumberRegex)) {
            return true;
        } else {
            return false;
        }
    };

    return validationUtils;
});
module.factory('CurlUtils', function () {
    var curlUtils = {};

    /**
     * @param {string} [method]
     * @returns {string}
     */
    var getCurlMethod = function (method) {
        var result = "";
        if (method) {
            var types = {
                GET: '--request GET',
                POST: '--request POST',
                PUT: '--request PUT',
                PATCH: '--request PATCH',
                DELETE: '--request DELETE',
            };
            result = types[method.toUpperCase()];
        }
        return result;
    };
    /**
     * @param {StringMap} headers
     * @returns {string}
     */
    var getCurlHeaders = function (headers) {
        var result = "";
        if (headers) {
            Object.keys(headers).map(function (val) {
                result += "--header \"" + val + ": " + headers[val].replace(/(\\|")/g, '\\$1') + "\" ";
            });
        }
        return result.trim();
    };
    /**
     * @param {Object} body
     * @returns {string}
     */
    var getCurlBody = function (header, body) {
        var result = "";
        if (header && body) {
            if (JSON.stringify(header).includes("application/json")) {
                result += "--data \"" + (JSON.stringify(body)).replace(/(\\|")/g, '\\$1') + "\"";
            } else if (JSON.stringify(header).includes("application/x-www-form-urlencoded")) {
                var dataParams = "";
                for (const key in body) {
                    dataParams += key + "=" + body[key] + "&";
                }
                result = "--data \"" + dataParams.slice(0, dataParams.length - 1) + "\"";
            }
        }
        return result;
    };

    curlUtils.generateCurl = function (options) {
        var curlSnippet = "curl ";
        curlSnippet += getCurlMethod(options.method) + " \\" + "\n";
        curlSnippet += " " + "--url " + options.url + " \\" + "\n";
        curlSnippet += " " + getCurlHeaders(options.headers) + " \\" + "\n";
        curlSnippet += " " + getCurlBody(options.headers, options.body);
        return curlSnippet.trim().toString();
    };
    return curlUtils;
});

module.factory('ComponentUtils', function () {

    function sortGroups(prop, arr) {
        // sort current elements
        arr.sort(function (a, b) {
            if (a[prop] < b[prop]) { return -1; }
            if (a[prop] > b[prop]) { return 1; }
            return 0;
        });
        // check sub groups
        arr.forEach(function (item, index) {
            if (!!item.subGroups) {
                sortGroups(prop, item.subGroups);
            }
        });
        return arr;
    };

    const changeDirCss = (url, dir, id, oldElement) => {
        const style = document.createElement("link");
        style.rel = "stylesheet"
        style.id = id;
        fetch(url)
        .then(response => response.text())
        .then(css => {
            style.href = 'data:text/css;charset=utf-8,' + encodeURIComponent(css);
            style.addEventListener('load', () => {
                oldElement.remove();
                document.body.dir = dir;
                $('#language-switching-loader').css("display", "none");
            });
            document.head.appendChild(style);
        })
        .catch(error => {
            $('#language-switching-loader').css("display", "none");
            console.error('Error fetching CSS:', error)
        });
    }

    var utils = {};

    utils.sortGroups = sortGroups;

    utils.findIndexById = function (array, id) {
        for (var i = 0; i < array.length; i++) {
            if (array[i].id === id) return i;
        }
        return -1;
    };

    utils.changeViewToRtl = function() {
        const ltrStyle = document.getElementById("dashboard-style-sheet")
        changeDirCss(`${resourceUrl}/themes/css/main-rtl.css?version=${serverVersion}`, "rtl", 'dashboard-rtl-style-sheet', ltrStyle);
        document.getElementById('tree-view-style-sheet').href = `${resourceUrl}/themes/css/roles-group-tree-rtl.css?version=${serverVersion}`;
        $('#launcher-frame').css('left', '22px');
        $('#launcher-frame').css('right', 'initial');
    }

    utils.changeViewToLtr = function() {
        const rtlStyle = document.getElementById("dashboard-rtl-style-sheet")
        changeDirCss(`${resourceUrl}/themes/css/main.css?version=${serverVersion}`, "ltr", 'dashboard-style-sheet', rtlStyle);
        document.getElementById('tree-view-style-sheet').href = `${resourceUrl}/themes/css/roles-group-tree.css?version=${serverVersion}`;
        $('#launcher-frame').css('right', '22px');
        $('#launcher-frame').css('left', 'initial');
    }

    utils.convertAllMultivaluedStringValuesToList = function (properties, config) {
        if (!properties || !config) {
            return;
        }
        for (var i = 0; i < properties.length; i++) {
            var prop = properties[i];
            if (prop.type === 'MultivaluedString') {
                var configProperty = config[prop.name];

                if (configProperty == null) {
                    configProperty = [];
                    config[prop.name] = configProperty;
                }

                if (typeof configProperty === "string") {
                    configProperty = configProperty.split("##");
                    config[prop.name] = configProperty.filter(e => e);
                }
            }
        }
    };
    utils.removeEmptyStringInMultivaluedString = function (properties, config) {
        if (properties && properties.length > 0) {
            for (const prop of properties) {
                if (prop.type === 'MultivaluedString') {
                    config[prop.name] = config[prop.name].filter(e => e);
                }
            }
        }
    };

    utils.convertNumberTypeStringValueToInt = function (properties, config) {
        if (!properties) {
            return;
        }
        for (var i = 0; i < properties.length; i++) {
            var prop = properties[i];
            if (prop.type === 'Number') {
                config[prop.name] = Number(config[prop.name]);
            }
        }
    };
    utils.checkNumberTypeMaxValueValidity = function (properties, config) {
        if (!properties) {
            return;
        }
        for (var i = 0; i < properties.length; i++) {
            var prop = properties[i];
            if (prop.type === 'Number') {
                if (prop.maxVal) {
                    if (checkConfigValid(config, prop) && (config[prop.name] > prop.maxVal || (config[prop.name] !== prop.minVal && !config[prop.name]))) {
                        return false;
                    }
                }
            }
        }
        return true;
    };
    function checkConfigValid(config, property) {
        if (property.dependsOn) {
            var conditions = [];
            for (const dependsKey of Object.keys(property.dependsOn)) {
                if (Array.isArray(property.dependsOn[dependsKey])) {
                    conditions.push(property.dependsOn[dependsKey].indexOf(config[dependsKey]) >= 0);
                }
                else {
                    conditions.push(config[dependsKey] === String(property.dependsOn[dependsKey]));
                }
            }
            if (conditions.length > 0) {
                if (property.validationType == "anyone_true") {
                    return conditions.some(c => c);
                }
                return conditions.every(c => c);
            }
            return false;
        } else {
            return true;
        }
    }
    utils.convertAllListValuesToMultivaluedString = function (properties, config) {
        if (!properties) {
            return;
        }
        for (var i = 0; i < properties.length; i++) {
            var prop = properties[i];
            if (prop.type === 'MultivaluedString' || prop.type === 'MultivaluedList' || prop.type === 'MultivaluedClaimList') {
                var configVal = config[prop.name];

                if (configVal) {
                    if (configVal.length > 0) {
                        var lastVal = configVal[configVal.length - 1];
                        if (lastVal === '') {
                            configVal.splice(configVal.length - 1, 1);
                        }
                    }
                    var attrVals = typeof configVal !== 'string' ? configVal.join("##") : configVal;
                    config[prop.name] = attrVals;

                }
            } 
            if (prop.type === 'Map' || prop.type === 'MultivaluedListFromObject' ){
                config[prop.name] = JSON.stringify(config[prop.name]);
            }
        }
    };



    utils.addLastEmptyValueToMultivaluedLists = function (properties, config) {
        if (!properties || !config) {
            return;
        }

        for (var i = 0; i < properties.length; i++) {
            var prop = properties[i];
            if (prop.type === 'MultivaluedString') {
                var configProperty = config[prop.name];

                if (configProperty == null) {
                    configProperty = [];
                    config[prop.name] = configProperty;
                }

                if (configProperty.length == 0 || configProperty[configProperty.length - 1].length > 0) {
                    configProperty.push('');
                }
            }
        }
    };

    utils.convertBackToMultivaluedList = function (properties, config) {
        if (!properties) {
            return;
        }
        for (var i = 0; i < properties.length; i++) {
            var prop = properties[i];
            if (config && (prop.type === 'MultivaluedList' || prop.type === 'MultivaluedString' || prop.type === 'MultivaluedClaimList')) {
                var configProperty = config[prop.name];
                if (configProperty == null || configProperty === "" || configProperty.length === 0) {
                    config[prop.name] = null;
                } else if (typeof configProperty === "string") {
                    configProperty = configProperty.split("##");
                    config[prop.name] = configProperty;
                }
            }
        }
    };

    utils.checkExpiryTimeValid = function (properties, config) {
        if (!properties) {
            return;
        }
        //regex to check the expiry time (eg. 2s,20m,2h,2d are only allowed)
        const ExpiryTimeRegex = "^([1-9][0-9]*)+(s|m|h|d)$";
        for (const prop of properties) {
            if (prop.type === 'ExpiryTime') {
                var configVal = config[prop.name];
                if (configVal.trim() === '') {
                    return true;
                }
                if (configVal && configVal.match(ExpiryTimeRegex)) {
                    return true;
                } else {
                    return false;
                }
            }
        }
    };

    utils.removeLastEmptyValue = function (componentConfig) {

        for (var configPropertyName in componentConfig) {
            var configVal = componentConfig[configPropertyName];
            if (configVal && configVal.length > 0) {
                var lastVal = configVal[configVal.length - 1];
                if (lastVal === '') {
                    console.log('Remove empty value from config property: ' + configPropertyName);
                    configVal.splice(configVal.length - 1, 1);
                }
            }
        }
    };

    // Allows you to use ui-select2 with <input> tag.
    // In HTML you will then use property.mvOptions like this:
    // <input ui-select2="prop.mvOptions" ng-model="...
    utils.addMvOptionsToMultivaluedLists = function (properties) {
        if (!properties) return;

        for (var i = 0; i < properties.length; i++) {
            var prop = properties[i];
            if (prop.type !== 'MultivaluedList') continue;

            prop.mvOptions = {
                'multiple': true,
                'simple_tags': true,
                'tags': angular.copy(prop.options)
            };
        }

    };

    return utils;
});

module.factory('Session', function () {
    let session = {
        setItem(key, value) {
            if (key && value) {
                sessionStorage.setItem(key, JSON.stringify(value));
            }
        },
        getItem(key) {
            if (key && sessionStorage.getItem(key)) {
                return JSON.parse(sessionStorage.getItem(key));
            }
            return null;
        }
    };

    return session;
});

module.factory("LocalStore", function () {
    let local = {
        setItem(key, value) {
            if (key && value) {
                localStorage.setItem(key, JSON.stringify(value));
            }
        },
        getItem(key) {
            if (key && localStorage.getItem(key)) {
                return JSON.parse(localStorage.getItem(key));
            }
            return null;
        }
    };

    return local;
});

module.factory("IntroManagement", function (LocalStore, $location, $route) {
    const introManagement = {
        //function for intro details
        getIntroInfo() {
            const introInfo = LocalStore.getItem("introInfo")
                ? LocalStore.getItem("introInfo")
                : {
                    introOn: true,
                    skipOn: false,
                    introList: [
                        "home",
                        "application",
                        "onboarding",
                        "login",
                        "recovery"
                    ],
                    introDetailedList: {
                        home: {
                            stage: "stage_1",
                            present: true
                        },
                        application: {
                            stage: "stage_1",
                            present: true
                        },
                        onboarding: {
                            stage: "stage_1",
                            present: true
                        },
                        login: {
                            stage: "stage_1",
                            present: true
                        },
                        recovery: {
                            stage: "stage_1",
                            present: true
                        }
                    }
                };

            return introInfo;
        },
        //function for checking if the intro is valid.
        isIntroValid(introName, introStage) {
            let introValid = false;
            const introInfo = this.getIntroInfo();
            const { introOn, introList, introDetailedList } = introInfo;
            if (
                introOn &&
                introList.includes(introName) &&
                introDetailedList[introName]?.stage === introStage &&
                introDetailedList[introName]?.present
            ) {
                introValid = true;
            }
            return introValid;
        },
        //function to set the intro detail info.
        setIntroDetails(introName, introDetails) {
            let introInfo = this.getIntroInfo();
            introInfo.introDetailedList[introName] = {
                ...introInfo.introDetailedList[introName],
                ...introDetails
            };
            LocalStore.setItem("introInfo", introInfo);
        },
        //function to set the intro on.
        setIntroInfo(introOn) {
            let introInfo = this.getIntroInfo();
            introInfo.introOn = introOn;
            LocalStore.setItem("introInfo", introInfo);
        },
        setIntroSkipInfo(skipOn) {
            let introInfo = this.getIntroInfo();
            introInfo.skipOn = skipOn;
            LocalStore.setItem("introInfo", introInfo);
        },
        //function to redirect to specific intro.
        setInfoRedirect(introName, redirectUri, modalInstance, introExit) {
            introExit();
            this.setIntroDetails(introName, {
                stage: "stage_1",
                present: true
            });
            this.setIntroInfo(true);
            if ($location.path() === redirectUri) {
                $route.reload();
            }
            $location.url(redirectUri);
        }
    };
    return introManagement;
});

module.factory(
    "IntroJs",
    function (OnboardingDoneDialog, IntroManagement, OnboardingSkipDialog) {
        //variables and functions
        const baseIntro = introJs();

        //intro object with functions required.
        const intro = {
            toursList: [],
            steps: {},
            start(introName, introStage, introStep) {
                let introOptions = { disableInteraction: 1 };
                if (this.steps[introName][introStage]?.options) {
                    introOptions = {
                        ...introOptions,
                        ...this.steps[introName][introStage].options
                    };
                }

                baseIntro
                    .setOptions({
                        ...introOptions,
                        steps: this.steps[introName][introStage].steps
                    })
                    .start()
                    .onskip(() => {
                        if (this.steps[introName][introStage].skip) {
                            this.steps[introName][introStage].skip();
                        }
                    })
                    .onexit(() => {
                        if (this.steps[introName][introStage].done) {
                            this.steps[introName][introStage].done();
                        }
                    });
            },
            exit() {
                baseIntro.exit();
            },
            isIntroValid(introDetail) {
                let { introName, introStage } = introDetail;
                return IntroManagement.isIntroValid(introName, introStage);
            },
            getFirstValidIntro(introsList) {
                return introsList.find(this.isIntroValid);
            },
            init(tourLayout) {
                const steps = {};
                for (const tourProps in tourLayout) {
                    steps[tourProps] = {};
                    for (const stepProps in tourLayout[tourProps]) {
                        let nextStage = tourLayout[tourProps][stepProps].nextStage;
                        let elementsList = [];
                        tourLayout[tourProps][stepProps].steps.forEach((stepData) => {
                            stepData.element ? elementsList.push(stepData.element) : "";
                        });
                        steps[tourProps][stepProps] = {
                            ...tourLayout[tourProps][stepProps],
                            tourName: tourProps,
                            isExecuted: false,
                            isLast: !nextStage ? true : false,
                            skipDetails: { present: false },
                            doneDetails: !this.isLast
                                ? {
                                    stage: nextStage,
                                    present: true
                                }
                                : { present: false },
                            exitDetails: {
                                present: false
                            },
                            elementsList,
                            onComplete(details) {
                                if (!this.isExecuted) {
                                    this.isExecuted = true;
                                    IntroManagement.setIntroDetails(this.tourName, details);
                                    this.isLast ? OnboardingDoneDialog.open(this.tourName) : "";
                                }
                            },
                            skip() {
                                !this.isLast ? OnboardingSkipDialog.open() : "";
                                this.onComplete({ present: false });
                            },
                            done() {
                                this.onComplete(this.doneDetails);
                            }
                        };
                    }
                }
                this.steps = steps;
            },
            resetExcecuted(introName) {
                let stages = this.steps[introName];
                for (const property in stages) {
                    stages[property].isExecuted = false;
                }
            }
        };
        return intro;
    }
);

module.factory('ProcessInput', function () {

    const processInput = {};
    // For processing the number input with min, max value check.
    processInput.validateRange = function (config, option) {
        let value = config[option.name]

        if (value && option && ( option.minVal || option.minVal == 0 ) && option.maxVal) {
            return value >= option.minVal && value <= option.maxVal;
        }

        return true

    }

    return processInput;
});


module.factory('Realm', function ($resource) {
    return $resource(authUrl + '/admin/realms/:id', {
        id: '@realm'
    }, {
        update: {
            method: 'PUT'
        },
        create: {
            method: 'POST',
            params: { id: '' }
        }

    });
});


module.factory('ValidateForceDelete', function ($resource) {
    return $resource(authUrl + '/admin/realms/:id/validate-force-delete', {
        id: '@realm'
    }, {
        validate: {
            method: 'POST',
        }
    });
});

module.factory('GetAllRealms', function ($resource) {
    return $resource(authUrl + '/admin/realms/getAllRealms');
});

module.factory('GetRealmDetails', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/workspace-management/:name/details' , {
        realm: '@realm',
        name: '@name'
    },{
        query: {
            isArray: false,
            method: 'GET',
            transformResponse: function(data) {
                return angular.fromJson(data)
            }
        }
   });
});

module.factory('RealmKeys', function ($resource) {
    return $resource(authUrl + '/admin/realms/:id/keys', {
        id: '@realm'
    });
});

module.factory('RealmBlackList', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/blacklisting', {
        realm: '@realm'
    }, {
        update: {
            method: 'PUT'
        }
    });
});

module.factory('RealmEventsConfig', function ($resource) {
    return $resource(authUrl + '/admin/realms/:id/events/config', {
        id: '@realm'
    }, {
        update: {
            method: 'PUT'
        }
    });
});

module.factory('RealmApiLogsConfig', function ($resource) {
    return $resource(authUrl + '/realms/api/api-connector/:realm/api-logs', {
        realm: '@realm'
    }, {
        get: {
            method: 'GET'
        }
    });
});

module.factory('RealmMFAConfig', function ($resource) {
    return $resource(authUrl + '/admin/realms/:id/mfa/config', {
        id: '@realm'
    }, {
        update: {
            method: 'PUT'
        }
    });
});

module.factory('RealmMarketplaceConfig', function ($resource) {
    return $resource(authUrl + '/admin/realms/:id/marketplace/config', {
        id: '@realm'
    }, {
        update: {
            method: 'PUT'
        }
    });
});

module.factory('RealmEmailConfig', function ($resource) {
    return $resource(authUrl + '/admin/realms/:id/email/config', {
        id: '@realm'
    }, {
        update: {
            method: 'PUT'
        }
    });
});

module.factory('RealmSmsConfig', function ($resource) {
    return $resource(authUrl + '/admin/realms/:id/sms/config', {
        id: '@realm'
    }, {
        update: {
            method: 'PUT'
        }
    });
});

module.factory('RealmEventsFetcher', function ($resource) {
    return $resource(authUrl + '/admin/realms/:id/reports/events', {
        id: '@realm'
    });
});

module.factory('RealmEventsGenerator', function ($resource) {
    return $resource(authUrl + '/admin/realms/:id/reports/events/generate', {
        id: '@realm'
    });
});

module.factory('RealmAPIEventsGenerator', function ($resource) {
    return $resource(authUrl + '/admin/realms/:id/reports/api-logs/generate', {
        id: '@realm'
    });
});

module.factory('RealmAdminEventsGenerator', function ($resource) {
    return $resource(authUrl + '/admin/realms/:id/reports/admin-events/generate', {
        id: '@realm'
    });
});


module.factory('RealmEvents', function ($resource) {
    return $resource(authUrl + '/admin/realms/:id/ev/events', {
        id: '@realm'
    });
});

module.factory('RealmAdminEvents', function ($resource) {
    return $resource(authUrl + '/admin/realms/:id/ev/admin-events', {
        id: '@realm'
    });
});

module.factory('BruteForce', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/attack-detection/brute-force/users', {
        realm: '@realm'
    });
});

module.factory('BruteForceUser', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/attack-detection/brute-force/users/:userId', {
        realm: '@realm',
        userId: '@userId'
    });
});

module.factory('RequiredActionsQuery', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/required-actions/:action', {
        realm: '@realm',
        action: '@action'
    });
});

module.factory('RequiredActions', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/required-action/:alias', {
        realm: '@realm',
        alias: '@alias'
    }, {
        update: {
            method: 'PUT'
        }
    });
});

module.factory('TermsConditionsVersions', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/required-action/:id/versions', {
        realm: '@realm',
        id: '@id'
    }, {
        query: {
            isArray: true,
            method: 'GET',
            transformResponse: function (data) {
                return angular.fromJson(data);
            }
        }
   });
});

module.factory('TermsConditionsVersion', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/required-action/:id/:version', {
        realm: '@realm',
        id: '@id',
        version: '@version'
    }, {
        query: {
            isArray: false,
            method: 'GET',
            transformResponse: function(data) {
                return angular.fromJson(data)
            }
        }
   });
});


module.factory('RequiredActionRaisePriority', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/required-action/:action/:alias/raise-priority', {
        realm: '@realm',
        alias: '@alias',
        action: '@action'
    });
});

module.factory('RequiredActionLowerPriority', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/required-action/:action/:alias/lower-priority', {
        realm: '@realm',
        alias: '@alias',
        action: '@action'
    });
});

module.factory('UnregisteredRequiredActions', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/unregistered-required-actions/:action', {
        realm: '@realm',
        action: '@action'
    });
});

module.factory('RegisterRequiredAction', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/register-required-action/:action', {
        realm: '@realm',
        action: '@action'
    });
});

module.factory('RegisterRequiredActions', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/register-required-actions/:action', {
        realm: '@realm',
        action: '@action'
    });
});

module.factory('RealmLDAPConnectionTester', function ($resource, $httpParamSerializer) {
    return $resource(authUrl + '/admin/realms/:realm/testLDAPConnection', {
        realm: '@realm'
    }, {
        save: {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },
            transformRequest: function (data) {
                return $httpParamSerializer(data);
            }
        }
    });
});

module.factory('RealmSMTPConnectionTester', function ($resource, $httpParamSerializer) {
    return $resource(authUrl + '/admin/realms/:realm/testSMTPConnection', {
        realm: '@realm'
    }, {
        send: {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },
            transformRequest: function (data) {
                return $httpParamSerializer(data);
            }
        }
    });
});
module.factory('RealmSMSConnectionTester', function ($resource, $httpParamSerializer) {
    return $resource(authUrl + '/admin/realms/:realm/testSMSConnection', {
        realm: '@realm'
    }, {
        send: {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },
            transformRequest: function (data) {
                return $httpParamSerializer(data);
            }
        }
    });
});
module.factory('ShowSecret', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/common/showSecret', {
        realm: '@realm'
    }, {
        show: {
            method: 'POST'
        }
    });
});

module.service('WhoAmI', function ($resource, $q, $http, $location) {
    var delay = $q.defer();
    function getServerInfo(realmName, callback) {
        $http.get(consoleBaseUrl + 'whoami?realm=' + realmName).then(function (response) {
            callback && callback(response);
            onSuccess(response);
        });
    }
    return {
        get: function (realmName, callback) {
            if (realmName) {
                getServerInfo(realmName, callback);
            } else {
                let realm = $location.path().substring(1).split("/")[1];
                getServerInfo(realm, callback);
            }
        },
        promise: delay.promise
    };
});

module.factory('AuthCounter', function ($resource, $httpParamSerializer) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/counter', {
        realm: '@realm'
    });
});

module.service('ServerInfo', function ($resource, $q, $http, $location) {
    var info = {};
    var delay = $q.defer();
    let realm = '';
    function copyInfo(data, info) {
        angular.copy(data, info);

        info.listProviderIds = function (spi) {
            var providers = info.providers[spi].providers;
            var ids = Object.keys(providers);
            ids.sort(function (a, b) {
                var s1;
                var s2;

                if (providers[a].order != providers[b].order) {
                    s1 = providers[b].order;
                    s2 = providers[a].order;
                } else {
                    s1 = a;
                    s2 = b;
                }

                if (s1 < s2) {
                    return -1;
                } else if (s1 > s2) {
                    return 1;
                } else {
                    return 0;
                }
            });
            return ids;
        };

        info.featureEnabled = function (provider) {
            return info.profileInfo.disabledFeatures.indexOf(provider) == -1;
        };
    }

    function onSuccess(response) {
        copyInfo(response.data, info);
        delay.resolve(info);
    }
    function getServerInfo(realmName, callback) {
        $http.get(authUrl + '/admin/serverinfo', {
            params: {
                realmName: realmName
            }
        }).then(function (response) {
            callback && callback(response);
            onSuccess(response);
        });
    }
    return {
        get: function (realmName, callback) {
            if (realmName) {
                getServerInfo(realmName, callback);
            } else {
                let path = $location.path().substring(1);
                realm = path.startsWith("create") ? $location.path().substring(1).split("/")[2] : $location.path().substring(1).split("/")[1];
                getServerInfo(realm, callback);
            }
            return info;
        },
        reload: function (realmName, callback) {
            if (realmName) {
                getServerInfo(realmName, callback);
            } else {
                let path = $location.path().substring(1);
                realm = path.startsWith("create") ? $location.path().substring(1).split("/")[2] : $location.path().substring(1).split("/")[1];
                getServerInfo(realm);
            }
            return info;
        },
        promise: delay.promise
    };
});

module.service('SetupGuideReq', function () {
    let myData = null;
    async function walkthrough() {
        return await fetch(`${licenseUrl}/marketing/walkthrough`).then(function (res) { return res.json(); }).then(function (data) {
            myData = data.body.message.docsLinks;
        });
    }
    let promise = walkthrough;

    return {
        promise: promise,
        setData: function (data) {
            myData = data;
        },
        getData: function () {
            return myData;
        }
    };
});

module.factory('ClientInitialAccess', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients-initial-access/:id', {
        realm: '@realm',
        id: '@id'
    });
});

module.factory('ClientProtocolMapper', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/protocol-mappers/models/:id', {
        realm: '@realm',
        client: '@client',
        id: "@id"
    }, {
        update: {
            method: 'PUT'
        }
    });
});

module.factory('ClientScopeProtocolMapper', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/client-scopes/:clientScope/protocol-mappers/models/:id', {
        realm: '@realm',
        clientScope: '@clientScope',
        id: "@id"
    }, {
        update: {
            method: 'PUT'
        }
    });
});

module.factory('User', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:userId', {
        realm: '@realm',
        userId: '@userId'
    }, {
        update: {
            method: 'PUT'
        }
    });
});

module.factory('AllUsers', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/all', {
        realm: '@realm',
    });
});

module.factory('AllUsersCount', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/count/all', {
        realm: '@realm'
    });
});

module.factory('Users', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users', {
        realm: '@realm',
    });
});

module.factory('UsersCount', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/count?search=:search', {
        realm: '@realm',
        search: '@search',
    }, {
        query: {
            isArray: false,
            method: 'GET',
            transformResponse: function(data) {
                return angular.fromJson({count:data})
            }
        }
   });
});

module.factory('UserAdditonalClaims', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:user/bo-claims?verificationId=:verificationId', {
        realm: '@realm',
        verificationId: '@verificationId',
    }, {
        query: {
            method: 'GET'
        }
   });
});

module.factory('AdminUsersCount', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:user/admins/count?search=:search', {
        realm: '@realm',
        search: '@search',
        user:'@user'
    }, {
        query: {
            isArray: false,
            method: 'GET',
            transformResponse: function(data) {
                return angular.fromJson({count:data})
            }
        }
   });
});

module.factory('Admin', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/admins', {
        realm: '@realm',
    });
});

module.factory('AdminUser', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/add-admin', {
        realm: '@realm',
    });
});
module.factory('ConvertPictureToUrl', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:userId/upload-userpicture', {
        realm: '@realm',
        userId: '@userId'
    }, {
        create: {
            method: 'POST',
            headers: { 'Content-Type': undefined },
        }
    });
});
module.factory('UserInviteResend', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:userId/resend-invite', {
        realm: '@realm',
        userId: '@userId'
    }, {
        update: {
            method: 'PUT'
        }
    });
});
module.factory('AssignUserToGroups', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:userId/assign-groups', {
        realm: '@realm',
        userId: '@userId'
    }, {
        create: {
            method: 'POST'
        }
    });
});
module.service('UserSearchState', function () {
    this.isFirstSearch = true;
    this.query = {
        max: 20,
        first: 0
    };
});

module.service('AdminSearchState', function () {
    this.isFirstSearch = true;
    this.query = {
        max: 20,
        first: 0
    };
});

module.service('ClientListSearchState', function () {
    this.isFirstSearch = true;
    this.query = {
        max: 10,
        first: 0,
        search: true
    };
});

// Service tracks the last flow selected in Authentication-->Flows tab
module.service('LastFlowSelected', function () {
    this.alias = null;
});

module.service('RealmRoleRemover', function (Notifications, $translate, Dialog, $location) {
    this.remove = function (role, realm) {
        Dialog.confirmDelete(role.name, 'role', function () {
            role.$remove({
                realm: realm.realm,
                role: role.id
            }, function () {
                $location.url("/realms/" + realm.realm + "/roles");
                Notifications.success($translate.instant("role-has-been-deleted"));
            });
        });
    };
});

module.factory('UserSessionStats', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:user/session-stats', {
        realm: '@realm',
        user: '@user'
    });
});
module.factory('UserSessions', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:user/sessions', {
        realm: '@realm',
        user: '@user'
    });
});
module.factory('UserOfflineSessions', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:user/offline-sessions/:client', {
        realm: '@realm',
        user: '@user',
        client: '@client'
    });
});

module.factory('UserSessionLogout', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/sessions/:session', {
        realm: '@realm',
        session: '@session'
    });
});

module.factory('UserLogout', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:user/logout', {
        realm: '@realm',
        user: '@user'
    });
});

module.factory('UserFederatedIdentities', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:user/federated-identity', {
        realm: '@realm',
        user: '@user'
    });
});
module.factory('UserFederatedIdentity', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:user/federated-identity/:provider', {
        realm: '@realm',
        user: '@user',
        provider: '@provider'
    });
});

module.factory('UserConsents', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:user/consents/:client', {
        realm: '@realm',
        user: '@user',
        client: '@client'
    });
});

module.factory('UserImpersonation', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:user/impersonation', {
        realm: '@realm',
        user: '@user'
    });
});

module.factory('UserCredentials', function ($resource) {
    var credentials = {};

    credentials.getCredentials = $resource(authUrl + '/admin/realms/:realm/users/:userId/credentials', {
        realm: '@realm',
        userId: '@userId'
    }).query;

    credentials.getConfiguredUserStorageCredentialTypes = $resource(authUrl + '/admin/realms/:realm/users/:userId/configured-user-storage-credential-types', {
        realm: '@realm',
        userId: '@userId'
    }).query;

    credentials.deleteCredential = $resource(authUrl + '/admin/realms/:realm/users/:userId/credentials/:credentialId', {
        realm: '@realm',
        userId: '@userId',
        credentialId: '@credentialId'
    }).delete;

    credentials.updateCredentialLabel = $resource(authUrl + '/admin/realms/:realm/users/:userId/credentials/:credentialId/userLabel', {
        realm: '@realm',
        userId: '@userId',
        credentialId: '@credentialId'
    }, {
        update: {
            method: 'PUT',
            headers: {
                'Content-Type': 'text/plain;charset=utf-8'
            },
            transformRequest: function (credential, getHeaders) {
                return credential.userLabel;
            }
        }
    }).update;

    credentials.enableCredential = $resource(authUrl + '/admin/realms/:realm/users/:userId/credentials/:credentialId/enable', {
        realm: '@realm',
        userId: '@userId',
        credentialId: '@credentialId'
    }, {
        update: {
            method: 'PUT',
            headers: {
                'Content-Type': 'text/plain;charset=utf-8'
            }
        }
    }).update;

    credentials.disableCredential = $resource(authUrl + '/admin/realms/:realm/users/:userId/credentials/:credentialId/disable', {
        realm: '@realm',
        userId: '@userId',
        credentialId: '@credentialId'
    }, {
        update: {
            method: 'PUT',
            headers: {
                'Content-Type': 'text/plain;charset=utf-8'
            }
        }
    }).update;
    
    credentials.markTokenCredentialAsLastUsed = $resource(authUrl + '/admin/realms/:realm/users/:userId/credentials/:credentialId/mark-as-last-used', {
        realm: '@realm',
        userId: '@userId',
        credentialId: '@credentialId'
    }, {
        update: {
            method: 'PUT',
            headers: {
                'Content-Type': 'text/plain;charset=utf-8'
            }
        }
    }).update;

    credentials.resetPassword = $resource(authUrl + '/admin/realms/:realm/users/:userId/reset-password', {
        realm: '@realm',
        userId: '@userId',
        client_id: '@client_id'
    }, {
        update: {
            method: 'PUT'
        }
    }).update;

    credentials.removeAdminRole = $resource(authUrl + '/admin/realms/:realm/users/:userId/revoke-adminRole', {
        realm: '@realm',
        userId: '@userId',
        client_id: '@client_id'
    }, {
        update: {
            method: 'PUT'
        }
    }).update;

    credentials.addAdminRole = $resource(authUrl + '/admin/realms/:realm/users/:userId/add-adminRole', {
        realm: '@realm',
        userId: '@userId',
        client_id: '@client_id'
    }, {
        update: {
            method: 'PUT'
        }
    }).update;


    credentials.removeTotp = $resource(authUrl + '/admin/realms/:realm/users/:userId/remove-totp', {
        realm: '@realm',
        userId: '@userId'
    }, {
        update: {
            method: 'PUT'
        }
    }).update;

    credentials.disableCredentialTypes = $resource(authUrl + '/admin/realms/:realm/users/:userId/disable-credential-types', {
        realm: '@realm',
        userId: '@userId'
    }, {
        update: {
            method: 'PUT'
        }
    }).update;

    credentials.moveCredentialAfter = $resource(authUrl + '/admin/realms/:realm/users/:userId/credentials/:credentialId/moveAfter/:newPreviousCredentialId', {
        realm: '@realm',
        userId: '@userId',
        credentialId: '@credentialId',
        newPreviousCredentialId: '@newPreviousCredentialId'
    }, {
        update: {
            method: 'POST'
        }
    }).update;

    credentials.moveToFirst = $resource(authUrl + '/admin/realms/:realm/users/:userId/credentials/:credentialId/moveToFirst', {
        realm: '@realm',
        userId: '@userId',
        credentialId: '@credentialId'
    }, {
        update: {
            method: 'POST'
        }
    }).update;

    return credentials;
});

module.factory('UserExecuteActionsEmail', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:userId/execute-actions-email', {
        realm: '@realm',
        userId: '@userId',
        lifespan: '@lifespan',
    }, {
        update: {
            method: 'PUT'
        }
    });
});

module.factory('AccountCreator', function ($resource) {
    var accountCreator = {}
    accountCreator.sendOtp = $resource(authUrl + '/admin/realms/:realm/users/:userId/send-otp-to-account-creator', {
        id: '@realm',
        userId: '@userId'
    }).query;

    accountCreator.addAccountCreator = $resource(authUrl + '/admin/realms/:realm/users/:userId/add-account-creator', {
        id: '@realm',
        userId: '@userId'
    },{
        update: {
            method: 'PUT',
        }
    }).update;
    return accountCreator
});

module.factory('RealmRoleMapping', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:userId/role-mappings/realm', {
        realm: '@realm',
        userId: '@userId'
    });
});

module.factory('RealmRoleMappingCount', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:userId/role-mappings/realm/count', {
        realm: '@realm',
        userId: '@userId'
    }, {
        query: {
            isArray: false,
            method: 'GET',
            params: {},
            transformResponse: function (data) {
                return angular.fromJson(data);
            }
        }
    });
});

module.factory('CompositeRealmRoleMapping', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:userId/role-mappings/realm/composite', {
        realm: '@realm',
        userId: '@userId'
    });
});

module.factory('AvailableRealmRoleMapping', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:userId/role-mappings/realm/available', {
        realm: '@realm',
        userId: '@userId'
    });
});

module.factory('AvailableRealmRoleMappingCount', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:userId/role-mappings/realm/available/count', {
        realm: '@realm',
        userId: '@userId'
    }, {
        query: {
            isArray: false,
            method: 'GET',
            params: {},
            transformResponse: function (data) {
                return angular.fromJson(data);
            }
        }
    });
});


module.factory('ClientRoleMapping', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:userId/role-mappings/clients/:client', {
        realm: '@realm',
        userId: '@userId',
        client: "@client"
    });
});

module.factory('AvailableClientRoleMapping', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:userId/role-mappings/clients/:client/available', {
        realm: '@realm',
        userId: '@userId',
        client: "@client"
    });
});

module.factory('CompositeClientRoleMapping', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:userId/role-mappings/clients/:client/composite', {
        realm: '@realm',
        userId: '@userId',
        client: "@client"
    });
});

module.factory('ClientRealmScopeMapping', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/scope-mappings/realm', {
        realm: '@realm',
        client: '@client'
    });
});

module.factory('ClientAvailableRealmScopeMapping', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/scope-mappings/realm/available', {
        realm: '@realm',
        client: '@client'
    });
});

module.factory('ClientCompositeRealmScopeMapping', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/scope-mappings/realm/composite', {
        realm: '@realm',
        client: '@client'
    });
});

module.factory('ClientClientScopeMapping', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/scope-mappings/clients/:targetClient', {
        realm: '@realm',
        client: '@client',
        targetClient: '@targetClient'
    });
});

module.factory('ClientAvailableClientScopeMapping', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/scope-mappings/clients/:targetClient/available', {
        realm: '@realm',
        client: '@client',
        targetClient: '@targetClient'
    });
});

module.factory('ClientCompositeClientScopeMapping', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/scope-mappings/clients/:targetClient/composite', {
        realm: '@realm',
        client: '@client',
        targetClient: '@targetClient'
    });
});

module.factory('ClientsCount', function($resource) {
return $resource(authUrl + '/admin/realms/:realm/clients/count?:queryParams', {
        realm: '@realm',
        queryParams: '@queryParams'
    }, {
        query: {
            isArray: false,
            method: 'GET',
            transformResponse: function(data) {
                return angular.fromJson({count:data})
            }
        }
   });
})


module.factory('RealmRoles', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/roles', {
        realm: '@realm'
    });
});

module.factory('RoleRealmComposites', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/roles-by-id/:role/composites/realm', {
        realm: '@realm',
        role: '@role'
    });
});

module.factory('RealmPushRevocation', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/push-revocation', {
        realm: '@realm'
    });
});

module.factory('RealmClearUserCache', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clear-user-cache', {
        realm: '@realm'
    });
});

module.factory('RealmClearRealmCache', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clear-realm-cache', {
        realm: '@realm'
    });
});

module.factory('RealmClearKeysCache', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clear-keys-cache', {
        realm: '@realm'
    });
});

module.factory('RealmClearReportQueue', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clear-report-queue', {
        realm: '@realm'
    });
});

module.factory('RealmSessionStats', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/session-stats', {
        realm: '@realm'
    });
});

module.factory('RealmClientSessionStats', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/client-session-stats', {
        realm: '@realm'
    });
});


module.factory('RoleClientComposites', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/roles-by-id/:role/composites/clients/:client', {
        realm: '@realm',
        role: '@role',
        client: "@client"
    });
});

module.factory('RealmSpecialAttributesAvailable', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/special-attributes/available', {
        realm: '@realm'
    });
});

module.factory('RealmSpecialAttributes', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/special-attributes', {
        realm: '@realm'
    });
});

module.factory('RealmSpecialAttributesUpdate', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/special-attributes/update/:tabName', {
        realm: '@realm',
        tabName: '@tabName'
    }, {
        send: {
            method: 'POST',
        }
    });
});

module.factory('RealmSpecialAttributesRotate', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/special-attributes/rotate/:providerId', {
        realm: '@realm',
        providerId: '@providerId'
    }, {
        send: {
            method: 'POST',
        }
    });
});

module.factory('RealmSpecialAttributesGenerate', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/special-attributes/generate/:providerId', {
        realm: '@realm',
        providerId: '@providerId'
    }, {
        send: {
            method: 'POST',
        }
    });
});

/* Custom domain requests starts */
module.factory('CustomDomainCreate', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/dns/txt/create', {
        realm: '@realm'
    }, {
        create: {
            method: 'POST',
        }
    });
});

module.factory('GetAllDomains', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/dns/getAllCustomDomains', {
        realm: '@realm'
    });
});

module.factory('CustomDomainVerify', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/dns/txt/verify', {
        realm: '@realm'
    }, {
        verify: {
            method: 'POST',
        }
    });
});

module.factory('ARecordCreate', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/dns/a/create', {
        realm: '@realm'
    }, {
        create: {
            method: 'POST',
        }
    });
});

module.factory('ARecordVerify', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/dns/a/verify', {
        realm: '@realm'
    }, {
        verify: {
            method: 'POST',
        }
    });
});

module.factory('IngressCreate', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/dns/create/:domain', {
        realm: '@realm',
        domain: '@domain'
    }, {
        create: {
            method: 'POST',
        }
    });
});

module.factory('IngressRemove', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/dns/:domain/remove', {
        realm: '@realm',
        domain: '@domain'
    }, {
        remove: {
            method: 'POST',
        }
    });
});

module.factory('UpdatePrimaryDomain', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/dns/mark-as-primary', {
        realm: '@realm'
    }, {
        create: {
            method: 'POST',
        }
    });
});


/* Custom domain requests ends */

/* Ciba */
module.factory('CibaProviderDetails', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/trx-auth-config-description/:providerId', {
        realm: '@realm',
        client: '@client',
        providerId: '@providerId'
    });
});

module.factory('CibaProviderConfigDetails', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/trx-auth-config/:providerId', {
        realm: '@realm',
        client: '@client',
        providerId: '@providerId'
    }, {
        update: {
            method: 'PUT',
        }
    });
});
module.factory('CibaNotProviderDetails', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/trx-notification-config-description/:providerId', {
        realm: '@realm',
        client: '@client',
        providerId: '@providerId'
    });
});

module.factory('CibaNotProviderConfigDetails', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/trx-notification-config/:providerId', {
        realm: '@realm',
        client: '@client',
        providerId: '@providerId'
    }, {
        update: {
            method: 'PUT',
        }
    });
});

module.factory('CibaResultHookProviderDetails', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/dead-letter-hook-config-description/:providerId', {
        realm: '@realm',
        client: '@client',
        providerId: '@providerId'
    });
});

module.factory('CibaResultHookProviderConfigDetails', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/dead-letter-hook-notification-config/:providerId', {
        realm: '@realm',
        client: '@client',
        providerId: '@providerId'
    }, {
        update: {
            method: 'PUT',
        }
    });
});

module.factory('CibaTutorial', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/trx/tutorial', {
        realm: '@realm',
        client: '@client',
    });
});

module.factory('TestCheck', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/test/check', {
        realm: '@realm',
        client: '@client',
    });
});

module.factory('TestTransaction', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/test/transaction/:actionFlow', {
        realm: '@realm',
        client: '@client',
        actionFlow: '@actionFlow'
    });
});

module.factory('ResultTransaction', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/test/result/:actionFlow', {
        realm: '@realm',
        client: '@client',
        actionFlow: '@actionFlow'
    }, {
        send: {
            method: 'POST'
        }
    });
});


/* Ciba */
//introJS marketing api
module.factory('IntroJsDetails', function ($resource) {
    return $resource(licenseUrl + '/marketing/walkthrough');
});

function clientSelectControl($scope, realm, Client) {
    $scope.clientsUiSelect = {
        minimumInputLength: 1,
        delay: 500,
        allowClear: true,
        query: function (query) {
            var data = { results: [] };
            if ('' == query.term.trim()) {
                query.callback(data);
                return;
            }
            Client.query({ realm: realm, search: true, clientId: query.term.trim(), max: 20 }, function (response) {
                data.results = response;
                query.callback(data);
            });
        },
        formatResult: function (object, container, query) {
            object.text = object.clientId;
            return object.clientId;
        }
    };
}

function roleControl($scope, $route, realm, role, roles, Client,
    ClientRole, RoleById, RoleRealmComposites, RoleClientComposites,
    $http, $location, Notifications, Dialog, ComponentUtils, $translate) {
    $scope.$watch(function () {
        return $location.path();
    }, function () {
        $scope.path = $location.path().substring(1).split("/");
    });

    $scope.$watch('role', function () {
        if (!angular.equals($scope.role, role)) {
            $scope.changed = true;
        }
    }, true);

    $scope.update = function () {
        RoleById.update({
            realm: realm.realm,
            role: role.id
        }, $scope.role, function () {
            $scope.changed = false;
            role = angular.copy($scope.role);
            Notifications.success($translate.instant("changes-saved-to-role"));
        });
    };

    $scope.reset = function () {
        $scope.role = angular.copy(role);
        $scope.changed = false;
    };

    if (!role.id) return;

    $scope.compositeSwitch = role.composite;
    $scope.compositeSwitchDisabled = role.composite;
    $scope.realmRoles = angular.copy(roles);
    $scope.selectedRealmRoles = [];
    $scope.selectedRealmMappings = [];
    $scope.realmMappings = [];
    $scope.clientRoles = [];
    $scope.selectedClientRoles = [];
    $scope.selectedClientMappings = [];
    $scope.clientMappings = [];

    for (var j = 0; j < $scope.realmRoles.length; j++) {
        if ($scope.realmRoles[j].id == role.id) {
            var realmRole = $scope.realmRoles[j];
            var idx = $scope.realmRoles.indexOf(realmRole);
            $scope.realmRoles.splice(idx, 1);
            break;
        }
    }


    clientSelectControl($scope, $route.current.params.realm, Client);

    $scope.selectedClient = null;


    $scope.realmMappings = RoleRealmComposites.query({ realm: realm.realm, role: role.id }, function () {
        for (var i = 0; i < $scope.realmMappings.length; i++) {
            var role = $scope.realmMappings[i];
            for (var j = 0; j < $scope.realmRoles.length; j++) {
                var realmRole = $scope.realmRoles[j];
                if (realmRole.id == role.id) {
                    var idx = $scope.realmRoles.indexOf(realmRole);
                    if (idx != -1) {
                        $scope.realmRoles.splice(idx, 1);
                        break;
                    }
                }
            }
        }
    });

    $scope.addRealmRole = function () {
        $scope.compositeSwitchDisabled = true;
        $scope.selectedRealmRolesToAdd = JSON.parse('[' + $scope.selectedRealmRoles + ']');
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/roles-by-id/' + role.id + '/composites',
            $scope.selectedRealmRolesToAdd).then(function () {
                for (var i = 0; i < $scope.selectedRealmRolesToAdd.length; i++) {
                    var role = $scope.selectedRealmRolesToAdd[i];
                    var idx = ComponentUtils.findIndexById($scope.realmRoles, role.id);
                    if (idx != -1) {
                        $scope.realmRoles.splice(idx, 1);
                        $scope.realmMappings.push(role);
                    }
                }
                $scope.selectedRealmRoles = [];
                $scope.selectedRealmRolesToAdd = [];
                Notifications.success($translate.instant("role-added-to-composite"));
            });
    };

    $scope.deleteRealmRole = function () {
        $scope.compositeSwitchDisabled = true;
        $scope.selectedRealmMappingsToRemove = JSON.parse('[' + $scope.selectedRealmMappings + ']');
        $http.delete(authUrl + '/admin/realms/' + realm.realm + '/roles-by-id/' + role.id + '/composites',
            { data: $scope.selectedRealmMappingsToRemove, headers: { "content-type": "application/json" } }).then(function () {
                for (var i = 0; i < $scope.selectedRealmMappingsToRemove.length; i++) {
                    var role = $scope.selectedRealmMappingsToRemove[i];
                    var idx = ComponentUtils.findIndexById($scope.realmMappings, role.id);
                    if (idx != -1) {
                        $scope.realmMappings.splice(idx, 1);
                        $scope.realmRoles.push(role);
                    }
                }
                $scope.selectedRealmMappings = [];
                $scope.selectedRealmMappingsToRemove = [];
                Notifications.success($translate.instant("role-removed-from-composite"));
            });
    };

    $scope.addClientRole = function () {
        $scope.compositeSwitchDisabled = true;
        $scope.selectedClientRolesToAdd = JSON.parse('[' + $scope.selectedClientRoles + ']');
        $http.post(authUrl + '/admin/realms/' + realm.realm + '/roles-by-id/' + role.id + '/composites',
            $scope.selectedClientRolesToAdd).then(function () {
                for (var i = 0; i < $scope.selectedClientRolesToAdd.length; i++) {
                    var role = $scope.selectedClientRolesToAdd[i];
                    var idx = ComponentUtils.findIndexById($scope.clientRoles, role.id);
                    if (idx != -1) {
                        $scope.clientRoles.splice(idx, 1);
                        $scope.clientMappings.push(role);
                    }
                }
                $scope.selectedClientRoles = [];
                $scope.selectedClientRolesToAdd = [];
                Notifications.success($translate.instant("client-role-added"));
            });
    };

    $scope.deleteClientRole = function () {
        $scope.compositeSwitchDisabled = true;
        $scope.selectedClientMappingsToRemove = JSON.parse('[' + $scope.selectedClientMappings + ']');
        $http.delete(authUrl + '/admin/realms/' + realm.realm + '/roles-by-id/' + role.id + '/composites',
            { data: $scope.selectedClientMappingsToRemove, headers: { "content-type": "application/json" } }).then(function () {
                for (var i = 0; i < $scope.selectedClientMappingsToRemove.length; i++) {
                    var role = $scope.selectedClientMappingsToRemove[i];
                    var idx = ComponentUtils.findIndexById($scope.clientMappings, role.id);
                    if (idx != -1) {
                        $scope.clientMappings.splice(idx, 1);
                        $scope.clientRoles.push(role);
                    }
                }
                $scope.selectedClientMappings = [];
                $scope.selectedClientMappingsToRemove = [];
                Notifications.success($translate.instant("clien-role-removed"));
            });
    };


    $scope.changeClient = function (client) {
        console.log("selected client: ", client);
        if (!client || !client.id) {
            $scope.selectedClient = null;
            return;
        }
        $scope.selectedClient = client;
        $scope.clientRoles = ClientRole.query({ realm: realm.realm, client: client.id }, function () {
            $scope.clientMappings = RoleClientComposites.query({ realm: realm.realm, role: role.id, client: client.id }, function () {
                for (var i = 0; i < $scope.clientMappings.length; i++) {
                    var role = $scope.clientMappings[i];
                    for (var j = 0; j < $scope.clientRoles.length; j++) {
                        var realmRole = $scope.clientRoles[j];
                        if (realmRole.id == role.id) {
                            var idx = $scope.clientRoles.indexOf(realmRole);
                            if (idx != -1) {
                                $scope.clientRoles.splice(idx, 1);
                                break;
                            }
                        }
                    }
                }
            });
            for (var j = 0; j < $scope.clientRoles.length; j++) {
                if ($scope.clientRoles[j] == role.id) {
                    var appRole = $scope.clientRoles[j];
                    var idx = $scope.clientRoles.indexof(appRole);
                    $scope.clientRoles.splice(idx, 1);
                    break;
                }
            }
        }
        );
    };




}


module.factory('Role', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/roles/:role', {
        realm: '@realm',
        role: '@role'
    }, {
        update: {
            method: 'PUT'
        }
    });
});

module.factory('RoleById', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/roles-by-id/:role', {
        realm: '@realm',
        role: '@role'
    }, {
        update: {
            method: 'PUT'
        }
    });
});

module.factory('ClientRole', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/roles/:role', {
        realm: '@realm',
        client: "@client",
        role: '@role'
    }, {
        update: {
            method: 'PUT'
        }
    });
});

module.factory('ClientDefaultClientScopes', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/default-client-scopes/:clientScopeId', {
        realm: '@realm',
        client: "@client",
        clientScopeId: '@clientScopeId'
    }, {
        update: {
            method: 'PUT'
        }
    });
});

module.factory('ClientOptionalClientScopes', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/optional-client-scopes/:clientScopeId', {
        realm: '@realm',
        client: "@client",
        clientScopeId: '@clientScopeId'
    }, {
        update: {
            method: 'PUT'
        }
    });
});

module.factory('ClientEvaluateProtocolMappers', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/evaluate-scopes/protocol-mappers?scope=:scopeParam', {
        realm: '@realm',
        client: "@client",
        scopeParam: "@scopeParam"
    });
});

module.factory('ClientEvaluateGrantedRoles', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/evaluate-scopes/scope-mappings/:roleContainer/granted?scope=:scopeParam', {
        realm: '@realm',
        client: "@client",
        roleContainer: "@roleContainer",
        scopeParam: "@scopeParam"
    });
});

module.factory('ClientEvaluateNotGrantedRoles', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/evaluate-scopes/scope-mappings/:roleContainer/not-granted?scope=:scopeParam', {
        realm: '@realm',
        client: "@client",
        roleContainer: "@roleContainer",
        scopeParam: "@scopeParam"
    });
});

module.factory('ClientEvaluateGenerateExampleToken', function ($resource) {
    var url = authUrl + '/admin/realms/:realm/clients/:client/evaluate-scopes/generate-example-access-token?scope=:scopeParam&userId=:userId';
    return {
        url: function (parameters) {
            return url
                .replace(':realm', parameters.realm)
                .replace(':client', parameters.client)
                .replace(':scopeParam', parameters.scopeParam)
                .replace(':userId', parameters.userId);
        }
    };
});

module.factory('ClientProtocolMappersByProtocol', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/protocol-mappers/protocol/:protocol', {
        realm: '@realm',
        client: "@client",
        protocol: "@protocol"
    });
});

module.factory('ClientScopeProtocolMappersByProtocol', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/client-scopes/:clientScope/protocol-mappers/protocol/:protocol', {
        realm: '@realm',
        clientScope: "@clientScope",
        protocol: "@protocol"
    });
});

module.factory('ClientScopeRealmScopeMapping', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/client-scopes/:clientScope/scope-mappings/realm', {
        realm: '@realm',
        clientScope: '@clientScope'
    });
});

module.factory('ClientScopeRealmScopeMappingCount', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/client-scopes/:clientScope/scope-mappings/realm/count', {
        realm: '@realm',
        clientScope: '@clientScope'
    }, {
        query: {
            isArray: false,
            method: 'GET',
            params: {},
            transformResponse: function (data) {
                return angular.fromJson(data);
            }
        }
    });
});

module.factory('ClientScopeAvailableRealmScopeMapping', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/client-scopes/:clientScope/scope-mappings/realm/available', {
        realm: '@realm',
        clientScope: '@clientScope'
    });
});

module.factory('ClientScopeAvailableRealmScopeMappingCount', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/client-scopes/:clientScope/scope-mappings/realm/available/count', {
        realm: '@realm',
        clientScope: '@clientScope'
    }, {
        query: {
            isArray: false,
            method: 'GET',
            params: {},
            transformResponse: function (data) {
                return angular.fromJson(data);
            }
        }
    });
});

module.factory('ClientScopeCompositeRealmScopeMapping', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/client-scopes/:clientScope/scope-mappings/realm/composite', {
        realm: '@realm',
        clientScope: '@clientScope'
    });
});

module.factory('ClientScopeClientScopeMapping', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/client-scopes/:clientScope/scope-mappings/clients/:targetClient', {
        realm: '@realm',
        clientScope: '@clientScope',
        targetClient: '@targetClient'
    });
});

module.factory('ClientScopeAvailableClientScopeMapping', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/client-scopes/:clientScope/scope-mappings/clients/:targetClient/available', {
        realm: '@realm',
        clientScope: '@clientScope',
        targetClient: '@targetClient'
    });
});

module.factory('ClientScopeCompositeClientScopeMapping', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/client-scopes/:clientScope/scope-mappings/clients/:targetClient/composite', {
        realm: '@realm',
        clientScope: '@clientScope',
        targetClient: '@targetClient'
    });
});


module.factory('ClientSessionStats', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/session-stats', {
        realm: '@realm',
        client: "@client"
    });
});

module.factory('ClientSessionStatsWithUsers', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/session-stats?users=true', {
        realm: '@realm',
        client: "@client"
    });
});

module.factory('ClientSessionCount', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/session-count', {
        realm: '@realm',
        client: "@client"
    });
});

module.factory('ClientUserSessions', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/user-sessions', {
        realm: '@realm',
        client: "@client"
    });
});

module.factory('ClientOfflineSessionCount', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/offline-session-count', {
        realm: '@realm',
        client: "@client"
    });
});

module.factory('ClientOfflineSessions', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/offline-sessions', {
        realm: '@realm',
        client: "@client"
    });
});

module.factory('RealmLogoutAll', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/logout-all', {
        realm: '@realm'
    });
});

module.factory('ClientPushRevocation', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/push-revocation', {
        realm: '@realm',
        client: "@client"
    });
});

module.factory('ClientClusterNode', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/nodes/:node', {
        realm: '@realm',
        client: "@client"
    });
});

module.factory('ClientTestNodesAvailable', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/test-nodes-available', {
        realm: '@realm',
        client: "@client"
    });
});

module.factory('ClientCertificate', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/certificates/:attribute', {
        realm: '@realm',
        client: "@client",
        attribute: "@attribute"
    });
});

module.factory('ClientCertificateGenerate', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/certificates/:attribute/generate', {
        realm: '@realm',
        client: "@client",
        attribute: "@attribute"
    },
        {
            generate: {
                method: 'POST'
            }
        });
});

module.factory('ClientCertificateDownload', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/certificates/:attribute/download', {
        realm: '@realm',
        client: "@client",
        attribute: "@attribute"
    },
        {
            download: {
                method: 'POST',
                responseType: 'arraybuffer'
            }
        });
});

module.factory('Client', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client', {
        realm: '@realm',
        client: '@client'
    }, {
        update: {
            method: 'PUT'
        }
    });
});

module.factory('DuplicateClient', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/duplicate', {
        realm: '@realm',
        client: '@client'
    }, {
        create: {
            method: 'POST'
        }
    });
});

module.factory('ChangeApplicationMode', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/change-sandbox-mode', {
        realm: '@realm',
        client: '@client'
    }, {
        update: {
            method: 'POST'
        }
    });
});

module.factory('ClientSSO', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/sso/:client', {
        realm: '@realm',
        client: '@client'
    }, {
        update: {
            method: 'PUT'
        }
    });
});

module.factory('ClientsSSOCount', function($resource) {
return $resource(authUrl + '/admin/realms/:realm/clients/sso/count?:queryParams', {
        realm: '@realm',
        queryParams: '@queryParams'
    }, {
        query: {
            isArray: false,
            method: 'GET',
            transformResponse: function(data) {
                return angular.fromJson({count:data})
            }
        }
   });
})

module.factory('ClientSSOMetadata', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/sso/:client/metadata', {
        realm: '@realm',
        client: '@client'
    });
});

module.factory('ClientScope', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/client-scopes/:clientScope', {
        realm: '@realm',
        clientScope: '@clientScope'
    }, {
        update: {
            method: 'PUT'
        }
    });
});

module.factory('RealmDefaultClientScopes', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/default-default-client-scopes/:clientScopeId', {
        realm: '@realm',
        clientScopeId: '@clientScopeId'
    }, {
        update: {
            method: 'PUT'
        }
    });
});

module.factory('RealmOptionalClientScopes', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/default-optional-client-scopes/:clientScopeId', {
        realm: '@realm',
        clientScopeId: '@clientScopeId'
    }, {
        update: {
            method: 'PUT'
        }
    });
});


module.factory('ClientDescriptionConverter', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/client-description-converter', {
        realm: '@realm'
    });
});

/*
module.factory('ClientInstallation', function($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/installation/providers/:provider', {
        realm : '@realm',
        client : '@client',
        provider : '@provider'
    });
});
*/



module.factory('ClientInstallation', function ($resource) {
    var url = authUrl + '/admin/realms/:realm/clients/:client/installation/providers/:provider';
    return {
        url: function (parameters) {
            return url.replace(':realm', parameters.realm).replace(':client', parameters.client).replace(':provider', parameters.provider);
        }
    };
});

module.factory('ClientInstallationJBoss', function ($resource) {
    var url = authUrl + '/admin/realms/:realm/clients/:client/installation/jboss';
    return {
        url: function (parameters) {
            return url.replace(':realm', parameters.realm).replace(':client', parameters.client);
        }
    };
});

module.factory('ClientSecret', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/client-secret', {
        realm: '@realm',
        client: '@client'
    }, {
        update: {
            method: 'POST'
        }
    });
});

module.factory('ClientRegistrationAccessToken', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/registration-access-token', {
        realm: '@realm',
        client: '@client'
    }, {
        update: {
            method: 'POST'
        }
    });
});

module.factory('ClientOrigins', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/allowed-origins', {
        realm: '@realm',
        client: '@client'
    }, {
        update: {
            method: 'PUT',
            isArray: true
        }
    });
});

module.factory('ClientServiceAccountUser', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/service-account-user', {
        realm: '@realm',
        client: '@client'
    });
});

module.factory('ClientTryUrl', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/try', {
        realm: '@realm',
        client: '@client'
    });
});

module.factory('Current', function (Realm, $route, $rootScope) {
    var current = {
        realm: null
    };

    $rootScope.$on('$routeChangeStart', function (event, newurl, oldurl) {
        current.realm = angular.copy(realmObject);
    });

    return current;
});

module.factory('TimeUnit', function () {
    var t = {};

    t.autoUnit = function (time) {
        if (!time) {
            return 'Hours';
        }

        var unit = 'Seconds';
        if (time % 60 == 0) {
            unit = 'Minutes';
            time = time / 60;
        }
        if (time % 60 == 0) {
            unit = 'Hours';
            time = time / 60;
        }
        if (time % 24 == 0) {
            unit = 'Days';
            time = time / 24;
        }
        return unit;
    };

    t.toSeconds = function (time, unit) {
        switch (unit) {
            case 'Seconds': return time;
            case 'Minutes': return time * 60;
            case 'Hours': return time * 3600;
            case 'Days': return time * 86400;
            default: throw 'invalid unit ' + unit;
        }
    };

    t.toUnit = function (time, unit) {
        switch (unit) {
            case 'Seconds': return time;
            case 'Minutes': return Math.ceil(time / 60);
            case 'Hours': return Math.ceil(time / 3600);
            case 'Days': return Math.ceil(time / 86400);
            default: throw 'invalid unit ' + unit;
        }
    };

    return t;
});

module.factory('TimeUnit2', function () {
    var t = {};

    t.asUnit = function (time) {

        var unit = 'Minutes';

        if (time) {
            if (time == -1) {
                time = -1;
            } else {
                if (time < 60) {
                    time = 60;
                }

                if (time % 60 == 0) {
                    unit = 'Minutes';
                    time = time / 60;
                }
                if (time % 60 == 0) {
                    unit = 'Hours';
                    time = time / 60;
                }
                if (time % 24 == 0) {
                    unit = 'Days';
                    time = time / 24;
                }
            }
        }

        var v = {
            unit: unit,
            time: time,
            toSeconds: function () {
                switch (v.unit) {
                    case 'Minutes':
                        return v.time * 60;
                    case 'Hours':
                        return v.time * 3600;
                    case 'Days':
                        return v.time * 86400;
                }
            }
        };

        return v;
    };

    return t;
});

module.filter('removeSelectedPolicies', function () {
    return function (policies, selectedPolicies) {
        var result = [];
        for (var i in policies) {
            var policy = policies[i];
            var policyAvailable = true;
            for (var j in selectedPolicies) {
                if (policy.id === selectedPolicies[j].id && !policy.multipleSupported) {
                    policyAvailable = false;
                }
            }
            if (policyAvailable) {
                result.push(policy);
            }
        }
        return result;
    };
});

module.factory('EmailProvider', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/getConfiguredEmailProviders', {
        realm: '@realm'
    });
});

module.factory('IdentityProvider', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/identity-provider/instances/:alias', {
        realm: '@realm',
        alias: '@alias'
    }, {
        update: {
            method: 'PUT'
        }
    });
});

module.factory('IdentityProviderExport', function ($resource) {
    var url = authUrl + '/admin/realms/:realm/identity-provider/instances/:alias/export';
    return {
        url: function (parameters) {
            return url.replace(':realm', parameters.realm).replace(':alias', parameters.alias);
        }
    };
});

module.factory('IdentityProviderFactory', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/identity-provider/providers/:provider_id', {
        realm: '@realm',
        provider_id: '@provider_id'
    });
});

module.factory('IdentityProviderMapperTypes', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/identity-provider/instances/:alias/mapper-types', {
        realm: '@realm',
        alias: '@alias'
    });
});

module.factory('IdentityProviderMappers', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/identity-provider/instances/:alias/mappers', {
        realm: '@realm',
        alias: '@alias'
    });
});

module.factory('IdentityProviderMapper', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/identity-provider/instances/:alias/mappers/:mapperId', {
        realm: '@realm',
        alias: '@alias',
        mapperId: '@mapperId'
    }, {
        update: {
            method: 'PUT'
        }
    });
});

module.factory('AuthenticationFlowPriority', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/executions/priority', {
        realm: '@realm',
    });
});

module.factory('AuthenticationFlowExecutions', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/flows/:alias/executions', {
        realm: '@realm',
        alias: '@alias'
    }, {
        update: {
            method: 'PUT'
        }
    });
});

module.factory('CreateExecutionFlow', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/flows/:alias/executions/flow', {
        realm: '@realm',
        alias: '@alias'
    });
});

module.factory('CreateExecution', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/flows/:alias/executions/execution', {
        realm: '@realm',
        alias: '@alias'
    });
});

module.factory('AuthenticationFlows', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/flows/:flow', {
        realm: '@realm',
        flow: '@flow'
    });
});
module.factory('SaveCustomAuthenticationFlows', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/templates/:templatetype/save', {
        realm: '@realm',
        templatetype: '@templatetype'
    });
});
module.factory('SaveEmailTemplate', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/saveEmailTemplate', {
        realm: '@realm',
    });
});

module.factory('ResetEmailTemplate', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/resetToDefault/:templatename', {
        realm: '@realm',
        templatename: '@templatename'
    });
});
module.factory('TestEmailTemplate', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/testTemplate', {
        realm: '@realm',
    });
});

module.factory('AuthenticationInstalledFlows', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/installed/flows', {
        realm: '@realm'
    });
});

module.factory('AuthenticationInstalledOnboardingFlows', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/installed/onboarding/flows', {
        realm: '@realm'
    });
});

module.factory('AuthenticationInstalledLoginFlows', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/installed/login/flows', {
        realm: '@realm'
    });
});

module.factory('AuthenticationInstalledDirectGrantFlows', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/installed/direct_grant/flows', {
        realm: '@realm'
    });
});

module.factory('AuthenticationInstalledRecoveryFlows', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/installed/recovery/flows', {
        realm: '@realm'
    });
});

module.factory('MarketplaceTemplates', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/marketplace/templates/:category', {
        realm: '@realm',
        category: '@category'
    });
});

module.factory('MarketplaceCategories', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/marketplace/categories', {
        realm: '@realm'
    });
});

module.factory('MarketplaceTemplate', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/marketplace/templates/:category/:templateid', {
        realm: '@realm',
        category: '@category',
        templateid: '@templateid'
    });
});

module.factory('MarketplaceTrialTemplate', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/marketplace/templates/:category/:templateid/trial', {
        realm: '@realm',
        category: '@category',
        templateid: '@templateid'
    });
});

module.factory('MarketplaceTemplateSearch', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/marketplace/templates/search?pattern=:pattern', {
        realm: '@realm',
        pattern: '@pattern'
    });
});

module.factory('AuthenticationFormProviders', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/form-providers', {
        realm: '@realm'
    });
});


module.factory('AuthenticationFormActionProviders', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/form-action-providers', {
        realm: '@realm'
    });
});

module.factory('AuthenticatorProviders', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/authenticator-providers?type=:flowtype', {
        realm: '@realm',
        flowtype: '@flowtype'
    });
});

module.factory('ClientAuthenticatorProviders', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/client-authenticator-providers', {
        realm: '@realm'
    });
});


module.factory('AuthenticationFlowsCopy', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/flows/:alias/copy', {
        realm: '@realm',
        alias: '@alias'
    });
});
module.factory('AuthenticationConfigDescription', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/config-description/:provider', {
        realm: '@realm',
        provider: '@provider'
    });
});
module.factory('PerClientAuthenticationConfigDescription', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/per-client-config-description', {
        realm: '@realm'
    });
});

module.factory('AuthenticationConfig', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/config/:config', {
        realm: '@realm',
        config: '@config'
    }, {
        update: {
            method: 'PUT'
        }
    });
});
module.factory('AuthenticationExecutionConfig', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/executions/:execution/config', {
        realm: '@realm',
        execution: '@execution'
    });
});

module.factory('AuthenticationExecution', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/executions/:execution', {
        realm: '@realm',
        execution: '@execution'
    }, {
        update: {
            method: 'PUT'
        }
    });
});




module.service('SelectRoleDialog', function ($modal) {
    var dialog = {};

    var openDialog = function (title, message, btns) {
        var controller = function ($scope, $modalInstance, title, message, btns) {
            $scope.title = title;
            $scope.message = message;
            $scope.btns = btns;

            $scope.ok = function () {
                $modalInstance.close();
            };
            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
            });
        };

        return $modal.open({
            templateUrl: resourceUrl + '/templates/kc-modal.html',
            controller: controller,
            backdrop: 'static',
            resolve: {
                title: function () {
                    return title;
                },
                message: function () {
                    return message;
                },
                btns: function () {
                    return btns;
                }
            }
        }).result;
    };

    var escapeHtml = function (str) {
        var div = document.createElement('div');
        div.appendChild(document.createTextNode(str));
        return div.innerHTML;
    };

    dialog.confirmDelete = function (name, type, success, cancel) {
        var title = $translate.instant('delete') + $translate.instant(escapeHtml(type.charAt(0).toUpperCase() + type.slice(1)));
        var msg = $translate.instant('dialogs.delete.message') + " " + $translate.instant(type) + ' ' + name + $translate.instant('question-mark');
        var btns = {
            ok: {
                label: 'Delete',
                cssClass: 'btn btn-danger'
            },
            cancel: {
                label: 'Cancel',
                cssClass: 'btn btn-secondary'
            }
        };

        openDialog(title, msg, btns).then(success, cancel);
    };

    dialog.confirmGenerateKeys = function (name, type, success) {
        var title = 'Generate new keys for realm';
        var msg = 'Are you sure you want to permanently generate new keys for ' + name + $translate.instant('question-mark');
        var btns = {
            ok: {
                label: 'Generate Keys',
                cssClass: 'btn btn-danger'
            },
            cancel: {
                label: 'Cancel',
                cssClass: 'btn btn-secondary'
            }
        };

        openDialog(title, msg, btns).then(success);
    };

    dialog.confirm = function (title, message, success, cancel) {
        var btns = {
            ok: {
                label: title,
                cssClass: 'btn btn-danger'
            },
            cancel: {
                label: 'Cancel',
                cssClass: 'btn btn-secondary'
            }
        };

        openDialog(title, message, btns).then(success, cancel);
    };

    return dialog;
});
module.service('SelectRoleDialog', function ($modal) {
    var dialog = {};

    var openDialog = function (title, message, btns) {
        var controller = function ($scope, $modalInstance, title, message, btns) {
            $scope.title = title;
            $scope.message = message;
            $scope.btns = btns;

            $scope.ok = function () {
                $modalInstance.close();
            };
            $scope.cancel = function () {
                $modalInstance.dismiss('cancel');
            };
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
            });
        };

        return $modal.open({
            templateUrl: resourceUrl + '/templates/kc-modal.html',
            controller: controller,
            backdrop: 'static',
            resolve: {
                title: function () {
                    return title;
                },
                message: function () {
                    return message;
                },
                btns: function () {
                    return btns;
                }
            }
        }).result;
    };

    var escapeHtml = function (str) {
        var div = document.createElement('div');
        div.appendChild(document.createTextNode(str));
        return div.innerHTML;
    };

    dialog.confirmDelete = function (name, type, success, cancel) {
        console.log('3aa');
        var title = $translate.instant('delete') + escapeHtml(type.charAt(0).toUpperCase() + type.slice(1));
        var msg = $translate.instant('dialogs.delete.message') + " " + type + ' ' + name + $translate.instant('question-mark');
        var btns = {
            ok: {
                label: 'Delete',
                cssClass: 'btn btn-danger'
            },
            cancel: {
                label: 'Cancel',
                cssClass: 'btn btn-secondary'
            }
        };

        openDialog(title, msg, btns).then(success, cancel);
    };

    dialog.confirmGenerateKeys = function (name, type, success) {
        var title = 'Generate new keys for realm';
        var msg = 'Are you sure you want to permanently generate new keys for ' + name + $translate.instant('question-mark');
        var btns = {
            ok: {
                label: 'Generate Keys',
                cssClass: 'btn btn-danger'
            },
            cancel: {
                label: 'Cancel',
                cssClass: 'btn btn-secondary'
            }
        };

        openDialog(title, msg, btns).then(success);
    };

    dialog.confirm = function (title, message, success, cancel) {
        var btns = {
            ok: {
                label: title,
                cssClass: 'btn btn-danger'
            },
            cancel: {
                label: 'Cancel',
                cssClass: 'btn btn-secondary'
            }
        };

        openDialog(title, message, btns).then(success, cancel);
    };

    return dialog;
});

// To send invite to group users.

module.factory('GroupInvite', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/groups/:groupId/send-group-invite', {
        realm: '@realm',
        groupId: '@groupId',
    }, {
        send: {
            method: 'POST'
        }
    });
});

module.factory('Group', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/groups/:groupId', {
        realm: '@realm',
        userId: '@groupId'
    }, {
        update: {
            method: 'PUT'
        }
    });
});

module.factory('GroupChildren', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/groups/:groupId/children', {
        realm: '@realm',
        groupId: '@groupId'
    });
});

module.factory('GroupsCount', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/groups/count', {
        realm: '@realm'
    },
        {
            query: {
                isArray: false,
                method: 'GET',
                params: {},
                transformResponse: function (data) {
                    return angular.fromJson(data);
                }
            }
        });
});

module.factory('Groups', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/groups', {
        realm: '@realm'
    });
});

module.factory('GroupRealmRoleMapping', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/groups/:groupId/role-mappings/realm', {
        realm: '@realm',
        groupId: '@groupId'
    });
});

module.factory('GroupRealmRoleMappingCount', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/groups/:groupId/role-mappings/realm/count', {
        realm: '@realm',
        groupId: '@groupId'
    }, {
        query: {
            isArray: false,
            method: 'GET',
            params: {},
            transformResponse: function (data) {
                return angular.fromJson(data);
            }
        }
    });
});

module.factory('GroupCompositeRealmRoleMapping', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/groups/:groupId/role-mappings/realm/composite', {
        realm: '@realm',
        groupId: '@groupId'
    });
});

module.factory('GroupAvailableRealmRoleMapping', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/groups/:groupId/role-mappings/realm/available', {
        realm: '@realm',
        groupId: '@groupId'
    });
});

module.factory('GroupAvailableRealmRoleMappingCount', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/groups/:groupId/role-mappings/realm/available/count', {
        realm: '@realm',
        groupId: '@groupId'
    }, {
        query: {
            isArray: false,
            method: 'GET',
            params: {},
            transformResponse: function (data) {
                return angular.fromJson(data);
            }
        }
    });
});


module.factory('GroupClientRoleMapping', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/groups/:groupId/role-mappings/clients/:client', {
        realm: '@realm',
        groupId: '@groupId',
        client: "@client"
    });
});

module.factory('GroupAvailableClientRoleMapping', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/groups/:groupId/role-mappings/clients/:client/available', {
        realm: '@realm',
        groupId: '@groupId',
        client: "@client"
    });
});

module.factory('GroupCompositeClientRoleMapping', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/groups/:groupId/role-mappings/clients/:client/composite', {
        realm: '@realm',
        groupId: '@groupId',
        client: "@client"
    });
});

module.factory('GroupMembership', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/groups/:groupId/members', {
        realm: '@realm',
        groupId: '@groupId'
    });
});

module.factory('AssignUsersToGroup', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/groups/:groupId/assign-users', {
        realm: '@realm',
        groupId: '@groupId'
    }, {
        save: {
            method: 'post'
        }
    });
});

module.factory('RoleList', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/roles', {
        realm: '@realm'
    });
});

module.factory('AssignUsersToRole', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/roles-by-id/:roleId/assign-users', {
        realm: '@realm',
        groupId: '@roleId'
    }, {
        save: {
            method: 'post'
        }
    });
});

/* directory sync api */
module.factory('UserSyncAvailable', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users-sync/available-syncs', {
        realm: '@realm'
    });
});

module.factory('DirectorySyncList', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users-sync/connections', {
        realm: '@realm'
    });
});
//Get Available Sync Connections
module.factory('DirectorySyncConnList', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users-sync/connections', {
        realm: '@realm'
    });
});
//Get Sync Connection using conn_id
module.factory('DirectorySyncInfo', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users-sync/:providerId/connections/:connectionId', {
        realm: '@realm',
        providerId: '@providerId',
        connectionId: '@connectionId'
    });
});
//Create New Sync Connection
module.factory('DirectorySync', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users-sync/connections', {
        realm: '@realm'
    }, {
        create: {
            method: 'post'
        }
    });
});
//Update Existing Sync Connection
module.factory('DirectorySyncUpdate', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users-sync/connections/:connectionId', {
        realm: '@realm',
        providerId: '@providerId',
        connectionId: '@connectionId'
    }, {
        update: {
            method: 'put'
        }
    });
});
//Delete Sync Connection
module.factory('DirectorySyncDelete', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users-sync/connections/:connectionId', {
        realm: '@realm',
        providerId: '@providerId',
        connectionId: '@connectionId'
    }, {
        remove: {
            method: 'delete'
        }
    });
});

module.factory('DirectorySyncRedirect', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users-sync/:providerId/connections/:connectionId/login', {
        realm: '@realm',
        providerId: '@providerId',
        connectionId: '@connectionId'
    });
});
module.factory('DirectorySyncUserData', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users-sync/:providerId/connections/:connectionId/trigger', {
        realm: '@realm',
        providerId: '@providerId',
        connectionId: '@connectionId'
    }, {
        sync: {
            method: 'post'
        }
    });
});
module.factory('DirectorySyncHistory', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users-sync/:providerId/connections/:connectionId/synchistory', {
        realm: '@realm',
        providerId: '@providerId',
        connectionId: '@connectionId'
    });
});
module.factory('DirectorySyncStatus', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users-sync/:providerId/connections/:connectionId/status', {
        realm: '@realm',
        providerId: '@providerId',
        connectionId: '@connectionId',
        triggerId: '@triggerId'
    });
});
module.factory('CancelDirectorySync', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users-sync/:providerId/connections/:connectionId/trigger/cancel', {
        realm: '@realm',
        providerId: '@providerId',
        connectionId: '@connectionId'
    });
});
/* directoru sync api */
module.factory('RoleMembership', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/roles/:role/users', {
        realm: '@realm',
        role: '@role'
    });
});
module.factory('RoleMembershipSearch', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/admins', {
        realm: '@realm'
    });
});
module.factory('ClientRoleList', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/roles', {
        realm: '@realm',
        client: '@client'
    });
});

module.factory('ClientRoleMembership', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/roles/:role/users', {
        realm: '@realm',
        client: '@client',
        role: '@role'
    });
});

module.factory('UserGroupMembership', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:userId/groups', {
        realm: '@realm',
        userId: '@userId'
    });
});

module.factory('UserGroupMapping', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:userId/groups/:groupId', {
        realm: '@realm',
        userId: '@userId',
        groupId: '@groupId'
    }, {
        update: {
            method: 'PUT'
        }
    });
});

module.factory('DefaultGroups', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/default-groups/:groupId', {
        realm: '@realm',
        groupId: '@groupId'
    }, {
        update: {
            method: 'PUT'
        }
    });
});

module.factory('SubComponentTypes', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/components/:componentId/sub-component-types', {
        realm: '@realm',
        componentId: '@componentId'
    });
});

module.factory('Components', function ($resource, ComponentUtils) {
    return $resource(authUrl + '/admin/realms/:realm/components/:componentId', {
        realm: '@realm',
        componentId: '@componentId'
    }, {
        update: {
            method: 'PUT',
            transformRequest: function (componentInstance) {

                if (componentInstance.config) {
                    ComponentUtils.removeLastEmptyValue(componentInstance.config);
                }

                return angular.toJson(componentInstance);
            }
        },
        save: {
            method: 'POST',
            transformRequest: function (componentInstance) {

                if (componentInstance.config) {
                    ComponentUtils.removeLastEmptyValue(componentInstance.config);
                }

                return angular.toJson(componentInstance);
            }
        }
    });
});

module.factory('UserStorageOperations', function ($resource) {
    var object = {};
    object.sync = $resource(authUrl + '/admin/realms/:realm/user-storage/:componentId/sync', {
        realm: '@realm',
        componentId: '@componentId'
    });
    object.removeImportedUsers = $resource(authUrl + '/admin/realms/:realm/user-storage/:componentId/remove-imported-users', {
        realm: '@realm',
        componentId: '@componentId'
    });
    object.unlinkUsers = $resource(authUrl + '/admin/realms/:realm/user-storage/:componentId/unlink-users', {
        realm: '@realm',
        componentId: '@componentId'
    });
    object.simpleName = $resource(authUrl + '/admin/realms/:realm/user-storage/:componentId/name', {
        realm: '@realm',
        componentId: '@componentId'
    });
    return object;
});


module.factory('ClientStorageOperations', function ($resource) {
    var object = {};
    object.simpleName = $resource(authUrl + '/admin/realms/:realm/client-storage/:componentId/name', {
        realm: '@realm',
        componentId: '@componentId'
    });
    return object;
});


module.factory('ClientRegistrationPolicyProviders', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/client-registration-policy/providers', {
        realm: '@realm',
    });
});

module.factory('LDAPMapperSync', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/user-storage/:parentId/mappers/:mapperId/sync', {
        realm: '@realm',
        componentId: '@componentId',
        mapperId: '@mapperId'
    });
});


module.factory('UserGroupMembershipCount', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/users/:userId/groups/count', {
        realm: '@realm',
        userId: '@userId'
    },
        {
            query: {
                isArray: false,
                method: 'GET',
                params: {},
                transformResponse: function (data) {
                    return angular.fromJson(data);
                }
            }
        });
});

module.service('PoliciesSearchState', function () {
    this.isFirstSearch = true;
    this.query = {
        max: 10,
        first: 0,
        search: true
    };
});

module.service('BrandingSearchState', function () {
    this.isFirstSearch = true;
    this.query = {
        max: 10,
        first: 0,
        search: true
    };
});

module.factory('CibaPolicies', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/policies?policytype=ciba', {
        realm: '@realm'
    });
});

module.factory('DeviceApplicationRegister', function ($resource) {
    return $resource(authUrl + '/realms/api/app-updates/:realmId/application/register', {
        realmId: '@realmId'
    },{
        save: {
            method: 'POST'
        }
    });
});

module.factory('GetDeviceApplications', function ($resource) {
    return $resource(authUrl + '/realms/api/app-updates/:realmId/applications/:appType?first=:first&max=:max&appName=:appName&platform=:platform', {
        realmId: '@realmId',
        appType: '@appType',
        first: '@first',
        max: '@max',
        appName: '@appName',
        platform: '@platform'
    });
});

module.factory('DeviceApplication', function ($resource) {
    return $resource(authUrl + '/realms/api/app-updates/:realmId/:appId', {
        realmId: '@realmId',
        appId: '@appId'
    },{
        update: {
            method: 'PUT'
        }
    },{
        remove: {
            method: 'DELETE'
        }
    });
});


module.factory('ActionPolicies', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/policies?policytype=action', {
        realm: '@realm'
    });
});

module.factory('PasswordPolicies', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/policies?policytype=password', {
        realm: '@realm'
    });
});

module.factory('UsernamePolicies', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/policies?policytype=username', {
        realm: '@realm'
    });
});

module.factory('BrandingConfig', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/brandings/config', {
        realm: '@realm'
    });
});

module.factory('BrandingAll', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/brandings/all', {
        realm: '@realm'
    });
});

module.factory('Brandings', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/brandings/:branding', {
        realm: '@realm',
        branding: '@branding'
    }, {
        update: {
            method: 'POST'
        }
    });
});

module.factory('BrandingDefault', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/brandings/default', {
        realm: '@realm'
    });
});

module.factory('ConvertImageToUrl', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/uploadBrandImg', {
        realm: '@realm'
    }, {
        create: {
            method: 'POST',
            headers: { 'Content-Type': undefined },
        }
    });
});


module.factory("ConvertSSOLogoToUrl", function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/upload_picture?type=:type', {
        realm: '@realm',
        client: '@client',
        type: '@type'
    }, {
        create: {
            method: 'POST',
            headers: { 'Content-Type': undefined},
        }
    });
});

module.factory('ConvertLogoToUrl', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/uploadRealmLogo', {
        realm: '@realm'
    }, {
        create: {
            method: 'POST',
            headers: { 'Content-Type': undefined },
        }
    });
});


module.factory('Policies', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/policies/:policy', {
        realm: '@realm',
        policy: '@policy'
    }, {
        update: {
            method: 'POST'
        }
    });
});

module.factory('EmailTemplates', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/getAllEmailTemplates', {
        realm: '@realm'
    });
});

module.factory('ActionsPriority', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/actions/priority', {
        realm: '@realm',
    });
});

module.factory('Actions', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/actions/:action', {
        realm: '@realm',
        action: '@action'
    });
});

module.factory('SubWorkflows', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/sub-workflows', {
        realm: '@realm',
        action: '@action'
    });
});

module.factory('WorkflowPreviewGif', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/actions/gif/:clientId/:action/:actionFlow', {
        realm: '@realm',
        clientId: '@clientId',
        action: '@action',
        actionFlow: '@actionFlow'
    });
});

module.factory('WorkflowVisualizeGif', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/actions/:action/gif', {
        realm: '@realm',
        action: '@action'
    });
});

module.factory("ActionVariables", function ($resource) {
  return $resource(
    authUrl +
      "/admin/realms/:realm/authentication/actions/:actionId/action-variables",
    {
      realm: "@realm",
      actionId: "@actionId",
      actionVariables: "@actionVariables"
    },
    {
      save: {
        method: "POST",
      },
    }
  );
});

module.factory('ActionsList', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/actions', {
        realm: '@realm'
    });
});

module.factory('SubWorkflowsList', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/sub-workflows', {
        realm: '@realm'
    });
});

module.factory('Activity', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/activity', {
        realm: '@realm',
        getSpecificTenant: '@getSpecificTenant'
    });
});

module.factory('BoActivity', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/activity/bo', {
        realm: '@realm',
        getSpecificTenant: '@getSpecificTenant'
    });
});

module.factory('EventsConfigDescription', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/event-config-description/:provider', {
        realm: '@realm',
        provider: '@provider'
    });
});

module.factory('EventsConfig', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/event-config/:provider', {
        realm: '@realm',
        provider: '@provider'
    }, {
        update: {
            method: 'PUT'
        }
    });
});

module.factory('TestEventsConfig', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/test-event-config/:provider', {
        realm: '@realm',
        provider: '@provider'
    });
});

module.factory('MFAConfigDescription', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/mfa-config-description/:provider', {
        realm: '@realm',
        provider: '@provider'
    });
});

module.factory('MarketplaceConfig', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/marketplace/:provider', {
        realm: '@realm',
        provider: '@provider'
    }, {
        update: {
            method: 'PUT'
        }
    });
});

module.factory('MFAConfig', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/mfa-config/:provider', {
        realm: '@realm',
        provider: '@provider'
    }, {
        update: {
            method: 'PUT'
        }
    });
});

module.factory('EmailConfigDescription', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/email-config-description/:provider', {
        realm: '@realm',
        provider: '@provider'
    });
});

module.factory('EmailConfig', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/email-config/:provider', {
        realm: '@realm',
        provider: '@provider'
    }, {
        update: {
            method: 'PUT'
        }
    });
});

module.factory('TestEmailConfig', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/test-email-config/:provider', {
        realm: '@realm',
        provider: '@provider'
    });
});

module.factory('SmsConfigDescription', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/sms-config-description/:provider', {
        realm: '@realm',
        provider: '@provider'
    });
});

module.factory('SmsConfig', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/sms-config/:provider', {
        realm: '@realm',
        provider: '@provider'
    }, {
        update: {
            method: 'PUT'
        }
    });
});

module.factory('TestSmsConfig', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/test-sms-config/:provider', {
        realm: '@realm',
        provider: '@provider'
    });
});

module.factory('LicenseCreator', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/license', {
        realm: '@realm'
    });
});

module.factory('LicenseExtender', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/license/extend', {
        realm: '@realm'
    });
});

// module.factory('LicenseValidator', function ($resource) {
//     return $resource(authUrl + '/admin/realms/:realm/license/validate', {
//         realm: '@realm'
//     });
// });

module.factory('LicenseChecker', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/license/check', {
        realm: '@realm'
    });
});

// Management APIs

module.factory('GetAccessKeys', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/mgmt_access_keys?first=:first&max=:max&search=:search', {
        realm: '@realm',
        first: '@first',
        max: '@max',
        search: '@search'
    });
});

module.factory('CreateAccessKey', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/mgmt_access_keys/generate', {
        realm: '@realm',
    }, {
        save: {
            method: 'POST'
        }
    });
});

module.factory('RotateAccessKey', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/mgmt_access_keys/rotate?id=:accessKey', {
        realm: '@realm',
        accessKey: '@accessKey'
    });
});

module.factory('AccessKey', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/mgmt_access_keys/:accessKey', {
        realm: '@realm',
        accessKey: '@accessKey'
    }, {
        update: {
            method: 'PUT'
        },
        remove: {
            method: 'DELETE'
        }
    });
});

// Cost Calculation for workflow module

module.factory('GetWorkflowEstimatedCost', function ($resource) {
    return $resource(authUrl + '/realms/api/ev/workflow-pricing/:realmId/:actionId', {
        realmId: '@realmId',
        actionId: '@actionId'
    });
});

// License Manager Product and Pricing

module.factory('CreateLicenseProduct', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/license/products/create', {
        realm: '@realm'
    }, {
        create: {
            method: 'POST'
        }
    });
});

module.factory('LicenseProducts', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/license/products', {
        realm: '@realm'
    }, {
        update: {
            method: 'PUT'
        }
    });
});

module.factory('GetProductDetail', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/license/products/:productId', {
        realm: '@realm',
        productId: '@productId'
    });
});

module.factory('CreateLicensePricing', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/license/pricing/create?addToCustomSubtenant=:addToCustomSubtenant', {
        realm: '@realm',
        addToCustomSubtenant: "@addToCustomSubtenant"
    }, {
        create: {
            method: 'POST'
        }
    });
});

module.factory('GetLicensePricing', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/license/pricing/:pricingId?pricingType=:priceType', {
        realm: '@realm',
        pricingId: '@pricingId',
        priceType: '@priceType'
    });
});

module.factory('ImportWorkflow', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/register-required-action/import?alias=:workflowName', {
        realm: '@realm',
        workflowName: '@workflowName'
    }, {
        create : {
            method: 'POST'
        }
    });
});

module.factory('ImportSubWorkflow', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/register-required-action/import?alias=:workflowName&isSubWorkflow=:subWorkflow', {
        realm: '@realm',
        workflowName: '@workflowName'
    }, {
        create : {
            method: 'POST'
        }
    });
});


module.factory('UpdateLicensePricing', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/license/pricing/:pricingId', {
        realm: '@realm',
        pricingId: '@pricingId',
    }, {
        update: {
            method: 'PUT'
        }
    });
});

module.factory('UpdateRealmPricingType', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/license/change-pricing/:realmId?pricingType=:pricingType', {
        realm: '@realm',
        realmId: '@realmId',
        pricingType: '@pricingType'
    }, {
        update: {
            method: 'POST'
        }
    });
});

module.factory('AuditHistory', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/ev/audit-flow', {
        realm: '@realm'
    },{ query: {
        method: 'GET',
        isArray: false,
        transformResponse: function(data) {
          var parsedData = angular.fromJson(data);
          return parsedData;
        }
    }});
});

module.factory('AuditHistoryById', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/ev/audit-flow/:requestId', {
        realm: '@realm',
        requestId: '@requestId'
    },{ query: {
        method: 'GET',
        isArray: true,
        transformResponse: function(data) {
          var parsedData = angular.fromJson(data);
          return parsedData;
        }
    }});
});

module.service('smtpEnabled', function () {
    return {};
});

module.service('enabledSmsProvider', function () {
    return {};
});
module.service('eventsLength', function () {
    return {};
});
module.service('logsEnabled', function () {
    return {};
});
module.service('adminLogsEnabled', function () {
    return {};
});
module.service('FieldValidationUtils', function (ValidationUtils, Notifications, $translate) {
    var utilsCheck = {};
    utilsCheck.validateType = function ($scope) {
        let isValid = true;
        for (const property of $scope.configType.properties) {
            if (property["type"].toLowerCase() === "email" && !ValidationUtils.validateEmail($scope.config.config[property["name"]])) {
                isValid = false;
            } else if (property["type"].toLowerCase() === "url" && !ValidationUtils.validateURL($scope.config.config[property["name"]])) {
                Notifications.error($translate.instant("invalid-gateway-url"));
                isValid = false;
            } else if (property["type"].toLowerCase() === "from_mobile_number" && !ValidationUtils.validateMobileNumber($scope.config.config[property["name"]])) {
                Notifications.error($translate.instant("invalid-phone-number"));
                isValid = false;
            }
        }
        return isValid;
    };
    return utilsCheck;
});

module.service("TextEditor", function ($modal) {
    var dialog = {};
    dialog.open = function (content, success, isReadyOnly) {
        var controller = function ($scope, $modalInstance, $http, Notifications, $translate) {
            $scope.changed = false;
            $scope.cancel = () => {
                $modalInstance.dismiss('cancel');
            }
            $scope.save = () => {
                success($scope.quill.getContents());
                $modalInstance.dismiss('cancel');
            }
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
            });
            $(document).ready(function () {
                $scope.quill = new Quill('#editor', {
                    theme: 'snow',
                    readOnly: isReadyOnly,
                    modules: {
                        toolbar: !isReadyOnly,
                    },
                    placeholder: 'Enter the terms and conditions...',
                });
                $('#saveBtn').prop('disabled', true);
                $scope.quill.setContents(content);
                $scope.quill.on(Quill.events.TEXT_CHANGE, () => { 
                    if (JSON.stringify($scope.quill.getContents()) !== JSON.stringify(content) && JSON.stringify($scope.quill.getContents().ops[0].insert) != JSON.stringify("\n")){
                        $('#saveBtn').prop('disabled', false);
                    } else {
                        $('#saveBtn').prop('disabled', true);
                    }
                });
            });
        };
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/text-editor.html',
            controller: controller,
            backdrop: 'static',
            size: 'lg'
        });
    };
    return dialog;
});


module.service("AddTemplateToWorkflow", function ($modal) {
    var dialog = {};
    dialog.open = function (templates, success, node, isMiddle, closeModal) {
        var controller = function ($scope, $modalInstance, $http, Notifications, $translate, $timeout) {
            $scope.changed = false;
            $scope.searchResults = [];
            $scope.search = '';
            $scope.node = node;
            $scope.templates = templates
            let clicked = false;
            $scope.success = (node, template) => {
                if (!clicked)
                {
                    clicked = true;
                    success(node, template);
                }
            } 

            if (isMiddle) {
                $scope.templates = $scope.templates.filter( template => template.providerId !== "bo-custom-message");
            }
            
            $scope.close = function () {
                $modalInstance.close()
            };
            $scope.handleSearch = (search) => {
                $scope.searchResults = $scope.templates.filter(template => $translate.instant(template.name).toLowerCase().includes(search.toLowerCase()))
            }
            $scope.emptySearch = () => {
                $('#searchInputField').val('');
            }
            $scope.cancel = () => {
                $modalInstance.dismiss('cancel');
            }
            $scope.save = () => {
                $modalInstance.dismiss('cancel');
            }
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
            });
            $timeout(() => $('#searchInputField').focus(), 500);
            closeModal();
        };
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/add-template-to-workflow.html',
            controller: controller,
            backdrop: 'static',
            size: 'md'
        });
    };
    return dialog;
});

module.service("CreateVersion", function ($modal) {
    var dialog = {};
    dialog.open = function (success) {
        var controller = function ($scope, $modalInstance, $http, Notifications, $translate) {
            $scope.changed = false;
            $scope.version = "";
            $scope.handleVersion = (version) =>{
                $scope.changed = version ? true : false;
            }
            $scope.cancel = () => {
                $modalInstance.dismiss('cancel');
            }
            $scope.save = (version) => {
                success(version);
                $modalInstance.dismiss('cancel');
            }
            $scope.$on('$routeChangeStart', function () {
                $modalInstance.close();
            });
        };
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/create-version.html',
            controller: controller,
            backdrop: 'static',
            size: 'md'
        });
    };
    return dialog;
});

module.service('APICurlTest', function ($modal, $timeout) {
    var dialog = {};
    dialog.open = function (realm, requestDetails, basicAuth, clientId) {
        var ctrl = function (
          $scope,
          $translate,
          $http,
          $modalInstance,
          Notifications,
          TestApi
        ) {
        $scope.title = requestDetails.verifierName;
        $scope.modalData = {
            selectedLanguage: "curl",
            apiRequestLoading: false,
            testMetadata: {},
            testResponse: ""
        }
        $scope.supportedLanguages = ['curl'];
        $scope.showTest = false;
        $scope.snippet = "";
        $scope.isTestPossible = true;
        $scope.tooltipTxt = $translate.instant("copy");
        const contentType = {
            appJson :"application/json",
            formMultipart : "multipart/form-data"
        }

        const formInput = `           --form '#fileInput=#value' \\ \n`;


        curl_converter.then((curlConverterJs) => {
            $scope.curlConverterJs = curlConverterJs;
            $scope.supportedLanguages = [...$scope.supportedLanguages,  ...$scope.curlConverterJs.supportedLanguges];
        });

        const curlTemplate = `curl --location --request POST '#requestUrl' \\
            --header 'Authorization: Basic #basicAuth' \\
            --header 'Content-Type: #contentType' \\
            --header 'api-version: 1' \\
            --header 'Accept: application/json' \\
            --data-raw '#requestBody'`;
        $scope.basicCurl = curlTemplate;
        
        $scope.$watch("modalData.selectedLanguage", function () {
            if ($scope.modalData.selectedLanguage === 'curl') {
                $scope.snippet = $scope.basicCurl;
            } else {
                $scope.snippet = $scope.curlConverterJs.convertCurlToSnippets($scope.basicCurl.replaceAll(/\\/g, "\\").replaceAll("\n", ""), $scope.modalData.selectedLanguage);
            }
        })


        const generateRequestBody = (data, curl) => {
            const body = JSON.parse(data);
            const requestBody = {};
            requestBody['requestedVerification'] = requestDetails.verifierName;
            let formString = "";
            if (Object.values(body).find(value => value === "${file}")) {
                formString += formInput.replace("#fileInput", 'requestedVerification').replace("#value", requestDetails.verifierName);
                Object.entries(body).map(([k, v]) => {
                    if (v === "${file}") {
                        formString += formInput.replace("#fileInput", v.replace("${","").replace("}","")).replace("#value", "@/absolute/path/to/file.jpg")
                    } else if (typeof v === "string" && v !== '${GenerateUUID}' && v.startsWith("${") && v.endsWith("}")){
                        formString += formInput.replace("#fileInput", v).replace("${","").replace("}","").replace("#value", v)
                    }
                })
                curl = curl.replace("#contentType", contentType.formMultipart).replace("--data-raw '#requestBody'", "");
                formString = formString.trimStart();
                curl += formString.slice(0, formString.length - 4);
            } else {
                Object.values(body).forEach( (value) => {
                    if (typeof value === "string" && value !== '${GenerateUUID}' && value.startsWith("${") && value.endsWith("}")) {
                        requestBody[value.replace("${","").replace("}","")] = value;
                    } else if (typeof value === "object" && !Array.isArray(value)) {
                         Object.values(value).forEach( v => {
                             requestBody[v.replace("${","").replace("}","")] = v;
                         })
                    }
                })
                curl = curl.replace("#requestBody", JSON.stringify(requestBody))
                curl = curl.replace("#contentType", contentType.appJson)
            }
            return curl;
        }


        const generateCurl = () => {
            $scope.basicCurl = curlTemplate;
            const url = authUrl + `/realms/${realm.realm}/protocol/openid-connect/transaction/`;
            $scope.basicCurl = $scope.basicCurl.replace("#requestUrl", url).replace("#basicAuth", basicAuth)
            $scope.basicCurl = generateRequestBody(requestDetails.requestBody, $scope.basicCurl)
            $scope.snippet = angular.copy($scope.basicCurl);
        }

        const generateTestMetaData = () => {
            const body = JSON.parse(requestDetails.requestBody);
            const requestBody = {};
            requestBody['requestedVerification'] = requestDetails.verifierName;
            Object.values(body).map(value=> {
                if (value === "${file}") {
                    $scope.modalData.testMetadata[value.replace("${","").replace("}","")] =  {
                        input: null,
                        valueKey: value,
                        isFile: true
                    };
                    
                } else if(typeof value === "string" && value.includes("${") && value !== '${GenerateUUID}') {
                    $scope.modalData.testMetadata[value.replace("${","").replace("}","")] = {
                        input: "",
                        valueKey: value
                    }
                } else if (typeof value === "object" && !Array.isArray(value)) {
                    Object.values(value).forEach( v => {
                        $scope.modalData.testMetadata[v.replace("${","").replace("}","")] = {
                        input: "",
                        valueKey: v
                        }
                    })
                }
            })
        }

        $scope.uploadFile = function (key) {
            const inputId = key.replaceAll(' ', '-') + '-test-field-file';
            const fileInput = document.getElementById(inputId);
            if (fileInput) {
                fileInput.onchange = function (event) {
                    const file = event.target.files[0];
                    if (file) {
                        $scope.modalData.testMetadata[key].input = file;
                        $scope.modalData.selectedFile = file.name;
                        $scope.$apply();
                    }
                    fileInput.value = '';
                }
                fileInput.click();
            }
        };

        generateCurl();
        generateTestMetaData();

        $scope.sendTestRequest = function () {
            let requestCurl = angular.copy($scope.basicCurl);
            let hasFile = Object.values($scope.modalData.testMetadata).some(x=>x.isFile);
            let file = null;
            const body = {}
            const formData = new FormData();
            $scope.modalData.apiRequestLoading = true;

            if(hasFile) {
                const url = `${authUrl}/admin/realms/${realm.realm}/clients/${clientId}/test-ocr/api-transaction`;
                const formData = new FormData();

                Object.values($scope.modalData.testMetadata).forEach(value => {
                    if(value.isFile && value.input instanceof File) {
                        formData.append("file", value.input);
                        file = value.input;

                    } else {
                        formData.append(value.valueKey.replace("${","").replace("}",""), value.input);
                    }
                })
                formData.append('requestedVerification', requestDetails.verifierName);
                $http.post(url, formData, {
                    headers: { 'Content-Type': undefined },
                    transformRequest: angular.identity
                })
                .then(function(response) {
                    $scope.modalData.testResponse = JSON.stringify(response.data, null, 2);
                    $scope.modalData.apiRequestLoading = false;
                })
                .catch(function(error) {
                    $scope.modalData.apiRequestLoading = false;
                    Notifications.error($translate.instant("unsupported-file"));
                });

            } else {

                Object.values($scope.modalData.testMetadata).forEach(value => {
                    if (value.valueKey === "${namesList}") {
                        try {
                            const key = value.valueKey.replace("${","").replace("}","");
                            body[key] = value.input;
                            requestCurl = requestCurl.replace(`"${value.valueKey}"`, JSON.stringify(value.input));
                        } catch (error) {
                            Notifications.error($translate.instant("invalid-namelist-json"));
                            return;
                        }
                    } else {
                        requestCurl = requestCurl.replace(value.valueKey, value.input)
                        const key = value.valueKey.replace("${","").replace("}","");
                        body[key] = value.input;
                    }
                })

                body['requestedVerification'] = requestDetails.verifierName;
                TestApi.create({ realm: realm.realm, client: clientId }, { data : body }, function(data) {
                    $scope.modalData.testResponse = JSON.stringify(data, null, 2);
                    $scope.modalData.apiRequestLoading = false;
                });
            }
        }

        $scope.resetMetadata = () => {
            Object.keys($scope.modalData.testMetadata).forEach(key => {
                if (key == 'file') {
                    $scope.modalData.selectedFile = '';
                    $scope.modalData.testMetadata[key.replace("${","").replace("}","")] =  {
                        input: null,
                        valueKey: key,
                        isFile: true
                    };
                } else {
                    $scope.modalData.testMetadata[key] = "";
                }
            })
            $scope.modalData.testResponse = "";
        }

        $scope.close = function () {
            $modalInstance.dismiss("cancel");
        };

        $scope.$on("$routeChangeStart", function () {
            $modalInstance.close();
        });
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
        };

        $modal.open({
            controller: ctrl,
            backdrop: 'static',
            size: 'lg',
            templateUrl: resourceUrl + '/templates/kc-api-curl-test.html',
        });
        
    };

    return dialog;
});

module.service("CustomDomainList", function($modal) {
    var dialog = {};
    dialog.open = function (realm, callback) {
        var controller = function ($scope, $modalInstance, $translate, Dialog, IngressRemove, $route, Notifications, UpdatePrimaryDomain, GetAllDomains) {
            $scope.domains = [];
            $scope.copytooltip = $translate.instant('copy');
            $scope.showLoader = true;
            GetAllDomains.get({ realm: realm.realm}, function (data) {
                $scope.domains = data.body?.customDomains;
                $scope.showLoader = false;
            })
            
            $scope.copyURL = (domain) => {
                $scope.copytooltip = $translate.instant('copied');
                var $temp = $("<input>");
                $("body").append($temp);
                $temp.val(domain).select();
                document.execCommand("copy");
                $temp.remove();
                setTimeout(() => $scope.copytooltip = $translate.instant('copy'), 100)
            };

            $scope.makeAsPrimary = (domain) => {
                $scope.showLoader = true;
                UpdatePrimaryDomain.create(
                    { realm: realm.realm},
                    {
                        domainName:domain.customDomain
                    },
                    function (data) {
                        if (data.status === 200) {
                            Notifications.success($translate.instant('primary-domain-updated'));
                            $scope.showLoader = false;
                            $scope.close();
                        } else {
                            $scope.showLoader = false;
                            Notifications.error($translate.instant('something-went-wrong-error'));
                            $scope.close();
                        }
                    },
                    function (data) {
                        $scope.showLoader = false;
                        Notifications.error($translate.instant('something-went-wrong-error'));
                        $scope.close();
                    }
                )
            }
        
            $scope.realm = realm;
            

            $scope.removeCustomDomain = function (domain) {
                $scope.showLoader = true;
                Dialog.confirmDelete(domain, $translate.instant('custom-domain'), function () {
                    IngressRemove.remove(
                        { realm: realm.realm, domain },
                        {},
                        function (data) {
                            if (data.status === 200) {
                                $scope.showLoader = false;
                                Notifications.success($translate.instant('successfully-deleted'));
                                $scope.close();
                               
                            } else {
                                $scope.showLoader = false;
                                Notifications.error($translate.instant('something-went-wrong-error'));
                                $scope.close();
                            }
                        },
                        function (data) {
                            $scope.showLoader = false;
                            Notifications.error($translate.instant('something-went-wrong-error'));
                            $scope.close();
                        }
                    );
                }, ()=> $scope.showLoader = false );
            };

            $scope.toggleShow = function (index) {
                $scope.showDropdownIndex = index;
            };

            $scope.close = function () {
                callback();
                $modalInstance.dismiss();
            };
        }
        $modal.open({
            templateUrl: resourceUrl + '/partials/modal/custom-domain-list.html',
            controller: controller,
            backdrop: 'static',
            size: 'md',
            resolve: {}
        });
    }
    return dialog;
})

// EZTO Verify Marketplace API Service

module.factory('CreateBOMarketplaceTemplate', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/marketplace/create', {
        realm: '@realm'
    }, {
        create: {
            method: 'POST'
        }
    });
});

module.factory('GetAllBOMarketplaceTemplate', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/marketplace/templates?search=:searchValue&filterByCategory=:category&filterBy=:order&filterByPublishedStatus=:isPublished', {
        realm: '@realm',
        searchValue: '@searchValue',
        category: '@category',
        order: '@order',
        isPublished: '@isPublished'
    });
});

module.factory('BOMarketplaceTemplate', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/marketplace/template/:templateId', {
        realm: '@realm',
        templateId: '@templateId'
    }, {
        remove: {
            method: 'DELETE'
        },
        update: {
            method: 'PUT'
        }
    });
});

//  EZTO Verify Marketplace Category API Service

module.factory('CreateBOMarketplaceCategory', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/marketplace/category/create', {
        realm: '@realm',
    }, {
        create: {
            method: 'POST'
        }
    });
});

module.factory('GetAllBOMarketplaceCategory', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/marketplace/categories?first=:first&last=:last&max=:max&search=:search', {
        realm: '@realm',
        first: '@first',
        last: '@last',
        max: '@max',
        search: '@search'
    });
});

module.factory('BOMarketplaceCategory', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/marketplace/category/:categoryName', {
        realm: '@realm',
        categoryName: '@categoryName'
    }, {
        remove: {
            method: 'DELETE'
        }
    });
});

module.factory('GetValidationConfigs', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/validator-action-config/action-claims', {
        realm: '@realm',
    }, {
        create: {
            method: 'POST'
        }
    });
});

module.factory('UpdateRequiredActions', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/authentication/update-required-actions/:action', {
        realm: '@realm',
    }, {
        update: {
            method: 'PUT'
        }
    });
});



// API provider API's
module.factory('ApiProviders', function ($resource) {
    return $resource(authUrl + '/realms/api/api-connector/:realm/providers', {
        realm: '@realm',
        first: '@first',
        max: '@max',
        search: '@search'
    },{query: {
        method: 'GET',
        isArray: true,
        transformResponse: function(data) {
          var parsedData = angular.fromJson(data);
          return parsedData;
        }
      }});
});

module.factory('ApiVerifiers', function ($resource) {
    return $resource(authUrl + '/realms/api/api-connector/:realm/verifiers', {
        realm: '@realm',
        first: '@first',
        max: '@max',
        search: '@search',
        sort: '@sort',
        category: '@category'
    }, {query: {
        method: 'GET',
        isArray: false,
        transformResponse: function(data) {
          var parsedData = angular.fromJson(data);
          return parsedData;
        }
      }});
});

module.factory('ApiCategories', function ($resource) {
    return $resource(authUrl + '/realms/api/api-connector/:realm/categories', {
        realm: '@realm'
    }, 
    {
        query: {
                method: 'GET',
                isArray: true
            }
        }
   );
})

module.factory('ApiVerifiersName', function ($resource) {
    return $resource(authUrl + '/realms/api/api-connector/:realm/verifiers-name?verifierName=:verifierName', {
        realm: '@realm',
        verifierName: '@verifierName'
    },
    {
        query: {
                method: 'GET',
                isArray: true
            }
        }
   );
})


module.factory('ApiProvidersSync', function ($resource) {
    return $resource(authUrl + '/realms/api/api-connector/:realm/sync',
        { realm: '@realm'
        },
        {
            query: {
                method: 'GET',
                isArray: true
            }
        }
    );
});

module.factory('ApiConfig', function ($resource) {
    return $resource(authUrl + '/realms/api/api-connector/:realm/api-config',
        { realm: '@realm'
        },
        {
            query: {
                method: 'GET',
                isArray: true
            }
        }
    );
});

module.factory('TestApi', function ($resource, $httpParamSerializer) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/test/api-transaction',
    { realm: '@realm', client: '@client' },
    {
        create: {
            method: 'POST',
        }
    });
});

module.factory('FetchClientSecret', function ($resource) {
    return $resource(authUrl + '/admin/realms/:realm/clients/:client/client-secret',
    { realm: '@realm' },
    {
        query: {
            method: 'GET',
        }
    });
});


module.factory('UpdateApiProvidersPriority', function ($resource) {
    return $resource(authUrl + '/realms/api/api-connector/:realm/:providerId/update-provider-priority',
        { realm: '@realm', providerId: '@providerId' },
        {
            update: {
                method: 'PUT',
                isArray: false
            }
        }
    );
});

module.factory('UpdateApiVerifierPriority', function ($resource) {
    return $resource(authUrl + '/realms/api/api-connector/:realm/:verifierId/update-verifier-priority',
        { realm: '@realm', verifierId: '@verifierId' },
        {
            update: {
                method: 'PUT',
                isArray: false
            }
        }
    );
});


module.factory('UpdateApiProviders', function ($resource) {
    return $resource(authUrl + '/realms/api/api-connector/:realm/edit-providers',
        { realm: '@realm' },
        {
            update: {
                method: 'PUT',
                isArray: true
            }
        }
    );
});

module.factory('UpdateApiVerifiers', function ($resource) {
    return $resource(authUrl + '/realms/api/api-connector/:realm/update-verifier',
        { realm: '@realm' },
        {
            update: {
                method: 'PUT',
                isArray: false
            }
        }
    );
});