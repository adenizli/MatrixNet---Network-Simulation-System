/**
 * Generic hash table implementation using open hashing (separate chaining).
 * Automatically rehashes when load factor exceeds 0.5.
 * 
 * @param <Payload> the type of elements stored in the hash table
 */
public class HashTable<Payload> {
    /**
     * Available hashing methods for the hash table.
     */
    enum HashingMethods {
        OpenHashing
    }

    /**
     * Configuration constants for the hash table.
     */
    static class Config {
        static final int INITIAL_HASH_TABLE_SIZE = 11;
    }

    private class HashtableItem extends Indexable {
        private Payload payload;

        public HashtableItem(String k, Payload p) {
            super(k);
            this.payload = p;
        }

        public Payload getPayload() {
            return this.payload;
        }

        public void setPayload(Payload p) {
            this.payload = p;
        }
    }

    private final HashingMethods hashingMethod;
    private DoublyLinkedList<HashtableItem>[] table;
    private int tableSize;
    private int numberOfElements;

    /**
     * Creates a new hash table with the specified hashing method.
     * 
     * @param hashingMethod the hashing method to use
     */

    public HashTable() {
        this(HashingMethods.OpenHashing);
    }

    public HashTable(HashingMethods hashingMethod) {
        this(hashingMethod, HashTable.Config.INITIAL_HASH_TABLE_SIZE);
    }

    public HashTable(int initialSize) {
        this(HashingMethods.OpenHashing, initialSize);
    }

    public HashTable(HashingMethods hashingMethod, int initialSize) {
        this.tableSize = Math.max(HashTable.Config.INITIAL_HASH_TABLE_SIZE, initialSize);
        this.table = new DoublyLinkedList[this.tableSize];
        this.hashingMethod = hashingMethod;
    }

    /**
     * Inserts a key-value pair into the hash table.
     * 
     * @param s the key string
     * @param p the payload to store
     */
    public void insert(String s, Payload p) {
        int hash = this.hashFunction(s);
        if (this.table[hash] == null) {
            this.table[hash] = new DoublyLinkedList<>();
        }

        DoublyLinkedList<HashtableItem> bucket = this.table[hash];
        DoublyLinkedListNode<HashtableItem> currentElement = bucket.getHead();

        while (currentElement != null) {
            HashtableItem existing = currentElement.getPayload();
            if (existing.key().equals(s)) {
                existing.setPayload(p); // overwrite existing entry
                return;
            }
            currentElement = currentElement.next;
        }

        bucket.add(new HashtableItem(s, p));
        numberOfElements++;
        handleLoadFactor();
    }

    /**
     * Checks load factor and triggers rehashing if necessary.
     */
    public void handleLoadFactor() {
        double loadFactor = (double) numberOfElements / tableSize;
        if (loadFactor >= 0.5) {
            rehash();
        }
    }

    /**
     * Finds and returns the payload associated with the given key.
     * 
     * @param key the key to search for
     * @return the payload, or null if not found
     */
    public Payload find(String key) {
        int hash = this.hashFunction(key);
        DoublyLinkedList<HashtableItem> list = this.table[hash];
        if (list == null) return null;

        DoublyLinkedListNode<HashtableItem> currentElement = list.getHead();

        while (currentElement != null) {
            HashtableItem item = currentElement.getPayload();

            if (item.key().equals(key)) return item.getPayload();

            currentElement = currentElement.next;
        }
        return null;
    }

    public boolean updateValue(String key, Payload newVal) {
        int hash = this.hashFunction(key);
        DoublyLinkedList<HashtableItem> list = this.table[hash];
        if (list == null) return false;

        DoublyLinkedListNode<HashtableItem> currentElement = list.getHead();

        while (currentElement != null) {
            HashtableItem item = currentElement.getPayload();

            if (item.key().equals(key)) {
                item.setPayload(newVal);
                return true;
            }

            currentElement = currentElement.next;
        }
        return false;
    }

    /**
     * Returns the number of elements in the hash table.
     * 
     * @return the size
     */
    public int size() {
        return this.numberOfElements;
    }

    /**
     * Computes hash code for a string using polynomial rolling hash.
     * 
     * @param s the string to hash
     * @return the hash code
     */
    private int hashFunction(String s) {
        int hash = 0;
        int prime = 31; // Prime number for Horner's method

        for (int i = 0; i < s.length(); i++) {
            hash = (hash * prime + s.charAt(i)) % tableSize;
        }

        return hash;
    }

    /**
     * Doubles the table size and rehashes all elements.
     */
    private void rehash() {
        int prevTableSize = tableSize;
        DoublyLinkedList<HashtableItem>[] prevTable = this.table;

        this.tableSize = this.tableSize * 2;
        this.numberOfElements = 0;
        this.table = new DoublyLinkedList[tableSize];

        int currentIndex = 0;
        while (currentIndex < prevTableSize) {
            DoublyLinkedList<HashtableItem> list = prevTable[currentIndex];
            currentIndex++;
            if (list == null) continue;

            DoublyLinkedListNode<HashtableItem> currentElement = list.getHead();
            while (currentElement != null) {
                HashtableItem item = currentElement.getPayload();
                this.insert(item.key(), item.getPayload());
                currentElement = currentElement.next;
            }

        }
    }

    /**
     * Removes the element with the specified key from the hash table.
     * 
     * @param key the key of the element to remove
     */
    public void remove(String key) {
        int hash = this.hashFunction(key);
        DoublyLinkedList<HashtableItem> list = this.table[hash];
        if (list == null) return;

        DoublyLinkedListNode<HashtableItem> currentElement = list.getHead();
        while (currentElement != null) {
            HashtableItem item = currentElement.getPayload();
            if (item.key().equals(key)) {
                list.remove(currentElement);
                numberOfElements--;
                return;
            }
            currentElement = currentElement.next;
        }
    }

}
