package com.qlikverify.transactionState.spi;

import org.keycloak.models.CibaStateModel;
import org.keycloak.provider.Provider;

public interface TransactionStateProvider extends Provider {
    CibaStateModel addCibaState(CibaStateModel model);
    void removeCibaState(CibaStateModel model);
    CibaStateModel getCibaStateById(String cibaStateId);
}
