# hypenews
HypeNews is a server side part of an application responsible for retrieving news and comments from predefined sources.
Options:
- YandexConstants.FETCH_COMMENTS_DELAY_IN_HOURS - the period of time in hours since the news has been added, after which bot will start to look for comments;
- News.MIN_NUMBER_OF_COMMENTS - number of comments for the news so that it will be saved to the database. If the number of found comments is less the news will be skipped;
- Agency.minRating - a field in every specific Agency class. Defines a threshold of a comment to be attached to the news.