/**
 * Base class for objects that can be stored in indexed data structures.
 * Provides a unique string key and tracks current index in priority queue.
 */
public class Indexable {
    private final String key;
    private int currentIndex;

    /**
     * Creates an Indexable with the specified key. Index is initialized to -1
     * indicating not in any queue.
     * 
     * @param key the unique identifier
     */
    public Indexable(String key) {
        this.key = key;
        this.currentIndex = -1;
    }

    /**
     * Gets the string key for this object.
     * 
     * @return the unique identifier
     */
    public String key() {
        return this.key;
    }

    /**
     * Gets the current index in the priority queue.
     * 
     * @return the index, or -1 if not in a queue
     */
    public int getCurrentIndex() {
        return this.currentIndex;
    }

    /**
     * Sets the current index in the priority queue.
     * 
     * @param currentIndex the new index
     */
    public void setCurrentIndex(int currentIndex) {
        this.currentIndex = currentIndex;
    }

    /**
     * Gets the string key for lexicographic comparison. Used for tie-breaking in
     * PriorityQueue when all criteria are equal.
     * 
     * @return the key for lexicographic comparison
     */
    public String getLexoCompareKey() {
        return this.key;
    }
}
