package conditionalproto;

public class HandlerWrapper<T> {
    private final int id;
    private final T handler;

    public HandlerWrapper(int id, T handler) {
        this.id = id;
        this.handler = handler;
    }

    public int getId() {
        return id;
    }

    public T getHandler() {
        return handler;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HandlerWrapper<?> that = (HandlerWrapper<?>) o;
        return id ==that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
