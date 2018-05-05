package mlga;

public interface Consumer<T> {
	void call(T t);
}