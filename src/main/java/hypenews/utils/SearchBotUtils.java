package hypenews.utils;

import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class SearchBotUtils {

	public static Logger getLogger(Class className) {
		Logger logger = Logger.getLogger(className.getName());
		try {
			FileHandler handler;
			handler = new FileHandler("hypenews.log", true);
			handler.setFormatter(new SimpleFormatter());
			logger.addHandler(handler);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return logger;
	}

}
