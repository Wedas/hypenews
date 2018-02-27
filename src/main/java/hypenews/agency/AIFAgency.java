package hypenews.agency;

import java.util.ArrayList;
import java.util.List;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import hypenews.entity.Comment;

public class AIFAgency extends Agency {

	public AIFAgency() {
		minRating = 4;
		name = NewsAgencies.AIF;
	}

	@Override
	public List<Comment> getComments(String agencyURL) {
		log.info(String.format("Searching for comments at %s", name.value()));
		List<Comment> commentsList = new ArrayList<>();
		try (WebClient client = getConfiguredWebClient()) {
			HtmlPage agencyNewsPage = client.getPage(agencyURL);		

			// check if there are more comments button
			HtmlElement commentsSection = agencyNewsPage.getFirstByXPath("//a[text()=\" Все комментарии \"]");
			HtmlPage allCommentsPage = commentsSection == null ? agencyNewsPage : commentsSection.click();
			
			List<HtmlElement> commentsElementsList = allCommentsPage.getByXPath("//div/ol/li[@class='simple_comment']");

			// if commentsElementsList is empty then there is nothing to process
			if (commentsElementsList.size() == 0)
				return null;

			for (HtmlElement commentElement : commentsElementsList) {

				int rating = Integer
						.valueOf(((HtmlElement) commentElement.getFirstByXPath(".//div[@class='comment_like_number']"))
								.getTextContent());

				// skip comments without rating
				if (rating < this.getMinRating())
					continue;
				String commentText = ((HtmlElement) commentElement
						.getFirstByXPath(".//div[@class='comment_border_margin']")).getTextContent();
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
