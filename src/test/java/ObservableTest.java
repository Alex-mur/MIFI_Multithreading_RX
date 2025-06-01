import fun.justdevelops.CustomRx.Disposable;
import fun.justdevelops.CustomRx.Observable;
import fun.justdevelops.CustomRx.Observer;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

public class ObservableTest {

    // Проверка базовой подписки с Observer
    @Test
    void subscribeWithObserver() {
        List<Integer> items = new ArrayList<>();
        AtomicBoolean isCompleted = new AtomicBoolean(false);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Observable<Integer> observable = Observable.create(emitter -> {
            emitter.onNext(1);
            emitter.onNext(2);
            emitter.onNext(3);
            emitter.onComplete();
        });

        Observer<Integer> observer = new Observer<>() {
            @Override
            public void onNext(Integer item) {
                items.add(item);
            }

            @Override
            public void onError(Throwable t) {
                error.set(t);
            }

            @Override
            public void onComplete() {
                isCompleted.set(true);
            }
        };

        Disposable disposable = observable.subscribe(observer);

        assertEquals(Arrays.asList(1, 2, 3), items);
        assertTrue(isCompleted.get());
        assertNull(error.get());
        assertFalse(disposable.isDisposed());
    }

    // Проверка подписки с отдельными обработчиками
    @Test
    void subscribeWithHandlers() {
        List<String> items = new ArrayList<>();
        AtomicBoolean isCompleted = new AtomicBoolean(false);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Observable<String> observable = Observable.create(emitter -> {
            emitter.onNext("A");
            emitter.onNext("B");
            emitter.onComplete();
        });

        Disposable disposable = observable.subscribe(
                items::add,
                error::set,
                () -> isCompleted.set(true)
        );

        assertEquals(Arrays.asList("A", "B"), items);
        assertTrue(isCompleted.get());
        assertNull(error.get());
        assertFalse(disposable.isDisposed());
    }

    // Проверка преобразования через map
    @Test
    void mapOperation() {
        List<String> results = new ArrayList<>();
        Observable<Integer> numbers = Observable.create(emitter -> {
            emitter.onNext(10);
            emitter.onNext(20);
            emitter.onComplete();
        });

        numbers.map(num -> "Value: " + num)
                .subscribe(results::add, e -> {}, () -> {});

        assertEquals(Arrays.asList("Value: 10", "Value: 20"), results);
    }

    // Проверка фильтрации через filter
    @Test
    void filterOperation() {
        List<Integer> results = new ArrayList<>();
        Observable<Integer> numbers = Observable.create(emitter -> {
            emitter.onNext(5);
            emitter.onNext(10);
            emitter.onNext(15);
            emitter.onComplete();
        });

        numbers.filter(num -> num > 7)
                .subscribe(results::add, e -> {}, () -> {});

        assertEquals(Arrays.asList(10, 15), results);
    }

    // Проверка обработки ошибок в Observable
    @Test
    void errorHandling() {
        AtomicReference<Throwable> receivedError = new AtomicReference<>();
        RuntimeException testError = new RuntimeException("Test error");

        Observable<String> observable = Observable.create(emitter -> {
            emitter.onNext("OK");
            emitter.onError(testError);
        });

        observable.subscribe(
                item -> {},
                receivedError::set,
                () -> {}
        );

        assertEquals(testError, receivedError.get());
    }

    // Проверка обработки ошибок в map
    @Test
    void mapErrorHandling() {
        AtomicReference<Throwable> receivedError = new AtomicReference<>();
        RuntimeException testError = new RuntimeException("Map error");

        Observable<Integer> numbers = Observable.create(emitter -> {
            emitter.onNext(1);
            emitter.onNext(2);
        });

        numbers.map(num -> {
                    if (num == 2) throw testError;
                    return num;
                })
                .subscribe(
                        item -> {},
                        receivedError::set,
                        () -> {}
                );

        assertEquals(testError, receivedError.get());
    }

    // Проверка обработки ошибок в filter
    @Test
    void filterErrorHandling() {
        AtomicReference<Throwable> receivedError = new AtomicReference<>();
        RuntimeException testError = new RuntimeException("Filter error");

        Observable<Integer> numbers = Observable.create(emitter -> {
            emitter.onNext(1);
            emitter.onNext(2);
        });

        numbers.filter(num -> {
                    if (num == 2) throw testError;
                    return true;
                })
                .subscribe(
                        item -> {},
                        receivedError::set,
                        () -> {}
                );

        assertEquals(testError, receivedError.get());
    }

