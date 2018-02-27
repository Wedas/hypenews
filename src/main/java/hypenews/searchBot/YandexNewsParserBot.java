package hypenews.searchBot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import hypenews.agency.Agency;
import hypenews.agency.AgencyFactory;
import hypenews.agency.NewsAgencies;
import hypenews.dao.NewsDAO;
import hypenews.entity.Comment;
import hypenews.entity.News;
import hypenews.utils.SearchBotUtils;
import hypenews.utils.YandexConstants;

@Singleton
public class YandexNewsParserBot {

	@Resource
	SessionContext									context;

	@Inject
	NewsDAO											newsDAO;

	// holds news to process later
	private Map<String, LocalDateTime>				newsMapToProcess		= new ConcurrentHashMap<>();

	// holds daily processed news to avoid multiple processing of the same news
	private Map<String, LocalDateTime>				processedNews			= new ConcurrentHashMap<>();

	// holds news and comments to persist after retrieving all comments
	private Map<News, Queue<Future<List<Comment>>>>	futureNewsCommentsMap	= new ConcurrentHashMap<>();
	private Logger									log;

	@PostConstruct
	public void init() {
		log = SearchBotUtils.getLogger(YandexNewsParserBot.class);
		log.info("YandexNewsParser initialized.");
	}

	@Asynchronous
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public void getNews() {
		Document yandexMainPage = Jsoup.parse(getHTMLPageAsString(YandexConstants.YANDEX_URL));
		Elements newsListContainers = yandexMainPage.getElementsByClass(YandexConstants.NEWS_LIST_CLASS);
		for (Element newsListContainer : newsListContainers) {
			Elements newsListElements = newsListContainer.getElementsByClass(YandexConstants.NEWS_ITEM_CLASS);
			for (Element newsListItem : newsListElements) {
				Elements listAElements = newsListItem.getElementsByTag(YandexConstants.NEWS_A_TAG);
				for (Element listAElement : listAElements) {
					String newsURL = listAElement.attr(YandexConstants.NEWS_HREF_ATTR);
					StringTokenizer tokenizer = new StringTokenizer(newsURL, YandexConstants.DELIMETER);
					newsURL = tokenizer.nextToken();
					if (!newsMapToProcess.containsKey(newsURL) && !processedNews.containsKey(newsURL)) {
						newsMapToProcess.put(newsURL, LocalDateTime.now());
						log.info(String.format("Added news to process. URL: %s", newsURL));
					}
				}
			}
			// skip home news
			break;
		}
		log.info(String.format("Total number of news to process: %s", newsMapToProcess.size()));
	}

	private String getHTMLPageAsString(String url) {
		StringBuilder html = new StringBuilder();
		try {
			URL yandexURL = new URL(url);
			URLConnection connection = yandexURL.openConnection();
			connection.setRequestProperty("User-Agent",
					"Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:58.0) Gecko/20100101 Firefox/58.0");
			BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String htmlPart;
			while ((htmlPart = br.readLine()) != null) {
				html.append(htmlPart);
			}

		} catch (Exception e) {
			log.severe(e.getCause() == null ? e.getMessage() : e.getCause().getMessage());
		}
		return html.toString();
	}

	@Asynchronous
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public void getAllNewsComments() {

		if (newsMapToProcess.isEmpty()) {
			log.info("No news to process");
			return;
		}

		for (String newsURL : newsMapToProcess.keySet()) {
			if (LocalDateTime.now()
					.isAfter(newsMapToProcess.get(newsURL).plusHours(YandexConstants.FETCH_COMMENTS_DELAY_IN_HOURS))) {
				context.getBusinessObject(YandexNewsParserBot.class).getParticularNewsComments(newsURL);
			}
		}

		log.info(String.format("News items remained to process: %d", newsMapToProcess.size()));
	}

