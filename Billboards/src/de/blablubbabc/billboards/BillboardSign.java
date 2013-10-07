package de.blablubbabc.billboards;

public class BillboardSign {
	private SoftLocation location;
	private String creator;
	private String owner;
	private int durationInDays;
	private int price;
	private long startTime;
	
	public BillboardSign(SoftLocation location, String creator, String owner, int durationInDays, int price, long startTime) {
		this.location = location;
		this.creator = (creator == null || creator.isEmpty()) ? "SERVER" : creator;
		this.owner = (owner == null || owner.isEmpty()) ? "SERVER" : owner;
		this.durationInDays = durationInDays;
		this.price = price;
		this.startTime = startTime;
	}

	public SoftLocation getLocation() {
		return location;
	}

	public void setLocation(SoftLocation location) {
		this.location = location;
	}

	public String getCreator() {
		return creator;
	}
	
	public boolean hasCreator() {
		return !this.creator.equals("SERVER");
	}
	
	public String getOwner() {
		return owner;
	}
	
	public boolean hasOwner() {
		return !this.owner.equals("SERVER");
	}

	public void setOwner(String owner) {
		this.owner = owner == null ? "SERVER" : owner;
	}
	
	public void resetOwner() {
		setOwner(null);
		startTime = 0;
	}

	public int getDurationInDays() {
		return durationInDays;
	}

	public void setDurationInDays(int durationInDays) {
		this.durationInDays = durationInDays;
	}

	public int getPrice() {
		return price;
	}

	public void setPrice(int price) {
		this.price = price;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	
	public long getEndTime() {
		return startTime + (durationInDays * 24 * 3600 * 1000);
	}
	
	public long getTimeLeft() {
		return getEndTime() - System.currentTimeMillis();
	}
	
	public boolean isRentOver() {
		return !hasOwner() || getTimeLeft() <= 0;
	}
}
