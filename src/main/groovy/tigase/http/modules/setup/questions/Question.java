package tigase.http.modules.setup.questions;

import tigase.http.modules.setup.pages.Page;

public abstract class Question {

	private final String id;
	private Page page;
	private boolean secret = false;

	public Question(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return page.getId() + "_" + getId();
	}

	public boolean isSecret() {
		return secret;
	}

	public void setSecret(boolean secret) {
		this.secret = secret;
	}

	public void setPage(Page page) {
		this.page = page;
	}

	public abstract void setValues(String[] values);
}