	@Asynchronous
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public void getParticularNewsComments(String newsURL) {
		Map<String, String> urlAgencyMap = new HashMap<>();
		News news = new News();
		news.setYandexURL(newsURL);
		news.setNewsDateTime(newsMapToProcess.get(newsURL));

		// get a page with news description
		try (WebClient client = getConfiguredWebClient()) {
			HtmlPage newsPage = client.getPage(newsURL);

			// get a short description
			HtmlElement descElement = newsPage
					.getFirstByXPath(String.format("//div[@class='%s']", YandexConstants.NEWS_DESC_CLASS));
			String newsDescription = descElement.getTextContent();
			news.setDescription(newsDescription);

			// get a storyHead
			HtmlElement headElement = newsPage
					.getFirstByXPath(String.format("//h1[@class='%s']", YandexConstants.NEWS_STORY_HEAD));
			String storyHead = headElement.getTextContent();
			news.setStoryHead(storyHead);

			// get a URL to all sources
			HtmlElement sourcesElement = newsPage
					.getFirstByXPath(String.format("//a[@class='%s']", YandexConstants.NEWS_ALL_SOURCES_CLASS));
			String sourcesURL = newsPage
					.getFullyQualifiedUrl(sourcesElement.getAttribute(YandexConstants.NEWS_HREF_ATTR)).toString();

			log.info(
					String.format("Processing news '%s'. All sources yandex URL: %s", news.getStoryHead(), sourcesURL));

			HtmlPage currentPage = client.getPage(sourcesURL);

			// getting agencies till there is 'next' button
			while (currentPage != null) {
				List<HtmlElement> agencyElements = currentPage
						.getByXPath(String.format("//div[@class='%s']", YandexConstants.NEWS_AGENCIES_CLASS));

				for (HtmlElement agencyElement : agencyElements) {
					String agencyName = ((HtmlElement) agencyElement.getFirstByXPath(
							String.format(".//div[@class='%s']", YandexConstants.NEWS_AGENCY_NAME_CLASS)))
									.getTextContent();

					String agencyNewsURL = ((HtmlElement) agencyElement
							.getFirstByXPath(String.format(".//%s", YandexConstants.NEWS_A_TAG)))
									.getAttribute(YandexConstants.NEWS_HREF_ATTR);

					if (NewsAgencies.contains(agencyName)) {
						urlAgencyMap.put(agencyNewsURL, agencyName);
						log.info(String.format("Found agency: %s", agencyName));
					}
				}

				HtmlElement nextButton = currentPage.getFirstByXPath(String.format("//%s[@class='%s']",
						YandexConstants.NEWS_A_TAG, YandexConstants.NEXT_BUTTON_CLASS));
				currentPage = nextButton == null ? null : nextButton.click();

			}
		} catch (Exception e) {
			log.severe(e.getCause() == null ? e.getMessage() : e.getCause().getMessage());
		}

		if (!urlAgencyMap.isEmpty()) {
			context.getBusinessObject(YandexNewsParserBot.class).searchForNewsAndComments(news, urlAgencyMap);
		} else
			log.info(String.format("No agencies found for the news '%s'. URL: %s Skipping this news.",
					news.getStoryHead(), news.getYandexURL()));

		processedNews.put(newsURL, LocalDateTime.now());
		// remove processed news from the map to process
		newsMapToProcess.remove(newsURL);
	}

	@Asynchronous
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public void searchForNewsAndComments(News news, Map<String, String> urlAgencyMap) {
		log.info(String.format("Getting comments for news: '%s'. URL: %s Number of sources is %d.", news.getStoryHead(),
				news.getYandexURL(), urlAgencyMap.size()));
		Queue<Future<List<Comment>>> futureCommentsList = new ConcurrentLinkedQueue<>();
		for (String agencyURL : urlAgencyMap.keySet()) {
			String agencyName = urlAgencyMap.get(agencyURL);
			Future<List<Comment>> futureSingleAgencyList = context.getBusinessObject(YandexNewsParserBot.class)
					.getAgencyCommentsForNews(agencyName, agencyURL);
			futureCommentsList.add(futureSingleAgencyList);
			log.info(String.format("Added future comments from %s", agencyName));
		}

		// add queue with future comments to the map to process after completion
		futureNewsCommentsMap.put(news, futureCommentsList);
	}

	@Asynchronous
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public Future<List<Comment>> getAgencyCommentsForNews(String agencyName, String agencyURL) {
		Agency agency = AgencyFactory.getAgency(agencyName);
		return new AsyncResult<List<Comment>>(agency.getComments(agencyURL));
	}

	private void saveNews(News news) {
		log.info("Found enough comments. Starting to save the news to the database...");
		newsDAO.persist(news);
		log.info("News is successfully persisted");
	}

	private WebClient getConfiguredWebClient() {
		WebClient client = new WebClient(BrowserVersion.FIREFOX_52);
		java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);
		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
		client.getOptions().setJavaScriptEnabled(true);
		client.getOptions().setThrowExceptionOnScriptError(false);
		client.getOptions().setThrowExceptionOnFailingStatusCode(false);
		client.setJavaScriptTimeout(10 * 1000);
		client.getOptions().setTimeout(10 * 1000);
		client.getCookieManager().setCookiesEnabled(true);
		client.setAjaxController(new NicelyResynchronizingAjaxController());
		return client;
	}

	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public void processNewsAndComments() {
		if (futureNewsCommentsMap.isEmpty()) {
			log.info("Map of future news and comments is empty. Nothing to process");
			return;
		}

		for (News news : futureNewsCommentsMap.keySet()) {
			Queue<Future<List<Comment>>> futureCommentsList = futureNewsCommentsMap.get(news);
			futureNewsCommentsMap.remove(news);
			for (Future<List<Comment>> futureSingleAgencyList : futureCommentsList) {
				try {
					List<Comment> commentsList = futureSingleAgencyList.get();
					// if comments are present add them to the news
					if (commentsList != null) {
						news.addAllComments(commentsList);
						for (Comment comment : commentsList)
							comment.setNews(news);
					}
				} catch (Exception e) {
					log.severe(String.format("Exception occured while retrieving comments. %s",
							e.getCause() == null ? e.getMessage() : e.getCause().getMessage()));
				}

			}

			// if number of comments is enough then store the processed news and
			// comments in a database
			if (!news.hasEnoughComments()) {
				log.info(String.format("Skip news '%s'. Number of comments is %d. URL: %s", news.getStoryHead(),
						news.getNumberOfComments(), news.getYandexURL()));
				return;
			}
			saveNews(news);
		}
	}

	@Asynchronous
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public void updateProcessedNews() {
		if (processedNews.isEmpty())
			return;
		for (String newsYandexURL : processedNews.keySet()) {
			LocalDateTime processedNewsTime = processedNews.get(newsYandexURL);
			if (processedNewsTime.plusHours(24).isBefore(LocalDateTime.now()))
				processedNews.remove(newsYandexURL);
		}
	}
}