    // Проверка отмены подписки через Disposable
    @Test
    void disposableStopsEvents() {
        AtomicInteger counter = new AtomicInteger(0);
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicReference<Observer<Integer>> emitterRef = new AtomicReference<>();
        AtomicReference<Disposable> disposableRef = new AtomicReference<>();

        // Создаем Observable с контролируемой эмиссией
        Observable<Integer> observable = Observable.create(emitter -> {
            emitterRef.set(emitter); // Сохраняем emitter для внешнего контроля
        });

        Observer<Integer> observer = new Observer<>() {
            @Override
            public void onNext(Integer item) {
                counter.incrementAndGet();
                if (item == 2) {
                    disposableRef.get().dispose(); // Теперь Disposable доступен
                }
            }

            @Override
            public void onError(Throwable t) {}

            @Override
            public void onComplete() {
                completed.set(true);
            }
        };

        // Сначала создаем подписку
        disposableRef.set(observable.subscribe(observer));

        // Теперь эмитируем элементы по одному с внешним контролем
        Observer<Integer> emitter = emitterRef.get();
        emitter.onNext(0);
        emitter.onNext(1);
        emitter.onNext(2); // Здесь срабатывает отписка
        emitter.onNext(3);
        emitter.onNext(4);
        emitter.onComplete();

        assertEquals(3, counter.get()); // Получены 0, 1, 2
        assertFalse(completed.get()); // onComplete не должен дойти
        assertTrue(disposableRef.get().isDisposed()); // Теперь проверка проходит
    }

    @Test
    void flatMapTransformsAndFlattensItems() {
        List<Integer> results = new ArrayList<>();
        Observable<Integer> source = Observable.create(emitter -> {
            emitter.onNext(1);
            emitter.onNext(2);
            emitter.onComplete();
        });

        source.flatMap(num -> Observable.create(innerEmitter -> {
            innerEmitter.onNext(num * 10);
            innerEmitter.onNext(num * 20);
            innerEmitter.onComplete();
        })).subscribe(
                item -> results.add((Integer)item),  // Исправлено: лямбда вместо method reference
                e -> {},
                () -> {}
        );

        assertEquals(Arrays.asList(10, 20, 20, 40), results);
    }

    @Test
    void flatMapHandlesEmptyInnerObservables() {
        List<Integer> results = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);

        Observable<Integer> source = Observable.create(emitter -> {
            emitter.onNext(1);
            emitter.onNext(2);
            emitter.onComplete();
        });

        source.flatMap(num -> Observable.<Integer>create(innerEmitter -> {
            innerEmitter.onComplete();
        })).subscribe(
                item -> results.add(item),  // Исправлено
                e -> {},
                () -> completed.set(true)
        );

        assertTrue(results.isEmpty());
        assertTrue(completed.get());
    }

    @Test
    void flatMapPropagatesMapperErrors() {
        AtomicReference<Throwable> receivedError = new AtomicReference<>();
        RuntimeException testError = new RuntimeException("Mapper error");

        Observable<Integer> source = Observable.create(emitter -> {
            emitter.onNext(1);
            emitter.onNext(2);
        });

        source.flatMap(num -> {
            if (num == 2) throw testError;
            return Observable.just(num * 10);
        }).subscribe(
                i -> {},  // Исправлено
                receivedError::set,
                () -> {}
        );

        assertEquals(testError, receivedError.get());
    }

    @Test
    void flatMapPropagatesInnerObservableErrors() {
        AtomicReference<Throwable> receivedError = new AtomicReference<>();
        RuntimeException testError = new RuntimeException("Inner error");

        Observable<Integer> source = Observable.create(emitter -> {
            emitter.onNext(1);
            emitter.onNext(2);
        });

        source.flatMap(num -> Observable.create(innerEmitter -> {
            if (num == 2) innerEmitter.onError(testError);
            else innerEmitter.onNext(num);
        })).subscribe(
                i -> {},  // Исправлено
                receivedError::set,
                () -> {}
        );

        assertEquals(testError, receivedError.get());
    }
}