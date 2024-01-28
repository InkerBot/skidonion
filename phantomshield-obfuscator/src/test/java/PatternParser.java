public class PatternParser {
    public static void main(String[] args) {
        String pattern = "methodd(asdasdsadsaddassdsa,dsadsadsadas,dsadsadasdsa)";
        System.out.println(pattern.indexOf("("));
        System.out.println(pattern.substring(7));
    }
}
