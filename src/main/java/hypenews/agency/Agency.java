package hypenews.agency;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import hypenews.entity.Comment;
import hypenews.utils.SearchBotUtils;

public abstract class Agency {

	protected Logger	log	= SearchBotUtils.getLogger(getClass());
	protected int		minRating;
	protected NewsAgencies name;

	public int getMinRating() {
		return minRating;
	}

	public abstract List<Comment> getComments(String agencyURL);

	protected WebClient getConfiguredWebClient() {
		WebClient client = new WebClient(BrowserVersion.FIREFOX_52);
		java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);
		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
		client.getOptions().setJavaScriptEnabled(true);
		client.getOptions().setThrowExceptionOnScriptError(false);
		client.getOptions().setThrowExceptionOnFailingStatusCode(false);		
		client.setJavaScriptTimeout(10*1000);
		client.getOptions().setTimeout(10*1000);
		client.getCookieManager().setCookiesEnabled(true);
		client.setAjaxController(new NicelyResynchronizingAjaxController());
		return client;
	}
}
