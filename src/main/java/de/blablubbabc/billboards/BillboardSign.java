package de.blablubbabc.billboards;

import java.util.UUID;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;

import de.blablubbabc.billboards.util.SoftBlockLocation;
import de.blablubbabc.billboards.util.Utils;

public class BillboardSign {

	private final SoftBlockLocation location;
	private final UUID creatorUUID; // null if created by the server (by an admin)
	private String lastKnownCreatorName; // null if unknown or created by the server

	private UUID ownerUUID; // null if owned by the server
	private String lastKnownOwnerName; // null if unknown or owned by the server
	private int durationInDays;
	private int price;
	private long startTime;

	private boolean valid = false;

	// shortcut: used when creating a new billboard, creator is null if owned by the server
	public BillboardSign(SoftBlockLocation location, Player creator, int durationInDays, int price) {
		this(location, (creator != null ? creator.getUniqueId() : null), (creator != null ? creator.getName() : null), null, null, durationInDays, price, 0);
	}

	// full: used when loading billboards
	public BillboardSign(	SoftBlockLocation location, UUID creatorUUID, String lastKnownCreatorName,
							UUID ownerUUID, String lastKnownOwnerName, int durationInDays, int price, long startTime) {
		Validate.notNull(location, "Location is null!");
		this.location = location;
		if (creatorUUID == null) {
			Validate.isTrue(Utils.isEmpty(lastKnownCreatorName), "Expecting empty creator name if created by the server!");
			this.creatorUUID = null;
			this.lastKnownCreatorName = null;
		} else {
			Validate.isTrue(!Utils.isEmpty(lastKnownCreatorName), "Empty creator name!");
			this.creatorUUID = creatorUUID;
			this.lastKnownCreatorName = lastKnownCreatorName;
		}

		this.setOwner(ownerUUID, lastKnownOwnerName);
		this.durationInDays = durationInDays;
		this.price = price;
		this.startTime = startTime;
	}

	public boolean isValid() {
		return valid;
	}

	void setValid(boolean valid) {
		this.valid = valid;
	}

	public SoftBlockLocation getLocation() {
		return location;
	}

	public UUID getCreatorUUID() {
		return creatorUUID;
	}

	public boolean hasCreator() {
		return (creatorUUID != null);
	}

	public boolean isCreator(Player player) {
		return player != null && this.isCreator(player.getUniqueId());
	}

	public boolean isCreator(UUID playerUUID) {
		return playerUUID != null && playerUUID.equals(creatorUUID);
	}

	// can return null!
	public String getLastKnownCreatorName() {
		return lastKnownCreatorName;
	}

	public void setLastKnownCreatorName(String lastKnownCreatorName) {
		Validate.isTrue(this.hasCreator(), "Cannot set last known creator name if created by the server!");
		Validate.isTrue(!Utils.isEmpty(lastKnownCreatorName), "Empty name!");
		this.lastKnownCreatorName = lastKnownCreatorName;
	}

	public UUID getOwnerUUID() {
		return ownerUUID;
	}

	public boolean hasOwner() {
		return (ownerUUID != null);
	}

	public boolean isOwner(Player player) {
		return player != null && this.isOwner(player.getUniqueId());
	}

	public boolean isOwner(UUID playerUUID) {
		return playerUUID != null && playerUUID.equals(ownerUUID);
	}

	// can return null!
	public String getLastKnownOwnerName() {
		return lastKnownOwnerName;
	}

	public void setLastOwnerCreatorName(String lastKnownOwnerName) {
		Validate.isTrue(this.hasOwner(), "Cannot set last known owner name if owned by the server!");
		Validate.isTrue(!Utils.isEmpty(lastKnownOwnerName), "Empty name!");
		this.lastKnownOwnerName = lastKnownOwnerName;
	}

	public void setOwner(Player owner) {
		if (owner == null) {
			this.setOwner(null, null);
		} else {
			this.setOwner(owner.getUniqueId(), owner.getName());
		}
	}

	public void setOwner(UUID ownerUUID, String ownerName) {
		if (ownerUUID == null) {
			// owned by the server:
			Validate.isTrue(Utils.isEmpty(ownerName), "Expecting empty owner name if owned by the server!");
			this.ownerUUID = null;
			this.lastKnownOwnerName = null;
		} else {
			// owned by a player:
			Validate.isTrue(!Utils.isEmpty(ownerName), "Empty owner name!");
			this.ownerUUID = ownerUUID;
			this.lastKnownOwnerName = ownerName;
		}
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
		return Utils.addSaturated(startTime, durationInDays * 24L * 3600L * 1000L);
	}

	public long getTimeLeft() {
		return Utils.addSaturated(this.getEndTime(), -System.currentTimeMillis());
	}

	public boolean isRentOver() {
		return !hasOwner() || this.getTimeLeft() <= 0;
	}

	public boolean canEdit(Player player) {
		if (player == null) return false;
		return this.hasOwner() && (this.isOwner(player) || player.hasPermission(BillboardsPlugin.ADMIN_PERMISSION));
	}

	public boolean canBreak(Player player) {
		if (player == null) return false;
		return (!this.hasOwner() && this.isCreator(player)) || player.hasPermission(BillboardsPlugin.ADMIN_PERMISSION);
	}

	// message utilities:

	public String[] getMessageArgs() {
		return new String[] {
				String.valueOf(price),
				String.valueOf(durationInDays),
				this.getMessageCreatorName(),
				this.getMessageCreatorUUID(),
				this.getMessageOwnerName(),
				this.getMessageOwnerUUID()
		};
	}

	public String getMessageCreatorUUID() {
		return Messages.getUUIDStringOrUnknown(creatorUUID);
	}

	public String getMessageCreatorName() {
		if (this.hasCreator()) {
			return Messages.getPlayerNameOrUnknown(lastKnownCreatorName);
		} else {
			return Messages.getMessage(Message.SERVER_OWNER_NAME);
		}
	}

	public String getMessageOwnerUUID() {
		return Messages.getUUIDStringOrUnknown(ownerUUID);
	}

	public String getMessageOwnerName() {
		if (this.hasOwner()) {
			return Messages.getPlayerNameOrUnknown(lastKnownOwnerName);
		} else {
			return Messages.getMessage(Message.SERVER_OWNER_NAME);
		}
	}
}
