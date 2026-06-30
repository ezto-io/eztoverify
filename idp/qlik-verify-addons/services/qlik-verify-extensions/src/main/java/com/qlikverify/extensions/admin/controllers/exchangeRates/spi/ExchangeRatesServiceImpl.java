package com.qlikverify.extensions.admin.controllers.exchangeRates.spi;

import com.qlikverify.exchangeRates.spi.ExchangeRatesService;
import com.qlikverify.extensions.admin.controllers.exchangeRates.jpa.ExchangeRatesEntity;
import org.infinispan.Cache;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ExchangeRatesServiceImpl implements ExchangeRatesService {

    private final Cache<String, Double> cache;

    private final EntityManager entityManager;

    public ExchangeRatesServiceImpl(KeycloakSession session) {
        this.cache = fetchCache(session);
        this.entityManager = session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    @Override
    public boolean isExchangeRatesAvailable() {
        TypedQuery<Long> countAll = entityManager.createNamedQuery("countAll", Long.class);
        List<Long> resultList = countAll.getResultList();
        if (!resultList.isEmpty()) {
            Long count = resultList.get(0);
            return count > 0;
        }

        return false;
    }

    @Override
    public Optional<Double> getRate(String currencyCode) {
        Double cachedRate = cache.get(currencyCode);
        if (cachedRate != null) {
            return Optional.of(cachedRate);
        }

        ExchangeRatesEntity rate = entityManager.find(ExchangeRatesEntity.class, currencyCode);
        if (rate != null) {
            cache.put(currencyCode, rate.getRate());
            return Optional.of(rate.getRate());
        }

        return Optional.empty();
    }

    @Override
    @Transactional
    public void updateRates(Map<String, Double> newRates) {
        for (Map.Entry<String, Double> entry : newRates.entrySet()) {
            String currencyCode = entry.getKey();
            Double newRate = entry.getValue();

            ExchangeRatesEntity rate = entityManager.find(ExchangeRatesEntity.class, currencyCode);
            if (rate != null) {
                rate.setRate(newRate);
                entityManager.merge(rate);
            } else {
                rate = new ExchangeRatesEntity();
                rate.setCurrencyCode(currencyCode);
                rate.setRate(newRate);
                entityManager.persist(rate);
            }

            cache.put(currencyCode, newRate);
        }

    }

    private Cache<String, Double> fetchCache(KeycloakSession session) {
        InfinispanConnectionProvider infinispanProvider = session.getProvider(InfinispanConnectionProvider.class);
        return infinispanProvider.getCache("exchangeRates");
    }

    @Override
    public void close() {
        //NOP
    }
}
