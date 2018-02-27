package hypenews.agency;

import java.util.ArrayList;
import java.util.List;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hypenews.entity.Comment;

public class SVAgency extends Agency {

	public SVAgency() {
		minRating = 4;
		name = NewsAgencies.SVPRESSA;
	}

	@Override
	public List<Comment> getComments(String agencyURL) {
		log.info(String.format("Searching for comments at %s", name.value()));
		List<Comment> commentsList = new ArrayList<>();
		try (WebClient client = getConfiguredWebClient()) {

			HtmlPage agencyNewsPage = client.getPage(agencyURL);
			client.waitForBackgroundJavaScript(5 * 1000);
			client.waitForBackgroundJavaScriptStartingBefore(5 * 1000);

			// trying to load all comments
			HtmlElement commentsSection = agencyNewsPage
					.getFirstByXPath("//button[@class='mc-btn2 mc-btn2-bck mc-comment-next']");
			String styleAttrValue = ((HtmlElement) agencyNewsPage
					.getFirstByXPath("//div[@class='mc-pagination']")).getAttribute("style");
			while (!styleAttrValue.equals("display: none;")&&!styleAttrValue.equals("display:none")) {				
				agencyNewsPage = commentsSection.click();
				commentsSection = agencyNewsPage
						.getFirstByXPath("//button[@class='mc-btn2 mc-btn2-bck mc-comment-next']");
				styleAttrValue = ((HtmlElement) agencyNewsPage
						.getFirstByXPath("//div[@class='mc-pagination']")).getAttribute("style");				
			}

			// there is no more possible comments to load on page check whether comments are
			// present
			List<HtmlElement> commentsElementsList = agencyNewsPage.getByXPath("//div[@class='mc-comments']/div[@class='mc-comment']");			
			// if commentsElementsList is empty there are no comments
			if (commentsElementsList.size() == 0) {
				log.info(String.format("Found no comments at %s", name.value()));
				return null;
			}

			for (HtmlElement commentElement : commentsElementsList) {
				HtmlElement ratingElement = commentElement
						.getFirstByXPath("./div/div/div/div/span[@class='mc-comment-rating mc-comment-up']");
				int rating = Integer.valueOf(ratingElement == null ? "0" : ratingElement.getTextContent());

				// skip comments without rating
				if (rating < this.getMinRating())
					continue;
				String commentText = ((HtmlElement) commentElement.getFirstByXPath(
						"./div/div/div/div/div/div[@class='mc-comment-msg']")).getTextContent();
				Comment comment = new Comment(rating, commentText, agencyURL, name);
				commentsList.add(comment);
			}
			log.info(String.format("Found %d comments at %s", commentsList.size(), name.value()));
		} catch (Exception e) {
			log.severe(e.getCause()==null?e.getMessage():e.getCause().getMessage());
		}
		return commentsList.size() == 0 ? null : commentsList;
	}

}
