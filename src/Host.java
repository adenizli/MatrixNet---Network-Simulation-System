
import java.util.ArrayList;

/**
 * Represents a host (access point) in the network. Stores host identity,
 * clearance level, and incident backdoors.
 */
public class Host extends Indexable {
    private final String id;
    private final int clearanceLevel;
    private final HashTable<Backdoor> backdoors;
    private final ArrayList<Backdoor> backdoorList;

    public Host(String id, int clearanceLevel) {
        super(id);
        this.id = id;
        this.clearanceLevel = clearanceLevel;
        this.backdoors = new HashTable<>(HashTable.HashingMethods.OpenHashing);
        this.backdoorList = new ArrayList<>();
    }

    public void addBackdoor(Backdoor bd) {
        this.backdoors.insert(bd.key(), bd);
        this.backdoorList.add(bd);
    }

    public boolean hasBackdoor(Host dest) {
        String bdKey;

        // String#compareTo is not limited to {-1,0,1}, so we must check sign.
        if (this.key().compareTo(dest.key()) > 0) bdKey = this.key() + dest.key();
        else bdKey = dest.key() + this.key();

        Backdoor bd = this.backdoors.find(bdKey);
        return bd != null;
    }

    public Backdoor findBackdoor(Host dest) {
        String bdKey = Backdoor.generateBackdoorKey(this, dest);
        return this.backdoors.find(bdKey);
    }

    public String getId() {
        return id;
    }

    public int getClearanceLevel() {
        return clearanceLevel;
    }

    /** Returns backdoors incident to this host (read-only iteration intended). */
    public Iterable<Backdoor> getBackdoors() {
        return backdoorList;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Host host) {
            return host.id.equals(this.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}