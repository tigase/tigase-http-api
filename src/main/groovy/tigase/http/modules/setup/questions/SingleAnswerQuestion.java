package tigase.http.modules.setup.questions;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class SingleAnswerQuestion extends Question {

	private final Supplier<String> getter;
	private final String label;
	private final Consumer<String> setter;

	public SingleAnswerQuestion(String id, Supplier<String> getter, Consumer<String> setter) {
		this(id, null, getter, setter);
	}

	public SingleAnswerQuestion(String id, String label, Supplier<String> getter, Consumer<String> setter) {
		super(id);
		this.label = label;
		this.getter = getter;
		this.setter = setter;
	}

	public String getLabel() {
		return label;
	}

	public String getValue() {
		return getter.get();
	}

	public void setValue(String value) {
		setter.accept(value);
	}

	public boolean isSelected(String value) {
		return value != null && value.equals(getValue());
	}

	@Override
	public void setValues(String[] values) {
		setValue((values == null || values.length == 0) ? null : (values[0].trim().isEmpty() ? null : values[0]));
	}

}
