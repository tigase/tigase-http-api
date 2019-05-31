package tigase.http.modules.setup.questions;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class MultiAnswerQuestion extends Question {

	private final Supplier<String[]> getter;
	private final Consumer<String[]> setter;

	public MultiAnswerQuestion(String id, Supplier<String[]> getter, Consumer<String[]> setter) {
		super(id);
		this.getter = getter;
		this.setter = setter;
	}

	public String[] getValues() {
		return getter.get();
	}

	public void setValues(String[] value) {
		setter.accept(value);
	}

}
