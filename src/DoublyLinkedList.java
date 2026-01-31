/**
 * A generic doubly linked list implementation with O(1) operations at both
 * ends.
 * <p>
 * This list maintains references to both head and tail nodes, enabling
 * efficient insertion and removal at both ends. It tracks its size for
 * constant-time size queries. The list supports bidirectional traversal through
 * its nodes.
 * 
 * @param <Payload> the type of elements stored in the list
 */
public class DoublyLinkedList<Payload> {
    private DoublyLinkedListNode<Payload> head;
    private DoublyLinkedListNode<Payload> tail;
    private int size;

    /**
     * Creates an empty doubly linked list.
     */
    public DoublyLinkedList() {
        this.head = null;
        this.tail = null;
        this.size = 0;
    }

    /**
     * Returns the payload of the first element in the list.
     * <p>
     * Time Complexity: O(1)
     * 
     * @return the data in the first node
     * @throws NullPointerException if the list is empty
     */
    public Payload getFirst() {
        return this.head.getPayload();
    }

    /**
     * Returns the payload of the last element in the list.
     * <p>
     * Time Complexity: O(1)
     * 
     * @return the data in the last node
     * @throws NullPointerException if the list is empty
     */
    public Payload getLast() {
        return this.tail.getPayload();
    }

    /**
     * Adds an element to the end of the list.
     * <p>
     * This is equivalent to {@link #addLast(Object)}.
     * <p>
     * Time Complexity: O(1)
     * 
     * @param payload the element to be added to the list
     */
    public void add(Payload payload) {
        DoublyLinkedListNode<Payload> newNode = new DoublyLinkedListNode<>(payload);
        if (this.head == null) {
            this.head = newNode;
            this.tail = newNode;
        } else {
            newNode.prev = this.tail;
            this.tail.next = newNode;
            this.tail = newNode;
        }
        this.size++;
    }

    /**
     * Inserts an element at the beginning of the list.
     * <p>
     * Time Complexity: O(1)
     * 
     * @param payload the element to be added at the front
     */
    public void addFirst(Payload payload) {
        DoublyLinkedListNode<Payload> newNode = new DoublyLinkedListNode<>(payload);
        if (this.head == null) {
            this.head = newNode;
            this.tail = newNode;
        } else {
            newNode.next = this.head;
            this.head.prev = newNode;
            this.head = newNode;
        }
        this.size++;
    }

    /**
     * Appends an element to the end of the list.
     * <p>
     * Time Complexity: O(1)
     * 
     * @param payload the element to be added at the end
     */
    public void addLast(Payload payload) {
        DoublyLinkedListNode<Payload> newNode = new DoublyLinkedListNode<>(payload);
        if (this.tail == null) {
            this.head = newNode;
            this.tail = newNode;
        } else {
            newNode.prev = this.tail;
            this.tail.next = newNode;
            this.tail = newNode;
        }
        this.size++;
    }

    /**
     * Removes the first occurrence of the specified element from the list.
     * <p>
     * Traverses the list from head to tail, comparing each element using
     * {@link Object#equals(Object)}. If a match is found, the element is removed
     * and the method returns. If no match is found, the list remains unchanged.
     * <p>
     * Time Complexity: O(n) where n is the number of elements in the list
     * 
     * @param payload the element to be removed from the list
     */
    public void remove(Payload payload) {
        DoublyLinkedListNode<Payload> currentNode = this.head;
        while (currentNode != null) {
            if (currentNode.getPayload().equals(payload)) {
                if (currentNode.prev != null) {
                    currentNode.prev.next = currentNode.next;
                } else {
                    // removing head
                    this.head = currentNode.next;
                }

                if (currentNode.next != null) {
                    currentNode.next.prev = currentNode.prev;
                } else {
                    // removing tail
                    this.tail = currentNode.prev;
                }

                this.size--;
                return;
            }
            currentNode = currentNode.next;
        }
    }

    public void remove(DoublyLinkedListNode<Payload> node) {
        if (node.prev != null) {
            node.prev.next = node.next;
        } else {
            this.head = node.next;
        }
        if (node.next != null) {
            node.next.prev = node.prev;
        } else {
            this.tail = node.prev;
        }
    }

    /**
     * Removes and returns the first element from the list.
     * <p>
     * Properly handles edge cases including empty lists and single-element lists,
     * ensuring all pointers (head, tail, prev) are correctly updated.
     * <p>
     * Time Complexity: O(1)
     * 
     * @return the payload of the removed first element, or null if the list is
     * empty
     */
    public Payload removeFirst() {
        if (this.head == null) return null;

        Payload payload = this.head.getPayload();
        this.head = this.head.next;
        if (this.head != null) {
            this.head.prev = null; // Fix dangling prev pointer
        } else {
            this.tail = null; // List is now empty
        }
        this.size--;
        return payload;
    }

    /**
     * Removes and returns the last element from the list.
     * <p>
     * Properly handles edge cases including empty lists and single-element lists,
     * ensuring all pointers (head, tail, next) are correctly updated.
     * <p>
     * Time Complexity: O(1)
     * 
     * @return the payload of the removed last element, or null if the list is empty
     */
    public Payload removeLast() {
        if (this.tail == null) {
            return null;
        }
        Payload payload = this.tail.getPayload();
        this.tail = this.tail.prev;
        if (this.tail != null) {
            this.tail.next = null; // Fix dangling next pointer
        } else {
            this.head = null; // List is now empty
        }
        this.size--;
        return payload;
    }

    /**
     * Returns the number of elements in the list.
     * <p>
     * Time Complexity: O(1)
     * 
     * @return the current size of the list
     */
    public int size() {
        return this.size;
    }

    /**
     * Checks if the list contains no elements.
     * <p>
     * Time Complexity: O(1)
     * 
     * @return true if the list is empty, false otherwise
     */
    public boolean isEmpty() {
        return this.size == 0;
    }

    /**
     * Searches for the first occurrence of the specified element in the list.
     * <p>
     * Traverses the list from head to tail, comparing each element using
     * {@link Object#equals(Object)}. If a match is found, the element is returned.
     * If no match is found, null is returned.
     * <p>
     * Time Complexity: O(n) where n is the number of elements in the list
     * 
     * @param payload the element to search for in the list
     * @return the found element if it exists in the list, null otherwise
     */
    public Payload find(Payload payload) {
        DoublyLinkedListNode<Payload> currentNode = this.head;
        while (currentNode != null) {
            if (currentNode.getPayload().equals(payload)) {
                return currentNode.getPayload();
            }
            currentNode = currentNode.next;
        }
        return null;
    }

    /**
     * Returns the head node of the list.
     * <p>
     * Time Complexity: O(1)
     * 
     * @return the head node of the list, or null if the list is empty
     */
    public DoublyLinkedListNode<Payload> getHead() {
        return this.head;
    }

    /**
     * Returns the tail node of the list.
     * <p>
     * Time Complexity: O(1)
     * 
     * @return the tail node of the list, or null if the list is empty
     */
    public DoublyLinkedListNode<Payload> getTail() {
        return this.tail;
    }

}
