import java.util.LinkedList;

public record Route(int totalLatency, LinkedList<Host> intersections) {

}
