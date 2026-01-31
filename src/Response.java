public class Response {
    private final String endpoint;
    private final boolean isError;
    private final String message;

    public Response(String endpoint, boolean isError, String message) {
        this.endpoint = endpoint;
        this.isError = isError;
        this.message = message;
    }

    public static Response success(String endpoint, String message) {
        return new Response(endpoint, false, message);
    }

    public static Response failure(String endpoint, String message) {
        // System.out.println(endpoint + ", message -> " + message);
        return new Response(endpoint, true, message);
    }

    public String generateResponseMessage() {
        if (this.isError) {
            return generateEndpointError(this.endpoint);
        }
        return this.message;
    }

    public static String generateEndpointError(String endpoint) {
        return "Some error occurred in " + endpoint + ".";
    }

    public static String endpointNotFound() {
        return "ENDPOINT_NOT_FOUND";
    }
}
