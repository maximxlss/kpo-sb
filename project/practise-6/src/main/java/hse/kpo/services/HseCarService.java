package hse.kpo.services;

import hse.kpo.interfaces.SellObserver;
import hse.kpo.interfaces.cars.CarProvider;
import hse.kpo.interfaces.CustomerProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Сервис продажи машин.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HseCarService {

    private final CarProvider carProvider;

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
     * Метод продажи машин
     */
    public void sellCars() {
        // получаем список покупателей
        var customers = customerProvider.getCustomers();
        // пробегаемся по полученному списку
        customers.stream().filter(customer -> Objects.isNull(customer.getCar()))
                .forEach(customer -> {
                    var car = carProvider.takeCar(customer);

                    if (Objects.nonNull(car)) {
                        customer.setCar(car);
                        notifySellObservers("Car " + car + " sold to " + customer);
                    } else {
                        log.warn("No car in CarService");
                    }
                });
    }
}