package tigase.http.modules.setup;

import tigase.http.jaxrs.Handler;

public interface SetupHandler extends Handler {

	String getPath();

	String getTitle();
	
}
