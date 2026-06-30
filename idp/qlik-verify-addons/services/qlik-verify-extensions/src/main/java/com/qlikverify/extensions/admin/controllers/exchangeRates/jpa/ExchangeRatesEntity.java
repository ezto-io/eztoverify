package com.qlikverify.extensions.admin.controllers.exchangeRates.jpa;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Setter
@Getter
@Entity
@Table(name = "exchange_rates")
@NamedQueries({@NamedQuery(name = "countAll", query = "SELECT COUNT(e) FROM ExchangeRatesEntity e")})
public class ExchangeRatesEntity {
    @Id
    @Column(name = "currency_code")
    private String currencyCode;

    private double rate;
}