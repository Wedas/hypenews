package hypenews.agency;

import java.util.ArrayList;
import java.util.List;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import hypenews.entity.Comment;

public class MKAgency extends Agency {

	public MKAgency() {
		minRating = 10;
		name = NewsAgencies.MK;
	}

	@Override
	public List<Comment> getComments(String agencyURL) {
		log.info(String.format("Searching for comments at %s", name.value()));
		List<Comment> commentsList = new ArrayList<>();
		try (WebClient client = getConfiguredWebClient()){
			HtmlPage agencyNewsPage = client.getPage(agencyURL);

			HtmlElement commentsSection = agencyNewsPage.getFirstByXPath("//li[text()=\"Комментарии\"]");

			// if there are no comments we should skip this source
			if (commentsSection == null)
				return null;

			HtmlPage commentsPage = commentsSection.click();

			// check if there are more comments
			HtmlElement buttonAllComments = commentsPage.getFirstByXPath("//input[@value='Все']");
			HtmlPage allCommentsPage = buttonAllComments == null ? commentsPage : buttonAllComments.click();

			List<HtmlElement> commentsElementsList = allCommentsPage.getByXPath("//div[@class='comment']");

			for (HtmlElement commentElement : commentsElementsList) {
				// skip responses to other comments
				if (commentElement.getFirstByXPath(".//div[@class='quote']") != null)
					continue;
				int rating = Integer
						.valueOf(((HtmlElement) commentElement.getFirstByXPath(".//div/span")).getTextContent());

				// skip comments without rating
				if (rating < this.getMinRating())
					continue;
				String commentText = ((HtmlElement) commentElement.getFirstByXPath(".//p")).getTextContent();
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
