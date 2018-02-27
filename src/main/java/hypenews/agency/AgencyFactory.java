package hypenews.agency;

public class AgencyFactory {

	private AgencyFactory() {
	}

	public static Agency getAgency(String agencyName) {
		NewsAgencies agency = NewsAgencies.fromName(agencyName);
		switch (agency) {
		case MK:
			return new MKAgency();
		case AIF:
			return new AIFAgency();		
		case SVPRESSA:
			return new SVAgency();
		default:
			return null;
		}

	}

}
