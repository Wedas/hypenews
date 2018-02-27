package hypenews.searchBot;

import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

/**
 * Class is used as a scheduler to avoid rolling back transactions managed by
 * GlassFish due to timeout and to be able to use @Asyncronous annotation
 */
@Singleton
@Startup
public class BotScheduler {

	@Inject
	YandexNewsParserBot parserBot;

	
	@Schedule(hour = "*/1")
	public void parseNews() {
		parserBot.getNews();
	}

	@Schedule(hour = "*/2")
	public void processNews() {
		parserBot.getAllNewsComments();
	}

	@Schedule(hour = "*", minute="*/45")
	public void processNewsAndComments() {
		parserBot.processNewsAndComments();
	}
	
	@Schedule(hour = "*/2")
	public void cleanUpProcessedNews() {
		parserBot.updateProcessedNews();
	}
}
