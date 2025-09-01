package conditionalproto;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class HandlerMap {
    private final AtomicInteger counter = new AtomicInteger(0);
    private final Map<Integer, Object> registry = new ConcurrentHashMap<>();

    /**
     * Wraps the given handler in a HandlerWrapper with an automatically assigned unique id.
     *
     * @param handler the handler to wrap
     * @param <T>     the type of the handler
     * @return a new HandlerWrapper containing the handler and its assigned id
     */
    public <T> short register(T handler) {
        int id = counter.incrementAndGet();
        registry.put(id, handler);
        return (short) id;
    }

    /**
     * Retrieves the handler associated with the given id.
     *
     * @param id the unique identifier of the handler
     * @return the handler if found, or null otherwise
     */
    public Object getHandlerById(int id) {
        return registry.get(id);
    }

    /**
     * Returns the entire registry mapping ids to handlers.
     *
     * @return the registry map
     */
    public Map<Integer, Object> getRegistry() {
        return registry;
    }
}

