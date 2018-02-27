package hypenews.agency;

public enum NewsAgencies {

	MK("Московский Комсомолец"), AIF("Аргументы и Факты"), SVPRESSA("Свободная Пресса");

	private final String value;

	private NewsAgencies(String value) {
		this.value = value;
	}

	public String value() {
		return this.value;
	}

	public static boolean contains(String agencyName) {
		for (NewsAgencies agency : NewsAgencies.values()) {
			if (agency.value.equals(agencyName))
				return true;
		}
		return false;
	}
	
	public static NewsAgencies fromName(String agencyName) {
		for(NewsAgencies agency : NewsAgencies.values()) {
			if(agency.value.equals(agencyName))
				return agency;
		}
		return null;
	}

}
