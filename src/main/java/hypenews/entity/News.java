package hypenews.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

@Entity
public class News {
	
	@Transient
	private static final int MIN_NUMBER_OF_COMMENTS = 10;
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name="N_ID")
	private int id;
	
	@OneToMany(mappedBy="news", cascade=CascadeType.PERSIST)	
	private List<Comment> comments = new ArrayList<>();
	@Column(name="STORY_HEAD")
	private String storyHead;
	private String description;
	@Column(name="YANDEX_URL")
	private String yandexURL;	
	@Column(name="NEWS_DATE")
	private LocalDateTime newsDateTime;
	
	public News() {}
	
	public void addComment(Comment comment) {
		comments.add(comment);
	}

	public List<Comment> getComments() {
		return comments;
	}

	public void setComments(List<Comment> comments) {
		this.comments = comments;
	}

	public String getStoryHead() {
		return storyHead;
	}

	public void setStoryHead(String storyHead) {
		this.storyHead = storyHead;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getYandexURL() {
		return yandexURL;
	}

	public void setYandexURL(String yandexURL) {
		this.yandexURL = yandexURL;
	}

	public LocalDateTime getNewsDateTime() {
		return newsDateTime;
	}

	public void setNewsDateTime(LocalDateTime newsDateTime) {
		this.newsDateTime = newsDateTime;
	}

	public void addAllComments(List<Comment> commentsList) {
		for(Comment comment: commentsList)
			comments.add(comment);
	}
	public int getNumberOfComments() {
		return comments.size();
	}
	
	public boolean hasEnoughComments() {
		return this.getNumberOfComments()>=MIN_NUMBER_OF_COMMENTS;
	}
	
	@Override
	public String toString() {
		return String.format("%s\n%s\n%s\n%s\nNumber of comments: %d.", this.getNewsDateTime().toString(),
				this.getStoryHead(), this.getYandexURL(), this.getDescription(), this.getNumberOfComments());
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
}
