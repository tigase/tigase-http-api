package tigase.http.jaxrs;

import tigase.http.modules.Module;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

public interface JaxRsModule<H extends Handler> extends Module {

	ScheduledExecutorService getExecutorService();

	List<H> getHandlers();

}
