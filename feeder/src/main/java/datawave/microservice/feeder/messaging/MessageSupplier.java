package datawave.microservice.feeder.messaging;

import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.util.function.Supplier;

public class MessageSupplier implements Supplier<Flux<Message<String>>> {
    private final Sinks.Many<Message<String>> messagingSink = Sinks.many().multicast().onBackpressureBuffer();
    
    public boolean send(Message<String> toSend) {
        return messagingSink.tryEmitNext(toSend).isSuccess();
    }
    
    @Override
    public Flux<Message<String>> get() {
        return messagingSink.asFlux().subscribeOn(Schedulers.boundedElastic()).share();
    }
}
