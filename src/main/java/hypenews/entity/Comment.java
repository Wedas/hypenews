package hypenews.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import hypenews.agency.NewsAgencies;

@Entity
public class Comment {

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)	
	private int id;
	@ManyToOne()
	@JoinColumn(name="FK_NEWS_ID")
	private News news;
	private int rating;	
	private String comment;
	private String url;
	@Column(name="AGENCY_NAME")
	private String agencyName;
	
	public Comment() {}
	public Comment(int rating, String comment, String url, NewsAgencies name) {
		this.rating = rating;
		this.comment = comment;
		this.url = url;
		this.setAgencyName(name.value());
	}
	public int getRating() {
		return rating;
	}
	public void setRating(int rating) {
		this.rating = rating;
	}
	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}
	@Override
	public String toString() {
		return String.format("Agency: %s Rating: %d Comment: %s", agencyName, rating, comment);
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getAgencyName() {
		return agencyName;
	}
	public void setAgencyName(String agencyName) {
		this.agencyName = agencyName;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public News getNews() {
		return news;
	}
	public void setNews(News news) {
		this.news = news;
	}
	
}
