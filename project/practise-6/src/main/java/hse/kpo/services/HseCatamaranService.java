package hse.kpo.services;

import hse.kpo.interfaces.SellObserver;
import hse.kpo.interfaces.CustomerProvider;
import hse.kpo.interfaces.catamarans.CatamaranProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Сервис продажи катамаранов.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HseCatamaranService {

    private final CatamaranProvider catamaranProvider;

    private final CustomerProvider customerProvider;

    private final List<SellObserver> sellObservers = new ArrayList<>();

    public void addSellObserver(SellObserver observer) {
        sellObservers.add(observer);
    }

    private void notifySellObservers(String operation) {
        for (SellObserver observer: sellObservers) {
            observer.updateSell(operation);
        }
    }

    /**
     * Метод продажи катамаранов.
     */
    public void sellCatamarans() {
        // получаем список покупателей
        var customers = customerProvider.getCustomers();
        // пробегаемся по полученному списку
        customers.stream().filter(customer -> Objects.isNull(customer.getCatamaran()))
                .forEach(customer -> {
                    var catamaran = catamaranProvider.takeCatamaran(customer);

                    if (Objects.nonNull(catamaran)) {
                        customer.setCatamaran(catamaran);
                        notifySellObservers("Catamaran " + catamaran + " sold to " + customer);
                    } else {
                        log.warn("No catamaran in CatamaranService");
                    }
                });
    }
}