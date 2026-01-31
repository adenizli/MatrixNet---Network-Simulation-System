/**
 * A node in a doubly linked list containing a payload and bidirectional links.
 * <p>
 * Each node maintains references to both its next and previous nodes, enabling
 * efficient bidirectional traversal. The next and prev fields are intentionally
 * package-private for direct manipulation by the DoublyLinkedList class. The
 * payload is mutable (used internally by some structures) once set.
 * 
 * @param <Payload> the type of data stored in this node
 */
public class DoublyLinkedListNode<Payload> {
    private Payload payload;
    DoublyLinkedListNode<Payload> next;
    DoublyLinkedListNode<Payload> prev;

    /**
     * Creates a new node with the specified payload.
     * <p>
     * The next and prev references are initialized to null, indicating this node is
     * not yet part of a list. The payload is immutable and cannot be changed after
     * construction.
     * 
     * @param payload the data to be stored in this node
     */
    public DoublyLinkedListNode(Payload payload) {
        this.payload = payload;
        this.next = null;
        this.prev = null;
    }

    /**
     * Returns the payload stored in this node.
     * 
     * @return the immutable data contained in this node
     */
    public Payload getPayload() {
        return this.payload;
    }

    public void updatePayload(Payload p) {
        this.payload = p;
    }
}
