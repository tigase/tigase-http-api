package tigase.http.util;

public class StringUtils {

    public static String trimString(Object input, int maxLength) {
        if (input == null) {
            return null;
        }
        String inputString = input.toString();

        return inputString.length() <= maxLength ? inputString : inputString.substring(0, maxLength) + "…";
    }
}
