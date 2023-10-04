package com.isoterik.microservices.currencyconversionservice;

import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@RestController
public class CurrencyConversionController {
    private final Environment environment;
    private final CurrencyExchangeProxy proxy;

    public CurrencyConversionController(Environment environment, CurrencyExchangeProxy proxy) {
        this.environment = environment;
        this.proxy = proxy;
    }

    @GetMapping("/currency-conversion/from/{from}/to/{to}/quantity/{quantity}")
    public CurrencyConversion calculateCurrencyConversion(@PathVariable String from, @PathVariable String to,
                                                          @PathVariable int quantity) {
        var response = new RestTemplate().getForEntity("http://localhost:8000/currency-exchange/from/{from}/to/{to}",
                CurrencyConversion.class, from, to);
        var conversion = response.getBody();

        return getCurrencyConversion(from, to, quantity, conversion);
    }

    @GetMapping("/currency-conversion-feign/from/{from}/to/{to}/quantity/{quantity}")
    public CurrencyConversion calculateCurrencyConversionFeign(@PathVariable String from, @PathVariable String to,
                                                          @PathVariable int quantity) {
        var conversion = proxy.retrieveExchangeValue(from, to);

        return getCurrencyConversion(from, to, quantity, conversion);
    }

    private CurrencyConversion getCurrencyConversion(@PathVariable String from, @PathVariable String to, @PathVariable int quantity, CurrencyConversion conversion) {
        if (conversion == null)
            throw new RuntimeException("Unable to find data for " + from + " to " + to);

        conversion.setEnvironment(String.format("%s/%s", conversion.getEnvironment(),
                environment.getProperty("local.server.port")));
        conversion.setQuantity(BigDecimal.valueOf(quantity));
        conversion.setTotalCalculatedAmount(conversion.getConversionMultiple().multiply(conversion.getQuantity()));

        return conversion;
    }
}
