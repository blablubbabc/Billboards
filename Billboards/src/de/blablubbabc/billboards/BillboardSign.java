package de.blablubbabc.billboards;

public class BillboardSign {

	private SoftLocation location;
	private String creatorName;
	private String ownerName;
	private int durationInDays;
	private int price;
	private long startTime;

	public BillboardSign(SoftLocation location, String creatorName, String ownerName, int durationInDays, int price, long startTime) {
		this.location = location;
		this.creatorName = (creatorName == null || creatorName.isEmpty()) ? "SERVER" : creatorName;
		this.ownerName = (ownerName == null || ownerName.isEmpty()) ? "SERVER" : ownerName;
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
	
	public String getCreatorName() {
		return creatorName;
	}

	public boolean hasCreator() {
		return !creatorName.equals("SERVER");
	}
	
	public String getOwnerName() {
		return ownerName;
	}

	public boolean hasOwner() {
		return !ownerName.equals("SERVER");
	}

	public void setOwner(String owner) {
		this.ownerName = owner == null ? "SERVER" : owner;
	}

	public void resetOwner() {
		this.setOwner(null);
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
		return this.getEndTime() - System.currentTimeMillis();
	}

	public boolean isRentOver() {
		return !hasOwner() || this.getTimeLeft() <= 0;
	}
}